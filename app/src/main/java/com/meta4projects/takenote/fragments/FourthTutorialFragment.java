package com.meta4projects.takenote.fragments;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.fragment.app.Fragment;
import androidx.viewpager2.widget.ViewPager2;

import com.meta4projects.takenote.R;

public class FourthTutorialFragment extends Fragment {

    TextView textViewNext, textViewBack;
    ViewPager2 viewPager;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_fourth_tutorial, container, false);
        textViewNext = view.findViewById(R.id.textViewNextFourth);
        textViewBack = view.findViewById(R.id.textViewBackFourth);
        viewPager = requireActivity().findViewById(R.id.view_pager);

        textViewNext.setOnClickListener(v -> viewPager.setCurrentItem(4));
        textViewBack.setOnClickListener(v -> viewPager.setCurrentItem(2));
        return view;
    }
}