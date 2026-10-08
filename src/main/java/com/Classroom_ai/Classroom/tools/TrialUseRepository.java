package com.Classroom_ai.Classroom.tools;

import org.springframework.data.jpa.repository.JpaRepository;

public interface TrialUseRepository extends JpaRepository<TrialUse, Long> {

    boolean existsByCookieHashOrIpHash(String cookieHash, String ipHash);
}
