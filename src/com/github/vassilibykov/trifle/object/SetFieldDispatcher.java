// Copyright (c) 2018 Vassili Bykov. Licensed under the Apache License, Version 2.0.

package com.github.vassilibykov.trifle.object;

import com.github.vassilibykov.trifle.core.CallDispatcher;
import com.github.vassilibykov.trifle.core.CallNode;
import com.github.vassilibykov.trifle.core.CodeGenerator;
import com.github.vassilibykov.trifle.core.CompilerError;
import com.github.vassilibykov.trifle.core.EvaluatorNode;
import com.github.vassilibykov.trifle.core.Gist;
import com.github.vassilibykov.trifle.core.JvmType;
import com.github.vassilibykov.trifle.core.RuntimeError;
import org.objectweb.asm.Opcodes;

import java.lang.invoke.MethodType;
import java.util.Optional;

class SetFieldDispatcher implements CallDispatcher {

    private final String fieldName;

    SetFieldDispatcher(String fieldName) {
        this.fieldName = fieldName;
    }

    @Override
    public Optional<EvaluatorNode> evaluatorNode() {
        return Optional.empty();
    }

    @Override
    public Object execute(CallNode call, EvaluatorNode.Visitor<Object> interpreter) {
        if (call.arity() != 2) {
            throw RuntimeError.message("invalid call expression"); // TODO should probably use a different exception
        }
        var object = call.argument(0).accept(interpreter);
        var value = call.argument(1).accept(interpreter);
        if (object instanceof FixedObject) {
            ((FixedObject) object).set(fieldName, value);
            return value;
        } else {
            throw RuntimeError.message("not an object with fields: " + object);
        }
    }

    @Override
    public Gist generateCode(CallNode call, CodeGenerator generator) {
        if (call.arity() != 2) {
            throw new CompilerError("invalid call expression");
        }
        var gist1 = generator.generateCode(call.argument(0));
        generator.writer().adaptValue(gist1.type(), JvmType.REFERENCE);
        var gist2 = generator.generateCode(call.argument(1));
        generator.writer().adaptValue(gist2.type(), JvmType.REFERENCE);
        generator.writer().asm().visitInsn(Opcodes.DUP_X1);
        generator.writer().invokeDynamic(
            FixedObject.accessImplementation().setterBootstrapper(),
            FieldAccessImplementation.setterName(fieldName),
            MethodType.methodType(void.class, Object.class, Object.class));
        return Gist.INFALLIBLE_REFERENCE;
    }
}
