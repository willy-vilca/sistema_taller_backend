# Recordatorios diarios de servicios programados

Esta versión envía un correo a cada administrador activo que tenga activada la preferencia de recordatorios y una dirección de correo registrada. El mensaje se genera a las **2:00 p. m. de Lima** e incluye los servicios pendientes del día siguiente.

La solución usa tres piezas:

1. La API Spring Boot obtiene los servicios, compone el correo y registra cada envío.
2. Azure Communication Services entrega el correo.
3. Una Azure Function con temporizador invoca la API todos los días.

## 1. Actualizar la base de datos

Antes de desplegar la API, ejecutar una sola vez el script:

`database/migrations/20260805_add_admin_email_notifications.sql`

El script agrega el correo y la preferencia de notificaciones a los usuarios, además de una tabla de auditoría para impedir envíos duplicados a un mismo administrador en la misma fecha.

Después de desplegar la API, iniciar sesión con un administrador, ir a **Administración > Usuarios**, editar cada administrador que deba recibir correos, registrar su dirección y activar **Recibir recordatorio diario**.

## 2. Crear Azure Communication Services Email

1. En Azure Portal, crear un recurso **Email Communication Service**. Para una prueba inicial se puede usar un dominio administrado por Azure; para producción conviene verificar un dominio propio del taller.
2. Crear o usar un recurso **Azure Communication Services**.
3. En el recurso de correo, conectar el dominio activo al recurso de Communication Services.
4. En el recurso de Communication Services, abrir **Keys** y copiar la **Connection string**.
5. Anotar la dirección que aparece como **MailFrom address** del dominio conectado. Esa será la dirección remitente, por ejemplo `DoNotReply@xxxxxxxx-xxxx-xxxx-xxxx-xxxxxxxxxxxx.azurecomm.net`.

La documentación oficial de Microsoft describe la creación del recurso y el envío de correos: <https://learn.microsoft.com/en-us/azure/communication-services/quickstarts/email/send-email>.

## 3. Configurar la API en App Service

En la App Service de la API, abrir **Configuración > Variables de entorno** y agregar estas variables. Guardar y reiniciar la aplicación después de hacerlo.

| Variable | Valor |
| --- | --- |
| `APP_NOTIFICATIONS_ENABLED` | `true` |
| `APP_NOTIFICATIONS_AZURE_COMMUNICATION_CONNECTION_STRING` | La Connection string copiada del recurso Azure Communication Services. |
| `APP_NOTIFICATIONS_SENDER_EMAIL` | La dirección exacta MailFrom del dominio conectado. |
| `APP_NOTIFICATIONS_INTERNAL_SECRET` | Un secreto aleatorio largo, de al menos 32 caracteres. No usar una contraseña ni guardarlo en Git. |

El último secreto protege la ruta interna que ejecuta el envío. Se reutiliza, exactamente igual, en la Function App.

Para generar un secreto en PowerShell:

```powershell
[Convert]::ToBase64String((1..48 | ForEach-Object { Get-Random -Maximum 256 }))
```

## 4. Crear y configurar la Azure Function

1. En Azure Portal, crear una **Function App** en la misma región que la API, con runtime **Node.js 22**. Azure solicitará una cuenta de almacenamiento; es necesaria para coordinar correctamente el temporizador.
2. Abrir la Function App y entrar a **Configuración > Variables de entorno**.
3. Agregar estas variables:

| Variable | Valor |
| --- | --- |
| `BACKEND_URL` | La URL base pública de la API, sin `/api/v1`. Ejemplo: `https://sistema-taller-api.azurewebsites.net` |
| `INTERNAL_NOTIFICATION_SECRET` | El mismo valor de `APP_NOTIFICATIONS_INTERNAL_SECRET` configurado en la API. |
| `DAILY_REMINDER_SCHEDULE` | `0 0 19 * * *` |

`19:00 UTC` equivale a `2:00 p. m.` en Lima. El temporizador usa una expresión CRON de seis campos, tal como documenta Azure Functions: <https://learn.microsoft.com/en-us/azure/azure-functions/functions-bindings-timer>.

`AzureWebJobsStorage` normalmente se crea automáticamente al crear la Function App; no se debe borrar.

> **Importante para Flex Consumption:** no agregar `FUNCTIONS_WORKER_RUNTIME` ni `FUNCTIONS_WORKER_RUNTIME_VERSION` como variables de entorno en Azure. En ese plan, Azure guarda el runtime seleccionado al crear la Function App y rechaza esas dos variables. `FUNCTIONS_WORKER_RUNTIME=node` solo se mantiene en `local.settings.json` para ejecutar el proyecto localmente.

## 5. Habilitar el despliegue desde GitHub

El workflow ya está en `.github/workflows/deploy-scheduled-reminder.yml` y se ejecutará al integrar cambios en `main`.

En el repositorio del backend, abrir **Settings > Secrets and variables > Actions** y crear:

| Tipo | Nombre | Valor |
| --- | --- | --- |
| Secret | `AZURE_FUNCTIONAPP_PUBLISH_PROFILE` | El contenido completo de **Get publish profile** descargado desde la nueva Function App. |
| Variable | `AZURE_FUNCTIONAPP_NAME` | El nombre exacto de la Function App creada en Azure. |

Al hacer merge de `pruebas` hacia `main`, GitHub instalará las dependencias de `reminder-function` y desplegará la Function automáticamente. También se puede lanzar manualmente desde la pestaña **Actions**.

## 6. Probar antes de esperar al horario diario

1. Crear por lo menos un servicio programado para mañana.
2. Confirmar que existe al menos un administrador activo con correo y la preferencia activada.
3. Confirmar que las cuatro variables de notificación de la API tienen los valores correctos.
4. Ejecutar temporalmente la ruta interna desde PowerShell, reemplazando URL y secreto:

```powershell
$headers = @{ 'X-Notification-Secret' = 'TU_SECRETO' }
Invoke-RestMethod -Method Post `
  -Uri 'https://TU-API.azurewebsites.net/api/v1/internal/notifications/tomorrow-scheduled-services' `
  -Headers $headers
```

Una ejecución correcta devuelve cantidades de servicios, administradores elegibles y correos enviados. El mismo administrador no recibirá una segunda copia para la misma fecha, incluso si se repite la llamada.

Para probar la Function localmente, copiar `reminder-function/local.settings.json.example` como `local.settings.json`, colocar los valores reales y ejecutar `npm install` seguido de `npm start` dentro de `reminder-function`. Se requiere Azure Functions Core Tools y Azurite para el almacenamiento local.

## Comportamiento esperado

- Si no hay servicios programados para mañana, no se envía correo.
- Solo participan usuarios `ADMIN` activos, con correo y preferencia activada.
- Los fallos de un destinatario se registran en `scheduled_service_notification_deliveries`; los demás administradores siguen recibiendo su resumen.
- Los logs de ejecución se consultan en **Function App > Monitor** y los errores de entrega de correo se revisan en el recurso de Azure Communication Services.
