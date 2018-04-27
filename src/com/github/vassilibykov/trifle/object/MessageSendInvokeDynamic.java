// Copyright (c) 2018 Vassili Bykov. Licensed under the Apache License, Version 2.0.

package com.github.vassilibykov.trifle.object;

import com.github.vassilibykov.trifle.core.GhostWriter;
import com.github.vassilibykov.trifle.core.InlineCachingCallSite;
import com.github.vassilibykov.trifle.core.RuntimeError;
import org.objectweb.asm.Handle;
import org.objectweb.asm.Opcodes;

import java.lang.invoke.CallSite;
import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodHandles;
import java.lang.invoke.MethodType;

public class MessageSendInvokeDynamic {

    public static final Handle BOOTSTRAP = new Handle(
        Opcodes.H_INVOKESTATIC,
        GhostWriter.internalClassName(MessageSendInvokeDynamic.class),
        "bootstrap",
        MethodType.methodType(CallSite.class, MethodHandles.Lookup.class, String.class, MethodType.class).toMethodDescriptorString(),
        false);

    static String indyName(String selector) {
        return "send|" + selector;
    }

    static String extractSelector(String indyName) {
        var index = indyName.indexOf('|');
        if (index < 0) throw new AssertionError();
        return indyName.substring(index + 1);
    }

    public static CallSite bootstrap(MethodHandles.Lookup lookup, String indyName, MethodType callSiteType) {
        var selector = extractSelector(indyName);
        var handler = DISPATCH.bindTo(selector);
        handler = handler.asCollector(Object[].class, callSiteType.parameterCount());
        return new InlineCachingCallSite(callSiteType, handler);
    }

    public static Object dispatch(String selector, InlineCachingCallSite thisSite, Object[] args) throws Throwable {
        // args are guaranteed by the compiler to not be empty
        var firstArg = args[0];
        if (!(firstArg instanceof MessageReceiver)) {
            throw RuntimeError.message(firstArg + " is not a valid message receiver");
        }
        var receiver = (MessageReceiver) firstArg;
        var method = receiver.lookupSelector(selector);
        if (!method.isPresent()) {
            throw RuntimeError.message("message not understood: " + selector);
        }
        var invoker = method.get().invoker(thisSite.type());
        var flushableInvoker = receiver.invalidationSwitchPoint().guardWithTest(
            invoker,
            thisSite.resetAndDispatchInvoker());
        thisSite.addCacheEntry(CHECK_BEHAVIOR.bindTo(receiver.behaviorToken()), flushableInvoker);
        return invoker.invokeWithArguments(args);
    }

    private static boolean checkBehavior(Object expectedToken, Object receiver) {
        return receiver instanceof MessageReceiver
            && ((MessageReceiver) receiver).behaviorToken() == expectedToken;
    }

    private static final MethodHandle DISPATCH;
    private static final MethodHandle CHECK_BEHAVIOR;
    static {
        var lookup = MethodHandles.lookup();
        try {
            DISPATCH = lookup.findStatic(
                MessageSendInvokeDynamic.class,
                "dispatch",
                MethodType.methodType(Object.class, String.class, InlineCachingCallSite.class, Object[].class));
            CHECK_BEHAVIOR = lookup.findStatic(
                MessageSendInvokeDynamic.class,
                "checkBehavior",
                MethodType.methodType(boolean.class, Object.class, Object.class));
        } catch (NoSuchMethodException | IllegalAccessException e) {
            throw new AssertionError(e);
        }
    }
}
