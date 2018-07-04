// Copyright (c) 2018 Vassili Bykov. Licensed under the Apache License, Version 2.0.

package com.github.vassilibykov.trifle.smalltalk.core;

import com.github.vassilibykov.trifle.expression.Lambda;
import com.github.vassilibykov.trifle.expression.Variable;
import com.github.vassilibykov.trifle.smalltalk.grammar.AstBuilder;
import com.github.vassilibykov.trifle.smalltalk.grammar.Block;
import com.github.vassilibykov.trifle.smalltalk.grammar.MethodDeclaration;

import java.util.HashMap;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.Optional;

/**
 * Compiles methods for a specific class into Trifle {@link Lambda Lambdas}.
 */
class Compiler {

    class Scope {
        private final Map<String, Binding> localsAndInstVars = new HashMap<>();

        Scope() {
            targetClass.allInstanceVariableNames().forEach(
                each -> localsAndInstVars.put(each, new Binding.InstVarBinding(each)));
        }

        private void defineLocal(String name) {
            Variable variable = Variable.named(name);
            localsAndInstVars.put(name, new Binding.LocalBinding(variable));
        }

        Optional<Binding> lookup(String name) {
            var binding = localsAndInstVars.get(name);
            if (binding != null) return Optional.of(binding);
            return system.lookupGlobalEntry(name).map(it -> new Binding.GlobalBinding(it));
        }

        Variable lookupRequiredLocal(String name) {
            var binding = lookup(name).orElseThrow();
            if (binding instanceof Binding.LocalBinding) {
                return ((Binding.LocalBinding) binding).variable();
            } else {
                throw new NoSuchElementException();
            }
        }

        Scope nestedScopeFor(Block block) {
            var subscope = new Scope();
            localsAndInstVars.forEach(subscope.localsAndInstVars::put);
            block.argumentNames().forEach(subscope::defineLocal);
            block.tempNames().forEach(subscope::defineLocal);
            return subscope;
        }
    }

    private final Smalltalk system;
    private final SmalltalkClass targetClass;

    Compiler(Smalltalk system, SmalltalkClass targetClass) {
        this.system = system;
        this.targetClass = targetClass;
    }

    Lambda compile(String methodSource) {
        return compile(AstBuilder.parseMethod(methodSource));
    }

    Lambda compile(MethodDeclaration method) {
        return SmalltalkToTrifleTranslator.translate(method, computeMethodScope(method));
    }

    private Scope computeMethodScope(MethodDeclaration methodDecl) {
        var scope = new Scope();
        scope.defineLocal("self");
        methodDecl.argumentNames().forEach(scope::defineLocal);
        methodDecl.tempNames().forEach(scope::defineLocal);
        return scope;
    }

}
