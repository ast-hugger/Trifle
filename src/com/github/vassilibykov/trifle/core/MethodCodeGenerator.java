// Copyright (c) 2018 Vassili Bykov. Licensed under the Apache License, Version 2.0.

package com.github.vassilibykov.trifle.core;

import com.github.vassilibykov.trifle.primitive.IfAware;
import org.objectweb.asm.Label;
import org.objectweb.asm.MethodVisitor;

import java.lang.invoke.MethodType;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Supplier;
import java.util.stream.Stream;

import static com.github.vassilibykov.trifle.core.JvmType.BOOL;
import static com.github.vassilibykov.trifle.core.JvmType.REFERENCE;

/**
 * Generates the "normal" executable representation of a function.
 *
 * @see RecoveryCodeGenerator
 */
class MethodCodeGenerator implements CodeGenerator {

    /**
     * An SPE handler to be generated once we are done with the method proper.
     */
    private static class SquarePegHandler {
        /**
         * The label to be set at the beginning of the handler code.
         */
        private final Label handlerStart;
        /**
         * Local variables which are live upon firing the handler. The list does
         * NOT include any parameters, since they are by definition always live.
         */
        private final List<AbstractVariable> liveLocals;
        /**
         * The label set in recovery code at the location where execution should
         * resume.
         */
        private final Label recoverySiteLabel;

        private SquarePegHandler(Label handlerStart, List<AbstractVariable> liveLocals) {
            this.handlerStart = handlerStart;
            this.liveLocals = liveLocals;
            this.recoverySiteLabel = new Label();
        }
    }

    /*
        Instance
     */

    private final FunctionImplementation function;
    private final GhostWriter writer;
    private final List<AbstractVariable> liveLocals = new ArrayList<>();
    private final List<SquarePegHandler> squarePegHandlers = new ArrayList<>();

    MethodCodeGenerator(FunctionImplementation function, MethodVisitor writer) {
        this.function = function;
        this.writer = new GhostWriter(writer);
    }

    @Override
    public GhostWriter writer() {
        return writer;
    }

    void generate() {
        generatePrologue();
        var bodyGist = function.body().accept(this);
        writer.bridgeValue(bodyGist.type(), function.specializedReturnType());
        /*
         * Here we don't care whether the body of the bridging of the result can fail.
         * There is no need to set up an SPE handler because if they do,
         * the unhandled SPE is a proper way to return the unreturnable value.
         */
        writer.ret(function.specializedReturnType());
        if (!squarePegHandlers.isEmpty()) {
            generateRecoveryHandlers();
            generateRecoveryCode();
        }
    }

    /**
     * For any function parameter which is boxed, replace the value passed in
     * with a box containing that value. This only concerns declared parameters;
     * any copied outer context values have already been boxed by the caller.
     */
    private void generatePrologue() {
        for (var each : function.declaredParameters()) {
            if (each.isBoxed()) {
                var paramType = each.specializedType();
                int index = each.index();
                writer
                    .loadLocal(paramType, index)
                    .initBoxedVariable(paramType, index);
            }
        }
    }

    private void generateRecoveryHandlers() {
        squarePegHandlers.forEach(this::generateRecoveryHandler);
    }

    private void generateRecoveryCode() {
        var generator = new RecoveryCodeGenerator(function, writer);
        generator.generate();
    }

    /*
        An overview of how values produced by visitor methods are handled.

        I believe the following are fundamental invariants. If a method doesn't
        do what they say it should, or does something they say it doesn't have
        to, the method is probably wrong.

        A visitor method always returns the JvmType left on the stack by the
        code the method generated.

        A visitor is free to generate a value of any type it wants; it is the
        visitor caller's (the parent expression visitor's) responsibility to
        bridge the value to the type it needs for its own code.

        Bridging may cause SPEs if it's type-narrowing. Thus a visitor method
        which performs bridging must make recovery provisions if it uses the
        value itself. Examples are the visitors of 'let', 'letrec', 'set!'
        and 'return'.

        If a complex subexpression code is generated and bridged in the tail
        position of the visitor code so the visitor does not use it itself,
        like the visitor of 'if' when it bridges each branch value to the
        union type of both branches, there is no need to handle a possible SPE,
        as the expression value is already produced and the SPE handler of
        the complex expression that accepts the value will take care of it.
     */

