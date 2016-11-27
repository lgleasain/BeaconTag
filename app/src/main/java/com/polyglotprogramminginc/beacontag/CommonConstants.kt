package com.polyglotprogramminginc.beacontag

/**
 *
 * Created by Lance Gleason of Polyglot Programming LLC. on 11/25/16.
 * http://www.polyglotprogramminginc.com
 * https://github.com/lgleasain
 * Twitter: @lgleasain
 *
 */
data class CommonConstants(
        var SERVICE_NOTIFICATION: String = "com.polyglotprogramminginc.beacontag.GAME_SERVICE_NOTIFICATION",
        var FIREBASE_USERS_REFERENCE: String = "users",
        var FIREBASE_GAME_STATUS_REFERENCE: String = "game_status",
        var GAME_STARTED: String = "started",
        var GAME_STOPPED: String = "stopped",
        var PREFERENCES_NAME: String = "com.polyglotprogramminginc.beacontag",
        var NO_METAWEAR: String = "No MetaWear",
        var MAIN_ACTIVITY_DATA: String = "MainActivityData",
        var SELECT_METAWEAR: String = "SelectMetaWear",
        var COMMAND: String = "Command",
        var MAC_ADDRESS: String = "MAC_ADDRESS",
        var CONNECT: String = "Connect",
        var DISCONNECT: String = "Disconnect",
        var BLE_DEVICE: String = "BLE_DEVICE")
