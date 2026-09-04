package com.nicue.onetwo.utils;

import android.content.Context;
import android.util.AttributeSet;
import android.view.View;
import androidx.annotation.NonNull;
import androidx.coordinatorlayout.widget.CoordinatorLayout;
import androidx.core.view.ViewCompat;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import java.lang.ref.WeakReference;

/**
 * Hides the FAB while the user scrolls down a list and brings it back when they scroll up.
 *
 * <p>The type-aware overloads are the ones to implement: CoordinatorLayout only falls back to the
 * deprecated untyped ones for {@link ViewCompat#TYPE_TOUCH}, so a behavior that overrides those
 * never runs during a fling.
 */
public class ScrollAwareFABBehavior extends FloatingActionButton.Behavior {
    private WeakReference<View> lastScrollTarget;
    private View.OnLayoutChangeListener targetLayoutListener;

    public ScrollAwareFABBehavior(Context context, AttributeSet attrs) {
        super();
    }

    @Override
    public boolean onStartNestedScroll(
            @NonNull CoordinatorLayout coordinatorLayout,
            @NonNull FloatingActionButton child,
            @NonNull View directTargetChild,
            @NonNull View target,
            int axes,
            int type) {
        return (axes & ViewCompat.SCROLL_AXIS_VERTICAL) != 0
                || super.onStartNestedScroll(
                        coordinatorLayout, child, directTargetChild, target, axes, type);
    }

    @Override
    public void onNestedScroll(
            @NonNull CoordinatorLayout coordinatorLayout,
            @NonNull FloatingActionButton child,
            @NonNull View target,
            int dxConsumed,
            int dyConsumed,
            int dxUnconsumed,
            int dyUnconsumed,
            int type) {
        watchForNothingLeftToScroll(target, child);

        if (dyConsumed > 0 && type == ViewCompat.TYPE_TOUCH) {
            // Only a finger drag may hide the FAB. RecyclerView reports both flings and
            // smoothScrollToPosition as TYPE_NON_TOUCH, and CounterFragment smooth-scrolls to the
            // new row whenever a counter is added - hiding there would pull the button out from
            // under the user the moment they used it.
            child.hide();
        } else if (dyConsumed < 0) {
            // Showing accepts any scroll type: scrolling back up a long list is usually a fling,
            // and that is the case that used to leave the FAB stranded.
            child.show();
        }
    }

    @Override
    public void onDetachedFromLayoutParams() {
        stopWatchingTarget();
        super.onDetachedFromLayoutParams();
    }

    /**
     * A list that shrinks below one screen while the FAB is hidden can no longer produce the upward
     * scroll that would bring it back, which would strand the only control for adding an item.
     * Watching the target's layout catches that the moment the list resizes.
     */
    private void watchForNothingLeftToScroll(final View target, final FloatingActionButton child) {
        View tracked = lastScrollTarget == null ? null : lastScrollTarget.get();
        if (tracked == target) {
            return;
        }
        stopWatchingTarget();

        lastScrollTarget = new WeakReference<>(target);
        final WeakReference<FloatingActionButton> fabReference = new WeakReference<>(child);
        targetLayoutListener =
                new View.OnLayoutChangeListener() {
                    @Override
                    public void onLayoutChange(
                            View view,
                            int left,
                            int top,
                            int right,
                            int bottom,
                            int oldLeft,
                            int oldTop,
                            int oldRight,
                            int oldBottom) {
                        FloatingActionButton fab = fabReference.get();
                        if (fab != null) {
                            showIfNothingLeftToScroll(view, fab);
                        }
                    }
                };
        target.addOnLayoutChangeListener(targetLayoutListener);
    }

    private void stopWatchingTarget() {
        View tracked = lastScrollTarget == null ? null : lastScrollTarget.get();
        if (tracked != null && targetLayoutListener != null) {
            tracked.removeOnLayoutChangeListener(targetLayoutListener);
        }
        lastScrollTarget = null;
        targetLayoutListener = null;
    }

    private void showIfNothingLeftToScroll(View target, FloatingActionButton child) {
        if (!target.canScrollVertically(-1) && !target.canScrollVertically(1)) {
            child.show();
        }
    }
}
