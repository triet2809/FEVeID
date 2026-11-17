package feveid.department.enums;

// Thứ tự khai báo = thứ tự hiển thị (enum so sánh theo ordinal): Trưởng ban -> Phó ban -> Thành viên
public enum DepartmentPosition {
    LEADER,
    VICE_LEADER,
    MEMBER;

    public boolean isManager() {
        return this == LEADER || this == VICE_LEADER;
    }
}