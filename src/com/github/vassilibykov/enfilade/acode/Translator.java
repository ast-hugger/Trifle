// Copyright (c) 2018 Vassili Bykov. Licensed under the Apache License, Version 2.0.

package com.github.vassilibykov.enfilade.acode;

import com.github.vassilibykov.enfilade.core.Call0;
import com.github.vassilibykov.enfilade.core.Call1;
import com.github.vassilibykov.enfilade.core.Call2;
import com.github.vassilibykov.enfilade.core.Const;
import com.github.vassilibykov.enfilade.core.Expression;
import com.github.vassilibykov.enfilade.core.If;
import com.github.vassilibykov.enfilade.core.Let;
import com.github.vassilibykov.enfilade.core.Primitive1;
import com.github.vassilibykov.enfilade.core.Primitive2;
import com.github.vassilibykov.enfilade.core.Prog;
import com.github.vassilibykov.enfilade.core.Ret;
import com.github.vassilibykov.enfilade.core.VarRef;
import com.github.vassilibykov.enfilade.core.VarSet;

import java.util.ArrayList;
import java.util.List;

/**
 * Translates an A-normal form expression into the equivalent A-code.
 */
public class Translator implements Expression.Visitor<Void> {

    public static Instruction[] translate(Expression expression) {
        Translator translator = new Translator();
        expression.accept(translator);
        return translator.code.toArray(new Instruction[translator.code.size()]);
    }

    /*
        Instance
     */

    private final List<Instruction> code = new ArrayList<>();

    private Translator() {}

    @Override
    public Void visitCall0(Call0 call) {
        code.add(new Call(call));
        return null;
    }

    @Override
    public Void visitCall1(Call1 call) {
        code.add(new Call(call));
        return null;
    }

    @Override
    public Void visitCall2(Call2 call) {
        code.add(new Call(call));
        return null;
    }

    @Override
    public Void visitConst(Const aConst) {
        code.add(new Load(aConst));
        return null;
    }

    @Override
    public Void visitIf(If anIf) {
        Branch branch = new Branch(anIf.condition(), Integer.MAX_VALUE);
        code.add(branch);
        anIf.falseBranch().accept(this);
        Goto theGoto = new Goto(Integer.MAX_VALUE);
        code.add(theGoto);
        branch.address = code.size();
        anIf.trueBranch().accept(this);
        theGoto.address = code.size();
        return null;
    }

    @Override
    public Void visitLet(Let let) {
        let.initializer().accept(this);
        code.add(new Store(let.variable()));
        let.body().accept(this);
        return null;
    }

    @Override
    public Void visitPrimitive1(Primitive1 primitive) {
        code.add(new Load(primitive));
        return null;
    }

    @Override
    public Void visitPrimitive2(Primitive2 primitive) {
        code.add(new Load(primitive));
        return null;
    }

    @Override
    public Void visitProg(Prog prog) {
        Expression[] expressions = prog.expressions();
        if (expressions.length == 0) {
            code.add(new Load(new Const(null)));
            return null;
        }
        for (int i = 0; i < expressions.length - 1; i++) {
            expressions[i].accept(this);
            code.add(new Drop());
        }
        expressions[expressions.length - 1].accept(this);
        return null;
    }

    @Override
    public Void visitRet(Ret ret) {
        ret.value().accept(this);
        code.add(new Return());
        return null;
    }

    @Override
    public Void visitVarSet(VarSet set) {
        set.value().accept(this);
        code.add(new Store(set.variable()));
        return null;
    }

    @Override
    public Void visitVarRef(VarRef varRef) {
        code.add(new Load(varRef));
        return null;
    }
}
