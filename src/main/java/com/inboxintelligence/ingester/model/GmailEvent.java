package com.inboxintelligence.ingester.model;

public record GmailEvent(
        String emailAddress,
        long historyId
) {}