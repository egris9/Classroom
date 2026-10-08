package com.Classroom_ai.Classroom.course;

import com.Classroom_ai.Classroom.auth.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface CourseRepository extends JpaRepository<Course, Long> {
    Optional<Course> findByAccessCode(String accessCode);
    boolean existsByTeacherIdAndCourseName(Long teacherId, String courseName);
    List<Course> findByTeacherId(Long teacherId);
    List<Course> findByStudentsContaining(User student);
}
