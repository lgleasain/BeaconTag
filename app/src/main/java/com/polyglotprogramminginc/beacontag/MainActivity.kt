package com.polyglotprogramminginc.beacontag

import android.app.Fragment
import android.app.FragmentTransaction
import android.bluetooth.BluetoothDevice
import android.content.*
import android.content.pm.PackageManager
import android.databinding.DataBindingUtil
import android.os.Build
import android.support.v7.app.AppCompatActivity
import android.os.Bundle
import android.support.v7.app.AlertDialog
import android.util.Log
import android.view.View
import android.widget.ListView
import android.widget.TextView
import android.widget.Toast
import com.firebase.ui.database.FirebaseListAdapter
import com.google.firebase.FirebaseApp
import com.google.firebase.database.*
import com.mbientlab.bletoolbox.scanner.BleScannerFragment
import com.mbientlab.metawear.MetaWearBleService
import com.mbientlab.metawear.MetaWearBoard
import com.polyglotprogramminginc.beacontag.databinding.ActivityMainBinding
import com.polyglotprogramminginc.beacontag.fragments.MWDeviceConfirmationFragment
import com.polyglotprogramminginc.beacontag.fragments.MWScannerFragment
import com.polyglotprogramminginc.beacontag.model.GameStatus
import com.polyglotprogramminginc.beacontag.model.MainActivityData
import com.polyglotprogramminginc.beacontag.model.MainActivityStatusData
import com.polyglotprogramminginc.beacontag.utils.FirebaseController
import com.polyglotprogramminginc.beacontag.utils.SettingsManager
import java.util.*

