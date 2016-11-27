package com.polyglotprogramminginc.beacontag.model

import android.databinding.BaseObservable
import android.databinding.Bindable
import android.support.v4.content.ContextCompat
import com.polyglotprogramminginc.beacontag.BR

/**
 *
 * Created by Lance Gleason of Polyglot Programming LLC. on 11/25/16.
 * http://www.polyglotprogramminginc.com
 * https://github.com/lgleasain
 * Twitter: @lgleasain
 *
 */
class MainActivityStatusData constructor(private var startStopButtonText : String = "Start",
                                         private var mainStatusText: String = "Not Playing",
                                         private var adapter: String = "No Adapter"): BaseObservable(){

    @Bindable
    fun getStartStopButtonText() : String{
        return startStopButtonText
    }

    fun setStartStopButtonText(startStopButtonText: String){
        this.startStopButtonText = startStopButtonText
        notifyPropertyChanged(BR.startStopButtonText)
    }

    @Bindable
    fun getMainStatusText(): String{
        return mainStatusText
    }

    fun setMainStatusText(mainStatusText: String){
        this.mainStatusText = mainStatusText
        notifyPropertyChanged(BR.mainStatusText)
    }

    @Bindable
    fun getAdapter(): String {
        return adapter
    }

    fun setAdapter(adapter: String){
        this.adapter = adapter
        notifyPropertyChanged(BR.adapter)
    }
}
