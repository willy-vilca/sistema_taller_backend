package com.tallermecanico.api.notification;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "app.notifications")
public record NotificationProperties(
        boolean enabled,
        String internalSecret,
        String azureCommunicationConnectionString,
        String senderEmail
) {
    public boolean hasCompleteEmailConfiguration() {
        return hasText(azureCommunicationConnectionString) && hasText(senderEmail);
    }

    private boolean hasText(String value) {
        return value != null && !value.isBlank();
    }
}
