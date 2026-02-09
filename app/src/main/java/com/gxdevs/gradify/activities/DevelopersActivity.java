package com.gxdevs.gradify.activities;

import android.content.Intent;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.os.Bundle;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.bumptech.glide.Glide;
import com.gxdevs.gradify.R;
import com.gxdevs.gradify.Utils.Utils;

import eightbitlab.com.blurview.BlurView;

public class DevelopersActivity extends AppCompatActivity {

    private static final String GARVIT_GITHUB_URL = "https://github.com/gtxprime";
    private static final String GARVIT_LINKEDIN_URL = "https://linkedin.com/in/gtxprime";
    private static final String GARVIT_INSTAGRAM_URL = "https://instagram.com/gtxprime";
    private static final String GARVIT_TELEGRAM_URL = "https://t.me/gtxprime";
    private static final String GARVIT_PFP_URL = "https://github.com/gtxprime.png";

    private static final String Dev2Git = "https://github.com/Yadav-Aayansh";
    private static final String Dev2LinkedIn = "https://www.linkedin.com/in/yadav-aayansh";

    private static final String Dev3Git = "https://github.com/shashwatology";
    private static final String Dev3LinkedIn = "https://www.linkedin.com/in/shashwatupadhyay";
    private static final String Dev3Insta = "https://instagram.com/_shashwatology";

    private ViewGroup rootLayout;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_developers);
        Utils.setPad(findViewById(R.id.root_developers_layout), "bottom", this);

        rootLayout = findViewById(R.id.root_developers_layout);

        // Initialize BlurViews
        BlurView blurViewGarvit = findViewById(R.id.blur_view_garvit);
        BlurView blurViewContributor1 = findViewById(R.id.blur_view_contributor1);
        BlurView blurViewContributor2 = findViewById(R.id.blur_view_contributor2);

        setupBlurView(blurViewGarvit);
        setupBlurView(blurViewContributor1);
        setupBlurView(blurViewContributor2);

        ImageView backBtn = findViewById(R.id.backBtn);
        backBtn.setOnClickListener(v -> onBackPressed());

        ImageView imgGarvitPfp = findViewById(R.id.img_garvit_pfp);
        ImageView img_contributor1_pfp = findViewById(R.id.img_contributor1_pfp);
        ImageView img_contributor2_pfp = findViewById(R.id.img_contributor2_pfp);
        ImageButton btnGarvitGithub = findViewById(R.id.btn_garvit_github);
        ImageButton btnGarvitLinkedin = findViewById(R.id.btn_garvit_linkedin);
        ImageButton btnGarvitInstagram = findViewById(R.id.btn_garvit_instagram);
        ImageButton btnGarvitTelegram = findViewById(R.id.btn_garvit_telegram);

        // Load Garvit's PFP
        Glide.with(this)
                .load(GARVIT_PFP_URL)
                .placeholder(R.drawable.ic_profile_placeholder)
                .error(R.drawable.ic_profile_placeholder)
                .circleCrop()
                .into(imgGarvitPfp);

        Glide.with(this)
                .load(Dev2Git+".png")
                .placeholder(R.drawable.ic_profile_placeholder)
                .error(R.drawable.ic_profile_placeholder)
                .circleCrop()
                .into(img_contributor1_pfp);

        Glide.with(this)
                .load(Dev3Git+".png")
                .placeholder(R.drawable.ic_profile_placeholder)
                .error(R.drawable.ic_profile_placeholder)
                .circleCrop()
                .into(img_contributor2_pfp);

        // Setup click listeners for Garvit's social media
        btnGarvitGithub.setOnClickListener(v -> openUrl(GARVIT_GITHUB_URL));
        btnGarvitLinkedin.setOnClickListener(v -> openUrl(GARVIT_LINKEDIN_URL));
        btnGarvitInstagram.setOnClickListener(v -> openUrl(GARVIT_INSTAGRAM_URL));
        btnGarvitTelegram.setOnClickListener(v -> openUrl(GARVIT_TELEGRAM_URL));

        // Find contributor social buttons (no actions assigned yet as per request)
        ImageButton btnContributor1Github = findViewById(R.id.btn_contributor1_github);
        ImageButton btnContributor1Linkedin = findViewById(R.id.btn_contributor1_linkedin);
        ImageButton btnContributor1Other = findViewById(R.id.btn_contributor1_other);

        ImageButton btnContributor2Github = findViewById(R.id.btn_contributor2_github);
        ImageButton btnContributor2Linkedin = findViewById(R.id.btn_contributor2_linkedin);
        ImageButton btnContributor2Other = findViewById(R.id.btn_contributor2_other);

        btnContributor1Github.setOnClickListener(v->openUrl(Dev2Git));
        btnContributor1Linkedin.setOnClickListener(v->openUrl(Dev2LinkedIn));
        btnContributor1Other.setOnClickListener(v-> Toast.makeText(this, "Not available", Toast.LENGTH_SHORT).show());

        btnContributor2Github.setOnClickListener(v->openUrl(Dev3Git));
        btnContributor2Linkedin.setOnClickListener(v->openUrl(Dev3LinkedIn));
        btnContributor2Other.setOnClickListener(v->openUrl(Dev3Insta));

    }

    private void setupBlurView(BlurView blurView) {
        if (blurView == null) return;
        float radius = 20f;
        View decorView = getWindow().getDecorView();
        Drawable windowBackground = decorView.getBackground();

        blurView.setupWith(rootLayout)
                .setFrameClearDrawable(windowBackground)
                .setBlurRadius(radius)
                .setBlurAutoUpdate(true);
    }

    private void openUrl(String url) {
        if (url == null || url.isEmpty()) {
            Toast.makeText(this, "Link not available", Toast.LENGTH_SHORT).show();
            return;
        }
        Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse(url));
        try {
            startActivity(intent);
        } catch (Exception e) {
            Toast.makeText(this, "Could not open link", Toast.LENGTH_SHORT).show();
        }
    }
} 