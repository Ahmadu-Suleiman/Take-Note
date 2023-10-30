package com.meta4projects.takenote.fragments

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.fragment.app.Fragment
import androidx.viewpager2.widget.ViewPager2
import com.meta4projects.takenote.R

class SecondTutorialFragment : Fragment() {
    private lateinit var textViewNext: TextView
    private lateinit var textViewBack: TextView
    private lateinit var viewPager: ViewPager2
    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        val view = inflater.inflate(R.layout.fragment_second_tutorial, container, false)
        textViewNext = view.findViewById(R.id.textViewNextSecond)
        textViewBack = view.findViewById(R.id.textViewBackSecond)
        viewPager = requireActivity().findViewById(R.id.view_pager)
        textViewNext.setOnClickListener { viewPager.currentItem = 2 }
        textViewBack.setOnClickListener { viewPager.currentItem = 0 }
        return view
    }
}