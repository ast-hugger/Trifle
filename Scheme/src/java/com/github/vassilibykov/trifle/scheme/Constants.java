// Copyright (c) 2018 Vassili Bykov. Licensed under the Apache License, Version 2.0.

package com.github.vassilibykov.trifle.scheme;

final class Constants {

    static final String BEGIN = "begin";
    static final String DEFINE = "define";
    static final String DEFINE_MACRO = "define-macro";
    static final String IF = "if";
    static final String IF_A = "if/a";
    static final String LAMBDA = "lambda";
    static final String LAMBDA_A = "lambda/a";
    static final String LET = "let";
    static final String LET_A = "let/a";
    static final String QUOTE = "quote";
    static final String SET_BANG = "set!";

    static final class Symbols {
        static final Symbol BEGIN = Symbol.named(Constants.BEGIN);
        static final Symbol DEFINE = Symbol.named(Constants.DEFINE);
        static final Symbol IF = Symbol.named(Constants.IF);
        static final Symbol IF_A = Symbol.named(Constants.IF_A);
        static final Symbol LAMBDA = Symbol.named(Constants.LAMBDA);
        static final Symbol LAMBDA_A = Symbol.named(Constants.LAMBDA_A);
        static final Symbol LET = Symbol.named(Constants.LET);
        static final Symbol LET_A = Symbol.named(Constants.LET_A);
        static final Symbol QUOTE = Symbol.named(Constants.QUOTE);
        static final Symbol SET_BANG = Symbol.named(Constants.SET_BANG);
    }

}
