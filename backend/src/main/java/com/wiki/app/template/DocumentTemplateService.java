package com.wiki.app.template;

import com.wiki.app.common.BusinessException;
import com.wiki.app.common.ErrorCode;
import com.wiki.app.common.SnowflakeIdGenerator;
import com.wiki.app.kb.KnowledgeBaseService;
import com.wiki.app.log.OperationLogService;
import com.wiki.app.security.CurrentUser;
import com.wiki.app.template.dto.CreateTemplateRequest;
import com.wiki.app.template.dto.TemplateResponse;
import com.wiki.app.user.UserAccount;
import com.wiki.app.user.UserRepository;
import jakarta.transaction.Transactional;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.stream.Collectors;

@Service
public class DocumentTemplateService {
    private final DocumentTemplateRepository templateRepository;
    private final KnowledgeBaseService knowledgeBaseService;
    private final UserRepository userRepository;
    private final SnowflakeIdGenerator idGenerator;
    private final OperationLogService operationLogService;

    public DocumentTemplateService(DocumentTemplateRepository templateRepository,
                                  KnowledgeBaseService knowledgeBaseService,
                                  UserRepository userRepository,
                                  SnowflakeIdGenerator idGenerator,
                                  OperationLogService operationLogService) {
        this.templateRepository = templateRepository;
        this.knowledgeBaseService = knowledgeBaseService;
        this.userRepository = userRepository;
        this.idGenerator = idGenerator;
        this.operationLogService = operationLogService;
    }

    @Transactional
    public TemplateResponse create(CreateTemplateRequest request, CurrentUser user, String ip) {
        if (request.getKbId() != null) {
            knowledgeBaseService.ensureKbEditor(request.getKbId(), user);
        }

        DocumentTemplate template = new DocumentTemplate();
        template.setId(idGenerator.nextId());
        template.setName(request.getName());
        template.setDescription(request.getDescription());
        template.setKbId(request.getKbId());
        template.setCreatorId(user.getUserId());
        template.setMarkdownContent(request.getMarkdownContent());
        template.setIsPublic(request.getIsPublic() != null ? request.getIsPublic() : false);
        template.setCategory(request.getCategory());
        template.setCoverUrl(request.getCoverUrl());
        template.setUseCount(0);

        template = templateRepository.save(template);

        operationLogService.record(user.getUserId(), user.getUsername(), "CREATE_TEMPLATE",
                "TEMPLATE", template.getId().toString(), ip, "创建模板: " + template.getName());

        return toResponse(template);
    }

    public List<TemplateResponse> listAvailable(Long kbId, CurrentUser user) {
        List<DocumentTemplate> templates = templateRepository.findAvailableTemplates(kbId, user.getUserId());
        return templates.stream().map(this::toResponse).collect(Collectors.toList());
    }

    public List<TemplateResponse> listByCategory(String category, Long kbId, CurrentUser user) {
        List<DocumentTemplate> templates = templateRepository.findByCategory(category, kbId, user.getUserId());
        return templates.stream().map(this::toResponse).collect(Collectors.toList());
    }

    public TemplateResponse get(Long templateId, CurrentUser user) {
        DocumentTemplate template = templateRepository.findById(templateId)
                .orElseThrow(() -> new BusinessException(ErrorCode.NOT_FOUND, "模板不存在"));

        if (template.getDeletedAt() != null) {
            throw new BusinessException(ErrorCode.NOT_FOUND, "模板已删除");
        }

        if (!template.getIsPublic() &&
            !template.getCreatorId().equals(user.getUserId()) &&
            (template.getKbId() != null)) {
            try {
                knowledgeBaseService.ensureKbVisible(template.getKbId(), user);
            } catch (BusinessException e) {
                throw new BusinessException(ErrorCode.FORBIDDEN, "无权访问此模板");
            }
        }

        return toResponse(template);
    }

    @Transactional
    public TemplateResponse update(Long templateId, CreateTemplateRequest request, CurrentUser user, String ip) {
        DocumentTemplate template = templateRepository.findById(templateId)
                .orElseThrow(() -> new BusinessException(ErrorCode.NOT_FOUND, "模板不存在"));

        if (!template.getCreatorId().equals(user.getUserId())) {
            throw new BusinessException(ErrorCode.FORBIDDEN, "只有创建者可以编辑模板");
        }

        template.setName(request.getName());
        template.setDescription(request.getDescription());
        template.setMarkdownContent(request.getMarkdownContent());
        template.setIsPublic(request.getIsPublic() != null ? request.getIsPublic() : template.getIsPublic());
        template.setCategory(request.getCategory());
        template.setCoverUrl(request.getCoverUrl());

        template = templateRepository.save(template);

        operationLogService.record(user.getUserId(), user.getUsername(), "UPDATE_TEMPLATE",
                "TEMPLATE", template.getId().toString(), ip, "更新模板: " + template.getName());

        return toResponse(template);
    }

    @Transactional
    public void delete(Long templateId, CurrentUser user, String ip) {
        DocumentTemplate template = templateRepository.findById(templateId)
                .orElseThrow(() -> new BusinessException(ErrorCode.NOT_FOUND, "模板不存在"));

        if (!template.getCreatorId().equals(user.getUserId())) {
            throw new BusinessException(ErrorCode.FORBIDDEN, "只有创建者可以删除模板");
        }

        template.setDeletedAt(java.time.LocalDateTime.now());
        templateRepository.save(template);

        operationLogService.record(user.getUserId(), user.getUsername(), "DELETE_TEMPLATE",
                "TEMPLATE", template.getId().toString(), ip, "删除模板: " + template.getName());
    }

    @Transactional
    public void incrementUseCount(Long templateId) {
        DocumentTemplate template = templateRepository.findById(templateId).orElse(null);
        if (template != null && template.getDeletedAt() == null) {
            template.setUseCount(template.getUseCount() + 1);
            templateRepository.save(template);
        }
    }

    private TemplateResponse toResponse(DocumentTemplate template) {
        TemplateResponse response = new TemplateResponse();
        response.setId(String.valueOf(template.getId()));
        response.setName(template.getName());
        response.setDescription(template.getDescription());
        response.setKbId(template.getKbId() != null ? String.valueOf(template.getKbId()) : null);
        response.setCreatorId(String.valueOf(template.getCreatorId()));
        response.setMarkdownContent(template.getMarkdownContent());
        response.setIsPublic(template.getIsPublic());
        response.setUseCount(template.getUseCount());
        response.setCategory(template.getCategory());
        response.setCoverUrl(template.getCoverUrl());
        response.setCreatedAt(template.getCreatedAt());
        response.setUpdatedAt(template.getUpdatedAt());

        UserAccount creator = userRepository.findById(template.getCreatorId()).orElse(null);
        if (creator != null) {
            response.setCreatorName(creator.getNickname() != null ? creator.getNickname() : creator.getUsername());
        }

        return response;
    }
}
