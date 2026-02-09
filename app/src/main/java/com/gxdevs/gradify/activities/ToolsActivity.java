package com.gxdevs.gradify.activities;

import android.graphics.Color;
import android.os.Bundle;
import android.widget.ImageView;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;
import androidx.viewpager2.widget.ViewPager2;

import com.gxdevs.gradify.R;
import com.gxdevs.gradify.Utils.Utils;
import com.gxdevs.gradify.adapters.ViewPagerAdapter;
import com.google.android.material.tabs.TabLayout;
import com.google.android.material.tabs.TabLayoutMediator;

public class ToolsActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_tools);

        Utils.setPad(findViewById(R.id.toolsContainer), "bottom", this);

        TabLayout tabLayout = findViewById(R.id.tab_layout);
        ViewPager2 viewPager = findViewById(R.id.view_pager);
        ViewPagerAdapter viewPagerAdapter = new ViewPagerAdapter(this);
        ImageView decor = findViewById(R.id.toolDecor);
        ImageView decor1 = findViewById(R.id.toolDecor1);
        ImageView decor2 = findViewById(R.id.toolDecor2);
        ImageView decor3 = findViewById(R.id.toolDecor3);
        viewPager.setAdapter(viewPagerAdapter);
        tabLayout.setTabTextColors(Color.WHITE, ContextCompat.getColor(this, R.color.glowColor));
        tabLayout.setSelectedTabIndicatorColor(ContextCompat.getColor(this, R.color.glowColor));

        Utils.setTheme(this, decor, decor1, decor2, decor3);

        findViewById(R.id.backBtnT).setOnClickListener(v -> onBackPressed());

        new TabLayoutMediator(tabLayout, viewPager, (tab, position) -> {
            switch (position) {
                case 0:
                    tab.setText("Grade Calculation");
                    break;
                case 1:
                    tab.setText("CGPA Calculation");
                    break;
            }
        }).attach();
    }
} 