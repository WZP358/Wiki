package com.wiki.app.content;

import com.wiki.app.common.BusinessException;
import com.wiki.app.common.ErrorCode;
import com.wiki.app.common.SnowflakeIdGenerator;
import com.wiki.app.doc.DocumentService;
import com.wiki.app.doc.MarkdownService;
import com.wiki.app.doc.dto.DocumentResponse;
import com.wiki.app.kb.KnowledgeBaseService;
import com.wiki.app.log.OperationLogService;
import com.wiki.app.security.CurrentUser;
import com.wiki.app.share.ShareLink;
import com.wiki.app.share.ShareLinkRepository;
import com.wiki.app.share.ShareService;
import com.wiki.app.share.dto.ShareResponse;
import com.wiki.app.template.DocumentTemplate;
import com.wiki.app.template.DocumentTemplateRepository;
import com.wiki.app.template.DocumentTemplateService;
import com.wiki.app.template.dto.CreateTemplateRequest;
import com.wiki.app.user.UserAccount;
import com.wiki.app.user.UserRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ContentUtilityServicesTest {

    @Mock
    private ShareLinkRepository shareLinkRepository;
    @Mock
    private DocumentService documentService;
    @Mock
    private SnowflakeIdGenerator idGenerator;
    @Mock
    private OperationLogService operationLogService;
    @Mock
    private DocumentTemplateRepository templateRepository;
    @Mock
    private KnowledgeBaseService knowledgeBaseService;
    @Mock
    private UserRepository userRepository;

    private final CurrentUser alice = new CurrentUser(10L, "alice", "USER");

    @Test
    void shareCreationRequiresReadableDocumentAndPersistsSevenDayLink() {
        ShareService service = new ShareService(shareLinkRepository, documentService, idGenerator, operationLogService);
        DocumentResponse doc = DocumentResponse.builder().id(99L).title("Doc").build();
        when(documentService.get(99L, alice, "127.0.0.1", null)).thenReturn(doc);
        when(idGenerator.nextId()).thenReturn(500L);
        when(documentService.generateShareToken()).thenReturn("abc123");

        ShareResponse response = service.createDocShare(99L, alice, "127.0.0.1");

        assertThat(response.getToken()).isEqualTo("abc123");
        assertThat(response.getUrl()).isEqualTo("/api/shares/public/abc123");
        verify(shareLinkRepository).save(any(ShareLink.class));
    }

    @Test
    void expiredShareLinkCannotBeViewed() {
        ShareService service = new ShareService(shareLinkRepository, documentService, idGenerator, operationLogService);
        ShareLink link = new ShareLink();
        link.setId(1L);
        link.setToken("expired");
        link.setDocId(99L);
        link.setCreatorId(10L);
        link.setExpireAt(LocalDateTime.now().minusMinutes(1));
        when(shareLinkRepository.findByToken("expired")).thenReturn(Optional.of(link));

        assertThatThrownBy(() -> service.publicView("expired"))
                .isInstanceOf(BusinessException.class)
                .extracting("code")
                .isEqualTo(ErrorCode.FORBIDDEN);
    }

    @Test
    void templateCreateForKnowledgeBaseRequiresEditorPermission() {
        DocumentTemplateService service = new DocumentTemplateService(
                templateRepository, knowledgeBaseService, userRepository, idGenerator, operationLogService);
        CreateTemplateRequest request = templateRequest();
        request.setKbId(7L);
        doThrow(new BusinessException(ErrorCode.FORBIDDEN, "read only"))
                .when(knowledgeBaseService).ensureKbEditor(7L, alice);

        assertThatThrownBy(() -> service.create(request, alice, "127.0.0.1"))
                .isInstanceOf(BusinessException.class)
                .extracting("code")
                .isEqualTo(ErrorCode.FORBIDDEN);
        verify(templateRepository, never()).save(any(DocumentTemplate.class));
    }

    @Test
    void onlyTemplateCreatorCanUpdateTemplate() {
        DocumentTemplateService service = new DocumentTemplateService(
                templateRepository, knowledgeBaseService, userRepository, idGenerator, operationLogService);
        DocumentTemplate template = template(1L, 99L);
        when(templateRepository.findById(1L)).thenReturn(Optional.of(template));

        assertThatThrownBy(() -> service.update(1L, templateRequest(), alice, "127.0.0.1"))
                .isInstanceOf(BusinessException.class)
                .extracting("code")
                .isEqualTo(ErrorCode.FORBIDDEN);
    }

    @Test
    void creatorCanReadPrivateGlobalTemplate() {
        DocumentTemplateService service = new DocumentTemplateService(
                templateRepository, knowledgeBaseService, userRepository, idGenerator, operationLogService);
        DocumentTemplate template = template(1L, 10L);
        template.setIsPublic(false);
        template.setKbId(null);
        UserAccount user = new UserAccount();
        user.setId(10L);
        user.setUsername("alice");
        user.setNickname("Alice");
        when(templateRepository.findById(1L)).thenReturn(Optional.of(template));
        when(userRepository.findById(10L)).thenReturn(Optional.of(user));

        assertThat(service.get(1L, alice).getCreatorName()).isEqualTo("Alice");
    }

    @Test
    void markdownRenderingKeepsSoftLineBreaksAndTaskCheckboxes() {
        MarkdownService service = new MarkdownService();

        String html = service.toHtml("jdas\ndsad\n\n- [ ] 用户注册\n- [x] 用户登录");

        assertThat(html).contains("jdas<br");
        assertThat(html).contains("dsad");
        assertThat(html).contains("checkbox");
        assertThat(html).contains("用户注册");
        assertThat(html).contains("用户登录");
    }

    private CreateTemplateRequest templateRequest() {
        CreateTemplateRequest request = new CreateTemplateRequest();
        request.setName("Template");
        request.setMarkdownContent("# Title");
        request.setIsPublic(false);
        request.setCategory("general");
        return request;
    }

    private DocumentTemplate template(Long id, Long creatorId) {
        DocumentTemplate template = new DocumentTemplate();
        template.setId(id);
        template.setName("Template");
        template.setCreatorId(creatorId);
        template.setMarkdownContent("# Title");
        template.setIsPublic(false);
        template.setUseCount(0);
        return template;
    }
}
