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
import org.antlr.v4.runtime.RuleContext;
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
import java.util.function.Function;
import java.util.function.Supplier;
import java.util.stream.Collectors;

import static org.openrewrite.Tree.randomId;
import static org.openrewrite.cobol.tree.Space.format;
import static org.openrewrite.internal.StringUtils.indexOfNextNonWhitespace;

public class CobolParserVisitor extends CobolBaseVisitor<Object> {
    private final Path path;

    @Nullable
    private final FileAttributes fileAttributes;

    private final String source;
    private final Charset charset;
    private final boolean charsetBomMarked;

    private int cursor = 0;

    public CobolParserVisitor(Path path, @Nullable FileAttributes fileAttributes,
                              String source, Charset charset, boolean charsetBomMarked) {
        this.path = path;
        this.fileAttributes = fileAttributes;
        this.source = source;
        this.charset = charset;
        this.charsetBomMarked = charsetBomMarked;
    }

    public <T> T visit(@Nullable ParseTree... trees) {
        for (ParseTree tree : trees) {
            if (tree != null) {
                //noinspection unchecked
                return (T) visit(tree);
            }
        }
        throw new IllegalStateException("Expected one of the supplied trees to be non-null");
    }

    public <T> T visitNullable(@Nullable ParseTree tree) {
        if (tree == null) {
            //noinspection ConstantConditions
            return null;
        }
        //noinspection unchecked
        return (T) super.visit(tree);
    }

    @Override
    public Cobol.CompilationUnit visitStartRule(CobolParser.StartRuleContext ctx) {
        return visitCompilationUnit(ctx.compilationUnit());
    }

    @Override
    public Cobol.CompilationUnit visitCompilationUnit(CobolParser.CompilationUnitContext ctx) {
        Space prefix = prefix(ctx);
        List<CobolRightPadded<Cobol.ProgramUnit>> programUnits = new ArrayList<>(ctx.programUnit().size());
        for (CobolParser.ProgramUnitContext pu : ctx.programUnit()) {
            programUnits.add(CobolRightPadded.build(visitProgramUnit(pu)));
        }
        return new Cobol.CompilationUnit(
                randomId(),
                path,
                fileAttributes,
                prefix,
                Markers.EMPTY,
                charset.name(),
                charsetBomMarked,
                null,
                programUnits,
                source.substring(cursor)
        );
    }

    @Override
    public Cobol.Add visitAddStatement(CobolParser.AddStatementContext ctx) {
        return new Cobol.Add(
                randomId(),
                sourceBefore(ctx.ADD().getText()),
                Markers.EMPTY,
                ctx.ADD().getText(),
                visit(ctx.addToStatement(), ctx.addToGivingStatement(), ctx.addCorrespondingStatement()),
                ctx.onSizeErrorPhrase() == null ? visitNullable(ctx.notOnSizeErrorPhrase()) :
                        visitNullable(ctx.onSizeErrorPhrase()),
                padLeft(ctx.END_ADD())
        );
    }

    @Override
    public Cobol.AddTo visitAddToStatement(CobolParser.AddToStatementContext ctx) {
        CobolContainer<Name> from = convertAllContainer(ctx.addFrom());
        Space beforeTo = sourceBefore("TO");
        return new Cobol.AddTo(
                randomId(),
                Space.EMPTY,
                Markers.EMPTY,
                from,
                this.<Name, CobolParser.AddToContext>convertAllContainer(ctx.addTo())
                        .withBefore(beforeTo),
                null
        );
    }

    @Override
    public Cobol.Set visitSetStatement(CobolParser.SetStatementContext ctx) {
        return new Cobol.Set(
                randomId(),
                sourceBefore(ctx.SET().getText()),
                Markers.EMPTY,
                ctx.SET().getText(),
                convertAllContainer(ctx.setToStatement()),
                visitNullable(ctx.setUpDownByStatement())
        );
    }

    @Override
    public Cobol.SetTo visitSetToStatement(CobolParser.SetToStatementContext ctx) {
        Space prefix = prefix(ctx);
        CobolContainer<Cobol.Identifier> to = convertAllContainer(ctx.setTo());
        Space beforeValues = sourceBefore("TO");
        return new Cobol.SetTo(
                randomId(),
                prefix,
                Markers.EMPTY,
                to,
                this.<Name, CobolParser.SetToValueContext>convertAllContainer(ctx.setToValue())
                        .withBefore(beforeValues)
        );
    }

