package feveid.member.service;

import feveid.member.dto.MemberResponse;
import feveid.member.dto.UpdateMyProfileRequest;
import feveid.member.entity.Member;

public interface MemberService {
    MemberResponse getMyProfile(String email);
    MemberResponse updateMyProfile(String email, UpdateMyProfileRequest request);

}
