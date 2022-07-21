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
    public Cobol.ChannelClause visitChannelClause(CobolParser.ChannelClauseContext ctx) {
        return new Cobol.ChannelClause(
                randomId(),
                prefix(ctx),
                Markers.EMPTY,
                words(ctx.CHANNEL()),
                (Cobol.Literal) visit(ctx.integerLiteral()),
                padLeft(ctx.IS()),
                (Cobol.Identifier) visit(ctx.mnemonicName())
        );
    }

    @Override
    public Cobol.CurrencyClause visitCurrencySignClause(CobolParser.CurrencySignClauseContext ctx) {
        return new Cobol.CurrencyClause(
                randomId(),
                prefix(ctx),
                Markers.EMPTY,
                words(ctx.CURRENCY(), ctx.SIGN(), ctx.IS()),
                (Cobol.Literal) visit(ctx.literal(0)),
                ctx.literal().size() > 1 ? padLeft(whitespace(), words(ctx.WITH(), ctx.PICTURE(), ctx.SYMBOL())) : null,
                ctx.literal().size() > 1 ? (Cobol.Literal) visit(ctx.literal(1)) : null
        );
    }

    @Override
    public Cobol.OdtClause visitOdtClause(CobolParser.OdtClauseContext ctx) {
        return new Cobol.OdtClause(
                randomId(),
                prefix(ctx),
                Markers.EMPTY,
                words(ctx.ODT(), ctx.IS()),
                (Cobol.Identifier) visit(ctx.mnemonicName())
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
    public Cobol.ReserveNetworkClause visitReserveNetworkClause(CobolParser.ReserveNetworkClauseContext ctx) {
        return new Cobol.ReserveNetworkClause(
                randomId(),
                prefix(ctx),
                Markers.EMPTY,
                words(ctx.RESERVE(), ctx.WORDS(), ctx.LIST(), ctx.IS(), ctx.NETWORK(), ctx.CAPABLE())
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
        Cobol.Identifier name = (Cobol.Identifier) visit(ctx.alphabetName());
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
                (Cobol.Identifier) visit(ctx.alphabetName()),
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
                (Cobol.Literal) visit(ctx.literal()),
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
                (Cobol.Literal) visit(ctx.literal())
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
                (Cobol.Identifier) visit(ctx.alphabetName())
        );
    }

    @Override
    public Object visitCollatingSequenceClauseNational(CobolParser.CollatingSequenceClauseNationalContext ctx) {
        return new Cobol.CollatingSequenceAlphabet(
                randomId(),
                prefix(ctx),
                Markers.EMPTY,
                words(ctx.FOR(), ctx.NATIONAL(), ctx.IS()),
                (Cobol.Identifier) visit(ctx.alphabetName())
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
    public Object visitScreenDescriptionBlankClause(CobolParser.ScreenDescriptionBlankClauseContext ctx) {
        return new Cobol.ScreenDescriptionBlankClause(
                randomId(),
                prefix(ctx),
                Markers.EMPTY,
                words(ctx.BLANK(), ctx.SCREEN(), ctx.LINE())
        );
    }

    @Override
    public Object visitScreenDescriptionControlClause(CobolParser.ScreenDescriptionControlClauseContext ctx) {
        return new Cobol.ScreenDescriptionControlClause(
                randomId(),
                prefix(ctx),
                Markers.EMPTY,
                words(ctx.CONTROL(), ctx.IS()),
                visitIdentifier(ctx.identifier())
        );
    }

    @Override
    public Object visitScreenDescriptionSizeClause(CobolParser.ScreenDescriptionSizeClauseContext ctx) {
        return new Cobol.ScreenDescriptionSizeClause(
                randomId(),
                prefix(ctx),
                Markers.EMPTY,
                words(ctx.SIZE(), ctx.IS()),
                visitIdentifier(ctx.identifier())
        );
    }

    @Override
    public Object visitScreenDescriptionToClause(CobolParser.ScreenDescriptionToClauseContext ctx) {
        return new Cobol.ScreenDescriptionToClause(
                randomId(),
                prefix(ctx),
                Markers.EMPTY,
                words(ctx.TO()),
                visitIdentifier(ctx.identifier())
        );
    }

    @Override
    public Object visitScreenDescriptionUsingClause(CobolParser.ScreenDescriptionUsingClauseContext ctx) {
        return new Cobol.ScreenDescriptionUsingClause(
                randomId(),
                prefix(ctx),
                Markers.EMPTY,
                words(ctx.USING()),
                visitIdentifier(ctx.identifier())
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
                convertAllContainer(emptyList()) // add screen clauses
                        .withLastSpace(sourceBefore("."))
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
                visitIdentifier(ctx.identifier()),
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
    public Cobol.Identifier visitCobolWord(CobolParser.CobolWordContext ctx) {
        return new Cobol.Identifier(
                randomId(),
                sourceBefore(ctx.getText()),
                Markers.EMPTY,
                ctx.getText()
        );
    }

    @Override
    public Cobol.Identifier visitIdentifier(CobolParser.IdentifierContext ctx) {
        return new Cobol.Identifier(
                randomId(),
                sourceBefore(ctx.getText()),
                Markers.EMPTY,
                ctx.getText()
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
                (Cobol.Literal) visit(ctx.literal(0)),
                words(ctx.INVOKE()),
                (Cobol.Literal) visit(ctx.literal(1))
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
    public Object visitLinkageSection(CobolParser.LinkageSectionContext ctx) {
        return new Cobol.LinkageSection(
                randomId(),
                sourceBefore(ctx.LINKAGE().getText()),
                Markers.EMPTY,
                words(ctx.LINKAGE(), ctx.SECTION()),
                convertAllContainer(sourceBefore("."), ctx.dataDescriptionEntry())
        );
    }

    @Override
    public Object visitLocalStorageSection(CobolParser.LocalStorageSectionContext ctx) {
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
        return new Cobol.EnvironmentDivision(
                randomId(),
                sourceBefore(ctx.ENVIRONMENT().getText()),
                Markers.EMPTY,
                words(ctx.ENVIRONMENT(), ctx.DIVISION()),
                convertAllContainer(sourceBefore("."), ctx.environmentDivisionBody())
        );
    }

    @Override
    public Object visitFileSection(CobolParser.FileSectionContext ctx) {
        return new Cobol.FileSection(
                randomId(),
                prefix(ctx),
                Markers.EMPTY,
                words(ctx.FILE(), ctx.SECTION()),
                convertAllContainer(sourceBefore("."), ctx.fileDescriptionEntry())
        );
    }

    @Override
    public Object visitFileDescriptionEntry(CobolParser.FileDescriptionEntryContext ctx) {
        Space prefix = prefix(ctx);
        String words = words(ctx.FD() == null ? ctx.SD() : ctx.FD());
        Cobol.Identifier name = (Cobol.Identifier) visit(ctx.fileName());
        return new Cobol.FileDescriptionEntry(
                randomId(),
                prefix,
                Markers.EMPTY,
                words,
                name,
                CobolContainer.empty(),
                convertAllContainer(sourceBefore("."), ctx.dataDescriptionEntry())
        );
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
                        .map(this::skip)
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

    private <C extends Cobol, T extends ParseTree> CobolContainer<C> convertAllContainer(@Nullable CobolLeftPadded<String> preposition, List<T> trees) {
        return this.<C, T>convertAllContainer(trees, () -> Space.EMPTY).withPreposition(preposition);
    }

    private <C extends Cobol, T extends ParseTree> CobolContainer<C> convertAllContainer(Space before, List<T> trees) {
        return this.<C, T>convertAllContainer(trees, () -> Space.EMPTY).withBefore(before);
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

    private String skip(String string) {
        cursor += string.length();
        return string;
    }
}
