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

public class FifthTutorialFragment extends Fragment {

    TextView textViewDone, textViewBack;
    ViewPager2 viewPager;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_fifth_tutorial, container, false);
        textViewDone = view.findViewById(R.id.textViewDoneFifth);
        textViewBack = view.findViewById(R.id.textViewBackFifth);
        viewPager = requireActivity().findViewById(R.id.view_pager);

        textViewDone.setOnClickListener(v -> {
            startActivity(new Intent(getActivity(), MainActivity.class));
            requireActivity().finish();
        });

        textViewBack.setOnClickListener(v -> viewPager.setCurrentItem(3));
        return view;
    }
}
