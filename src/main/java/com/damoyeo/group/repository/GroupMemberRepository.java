package com.damoyeo.group.repository;

import com.damoyeo.group.domain.GroupMember;
import com.damoyeo.group.domain.GroupMemberStatus;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface GroupMemberRepository extends JpaRepository<GroupMember, Long> {
    Optional<GroupMember> findByGroupIdAndUserId(Long groupId, Long userId);

    List<GroupMember> findAllByGroupIdAndStatusOrderByJoinedAtAsc(Long groupId, GroupMemberStatus status);
}