    @Override
    public Cobol.SetUpDown visitSetUpDownByStatement(CobolParser.SetUpDownByStatementContext ctx) {
        return new Cobol.SetUpDown(
                randomId(),
                prefix(ctx),
                Markers.EMPTY,
                convertAllContainer(ctx.setTo()),
                padLeft(ctx.UP() == null ? ctx.DOWN() : ctx.UP()),
                (Name) visit(ctx.setByValue())
        );
    }

    @Override
    public Cobol.DataDivision visitDataDivision(CobolParser.DataDivisionContext ctx) {
        return new Cobol.DataDivision(
                randomId(),
                sourceBefore(ctx.DATA().getText()),
                Markers.EMPTY,
                words(ctx.DATA(), ctx.DIVISION()),
                convertAllContainer(ctx.dataDivisionSection())
        );
    }

    @Override
    public Cobol.WorkingStorageSection visitWorkingStorageSection(CobolParser.WorkingStorageSectionContext ctx) {
        return new Cobol.WorkingStorageSection(
                randomId(),
                sourceBefore(ctx.WORKING_STORAGE().getText()),
                Markers.EMPTY,
                words(ctx.WORKING_STORAGE(), ctx.SECTION()),
                convertAllContainer(ctx.dataDescriptionEntry())
        );
    }

    @Override
    public Cobol.DataDescriptionEntry visitDataDescriptionEntryFormat1(CobolParser.DataDescriptionEntryFormat1Context ctx) {
        TerminalNode level = ctx.INTEGERLITERAL() == null ? ctx.LEVEL_NUMBER_77() : ctx.INTEGERLITERAL();
        return new Cobol.DataDescriptionEntry(
                randomId(),
                sourceBefore(level.getText()),
                Markers.EMPTY,
                Integer.parseInt(level.getText()),
                ctx.FILLER() == null ?
                        (ctx.dataName() == null ? null : padLeft(ctx.dataName())) :
                        padLeft(ctx.FILLER()),
                convertAllContainer(ctx.dataDescriptionEntryFormat1Clause())
        );
    }

    @Override
    public Cobol.ProgramUnit visitProgramUnit(CobolParser.ProgramUnitContext ctx) {
        return new Cobol.ProgramUnit(
                randomId(),
                prefix(ctx),
                Markers.EMPTY,
                (Cobol.IdentificationDivision) visitIdentificationDivision(ctx.identificationDivision()),
                visitNullable(ctx.environmentDivision()),
                visitNullable(ctx.dataDivision()),
                visitNullable(ctx.procedureDivision()),
                convertAllContainer(ctx.programUnit()),
                ctx.endProgramStatement() == null ? null : padRight(visitEndProgramStatement(ctx.endProgramStatement()),
                        sourceBefore("."))
        );
    }

    @Override
    public Cobol.EndProgram visitEndProgramStatement(CobolParser.EndProgramStatementContext ctx) {
        return new Cobol.EndProgram(
                randomId(),
                sourceBefore(ctx.END().getText()),
                Markers.EMPTY,
                words(ctx.END(), ctx.PROGRAM()),
                visitProgramName(ctx.programName())
        );
    }

    @Override
    public Cobol.EnvironmentDivision visitEnvironmentDivision(CobolParser.EnvironmentDivisionContext ctx) {
        return (Cobol.EnvironmentDivision) super.visitEnvironmentDivision(ctx);
    }

    @Override
    public Cobol.ProcedureDivision visitProcedureDivision(CobolParser.ProcedureDivisionContext ctx) {
        if (ctx.procedureDivisionUsingClause() != null || ctx.procedureDivisionGivingClause() != null ||
                ctx.procedureDeclaratives() != null) {
            throw new UnsupportedOperationException("Implement me");
        }
        return new Cobol.ProcedureDivision(
                randomId(),
                sourceBefore(ctx.PROCEDURE().getText()),
                Markers.EMPTY,
                words(ctx.PROCEDURE(), ctx.DIVISION()),
                padLeft(sourceBefore("."), visitProcedureDivisionBody(ctx.procedureDivisionBody()))
        );
    }

