package com.damoyeo.meeting.repository;

import com.damoyeo.meeting.domain.MeetingSuggestion;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface MeetingSuggestionRepository extends JpaRepository<MeetingSuggestion, Long> {
    List<MeetingSuggestion> findAllByMeetingIdAndGenerationOrderByRankAsc(long meetingId, int generation);
    Optional<MeetingSuggestion> findByIdAndMeetingId(long id, long meetingId);
    Optional<MeetingSuggestion> findFirstByMeetingIdOrderByGenerationDescRankAsc(long meetingId);
}
