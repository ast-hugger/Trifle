package com.github.vassilibykov.enfilade;

import org.objectweb.asm.Handle;
import org.objectweb.asm.Opcodes;

import java.lang.invoke.CallSite;
import java.lang.invoke.MethodHandles.Lookup;
import java.lang.invoke.MethodType;

/**
 * An invokedynamic instruction for a call expression whose function is a direct
 * pointer at another function. The target is encoded as an integer ID in the
 * {@link FunctionRegistry}, passed as an extra bootstrapper argument.
 */
public class DirectCall {

    public static final Handle BOOTSTRAP = new Handle(
        Opcodes.H_INVOKESTATIC,
        Compiler.internalClassName(DirectCall.class),
        "bootstrap",
        MethodType.methodType(CallSite.class, Lookup.class, String.class, MethodType.class, Integer.class).toMethodDescriptorString(),
        false);

    @SuppressWarnings("unused") // called by generated code
    public static CallSite bootstrap(Lookup lookupAtCaller, String name, MethodType callSiteType, Integer targetId) {
        Function target = FunctionRegistry.INSTANCE.lookup(targetId);
        if (target == null) {
            throw new AssertionError("target function ID not found: " + targetId);
        }
        return target.nexus.callSite(callSiteType);
    }
}