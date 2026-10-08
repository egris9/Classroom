package com.Classroom_ai.Classroom.generation;

import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface SummaryRepository extends GeneratedRepository<Summary> {

    /** Published summaries of the file, plus the user's own. */
    @Query("select s from Summary s where s.file.id = :fileId and (s.published = true or s.author.id = :userId) order by s.id desc")
    List<Summary> visibleTo(@Param("fileId") Long fileId, @Param("userId") Long userId);
}
