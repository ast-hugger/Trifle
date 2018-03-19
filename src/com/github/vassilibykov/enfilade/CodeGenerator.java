package com.github.vassilibykov.enfilade;

import org.objectweb.asm.Label;
import org.objectweb.asm.MethodVisitor;

import static com.github.vassilibykov.enfilade.ValueCategory.INT;
import static org.objectweb.asm.Opcodes.*;

public class CodeGenerator implements Expression.Visitor<ValueCategory> {

    private static final int[] SPECIAL_LOAD_INT_OPCODES = new int[] {
        ICONST_0, ICONST_1, ICONST_2, ICONST_3, ICONST_4, ICONST_5 };

    private MethodVisitor writer;

    @Override
    public ValueCategory visitCall0(Call0 call) {
        throw new UnsupportedOperationException("not implemented yet"); // TODO implement
    }

    @Override
    public ValueCategory visitCall1(Call1 call1) {
        throw new UnsupportedOperationException("not implemented yet"); // TODO implement
    }

    @Override
    public ValueCategory visitCall2(Call2 call2) {
        throw new UnsupportedOperationException("not implemented yet"); // TODO implement
    }

    @Override
    public ValueCategory visitConst(Const aConst) {
        Object value = aConst.value();
        if (value instanceof Integer) {
            generateLoadInt((Integer) value);
            writer.visitMethodInsn(
                INVOKESTATIC,
                "java/lang/Integer",
                "valueOf",
                "(I)Ljava/lang/Integer;",
                false);
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
        writer.visitMethodInsn(INVOKESTATIC,
            "java/lang/Boolean",
            "value",
            "(Ljava/lang/Boolean;)I",
            false);
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
        throw new UnsupportedOperationException("not implemented yet"); // TODO implement
    }

    @Override
    public ValueCategory visitPrimitive2(Primitive2 primitive2) {
        throw new UnsupportedOperationException("not implemented yet"); // TODO implement
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
        throw new UnsupportedOperationException("not implemented yet"); // TODO implement
//        set.value().accept(this);
//        writer.visitVarInsn(ASTORE, set.variable().index());
//        return null;
    }

    @Override
    public ValueCategory visitVar(Var var) {
        ValueCategory category = var.compilerAnnotation().valueCategory();
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
}
