package com.letdigitalfly.ibamail.sources

import com.letdigitalfly.ibamail.domain.model.RawMail
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.asSharedFlow

class InstantMailSource : MailSource {

    override val id: String = "instant_source"
    override val displayName: String = "Instant Mode (Notifications)"
    override val isConfigured: Boolean = true
    override val isConnected: Boolean = true

    companion object {
        private val _mailFlow = MutableSharedFlow<RawMail>(extraBufferCapacity = 20)
        val mailFlow: SharedFlow<RawMail> = _mailFlow.asSharedFlow()

        fun emitMail(mail: RawMail) {
            _mailFlow.tryEmit(mail)
        }
    }

    override val updatesFlow: SharedFlow<RawMail> = mailFlow

    override suspend fun fetchNew(): List<RawMail> {
        return emptyList() // Event-driven via updatesFlow
    }
}
