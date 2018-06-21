// Copyright (c) 2018 Vassili Bykov. Licensed under the Apache License, Version 2.0.

package com.github.vassilibykov.trifle.core;

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
        output.append(" [").append(Objects.toString(node.inferredType()));
        output.append(" ").append(Objects.toString(node.specializedType()));
    }

    @Override
    public Void visitBlock(BlockNode block) {
        throw new UnsupportedOperationException("not implemented yet"); // TODO implement
    }

    @Override
    public Void visitCall(CallNode call) {
        printLine(() -> {
            output.append(call.toString());
            printNodeProfile(call);
        });
        int[] i = {0};
        call.arguments().forEach(each -> {
            printLine(() -> output.append("arg").append(String.valueOf(i[0]++)).append(":"));
            indented(() -> each.accept(NodePrettyPrinter.this));
        });
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
    public Void visitConstant(ConstantNode aConst) {
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
        printLet("let", let);
        return null;
    }

    private void printLet(String keyword, LetNode letOrLetrec) {
        printLine(() -> {
            output
                .append(keyword).append(" ")
                .append(letOrLetrec.variable().toString());
            printNodeProfile(letOrLetrec);
        });
        printLine(() -> output.append("initExpr:"));
        indented(() -> letOrLetrec.initializer().accept(this));
        printLine(() -> output.append("body:"));
        indented(() -> letOrLetrec.body().accept(this));
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
    public Void visitReturn(ReturnNode ret) {
        throw new UnsupportedOperationException("not implemented yet"); // TODO implement
    }

    @Override
    public Void visitSetVar(SetVariableNode set) {
        throw new UnsupportedOperationException("not implemented yet"); // TODO implement
    }

    @Override
    public Void visitFreeFunctionReference(FreeFunctionReferenceNode topLevelBinding) {
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

    @Override
    public Void visitWhile(WhileNode whileNode) {
        printLine(() -> {
            output.append("while ");
            printNodeProfile(whileNode);
        });
        printLine(() -> output.append("condition:"));
        indented(() -> whileNode.condition().accept(this));
        printLine(() -> output.append("body: ").append(Objects.toString(whileNode.bodyCount.get())));
        indented(() -> whileNode.body().accept(this));
        return null;
    }
}
