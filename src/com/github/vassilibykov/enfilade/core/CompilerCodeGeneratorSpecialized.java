// Copyright (c) 2018 Vassili Bykov. Licensed under the Apache License, Version 2.0.

package com.github.vassilibykov.enfilade.core;

import com.github.vassilibykov.enfilade.primitives.LessThan;
import org.jetbrains.annotations.TestOnly;
import org.objectweb.asm.Label;
import org.objectweb.asm.MethodVisitor;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static com.github.vassilibykov.enfilade.core.JvmType.BOOL;
import static com.github.vassilibykov.enfilade.core.JvmType.INT;
import static com.github.vassilibykov.enfilade.core.JvmType.REFERENCE;
import static com.github.vassilibykov.enfilade.core.JvmType.VOID;

class CompilerCodeGeneratorSpecialized implements EvaluatorNode.Visitor<JvmType> {
    private final FunctionImplementation function;
    private final GhostWriter writer;
    private final Deque<JvmType> continuationTypes = new ArrayDeque<>();
    private final List<AbstractVariable> liveLocals = new ArrayList<>();
    private final List<SquarePegHandler> squarePegHandlers = new ArrayList<>();
    private final JvmType functionReturnType;

    private static class SquarePegHandler {
        private final Label handlerStart;
        private final List<AbstractVariable> capturedLocals;
        private final int acodeInitialPC;

        private SquarePegHandler(Label handlerStart, List<AbstractVariable> capturedLocals, int acodeInitialPC) {
            this.handlerStart = handlerStart;
            this.capturedLocals = capturedLocals;
            this.acodeInitialPC = acodeInitialPC;
        }
    }

    /**
     * Sets {@link VariableDefinition#specializedIndex} fields in an evaluator
     * node tree. Specialized indices have to be set so that variables of
     * different types are not mapped onto the same local index.
     *
     * <p>There is no need to validate scope of variable uses because it's
     * already been done by the generic indexer.
     */
    static class VariableIndexer extends EvaluatorNode.VisitorSkeleton<Void> {

        private class IndexPool {
            private final List<Integer> availableIndices = new ArrayList<>();
            private int currentIndex = 0;

            public int allocate() {
                if (currentIndex == availableIndices.size()) {
                    availableIndices.add(nextGlobalIndex++);
                }
                return availableIndices.get(currentIndex++);
            }

            public void release() {
                currentIndex--;
            }
        }

        private int nextGlobalIndex;
        private Map<JvmType, IndexPool> poolsByType = new HashMap<>();

        VariableIndexer(int startingIndex) {
            this.nextGlobalIndex = startingIndex;
            poolsByType.put(JvmType.REFERENCE, new IndexPool());
            poolsByType.put(JvmType.INT, new IndexPool());
            poolsByType.put(JvmType.BOOL, new IndexPool());
        }

        public int frameSize() {
            return nextGlobalIndex;
        }

        @Override
        public Void visitLet(LetNode let) {
            let.initializer().accept(this);
            JvmType varType = let.variable().specializationType();
            IndexPool pool = poolsByType.get(varType);
            let.variable().specializedIndex = pool.allocate();
            let.body().accept(this);
            pool.release();
            return null;
        }

        @TestOnly
        int allocate(JvmType type) {
            return poolsByType.get(type).allocate();
        }

        @TestOnly
        void release(JvmType type) {
            poolsByType.get(type).release();
        }
    }

    /*
        Instance
     */

    CompilerCodeGeneratorSpecialized(FunctionImplementation function, MethodVisitor writer) {
        this.function = function;
        this.functionReturnType = function.body().specializationType();
        this.writer = new GhostWriter(writer);
    }

    public GhostWriter writer() {
        return writer;
    }

    void generate() {
        function.acode = ACodeTranslator.translate(function.body());
        continuationTypes.push(functionReturnType);
        VariableIndexer variableIndexer = new VariableIndexer(function.arity());
        function.body().accept(variableIndexer);
        function.body().accept(this);
        continuationTypes.pop();
        writer.ret(functionReturnType);
        if (!squarePegHandlers.isEmpty()) {
            Label epilogue = new Label();
            for (int i = 0; i < squarePegHandlers.size(); i++) {
                boolean isLastHandler = i == squarePegHandlers.size() - 1;
                generateSquarePegHandler(squarePegHandlers.get(i), isLastHandler, epilogue);
            }
            generateEpilogue(epilogue);
        }
    }

