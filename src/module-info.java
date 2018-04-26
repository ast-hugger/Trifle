module Enfilade {
    requires jdk.unsupported;
    requires org.objectweb.asm;
    requires annotations.java8;
    exports com.github.vassilibykov.trifle.core;
    exports com.github.vassilibykov.trifle.expression;
    exports com.github.vassilibykov.trifle.builtin;
    exports com.github.vassilibykov.trifle.primitive;
}