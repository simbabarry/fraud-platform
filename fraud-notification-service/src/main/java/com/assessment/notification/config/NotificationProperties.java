package com.assessment.notification.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Getter
@Setter
@Component
@ConfigurationProperties(prefix = "fraud.notification")
public class NotificationProperties {

    /** Single fraud-ops recipient (no customer emails required). */
    private String recipient = "fraud-team@example.com";

    /** From address shown in Mailpit / SMTP. */
    private String alertFrom = "fraud-alerts@example.com";
}
