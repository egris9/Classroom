package com.Classroom_ai.Classroom.course;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface CourseFileRepository extends JpaRepository<CourseFile, Long> {
    List<CourseFile> findByCourseId(Long courseId);
}
