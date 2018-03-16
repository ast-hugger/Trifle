package com.github.vassilibykov.enfilade;

import org.jetbrains.annotations.NotNull;

/**
 * A sequence, like {@code begin} in Scheme or {@code progn} in Common Lisp.
 * In theory it's expressible as a chain of {@code let}s, but it's convenient
 * to treat it as a distinct construct.
 */
public class Prog extends ComplexExpression {
    @NotNull private final Expression[] expressions;

    Prog(@NotNull Expression[] expressions) {
        this.expressions = expressions;
    }

    public Expression[] expressions() {
        return expressions;
    }

    @Override
    public <T> T accept(Visitor<T> visitor) {
        return visitor.visitProg(this);
    }

    @Override
    public String toString() {
        return "(prog [" + expressions.length + "])";
    }
}
