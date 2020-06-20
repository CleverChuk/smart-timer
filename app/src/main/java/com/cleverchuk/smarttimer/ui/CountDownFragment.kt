package com.cleverchuk.smarttimer.ui

import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.lifecycle.observe
import androidx.navigation.fragment.findNavController
import com.cleverchuk.smarttimer.R
import com.cleverchuk.smarttimer.databinding.FragmentCountDownBinding
import com.google.android.material.floatingactionbutton.FloatingActionButton
import java.util.concurrent.Executors


class CountDownFragment : Fragment() {
    val executor = Executors.newSingleThreadExecutor()
    lateinit var binding: FragmentCountDownBinding

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?,
                              savedInstanceState: Bundle?): View? {
        // Inflate the layout for this fragment
        binding = FragmentCountDownBinding.inflate(inflater)
        val args = CountDownFragmentArgs.fromBundle(requireArguments())
        binding.countDown = args.timeFragments

        val timer = Timer(executor)
        timer.timeFragment.observe(viewLifecycleOwner) { binding.countDown = it }
        timer.state.observe(viewLifecycleOwner) {
            if ((it == Timer.State.STOPPED) and args.timeFragments.repeat)
                binding.root.postDelayed({ timer.start(timeFragment = args.timeFragments) }, args.timeFragments.delay * 1000L)
        }

        binding.linearLayout.postDelayed({ timer.start(args.timeFragments) }, 1000)
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
        binding.cancel.setOnClickListener { findNavController().navigate(R.id.clock_dest) }
        return binding.root
    }

    override fun onDestroy() {
        super.onDestroy()
        executor.shutdown()
    }
}