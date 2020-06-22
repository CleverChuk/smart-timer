package com.cleverchuk.smarttimer.ui

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.lifecycle.observe
import androidx.navigation.fragment.findNavController
import com.cleverchuk.smarttimer.R
import com.cleverchuk.smarttimer.databinding.FragmentCountDownBinding
import com.google.android.material.floatingactionbutton.FloatingActionButton


class CountDownFragment : Fragment() {
    lateinit var binding: FragmentCountDownBinding

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?,
                              savedInstanceState: Bundle?): View? {
        // Inflate the layout for this fragment
        binding = FragmentCountDownBinding.inflate(inflater)
        val args = CountDownFragmentArgs.fromBundle(requireArguments())
        binding.countDown = args.timeFragments

        val timer = Timer()
        timer.timeFragment.observe(viewLifecycleOwner) { binding.countDown = it }
        timer.state.observe(viewLifecycleOwner) {
            if ((it == Timer.State.DONE) and args.timeFragments.repeat)
                binding.root.postDelayed({ timer.start(timeFragment = args.timeFragments) }, args.timeFragments.delay * 1000L)
        }

        binding.pausePlay.setOnClickListener {
            if (timer.isPaused()){
                timer.resume()
                (it as FloatingActionButton).setImageResource(android.R.drawable.ic_media_pause)
            }

            if (timer.isCounting()){
                timer.pause()
                (it as FloatingActionButton).setImageResource(android.R.drawable.ic_media_play)
            }
        }

        binding.cancel.setOnClickListener {
            findNavController().navigate(R.id.clock_dest)
            timer.cancel()
        }
        timer.start(args.timeFragments)
        return binding.root
    }
}