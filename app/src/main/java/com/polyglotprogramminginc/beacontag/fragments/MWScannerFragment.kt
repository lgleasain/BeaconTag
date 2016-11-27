package com.polyglotprogramminginc.beacontag.fragments

import android.app.DialogFragment
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup

import com.polyglotprogramminginc.beacontag.R

/**
 * Created by Lance Gleason of Polyglot Programming LLC. on 11/26/16.
 * http://www.polyglotprogramminginc.com
 * https://github.com/lgleasain
 * Twitter: @lgleasain
 */

class MWScannerFragment : DialogFragment() {


    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        return inflater.inflate(R.layout.fragment_mwscanner, container, false)
    }

}

