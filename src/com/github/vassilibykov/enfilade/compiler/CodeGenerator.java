package com.github.vassilibykov.enfilade.compiler;

import com.github.vassilibykov.enfilade.*;
import org.objectweb.asm.MethodVisitor;

import static com.github.vassilibykov.enfilade.compiler.ValueCategory.INT;
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
        ValueCategory category = aConst.compilerAnnotation().valueCategory();
        Object value = aConst.value();
        if (category == INT) {
            generateLoadInt((Integer) value);
        } else {
            if (value instanceof String) {
                writer.visitLdcInsn(value);
            } else {
                throw new UnsupportedOperationException("unsupported constant type");
            }
        }
        return category;
    }

    @Override
    public ValueCategory visitIf(If anIf) {
        throw new UnsupportedOperationException("not implemented yet"); // TODO implement
    }

    @Override
    public ValueCategory visitLet(Let let) {
        throw new UnsupportedOperationException("not implemented yet"); // TODO implement
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
        throw new UnsupportedOperationException("not implemented yet"); // TODO implement
    }

    @Override
    public ValueCategory visitRet(Ret ret) {
        ValueCategory valueCategory = ret.value().accept(this);
        if (valueCategory == INT) {
            writer.visitInsn(IRETURN);
        } else {
            writer.visitInsn(ARETURN);
        }
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
        if (category == INT) {
            writer.visitVarInsn(ILOAD, var.index());
        } else {
            writer.visitVarInsn(ALOAD, var.index());
        }
        return category;
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
