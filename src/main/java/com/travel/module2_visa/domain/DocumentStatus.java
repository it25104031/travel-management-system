package com.travel.module2_visa.domain;

public enum DocumentStatus {
    UPLOADED,     // awaiting operator review
    VERIFIED,
    REJECTED,     // customer must upload a replacement
    SUPERSEDED,   // replaced by a newer version; kept for history
    INVALIDATED   // e.g. passport reported lost during the tour
}