    private JvmType currentContinuationType() {
        return continuationTypes.peek();
    }

    @Override
    public JvmType visitCall0(CallNode.Call0 call) {
        throw new UnsupportedOperationException("not implemented yet"); // TODO implement
//        int id = FunctionRegistry.INSTANCE.lookup(call.function().implementation);
//        JvmType returnType = call.specializationType();
//        MethodType callSiteType = MethodType.methodType(returnType.representativeClass());
//        writer.invokeDynamic(
//            DirectCall.BOOTSTRAP,
//            "call#" + id,
//            callSiteType,
//            id);
//        assertPassage(returnType, currentContinuationType());
//        return null;
    }

    @Override
    public JvmType visitCall1(CallNode.Call1 call) {
        // FIXME this (and the 2-arg version) will fail if arguments are specialized so the call site
        // has a non-generic signature, but the specialization available in the nexus has a different signature.
        // We'll need to revise the scheme of managing implementations and call sites in FunctionImplementation
        // to fix this.
        throw new UnsupportedOperationException("not implemented yet"); // TODO implement
//        call.arg().accept(this);
//        int id = FunctionRegistry.INSTANCE.lookup(call.function().implementation);
//        JvmType returnType = call.specializationType();
//        MethodType callSiteType = MethodType.methodType(
//            returnType.representativeClass(),
//            call.arg().specializationType().representativeClass());
//        writer.invokeDynamic(
//            DirectCall.BOOTSTRAP,
//            "call#" + id,
//            callSiteType,
//            id);
//        assertPassage(returnType, currentContinuationType());
//        return null;
    }

    @Override
    public JvmType visitCall2(CallNode.Call2 call) {
        throw new UnsupportedOperationException("not implemented yet"); // TODO implement
//        call.arg1().accept(this);
//        call.arg2().accept(this);
//        int id = FunctionRegistry.INSTANCE.lookup(call.function().implementation);
//        JvmType returnType = call.specializationType();
//        MethodType callSiteType = MethodType.methodType(
//            returnType.representativeClass(),
//            call.arg1().specializationType().representativeClass(),
//            call.arg2().specializationType().representativeClass());
//        writer.invokeDynamic(
//            DirectCall.BOOTSTRAP,
//            "call#" + id,
//            callSiteType,
//            id);
//        assertPassage(returnType, currentContinuationType());
//        return null;
    }

    @Override
    public JvmType visitClosure(ClosureNode closure) {
        throw new UnsupportedOperationException("not implemented yet"); // TODO implement
    }

    @Override
    public JvmType visitConst(ConstNode aConst) {
        Object value = aConst.value();
        if (value instanceof Integer) {
            writer.loadInt((Integer) value);
            assertPassage(INT, currentContinuationType());
        } else if (value instanceof String) {
            writer.loadString((String) value);
            assertPassage(REFERENCE, currentContinuationType());
        } else {
            throw new CompilerError("unexpected const value: " + value);
        }
        return null;
    }

    @Override
    public JvmType visitIf(IfNode anIf) {
        if (anIf.condition() instanceof LessThan) {
            ((LessThan) anIf.condition()).generateIf(
                (type, arg) -> generateExpecting(type, arg),
                () -> generateForCurrentContinuation(anIf.trueBranch()),
                () -> generateForCurrentContinuation(anIf.falseBranch()),
                writer);
            return null;
        }
        generateExpecting(BOOL, anIf.condition());
        writer.ifThenElse(
            () -> generateForCurrentContinuation(anIf.trueBranch()),
            () -> generateForCurrentContinuation(anIf.falseBranch())
        );
        return null;
    }

