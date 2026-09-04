package com.nicue.onetwo.utils;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.robolectric.Shadows.shadowOf;

import android.content.Context;
import android.os.Looper;
import android.view.LayoutInflater;
import android.view.View;
import androidx.coordinatorlayout.widget.CoordinatorLayout;
import androidx.core.view.ViewCompat;
import androidx.fragment.app.testing.FragmentScenario;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.test.core.app.ApplicationProvider;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.nicue.onetwo.R;
import com.nicue.onetwo.ui.dice.DiceFragment;
import com.nicue.onetwo.ui.dice.DiceViewModel;
import java.time.Duration;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.annotation.Config;

/**
 * The FAB reported VISIBLE throughout the original bug while being scaled to zero, so every
 * assertion here checks scale as well as visibility.
 */
@RunWith(RobolectricTestRunner.class)
@Config(sdk = 34)
public class ScrollAwareFABBehaviorTest {

    private static final int WIDTH = 1080;
    private static final int HEIGHT = 1920;

    @Test
    public void dragDownThenDragUp_bringsTheFabBack() {
        onDiceScreen(
                (root, fab, recyclerView, behavior) -> {
                    dispatchScroll(behavior, root, fab, recyclerView, 60, ViewCompat.TYPE_TOUCH);
                    assertHidden(fab);

                    dispatchScroll(behavior, root, fab, recyclerView, -60, ViewCompat.TYPE_TOUCH);
                    assertShown(fab);
                });
    }

    /**
     * The reported bug: the FAB hides on a drag, but scrolling back up a long list is almost always
     * a fling, and a fling never reaches the behavior.
     */
    @Test
    public void hiddenByDrag_thenFlungUp_bringsTheFabBack() {
        onDiceScreen(
                (root, fab, recyclerView, behavior) -> {
                    dispatchScroll(behavior, root, fab, recyclerView, 60, ViewCompat.TYPE_TOUCH);
                    assertHidden(fab);

                    dispatchScroll(
                            behavior, root, fab, recyclerView, -60, ViewCompat.TYPE_NON_TOUCH);
                    assertShown(fab);
                });
    }

    /**
     * RecyclerView reports smoothScrollToPosition as TYPE_NON_TOUCH just like a fling, and
     * CounterFragment smooth-scrolls to the new row on every add, so a downward non-touch scroll
     * must never hide the FAB.
     */
    @Test
    public void programmaticScrollDown_leavesTheFabAlone() {
        onDiceScreen(
                (root, fab, recyclerView, behavior) -> {
                    dispatchScroll(
                            behavior, root, fab, recyclerView, 60, ViewCompat.TYPE_NON_TOUCH);
                    assertShown(fab);
                });
    }

    @Test
    public void listBecomesUnscrollableWhileHidden_bringsTheFabBack() {
        FragmentScenario<DiceFragment> scenario = launchDice();
        scenario.onFragment(
                fragment -> {
                    View root = fragment.getView();
                    FloatingActionButton fab = root.findViewById(R.id.fab_dice);
                    RecyclerView recyclerView = root.findViewById(R.id.recyclerview_dice);
                    CoordinatorLayout.Behavior<FloatingActionButton> behavior = behaviorOf(fab);

                    assertTrue(
                            "precondition: the list must start out scrollable",
                            recyclerView.canScrollVertically(1));

                    dispatchScroll(
                            behavior,
                            (CoordinatorLayout) root,
                            fab,
                            recyclerView,
                            60,
                            ViewCompat.TYPE_TOUCH);
                    assertHidden(fab);

                    // Deleting dice until the grid fits on screen leaves no way to scroll back up,
                    // so the FAB - the only way to add a die - must return on its own.
                    DiceViewModel viewModel =
                            new androidx.lifecycle.ViewModelProvider(fragment)
                                    .get(DiceViewModel.class);
                    while (viewModel.getUiState().getValue().getDice().size() > 1) {
                        viewModel.removeDie(0);
                    }
                    layOut(root);

                    assertFalse(
                            "precondition: deleting dice must leave the list unscrollable",
                            recyclerView.canScrollVertically(1));
                    assertShown(fab);
                });
    }

    @Test
    public void counterScreenUsesTheSameBehaviour() {
        Context context = ApplicationProvider.getApplicationContext();
        context.setTheme(R.style.AppTheme);
        View root = LayoutInflater.from(context).inflate(R.layout.counter_layout, null);
        layOut(root);

        FloatingActionButton fab = root.findViewById(R.id.fab);
        assertNotNull(fab);
        CoordinatorLayout.Behavior<FloatingActionButton> behavior = behaviorOf(fab);
        assertTrue(
                "counter screen should share the fixed behavior",
                behavior instanceof ScrollAwareFABBehavior);

        RecyclerView recyclerView = root.findViewById(R.id.recyclerview_counters);
        assertNotNull(recyclerView);
        recyclerView.setLayoutManager(new LinearLayoutManager(context));
        recyclerView.setAdapter(new TallRowsAdapter(40));
        layOut(root);
        assertTrue(
                "precondition: the counter list must be scrollable",
                recyclerView.canScrollVertically(1));

        dispatchScroll(
                behavior,
                (CoordinatorLayout) root,
                fab,
                recyclerView,
                60,
                ViewCompat.TYPE_NON_TOUCH);
        assertShown(fab);

        dispatchScroll(
                behavior, (CoordinatorLayout) root, fab, recyclerView, 60, ViewCompat.TYPE_TOUCH);
        assertHidden(fab);

        dispatchScroll(
                behavior, (CoordinatorLayout) root, fab, recyclerView, -60, ViewCompat.TYPE_TOUCH);
        assertShown(fab);
    }

