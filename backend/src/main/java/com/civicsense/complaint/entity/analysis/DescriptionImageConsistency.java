package com.civicsense.complaint.entity.analysis;

public enum DescriptionImageConsistency {

    CONSISTENT,
    INCONSISTENT,
    UNCERTAIN,
    /** No image was submitted; description-image consistency is not applicable. */
    NOT_APPLICABLE
}