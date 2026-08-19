package com.damoyeo.meeting.repository;

import com.damoyeo.meeting.domain.MeetingMemory;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface MeetingMemoryRepository extends JpaRepository<MeetingMemory, Long> {

    @Query("""
            select mm from MeetingMemory mm
            join fetch mm.meeting m
            where m.group.id = :groupId
            order by mm.updatedAt desc
            """)
    List<MeetingMemory> findAllByGroupIdOrderByUpdatedAtDesc(@Param("groupId") Long groupId);
}
