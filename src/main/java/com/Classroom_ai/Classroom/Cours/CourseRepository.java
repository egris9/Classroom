package com.Classroom_ai.Classroom.Cours;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface CourseRepository extends JpaRepository<Course, Long> {
    Optional<Course> findByCourseName(String courseName);
    Optional<Course> findByAccessCode(String accessCode); // Recherche par code d'accès
}