    @Override
    public Cobol.ProcedureDivisionBody visitProcedureDivisionBody(CobolParser.ProcedureDivisionBodyContext ctx) {
        if (ctx.procedureSection() != null && !ctx.procedureSection().isEmpty()) {
            throw new UnsupportedOperationException("Implement me");
        }
        return new Cobol.ProcedureDivisionBody(
                randomId(),
                prefix(ctx),
                Markers.EMPTY,
                visitParagraphs(ctx.paragraphs())
        );
    }

    @Override
    public Cobol.Paragraphs visitParagraphs(CobolParser.ParagraphsContext ctx) {
        if (ctx.paragraph() != null && !ctx.paragraph().isEmpty()) {
            throw new UnsupportedOperationException("Implement me");
        }
        return new Cobol.Paragraphs(
                randomId(),
                prefix(ctx),
                Markers.EMPTY,
                convertAllContainer(ctx.sentence(), () -> sourceBefore("."))
        );
    }

    @Override
    public Cobol.Sentence visitSentence(CobolParser.SentenceContext ctx) {
        return new Cobol.Sentence(
                randomId(),
                prefix(ctx),
                Markers.EMPTY,
                convertAll(ctx.statement())
        );
    }

    @Override
    public Cobol.Stop visitStopStatement(CobolParser.StopStatementContext ctx) {
        if (ctx.literal() != null || ctx.stopStatementGiving() != null) {
            throw new UnsupportedOperationException("Implement me");
        }
        return new Cobol.Stop(
                randomId(),
                sourceBefore(ctx.STOP().getText()),
                Markers.EMPTY,
                words(ctx.STOP(), ctx.RUN()),
                null
        );
    }

    @Override
    public Cobol.Display visitDisplayStatement(CobolParser.DisplayStatementContext ctx) {
        if (ctx.displayAt() != null || ctx.displayUpon() != null || ctx.displayWith() != null ||
                ctx.onExceptionClause() != null || ctx.notOnExceptionClause() != null ||
                ctx.END_DISPLAY() != null) {
            throw new UnsupportedOperationException("Implement me");
        }
        return new Cobol.Display(
                randomId(),
                sourceBefore(ctx.DISPLAY().getText()),
                Markers.EMPTY,
                ctx.DISPLAY().getText(),
                convertAll(ctx.displayOperand())
        );
    }

    @Override
    public Cobol visitIdentificationDivision(CobolParser.IdentificationDivisionContext ctx) {
        if (ctx.identificationDivisionBody() != null && ctx.identificationDivisionBody().size() != 0) {
            throw new UnsupportedOperationException("Implement me");
        }
        TerminalNode id = (ctx.IDENTIFICATION() == null ? ctx.ID() : ctx.IDENTIFICATION());
        return new Cobol.IdentificationDivision(
                randomId(),
                sourceBefore(id.getText()),
                Markers.EMPTY,
                words(id, ctx.DIVISION()),
                padLeft(sourceBefore("."), visitProgramIdParagraph(ctx.programIdParagraph()))
        );
    }

    @Override
    public Cobol.Literal visitLiteral(CobolParser.LiteralContext ctx) {
        return new Cobol.Literal(
                randomId(),
                sourceBefore(ctx.getText()),
                Markers.EMPTY,
                ctx.getText(), // TODO extract literal values from various literal types
                ctx.getText()
        );
    }

    @Override
    public Cobol.Picture visitPicture(CobolParser.PictureContext ctx) {
        return new Cobol.Picture(
                randomId(),
                prefix(ctx),
                Markers.EMPTY,
                ctx.pictureChars().stream()
                        .map(RuleContext::getText)
                        .collect(Collectors.joining("")),
                padLeft(ctx.pictureCardinality())
        );
    }

    @Override
    public Cobol.ProgramIdParagraph visitProgramIdParagraph(CobolParser.ProgramIdParagraphContext ctx) {
        return new Cobol.ProgramIdParagraph(
                randomId(),
                sourceBefore(ctx.PROGRAM_ID().getText()),
                Markers.EMPTY,
                ctx.PROGRAM_ID().getText(),
                padLeft(sourceBefore("."), visitProgramName(ctx.programName())),
                padLeft(whitespace(), words(ctx.IS(), ctx.COMMON(), ctx.INITIAL(), ctx.LIBRARY(), ctx.DEFINITION(), ctx.RECURSIVE(), ctx.PROGRAM())),
                ctx.DOT_FS().size() > 1 ? padLeft(ctx.DOT_FS(1)) : null
        );
    }

