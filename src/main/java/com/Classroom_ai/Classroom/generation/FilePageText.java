package com.Classroom_ai.Classroom.generation;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Lob;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.Length;

/** The text of one page of a CourseFile, kept so the PDF is read once. Pages are numbered from 1. */
@Getter
@Setter
@Entity
@Table(name = "file_page_texts", uniqueConstraints = @UniqueConstraint(columnNames = {"file_id", "page_number"}))
public class FilePageText {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "file_id", nullable = false)
    private Long fileId;

    @Column(name = "page_number", nullable = false)
    private int pageNumber;

    /** The length is spelled out: MySQL would otherwise be given its 255-byte text type. */
    @Lob
    @Column(nullable = false, length = Length.LONG32)
    private String text;

    @Column(name = "by_ocr", nullable = false)
    private boolean byOcr;
}
