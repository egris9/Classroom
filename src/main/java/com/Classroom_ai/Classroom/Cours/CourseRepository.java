package com.Classroom_ai.Classroom.Cours;

import com.Classroom_ai.Classroom.User.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;


import java.util.Optional;

@Repository
public interface CourseRepository extends JpaRepository<Course, Long> {
    Optional<Course> findByCourseName(String courseName);
    Optional<Course> findByAccessCode(String accessCode);
    List<Course> findByTeacherId(Long teacherId);
    List<Course> findByStudentsContaining(User student);
}


