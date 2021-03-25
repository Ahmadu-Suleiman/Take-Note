package com.meta4projects.takenote.adapters;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentActivity;
import androidx.viewpager2.adapter.FragmentStateAdapter;

import com.meta4projects.takenote.fragments.FifthTutorialFragment;
import com.meta4projects.takenote.fragments.FirstTutorialFragment;
import com.meta4projects.takenote.fragments.FourthTutorialFragment;
import com.meta4projects.takenote.fragments.SecondTutorialFragment;
import com.meta4projects.takenote.fragments.ThirdTutorialFragment;

public class SliderAdapter extends FragmentStateAdapter {

    public SliderAdapter(@NonNull FragmentActivity fragmentActivity) {
        super(fragmentActivity);
    }

    @NonNull
    @Override
    public Fragment createFragment(int position) {
        switch (position) {
            case 0:
                return new FirstTutorialFragment();
            case 1:
                return new SecondTutorialFragment();
            case 2:
                return new ThirdTutorialFragment();
            case 3:
                return new FourthTutorialFragment();
            case 4:
                return new FifthTutorialFragment();
        }

        return new FirstTutorialFragment();
    }

    @Override
    public int getItemCount() {
        return 5;
    }
}
