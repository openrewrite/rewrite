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

import lombok.Getter;
import lombok.With;
import org.antlr.v4.runtime.Lexer;
import org.antlr.v4.runtime.ParserRuleContext;
import org.antlr.v4.runtime.Token;
import org.antlr.v4.runtime.TokenSource;
import org.antlr.v4.runtime.tree.ParseTree;
import org.antlr.v4.runtime.tree.TerminalNode;
import org.openrewrite.FileAttributes;
import org.openrewrite.cobol.internal.grammar.CobolBaseVisitor;
import org.openrewrite.cobol.internal.grammar.CobolLexer;
import org.openrewrite.cobol.internal.grammar.CobolParser;
import org.openrewrite.cobol.tree.Cobol;
import org.openrewrite.cobol.tree.CobolRightPadded;
import org.openrewrite.cobol.tree.Space;
import org.openrewrite.internal.ListUtils;
import org.openrewrite.internal.lang.Nullable;
import org.openrewrite.marker.Markers;

import java.nio.charset.Charset;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.function.BiFunction;
import java.util.stream.Collectors;

import static org.openrewrite.Tree.randomId;

public class CobolParserVisitor extends CobolBaseVisitor<Cobol> {
    private final Path path;

    @Nullable
    private final FileAttributes fileAttributes;

    private final String source;
    private final Charset charset;
    private final boolean charsetBomMarked;

    private int cursor = 0;

    public CobolParserVisitor(Path path, @Nullable FileAttributes fileAttributes, String source, Charset charset, boolean charsetBomMarked) {
        this.path = path;
        this.fileAttributes = fileAttributes;
        this.source = source;
        this.charset = charset;
        this.charsetBomMarked = charsetBomMarked;
    }

    private int startIndex(ParserRuleContext ctx) {
        return ctx.start.getStartIndex();
    }

    private int stopIndex(ParserRuleContext ctx) {
        return ctx.stop.getStopIndex();
    }

    private String space(TerminalNode n1, TerminalNode n2) {
        return source.substring(n1.getSymbol().getStopIndex() + 1, n2.getSymbol().getStartIndex());
    }

    private String space(TerminalNode n1, ParserRuleContext ctx2) {
        return source.substring(n1.getSymbol().getStopIndex() + 1, ctx2.getStart().getStartIndex());
    }

    private String space(ParserRuleContext ctx1, TerminalNode n2) {
        return source.substring(ctx1.getStop().getStopIndex() + 1, n2.getSymbol().getStartIndex());
    }

    private String space(ParserRuleContext ctx1, ParserRuleContext ctx2) {
        return source.substring(ctx1.getStop().getStopIndex() + 1, ctx2.getStart().getStartIndex());
    }
    
    private Space prefix(TerminalNode terminal) {
        TerminalNode previousNode = previousTerminalNode(terminal);
        if(previousNode == null) {
            return Space.build(source.substring(0, terminal.getSymbol().getStartIndex()));
        } else {
            return Space.build(source.substring(previousNode.getSymbol().getStopIndex()+1, terminal.getSymbol().getStartIndex()));
        }
    }

    private Space prefix(ParserRuleContext tree) {
        TerminalNode previousNode = previousTerminalNode(tree);
        if(previousNode == null) {
            return Space.build(source.substring(0, tree.getStart().getStartIndex()));
        } else {
            return Space.build(source.substring(previousNode.getSymbol().getStopIndex(), tree.getStart().getStartIndex()));
        }
    }

    private TerminalNode previousTerminalNode(ParseTree n) {
        ParseTree parent = n.getParent();
        if(n.getParent() == null) return null;
        int pos = positionInParent(n);
        if(pos == 0) {
            return previousTerminalNode(parent);
        } else {
            return lastTerminalNode(parent.getChild(pos-1));
        }
    }

    private int positionInParent(ParseTree n) {
        ParseTree parent = n.getParent();
        int pos;
        for(pos = 0; pos < parent.getChildCount(); pos++) {
            if(parent.getChild(pos) == n) break;
        }
        assert pos < parent.getChildCount();
        return pos;
    }

