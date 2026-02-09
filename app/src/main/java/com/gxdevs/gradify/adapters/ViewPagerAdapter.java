package com.gxdevs.gradify.adapters;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentActivity;
import androidx.viewpager2.adapter.FragmentStateAdapter;

import com.gxdevs.gradify.fragment.CgpaCalculationFragment;
import com.gxdevs.gradify.fragment.GradeCalculationFragment;

public class ViewPagerAdapter extends FragmentStateAdapter {

    public ViewPagerAdapter(@NonNull FragmentActivity fragmentActivity) {
        super(fragmentActivity);
    }

    @NonNull
    @Override
    public Fragment createFragment(int position) {
        switch (position) {
            case 0:
                return new GradeCalculationFragment();
            case 1:
                return new CgpaCalculationFragment();
            default:
                return new GradeCalculationFragment(); // Default case
        }
    }

    @Override
    public int getItemCount() {
        return 2; // We have two tabs
    }
} 