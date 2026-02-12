package com.gxdevs.gradify.activities;

import android.os.Bundle;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;
import androidx.viewpager2.widget.ViewPager2;

import com.gxdevs.gradify.R;
import com.gxdevs.gradify.Utils.Utils;
import com.gxdevs.gradify.adapters.ViewPagerAdapter;

public class ToolsActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_tools);

        Utils.setPad(findViewById(R.id.toolsContainer), "bottom", this);

        TextView tabGrade = findViewById(R.id.tab_grade);
        TextView tabCgpa = findViewById(R.id.tab_cgpa);
        ViewPager2 viewPager = findViewById(R.id.view_pager);

        ViewPagerAdapter viewPagerAdapter = new ViewPagerAdapter(this);
        viewPager.setAdapter(viewPagerAdapter);

        findViewById(R.id.backBtnT).setOnClickListener(v -> getOnBackPressedDispatcher().onBackPressed());

        tabGrade.setOnClickListener(v -> viewPager.setCurrentItem(0));
        tabCgpa.setOnClickListener(v -> viewPager.setCurrentItem(1));

        viewPager.registerOnPageChangeCallback(new ViewPager2.OnPageChangeCallback() {
            @Override
            public void onPageSelected(int position) {
                super.onPageSelected(position);
                updateTabs(position, tabGrade, tabCgpa);
            }
        });
    }

    private void updateTabs(int position, TextView grade, TextView cgpa) {
        android.view.View indicator = findViewById(R.id.tab_indicator_tools);
        android.view.ViewGroup container = findViewById(R.id.tab_selector_container);

        if (position == 0) {
            Utils.updateTabIndicator(indicator, grade, container);
            grade.setTextColor(ContextCompat.getColor(this, R.color.white));
            cgpa.setTextColor(ContextCompat.getColor(this, R.color.unselectedLevelText));
        } else {
            Utils.updateTabIndicator(indicator, cgpa, container);
            cgpa.setTextColor(ContextCompat.getColor(this, R.color.white));
            grade.setTextColor(ContextCompat.getColor(this, R.color.unselectedLevelText));
        }
    }
}