    private TerminalNode lastTerminalNode(ParseTree n) {
        if(n instanceof TerminalNode) {
            return (TerminalNode) n;
        } else {
            return lastTerminalNode(n.getChild(n.getChildCount()-1));
        }
    }



    @Override
    public Cobol.CompilationUnit visitStartRule(CobolParser.StartRuleContext ctx) {
        return visitCompilationUnit(ctx.compilationUnit()).withEof(sourceBefore(source.substring(cursor)));
    }

    @Override
    public Cobol.CompilationUnit visitCompilationUnit(CobolParser.CompilationUnitContext ctx) {

        List<CobolParser.ProgramUnitContext> puCtxs = ctx.programUnit();
        List<CobolRightPadded<Cobol.ProgramUnit>> programUnits = new ArrayList<>();
        for(int i=0; i<puCtxs.size(); i++) {
            Cobol.ProgramUnit pu = (Cobol.ProgramUnit) visitProgramUnit(puCtxs.get(i));
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
                // List<CobolRightPadded<Cobol>> programUnits;
                programUnits,
                Space.EMPTY
        );
    }

    @Override
    public Cobol visitProgramUnit(CobolParser.ProgramUnitContext ctx) {
        // programUnit
        //   : identificationDivision environmentDivision? dataDivision? procedureDivision? programUnit* endProgramStatement?
        //   ;
        Cobol.IdentificationDivision identificationDivision = (Cobol.IdentificationDivision) visitIdentificationDivision(ctx.identificationDivision());
        Optional<Cobol.ProcedureDivision> procedureDivision;
        if(ctx.procedureDivision() == null) {
            procedureDivision = Optional.empty();
        } else {
            procedureDivision = Optional.of((Cobol.ProcedureDivision) visitProcedureDivision(ctx.procedureDivision()));
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
        // procedureDivision
        //   : PROCEDURE DIVISION procedureDivisionUsingClause? procedureDivisionGivingClause? DOT_FS procedureDeclaratives? procedureDivisionBody
        //   ;
        if(ctx.procedureDivisionUsingClause() != null) {
            throw new UnsupportedOperationException("Not implemented");
        }
        if(ctx.procedureDivisionGivingClause() != null) {
            throw new UnsupportedOperationException("Not implemented");
        }
        if(ctx.procedureDeclaratives() != null) {
            throw new UnsupportedOperationException("Not implemented");
        }

        Cobol.ProcedureDivisionBody procedureDivisionBody = (Cobol.ProcedureDivisionBody) visitProcedureDivisionBody(ctx.procedureDivisionBody());
        return new Cobol.ProcedureDivision(
                randomId(),
                prefix(ctx),
                Markers.EMPTY,
                procedureDivisionBody
        );
    }

    @Override
    public Cobol visitIdentificationDivision(CobolParser.IdentificationDivisionContext ctx) {
        // identificationDivision :  (IDENTIFICATION | ID) 1 DIVISION 2 DOT_FS 3 programIdParagraph identificationDivisionBody*
        Cobol.ProgramIdParagraph programIdParagraph = (Cobol.ProgramIdParagraph) visitProgramIdParagraph(ctx.programIdParagraph());
        TerminalNode idTerminal = ctx.IDENTIFICATION() == null ? ctx.ID() : ctx.IDENTIFICATION();
        String space1 = space(idTerminal, ctx.DIVISION());
        Cobol.IdentificationDivision.IdKeyword idKeyword = ctx.IDENTIFICATION() == null ? Cobol.IdentificationDivision.IdKeyword.Id : Cobol.IdentificationDivision.IdKeyword.Identification;
        String space2 = space(ctx.DIVISION(), ctx.DOT_FS());
        String space3 = space(ctx.DOT_FS(), ctx.programIdParagraph());

        if(ctx.identificationDivisionBody() != null && ctx.identificationDivisionBody().size() != 0) {
            throw new UnsupportedOperationException("Not implemented");
        }

        Cobol.IdentificationDivision id = new Cobol.IdentificationDivision(
                randomId(),
                prefix(ctx),
                Markers.EMPTY,
                CobolRightPadded.build(idKeyword).withAfter(Space.build(space1)),
                CobolRightPadded.build(Space.build(ctx.DIVISION().getText())).withAfter(Space.build(space2)),
                CobolRightPadded.build(Space.build(ctx.DOT_FS().getText())).withAfter(Space.build(space3)),
                programIdParagraph
        );
        return id;
    }

    @Override
    public Cobol visitProgramIdParagraph(CobolParser.ProgramIdParagraphContext ctx) {
        // programIdParagraph
        //   : PROGRAM_ID DOT_FS programName (IS? (COMMON | INITIAL | LIBRARY | DEFINITION | RECURSIVE) PROGRAM?)? DOT_FS? commentEntry?
        //   ;
        CobolRightPadded<Space> programId = CobolRightPadded.build(Space.build("PROGRAM-ID"))
                .withAfter(Space.build(space(ctx.PROGRAM_ID(), ctx.programName())));
        String programName = ctx.programName().getText();
        return new Cobol.ProgramIdParagraph(
                randomId(),
                prefix(ctx),
                Markers.EMPTY,
                programId,
                programName
        );
    }

    @Nullable
    private <C extends ParserRuleContext, T> T convert(C ctx, BiFunction<C, Space, T> conversion) {
        //noinspection ConstantConditions
        if (ctx == null) {
            return null;
        }

        T t = conversion.apply(ctx, prefix(ctx));
        if (ctx.getStop() != null) {
            cursor = ctx.getStop().getStopIndex() + (Character.isWhitespace(source.charAt(ctx.getStop().getStopIndex())) ? 0 : 1);
        }

        return t;
    }

    private <T> T convert(TerminalNode node, BiFunction<TerminalNode, Space, T> conversion) {
        T t = conversion.apply(node, prefix(node));
        cursor = node.getSymbol().getStopIndex() + 1;
        return t;
    }

    private void skip(TerminalNode node) {
        cursor = node.getSymbol().getStopIndex() + 1;
    }

    /**
     * @return Source from <code>cursor</code> to next occurrence of <code>untilDelim</code>,
     * and if not found in the remaining source, the empty String. If <code>stop</code> is reached before
     * <code>untilDelim</code> return the empty String.
     */
    private Space sourceBefore(String untilDelim) {
        int delimIndex = positionOfNext(untilDelim, null);
        if (delimIndex < 0) {
            return Space.EMPTY; // unable to find this delimiter
        }

        String prefix = source.substring(cursor, delimIndex);
        cursor += prefix.length() + untilDelim.length(); // advance past the delimiter
        return Space.format(prefix);
    }

    private int positionOfNext(String untilDelim, @Nullable Character stop) {
        boolean inMultiLineComment = false;
        boolean inSingleLineComment = false;

        int delimIndex = cursor;
        for (; delimIndex < source.length() - untilDelim.length() + 1; delimIndex++) {
            if (inSingleLineComment) {
                if (source.charAt(delimIndex) == '\n') {
                    inSingleLineComment = false;
                }
            } else {
                if (source.length() - untilDelim.length() > delimIndex + 1) {
                    switch (source.substring(delimIndex, delimIndex + 2)) {
                        case "//":
                            inSingleLineComment = true;
                            delimIndex++;
                            break;
                        case "/*":
                            inMultiLineComment = true;
                            delimIndex++;
                            break;
                        case "*/":
                            inMultiLineComment = false;
                            delimIndex = delimIndex + 2;
                            break;
                    }
                }

                if (!inMultiLineComment && !inSingleLineComment) {
                    if (stop != null && source.charAt(delimIndex) == stop) {
                        return -1; // reached stop word before finding the delimiter
                    }

                    if (source.startsWith(untilDelim, delimIndex)) {
                        break; // found it!
                    }
                }
            }
        }

        return delimIndex > source.length() - untilDelim.length() ? -1 : delimIndex;
    }
}
