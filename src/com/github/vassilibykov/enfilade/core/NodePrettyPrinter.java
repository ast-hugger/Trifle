// Copyright (c) 2018 Vassili Bykov. Licensed under the Apache License, Version 2.0.

package com.github.vassilibykov.enfilade.core;

import java.io.PrintWriter;
import java.util.Objects;

public class NodePrettyPrinter implements EvaluatorNode.Visitor<Void> {
    public static void print(EvaluatorNode root) {
        PrintWriter output = new PrintWriter(System.out);
        NodePrettyPrinter printer = new NodePrettyPrinter(output);
        root.accept(printer);
        output.flush();
    }

    private final PrintWriter output;
    private int indent = 0;

    public NodePrettyPrinter(PrintWriter output) {
        this.output = output;
    }

    private void indented(Runnable action) {
        indent++;
        action.run();
        indent--;
    }

    private void printIndent() {
        for (int i = 0; i < indent; i++) {
            output.append(".   ");
        }
    }

    private void printLine(Runnable action) {
        printIndent();
        action.run();
        output.append('\n');
    }

    private void printNodeProfile(EvaluatorNode node) {
        output.append(" [").append(node.inferredType().toString());
        output.append(" ").append(node.observedType().toString());
        output.append("] ").append(node.specializationType().toString());
    }

    @Override
    public Void visitBlock(BlockNode block) {
        throw new UnsupportedOperationException("not implemented yet"); // TODO implement
    }

    @Override
    public Void visitCall0(CallNode.Call0 call) {
        printLine(() -> {
            output.append(call.toString());
            printNodeProfile(call);
        });
        return null;
    }

    @Override
    public Void visitCall1(CallNode.Call1 call) {
        printLine(() -> {
            output.append(call.toString());
            printNodeProfile(call);
        });
        printLine(() -> output.append("arg:"));
        indented(() -> call.arg().accept(this));
        return null;
    }

    @Override
    public Void visitCall2(CallNode.Call2 call) {
        printLine(() -> {
            output.append(call.toString());
            printNodeProfile(call);
        });
        printLine(() -> output.append("arg1:"));
        indented(() -> call.arg1().accept(this));
        printLine(() -> output.append("arg2:"));
        indented(() -> call.arg2().accept(this));
        return null;
    }

    @Override
    public Void visitClosure(ClosureNode closure) {
        printLine(() -> {
            output.append("closure");
            printNodeProfile(closure);
        });
        indented(() -> closure.function().body().accept(this));
        return null;
    }

    @Override
    public Void visitConst(ConstNode aConst) {
        printLine(() -> {
            output
                .append("const ")
                .append(aConst.value().toString());
            printNodeProfile(aConst);
        });
        return null;
    }

    @Override
    public Void visitIf(IfNode anIf) {
        printLine(() -> {
            output.append("if ");
            printNodeProfile(anIf);
        });
        printLine(() -> output.append("condition:"));
        indented(() -> anIf.condition().accept(this));
        printLine(() -> output.append("trueBranch: ").append(Objects.toString(anIf.trueBranchCount.get())));
        indented(() -> anIf.trueBranch().accept(this));
        printLine(() -> output.append("falseBranch: ").append(Objects.toString(anIf.falseBranchCount.get())));
        indented(() -> anIf.falseBranch().accept(this));
        return null;
    }

    @Override
    public Void visitLet(LetNode let) {
        printLine(() -> {
            output
                .append("let ")
                .append(let.variable().toString());
            printNodeProfile(let);
        });
        printLine(() -> output.append("initExpr:"));
        indented(() -> let.initializer().accept(this));
        printLine(() -> output.append("body:"));
        indented(() -> let.body().accept(this));
        return null;
    }

    @Override
    public Void visitPrimitive1(Primitive1Node primitive) {
        printLine(() -> {
            output.append(primitive.toString());
            printNodeProfile(primitive);
        });
        printLine(() -> output.append("arg:"));
        indented(() -> primitive.argument().accept(this));
        return null;
    }

    @Override
    public Void visitPrimitive2(Primitive2Node primitive) {
        printLine(() -> {
            output.append(primitive.toString());
            printNodeProfile(primitive);
        });
        printLine(() -> output.append("arg1:"));
        indented(() -> primitive.argument1().accept(this));
        printLine(() -> output.append("arg2:"));
        indented(() -> primitive.argument2().accept(this));
        return null;
    }

    @Override
    public Void visitRet(ReturnNode ret) {
        throw new UnsupportedOperationException("not implemented yet"); // TODO implement
    }

    @Override
    public Void visitSetVar(SetVariableNode set) {
        throw new UnsupportedOperationException("not implemented yet"); // TODO implement
    }

    @Override
    public Void visitTopLevelFunction(TopLevelFunctionNode topLevelBinding) {
        throw new UnsupportedOperationException("not implemented yet"); // TODO implement
    }

    @Override
    public Void visitGetVar(GetVariableNode varRef) {
        printLine(() -> {
            output.append(varRef.toString());
            printNodeProfile(varRef);
        });
        return null;
    }
}