    /** Minimal adapter whose rows are tall enough to make the list scroll. */
    private static class TallRowsAdapter extends RecyclerView.Adapter<RecyclerView.ViewHolder> {
        private final int rowCount;

        TallRowsAdapter(int rowCount) {
            this.rowCount = rowCount;
        }

        @Override
        public RecyclerView.ViewHolder onCreateViewHolder(android.view.ViewGroup parent, int type) {
            View row = new View(parent.getContext());
            row.setLayoutParams(new RecyclerView.LayoutParams(WIDTH, 200));
            return new RecyclerView.ViewHolder(row) {};
        }

        @Override
        public void onBindViewHolder(RecyclerView.ViewHolder holder, int position) {}

        @Override
        public int getItemCount() {
            return rowCount;
        }
    }

    // helpers

    private interface DiceScreenAssertions {
        void run(
                CoordinatorLayout root,
                FloatingActionButton fab,
                RecyclerView recyclerView,
                CoordinatorLayout.Behavior<FloatingActionButton> behavior);
    }

    private void onDiceScreen(DiceScreenAssertions assertions) {
        launchDice()
                .onFragment(
                        fragment -> {
                            View root = fragment.getView();
                            assertions.run(
                                    (CoordinatorLayout) root,
                                    root.findViewById(R.id.fab_dice),
                                    root.findViewById(R.id.recyclerview_dice),
                                    behaviorOf(root.findViewById(R.id.fab_dice)));
                        });
    }

    private FragmentScenario<DiceFragment> launchDice() {
        FragmentScenario<DiceFragment> scenario =
                FragmentScenario.launchInContainer(DiceFragment.class, null, R.style.AppTheme);
        scenario.onFragment(
                fragment -> {
                    DiceViewModel viewModel =
                            new androidx.lifecycle.ViewModelProvider(fragment)
                                    .get(DiceViewModel.class);
                    while (!viewModel.getUiState().getValue().getDice().isEmpty()) {
                        viewModel.removeDie(0);
                    }
                    for (int i = 0; i < 16; i++) {
                        viewModel.addDie(6);
                    }
                    layOut(fragment.getView());
                    settleFabEntrance(fragment.getView().findViewById(R.id.fab_dice));
                });
        return scenario;
    }

    @SuppressWarnings("unchecked")
    private static CoordinatorLayout.Behavior<FloatingActionButton> behaviorOf(View fab) {
        CoordinatorLayout.LayoutParams params =
                (CoordinatorLayout.LayoutParams) fab.getLayoutParams();
        return (CoordinatorLayout.Behavior<FloatingActionButton>) params.getBehavior();
    }

    /**
     * Mirrors CoordinatorLayout: a behavior only receives onNestedScroll for a scroll type it
     * accepted in onStartNestedScroll.
     */
    private static void dispatchScroll(
            CoordinatorLayout.Behavior<FloatingActionButton> behavior,
            CoordinatorLayout root,
            FloatingActionButton fab,
            View target,
            int dyConsumed,
            int type) {
        boolean accepted =
                behavior.onStartNestedScroll(
                        root, fab, target, target, ViewCompat.SCROLL_AXIS_VERTICAL, type);
        if (!accepted) {
            return;
        }
        scroll(behavior, root, fab, target, dyConsumed, type);
    }

    private static void scroll(
            CoordinatorLayout.Behavior<FloatingActionButton> behavior,
            CoordinatorLayout root,
            FloatingActionButton fab,
            View target,
            int dyConsumed,
            int type) {
        // CoordinatorLayout dispatches the 9-arg overload; reaching our override through the base
        // class's delegation is part of what these tests verify.
        behavior.onNestedScroll(root, fab, target, 0, dyConsumed, 0, 0, type, new int[2]);
        shadowOf(Looper.getMainLooper()).idle();
    }

    /**
     * DiceFragment starts the FAB at scale 0 and restores it from a delayed post, so tests must let
     * that finish or every assertion about scale is measuring the entrance animation.
     */
    private static void settleFabEntrance(FloatingActionButton fab) {
        shadowOf(Looper.getMainLooper()).idleFor(Duration.ofMillis(1000));
        fab.setScaleX(1f);
        fab.setScaleY(1f);
        assertEquals(View.VISIBLE, fab.getVisibility());
    }

    private static void layOut(View view) {
        view.measure(
                View.MeasureSpec.makeMeasureSpec(WIDTH, View.MeasureSpec.EXACTLY),
                View.MeasureSpec.makeMeasureSpec(HEIGHT, View.MeasureSpec.EXACTLY));
        view.layout(0, 0, WIDTH, HEIGHT);
        shadowOf(Looper.getMainLooper()).idle();
    }

    private static void assertHidden(FloatingActionButton fab) {
        assertEquals("fab should be gone after scrolling down", View.GONE, fab.getVisibility());
    }

    private static void assertShown(FloatingActionButton fab) {
        assertEquals("fab should be visible again", View.VISIBLE, fab.getVisibility());
        assertEquals("fab should be back at full scale", 1f, fab.getScaleX(), 0.001f);
        assertEquals("fab should be back at full scale", 1f, fab.getScaleY(), 0.001f);
    }
}
