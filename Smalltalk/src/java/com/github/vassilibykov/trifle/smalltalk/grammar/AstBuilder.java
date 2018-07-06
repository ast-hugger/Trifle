// Copyright (c) 2018 Vassili Bykov. Licensed under the Apache License, Version 2.0.

package com.github.vassilibykov.trifle.smalltalk.grammar;

import org.antlr.v4.runtime.CharStreams;
import org.antlr.v4.runtime.CommonTokenStream;
import org.antlr.v4.runtime.ParserRuleContext;
import org.antlr.v4.runtime.tree.TerminalNode;

import java.io.IOException;
import java.io.Reader;
import java.io.StringReader;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * A translator of ANTRLR contexts into {@link AstNode AstNodes}.
 */
public class AstBuilder extends SmalltalkBaseVisitor<AstNode> {

    public static SourceUnit parseClass(String input) {
        return parseClass(new StringReader(input));
    }

    public static SourceUnit parseClass(Reader reader) {
        var parser = createParser(reader);
        var sourceUnitContext = parser.sourceUnit();
        if (parser.getNumberOfSyntaxErrors() > 0) {
            throw new RuntimeException("parsing failed");
        }
        var builder = new AstBuilder();
        return (SourceUnit) sourceUnitContext.accept(builder);
    }

    public static MethodDeclaration parseMethod(String methodSource) {
        var parser = createParser(new StringReader(methodSource));
        var methodContext = parser.methodDecl();
        if (parser.getNumberOfSyntaxErrors() > 0) {
            throw new RuntimeException("method parsing failed");
        }
        var builder = new AstBuilder();
        return (MethodDeclaration) methodContext.accept(builder);
    }

    private static SmalltalkParser createParser(Reader reader) {
        try {
            var input = CharStreams.fromReader(reader);
            var lexer = new SmalltalkLexer(input);
            var tokens = new CommonTokenStream(lexer);
            return new SmalltalkParser(tokens);
        } catch (IOException e) {
            throw new RuntimeException("error reading compiler input", e);
        }
    }

    /*
        Instance side
     */

    private AstBuilder() {}

    @Override
    public AstNode visitSourceUnit(SmalltalkParser.SourceUnitContext ctx) {
        var classDecl = (ClassDeclaration) ctx.classDecl().accept(this);
        var instanceMethods = new ArrayList<MethodDeclaration>();
        var classMethods = new ArrayList<MethodDeclaration>();
        ctx.methodEntry().forEach(each -> {
            var isClassMethod = "!!".equals(each.getChild(0).getText());
            var method = (MethodDeclaration) each.methodDecl().accept(this);
            if (isClassMethod) classMethods.add(method);
            else instanceMethods.add(method);
        });
        return new SourceUnit(classDecl, instanceMethods, classMethods);
    }

    @Override
    public AstNode visitClassDecl(SmalltalkParser.ClassDeclContext ctx) {
        var superclassName = ctx.IDENTIFIER(0).getText();
        var className = ctx.IDENTIFIER(1).getText();
        var instVarNames = ctx.instVarName().stream()
            .map(each -> each.IDENTIFIER().getText())
            .collect(Collectors.toList());
        return new ClassDeclaration(className, superclassName, instVarNames);
    }

    @Override
    public AstNode visitMethodDecl(SmalltalkParser.MethodDeclContext ctx) {
        var selector = selectorOf(ctx.messagePattern());
        var argNames = argumentNames(ctx.messagePattern());
        var tempNames = ctx.IDENTIFIER().stream()
            .map(each -> each.getText())
            .collect(Collectors.toList());
        var expressions = ctx.codeBody().statement().stream()
            .map(each -> each.accept(this))
            .collect(Collectors.toList());
        return new MethodDeclaration(selector, argNames, tempNames, expressions);
    }

    private String selectorOf(SmalltalkParser.MessagePatternContext ctx) {
        if (ctx instanceof SmalltalkParser.UnaryPatternContext) {
            return ((SmalltalkParser.UnaryPatternContext) ctx).IDENTIFIER().getText();
        } else if (ctx instanceof SmalltalkParser.BinaryPatternContext) {
            return ((SmalltalkParser.BinaryPatternContext) ctx).IDENTIFIER().getText();
        } else {
            var result = new StringBuilder();
            for (int i = 0; i < ctx.getChildCount(); i += 2) {
                result.append(ctx.getChild(i).getText());
            }
            return result.toString();
        }
    }

    private List<String> argumentNames(SmalltalkParser.MessagePatternContext ctx) {
        if (ctx instanceof SmalltalkParser.UnaryPatternContext) {
            return List.of();
        } else if (ctx instanceof SmalltalkParser.BinaryPatternContext) {
            return List.of(((SmalltalkParser.BinaryPatternContext) ctx).IDENTIFIER().getText());
        } else {
            var result = new ArrayList<String>();
            for (int i = 1; i < ctx.getChildCount(); i += 2) {
                result.add(ctx.getChild(i).getText());
            }
            return result;
        }
    }

