package com.github.vassilibykov.enfilade;

import org.jetbrains.annotations.Nullable;

public class Var extends AtomicExpression {
    @Nullable private final String name;
    private int index = -1;

    Var(@Nullable String name) {
        this.name = name;
    }

    public String name() {
        return name;
    }

    public int index() {
        return index;
    }

    public void setIndex(int index) {
        this.index = index;
    }

    @Override
    public <T> T accept(Visitor<T> visitor) {
        return visitor.visitVar(this);
    }

    @Override
    public String toString() {
        return name != null ? name : "<var" + hashCode() + ">";
    }
}
