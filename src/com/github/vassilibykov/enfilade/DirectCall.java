package com.github.vassilibykov.enfilade;

import org.objectweb.asm.Handle;
import org.objectweb.asm.Opcodes;

import java.lang.invoke.CallSite;
import java.lang.invoke.ConstantCallSite;
import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodHandles;
import java.lang.invoke.MethodType;

/**
 * Am implementation of an invokedynamic instruction for a call
 * expression whose function is a direct pointer at another function.
 */
public class DirectCall {

    public static final Handle BOOTSTRAP = new Handle(
        Opcodes.H_INVOKESTATIC,
        Compiler.internalClassName(DirectCall.class),
        "bootstrap",
        MethodType.methodType(CallSite.class, MethodHandles.Lookup.class, String.class, MethodType.class, Integer.class).toMethodDescriptorString(),
        false);

    public static CallSite bootstrap(MethodHandles.Lookup lookupAtCallSite, String name, MethodType callSiteType, Integer targetId) {
        Function target = DirectCallRegistry.INSTANCE.lookup(targetId);
        if (target == null) {
            throw new AssertionError("target function ID not found: " + targetId);
        }
        // FIXME for now just binding to the compiled form in the nexus assumed to exist
        return new ConstantCallSite(target.nexus.compiledForm);
    }
}