    @Override
    public Name visitProgramName(CobolParser.ProgramNameContext ctx) {
        return ctx.NONNUMERICLITERAL() == null ?
                new Cobol.Identifier(randomId(),
                        sourceBefore(ctx.getText()), Markers.EMPTY,
                        ctx.getText()) :
                new Cobol.Literal(randomId(),
                        sourceBefore(ctx.getText()), Markers.EMPTY,
                        ctx.getText(), ctx.getText());
    }

    private <C, T extends ParseTree> List<C> convertAll(List<T> trees, Function<T, C> convert) {
        List<C> converted = new ArrayList<>(trees.size());
        for (T tree : trees) {
            converted.add(convert.apply(tree));
        }
        return converted;
    }

    private <C extends Cobol, T extends ParseTree> List<C> convertAll(List<T> trees) {
        //noinspection unchecked
        return convertAll(trees, t -> (C) visit(t));
    }

    private <C extends Cobol, T extends ParseTree> CobolContainer<C> convertAllContainer(List<T> trees) {
        return convertAllContainer(trees, () -> Space.EMPTY);
    }

    private <C extends Cobol, T extends ParseTree> CobolContainer<C> convertAllContainer(List<T> trees, Supplier<Space> sourceBefore) {
        //noinspection unchecked
        return CobolContainer.build(convertAll(trees, t -> padRight((C) visit(t), sourceBefore.get())));
    }

    private Space prefix(ParserRuleContext ctx) {
        return prefix(ctx.getStart());
    }

    private Space prefix(Token token) {
        int start = token.getStartIndex();
        if (start < cursor) {
            return format("");
        }
        String prefix = source.substring(cursor, start);
        cursor = start;
        return format(prefix);
    }

    private Space sourceBefore(String untilDelim) {
        return sourceBefore(untilDelim, null);
    }

    /**
     * @return Source from <code>cursor</code> to next occurrence of <code>untilDelim</code>,
     * and if not found in the remaining source, the empty String. If <code>stop</code> is reached before
     * <code>untilDelim</code> return the empty String.
     */
    private Space sourceBefore(String untilDelim, @Nullable Character stop) {
        int delimIndex = positionOfNext(untilDelim, stop);
        if (delimIndex < 0) {
            return Space.EMPTY; // unable to find this delimiter
        }

        String prefix = source.substring(cursor, delimIndex);
        cursor += prefix.length() + untilDelim.length(); // advance past the delimiter
        return format(prefix);
    }

    private <T> CobolRightPadded<T> padRight(T tree, Space right) {
        return new CobolRightPadded<>(tree, right, Markers.EMPTY);
    }

    private <T> CobolLeftPadded<T> padLeft(Space left, T tree) {
        return new CobolLeftPadded<>(left, tree, Markers.EMPTY);
    }

    private CobolLeftPadded<String> padLeft(@Nullable ParseTree pt) {
        if (pt == null) {
            //noinspection ConstantConditions
            return null;
        }
        return padLeft(sourceBefore(pt.getText()), pt.getText());
    }

    private int positionOfNext(String untilDelim, @Nullable Character stop) {
        int delimIndex = cursor;
        for (; delimIndex < source.length() - untilDelim.length() + 1; delimIndex++) {
            if (stop != null && source.charAt(delimIndex) == stop)
                return -1; // reached stop word before finding the delimiter

            if (source.startsWith(untilDelim, delimIndex)) {
                break; // found it!
            }
        }

        return delimIndex > source.length() - untilDelim.length() ? -1 : delimIndex;
    }

    private String words(TerminalNode... wordNodes) {
        StringBuilder words = new StringBuilder();
        for (TerminalNode wordNode : wordNodes) {
            if (wordNode != null) {
                words.append(sourceBefore(wordNode.getText()).getWhitespace());
                words.append(wordNode.getText());
            }
        }
        return words.toString();
    }

    private Space whitespace() {
        String prefix = source.substring(cursor, indexOfNextNonWhitespace(cursor, source));
        cursor += prefix.length();
        return format(prefix);
    }
}
