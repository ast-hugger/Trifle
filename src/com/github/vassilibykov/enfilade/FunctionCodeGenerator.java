package com.github.vassilibykov.enfilade;

import org.objectweb.asm.Label;
import org.objectweb.asm.MethodVisitor;

import static org.objectweb.asm.Opcodes.*;

public class FunctionCodeGenerator implements Expression.Visitor<ValueCategory> {

    private static final String BOOLEAN = "java/lang/Boolean";
    private static final String INTEGER = "java/lang/Integer";

    private static final String TO_BOOL = "()Z";
    private static final String INT_TO_INTEGER = "(I)Ljava/lang/Integer;";
    private static final String OBJECT_TO_OBJECT = "(Ljava/lang/Object;)Ljava/lang/Object;";
    private static final String OBJECT2_TO_OBJECT = "(Ljava/lang/Object;Ljava/lang/Object;)Ljava/lang/Object;";

    private final MethodVisitor writer;

    public FunctionCodeGenerator(MethodVisitor writer) {
        this.writer = writer;
    }

    @Override
    public ValueCategory visitCall0(Call0 call) {
        int id = FunctionRegistry.INSTANCE.lookup(call.function());
        writer.visitInvokeDynamicInsn(
            "call0",
            "()Ljava/lang/Object;",
            DirectCall.BOOTSTRAP,
            id);
        return null;
    }

    @Override
    public ValueCategory visitCall1(Call1 call) {
        call.arg().accept(this);
        int id = FunctionRegistry.INSTANCE.lookup(call.function());
        writer.visitInvokeDynamicInsn(
            "call1",
            "(Ljava/lang/Object;)Ljava/lang/Object;",
            DirectCall.BOOTSTRAP,
            id);
        return null;
    }

    @Override
    public ValueCategory visitCall2(Call2 call) {
        call.arg1().accept(this);
        call.arg2().accept(this);
        int id = FunctionRegistry.INSTANCE.lookup(call.function());
        writer.visitInvokeDynamicInsn(
            "call2",
            "(Ljava/lang/Object;Ljava/lang/Object;)Ljava/lang/Object;",
            DirectCall.BOOTSTRAP,
            id);
        return null;
    }

    @Override
    public ValueCategory visitConst(Const aConst) {
        Object value = aConst.value();
        if (value instanceof Integer) {
            generateLoadInt((Integer) value);
            writer.visitMethodInsn(INVOKESTATIC, INTEGER, "valueOf", INT_TO_INTEGER, false);
        } else if (value instanceof String) {
            writer.visitLdcInsn(value);
        } else {
            throw new UnsupportedOperationException("unsupported constant type");
        }
        return null;
    }

    @Override
    public ValueCategory visitIf(If anIf) {
        anIf.condition().accept(this);
        writer.visitTypeInsn(CHECKCAST, BOOLEAN);
        writer.visitMethodInsn(INVOKEVIRTUAL, BOOLEAN, "booleanValue", TO_BOOL, false);
        Label elseStart = new Label();
        Label end = new Label();
        writer.visitJumpInsn(IFEQ, elseStart);
        anIf.trueBranch().accept(this);
        writer.visitJumpInsn(GOTO, end);
        writer.visitLabel(elseStart);
        anIf.falseBranch().accept(this);
        writer.visitLabel(end);
        return null;
    }

    @Override
    public ValueCategory visitLet(Let let) {
        let.initializer().accept(this);
        writer.visitVarInsn(ASTORE, let.variable().index());
        let.body().accept(this);
        return null;
    }

    @Override
    public ValueCategory visitPrimitive1(Primitive1 primitive1) {
        primitive1.argument().accept(this);
        String implClassName = Compiler.internalClassName(primitive1.getClass());
        writer.visitMethodInsn(INVOKESTATIC, implClassName, "staticApply", OBJECT_TO_OBJECT, false);
        return null;
    }

    @Override
    public ValueCategory visitPrimitive2(Primitive2 primitive2) {
        primitive2.argument1().accept(this);
        primitive2.argument2().accept(this);
        String implClassName = Compiler.internalClassName(primitive2.getClass());
        writer.visitMethodInsn(INVOKESTATIC, implClassName, "staticApply", OBJECT2_TO_OBJECT, false);
        return null;
    }

    @Override
    public ValueCategory visitProg(Prog prog) {
        if (prog.expressions().length == 0) {
            writer.visitInsn(NULL);
            return null;
        }
        int i;
        for (i = 0; i < prog.expressions().length - 1; i++) {
            Expression expr = prog.expressions()[i];
            expr.accept(this);
            writer.visitInsn(POP);
        }
        prog.expressions()[i].accept(this);
        return null;
    }

    @Override
    public ValueCategory visitRet(Ret ret) {
        ret.value().accept(this);
        writer.visitInsn(ARETURN);
        return null;
    }

    @Override
    public ValueCategory visitSetVar(SetVar set) {
        set.value().accept(this);
        writer.visitInsn(DUP);
        writer.visitVarInsn(ASTORE, set.variable().index());
        return null;
    }

    @Override
    public ValueCategory visitVar(Var var) {
//        ValueCategory category = var.compilerAnnotation().valueCategory();
        writer.visitVarInsn(ALOAD, var.index());
        return null;
    }

    /**
     * Using the supplied MethodVisitor, pick and generate an optimal
     * instruction to load the specified int value.
     */
    private void generateLoadInt(int value) {
        if (0 <= value && value <= 5) {
            writer.visitInsn(SPECIAL_LOAD_INT_OPCODES[value]);
        } else if (-128 <= value && value <= 127) {
            writer.visitIntInsn(BIPUSH, value);
        } else {
            writer.visitIntInsn(SIPUSH, value);
        }
    }

    private static final int[] SPECIAL_LOAD_INT_OPCODES = new int[] {
        ICONST_0, ICONST_1, ICONST_2, ICONST_3, ICONST_4, ICONST_5 };
}
