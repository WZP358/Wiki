package com.wiki.app.collab;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.wiki.app.doc.DocumentService;
import com.wiki.app.doc.dto.RealtimeDocSnapshot;
import com.wiki.app.security.CurrentUser;
import com.wiki.app.security.JwtTokenProvider;
import com.wiki.app.user.UserAccount;
import com.wiki.app.user.UserRepository;
import io.jsonwebtoken.Claims;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.*;
import org.springframework.web.socket.handler.TextWebSocketHandler;

import java.io.IOException;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

@Component
public class CollabWebSocketHandler extends TextWebSocketHandler {
    private final ObjectMapper objectMapper;
    private final JwtTokenProvider jwtTokenProvider;
    private final DocumentService documentService;
    private final UserRepository userRepository;

    private final Map<String, WebSocketSession> sessions = new ConcurrentHashMap<>();
    private final Map<String, CurrentUser> users = new ConcurrentHashMap<>();
    private final Map<String, Long> sessionDoc = new ConcurrentHashMap<>();
    private final Map<Long, CollabRoom> rooms = new ConcurrentHashMap<>();

    public CollabWebSocketHandler(ObjectMapper objectMapper,
                                  JwtTokenProvider jwtTokenProvider,
                                  DocumentService documentService,
                                  UserRepository userRepository) {
        this.objectMapper = objectMapper;
        this.jwtTokenProvider = jwtTokenProvider;
        this.documentService = documentService;
        this.userRepository = userRepository;
    }

    @Override
    public void afterConnectionEstablished(WebSocketSession session) throws Exception {
        CurrentUser user = authenticate(session);
        if (user == null) {
            session.close(CloseStatus.NOT_ACCEPTABLE.withReason("unauthorized"));
            return;
        }
        sessions.put(session.getId(), session);
        users.put(session.getId(), user);
    }

    @Override
    protected void handleTextMessage(WebSocketSession session, TextMessage message) throws Exception {
        JsonNode node = objectMapper.readTree(message.getPayload());
        String type = node.path("type").asText();
        switch (type) {
            case "join" -> handleJoin(session, node);
            case "update" -> handleUpdate(session, node);
            case "cursor" -> handleCursor(session, node);
            case "leave" -> leaveRoom(session.getId());
            default -> send(session, Map.of("type", "error", "message", "unknown message type"));
        }
    }

    @Override
    public void handleTransportError(WebSocketSession session, Throwable exception) throws Exception {
        leaveRoom(session.getId());
        session.close(CloseStatus.SERVER_ERROR);
    }

    @Override
    public void afterConnectionClosed(WebSocketSession session, CloseStatus status) {
        leaveRoom(session.getId());
        sessions.remove(session.getId());
        users.remove(session.getId());
        sessionDoc.remove(session.getId());
    }

    private void handleJoin(WebSocketSession session, JsonNode node) throws IOException {
        CurrentUser user = users.get(session.getId());
        if (user == null) {
            return;
        }

        long docId = node.path("docId").asLong();
        RealtimeDocSnapshot snapshot = documentService.realtimeSnapshot(docId, user);

        CollabRoom room = rooms.computeIfAbsent(docId,
                ignored -> new CollabRoom(snapshot.getDocId(), snapshot.getTitle(), snapshot.getMarkdownContent(), snapshot.getVersionNo()));

        room.getLock().lock();
        try {
            if (snapshot.getVersionNo() > room.getVersion()) {
                room.update(snapshot.getTitle(), snapshot.getMarkdownContent(), snapshot.getVersionNo(), null);
            }

            UserAccount account = userRepository.findById(user.getUserId()).orElse(null);
            CollabParticipant participant = CollabParticipant.builder()
                    .sessionId(session.getId())
                    .userId(user.getUserId())
                    .username(user.getUsername())
                    .avatarUrl(account == null ? null : account.getAvatarUrl())
                    .cursorStart(node.path("cursorStart").asInt(0))
                    .cursorEnd(node.path("cursorEnd").asInt(0))
                    .build();

            room.getParticipants().put(session.getId(), participant);
            sessionDoc.put(session.getId(), docId);

            send(session, Map.of(
                    "type", "init",
                    "docId", docId,
                    "mySessionId", session.getId(),
                    "title", room.getTitle(),
                    "content", room.getContent(),
                    "version", room.getVersion(),
                    "participants", room.getParticipants().values()
            ));

            broadcastOthers(docId, session.getId(), Map.of(
                    "type", "user_joined",
                    "docId", docId,
                    "participant", participant
            ));
        } finally {
            room.getLock().unlock();
        }
    }

