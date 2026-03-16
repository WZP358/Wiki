package com.wiki.app.kb;

import com.wiki.app.common.BusinessException;
import com.wiki.app.common.ErrorCode;
import com.wiki.app.common.SnowflakeIdGenerator;
import com.wiki.app.dept.Department;
import com.wiki.app.dept.DepartmentRepository;
import com.wiki.app.kb.dto.*;
import com.wiki.app.log.OperationLogService;
import com.wiki.app.security.CurrentUser;
import com.wiki.app.user.UserAccount;
import com.wiki.app.user.UserRepository;
import jakarta.transaction.Transactional;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

@Service
public class KnowledgeBaseService {
    private final KnowledgeBaseRepository kbRepository;
    private final DepartmentRepository departmentRepository;
    private final UserRepository userRepository;
    private final SnowflakeIdGenerator idGenerator;
    private final OperationLogService operationLogService;

    public KnowledgeBaseService(KnowledgeBaseRepository kbRepository,
                                DepartmentRepository departmentRepository,
                                UserRepository userRepository,
                                SnowflakeIdGenerator idGenerator,
                                OperationLogService operationLogService) {
        this.kbRepository = kbRepository;
        this.departmentRepository = departmentRepository;
        this.userRepository = userRepository;
        this.idGenerator = idGenerator;
        this.operationLogService = operationLogService;
    }

    @Transactional
    public KnowledgeBaseResponse create(CreateKnowledgeBaseRequest request, CurrentUser user, String ip) {
        KnowledgeBase kb = new KnowledgeBase();
        kb.setId(idGenerator.nextId());
        kb.setName(request.getName());
        kb.setType(request.getType());
        kb.setDescription(request.getDescription());
        kb.setOwnerId(user.getUserId());
        kbRepository.save(kb);

        operationLogService.record(user.getUserId(), user.getUsername(), "CREATE_KB", "KB", kb.getId().toString(), ip, "创建知识库");
        return toResponse(kb);
    }

    public List<KnowledgeBaseResponse> listMine(CurrentUser user) {
        List<KnowledgeBaseResponse> responses = new ArrayList<>();
        UserAccount userAccount = userRepository.findById(user.getUserId()).orElse(null);

        // 1. 用户自己创建的知识库（所有类型）
        List<KnowledgeBase> ownedKbs = kbRepository.findByOwnerIdAndDeletedAtIsNull(user.getUserId());
        for (KnowledgeBase kb : ownedKbs) {
            responses.add(toResponse(kb));
        }

        // 2. 公司公开知识库（其他人创建的）
        List<KnowledgeBase> companyKbs = kbRepository.findByTypeAndDeletedAtIsNull(KnowledgeBaseType.COMPANY);
        for (KnowledgeBase kb : companyKbs) {
            if (!kb.getOwnerId().equals(user.getUserId())) {
                responses.add(toResponse(kb));
            }
        }

        // 3. 用户所在部门的知识库（如果用户有部门）
        if (userAccount != null && userAccount.getDepartmentId() != null) {
            List<KnowledgeBase> deptKbs = kbRepository.findByTypeAndDeletedAtIsNull(KnowledgeBaseType.DEPARTMENT);
            for (KnowledgeBase kb : deptKbs) {
                if (!kb.getOwnerId().equals(user.getUserId())) {
                    UserAccount kbOwner = userRepository.findById(kb.getOwnerId()).orElse(null);
                    if (kbOwner != null && userAccount.getDepartmentId().equals(kbOwner.getDepartmentId())) {
                        responses.add(toResponse(kb));
                    }
                }
            }
        }

        return responses;
    }

