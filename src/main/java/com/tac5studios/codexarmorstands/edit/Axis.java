package com.tac5studios.codexarmorstands.edit;

public enum Axis {
    X, Y, Z;

    public Axis next(int dir) {
        Axis[] all = values();
        return all[Math.floorMod(ordinal() + dir, all.length)];
    }
}