    private void handleUpdate(WebSocketSession session, JsonNode node) throws IOException {
        CurrentUser user = users.get(session.getId());
        Long docId = sessionDoc.get(session.getId());
        if (user == null || docId == null) {
            return;
        }

        CollabRoom room = rooms.get(docId);
        if (room == null) {
            return;
        }

        room.getLock().lock();
        try {
            int baseVersion = node.path("baseVersion").asInt(room.getVersion());
            String incomingTitle = node.hasNonNull("title") ? node.path("title").asText() : room.getTitle();
            String incomingContent = node.hasNonNull("content") ? node.path("content").asText() : room.getContent();

            boolean autoMerged = false;
            String baseContent = room.getVersionContent().get(baseVersion);
            if (baseContent == null) {
                sendConflict(session, room, incomingContent, "当前变更跨度过大，请手动处理冲突");
                return;
            }

            OtTextOperation op = OtTextOperation.between(baseContent, incomingContent);
            if (op == null) {
                return;
            }

            if (baseVersion != room.getVersion()) {
                for (int v = baseVersion + 1; v <= room.getVersion(); v++) {
                    OtTextOperation remoteOp = room.getVersionOps().get(v);
                    OtTextOperation transformed = op.transformAgainst(remoteOp);
                    if (transformed == null) {
                        sendConflict(session, room, incomingContent, "检测到重叠修改，需手动处理冲突");
                        return;
                    }
                    op = transformed;
                }
                autoMerged = true;
            }

            String resolved;
            try {
                resolved = op.apply(room.getContent());
            } catch (Exception ex) {
                sendConflict(session, room, incomingContent, "自动合并失败，请手动解决冲突");
                return;
            }

            RealtimeDocSnapshot saved = documentService.applyRealtimeUpdate(
                    docId,
                    incomingTitle,
                    resolved,
                    user,
                    resolveIp(session),
                    autoMerged ? "自动合并协作更新" : "协作更新"
            );

            room.update(saved.getTitle(), saved.getMarkdownContent(), saved.getVersionNo(), op);
            broadcastAll(docId, Map.of(
                    "type", "update_applied",
                    "docId", docId,
                    "title", saved.getTitle(),
                    "content", saved.getMarkdownContent(),
                    "version", saved.getVersionNo(),
                    "autoMerged", autoMerged,
                    "manualResolve", false,
                    "by", Map.of("userId", user.getUserId(), "username", user.getUsername())
            ));
        } finally {
            room.getLock().unlock();
        }
    }

    private void handleCursor(WebSocketSession session, JsonNode node) {
        Long docId = sessionDoc.get(session.getId());
        if (docId == null) {
            return;
        }
        CollabRoom room = rooms.get(docId);
        if (room == null) {
            return;
        }

        CollabParticipant participant = room.getParticipants().get(session.getId());
        if (participant == null) {
            return;
        }

        participant.setCursorStart(node.path("cursorStart").asInt(0));
        participant.setCursorEnd(node.path("cursorEnd").asInt(0));

        broadcastOthers(docId, session.getId(), Map.of(
                "type", "cursor",
                "docId", docId,
                "participant", participant
        ));
    }

    private void sendConflict(WebSocketSession session, CollabRoom room, String clientContent, String reason) throws IOException {
        String suggested = "<<<<<<< 本地修改\n" + clientContent + "\n=======\n" + room.getContent() + "\n>>>>>>> 远端版本";
        send(session, Map.of(
                "type", "conflict",
                "docId", room.getDocId(),
                "serverVersion", room.getVersion(),
                "serverContent", room.getContent(),
                "clientContent", clientContent,
                "suggestedContent", suggested,
                "manualResolve", true,
                "message", reason
        ));
    }

    private void leaveRoom(String sessionId) {
        Long docId = sessionDoc.remove(sessionId);
        if (docId == null) {
            return;
        }
        CollabRoom room = rooms.get(docId);
        if (room == null) {
            return;
        }

        CollabParticipant removed = room.getParticipants().remove(sessionId);
        if (removed != null) {
            broadcastOthers(docId, sessionId, Map.of(
                    "type", "user_left",
                    "docId", docId,
                    "participant", removed
            ));
        }

        if (room.getParticipants().isEmpty()) {
            rooms.remove(docId);
        }
    }

    private void broadcastAll(Long docId, Map<String, Object> payload) {
        CollabRoom room = rooms.get(docId);
        if (room == null) {
            return;
        }
        for (String sid : room.getParticipants().keySet()) {
            WebSocketSession ws = sessions.get(sid);
            if (ws == null) {
                continue;
            }
            try {
                send(ws, payload);
            } catch (IOException ignored) {
            }
        }
    }

    private void broadcastOthers(Long docId, String currentSessionId, Map<String, Object> payload) {
        CollabRoom room = rooms.get(docId);
        if (room == null) {
            return;
        }
        for (String sid : room.getParticipants().keySet()) {
            if (sid.equals(currentSessionId)) {
                continue;
            }
            WebSocketSession ws = sessions.get(sid);
            if (ws == null) {
                continue;
            }
            try {
                send(ws, payload);
            } catch (IOException ignored) {
            }
        }
    }

    private void send(WebSocketSession session, Map<String, Object> payload) throws IOException {
        if (session.isOpen()) {
            session.sendMessage(new TextMessage(objectMapper.writeValueAsString(payload)));
        }
    }

    private CurrentUser authenticate(WebSocketSession session) {
        String token = getQueryParam(session, "token");
        if (token == null || token.isBlank()) {
            return null;
        }
        try {
            Claims claims = jwtTokenProvider.parse(token);
            return new CurrentUser(
                    claims.get("uid", Long.class),
                    claims.get("username", String.class),
                    claims.get("role", String.class)
            );
        } catch (Exception ignored) {
            return null;
        }
    }

    private String getQueryParam(WebSocketSession session, String key) {
        if (session.getUri() == null || session.getUri().getQuery() == null) {
            return null;
        }
        String[] items = session.getUri().getQuery().split("&");
        for (String item : items) {
            String[] pair = item.split("=", 2);
            if (pair.length == 2 && key.equals(pair[0])) {
                return URLDecoder.decode(pair[1], StandardCharsets.UTF_8);
            }
        }
        return null;
    }

    private String resolveIp(WebSocketSession session) {
        if (session.getRemoteAddress() == null || session.getRemoteAddress().getAddress() == null) {
            return "unknown";
        }
        return session.getRemoteAddress().getAddress().getHostAddress();
    }
}
