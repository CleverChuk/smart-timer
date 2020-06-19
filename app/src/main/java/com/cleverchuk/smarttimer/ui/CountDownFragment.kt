package com.cleverchuk.smarttimer.ui

import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.lifecycle.observe
import androidx.navigation.fragment.findNavController
import androidx.navigation.fragment.navArgs
import com.cleverchuk.smarttimer.R
import com.cleverchuk.smarttimer.databinding.FragmentCountDownBinding
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.supervisorScope
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

        val timer = Timer(args.timeFragments.hr, args.timeFragments.min, args.timeFragments.sec, executor)
        timer.timeFragment.observe(viewLifecycleOwner) { binding.countDown = it }
        timer.state.observe(viewLifecycleOwner) {
            if ((it == Timer.State.STOPPED) and args.timeFragments.repeat)
                binding.root.postDelayed({ timer.countDown(timeFragment = args.timeFragments) }, args.timeFragments.delay * 1000L)
        }

        binding.linearLayout.postDelayed({ timer.countDown() }, 1000)
        binding.cancel.setOnClickListener { findNavController().navigate(R.id.clock_dest) }
        return binding.root
    }

    override fun onDestroy() {
        super.onDestroy()
        executor.shutdown()
    }
}