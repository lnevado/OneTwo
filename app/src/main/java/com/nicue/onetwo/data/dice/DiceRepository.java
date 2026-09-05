package com.nicue.onetwo.data.dice;

import java.util.List;

public class DiceRepository {
    private final DicePrefsDataSource dataSource;

    public DiceRepository(DicePrefsDataSource dataSource) {
        this.dataSource = dataSource;
    }

    public List<DieRecord> readDice() {
        return dataSource.readDice();
    }

    public void writeDice(List<DieRecord> dice) {
        dataSource.writeDice(dice);
    }
}
