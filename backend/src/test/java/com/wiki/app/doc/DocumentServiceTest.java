package com.wiki.app.doc;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.wiki.app.common.SnowflakeIdGenerator;
import com.wiki.app.dept.DepartmentRepository;
import com.wiki.app.doc.dto.DocumentResponse;
import com.wiki.app.doc.dto.UpdateDocumentRequest;
import com.wiki.app.doc.search.IDocumentSearchService;
import com.wiki.app.kb.KnowledgeBaseMemberRepository;
import com.wiki.app.kb.KnowledgeBaseRepository;
import com.wiki.app.kb.KnowledgeBaseService;
import com.wiki.app.log.OperationLogService;
import com.wiki.app.security.CurrentUser;
import com.wiki.app.user.UserAccount;
import com.wiki.app.user.UserRepository;
import com.wiki.app.user.UserTeamMembershipRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ValueOperations;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class DocumentServiceTest {

    @Mock
    private WikiDocumentRepository documentRepository;
    @Mock
    private DocumentVersionRepository versionRepository;
    @Mock
    private DocumentDraftRepository draftRepository;
    @Mock
    private DocumentViewLogRepository viewLogRepository;
    @Mock
    private DocumentEditLogRepository editLogRepository;
    @Mock
    private KnowledgeBaseService knowledgeBaseService;
    @Mock
    private KnowledgeBaseRepository knowledgeBaseRepository;
    @Mock
    private KnowledgeBaseMemberRepository knowledgeBaseMemberRepository;
    @Mock
    private UserRepository userRepository;
    @Mock
    private UserTeamMembershipRepository teamMembershipRepository;
    @Mock
    private DepartmentRepository departmentRepository;
    @Mock
    private SnowflakeIdGenerator idGenerator;
    @Mock
    private StringRedisTemplate redisTemplate;
    @Mock
    private ValueOperations<String, String> valueOperations;
    @Mock
    private OperationLogService operationLogService;
    @Mock
    private AsyncCleanupService asyncCleanupService;
    @Mock
    private LocalDocStorageService localDocStorageService;
    @Mock
    private IDocumentSearchService documentSearchService;

    private DocumentService documentService;

    @BeforeEach
    void setUp() {
        when(redisTemplate.opsForValue()).thenReturn(valueOperations);
        documentService = new DocumentService(
                documentRepository,
                versionRepository,
                draftRepository,
                viewLogRepository,
                editLogRepository,
                knowledgeBaseService,
                knowledgeBaseRepository,
                knowledgeBaseMemberRepository,
                userRepository,
                teamMembershipRepository,
                departmentRepository,
                new MarkdownService(),
                idGenerator,
                redisTemplate,
                new ObjectMapper(),
                operationLogService,
                asyncCleanupService,
                localDocStorageService,
                documentSearchService
        );
    }

    @Test
    void ownerCanUpdatePrivateDocument() {
        CurrentUser owner = new CurrentUser(10L, "alice", "USER");
        WikiDocument doc = privateDocument();
        UserAccount ownerAccount = new UserAccount();
        ownerAccount.setId(owner.getUserId());
        ownerAccount.setUsername(owner.getUsername());
        ownerAccount.setNickname("Alice");

        UpdateDocumentRequest request = new UpdateDocumentRequest();
        request.setTitle("New Title");
        request.setMarkdownContent("updated content");
        request.setBaseVersion(1);
        request.setCommitMessage("owner edit");

        when(documentRepository.findById(doc.getId())).thenReturn(Optional.of(doc));
        when(documentRepository.findByKbIdAndTitleStartingWithAndDeletedAtIsNull(doc.getKbId(), "New Title"))
                .thenReturn(List.of());
        when(documentRepository.save(any(WikiDocument.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(idGenerator.nextId()).thenReturn(1000L, 1001L);
        when(userRepository.findById(owner.getUserId())).thenReturn(Optional.of(ownerAccount));

        DocumentResponse response = documentService.update(doc.getId(), request, owner, "127.0.0.1");

        assertThat(response.getTitle()).isEqualTo("New Title");
        assertThat(response.getMarkdownContent()).isEqualTo("updated content");
        assertThat(response.getVersionNo()).isEqualTo(2);
        assertThat(response.getVisibility()).isEqualTo(DocVisibility.PRIVATE);
        verify(versionRepository).save(any(DocumentVersion.class));
        verify(editLogRepository).save(any(DocumentEditLog.class));
        verify(documentSearchService).upsert(doc);
        verify(operationLogService).record(eq(owner.getUserId()), eq(owner.getUsername()), eq("UPDATE_DOC"),
                eq("DOC"), eq(doc.getId().toString()), eq("127.0.0.1"), eq("New Title"));
    }

    private WikiDocument privateDocument() {
        WikiDocument doc = new WikiDocument();
        doc.setId(99L);
        doc.setKbId(7L);
        doc.setTitle("Old Title");
        doc.setMarkdownContent("old content");
        doc.setHtmlContent("<p>old content</p>\n");
        doc.setOwnerId(10L);
        doc.setVisibility(DocVisibility.PRIVATE);
        doc.setPublished(false);
        doc.setVersionNo(1);
        doc.setViewCount(0L);
        return doc;
    }
}
