package com.nigdroid.sangeet;

import android.animation.ValueAnimator;
import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.RadialGradient;
import android.graphics.Shader;
import android.util.AttributeSet;
import android.view.View;
import android.view.animation.LinearInterpolator;

public class AuroraBackgroundView extends View {

    private float phase;
    private ValueAnimator animator;
    private final Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG);

    public AuroraBackgroundView(Context context) {
        super(context);
        init();
    }

    public AuroraBackgroundView(Context context, AttributeSet attrs) {
        super(context, attrs);
        init();
    }

    public AuroraBackgroundView(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init();
    }

    private void init() {
        setLayerType(LAYER_TYPE_HARDWARE, null);
    }

    @Override
    protected void onAttachedToWindow() {
        super.onAttachedToWindow();
        if (animator == null) {
            animator = ValueAnimator.ofFloat(0f, 1f);
            animator.setDuration(14000);
            animator.setRepeatCount(ValueAnimator.INFINITE);
            animator.setInterpolator(new LinearInterpolator());
            animator.addUpdateListener(a -> {
                phase = (float) a.getAnimatedValue();
                invalidate();
            });
            animator.start();
        }
    }

    @Override
    protected void onDetachedFromWindow() {
        if (animator != null) {
            animator.cancel();
            animator = null;
        }
        super.onDetachedFromWindow();
    }

    @Override
    protected void onDraw(Canvas canvas) {
        int w = getWidth();
        int h = getHeight();
        if (w == 0 || h == 0) {
            return;
        }
        float cx1 = w * (0.25f + 0.6f * phase);
        float cy1 = h * (0.20f + 0.25f * (float) Math.sin(phase * Math.PI * 2));
        float cx2 = w * (0.75f - 0.5f * phase);
        float cy2 = h * (0.70f + 0.2f * (float) Math.cos(phase * Math.PI * 2));

        float radius = Math.max(w, h) * 0.55f;
        RadialGradient g1 = new RadialGradient(
                cx1,
                cy1,
                radius,
                new int[]{Color.parseColor("#66A855F7"), Color.parseColor("#003156A9")},
                new float[]{0f, 1f},
                Shader.TileMode.CLAMP
        );
        paint.setShader(g1);
        canvas.drawRect(0, 0, w, h, paint);

        RadialGradient g2 = new RadialGradient(
                cx2,
                cy2,
                Math.max(w, h) * 0.45f,
                new int[]{Color.parseColor("#88EC4899"), Color.parseColor("#004C1D95")},
                new float[]{0f, 1f},
                Shader.TileMode.CLAMP
        );
        paint.setShader(g2);
        canvas.drawRect(0, 0, w, h, paint);

        RadialGradient g3 = new RadialGradient(
                w * 0.5f,
                h * (0.35f + 0.1f * phase),
                w * 0.4f,
                new int[]{Color.parseColor("#5527D1C1"), Color.parseColor("#000B1320")},
                new float[]{0f, 1f},
                Shader.TileMode.CLAMP
        );
        paint.setShader(g3);
        canvas.drawRect(0, 0, w, h, paint);

        paint.setShader(null);
    }
}
