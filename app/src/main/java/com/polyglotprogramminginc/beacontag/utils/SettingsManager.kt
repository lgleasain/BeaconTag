package com.polyglotprogramminginc.beacontag.utils

import android.content.Context
import android.content.ContextWrapper
import android.content.SharedPreferences
import com.polyglotprogramminginc.beacontag.CommonConstants
import com.polyglotprogramminginc.beacontag.model.MainActivityData

/**
 *
 * Created by Lance Gleason of Polyglot Programming LLC. on 11/25/16.
 * http://www.polyglotprogramminginc.com
 * https://github.com/lgleasain
 * Twitter: @lgleasain
 *
 */
class SettingsManager constructor(activity: ContextWrapper){
    val USER_EMAIL : String = "com.polyglotprogramminginc.beacontag.USER_EMAIL"
    val USER_BEACON_ID : String = "com.polyglotprogramminginc.beacontag.USER_BEACON_ID"
    val METAWEAR_MAC_ID : String = "com.polyglotprogramminginc.beacontag.METAWEAR_MAC_ID"
    val commonConstants : CommonConstants = CommonConstants()

    val sharedPreferences : SharedPreferences by lazy {activity!!.getSharedPreferences(commonConstants.PREFERENCES_NAME, Context.MODE_PRIVATE)}
    val editor by lazy {sharedPreferences.edit()}

    fun readMainActivityDatatPreferences(mainActivityData: MainActivityData) : MainActivityData {
        mainActivityData.email = sharedPreferences.getString(USER_EMAIL, mainActivityData.email)
        mainActivityData.beaconId = sharedPreferences.getString(USER_BEACON_ID, mainActivityData.beaconId)
        return mainActivityData
    }

    fun setMainActivityDataPreferences(mainActivityData: MainActivityData){
        editor.putString(USER_EMAIL, mainActivityData.email)
        editor.putString(USER_BEACON_ID, mainActivityData.beaconId)
        editor.commit()
    }

    fun readMetawearMacId() : String{
        return sharedPreferences.getString(METAWEAR_MAC_ID, commonConstants.NO_METAWEAR)
    }

    fun setMetawearMacId(metawearMacId : String){
        editor.putString(METAWEAR_MAC_ID, metawearMacId)
        editor.commit()
    }
}
