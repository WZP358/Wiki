package com.wiki.app.kb;

import com.wiki.app.common.SnowflakeIdGenerator;
import com.wiki.app.dept.Department;
import com.wiki.app.dept.DepartmentRepository;
import com.wiki.app.user.UserAccount;
import com.wiki.app.user.UserRepository;
import com.wiki.app.user.UserRole;
import com.wiki.app.user.UserTeamMembership;
import com.wiki.app.user.UserTeamMembershipRepository;
import jakarta.transaction.Transactional;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

@Service
public class KnowledgeBaseMemberSyncService {
    private final KnowledgeBaseRepository kbRepository;
    private final KnowledgeBaseMemberRepository memberRepository;
    private final UserRepository userRepository;
    private final UserTeamMembershipRepository teamMembershipRepository;
    private final DepartmentRepository departmentRepository;
    private final SnowflakeIdGenerator idGenerator;

    public KnowledgeBaseMemberSyncService(KnowledgeBaseRepository kbRepository,
                                          KnowledgeBaseMemberRepository memberRepository,
                                          UserRepository userRepository,
                                          UserTeamMembershipRepository teamMembershipRepository,
                                          DepartmentRepository departmentRepository,
                                          SnowflakeIdGenerator idGenerator) {
        this.kbRepository = kbRepository;
        this.memberRepository = memberRepository;
        this.userRepository = userRepository;
        this.teamMembershipRepository = teamMembershipRepository;
        this.departmentRepository = departmentRepository;
        this.idGenerator = idGenerator;
    }

    @Transactional
    public void syncAllAutoReaders() {
        for (KnowledgeBase kb : kbRepository.findByTypeAndDeletedAtIsNull(KnowledgeBaseType.COMPANY)) {
            syncKnowledgeBaseAutoReaders(kb);
        }
        for (KnowledgeBase kb : kbRepository.findByTypeAndDeletedAtIsNull(KnowledgeBaseType.DEPARTMENT)) {
            syncKnowledgeBaseAutoReaders(kb);
        }
    }

    @Transactional
    public void syncUserAutoReaders(Long userId) {
        UserAccount user = userRepository.findById(userId).orElse(null);
        for (KnowledgeBase kb : kbRepository.findByTypeAndDeletedAtIsNull(KnowledgeBaseType.COMPANY)) {
            syncUserForKnowledgeBase(user, kb);
        }
        for (KnowledgeBase kb : kbRepository.findByTypeAndDeletedAtIsNull(KnowledgeBaseType.DEPARTMENT)) {
            syncUserForKnowledgeBase(user, kb);
        }
    }

    @Transactional
    public void syncKnowledgeBaseAutoReaders(KnowledgeBase kb) {
        if (kb == null || kb.getDeletedAt() != null || kb.getType() == KnowledgeBaseType.PRIVATE) {
            return;
        }
        for (UserAccount user : userRepository.findAll()) {
            syncUserForKnowledgeBase(user, kb);
        }
    }

    private void syncUserForKnowledgeBase(UserAccount user, KnowledgeBase kb) {
        if (user == null || kb == null || kb.getDeletedAt() != null || kb.getType() == KnowledgeBaseType.PRIVATE) {
            return;
        }

        boolean shouldBeReader = shouldAutoRead(user, kb);
        KnowledgeBaseMember member = memberRepository.findByKbIdAndUserId(kb.getId(), user.getId()).orElse(null);

        if (shouldBeReader) {
            if (member == null) {
                member = new KnowledgeBaseMember();
                member.setId(idGenerator.nextId());
                member.setKbId(kb.getId());
                member.setUserId(user.getId());
                member.setRole(MemberRole.READER);
            } else if (member.getRole() == null) {
                member.setRole(MemberRole.READER);
            }
            member.setDeletedAt(null);
            memberRepository.save(member);
            return;
        }

        if (member != null && member.getRole() == MemberRole.READER && member.getDeletedAt() == null) {
            member.setDeletedAt(LocalDateTime.now());
            memberRepository.save(member);
        }
    }

    private boolean shouldAutoRead(UserAccount user, KnowledgeBase kb) {
        if (user.getDeletedAt() != null || user.getRole() != UserRole.USER || !isAssigned(user)) {
            return false;
        }
        if (kb.getType() == KnowledgeBaseType.COMPANY) {
            return true;
        }
        if (kb.getType() == KnowledgeBaseType.DEPARTMENT) {
            Long kbTeamId = effectiveTeamId(kb);
            return userTeamIds(user).stream().anyMatch(userTeamId -> isSameOrChildTeam(userTeamId, kbTeamId));
        }
        return false;
    }

    private boolean isAssigned(UserAccount user) {
        return user.getDepartmentId() != null || teamMembershipRepository.countByUserIdAndDeletedAtIsNull(user.getId()) > 0;
    }

    private Set<Long> userTeamIds(UserAccount user) {
        Set<Long> result = new HashSet<>();
        if (user.getDepartmentId() != null) {
            result.add(user.getDepartmentId());
        }
        List<UserTeamMembership> memberships = teamMembershipRepository.findByUserIdAndDeletedAtIsNull(user.getId());
        for (UserTeamMembership membership : memberships) {
            result.add(membership.getTeamId());
        }
        return result;
    }

    private Long effectiveTeamId(KnowledgeBase kb) {
        if (kb.getTeamId() != null) {
            return kb.getTeamId();
        }
        UserAccount owner = userRepository.findById(kb.getOwnerId()).orElse(null);
        return owner == null ? null : owner.getDepartmentId();
    }

    private boolean isSameOrChildTeam(Long userTeamId, Long kbTeamId) {
        if (userTeamId == null || kbTeamId == null) {
            return false;
        }
        Long currentId = userTeamId;
        for (int depth = 0; currentId != null && depth < 32; depth++) {
            if (kbTeamId.equals(currentId)) {
                return true;
            }
            currentId = departmentRepository.findById(currentId)
                    .map(Department::getParentId)
                    .orElse(null);
        }
        return false;
    }
}
