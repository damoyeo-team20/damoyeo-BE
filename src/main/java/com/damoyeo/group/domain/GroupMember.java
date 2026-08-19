package com.damoyeo.group.domain;

import com.damoyeo.common.domain.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import java.time.Instant;

@Entity
@Table(
        name = "group_members",
        uniqueConstraints = @UniqueConstraint(name = "uk_group_members_group_user", columnNames = {"group_id", "user_id"})
)
public class GroupMember extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "group_id", nullable = false)
    private MeetingGroup group;

    @Column(name = "user_id", nullable = false)
    private Long userId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private GroupMemberRole role;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private GroupMemberStatus status;

    @Column(name = "joined_at")
    private Instant joinedAt;

    protected GroupMember() {
    }

    public static GroupMember host(MeetingGroup group, long userId) {
        GroupMember member = new GroupMember();
        member.group = group;
        member.userId = userId;
        member.role = GroupMemberRole.HOST;
        member.status = GroupMemberStatus.JOINED;
        member.joinedAt = Instant.now();
        return member;
    }

    public Long getId() {
        return id;
    }

    public MeetingGroup getGroup() {
        return group;
    }

    public Long getUserId() {
        return userId;
    }

    public GroupMemberRole getRole() {
        return role;
    }

    public GroupMemberStatus getStatus() {
        return status;
    }

    public Instant getJoinedAt() {
        return joinedAt;
    }
}
