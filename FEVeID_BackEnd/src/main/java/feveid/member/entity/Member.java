package feveid.member.entity;

import feveid.account.entity.Account;
import feveid.member.enums.MemberStatus;
import jakarta.persistence.*;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.*;
import org.hibernate.annotations.*;


import java.time.LocalDate;
import java.time.LocalDateTime;

@Getter
@Setter
@Entity
@Table(name = "members")
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Member {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id", nullable = false)
    private Long id;

    @NotNull
    @OneToOne(fetch = FetchType.LAZY, optional = false)
    @OnDelete(action = OnDeleteAction.CASCADE)
    @JoinColumn(name = "account_id", nullable = false, unique = true)
    private Account account;

    @Size(max = 100)
    @NotNull
    @Column(name = "full_name", nullable = false, length = 100)
    private String fullName;

    @Size(max = 20)
    @Column(name = "student_code", length = 20, unique = true)
    private String studentCode;

    @Size(max = 20)
    @Column(name = "phone", length = 20)
    private String phone;

    @Size(max = 500)
    @Column(name = "avatar_url", length = 500)
    private String avatarUrl;


    @Enumerated(EnumType.STRING)
    @NotNull
    @ColumnDefault("'CTV'")
    @Column(name = "member_status", nullable = false, length = 30)
    @Builder.Default
    private MemberStatus memberStatus = MemberStatus.CTV;

    @NotNull
    @ColumnDefault("CURRENT_DATE")
    @Column(name = "joined_at", nullable = false)
    @Builder.Default
    private LocalDate joinedAt =  LocalDate.now();

    @CreationTimestamp
    @ColumnDefault("CURRENT_TIMESTAMP")
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @ColumnDefault("CURRENT_TIMESTAMP")
    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;


}