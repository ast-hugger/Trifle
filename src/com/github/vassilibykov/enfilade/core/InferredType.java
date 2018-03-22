// Copyright (c) 2018 Vassili Bykov. Licensed under the Apache License, Version 2.0.

package com.github.vassilibykov.enfilade.core;

import java.util.Optional;

/**
 * A type assigned to an expression by the type inferencer. Inferred type is
 * stored in the expression's {@link Expression#compilerAnnotation}.
 *
 * <p>An expression's inferred type, not to be confused with <em>observed
 * type</em> as recorded by the profiling interpreter, indicates what we know
 * about the value of the expression from static analysis of the expression
 * itself. For example, the inferred type of {@code (const 1)} is {@code int}
 * and the inferred type of {@code (const "foo")} is {@code reference}. Here, as
 * in many other places by type we mean the {@link TypeCategory} of a value, not
 * its type in the Java sense.
 *
 * <p>An inferred type of an expression can be known, as in the examples above,
 * or unknown, as it would be for a call expression. These two possibilities are
 * modeled with two nested classes, however the classes are private to not
 * expose the distinction at the class membership level. Instead, the
 * distinction is exposed through the uniform API of the abstract methods of
 * this class. The {@link #match} method together with the {@link Matcher}
 * interface mimic patching of functional languages. Alternatively, the
 * {@link #typeCategory()} method returns an empty optional for unknown types
 * and an optional with a value for known types.
 */
public abstract class InferredType {

    public static InferredType unknown() {
        return new Unknown();
    }

    public static InferredType known(TypeCategory type) {
        return new Known(type);
    }

    /** A poor man's pattern matching. */
    public interface Matcher<T> {
        T ifUnknown();
        T ifKnown(TypeCategory category);
    }

    private static class Unknown extends InferredType {
        private Unknown() {}

        @Override
        public <T> T match(Matcher<T> matcher) {
            return matcher.ifUnknown();
        }

        @Override
        public boolean isUnknown() {
            return true;
        }

        @Override
        public Optional<TypeCategory> typeCategory() {
            return Optional.empty();
        }

        @Override
        public InferredType union(InferredType other) {
            return this; // unknown
        }
    }

    private static class Known extends InferredType {
        private final TypeCategory type;

        private Known(TypeCategory type) {
            this.type = type;
        }

        @Override
        public <T> T match(Matcher<T> matcher) {
            return matcher.ifKnown(type);
        }

        @Override
        public boolean isUnknown() {
            return false;
        }

        @Override
        public Optional<TypeCategory> typeCategory() {
            return Optional.of(type);
        }

        @Override
        public InferredType union(InferredType other) {
            return other.match(new Matcher<InferredType>() {
                public InferredType ifUnknown() {
                    return other;
                }
                public InferredType ifKnown(TypeCategory otherType) {
                    return InferredType.known(type.union(otherType));
                }
            });
        }
    }

    /**
     * Apply the matcher to this type. Depending on whether this type is known
     * or unknown, the proper method of the matcher is invoked.
     */
    public abstract <T> T match(Matcher<T> matcher);

    /**
     * Indicate whether this type is unknown.
     */
    public abstract boolean isUnknown();

    /**
     * Return this type's category as an optional, empty if this is an unknown
     * type.
     */
    public abstract Optional<TypeCategory> typeCategory();

    public abstract InferredType union(InferredType other);
}
