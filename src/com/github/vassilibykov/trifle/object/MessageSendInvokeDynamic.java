// Copyright (c) 2018 Vassili Bykov. Licensed under the Apache License, Version 2.0.

package com.github.vassilibykov.trifle.object;

import com.github.vassilibykov.trifle.core.GhostWriter;
import com.github.vassilibykov.trifle.core.InlineCachingCallSite;
import com.github.vassilibykov.trifle.core.Invocable;
import org.objectweb.asm.Handle;
import org.objectweb.asm.Opcodes;

import java.lang.invoke.CallSite;
import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodHandles;
import java.lang.invoke.MethodType;
import java.util.Map;
import java.util.Optional;

public class MessageSendInvokeDynamic {

    public static final Handle BOOTSTRAP = new Handle(
        Opcodes.H_INVOKESTATIC,
        GhostWriter.internalClassName(MessageSendInvokeDynamic.class),
        "bootstrap",
        MethodType.methodType(CallSite.class, MethodHandles.Lookup.class, String.class, MethodType.class).toMethodDescriptorString(),
        false);

    /**
     * Given a Smalltalk selector, compute the name to use as the method name in an
     * {@code invokedynamic} instruction sending a message with the selector.
     * JVM spec prohibits certain characters from appearing in a method name. Even
     * though in the case of invokedynamic the method name has no immediate execution
     * semantics, the prohibition still applies.
     */
    static String indyName(String selector) {
        var cleanedUp = escapeIllegalChars(selector);
        return cleanedUp.equals(selector)
            ? "send|" + selector
            : "send|$" + cleanedUp;
    }

    static String extractSelector(String indyName) {
        var index = indyName.indexOf('|');
        if (index < 0) throw new AssertionError();
        var selector = indyName.substring(index + 1);
        return selector.charAt(0) == '$'
            ? unescapeIllegalChars(selector)
            : selector;
    }

    private static final Map<Character, String> CHARS_TO_ESCAPES = Map.of(
        '.', "P",
        ';', "S",
        '[', "B",
        '/', "H",
        '<', "L",
        '>', "R",
        '$', "$");

    private static final Map<Character, Character> ESCAPES_TO_CHARS = Map.of(
        'P', '.',
        'S', ';',
        'B', '[',
        'H', '/',
        'L', '<',
        'R', '>',
        '$', '$');

    private static String escapeIllegalChars(String string) {
        var result = new StringBuilder();
        for (int i = 0; i < string.length(); i++) {
            var character = string.charAt(i);
            var replacement = CHARS_TO_ESCAPES.get(character);
            if (replacement != null) {
                result.append("$").append(replacement);
            } else {
                result.append(character);
            }
        }
        return result.toString();
    }

    private static String unescapeIllegalChars(String string) {
        var result = new StringBuilder();
        var iterator = string.chars().iterator();
        iterator.next(); // skip the initial '$'
        while (iterator.hasNext()) {
            var character = Character.toChars(iterator.next())[0]; // bleah
            if (character == '$') {
                character = ESCAPES_TO_CHARS.get(Character.toChars(iterator.next())[0]);
            }
            result.append(character);
        }
        return result.toString();
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
        var isProperReceiver = firstArg instanceof MessageReceiver;
        Optional<? extends Invocable> method;
        if (isProperReceiver) {
            method = ((MessageReceiver) firstArg).lookupSelector(selector);
        } else {
            method = MessageSendDispatcher.extension().lookupStrangeReceiverSelector(selector, args);
        }
        if (!method.isPresent()) {
            return MessageSendDispatcher.extension().messageNotUnderstood(selector, args);
        }
        var invoker = method.get().invoker(thisSite.type());
        MethodHandle cacheGuard;
        MethodHandle flushableInvoker;
        if (isProperReceiver) {
            var receiver = (MessageReceiver) firstArg;
            cacheGuard = CHECK_BEHAVIOR.bindTo(receiver.behaviorToken());
            flushableInvoker = receiver.invalidationSwitchPoint().guardWithTest(
                invoker,
                thisSite.resetAndDispatchInvoker());
        } else if (thisSite.type().parameterType(0).isPrimitive()) {
            // If the discriminating parameter at the call site is a primitive,
            // we are guaranteed handler applicability every time - no need to guard.
            thisSite.setTarget(invoker.asType(thisSite.type()));
            return invoker.invokeWithArguments(args);
        } else {
            cacheGuard = firstArg == null ? CHECK_NULL : CHECK_CLASS.bindTo(firstArg.getClass());
            flushableInvoker = invoker;
        }
        thisSite.addCacheEntry(cacheGuard, flushableInvoker);
        return invoker.invokeWithArguments(args);
    }

    private static boolean checkBehavior(Object expectedToken, Object receiver) {
        return receiver instanceof MessageReceiver
            && ((MessageReceiver) receiver).behaviorToken() == expectedToken;
    }

    private static boolean checkClass(Class<?> expectedClass, Object receiver) {
        return expectedClass.isInstance(receiver);
    }

    /**
     * A separate checker method for the receiver being {@code null}, required
     * because strangely, even though {@code null} is a legal return value
     * of a method with the return type {@code Void},
     * {@code Void.class.isInstance(null)} is false.
     */
    private static boolean checkNull(Object receiver) {
        return receiver == null;
    }

    private static final MethodHandle DISPATCH;
    private static final MethodHandle CHECK_BEHAVIOR;
    private static final MethodHandle CHECK_CLASS;
    private static final MethodHandle CHECK_NULL;
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
            CHECK_CLASS = lookup.findStatic(
                MessageSendInvokeDynamic.class,
                "checkClass",
                MethodType.methodType(boolean.class, Class.class, Object.class));
            CHECK_NULL = lookup.findStatic(
                MessageSendInvokeDynamic.class,
                "checkNull",
                MethodType.methodType(boolean.class, Object.class));
        } catch (NoSuchMethodException | IllegalAccessException e) {
            throw new AssertionError(e);
        }
    }
}