    @Override
    public JvmType visitLet(LetNode let) {
        VariableDefinition var = let.variable();
        JvmType varType = var.specializationType();
        if (varType == REFERENCE) {
            generateExpecting(REFERENCE, let.initializer());
        } else {
            withSquarePegRecovery(let, () -> generateExpecting(varType, let.initializer()));
        }
        writer.storeLocal(varType, var.specializedIndex());
        liveLocals.add(var);
        generateForCurrentContinuation(let.body());
        liveLocals.remove(var);
        return null;
    }

    @Override
    public JvmType visitPrimitive1(Primitive1Node primitive) {
        primitive.argument().accept(this);
        primitive.generate(writer, primitive.argument().specializationType());
        assertPassage(primitive.jvmType(), currentContinuationType());
        return null;
    }

    @Override
    public JvmType visitPrimitive2(Primitive2Node primitive) {
        primitive.argument1().accept(this);
        primitive.argument2().accept(this);
        primitive.generate(
            writer,
            primitive.argument1().specializationType(),
            primitive.argument2().specializationType());
        assertPassage(primitive.jvmType(), currentContinuationType());
        return null;
    }

    @Override
    public JvmType visitBlock(BlockNode block) {
        EvaluatorNode[] expressions = block.expressions();
        if (expressions.length == 0) {
            writer
                .loadNull()
                .adaptType(REFERENCE, currentContinuationType());
            return null;
        }
        int i;
        for (i = 0; i < expressions.length - 1; i++) {
            EvaluatorNode expr = expressions[i];
            generateExpecting(VOID, expr);
            writer.pop();
        }
        generateForCurrentContinuation(expressions[i]);
        return null;
    }

    @Override
    public JvmType visitRet(ReturnNode ret) {
        throw new UnsupportedOperationException("not implemented yet"); // TODO implement
    }

    @Override
    public JvmType visitSetVar(SetVariableNode set) {
        var var = set.variable();
        JvmType varType = var.specializationType();
        generateExpecting(varType, set.value());
        writer
            .dup()
            .storeLocal(varType, var.specializedIndex());
        return null;
    }

    @Override
    public JvmType visitTopLevelFunction(TopLevelFunctionNode topLevelBinding) {
        throw new UnsupportedOperationException("not implemented yet"); // TODO implement
    }

    @Override
    public JvmType visitGetVar(GetVariableNode varRef) {
        JvmType varType = varRef.variable().specializationType();
        writer.loadLocal(varType, varRef.variable().specializedIndex());
        assertPassage(varType, currentContinuationType());
        return null;
    }

    private void generateExpecting(JvmType expectedType, EvaluatorNode expression) {
        continuationTypes.push(expectedType);
        expression.accept(this);
        continuationTypes.pop();
    }

    private void generateForCurrentContinuation(EvaluatorNode expression) {
        expression.accept(this);
    }

    /**
     * Assuming that a value of type 'from' is on the stack in the context
     * whose continuation expects a value of type 'to', generate code that will
     * ensure the continuation will successfully receive the value.
     *
     * <p>If the from/to pair of types is such that a value of 'from' cannot in
     * the general case be converted to a value of 'to', for example {@code
     * reference -> int}, the generated code will throw an exception to complete
     * the evaluation in emergency mode.
     *
     * <p>If the 'to' type is VOID, that means the value passed to the continuation
     * will be discarded. In that case it can be anything.
     */
    private void assertPassage(JvmType from, JvmType to) {
        from.match(new JvmType.VoidMatcher() {
            public void ifReference() {
                to.match(new JvmType.VoidMatcher() {
                    public void ifReference() { }
                    public void ifInt() { writer.throwSquarePegException(); }
                    public void ifBoolean() { writer.throwSquarePegException(); }
                    public void ifVoid() { }
                });
            }
            public void ifInt() {
                to.match(new JvmType.VoidMatcher() {
                    public void ifReference() { writer.boxInteger(); }
                    public void ifInt() { }
                    public void ifBoolean() { writer.boxInteger().throwSquarePegException(); }
                    public void ifVoid() { }
                });
            }
            public void ifBoolean() {
                to.match(new JvmType.VoidMatcher() {
                    public void ifReference() { writer.boxBoolean(); }
                    public void ifInt() { writer.boxBoolean().throwSquarePegException(); }
                    public void ifBoolean() { }
                    public void ifVoid() { }
                });
            }
        });
    }

