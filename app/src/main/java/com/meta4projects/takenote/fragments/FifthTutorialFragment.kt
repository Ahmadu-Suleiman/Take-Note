package com.meta4projects.takenote.fragments

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.fragment.app.Fragment
import androidx.viewpager2.widget.ViewPager2
import com.meta4projects.takenote.R
import com.meta4projects.takenote.activities.MainActivity

class FifthTutorialFragment : Fragment() {
    private lateinit var textViewDone: TextView
    private lateinit var textViewBack: TextView
    private lateinit var viewPager: ViewPager2
    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        val view = inflater.inflate(R.layout.fragment_fifth_tutorial, container, false)
        textViewDone = view.findViewById(R.id.textViewDoneFifth)
        textViewBack = view.findViewById(R.id.textViewBackFifth)
        viewPager = requireActivity().findViewById(R.id.view_pager)
        textViewDone.setOnClickListener {
            startActivity(Intent(activity, MainActivity::class.java))
            requireActivity().finish()
        }
        textViewBack.setOnClickListener { viewPager.currentItem = 3 }
        return view
    }
}