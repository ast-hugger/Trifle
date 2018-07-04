// Copyright (c) 2018 Vassili Bykov. Licensed under the Apache License, Version 2.0.

package com.github.vassilibykov.trifle.scheme;

import java.io.CharArrayReader;
import java.io.IOException;
import java.util.Collection;
import java.util.Optional;
import java.util.Stack;

public class Reader {

    public static Reader on(java.io.Reader input) {
        return new Reader(input);
    }

    public static Object read(java.io.Reader input) {
        return new Reader(input).readObject();
    }

    public static Object read(String chars) {
        return read(new CharArrayReader(chars.toCharArray()));
    }

    private enum TokenClass {
        IDENTIFIER,
        NUMBER,
        STRING,
        QUOTE,
        OPENPAREN,
        CLOSEPAREN,
        PERIOD,
        END
    }

    private static class Token {
        private final TokenClass tokenClass;
        private final String content;

        private Token(TokenClass tokenClass, String content) {
            this.tokenClass = tokenClass;
            this.content = content;
        }

        @Override
        public String toString() {
            return "Token(" + tokenClass + ", '" + content + "')";
        }
    }

    private static final char EOF = (char) -1;

    private java.io.Reader input;
    private char hereChar;
    private Token lookahead;

    private Reader(java.io.Reader input) {
        this.input = input;
        scanChar();
        scan();
    }

    public Optional<Object> read() {
        if (lookahead.tokenClass == TokenClass.END) {
            return Optional.empty();
        } else {
            return Optional.of(readObject());
        }
    }

    private Object readObject() {
        switch (lookahead.tokenClass) {
            case END:
                throw new RuntimeException("unexpected end of input");
            case IDENTIFIER:
                var content = lookahead.content;
                scan();
                return "null".equals(content) ? null : Symbol.named(content);
            case NUMBER:
                return readNumber();
            case STRING:
                var string = lookahead.content;
                scan();
                return string;
            case OPENPAREN:
                return readList();
            case QUOTE:
                scan();
                var datum = readObject();
                return Pair.of(Symbol.named("quote"), Pair.of(datum, null));
            default:
                throw new AssertionError("unexpected token: " + lookahead);
        }
    }

    private Pair readList() {
        match(TokenClass.OPENPAREN);
        Stack<Object> reversedElements = new Stack<>();
        while (lookahead.tokenClass != TokenClass.CLOSEPAREN) {
            if (lookahead.tokenClass == TokenClass.PERIOD) {
                match(TokenClass.PERIOD);
                var dottedTail = readObject();
                var dottedHead = reversedElements.pop();
                match(TokenClass.CLOSEPAREN);
                return makeList(reversedElements, Pair.of(dottedHead, dottedTail));
            }
            reversedElements.push(readObject());
        }
        match(TokenClass.CLOSEPAREN);
        return makeList(reversedElements, null);
    }

    private Pair makeList(Stack<Object> reversedElements, Pair tail) {
        var head = tail;
        while (!reversedElements.isEmpty()) {
            head = Pair.of(reversedElements.pop(), head);
        }
        return head;
    }

    private Object readNumber() {
        var content = lookahead.content;
        scan();
        return Integer.parseInt(content);
    }

    private void scan() {
        lookahead = nextToken();
    }

    private void match(TokenClass expected) {
        if (lookahead.tokenClass != expected) {
            throw new RuntimeException("expected: " + expected + ", got: " + lookahead);
        }
        scan();
    }

    /*
        Scanner
     */

    private Token nextToken() {
        skipWhitespace();
        switch (hereChar) {
            case EOF:
                return new Token(TokenClass.END, "");
            case '(':
                scanChar();
                return new Token(TokenClass.OPENPAREN, "(");
            case ')':
                scanChar();
                return new Token(TokenClass.CLOSEPAREN, ")");
            case '\'':
                scanChar();
                return new Token(TokenClass.QUOTE, "'");
            case '"':
                return scanString();
            case '.':
                scanChar();
                return new Token(TokenClass.PERIOD, ".");
        }
        if (Character.isDigit(hereChar)) {
            return scanNumber();
        } else {
            return scanIdentifier();
        }
    }

    private void skipWhitespace() {
        while (Character.isWhitespace(hereChar)) {
            scanChar();
        }
    }

    private char scanChar() {
        try {
            hereChar = (char) input.read();
            if (hereChar == ';') {
                while (!(hereChar == '\n' || hereChar == '\r' || hereChar == EOF)) {
                    hereChar = (char) input.read();
                }
            }
            return hereChar;
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    private Token scanNumber() {
        StringBuilder builder = new StringBuilder();
        while (Character.isDigit(hereChar)) {
            builder.append(hereChar);
            scanChar();
        }
        return new Token(TokenClass.NUMBER, builder.toString());
    }

    private Token scanIdentifier() {
        StringBuilder builder = new StringBuilder();
        builder.append(hereChar);
        scanChar();
        while (isIdentifierRest(hereChar)) {
            builder.append(hereChar);
            scanChar();
        }
        return new Token(TokenClass.IDENTIFIER, builder.toString());
    }

    private Token scanString() {
        scanChar(); // the opening "
        StringBuilder builder = new StringBuilder();
        while (hereChar != '"' && hereChar != EOF) {
            builder.append(hereChar);
            scanChar();
        }
        if (hereChar == '"') scanChar(); // the closing "
        return new Token(TokenClass.STRING, builder.toString());
    }

    private boolean isIdentifierRest(char c) {
        return !(Character.isWhitespace(c) ||  c == EOF || c == '(' || c == ')' || c == '.');
    }
}