    private void withSquarePegRecovery(LetNode requestor, Runnable generate) {
        Label handlerStart = new Label();
        SquarePegHandler handler = new SquarePegHandler(
            handlerStart,
            new ArrayList<>(liveLocals),
            requestor.setInstructionAddress());
        squarePegHandlers.add(handler);
        writer.withLabelsAround((begin, end) -> {
            generate.run();
            writer.handleSquarePegException(begin, end, handlerStart);
        });
    }

    /**
     * Generate code that loads onto the stack a replica of the frame locals
     * live for the specified handler (an Object[]) and the restart position in
     * A-code, then jumps to the epilogue unless this is the last handler.
     */
    private void generateSquarePegHandler(SquarePegHandler handler, boolean isLastHandler, Label epilogueStart) {
        // TODO: 3/23/18 an optimization opportunity
        // Each handler is currently loading the entire set of its live locals, as
        // generated by generateFrameReplicator(). These sets often have common subsets.
        // For example, each of them includes the function arguments. A smarter approach
        // would be to detect these commonalities and factor them out similarly to how the
        // function epilogue is factored out.
        writer.asm().visitLabel(handler.handlerStart);
        // stack: SquarePegException
        writer.loadInt(handler.acodeInitialPC);
        generateFrameReplicator(handler);
        // stack: SPE, int, Object[]
        if (!isLastHandler) writer.jump(epilogueStart);
    }

    /**
     * Generate a code fragment creating an object array of size equal to the
     * function's locals count and populating it with the values of currently
     * live local variables. The array is left on the stack.
     */
    private void generateFrameReplicator(SquarePegHandler handler) {
        int size = function.frameSize();
        writer.newObjectArray(size);
        handler.capturedLocals.forEach(this::storeInFrameReplica);
    }

    private void storeInFrameReplica(AbstractVariable local) {
        JvmType localType = local.specializationType();
        writer.storeArray(local.specializedIndex(), () -> {
            writer.loadLocal(localType, local.specializedIndex());
            writer.adaptType(localType, REFERENCE);
        });
    }

    private void generateEpilogue(Label epilogueStart) {
        writer.asm().visitLabel(epilogueStart);
        // stack: SPE, int initialPC, Object[] frame
        function.parameters().forEach(this::storeInFrameReplica);
        // stack: SPE, int initialPC, Object[] frame
        writer
            .loadInt(FunctionRegistry.INSTANCE.lookup(function))
            // stack: SPE, int initialPC, Object[] frame, int functionId
            .invokeStatic(ACodeInterpreter.class, "forRecovery", ACodeInterpreter.class, int.class, Object[].class, int.class)
            // stack: SPE, Interpreter
            .swap()
            // stack: Interpreter, SPE
            .invokeVirtual(SquarePegException.class, "value", Object.class)
            // stack: Interpreter, Object
            .invokeVirtual(ACodeInterpreter.class, "interpret", Object.class, Object.class);
        functionReturnType.match(new JvmType.VoidMatcher() {
            @Override
            public void ifReference() {
                writer.ret(REFERENCE);
            }

            @Override
            public void ifInt() {
                writer.dup();
                writer.instanceOf(Integer.class);
                writer.ifThenElse(
                    () -> {
                        writer.checkCast(Integer.class);
                        writer.invokeVirtual(Integer.class, "intValue", int.class);
                        writer.ret(INT);
                    },
                    () -> {
                        writer.throwSquarePegException();
                    }
                );
            }

            @Override
            public void ifBoolean() {
                writer.dup();
                writer.instanceOf(Boolean.class);
                writer.ifThenElse(
                    () -> {
                        writer.checkCast(Boolean.class);
                        writer.invokeVirtual(Boolean.class, "booleanValue", boolean.class);
                        writer.ret(BOOL);
                    },
                    () -> {
                        writer.throwSquarePegException();
                    }
                );
            }
        });
    }
}
