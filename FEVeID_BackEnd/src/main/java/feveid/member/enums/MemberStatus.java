package feveid.member.enums;

public enum MemberStatus {
    CTV,
    ACTIVE,
    INACTIVE,
    DISQUALIFIED;

    //Luật chuyển trạng thái. Không ai được chuyển về CTV
    public boolean canTransitionTo(MemberStatus target){
        return switch (this){
            case CTV, INACTIVE -> target == ACTIVE || target == DISQUALIFIED;
            case ACTIVE ->  target == INACTIVE || target == DISQUALIFIED;
            case DISQUALIFIED -> false;
        };
    }
}
