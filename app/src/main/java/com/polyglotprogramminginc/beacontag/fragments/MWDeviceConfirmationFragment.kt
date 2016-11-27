package com.polyglotprogramminginc.beacontag.fragments

import android.app.Activity
import android.app.DialogFragment
import android.app.FragmentManager
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button

import com.mbientlab.metawear.MetaWearBoard
import com.mbientlab.metawear.UnsupportedModuleException
import com.mbientlab.metawear.module.Led
import com.polyglotprogramminginc.beacontag.R

/**
 * Created by Lance Gleason of Polyglot Programming LLC. on 11/27/16.
 * http://www.polyglotprogramminginc.com
 * https://github.com/lgleasain
 * Twitter: @lgleasain
 */

class MWDeviceConfirmationFragment : DialogFragment() {

    private var ledModule: Led? = null
    private var yesButton: Button? = null
    private var noButton: Button? = null
    private var callback: DeviceConfirmCallback? = null

    interface DeviceConfirmCallback {
        fun pairDevice()
        fun dontPairDevice()
    }


    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?,
                              savedInstanceState: Bundle?): View? {
        // Inflate the layout for this fragment
        return inflater.inflate(R.layout.fragment_mwdevice_confirmation, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        noButton = view.findViewById(R.id.confirm_no) as Button
        noButton!!.setOnClickListener {
            ledModule!!.stop(true)
            callback!!.dontPairDevice()
            dismiss()
        }

        yesButton = view.findViewById(R.id.confirm_yes) as Button
        yesButton!!.setOnClickListener {
            ledModule!!.stop(true)
            callback!!.pairDevice()
            dismiss()
        }

    }

    override fun onAttach(activity: Activity) {
        if (activity !is DeviceConfirmCallback) {
            throw RuntimeException("Acitivty does not implement DeviceConfirmationCallback interface")
        }

        callback = activity
        super.onAttach(activity)
    }

    fun flashDeviceLight(mwBoard: MetaWearBoard?, fragmentManager: FragmentManager?) {
        try {
            ledModule = mwBoard!!.getModule(Led::class.java)
        } catch (e: UnsupportedModuleException) {
            Log.e("Led Fragment", e.toString())
        }

        ledModule!!.configureColorChannel(Led.ColorChannel.BLUE).setRiseTime(750.toShort()).setPulseDuration(2000.toShort()).setRepeatCount((-1).toByte()).setHighTime(500.toShort()).setFallTime(750.toShort()).setLowIntensity(0.toByte()).setHighIntensity(31.toByte()).commit()

        ledModule!!.play(true)

        show(fragmentManager, "device_confirm_callback")
    }


}// Required empty public constructor

