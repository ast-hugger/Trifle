package com.github.vassilibykov.enfilade;

import org.junit.Test;

import static com.github.vassilibykov.enfilade.ExpressionLanguage.*;
import static org.junit.Assert.assertEquals;

public class BasicCompilerTests {

    @Test
    public void testConst() {
        assertEquals(42, compile(const_(42)).invoke());
    }

    @Test
    public void testArg() {
        Method method = unaryMethod(arg -> arg);
        method.nexus.forceCompile();
        assertEquals(42, method.invoke(42));
    }

    @Test
    public void testIf() {
        Method method = unaryMethod(
            arg ->
                if_(arg,
                    const_("true"),
                    const_("false")));
        method.nexus.forceCompile();
        assertEquals("true", method.invoke(true));
        assertEquals("false", method.invoke(false));
    }

    private Method compile(Expression methodBody) {
        Method method = Method.with(new Var[0], methodBody);
        method.nexus.forceCompile();
        return method;
    }
}
