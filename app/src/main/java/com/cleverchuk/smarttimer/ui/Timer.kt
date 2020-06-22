package com.cleverchuk.smarttimer.ui

import android.os.Parcel
import android.os.Parcelable
import androidx.annotation.AnyThread
import androidx.annotation.UiThread
import androidx.annotation.WorkerThread
import androidx.lifecycle.MutableLiveData
import kotlinx.coroutines.*

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

class Timer() {
    val timeFragment: MutableLiveData<TimeFragment> = MutableLiveData()
    var state = MutableLiveData<State>(State.IDLE)
    var time: Int = 0

    private var job: Job? = null
    var hr: Int = 0
        get() = extractHour(time)
        private set
    var min: Int = 0
        get() = extractMinute(time)
        private set
    var sec: Int = 0
        get() = extractSecond(time)
        private set

    @AnyThread
    private fun start() {
        job = CoroutineScope(Dispatchers.Default).launch {
            state.postValue(State.COUNTING)
            while (time > 0) {
                delay(1000)
                time--
                if (extractMinute(time) > 59 && extractSecond(time) > 59)
                    time = combineTime(extractHour(time), 59, 59)
                else if (extractSecond(time) > 59)
                    time = combineTime(extractHour(time), extractMinute(time), 59)
                else if (extractMinute(time) > 0 && extractSecond(time) == 0)
                    time = combineTime(extractHour(time), extractMinute(time) - 1, 59)

                timeFragment.postValue(TimeFragment(extractHour(time), extractMinute(time), extractSecond(time)))
            }
            state.postValue(State.DONE)
        }
    }

    @UiThread
    fun start(timeFragment: TimeFragment) {
        state.value = State.STARTED
        time = combineTime(timeFragment.hr, timeFragment.min, timeFragment.sec)
        start()
    }

    @WorkerThread
    fun start(time: Int) {
        state.postValue(State.STARTED)
        this.time = time
        start()
    }

    @UiThread
    fun resume() {
        state.value = State.STARTED
        start()
    }

    @UiThread
    fun pause() {
        state.value = State.PAUSED
        job?.cancel()
    }

    @UiThread
    fun cancel() {
        pause()
        state.value = State.CANCELLED
    }

    fun isCancelled() = state.value == State.CANCELLED

    fun isDone() = state.value == State.DONE

    @AnyThread
    fun isPaused() = state.value == State.PAUSED

    @AnyThread
    fun isCounting() = state.value == State.COUNTING

    @AnyThread
    fun extractHour(time: Int) = (time and LAST_FOUR_BIT_MASK) shr 12

    @AnyThread
    fun extractMinute(time: Int) = (time and SECOND_SIX_MASK) shr 6

    @AnyThread
    fun extractSecond(time: Int) = (time and FIRST_SIX_BIT_MASK)

    @AnyThread
    fun combineTime(hr: Int, min: Int, sec: Int) = (hr shl 12) or (min shl 6) or sec

    fun isIdle() = state.value == State.IDLE

    enum class State {
        IDLE,
        STARTED,
        COUNTING,
        CANCELLED,
        PAUSED,
        DONE
    }

    companion object {
        const val FIRST_SIX_BIT_MASK: Int = 63
        const val SECOND_SIX_MASK: Int = 4032
        const val LAST_FOUR_BIT_MASK = 126976
    }
}