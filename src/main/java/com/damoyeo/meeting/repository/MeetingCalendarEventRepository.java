package com.damoyeo.meeting.repository;

import com.damoyeo.meeting.domain.MeetingCalendarEvent;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface MeetingCalendarEventRepository extends JpaRepository<MeetingCalendarEvent, Long> {
    List<MeetingCalendarEvent> findAllByMeetingIdOrderByIdAsc(long meetingId);
}
