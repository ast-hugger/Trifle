// Copyright (c) 2018 Vassili Bykov. Licensed under the Apache License, Version 2.0.

package com.github.vassilibykov.enfilade.core;

import java.util.stream.Stream;

/**
 * A node of an expression tree accepted by Enfilade as input.
 *
 * <p>Expression structure is modeled after A-normal forms, with any specific
 * expression classified as either atomic or complex. Complex expressions are
 * not allowed as subexpression of certain expressions. For example, an argument
 * of a function or a primitive call must be atomic. Because function calls are
 * complex, a function call may not appear as an argument of another function
 * call. In contrast, a primitive call is atomic so an argument of a primitive
 * call may be another primitive call. (So atomicity should not be confused with
 * being a terminal of the expression grammar).
 *
 * <p>This structure (statically verifiable by the type system!) of the input
 * language has beneficial properties for addressing some of the key issues
 * in adaptively translating these expressions into the JVM code.
 *
 * @author Vassili Bykov
 */
public abstract class Expression {

    public interface Visitor<T> {
        T visitCall0(Call0 call);
        T visitCall1(Call1 call);
        T visitCall2(Call2 call);
        T visitConst(Const aConst);
        T visitIf(If anIf);
        T visitLet(Let let);
        T visitPrimitive1(Primitive1 primitive);
        T visitPrimitive2(Primitive2 primitive);
        T visitProg(Prog prog);
        T visitRet(Ret ret);
        T visitSetVar(SetVar set);
        T visitVar(Var var);
    }

    public static abstract class VisitorSkeleton<T> implements Visitor<T> {
        @Override
        public T visitCall0(Call0 call) {
            return null;
        }

        @Override
        public T visitCall1(Call1 call) {
            call.arg().accept(this);
            return null;
        }

        @Override
        public T visitCall2(Call2 call) {
            call.arg1().accept(this);
            call.arg2().accept(this);
            return null;
        }

        @Override
        public T visitConst(Const aConst) {
            return null;
        }

        @Override
        public T visitIf(If anIf) {
            anIf.condition().accept(this);
            anIf.trueBranch().accept(this);
            anIf.falseBranch().accept(this);
            return null;
        }

        @Override
        public T visitLet(Let let) {
            let.variable().accept(this);
            let.initializer().accept(this);
            let.body().accept(this);
            return null;
        }

        @Override
        public T visitPrimitive1(Primitive1 primitive) {
            return null;
        }

        @Override
        public T visitPrimitive2(Primitive2 primitive) {
            return null;
        }

        @Override
        public T visitProg(Prog prog) {
            Stream.of(prog.expressions()).forEach(this::visit);
            return null;
        }

        @Override
        public T visitRet(Ret ret) {
            return ret.value().accept(this);
        }

        @Override
        public T visitSetVar(SetVar set) {
            set.variable().accept(this);
            set.value().accept(this);
            return null;
        }

        @Override
        public T visitVar(Var var) {
            return null;
        }

        private T visit(Expression expr) {
            return expr.accept(this);
        }
    }

    /*
        Instance
     */

    /*internal*/ final CompilerAnnotation compilerAnnotation = new CompilerAnnotation();

    public abstract <T> T accept(Visitor<T> visitor);
}
