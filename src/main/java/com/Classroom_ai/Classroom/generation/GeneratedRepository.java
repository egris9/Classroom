package com.Classroom_ai.Classroom.generation;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.repository.NoRepositoryBean;

import java.util.List;

@NoRepositoryBean
public interface GeneratedRepository<T extends Generated> extends JpaRepository<T, Long> {

    List<T> findByStatus(Status status);

    /** Whether the user has a request of this kind on the file that is still in this status. */
    boolean existsByFileIdAndAuthorIdAndStatus(Long fileId, Long authorId, Status status);
}
