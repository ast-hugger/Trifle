// Copyright (c) 2018 Vassili Bykov. Licensed under the Apache License, Version 2.0.

package com.github.vassilibykov.trifle.scheme;

import com.github.vassilibykov.trifle.core.Dictionary;
import com.github.vassilibykov.trifle.core.FreeFunction;
import com.github.vassilibykov.trifle.core.Library;
import com.github.vassilibykov.trifle.expression.AtomicExpression;
import com.github.vassilibykov.trifle.expression.Block;
import com.github.vassilibykov.trifle.expression.Call;
import com.github.vassilibykov.trifle.expression.Const;
import com.github.vassilibykov.trifle.expression.DictionaryGetter;
import com.github.vassilibykov.trifle.expression.DictionarySetter;
import com.github.vassilibykov.trifle.expression.Expression;
import com.github.vassilibykov.trifle.expression.FreeFunctionReference;
import com.github.vassilibykov.trifle.expression.If;
import com.github.vassilibykov.trifle.expression.Lambda;
import com.github.vassilibykov.trifle.expression.Let;
import com.github.vassilibykov.trifle.expression.Primitive;
import com.github.vassilibykov.trifle.expression.PrimitiveCall;
import com.github.vassilibykov.trifle.expression.SetVariable;
import com.github.vassilibykov.trifle.expression.Variable;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import static com.github.vassilibykov.trifle.scheme.Constants.BEGIN;
import static com.github.vassilibykov.trifle.scheme.Constants.IF_A;
import static com.github.vassilibykov.trifle.scheme.Constants.LAMBDA_A;
import static com.github.vassilibykov.trifle.scheme.Constants.LET_A;
import static com.github.vassilibykov.trifle.scheme.Constants.QUOTE;
import static com.github.vassilibykov.trifle.scheme.Constants.SET_BANG;
import static com.github.vassilibykov.trifle.scheme.Helpers.asListOfSymbolNames;
import static com.github.vassilibykov.trifle.scheme.Helpers.caddr;
import static com.github.vassilibykov.trifle.scheme.Helpers.cadr;
import static com.github.vassilibykov.trifle.scheme.Helpers.car;
import static com.github.vassilibykov.trifle.scheme.Helpers.cdr;

/**
 * Translates a Scheme expression into a Trifle input expression.
 * The Scheme expression should have already been A-normalized.
 */
class NormalizedTranslator {

    private static abstract class Environment {
        private final Environment parent;

        Environment(Environment parent) {
            this.parent = parent;
        }

        Variable lookup(String name) {
            return parent != null ? parent.lookup(name) : null;
        }
    }

    private static class SingleEntryEnvironment extends Environment {
        private final Variable entry;

        SingleEntryEnvironment(Variable entry, Environment parent) {
            super(parent);
            this.entry = entry;
        }

        @Override
        Variable lookup(String name) {
            return entry.name().equals(name) ? entry : super.lookup(name);
        }
    }

    private static class MapEnvironment extends Environment {
        private final Map<String, Variable> variables = new HashMap<>();

        MapEnvironment(Environment parent) {
            super(parent);
        }

        void add(Variable variable) {
            variables.put(variable.name(), variable);
        }

        Variable lookup(String name) {
            var local = variables.get(name);
            return local != null ? local : super.lookup(name);
        }
    }

    /*
        Instance
     */

    private final Map<String, Class<? extends Primitive>> primitives;
    private final Dictionary globals;
    private final Map<String, FreeFunctionReference> outerFunctions = new HashMap<>();
    private Environment environment;

    NormalizedTranslator(
        Map<String, Class<? extends Primitive>> primitives,
        Dictionary globals,
        Map<String, ? extends FreeFunction> globalFunctions,
        Library compilationUnit,
        List<Variable> parameters)
    {
        this.primitives = primitives;
        this.globals = globals;
        globalFunctions.forEach((key, value) -> outerFunctions.put(key, FreeFunctionReference.to(value)));
        compilationUnit.functions().forEach(each ->
            outerFunctions.put(each.name(), FreeFunctionReference.to(each)));
        var topEnvironment = new MapEnvironment(null);
        parameters.forEach(topEnvironment::add);
        this.environment = topEnvironment;
    }

    Expression translate(Object object) {
        if (object instanceof Integer || object instanceof String || object == null) {
            return Const.value(object);
        } else if (object instanceof Symbol) {
            return lookupReference(((Symbol) object).name());
        } else if (object instanceof Pair) {
            return translateForm((Pair) object);
        } else {
            throw new AssertionError("unexpected object: " + object);
        }
    }

