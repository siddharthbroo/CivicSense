package com.civicsense.officer.entity;

/**
 * Lifecycle verification status for municipal officers.
 *
 * <p>Decoupled from standard user account activation status (ACTIVE, INACTIVE).
 * An officer account may be an active user account but not yet verified or authorized
 * to view or manage complaints.
 */
public enum OfficerVerificationStatus {
    /**
     * Officer profile provisioned by administrative command, awaiting official credential verification.
     */
    PENDING_VERIFICATION,

    /**
     * Official credentials and municipal identity verified by administrator.
     * The officer is operationally authorized.
     */
    VERIFIED,

    /**
     * Profile rejected during official verification.
     */
    REJECTED,

    /**
     * Officer credentials temporarily or permanently suspended (e.g. pending inquiry, transfer, or leave).
     */
    SUSPENDED
}

