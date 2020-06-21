package com.cleverchuk.smarttimer.ui

import android.R
import android.content.Context
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.lifecycle.observe
import com.cleverchuk.smarttimer.data.StateConverter
import com.cleverchuk.smarttimer.data.TimerDatabase
import com.cleverchuk.smarttimer.data.TimerState
import com.cleverchuk.smarttimer.databinding.CombinedViewBinding
import com.google.android.material.floatingactionbutton.FloatingActionButton
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors

class CombinedFragment : Fragment() {
    private lateinit var binding: CombinedViewBinding
    private val timeFragment: TimeFragment = TimeFragment()
    private val resetTimeFragment = TimeFragment()
    private var currentScreen: Int = 0
    private lateinit var timerDatabase: TimerDatabase
    private val executor: ExecutorService = Executors.newSingleThreadExecutor()
    private lateinit var timer: Timer

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

        timer = Timer(executor)
        timer.timeFragment
                .observe(viewLifecycleOwner) { binding.countDownDest.countDown = it }
        timer.state
                .observe(viewLifecycleOwner) {
                    if ((it == Timer.State.DONE) and timeFragment.repeat) {
                        binding.root.postDelayed({ timer.start(timeFragment) }, timeFragment.delay * 1000L)
                        CoroutineScope(Dispatchers.IO).launch {
                            timerDatabase.timerStateDao().delete()
                        }
                    }
                }

        binding.countDownDest.pausePlay.setOnClickListener {
            if (timer.isPaused()) {
                timer.resume()
                (it as FloatingActionButton).setImageResource(android.R.drawable.ic_media_pause)
            }
            if (timer.isCounting()) {
                timer.pause()
                (it as FloatingActionButton).setImageResource(android.R.drawable.ic_media_play)
            }
        }

        binding.countDownDest.cancel.setOnClickListener {
            currentScreen = 0
            binding.viewSwitcher.displayedChild = 0
            binding.countDownDest.countDown = resetTimeFragment
            timer.stop()
            CoroutineScope(Dispatchers.IO).launch {
                timerDatabase.timerStateDao().delete()
            }
        }

        binding.clockDest.loop.setOnClickListener {
            binding.countDownDest.countDown = timeFragment
            currentScreen = 1
            binding.viewSwitcher.displayedChild = currentScreen
            timer.start(timeFragment)
        }
        binding.viewSwitcher.displayedChild = currentScreen

        // Restore state
        timerDatabase.timerStateDao()
                .observableFindById(1)
                .observe(viewLifecycleOwner) {
                    if ((it?.currentScreen == 1)) {
                        timer.time = it.time

                        when (StateConverter.intToState(it.state)) {
                            Timer.State.COUNTING -> timer.start(it.time)
                            Timer.State.PAUSED -> {
                                binding.countDownDest.pausePlay.setImageResource(R.drawable.ic_media_play)
                                timer.state.value = Timer.State.PAUSED
                                binding.countDownDest.countDown = TimeFragment(
                                        timer.hr,
                                        timer.min,
                                        timer.sec
                                )
                            }

                            Timer.State.IDLE -> timer.state.value = Timer.State.IDLE
                            Timer.State.STARTED -> timer.state.value = Timer.State.STARTED

                            Timer.State.STOPPED -> timer.state.value = Timer.State.STOPPED
                            Timer.State.DONE -> {
                                timeFragment.hr = timer.extractHour(it.fullTime)
                                timeFragment.min = timer.extractMinute(it.fullTime)

                                timeFragment.sec = timer.extractSecond(it.fullTime)
                                timeFragment.delay = it.delay
                                timer.state.value = Timer.State.DONE
                            }
                        }
                    }

                    currentScreen = it?.currentScreen ?: 0
                    binding.viewSwitcher.displayedChild = currentScreen
                }

        return binding.root
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
        super.onPause()
    }
}