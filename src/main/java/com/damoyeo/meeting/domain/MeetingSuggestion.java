package com.damoyeo.meeting.domain;

import com.damoyeo.ai.AiClient;
import com.damoyeo.common.domain.BaseEntity;
import jakarta.persistence.*;
import java.time.Instant;
import java.time.OffsetDateTime;
import java.util.List;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

@Entity
@Table(name = "meeting_suggestions")
public class MeetingSuggestion extends BaseEntity {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY) private Long id;
    @ManyToOne(fetch = FetchType.LAZY, optional = false) @JoinColumn(name = "meeting_id") private Meeting meeting;
    @Column(nullable = false) private int generation;
    @Column(nullable = false) private int rank;
    @Column(name = "external_place_id", nullable = false) private String externalPlaceId;
    @Column(nullable = false) private String category;
    @Column(name = "place_provider", nullable = false) private String placeProvider;
    @Column(nullable = false) private String name;
    @Column(nullable = false) private String address;
    @Column(name = "external_url", columnDefinition = "TEXT") private String externalUrl;
    @Column(name = "proposed_start_at", nullable = false) private Instant proposedStartAt;
    @Column(name = "proposed_end_at", nullable = false) private Instant proposedEndAt;
    @JdbcTypeCode(SqlTypes.JSON) @Column(nullable = false) private List<String> reasons;
    protected MeetingSuggestion() {}
    public MeetingSuggestion(Meeting meeting, int generation, AiClient.CandidateSuggestion value) {
        this.meeting = meeting; this.generation = generation; this.rank = value.rank(); this.externalPlaceId = value.externalPlaceId();
        this.category = value.category(); this.placeProvider = value.placeProvider(); this.name = value.name(); this.address = value.address();
        this.externalUrl = value.externalUrl(); this.proposedStartAt = OffsetDateTime.parse(value.proposedStartAt()).toInstant();
        this.proposedEndAt = OffsetDateTime.parse(value.proposedEndAt()).toInstant(); this.reasons = value.reasons();
    }
    public Long getId(){return id;} public int getGeneration(){return generation;} public int getRank(){return rank;} public String getExternalPlaceId(){return externalPlaceId;}
    public Instant getProposedStartAt(){return proposedStartAt;} public Instant getProposedEndAt(){return proposedEndAt;}
    public String getName(){return name;} public String getCategory(){return category;} public String getAddress(){return address;}
}
