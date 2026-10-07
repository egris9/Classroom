package com.Classroom_ai.Classroom.generation;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.repository.NoRepositoryBean;

import java.util.List;

@NoRepositoryBean
public interface GeneratedRepository<T extends Generated> extends JpaRepository<T, Long> {

    List<T> findByStatus(Status status);
}