    @Override
    public Gist generateCode(EvaluatorNode node) {
        return node.accept(this);
    }

    @Override
    public Gist visitCall(CallNode call) {
        return call.dispatcher().generateCode(call, this);
    }

    @Override
    public MethodType generateArgumentLoad(CallNode call) {
        return call.match(new CallNode.ArityMatcher<>() {
            @Override
            public MethodType ifNullary() {
                var returnType = call.specializedType().representativeClass();
                return MethodType.methodType(returnType);
            }

            @Override
            public MethodType ifUnary(EvaluatorNode arg) {
                var argType = arg.accept(MethodCodeGenerator.this).type().representativeClass();
                var returnType = call.specializedType().representativeClass();
                return MethodType.methodType(returnType, argType);
            }

            @Override
            public MethodType ifBinary(EvaluatorNode arg1, EvaluatorNode arg2) {
                var arg1Type = arg1.accept(MethodCodeGenerator.this).type().representativeClass();
                var arg2Type = arg2.accept(MethodCodeGenerator.this).type().representativeClass();
                var returnType = call.specializedType().representativeClass();
                return MethodType.methodType(returnType, arg1Type, arg2Type);
            }
        });
    }

    @Override
    public Gist visitClosure(ClosureNode closure) {
        var copiedOuterVariables = closure.copiedOuterVariables;
        for (var copiedVar : copiedOuterVariables) {
            if (copiedVar.isBoxed()) {
                writer.loadLocal(REFERENCE, copiedVar.index());
            } else {
                JvmType variableType = copiedVar.specializedType();
                writer
                    .loadLocal(variableType, copiedVar.index())
                    .adaptValue(variableType, REFERENCE);
            }
        }
        writer.invokeDynamic(
            ClosureCreationInvokeDynamic.BOOTSTRAP,
            "createClosure",
            MethodType.genericMethodType(copiedOuterVariables.size()),
            closure.function().id());
        return Gist.INFALLIBLE_REFERENCE;
    }

    @Override
    public Gist visitConstant(ConstantNode aConst) {
        Object value = aConst.value();
        if (value instanceof Integer) {
            writer.loadInt((Integer) value);
        } else if (value instanceof String) {
            writer.loadString((String) value);
        } else if (value == null) {
            writer.loadNull();
        } else if (value instanceof Boolean) {
            writer.loadInt((Boolean) value ? 1 : 0);
        } else {
            throw new CompilerError("unexpected const value: " + value);
        }
        return Gist.infallible(aConst.specializedType());
    }

    @Override
    public Gist visitGetVar(GetVariableNode varRef) {
        var variable = varRef.variable();
        var varType = variable.specializedType();
        if (variable.isBoxed()) {
            writer
                .loadLocal(REFERENCE, variable.index())
                .unboxValue(varType);
        } else {
            writer.loadLocal(varType, variable.index());
        }
        return Gist.infallible(varType);
    }

