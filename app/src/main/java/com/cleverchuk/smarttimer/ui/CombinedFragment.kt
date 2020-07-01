package com.cleverchuk.smarttimer.ui


import android.content.Context
import android.content.Intent
import android.media.MediaPlayer
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.lifecycle.observe
import com.cleverchuk.smarttimer.R
import com.cleverchuk.smarttimer.data.StateConverter
import com.cleverchuk.smarttimer.data.TimerDatabase
import com.cleverchuk.smarttimer.data.TimerState
import com.cleverchuk.smarttimer.databinding.CombinedViewBinding
import com.google.android.material.floatingactionbutton.FloatingActionButton
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class CombinedFragment : Fragment() {
    private lateinit var binding: CombinedViewBinding
    private val timeFragment: TimeFragment = TimeFragment()
    private val resetTimeFragment = TimeFragment()
    private var currentScreen: Int = 0
    private lateinit var timerDatabase: TimerDatabase
    private lateinit var timer: Timer
    private var mediaPlayer: MediaPlayer? = null

    override fun onAttach(context: Context) {
        super.onAttach(context)
        timerDatabase = TimerDatabase.getDatabase(context)
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?,
                              savedInstanceState: Bundle?): View? {
        // Inflate the layout for this fragment
        binding = CombinedViewBinding.inflate(inflater)

        binding.clockDest.hourPicker.maxValue = 23
        binding.clockDest.hourPicker.minValue = 0
        binding.clockDest.minutePicker.maxValue = 59

        binding.clockDest.minutePicker.minValue = 0
        binding.clockDest.secondPicker.maxValue = 59
        binding.clockDest.secondPicker.minValue = 0

        binding.clockDest.repeatDelayPicker.minValue = 0
        binding.clockDest.repeatDelayPicker.maxValue = 59

        binding.clockDest.hourPicker.displayedValues = (0..23).map { it.toString() }.toList().toTypedArray()
        binding.clockDest.minutePicker.displayedValues = (0..59).map { it.toString() }.toList().toTypedArray()
        binding.clockDest.secondPicker.displayedValues = (0..59).map { it.toString() }.toList().toTypedArray()

        binding.clockDest.repeatDelayPicker.displayedValues = (0..59).map { it.toString() }.toList().toTypedArray()
        binding.clockDest.repeat.setOnCheckedChangeListener { _, isChecked -> timeFragment.repeat = isChecked }
        binding.clockDest.repeatDelayPicker.setOnValueChangedListener { _, _, newVal -> timeFragment.delay = newVal }

        binding.clockDest.hourPicker.setOnValueChangedListener { _, _, newVal -> timeFragment.hr = newVal }
        binding.clockDest.secondPicker.setOnValueChangedListener { _, _, newVal -> timeFragment.sec = newVal }
        binding.clockDest.minutePicker.setOnValueChangedListener { _, _, newVal -> timeFragment.min = newVal }


        binding.clockDest.repeatDelayPicker.value = timeFragment.delay
        binding.clockDest.hourPicker.value = timeFragment.hr
        binding.clockDest.minutePicker.value = timeFragment.min
        binding.clockDest.secondPicker.value = timeFragment.sec

        timer = Timer()
        timer.timeFragment.observe(viewLifecycleOwner) { binding.countDownDest.countDown = it }
        timer.state.observe(viewLifecycleOwner, this::stateObserver)

        binding.countDownDest.pausePlay.setOnClickListener(this::togglePausePlay)
        binding.countDownDest.cancel.setOnClickListener(this::cancelCountDown)
        binding.clockDest.loop.setOnClickListener(this::loop)
        binding.viewSwitcher.displayedChild = currentScreen

        return binding.root
    }

    private fun updateState(timerState: TimerState?) {
        if ((timerState?.currentScreen == 1) && !timer.isCounting()) { // handling for when user leaves screen but coroutine wasn't cancelled
            timer.time = timerState.time
            timeFragment.hr = timer.extractHour(timerState.fullTime)
            timeFragment.min = timer.extractMinute(timerState.fullTime)
            timeFragment.sec = timer.extractSecond(timerState.fullTime)

            when (StateConverter.intToState(timerState.state)) {
                Timer.State.COUNTING -> timer.start(timerState.time)
                Timer.State.PAUSED -> {
                    binding.countDownDest.pausePlay.setImageResource(android.R.drawable.ic_media_play)
                    timer.state.value = Timer.State.PAUSED
                    binding.countDownDest.countDown = TimeFragment(
                            timer.hr,
                            timer.min,
                            timer.sec
                    )
                }

                Timer.State.IDLE -> timer.state.value = Timer.State.IDLE
                Timer.State.STARTED -> timer.state.value = Timer.State.STARTED
                Timer.State.CANCELLED -> timer.state.value = Timer.State.CANCELLED

                Timer.State.DONE -> {
                    timeFragment.hr = timer.extractHour(timerState.fullTime)
                    timeFragment.min = timer.extractMinute(timerState.fullTime)

                    timeFragment.sec = timer.extractSecond(timerState.fullTime)
                    timeFragment.delay = timerState.delay
                    timer.state.value = Timer.State.DONE
                }
            }
        }

        if (timerState != null) currentScreen = timerState.currentScreen
        binding.viewSwitcher.displayedChild = currentScreen
    }

    private fun togglePausePlay(view: View) {
        if (timer.isPaused()) {
            timer.resume()
            (view as FloatingActionButton).setImageResource(android.R.drawable.ic_media_pause)
        }
        if (timer.isCounting()) {
            timer.pause()
            (view as FloatingActionButton).setImageResource(android.R.drawable.ic_media_play)
        }
    }

    private fun cancelCountDown(view: View) {
        currentScreen = 0
        binding.viewSwitcher.displayedChild = 0
        binding.countDownDest.countDown = resetTimeFragment

        mediaPlayer?.stop()
        mediaPlayer?.prepare()
        timer.cancel()
        
        CoroutineScope(Dispatchers.IO).launch {
            timerDatabase.timerStateDao().delete()
        }
    }

    private fun loop(view: View) {
        timeFragment.delay = binding.clockDest.repeatDelayPicker.value
        timeFragment.hr = binding.clockDest.hourPicker.value
        timeFragment.min = binding.clockDest.minutePicker.value
        timeFragment.sec = binding.clockDest.secondPicker.value

        binding.countDownDest.countDown = timeFragment
        currentScreen = 1
        binding.viewSwitcher.displayedChild = currentScreen
        binding.countDownDest.pausePlay.setImageResource(android.R.drawable.ic_media_pause)
        timer.start(timeFragment)
    }

    private fun stateObserver(timerState: Timer.State?) {
        if ((timerState == Timer.State.DONE) && timeFragment.repeat) {
            binding.root.postDelayed(
                    {
                        mediaPlayer?.stop()
                        mediaPlayer?.prepareAsync()
                        if (timer.isDone())
                            timer.start(timeFragment)
                    },
                    timeFragment.delay * 1000L
            )

            mediaPlayer?.start()
            CoroutineScope(Dispatchers.IO).launch {
                timerDatabase.timerStateDao().delete() // TODO interim solution
            }
        }
    }

    override fun onResume() {
        // Restore state
        timerDatabase.timerStateDao()
                .observableFindById(1)
                .observe(viewLifecycleOwner, this::updateState)

        Intent(requireContext(), CountDownService::class.java)
                .also { intent ->
                    intent.action = Timer.State.CANCELLED.name
                    ContextCompat.startForegroundService(requireContext(), intent)
                }
        mediaPlayer = MediaPlayer.create(context, R.raw.faded_chords)
        mediaPlayer?.isLooping = true
        super.onResume()
    }

    override fun onPause() {
        // Save state
        CoroutineScope(Dispatchers.IO).launch {
            timerDatabase.timerStateDao().insert(TimerState(
                    1,
                    currentScreen,
                    timer.time,
                    timer.combineTime(timeFragment.hr, timeFragment.min, timeFragment.sec),
                    timeFragment.delay,
                    StateConverter.stateToInt(timer.state.value)
            ))
        }

        mediaPlayer?.release()
        mediaPlayer = null

        Intent(requireContext(), CountDownService::class.java)
                .also {
                    it.putExtra("fulltime", timeFragment)
                    it.putExtra("time", timer.time)
                    it.action = timer.state.value?.name
                    ContextCompat.startForegroundService(requireContext(), it)
                }
        super.onPause()
    }
}