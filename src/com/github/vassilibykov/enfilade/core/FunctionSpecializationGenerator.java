package com.github.vassilibykov.enfilade.core;

import org.objectweb.asm.MethodVisitor;

import static com.github.vassilibykov.enfilade.core.TypeCategory.INT;

class FunctionSpecializationGenerator extends FunctionCodeGenerator {
    FunctionSpecializationGenerator(MethodVisitor writer) {
        super(writer);
    }

    @Override
    public Void visitCall0(Call0 call) {
        throw new UnsupportedOperationException("not implemented yet"); // TODO implement
    }

    @Override
    public Void visitCall1(Call1 call) {
        throw new UnsupportedOperationException("not implemented yet"); // TODO implement
    }

    @Override
    public Void visitCall2(Call2 call) {
        throw new UnsupportedOperationException("not implemented yet"); // TODO implement
    }

    @Override
    public Void visitConst(Const aConst) {

        return null;
    }

    @Override
    public Void visitIf(If anIf) {
        throw new UnsupportedOperationException("not implemented yet"); // TODO implement
    }

    @Override
    public Void visitLet(Let let) {
        throw new UnsupportedOperationException("not implemented yet"); // TODO implement
    }

    @Override
    public Void visitPrimitive1(Primitive1 primitive) {
        throw new UnsupportedOperationException("not implemented yet"); // TODO implement
    }

    @Override
    public Void visitPrimitive2(Primitive2 primitive) {
        throw new UnsupportedOperationException("not implemented yet"); // TODO implement
    }

    @Override
    public Void visitProg(Prog prog) {
        throw new UnsupportedOperationException("not implemented yet"); // TODO implement
    }

    @Override
    public Void visitRet(Ret ret) {
        throw new UnsupportedOperationException("not implemented yet"); // TODO implement
    }

    @Override
    public Void visitSetVar(SetVar set) {
        if (set.variable().compilerAnnotation.valueCategory() == INT) {
            set.value().accept(this);
            writer.dup();
            writer.storeLocal(INT, set.variable().index());
            return null;
        } else {
            return super.visitSetVar(set);
        }
    }

    @Override
    public Void visitVar(Var var) {
        if (var.compilerAnnotation.valueCategory() == INT) {
            writer.loadLocal(INT, var.index());
            return null;
        } else {
            return super.visitVar(var);
        }
    }
}
