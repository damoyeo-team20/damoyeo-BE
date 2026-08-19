package com.damoyeo.group.repository;

import com.damoyeo.group.domain.MeetingGroup;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface MeetingGroupRepository extends JpaRepository<MeetingGroup, Long> {

    boolean existsByInviteCode(String inviteCode);

    Optional<MeetingGroup> findByInviteCode(String inviteCode);

    @Query("""
            select g from MeetingGroup g
            join GroupMember gm on gm.group = g
            where gm.userId = :userId
            order by g.createdAt desc
            """)
    List<MeetingGroup> findAllJoinedByUserId(@Param("userId") long userId);
}
