package com.Classroom_ai.Classroom.generation;

import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Lob;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Entity
@Table(name = "summaries")
public class Summary extends Generated {
    @Lob
    private String content;

    @Enumerated(EnumType.STRING)
    private SummaryLevel level;

    /** A row stored before summaries had a level has none: it was written in what is now the medium format. */
    public SummaryLevel getLevel() {
        return level == null ? SummaryLevel.MEDIUM : level;
    }
}
