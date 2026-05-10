package com.wiki.app.engagement;

import com.wiki.app.comment.DocumentComment;
import com.wiki.app.comment.DocumentCommentRepository;
import com.wiki.app.comment.DocumentCommentService;
import com.wiki.app.comment.dto.CommentResponse;
import com.wiki.app.comment.dto.CreateCommentRequest;
import com.wiki.app.common.BusinessException;
import com.wiki.app.common.ErrorCode;
import com.wiki.app.common.SnowflakeIdGenerator;
import com.wiki.app.doc.DocumentService;
import com.wiki.app.doc.WikiDocument;
import com.wiki.app.doc.WikiDocumentRepository;
import com.wiki.app.favorite.FavoriteDocument;
import com.wiki.app.favorite.FavoriteDocumentRepository;
import com.wiki.app.favorite.FavoriteService;
import com.wiki.app.kb.KnowledgeBaseRepository;
import com.wiki.app.log.OperationLogService;
import com.wiki.app.reaction.DocumentReaction;
import com.wiki.app.reaction.DocumentReactionRepository;
import com.wiki.app.reaction.DocumentReactionService;
import com.wiki.app.reaction.ReactionType;
import com.wiki.app.security.CurrentUser;
import com.wiki.app.user.UserAccount;
import com.wiki.app.user.UserRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class EngagementServicesTest {

    @Mock
    private DocumentCommentRepository commentRepository;
    @Mock
    private WikiDocumentRepository documentRepository;
    @Mock
    private DocumentService documentService;
    @Mock
    private UserRepository userRepository;
    @Mock
    private SnowflakeIdGenerator idGenerator;
    @Mock
    private OperationLogService operationLogService;
    @Mock
    private DocumentReactionRepository reactionRepository;
    @Mock
    private FavoriteDocumentRepository favoriteRepository;
    @Mock
    private KnowledgeBaseRepository kbRepository;

    private final CurrentUser alice = new CurrentUser(10L, "alice", "USER");

    @Test
    void commentCreationRequiresDocumentReadPermission() {
        DocumentCommentService service = new DocumentCommentService(
                commentRepository, documentRepository, documentService, userRepository, idGenerator, operationLogService);
        CreateCommentRequest request = new CreateCommentRequest();
        request.setContent("hello");

        when(documentService.requireReadable(99L, alice))
                .thenThrow(new BusinessException(ErrorCode.FORBIDDEN, "no access"));

        assertThatThrownBy(() -> service.create(99L, request, alice, "127.0.0.1"))
                .isInstanceOf(BusinessException.class)
                .extracting("code")
                .isEqualTo(ErrorCode.FORBIDDEN);
        verify(commentRepository, never()).save(any(DocumentComment.class));
    }

    @Test
    void commentListBuildsThreadedResponsesAfterReadPermissionPasses() {
        DocumentCommentService service = new DocumentCommentService(
                commentRepository, documentRepository, documentService, userRepository, idGenerator, operationLogService);
        WikiDocument doc = doc(99L, 7L, "Doc");
        DocumentComment top = comment(1L, 99L, null, "top", 10L);
        DocumentComment reply = comment(2L, 99L, 1L, "reply", 11L);
        UserAccount author = user(10L, "alice", "Alice");
        UserAccount replier = user(11L, "bob", "Bob");

        when(documentService.requireReadable(99L, alice)).thenReturn(doc);
        when(documentRepository.findById(99L)).thenReturn(Optional.of(doc));
        when(commentRepository.findByDocumentIdAndParentIdIsNullAndDeletedAtIsNullOrderByCreatedAtDesc(99L))
                .thenReturn(List.of(top));
        when(commentRepository.findByParentIdAndDeletedAtIsNullOrderByCreatedAtAsc(1L)).thenReturn(List.of(reply));
        when(userRepository.findById(10L)).thenReturn(Optional.of(author));
        when(userRepository.findById(11L)).thenReturn(Optional.of(replier));

        List<CommentResponse> responses = service.listByDocument(99L, alice);

        assertThat(responses).hasSize(1);
        assertThat(responses.get(0).getAuthorName()).isEqualTo("Alice");
        assertThat(responses.get(0).getReplies()).hasSize(1);
        assertThat(responses.get(0).getReplies().get(0).getAuthorName()).isEqualTo("Bob");
    }

    @Test
    void reactionToggleRequiresDocumentReadPermission() {
        DocumentReactionService service = new DocumentReactionService(
                reactionRepository, documentRepository, documentService, idGenerator, operationLogService);
        when(documentService.requireReadable(99L, alice))
                .thenThrow(new BusinessException(ErrorCode.FORBIDDEN, "no access"));

        assertThatThrownBy(() -> service.toggle(99L, ReactionType.LIKE, alice, "127.0.0.1"))
                .isInstanceOf(BusinessException.class)
                .extracting("code")
                .isEqualTo(ErrorCode.FORBIDDEN);
        verify(reactionRepository, never()).save(any(DocumentReaction.class));
    }

    @Test
    void favoriteCreationRequiresDocumentReadPermissionAndSkipsDuplicates() {
        FavoriteService service = new FavoriteService(favoriteRepository, documentRepository, kbRepository, documentService);
        when(favoriteRepository.existsByUserIdAndDocId(alice.getUserId(), 99L)).thenReturn(false);
        when(documentService.requireReadable(99L, alice)).thenReturn(doc(99L, 7L, "Doc"));

        service.addFavorite(alice, 99L);

        verify(favoriteRepository).save(any(FavoriteDocument.class));
    }

    @Test
    void unreadableFavoritesAreFilteredFromLists() {
        FavoriteService service = new FavoriteService(favoriteRepository, documentRepository, kbRepository, documentService);
        FavoriteDocument readable = favorite(1L, 10L, 99L, 7L);
        FavoriteDocument hidden = favorite(2L, 10L, 100L, 7L);
        when(favoriteRepository.findByUserId(alice.getUserId())).thenReturn(List.of(readable, hidden));
        when(documentService.requireReadable(99L, alice)).thenReturn(doc(99L, 7L, "Readable"));
        when(documentService.requireReadable(100L, alice))
                .thenThrow(new BusinessException(ErrorCode.FORBIDDEN, "no access"));
        when(documentRepository.findById(99L)).thenReturn(Optional.of(doc(99L, 7L, "Readable")));
        when(kbRepository.findById(7L)).thenReturn(Optional.empty());

        assertThat(service.getAllFavorites(alice)).hasSize(1);
    }

    private WikiDocument doc(Long id, Long kbId, String title) {
        WikiDocument doc = new WikiDocument();
        doc.setId(id);
        doc.setKbId(kbId);
        doc.setTitle(title);
        doc.setMarkdownContent("content");
        doc.setHtmlContent("<p>content</p>");
        doc.setOwnerId(1L);
        doc.setViewCount(0L);
        doc.setVersionNo(1);
        return doc;
    }

    private DocumentComment comment(Long id, Long docId, Long parentId, String content, Long authorId) {
        DocumentComment comment = new DocumentComment();
        comment.setId(id);
        comment.setDocumentId(docId);
        comment.setParentId(parentId);
        comment.setContent(content);
        comment.setAuthorId(authorId);
        comment.setLikeCount(0);
        comment.setIsResolved(false);
        return comment;
    }

    private UserAccount user(Long id, String username, String nickname) {
        UserAccount user = new UserAccount();
        user.setId(id);
        user.setUsername(username);
        user.setNickname(nickname);
        return user;
    }

    private FavoriteDocument favorite(Long id, Long userId, Long docId, Long kbId) {
        FavoriteDocument favorite = new FavoriteDocument();
        favorite.setId(id);
        favorite.setUserId(userId);
        favorite.setDocId(docId);
        favorite.setKbId(kbId);
        favorite.setCreatedAt(LocalDateTime.now());
        return favorite;
    }
}
