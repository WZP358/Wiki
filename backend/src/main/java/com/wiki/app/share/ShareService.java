package com.wiki.app.share;

import com.wiki.app.common.BusinessException;
import com.wiki.app.common.ErrorCode;
import com.wiki.app.common.SnowflakeIdGenerator;
import com.wiki.app.doc.DocumentService;
import com.wiki.app.doc.dto.DocumentResponse;
import com.wiki.app.log.OperationLogService;
import com.wiki.app.security.CurrentUser;
import com.wiki.app.share.dto.ShareResponse;
import jakarta.transaction.Transactional;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;

@Service
public class ShareService {
    private final ShareLinkRepository shareLinkRepository;
    private final DocumentService documentService;
    private final SnowflakeIdGenerator idGenerator;
    private final OperationLogService operationLogService;

    public ShareService(ShareLinkRepository shareLinkRepository,
                        DocumentService documentService,
                        SnowflakeIdGenerator idGenerator,
                        OperationLogService operationLogService) {
        this.shareLinkRepository = shareLinkRepository;
        this.documentService = documentService;
        this.idGenerator = idGenerator;
        this.operationLogService = operationLogService;
    }

    @Transactional
    public ShareResponse createDocShare(Long docId, CurrentUser user, String ip) {
        documentService.get(docId, user);

        ShareLink link = new ShareLink();
        link.setId(idGenerator.nextId());
        link.setToken(documentService.generateShareToken());
        link.setDocId(docId);
        link.setCreatorId(user.getUserId());
        link.setExpireAt(LocalDateTime.now().plusDays(7));
        shareLinkRepository.save(link);

        operationLogService.record(user.getUserId(), user.getUsername(), "SHARE_DOC", "DOC", docId.toString(), ip, "创建分享链接");
        return new ShareResponse(link.getToken(), "/api/shares/public/" + link.getToken());
    }

    public DocumentResponse publicView(String token) {
        ShareLink link = shareLinkRepository.findByToken(token)
                .orElseThrow(() -> new BusinessException(ErrorCode.NOT_FOUND, "分享链接不存在"));
        if (link.getExpireAt() != null && link.getExpireAt().isBefore(LocalDateTime.now())) {
            throw new BusinessException(ErrorCode.FORBIDDEN, "分享链接已过期");
        }
        return documentService.publicView(link.getDocId());
    }
}
