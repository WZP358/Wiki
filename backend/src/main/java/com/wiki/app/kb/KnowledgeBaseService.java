package com.wiki.app.kb;

import com.wiki.app.common.BusinessException;
import com.wiki.app.common.ErrorCode;
import com.wiki.app.common.SnowflakeIdGenerator;
import com.wiki.app.kb.dto.*;
import com.wiki.app.log.OperationLogService;
import com.wiki.app.security.CurrentUser;
import jakarta.transaction.Transactional;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

@Service
public class KnowledgeBaseService {
    private final KnowledgeBaseRepository kbRepository;
    private final KnowledgeBaseMemberRepository memberRepository;
    private final SnowflakeIdGenerator idGenerator;
    private final OperationLogService operationLogService;

    public KnowledgeBaseService(KnowledgeBaseRepository kbRepository,
                                KnowledgeBaseMemberRepository memberRepository,
                                SnowflakeIdGenerator idGenerator,
                                OperationLogService operationLogService) {
        this.kbRepository = kbRepository;
        this.memberRepository = memberRepository;
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

        KnowledgeBaseMember ownerMember = new KnowledgeBaseMember();
        ownerMember.setId(idGenerator.nextId());
        ownerMember.setKbId(kb.getId());
        ownerMember.setUserId(user.getUserId());
        ownerMember.setRole(MemberRole.ADMIN);
        memberRepository.save(ownerMember);

        operationLogService.record(user.getUserId(), user.getUsername(), "CREATE_KB", "KB", kb.getId().toString(), ip, "创建知识库");
        return toResponse(kb, MemberRole.ADMIN);
    }

    public List<KnowledgeBaseResponse> listMine(CurrentUser user) {
        List<KnowledgeBaseResponse> responses = new ArrayList<>();
        List<KnowledgeBaseMember> memberships = memberRepository.findByUserIdAndDeletedAtIsNull(user.getUserId());
        for (KnowledgeBaseMember membership : memberships) {
            kbRepository.findById(membership.getKbId()).ifPresent(kb -> {
                if (kb.getDeletedAt() == null) {
                    responses.add(toResponse(kb, membership.getRole()));
                }
            });
        }
        return responses;
    }

    @Transactional
    public MemberResponse inviteOrUpdateMember(Long kbId, InviteMemberRequest request, CurrentUser user, String ip) {
        ensureKbAdmin(kbId, user);

        KnowledgeBaseMember member = memberRepository.findByKbIdAndUserIdAndDeletedAtIsNull(kbId, request.getUserId())
                .orElseGet(() -> {
                    KnowledgeBaseMember created = new KnowledgeBaseMember();
                    created.setId(idGenerator.nextId());
                    created.setKbId(kbId);
                    created.setUserId(request.getUserId());
                    return created;
                });
        member.setRole(request.getRole());
        memberRepository.save(member);

        operationLogService.record(user.getUserId(), user.getUsername(), "KB_MEMBER_PERMISSION_CHANGE", "KB", kbId.toString(), ip,
                "成员" + request.getUserId() + "角色更新为" + request.getRole());
        return MemberResponse.builder().userId(member.getUserId()).role(member.getRole()).build();
    }

    public List<MemberResponse> listMembers(Long kbId, CurrentUser user) {
        ensureKbVisible(kbId, user);
        return memberRepository.findByKbIdAndDeletedAtIsNull(kbId)
                .stream()
                .map(member -> MemberResponse.builder().userId(member.getUserId()).role(member.getRole()).build())
                .toList();
    }

    public void ensureKbVisible(Long kbId, CurrentUser user) {
        KnowledgeBase kb = kbRepository.findById(kbId)
                .orElseThrow(() -> new BusinessException(ErrorCode.NOT_FOUND, "知识库不存在"));
        if (kb.getDeletedAt() != null) {
            throw new BusinessException(ErrorCode.NOT_FOUND, "知识库不存在");
        }
        if (kb.getType() == KnowledgeBaseType.PUBLIC) {
            return;
        }
        if (kb.getOwnerId().equals(user.getUserId())) {
            return;
        }
        memberRepository.findByKbIdAndUserIdAndDeletedAtIsNull(kbId, user.getUserId())
                .orElseThrow(() -> new BusinessException(ErrorCode.FORBIDDEN, "无权访问该知识库"));
    }

    public MemberRole getMemberRole(Long kbId, Long userId) {
        return memberRepository.findByKbIdAndUserIdAndDeletedAtIsNull(kbId, userId)
                .map(KnowledgeBaseMember::getRole)
                .orElse(null);
    }

    public void ensureKbEditor(Long kbId, CurrentUser user) {
        KnowledgeBase kb = kbRepository.findById(kbId)
                .orElseThrow(() -> new BusinessException(ErrorCode.NOT_FOUND, "知识库不存在"));
        if (kb.getDeletedAt() != null) {
            throw new BusinessException(ErrorCode.NOT_FOUND, "知识库不存在");
        }
        if (kb.getOwnerId().equals(user.getUserId())) {
            return;
        }
        KnowledgeBaseMember member = memberRepository.findByKbIdAndUserIdAndDeletedAtIsNull(kbId, user.getUserId())
                .orElseThrow(() -> new BusinessException(ErrorCode.FORBIDDEN, "无编辑权限"));
        if (member.getRole() == MemberRole.READER) {
            throw new BusinessException(ErrorCode.FORBIDDEN, "无编辑权限");
        }
    }

    public void ensureKbAdmin(Long kbId, CurrentUser user) {
        KnowledgeBase kb = kbRepository.findById(kbId)
                .orElseThrow(() -> new BusinessException(ErrorCode.NOT_FOUND, "知识库不存在"));
        if (kb.getDeletedAt() != null) {
            throw new BusinessException(ErrorCode.NOT_FOUND, "知识库不存在");
        }
        if (kb.getOwnerId().equals(user.getUserId())) {
            return;
        }
        KnowledgeBaseMember member = memberRepository.findByKbIdAndUserIdAndDeletedAtIsNull(kbId, user.getUserId())
                .orElseThrow(() -> new BusinessException(ErrorCode.FORBIDDEN, "仅管理员可操作"));
        if (member.getRole() != MemberRole.ADMIN) {
            throw new BusinessException(ErrorCode.FORBIDDEN, "仅管理员可操作");
        }
    }

    private KnowledgeBaseResponse toResponse(KnowledgeBase kb, MemberRole role) {
        return KnowledgeBaseResponse.builder()
                .id(kb.getId())
                .name(kb.getName())
                .type(kb.getType())
                .description(kb.getDescription())
                .ownerId(kb.getOwnerId())
                .myRole(role == null ? null : role.name())
                .build();
    }
}
