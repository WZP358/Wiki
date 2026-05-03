package com.wiki.app.admin.dept;

import com.wiki.app.admin.dept.dto.AdminDepartmentRequest;
import com.wiki.app.admin.dept.dto.AdminDepartmentResponse;
import com.wiki.app.common.ApiResponse;
import com.wiki.app.common.BusinessException;
import com.wiki.app.common.ErrorCode;
import com.wiki.app.common.SnowflakeIdGenerator;
import com.wiki.app.dept.Department;
import com.wiki.app.dept.DepartmentRepository;
import com.wiki.app.user.UserRepository;
import jakarta.validation.Valid;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.List;

@RestController
@RequestMapping("/api/admin/departments")
@PreAuthorize("hasRole('ADMIN')")
public class AdminDepartmentController {
    private final DepartmentRepository departmentRepository;
    private final UserRepository userRepository;
    private final SnowflakeIdGenerator idGenerator;

    public AdminDepartmentController(DepartmentRepository departmentRepository,
                                     UserRepository userRepository,
                                     SnowflakeIdGenerator idGenerator) {
        this.departmentRepository = departmentRepository;
        this.userRepository = userRepository;
        this.idGenerator = idGenerator;
    }

    @GetMapping
    public ApiResponse<List<AdminDepartmentResponse>> list(@RequestParam(required = false) Boolean active) {
        // For now, list is small enough; keep it simple. Frontend can build tree by parentId.
        List<Department> all = departmentRepository.findAll();
        return ApiResponse.ok(all.stream()
                .filter(d -> active == null || (active ? d.getDeletedAt() == null : d.getDeletedAt() != null))
                .map(this::toResponse)
                .toList());
    }

    @PostMapping
    public ApiResponse<AdminDepartmentResponse> create(@Valid @RequestBody AdminDepartmentRequest request) {
        Department d = new Department();
        d.setId(idGenerator.nextId());
        d.setName(request.getName().trim());
        d.setParentId(request.getParentId());
        d.setManagerId(request.getManagerId());
        d.setDescription(request.getDescription());

        validateReferences(d);
        departmentRepository.save(d);
        return ApiResponse.ok(toResponse(d));
    }

    @PutMapping("/{deptId}")
    public ApiResponse<AdminDepartmentResponse> update(@PathVariable Long deptId,
                                                       @Valid @RequestBody AdminDepartmentRequest request) {
        Department d = departmentRepository.findById(deptId)
                .orElseThrow(() -> new BusinessException(ErrorCode.NOT_FOUND, "Department not found"));
        d.setName(request.getName().trim());
        d.setParentId(request.getParentId());
        d.setManagerId(request.getManagerId());
        d.setDescription(request.getDescription());

        validateReferences(d);
        departmentRepository.save(d);
        return ApiResponse.ok(toResponse(d));
    }

    @PostMapping("/{deptId}/active")
    public ApiResponse<AdminDepartmentResponse> setActive(@PathVariable Long deptId,
                                                          @RequestParam boolean active) {
        Department d = departmentRepository.findById(deptId)
                .orElseThrow(() -> new BusinessException(ErrorCode.NOT_FOUND, "Department not found"));
        d.setDeletedAt(active ? null : LocalDateTime.now());
        departmentRepository.save(d);
        return ApiResponse.ok(toResponse(d));
    }

    @DeleteMapping("/{deptId}")
    public ApiResponse<Void> delete(@PathVariable Long deptId) {
        // soft-delete to keep FK safe
        Department d = departmentRepository.findById(deptId)
                .orElseThrow(() -> new BusinessException(ErrorCode.NOT_FOUND, "Department not found"));
        d.setDeletedAt(LocalDateTime.now());
        departmentRepository.save(d);
        return ApiResponse.ok(null);
    }

    private void validateReferences(Department d) {
        if (d.getParentId() != null && d.getParentId().equals(d.getId())) {
            throw new BusinessException(ErrorCode.BAD_REQUEST, "Department parent cannot be itself");
        }
        if (d.getParentId() != null && departmentRepository.findById(d.getParentId()).isEmpty()) {
            throw new BusinessException(ErrorCode.BAD_REQUEST, "Parent department not found");
        }
        if (d.getManagerId() != null && userRepository.findById(d.getManagerId()).isEmpty()) {
            throw new BusinessException(ErrorCode.BAD_REQUEST, "Manager user not found");
        }
    }

    private AdminDepartmentResponse toResponse(Department d) {
        return AdminDepartmentResponse.builder()
                .id(d.getId())
                .name(d.getName())
                .parentId(d.getParentId())
                .managerId(d.getManagerId())
                .description(d.getDescription())
                .active(d.getDeletedAt() == null)
                .createdAt(d.getCreatedAt())
                .updatedAt(d.getUpdatedAt())
                .build();
    }
}

