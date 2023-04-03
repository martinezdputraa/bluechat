package com.martinezdputra.bluechat.presentation

import com.martinezdputra.bluechat.domain.chat.BluetoothDeviceDomain

data class BluetoothUiState(
    val scannedDevices: List<BluetoothDeviceDomain> = emptyList(),
    val pairedDevices: List<BluetoothDeviceDomain> = emptyList(),
)
