package com.damoyeo.meeting.repository;

import com.damoyeo.meeting.domain.Meeting;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface MeetingRepository extends JpaRepository<Meeting, Long> {
    List<Meeting> findAllByGroupIdOrderByCreatedAtDesc(Long groupId);
}
