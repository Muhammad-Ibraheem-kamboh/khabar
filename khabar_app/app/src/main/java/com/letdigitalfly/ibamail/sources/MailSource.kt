package com.letdigitalfly.ibamail.sources

import com.letdigitalfly.ibamail.domain.model.RawMail
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.emptyFlow

interface MailSource {
    val id: String
    val displayName: String
    val isConfigured: Boolean
    val isConnected: Boolean

    suspend fun fetchNew(): List<RawMail>
    val updatesFlow: Flow<RawMail> get() = emptyFlow()
}
