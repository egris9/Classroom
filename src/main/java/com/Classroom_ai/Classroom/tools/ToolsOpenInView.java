package com.Classroom_ai.Classroom.tools;

import jakarta.persistence.EntityManagerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.orm.jpa.support.OpenEntityManagerInViewInterceptor;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

/**
 * Open-in-view gives every web request one Hibernate session, and with it one database connection, until the request
 * is over. A request to the AI tools stays open while it waits for the model, most of it queued behind other work,
 * and a chat stays open for the whole reply, so a handful of them would use up the connection pool and stop the
 * whole app. Spring Boot's own open-in-view is therefore off ({@code spring.jpa.open-in-view=false}) and the same
 * interceptor is registered here for every route but {@code /api/tools/**}, whose controllers load no lazy data.
 */
@Configuration
class ToolsOpenInView implements WebMvcConfigurer {

    private final EntityManagerFactory entityManagerFactory;

    ToolsOpenInView(EntityManagerFactory entityManagerFactory,
                    @Value("${spring.jpa.open-in-view:true}") boolean bootOpenInView) {
        if (bootOpenInView) {
            throw new IllegalStateException(
                    "spring.jpa.open-in-view must be false: ToolsOpenInView registers it for every route but /api/tools/**.");
        }
        this.entityManagerFactory = entityManagerFactory;
    }

    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        OpenEntityManagerInViewInterceptor interceptor = new OpenEntityManagerInViewInterceptor();
        interceptor.setEntityManagerFactory(entityManagerFactory);
        registry.addWebRequestInterceptor(interceptor).excludePathPatterns("/api/tools/**");
    }
}
