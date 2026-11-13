package feveid.department.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record DepartmentRequest(
        @NotBlank
        @Size(max = 100)
        String name,

        @Size (max = 1000)
        String description
) {
    public DepartmentRequest {
        if (name != null) {
            name = name.trim();
        }
    }
}
