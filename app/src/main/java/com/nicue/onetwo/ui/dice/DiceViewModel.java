package com.nicue.onetwo.ui.dice;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.SavedStateHandle;
import androidx.lifecycle.ViewModel;
import com.nicue.onetwo.data.dice.DicePrefsDataSource;
import com.nicue.onetwo.data.dice.DiceRepository;
import com.nicue.onetwo.data.dice.DieRecord;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;

public class DiceViewModel extends ViewModel {
    private static final String KEY_FACES = "dice_faces";
    private static final String KEY_VALUES = "dice_values";
    private static final String KEY_IDS = "dice_ids";
    private static final String KEY_LOCKED = "dice_locked";
    private static final String KEY_COLORS = "dice_colors";
    private static final String KEY_LABELS = "dice_labels";
    private static final String KEY_NEXT_ID = "dice_next_id";

    public static final int MAX_LABEL_LENGTH = 12;

    private final DiceRepository diceRepository;
    private final SavedStateHandle savedStateHandle;
    private final MutableLiveData<DiceUiState> uiState = new MutableLiveData<>();
    private final Random random = new Random();

    public DiceViewModel(DiceRepository diceRepository, SavedStateHandle savedStateHandle) {
        this.diceRepository = diceRepository;
        this.savedStateHandle = savedStateHandle;
        publish(restoreState());
    }

    public LiveData<DiceUiState> getUiState() {
        return uiState;
    }

    public void addDie(int faces) {
        DiceState state = readState();
        long nextId = nextId();
        int boundedFaces = Math.max(2, faces);

        state.faces.add(boundedFaces);
        state.values.add(boundedFaces);
        state.ids.add(nextId);
        state.locked.add(Boolean.FALSE);
        // Colour follows creation order rather than grid position, so a die keeps its colour when
        // the dice around it are removed.
        state.colors.add((int) (nextId % DicePrefsDataSource.COLOR_COUNT));
        state.labels.add("");

        savedStateHandle.set(KEY_NEXT_ID, nextId + 1);
        persist(state);
        publish(state);
    }

    public void removeDie(int position) {
        DiceState state = readState();
        if (!state.contains(position)) {
            return;
        }
        state.removeAt(position);
        persist(state);
        publish(state);
    }

    public void rollDie(int position) {
        DiceState state = readState();
        if (!state.contains(position) || state.isLocked(position)) {
            return;
        }
        state.values.set(position, roll(state.faces.get(position)));
        publish(state);
    }

    public void rollAllDice() {
        DiceState state = readState();
        for (int i = 0; i < state.size(); i++) {
            if (state.isLocked(i)) {
                continue;
            }
            state.values.set(i, roll(state.faces.get(i)));
        }
        publish(state);
    }

    public void toggleLock(int position) {
        DiceState state = readState();
        if (!state.contains(position)) {
            return;
        }
        state.locked.set(position, !state.isLocked(position));
        publish(state);
    }

    public void unlockAllDice() {
        DiceState state = readState();
        boolean changed = false;
        for (int i = 0; i < state.size(); i++) {
            if (state.isLocked(i)) {
                state.locked.set(i, Boolean.FALSE);
                changed = true;
            }
        }
        if (changed) {
            publish(state);
        }
    }

    /** A blank label clears the label, which is how a die goes back to showing its die type. */
    public void setDieAppearance(int position, int colorIndex, String label) {
        DiceState state = readState();
        if (!state.contains(position)) {
            return;
        }
        state.colors.set(position, boundColorIndex(colorIndex));
        state.labels.set(position, boundLabel(label));
        persist(state);
        publish(state);
    }

    private static int boundColorIndex(int colorIndex) {
        int wrapped = colorIndex % DicePrefsDataSource.COLOR_COUNT;
        return wrapped < 0 ? wrapped + DicePrefsDataSource.COLOR_COUNT : wrapped;
    }

    private static String boundLabel(String label) {
        if (label == null) {
            return "";
        }
        String trimmed = label.trim();
        if (trimmed.length() <= MAX_LABEL_LENGTH) {
            return trimmed;
        }
        // Trim again: cutting mid-string can leave the label ending in a space.
        return trimmed.substring(0, MAX_LABEL_LENGTH).trim();
    }

    private int roll(int faces) {
        return random.nextInt(faces) + 1;
    }

    private long nextId() {
        Long nextId = savedStateHandle.get(KEY_NEXT_ID);
        return nextId == null ? 0L : nextId;
    }

    private void persist(DiceState state) {
        ArrayList<DieRecord> records = new ArrayList<>();
        for (int i = 0; i < state.size(); i++) {
            records.add(
                    new DieRecord(state.faces.get(i), state.colors.get(i), state.labels.get(i)));
        }
        diceRepository.writeDice(records);
    }

