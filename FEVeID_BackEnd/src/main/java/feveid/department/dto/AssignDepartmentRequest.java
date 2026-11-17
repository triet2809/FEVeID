package feveid.department.dto;

import feveid.department.enums.DepartmentPosition;
import jakarta.validation.constraints.NotNull;

public record AssignDepartmentRequest(

        @NotNull
        Long departmentId,

        @NotNull
        DepartmentPosition position
) {

}