    public void ensureKbVisible(Long kbId, CurrentUser user) {
        KnowledgeBase kb = kbRepository.findById(kbId)
                .orElseThrow(() -> new BusinessException(ErrorCode.NOT_FOUND, "知识库不存在"));
        if (kb.getDeletedAt() != null) {
            throw new BusinessException(ErrorCode.NOT_FOUND, "知识库不存在");
        }

        if (user.isAdmin()) {
            return;
        }

        if (kb.getType() == KnowledgeBaseType.COMPANY) {
            return;
        }

        if (kb.getType() == KnowledgeBaseType.PRIVATE) {
            if (!kb.getOwnerId().equals(user.getUserId())) {
                throw new BusinessException(ErrorCode.FORBIDDEN, "无权访问该知识库");
            }
            return;
        }

        if (kb.getType() == KnowledgeBaseType.DEPARTMENT) {
            UserAccount currentUser = userRepository.findById(user.getUserId()).orElse(null);
            UserAccount kbOwner = userRepository.findById(kb.getOwnerId()).orElse(null);

            if (currentUser == null || kbOwner == null || currentUser.getDepartmentId() == null) {
                throw new BusinessException(ErrorCode.FORBIDDEN, "无权访问该知识库");
            }

            if (!currentUser.getDepartmentId().equals(kbOwner.getDepartmentId())) {
                throw new BusinessException(ErrorCode.FORBIDDEN, "无权访问该知识库");
            }
        }
    }

    public void ensureKbEditor(Long kbId, CurrentUser user) {
        KnowledgeBase kb = kbRepository.findById(kbId)
                .orElseThrow(() -> new BusinessException(ErrorCode.NOT_FOUND, "知识库不存在"));
        if (kb.getDeletedAt() != null) {
            throw new BusinessException(ErrorCode.NOT_FOUND, "知识库不存在");
        }

        if (user.isAdmin()) {
            return;
        }

        if (kb.getType() == KnowledgeBaseType.COMPANY) {
            return;
        }

        if (kb.getType() == KnowledgeBaseType.PRIVATE) {
            if (!kb.getOwnerId().equals(user.getUserId())) {
                throw new BusinessException(ErrorCode.FORBIDDEN, "无编辑权限");
            }
            return;
        }

        if (kb.getType() == KnowledgeBaseType.DEPARTMENT) {
            UserAccount currentUser = userRepository.findById(user.getUserId()).orElse(null);
            UserAccount kbOwner = userRepository.findById(kb.getOwnerId()).orElse(null);

            if (currentUser == null || kbOwner == null || currentUser.getDepartmentId() == null) {
                throw new BusinessException(ErrorCode.FORBIDDEN, "无编辑权限");
            }

            if (!currentUser.getDepartmentId().equals(kbOwner.getDepartmentId())) {
                throw new BusinessException(ErrorCode.FORBIDDEN, "无编辑权限");
            }
        }
    }

    public void ensureKbAdmin(Long kbId, CurrentUser user) {
        KnowledgeBase kb = kbRepository.findById(kbId)
                .orElseThrow(() -> new BusinessException(ErrorCode.NOT_FOUND, "知识库不存在"));
        if (kb.getDeletedAt() != null) {
            throw new BusinessException(ErrorCode.NOT_FOUND, "知识库不存在");
        }

        if (user.isAdmin()) {
            return;
        }

        if (kb.getType() == KnowledgeBaseType.DEPARTMENT) {
            UserAccount currentUser = userRepository.findById(user.getUserId()).orElse(null);
            UserAccount kbOwner = userRepository.findById(kb.getOwnerId()).orElse(null);

            if (currentUser != null && kbOwner != null && currentUser.getDepartmentId() != null) {
                Department dept = departmentRepository.findById(currentUser.getDepartmentId()).orElse(null);
                if (dept != null && user.getUserId().equals(dept.getManagerId())) {
                    return;
                }
            }
        }

        if (!kb.getOwnerId().equals(user.getUserId())) {
            throw new BusinessException(ErrorCode.FORBIDDEN, "仅创建者或部门部长可操作");
        }
    }

    private KnowledgeBaseResponse toResponse(KnowledgeBase kb) {
        return KnowledgeBaseResponse.builder()
                .id(kb.getId())
                .name(kb.getName())
                .type(kb.getType())
                .description(kb.getDescription())
                .ownerId(kb.getOwnerId())
                .build();
    }
}
