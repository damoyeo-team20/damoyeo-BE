package com.damoyeo.group.domain;

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

@Entity
@Table(name = "group_memories")
public class GroupMemory {
    @Id
    @Column(name = "group_id")
    private Long groupId;
    @MapsId
    @OneToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "group_id")
    private MeetingGroup group;
    @Column(nullable = false, columnDefinition = "TEXT")
    private String summary;
    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;
    protected GroupMemory() {}
    public GroupMemory(MeetingGroup group, String summary) { this.group = group; update(summary); }
    public void update(String summary) { this.summary = summary; }
    @PrePersist @PreUpdate void stamp() { updatedAt = Instant.now(); }
    public String getSummary() { return summary; }
}
