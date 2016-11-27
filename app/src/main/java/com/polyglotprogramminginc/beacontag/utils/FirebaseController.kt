package com.polyglotprogramminginc.beacontag.utils

import android.content.ContextWrapper
import android.content.Intent
import android.util.Log
import com.google.firebase.database.*
import com.polyglotprogramminginc.beacontag.CommonConstants
import com.polyglotprogramminginc.beacontag.model.GameStatus
import com.polyglotprogramminginc.beacontag.model.MainActivityData
import java.util.*

/**
 *
 * Created by Lance Gleason of Polyglot Programming LLC. on 11/24/16.
 * http://www.polyglotprogramminginc.com
 * https://github.com/lgleasain
 * Twitter: @lgleasain
 *
 */

class FirebaseController constructor(val activity: ContextWrapper,
                                     var gameStatus: GameStatus?) {

    var database: FirebaseDatabase
    var users: DatabaseReference
    var gameStatusReference: DatabaseReference
    val commonConstants: CommonConstants = CommonConstants()
    val settingsManager: SettingsManager = SettingsManager(activity)
    val currentUsers: HashMap<String, MainActivityData> = hashMapOf<String, MainActivityData>()

    init {
        database = FirebaseDatabase.getInstance()
        users = database!!.getReference(commonConstants.FIREBASE_USERS_REFERENCE)
        gameStatusReference = database!!.getReference(commonConstants.FIREBASE_GAME_STATUS_REFERENCE)
        users.addValueEventListener(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot?) {
                currentUsers.clear()
                for (child in snapshot!!.children) {
                    val castChild = child.getValue(MainActivityData::class.java)
                    currentUsers[castChild.beaconId] = castChild
                    Log.i("FirebaseCntl Child", child.toString())
                }
            }

            override fun onCancelled(databaseError: DatabaseError?) {
            }
        }

        )
    }

    fun updateUser(mainActivityData: MainActivityData) {
        Log.i("FirebaseController", "updating user")
        Log.i("email", mainActivityData.email)
        val firebaseUserRecord = users.child(mainActivityData.email)
        firebaseUserRecord.setValue(mainActivityData)
        settingsManager.setMainActivityDataPreferences(mainActivityData)
        val intent: Intent = Intent(commonConstants.SERVICE_NOTIFICATION)
        intent.putExtra("MainActivityData", mainActivityData.toString())
    }

    fun startStop() {
        Log.i("game status", gameStatus.toString())
        if (gameStatus!!.status.equals(commonConstants.GAME_STARTED)) {
            gameStatus!!.status = commonConstants.GAME_STOPPED
            gameStatus!!.itPerson = ""
            for (user in currentUsers) {
                val userData = user.value
                userData.isIt = false
                updateUser(userData)
            }
        } else {
            gameStatus!!.status = commonConstants.GAME_STARTED
            val r: Random = Random()
            val it: Int = r.nextInt(currentUsers.size)
            Log.i("it", currentUsers.toList().toTypedArray()[it].toString())
            val itUser: MainActivityData = currentUsers.toList().toTypedArray()[it].second
            gameStatus!!.itPerson = itUser.beaconId
            itUser.isIt = true
            updateUser(itUser)
        }
        Log.i("game status after", gameStatus.toString())
        gameStatusReference.setValue(gameStatus)
    }

    fun reset() {
        Log.i("FirebaseController", "resetting game")
        users.setValue(null)
        gameStatusReference.setValue(null)
    }
}

