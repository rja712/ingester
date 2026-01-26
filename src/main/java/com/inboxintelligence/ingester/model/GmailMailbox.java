package com.inboxintelligence.ingester.model;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.Instant;

@Getter
@Setter
@NoArgsConstructor
@Entity
@Table(name = "gmail_mailbox")
public class GmailMailbox {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /* ---------- identity ---------- */

    @Column(name = "email_address", nullable = false, unique = true)
    private String emailAddress;

    /* ---------- OAuth (Gmail-specific) ---------- */

    @Column(name = "access_token")
    private String accessToken;

    @Column(name = "refresh_token", nullable = false)
    private String refreshToken;

    @Column(name = "access_token_expires_at")
    private Instant accessTokenExpiresAt;

    /* ---------- Gmail sync ---------- */

    @Column(name = "history_id", nullable = false)
    private Long historyId;

    @Column(name = "watch_expires_at")
    private Long watchExpiresAt;

    /* ---------- system-level (temporary, phase-1) ---------- */

    @Enumerated(EnumType.STRING)
    @Column(name = "sync_status", nullable = false)
    private SyncStatus syncStatus = SyncStatus.ACTIVE;

    @Column(name = "last_synced_at")
    private Instant lastSyncedAt;

    @Column(name = "last_sync_error")
    private String lastSyncError;

    /* ---------- audit ---------- */

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt = Instant.now();

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt = Instant.now();

    /* ---------- lifecycle ---------- */

    @PreUpdate
    protected void onUpdate() {
        this.updatedAt = Instant.now();
    }
}
