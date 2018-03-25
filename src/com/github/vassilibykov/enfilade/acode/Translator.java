// Copyright (c) 2018 Vassili Bykov. Licensed under the Apache License, Version 2.0.

package com.github.vassilibykov.enfilade.acode;

import com.github.vassilibykov.enfilade.core.CallNode;
import com.github.vassilibykov.enfilade.core.ConstNode;
import com.github.vassilibykov.enfilade.core.EvaluatorNode;
import com.github.vassilibykov.enfilade.core.IfNode;
import com.github.vassilibykov.enfilade.core.LetNode;
import com.github.vassilibykov.enfilade.core.Primitive1Node;
import com.github.vassilibykov.enfilade.core.Primitive2Node;
import com.github.vassilibykov.enfilade.core.BlockNode;
import com.github.vassilibykov.enfilade.core.ReturnNode;
import com.github.vassilibykov.enfilade.core.VariableReferenceNode;
import com.github.vassilibykov.enfilade.core.SetVariableNode;

import java.util.ArrayList;
import java.util.List;

/**
 * Translates an A-normal form expression into the equivalent A-code.
 */
public class Translator implements EvaluatorNode.Visitor<Void> {

    public static Instruction[] translate(EvaluatorNode functionBody) {
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
    public Void visitCall0(CallNode.Call0 call) {
        emit(new Call(call));
        return null;
    }

    @Override
    public Void visitCall1(CallNode.Call1 call) {
        emit(new Call(call));
        return null;
    }

    @Override
    public Void visitCall2(CallNode.Call2 call) {
        emit(new Call(call));
        return null;
    }

    @Override
    public Void visitConst(ConstNode aConst) {
        emit(new Load(aConst));
        return null;
    }

    @Override
    public Void visitIf(IfNode anIf) {
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
    public Void visitLet(LetNode let) {
        let.initializer().accept(this);
        let.setSetInstructionAddress(nextInstructionAddress());
        emit(new Store(let.variable()));
        let.body().accept(this);
        return null;
    }

    @Override
    public Void visitPrimitive1(Primitive1Node primitive) {
        emit(new Load(primitive));
        return null;
    }

    @Override
    public Void visitPrimitive2(Primitive2Node primitive) {
        emit(new Load(primitive));
        return null;
    }

    @Override
    public Void visitBlock(BlockNode block) {
        EvaluatorNode[] expressions = block.expressions();
        if (expressions.length == 0) {
            emit(new Load(new ConstNode(null)));
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
    public Void visitRet(ReturnNode ret) {
        ret.value().accept(this);
        emit(new Return());
        return null;
    }

    @Override
    public Void visitVarSet(SetVariableNode set) {
        set.value().accept(this);
        emit(new Store(set.variable()));
        return null;
    }

    @Override
    public Void visitVarRef(VariableReferenceNode varRef) {
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
