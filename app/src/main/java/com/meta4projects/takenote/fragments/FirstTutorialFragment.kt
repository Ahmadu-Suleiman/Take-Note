package com.meta4projects.takenote.fragments

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.fragment.app.Fragment
import androidx.viewpager2.widget.ViewPager2
import com.meta4projects.takenote.R

class FirstTutorialFragment : Fragment() {
    private lateinit var textViewNext: TextView
    private lateinit var viewPager: ViewPager2
    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        val view = inflater.inflate(R.layout.fragment_first_tutorial, container, false)
        textViewNext = view.findViewById(R.id.textViewNextFirst)
        viewPager = requireActivity().findViewById(R.id.view_pager)
        textViewNext.setOnClickListener { viewPager.currentItem = 1 }
        return view
    }
}