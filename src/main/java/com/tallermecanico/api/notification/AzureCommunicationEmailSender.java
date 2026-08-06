package com.tallermecanico.api.notification;

import com.azure.communication.email.EmailClient;
import com.azure.communication.email.EmailClientBuilder;
import com.azure.communication.email.models.EmailMessage;
import com.azure.communication.email.models.EmailSendResult;
import com.azure.core.util.polling.SyncPoller;
import com.tallermecanico.api.scheduledservice.ScheduledService;
import org.springframework.stereotype.Component;

import java.time.format.DateTimeFormatter;
import java.util.Locale;

@Component
public class AzureCommunicationEmailSender {
    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("EEEE d 'de' MMMM", new Locale("es", "PE"));

    private final NotificationProperties properties;
    private volatile EmailClient emailClient;

    public AzureCommunicationEmailSender(NotificationProperties properties) {
        this.properties = properties;
    }

    public String send(ScheduledServiceReminderEmail reminder) {
        if (!properties.hasCompleteEmailConfiguration()) {
            throw new IllegalStateException("La configuración de Azure Communication Services para correo está incompleta.");
        }

        EmailMessage message = new EmailMessage()
                .setSenderAddress(properties.senderEmail().trim())
                .setToRecipients(reminder.recipient().getEmail())
                .setSubject("Recordatorio: " + reminder.scheduledServices().size() + " servicio(s) programado(s) para mañana")
                .setBodyPlainText(buildPlainTextBody(reminder))
                .setBodyHtml(buildHtmlBody(reminder));

        SyncPoller<EmailSendResult, EmailSendResult> poller = client().beginSend(message);
        EmailSendResult result = poller.waitForCompletion().getValue();
        if (result == null || result.getId() == null || result.getId().isBlank()) {
            throw new IllegalStateException("Azure Communication Services no confirmó el envío del correo.");
        }
        return result.getId();
    }

    private EmailClient client() {
        EmailClient current = emailClient;
        if (current != null) {
            return current;
        }
        synchronized (this) {
            if (emailClient == null) {
                emailClient = new EmailClientBuilder()
                        .connectionString(properties.azureCommunicationConnectionString().trim())
                        .buildClient();
            }
            return emailClient;
        }
    }

    private String buildPlainTextBody(ScheduledServiceReminderEmail reminder) {
        StringBuilder body = new StringBuilder();
        body.append("Hola, ").append(reminder.recipient().getFullName()).append(".\n\n")
                .append("Estos son los servicios programados para mañana, ")
                .append(DATE_FORMATTER.format(reminder.scheduledDate())).append(":\n\n");
        for (ScheduledService service : reminder.scheduledServices()) {
            body.append("- ").append(service.getVehicle().getClient().getFullName())
                    .append(" | ").append(service.getVehicle().getLicensePlate())
                    .append(" | ").append(service.getVehicle().getModel())
                    .append(" | ").append(service.getDescription() == null ? "Servicio programado" : service.getDescription())
                    .append("\n");
        }
        body.append("\nRevisa el sistema para ver el detalle o actualizar la programación.");
        return body.toString();
    }

    private String buildHtmlBody(ScheduledServiceReminderEmail reminder) {
        StringBuilder rows = new StringBuilder();
        for (ScheduledService service : reminder.scheduledServices()) {
            rows.append("<tr>")
                    .append("<td style=\"padding:12px;border-bottom:1px solid #e2e8f0\">").append(escapeHtml(service.getVehicle().getClient().getFullName())).append("</td>")
                    .append("<td style=\"padding:12px;border-bottom:1px solid #e2e8f0;font-weight:700\">").append(escapeHtml(service.getVehicle().getLicensePlate())).append("</td>")
                    .append("<td style=\"padding:12px;border-bottom:1px solid #e2e8f0\">").append(escapeHtml(service.getVehicle().getModel())).append("</td>")
                    .append("<td style=\"padding:12px;border-bottom:1px solid #e2e8f0\">").append(escapeHtml(service.getDescription() == null ? "Servicio programado" : service.getDescription())).append("</td>")
                    .append("</tr>");
        }
        return """
                <!doctype html>
                <html lang="es"><body style="margin:0;background:#f8fafc;font-family:Arial,sans-serif;color:#0f172a">
                <main style="max-width:720px;margin:24px auto;background:#ffffff;border:1px solid #e2e8f0;border-radius:16px;overflow:hidden">
                  <header style="padding:28px 32px;background:#0f172a;color:#ffffff">
                    <p style="margin:0 0 8px;color:#fb923c;font-size:13px;font-weight:700;text-transform:uppercase;letter-spacing:.08em">Taller San Marcos</p>
                    <h1 style="margin:0;font-size:24px">Servicios programados para mañana</h1>
                  </header>
                  <section style="padding:28px 32px">
                    <p style="margin:0 0 8px">Hola, <strong>%s</strong>.</p>
                    <p style="margin:0 0 22px;color:#475569">Hay <strong>%d servicio(s)</strong> programado(s) para <strong>%s</strong>. Puedes preparar el trabajo y recordar al cliente con anticipación.</p>
                    <table style="border-collapse:collapse;width:100%%;font-size:14px">
                      <thead><tr style="background:#fff7ed;text-align:left;color:#9a3412"><th style="padding:12px">Cliente</th><th style="padding:12px">Placa</th><th style="padding:12px">Vehículo</th><th style="padding:12px">Trabajo previsto</th></tr></thead>
                      <tbody>%s</tbody>
                    </table>
                    <p style="margin:24px 0 0;color:#64748b;font-size:13px">Ingresa al sistema para revisar todos los detalles o ajustar una programación.</p>
                  </section>
                </main></body></html>
                """.formatted(
                escapeHtml(reminder.recipient().getFullName()),
                reminder.scheduledServices().size(),
                escapeHtml(DATE_FORMATTER.format(reminder.scheduledDate())),
                rows
        );
    }

    private String escapeHtml(String value) {
        return value.replace("&", "&amp;")
                .replace("<", "&lt;")
                .replace(">", "&gt;")
                .replace("\"", "&quot;")
                .replace("'", "&#39;");
    }
}
