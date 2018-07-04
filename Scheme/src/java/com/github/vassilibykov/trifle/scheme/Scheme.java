// Copyright (c) 2018 Vassili Bykov. Licensed under the Apache License, Version 2.0.

package com.github.vassilibykov.trifle.scheme;

import com.github.vassilibykov.trifle.builtin.Add;
import com.github.vassilibykov.trifle.builtin.BuiltinFunction;
import com.github.vassilibykov.trifle.builtin.LessThan;
import com.github.vassilibykov.trifle.builtin.Multiply;
import com.github.vassilibykov.trifle.builtin.Subtract;
import com.github.vassilibykov.trifle.core.Dictionary;
import com.github.vassilibykov.trifle.core.FreeFunction;
import com.github.vassilibykov.trifle.core.Invocable;
import com.github.vassilibykov.trifle.core.Library;
import com.github.vassilibykov.trifle.expression.Primitive;
import com.github.vassilibykov.trifle.primitive.EQ;
import com.github.vassilibykov.trifle.primitive.GetClass;
import com.github.vassilibykov.trifle.scheme.builtins.Car;
import com.github.vassilibykov.trifle.scheme.builtins.Cdr;
import com.github.vassilibykov.trifle.scheme.builtins.Cons;
import com.github.vassilibykov.trifle.scheme.builtins.PairP;
import com.github.vassilibykov.trifle.scheme.builtins.Print;
import com.github.vassilibykov.trifle.scheme.builtins.TimeToRun;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class Scheme {

    static final String TOP_FUNCTION_NAME = "$top";

    public static void main(String[] args) {
        if (args.length != 1) {
            System.out.println("syntax: scheme <file name>");
            return;
        }
        var scheme = new Scheme();
        scheme.load(Paths.get(args[0]));
    }

    private static final Map<String, Class<? extends Primitive>> PRIMITIVES = Map.of(
        "eqv?", EQ.class,
        "java-class-of", GetClass.class);

    private static final Map<String, BuiltinFunction> BUILTINS;
    static {
        var builtins = new HashMap<String, BuiltinFunction>();
        builtins.put("+", Add.INSTANCE);
        builtins.put("*", Multiply.INSTANCE);
        builtins.put("-", Subtract.INSTANCE);
        builtins.put("<", LessThan.INSTANCE);

        builtins.put("cons", Cons.INSTANCE);
        builtins.put("car", Car.INSTANCE);
        builtins.put("cdr", Cdr.INSTANCE);
        builtins.put("pair?", PairP.INSTANCE);

        builtins.put("print", Print.INSTANCE);
        builtins.put("ms-to-run", TimeToRun.INSTANCE);
        BUILTINS = Collections.unmodifiableMap(builtins);
    }

    /*
        Instance
     */

    private final Map<String, Invocable> macroexpanders = new HashMap<>();
    private final List<Library> loadedUnits = new ArrayList<>();
    private final Dictionary globals = Dictionary.create();

    /*
        The overall current implementation is in fact not faithful to Scheme's
        Lisp-1 approach. Compiled functions and globals should not be separated
        into distinct namespaces.
     */

    public Scheme() {
        load(Paths.get("src/scm/prelude.scm"));
    }

    public Object load(Path path) {
        try (var reader = Files.newBufferedReader(path)) {
            return load(reader);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    public Object load(java.io.Reader input) {
        var compilationUnit = new Loader(this).load(input);
        loadedUnits.add(compilationUnit);
        return compilationUnit.get(TOP_FUNCTION_NAME).invoke();
    }

    Map<String, Class<? extends Primitive>> primitives() {
        return PRIMITIVES;
    }

    Map<String, ? extends FreeFunction> globalFunctions() {
        Map<String, FreeFunction> globals = new HashMap<>();
        BUILTINS.forEach((name, function) -> globals.put(name, function));
        loadedUnits.forEach(
            each -> each.functions().forEach(
                function -> globals.put(function.name(), function)));
        return Collections.unmodifiableMap(globals);
    }

    Map<String, Invocable> macroexpanders() {
        return Collections.unmodifiableMap(macroexpanders);
    }

    void addMacroexpander(String name, Invocable expander) {
        macroexpanders.put(name, expander);
    }

    Dictionary globals() {
        return globals;
    }
}
