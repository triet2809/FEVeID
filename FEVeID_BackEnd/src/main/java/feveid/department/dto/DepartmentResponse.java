package feveid.department.dto;

import feveid.department.entity.Department;

public record DepartmentResponse(Long id, String name, String description) {
    public static DepartmentResponse from(Department department) {
        return new DepartmentResponse(department.getId(), department.getName(), department.getDescription());
    }
}
