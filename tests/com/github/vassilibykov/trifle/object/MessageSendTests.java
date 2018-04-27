// Copyright (c) 2018 Vassili Bykov. Licensed under the Apache License, Version 2.0.

package com.github.vassilibykov.trifle.object;

import com.github.vassilibykov.trifle.core.UserFunction;
import com.github.vassilibykov.trifle.primitive.Add;
import org.junit.Before;
import org.junit.Test;

import java.util.List;

import static com.github.vassilibykov.trifle.expression.ExpressionLanguage.bind;
import static com.github.vassilibykov.trifle.expression.ExpressionLanguage.call;
import static com.github.vassilibykov.trifle.expression.ExpressionLanguage.const_;
import static com.github.vassilibykov.trifle.expression.ExpressionLanguage.lambda;
import static com.github.vassilibykov.trifle.expression.ExpressionLanguage.primitive;
import static org.junit.Assert.assertEquals;

public class MessageSendTests {

    private ToySmalltalkClass classA;
    private ToySmalltalkClass classB;
    private UserFunction sendAdd;
    private ToySmalltalkObject instA;
    private ToySmalltalkObject instB;

    @Before
    public void setUp() throws Exception {
        classA = new ToySmalltalkClass(List.of("foo", "bar"));
        classA.defineMethod("add",
            lambda(self ->
                bind(call(GetField.named("foo"), self), foo ->
                    bind(call(GetField.named("bar"), self), bar ->
                        primitive(Add.class, foo, bar)))));
        classB = new ToySmalltalkClass(List.of());
        classB.defineMethod("add",
            lambda(self -> const_("added")));
        sendAdd = UserFunction.construct("sendAdd",
            lambda(a -> call(MessageSend.selector("add"), a)));
        instA = classA.newInstance();
        instA.set("foo", 3);
        instA.set("bar", 4);
        instB = classB.newInstance();
    }

    @Test
    public void messageSend() {
        assertEquals(7, sendAdd.invoke(instA));
        assertEquals("added", sendAdd.invoke(instB));
    }

    @Test
    public void messageSendCompiled() {
        sendAdd.forceCompile();
        assertEquals(7, sendAdd.invoke(instA));
        assertEquals("added", sendAdd.invoke(instB));
    }

    @Test
    public void messageSendCompiledWithInlineCache() {
        sendAdd.forceCompile();
        assertEquals(7, sendAdd.invoke(instA));
        assertEquals("added", sendAdd.invoke(instB));
        // second invocation should go through the cache
        assertEquals(7, sendAdd.invoke(instA));
        assertEquals("added", sendAdd.invoke(instB));
    }

    @Test
    public void methodReplacement() {
        sendAdd.forceCompile();
        assertEquals("added", sendAdd.invoke(instB));
        classB.defineMethod("add", lambda(self -> const_("replaced")));
        assertEquals("replaced", sendAdd.invoke(instB));
    }
}
