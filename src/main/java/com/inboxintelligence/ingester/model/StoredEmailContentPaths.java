package com.inboxintelligence.ingester.model;

public record StoredEmailContentPaths(
        String rawMessagePath,
        String bodyContentPath,
        String bodyHtmlContentPath
) {}
