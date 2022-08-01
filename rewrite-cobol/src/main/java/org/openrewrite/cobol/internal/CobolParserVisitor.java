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
import java.util.*;
import java.util.function.Function;
import java.util.function.Supplier;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static java.util.Collections.emptyList;
import static java.util.Collections.singletonList;
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
    public Object visitComputeStatement(CobolParser.ComputeStatementContext ctx) {
        return new Cobol.Compute(
                randomId(),
                prefix(ctx),
                Markers.EMPTY,
                words(ctx.COMPUTE()),
                convertAllContainer(ctx.computeStore()),
                words(ctx.EQUALCHAR(), ctx.EQUAL()),
                (Cobol.ArithmeticExpression) visit(ctx.arithmeticExpression()),
                visitNullable(ctx.onSizeErrorPhrase()),
                visitNullable(ctx.notOnSizeErrorPhrase()),
                words(ctx.END_COMPUTE())
        );
    }

    @Override
    public Object visitAbbreviation(CobolParser.AbbreviationContext ctx) {
        return new Cobol.Abbreviation(
                randomId(),
                prefix(ctx),
                Markers.EMPTY,
                words(ctx.NOT()),
                visitNullable(ctx.relationalOperator()),
                words(ctx.LPARENCHAR()),
                (Cobol) visit(ctx.arithmeticExpression()),
                visitNullable(ctx.abbreviation()),
                words(ctx.RPARENCHAR())
        );
    }

    @Override
    public Object visitAcceptStatement(CobolParser.AcceptStatementContext ctx) {
        return new Cobol.Accept(
                randomId(),
                prefix(ctx),
                Markers.EMPTY,
                words(ctx.ACCEPT()),
                (Identifier) visit(ctx.identifier()),
                visit(ctx.acceptFromDateStatement(), ctx.acceptFromEscapeKeyStatement(), ctx.acceptFromMnemonicStatement(), ctx.acceptMessageCountStatement()),
                visitNullable(ctx.onExceptionClause()),
                visitNullable(ctx.notOnExceptionClause()),
                padLeft(ctx.END_ACCEPT())
        );
    }

    @Override
    public Object visitAcceptFromDateStatement(CobolParser.AcceptFromDateStatementContext ctx) {
        return new Cobol.AcceptFromDateStatement(
                randomId(),
                prefix(ctx),
                Markers.EMPTY,
                words(ctx.FROM(), ctx.DATE(), ctx.YYYYMMDD(), ctx.DAY(), ctx.YYYYDDD(), ctx.DAY_OF_WEEK(), ctx.TIME(), ctx.TIMER(), ctx.TODAYS_DATE(), ctx.MMDDYYYY(), ctx.TODAYS_NAME(), ctx.YEAR())
        );
    }

    @Override
    public Object visitAcceptFromMnemonicStatement(CobolParser.AcceptFromMnemonicStatementContext ctx) {
        return new Cobol.AcceptFromMnemonicStatement(
                randomId(),
                prefix(ctx),
                Markers.EMPTY,
                words(ctx.FROM()),
                (Identifier) visit(ctx.mnemonicName())
        );
    }

    @Override
    public Object visitAcceptFromEscapeKeyStatement(CobolParser.AcceptFromEscapeKeyStatementContext ctx) {
        return new Cobol.AcceptFromEscapeKeyStatement(
                randomId(),
                prefix(ctx),
                Markers.EMPTY,
                words(ctx.FROM(), ctx.ESCAPE(), ctx.KEY())
        );
    }

    @Override
    public Object visitAcceptMessageCountStatement(CobolParser.AcceptMessageCountStatementContext ctx) {
        return new Cobol.AcceptMessageCountStatement(
                randomId(),
                prefix(ctx),
                Markers.EMPTY,
                words(ctx.MESSAGE(), ctx.COUNT())
        );
    }

    @Override
    public Object visitAccessModeClause(CobolParser.AccessModeClauseContext ctx) {
        return new Cobol.AccessModeClause(
                randomId(),
                prefix(ctx),
                Markers.EMPTY,
                words(ctx.ACCESS(), ctx.MODE(), ctx.IS(), ctx.SEQUENTIAL(), ctx.RANDOM(), ctx.DYNAMIC(), ctx.EXCLUSIVE())
        );
    }

    @Override
    public Cobol.AlterStatement visitAlterStatement(CobolParser.AlterStatementContext ctx) {
        return new Cobol.AlterStatement(
                randomId(),
                prefix(ctx),
                Markers.EMPTY,
                words(ctx.ALTER()),
                convertAll(ctx.alterProceedTo())
        );
    }

    @Override
    public Object visitAlternateRecordKeyClause(CobolParser.AlternateRecordKeyClauseContext ctx) {
        return new Cobol.AlternateRecordKeyClause(
                randomId(),
                prefix(ctx),
                Markers.EMPTY,
                words(ctx.ALTERNATE(), ctx.RECORD(), ctx.KEY(), ctx.IS()),
                (Cobol.QualifiedDataName) visit(ctx.qualifiedDataName()),
                visitNullable(ctx.passwordClause()),
                words(ctx.WITH(), ctx.DUPLICATES())
        );
    }

    @Override
    public Cobol.AlterProceedTo visitAlterProceedTo(CobolParser.AlterProceedToContext ctx) {
        return new Cobol.AlterProceedTo(
                randomId(),
                prefix(ctx),
                Markers.EMPTY,
                (Cobol.ProcedureName) visit(ctx.procedureName(0)),
                words(ctx.TO(0), ctx.PROCEED(), ctx.PROCEED() != null ? ctx.TO(1) : null),
                (Cobol.ProcedureName) visit(ctx.procedureName(1))
        );
    }

    @Override
    public Object visitAndOrCondition(CobolParser.AndOrConditionContext ctx) {
        return new Cobol.AndOrCondition(
                randomId(),
                prefix(ctx),
                Markers.EMPTY,
                words(ctx.AND(), ctx.OR()),
                (Cobol.CombinableCondition) visit(ctx.combinableCondition()),
                convertAllContainer(ctx.abbreviation())
        );
    }

    @Override
    public Object visitArgument(CobolParser.ArgumentContext ctx) {
        return new Cobol.Argument(
                randomId(),
                prefix(ctx),
                Markers.EMPTY,
                visit(ctx.literal(), ctx.identifier(), ctx.qualifiedDataName(), ctx.indexName(), ctx.arithmeticExpression()),
                visitNullable(ctx.integerLiteral())
        );
    }

    @Override
    public Object visitArithmeticExpression(CobolParser.ArithmeticExpressionContext ctx) {
        return new Cobol.ArithmeticExpression(
                randomId(),
                prefix(ctx),
                Markers.EMPTY,
                (Cobol.MultDivs) visit(ctx.multDivs()),
                convertAllContainer(ctx.plusMinus())
        );
    }

    @Override
    public Object visitAssignClause(CobolParser.AssignClauseContext ctx) {
        return new Cobol.AssignClause(
                randomId(),
                prefix(ctx),
                Markers.EMPTY,
                words(ctx.ASSIGN(), ctx.TO(), ctx.DISK(), ctx.DISPLAY(), ctx.KEYBOARD(), ctx.PORT(), ctx.PRINTER(),
                        ctx.READER(), ctx.REMOTE(), ctx.TAPE(), ctx.VIRTUAL(), ctx.DYNAMIC(), ctx.EXTERNAL()),
                ctx.assignmentName() != null ? visitNullable(ctx.assignmentName()) :
                        ctx.literal() != null ? visitNullable(ctx.literal()) : null
        );
    }

    @Override
    public Cobol.StatementPhrase visitAtEndPhrase(CobolParser.AtEndPhraseContext ctx) {
        return new Cobol.StatementPhrase(
                randomId(),
                prefix(ctx),
                Markers.EMPTY,
                words(ctx.AT(), ctx.END()),
                convertAllContainer(ctx.statement())
        );
    }

    @Override
    public Object visitMultDivs(CobolParser.MultDivsContext ctx) {
        return new Cobol.MultDivs(
                randomId(),
                prefix(ctx),
                Markers.EMPTY,
                (Cobol.Powers) visit(ctx.powers()),
                convertAllContainer(ctx.multDiv())
        );
    }

    @Override
    public Object visitMultDiv(CobolParser.MultDivContext ctx) {
        return new Cobol.MultDiv(
                randomId(),
                prefix(ctx),
                Markers.EMPTY,
                words(ctx.ASTERISKCHAR(), ctx.SLASHCHAR()),
                (Cobol.Powers) visit(ctx.powers())
        );
    }

    @Override
    public Object visitPowers(CobolParser.PowersContext ctx) {
        return new Cobol.Powers(
                randomId(),
                prefix(ctx),
                Markers.EMPTY,
                words(ctx.PLUSCHAR(), ctx.MINUSCHAR()),
                (Cobol)visit(ctx.basis()),
                convertAllContainer(ctx.power())
        );
    }

    @Override
    public Object visitPower(CobolParser.PowerContext ctx) {
        return new Cobol.Power(
                randomId(),
                prefix(ctx),
                Markers.EMPTY,
                words(ctx.DOUBLEASTERISKCHAR()),
                (Cobol) visit(ctx.basis())
        );
    }

    @Override
    public Object visitBasis(CobolParser.BasisContext ctx) {
        if (ctx.LPARENCHAR() != null) {
            return new Cobol.Parenthesized(
                    randomId(),
                    prefix(ctx),
                    Markers.EMPTY,
                    words(ctx.LPARENCHAR()),
                    singletonList((Cobol) visit(ctx.arithmeticExpression())),
                    words(ctx.RPARENCHAR())
            );
        } else {
            return visit(ctx.identifier(), ctx.literal());
        }
    }

    @Override
    public Object visitPlusMinus(CobolParser.PlusMinusContext ctx) {
        return new Cobol.PlusMinus(
                randomId(),
                prefix(ctx),
                Markers.EMPTY,
                words(ctx.PLUSCHAR(), ctx.MINUSCHAR()),
                (Cobol.MultDivs) visit(ctx.multDivs())
        );
    }

    @Override
    public Cobol.ProcedureName visitProcedureName(CobolParser.ProcedureNameContext ctx) {
        return new Cobol.ProcedureName(
                randomId(),
                prefix(ctx),
                Markers.EMPTY,
                (Name) visit(ctx.paragraphName()),
                visitNullable(ctx.inSection()),
                visitNullable(ctx.sectionName())
        );
    }

    @Override
    public Cobol.InSection visitInSection(CobolParser.InSectionContext ctx) {
        return new Cobol.InSection(
                randomId(),
                prefix(ctx),
                Markers.EMPTY,
                words(ctx.IN(), ctx.OF()),
                (Name) visit(ctx.sectionName())
        );
    }

    @Override
    public Object visitOnExceptionClause(CobolParser.OnExceptionClauseContext ctx) {
        return new Cobol.StatementPhrase(
                randomId(),
                prefix(ctx),
                Markers.EMPTY,
                words(ctx.ON(), ctx.EXCEPTION()),
                convertAllContainer(ctx.statement())
        );
    }

    @Override
    public Cobol.StatementPhrase visitOnOverflowPhrase(CobolParser.OnOverflowPhraseContext ctx) {
        return new Cobol.StatementPhrase(
                randomId(),
                prefix(ctx),
                Markers.EMPTY,
                words(ctx.ON(), ctx.OVERFLOW()),
                convertAllContainer(ctx.statement())
        );
    }

    @Override
    public Object visitOnSizeErrorPhrase(CobolParser.OnSizeErrorPhraseContext ctx) {
        return new Cobol.StatementPhrase(
                randomId(),
                prefix(ctx),
                Markers.EMPTY,
                words(ctx.ON(), ctx.SIZE(), ctx.ERROR()),
                convertAllContainer(ctx.statement())
        );
    }

    @Override
    public Cobol.StatementPhrase visitNotInvalidKeyPhrase(CobolParser.NotInvalidKeyPhraseContext ctx) {
        return new Cobol.StatementPhrase(
                randomId(),
                prefix(ctx),
                Markers.EMPTY,
                words(ctx.NOT(), ctx.INVALID(), ctx.KEY()),
                convertAllContainer(ctx.statement())
        );
    }

    @Override
    public Cobol.StatementPhrase visitNotAtEndPhrase(CobolParser.NotAtEndPhraseContext ctx) {
        return new Cobol.StatementPhrase(
                randomId(),
                prefix(ctx),
                Markers.EMPTY,
                words(ctx.NOT(), ctx.AT(), ctx.END()),
                convertAllContainer(ctx.statement())
        );
    }

    @Override
    public Object visitNotOnExceptionClause(CobolParser.NotOnExceptionClauseContext ctx) {
        return new Cobol.StatementPhrase(
                randomId(),
                prefix(ctx),
                Markers.EMPTY,
                words(ctx.NOT(), ctx.ON(), ctx.EXCEPTION()),
                convertAllContainer(ctx.statement())
        );
    }

    @Override
    public Cobol.StatementPhrase visitNotOnOverflowPhrase(CobolParser.NotOnOverflowPhraseContext ctx) {
        return new Cobol.StatementPhrase(
                randomId(),
                prefix(ctx),
                Markers.EMPTY,
                words(ctx.NOT(), ctx.ON(), ctx.OVERFLOW()),
                convertAllContainer(ctx.statement())
        );
    }

    @Override
    public Cobol.StatementPhrase visitNotOnSizeErrorPhrase(CobolParser.NotOnSizeErrorPhraseContext ctx) {
        return new Cobol.StatementPhrase(
                randomId(),
                prefix(ctx),
                Markers.EMPTY,
                words(ctx.NOT(), ctx.ON(), ctx.SIZE(), ctx.ERROR()),
                convertAllContainer(ctx.statement())
        );
    }

    @Override
    public Object visitBlockContainsClause(CobolParser.BlockContainsClauseContext ctx) {
        return new Cobol.BlockContainsClause(
                randomId(),
                prefix(ctx),
                Markers.EMPTY,
                words(ctx.BLOCK(), ctx.CONTAINS()),
                (Cobol.CobolWord) visit(ctx.integerLiteral()),
                visitNullable(ctx.blockContainsTo()),
                words(ctx.RECORDS(), ctx.CHARACTERS())
        );
    }

    @Override
    public Object visitBlockContainsTo(CobolParser.BlockContainsToContext ctx) {
        return new Cobol.BlockContainsTo(
                randomId(),
                prefix(ctx),
                Markers.EMPTY,
                words(ctx.TO()),
                (Cobol.CobolWord) visit(ctx.integerLiteral())
        );
    }

    @Override
    public Object visitCallStatement(CobolParser.CallStatementContext ctx) {
        return new Cobol.Call(
                randomId(),
                prefix(ctx),
                Markers.EMPTY,
                words(ctx.CALL()),
                visit(ctx.identifier(), ctx.literal()),
                visitNullable(ctx.callUsingPhrase()),
                visitNullable(ctx.callGivingPhrase()),
                visitNullable(ctx.onOverflowPhrase()),
                visitNullable(ctx.onExceptionClause()),
                visitNullable(ctx.notOnExceptionClause()),
                padLeft(ctx.END_CALL())
        );
    }

    @Override
    public Object visitCallUsingPhrase(CobolParser.CallUsingPhraseContext ctx) {
        return new Cobol.CallPhrase(
                randomId(),
                prefix(ctx),
                Markers.EMPTY,
                words(ctx.USING()),
                convertAllContainer(ctx.callUsingParameter())
        );
    }

    @Override
    public Object visitCallByReferencePhrase(CobolParser.CallByReferencePhraseContext ctx) {
        return new Cobol.CallPhrase(
                randomId(),
                prefix(ctx),
                Markers.EMPTY,
                words(ctx.BY(), ctx.REFERENCE()),
                convertAllContainer(ctx.callByReference())
        );
    }

    @Override
    public Object visitCallByReference(CobolParser.CallByReferenceContext ctx) {
        return new Cobol.CallBy(
                randomId(),
                prefix(ctx),
                Markers.EMPTY,
                words(ctx.ADDRESS(), ctx.OF(), ctx.INTEGER(), ctx.STRING(), ctx.OMITTED()),
                ctx.identifier() != null ? (Name) visit(ctx.identifier()) :
                        ctx.literal() != null ? (Name) visit(ctx.literal()) :
                                ctx.fileName() != null ? (Name) visit(ctx.fileName()) : null
        );
    }

    @Override
    public Object visitCallByValuePhrase(CobolParser.CallByValuePhraseContext ctx) {
        return new Cobol.CallPhrase(
                randomId(),
                prefix(ctx),
                Markers.EMPTY,
                words(ctx.BY(), ctx.VALUE()),
                convertAllContainer(ctx.callByValue())
        );
    }

    @Override
    public Object visitCallByValue(CobolParser.CallByValueContext ctx) {
        return new Cobol.CallBy(
                randomId(),
                prefix(ctx),
                Markers.EMPTY,
                words(ctx.ADDRESS(), ctx.LENGTH(), ctx.OF()),
                visit(ctx.identifier(), ctx.literal())
        );
    }

    @Override
    public Object visitCallByContentPhrase(CobolParser.CallByContentPhraseContext ctx) {
        return new Cobol.CallPhrase(
                randomId(),
                prefix(ctx),
                Markers.EMPTY,
                words(ctx.BY(), ctx.CONTENT()),
                convertAllContainer(ctx.callByContent())
        );
    }

    @Override
    public Object visitCallByContent(CobolParser.CallByContentContext ctx) {
        return new Cobol.CallBy(
                randomId(),
                prefix(ctx),
                Markers.EMPTY,
                words(ctx.ADDRESS(), ctx.LENGTH(), ctx.OF(), ctx.OMITTED()),
                ctx.identifier() != null ? (Name) visit(ctx.identifier()) :
                        ctx.literal() != null ? (Name) visit(ctx.literal()) : null
        );
    }

    @Override
    public Object visitCallGivingPhrase(CobolParser.CallGivingPhraseContext ctx) {
        return new Cobol.CallGivingPhrase(
                randomId(),
                prefix(ctx),
                Markers.EMPTY,
                words(ctx.GIVING(), ctx.RETURNING()),
                (Name) visit(ctx.identifier())
        );
    }

    @Override
    public Object visitCancelStatement(CobolParser.CancelStatementContext ctx) {
        return new Cobol.Cancel(
                randomId(),
                prefix(ctx),
                Markers.EMPTY,
                words(ctx.CANCEL()),
                convertAllContainer(ctx.cancelCall())
        );
    }

    @Override
    public Object visitCancelCall(CobolParser.CancelCallContext ctx) {
        return new Cobol.CancelCall(
                randomId(),
                prefix(ctx),
                Markers.EMPTY,
                visitNullable(ctx.libraryName()),
                words(ctx.BYTITLE(), ctx.BYFUNCTION()),
                visitNullable(ctx.identifier()),
                visitNullable(ctx.literal())
        );
    }

    @Override
    public Cobol.ChannelClause visitChannelClause(CobolParser.ChannelClauseContext ctx) {
        return new Cobol.ChannelClause(
                randomId(),
                prefix(ctx),
                Markers.EMPTY,
                words(ctx.CHANNEL()),
                (Literal) visit(ctx.integerLiteral()),
                padLeft(ctx.IS()),
                (Identifier) visit(ctx.mnemonicName())
        );
    }

    @Override
    public Object visitClassClause(CobolParser.ClassClauseContext ctx) {
        return new Cobol.ClassClause(
                randomId(),
                prefix(ctx),
                Markers.EMPTY,
                words(ctx.CLASS()),
                (Cobol.CobolWord) visit(ctx.className()),
                words(ctx.FOR(), ctx.ALPHANUMERIC(), ctx.NATIONAL(), ctx.IS()),
                convertAllContainer(ctx.classClauseThrough())
        );
    }

    @Override
    public Object visitClassClauseThrough(CobolParser.ClassClauseThroughContext ctx) {
        return new Cobol.ClassClauseThrough(
                randomId(),
                prefix(ctx),
                Markers.EMPTY,
                (Cobol.CobolWord) visit(ctx.classClauseFrom()),
                visitNullable(ctx.THROUGH() != null ? ctx.THROUGH() : (ctx.THRU() != null ? ctx.THRU() : null)),
                visitNullable(ctx.classClauseTo())
        );
    }

    @Override
    public Object visitClassCondition(CobolParser.ClassConditionContext ctx) {
        return new Cobol.ClassCondition(
                randomId(),
                prefix(ctx),
                Markers.EMPTY,
                (Name) visit(ctx.identifier()),
                words(ctx.IS(), ctx.NOT(), ctx.NUMERIC(), ctx.ALPHABETIC(), ctx.ALPHABETIC_LOWER(), ctx.ALPHABETIC_UPPER(), ctx.DBCS(), ctx.KANJI()),
                visitNullable(ctx.className())
        );
    }

    @Override
    public Object visitCloseStatement(CobolParser.CloseStatementContext ctx) {
        return new Cobol.Close(
                randomId(),
                prefix(ctx),
                Markers.EMPTY,
                words(ctx.CLOSE()),
                convertAllContainer(ctx.closeFile())
        );
    }

    @Override
    public Object visitCloseFile(CobolParser.CloseFileContext ctx) {
        return new Cobol.CloseFile(
                randomId(),
                prefix(ctx),
                Markers.EMPTY,
                (Name) visit(ctx.fileName()),
                ctx.closeReelUnitStatement() != null || ctx.closeRelativeStatement() != null || ctx.closePortFileIOStatement() != null ?
                        visit(ctx.closeReelUnitStatement(), ctx.closeRelativeStatement(), ctx.closePortFileIOStatement()) : null
        );
    }

    @Override
    public Object visitCloseReelUnitStatement(CobolParser.CloseReelUnitStatementContext ctx) {
        return new Cobol.CloseReelUnitStatement(
                randomId(),
                prefix(ctx),
                Markers.EMPTY,
                words(ctx.REEL(), ctx.UNIT(), ctx.FOR(), ctx.REMOVAL(), ctx.WITH(), ctx.NO(), ctx.REWIND(), ctx.LOCK())
        );
    }

    @Override
    public Object visitCloseRelativeStatement(CobolParser.CloseRelativeStatementContext ctx) {
        return new Cobol.CloseRelativeStatement(
                randomId(),
                prefix(ctx),
                Markers.EMPTY,
                words(ctx.WITH(), ctx.NO(), ctx.REWIND(), ctx.LOCK())
        );
    }

    @Override
    public Object visitClosePortFileIOStatement(CobolParser.ClosePortFileIOStatementContext ctx) {
        return new Cobol.ClosePortFileIOStatement(
                randomId(),
                prefix(ctx),
                Markers.EMPTY,
                words(ctx.WITH(), ctx.NO(), ctx.WAIT(), ctx.USING()),
                convertAllContainer(ctx.closePortFileIOUsing())
        );
    }

    @Override
    public Object visitClosePortFileIOUsingCloseDisposition(CobolParser.ClosePortFileIOUsingCloseDispositionContext ctx) {
        return new Cobol.ClosePortFileIOUsingCloseDisposition(
                randomId(),
                prefix(ctx),
                Markers.EMPTY,
                words(ctx.CLOSE_DISPOSITION(), ctx.OF(), ctx.ABORT(), ctx.ORDERLY())
        );
    }

    @Override
    public Object visitClosePortFileIOUsingAssociatedData(CobolParser.ClosePortFileIOUsingAssociatedDataContext ctx) {
        return new Cobol.ClosePortFileIOUsingAssociatedData(
                randomId(),
                prefix(ctx),
                Markers.EMPTY,
                words(ctx.ASSOCIATED_DATA()),
                visit(ctx.identifier(), ctx.integerLiteral())
        );
    }

    @Override
    public Object visitClosePortFileIOUsingAssociatedDataLength(CobolParser.ClosePortFileIOUsingAssociatedDataLengthContext ctx) {
        return new Cobol.ClosePortFileIOUsingAssociatedDataLength(
                randomId(),
                prefix(ctx),
                Markers.EMPTY,
                words(ctx.ASSOCIATED_DATA_LENGTH(), ctx.OF()),
                visit(ctx.identifier(), ctx.integerLiteral())
        );
    }

    @Override
    public Object visitCombinableCondition(CobolParser.CombinableConditionContext ctx) {
        return new Cobol.CombinableCondition(
                randomId(),
                prefix(ctx),
                Markers.EMPTY,
                words(ctx.NOT()),
                (Cobol) visit(ctx.simpleCondition())
        );
    }

    @Override
    public Object visitCommitmentControlClause(CobolParser.CommitmentControlClauseContext ctx) {
        return new Cobol.CommitmentControlClause(
                randomId(),
                prefix(ctx),
                Markers.EMPTY,
                words(ctx.COMMITMENT(), ctx.CONTROL(), ctx.FOR()),
                (Cobol.CobolWord) visit(ctx.fileName())
        );
    }

    @Override
    public Object visitCommunicationDescriptionEntryFormat1(CobolParser.CommunicationDescriptionEntryFormat1Context ctx) {
        return new Cobol.CommunicationDescriptionEntryFormat1(
                randomId(),
                prefix(ctx),
                Markers.EMPTY,
                words(ctx.CD()),
                (Cobol.CobolWord) visit(ctx.cdName()),
                words(ctx.FOR(), ctx.INITIAL(), ctx.INPUT()),
                convertAllContainer(ctx.symbolicQueueClause(),
                        ctx.symbolicSubQueueClause(),
                        ctx.messageDateClause(),
                        ctx.messageTimeClause(),
                        ctx.symbolicSourceClause(),
                        ctx.textLengthClause(),
                        ctx.endKeyClause(),
                        ctx.statusKeyClause(),
                        ctx.messageCountClause(),
                        ctx.dataDescName()).withLastSpace(sourceBefore("."))
        );
    }

    @Override
    public Object visitCommunicationDescriptionEntryFormat2(CobolParser.CommunicationDescriptionEntryFormat2Context ctx) {
        return new Cobol.CommunicationDescriptionEntryFormat2(
                randomId(),
                prefix(ctx),
                Markers.EMPTY,
                words(ctx.CD()),
                (Cobol.CobolWord) visit(ctx.cdName()),
                words(ctx.FOR(), ctx.OUTPUT()),
                convertAllContainer(ctx.destinationCountClause(),
                        ctx.textLengthClause(),
                        ctx.statusKeyClause(),
                        ctx.destinationTableClause(),
                        ctx.errorKeyClause(),
                        ctx.symbolicDestinationClause()).withLastSpace(sourceBefore("."))
        );
    }

    @Override
    public Object visitCommunicationDescriptionEntryFormat3(CobolParser.CommunicationDescriptionEntryFormat3Context ctx) {
        return new Cobol.CommunicationDescriptionEntryFormat3(
                randomId(),
                prefix(ctx),
                Markers.EMPTY,
                words(ctx.CD()),
                (Cobol.CobolWord) visit(ctx.cdName()),
                words(ctx.FOR(), ctx.INITIAL(), ctx.I_O()),
                convertAllContainer(ctx.messageDateClause(),
                        ctx.messageTimeClause(),
                        ctx.symbolicTerminalClause(),
                        ctx.textLengthClause(),
                        ctx.endKeyClause(),
                        ctx.statusKeyClause(),
                        ctx.dataDescName()).withLastSpace(sourceBefore("."))
        );
    }

    @Override
    public Object visitCommunicationSection(CobolParser.CommunicationSectionContext ctx) {
        return new Cobol.CommunicationSection(
                randomId(),
                prefix(ctx),
                Markers.EMPTY,
                words(ctx.COMMUNICATION(), ctx.SECTION(), ctx.DOT_FS()),
                convertAllContainer(ctx.communicationDescriptionEntry(), ctx.dataDescriptionEntry())
        );
    }

    @Override
    public Object visitCondition(CobolParser.ConditionContext ctx) {
        return new Cobol.Condition(
                randomId(),
                prefix(ctx),
                Markers.EMPTY,
                (Cobol.CombinableCondition) visit(ctx.combinableCondition()),
                convertAllContainer(ctx.andOrCondition())
        );
    }

    @Override
    public Cobol.ConditionNameReference visitConditionNameReference(CobolParser.ConditionNameReferenceContext ctx) {
        return new Cobol.ConditionNameReference(
                randomId(),
                prefix(ctx),
                Markers.EMPTY,
                (Name) visit(ctx.conditionName()),
                convertAllContainer(ctx.inData()),
                visitNullable(ctx.inFile()),
                convertAllContainer(ctx.conditionNameSubscriptReference()),
                convertAllContainer(ctx.inMnemonic())
        );
    }

    @Override
    public Cobol.ConditionNameSubscriptReference visitConditionNameSubscriptReference(CobolParser.ConditionNameSubscriptReferenceContext ctx) {
        return new Cobol.ConditionNameSubscriptReference(
                randomId(),
                prefix(ctx),
                Markers.EMPTY,
                words(ctx.LPARENCHAR()),
                convertAllPrefixedList(singletonList(","), ctx.subscript()),
                words(ctx.RPARENCHAR())
        );
    }

    @Override
    public Cobol.CobolWord visitTerminal(TerminalNode node) {
        return new Cobol.CobolWord(
                randomId(),
                sourceBefore(node.getText()),
                Markers.EMPTY,
                node.getText()
        );
    }

    @Override
    public Object visitContinueStatement(CobolParser.ContinueStatementContext ctx) {
        return new Cobol.Continue(
                randomId(),
                prefix(ctx),
                Markers.EMPTY,
                words(ctx.CONTINUE())
        );
    }

    @Override
    public Cobol.CurrencyClause visitCurrencySignClause(CobolParser.CurrencySignClauseContext ctx) {
        return new Cobol.CurrencyClause(
                randomId(),
                prefix(ctx),
                Markers.EMPTY,
                words(ctx.CURRENCY(), ctx.SIGN(), ctx.IS()),
                (Literal) visit(ctx.literal(0)),
                ctx.literal().size() > 1 ? padLeft(whitespace(), words(ctx.WITH(), ctx.PICTURE(), ctx.SYMBOL())) : null,
                ctx.literal().size() > 1 ? (Literal) visit(ctx.literal(1)) : null
        );
    }

    @Override
    public Object visitFileControlEntry(CobolParser.FileControlEntryContext ctx) {
        return new Cobol.FileControlEntry(
                randomId(),
                prefix(ctx),
                Markers.EMPTY,
                (Cobol) visit(ctx.selectClause()),
                convertAllContainer(ctx.fileControlClause())
        );
    }

    @Override
    public Object visitFileControlParagraph(CobolParser.FileControlParagraphContext ctx) {
        return new Cobol.FileControlParagraph(
                randomId(),
                prefix(ctx),
                Markers.EMPTY,
                words(ctx.FILE_CONTROL()),
                convertAllList(singletonList("."), ctx.fileControlEntry()),
                padLeft(ctx.DOT_FS().get(ctx.DOT_FS().size() - 1))
        );
    }

    @Override
    public Object visitFileStatusClause(CobolParser.FileStatusClauseContext ctx) {
        return new Cobol.FileStatusClause(
                randomId(),
                prefix(ctx),
                Markers.EMPTY,
                words(ctx.FILE(), ctx.STATUS(), ctx.IS()),
                convertAllContainer(ctx.qualifiedDataName())
        );
    }

    @Override
    public Object visitGobackStatement(CobolParser.GobackStatementContext ctx) {
        return new Cobol.GoBack(
                randomId(),
                prefix(ctx),
                Markers.EMPTY,
                (Cobol.CobolWord) visit(ctx.GOBACK())
        );
    }

    @Override
    public Object visitGoToStatement(CobolParser.GoToStatementContext ctx) {
        return new Cobol.GoTo(
                randomId(),
                prefix(ctx),
                Markers.EMPTY,
                words(ctx.GO(), ctx.TO()),
                visit(ctx.goToStatementSimple(), ctx.goToDependingOnStatement())
        );
    }

    @Override
    public Object visitGoToDependingOnStatement(CobolParser.GoToDependingOnStatementContext ctx) {
        return new Cobol.GoToDependingOnStatement(
                randomId(),
                prefix(ctx),
                Markers.EMPTY,
                convertAllContainer(ctx.procedureName()),
                words(ctx.MORE_LABELS(), ctx.DEPENDING(), ctx.ON()),
                visitNullable(ctx.identifier())
        );
    }

    @Override
    public Object visitGlobalClause(CobolParser.GlobalClauseContext ctx) {
        return new Cobol.GlobalClause(
                randomId(),
                prefix(ctx),
                Markers.EMPTY,
                words(ctx.IS(), ctx.GLOBAL())
        );
    }

    @Override
    public Cobol.OdtClause visitOdtClause(CobolParser.OdtClauseContext ctx) {
        return new Cobol.OdtClause(
                randomId(),
                prefix(ctx),
                Markers.EMPTY,
                words(ctx.ODT(), ctx.IS()),
                (Identifier) visit(ctx.mnemonicName())
        );
    }

    @Override
    public Cobol.SymbolicCharactersClause visitSymbolicCharactersClause(CobolParser.SymbolicCharactersClauseContext ctx) {
        return new Cobol.SymbolicCharactersClause(
                randomId(),
                prefix(ctx),
                Markers.EMPTY,
                words(ctx.SYMBOLIC(), ctx.CHARACTERS(), ctx.FOR(), ctx.ALPHANUMERIC(), ctx.NATIONAL()),
                convertAllContainer(ctx.symbolicCharacters()),
                padLeft(ctx.IN()),
                visitNullable(ctx.alphabetName())
        );
    }

    @Override
    public Cobol.SymbolicCharacter visitSymbolicCharacters(CobolParser.SymbolicCharactersContext ctx) {
        return new Cobol.SymbolicCharacter(
                randomId(),
                prefix(ctx),
                Markers.EMPTY,
                convertAllContainer(ctx.symbolicCharacter()),
                convertAllContainer(
                        ctx.IS() != null || ctx.ARE() != null ?
                                padLeft(whitespace(), words(ctx.IS(), ctx.ARE())) : null,
                        ctx.integerLiteral()
                )
        );
    }

    @Override
    public Object visitSymbolicDestinationClause(CobolParser.SymbolicDestinationClauseContext ctx) {
        throw new UnsupportedOperationException("Implement me");
    }

    @Override
    public Object visitRelationSignCondition(CobolParser.RelationSignConditionContext ctx) {
        return new Cobol.RelationSignCondition(
                randomId(),
                prefix(ctx),
                Markers.EMPTY,
                (Cobol.ArithmeticExpression) visit(ctx.arithmeticExpression()),
                words(ctx.IS(), ctx.NOT(), ctx.POSITIVE(), ctx.NEGATIVE(), ctx.ZERO())
        );
    }

    @Override
    public Object visitRelationArithmeticComparison(CobolParser.RelationArithmeticComparisonContext ctx) {
        return new Cobol.RelationArithmeticComparison(
                randomId(),
                prefix(ctx),
                Markers.EMPTY,
                (Cobol.ArithmeticExpression) visit(ctx.arithmeticExpression().get(0)),
                (Cobol.RelationalOperator) visit(ctx.relationalOperator()),
                (Cobol.ArithmeticExpression) visit(ctx.arithmeticExpression().get(1))
        );
    }

    @Override
    public Object visitRelationCombinedComparison(CobolParser.RelationCombinedComparisonContext ctx) {
        return new Cobol.RelationCombinedComparison(
                randomId(),
                prefix(ctx),
                Markers.EMPTY,
                (Cobol.ArithmeticExpression) visit(ctx.arithmeticExpression()),
                (Cobol.RelationalOperator) visit(ctx.relationalOperator()),
                new Cobol.Parenthesized(
                        randomId(),
                        prefix(ctx),
                        Markers.EMPTY,
                        words(ctx.LPARENCHAR()),
                        singletonList((Cobol) visit(ctx.relationCombinedCondition())),
                        words(ctx.RPARENCHAR())
                )
        );
    }

    @Override
    public Object visitRelationCombinedCondition(CobolParser.RelationCombinedConditionContext ctx) {
        return new Cobol.RelationCombinedCondition(
                randomId(),
                prefix(ctx),
                Markers.EMPTY,
                convertAllPrefixedList(Arrays.asList("AND", "OR"), ctx.arithmeticExpression())
        );
    }

    @Override
    public Object visitRelationalOperator(CobolParser.RelationalOperatorContext ctx) {
        return new Cobol.RelationalOperator(
                randomId(),
                prefix(ctx),
                Markers.EMPTY,
                words(ctx.IS(), ctx.ARE(), ctx.NOT(),
                        ctx.GREATER(), ctx.LESS(), ctx.THAN(), ctx.OR(), ctx.EQUAL(), ctx.TO(),
                        ctx.MORETHANCHAR(), ctx.LESSTHANCHAR(), ctx.EQUALCHAR(), ctx.NOTEQUALCHAR(),
                        ctx.MORETHANOREQUAL(), ctx.LESSTHANOREQUAL()
                )
        );
    }

    @Override
    public Object visitRerunClause(CobolParser.RerunClauseContext ctx) {
        return new Cobol.RerunClause(
                randomId(),
                prefix(ctx),
                Markers.EMPTY,
                words(ctx.RERUN()),
                words(ctx.ON()),
                visit(ctx.assignmentName(), ctx.fileName()),
                words(ctx.EVERY()),
                visit(ctx.rerunEveryRecords(), ctx.rerunEveryOf(), ctx.rerunEveryClock())
        );
    }

    @Override
    public Object visitRerunEveryClock(CobolParser.RerunEveryClockContext ctx) {
        return new Cobol.RerunEveryClock(
                randomId(),
                prefix(ctx),
                Markers.EMPTY,
                (Cobol.CobolWord) visit(ctx.integerLiteral()),
                words(ctx.CLOCK_UNITS())
        );
    }

    @Override
    public Object visitRerunEveryOf(CobolParser.RerunEveryOfContext ctx) {
        return new Cobol.RerunEveryOf(
                randomId(),
                prefix(ctx),
                Markers.EMPTY,
                words(ctx.END(), ctx.OF().size() == 1 ? null : ctx.OF(0), ctx.REEL(), ctx.UNIT(), ctx.OF(ctx.OF().size() == 1 ? 0 : 1)),
                (Cobol.CobolWord) visit(ctx.fileName())
        );
    }

    @Override
    public Object visitRerunEveryRecords(CobolParser.RerunEveryRecordsContext ctx) {
        return new Cobol.RerunEveryRecords(
                randomId(),
                prefix(ctx),
                Markers.EMPTY,
                (Cobol.CobolWord) visit(ctx.integerLiteral()),
                words(ctx.RECORDS())
        );
    }

    @Override
    public Cobol.ReserveNetworkClause visitReserveNetworkClause(CobolParser.ReserveNetworkClauseContext ctx) {
        return new Cobol.ReserveNetworkClause(
                randomId(),
                prefix(ctx),
                Markers.EMPTY,
                words(ctx.RESERVE(), ctx.WORDS(), ctx.LIST(), ctx.IS(), ctx.NETWORK(), ctx.CAPABLE())
        );
    }

    @Override
    public Object visitRewriteStatement(CobolParser.RewriteStatementContext ctx) {
        return new Cobol.Rewrite(
                randomId(),
                prefix(ctx),
                Markers.EMPTY,
                words(ctx.REWRITE()),
                visitNullable(ctx.recordName()),
                visitNullable(ctx.invalidKeyPhrase()),
                visitNullable(ctx.notInvalidKeyPhrase()),
                padLeft(ctx.END_REWRITE())
        );
    }

    @Override
    public Object visitRewriteFrom(CobolParser.RewriteFromContext ctx) {
        return new Cobol.RewriteFrom(
                randomId(),
                prefix(ctx),
                Markers.EMPTY,
                words(ctx.FROM()),
                (Name) visit(ctx.identifier())
        );
    }

    @Override
    public Cobol.DecimalPointClause visitDecimalPointClause(CobolParser.DecimalPointClauseContext ctx) {
        return new Cobol.DecimalPointClause(
                randomId(),
                prefix(ctx),
                Markers.EMPTY,
                words(ctx.DECIMAL_POINT(), ctx.IS(), ctx.COMMA())
        );
    }

    @Override
    public Cobol.DefaultComputationalSignClause visitDefaultComputationalSignClause(CobolParser.DefaultComputationalSignClauseContext ctx) {
        return new Cobol.DefaultComputationalSignClause(
                randomId(),
                prefix(ctx),
                Markers.EMPTY,
                words(ctx.DEFAULT(), ctx.COMPUTATIONAL(), ctx.COMP(), ctx.SIGN(), ctx.IS(),
                        ctx.LEADING(), ctx.TRAILING(), ctx.SEPARATE(), ctx.CHARACTER())
        );
    }

    @Override
    public Cobol.DefaultDisplaySignClause visitDefaultDisplaySignClause(CobolParser.DefaultDisplaySignClauseContext ctx) {
        return new Cobol.DefaultDisplaySignClause(
                randomId(),
                prefix(ctx),
                Markers.EMPTY,
                words(ctx.DEFAULT_DISPLAY(), ctx.SIGN(), ctx.IS(), ctx.LEADING(), ctx.TRAILING(),
                        ctx.SEPARATE(), ctx.CHARACTER())
        );
    }

    @Override
    public Object visitDeleteStatement(CobolParser.DeleteStatementContext ctx) {
        return new Cobol.Delete(
                randomId(),
                prefix(ctx),
                Markers.EMPTY,
                words(ctx.DELETE()),
                (Name) visit(ctx.fileName()),
                words(ctx.RECORD()),
                (Cobol.StatementPhrase) visit(ctx.invalidKeyPhrase()),
                (Cobol.StatementPhrase) visit(ctx.notInvalidKeyPhrase()),
                padLeft(ctx.END_DELETE())
        );
    }

    @Override
    public Object visitDestinationCountClause(CobolParser.DestinationCountClauseContext ctx) {
        throw new UnsupportedOperationException("Implement me");
    }

    @Override
    public Object visitDestinationTableClause(CobolParser.DestinationTableClauseContext ctx) {
        throw new UnsupportedOperationException("Implement me");
    }

    @Override
    public Object visitDisableStatement(CobolParser.DisableStatementContext ctx) {
        return new Cobol.Disable(
                randomId(),
                prefix(ctx),
                Markers.EMPTY,
                ctx.DISABLE().getText(),
                words(ctx.INPUT(), ctx.I_O(), ctx.TERMINAL(), ctx.OUTPUT()),
                (Name) visit(ctx.cdName()),
                ctx.WITH() == null ? null : words(ctx.WITH()),
                words(ctx.KEY()),
                visit(ctx.identifier(), ctx.literal())
        );
    }

    @Override
    public Object visitDivideByGivingStatement(CobolParser.DivideByGivingStatementContext ctx) {
        return new Cobol.DivideGiving(
                randomId(),
                prefix(ctx),
                Markers.EMPTY,
                words(ctx.BY()),
                visit(ctx.identifier(), ctx.literal()),
                visitNullable(ctx.divideGivingPhrase())
        );
    }

    @Override
    public Object visitDivideGivingPhrase(CobolParser.DivideGivingPhraseContext ctx) {
        return new Cobol.DivideGivingPhrase(
                randomId(),
                prefix(ctx),
                Markers.EMPTY,
                words(ctx.GIVING()),
                convertAllContainer(ctx.divideGiving())
        );
    }

    @Override
    public Object visitDivideIntoGivingStatement(CobolParser.DivideIntoGivingStatementContext ctx) {
        return new Cobol.DivideGiving(
                randomId(),
                prefix(ctx),
                Markers.EMPTY,
                words(ctx.INTO()),
                visit(ctx.identifier(), ctx.literal()),
                visitNullable(ctx.divideGivingPhrase())
        );
    }

    @Override
    public Object visitDivideIntoStatement(CobolParser.DivideIntoStatementContext ctx) {
        return new Cobol.DivideInto(
                randomId(),
                prefix(ctx),
                Markers.EMPTY,
                words(ctx.INTO()),
                convertAllContainer(ctx.divideInto())
        );
    }

    @Override
    public Object visitDivideRemainder(CobolParser.DivideRemainderContext ctx) {
        return new Cobol.DivideRemainder(
                randomId(),
                prefix(ctx),
                Markers.EMPTY,
                words(ctx.REMAINDER()),
                (Name) visit(ctx.identifier())
        );
    }

    @Override
    public Object visitDivideStatement(CobolParser.DivideStatementContext ctx) {
        return new Cobol.Divide(
                randomId(),
                prefix(ctx),
                Markers.EMPTY,
                words(ctx.DIVIDE()),
                visit(ctx.identifier(), ctx.literal()),
                visit(ctx.divideIntoStatement(), ctx.divideIntoGivingStatement(), ctx.divideByGivingStatement()),
                visitNullable(ctx.divideRemainder()),
                visitNullable(ctx.onSizeErrorPhrase()),
                visitNullable(ctx.notOnSizeErrorPhrase()),
                padLeft(ctx.END_DIVIDE())
        );
    }

    @Override
    public Cobol.SpecialNames visitSpecialNamesParagraph(CobolParser.SpecialNamesParagraphContext ctx) {
        return new Cobol.SpecialNames(
                randomId(),
                sourceBefore(ctx.SPECIAL_NAMES().getText()),
                Markers.EMPTY,
                ctx.SPECIAL_NAMES().getText(),
                ctx.specialNameClause() == null ?
                        convertAllContainer(sourceBefore("."), emptyList()) :
                        convertAllContainer(sourceBefore("."), ctx.specialNameClause())
                                .withLastSpace(sourceBefore("."))
        );
    }

    @Override
    public Cobol.AlphabetClause visitAlphabetClauseFormat1(CobolParser.AlphabetClauseFormat1Context ctx) {
        Space prefix = sourceBefore(ctx.ALPHABET().getText());
        Identifier name = (Identifier) visit(ctx.alphabetName());
        CobolLeftPadded<String> standard = padLeft(whitespace(), words(ctx.FOR(), ctx.ALPHANUMERIC(), ctx.IS(), ctx.EBCDIC(), ctx.ASCII(), ctx.STANDARD_1(), ctx.STANDARD_2(), ctx.NATIVE()));
        if (ctx.cobolWord() != null) {
            standard = standard.withElement(ctx.cobolWord().getText());
        }
        if (standard.getElement().isEmpty()) {
            standard = null;
        }
        return new Cobol.AlphabetClause(
                randomId(),
                prefix,
                Markers.EMPTY,
                ctx.ALPHABET().getText(),
                name,
                standard,
                ctx.alphabetLiterals() == null ? null : convertAllContainer(ctx.alphabetLiterals())
        );
    }

    @Override
    public Cobol.AlphabetClause visitAlphabetClauseFormat2(CobolParser.AlphabetClauseFormat2Context ctx) {
        return new Cobol.AlphabetClause(
                randomId(),
                prefix(ctx),
                Markers.EMPTY,
                words(ctx.ALPHABET()),
                (Identifier) visit(ctx.alphabetName()),
                padLeft(whitespace(), words(ctx.FOR(), ctx.NATIONAL(), ctx.IS(), ctx.NATIVE(), ctx.CCSVERSION())),
                ctx.CCSVERSION() == null ? null : convertAllContainer(singletonList(ctx.literal()))
        );
    }

    @Override
    public Cobol.AlphabetLiteral visitAlphabetLiterals(CobolParser.AlphabetLiteralsContext ctx) {
        return new Cobol.AlphabetLiteral(
                randomId(),
                prefix(ctx),
                Markers.EMPTY,
                (Literal) visit(ctx.literal()),
                visitNullable(ctx.alphabetThrough()),
                ctx.alphabetAlso() == null ? null : convertAllContainer(ctx.alphabetAlso())
        );
    }

    @Override
    public Cobol.AlphabetThrough visitAlphabetThrough(CobolParser.AlphabetThroughContext ctx) {
        return new Cobol.AlphabetThrough(
                randomId(),
                prefix(ctx),
                Markers.EMPTY,
                words(ctx.THROUGH(), ctx.THRU()),
                (Literal) visit(ctx.literal())
        );
    }

    @Override
    public Cobol.AlphabetAlso visitAlphabetAlso(CobolParser.AlphabetAlsoContext ctx) {
        return new Cobol.AlphabetAlso(
                randomId(),
                sourceBefore(ctx.ALSO().getText()),
                Markers.EMPTY,
                ctx.ALSO().getText(),
                convertAllContainer(ctx.literal())
        );
    }

    @Override
    public Cobol.CollatingSequenceClause visitCollatingSequenceClause(CobolParser.CollatingSequenceClauseContext ctx) {
        return new Cobol.CollatingSequenceClause(
                randomId(),
                prefix(ctx),
                Markers.EMPTY,
                words(ctx.PROGRAM(), ctx.COLLATING(), ctx.SEQUENCE()),
                convertAllContainer(padLeft(ctx.IS()), ctx.alphabetName()),
                visitNullable(ctx.collatingSequenceClauseAlphanumeric()),
                visitNullable(ctx.collatingSequenceClauseNational())
        );
    }

    @Override
    public Cobol.CollatingSequenceAlphabet visitCollatingSequenceClauseAlphanumeric(CobolParser.CollatingSequenceClauseAlphanumericContext ctx) {
        return new Cobol.CollatingSequenceAlphabet(
                randomId(),
                prefix(ctx),
                Markers.EMPTY,
                words(ctx.FOR(), ctx.ALPHANUMERIC(), ctx.IS()),
                (Identifier) visit(ctx.alphabetName())
        );
    }

    @Override
    public Object visitCollatingSequenceClauseNational(CobolParser.CollatingSequenceClauseNationalContext ctx) {
        return new Cobol.CollatingSequenceAlphabet(
                randomId(),
                prefix(ctx),
                Markers.EMPTY,
                words(ctx.FOR(), ctx.NATIONAL(), ctx.IS()),
                (Identifier) visit(ctx.alphabetName())
        );
    }

    @Override
    public Cobol.ConfigurationSection visitConfigurationSection(CobolParser.ConfigurationSectionContext ctx) {
        return new Cobol.ConfigurationSection(
                randomId(),
                prefix(ctx),
                Markers.EMPTY,
                words(ctx.CONFIGURATION(), ctx.SECTION()),
                convertAllContainer(sourceBefore("."), ctx.configurationSectionParagraph())
        );
    }

    @Override
    public Cobol.ObjectComputer visitObjectComputerParagraph(CobolParser.ObjectComputerParagraphContext ctx) {
        return new Cobol.ObjectComputer(
                randomId(),
                prefix(ctx),
                Markers.EMPTY,
                padRight(words(ctx.OBJECT_COMPUTER()), sourceBefore(".")),
                ctx.computerName() == null ? null : padRight(
                        new Cobol.ObjectComputerDefinition(
                                randomId(),
                                sourceBefore(ctx.computerName().getText()),
                                Markers.EMPTY,
                                ctx.computerName().getText(),
                                convertAllContainer(ctx.objectComputerClause())
                        ),
                        sourceBefore(".")
                )
        );
    }

    @Override
    public Object visitLabelRecordsClause(CobolParser.LabelRecordsClauseContext ctx) {
        return new Cobol.LabelRecordsClause(
                randomId(),
                prefix(ctx),
                Markers.EMPTY,
                words(ctx.LABEL(), ctx.RECORD(), ctx.IS(), ctx.RECORDS(), ctx.ARE(), ctx.OMITTED(), ctx.STANDARD()),
                convertAllContainer(ctx.dataName())
        );
    }

    @Override
    public Object visitLibraryAttributeClauseFormat1(CobolParser.LibraryAttributeClauseFormat1Context ctx) {
        return new Cobol.LibraryAttributeClauseFormat1(
                randomId(),
                prefix(ctx),
                Markers.EMPTY,
                words(ctx.ATTRIBUTE(), ctx.SHARING(), ctx.IS(), ctx.DONTCARE(), ctx.PRIVATE(), ctx.SHAREDBYRUNUNIT(), ctx.SHAREDBYALL())
        );
    }

    @Override
    public Object visitLibraryAttributeClauseFormat2(CobolParser.LibraryAttributeClauseFormat2Context ctx) {
        return new Cobol.LibraryAttributeClauseFormat2(
                randomId(),
                prefix(ctx),
                Markers.EMPTY,
                words(ctx.ATTRIBUTE()),
                visitNullable(ctx.libraryAttributeFunction()),
                words(ctx.LIBACCESS(), ctx.IS(), ctx.BYFUNCTION(), ctx.BYTITLE()),
                visitNullable(ctx.libraryAttributeParameter()),
                visitNullable(ctx.libraryAttributeTitle())
        );
    }

    @Override
    public Object visitLibraryAttributeFunction(CobolParser.LibraryAttributeFunctionContext ctx) {
        return new Cobol.LibraryAttributeFunction(
                randomId(),
                prefix(ctx),
                Markers.EMPTY,
                words(ctx.FUNCTIONNAME(), ctx.IS()),
                (Name) visit(ctx.literal())
        );
    }

    @Override
    public Object visitLibraryAttributeParameter(CobolParser.LibraryAttributeParameterContext ctx) {
        return new Cobol.LibraryAttributeParameter(
                randomId(),
                prefix(ctx),
                Markers.EMPTY,
                words(ctx.LIBPARAMETER(), ctx.IS()),
                (Name) visit(ctx.literal())
        );
    }

    @Override
    public Object visitLibraryAttributeTitle(CobolParser.LibraryAttributeTitleContext ctx) {
        return new Cobol.LibraryAttributeTitle(
                randomId(),
                prefix(ctx),
                Markers.EMPTY,
                words(ctx.TITLE(), ctx.IS()),
                (Name) visit(ctx.literal())
        );
    }

    @Override
    public Object visitLibraryDescriptionEntryFormat1(CobolParser.LibraryDescriptionEntryFormat1Context ctx) {
        return new Cobol.LibraryDescriptionEntryFormat1(
                randomId(),
                prefix(ctx),
                Markers.EMPTY,
                words(ctx.LD()),
                (Cobol.CobolWord) visit(ctx.libraryName()),
                words(ctx.EXPORT()),
                visitNullable(ctx.libraryAttributeClauseFormat1()),
                visitNullable(ctx.libraryEntryProcedureClauseFormat1())
        );
    }

    @Override
    public Object visitLibraryDescriptionEntryFormat2(CobolParser.LibraryDescriptionEntryFormat2Context ctx) {
        return new Cobol.LibraryDescriptionEntryFormat2(
                randomId(),
                prefix(ctx),
                Markers.EMPTY,
                words(ctx.LB()),
                (Cobol.CobolWord) visit(ctx.libraryName()),
                words(ctx.IMPORT()),
                visitNullable(ctx.libraryIsGlobalClause()),
                visitNullable(ctx.libraryIsCommonClause()),
                convertAllContainer(ctx.libraryAttributeClauseFormat2(), ctx.libraryEntryProcedureClauseFormat2())
        );
    }

    @Override
    public Object visitLibraryEntryProcedureClauseFormat1(CobolParser.LibraryEntryProcedureClauseFormat1Context ctx) {
        return new Cobol.LibraryEntryProcedureClauseFormat1(
                randomId(),
                prefix(ctx),
                Markers.EMPTY,
                words(ctx.ENTRY_PROCEDURE()),
                (Cobol.CobolWord) visit(ctx.programName()),
                visitNullable(ctx.libraryEntryProcedureForClause())
        );
    }

    @Override
    public Object visitLibraryEntryProcedureClauseFormat2(CobolParser.LibraryEntryProcedureClauseFormat2Context ctx) {
        return new Cobol.LibraryEntryProcedureClauseFormat2(
                randomId(),
                prefix(ctx),
                Markers.EMPTY,
                words(ctx.ENTRY_PROCEDURE()),
                (Cobol.CobolWord) visit(ctx.programName()),
                visitNullable(ctx.libraryEntryProcedureForClause()),
                visitNullable(ctx.libraryEntryProcedureWithClause()),
                visitNullable(ctx.libraryEntryProcedureUsingClause()),
                visitNullable(ctx.libraryEntryProcedureGivingClause())
        );
    }

    @Override
    public Object visitLibraryEntryProcedureForClause(CobolParser.LibraryEntryProcedureForClauseContext ctx) {
        return new Cobol.LibraryEntryProcedureForClause(
                randomId(),
                prefix(ctx),
                Markers.EMPTY,
                words(ctx.FOR()),
                (Name) visit(ctx.literal())
        );
    }

    @Override
    public Object visitLibraryEntryProcedureGivingClause(CobolParser.LibraryEntryProcedureGivingClauseContext ctx) {
        return new Cobol.LibraryEntryProcedureGivingClause(
                randomId(),
                prefix(ctx),
                Markers.EMPTY,
                words(ctx.GIVING()),
                (Cobol.CobolWord) visit(ctx.dataName())
        );
    }

    @Override
    public Object visitLibraryEntryProcedureUsingClause(CobolParser.LibraryEntryProcedureUsingClauseContext ctx) {
        return new Cobol.LibraryEntryProcedureUsingClause(
                randomId(),
                prefix(ctx),
                Markers.EMPTY,
                words(ctx.USING()),
                convertAllContainer(ctx.libraryEntryProcedureUsingName())
        );
    }

    @Override
    public Object visitLibraryEntryProcedureWithClause(CobolParser.LibraryEntryProcedureWithClauseContext ctx) {
        return new Cobol.LibraryEntryProcedureWithClause(
                randomId(),
                prefix(ctx),
                Markers.EMPTY,
                words(ctx.WITH()),
                convertAllContainer(ctx.libraryEntryProcedureWithName())
        );
    }

    @Override
    public Object visitLibraryIsCommonClause(CobolParser.LibraryIsCommonClauseContext ctx) {
        return new Cobol.LibraryIsCommonClause(
                randomId(),
                prefix(ctx),
                Markers.EMPTY,
                words(ctx.IS(), ctx.COMMON())
        );
    }

    @Override
    public Object visitLibraryIsGlobalClause(CobolParser.LibraryIsGlobalClauseContext ctx) {
        return new Cobol.LibraryIsGlobalClause(
                randomId(),
                prefix(ctx),
                Markers.EMPTY,
                words(ctx.IS(), ctx.GLOBAL())
        );
    }

    @Override
    public Cobol.ValuedObjectComputerClause visitMemorySizeClause(CobolParser.MemorySizeClauseContext ctx) {
        return new Cobol.ValuedObjectComputerClause(
                randomId(),
                prefix(ctx),
                Markers.EMPTY,
                Cobol.ValuedObjectComputerClause.Type.Memory,
                words(ctx.MEMORY(), ctx.SIZE()),
                (Cobol) visit(ctx.integerLiteral() == null ? ctx.cobolWord() : ctx.integerLiteral()),
                ctx.WORDS() != null || ctx.CHARACTERS() != null || ctx.MODULES() != null ?
                        padLeft(whitespace(), words(ctx.WORDS(), ctx.CHARACTERS(), ctx.MODULES())) :
                        null
        );
    }

    @Override
    public Object visitMessageCountClause(CobolParser.MessageCountClauseContext ctx) {
        throw new UnsupportedOperationException("Implement me");
    }

    @Override
    public Object visitMessageDateClause(CobolParser.MessageDateClauseContext ctx) {
        throw new UnsupportedOperationException("Implement me");
    }

    @Override
    public Object visitMessageTimeClause(CobolParser.MessageTimeClauseContext ctx) {
        throw new UnsupportedOperationException("Implement me");
    }

    @Override
    public Cobol.ValuedObjectComputerClause visitDiskSizeClause(CobolParser.DiskSizeClauseContext ctx) {
        return new Cobol.ValuedObjectComputerClause(
                randomId(),
                prefix(ctx),
                Markers.EMPTY,
                Cobol.ValuedObjectComputerClause.Type.Disk,
                words(ctx.DISK(), ctx.SIZE()),
                (Cobol) visit(ctx.integerLiteral() == null ? ctx.cobolWord() : ctx.integerLiteral()),
                ctx.WORDS() != null || ctx.MODULES() != null ?
                        padLeft(whitespace(), words(ctx.WORDS(), ctx.MODULES())) :
                        null
        );
    }

    @Override
    public Object visitPasswordClause(CobolParser.PasswordClauseContext ctx) {
        return new Cobol.PasswordClause(
                randomId(),
                prefix(ctx),
                Markers.EMPTY,
                words(ctx.PASSWORD(), ctx.IS()),
                (Cobol.CobolWord) visit(ctx.dataName())
        );
    }

    @Override
    public Object visitProgramLibrarySection(CobolParser.ProgramLibrarySectionContext ctx) {
        return new Cobol.ProgramLibrarySection(
                randomId(),
                prefix(ctx),
                Markers.EMPTY,
                words(ctx.PROGRAM_LIBRARY(), ctx.SECTION(), ctx.DOT_FS()),
                convertAllContainer(ctx.libraryDescriptionEntry())
        );
    }

    @Override
    public Object visitRecordKeyClause(CobolParser.RecordKeyClauseContext ctx) {
        return new Cobol.RecordKeyClause(
                randomId(),
                prefix(ctx),
                Markers.EMPTY,
                words(ctx.RECORD(), ctx.KEY(), ctx.IS()),
                (Cobol.QualifiedDataName) visit(ctx.qualifiedDataName()),
                visitNullable(ctx.passwordClause()),
                words(ctx.WITH(), ctx.DUPLICATES())
        );
    }

    @Override
    public Object visitReferenceModifier(CobolParser.ReferenceModifierContext ctx) {
        return new Cobol.ReferenceModifier(
                randomId(),
                prefix(ctx),
                Markers.EMPTY,
                words(ctx.LPARENCHAR()),
                (Cobol.ArithmeticExpression) visit(ctx.characterPosition()),
                words(ctx.COLONCHAR()),
                (Cobol.ArithmeticExpression) visit(ctx.length()),
                words(ctx.RPARENCHAR())
        );
    }

    @Override
    public Object visitRelativeKeyClause(CobolParser.RelativeKeyClauseContext ctx) {
        return new Cobol.RelativeKeyClause(
                randomId(),
                prefix(ctx),
                Markers.EMPTY,
                words(ctx.RELATIVE(), ctx.KEY(), ctx.IS()),
                (Cobol.QualifiedDataName) visit(ctx.qualifiedDataName())
        );
    }

    @Override
    public Object visitReportDescription(CobolParser.ReportDescriptionContext ctx) {
        return new Cobol.ReportDescription(
                randomId(),
                prefix(ctx),
                Markers.EMPTY,
                (Cobol.ReportDescriptionEntry) visit(ctx.reportDescriptionEntry()),
                convertAllContainer(ctx.reportGroupDescriptionEntry())
        );
    }

    @Override
    public Object visitReportDescriptionEntry(CobolParser.ReportDescriptionEntryContext ctx) {
        return new Cobol.ReportDescriptionEntry(
                randomId(),
                prefix(ctx),
                Markers.EMPTY,
                words(ctx.RD()),
                (Cobol.QualifiedDataName) visit(ctx.reportName()),
                visitNullable(ctx.reportDescriptionGlobalClause()),
                visitNullable(ctx.reportDescriptionPageLimitClause()),
                visitNullable(ctx.reportDescriptionHeadingClause()),
                visitNullable(ctx.reportDescriptionFirstDetailClause()),
                visitNullable(ctx.reportDescriptionLastDetailClause()),
                visitNullable(ctx.reportDescriptionFootingClause()),
                padLeft(ctx.DOT_FS())
        );
    }

    @Override
    public Object visitReportDescriptionFirstDetailClause(CobolParser.ReportDescriptionFirstDetailClauseContext ctx) {
        throw new UnsupportedOperationException("Implement me");
    }

    @Override
    public Object visitReportDescriptionFootingClause(CobolParser.ReportDescriptionFootingClauseContext ctx) {
        throw new UnsupportedOperationException("Implement me");
    }

    @Override
    public Object visitReportDescriptionHeadingClause(CobolParser.ReportDescriptionHeadingClauseContext ctx) {
        throw new UnsupportedOperationException("Implement me");
    }

    @Override
    public Object visitReportDescriptionLastDetailClause(CobolParser.ReportDescriptionLastDetailClauseContext ctx) {
        throw new UnsupportedOperationException("Implement me");
    }

    @Override
    public Object visitReportDescriptionPageLimitClause(CobolParser.ReportDescriptionPageLimitClauseContext ctx) {
        throw new UnsupportedOperationException("Implement me");
    }

    @Override
    public Object visitReportGroupBlankWhenZeroClause(CobolParser.ReportGroupBlankWhenZeroClauseContext ctx) {
        throw new UnsupportedOperationException("Implement me");
    }

    @Override
    public Object visitReportGroupColumnNumberClause(CobolParser.ReportGroupColumnNumberClauseContext ctx) {
        throw new UnsupportedOperationException("Implement me");
    }

    @Override
    public Object visitReportGroupDescriptionEntryFormat1(CobolParser.ReportGroupDescriptionEntryFormat1Context ctx) {
        return new Cobol.ReportGroupDescriptionEntryFormat1(
                randomId(),
                prefix(ctx),
                Markers.EMPTY,
                (Cobol.CobolWord) visit(ctx.integerLiteral()),
                (Cobol.CobolWord) visit(ctx.dataName()),
                visitNullable(ctx.reportGroupLineNumberClause()),
                visitNullable(ctx.reportGroupNextGroupClause()),
                (Cobol.ReportGroupTypeClause) visit(ctx.reportGroupTypeClause()),
                visitNullable(ctx.reportGroupUsageClause()),
                padLeft(ctx.DOT_FS())
        );
    }

    @Override
    public Object visitReportGroupDescriptionEntryFormat2(CobolParser.ReportGroupDescriptionEntryFormat2Context ctx) {
        return new Cobol.ReportGroupDescriptionEntryFormat2(
                randomId(),
                prefix(ctx),
                Markers.EMPTY,
                (Cobol.CobolWord) visit(ctx.integerLiteral()),
                visitNullable(ctx.dataName()),
                visitNullable(ctx.reportGroupLineNumberClause()),
                (Cobol.ReportGroupUsageClause) visit(ctx.reportGroupUsageClause()),
                padLeft(ctx.DOT_FS())
        );
    }

    @Override
    public Object visitReportGroupDescriptionEntryFormat3(CobolParser.ReportGroupDescriptionEntryFormat3Context ctx) {
        return new Cobol.ReportGroupDescriptionEntryFormat3(
                randomId(),
                prefix(ctx),
                Markers.EMPTY,
                (Cobol.CobolWord) visit(ctx.integerLiteral()),
                visitNullable(ctx.dataName()),
                convertAllContainer(ctx.reportGroupPictureClause(),
                        ctx.reportGroupUsageClause(),
                        ctx.reportGroupSignClause(),
                        ctx.reportGroupJustifiedClause(),
                        ctx.reportGroupBlankWhenZeroClause(),
                        ctx.reportGroupLineNumberClause(),
                        ctx.reportGroupColumnNumberClause(),
                        ctx.reportGroupSourceClause(),
                        ctx.reportGroupValueClause(),
                        ctx.reportGroupSumClause(),
                        ctx.reportGroupResetClause(),
                        ctx.reportGroupIndicateClause()),
                padLeft(ctx.DOT_FS())
        );
    }

    @Override
    public Object visitReportGroupIndicateClause(CobolParser.ReportGroupIndicateClauseContext ctx) {
        throw new UnsupportedOperationException("Implement me");
    }

    @Override
    public Object visitReportGroupJustifiedClause(CobolParser.ReportGroupJustifiedClauseContext ctx) {
        throw new UnsupportedOperationException("Implement me");
    }

    @Override
    public Object visitReportGroupLineNumberClause(CobolParser.ReportGroupLineNumberClauseContext ctx) {
        return new Cobol.ReportGroupLineNumberClause(
                randomId(),
                prefix(ctx),
                Markers.EMPTY,
                words(ctx.LINE(), ctx.NUMBER(), ctx.IS()),
                visit(ctx.reportGroupLineNumberNextPage(), ctx.reportGroupLineNumberPlus())
        );
    }

    @Override
    public Object visitReportGroupLineNumberNextPage(CobolParser.ReportGroupLineNumberNextPageContext ctx) {
        return new Cobol.ReportGroupLineNumberNextPage(
                randomId(),
                prefix(ctx),
                Markers.EMPTY,
                (Cobol.CobolWord) visit(ctx.integerLiteral()),
                words(ctx.ON(), ctx.NEXT(), ctx.PAGE())
        );
    }

    @Override
    public Object visitReportGroupLineNumberPlus(CobolParser.ReportGroupLineNumberPlusContext ctx) {
        return new Cobol.ReportGroupLineNumberPlus(
                randomId(),
                prefix(ctx),
                Markers.EMPTY,
                words(ctx.PLUS()),
                (Cobol.CobolWord) visit(ctx.integerLiteral())
        );
    }

    @Override
    public Object visitReportGroupNextGroupClause(CobolParser.ReportGroupNextGroupClauseContext ctx) {
        return new Cobol.ReportGroupNextGroupClause(
                randomId(),
                prefix(ctx),
                Markers.EMPTY,
                words(ctx.NEXT(), ctx.GROUP(), ctx.IS()),
                visit(ctx.integerLiteral(), ctx.reportGroupNextGroupNextPage(), ctx.reportGroupNextGroupPlus())
        );
    }

    @Override
    public Object visitReportGroupNextGroupNextPage(CobolParser.ReportGroupNextGroupNextPageContext ctx) {
        return new Cobol.ReportGroupNextGroupNextPage(
                randomId(),
                prefix(ctx),
                Markers.EMPTY,
                words(ctx.NEXT(), ctx.PAGE())
        );
    }

    @Override
    public Object visitReportGroupNextGroupPlus(CobolParser.ReportGroupNextGroupPlusContext ctx) {
        return new Cobol.ReportGroupNextGroupPlus(
                randomId(),
                prefix(ctx),
                Markers.EMPTY,
                words(ctx.PLUS()),
                (Cobol.CobolWord) visit(ctx.integerLiteral())
        );
    }

    @Override
    public Object visitReportGroupPictureClause(CobolParser.ReportGroupPictureClauseContext ctx) {
        throw new UnsupportedOperationException("Implement me");
    }

    @Override
    public Object visitReportGroupResetClause(CobolParser.ReportGroupResetClauseContext ctx) {
        throw new UnsupportedOperationException("Implement me");
    }

    @Override
    public Object visitReportGroupSignClause(CobolParser.ReportGroupSignClauseContext ctx) {
        throw new UnsupportedOperationException("Implement me");
    }

    @Override
    public Object visitReportGroupSourceClause(CobolParser.ReportGroupSourceClauseContext ctx) {
        throw new UnsupportedOperationException("Implement me");
    }

    @Override
    public Object visitReportGroupSumClause(CobolParser.ReportGroupSumClauseContext ctx) {
        throw new UnsupportedOperationException("Implement me");
    }

    @Override
    public Object visitReportGroupTypeClause(CobolParser.ReportGroupTypeClauseContext ctx) {
        return new Cobol.ReportGroupTypeClause(
                randomId(),
                prefix(ctx),
                Markers.EMPTY,
                words(ctx.TYPE(), ctx.IS()),
                visit(ctx.reportGroupTypeReportHeading(),
                        ctx.reportGroupTypeReportFooting(),
                        ctx.reportGroupTypePageHeading(),
                        ctx.reportGroupTypePageFooting(),
                        ctx.reportGroupTypeControlHeading(),
                        ctx.reportGroupTypeControlFooting(),
                        ctx.reportGroupTypeDetail())
        );
    }

    @Override
    public Object visitReportGroupTypeControlHeading(CobolParser.ReportGroupTypeControlHeadingContext ctx) {
        throw new UnsupportedOperationException("Implement me");
    }

    @Override
    public Object visitReportGroupTypeControlFooting(CobolParser.ReportGroupTypeControlFootingContext ctx) {
        throw new UnsupportedOperationException("Implement me");
    }

    @Override
    public Object visitReportGroupTypeDetail(CobolParser.ReportGroupTypeDetailContext ctx) {
        throw new UnsupportedOperationException("Implement me");
    }

    @Override
    public Object visitReportGroupTypePageHeading(CobolParser.ReportGroupTypePageHeadingContext ctx) {
        throw new UnsupportedOperationException("Implement me");
    }

    @Override
    public Object visitReportGroupTypePageFooting(CobolParser.ReportGroupTypePageFootingContext ctx) {
        throw new UnsupportedOperationException("Implement me");
    }

    @Override
    public Object visitReportGroupTypeReportHeading(CobolParser.ReportGroupTypeReportHeadingContext ctx) {
        throw new UnsupportedOperationException("Implement me");
    }

    @Override
    public Object visitReportGroupTypeReportFooting(CobolParser.ReportGroupTypeReportFootingContext ctx) {
        throw new UnsupportedOperationException("Implement me");
    }

    @Override
    public Object visitReportGroupUsageClause(CobolParser.ReportGroupUsageClauseContext ctx) {
        return new Cobol.ReportGroupUsageClause(
                randomId(),
                prefix(ctx),
                Markers.EMPTY,
                words(ctx.USAGE(), ctx.IS(), ctx.DISPLAY(), ctx.DISPLAY_1())
        );
    }

    @Override
    public Object visitReportGroupValueClause(CobolParser.ReportGroupValueClauseContext ctx) {
        throw new UnsupportedOperationException("Implement me");
    }

    @Override
    public Object visitReportDescriptionGlobalClause(CobolParser.ReportDescriptionGlobalClauseContext ctx) {
        return new Cobol.ReportDescriptionGlobalClause(
                randomId(),
                prefix(ctx),
                Markers.EMPTY,
                words(ctx.IS(), ctx.GLOBAL())
        );
    }

    @Override
    public Object visitReportSection(CobolParser.ReportSectionContext ctx) {
        return new Cobol.ReportSection(
                randomId(),
                prefix(ctx),
                Markers.EMPTY,
                words(ctx.REPORT(), ctx.SECTION(), ctx.DOT_FS()),
                convertAllContainer(ctx.reportDescription())
        );
    }

    @Override
    public Object visitReserveClause(CobolParser.ReserveClauseContext ctx) {
        return new Cobol.ReserveClause(
                randomId(),
                prefix(ctx),
                Markers.EMPTY,
                convertAllList(emptyList(), singletonList(ctx.RESERVE()), singletonList(ctx.NO()),
                        singletonList(ctx.integerLiteral()), singletonList(ctx.ALTERNATE()),
                        singletonList(ctx.AREA()), singletonList(ctx.AREAS()))
        );
    }

    @Override
    public Object visitSameClause(CobolParser.SameClauseContext ctx) {
        return new Cobol.SameClause(
                randomId(),
                prefix(ctx),
                Markers.EMPTY,
                words(ctx.SAME(), ctx.RECORD(), ctx.SORT(), ctx.SORT_MERGE(), ctx.AREA(), ctx.FOR()),
                convertAllContainer(ctx.fileName())
        );
    }

    @Override
    public Object visitSubtractStatement(CobolParser.SubtractStatementContext ctx) {
        return new Cobol.Subtract(
                randomId(),
                prefix(ctx),
                Markers.EMPTY,
                (Cobol.CobolWord) visit(ctx.SUBTRACT()),
                visit(ctx.subtractFromStatement(), ctx.subtractFromGivingStatement(), ctx.subtractCorrespondingStatement()),
                visitNullable(ctx.onSizeErrorPhrase()),
                visitNullable(ctx.notOnSizeErrorPhrase()),
                padLeft(ctx.END_SUBTRACT())
        );
    }

    @Override
    public Object visitSubtractFromStatement(CobolParser.SubtractFromStatementContext ctx) {
        return new Cobol.SubtractFromStatement(
                randomId(),
                prefix(ctx),
                Markers.EMPTY,
                convertAllContainer(ctx.subtractSubtrahend()),
                (Cobol.CobolWord) visit(ctx.FROM()),
                convertAllContainer(ctx.subtractMinuend())
        );
    }

    @Override
    public Object visitSubtractFromGivingStatement(CobolParser.SubtractFromGivingStatementContext ctx) {
        return new Cobol.SubtractFromGivingStatement(
                randomId(),
                prefix(ctx),
                Markers.EMPTY,
                convertAllContainer(ctx.subtractSubtrahend()),
                (Cobol.CobolWord) visit(ctx.FROM()),
                (Cobol.CobolWord) visit(ctx.subtractMinuendGiving()),
                (Cobol.CobolWord) visit(ctx.GIVING()),
                convertAllContainer(ctx.subtractGiving())
        );
    }

    @Override
    public Object visitSubtractCorrespondingStatement(CobolParser.SubtractCorrespondingStatementContext ctx) {
        return new Cobol.SubtractCorrespondingStatement(
                randomId(),
                prefix(ctx),
                Markers.EMPTY,
                visit(ctx.CORRESPONDING(), ctx.CORR()),
                (Cobol.QualifiedDataName) visit(ctx.qualifiedDataName()),
                (Cobol.CobolWord) visit(ctx.FROM()),
                (Cobol.SubtractMinuendCorresponding) visit(ctx.subtractMinuendCorresponding())
        );
    }

    @Override
    public Object visitSubtractMinuendCorresponding(CobolParser.SubtractMinuendCorrespondingContext ctx) {
        return new Cobol.SubtractMinuendCorresponding(
                randomId(),
                prefix(ctx),
                Markers.EMPTY,
                (Cobol.QualifiedDataName) visit(ctx.qualifiedDataName()),
                visitNullable(ctx.ROUNDED())
        );
    }

    @Override
    public Object visitScreenDescriptionAutoClause(CobolParser.ScreenDescriptionAutoClauseContext ctx) {
        return new Cobol.ScreenDescriptionAutoClause(
                randomId(),
                prefix(ctx),
                Markers.EMPTY,
                words(ctx.AUTO(), ctx.AUTO_SKIP())
        );
    }

    @Override
    public Object visitScreenDescriptionBackgroundColorClause(CobolParser.ScreenDescriptionBackgroundColorClauseContext ctx) {
        return new Cobol.ScreenDescriptionBackgroundColorClause(
                randomId(),
                prefix(ctx),
                Markers.EMPTY,
                words(ctx.BACKGROUND_COLOR(), ctx.BACKGROUND_COLOUR()),
                words(ctx.IS()),
                visit(ctx.identifier(), ctx.identifier())
        );
    }

    @Override
    public Object visitScreenDescriptionBellClause(CobolParser.ScreenDescriptionBellClauseContext ctx) {
        return new Cobol.ScreenDescriptionBellClause(
                randomId(),
                prefix(ctx),
                Markers.EMPTY,
                words(ctx.BELL())
        );
    }

    @Override
    public Object visitScreenDescriptionBlankClause(CobolParser.ScreenDescriptionBlankClauseContext ctx) {
        return new Cobol.ScreenDescriptionBlankClause(
                randomId(),
                prefix(ctx),
                Markers.EMPTY,
                words(ctx.BLANK(), ctx.SCREEN(), ctx.LINE())
        );
    }

    @Override
    public Object visitScreenDescriptionBlankWhenZeroClause(CobolParser.ScreenDescriptionBlankWhenZeroClauseContext ctx) {
        return new Cobol.ScreenDescriptionBlankWhenZeroClause(
                randomId(),
                prefix(ctx),
                Markers.EMPTY,
                words(ctx.BLANK(), ctx.WHEN(), ctx.ZERO())
        );
    }

    @Override
    public Object visitScreenDescriptionBlinkClause(CobolParser.ScreenDescriptionBlinkClauseContext ctx) {
        return new Cobol.ScreenDescriptionBlankWhenZeroClause(
                randomId(),
                prefix(ctx),
                Markers.EMPTY,
                words(ctx.BLINK())
        );
    }

    @Override
    public Object visitScreenDescriptionColumnClause(CobolParser.ScreenDescriptionColumnClauseContext ctx) {
        return new Cobol.ScreenDescriptionColumnClause(
                randomId(),
                prefix(ctx),
                Markers.EMPTY,
                words(ctx.COLUMN(), ctx.COL(), ctx.NUMBER(), ctx.IS(), ctx.PLUS(), ctx.PLUSCHAR(), ctx.MINUSCHAR()),
                visit(ctx.identifier(), ctx.integerLiteral())
        );
    }

    @Override
    public Object visitScreenDescriptionControlClause(CobolParser.ScreenDescriptionControlClauseContext ctx) {
        return new Cobol.ScreenDescriptionControlClause(
                randomId(),
                prefix(ctx),
                Markers.EMPTY,
                words(ctx.CONTROL(), ctx.IS()),
                (Identifier) visit(ctx.identifier())
        );
    }


    @Override
    public Object visitScreenDescriptionEntry(CobolParser.ScreenDescriptionEntryContext ctx) {
        return new Cobol.ScreenDescriptionEntry(
                randomId(),
                prefix(ctx),
                Markers.EMPTY,
                words(ctx.INTEGERLITERAL()),
                ctx.FILLER() == null ?
                        (ctx.screenName() == null ? null : padLeft(ctx.screenName())) :
                        padLeft(ctx.FILLER()),
                convertAllContainer(ctx.screenDescriptionBlankClause(),
                        ctx.screenDescriptionAutoClause(),
                        ctx.screenDescriptionBellClause(),
                        ctx.screenDescriptionBlinkClause(),
                        ctx.screenDescriptionBlankWhenZeroClause(),
                        ctx.screenDescriptionBackgroundColorClause(),
                        ctx.screenDescriptionEraseClause(),
                        ctx.screenDescriptionLightClause(),
                        ctx.screenDescriptionGridClause(),
                        ctx.screenDescriptionReverseVideoClause(),
                        ctx.screenDescriptionUnderlineClause(),
                        ctx.screenDescriptionSizeClause(),
                        ctx.screenDescriptionLineClause(),
                        ctx.screenDescriptionColumnClause(),
                        ctx.screenDescriptionForegroundColorClause(),
                        ctx.screenDescriptionControlClause(),
                        ctx.screenDescriptionValueClause(),
                        ctx.screenDescriptionPictureClause(),
                        ctx.screenDescriptionFromClause(),
                        ctx.screenDescriptionUsingClause(),
                        ctx.screenDescriptionUsageClause(),
                        ctx.screenDescriptionJustifiedClause(),
                        ctx.screenDescriptionSignClause(),
                        ctx.screenDescriptionSecureClause(),
                        ctx.screenDescriptionRequiredClause(),
                        ctx.screenDescriptionPromptClause(),
                        ctx.screenDescriptionFullClause(),
                        ctx.screenDescriptionZeroFillClause())
                        .withLastSpace(sourceBefore("."))
        );
    }

    @Override
    public Object visitScreenDescriptionEraseClause(CobolParser.ScreenDescriptionEraseClauseContext ctx) {
        return new Cobol.ScreenDescriptionEraseClause(
                randomId(),
                prefix(ctx),
                Markers.EMPTY,
                words(ctx.ERASE(), ctx.EOL(), ctx.EOS())
        );
    }

    @Override
    public Object visitScreenDescriptionForegroundColorClause(CobolParser.ScreenDescriptionForegroundColorClauseContext ctx) {
        return new Cobol.ScreenDescriptionForegroundColorClause(
                randomId(),
                prefix(ctx),
                Markers.EMPTY,
                words(ctx.FOREGROUND_COLOR(), ctx.FOREGROUND_COLOUR(), ctx.IS()),
                visit(ctx.identifier(), ctx.integerLiteral())
        );
    }

    @Override
    public Object visitScreenDescriptionFromClause(CobolParser.ScreenDescriptionFromClauseContext ctx) {
        return new Cobol.ScreenDescriptionFromClause(
                randomId(),
                prefix(ctx),
                Markers.EMPTY,
                words(ctx.FROM()),
                visit(ctx.identifier(), ctx.literal()),
                visitNullable(ctx.screenDescriptionToClause())
        );
    }

    @Override
    public Object visitScreenDescriptionFullClause(CobolParser.ScreenDescriptionFullClauseContext ctx) {
        return new Cobol.ScreenDescriptionFullClause(
                randomId(),
                prefix(ctx),
                Markers.EMPTY,
                words(ctx.FULL(), ctx.LENGTH_CHECK())
        );
    }

    @Override
    public Object visitScreenDescriptionGridClause(CobolParser.ScreenDescriptionGridClauseContext ctx) {
        return new Cobol.ScreenDescriptionFullClause(
                randomId(),
                prefix(ctx),
                Markers.EMPTY,
                words(ctx.GRID(), ctx.LEFTLINE(), ctx.OVERLINE())
        );
    }

    @Override
    public Object visitScreenDescriptionJustifiedClause(CobolParser.ScreenDescriptionJustifiedClauseContext ctx) {
        return new Cobol.ScreenDescriptionJustifiedClause(
                randomId(),
                prefix(ctx),
                Markers.EMPTY,
                words(ctx.JUSTIFIED(), ctx.JUST(), ctx.RIGHT())
        );
    }

    @Override
    public Object visitScreenDescriptionLightClause(CobolParser.ScreenDescriptionLightClauseContext ctx) {
        return new Cobol.ScreenDescriptionLightClause(
                randomId(),
                prefix(ctx),
                Markers.EMPTY,
                words(ctx.HIGHLIGHT(), ctx.LOWLIGHT())
        );
    }

    @Override
    public Object visitScreenDescriptionLineClause(CobolParser.ScreenDescriptionLineClauseContext ctx) {
        return new Cobol.ScreenDescriptionLineClause(
                randomId(),
                prefix(ctx),
                Markers.EMPTY,
                words(ctx.LINE(), ctx.NUMBER(), ctx.IS(), ctx.PLUS(), ctx.PLUSCHAR(), ctx.MINUSCHAR()),
                visit(ctx.identifier(), ctx.integerLiteral())
        );
    }

    @Override
    public Object visitScreenDescriptionPictureClause(CobolParser.ScreenDescriptionPictureClauseContext ctx) {
        return new Cobol.ScreenDescriptionPictureClause(
                randomId(),
                prefix(ctx),
                Markers.EMPTY,
                words(ctx.PICTURE(), ctx.PIC(), ctx.IS()),
                (Cobol.PictureString) visit(ctx.pictureString())
        );
    }

    @Override
    public Object visitScreenDescriptionPromptClause(CobolParser.ScreenDescriptionPromptClauseContext ctx) {
        return new Cobol.ScreenDescriptionPromptClause(
                randomId(),
                prefix(ctx),
                Markers.EMPTY,
                words(ctx.PROMPT(), ctx.CHARACTER(), ctx.IS()),
                visit(ctx.identifier(), ctx.literal()),
                visitNullable(ctx.screenDescriptionPromptOccursClause())
        );
    }

    @Override
    public Object visitScreenDescriptionPromptOccursClause(CobolParser.ScreenDescriptionPromptOccursClauseContext ctx) {
        return new Cobol.ScreenDescriptionPromptOccursClause(
                randomId(),
                prefix(ctx),
                Markers.EMPTY,
                words(ctx.OCCURS()),
                (Cobol.CobolWord) visit(ctx.integerLiteral()),
                words(ctx.TIMES())
        );
    }

    @Override
    public Object visitScreenDescriptionRequiredClause(CobolParser.ScreenDescriptionRequiredClauseContext ctx) {
        return new Cobol.ScreenDescriptionRequiredClause(
                randomId(),
                prefix(ctx),
                Markers.EMPTY,
                words(ctx.REQUIRED(), ctx.EMPTY_CHECK())
        );
    }

    @Override
    public Object visitScreenDescriptionReverseVideoClause(CobolParser.ScreenDescriptionReverseVideoClauseContext ctx) {
        return new Cobol.ScreenDescriptionReverseVideoClause(
                randomId(),
                prefix(ctx),
                Markers.EMPTY,
                words(ctx.REVERSE_VIDEO())
        );
    }

    @Override
    public Object visitScreenDescriptionSignClause(CobolParser.ScreenDescriptionSignClauseContext ctx) {
        return new Cobol.ScreenDescriptionSignClause(
                randomId(),
                prefix(ctx),
                Markers.EMPTY,
                words(ctx.SIGN(), ctx.IS(), ctx.LEADING(), ctx.TRAILING(), ctx.SEPARATE(), ctx.CHARACTER())
        );
    }

    @Override
    public Object visitScreenDescriptionSizeClause(CobolParser.ScreenDescriptionSizeClauseContext ctx) {
        return new Cobol.ScreenDescriptionSizeClause(
                randomId(),
                prefix(ctx),
                Markers.EMPTY,
                words(ctx.SIZE(), ctx.IS()),
                (Identifier) visit(ctx.identifier())
        );
    }

    @Override
    public Object visitScreenDescriptionToClause(CobolParser.ScreenDescriptionToClauseContext ctx) {
        return new Cobol.ScreenDescriptionToClause(
                randomId(),
                prefix(ctx),
                Markers.EMPTY,
                words(ctx.TO()),
                (Identifier) visit(ctx.identifier())
        );
    }

    @Override
    public Object visitScreenDescriptionUnderlineClause(CobolParser.ScreenDescriptionUnderlineClauseContext ctx) {
        return new Cobol.ScreenDescriptionUnderlineClause(
                randomId(),
                prefix(ctx),
                Markers.EMPTY,
                words(ctx.UNDERLINE())
        );
    }

    @Override
    public Object visitScreenDescriptionUsageClause(CobolParser.ScreenDescriptionUsageClauseContext ctx) {
        return new Cobol.ScreenDescriptionUsageClause(
                randomId(),
                prefix(ctx),
                Markers.EMPTY,
                words(ctx.USAGE(), ctx.IS(), ctx.DISPLAY(), ctx.DISPLAY_1())
        );
    }

    @Override
    public Object visitScreenDescriptionUsingClause(CobolParser.ScreenDescriptionUsingClauseContext ctx) {
        return new Cobol.ScreenDescriptionUsingClause(
                randomId(),
                prefix(ctx),
                Markers.EMPTY,
                words(ctx.USING()),
                (Identifier) visit(ctx.identifier())
        );
    }

    @Override
    public Object visitScreenDescriptionValueClause(CobolParser.ScreenDescriptionValueClauseContext ctx) {
        return new Cobol.ScreenDescriptionValueClause(
                randomId(),
                prefix(ctx),
                Markers.EMPTY,
                words(ctx.VALUE(), ctx.IS()),
                (Name) visit(ctx.literal())
        );
    }

    @Override
    public Object visitScreenDescriptionZeroFillClause(CobolParser.ScreenDescriptionZeroFillClauseContext ctx) {
        return new Cobol.ScreenDescriptionZeroFillClause(
                randomId(),
                prefix(ctx),
                Markers.EMPTY,
                words(ctx.ZERO_FILL())
        );
    }

    @Override
    public Cobol.Search visitSearchStatement(CobolParser.SearchStatementContext ctx) {
        return new Cobol.Search(
                randomId(),
                prefix(ctx),
                Markers.EMPTY,
                words(ctx.SEARCH(), ctx.ALL()),
                (Cobol.QualifiedDataName) visit(ctx.qualifiedDataName()),
                visitNullable(ctx.searchVarying()),
                visitNullable(ctx.atEndPhrase()),
                convertAllContainer(ctx.searchWhen()),
                words(ctx.END_SEARCH())
        );
    }

    @Override
    public Cobol.SearchVarying visitSearchVarying(CobolParser.SearchVaryingContext ctx) {
        return new Cobol.SearchVarying(
                randomId(),
                prefix(ctx),
                Markers.EMPTY,
                words(ctx.VARYING()),
                (Cobol.QualifiedDataName) visit(ctx.qualifiedDataName())
        );
    }

    @Override
    public Cobol.SearchWhen visitSearchWhen(CobolParser.SearchWhenContext ctx) {
        return new Cobol.SearchWhen(
                randomId(),
                prefix(ctx),
                Markers.EMPTY,
                words(ctx.WHEN()),
                (Cobol.Condition) visit(ctx.condition()),
                words(ctx.NEXT(), ctx.SENTENCE()),
                convertAllContainer(ctx.statement())
        );
    }

    @Override
    public Cobol.Send visitSendStatement(CobolParser.SendStatementContext ctx) {
        return new Cobol.Send(
                randomId(),
                prefix(ctx),
                Markers.EMPTY,
                words(ctx.SEND()),
                visit(ctx.sendStatementSync(), ctx.sendStatementAsync()),
                visitNullable(ctx.onExceptionClause()),
                visitNullable(ctx.notOnExceptionClause())
        );
    }

    @Override
    public Cobol.SendPhrase visitSendStatementAsync(CobolParser.SendStatementAsyncContext ctx) {
        return new Cobol.SendPhrase(
                randomId(),
                prefix(ctx),
                Markers.EMPTY,
                words(ctx.TO(), ctx.TOP(), ctx.BOTTOM()),
                (Cobol) visit(ctx.identifier())
        );
    }

    @Override
    public Cobol.SendStatementSync visitSendStatementSync(CobolParser.SendStatementSyncContext ctx) {
        return new Cobol.SendStatementSync(
                randomId(),
                prefix(ctx),
                Markers.EMPTY,
                visit(ctx.identifier(), ctx.literal()),
                visitNullable(ctx.sendFromPhrase()),
                visitNullable(ctx.sendWithPhrase()),
                visitNullable(ctx.sendReplacingPhrase()),
                visitNullable(ctx.sendAdvancingPhrase())
        );
    }

    @Override
    public Cobol.SendPhrase visitSendFromPhrase(CobolParser.SendFromPhraseContext ctx) {
        return new Cobol.SendPhrase(
                randomId(),
                prefix(ctx),
                Markers.EMPTY,
                words(ctx.FROM()),
                (Cobol) visit(ctx.identifier())
        );
    }

    @Override
    public Cobol.SendPhrase visitSendWithPhrase(CobolParser.SendWithPhraseContext ctx) {
        return new Cobol.SendPhrase(
                randomId(),
                prefix(ctx),
                Markers.EMPTY,
                words(ctx.WITH(), ctx.EGI(), ctx.EMI(), ctx.ESI()),
                visitNullable(ctx.identifier())
        );
    }

    @Override
    public Cobol.SendPhrase visitSendReplacingPhrase(CobolParser.SendReplacingPhraseContext ctx) {
        return new Cobol.SendPhrase(
                randomId(),
                prefix(ctx),
                Markers.EMPTY,
                words(ctx.REPLACING(), ctx.LINE()),
                null
        );
    }

    @Override
    public Cobol.SendPhrase visitSendAdvancingPhrase(CobolParser.SendAdvancingPhraseContext ctx) {
        return new Cobol.SendPhrase(
                randomId(),
                prefix(ctx),
                Markers.EMPTY,
                words(ctx.BEFORE(), ctx.AFTER(), ctx.ADVANCING()),
                visit(ctx.sendAdvancingPage(), ctx.sendAdvancingLines(), ctx.sendAdvancingMnemonic())
        );
    }

    @Override
    public Object visitSelectClause(CobolParser.SelectClauseContext ctx) {
        return new Cobol.SelectClause(
                randomId(),
                prefix(ctx),
                Markers.EMPTY,
                words(ctx.SELECT(), ctx.OPTIONAL()),
                (Cobol.CobolWord) visit(ctx.fileName())
        );
    }

    @Override
    public Cobol.SendAdvancingLines visitSendAdvancingLines(CobolParser.SendAdvancingLinesContext ctx) {
        return new Cobol.SendAdvancingLines(
                randomId(),
                prefix(ctx),
                Markers.EMPTY,
                visit(ctx.identifier(), ctx.literal()),
                words(ctx.LINE(), ctx.LINES())
        );
    }

    @Override
    public Object visitScreenSection(CobolParser.ScreenSectionContext ctx) {
        return new Cobol.ScreenSection(
                randomId(),
                prefix(ctx),
                Markers.EMPTY,
                words(ctx.SCREEN(), ctx.SECTION()),
                convertAllContainer(sourceBefore("."), ctx.screenDescriptionEntry())
        );
    }

    @Override
    public Cobol.ValuedObjectComputerClause visitSegmentLimitClause(CobolParser.SegmentLimitClauseContext ctx) {
        return new Cobol.ValuedObjectComputerClause(
                randomId(),
                prefix(ctx),
                Markers.EMPTY,
                Cobol.ValuedObjectComputerClause.Type.SegmentLimit,
                words(ctx.SEGMENT_LIMIT(), ctx.IS()),
                (Cobol) visit(ctx.integerLiteral()),
                null
        );
    }

    @Override
    public Object visitSimpleCondition(CobolParser.SimpleConditionContext ctx) {
        return ctx.condition() != null ? new Cobol.Parenthesized(
                randomId(),
                prefix(ctx),
                Markers.EMPTY,
                words(ctx.LPARENCHAR()),
                singletonList((Cobol) visit(ctx.condition())),
                words(ctx.RPARENCHAR())
        ) : visit(ctx.relationCondition(), ctx.classCondition(), ctx.conditionNameReference());
    }

    @Override
    public Object visitSymbolicSourceClause(CobolParser.SymbolicSourceClauseContext ctx) {
        throw new UnsupportedOperationException("Implement me");
    }

    @Override
    public Object visitSymbolicSubQueueClause(CobolParser.SymbolicSubQueueClauseContext ctx) {
        throw new UnsupportedOperationException("Implement me");
    }

    @Override
    public Object visitSymbolicTerminalClause(CobolParser.SymbolicTerminalClauseContext ctx) {
        throw new UnsupportedOperationException("Implement me");
    }

    @Override
    public Object visitSymbolicQueueClause(CobolParser.SymbolicQueueClauseContext ctx) {
        throw new UnsupportedOperationException("Implement me");
    }

    @Override
    public Cobol.ValuedObjectComputerClause visitCharacterSetClause(CobolParser.CharacterSetClauseContext ctx) {
        return new Cobol.ValuedObjectComputerClause(
                randomId(),
                prefix(ctx),
                Markers.EMPTY,
                Cobol.ValuedObjectComputerClause.Type.CharacterSet,
                words(ctx.CHARACTER(), ctx.SET(), ctx.DOT_FS()),
                null,
                null
        );
    }

    @Override
    public Cobol.SourceComputer visitSourceComputerParagraph(CobolParser.SourceComputerParagraphContext ctx) {
        return new Cobol.SourceComputer(
                randomId(),
                prefix(ctx),
                Markers.EMPTY,
                padRight(words(ctx.SOURCE_COMPUTER()), sourceBefore(".")),
                ctx.computerName() == null ? null : padRight(new Cobol.SourceComputerDefinition(
                        randomId(),
                        prefix(ctx.computerName()),
                        Markers.EMPTY,
                        skip(ctx.computerName().getText()),
                        padLeft(whitespace(), words(ctx.WITH(), ctx.DEBUGGING(), ctx.MODE()))
                ), sourceBefore("."))
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
        return new Cobol.AddTo(
                randomId(),
                Space.EMPTY,
                Markers.EMPTY,
                convertAllContainer(ctx.addFrom()),
                convertAllContainer(padLeft(ctx.TO()), ctx.addTo()),
                null
        );
    }

    @Override
    public Cobol.AddTo visitAddToGivingStatement(CobolParser.AddToGivingStatementContext ctx) {
        return new Cobol.AddTo(
                randomId(),
                Space.EMPTY,
                Markers.EMPTY,
                convertAllContainer(ctx.addFrom()),
                convertAllContainer(padLeft(ctx.TO()), ctx.addToGiving()),
                convertAllContainer(padLeft(ctx.GIVING()), ctx.addGiving())
        );
    }

    @Override
    public Cobol.Roundable visitRoundable(CobolParser.RoundableContext ctx) {
        return new Cobol.Roundable(
                randomId(),
                prefix(ctx),
                Markers.EMPTY,
                (Identifier) visit(ctx.identifier()),
                padLeft(ctx.ROUNDED())
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
        return new Cobol.SetTo(
                randomId(),
                prefix(ctx),
                Markers.EMPTY,
                convertAllContainer(ctx.setTo()),
                convertAllContainer(padLeft(ctx.TO()), ctx.setToValue())
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
    public Object visitTerminateStatement(CobolParser.TerminateStatementContext ctx) {
        return new Cobol.Terminate(
                randomId(),
                prefix(ctx),
                Markers.EMPTY,
                (Cobol.CobolWord) visit(ctx.TERMINATE()),
                (Cobol.QualifiedDataName) visit(ctx.reportName())
        );
    }

    @Override
    public Object visitTextLengthClause(CobolParser.TextLengthClauseContext ctx) {
        throw new UnsupportedOperationException("Implement me");
    }

    @Override
    public Object visitUnstringStatement(CobolParser.UnstringStatementContext ctx) {
        return new Cobol.UnString(
                randomId(),
                prefix(ctx),
                Markers.EMPTY,
                words(ctx.UNSTRING()),
                (Cobol.UnstringSendingPhrase) visit(ctx.unstringSendingPhrase()),
                (Cobol.UnstringIntoPhrase) visit(ctx.unstringIntoPhrase()),
                visitNullable(ctx.unstringWithPointerPhrase()),
                visitNullable(ctx.unstringTallyingPhrase()),
                visitNullable(ctx.onOverflowPhrase()),
                visitNullable(ctx.notOnOverflowPhrase()),
                padLeft(ctx.END_UNSTRING())
        );
    }

    @Override
    public Object visitUnstringSendingPhrase(CobolParser.UnstringSendingPhraseContext ctx) {
        return new Cobol.UnstringSendingPhrase(
                randomId(),
                prefix(ctx),
                Markers.EMPTY,
                (Identifier) visit(ctx.identifier()),
                visitNullable(ctx.unstringDelimitedByPhrase()),
                convertAllContainer(ctx.unstringOrAllPhrase())
        );
    }

    @Override
    public Object visitUnstringDelimitedByPhrase(CobolParser.UnstringDelimitedByPhraseContext ctx) {
        return new Cobol.UnstringDelimitedByPhrase(
                randomId(),
                prefix(ctx),
                Markers.EMPTY,
                words(ctx.DELIMITED(), ctx.BY(), ctx.ALL()),
                visit(ctx.identifier(), ctx.literal())
        );
    }

    @Override
    public Object visitUnstringOrAllPhrase(CobolParser.UnstringOrAllPhraseContext ctx) {
        return new Cobol.UnstringOrAllPhrase(
                randomId(),
                prefix(ctx),
                Markers.EMPTY,
                words(ctx.OR(), ctx.ALL()),
                visit(ctx.identifier(), ctx.literal())
        );
    }

    @Override
    public Object visitUnstringIntoPhrase(CobolParser.UnstringIntoPhraseContext ctx) {
        return new Cobol.UnstringIntoPhrase(
                randomId(),
                prefix(ctx),
                Markers.EMPTY,
                words(ctx.INTO()),
                convertAllContainer(ctx.unstringInto())
        );
    }

    @Override
    public Object visitUnstringInto(CobolParser.UnstringIntoContext ctx) {
        return new Cobol.UnstringInto(
                randomId(),
                prefix(ctx),
                Markers.EMPTY,
                (Identifier) visit(ctx.identifier()),
                visitNullable(ctx.unstringDelimiterIn()),
                visitNullable(ctx.unstringCountIn())
        );
    }

    @Override
    public Object visitUnstringDelimiterIn(CobolParser.UnstringDelimiterInContext ctx) {
        return new Cobol.UnstringDelimiterIn(
                randomId(),
                prefix(ctx),
                Markers.EMPTY,
                words(ctx.DELIMITER(), ctx.IN()),
                (Identifier) visit(ctx.identifier())
        );
    }

    @Override
    public Object visitUnstringCountIn(CobolParser.UnstringCountInContext ctx) {
        return new Cobol.UnstringCountIn(
                randomId(),
                prefix(ctx),
                Markers.EMPTY,
                words(ctx.COUNT(), ctx.IN()),
                (Identifier) visit(ctx.identifier())
        );
    }

    @Override
    public Object visitUnstringWithPointerPhrase(CobolParser.UnstringWithPointerPhraseContext ctx) {
        return new Cobol.UnstringWithPointerPhrase(
                randomId(),
                prefix(ctx),
                Markers.EMPTY,
                words(ctx.WITH(), ctx.POINTER()),
                (Cobol.QualifiedDataName) visit(ctx.qualifiedDataName())
        );
    }

    @Override
    public Object visitUnstringTallyingPhrase(CobolParser.UnstringTallyingPhraseContext ctx) {
        return new Cobol.UnstringTallyingPhrase(
                randomId(),
                prefix(ctx),
                Markers.EMPTY,
                words(ctx.TALLYING(), ctx.IN()),
                (Cobol.QualifiedDataName) visit(ctx.qualifiedDataName())
        );
    }

    @Override
    public Object visitUseStatement(CobolParser.UseStatementContext ctx) {
        return new Cobol.UseStatement(
                randomId(),
                prefix(ctx),
                Markers.EMPTY,
                words(ctx.USE()),
                visit(ctx.useAfterClause(), ctx.useDebugClause())
        );
    }

    @Override
    public Object visitUseAfterClause(CobolParser.UseAfterClauseContext ctx) {
        return new Cobol.UseAfterClause(
                randomId(),
                prefix(ctx),
                Markers.EMPTY,
                words(ctx.GLOBAL(), ctx.AFTER(), ctx.STANDARD(), ctx.EXCEPTION(), ctx.ERROR(), ctx.PROCEDURE(), ctx.ON()),
                (Cobol.UseAfterOn) visit(ctx.useAfterOn())
        );
    }

    @Override
    public Object visitUseAfterOn(CobolParser.UseAfterOnContext ctx) {
        return new Cobol.UseAfterOn(
                randomId(),
                prefix(ctx),
                Markers.EMPTY,
                words(ctx.INPUT(), ctx.OUTPUT(), ctx.I_O(), ctx.EXTEND()),
                convertAllContainer(ctx.fileName())
        );
    }

    @Override
    public Object visitUseDebugClause(CobolParser.UseDebugClauseContext ctx) {
        return new Cobol.UseDebugClause(
                randomId(),
                prefix(ctx),
                Markers.EMPTY,
                words(ctx.FOR(), ctx.DEBUGGING(), ctx.ON()),
                convertAllContainer(ctx.useDebugOn())
        );
    }

    @Override
    public Object visitUseDebugOn(CobolParser.UseDebugOnContext ctx) {
        return new Cobol.UseDebugClause(
                randomId(),
                prefix(ctx),
                Markers.EMPTY,
                words(ctx.ALL(), ctx.PROCEDURES(), ctx.REFERENCES(), ctx.OF()),
                ctx.PROCEDURES() != null ? null : visit(ctx.identifier(), ctx.procedureName(), ctx.fileName())
        );
    }

    @Override
    public Cobol.DataPictureClause visitDataPictureClause(CobolParser.DataPictureClauseContext ctx) {
        return new Cobol.DataPictureClause(
                randomId(),
                prefix(ctx),
                Markers.EMPTY,
                words(ctx.PICTURE(), ctx.PIC(), ctx.IS()),
                convertAllContainer(ctx.pictureString().picture())
        );
    }

    @Override
    public Object visitDataBaseSection(CobolParser.DataBaseSectionContext ctx) {
        return new Cobol.DataBaseSection(
                randomId(),
                prefix(ctx),
                Markers.EMPTY,
                words(ctx.DATA_BASE(), ctx.SECTION()),
                convertAllContainer(sourceBefore("."), ctx.dataBaseSectionEntry())
        );
    }

    @Override
    public Object visitDataBaseSectionEntry(CobolParser.DataBaseSectionEntryContext ctx) {
        return new Cobol.DataBaseSectionEntry(
                randomId(),
                sourceBefore(ctx.integerLiteral().getText()),
                Markers.EMPTY,
                ctx.integerLiteral().getText(),
                (Literal) visit(ctx.literal(0)),
                words(ctx.INVOKE()),
                (Literal) visit(ctx.literal(1))
        );
    }

    @Override
    public Cobol.DataDivision visitDataDivision(CobolParser.DataDivisionContext ctx) {
        return new Cobol.DataDivision(
                randomId(),
                prefix(ctx),
                Markers.EMPTY,
                words(ctx.DATA(), ctx.DIVISION()),
                convertAllContainer(sourceBefore("."), ctx.dataDivisionSection())
        );
    }

    @Override
    public Cobol.WorkingStorageSection visitWorkingStorageSection(CobolParser.WorkingStorageSectionContext ctx) {
        return new Cobol.WorkingStorageSection(
                randomId(),
                sourceBefore(ctx.WORKING_STORAGE().getText()),
                Markers.EMPTY,
                words(ctx.WORKING_STORAGE(), ctx.SECTION()),
                convertAllContainer(sourceBefore("."), ctx.dataDescriptionEntry())
        );
    }

    @Override
    public Object visitWriteStatement(CobolParser.WriteStatementContext ctx) {
        return new Cobol.Write(
                randomId(),
                prefix(ctx),
                Markers.EMPTY,
                words(ctx.WRITE()),
                (Cobol.QualifiedDataName) visit(ctx.recordName()),
                visitNullable(ctx.writeFromPhrase()),
                visitNullable(ctx.writeAdvancingPhrase()),
                visitNullable(ctx.writeAtEndOfPagePhrase()),
                visitNullable(ctx.writeNotAtEndOfPagePhrase()),
                visitNullable(ctx.invalidKeyPhrase()),
                visitNullable(ctx.notInvalidKeyPhrase()),
                padLeft(ctx.END_WRITE())
        );
    }

    @Override
    public Object visitWriteFromPhrase(CobolParser.WriteFromPhraseContext ctx) {
        return new Cobol.WriteFromPhrase(
                randomId(),
                prefix(ctx),
                Markers.EMPTY,
                words(ctx.FROM()),
                visit(ctx.identifier(), ctx.literal())
        );
    }

    @Override
    public Object visitWriteAdvancingPhrase(CobolParser.WriteAdvancingPhraseContext ctx) {
        return new Cobol.WriteAdvancingPhrase(
                randomId(),
                prefix(ctx),
                Markers.EMPTY,
                words(ctx.BEFORE(), ctx.AFTER(), ctx.ADVANCING()),
                visit(ctx.writeAdvancingPage(), ctx.writeAdvancingLines(), ctx.writeAdvancingMnemonic())
        );
    }

    @Override
    public Object visitWriteAdvancingPage(CobolParser.WriteAdvancingPageContext ctx) {
        return new Cobol.WriteAdvancingPage(
                randomId(),
                prefix(ctx),
                Markers.EMPTY,
                words(ctx.PAGE())
        );
    }

    @Override
    public Object visitWriteAdvancingLines(CobolParser.WriteAdvancingLinesContext ctx) {
        return new Cobol.WriteAdvancingLines(
                randomId(),
                prefix(ctx),
                Markers.EMPTY,
                visit(ctx.identifier(), ctx.literal()),
                words(ctx.LINE(), ctx.LINES())
        );
    }

    @Override
    public Object visitWriteAdvancingMnemonic(CobolParser.WriteAdvancingMnemonicContext ctx) {
        return new Cobol.WriteAdvancingMnemonic(
                randomId(),
                prefix(ctx),
                Markers.EMPTY,
                (Name) visit(ctx.mnemonicName())
        );
    }

    @Override
    public Object visitWriteAtEndOfPagePhrase(CobolParser.WriteAtEndOfPagePhraseContext ctx) {
        return new Cobol.StatementPhrase(
                randomId(),
                prefix(ctx),
                Markers.EMPTY,
                words(ctx.AT(), ctx.END_OF_PAGE(), ctx.EOP()),
                convertAllContainer(ctx.statement())
        );
    }

    @Override
    public Object visitWriteNotAtEndOfPagePhrase(CobolParser.WriteNotAtEndOfPagePhraseContext ctx) {
        return new Cobol.StatementPhrase(
                randomId(),
                prefix(ctx),
                Markers.EMPTY,
                words(ctx.NOT(), ctx.AT(), ctx.END_OF_PAGE(), ctx.EOP()),
                convertAllContainer(ctx.statement())
        );
    }

    @Override
    public Cobol.LinkageSection visitLinkageSection(CobolParser.LinkageSectionContext ctx) {
        return new Cobol.LinkageSection(
                randomId(),
                sourceBefore(ctx.LINKAGE().getText()),
                Markers.EMPTY,
                words(ctx.LINKAGE(), ctx.SECTION()),
                convertAllContainer(sourceBefore("."), ctx.dataDescriptionEntry())
        );
    }

    @Override
    public Cobol.LocalStorageSection visitLocalStorageSection(CobolParser.LocalStorageSectionContext ctx) {
        return new Cobol.LocalStorageSection(
                randomId(),
                prefix(ctx),
                Markers.EMPTY,
                words(ctx.LOCAL_STORAGE(), ctx.SECTION()),
                ctx.LD() == null ? null : ctx.LD().getText(),
                ctx.localName() == null ? null : (Name) visit(ctx.localName()),
                convertAllContainer(sourceBefore("."), ctx.dataDescriptionEntry())
        );
    }

    @Override
    public Cobol.Merge visitMergeStatement(CobolParser.MergeStatementContext ctx) {
        return new Cobol.Merge(
                randomId(),
                prefix(ctx),
                Markers.EMPTY,
                words(ctx.MERGE()),
                (Name) visit(ctx.fileName()),
                convertAllContainer(ctx.mergeOnKeyClause()),
                visitNullable(ctx.mergeCollatingSequencePhrase()),
                convertAllContainer(ctx.mergeUsing()),
                visitNullable(ctx.mergeOutputProcedurePhrase()),
                convertAllContainer(ctx.mergeGivingPhrase())
        );
    }

    @Override
    public Cobol.MergeOnKeyClause visitMergeOnKeyClause(CobolParser.MergeOnKeyClauseContext ctx) {
        return new Cobol.MergeOnKeyClause(
                randomId(),
                prefix(ctx),
                Markers.EMPTY,
                words(ctx.ON(), ctx.ASCENDING(), ctx.DESCENDING(), ctx.KEY()),
                convertAllContainer(ctx.qualifiedDataName())
        );
    }

    @Override
    public Cobol.MergeCollatingSequencePhrase visitMergeCollatingSequencePhrase(CobolParser.MergeCollatingSequencePhraseContext ctx) {
        return new Cobol.MergeCollatingSequencePhrase(
                randomId(),
                prefix(ctx),
                Markers.EMPTY,
                words(ctx.COLLATING(), ctx.SEQUENCE(), ctx.IS()),
                convertAllContainer(ctx.alphabetName()),
                visitNullable(ctx.mergeCollatingAlphanumeric()),
                visitNullable(ctx.mergeCollatingNational())
        );
    }

    @Override
    public Cobol.Mergeable visitMergeCollatingAlphanumeric(CobolParser.MergeCollatingAlphanumericContext ctx) {
        return new Cobol.Mergeable(
                randomId(),
                prefix(ctx),
                Markers.EMPTY,
                words(ctx.FOR(), ctx.ALPHANUMERIC(), ctx.IS()),
                (Name) visit(ctx.alphabetName())
        );
    }

    @Override
    public Cobol.Mergeable visitMergeCollatingNational(CobolParser.MergeCollatingNationalContext ctx) {
        return new Cobol.Mergeable(
                randomId(),
                prefix(ctx),
                Markers.EMPTY,
                words(ctx.FOR(), ctx.NATIONAL(), ctx.IS()),
                (Name) visit(ctx.alphabetName())
        );
    }

    @Override
    public Cobol.MergeUsing visitMergeUsing(CobolParser.MergeUsingContext ctx) {
        return new Cobol.MergeUsing(
                randomId(),
                prefix(ctx),
                Markers.EMPTY,
                words(ctx.USING()),
                convertAllContainer(ctx.fileName())
        );
    }

    @Override
    public Cobol.MergeOutputProcedurePhrase visitMergeOutputProcedurePhrase(CobolParser.MergeOutputProcedurePhraseContext ctx) {
        return new Cobol.MergeOutputProcedurePhrase(
                randomId(),
                prefix(ctx),
                Markers.EMPTY,
                words(ctx.OUTPUT(), ctx.PROCEDURE(), ctx.IS()),
                visitProcedureName(ctx.procedureName()),
                visitNullable(ctx.mergeOutputThrough())
        );
    }

    @Override
    public Cobol.MergeOutputThrough visitMergeOutputThrough(CobolParser.MergeOutputThroughContext ctx) {
        return new Cobol.MergeOutputThrough(
                randomId(),
                prefix(ctx),
                Markers.EMPTY,
                words(ctx.THROUGH(), ctx.THRU()),
                visitProcedureName(ctx.procedureName())
        );
    }

    @Override
    public Cobol.MergeGivingPhrase visitMergeGivingPhrase(CobolParser.MergeGivingPhraseContext ctx) {
        return new Cobol.MergeGivingPhrase(
                randomId(),
                prefix(ctx),
                Markers.EMPTY,
                words(ctx.GIVING()),
                convertAllContainer(ctx.mergeGiving())
        );
    }

    @Override
    public Cobol.MergeGiving visitMergeGiving(CobolParser.MergeGivingContext ctx) {
        return new Cobol.MergeGiving(
                randomId(),
                prefix(ctx),
                Markers.EMPTY,
                (Name) visit(ctx.fileName()),
                words(ctx.LOCK(), ctx.SAVE(), ctx.NO(), ctx.REWIND(), ctx.CRUNCH(), ctx.RELEASE(), ctx.WITH(), ctx.REMOVE(), ctx.CRUNCH())
        );
    }

    @Override
    public Cobol.MoveStatement visitMoveStatement(CobolParser.MoveStatementContext ctx) {
        return new Cobol.MoveStatement(
                randomId(),
                prefix(ctx),
                Markers.EMPTY,
                words(ctx.MOVE(), ctx.ALL()),
                visit(ctx.moveCorrespondingToStatement(), ctx.moveToStatement())
        );
    }

    @Override
    public Cobol.MoveToStatement visitMoveToStatement(CobolParser.MoveToStatementContext ctx) {
        return new Cobol.MoveToStatement(
                randomId(),
                prefix(ctx),
                Markers.EMPTY,
                (Name) visit(ctx.moveToSendingArea()),
                convertAllContainer(padLeft(ctx.TO()), ctx.identifier())
        );
    }

    @Override
    public Cobol.MoveCorrespondingToStatement visitMoveCorrespondingToStatement(CobolParser.MoveCorrespondingToStatementContext ctx) {
        return new Cobol.MoveCorrespondingToStatement(
                randomId(),
                prefix(ctx),
                Markers.EMPTY,
                words(ctx.CORRESPONDING(), ctx.CORR()),
                (Identifier) visit(ctx.moveCorrespondingToSendingArea()),
                convertAllContainer(padLeft(ctx.TO()), ctx.identifier())
        );
    }

    @Override
    public Cobol.Multiply visitMultiplyStatement(CobolParser.MultiplyStatementContext ctx) {
        return new Cobol.Multiply(
                randomId(),
                prefix(ctx),
                Markers.EMPTY,
                words(ctx.MULTIPLY()),
                visit(ctx.identifier(), ctx.literal()),
                words(ctx.BY()),
                visit(ctx.multiplyRegular(), ctx.multiplyGiving()),
                visitNullable(ctx.onSizeErrorPhrase()),
                visitNullable(ctx.notOnSizeErrorPhrase()),
                padLeft(ctx.END_MULTIPLY())
        );
    }

    @Override
    public Cobol.MultiplyRegular visitMultiplyRegular(CobolParser.MultiplyRegularContext ctx) {
        return new Cobol.MultiplyRegular(
                randomId(),
                prefix(ctx),
                Markers.EMPTY,
                convertAllContainer(ctx.multiplyRegularOperand())
        );
    }

    @Override
    public Cobol.MultiplyGiving visitMultiplyGiving(CobolParser.MultiplyGivingContext ctx) {
        return new Cobol.MultiplyGiving(
                randomId(),
                prefix(ctx),
                Markers.EMPTY,
                (Name) visit(ctx.multiplyGivingOperand()),
                convertAllContainer(padLeft(ctx.GIVING()), ctx.multiplyGivingResult())
        );
    }

    @Override
    public Cobol.NextSentence visitNextSentenceStatement(CobolParser.NextSentenceStatementContext ctx) {
        return new Cobol.NextSentence(
                randomId(),
                prefix(ctx),
                Markers.EMPTY,
                words(ctx.NEXT(), ctx.SENTENCE())
            );
    }

    @Override
    public Cobol.Open visitOpenStatement(CobolParser.OpenStatementContext ctx) {
        return new Cobol.Open(
                randomId(),
                prefix(ctx),
                Markers.EMPTY,
                words(ctx.OPEN()),
                convertAllContainer(ctx.openInputStatement(), ctx.openOutputStatement(), ctx.openIOStatement(),
                        ctx.openExtendStatement())
        );
    }

    @Override
    public Cobol.OpenInputOutputStatement visitOpenInputStatement(CobolParser.OpenInputStatementContext ctx) {
        return new Cobol.OpenInputOutputStatement(
                randomId(),
                prefix(ctx),
                Markers.EMPTY,
                words(ctx.INPUT()),
                convertAllContainer(ctx.openInput())
        );
    }

    @Override
    public Cobol.Openable visitOpenInput(CobolParser.OpenInputContext ctx) {
        return new Cobol.Openable(
                randomId(),
                prefix(ctx),
                Markers.EMPTY,
                (Name) visit(ctx.fileName()),
                words(ctx.REVERSED(), ctx.WITH(), ctx.NO(), ctx.REWIND())
        );
    }

    @Override
    public Cobol.OpenInputOutputStatement visitOpenOutputStatement(CobolParser.OpenOutputStatementContext ctx) {
        return new Cobol.OpenInputOutputStatement(
                randomId(),
                prefix(ctx),
                Markers.EMPTY,
                words(ctx.OUTPUT()),
                convertAllContainer(ctx.openOutput())
        );
    }

    @Override
    public Cobol.Openable visitOpenOutput(CobolParser.OpenOutputContext ctx) {
        return new Cobol.Openable(
                randomId(),
                prefix(ctx),
                Markers.EMPTY,
                (Name) visit(ctx.fileName()),
                words(ctx.WITH(), ctx.NO(), ctx.REWIND())
        );
    }

    @Override
    public Cobol.OpenIOExtendStatement visitOpenIOStatement(CobolParser.OpenIOStatementContext ctx) {
        return new Cobol.OpenIOExtendStatement(
                randomId(),
                prefix(ctx),
                Markers.EMPTY,
                words(ctx.I_O()),
                convertAllContainer(ctx.fileName())
        );
    }

    @Override
    public Cobol.OpenIOExtendStatement visitOpenExtendStatement(CobolParser.OpenExtendStatementContext ctx) {
        return new Cobol.OpenIOExtendStatement(
                randomId(),
                prefix(ctx),
                Markers.EMPTY,
                words(ctx.EXTEND()),
                convertAllContainer(ctx.fileName())
        );
    }

    @Override
    public Cobol.Perform visitPerformStatement(CobolParser.PerformStatementContext ctx) {
        return new Cobol.Perform(
                randomId(),
                prefix(ctx),
                Markers.EMPTY,
                words(ctx.PERFORM()),
                visit(ctx.performInlineStatement(), ctx.performProcedureStatement())
        );
    }

    @Override
    public Cobol.PerformInlineStatement visitPerformInlineStatement(CobolParser.PerformInlineStatementContext ctx) {
        return new Cobol.PerformInlineStatement(
                randomId(),
                prefix(ctx),
                Markers.EMPTY,
                (Cobol) visit(ctx.performType()),
                convertAllContainer(ctx.statement()),
                words(ctx.END_PERFORM())
        );
    }

    @Override
    public Cobol.PerformProcedureStatement visitPerformProcedureStatement(CobolParser.PerformProcedureStatementContext ctx) {
        return new Cobol.PerformProcedureStatement(
                randomId(),
                prefix(ctx),
                Markers.EMPTY,
                (Cobol.ProcedureName) visit(ctx.procedureName(0)),
                words(ctx.THROUGH(), ctx.THRU()),
                (ctx.procedureName().size() > 1) ? (Cobol.ProcedureName) visit(ctx.procedureName(1)) : null,
                visitNullable(ctx.performType())
        );
    }

    @Override
    public Cobol.PerformTimes visitPerformTimes(CobolParser.PerformTimesContext ctx) {
        return new Cobol.PerformTimes(
                randomId(),
                prefix(ctx),
                Markers.EMPTY,
                visit(ctx.identifier(), ctx.integerLiteral()),
                words(ctx.TIMES())
        );
    }

    @Override
    public Cobol.PerformUntil visitPerformUntil(CobolParser.PerformUntilContext ctx) {
        return new Cobol.PerformUntil(
                randomId(),
                prefix(ctx),
                Markers.EMPTY,
                visitNullable(ctx.performTestClause()),
                words(ctx.UNTIL()),
                (Cobol.Condition) visit(ctx.condition())
        );
    }

    @Override
    public Cobol.PerformVarying visitPerformVarying(CobolParser.PerformVaryingContext ctx) {
        if(ctx.performVaryingClause().getRuleIndex() == ctx.getRuleIndex()) {
            return new Cobol.PerformVarying(
                    randomId(),
                    prefix(ctx),
                    Markers.EMPTY,
                    (Cobol) visit(ctx.performVaryingClause()),
                    visitNullable(ctx.performTestClause())
            );
        }
        return new Cobol.PerformVarying(
                randomId(),
                prefix(ctx),
                Markers.EMPTY,
                (Cobol) visit(ctx.performTestClause()),
                (Cobol) visit(ctx.performVaryingClause())
        );
    }

    @Override
    public Cobol.PerformVaryingClause visitPerformVaryingClause(CobolParser.PerformVaryingClauseContext ctx) {
        return new Cobol.PerformVaryingClause(
                randomId(),
                prefix(ctx),
                Markers.EMPTY,
                words(ctx.VARYING()),
                (Cobol.PerformVaryingPhrase) visit(ctx.performVaryingPhrase()),
                convertAllContainer(ctx.performAfter())
        );
    }

    @Override
    public Cobol.PerformVaryingPhrase visitPerformVaryingPhrase(CobolParser.PerformVaryingPhraseContext ctx) {
        return new Cobol.PerformVaryingPhrase(
                randomId(),
                prefix(ctx),
                Markers.EMPTY,
                visit(ctx.identifier(), ctx.literal()),
                (Cobol.PerformFrom) visit(ctx.performFrom()),
                (Cobol.Performable) visit(ctx.performBy()),
                (Cobol.PerformUntil) visit(ctx.performUntil())
        );
    }

    @Override
    public Cobol.Performable visitPerformAfter(CobolParser.PerformAfterContext ctx) {
        return new Cobol.Performable(
                randomId(),
                prefix(ctx),
                Markers.EMPTY,
                words(ctx.AFTER()),
                (Cobol) visit(ctx.performVaryingPhrase())
        );
    }

    @Override
    public Cobol.Performable visitPerformFrom(CobolParser.PerformFromContext ctx) {
        return new Cobol.Performable(
                randomId(),
                prefix(ctx),
                Markers.EMPTY,
                words(ctx.FROM()),
                visit(ctx.identifier(), ctx.literal(), ctx.arithmeticExpression())
        );
    }

    @Override
    public Cobol.Performable visitPerformBy(CobolParser.PerformByContext ctx) {
        return new Cobol.Performable(
                randomId(),
                prefix(ctx),
                Markers.EMPTY,
                words(ctx.BY()),
                visit(ctx.identifier(), ctx.literal(), ctx.arithmeticExpression())
        );
    }

    @Override
    public Cobol.PerformTestClause visitPerformTestClause(CobolParser.PerformTestClauseContext ctx) {
        return new Cobol.PerformTestClause(
                randomId(),
                prefix(ctx),
                Markers.EMPTY,
                words(ctx.WITH(), ctx.TEST(), ctx.BEFORE(), ctx.AFTER())
        );
    }

    @Override
    public Cobol.Purge visitPurgeStatement(CobolParser.PurgeStatementContext ctx) {
        return new Cobol.Purge(
                randomId(),
                prefix(ctx),
                Markers.EMPTY,
                convertAllContainer(padLeft(ctx.PURGE()), ctx.cdName())
        );
    }

    @Override
    public Cobol.Read visitReadStatement(CobolParser.ReadStatementContext ctx) {
        return new Cobol.Read(
                randomId(),
                prefix(ctx),
                Markers.EMPTY,
                words(ctx.READ()),
                (Name) visit(ctx.fileName()),
                words(ctx.NEXT(), ctx.RECORD()),
                visitNullable(ctx.readInto()),
                visitNullable(ctx.readWith()),
                visitNullable(ctx.readKey()),
                visitNullable(ctx.invalidKeyPhrase()),
                visitNullable(ctx.notInvalidKeyPhrase()),
                visitNullable(ctx.atEndPhrase()),
                visitNullable(ctx.notAtEndPhrase()),
                words(ctx.END_READ())
        );
    }

    @Override
    public Cobol.ReadInto visitReadInto(CobolParser.ReadIntoContext ctx) {
        return new Cobol.ReadInto(
                randomId(),
                prefix(ctx),
                Markers.EMPTY,
                words(ctx.INTO()),
                (Identifier) visit(ctx.identifier())
        );
    }

    @Override
    public Cobol.ReadWith visitReadWith(CobolParser.ReadWithContext ctx) {
        return new Cobol.ReadWith(
                randomId(),
                prefix(ctx),
                Markers.EMPTY,
                words(ctx.WITH(), ctx.KEPT(), ctx.NO(), ctx.LOCK(), ctx.WAIT())
        );
    }

    @Override
    public Cobol.ReadKey visitReadKey(CobolParser.ReadKeyContext ctx) {
        return new Cobol.ReadKey(
                randomId(),
                prefix(ctx),
                Markers.EMPTY,
                words(ctx.KEY(), ctx.IS()),
                (Cobol.QualifiedDataName) visit(ctx.qualifiedDataName())
        );
    }

    @Override
    public Cobol.DataDescriptionEntry visitDataDescriptionEntryFormat1(CobolParser.DataDescriptionEntryFormat1Context ctx) {
        TerminalNode level = ctx.INTEGERLITERAL() == null ? ctx.LEVEL_NUMBER_77() : ctx.INTEGERLITERAL();
        return new Cobol.DataDescriptionEntry(
                randomId(),
                sourceBefore(level.getText()),
                Markers.EMPTY,
                level.getText(),
                ctx.FILLER() == null ?
                        (ctx.dataName() == null ? null : padLeft(ctx.dataName())) :
                        padLeft(ctx.FILLER()),
                convertAllContainer(ctx.dataDescriptionEntryFormat1Clause()).withLastSpace(sourceBefore("."))
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
    public Object visitQualifiedDataNameFormat1(CobolParser.QualifiedDataNameFormat1Context ctx) {
        return new Cobol.QualifiedDataNameFormat1(
                randomId(),
                prefix(ctx),
                Markers.EMPTY,
                visit(ctx.dataName(), ctx.conditionName()),
                convertAllContainer(ctx.qualifiedInData()),
                visitNullable(ctx.inFile())
        );
    }

    @Override
    public Object visitQualifiedDataNameFormat2(CobolParser.QualifiedDataNameFormat2Context ctx) {
        return new Cobol.QualifiedDataNameFormat2(
                randomId(),
                prefix(ctx),
                Markers.EMPTY,
                (Name) visit(ctx.paragraphName()),
                (Cobol.InSection) visit(ctx.inSection())
        );
    }

    @Override
    public Object visitQualifiedDataNameFormat3(CobolParser.QualifiedDataNameFormat3Context ctx) {
        return new Cobol.QualifiedDataNameFormat3(
                randomId(),
                prefix(ctx),
                Markers.EMPTY,
                (Name) visit(ctx.textName()),
                (Cobol.InLibrary) visit(ctx.inLibrary())
        );
    }

    @Override
    public Object visitQualifiedDataNameFormat4(CobolParser.QualifiedDataNameFormat4Context ctx) {
        return new Cobol.QualifiedDataNameFormat4(
                randomId(),
                prefix(ctx),
                Markers.EMPTY,
                words(ctx.LINAGE_COUNTER()),
                (Cobol.InFile) visit(ctx.inFile())
        );
    }

    @Override
    public Object visitQualifiedDataName(CobolParser.QualifiedDataNameContext ctx) {
        return new Cobol.QualifiedDataName(
                randomId(),
                prefix(ctx),
                Markers.EMPTY,
                visit(ctx.qualifiedDataNameFormat1(), ctx.qualifiedDataNameFormat1(), ctx.qualifiedDataNameFormat3(), ctx.qualifiedDataNameFormat4())
        );
    }

    @Override
    public Object visitQualifiedInData(CobolParser.QualifiedInDataContext ctx) {
        return new Cobol.QualifiedDataName(
                randomId(),
                prefix(ctx),
                Markers.EMPTY,
                visit(ctx.inData(), ctx.inTable())
        );
    }

    @Override
    public Cobol.Receive visitReceiveStatement(CobolParser.ReceiveStatementContext ctx) {
        return new Cobol.Receive(
                randomId(),
                prefix(ctx),
                Markers.EMPTY,
                words(ctx.RECEIVE()),
                visit(ctx.receiveFromStatement(), ctx.receiveIntoStatement()),
                visitNullable(ctx.onExceptionClause()),
                visitNullable(ctx.notOnExceptionClause()),
                words(ctx.END_RECEIVE())
        );
    }

    @Override
    public Cobol.ReceiveFromStatement visitReceiveFromStatement(CobolParser.ReceiveFromStatementContext ctx) {
        return new Cobol.ReceiveFromStatement(
                randomId(),
                prefix(ctx),
                Markers.EMPTY,
                (Cobol.CobolWord) visit(ctx.dataName()),
                words(ctx.FROM()),
                (Cobol.ReceiveFrom) visit(ctx.receiveFrom()),
                convertAllContainer(ctx.receiveBefore(), ctx.receiveWith(), ctx.receiveThread(), ctx.receiveSize(), ctx.receiveStatus())
        );
    }

    @Override
    public Cobol.ReceiveFrom visitReceiveFrom(CobolParser.ReceiveFromContext ctx) {
        return new Cobol.ReceiveFrom(
                randomId(),
                prefix(ctx),
                Markers.EMPTY,
                words(ctx.LAST(), ctx.ANY(), ctx.THREAD()),
                visitNullable(ctx.dataName())
        );
    }

    @Override
    public Cobol.ReceiveIntoStatement visitReceiveIntoStatement(CobolParser.ReceiveIntoStatementContext ctx) {
        return new Cobol.ReceiveIntoStatement(
                randomId(),
                prefix(ctx),
                Markers.EMPTY,
                (Cobol.CobolWord) visit(ctx.cdName()),
                words(ctx.MESSAGE(), ctx.SEGMENT(), ctx.INTO()),
                (Identifier) visit(ctx.identifier()),
                visitNullable(ctx.receiveNoData()),
                visitNullable(ctx.receiveWithData())
        );
    }

    @Override
    public Cobol.StatementPhrase visitReceiveNoData(CobolParser.ReceiveNoDataContext ctx) {
        return new Cobol.StatementPhrase(
                randomId(),
                prefix(ctx),
                Markers.EMPTY,
                words(ctx.NO(), ctx.DATA()),
                convertAllContainer(ctx.statement())
        );
    }

    @Override
    public Cobol.StatementPhrase visitReceiveWithData(CobolParser.ReceiveWithDataContext ctx) {
        return new Cobol.StatementPhrase(
                randomId(),
                prefix(ctx),
                Markers.EMPTY,
                words(ctx.WITH(), ctx.DATA()),
                convertAllContainer(ctx.statement())
        );
    }

    @Override
    public Cobol.Receivable visitReceiveBefore(CobolParser.ReceiveBeforeContext ctx) {
        return new Cobol.Receivable(
                randomId(),
                prefix(ctx),
                Markers.EMPTY,
                words(ctx.BEFORE(), ctx.TIME()),
                visit(ctx.numericLiteral(), ctx.identifier())
        );
    }

    @Override
    public Cobol.CobolWord visitReceiveWith(CobolParser.ReceiveWithContext ctx) {
        return new Cobol.CobolWord(
                randomId(),
                prefix(ctx),
                Markers.EMPTY,
                words(ctx.WITH(), ctx.NO(), ctx.WAIT())
        );
    }

    @Override
    public Cobol.Receivable visitReceiveThread(CobolParser.ReceiveThreadContext ctx) {
        return new Cobol.Receivable(
                randomId(),
                prefix(ctx),
                Markers.EMPTY,
                words(ctx.THREAD(), ctx.IN()),
                (Name) visit(ctx.dataName())
        );
    }

    @Override
    public Cobol.Receivable visitReceiveSize(CobolParser.ReceiveSizeContext ctx) {
        return new Cobol.Receivable(
                randomId(),
                prefix(ctx),
                Markers.EMPTY,
                words(ctx.SIZE(), ctx.IN()),
                visit(ctx.numericLiteral(), ctx.identifier())
        );
    }

    @Override
    public Cobol.Receivable visitReceiveStatus(CobolParser.ReceiveStatusContext ctx) {
        return new Cobol.Receivable(
                randomId(),
                prefix(ctx),
                Markers.EMPTY,
                words(ctx.STATUS(), ctx.IN()),
                (Name) visit(ctx.identifier())
        );
    }

    @Override
    public Cobol.Release visitReleaseStatement(CobolParser.ReleaseStatementContext ctx) {
        return new Cobol.Release(
                randomId(),
                prefix(ctx),
                Markers.EMPTY,
                words(ctx.RELEASE()),
                (Cobol.QualifiedDataName) visit(ctx.recordName()),
                words(ctx.FROM()),
                visitNullable(ctx.qualifiedDataName())
        );
    }

    @Override
    public Cobol.Return visitReturnStatement(CobolParser.ReturnStatementContext ctx) {
        return new Cobol.Return(
                randomId(),
                prefix(ctx),
                Markers.EMPTY,
                words(ctx.RETURN()),
                (Name) visit(ctx.fileName()),
                words(ctx.RECORD()),
                visitNullable(ctx.returnInto()),
                (Cobol.StatementPhrase) visit(ctx.atEndPhrase()),
                (Cobol.StatementPhrase) visit(ctx.notAtEndPhrase()),
                words(ctx.END_RETURN())
        );
    }

    @Override
    public Cobol.ReturnInto visitReturnInto(CobolParser.ReturnIntoContext ctx) {
        return new Cobol.ReturnInto(
                randomId(),
                prefix(ctx),
                Markers.EMPTY,
                words(ctx.INTO()),
                (Cobol.QualifiedDataName) visit(ctx.qualifiedDataName())
        );
    }

    @Override
    public Object visitEnableStatement(CobolParser.EnableStatementContext ctx) {
        return new Cobol.Enable(
                randomId(),
                prefix(ctx),
                Markers.EMPTY,
                ctx.ENABLE().getText(),
                words(ctx.INPUT(), ctx.I_O(), ctx.TERMINAL(), ctx.OUTPUT()),
                (Name) visit(ctx.cdName()),
                ctx.WITH() == null ? null : words(ctx.WITH()),
                words(ctx.KEY()),
                visit(ctx.identifier(), ctx.literal())
        );
    }

    @Override
    public Cobol.EndProgram visitEndProgramStatement(CobolParser.EndProgramStatementContext ctx) {
        return new Cobol.EndProgram(
                randomId(),
                sourceBefore(ctx.END().getText()),
                Markers.EMPTY,
                words(ctx.END(), ctx.PROGRAM()),
                (Name) visit(ctx.programName())
        );
    }

    @Override
    public Object visitEndKeyClause(CobolParser.EndKeyClauseContext ctx) {
        throw new UnsupportedOperationException("Implement me");
    }

    @Override
    public Object visitEntryStatement(CobolParser.EntryStatementContext ctx) {
        return new Cobol.Entry(
                randomId(),
                prefix(ctx),
                Markers.EMPTY,
                ctx.ENTRY().getText(),
                (Literal) visit(ctx.literal()),
                convertAllContainer(padLeft(ctx.USING()), ctx.identifier())
        );
    }

    @Override
    public Cobol.EnvironmentDivision visitEnvironmentDivision(CobolParser.EnvironmentDivisionContext ctx) {
        return new Cobol.EnvironmentDivision(
                randomId(),
                sourceBefore(ctx.ENVIRONMENT().getText()),
                Markers.EMPTY,
                words(ctx.ENVIRONMENT(), ctx.DIVISION()),
                convertAllContainer(sourceBefore("."), ctx.environmentDivisionBody())
        );
    }

    @Override
    public Object visitEvaluateStatement(CobolParser.EvaluateStatementContext ctx) {
        return new Cobol.Evaluate(
                randomId(),
                prefix(ctx),
                Markers.EMPTY,
                words(ctx.EVALUATE()),
                (Cobol) visit(ctx.evaluateSelect()),
                convertAllContainer(ctx.evaluateAlsoSelect()),
                convertAllContainer(ctx.evaluateWhenPhrase()),
                visitNullable(ctx.evaluateWhenOther()),
                padLeft(ctx.END_EVALUATE())
        );
    }

    @Override
    public Object visitEvaluateAlsoCondition(CobolParser.EvaluateAlsoConditionContext ctx) {
        return new Cobol.EvaluateAlsoCondition(
                randomId(),
                prefix(ctx),
                Markers.EMPTY,
                words(ctx.ALSO()),
                (Cobol.EvaluateCondition) visit(ctx.evaluateCondition())
        );
    }

    @Override
    public Object visitEvaluateAlsoSelect(CobolParser.EvaluateAlsoSelectContext ctx) {
        return new Cobol.EvaluateAlso(
                randomId(),
                prefix(ctx),
                Markers.EMPTY,
                words(ctx.ALSO()),
                (Cobol) visit(ctx.evaluateSelect())
        );
    }

    @Override
    public Object visitEvaluateCondition(CobolParser.EvaluateConditionContext ctx) {
        return new Cobol.EvaluateCondition(
                randomId(),
                prefix(ctx),
                Markers.EMPTY,
                words(ctx.ANY(), ctx.NOT()),
                ctx.ANY() != null ? null : visit(ctx.evaluateValue(), ctx.condition(), ctx.booleanLiteral()),
                visitNullable(ctx.evaluateThrough())
        );
    }

    @Override
    public Object visitEvaluateThrough(CobolParser.EvaluateThroughContext ctx) {
        return new Cobol.EvaluateThrough(
                randomId(),
                prefix(ctx),
                Markers.EMPTY,
                words(ctx.THROUGH(), ctx.THRU()),
                (Cobol) visit(ctx.evaluateValue())
        );
    }

    @Override
    public Object visitEvaluateWhenPhrase(CobolParser.EvaluateWhenPhraseContext ctx) {
        return new Cobol.EvaluateWhenPhrase(
                randomId(),
                prefix(ctx),
                Markers.EMPTY,
                convertAllContainer(ctx.evaluateWhen()),
                convertAllContainer(ctx.statement())
        );
    }

    @Override
    public Object visitEvaluateWhen(CobolParser.EvaluateWhenContext ctx) {
        return new Cobol.EvaluateWhen(
                randomId(),
                prefix(ctx),
                Markers.EMPTY,
                words(ctx.WHEN()),
                (Cobol.EvaluateCondition) visit(ctx.evaluateCondition()),
                convertAllContainer(ctx.evaluateAlsoCondition())
        );
    }

    @Override
    public Object visitEvaluateWhenOther(CobolParser.EvaluateWhenOtherContext ctx) {
        return new Cobol.StatementPhrase(
                randomId(),
                prefix(ctx),
                Markers.EMPTY,
                words(ctx.WHEN(), ctx.OTHER()),
                convertAllContainer(ctx.statement())
        );
    }

    @Override
    public Object visitExecCicsStatement(CobolParser.ExecCicsStatementContext ctx) {
        return new Cobol.ExecCicsStatement(
                randomId(),
                prefix(ctx),
                Markers.EMPTY,
                convertAllContainer(ctx.EXECCICSLINE())
        );
    }

    @Override
    public Object visitExecSqlImsStatement(CobolParser.ExecSqlImsStatementContext ctx) {
        return new Cobol.ExecSqlImsStatement(
                randomId(),
                prefix(ctx),
                Markers.EMPTY,
                convertAllContainer(ctx.EXECSQLIMSLINE())
        );
    }

    @Override
    public Object visitExecSqlStatement(CobolParser.ExecSqlStatementContext ctx) {
        return new Cobol.ExecSqlStatement(
                randomId(),
                prefix(ctx),
                Markers.EMPTY,
                convertAllContainer(ctx.EXECSQLLINE())
        );
    }

    @Override
    public Cobol.Exhibit visitExhibitStatement(CobolParser.ExhibitStatementContext ctx) {
        return new Cobol.Exhibit(
                randomId(),
                prefix(ctx),
                Markers.EMPTY,
                words(ctx.EXHIBIT(), ctx.NAMED(), ctx.CHANGED()),
                convertAllContainer(ctx.exhibitOperand())
        );
    }

    @Override
    public Object visitExitStatement(CobolParser.ExitStatementContext ctx) {
        return new Cobol.Exit(
                randomId(),
                prefix(ctx),
                Markers.EMPTY,
                words(ctx.EXIT(), ctx.PROGRAM())
        );
    }

    @Override
    public Object visitExternalClause(CobolParser.ExternalClauseContext ctx) {
        return new Cobol.ExternalClause(
                randomId(),
                prefix(ctx),
                Markers.EMPTY,
                words(ctx.IS(), ctx.EXTERNAL())
        );
    }

    @Override
    public Cobol.FileSection visitFileSection(CobolParser.FileSectionContext ctx) {
        return new Cobol.FileSection(
                randomId(),
                prefix(ctx),
                Markers.EMPTY,
                words(ctx.FILE(), ctx.SECTION()),
                convertAllContainer(sourceBefore("."), ctx.fileDescriptionEntry())
        );
    }

    @Override
    public Cobol.FileDescriptionEntry visitFileDescriptionEntry(CobolParser.FileDescriptionEntryContext ctx) {
        return new Cobol.FileDescriptionEntry(
                randomId(),
                prefix(ctx),
                Markers.EMPTY,
                words(ctx.FD(), ctx.SD()),
                (Cobol.CobolWord) visit(ctx.fileName()),
                convertAllList(singletonList("."), ctx.fileDescriptionEntryClause()),
                convertAllContainer(sourceBefore("."), ctx.dataDescriptionEntry())
        );
    }

    @Override
    public Object visitGenerateStatement(CobolParser.GenerateStatementContext ctx) {
        return new Cobol.Generate(
                randomId(),
                prefix(ctx),
                Markers.EMPTY,
                words(ctx.GENERATE()),
                (Cobol.QualifiedDataName) visit(ctx.reportName())
        );
    }

    @Override
    public Object visitOrganizationClause(CobolParser.OrganizationClauseContext ctx) {
        return new Cobol.OrganizationClause(
                randomId(),
                prefix(ctx),
                Markers.EMPTY,
                words(ctx.ORGANIZATION(), ctx.IS(), ctx.LINE(), ctx.RECORD(), ctx.BINARY(),
                        ctx.SEQUENTIAL(), ctx.RELATIVE(), ctx.INDEXED())
        );
    }

    @Override
    public Object visitPaddingCharacterClause(CobolParser.PaddingCharacterClauseContext ctx) {
        return new Cobol.PaddingCharacterClause(
                randomId(),
                whitespace(),
                Markers.EMPTY,
                words(ctx.PADDING(), ctx.CHARACTER(), ctx.IS()),
                visit(ctx.qualifiedDataName(), ctx.literal())
        );
    }

    @Override
    public Cobol.Paragraphs visitParagraphs(CobolParser.ParagraphsContext ctx) {
        return new Cobol.Paragraphs(
                randomId(),
                whitespace(),
                Markers.EMPTY,
                convertAllContainer(ctx.sentence()),
                convertAllContainer(ctx.paragraph())
        );
    }

    @Override
    public Object visitParagraph(CobolParser.ParagraphContext ctx) {
        return new Cobol.Paragraph(
                randomId(),
                whitespace(),
                Markers.EMPTY,
                (Name) visit(ctx.paragraphName()),
                ctx.DOT_FS() == null ? null : padLeft(ctx.DOT_FS()),
                visitNullable(ctx.alteredGoTo()),
                convertAllContainer(ctx.sentence())
        );
    }

    @Override
    public Object visitProcedureDeclaratives(CobolParser.ProcedureDeclarativesContext ctx) {
        return new Cobol.ProcedureDeclaratives(
                randomId(),
                whitespace(),
                Markers.EMPTY,
                words(ctx.DECLARATIVES(0)),
                convertAllContainer(sourceBefore("."), ctx.procedureDeclarative()),
                words(ctx.END(), ctx.DECLARATIVES(1)),
                padLeft(ctx.DOT_FS(1))
        );
    }

    @Override
    public Object visitProcedureDeclarative(CobolParser.ProcedureDeclarativeContext ctx) {
        return new Cobol.ProcedureDeclarative(
                randomId(),
                whitespace(),
                Markers.EMPTY,
                (Cobol.ProcedureSectionHeader) visit(ctx.procedureSectionHeader()),
                padLeft(sourceBefore("."), (Cobol.UseStatement) visit(ctx.useStatement())),
                padLeft(sourceBefore("."), (Cobol.Paragraphs) visit(ctx.paragraphs()))
        );
    }

    @Override
    public Cobol.ProcedureDivision visitProcedureDivision(CobolParser.ProcedureDivisionContext ctx) {
        return new Cobol.ProcedureDivision(
                randomId(),
                whitespace(),
                Markers.EMPTY,
                words(ctx.PROCEDURE(), ctx.DIVISION()),
                visitNullable(ctx.procedureDivisionUsingClause()),
                visitNullable(ctx.procedureDivisionGivingClause()),
                padLeft(ctx.DOT_FS()),
                visitNullable(ctx.procedureDeclaratives()),
                padLeft(sourceBefore(""), (Cobol.ProcedureDivisionBody) visit(ctx.procedureDivisionBody()))
        );
    }

    @Override
    public Cobol.ProcedureDivisionUsingClause visitProcedureDivisionUsingClause(CobolParser.ProcedureDivisionUsingClauseContext ctx) {
        return new Cobol.ProcedureDivisionUsingClause(
                randomId(),
                whitespace(),
                Markers.EMPTY,
                words(ctx.USING(), ctx.CHAINING()),
                convertAll(ctx.procedureDivisionUsingParameter())
        );
    }

    @Override
    public Object visitProcedureDivisionGivingClause(CobolParser.ProcedureDivisionGivingClauseContext ctx) {
        return new Cobol.ProcedureDivisionGivingClause(
                randomId(),
                whitespace(),
                Markers.EMPTY,
                words(ctx.GIVING(), ctx.RETURNING()),
                (Name) visit(ctx.dataName())
        );
    }

    @Override
    public Cobol.ProcedureDivisionByReferencePhrase visitProcedureDivisionByReferencePhrase(CobolParser.ProcedureDivisionByReferencePhraseContext ctx) {
        return new Cobol.ProcedureDivisionByReferencePhrase(
                randomId(),
                whitespace(),
                Markers.EMPTY,
                words(ctx.BY(), ctx.REFERENCE()),
                convertAll(ctx.procedureDivisionByReference())
        );
    }

    @Override
    public Cobol.ProcedureDivisionByReference visitProcedureDivisionByReference(CobolParser.ProcedureDivisionByReferenceContext ctx) {
        if(ctx.ANY() == null) {
            return new Cobol.ProcedureDivisionByReference(
                    randomId(),
                    whitespace(),
                    Markers.EMPTY,
                    words(ctx.OPTIONAL()),
                    (ctx.identifier() == null) ? (Name) visit(ctx.fileName()) : (Name) visit(ctx.identifier())
            );
        } else {
            return new Cobol.ProcedureDivisionByReference(
                    randomId(),
                    whitespace(),
                    Markers.EMPTY,
                    words(ctx.ANY()),
                    null);
        }
    }

    @Override
    public Cobol.ProcedureDivisionByValuePhrase visitProcedureDivisionByValuePhrase(CobolParser.ProcedureDivisionByValuePhraseContext ctx) {
        return new Cobol.ProcedureDivisionByValuePhrase(
                randomId(),
                whitespace(),
                Markers.EMPTY,
                words(ctx.BY(), ctx.VALUE()),
                convertAll(ctx.procedureDivisionByValue())
        );
    }

    @Override
    public Name visitProcedureDivisionByValue(CobolParser.ProcedureDivisionByValueContext ctx) {
        return visit(ctx.identifier(), ctx.literal(), ctx.ANY());
    }

    @Override
    public Cobol.ProcedureDivisionBody visitProcedureDivisionBody(CobolParser.ProcedureDivisionBodyContext ctx) {
        return new Cobol.ProcedureDivisionBody(
                randomId(),
                whitespace(),
                Markers.EMPTY,
                (Cobol.Paragraphs) visit(ctx.paragraphs()),
                convertAllContainer(ctx.procedureSection())
        );
    }

    @Override
    public Object visitProcedureSection(CobolParser.ProcedureSectionContext ctx) {
        return new Cobol.ProcedureSection(
                randomId(),
                whitespace(),
                Markers.EMPTY,
                (Cobol.ProcedureSectionHeader) visit(ctx.procedureSectionHeader()),
                padLeft(ctx.DOT_FS()),
                (Cobol.Paragraphs) visit(ctx.paragraphs())
        );
    }

    @Override
    public Object visitProcedureSectionHeader(CobolParser.ProcedureSectionHeaderContext ctx) {
        return new Cobol.ProcedureSectionHeader(
                randomId(),
                whitespace(),
                Markers.EMPTY,
                (Name) visit(ctx.sectionName()),
                words(ctx.SECTION()),
                visitNullable(ctx.integerLiteral())
        );
    }

    @Override
    public Object visitRecordDelimiterClause(CobolParser.RecordDelimiterClauseContext ctx) {
        return new Cobol.RecordDelimiterClause(
                randomId(),
                whitespace(),
                Markers.EMPTY,
                words(ctx.RECORD(), ctx.DELIMITER(), ctx.IS(), ctx.STANDARD_1(), ctx.IMPLICIT()),
                visitNullable(ctx.assignmentName())
        );
    }

    @Override
    public Cobol.Sentence visitSentence(CobolParser.SentenceContext ctx) {
        return new Cobol.Sentence(
                randomId(),
                whitespace(),
                Markers.EMPTY,
                convertAll(ctx.statement()),
                padLeft(ctx.DOT_FS())
        );
    }

    @Override
    public Cobol.Sort visitSortStatement(CobolParser.SortStatementContext ctx) {
        return new Cobol.Sort(
                randomId(),
                prefix(ctx),
                Markers.EMPTY,
                words(ctx.SORT()),
                (Cobol.CobolWord) visit(ctx.fileName()),
                convertAllContainer(ctx.sortOnKeyClause()),
                visitNullable(ctx.sortDuplicatesPhrase()),
                visitNullable(ctx.sortCollatingSequencePhrase()),
                visitNullable(ctx.sortInputProcedurePhrase()),
                convertAllContainer(ctx.sortUsing()),
                visitNullable(ctx.sortOutputProcedurePhrase()),
                convertAllContainer(ctx.sortGivingPhrase())
        );
    }

    @Override
    public Cobol.Sortable visitSortOnKeyClause(CobolParser.SortOnKeyClauseContext ctx) {
        return new Cobol.Sortable(
                randomId(),
                prefix(ctx),
                Markers.EMPTY,
                words(ctx.ON(), ctx.ASCENDING(), ctx.DESCENDING(), ctx.KEY()),
                convertAllContainer(ctx.qualifiedDataName())
        );
    }

    @Override
    public Cobol.Sortable visitSortDuplicatesPhrase(CobolParser.SortDuplicatesPhraseContext ctx) {
        return new Cobol.Sortable(
                randomId(),
                prefix(ctx),
                Markers.EMPTY,
                words(ctx.WITH(), ctx.DUPLICATES(), ctx.IN(), ctx.ORDER()),
                convertAllContainer(Collections.emptyList())
        );
    }

    @Override
    public Cobol.SortCollatingSequencePhrase visitSortCollatingSequencePhrase(CobolParser.SortCollatingSequencePhraseContext ctx) {
        return new Cobol.SortCollatingSequencePhrase(
                randomId(),
                prefix(ctx),
                Markers.EMPTY,
                words(ctx.COLLATING(), ctx.SEQUENCE(), ctx.IS()),
                convertAllContainer(ctx.alphabetName()),
                visitNullable(ctx.sortCollatingAlphanumeric()),
                visitNullable(ctx.sortCollatingNational())
        );
    }

    @Override
    public Cobol.SortProcedurePhrase visitSortCollatingAlphanumeric(CobolParser.SortCollatingAlphanumericContext ctx) {
        return new Cobol.SortProcedurePhrase(
                randomId(),
                prefix(ctx),
                Markers.EMPTY,
                words(ctx.FOR(), ctx.ALPHANUMERIC(), ctx.IS()),
                (Cobol.CobolWord) visit(ctx.alphabetName()),
                null
        );
    }

    @Override
    public Cobol.SortProcedurePhrase visitSortCollatingNational(CobolParser.SortCollatingNationalContext ctx) {
        return new Cobol.SortProcedurePhrase(
                randomId(),
                prefix(ctx),
                Markers.EMPTY,
                words(ctx.FOR(), ctx.NATIONAL(), ctx.IS()),
                (Cobol.CobolWord) visit(ctx.alphabetName()),
                null
        );
    }

    @Override
    public Cobol.SortProcedurePhrase visitSortInputProcedurePhrase(CobolParser.SortInputProcedurePhraseContext ctx) {
        return new Cobol.SortProcedurePhrase(
                randomId(),
                prefix(ctx),
                Markers.EMPTY,
                words(ctx.INPUT(), ctx.PROCEDURE(), ctx.IS()),
                (Cobol.CobolWord) visit(ctx.procedureName()),
                null
        );
    }

    @Override
    public Cobol.Sortable visitSortInputThrough(CobolParser.SortInputThroughContext ctx) {
        return new Cobol.Sortable(
                randomId(),
                prefix(ctx),
                Markers.EMPTY,
                words(ctx.THROUGH(), ctx.THRU()),
                convertAllContainer(Collections.singletonList(ctx.procedureName()))
        );
    }

    @Override
    public Cobol.Sortable visitSortUsing(CobolParser.SortUsingContext ctx) {
        return new Cobol.Sortable(
                randomId(),
                prefix(ctx),
                Markers.EMPTY,
                words(ctx.USING()),
                convertAllContainer(ctx.fileName())
        );
    }

    @Override
    public Cobol.SortProcedurePhrase visitSortOutputProcedurePhrase(CobolParser.SortOutputProcedurePhraseContext ctx) {
        return new Cobol.SortProcedurePhrase(
                randomId(),
                prefix(ctx),
                Markers.EMPTY,
                words(ctx.OUTPUT(), ctx.PROCEDURE(), ctx.IS()),
                (Cobol.CobolWord) visit(ctx.procedureName()),
                visitNullable(ctx.sortOutputThrough())
        );
    }

    @Override
    public Cobol.Sortable visitSortOutputThrough(CobolParser.SortOutputThroughContext ctx) {
        return new Cobol.Sortable(
                randomId(),
                prefix(ctx),
                Markers.EMPTY,
                words(ctx.THROUGH(), ctx.THRU()),
                convertAllContainer(Collections.singletonList(ctx.procedureName()))
        );
    }

    @Override
    public Cobol.Sortable visitSortGivingPhrase(CobolParser.SortGivingPhraseContext ctx) {
        return new Cobol.Sortable(
                randomId(),
                prefix(ctx),
                Markers.EMPTY,
                words(ctx.GIVING()),
                convertAllContainer(ctx.sortGiving())
        );
    }

    @Override
    public Cobol.SortGiving visitSortGiving(CobolParser.SortGivingContext ctx) {
        return new Cobol.SortGiving(
                randomId(),
                prefix(ctx),
                Markers.EMPTY,
                (Cobol.CobolWord) visit(ctx.fileName()),
                words(ctx.LOCK(), ctx.SAVE(), ctx.NO(), ctx.REWIND(), ctx.RELEASE(), ctx.WITH(), ctx.REMOVE(), ctx.CRUNCH())
        );
    }

    @Override
    public Object visitStartStatement(CobolParser.StartStatementContext ctx) {
        return new Cobol.Start(
                randomId(),
                prefix(ctx),
                Markers.EMPTY,
                words(ctx.START()),
                (Cobol.CobolWord) visit(ctx.fileName()),
                visitNullable(ctx.startKey()),
                visitNullable(ctx.invalidKeyPhrase()),
                visitNullable(ctx.notInvalidKeyPhrase()),
                padLeft(ctx.END_START())
        );
    }

    @Override
    public Object visitStatusKeyClause(CobolParser.StatusKeyClauseContext ctx) {
        throw new UnsupportedOperationException("Implement me");
    }

    @Override
    public Object visitStartKey(CobolParser.StartKeyContext ctx) {
        return new Cobol.StartKey(
                randomId(),
                prefix(ctx),
                Markers.EMPTY,
                words(ctx.KEY(), ctx.IS(),
                        ctx.NOT(), ctx.GREATER(), ctx.LESS(), ctx.THAN(), ctx.OR(), ctx.EQUAL(), ctx.TO(),
                        ctx.MORETHANCHAR(), ctx.LESSTHANCHAR(), ctx.MORETHANOREQUAL(), ctx.EQUALCHAR()),
                (Cobol.QualifiedDataName) visit(ctx.qualifiedDataName())
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
    public Object visitStringStatement(CobolParser.StringStatementContext ctx) {
        return new Cobol.StringStatement(
                randomId(),
                prefix(ctx),
                Markers.EMPTY,
                (Cobol.CobolWord) visit(ctx.STRING()),
                convertAllContainer(ctx.stringSendingPhrase()),
                (Cobol.StringIntoPhrase) visit(ctx.stringIntoPhrase()),
                visitNullable(ctx.stringWithPointerPhrase()),
                visitNullable(ctx.onOverflowPhrase()),
                visitNullable(ctx.notOnOverflowPhrase()),
                padLeft(ctx.END_STRING())
        );
    }

    @Override
    public Object visitStringSendingPhrase(CobolParser.StringSendingPhraseContext ctx) {
        return new Cobol.StringSendingPhrase(
                randomId(),
                prefix(ctx),
                Markers.EMPTY,
                convertAllPrefixedList(singletonList(","), ctx.stringSending()),
                visit(ctx.stringDelimitedByPhrase(), ctx.stringForPhrase())
        );
    }

    @Override
    public Object visitStringDelimitedByPhrase(CobolParser.StringDelimitedByPhraseContext ctx) {
        return new Cobol.StringDelimitedByPhrase(
                randomId(),
                prefix(ctx),
                Markers.EMPTY,
                words(ctx.DELIMITED(), ctx.BY()),
                (Cobol.CobolWord) visit(ctx.SIZE(), ctx.identifier(), ctx.literal())
        );
    }

    @Override
    public Object visitStringForPhrase(CobolParser.StringForPhraseContext ctx) {
        return new Cobol.StringForPhrase(
                randomId(),
                prefix(ctx),
                Markers.EMPTY,
                words(ctx.FOR()),
                visit(ctx.identifier(), ctx.literal())
        );
    }

    @Override
    public Object visitStringIntoPhrase(CobolParser.StringIntoPhraseContext ctx) {
        return new Cobol.StringIntoPhrase(
                randomId(),
                prefix(ctx),
                Markers.EMPTY,
                words(ctx.INTO()),
                (Identifier) visit(ctx.identifier())
        );
    }

    @Override
    public Object visitStringWithPointerPhrase(CobolParser.StringWithPointerPhraseContext ctx) {
        return new Cobol.StringWithPointerPhrase(
                randomId(),
                prefix(ctx),
                Markers.EMPTY,
                words(ctx.WITH(), ctx.POINTER()),
                (Cobol.QualifiedDataName) visit(ctx.qualifiedDataName())
        );
    }

    @Override
    public Cobol.Subscript visitSubscript(CobolParser.SubscriptContext ctx) {
        return new Cobol.Subscript(
                randomId(),
                prefix(ctx),
                Markers.EMPTY,
                visit(ctx.ALL(), ctx.qualifiedDataName(), ctx.indexName(), ctx.arithmeticExpression()),
                visitNullable(ctx.integerLiteral())
        );
    }

    @Override
    public Cobol.TableCall visitTableCall(CobolParser.TableCallContext ctx) {
        return new Cobol.TableCall(
                randomId(),
                prefix(ctx),
                Markers.EMPTY,
                (Cobol.QualifiedDataName) visit(ctx.qualifiedDataName()),
                convertAllContainer(ctx.tableCallSubscripts()),
                visitNullable(ctx.referenceModifier())
        );
    }

    @Override
    public Cobol.Parenthesized visitTableCallSubscripts(CobolParser.TableCallSubscriptsContext ctx) {
        return new Cobol.Parenthesized(
                randomId(),
                prefix(ctx),
                Markers.EMPTY,
                words(ctx.LPARENCHAR()),
                convertAllPrefixedList(Collections.singletonList(","), ctx.subscript()),
                words(ctx.RPARENCHAR())
        );
    }

    @Override
    public Cobol.FunctionCall visitFunctionCall(CobolParser.FunctionCallContext ctx) {
        return new Cobol.FunctionCall(
                randomId(),
                prefix(ctx),
                Markers.EMPTY,
                words(ctx.FUNCTION()),
                (Cobol.CobolWord) visit(ctx.functionName()),
                convertAllContainer(ctx.functionCallArguments()),
                visitNullable(ctx.referenceModifier())
        );
    }

    @Override
    public Object visitFunctionCallArguments(CobolParser.FunctionCallArgumentsContext ctx) {
        return new Cobol.Parenthesized(
                randomId(),
                prefix(ctx),
                Markers.EMPTY,
                words(ctx.LPARENCHAR()),
                convertAllPrefixedList(Collections.singletonList(","), ctx.argument()),
                words(ctx.RPARENCHAR())
        );
    }

    @Override
    public Cobol.Display visitDisplayStatement(CobolParser.DisplayStatementContext ctx) {
        if (ctx.displayAt() != null || ctx.displayUpon() != null || ctx.displayWith() != null ||
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
    public Object visitIfStatement(CobolParser.IfStatementContext ctx) {
        return new Cobol.If(
                randomId(),
                prefix(ctx),
                Markers.EMPTY,
                words(ctx.IF()),
                (Cobol.Condition) visit(ctx.condition()),
                (Cobol.IfThen) visit(ctx.ifThen()),
                visitNullable(ctx.ifElse()),
                padLeft(ctx.END_IF())
        );
    }

    @Override
    public Object visitIfElse(CobolParser.IfElseContext ctx) {
        return new Cobol.IfElse(
                randomId(),
                prefix(ctx),
                Markers.EMPTY,
                words(ctx.ELSE()),
                words(ctx.NEXT(), ctx.SENTENCE()),
                convertAllContainer(ctx.statement())
        );
    }

    @Override
    public Object visitIfThen(CobolParser.IfThenContext ctx) {
        return new Cobol.IfThen(
                randomId(),
                prefix(ctx),
                Markers.EMPTY,
                words(ctx.THEN()),
                words(ctx.NEXT(), ctx.SENTENCE()),
                convertAllContainer(ctx.statement())
        );
    }

    @Override
    public Object visitInData(CobolParser.InDataContext ctx) {
        return new Cobol.InData(
                randomId(),
                prefix(ctx),
                Markers.EMPTY,
                words(ctx.IN(), ctx.OF()),
                (Name) visit(ctx.dataName())
        );
    }

    @Override
    public Object visitInFile(CobolParser.InFileContext ctx) {
        return new Cobol.InFile(
                randomId(),
                prefix(ctx),
                Markers.EMPTY,
                words(ctx.IN(), ctx.OF()),
                (Name) visit(ctx.fileName())
        );
    }

    @Override
    public Object visitInMnemonic(CobolParser.InMnemonicContext ctx) {
        return new Cobol.InMnemonic(
                randomId(),
                prefix(ctx),
                Markers.EMPTY,
                words(ctx.IN(), ctx.OF()),
                (Name) visit(ctx.mnemonicName())
        );
    }

    @Override
    public Object visitInLibrary(CobolParser.InLibraryContext ctx) {
        return new Cobol.InLibrary(
                randomId(),
                prefix(ctx),
                Markers.EMPTY,
                words(ctx.IN(), ctx.OF()),
                (Name) visit(ctx.libraryName())
        );
    }

    @Override
    public Object visitInTable(CobolParser.InTableContext ctx) {
        return new Cobol.InLibrary(
                randomId(),
                prefix(ctx),
                Markers.EMPTY,
                words(ctx.IN(), ctx.OF()),
                (Name) visit(ctx.tableCall())
        );
    }

    @Override
    public Cobol.StatementPhrase visitInvalidKeyPhrase(CobolParser.InvalidKeyPhraseContext ctx) {
        return new Cobol.StatementPhrase(
                randomId(),
                prefix(ctx),
                Markers.EMPTY,
                words(ctx.INVALID(), ctx.KEY()),
                convertAllContainer(ctx.statement())
        );
    }

    @Override
    public Object visitInitializeStatement(CobolParser.InitializeStatementContext ctx) {
        return new Cobol.Initialize(
                randomId(),
                prefix(ctx),
                Markers.EMPTY,
                words(ctx.INITIALIZE()),
                convertAllContainer(ctx.identifier()),
                visitNullable(ctx.initializeReplacingPhrase())
        );
    }

    @Override
    public Object visitInitializeReplacingPhrase(CobolParser.InitializeReplacingPhraseContext ctx) {
        return new Cobol.InitializeReplacingPhrase(
                randomId(),
                prefix(ctx),
                Markers.EMPTY,
                words(ctx.REPLACING()),
                convertAllContainer(ctx.initializeReplacingBy())
        );
    }

    @Override
    public Object visitInitializeReplacingBy(CobolParser.InitializeReplacingByContext ctx) {
        return new Cobol.InitializeReplacingBy(
                randomId(),
                prefix(ctx),
                Markers.EMPTY,
                words(ctx.ALPHABETIC(), ctx.ALPHANUMERIC(), ctx.ALPHANUMERIC_EDITED(),
                        ctx.NATIONAL(), ctx.NATIONAL_EDITED(), ctx.NUMERIC(), ctx.NATIONAL_EDITED(),
                        ctx.DBCS(), ctx.EGCS(), ctx.DATA(), ctx.BY()),
                visit(ctx.identifier(), ctx.literal())
        );
    }

    @Override
    public Object visitInitiateStatement(CobolParser.InitiateStatementContext ctx) {
        return new Cobol.Initiate(
                randomId(),
                prefix(ctx),
                Markers.EMPTY,
                words(ctx.INITIATE()),
                convertAllContainer(ctx.reportName())
        );
    }

    @Override
    public Object visitInputOutputSection(CobolParser.InputOutputSectionContext ctx) {
        return new Cobol.InputOutputSection(
                randomId(),
                prefix(ctx),
                Markers.EMPTY,
                words(ctx.INPUT_OUTPUT(), ctx.SECTION(), ctx.DOT_FS()),
                convertAllContainer(ctx.inputOutputSectionParagraph())
        );
    }

    @Override
    public Object visitInspectStatement(CobolParser.InspectStatementContext ctx) {
        return new Cobol.Inspect(
                randomId(),
                prefix(ctx),
                Markers.EMPTY,
                words(ctx.INSPECT()),
                (Identifier) visit(ctx.identifier()),
                visit(ctx.inspectTallyingPhrase(), ctx.inspectReplacingPhrase(),
                        ctx.inspectTallyingReplacingPhrase(), ctx.inspectConvertingPhrase())
        );
    }

    @Override
    public Object visitInspectAllLeading(CobolParser.InspectAllLeadingContext ctx) {
        return new Cobol.InspectAllLeading(
                randomId(),
                prefix(ctx),
                Markers.EMPTY,
                visit(ctx.identifier(), ctx.literal()),
                convertAllContainer(ctx.inspectBeforeAfter())
        );
    }

    @Override
    public Object visitInspectAllLeadings(CobolParser.InspectAllLeadingsContext ctx) {
        return new Cobol.InspectAllLeading(
                randomId(),
                prefix(ctx),
                Markers.EMPTY,
                visit(ctx.ALL(), ctx.LEADING()),
                convertAllContainer(ctx.inspectAllLeading())
        );
    }

    @Override
    public Object visitInspectBeforeAfter(CobolParser.InspectBeforeAfterContext ctx) {
        return new Cobol.InspectBeforeAfter(
                randomId(),
                prefix(ctx),
                Markers.EMPTY,
                words(ctx.BEFORE(), ctx.AFTER(), ctx.INITIAL()),
                visit(ctx.identifier(), ctx.literal())
        );
    }

    @Override
    public Object visitInspectBy(CobolParser.InspectByContext ctx) {
        return new Cobol.InspectBy(
                randomId(),
                prefix(ctx),
                Markers.EMPTY,
                words(ctx.BY()),
                visit(ctx.identifier(), ctx.literal())
        );
    }

    @Override
    public Object visitInspectCharacters(CobolParser.InspectCharactersContext ctx) {
        return new Cobol.InspectCharacters(
                randomId(),
                prefix(ctx),
                Markers.EMPTY,
                words(ctx.CHARACTER(), ctx.CHARACTERS()),
                convertAllContainer(ctx.inspectBeforeAfter())
        );
    }

    @Override
    public Object visitInspectConvertingPhrase(CobolParser.InspectConvertingPhraseContext ctx) {
        return new Cobol.InspectConvertingPhrase(
                randomId(),
                prefix(ctx),
                Markers.EMPTY,
                words(ctx.CONVERTING()),
                visit(ctx.identifier(), ctx.literal()),
                (Cobol.InspectTo) visit(ctx.inspectTo()),
                convertAllContainer(ctx.inspectBeforeAfter())
        );
    }

    @Override
    public Object visitInspectFor(CobolParser.InspectForContext ctx) {
        return new Cobol.InspectFor(
                randomId(),
                prefix(ctx),
                Markers.EMPTY,
                (Identifier) visit(ctx.identifier()),
                words(ctx.FOR()),
                convertAllContainer(ctx.inspectCharacters(), ctx.inspectAllLeadings())
        );
    }

    @Override
    public Object visitInspectReplacingAllLeadings(CobolParser.InspectReplacingAllLeadingsContext ctx) {
        return new Cobol.InspectReplacingAllLeadings(
                randomId(),
                prefix(ctx),
                Markers.EMPTY,
                words(ctx.ALL(), ctx.LEADING(), ctx.FIRST()),
                convertAllContainer(ctx.inspectReplacingAllLeading())
        );
    }

    @Override
    public Object visitInspectReplacingAllLeading(CobolParser.InspectReplacingAllLeadingContext ctx) {
        return new Cobol.InspectReplacingAllLeading(
                randomId(),
                prefix(ctx),
                Markers.EMPTY,
                visit(ctx.identifier(), ctx.literal()),
                (Cobol.InspectBy) visit(ctx.inspectBy()),
                convertAllContainer(ctx.inspectBeforeAfter())
        );
    }

    @Override
    public Object visitInspectReplacingCharacters(CobolParser.InspectReplacingCharactersContext ctx) {
        return new Cobol.InspectReplacingCharacters(
                randomId(),
                prefix(ctx),
                Markers.EMPTY,
                words(ctx.CHARACTER(), ctx.CHARACTERS()),
                (Cobol.InspectBy) visit(ctx.inspectBy()),
                convertAllContainer(ctx.inspectBeforeAfter())
        );
    }

    @Override
    public Object visitInspectReplacingPhrase(CobolParser.InspectReplacingPhraseContext ctx) {
        return new Cobol.InspectReplacingPhrase(
                randomId(),
                prefix(ctx),
                Markers.EMPTY,
                words(ctx.REPLACING()),
                convertAllContainer(ctx.inspectReplacingCharacters(), ctx.inspectReplacingAllLeadings())
        );
    }

    @Override
    public Object visitInspectTallyingPhrase(CobolParser.InspectTallyingPhraseContext ctx) {
        return new Cobol.InspectTallyingPhrase(
                randomId(),
                prefix(ctx),
                Markers.EMPTY,
                words(ctx.TALLYING()),
                convertAllContainer(ctx.inspectFor())
        );
    }

    @Override
    public Object visitInspectTallyingReplacingPhrase(CobolParser.InspectTallyingReplacingPhraseContext ctx) {
        return new Cobol.InspectTallyingReplacingPhrase(
                randomId(),
                prefix(ctx),
                Markers.EMPTY,
                words(ctx.TALLYING()),
                convertAllContainer(ctx.inspectFor()),
                convertAllContainer(ctx.inspectReplacingPhrase())
        );
    }

    @Override
    public Object visitInspectTo(CobolParser.InspectToContext ctx) {
        return new Cobol.InspectTo(
                randomId(),
                prefix(ctx),
                Markers.EMPTY,
                words(ctx.TO()),
                visit(ctx.identifier(), ctx.literal())
        );
    }

    @Override
    public Object visitIoControlParagraph(CobolParser.IoControlParagraphContext ctx) {
        return new Cobol.IoControlParagraph(
                randomId(),
                prefix(ctx),
                Markers.EMPTY,
                words(ctx.I_O_CONTROL()),
                padLeft(ctx.DOT_FS(0)),
                visitNullable(ctx.fileName()),
                ctx.fileName() == null ? null : words(ctx.DOT_FS(1)),
                convertAllContainer(ctx.ioControlClause()).withLastSpace(sourceBefore("."))
        );
    }

    @Override
    public Cobol.Picture visitPicture(CobolParser.PictureContext ctx) {
        return new Cobol.Picture(
                randomId(),
                prefix(ctx),
                Markers.EMPTY,
                convertAllContainer(ctx.pictureChars()),
                visitNullable(ctx.pictureCardinality())
        );
    }

    @Override
    public Object visitPictureCardinality(CobolParser.PictureCardinalityContext ctx) {
        return new Cobol.Parenthesized(
                randomId(),
                prefix(ctx),
                Markers.EMPTY,
                words(ctx.LPARENCHAR()),
                singletonList((Cobol) visit(ctx.integerLiteral())),
                words(ctx.RPARENCHAR())
        );
    }

    @Override
    public Object visitPictureString(CobolParser.PictureStringContext ctx) {
        return new Cobol.PictureString(
                randomId(),
                prefix(ctx),
                Markers.EMPTY,
                convertAllContainer(ctx.picture())
        );
    }

    @Override
    public Cobol.ProgramIdParagraph visitProgramIdParagraph(CobolParser.ProgramIdParagraphContext ctx) {
        return new Cobol.ProgramIdParagraph(
                randomId(),
                sourceBefore(ctx.PROGRAM_ID().getText()),
                Markers.EMPTY,
                ctx.PROGRAM_ID().getText(),
                padLeft(sourceBefore("."), (Name) visit(ctx.programName())),
                padLeft(whitespace(), words(ctx.IS(), ctx.COMMON(), ctx.INITIAL(), ctx.LIBRARY(), ctx.DEFINITION(), ctx.RECURSIVE(), ctx.PROGRAM())),
                ctx.DOT_FS().size() > 1 ? padLeft(ctx.DOT_FS(1)) : null
        );
    }

    @Override
    public Object visitValueOfClause(CobolParser.ValueOfClauseContext ctx) {
        return new Cobol.ValueOfClause(
                randomId(),
                prefix(ctx),
                Markers.EMPTY,
                words(ctx.VALUE(), ctx.OF()),
                convertAllContainer(ctx.valuePair())
        );
    }

    @Override
    public Object visitValuePair(CobolParser.ValuePairContext ctx) {
        return new Cobol.ValuePair(
                randomId(),
                prefix(ctx),
                Markers.EMPTY,
                (Cobol.CobolWord) visit(ctx.systemName()),
                words(ctx.IS()),
                visit(ctx.qualifiedDataName(), ctx.literal())
        );
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

    private <C extends Cobol, T extends ParseTree> CobolContainer<C> convertAllContainer(@Nullable CobolLeftPadded<String> preposition, List<T> trees) {
        return this.<C, T>convertAllContainer(trees, () -> Space.EMPTY).withPreposition(preposition);
    }

    private <C extends Cobol, T extends ParseTree> CobolContainer<C> convertAllContainer(Space before, List<T> trees) {
        return this.<C, T>convertAllContainer(trees, () -> Space.EMPTY).withBefore(before);
    }

    private <C extends Cobol, T extends ParseTree> CobolContainer<C> convertAllContainer(List<T> trees) {
        return convertAllContainer(trees, () -> Space.EMPTY);
    }

    @SafeVarargs
    private final CobolContainer<Cobol> convertAllContainer(List<? extends ParserRuleContext>... trees) {
        return convertAllContainer(Arrays.stream(trees)
                        .filter(Objects::nonNull)
                        .flatMap(Collection::stream)
                        .sorted(Comparator.comparingInt(it -> it.start.getStartIndex()))
                        .collect(Collectors.toList()),
                () -> Space.EMPTY);
    }

    private <C extends Cobol, T extends ParseTree> CobolContainer<C> convertAllContainer(List<T> trees, Supplier<Space> sourceBefore) {
        //noinspection unchecked
        return CobolContainer.build(convertAll(trees, t -> padRight((C) visit(t), sourceBefore.get())));
    }

    @SafeVarargs
    private final List<Cobol> convertAllPrefixedList(List<String> delimiters, List<? extends ParseTree>... trees) {
        return Arrays.stream(trees)
                .flatMap(Collection::stream)
                .filter(Objects::nonNull)
                .flatMap(it -> {
                    Cobol cobol = (Cobol) visit(it);
                    int saveCursor = cursor;
                    Space prefix = whitespace();
                    for (String delimiter : delimiters) {
                        if (source.substring(cursor).startsWith(delimiter)) {
                            Cobol.CobolWord comma = new Cobol.CobolWord(
                                    randomId(),
                                    prefix,
                                    Markers.EMPTY,
                                    delimiter
                            );
                            cursor += delimiter.length(); // skip the delimiter
                            return Stream.of(cobol, comma);
                        }
                    }
                    cursor = saveCursor;
                    return Stream.of(cobol);
                })
                .collect(Collectors.toList());
    }

    @SafeVarargs
    private final List<Cobol> convertAllList(List<String> delimiters, List<? extends ParseTree>... trees) {
        return Arrays.stream(trees)
                .flatMap(Collection::stream)
                .filter(Objects::nonNull)
                .flatMap(it -> {
                    int saveCursor = cursor;
                    Space prefix = whitespace();
                    Cobol.CobolWord cw = null;
                    for (String delimiter : delimiters) {
                        if (source.substring(cursor).startsWith(delimiter)) {
                            cw = new Cobol.CobolWord(
                                    randomId(),
                                    prefix,
                                    Markers.EMPTY,
                                    delimiter
                            );
                            cursor += delimiter.length(); // skip the delimiter
                            break;
                        }
                    }

                    if (cw == null) {
                        cursor = saveCursor;
                    }

                    Cobol cobol = (Cobol) visit(it);
                    return cw == null ? Stream.of(cobol) : Stream.of(cw, cobol);
                })
                .collect(Collectors.toList());
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

    private String skip(String string) {
        cursor += string.length();
        return string;
    }
}
