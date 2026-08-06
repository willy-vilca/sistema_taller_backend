const { app } = require('@azure/functions')

const endpointPath = '/api/v1/internal/notifications/tomorrow-scheduled-services'

app.timer('daily-scheduled-service-reminder', {
  // 19:00 UTC equivale a las 2:00 p. m. de Lima (UTC-5).
  schedule: '%DAILY_REMINDER_SCHEDULE%',
  handler: async (timer, context) => {
    const backendUrl = requiredSetting('BACKEND_URL').replace(/\/$/, '')
    const internalSecret = requiredSetting('INTERNAL_NOTIFICATION_SECRET')
    const controller = new AbortController()
    const timeout = setTimeout(() => controller.abort(), 60_000)

    try {
      const response = await fetch(`${backendUrl}${endpointPath}`, {
        method: 'POST',
        headers: { 'X-Notification-Secret': internalSecret },
        signal: controller.signal,
      })
      const body = await response.text()
      if (!response.ok) {
        throw new Error(`La API respondió ${response.status}: ${body}`)
      }
      context.log(`Recordatorio diario ejecutado correctamente. Resultado: ${body}`)
    } finally {
      clearTimeout(timeout)
    }
  },
})

function requiredSetting(name) {
  const value = process.env[name]
  if (!value || !value.trim()) {
    throw new Error(`Falta configurar la variable ${name} en la Function App.`)
  }
  return value.trim()
}
