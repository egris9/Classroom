package com.Classroom_ai.Classroom.generation;

import org.hibernate.boot.Metadata;
import org.hibernate.boot.MetadataSources;
import org.hibernate.boot.registry.StandardServiceRegistry;
import org.hibernate.boot.registry.StandardServiceRegistryBuilder;
import org.hibernate.mapping.Column;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * The tests run on H2, where every text type holds any length. MySQL has four, and the smallest holds 255 bytes,
 * so the type MySQL is given for a page's text is checked here, with no database.
 */
class FilePageTextSchemaTest {

    @Test
    void onMySqlThePageTextColumnHoldsAWholePage() {
        StandardServiceRegistry registry = new StandardServiceRegistryBuilder()
                .applySetting("hibernate.dialect", "org.hibernate.dialect.MySQLDialect")
                .applySetting("hibernate.boot.allow_jdbc_metadata_access", false)
                .build();
        try {
            Metadata metadata = new MetadataSources(registry).addAnnotatedClass(FilePageText.class).buildMetadata();
            Column text = metadata.getEntityBinding(FilePageText.class.getName()).getProperty("text").getColumns().get(0);

            assertThat(text.getSqlType(metadata)).isEqualTo("longtext");
        } finally {
            StandardServiceRegistryBuilder.destroy(registry);
        }
    }
}
