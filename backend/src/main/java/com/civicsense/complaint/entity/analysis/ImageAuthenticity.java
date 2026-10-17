package com.civicsense.complaint.entity.analysis;

public enum ImageAuthenticity {

    LIKELY_REAL,
    SUSPICIOUS,
    UNCERTAIN,
    /** No image was submitted; image authenticity is not applicable. */
    NOT_APPLICABLE
}