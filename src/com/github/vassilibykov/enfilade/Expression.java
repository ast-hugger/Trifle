package com.github.vassilibykov.enfilade;

public abstract class Expression {

    public interface Visitor<T> {
        T visitCall(Call call);
        T visitConst(Const aConst);
        T visitIf(If anIf);
        T visitLet(Let let);
        T visitPrimitive(Primitive primitive);
        T visitProg(Prog prog);
        T visitRet(Ret ret);
        T visitSetVar(SetVar set);
        T visitVar(Var var);
    }

    public abstract <T> T accept(Visitor<T> visitor);
}