    private Expression translateForm(Pair pair) {
        var car = pair.car();
        if (!(car instanceof Symbol)) {
            throw new RuntimeException("invalid form head: " + car);
        }
        var name = ((Symbol) car).name();
        switch (name) {
            case BEGIN:
                return translateBegin(pair.cdr());
            case IF_A:
                return translateIf(pair.cdr());
            case LAMBDA_A:
                return translateLambda(pair.cdr());
            case LET_A:
                return translateLet(pair.cdr());
            case QUOTE:
                return translateQuote(pair.cdr());
            case SET_BANG:
                return translateSet(pair.cdr());
            default:
                return translateCall(pair);
        }
    }

    /*
        The following individual translators are intentionally dynamically typed.
        This way they are easier to write and read. Possible cast errors should
        eventually be caught or guarded against and reported as syntax errors.
     */

    private Expression translateBegin(Object pair) {
        var expressions = new ArrayList<Expression>();
        var head = (Pair) pair;
        while (head != null) {
            expressions.add(translate(head.car()));
            head = (Pair) head.cdr();
        }
        return Block.with(expressions);
    }

    private Expression translateCall(Object form) {
        var functionExpr = car(form);
        AtomicExpression function = null;
        if (functionExpr instanceof Symbol) {
            var name = ((Symbol) functionExpr).name();
            var primitive = primitives.get(name);
            if (primitive != null) {
                return translatePrimitiveCall(primitive, form);
            }
            function = outerFunctions.get(name);
        }
        if (function == null) {
            function = (AtomicExpression) translate(functionExpr);
        }
        var args = translateArgList(cdr(form)); // TODO report a proper error if not a Pair
        return Call.with(function, args);
    }

    private Expression translatePrimitiveCall(Class<? extends Primitive> primitiveClass, Object form) {
        var args = translateArgList(cdr(form));
        return PrimitiveCall.with(primitiveClass, args);
    }

    private List<AtomicExpression> translateArgList(Object pair) {
        var head = (Pair) pair;
        var result = new ArrayList<AtomicExpression>();
        while (head != null) {
            var expr = translate(head.car());
            if (!(expr instanceof AtomicExpression)) {
                throw new RuntimeException("call argument is not atomic: " + head.car());
            }
            result.add((AtomicExpression) expr);
            head = (Pair) head.cdr(); // TODO support dotted lists
        }
        return result;
    }

    private Expression translateIf(Object ifBody) {
        var condition = (AtomicExpression) translate(car(ifBody));
        var thenExpr = translate(cadr(ifBody));
        var elseExpr = translate(caddr(ifBody));
        return If.with(condition, thenExpr, elseExpr);
    }

    private Expression translateLambda(Object pair) {
        var names = asListOfSymbolNames(car(pair));
        var parameters = names.stream().map(each -> Variable.named(each)).collect(Collectors.toList());
        defineVariables(parameters);
        var body = cadr(pair);
        var bodyExpression = translate(body);
        popEnvironment();
        return Lambda.with(parameters, bodyExpression);
    }

    private Expression translateLet(Object pair) {
        var binding = car(pair);
        var varName = (Symbol) car(binding);
        var initializerExpr = cadr(binding);
        var variable = Variable.named(varName.name());
        var bodyExpr = cadr(pair);
        Expression translatedInitializer = translate(initializerExpr);
        defineVariable(variable);
        Expression translatedBody = translate(bodyExpr);
        popEnvironment();
        return Let.with(variable, translatedInitializer, translatedBody);
    }

    private Expression translateQuote(Object pair) {
        if (cdr(pair) != null) {
            throw new RuntimeException("invalid QUOTE form; more than one datum: " + pair);
        }
        return Const.value(car(pair));
    }

    private Expression translateSet(Object pair) {
        var variable = (Symbol) car(pair);
        var name = variable.name();
        var valueExpr = cadr(pair);
        if (globals.getEntry(name).isPresent()) {
            return Call.with(DictionarySetter.create(globals, name), (AtomicExpression) translate(valueExpr));
        } else {
            return SetVariable.with(Variable.named(variable.name()), translate(valueExpr));
        }
    }

    private void defineVariable(Variable variable) {
        environment = new SingleEntryEnvironment(variable, environment);
    }

    private void defineVariables(List<Variable> variables) {
        var newEnvironment = new MapEnvironment(environment);
        variables.forEach(newEnvironment::add);
        environment = newEnvironment;
    }

    private void popEnvironment() {
        environment = environment.parent;
    }

    private Expression lookupReference(String name) {
        var local = environment.lookup(name);
        if (local != null) return local;
        var outer = outerFunctions.get(name);
        if (outer != null) return outer;
        var global = globals.getEntry(name);
        if (global != null) return Call.with(DictionaryGetter.create(globals, name));
        throw new RuntimeException("undefined variable: " + name);
    }
}
