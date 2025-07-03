package com.environmentalcontrol;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.RectF;
import android.util.AttributeSet;
import android.view.View;

public class CircularMeterView extends View {
    
    private Paint backgroundPaint;
    private Paint progressPaint;
    private Paint textPaint;
    private Paint optimalRangePaint;
    private RectF rect;
    
    private float minValue = 0;
    private float maxValue = 100;
    private float currentValue = 0;
    private float optimalMin = 0;
    private float optimalMax = 100;
    private String unit = "";
    
    private int meterColor = Color.parseColor("#1B5E20"); // Dark green
    private int progressColor = Color.parseColor("#4CAF50"); // Bright green
    private int optimalColor = Color.parseColor("#66BB6A"); // Light green
    private int textColor = Color.WHITE;
    
    private float strokeWidth = 20f;
    private float startAngle = 135f; // Start from bottom left
    private float sweepAngle = 270f; // 3/4 circle
    
    public CircularMeterView(Context context) {
        super(context);
        init();
    }
    
    public CircularMeterView(Context context, AttributeSet attrs) {
        super(context, attrs);
        init();
    }
    
    public CircularMeterView(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init();
    }
    
    private void init() {
        backgroundPaint = new Paint();
        backgroundPaint.setColor(meterColor);
        backgroundPaint.setStyle(Paint.Style.STROKE);
        backgroundPaint.setStrokeWidth(strokeWidth);
        backgroundPaint.setAntiAlias(true);
        backgroundPaint.setStrokeCap(Paint.Cap.ROUND);
        
        progressPaint = new Paint();
        progressPaint.setColor(progressColor);
        progressPaint.setStyle(Paint.Style.STROKE);
        progressPaint.setStrokeWidth(strokeWidth);
        progressPaint.setAntiAlias(true);
        progressPaint.setStrokeCap(Paint.Cap.ROUND);
        
        optimalRangePaint = new Paint();
        optimalRangePaint.setColor(optimalColor);
        optimalRangePaint.setStyle(Paint.Style.STROKE);
        optimalRangePaint.setStrokeWidth(strokeWidth * 0.6f);
        optimalRangePaint.setAntiAlias(true);
        optimalRangePaint.setStrokeCap(Paint.Cap.ROUND);
        
        textPaint = new Paint();
        textPaint.setColor(textColor);
        textPaint.setTextSize(60f);
        textPaint.setAntiAlias(true);
        textPaint.setTextAlign(Paint.Align.CENTER);
        textPaint.setFakeBoldText(true);
        
        rect = new RectF();
    }
    
    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        
        int width = getWidth();
        int height = getHeight();
        int size = Math.min(width, height);
        
        float padding = strokeWidth * 2;
        rect.set(padding, padding, size - padding, size - padding);
        
        // Draw background arc
        canvas.drawArc(rect, startAngle, sweepAngle, false, backgroundPaint);
        
        // Draw optimal range arc
        if (optimalMin != optimalMax) {
            float optimalStartAngle = startAngle + (optimalMin - minValue) / (maxValue - minValue) * sweepAngle;
            float optimalSweepAngle = (optimalMax - optimalMin) / (maxValue - minValue) * sweepAngle;
            canvas.drawArc(rect, optimalStartAngle, optimalSweepAngle, false, optimalRangePaint);
        }
        
        // Draw progress arc
        float progressSweepAngle = (currentValue - minValue) / (maxValue - minValue) * sweepAngle;
        canvas.drawArc(rect, startAngle, progressSweepAngle, false, progressPaint);
        
        // Draw value text
        String valueText = String.format("%.1f", currentValue);
        String unitText = unit;
        
        float centerX = width / 2f;
        float centerY = height / 2f;
        
        // Draw main value
        textPaint.setTextSize(size * 0.15f);
        canvas.drawText(valueText, centerX, centerY - 10, textPaint);
        
        // Draw unit
        textPaint.setTextSize(size * 0.08f);
        canvas.drawText(unitText, centerX, centerY + 30, textPaint);
        
        // Draw range indicator
        textPaint.setTextSize(size * 0.06f);
        String rangeText = String.format("%.0f - %.0f", minValue, maxValue);
        canvas.drawText(rangeText, centerX, centerY + 60, textPaint);
    }
    
    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        int width = MeasureSpec.getSize(widthMeasureSpec);
        int height = MeasureSpec.getSize(heightMeasureSpec);
        int size = Math.min(width, height);
        setMeasuredDimension(size, size);
    }
    
    // Setter methods
    public void setRange(float min, float max) {
        this.minValue = min;
        this.maxValue = max;
        invalidate();
    }
    
    public void setValue(float value) {
        this.currentValue = Math.max(minValue, Math.min(maxValue, value));
        invalidate();
    }
    
    public void setOptimalRange(float min, float max) {
        this.optimalMin = min;
        this.optimalMax = max;
        invalidate();
    }
    
    public void setUnit(String unit) {
        this.unit = unit;
        invalidate();
    }
    
    public void setMeterColor(int color) {
        this.meterColor = color;
        backgroundPaint.setColor(color);
        invalidate();
    }
    
    public void setProgressColor(int color) {
        this.progressColor = color;
        progressPaint.setColor(color);
        invalidate();
    }
    
    public void setOptimalColor(int color) {
        this.optimalColor = color;
        optimalRangePaint.setColor(color);
        invalidate();
    }
    
    public void setTextColor(int color) {
        this.textColor = color;
        textPaint.setColor(color);
        invalidate();
    }
    
    // Getter methods
    public float getCurrentValue() {
        return currentValue;
    }
    
    public float getMinValue() {
        return minValue;
    }
    
    public float getMaxValue() {
        return maxValue;
    }
    
    public boolean isInOptimalRange() {
        return currentValue >= optimalMin && currentValue <= optimalMax;
    }
} 