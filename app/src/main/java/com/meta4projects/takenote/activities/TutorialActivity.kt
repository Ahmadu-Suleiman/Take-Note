package com.meta4projects.takenote.activities

import android.content.Intent
import android.os.Bundle
import androidx.activity.OnBackPressedCallback
import androidx.viewpager2.widget.ViewPager2
import com.meta4projects.takenote.R
import com.meta4projects.takenote.adapters.SliderAdapter

class TutorialActivity : FullscreenActivity() {

    private lateinit var onBackPressedCallback: OnBackPressedCallback
    private lateinit var viewPager: ViewPager2
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_tutorial)
        viewPager = findViewById(R.id.view_pager)
        viewPager.adapter = SliderAdapter(this)

        onBackPressedCallback = object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() {
                if (viewPager.currentItem == 0) {
                    startActivity(Intent(this@TutorialActivity, MainActivity::class.java))
                    finish()
                } else viewPager.currentItem = viewPager.currentItem - 1
            }
        }
        onBackPressedDispatcher.addCallback(this@TutorialActivity, onBackPressedCallback)
    }
}