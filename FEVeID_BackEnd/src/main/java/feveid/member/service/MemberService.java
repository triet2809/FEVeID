package feveid.member.service;

import feveid.member.dto.ChangeMemberStatusRequest;
import feveid.member.dto.MemberResponse;
import feveid.member.dto.UpdateMyProfileRequest;
import feveid.member.enums.MemberStatus;

import java.util.List;

public interface MemberService {
    MemberResponse getMyProfile(String email);
    
    MemberResponse updateMyProfile(String email, UpdateMyProfileRequest request);

    List<MemberResponse> getMembers(MemberStatus status);

    MemberResponse getMemberById(Long id);

    MemberResponse changeStatus(Long id, ChangeMemberStatusRequest request);
}
