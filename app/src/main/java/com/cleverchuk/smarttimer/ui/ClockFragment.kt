package com.cleverchuk.smarttimer.ui

import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.navigation.fragment.findNavController
import com.cleverchuk.smarttimer.R
import com.cleverchuk.smarttimer.databinding.FragmentClockBinding


class ClockFragment : Fragment() {
    lateinit var binding: FragmentClockBinding
    val timeFragment: TimeFragment = TimeFragment()

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?,
                              savedInstanceState: Bundle?): View? {
        // Inflate the layout for this fragment
        binding = FragmentClockBinding.inflate(inflater)

        binding.hourPicker.maxValue = 23
        binding.hourPicker.minValue = 0
        binding.minutePicker.maxValue = 59

        binding.minutePicker.minValue = 0
        binding.secondPicker.maxValue = 59
        binding.secondPicker.minValue = 0

        binding.repeatDelayPicker.minValue = 0
        binding.repeatDelayPicker.maxValue = 59

        binding.hourPicker.displayedValues = (0..23).map { it.toString() }.toList().toTypedArray()
        binding.minutePicker.displayedValues = (0..59).map { it.toString() }.toList().toTypedArray()
        binding.secondPicker.displayedValues = (0..59).map { it.toString() }.toList().toTypedArray()
        binding.repeatDelayPicker.displayedValues = (0..59).map { it.toString() }.toList().toTypedArray()

        binding.repeat.setOnCheckedChangeListener { buttonView, isChecked -> timeFragment.repeat = isChecked }
        binding.loop.setOnClickListener {
            findNavController().navigate(ClockFragmentDirections.actionClockFragmentToCountDownFragment(timeFragment))
        }

        binding.repeatDelayPicker.setOnValueChangedListener { picker, oldVal, newVal -> timeFragment.delay = newVal }
        binding.hourPicker.setOnValueChangedListener { picker, oldVal, newVal -> timeFragment.hr = newVal }
        binding.secondPicker.setOnValueChangedListener { picker, oldVal, newVal -> timeFragment.sec = newVal }
        binding.minutePicker.setOnValueChangedListener { picker, oldVal, newVal -> timeFragment.min = newVal }


        binding.repeatDelayPicker.value = timeFragment.delay
        binding.hourPicker.value = timeFragment.hr
        binding.minutePicker.value = timeFragment.min
        binding.secondPicker.value = timeFragment.sec

        return binding.root
    }
}