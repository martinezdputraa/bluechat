package com.martinezdputra.bluechat.data.chat

import android.Manifest
import android.annotation.SuppressLint
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothManager
import android.bluetooth.BluetoothServerSocket
import android.bluetooth.BluetoothSocket
import android.content.Context
import android.content.IntentFilter
import android.content.pm.PackageManager
import com.martinezdputra.bluechat.domain.chat.BluetoothController
import com.martinezdputra.bluechat.domain.chat.BluetoothDeviceDomain
import com.martinezdputra.bluechat.domain.chat.ConnectionResult
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.onCompletion
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.io.IOException
import java.util.*

@SuppressLint("MissingPermission")
class AndroidBluetoothController(
    private val context: Context
): BluetoothController {

    private val bluetoothManager by lazy {
        context.getSystemService(BluetoothManager::class.java)
    }
    private val bluetoothAdapter by lazy {
        bluetoothManager?.adapter
    }

    private val _isConnected = MutableStateFlow(false)
    override val isConnected: StateFlow<Boolean>
        get() = _isConnected.asStateFlow()

    private val _scannedDevices = MutableStateFlow<List<BluetoothDeviceDomain>>(emptyList())
    override val scannedDevices: StateFlow<List<BluetoothDeviceDomain>>
        get() = _scannedDevices.asStateFlow()

    private val _pairedDevices = MutableStateFlow<List<BluetoothDeviceDomain>>(emptyList())
    override val pairedDevices: StateFlow<List<BluetoothDeviceDomain>>
        get() = _pairedDevices.asStateFlow()

    private val _errors = MutableSharedFlow<String>()
    override val errors: SharedFlow<String>
        get() = _errors.asSharedFlow()

    private val foundDeviceReceiver = FoundDeviceReceiver { device ->
        _scannedDevices.update { devices ->
            val newDevice = device.toBluetoothDeviceDomain()
            if(newDevice in devices) devices else devices + newDevice
        }
    }

    private val bluetoothStateReceiver = BluetoothStateReceiver { isConnected, bluetoothDevice ->
        if(bluetoothAdapter?.bondedDevices?.contains(bluetoothDevice) == true) {
            _isConnected.update { isConnected }
        } else {
            CoroutineScope(Dispatchers.IO).launch {
                _errors.tryEmit("Can't connect to a non-paired device.")
            }
        }
    }

    private var currentServerSocket: BluetoothServerSocket? = null
    private var currentClientSocket: BluetoothSocket? = null

    init {
        updatePairedDevices()
        context.registerReceiver(
            bluetoothStateReceiver,
            IntentFilter().apply {
                addAction(BluetoothAdapter.ACTION_CONNECTION_STATE_CHANGED)
                addAction(BluetoothDevice.ACTION_ACL_CONNECTED)
                addAction(BluetoothDevice.ACTION_ACL_DISCONNECTED)
            }
        )
    }

    override fun startDiscovery() {
        doIfHasPermission(Manifest.permission.BLUETOOTH_SCAN) {
            context.registerReceiver(
                foundDeviceReceiver,
                IntentFilter(BluetoothDevice.ACTION_FOUND)
            )

            updatePairedDevices()

            bluetoothAdapter?.startDiscovery()
        }
    }

    override fun stopDiscovery() {
        doIfHasPermission(Manifest.permission.BLUETOOTH_SCAN) {
            bluetoothAdapter?.cancelDiscovery()
        }
    }

    override fun startBluetoothServer(): Flow<ConnectionResult> {
        return flow {
            doIfHasPermission(
                permission = Manifest.permission.BLUETOOTH_CONNECT,
                noPermissionAction = {
                    throw SecurityException("No BLUETOOTH_CONNECT permission")
                }
            ) {
                currentServerSocket = bluetoothAdapter?.listenUsingRfcommWithServiceRecord(
                    "blue_chat_service",
                    UUID.fromString(SERVICE_UUID)
                )

                var shouldLoop = true
                while(shouldLoop) {
                    currentClientSocket = try {
                        currentServerSocket?.accept()
                    } catch (e: IOException) {
                        shouldLoop = false
                        null
                    }
                    /**
                     * when a pair has been accepted,
                     * we no longer need the server
                     * that's why we close it **/
                    emit(ConnectionResult.ConnectionEstablished)
                    currentClientSocket?.let {
                        currentServerSocket?.close()
                    }
                }
            }
        }.onCompletion {
            closeConnection()
        }.flowOn(Dispatchers.IO)
    }

    override fun connectToDevice(device: BluetoothDeviceDomain): Flow<ConnectionResult> {
        return flow {
            doIfHasPermission(
                permission = Manifest.permission.BLUETOOTH_CONNECT,
                noPermissionAction = {
                    throw SecurityException("No BLUETOOTH_CONNECT permission")
                }
            ) {
                val bluetoothDevice = bluetoothAdapter
                    ?.getRemoteDevice(device.address)

                currentClientSocket = bluetoothDevice
                    ?.createRfcommSocketToServiceRecord(
                        UUID.fromString(SERVICE_UUID)
                    )
                stopDiscovery()

                if(bluetoothAdapter?.bondedDevices?.contains(bluetoothDevice) == false) {

                }

                currentClientSocket?.let { socket ->
                    try {
                        socket.connect()
                        emit(ConnectionResult.ConnectionEstablished)
                    } catch (e: IOException) {
                        socket.close()
                        currentClientSocket = null
                        emit(ConnectionResult.Error("Connection was interrupted"))
                    }
                }
            }
        }.onCompletion {
            closeConnection()
        }.flowOn(Dispatchers.IO)
    }

    override fun closeConnection() {
        currentClientSocket?.close()
        currentServerSocket?.close()
        currentClientSocket = null
        currentServerSocket = null
    }

    override fun release() {
        context.unregisterReceiver(foundDeviceReceiver)
        context.unregisterReceiver(bluetoothStateReceiver)
        closeConnection()
    }

    @SuppressLint("MissingPermission")
    private fun updatePairedDevices() {
        doIfHasPermission(Manifest.permission.BLUETOOTH_CONNECT) {
            bluetoothAdapter
                ?.bondedDevices
                ?.map { it.toBluetoothDeviceDomain() }
                ?.also { devices ->
                    _pairedDevices.update { devices }
                }
        }
    }

    private inline fun doIfHasPermission(
        permission: String,
        noPermissionAction: () -> Unit = {},
        hasPermissionAction: () -> Unit,
    ) {
        if(hasPermission(permission)) {
            hasPermissionAction()
        } else {
            noPermissionAction()
        }
    }

    private fun hasPermission(permission: String): Boolean {
        return context.checkSelfPermission(permission) == PackageManager.PERMISSION_GRANTED
    }

    companion object {
        private const val SERVICE_UUID = "be9048b3-0766-49b6-ba83-0ac8b101abb6"
    }
}