    @Override
    public AstNode visitUnarySend(SmalltalkParser.UnarySendContext ctx) {
        var receiver = ctx.object().accept(this);
        return buildUnaryChain(receiver, ctx.unaryMessage());
    }

    private AstNode buildUnaryChain(AstNode receiver, SmalltalkParser.UnaryMessageContext ctx) {
        var send = new MessageSend(receiver, ctx.IDENTIFIER().getText(), List.of());
        if (ctx.unaryMessage() == null) {
            return send;
        } else {
            return buildUnaryChain(send, ctx.unaryMessage());
        }
    }

    @Override
    public AstNode visitBinarySend(SmalltalkParser.BinarySendContext ctx) {
        var receiver = ctx.binaryReceiver().accept(this);
        return buildBinaryChain(receiver, ctx.binaryMessage());
    }

    private AstNode buildBinaryChain(AstNode receiver, SmalltalkParser.BinaryMessageContext ctx) {
        var selector = ctx.BINARY_SELECTOR().getText();
        var arg = ctx.binaryReceiver().accept(this);
        var send = new MessageSend(receiver, selector, List.of(arg));
        if (ctx.binaryMessage() == null) {
            return send;
        } else {
            return buildBinaryChain(send, ctx.binaryMessage());
        }
    }

    @Override
    public AstNode visitKeywordSend(SmalltalkParser.KeywordSendContext ctx) {
        var receiver = ctx.keywordReceiver().accept(this);
        return new MessageSend(receiver, selectorOf(ctx.keywordMessage()), argumentsOf(ctx.keywordMessage()));
    }

    private String selectorOf(SmalltalkParser.KeywordMessageContext ctx) {
        return ctx.KEYWORD().stream()
            .map(each -> each.getText())
            .collect(Collectors.joining());
    }

    private List<AstNode> argumentsOf(SmalltalkParser.KeywordMessageContext ctx) {
        return ctx.keywordReceiver().stream()
            .map(each -> each.accept(this))
            .collect(Collectors.toList());
    }

    @Override
    public AstNode visitVarReference(SmalltalkParser.VarReferenceContext ctx) {
        var name = ctx.IDENTIFIER().getText();
        return new VarReference(name);
    }

    @Override
    public AstNode visitAssignment(SmalltalkParser.AssignmentContext ctx) {
        return new Assignment(ctx.IDENTIFIER().getText(), ctx.expression().accept(this));
    }

    @Override
    public AstNode visitReturnStatement(SmalltalkParser.ReturnStatementContext ctx) {
        return new Return(ctx.expression().accept(this));
    }

    @Override
    public AstNode visitBlock(SmalltalkParser.BlockContext ctx) {
        var argNames = streamOverNullable(ctx.blockArgs(), it -> it.BLOCK_ARG())
            .map(each -> each.getText().substring(1))
            .collect(Collectors.toList());
        var tempNames = streamOverNullable(ctx.blockTemps(), it -> it.IDENTIFIER())
            .map(each -> each.getText())
            .collect(Collectors.toList());
        var expressions = ctx.codeBody().statement().stream()
            .map(each -> each.accept(this))
            .collect(Collectors.toList());
        return new Block(argNames, tempNames, expressions);
    }

    @Override
    public AstNode visitNilReceiver(SmalltalkParser.NilReceiverContext ctx) {
        return new Literal(null);
    }

    @Override
    public AstNode visitTrueReceiver(SmalltalkParser.TrueReceiverContext ctx) {
        return new Literal(true);
    }

    @Override
    public AstNode visitFalseReceiver(SmalltalkParser.FalseReceiverContext ctx) {
        return new Literal(false);
    }

    @Override
    public AstNode visitSelfReceiver(SmalltalkParser.SelfReceiverContext ctx) {
        return new VarReference("self");
    }

    @Override
    public AstNode visitSuperReceiver(SmalltalkParser.SuperReceiverContext ctx) {
        throw new UnsupportedOperationException("not implemented yet"); // TODO implement
    }

    @Override
    public AstNode visitIntegerLiteral(SmalltalkParser.IntegerLiteralContext ctx) {
        var value = Integer.valueOf(ctx.INTEGER().getText()); // FIXME no radix syntax support
        return new Literal(value);
    }

    @Override
    public AstNode visitStringLiteral(SmalltalkParser.StringLiteralContext ctx) {
        String text = ctx.STRING().getText();
        return new Literal(text.substring(1, text.length() - 1)); // strip the quotes
    }

    @Override
    public AstNode visitObject(SmalltalkParser.ObjectContext ctx) {
        if (ctx.expression() != null) return ctx.expression().accept(this);
        else return super.visitObject(ctx);
    }

    private <T extends ParserRuleContext> Stream<TerminalNode> streamOverNullable(T context, Function<T, List<TerminalNode>> listExtractor) {
        if (context != null) {
            return listExtractor.apply(context).stream();
        } else {
            return Stream.empty();
        }
    }
}
