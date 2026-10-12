package com.assessment.notification.config;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

class NotificationPropertiesTest {

    @Test
    void defaultsPointAtFraudOpsMailbox() {
        NotificationProperties properties = new NotificationProperties();

        assertThat(properties.getRecipient()).isEqualTo("fraud-team@example.com");
        assertThat(properties.getAlertFrom()).isEqualTo("fraud-alerts@example.com");
    }

    @Test
    void settersOverrideDefaults() {
        NotificationProperties properties = new NotificationProperties();
        properties.setRecipient("ops@example.com");
        properties.setAlertFrom("noreply@example.com");

        assertThat(properties.getRecipient()).isEqualTo("ops@example.com");
        assertThat(properties.getAlertFrom()).isEqualTo("noreply@example.com");
    }
}
