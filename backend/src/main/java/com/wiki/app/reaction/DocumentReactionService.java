package com.wiki.app.reaction;

import com.wiki.app.common.BusinessException;
import com.wiki.app.common.ErrorCode;
import com.wiki.app.common.SnowflakeIdGenerator;
import com.wiki.app.doc.WikiDocument;
import com.wiki.app.doc.WikiDocumentRepository;
import com.wiki.app.log.OperationLogService;
import com.wiki.app.reaction.dto.ReactionStatsResponse;
import com.wiki.app.security.CurrentUser;
import jakarta.transaction.Transactional;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
public class DocumentReactionService {
    private final DocumentReactionRepository reactionRepository;
    private final WikiDocumentRepository documentRepository;
    private final SnowflakeIdGenerator idGenerator;
    private final OperationLogService operationLogService;

    public DocumentReactionService(DocumentReactionRepository reactionRepository,
                                  WikiDocumentRepository documentRepository,
                                  SnowflakeIdGenerator idGenerator,
                                  OperationLogService operationLogService) {
        this.reactionRepository = reactionRepository;
        this.documentRepository = documentRepository;
        this.idGenerator = idGenerator;
        this.operationLogService = operationLogService;
    }

    @Transactional
    public void toggle(Long documentId, ReactionType reactionType, CurrentUser user, String ip) {
        WikiDocument document = documentRepository.findById(documentId)
                .orElseThrow(() -> new BusinessException(ErrorCode.NOT_FOUND, "文档不存在"));

        if (document.getDeletedAt() != null) {
            throw new BusinessException(ErrorCode.NOT_FOUND, "文档已删除");
        }

        var existing = reactionRepository.findByDocumentIdAndUserIdAndReactionType(
                documentId, user.getUserId(), reactionType);

        if (existing.isPresent()) {
            DocumentReaction reaction = existing.get();
            if (reaction.getDeletedAt() == null) {
                reaction
                        .setDeletedAt(java.time.LocalDateTime.now());
                reactionRepository.save(reaction);
                operationLogService.record(user.getUserId(), user.getUsername(), "REMOVE_REACTION",
                        "DOCUMENT", documentId.toString(), ip, "取消反应: " + reactionType);
            } else {
                reaction.setDeletedAt(null);
                reactionRepository.save(reaction);
                operationLogService.record(user.getUserId(), user.getUsername(), "ADD_REACTION",
                        "DOCUMENT", documentId.toString(), ip, "添加反应: " + reactionType);
            }
        } else {
            DocumentReaction reaction = new DocumentReaction();
            reaction.setId(idGenerator.nextId());
            reaction.setDocumentId(documentId);
            reaction.setUserId(user.getUserId());
            reaction.setReactionType(reactionType);
            reactionRepository.save(reaction);
            operationLogService.record(user.getUserId(), user.getUsername(), "ADD_REACTION",
                    "DOCUMENT", documentId.toString(), ip, "添加反应: " + reactionType);
        }
    }

    public ReactionStatsResponse getStats(Long documentId, CurrentUser user) {
        List<Object[]> counts = reactionRepository.countByDocumentIdGroupByType(documentId);

        Map<ReactionType, Long> countMap = new HashMap<>();
        for (Object[] row : counts) {
            countMap.put((ReactionType) row[0], (Long) row[1]);
        }

        Map<ReactionType, Boolean> userReactedMap = new HashMap<>();
        for (ReactionType type : ReactionType.values()) {
            userReactedMap.put(type, reactionRepository.existsByDocumentIdAndUserIdAndReactionTypeAndDeletedAtIsNull(
                    documentId, user.getUserId(), type));
        }

        ReactionStatsResponse response = new ReactionStatsResponse();
        response.setCounts(countMap);
        response.setUserReacted(userReactedMap);
        return response;
    }
}
