package com.Classroom_ai.Classroom;

import com.Classroom_ai.Classroom.generation.LocalTextGeneration;
import com.Classroom_ai.Classroom.generation.TextGeneration;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;

/** {@code generation.adapter=local} selects the local adapter and reads its settings from the properties. */
@SpringBootTest(properties = {
        "generation.adapter=local",
        "generation.base-url=http://127.0.0.1:1",
        "generation.model=wired-model"
})
class LocalAdapterWiringTest {

    @Autowired TextGeneration textGeneration;

    @Test
    void the_local_adapter_is_the_TextGeneration_and_carries_the_configured_model() {
        assertInstanceOf(LocalTextGeneration.class, textGeneration);
        assertEquals("wired-model", textGeneration.modelName());
    }
}
