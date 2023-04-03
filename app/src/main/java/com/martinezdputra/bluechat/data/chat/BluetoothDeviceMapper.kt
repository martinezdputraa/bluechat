package com.martinezdputra.bluechat.data.chat

import android.annotation.SuppressLint
import android.bluetooth.BluetoothDevice
import com.martinezdputra.bluechat.domain.chat.BluetoothDeviceDomain

@SuppressLint("MissingPermission")
fun BluetoothDevice.toBluetoothDeviceDomain() = BluetoothDeviceDomain(
    name = name,
    address = address,
)
