package feveid.member.service.impl;

import feveid.common.exception.NotFoundException;
import feveid.member.dto.MemberResponse;
import feveid.member.dto.UpdateMyProfileRequest;
import feveid.member.entity.Member;
import feveid.member.repository.MemberRepository;
import feveid.member.service.MemberService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class MemberServiceImpl implements MemberService {
    private final MemberRepository memberRepository;


    @Override
    @Transactional(readOnly = true)
    public MemberResponse getMyProfile(String email) {
        return MemberResponse.from(findByEmail(email));
    }


    @Override
    @Transactional
    public MemberResponse updateMyProfile(String email, UpdateMyProfileRequest request) {
        Member member = findByEmail(email);

        //PATCH: null = không gửi = giữ nguyên giá trị cũ
        if (request.phone() != null) {
            member.setPhone(request.phone());
        }
        if (request.avatarUrl() != null) {
            member.setAvatarUrl(request.avatarUrl());
        }
        //Không cần gọi save(): member đang được quản lý trong transaction
        // Hibernate tự UPDATE khi commit (dirty checking)

        return MemberResponse.from(member);
    }


    private Member findByEmail(String email) {
        return memberRepository.findByAccountEmail(email).orElseThrow(() -> new NotFoundException("Không tìm thấy hồ sơ thành viên"));
    }
}