class MainActivity : AppCompatActivity(), BleScannerFragment.ScannerCommunicationBus,
        MWDeviceConfirmationFragment.DeviceConfirmCallback {

    private val firebaseController: FirebaseController by lazy {
        FirebaseController(activity = this, gameStatus = gameStatus)
    }

    private val firebaseDatabase: FirebaseDatabase by lazy {
        FirebaseDatabase.getInstance()
    }

    private val users: DatabaseReference by lazy {
        firebaseDatabase!!.getReference(commonConstants.FIREBASE_USERS_REFERENCE)
    }

    private val gameStatusDatabase: DatabaseReference by lazy {
        firebaseDatabase!!.getReference(commonConstants.FIREBASE_GAME_STATUS_REFERENCE)
    }

    lateinit var usersFirebaseListAdapter: FirebaseListAdapter<MainActivityData>

    private val settingsManager: SettingsManager = SettingsManager(this)

    private var gameStatus: GameStatus = GameStatus()

    val mainActivityData: MainActivityData by lazy {
        val mainActivityData = MainActivityData()
        settingsManager.readMainActivityDatatPreferences(mainActivityData)
    }

    val commonConstants: CommonConstants = CommonConstants()
    val mainActivityStatusData: MainActivityStatusData = MainActivityStatusData()
    private val PERMISSION_REQUEST_COARSE_LOCATION = 1
    protected val TAG: String = "mainActivity"
    private var mwScannerFragment: MWScannerFragment? = null
    private var btDeviceSelected: Boolean = false
    private var mwBinder: MetaWearBleService.LocalBinder? = null
    private var mwBoard: MetaWearBoard? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            // Android M Permission check 
            if (this.checkSelfPermission(android.Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
                var builder = android.app.AlertDialog.Builder(this)
                builder.setTitle("This app needs location access")
                builder.setMessage("Please grant location access so this app can detect beacons.")
                builder.setPositiveButton(android.R.string.ok, null)
                builder.setOnDismissListener({ dialog: DialogInterface ->
                    requestPermissions(
                            arrayOf(android.Manifest.permission.ACCESS_COARSE_LOCATION),
                            PERMISSION_REQUEST_COARSE_LOCATION)
                }
                )
                builder.show()
            }
        }

        startService(Intent(this, GameService::class.java))
        // fix this
        FirebaseApp.initializeApp(applicationContext)


        val binding: ActivityMainBinding = DataBindingUtil.setContentView(this,
                R.layout.activity_main)

        binding.mainActivityData = mainActivityData
        binding.firebaseController = firebaseController
        binding.mainStatusData = mainActivityStatusData
        binding.mainActivity = this

        val intent: Intent = Intent(commonConstants.SERVICE_NOTIFICATION)
        intent.putExtra("MainActivityData", mainActivityData.toString())
        sendBroadcast(intent)

        setupFirebaseListeners()
    }

    fun scan() {
        Log.i(TAG, "scanning")
        if (mwScannerFragment != null) {
            val metawearBlescannerPopup: Fragment = getFragmentManager().findFragmentById(R.id.metawear_blescanner_popup_fragment)
            if (metawearBlescannerPopup != null) {
                val fragmentTransaction: FragmentTransaction = getFragmentManager().beginTransaction()
                fragmentTransaction.remove(metawearBlescannerPopup);
                fragmentTransaction.commit();
            }
            mwScannerFragment!!.dismiss();
        }
        mwScannerFragment = MWScannerFragment();
        mwScannerFragment!!.show(getFragmentManager(), "metawear_scanner_fragment");
    }

    override fun getFilterServiceUuids(): Array<out UUID> {
        return arrayOf(UUID.fromString("326a9000-85cb-9195-d9dd-464cfbbae75a"))
    }

    override fun onDeviceSelected(bluetoothDevice: BluetoothDevice?) {
        btDeviceSelected = true
        connectDevice(bluetoothDevice!!)
        val metawearBlescannerPopup: Fragment = getFragmentManager().findFragmentById(R.id.metawear_blescanner_popup_fragment)
        val fragmentTransaction: FragmentTransaction = getFragmentManager().beginTransaction()
        fragmentTransaction.remove(metawearBlescannerPopup)
        fragmentTransaction.commit()
        mwScannerFragment!!.dismiss()
    }

    override fun getScanDuration(): Long {
        return 10000
    }

    // MetaWear Connection Helper methods
    private fun connectDevice(device: BluetoothDevice) {
        val intent: Intent = Intent(commonConstants.SERVICE_NOTIFICATION)
        intent.putExtra(commonConstants.MAC_ADDRESS, device.address)
        intent.putExtra(commonConstants.COMMAND, commonConstants.CONNECT)
        sendBroadcast(intent)
    }

    internal inner class NLServiceReceiver : BroadcastReceiver() {

        override fun onReceive(context: Context, intent: Intent) {
            //todo put logic here to listen to broadcasts
        }
    }
    /**
     * connection handlers
     */
    private val connectionStateHandler: MetaWearBoard.ConnectionStateHandler = object : MetaWearBoard.ConnectionStateHandler() {

        override fun connected() {
            Log.i("Metawear Controller", "Device Connected");
            runOnUiThread(object : Runnable {
                override fun run() {
                    Toast.makeText(getApplicationContext(), "MetaWear Connected",
                            Toast.LENGTH_SHORT).show();
                }
            }
            )

            if (btDeviceSelected) {
                val mwDeviceConfirmFragment: MWDeviceConfirmationFragment = MWDeviceConfirmationFragment()
                mwDeviceConfirmFragment.flashDeviceLight(mwBoard, getFragmentManager())
                btDeviceSelected = false
            }
        }

        override fun disconnected() {
            Log.i("Metawear Controler", "Device Disconnected");
            runOnUiThread(object : Runnable {
                override fun run() {
                    Toast.makeText(getApplicationContext(), "MetaWear Disconnected", Toast.LENGTH_SHORT).show();
                }
            })

        }
    }

    override fun pairDevice() {
        settingsManager.setMetawearMacId(mwBoard!!.macAddress)
        val intent: Intent = Intent(commonConstants.SERVICE_NOTIFICATION)
        intent.putExtra(commonConstants.MAC_ADDRESS, mwBoard!!.macAddress)
        intent.putExtra(commonConstants.COMMAND, commonConstants.SELECT_METAWEAR)
        sendBroadcast(intent)
    }

    override fun dontPairDevice() {
        mwBoard!!.disconnect()
    }

    private fun setupFirebaseListeners() {
        usersFirebaseListAdapter = object : FirebaseListAdapter<MainActivityData>(this,
                MainActivityData::class.java, R.layout.player, users) {
            override fun populateView(view: View?, mainActivityData: MainActivityData?, position: Int) {
                Log.i("populate view", mainActivityData.toString())
                (view?.findViewById(R.id.listEmail) as TextView).text = mainActivityData!!.email
                (view?.findViewById(R.id.listBeaconId) as TextView).text = mainActivityData!!.beaconId
                var itColor: Int = android.R.color.white
                if (mainActivityData!!.isIt) {
                    itColor = android.R.color.holo_red_light
                }
                (view?.findViewById(R.id.listStatus) as TextView).setBackgroundResource(itColor)
            }
        }

        val usersView: ListView = findViewById(R.id.players) as ListView
        usersView.adapter = usersFirebaseListAdapter

        gameStatusDatabase.addValueEventListener(object : ValueEventListener {
            override fun onDataChange(databaseSnapshot: DataSnapshot?) {
                gameStatus = databaseSnapshot!!.getValue(GameStatus::class.java) ?: gameStatus
                Log.i("status changed", gameStatus.toString())
                firebaseController.gameStatus = gameStatus
                if (gameStatus.status.equals(commonConstants.GAME_STARTED)) {
                    mainActivityStatusData.setStartStopButtonText("Stop")
                    if (gameStatus.itPerson.equals(mainActivityData.beaconId)) {
                        (findViewById(R.id.status) as TextView).setBackgroundResource(android.R.color.holo_red_dark)
                        mainActivityStatusData.setMainStatusText("It")
                        mainActivityData.isIt = true
                    } else {
                        (findViewById(R.id.status) as TextView).setBackgroundResource(android.R.color.holo_green_dark)
                        mainActivityStatusData.setMainStatusText("Not It")
                        mainActivityData.isIt = false
                    }
                    val intent: Intent = Intent(commonConstants.SERVICE_NOTIFICATION)
                    intent.putExtra("MainActivityData", mainActivityData)
                    intent.putExtra(commonConstants.COMMAND, commonConstants.MAIN_ACTIVITY_DATA)
                    sendBroadcast(intent)
                } else {
                    (findViewById(R.id.status) as TextView).setBackgroundResource(android.R.color.darker_gray)
                    mainActivityStatusData.setStartStopButtonText("Start")
                    mainActivityStatusData.setMainStatusText("Not Playing")
                }
                Log.i("new button text", mainActivityStatusData.toString())
            }

            override fun onCancelled(databaseError: DatabaseError?) {
            }
        })
    }

    override fun onRequestPermissionsResult(requestCode: Int,
                                            permissions: Array<String>, grantResults: IntArray) {
        when (requestCode) {
            PERMISSION_REQUEST_COARSE_LOCATION -> {
                if (grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    Log.d(TAG, "coarse location permission granted")
                } else {
                    val builder = AlertDialog.Builder(this)
                    builder.setTitle("Functionality limited")
                    builder.setMessage("Since location access has not been granted, this app will not be able to discover beacons when in the background.")
                    builder.setPositiveButton(android.R.string.ok, null)
                    builder.setOnDismissListener(object : DialogInterface.OnDismissListener {
                        override fun onDismiss(dialog: DialogInterface) {
                        }
                    })
                    builder.show()
                }
                return
            }
        }
    }

}
