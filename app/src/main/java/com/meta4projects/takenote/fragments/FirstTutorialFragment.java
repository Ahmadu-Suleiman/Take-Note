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

public class FirstTutorialFragment extends Fragment {

    TextView textViewNext;
    ViewPager2 viewPager;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_first_tutorial, container, false);

        textViewNext = view.findViewById(R.id.textViewNextFirst);
        viewPager = Objects.requireNonNull(getActivity()).findViewById(R.id.view_pager);

        textViewNext.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                viewPager.setCurrentItem(1);
            }
        });

        return view;
    }
}
