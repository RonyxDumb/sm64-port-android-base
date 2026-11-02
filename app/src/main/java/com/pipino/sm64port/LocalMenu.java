package com.pipino.sm64port;

import android.app.Activity;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import java.util.concurrent.atomic.AtomicInteger;

/** Invisible hit targets only. All visible menu elements are drawn by SM64. */
final class LocalMenu extends View {
    private final Activity activity;
    private final AtomicInteger event = new AtomicInteger();
    private volatile int page;
    private int captured = -1, pointer = -1;

    LocalMenu(Activity activity, ViewGroup parent) {
        super(activity);
        this.activity = activity;
        setVisibility(GONE);
        setImportantForAccessibility(IMPORTANT_FOR_ACCESSIBILITY_NO);
        parent.addView(this, new ViewGroup.LayoutParams(-1, -1));
    }

    int update(int nextPage, int selected, int status, int ready, String detail) {
        if (page != nextPage) {
            page = nextPage;
            event.set(0);
            activity.runOnUiThread(() -> {
                captured = pointer = -1;
                setVisibility(page == 0 ? GONE : VISIBLE);
            });
        }
        return event.getAndSet(0);
    }

    private int target(float px, float py) {
        float scale = getHeight() / 240f;
        if (scale <= 0) return -1;
        float x = 160 + (px - getWidth() / 2f) / scale;
        float y = 240 - py / scale;
        if (page == 5) {
            if (y < 197 || y > 222) return -1;
            if (x >= 164 && x <= 200) return 20;
            if (x > 200 && x <= 244) return 21;
            return -1;
        }
        if (x < 64 || x > 256) return -1;
        if (y >= 102 && y <= 128) return 16;
        if (y >= 76 && y < 102) return 17;
        if (page == 6) {
            if (y >= 50 && y < 76) return 18;
        } else if (y >= 38 && y <= 66) {
            if (x >= 164 && x <= 200) return 20;
            if (x > 200 && x <= 244) return 21;
        }
        if (page != 1 && x >= 200 && y >= 14 && y <= 35) return 3;
        return -1;
    }

    @Override public boolean onTouchEvent(MotionEvent motion) {
        int action = motion.getActionMasked();
        if (action == MotionEvent.ACTION_DOWN) {
            int hit = target(motion.getX(), motion.getY());
            if (hit < 0) return false;
            pointer = motion.getPointerId(0);
            captured = (page << 8) | hit;
            return true;
        }
        if (pointer < 0) return false;
        if (action == MotionEvent.ACTION_UP) {
            int index = motion.findPointerIndex(pointer);
            if (index >= 0 && captured == ((page << 8) | target(motion.getX(index), motion.getY(index))))
                event.compareAndSet(0, captured);
            pointer = captured = -1;
        } else if (action == MotionEvent.ACTION_CANCEL) pointer = captured = -1;
        return true;
    }
}
