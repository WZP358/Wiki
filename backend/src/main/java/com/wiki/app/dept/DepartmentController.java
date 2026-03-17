package com.wiki.app.dept;

import com.wiki.app.common.ApiResponse;
import com.wiki.app.dept.dto.DepartmentResponse;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/departments")
public class DepartmentController {

    private final DepartmentRepository departmentRepository;

    public DepartmentController(DepartmentRepository departmentRepository) {
        this.departmentRepository = departmentRepository;
    }

    @GetMapping
    public ApiResponse<List<DepartmentResponse>> listAll() {
        List<Department> depts = departmentRepository.findByDeletedAtIsNull();
        List<DepartmentResponse> result = depts.stream()
                .map(d -> DepartmentResponse.builder()
                        .id(d.getId())
                        .name(d.getName())
                        .parentId(d.getParentId())
                        .build())
                .collect(Collectors.toList());
        return ApiResponse.ok(result);
    }
}

