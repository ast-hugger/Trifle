// Copyright (c) 2018 Vassili Bykov. Licensed under the Apache License, Version 2.0.

package com.github.vassilibykov.trifle.smalltalk.core;

import com.github.vassilibykov.trifle.builtin.Add;
import com.github.vassilibykov.trifle.builtin.LessThan;
import com.github.vassilibykov.trifle.builtin.Multiply;
import com.github.vassilibykov.trifle.builtin.Subtract;
import com.github.vassilibykov.trifle.core.Dictionary;
import com.github.vassilibykov.trifle.core.Invocable;
import com.github.vassilibykov.trifle.core.UserFunction;
import com.github.vassilibykov.trifle.smalltalk.grammar.AstBuilder;
import com.github.vassilibykov.trifle.smalltalk.grammar.ClassDeclaration;

import java.io.Reader;
import java.io.StringReader;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.Optional;

public class Smalltalk {

    static final SmalltalkClass OBJECT_CLASS = new SmalltalkClass(null, List.of());
    static final SmalltalkClass BOOLEAN_CLASS = new SmalltalkClass(OBJECT_CLASS, List.of());
    static final SmalltalkClass TRUE_CLASS = new SmalltalkClass(BOOLEAN_CLASS, List.of());
    static final SmalltalkClass FALSE_CLASS = new SmalltalkClass(BOOLEAN_CLASS, List.of());
    static final SmalltalkClass INTEGER_CLASS = new SmalltalkClass(OBJECT_CLASS, List.of());
    static final SmalltalkClass STRING_CLASS = new SmalltalkClass(OBJECT_CLASS, List.of());
    static final SmalltalkClass UNDEFINED_OBJECT_CLASS = new SmalltalkClass(OBJECT_CLASS, List.of());

    static {
        OBJECT_CLASS.installMethod("class", new PrimitiveMethod() {
            @Override
            public Object invoke(Object self) {
                return ((SmalltalkObject) self).smalltalkClass();
            }
        });
        OBJECT_CLASS.installMethod("print", new PrimitiveMethod() {
            @Override
            public Object invoke(Object self) {
                System.out.println(self);
                return self;
            }
        });

        TRUE_CLASS.installMethod("ifTrue:", new PrimitiveMethod() {
            @Override
            public Object invoke(Object self, Object block) {
                return ((Invocable) block).invoke();
            }
        });
        TRUE_CLASS.installMethod("ifFalse:", new PrimitiveMethod() {
            @Override
            public Object invoke(Object self, Object block) {
                return null;
            }
        });
        TRUE_CLASS.installMethod("ifTrue:ifFalse:", new PrimitiveMethod() {
            @Override
            public Object invoke(Object self, Object trueBlock, Object falseBlock) {
                return ((Invocable) trueBlock).invoke();
            }
        });

        FALSE_CLASS.installMethod("ifTrue:", new PrimitiveMethod() {
            @Override
            public Object invoke(Object o, Object block) {
                return null;
            }
        });
        FALSE_CLASS.installMethod("ifFalse:", new PrimitiveMethod() {
            @Override
            public Object invoke(Object self, Object block) {
                return ((Invocable) block).invoke();
            }
        });
        FALSE_CLASS.installMethod("ifTrue:ifFalse:", new PrimitiveMethod() {
            @Override
            public Object invoke(Object self, Object trueBlock, Object falseBlock) {
                return ((Invocable) falseBlock).invoke();
            }
        });

        INTEGER_CLASS.installMethod("+", Add.INSTANCE);
        INTEGER_CLASS.installMethod("-", Subtract.INSTANCE);
        INTEGER_CLASS.installMethod("*", Multiply.INSTANCE);
        INTEGER_CLASS.installMethod("<", LessThan.INSTANCE);
    }

    static {
        MessageDispatchCustomizations.install();
    }

    public static Smalltalk create() {
        return new Smalltalk();
    }

    /*
        Instance
     */

    private final Dictionary globals = Dictionary.create();

    private Smalltalk() {
        setupBuiltinClasses();
    }

    Optional<Dictionary.Entry> lookupGlobalEntry(String name) {
        return globals.getEntry(name);
    }

    public SmalltalkClass findClass(String name) {
        var value = globals.getEntry(name).map(it -> it.value()).orElseThrow();
        if (value instanceof SmalltalkClass) {
            return (SmalltalkClass) value;
        } else {
            throw new NoSuchElementException();
        }
    }

    public SmalltalkClass compileClass(String source) {
        return compileClass(new StringReader(source));
    }

    public SmalltalkClass compileClass(Reader reader) {
        // FIXME access to class should be synchronized
        var ast = AstBuilder.parseClass(reader);
        var stClass = createClass(ast.classDeclaration());
        var compiler = new Compiler(this, stClass);
        ast.instanceMethods().forEach(each -> {
            var selector = each.selector();
            var lambda = compiler.compile(each);
            var method = UserFunction.construct(selector, lambda);
            stClass.installMethod(selector, method);
        });
        globals.defineEntry(ast.classDeclaration().name()).setValue(stClass);
        return stClass;
    }

    private SmalltalkClass createClass(ClassDeclaration classDeclaration) {
        if (globals.getEntry(classDeclaration.name()).isPresent()) {
            throw new IllegalStateException("class already exists: " + classDeclaration.name());
        }
        var superclass = (SmalltalkClass) globals.getEntry(classDeclaration.superclassName()).orElseThrow().value();
        return new SmalltalkClass(superclass, classDeclaration.instVarNames());
    }


    private void setupBuiltinClasses() {
        globals.defineEntry("Object").setValue(OBJECT_CLASS);
        globals.defineEntry("Boolean").setValue(BOOLEAN_CLASS);
        globals.defineEntry("True").setValue(TRUE_CLASS);
        globals.defineEntry("False").setValue(FALSE_CLASS);
        globals.defineEntry("Integer").setValue(INTEGER_CLASS);
        globals.defineEntry("String").setValue(STRING_CLASS);
        globals.defineEntry("UndefinedObject").setValue(UNDEFINED_OBJECT_CLASS);
    }
}
