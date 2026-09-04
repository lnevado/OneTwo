package com.nicue.onetwo.ui;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.robolectric.Shadows.shadowOf;

import android.content.Context;
import android.os.Looper;
import android.view.LayoutInflater;
import android.view.View;
import androidx.coordinatorlayout.widget.CoordinatorLayout;
import androidx.fragment.app.testing.FragmentScenario;
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
 * The add button must stay on screen at all times. It has been lost twice: once to a scroll
 * behavior that hid it and never brought it back, and once to an entrance animation that scales it
 * to zero and is the only thing that ever scales it back.
 */
@RunWith(RobolectricTestRunner.class)
@Config(sdk = 34)
public class FabVisibilityTest {

    private static final int WIDTH = 1080;
    private static final int HEIGHT = 1920;

    @Test
    public void diceFab_staysOnScreenWhileScrolling() {
        FragmentScenario.launchInContainer(DiceFragment.class, null, R.style.AppTheme)
                .onFragment(
                        fragment -> {
                            DiceViewModel viewModel =
                                    new androidx.lifecycle.ViewModelProvider(fragment)
                                            .get(DiceViewModel.class);
                            while (!viewModel.getUiState().getValue().getDice().isEmpty()) {
                                viewModel.removeDie(0);
                            }
                            for (int i = 0; i < 24; i++) {
                                viewModel.addDie(6);
                            }

                            View root = fragment.getView();
                            layOut(root);
                            settleEntrance();

                            FloatingActionButton fab = root.findViewById(R.id.fab_dice);
                            RecyclerView list = root.findViewById(R.id.recyclerview_dice);
                            assertShown(fab);
                            assertTrue(
                                    "precondition: the list must be scrollable",
                                    list.canScrollVertically(1));

                            list.scrollBy(0, 900);
                            shadowOf(Looper.getMainLooper()).idle();
                            assertShown(fab);

                            list.scrollBy(0, -900);
                            shadowOf(Looper.getMainLooper()).idle();
                            assertShown(fab);
                        });
    }

    @Test
    public void diceFab_finishesItsEntranceAtFullScale() {
        FragmentScenario.launchInContainer(DiceFragment.class, null, R.style.AppTheme)
                .onFragment(
                        fragment -> {
                            layOut(fragment.getView());
                            settleEntrance();
                            assertShown(fragment.getView().findViewById(R.id.fab_dice));
                        });
    }

    @Test
    public void neitherScreenAttachesAScrollHidingBehaviour() {
        assertNoCustomBehaviour(R.layout.dice_layout, R.id.fab_dice);
        assertNoCustomBehaviour(R.layout.counter_layout, R.id.fab);
    }

    private void assertNoCustomBehaviour(int layoutRes, int fabId) {
        Context context = ApplicationProvider.getApplicationContext();
        context.setTheme(R.style.AppTheme);
        View root = LayoutInflater.from(context).inflate(layoutRes, null);
        FloatingActionButton fab = root.findViewById(fabId);
        assertNotNull(fab);

        CoordinatorLayout.Behavior<?> behavior =
                ((CoordinatorLayout.LayoutParams) fab.getLayoutParams()).getBehavior();
        // Material's own Behavior is fine - it only reacts to app bars and snackbars. Anything
        // else here would be a re-introduced scroll-hiding behavior.
        assertTrue(
                "unexpected FAB behavior: " + behavior,
                behavior == null || behavior.getClass() == FloatingActionButton.Behavior.class);
    }

    private static void settleEntrance() {
        shadowOf(Looper.getMainLooper()).idleFor(Duration.ofMillis(1000));
    }

    private static void layOut(View view) {
        view.measure(
                View.MeasureSpec.makeMeasureSpec(WIDTH, View.MeasureSpec.EXACTLY),
                View.MeasureSpec.makeMeasureSpec(HEIGHT, View.MeasureSpec.EXACTLY));
        view.layout(0, 0, WIDTH, HEIGHT);
        shadowOf(Looper.getMainLooper()).idle();
    }

    private static void assertShown(FloatingActionButton fab) {
        assertEquals("fab must stay visible", View.VISIBLE, fab.getVisibility());
        assertEquals("fab must stay at full scale", 1f, fab.getScaleX(), 0.001f);
        assertEquals("fab must stay at full scale", 1f, fab.getScaleY(), 0.001f);
    }
}
