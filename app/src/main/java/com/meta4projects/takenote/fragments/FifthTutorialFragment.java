package com.meta4projects.takenote.fragments;

import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.fragment.app.Fragment;
import androidx.viewpager2.widget.ViewPager2;

import com.meta4projects.takenote.R;
import com.meta4projects.takenote.activities.MainActivity;

import java.util.Objects;

public class FifthTutorialFragment extends Fragment {


    TextView textViewDone, textViewBack;
    ViewPager2 viewPager;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_fifth_tutorial, container, false);


        textViewDone = view.findViewById(R.id.textViewDoneFifth);
        textViewBack = view.findViewById(R.id.textViewBackFifth);
        viewPager = Objects.requireNonNull(getActivity()).findViewById(R.id.view_pager);

        textViewDone.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                startActivity(new Intent(getActivity(), MainActivity.class));
                Objects.requireNonNull(getActivity()).finish();
            }
        });

        textViewBack.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                viewPager.setCurrentItem(3);
            }
        });

        return view;
    }
}
