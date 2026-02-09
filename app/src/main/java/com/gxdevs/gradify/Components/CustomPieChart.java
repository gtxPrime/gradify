package com.gxdevs.gradify.Components;

import android.animation.ValueAnimator;
import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.RectF;
import android.util.AttributeSet;
import android.util.Log;
import android.view.View;
import android.view.animation.DecelerateInterpolator;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import android.graphics.Color;

public class CustomPieChart extends View {

    private List<Integer> data;
    private List<String> segmentNamesList;
    private float startAngle;
    private float gapDegrees;
    private Paint paint;
    private Paint textPaint;
    private Paint centerTextPaint;
    private Paint valueTextPaint;
    private int[] pieChartColors;
    private float[] arcValues;
    private int currentTotalDataSum;

    private float animatedFraction;
    private final RectF rectF = new RectF();

    private int selectedSegmentIndex = -1;
    private float defaultArcWidth;
    private float highlightedArcWidth;

    private static final int DIMMED_ALPHA = 100;
    private static final int FULL_ALPHA = 255;

    public CustomPieChart(Context context) {
        super(context);
        init();
    }

    public CustomPieChart(Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);
        init();
    }

    public CustomPieChart(Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init();
    }

    private void init() {
        defaultArcWidth = 80f;
        highlightedArcWidth = defaultArcWidth * 1.3f;
        startAngle = 140f;
        gapDegrees = 20f;

        paint = new Paint();
        paint.setAntiAlias(true);
        paint.setStyle(Paint.Style.STROKE);
        paint.setStrokeCap(Paint.Cap.ROUND);

        textPaint = new Paint();
        textPaint.setAntiAlias(true);
        textPaint.setStyle(Paint.Style.FILL);
        textPaint.setColor(Color.BLACK);
        textPaint.setTextSize(35f);
        textPaint.setTextAlign(Paint.Align.CENTER);

        centerTextPaint = new Paint();
        centerTextPaint.setAntiAlias(true);
        centerTextPaint.setColor(Color.BLACK);
        centerTextPaint.setTextSize(50f);
        centerTextPaint.setTextAlign(Paint.Align.CENTER);

        valueTextPaint = new Paint();
        valueTextPaint.setAntiAlias(true);
        valueTextPaint.setColor(Color.DKGRAY);
        valueTextPaint.setTextSize(40f);
        valueTextPaint.setTextAlign(Paint.Align.CENTER);

        setLayerType(LAYER_TYPE_SOFTWARE, null);
    }

    public void setData(List<Integer> dataInMinutes, List<String> names, int[] pieChartColors) {
        List<Integer> filteredDataInMinutes = new ArrayList<>();
        List<String> filteredNames = new ArrayList<>();

        if (dataInMinutes == null) {
            dataInMinutes = new ArrayList<>();
        }

        for (int i = 0; i < dataInMinutes.size(); i++) {
            int minutes = dataInMinutes.get(i);
            if (minutes > 0) {
                filteredDataInMinutes.add(minutes);
                if (names != null && i < names.size()) {
                    filteredNames.add(names.get(i));
                } else {
                    filteredNames.add("Unknown");
                }
            }
        }

        if (filteredDataInMinutes.isEmpty()) {
            Log.w("CustomPieChart", "No valid data (minutes > 0). No bars will be drawn.");
            this.data = new ArrayList<>();
            this.segmentNamesList = new ArrayList<>();
        } else {
            this.data = filteredDataInMinutes;
            this.segmentNamesList = filteredNames;
        }

        this.pieChartColors = pieChartColors;
        calculateArcValues();
        startAnimation();
        highlightSegmentByName(null);
    }

    private void calculateArcValues() {
        currentTotalDataSum = 0;
        if (this.data == null || this.data.isEmpty()) {
            arcValues = new float[0];
            return;
        }

        for (int valueInMinutes : this.data) {
            currentTotalDataSum += valueInMinutes;
        }

        if (currentTotalDataSum == 0) {
            Log.w("CustomPieChart", "Total sum of data (minutes) is 0. Arc values cannot be calculated properly.");
            arcValues = new float[this.data.size()];
            return;
        }

        float totalGaps = gapDegrees * this.data.size();
        float availableAngle = 280f - totalGaps;

        arcValues = new float[this.data.size()];
        for (int i = 0; i < this.data.size(); i++) {
            arcValues[i] = (float) this.data.get(i) / currentTotalDataSum * availableAngle;
        }
    }

    private void startAnimation() {
        ValueAnimator animator = ValueAnimator.ofFloat(0f, 1f);
        animator.setDuration(600);
        animator.setInterpolator(new DecelerateInterpolator());
        animator.addUpdateListener(animation -> {
            animatedFraction = (float) animation.getAnimatedValue();
            invalidate();
        });
        animator.start();
    }

    @Override
    protected void onDraw(@NonNull Canvas canvas) {
        super.onDraw(canvas);

        if (data == null || data.isEmpty()) {
            String placeholder = "No data available";
            Paint simplePaint = new Paint();
            simplePaint.setColor(Color.BLACK);
            simplePaint.setTextSize(50f);
            simplePaint.setTextAlign(Paint.Align.CENTER);
            canvas.drawText(placeholder, getWidth() / 2f, getHeight() / 2f, simplePaint);
            return;
        }

        float newStartAngle = startAngle;
        float maxPossibleArcWidth = (selectedSegmentIndex != -1) ? highlightedArcWidth : defaultArcWidth;
        float radius = Math.min(getWidth(), getHeight()) / 2f - maxPossibleArcWidth / 2f;
        float topMargin = 45f;

        float chartCenterX = getWidth() / 2f;
        float chartCenterY = getHeight() / 2f + topMargin;

        rectF.set(
                chartCenterX - radius,
                chartCenterY - radius,
                chartCenterX + radius,
                chartCenterY + radius
        );

        for (int i = 0; i < data.size(); i++) {
            float currentSegmentArcWidth = defaultArcWidth;
            int currentSegmentAlpha = FULL_ALPHA;
            int currentColor = pieChartColors[i % pieChartColors.length];

            if (selectedSegmentIndex != -1) {
                if (i == selectedSegmentIndex) {
                    currentSegmentArcWidth = highlightedArcWidth;
                } else {
                    currentSegmentAlpha = DIMMED_ALPHA;
                }
            }
            paint.setStrokeWidth(currentSegmentArcWidth);
            int segmentColorWithAlpha = Color.argb(currentSegmentAlpha, Color.red(currentColor), Color.green(currentColor), Color.blue(currentColor));
            paint.setColor(segmentColorWithAlpha);

            float shadowRadius = (i == selectedSegmentIndex) ? 70f : 60f;
            paint.setShadowLayer(shadowRadius, 0f, 0f, segmentColorWithAlpha);

            float currentArcSweepAngle = arcValues[i] * animatedFraction;
            canvas.drawArc(rectF, newStartAngle, currentArcSweepAngle, false, paint);
            paint.setShadowLayer(0, 0, 0, 0);

            if (currentArcSweepAngle > 0.1f && currentSegmentAlpha == FULL_ALPHA) {
                float percentage = (float) data.get(i) / currentTotalDataSum * 100f;
                String percentText = String.format(Locale.getDefault(), "%.0f%%", percentage);
                float arcCenterAngleDegrees = newStartAngle + currentArcSweepAngle / 2f;
                float arcCenterAngleRadians = (float) Math.toRadians(arcCenterAngleDegrees);

                float textRadius = radius - (maxPossibleArcWidth - defaultArcWidth)/2f;

                float textX = chartCenterX + (float) (textRadius * Math.cos(arcCenterAngleRadians));
                float textY = chartCenterY + (float) (textRadius * Math.sin(arcCenterAngleRadians));

                double brightness = Color.red(currentColor) * 0.299 +
                                    Color.green(currentColor) * 0.587 +
                                    Color.blue(currentColor) * 0.114;
                textPaint.setColor(brightness < 128 ? Color.WHITE : Color.BLACK);
                canvas.drawText(percentText, textX, textY - (textPaint.descent() + textPaint.ascent()) / 2f, textPaint);
            }
            newStartAngle += arcValues[i] + gapDegrees;
        }
    }

    public void highlightSegmentByName(String subjectName) {
        if (segmentNamesList == null || segmentNamesList.isEmpty() || data == null || data.isEmpty()) {
            selectedSegmentIndex = -1;
            invalidate();
            return;
        }

        if (subjectName == null) {
            selectedSegmentIndex = -1;
        } else {
            boolean found = false;
            for (int i = 0; i < segmentNamesList.size(); i++) {
                if (subjectName.equals(segmentNamesList.get(i))) {
                    if (selectedSegmentIndex == i) {
                        selectedSegmentIndex = -1; 
                    } else {
                        selectedSegmentIndex = i;
                    }
                    found = true;
                    break;
                }
            }
            if (!found) {
                selectedSegmentIndex = -1;
            }
        }
        invalidate();
    }
}
