package com.github.vassilibykov.enfilade.compiler;

import com.github.vassilibykov.enfilade.*;

public class Analyzer implements Expression.Visitor<Void> {
    private final MethodProfile methodProfile;

    Analyzer(MethodProfile methodProfile) {
        this.methodProfile = methodProfile;
    }

    @Override
    public Void visitCall0(Call0 call) {
        return null;
    }

    @Override
    public Void visitCall1(Call1 call1) {
        return null;
    }

    @Override
    public Void visitCall2(Call2 call2) {
        return null;
    }

    @Override
    public Void visitConst(Const aConst) {
        aConst.setCompilerAnnotation(new CompilerAnnotation(ValueCategory.ofObject(aConst.value())));
        return null;
    }

    @Override
    public Void visitIf(If anIf) {
        return null;
    }

    @Override
    public Void visitLet(Let let) {
        return null;
    }

    @Override
    public Void visitPrimitive1(Primitive1 primitive1) {
        return null;
    }

    @Override
    public Void visitPrimitive2(Primitive2 primitive2) {
        return null;
    }

    @Override
    public Void visitProg(Prog prog) {
        return null;
    }

    @Override
    public Void visitRet(Ret ret) {
        return null;
    }

    @Override
    public Void visitSetVar(SetVar set) {
        return null;
    }

    @Override
    public Void visitVar(Var var) {
        ValueCategory category;
        if (methodProfile.isVarProfiled(var) && methodProfile.isPureInt(var)) {
            category = ValueCategory.INT;
        } else {
            category = ValueCategory.REFERENCE;
        }
        var.setCompilerAnnotation(new CompilerAnnotation(category));
        return null;
    }
}
