// Copyright (c) 2018 Vassili Bykov. Licensed under the Apache License, Version 2.0.

package com.github.vassilibykov.trifle.scheme;

import com.github.vassilibykov.trifle.core.Library;
import com.github.vassilibykov.trifle.core.UserFunction;
import com.github.vassilibykov.trifle.expression.Lambda;
import com.github.vassilibykov.trifle.expression.Variable;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static com.github.vassilibykov.trifle.scheme.Constants.DEFINE_MACRO;
import static com.github.vassilibykov.trifle.scheme.Constants.Symbols.SET_BANG;
import static com.github.vassilibykov.trifle.scheme.Helpers.asListOfSymbolNames;
import static com.github.vassilibykov.trifle.scheme.Helpers.caar;
import static com.github.vassilibykov.trifle.scheme.Helpers.caddr;
import static com.github.vassilibykov.trifle.scheme.Helpers.cadr;
import static com.github.vassilibykov.trifle.scheme.Helpers.cdar;
import static com.github.vassilibykov.trifle.scheme.Helpers.schemeList;

class Loader {

    private final Scheme scheme;
    private Library currentLibrary;

    Loader(Scheme scheme) {
        this.scheme = scheme;
    }

    Library load(java.io.Reader input) {
        var reader = Reader.on(input);
        var elements = inputElements(reader);
        defineMacroexpanders(elements.stream());
        var definitions = elements.stream().filter(this::isFunctionDefinition);
        var globalDefs = elements.stream().filter(this::isVariableDefinition).collect(Collectors.toList());
        defineGlobals(globalDefs);
        var expressions = elements.stream().filter(
            some -> !(isMacroDefinition(some) || isFunctionDefinition(some) || isVariableDefinition(some)));
        var topFunction = topFunctionDefinition(globalDefs.stream(), expressions);
        return compileDefinitions(Stream.concat(
            definitions,
            Stream.of(topFunction)));
    }

    private List<Object> inputElements(Reader reader) {
        var elements = new ArrayList<>();
        var element = reader.read();
        while (element.isPresent()) {
            elements.add(element.get());
            element = reader.read();
        }
        return elements;
    }

    private void defineMacroexpanders(Stream<Object> definitions) {
        var macroDefinitions = definitions.filter(this::isMacroDefinition);
        var macroexpanders = compileDefinitions(macroDefinitions);
        macroexpanders.functions().forEach(
            expander -> scheme.addMacroexpander(expander.name(), expander));
    }

    private void defineGlobals(List<Object> globalDefs) {
        var globals = scheme.globals();
        globalDefs.forEach(each -> {
            var name = cadr(each);
            globals.defineEntry(((Symbol) name).name());
        });
    }

    private boolean isMacroDefinition(Object object) {
        if (!(object instanceof Pair)) return false;
        var head = ((Pair) object).car();
        if (!(head instanceof Symbol)) return false;
        var name = ((Symbol) head).name();
        return DEFINE_MACRO.equals(name);
    }

    private boolean isDefinition(Object object) {
        if (!(object instanceof Pair)) return false;
        Pair pair = (Pair) object;
        var head = pair.car();
        if (!(head instanceof Symbol)) return false;
        var name = ((Symbol) head).name();
        return Constants.DEFINE.equals(name);
    }

    private boolean isFunctionDefinition(Object object) {
        return isDefinition(object) && cadr(object) instanceof Pair;
    }

    private boolean isVariableDefinition(Object object) {
        return isDefinition(object) && !(cadr(object) instanceof Pair);
    }

    private Pair topFunctionDefinition(Stream<Object> globalDefs, Stream<Object> expressions) {
        var globalAssignments = globalDefs.map(each -> {
            var name = cadr(each);
            var initializer = caddr(each);
            return schemeList(SET_BANG, name, initializer);
        });
        var body = schemeList(
            Stream.concat(
                Stream.of(Symbol.named("begin")),
                Stream.concat(
                    globalAssignments,
                    expressions)));
        return schemeList(
            Stream.of(
                Symbol.named("define"),
                schemeList(Symbol.named(Scheme.TOP_FUNCTION_NAME)),
                body));
    }

    private Library compileDefinitions(Stream<Object> definitionStream) {
        var definitions = definitionStream.collect(Collectors.toList());
        var definitionForms = definitions.stream() // forms without the 'define' (or other) keyword
            .map(Helpers::cdr)
            .collect(Collectors.toList());
        var definitionNames = definitionForms.stream()
            .map(each -> {
                var symbol = (Symbol) caar(each);
                return symbol.name();
            })
            .collect(Collectors.toList());
        var definers = definitionForms.stream()
            .map(form -> definer(form))
            .collect(Collectors.toList());
        currentLibrary = new Library();
        currentLibrary.define(definitionNames, definers);
        return currentLibrary;
    }

    private Function<UserFunction, Lambda> definer(Object form) {
        return function -> translate(function, form);
    }

    private Lambda translate(UserFunction function, Object definitionForm) {
        var names = asListOfSymbolNames(cdar(definitionForm));
        var parameters = names.stream().map(each -> Variable.named(each)).collect(Collectors.toList());
        var body = cadr(definitionForm);
        var expander = new Macroexpander(scheme.macroexpanders());
        var expandedBody = expander.expand(body);
        var normalizer = new Normalizer(scheme.primitives().keySet(), scheme.globals().entries().keySet());
        var normalizedBody = normalizer.normalize(expandedBody);
        var translator = new NormalizedTranslator(
            scheme.primitives(),
            scheme.globals(),
            scheme.globalFunctions(),
            currentLibrary,
            parameters);
        var bodyExpression = translator.translate(normalizedBody);
        return Lambda.with(parameters, bodyExpression);
    }
}
