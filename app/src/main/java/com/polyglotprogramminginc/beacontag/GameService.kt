package com.polyglotprogramminginc.beacontag

import android.app.Service
import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothManager
import android.bluetooth.le.*
import android.content.*
import android.os.Handler
import android.os.IBinder
import android.os.Looper
import android.os.ParcelUuid
import android.util.Log

import com.google.firebase.FirebaseApp
import com.google.firebase.database.*
import com.mbientlab.metawear.MetaWearBleService
import com.mbientlab.metawear.MetaWearBoard
import com.mbientlab.metawear.UnsupportedModuleException
import com.mbientlab.metawear.module.Led
import com.polyglotprogramminginc.beacontag.model.GameStatus
import com.polyglotprogramminginc.beacontag.model.MainActivityData
import com.polyglotprogramminginc.beacontag.model.SampleBeacon
import com.polyglotprogramminginc.beacontag.utils.FirebaseController
import java.util.*

/**
 * Created by Lance Gleason of Polyglot Programming LLC. on 11/23/16.
 * http://www.polyglotprogramminginc.com
 * https://github.com/lgleasain
 * Twitter: @lgleasain
 */

class GameService : Service(), ServiceConnection {

    val commonConstants: CommonConstants = CommonConstants()
    private var database: FirebaseDatabase? = null
    private var firebaseController: FirebaseController? = null
    private var gameStatusReference: DatabaseReference? = null
    private var gameStatus: GameStatus? = null
    private val TAG: String = "GameService"
    lateinit private var mainActivityData: MainActivityData
    private val nlservicereciver: NLServiceReceiver by lazy {
        NLServiceReceiver()
    }
    private val btManager: BluetoothManager by lazy {
        getSystemService(Context.BLUETOOTH_SERVICE) as BluetoothManager
    }
    private var mwBinder: MetaWearBleService.LocalBinder? = null
    private var mwBoard: MetaWearBoard? = null
    private var ledModule: Led? = null
    private var btDevice: BluetoothDevice? = null
    private var gameService: GameService = this

    lateinit private var mBluetoothLeScanner: BluetoothLeScanner

