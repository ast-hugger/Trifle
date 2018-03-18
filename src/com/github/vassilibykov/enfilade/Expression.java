package com.github.vassilibykov.enfilade;

import com.github.vassilibykov.enfilade.compiler.CompilerAnnotation;

import java.util.stream.Stream;

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

    private CompilerAnnotation compilerAnnotation;

    public CompilerAnnotation compilerAnnotation() {
        return compilerAnnotation;
    }

    public void setCompilerAnnotation(CompilerAnnotation compilerAnnotation) {
        this.compilerAnnotation = compilerAnnotation;
    }

    public abstract <T> T accept(Visitor<T> visitor);
}