    @Override
    public Gist visitIf(IfNode anIf) {
        var trueBranch = anIf.trueBranch();
        var falseBranch = anIf.falseBranch();
        var resultType = anIf.specializedType();
        // Generate an optimized 'if' form, if possible
        if (anIf.condition() instanceof PrimitiveNode) {
            var primitiveCall = (PrimitiveNode) anIf.condition();
            if (primitiveCall.implementation() instanceof IfAware) {
                var generator = (IfAware) primitiveCall.implementation();
                var maybeOptimized = generator.optimizedFormFor(primitiveCall);
                if (maybeOptimized.isPresent()) {
                    var optimized = maybeOptimized.get();
                    optimized.loadArguments(each -> each.accept(this));
                    boolean[] canFail = new boolean[2];
                    writer.withLabelAtEnd(end -> {
                        writer.withLabelAtEnd(elseStart -> {
                            writer.asm().visitJumpInsn(optimized.jumpInstruction(), elseStart);
                            var valueGist = trueBranch.accept(this);
                            canFail[0] = writer.bridgeValue(valueGist.type(), resultType) || valueGist.canFail();
                            writer.jump(end);
                        });
                        var valueGist = falseBranch.accept(this);
                        canFail[1] = writer.bridgeValue(valueGist.type(), resultType) || valueGist.canFail();
                    });
                    /*
                     * Above we carefully track the fallibility of both branches, but don't set up
                     * an SPE handler if there is a possibility of SPE. That is because both branches
                     * are in the tail position within the 'if' expression. The thrown SPE is a
                     * proper way to return an unreturnable value, as long as we indicate to
                     * the containing expression that the 'if' as a whole is fallible.
                     */
                    return Gist.of(resultType, canFail[0] || canFail[1]);
                }
            }
        }
        // General 'if' form
        var conditionType = anIf.condition().accept(this).type();
        writer.ensureValue(conditionType, BOOL);
        boolean[] canFail = new boolean[2];
        writer.ifThenElse(
            () -> {
                var valueGist = trueBranch.accept(this);
                canFail[0] = writer.bridgeValue(valueGist.type(), resultType) || valueGist.canFail();
            },
            () -> {
                var valueGist = falseBranch.accept(this);
                canFail[1] = writer.bridgeValue(valueGist.type(), resultType) || valueGist.canFail();
            }
        );
        // The note above about tracking fallibility applies here as well.
        return Gist.of(resultType, canFail[0] || canFail[1]);
    }

    @Override
    public Gist visitLet(LetNode let) {
        VariableDefinition variable = let.variable();
        JvmType varType = variable.specializedType();
        /*
         * The initializer may be a fallible expression, and unlike 'if'
         * branches, it is not in the tail position within the 'let'. Thus, we
         * must pay attention to its fallibility and if required, set up an SPE
         * handler to flip over to the generic code if the initial value does
         * not fit the specialization.
         */
        withSquarePegRecovery(let, () -> {
            var initGist = let.initializer().accept(this);
            var bridgeCanFail = writer.bridgeValue(initGist.type(), varType);
            return initGist.canFail() || bridgeCanFail;
        });
        if (variable.isBoxed()) {
            writer.initBoxedVariable(varType, variable.index());
        } else {
            writer.storeLocal(varType, variable.index());
        }
        liveLocals.add(variable);
        var bodyGist = let.body().accept(this);
        liveLocals.remove(variable);
        return bodyGist;
    }

    @Override
    public Gist visitPrimitive1(Primitive1Node primitive) {
        // Primitive arguments are atomic and therefore always infallible; no need to check.
        var argGist = primitive.argument().accept(this);
        var type = primitive.implementation().generate(writer, argGist.type());
        return Gist.infallible(type);
    }

    @Override
    public Gist visitPrimitive2(Primitive2Node primitive) {
        // Primitive arguments are atomic and therefore always infallible; no need to check.
        var gist1 = primitive.argument1().accept(this);
        var gist2 = primitive.argument2().accept(this);
        var type = primitive.implementation().generate(writer, gist1.type(), gist2.type());
        return Gist.infallible(type);
    }

    @Override
    public Gist visitBlock(BlockNode block) {
        EvaluatorNode[] expressions = block.expressions();
        if (expressions.length == 0) {
            writer.loadNull();
            return Gist.INFALLIBLE_REFERENCE;
        }
        int i;
        for (i = 0; i < expressions.length - 1; i++) {
            expressions[i].accept(this);
            writer.pop();
        }
        return expressions[i].accept(this);
    }

