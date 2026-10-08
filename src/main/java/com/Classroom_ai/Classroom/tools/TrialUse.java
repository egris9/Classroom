package com.Classroom_ai.Classroom.tools;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import lombok.Getter;

import java.time.Instant;

/**
 * The one free use of the AI tools that an anonymous visitor has had. Only salted hashes are kept, never the cookie
 * or the address. Each hash is unique, so two uses that race for the same visitor cannot both be stored.
 */
@Getter
@Entity
public class TrialUse {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true, length = 64)
    private String cookieHash;

    @Column(nullable = false, unique = true, length = 64)
    private String ipHash;

    @Column(nullable = false)
    private Instant usedAt = Instant.now();

    protected TrialUse() {
    }

    public TrialUse(String cookieHash, String ipHash) {
        this.cookieHash = cookieHash;
        this.ipHash = ipHash;
    }
}
