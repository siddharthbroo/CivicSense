package com.civicsense.complaint.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "complaint_analyses")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class ComplaintAnalysis {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @OneToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "complaint_id", nullable = false, unique = true)
    private Complaint complaint;

    @Column(length = 50)
    private String language;

    @Column(length = 50)
    private String category;

    @Column(length = 30)
    private String severity;

    @Column(length = 1000)
    private String summary;

    @Column(length = 100)
    private String department;

    private double confidence;

    @Column(name = "image_authenticity", length = 50)
    private String imageAuthenticity;

    @Column(name = "description_image_consistency", length = 50)
    private String descriptionImageConsistency;

    @Column(nullable = false)
    private LocalDateTime createdAt;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
    }
}