    @Override
    public Gist visitReturn(ReturnNode returnNode) {
        /*
         * Unlike the implicit return at the end of a function which does not
         * need an SPE handler, an explicit return is more like a 'set'
         * expression. We must establish a handler if there is a possibility of
         * a failure. The return value is atomic and in principle there is no
         * need to place its code within an SPE handler. But just in case we
         * might lift that restriction and forget to modify the code here
         * accordingly, we track its fallibility.
         */
        withSquarePegRecovery(returnNode, () -> {
            var valueGist = returnNode.value().accept(this);
            var bridgeCanFail = writer.bridgeValue(valueGist.type(), function.specializedReturnType());
            return valueGist.canFail() || bridgeCanFail;
        });
        writer.ret(function.specializedReturnType());
        return Gist.INFALLIBLE_VOID;
    }

    @Override
    public Gist visitSetVar(SetVariableNode set) {
        var var = set.variable();
        var varType = var.specializedType();
        withSquarePegRecovery(set, () -> {
            var valueGist = set.value().accept(this);
            var bridgingCanFail = writer.bridgeValue(valueGist.type(), varType);
            return valueGist.canFail() && bridgingCanFail;
        });
        writer.dup(); // the duplicate is left on the stack as the expression value
        if (var.isBoxed()) {
            writer.storeBoxedVariable(varType, var.index());
        } else {
            writer.storeLocal(varType, var.index());
        }
        return Gist.infallible(varType);
    }

    @Override
    public Gist visitFreeFunctionReference(FreeFunctionReferenceNode reference) {
        return reference.generateLoad(writer);
    }

    @Override
    public Gist visitWhile(WhileNode whileNode) {
        writer.loadNull();
        writer.withLabelsAround((start, end) -> {
            var conditionGist = whileNode.condition().accept(this);
            writer.ensureValue(conditionGist.type(), BOOL);
            writer.jumpIf0(end);
            writer.pop(); // the prior iteration result or the initial null
            var bodyGist = whileNode.body().accept(this);
            writer.bridgeValue(bodyGist.type(), REFERENCE);
            writer.jump(start);
        });
        // TODO The loop as generated here is always treated as if of a reference type.
        // Perhaps we can do better.
        return Gist.INFALLIBLE_REFERENCE;
    }

    /**
     * Generate a fragment of code that may throw an SPE which must be
     * recovered from.
     *
     * @param requestor The evaluator node which contains the continuation
     *        receiving the value of the generated code.
     * @param generate Generates the code and returns an indication of
     *        whether an SPE is possible in the code as it was generated.
     */
    private void withSquarePegRecovery(RecoverySite requestor, Supplier<Boolean> generate) {
        writer.withLabelsAround((begin, end) -> {
            var spePossible = generate.get();
            if (spePossible) {
                Label handlerStart = new Label();
                SquarePegHandler handler = new SquarePegHandler(
                    handlerStart,
                    new ArrayList<>(liveLocals));
                requestor.setRecoverySiteLabel(handler.recoverySiteLabel);
                squarePegHandlers.add(handler);
                writer.handleSquarePegException(begin, end, handlerStart);
            }
        });
    }

    /**
     * Generate the code of an exception handler for recovering from an SPE. The
     * handler should unwrap the SPE currently on the stack and unspecialize any
     * specialized live locals, then jump to the continuation location in the
     * generic code. The unwrapped value of the SPE should be the only value on
     * the stack when jumping.
     */
    private void generateRecoveryHandler(SquarePegHandler handler) {
        // stack: SquarePegException
        writer
            .setLabelHere(handler.handlerStart)
            .unwrapSPE();
        // stack: continuation value
        Stream.concat(Stream.of(function.allParameters()), handler.liveLocals.stream()).forEach(var -> {
            var varType = var.specializedType();
            if (!var.isBoxed() && varType != REFERENCE) {
                writer
                    .loadLocal(varType, var.index())
                    .adaptValue(varType, REFERENCE)
                    .storeLocal(REFERENCE, var.index());
            }
        });
        writer.jump(handler.recoverySiteLabel);
    }
}
