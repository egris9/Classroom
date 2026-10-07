package com.Classroom_ai.Classroom.generation;

import jakarta.persistence.Entity;
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
}
