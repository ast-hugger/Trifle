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
import com.github.vassilibykov.enfilade.core.Block;
import com.github.vassilibykov.enfilade.core.Ret;
import com.github.vassilibykov.enfilade.core.VarRef;
import com.github.vassilibykov.enfilade.core.VarSet;

import java.util.ArrayList;
import java.util.List;

/**
 * Translates an A-normal form expression into the equivalent A-code.
 */
public class Translator implements Expression.Visitor<Void> {

    public static Instruction[] translate(Expression functionBody) {
        Translator translator = new Translator();
        functionBody.accept(translator);
        translator.emit(new Return());
        return translator.code.toArray(new Instruction[translator.code.size()]);
    }

    /*
        Instance
     */

    private final List<Instruction> code = new ArrayList<>();

    private Translator() {}

    @Override
    public Void visitCall0(Call0 call) {
        emit(new Call(call));
        return null;
    }

    @Override
    public Void visitCall1(Call1 call) {
        emit(new Call(call));
        return null;
    }

    @Override
    public Void visitCall2(Call2 call) {
        emit(new Call(call));
        return null;
    }

    @Override
    public Void visitConst(Const aConst) {
        emit(new Load(aConst));
        return null;
    }

    @Override
    public Void visitIf(If anIf) {
        Branch branch = new Branch(anIf.condition(), Integer.MAX_VALUE);
        emit(branch);
        anIf.falseBranch().accept(this);
        Goto theGoto = new Goto(Integer.MAX_VALUE);
        emit(theGoto);
        branch.address = nextInstructionAddress();
        anIf.trueBranch().accept(this);
        theGoto.address = nextInstructionAddress();
        return null;
    }

    @Override
    public Void visitLet(Let let) {
        let.initializer().accept(this);
        let.compilerAnnotation().setAcodeBookmark(nextInstructionAddress());
        emit(new Store(let.variable()));
        let.body().accept(this);
        return null;
    }

    @Override
    public Void visitPrimitive1(Primitive1 primitive) {
        emit(new Load(primitive));
        return null;
    }

    @Override
    public Void visitPrimitive2(Primitive2 primitive) {
        emit(new Load(primitive));
        return null;
    }

    @Override
    public Void visitBlock(Block block) {
        Expression[] expressions = block.expressions();
        if (expressions.length == 0) {
            emit(new Load(new Const(null)));
            return null;
        }
        for (int i = 0; i < expressions.length - 1; i++) {
            expressions[i].accept(this);
            emit(new Drop());
        }
        expressions[expressions.length - 1].accept(this);
        return null;
    }

    @Override
    public Void visitRet(Ret ret) {
        ret.value().accept(this);
        emit(new Return());
        return null;
    }

    @Override
    public Void visitVarSet(VarSet set) {
        set.value().accept(this);
        emit(new Store(set.variable()));
        return null;
    }

    @Override
    public Void visitVarRef(VarRef varRef) {
        emit(new Load(varRef));
        return null;
    }

    private void emit(Instruction instruction) {
        code.add(instruction);
    }

    private int nextInstructionAddress() {
        return code.size();
    }
}
