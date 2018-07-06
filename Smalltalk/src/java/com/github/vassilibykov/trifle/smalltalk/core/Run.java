// Copyright (c) 2018 Vassili Bykov. Licensed under the Apache License, Version 2.0.

package com.github.vassilibykov.trifle.smalltalk.core;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;

public class Run {
    public static void main(String[] args) {
        if (args.length != 1) {
            System.out.println("Error: program file name required.");
            return;
        }
        var scriptPath = Paths.get(args[0]);
        String source;
        try {
            source = new String(Files.readAllBytes(scriptPath));
        } catch (IOException e) {
            throw new AssertionError(e);
        }
        var smalltalk = Smalltalk.create();
        smalltalk.compileClass(source);
        var testClass = smalltalk.findClass("Script");
        var script = testClass.newInstance();
        var result = script.perform("doIt");
        System.out.println("result: " + result);
    }
}
