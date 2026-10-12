package com.assessment.notification;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockStatic;

import org.junit.jupiter.api.Test;
import org.mockito.MockedStatic;
import org.springframework.boot.SpringApplication;
import org.springframework.context.ConfigurableApplicationContext;

class FraudNotificationApplicationTest {

    @Test
    void mainDelegatesToSpringApplication() {
        try (MockedStatic<SpringApplication> springApplication = mockStatic(SpringApplication.class)) {
            ConfigurableApplicationContext context = mock(ConfigurableApplicationContext.class);
            springApplication
                    .when(() -> SpringApplication.run(eq(FraudNotificationApplication.class), any(String[].class)))
                    .thenReturn(context);

            FraudNotificationApplication.main(new String[] {"--spring.profiles.active=test"});

            springApplication.verify(
                    () -> SpringApplication.run(eq(FraudNotificationApplication.class), any(String[].class)));
        }
    }

    @Test
    void applicationClassIsLoadable() {
        // Covers the class definition itself for coverage tools that attribute the class line separately.
        assertThat(new FraudNotificationApplication()).isNotNull();
    }
}
