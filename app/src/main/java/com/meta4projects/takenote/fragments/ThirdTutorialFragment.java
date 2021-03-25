package com.meta4projects.takenote.fragments;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.fragment.app.Fragment;
import androidx.viewpager2.widget.ViewPager2;

import com.meta4projects.takenote.R;

import java.util.Objects;

public class ThirdTutorialFragment extends Fragment {

    TextView textViewNext, textViewBack;
    ViewPager2 viewPager;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_third_tutorial, container, false);

        textViewNext = view.findViewById(R.id.textViewNextThird);
        textViewBack = view.findViewById(R.id.textViewBackThird);
        viewPager = Objects.requireNonNull(getActivity()).findViewById(R.id.view_pager);

        textViewNext.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                viewPager.setCurrentItem(3);
            }
        });

        textViewBack.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                viewPager.setCurrentItem(1);
            }
        });

        return view;
    }
}
