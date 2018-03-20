package com.github.vassilibykov.enfilade.core;

import org.objectweb.asm.MethodVisitor;

import java.lang.invoke.MethodType;

import static com.github.vassilibykov.enfilade.core.TypeCategory.REFERENCE;

class FunctionCodeGenerator implements Expression.Visitor<Void> {
    protected final GhostWriter writer;

    FunctionCodeGenerator(MethodVisitor visitor) {
        this.writer = new GhostWriter(visitor);
    }

    @Override
    public Void visitCall0(Call0 call) {
        int id = FunctionRegistry.INSTANCE.lookup(call.function());
        writer.invokeDynamic(
            DirectCall.BOOTSTRAP,
            "call0",
            MethodType.methodType(Object.class),
            id);
        return null;
    }

    @Override
    public Void visitCall1(Call1 call) {
        call.arg().accept(this);
        int id = FunctionRegistry.INSTANCE.lookup(call.function());
        writer.invokeDynamic(
            DirectCall.BOOTSTRAP,
            "call1",
            MethodType.methodType(Object.class, Object.class),
            id);
        return null;
    }

    @Override
    public Void visitCall2(Call2 call) {
        call.arg1().accept(this);
        call.arg2().accept(this);
        int id = FunctionRegistry.INSTANCE.lookup(call.function());
        writer.invokeDynamic(
            DirectCall.BOOTSTRAP,
            "call2",
            MethodType.methodType(Object.class, Object.class, Object.class),
            id);
        return null;
    }

    @Override
    public Void visitConst(Const aConst) {
        Object value = aConst.value();
        if (value instanceof Integer) {
            writer
                .loadInt((Integer) value)
                .invokeStatic(Integer.class, "valueOf", Integer.class, int.class);
        } else if (value instanceof String) {
            writer.loadString((String) value);
        } else {
            throw new UnsupportedOperationException("unsupported constant type");
        }
        return null;
    }

    @Override
    public Void visitIf(If anIf) {
        anIf.condition().accept(this);
        writer
            .checkCast(Boolean.class)
            .invokeVirtual(Boolean.class, "booleanValue", boolean.class);
        writer.withLabelAtTheEnd(end -> {
            writer.withLabelAtTheEnd(elseStart -> {
                writer.jumpIf0(elseStart);
                anIf.trueBranch().accept(this);
                writer.jump(end);
            });
            anIf.falseBranch().accept(this);
        });
        return null;
    }

    @Override
    public Void visitLet(Let let) {
        let.initializer().accept(this);
        writer.storeLocal(REFERENCE, let.variable().index());
        let.body().accept(this);
        return null;
    }

    @Override
    public Void visitPrimitive1(Primitive1 primitive1) {
        primitive1.argument().accept(this);
        primitive1.generate(writer);
//        writer.invokeStatic(primitive1.getClass(), "staticApply", Object.class, Object.class);
        return null;
    }

    @Override
    public Void visitPrimitive2(Primitive2 primitive2) {
        primitive2.argument1().accept(this);
        primitive2.argument2().accept(this);
        primitive2.generate(writer);
//        writer.invokeStatic(primitive2.getClass(), "staticApply", Object.class, Object.class, Object.class);
        return null;
    }

    @Override
    public Void visitProg(Prog prog) {
        Expression[] expressions = prog.expressions();
        if (expressions.length == 0) {
            writer.loadNull();
            return null;
        }
        int i;
        for (i = 0; i < expressions.length - 1; i++) {
            Expression expr = expressions[i];
            expr.accept(this);
            writer.pop();
        }
        expressions[i].accept(this);
        return null;
    }

    @Override
    public Void visitRet(Ret ret) {
        ret.value().accept(this);
        writer.ret(REFERENCE);
        return null;
    }

    @Override
    public Void visitSetVar(SetVar set) {
        set.value().accept(this);
        writer
            .dup()
            .storeLocal(REFERENCE, set.variable().index());
        return null;
    }

    @Override
    public Void visitVar(Var var) {
        writer.loadLocal(REFERENCE, var.index());
        return null;
    }
}
