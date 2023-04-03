package com.martinezdputra.bluechat.domain.chat

import com.martinezdputra.bluechat.data.chat.BluetoothMessage

sealed interface ConnectionResult {
    object ConnectionEstablished: ConnectionResult

    data class TransferSucceeded(val message: BluetoothMessage): ConnectionResult
    data class Error(val message: String): ConnectionResult
}
