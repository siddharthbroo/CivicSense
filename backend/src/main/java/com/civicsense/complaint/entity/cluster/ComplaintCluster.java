package com.civicsense.complaint.entity.cluster;

import com.civicsense.complaint.entity.classification.ComplaintCategory;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "complaint_clusters")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class ComplaintCluster {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    /*
     * Category of the issue represented by this cluster.
     *
     * Example:
     * ROAD_DAMAGE
     * GARBAGE
     */
    @Enumerated(EnumType.STRING)
    @Column(length = 50, nullable = false)
    private ComplaintCategory category;

    /*
     * Number of complaints currently belonging
     * to this cluster.
     */
    @Column(nullable = false)
    private Integer complaintCount = 0;

    /*
     * Approximate center/location of the cluster.
     *
     * Later this can be used for hotspot analysis.
     */
    @Column(nullable = false)
    private Double latitude;

    @Column(nullable = false)
    private Double longitude;

    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(nullable = false)
    private LocalDateTime updatedAt;

    @PrePersist
    protected void onCreate() {

        LocalDateTime now = LocalDateTime.now();

        createdAt = now;
        updatedAt = now;
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }
}