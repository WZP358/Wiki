package com.wiki.app.comment;

import com.wiki.app.common.BusinessException;
import com.wiki.app.common.ErrorCode;
import com.wiki.app.common.SnowflakeIdGenerator;
import com.wiki.app.comment.dto.CommentResponse;
import com.wiki.app.comment.dto.CreateCommentRequest;
import com.wiki.app.doc.DocumentService;
import com.wiki.app.doc.WikiDocument;
import com.wiki.app.doc.WikiDocumentRepository;
import com.wiki.app.log.OperationLogService;
import com.wiki.app.security.CurrentUser;
import com.wiki.app.user.UserAccount;
import com.wiki.app.user.UserRepository;
import jakarta.transaction.Transactional;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
public class DocumentCommentService {
    private final DocumentCommentRepository commentRepository;
    private final WikiDocumentRepository documentRepository;
    private final UserRepository userRepository;
    private final SnowflakeIdGenerator idGenerator;
    private final OperationLogService operationLogService;

    public DocumentCommentService(DocumentCommentRepository commentRepository,
                                 WikiDocumentRepository documentRepository,
                                 UserRepository userRepository,
                                 SnowflakeIdGenerator idGenerator,
                                 OperationLogService operationLogService) {
        this.commentRepository = commentRepository;
        this.documentRepository = documentRepository;
        this.userRepository = userRepository;
        this.idGenerator = idGenerator;
        this.operationLogService = operationLogService;
    }

    @Transactional
    public CommentResponse create(Long documentId, CreateCommentRequest request, CurrentUser user, String ip) {
        WikiDocument document = documentRepository.findById(documentId)
                .orElseThrow(() -> new BusinessException(ErrorCode.NOT_FOUND, "文档不存在"));

        if (document.getDeletedAt() != null) {
            throw new BusinessException(ErrorCode.NOT_FOUND, "文档已删除");
        }

        if (request.getParentId() != null) {
            DocumentComment parent = commentRepository.findById(request.getParentId())
                    .orElseThrow(() -> new BusinessException(ErrorCode.NOT_FOUND, "父评论不存在"));
            if (parent.getDeletedAt() != null) {
                throw new BusinessException(ErrorCode.NOT_FOUND, "父评论已删除");
            }
        }

        DocumentComment comment = new DocumentComment();
        comment.setId(idGenerator.nextId());
        comment.setDocumentId(documentId);
        comment.setAuthorId(user.getUserId());
        comment.setParentId(request.getParentId());
        comment.setContent(request.getContent());
        comment.setLikeCount(0);
        comment.setIsResolved(false);

        comment = commentRepository.save(comment);

        operationLogService.record(user.getUserId(), user.getUsername(), "CREATE_COMMENT",
                "DOCUMENT", documentId.toString(), ip, "评论文档: " + document.getTitle());

        return toResponse(comment);
    }

    public List<CommentResponse> listByDocument(Long documentId, CurrentUser user) {
        WikiDocument document = documentRepository.findById(documentId)
                .orElseThrow(() -> new BusinessException(ErrorCode.NOT_FOUND, "文档不存在"));

        List<DocumentComment> topLevelComments = commentRepository
                .findByDocumentIdAndParentIdIsNullAndDeletedAtIsNullOrderByCreatedAtDesc(documentId);

        return topLevelComments.stream()
                .map(comment -> {
                    CommentResponse response = toResponse(comment);
                    response.setReplies(loadReplies(comment.getId()));
                    return response;
                })
                .collect(Collectors.toList());
    }

    private List<CommentResponse> loadReplies(Long parentId) {
        List<DocumentComment> replies = commentRepository
                .findByParentIdAndDeletedAtIsNullOrderByCreatedAtAsc(parentId);
        return replies.stream().map(this::toResponse).collect(Collectors.toList());
    }

    @Transactional
    public CommentResponse update(Long commentId, CreateCommentRequest request, CurrentUser user, String ip) {
        DocumentComment comment = commentRepository.findById(commentId)
                .orElseThrow(() -> new BusinessException(ErrorCode.NOT_FOUND, "评论不存在"));

        if (!comment.getAuthorId().equals(user.getUserId())) {
            throw new BusinessException(ErrorCode.FORBIDDEN, "只能编辑自己的评论");
        }

        comment.setContent(request.getContent());
        comment = commentRepository.save(comment);

        operationLogService.record(user.getUserId(), user.getUsername(), "UPDATE_COMMENT",
                "COMMENT", commentId.toString(), ip, "更新评论: " + commentId);

        return toResponse(comment);
    }

    @Transactional
    public void delete(Long commentId, CurrentUser user, String ip) {
        DocumentComment comment = commentRepository.findById(commentId)
                .orElseThrow(() -> new BusinessException(ErrorCode.NOT_FOUND, "评论不存在"));

        if (!comment.getAuthorId().equals(user.getUserId())) {
            throw new BusinessException(ErrorCode.FORBIDDEN, "只能删除自己的评论");
        }

        comment.setDeletedAt(java.time.LocalDateTime.now());
        commentRepository.save(comment);

        operationLogService.record(user.getUserId(), user.getUsername(), "DELETE_COMMENT",
                "COMMENT", commentId.toString(), ip, "删除评论: " + commentId);
    }

    @Transactional
    public void resolve(Long commentId, CurrentUser user, String ip) {
        DocumentComment comment = commentRepository.findById(commentId)
                .orElseThrow(() -> new BusinessException(ErrorCode.NOT_FOUND, "评论不存在"));

        comment.setIsResolved(true);
        comment.setResolvedBy(user.getUserId());
        comment.setResolvedAt(LocalDateTime.now());
        commentRepository.save(comment);

        operationLogService.record(user.getUserId(), user.getUsername(), "RESOLVE_COMMENT",
                "COMMENT", commentId.toString(), ip, "解决评论: " + commentId);
    }

    public Long countByDocument(Long documentId) {
        return commentRepository.countByDocumentId(documentId);
    }

    private CommentResponse toResponse(DocumentComment comment) {
        CommentResponse response = new CommentResponse();
        response.setId(String.valueOf(comment.getId()));
        response.setDocumentId(String.valueOf(comment.getDocumentId()));
        response.setAuthorId(String.valueOf(comment.getAuthorId()));
        response.setParentId(comment.getParentId() != null ? String.valueOf(comment.getParentId()) : null);
        response.setContent(comment.getContent());
        response.setLikeCount(comment.getLikeCount());
        response.setIsResolved(comment.getIsResolved());
        response.setResolvedBy(comment.getResolvedBy() != null ? String.valueOf(comment.getResolvedBy()) : null);
        response.setResolvedAt(comment.getResolvedAt());
        response.setCreatedAt(comment.getCreatedAt());
        response.setUpdatedAt(comment.getUpdatedAt());

        UserAccount author = userRepository.findById(comment.getAuthorId()).orElse(null);
        if (author != null) {
            response.setAuthorName(author.getNickname() != null ? author.getNickname() : author.getUsername());
            response.setAuthorAvatar(author.getAvatarUrl());
        }

        return response;
    }
}
