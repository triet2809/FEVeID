package feveid.department.service;

import feveid.department.dto.AssignDepartmentRequest;
import feveid.department.dto.DepartmentMemberResponse;
import feveid.department.dto.MembershipResponse;

import java.util.List;

public interface DepartmentMembershipService {
    MembershipResponse assign(Long memberId, AssignDepartmentRequest request);
    void remove(Long memberId);
    List<DepartmentMemberResponse> getCurrentMembers(Long departmentId);
}
