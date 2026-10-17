package com.civicsense.complaint.entity.image;

import com.civicsense.complaint.entity.Complaint;
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
     */
    @Column(length = 255)
    private String storageKey;

    @Column(nullable = false, length = 100)
    private String contentType;

    /*
     * Server-side extracted EXIF GPS & timestamp metadata (supporting hint only).
     * Never exposed publicly.
     */
    @Column(name = "exif_latitude")
    private Double exifLatitude;

    @Column(name = "exif_longitude")
    private Double exifLongitude;

    @Column(name = "exif_timestamp")
    private LocalDateTime exifTimestamp;

    @Column(name = "has_exif_gps")
    private Boolean hasExifGps = false;

    @Column(name = "exif_distance_meters")
    private Double exifDistanceMeters;

    @Column(name = "exif_location_mismatch")
    private Boolean exifLocationMismatch = false;

    /*
     * Cryptographic and perceptual image hashes for provenance and reuse detection.
     */
    @Column(name = "image_hash", length = 64)
    private String imageHash;

    @Column(name = "perceptual_hash", length = 32)
    private String perceptualHash;

    @Column(name = "image_reused")
    private Boolean imageReused = false;

    @Enumerated(EnumType.STRING)
    @Column(name = "image_reuse_type", length = 50)
    private ImageReuseType imageReuseType = ImageReuseType.NONE;

    @Column(name = "image_reuse_suspicious")
    private Boolean imageReuseSuspicious = false;

    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
    }
}