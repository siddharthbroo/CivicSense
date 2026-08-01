package com.civicsense.entity;

import jakarta.persistence.*;

import java.time.LocalDateTime;

@Entity
@Table(name="complaints")
public class Complaint {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String title;

    private String description;

    private String category;

    private String status;

    private LocalDateTime createdAt;

    private LocalDateTime updatedAt;
}
