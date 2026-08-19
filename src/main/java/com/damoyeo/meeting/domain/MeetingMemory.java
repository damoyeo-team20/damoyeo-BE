package com.damoyeo.meeting.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.MapsId;
import jakarta.persistence.OneToOne;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.Map;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

@Entity
@Table(name = "meeting_memories")
public class MeetingMemory {

    @Id
    @Column(name = "meeting_id")
    private Long meetingId;

    @MapsId
    @OneToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "meeting_id")
    private Meeting meeting;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(nullable = false)
    private Map<String, Object> memory;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    protected MeetingMemory() {
    }

    public MeetingMemory(Meeting meeting, Map<String, Object> memory) {
        this.meeting = meeting;
        this.memory = Map.copyOf(memory);
    }

    public void update(Map<String, Object> memory) {
        this.memory = Map.copyOf(memory);
    }

    @PrePersist
    @PreUpdate
    protected void updateTimestamp() {
        updatedAt = Instant.now();
    }

    public Long getMeetingId() {
        return meetingId;
    }

    public Meeting getMeeting() {
        return meeting;
    }

    public Map<String, Object> getMemory() {
        return memory;
    }

    public Instant getUpdatedAt() {
        return updatedAt;
    }
}
