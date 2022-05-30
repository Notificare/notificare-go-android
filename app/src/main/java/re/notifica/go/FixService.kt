package re.notifica.go

import re.notifica.Notificare
import re.notifica.push.gms.NotificarePushService

class FixService : NotificarePushService() {
    override fun onNewToken(token: String) {
        if (Notificare.isConfigured) {
            super.onNewToken(token)
        }
    }
}
