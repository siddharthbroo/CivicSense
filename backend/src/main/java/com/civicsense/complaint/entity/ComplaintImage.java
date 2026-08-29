package com.civicsense.complaint.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "complaint_images")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class ComplaintImage {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    /*
     * Complaint to which this image belongs.
     */
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "complaint_id", nullable = false)
    private Complaint complaint;

    /*
     * URL of the image stored in cloud storage.
     */
    @Column(nullable = false, length = 1000)
    private String imageUrl;

    /*
     * Cloud storage file identifier.
     *
     * Useful later if we need to delete or replace
     * the image from cloud storage.
     */
    @Column(length = 255)
    private String storageKey;

    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
    }
}