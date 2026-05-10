package com.wiki.app.admin;

import com.wiki.app.admin.dept.AdminDepartmentController;
import com.wiki.app.admin.dept.dto.AdminDepartmentRequest;
import com.wiki.app.common.BusinessException;
import com.wiki.app.common.ErrorCode;
import com.wiki.app.common.SnowflakeIdGenerator;
import com.wiki.app.dept.Department;
import com.wiki.app.dept.DepartmentRepository;
import com.wiki.app.user.UserAccount;
import com.wiki.app.user.UserRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AdminDepartmentControllerTest {

    @Mock
    private DepartmentRepository departmentRepository;
    @Mock
    private UserRepository userRepository;
    @Mock
    private SnowflakeIdGenerator idGenerator;

    @Test
    void createTrimsNameAndValidatesParentAndManager() {
        AdminDepartmentController controller = new AdminDepartmentController(departmentRepository, userRepository, idGenerator);
        AdminDepartmentRequest request = request(" 研发部 ", 1L, 10L);
        when(idGenerator.nextId()).thenReturn(2L);
        when(departmentRepository.findById(1L)).thenReturn(Optional.of(department(1L, "总部")));
        when(userRepository.findById(10L)).thenReturn(Optional.of(user(10L)));
        when(departmentRepository.save(any(Department.class))).thenAnswer(invocation -> invocation.getArgument(0));

        controller.create(request);

        ArgumentCaptor<Department> captor = ArgumentCaptor.forClass(Department.class);
        verify(departmentRepository).save(captor.capture());
        assertThat(captor.getValue().getName()).isEqualTo("研发部");
        assertThat(captor.getValue().getParentId()).isEqualTo(1L);
        assertThat(captor.getValue().getManagerId()).isEqualTo(10L);
    }

    @Test
    void updateRejectsSelfParent() {
        AdminDepartmentController controller = new AdminDepartmentController(departmentRepository, userRepository, idGenerator);
        Department existing = department(2L, "研发部");
        when(departmentRepository.findById(2L)).thenReturn(Optional.of(existing));

        assertThatThrownBy(() -> controller.update(2L, request("研发部", 2L, null)))
                .isInstanceOf(BusinessException.class)
                .extracting("code")
                .isEqualTo(ErrorCode.BAD_REQUEST);
    }

    @Test
    void listFiltersActiveDepartments() {
        AdminDepartmentController controller = new AdminDepartmentController(departmentRepository, userRepository, idGenerator);
        Department active = department(1L, "active");
        Department deleted = department(2L, "deleted");
        deleted.setDeletedAt(java.time.LocalDateTime.now());
        when(departmentRepository.findAll()).thenReturn(List.of(active, deleted));

        assertThat(controller.list(true).getData()).hasSize(1);
        assertThat(controller.list(false).getData()).hasSize(1);
        assertThat(controller.list(null).getData()).hasSize(2);
    }

    private AdminDepartmentRequest request(String name, Long parentId, Long managerId) {
        AdminDepartmentRequest request = new AdminDepartmentRequest();
        request.setName(name);
        request.setParentId(parentId);
        request.setManagerId(managerId);
        request.setDescription("desc");
        return request;
    }

    private Department department(Long id, String name) {
        Department department = new Department();
        department.setId(id);
        department.setName(name);
        return department;
    }

    private UserAccount user(Long id) {
        UserAccount user = new UserAccount();
        user.setId(id);
        user.setUsername("user" + id);
        return user;
    }
}
