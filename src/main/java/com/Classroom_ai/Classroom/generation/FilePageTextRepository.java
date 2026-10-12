package com.Classroom_ai.Classroom.generation;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface FilePageTextRepository extends JpaRepository<FilePageText, Long> {

    List<FilePageText> findByFileIdOrderByPageNumber(Long fileId);

    long countByFileId(Long fileId);

    /** One statement, run at once, so the rows are gone before the file row is deleted. */
    @Modifying
    @Query("delete from FilePageText p where p.fileId = :fileId")
    void deleteAllOf(@Param("fileId") Long fileId);
}
