package com.meta4projects.takenote.activities;

import android.os.Bundle;

import androidx.annotation.Nullable;
import androidx.fragment.app.FragmentActivity;
import androidx.viewpager2.widget.ViewPager2;

import com.meta4projects.takenote.R;
import com.meta4projects.takenote.adapters.SliderAdapter;

public class TutorialActivity extends FragmentActivity {

    ViewPager2 viewPager;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_tutorial);

        viewPager = findViewById(R.id.view_pager);

        SliderAdapter sliderAdapter = new SliderAdapter(this);
        viewPager.setAdapter(sliderAdapter);
    }

    @Override
    public void onBackPressed() {
        if (viewPager.getCurrentItem() == 0) {
            super.onBackPressed();
        } else {
            viewPager.setCurrentItem(viewPager.getCurrentItem() - 1);
        }
    }
}