    /** Rebuilds session state, falling back to the persisted dice when there is none. */
    private DiceState restoreState() {
        DiceState state = new DiceState();
        ArrayList<Integer> savedFaces = savedStateHandle.get(KEY_FACES);

        if (savedFaces == null) {
            List<DieRecord> records = diceRepository.readDice();
            long id = 0;
            for (DieRecord record : records) {
                state.faces.add(record.getFaces());
                state.values.add(record.getFaces());
                state.ids.add(id++);
                state.locked.add(Boolean.FALSE);
                state.colors.add(record.getColorIndex());
                state.labels.add(record.getLabel());
            }
            savedStateHandle.set(KEY_NEXT_ID, id);
            return state;
        }

        state.faces.addAll(savedFaces);
        int size = state.faces.size();
        state.values.addAll(
                sizedInts(savedStateHandle.<ArrayList<Integer>>get(KEY_VALUES), size, state.faces));
        state.ids.addAll(sizedIds(savedStateHandle.<ArrayList<Long>>get(KEY_IDS), size));
        state.locked.addAll(sizedLocks(savedStateHandle.<ArrayList<Boolean>>get(KEY_LOCKED), size));
        state.colors.addAll(
                sizedColors(savedStateHandle.<ArrayList<Integer>>get(KEY_COLORS), size));
        state.labels.addAll(sizedLabels(savedStateHandle.<ArrayList<String>>get(KEY_LABELS), size));
        return state;
    }

    private ArrayList<Integer> sizedInts(
            ArrayList<Integer> saved, int size, ArrayList<Integer> fallback) {
        if (saved != null && saved.size() == size) {
            return new ArrayList<>(saved);
        }
        return new ArrayList<>(fallback);
    }

    private ArrayList<Long> sizedIds(ArrayList<Long> saved, int size) {
        if (saved != null && saved.size() == size) {
            return new ArrayList<>(saved);
        }
        ArrayList<Long> ids = new ArrayList<>();
        for (int i = 0; i < size; i++) {
            ids.add((long) i);
        }
        savedStateHandle.set(KEY_NEXT_ID, (long) size);
        return ids;
    }

    private ArrayList<Boolean> sizedLocks(ArrayList<Boolean> saved, int size) {
        if (saved != null && saved.size() == size) {
            return new ArrayList<>(saved);
        }
        ArrayList<Boolean> locks = new ArrayList<>();
        for (int i = 0; i < size; i++) {
            locks.add(Boolean.FALSE);
        }
        return locks;
    }

    private ArrayList<Integer> sizedColors(ArrayList<Integer> saved, int size) {
        if (saved != null && saved.size() == size) {
            return new ArrayList<>(saved);
        }
        ArrayList<Integer> colors = new ArrayList<>();
        for (int i = 0; i < size; i++) {
            colors.add(i % DicePrefsDataSource.COLOR_COUNT);
        }
        return colors;
    }

    private ArrayList<String> sizedLabels(ArrayList<String> saved, int size) {
        if (saved != null && saved.size() == size) {
            return new ArrayList<>(saved);
        }
        ArrayList<String> labels = new ArrayList<>();
        for (int i = 0; i < size; i++) {
            labels.add("");
        }
        return labels;
    }

    private DiceState readState() {
        return restoreState();
    }

    private void publish(DiceState state) {
        savedStateHandle.set(KEY_FACES, new ArrayList<>(state.faces));
        savedStateHandle.set(KEY_VALUES, new ArrayList<>(state.values));
        savedStateHandle.set(KEY_IDS, new ArrayList<>(state.ids));
        savedStateHandle.set(KEY_LOCKED, new ArrayList<>(state.locked));
        savedStateHandle.set(KEY_COLORS, new ArrayList<>(state.colors));
        savedStateHandle.set(KEY_LABELS, new ArrayList<>(state.labels));

        ArrayList<DieUiModel> dice = new ArrayList<>();
        for (int i = 0; i < state.size(); i++) {
            dice.add(
                    new DieUiModel(
                            state.ids.get(i),
                            state.faces.get(i),
                            state.values.get(i),
                            state.isLocked(i),
                            state.colors.get(i),
                            state.labels.get(i)));
        }
        uiState.setValue(new DiceUiState(dice));
    }

    /**
     * Holds the per-die lists together so that adding, removing, or reordering a die touches them
     * as one unit. Several of them are lists of the same type, so passing them around separately
     * makes a silent transposition easy.
     */
    private static final class DiceState {
        final ArrayList<Integer> faces = new ArrayList<>();
        final ArrayList<Integer> values = new ArrayList<>();
        final ArrayList<Long> ids = new ArrayList<>();
        final ArrayList<Boolean> locked = new ArrayList<>();
        final ArrayList<Integer> colors = new ArrayList<>();
        final ArrayList<String> labels = new ArrayList<>();

        int size() {
            return faces.size();
        }

        boolean contains(int position) {
            return position >= 0 && position < size();
        }

        boolean isLocked(int position) {
            Boolean value = contains(position) ? locked.get(position) : null;
            return value != null && value;
        }

        void removeAt(int position) {
            faces.remove(position);
            values.remove(position);
            ids.remove(position);
            locked.remove(position);
            colors.remove(position);
            labels.remove(position);
        }
    }
}
