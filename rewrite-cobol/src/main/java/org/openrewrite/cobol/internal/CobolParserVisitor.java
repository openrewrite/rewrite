/*
 * Copyright 2022 the original author or authors.
 * <p>
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * <p>
 * https://www.apache.org/licenses/LICENSE-2.0
 * <p>
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.openrewrite.cobol.internal;

import org.antlr.v4.runtime.ParserRuleContext;
import org.antlr.v4.runtime.Token;
import org.antlr.v4.runtime.tree.ParseTree;
import org.antlr.v4.runtime.tree.TerminalNode;
import org.openrewrite.FileAttributes;
import org.openrewrite.cobol.internal.grammar.CobolBaseVisitor;
import org.openrewrite.cobol.internal.grammar.CobolParser;
import org.openrewrite.cobol.tree.*;
import org.openrewrite.internal.lang.Nullable;
import org.openrewrite.marker.Markers;

import java.nio.charset.Charset;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.function.BiFunction;

import static java.util.Collections.emptyList;
import static org.openrewrite.Tree.randomId;

public class CobolParserVisitor extends CobolBaseVisitor<Cobol> {
    private final Path path;

    @Nullable
    private final FileAttributes fileAttributes;

    private StringWithOriginalPositions input;

    private final Charset charset;
    private final boolean charsetBomMarked;

    public CobolParserVisitor(Path path, @Nullable FileAttributes fileAttributes, StringWithOriginalPositions input, Charset charset, boolean charsetBomMarked) {
        this.path = path;
        this.fileAttributes = fileAttributes;
        this.input = input;
        this.charset = charset;
        this.charsetBomMarked = charsetBomMarked;
    }

    private Space space(ParseTree ctx1, ParseTree ctx2) {
        int stop;
        if (ctx1 instanceof ParserRuleContext) {
            stop = ((ParserRuleContext) ctx1).getStop().getStopIndex();
        } else if (ctx1 instanceof TerminalNode) {
            stop = ((TerminalNode) ctx1).getSymbol().getStopIndex();
        } else {
            throw new IllegalArgumentException();
        }
        int start;
        if (ctx2 instanceof ParserRuleContext) {
            start = ((ParserRuleContext) ctx2).getStart().getStartIndex();
        } else if (ctx2 instanceof TerminalNode) {
            start = ((TerminalNode) ctx2).getSymbol().getStartIndex();
        } else {
            throw new IllegalArgumentException();
        }
        return Space.build(input, stop + 1, start);
    }

    private String prefix(TerminalNode terminal) {
        // prefix is attached to the outermost node, namely never to a terminal
        return "";
    }

    private Space prefix(ParserRuleContext tree) {
        if (positionInParent(tree) == 0) {
            // prefix will be attached to an ancestor node
            return Space.EMPTY;
        }
        Token firstToken = tree.getStart();
        Token previousToken = previousToken(tree);
        if (previousToken == null) {
            return Space.build(input, 0, firstToken.getStartIndex());
        } else {
            return Space.build(input, previousToken.getStopIndex() + 1, firstToken.getStartIndex());
        }
    }

    private int positionInParent(ParseTree n) {
        ParseTree parent = n.getParent();
        int pos;
        for (pos = 0; pos < parent.getChildCount(); pos++) {
            if (parent.getChild(pos) == n) {
                break;
            }
        }
        assert pos < parent.getChildCount();
        return pos;
    }

    @Nullable
    private Token previousToken(ParseTree n) {
        ParseTree parent = n.getParent();
        if (n.getParent() == null) {
            return null;
        }
        int pos = positionInParent(n);
        if (pos == 0) {
            return previousToken(parent);
        } else {
            return lastToken(parent.getChild(pos - 1));
        }
    }

    private Token lastToken(ParseTree n) {
        if (n instanceof TerminalNode) {
            return ((TerminalNode) n).getSymbol();
        } else {
            return ((ParserRuleContext) n).stop;
        }
    }

    @Override
    public Cobol.CompilationUnit visitStartRule(CobolParser.StartRuleContext ctx) {
        return visitCompilationUnit(ctx.compilationUnit()).withEof(ctx.EOF().getText());
    }

    @Override
    public Cobol.CompilationUnit visitCompilationUnit(CobolParser.CompilationUnitContext ctx) {

        List<CobolParser.ProgramUnitContext> puCtxs = ctx.programUnit();
        List<CobolRightPadded<Cobol.ProgramUnit>> programUnits = new ArrayList<>(puCtxs.size());
        for (CobolParser.ProgramUnitContext puCtx : puCtxs) {
            Cobol.ProgramUnit pu = (Cobol.ProgramUnit) visitProgramUnit(puCtx);
            CobolRightPadded<Cobol.ProgramUnit> ppu = CobolRightPadded.build(pu);
            programUnits.add(ppu);
        }

        return new Cobol.CompilationUnit(
                randomId(),
                path,
                fileAttributes,
                prefix(ctx),
                Markers.EMPTY,
                charset.name(),
                charsetBomMarked,
                null,
                programUnits,
                ""
        );
    }

    @Override
    public Cobol visitProgramUnit(CobolParser.ProgramUnitContext ctx) {
        Cobol.IdentificationDivision identificationDivision = (Cobol.IdentificationDivision) visitIdentificationDivision(ctx.identificationDivision());
        Cobol.ProcedureDivision procedureDivision;
        if (ctx.procedureDivision() == null) {
            procedureDivision = null;
        } else {
            procedureDivision = (Cobol.ProcedureDivision) visitProcedureDivision(ctx.procedureDivision());
        }
        return new Cobol.ProgramUnit(
                randomId(),
                prefix(ctx),
                Markers.EMPTY,
                identificationDivision,
                procedureDivision);
    }

    @Override
    public Cobol visitProcedureDivision(CobolParser.ProcedureDivisionContext ctx) {
        if (ctx.procedureDivisionUsingClause() != null) {
            throw new UnsupportedOperationException("Not implemented");
        }
        if (ctx.procedureDivisionGivingClause() != null) {
            throw new UnsupportedOperationException("Not implemented");
        }
        if (ctx.procedureDeclaratives() != null) {
            throw new UnsupportedOperationException("Not implemented");
        }

        CobolRightPadded<String> procedure = CobolRightPadded.build(ctx.PROCEDURE().getText()).withAfter(space(ctx.PROCEDURE(), ctx.DIVISION()));
        CobolRightPadded<String> division = CobolRightPadded.build(ctx.DIVISION().getText()).withAfter(space(ctx.DIVISION(), ctx.DOT_FS()));
        String dot = ctx.DOT_FS().getText();
        Cobol.ProcedureDivisionBody procedureDivisionBody = (Cobol.ProcedureDivisionBody) visitProcedureDivisionBody(ctx.procedureDivisionBody());
        return new Cobol.ProcedureDivision(
                randomId(),
                prefix(ctx),
                Markers.EMPTY,
                procedure,
                division,
                dot,
                procedureDivisionBody
        );
    }

    @Override
    public Cobol visitProcedureDivisionBody(CobolParser.ProcedureDivisionBodyContext ctx) {
        if (ctx.procedureSection() != null && ctx.procedureSection().size() > 0) {
            throw new UnsupportedOperationException("Not implemented");
        }
        Cobol.Paragraphs paragraphs = (Cobol.Paragraphs) visitParagraphs(ctx.paragraphs());
        return new Cobol.ProcedureDivisionBody(
                randomId(),
                prefix(ctx),
                Markers.EMPTY,
                paragraphs
        );
    }

    @Override
    public Cobol visitParagraphs(CobolParser.ParagraphsContext ctx) {
        if (ctx.paragraph() != null && ctx.paragraph().size() > 0) {
            throw new UnsupportedOperationException("Not implemented");
        }
        List<Cobol.Sentence> sentences = new ArrayList<>();
        for (int i = 0; i < ctx.sentence().size(); i++) {
            sentences.add((Cobol.Sentence) visitSentence(ctx.sentence(i)));
        }
        return new Cobol.Paragraphs(
                randomId(),
                prefix(ctx),
                Markers.EMPTY,
                sentences);
    }

    @Override
    public Cobol visitSentence(CobolParser.SentenceContext ctx) {
        List<Statement> statements = new ArrayList<>();
        for (int i = 0; i < ctx.statement().size(); i++) {
            statements.add((Statement) visit(ctx.statement(i)));
        }
        CobolLeftPadded<String> dot = CobolLeftPadded.build(ctx.DOT_FS().getText())
                .withBefore(ctx.statement().size() == 0 ? Space.EMPTY : space(ctx.statement(ctx.statement().size() - 1), ctx.DOT_FS()));
        return new Cobol.Sentence(
                randomId(),
                prefix(ctx),
                Markers.EMPTY,
                statements,
                dot);
    }

    @Override
    public Cobol visitStopStatement(CobolParser.StopStatementContext ctx) {
        if (ctx.literal() != null) {
            throw new UnsupportedOperationException("Not implemented");
        }
        if (ctx.stopStatementGiving() != null) {
            throw new UnsupportedOperationException("Not implemented");
        }

        String stop = ctx.STOP().getText();
        CobolLeftPadded<String> run = null;
        if (ctx.RUN() != null) {
            run = CobolLeftPadded.build(ctx.RUN().getText()).withBefore(space(ctx.STOP(), ctx.RUN()));
        }
        return new Cobol.Stop(
                randomId(),
                prefix(ctx),
                Markers.EMPTY,
                stop,
                run,
                null);
    }

    @Override
    public Cobol visitDisplayStatement(CobolParser.DisplayStatementContext ctx) {
        if (ctx.displayAt() != null) {
            throw new UnsupportedOperationException("Not implemented");
        }
        if (ctx.displayUpon() != null) {
            throw new UnsupportedOperationException("Not implemented");
        }
        if (ctx.displayWith() != null) {
            throw new UnsupportedOperationException("Not implemented");
        }
        if (ctx.onExceptionClause() != null) {
            throw new UnsupportedOperationException("Not implemented");
        }
        if (ctx.notOnExceptionClause() != null) {
            throw new UnsupportedOperationException("Not implemented");
        }
        if (ctx.END_DISPLAY() != null) {
            throw new UnsupportedOperationException("Not implemented");
        }

        List<CobolLeftPadded<String>> operands = new ArrayList<>();
        ParseTree previousCtx = ctx.DISPLAY();
        for (int i = 0; i < ctx.displayOperand().size(); i++) {
            ParserRuleContext operandCtx = ctx.displayOperand(i);
            Space before = space(previousCtx, operandCtx);
            previousCtx = operandCtx;
            String element = operandCtx.getText();
            operands.add(new CobolLeftPadded<>(before, element, Markers.EMPTY));
        }
        return new Cobol.Display(
                randomId(),
                prefix(ctx),
                Markers.EMPTY,
                operands);
    }

    @Override
    public Cobol visitIdentificationDivision(CobolParser.IdentificationDivisionContext ctx) {

        if (ctx.identificationDivisionBody() != null && ctx.identificationDivisionBody().size() != 0) {
            throw new UnsupportedOperationException("Not implemented");
        }

        Cobol.ProgramIdParagraph programIdParagraph = (Cobol.ProgramIdParagraph) visitProgramIdParagraph(ctx.programIdParagraph());
        TerminalNode idTerminal = ctx.IDENTIFICATION() == null ? ctx.ID() : ctx.IDENTIFICATION();
        Space space1 = space(idTerminal, ctx.DIVISION());
        Space space2 = space(ctx.DIVISION(), ctx.DOT_FS());

        CobolRightPadded<String> identification = CobolRightPadded.build(idTerminal.getText()).withAfter(space1);
        CobolRightPadded<String> division = CobolRightPadded.build(ctx.DIVISION().getText()).withAfter(space2);
        String dot = ctx.DOT_FS().getText();

        return new Cobol.IdentificationDivision(
                randomId(),
                prefix(ctx),
                Markers.EMPTY,
                identification,
                division,
                dot,
                programIdParagraph
        );
    }

    @Override
    public Cobol visitProgramIdParagraph(CobolParser.ProgramIdParagraphContext ctx) {
        CobolRightPadded<String> programId = CobolRightPadded.build(ctx.PROGRAM_ID().getText())
                .withAfter(space(ctx.PROGRAM_ID(), ctx.DOT_FS(0)));
        CobolRightPadded<String> dot1 = CobolRightPadded.build(ctx.DOT_FS(0).getText())
                .withAfter(space(ctx.DOT_FS(0), ctx.programName()));
        String programName = ctx.programName().getText();
        CobolLeftPadded<String> dot2 = null;
        if (ctx.DOT_FS().size() > 1) {
            dot2 = CobolLeftPadded.build(ctx.DOT_FS(1).getText())
                    .withBefore(space(ctx.programName(), ctx.DOT_FS(1)));
        }
        return new Cobol.ProgramIdParagraph(
                randomId(),
                prefix(ctx),
                Markers.EMPTY,
                programId,
                dot1,
                programName,
                dot2
        );
    }

    @Nullable
    private <C extends ParserRuleContext, T> T convert(@Nullable C ctx, BiFunction<C, Space, T> conversion) {
        if (ctx == null) {
            return null;
        }
        T t = conversion.apply(ctx, prefix(ctx));

        return t;
    }

    private <T> T convert(TerminalNode node, BiFunction<TerminalNode, String, T> conversion) {
        T t = conversion.apply(node, prefix(node));
        return t;
    }
}
