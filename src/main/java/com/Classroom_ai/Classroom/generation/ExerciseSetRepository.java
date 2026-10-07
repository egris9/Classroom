package com.Classroom_ai.Classroom.generation;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface ExerciseSetRepository extends JpaRepository<ExerciseSet, Long> {

    /** Published exercise sets of the file, plus the user's own. */
    @Query("select e from ExerciseSet e where e.file.id = :fileId and (e.published = true or e.author.id = :userId) order by e.id desc")
    List<ExerciseSet> visibleTo(@Param("fileId") Long fileId, @Param("userId") Long userId);
}
