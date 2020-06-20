package com.cleverchuk.smarttimer.ui

import android.os.Parcel
import android.os.Parcelable
import androidx.lifecycle.MutableLiveData
import java.util.concurrent.ExecutorService
import java.util.concurrent.Future

data class TimeFragment(var hr: Int = 0, var min: Int = 58, var sec: Int = 30, var delay: Int = 10, var repeat: Boolean = true) : Parcelable {
    constructor(parcel: Parcel) : this(
            parcel.readInt(),
            parcel.readInt(),
            parcel.readInt(),
            parcel.readInt(),
            parcel.readByte() != 0.toByte()) {
    }

    override fun writeToParcel(parcel: Parcel, flags: Int) {
        parcel.writeInt(hr)
        parcel.writeInt(min)
        parcel.writeInt(sec)
        parcel.writeInt(delay)
        parcel.writeByte(if (repeat) 1 else 0)
    }

    override fun describeContents(): Int {
        return 0
    }

    companion object CREATOR : Parcelable.Creator<TimeFragment> {
        override fun createFromParcel(parcel: Parcel): TimeFragment {
            return TimeFragment(parcel)
        }

        override fun newArray(size: Int): Array<TimeFragment?> {
            return arrayOfNulls(size)
        }
    }

}

class Timer(private val executor: ExecutorService) {
    val FIRST_SIX_BIT_MASK: Int = 63
    val SECOND_SIX_MASK: Int = 4032
    val LAST_FOUR_BIT_MASK = 61440

    val timeFragment: MutableLiveData<TimeFragment> = MutableLiveData()
    var state = MutableLiveData<State>(State.IDLE)
    private lateinit var task: Future<*>

    private var hr: Int = 0
    private var min: Int = 0
    private var sec: Int = 0
    private var time: Int = 0

    private fun start() {
        var time = combineTime(hr, min, sec)
        task = executor.submit {
            state.postValue(State.COUNTING)
            while (time > 0) {
                time--
                if (extractMinute(time) > 59 && extractSecond(time) > 59)
                    time = combineTime(extractHour(time), 59, 59)
                else if (extractSecond(time) > 59)
                    time = combineTime(extractHour(time), extractMinute(time), 59)
                else if (extractMinute(time) > 0 && extractSecond(time) == 0)
                    time = combineTime(extractHour(time), extractMinute(time) - 1, 59)

                this.time = time
                timeFragment.postValue(TimeFragment(extractHour(time), extractMinute(time), extractSecond(time)))
                Thread.sleep(1000)
            }
            state.postValue(State.STOPPED)
        }
    }

    fun start(timeFragment: TimeFragment) {
        state.value = State.STARTED
        hr = timeFragment.hr
        min = timeFragment.min
        sec = timeFragment.sec
        start()
    }

    fun resume() {
        state.value = State.STARTED
        hr = extractHour(time)
        min = extractMinute(time)
        sec = extractSecond(time)
        start()
    }

    fun pause() {
        state.value = State.PAUSED
        val cancelled = task.cancel(true)
    }

    fun isPaused() = state.value == State.PAUSED

    fun isCounting() = state.value == State.COUNTING

    fun extractHour(time: Int) = (time and LAST_FOUR_BIT_MASK) shr 12

    fun extractMinute(time: Int) = (time and SECOND_SIX_MASK) shr 6

    fun extractSecond(time: Int) = (time and FIRST_SIX_BIT_MASK)

    fun combineTime(hr: Int, min: Int, sec: Int) = (hr shl 12) or (min shl 6) or sec

    enum class State {
        IDLE,
        STARTED,
        COUNTING,
        STOPPED,
        PAUSED
    }
}