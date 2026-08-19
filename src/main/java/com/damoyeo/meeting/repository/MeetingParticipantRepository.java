package com.damoyeo.meeting.repository;

import com.damoyeo.meeting.domain.MeetingParticipant;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface MeetingParticipantRepository extends JpaRepository<MeetingParticipant, Long> {
    List<MeetingParticipant> findAllByMeetingIdOrderByIdAsc(Long meetingId);

    boolean existsByMeetingId(Long meetingId);

    void deleteAllByMeetingId(Long meetingId);

    Optional<MeetingParticipant> findByMeetingIdAndGroupMemberId(Long meetingId, Long groupMemberId);
}
