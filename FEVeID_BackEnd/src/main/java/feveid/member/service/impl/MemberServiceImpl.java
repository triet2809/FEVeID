package feveid.member.service.impl;

import feveid.account.enums.AccountStatus;
import feveid.common.exception.ConflictException;
import feveid.common.exception.NotFoundException;
import feveid.member.dto.ChangeMemberStatusRequest;
import feveid.member.dto.MemberResponse;
import feveid.member.dto.UpdateMyProfileRequest;
import feveid.member.entity.Member;
import feveid.member.enums.MemberStatus;
import feveid.member.repository.MemberRepository;
import feveid.member.service.MemberService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

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

    @Override
    @Transactional(readOnly = true)
    public List<MemberResponse> getMembers(MemberStatus status){
        List<Member> members = (status == null)
                ? memberRepository.findAllByOrderByFullNameAsc()
                : memberRepository.findByMemberStatusOrderByFullName(status);
        return members.stream().map(MemberResponse::from).toList();
    }
    @Override
    @Transactional(readOnly = true)
    public MemberResponse getMemberById(Long id){
        return MemberResponse.from(findById(id));
    }

    @Override
    @Transactional
    public MemberResponse changeStatus (Long id, ChangeMemberStatusRequest request){
    Member member = findById(id);
    MemberStatus current = member.getMemberStatus();
    MemberStatus target = request.status();

    if(!current.canTransitionTo(target)){
        throw new ConflictException("Không thể chuyển trạng thái từ %s sang %s".formatted(current,target));
    }
    member.setMemberStatus(target);
    // Disqualify = rời CLB -> Khóa đăng nhập luôn (login chặn DISABLE)
        if (target == MemberStatus.DISQUALIFIED){
            member.getAccount().setAccountStatus(AccountStatus.DISABLED);
        }
        //Không cần save(): member và account đều đang được quản lý trong transaction (dirty checking)
        return MemberResponse.from(member);
    }

    private Member findById(Long id) {
        return memberRepository.findById(id).orElseThrow(() -> new NotFoundException("Không tìm thấy thành viên"));
    }
}