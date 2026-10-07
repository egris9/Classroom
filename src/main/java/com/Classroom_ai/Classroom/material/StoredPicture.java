package com.Classroom_ai.Classroom.material;

import org.springframework.core.io.Resource;
import org.springframework.http.MediaType;

public record StoredPicture(Resource resource, MediaType type) {
}