    override fun onCreate() {
        FirebaseApp.initializeApp(applicationContext)
        firebaseController = FirebaseController(this, null)
        database = FirebaseDatabase.getInstance()

        val filter: IntentFilter = IntentFilter()
        filter.addAction(commonConstants.SERVICE_NOTIFICATION)
        registerReceiver(nlservicereciver, filter)
        Log.i(TAG, "on create done")

        val manager = getSystemService(Context.BLUETOOTH_SERVICE) as BluetoothManager
        mBluetoothLeScanner = manager.adapter.bluetoothLeScanner

        startScanning()

        gameStatusReference = database!!.getReference(commonConstants.FIREBASE_GAME_STATUS_REFERENCE)
        gameStatusReference!!.addValueEventListener(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot?) {
                gameStatus = snapshot!!.getValue(GameStatus::class.java)
                Log.i("GameService", gameStatus.toString())
                flashDeviceLight()
            }

            override fun onCancelled(databaseError: DatabaseError?) {
            }
        }
        )

        bindService(Intent(gameService, MetaWearBleService::class.java),
                gameService, Context.BIND_AUTO_CREATE)

        super.onCreate()
    }

    override fun onBind(intent: Intent): IBinder? {
        return null
    }

    internal inner class NLServiceReceiver : BroadcastReceiver() {

        override fun onReceive(context: Context, intent: Intent) {
            val command = intent.getSerializableExtra(commonConstants.COMMAND) as String
            if (command.equals(commonConstants.MAIN_ACTIVITY_DATA)) {
                Log.i("GameService", "setting main activity data")
                mainActivityData = intent.getSerializableExtra("MainActivityData") as MainActivityData
                Log.i("GameService", mainActivityData.toString())
                flashDeviceLight()
            } else if(command.equals(commonConstants.BLE_DEVICE)){
                Log.i(TAG, "recieved mac address")
                val macAddress = intent.getSerializableExtra(commonConstants.MAC_ADDRESS) as String
                Log.i(TAG, macAddress)
                btDevice = btManager.adapter.getRemoteDevice(macAddress)
                mwBoard = mwBinder!!.getMetaWearBoard(btDevice)
                mwBoard!!.setConnectionStateHandler(connectionStateHandler);
                mwBoard!!.connect()
            } else {
                Log.i(TAG, "recieved mac address")
                val macAddress = intent.getSerializableExtra(commonConstants.MAC_ADDRESS) as String
                Log.i(TAG, macAddress)
                btDevice = btManager.adapter.getRemoteDevice(macAddress)
                mwBoard = mwBinder!!.getMetaWearBoard(btDevice)
                mwBoard!!.setConnectionStateHandler(connectionStateHandler);
                mwBoard!!.connect()
            }
        }
    }

    override fun onDestroy() {
        Log.i(TAG, "on destroy called")
        stopScanning()
        super.onDestroy()
    }

    // MetaWear Code
    override fun onServiceConnected(componentName: ComponentName?, iBinder: IBinder?) {
        Log.i(TAG, "binding succeeded")
        mwBinder = iBinder as MetaWearBleService.LocalBinder
    }

    override fun onServiceDisconnected(p0: ComponentName?) {
    }

    /**
     * MetaWear connection handlers
     */
    private val connectionStateHandler: MetaWearBoard.ConnectionStateHandler = object : MetaWearBoard.ConnectionStateHandler() {

        override fun connected() {
            Log.i("Metawear Controller", "Device Connected");
            try {
                ledModule = mwBoard!!.getModule(Led::class.java)
            } catch (e: UnsupportedModuleException) {
                Log.e("Led Fragment", e.toString())
            }
            flashDeviceLight()
        }

        override fun disconnected() {
            Log.i("Metawear Controler", "Device Disconnected");
        }
    }

    fun flashDeviceLight() {
        Log.i(TAG, "flash device light")
        Log.i(TAG, mwBoard.toString())
        if (mwBoard != null && mwBoard!!.isConnected) {
            if (gameStatus!!.status.equals(commonConstants.GAME_STARTED)) {

                var colorChannel = Led.ColorChannel.GREEN

                if (mainActivityData.isIt) {
                    colorChannel = Led.ColorChannel.RED
                }

                ledModule!!.configureColorChannel(colorChannel).setRiseTime(375.toShort()).setPulseDuration(1000.toShort()).setRepeatCount((-1).toByte()).setHighTime(250.toShort()).setFallTime(375.toShort()).setLowIntensity(0.toByte()).setHighIntensity(31.toByte()).commit()

                ledModule!!.play(true)
            } else {
                ledModule!!.stop(true)
            }
        }
    }

    /* Being scanning for Eddystone advertisers */
    private fun startScanning() {
        val beaconFilter = ScanFilter.Builder().setServiceUuid(UID_SERVICE).setServiceData(UID_SERVICE, NAMESPACE_FILTER, NAMESPACE_FILTER_MASK).build()

        val telemetryFilter = ScanFilter.Builder().setServiceUuid(UID_SERVICE).setServiceData(UID_SERVICE, TLM_FILTER, TLM_FILTER_MASK).build()

        val filters = ArrayList<ScanFilter>()
        filters.add(beaconFilter)
        filters.add(telemetryFilter)

        val settings = ScanSettings.Builder().setScanMode(ScanSettings.SCAN_MODE_LOW_LATENCY).build()

        mBluetoothLeScanner!!.startScan(filters, settings, mScanCallback)
        if (DEBUG_SCAN) Log.d(TAG, "Scanning started…")
    }

    /* Terminate scanning */
    private fun stopScanning() {
        mBluetoothLeScanner!!.stopScan(mScanCallback)
        if (DEBUG_SCAN) Log.d(TAG, "Scanning stopped…")
    }

    // game logic
    private fun determineStatus(rssi: Int, id: String) {
        val integerBeaconValue: String = Integer.valueOf(id, 16).toString()
        Log.i("integerBeaconValue", integerBeaconValue)
        Log.i("mainActivityData", mainActivityData.toString())
        if (mainActivityData.isIt and !(mainActivityData.beaconId.toInt().equals(Integer.valueOf(id, 16)))) {
            val user: MainActivityData? = firebaseController!!.currentUsers[integerBeaconValue]
            Log.i("next logig", user.toString())
            Log.i("game status", gameStatus.toString())
            if ((user != null) and (rssi > -90) and
                    ((System.currentTimeMillis() - gameStatus!!.statusChange) > 5000)) {
                gameStatus!!.itPerson = integerBeaconValue
                gameStatus!!.statusChange = System.currentTimeMillis()
                gameStatusReference!!.setValue(gameStatus)
                user!!.isIt = true
                firebaseController!!.updateUser(user)
                mainActivityData.isIt = false
                firebaseController!!.updateUser(mainActivityData)
            }
        }
    }

    /* Handle UID packet discovery on the main thread */
    private fun processUidPacket(deviceAddress: String, rssi: Int, id: String) {
        if (DEBUG_SCAN) {
            Log.d(TAG, "Eddystone($deviceAddress) id = $id rssi $rssi")
            determineStatus(rssi, id)
        }
    }

    /* Handle TLM packet discovery on the main thread */
    private fun processTlmPacket(deviceAddress: String, battery: Float, temp: Float) {
        if (DEBUG_SCAN) {
            Log.d(TAG, "Eddystone(" + deviceAddress + ") battery = " + battery
                    + ", temp = " + temp)
        }
    }

    /* Process each unique BLE scan result */
    private val mScanCallback = object : ScanCallback() {
        private val mCallbackHandler = Handler(Looper.getMainLooper())

        override fun onScanResult(callbackType: Int, result: ScanResult) {
            processResult(result)
        }

        override fun onScanFailed(errorCode: Int) {
            Log.w(TAG, "Scan Error Code: " + errorCode)
        }

        override fun onBatchScanResults(results: List<ScanResult>) {
            for (result in results) {
                processResult(result)
            }
        }

        private fun processResult(result: ScanResult) {
            val data = result.scanRecord!!.getServiceData(UID_SERVICE)
            if (data == null) {
                Log.w(TAG, "Invalid Eddystone scan result.")
                return
            }

            val deviceAddress = result.device.address
            val rssi = result.rssi
            val frameType = data[0]
            when (frameType) {
                TYPE_UID -> {
                    val id = SampleBeacon.getInstanceId(data)

                    mCallbackHandler.post { processUidPacket(deviceAddress, rssi, id) }
                }
                TYPE_TLM -> {
                    //Parse out battery voltage
                    val battery = SampleBeacon.getTlmBattery(data)
                    val temp = SampleBeacon.getTlmTemperature(data)
                    mCallbackHandler.post { processTlmPacket(deviceAddress, battery, temp) }
                }
                TYPE_URL ->
                    //Do nothing, ignoring these
                    return
                else -> Log.w(TAG, "Invalid Eddystone scan result.")
            }
        }
    }

    companion object {
        private val TAG = GameService::class.java!!.getSimpleName()

        // …if you feel like making the log a bit noisier…
        private val DEBUG_SCAN = true

        // Eddystone service uuid (0xfeaa)
        private val UID_SERVICE = ParcelUuid.fromString("0000feaa-0000-1000-8000-00805f9b34fb")

        // Default namespace id for KST Eddystone beacons (d89bed6e130ee5cf1ba1)
        private val NAMESPACE_FILTER = byteArrayOf(0x00, //Frame type
                0x00, //TX power
                0xaa.toByte(), 0xaa.toByte(), 0xaa.toByte(), 0xaa.toByte(), 0xdd.toByte(), 0xdd.toByte(), 0xcc.toByte(), 0xcc.toByte(), 0x66.toByte(), 0x66.toByte(), 0x00, 0x00, 0x00, 0x00, 0x00, 0x00)

        // Force frame type and namespace id to match
        private val NAMESPACE_FILTER_MASK = byteArrayOf(0xFF.toByte(), 0x00, 0xFF.toByte(), 0xFF.toByte(), 0xFF.toByte(), 0xFF.toByte(), 0xFF.toByte(), 0xFF.toByte(), 0xFF.toByte(), 0xFF.toByte(), 0xFF.toByte(), 0xFF.toByte(), 0x00, 0x00, 0x00, 0x00, 0x00, 0x00)

        private val TLM_FILTER = byteArrayOf(0x20, //Frame type
                0x00, //Protocol version = 0
                0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00)

        // Force frame type and protocol to match
        private val TLM_FILTER_MASK = byteArrayOf(0xFF.toByte(), 0xFF.toByte(), 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00)

        // Eddystone frame types
        private val TYPE_UID: Byte = 0x00
        private val TYPE_URL: Byte = 0x10
        private val TYPE_TLM: Byte = 0x20
    }
}
