package com.polyglotprogramminginc.beacontag.model

import java.io.Serializable

/**
 *
 * Created by Lance Gleason of Polyglot Programming LLC. on 11/24/16.
 * http://www.polyglotprogramminginc.com
 * https://github.com/lgleasain
 * Twitter: @lgleasain
 *
 */
data class MainActivityData(var email: String = "", var beaconId: String = "",
                            var isIt: Boolean = false) : Serializable