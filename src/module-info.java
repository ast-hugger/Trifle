module Enfilade {
    requires jdk.unsupported;
    requires org.objectweb.asm;
    requires annotations.java8;
    exports com.github.vassilibykov.enfilade.core;
    exports com.github.vassilibykov.enfilade.expression;
    exports com.github.vassilibykov.enfilade.builtins;
    exports com.github.vassilibykov.enfilade.primitives;
}