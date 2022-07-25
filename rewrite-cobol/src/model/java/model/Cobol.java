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
package model;

import org.openrewrite.cobol.tree.CobolContainer;
import org.openrewrite.cobol.tree.CobolLeftPadded;
import org.openrewrite.cobol.tree.CobolRightPadded;
import org.openrewrite.internal.lang.Nullable;

import java.util.List;

public interface Cobol {

    class Accept implements Statement {
        String accept;
        Identifier identifier;
        Cobol operation;

        @Nullable
        StatementPhrase onExceptionClause;

        @Nullable
        StatementPhrase notOnExceptionClause;

        @Nullable
        CobolLeftPadded<String> endAccept;
    }

    class AcceptFromDateStatement implements Cobol {
        String words;
    }

    class AcceptFromMnemonicStatement implements Cobol {
        String from;

        Identifier mnemonicName;
    }

    class AcceptFromEscapeKeyStatement implements Cobol {
        String words;
    }

    class AcceptMessageCountStatement implements Cobol {
        String words;
    }

    class Add implements Statement {
        String add;
        Cobol operation;

        @Nullable
        StatementPhrase onSizeError;

        @Nullable
        CobolLeftPadded<String> endAdd;
    }

    class AddTo implements Cobol {
        CobolContainer<Name> from;

        @Nullable
        CobolContainer<Name> to;

        @Nullable
        CobolContainer<Name> giving;
    }

    class AlteredGoTo implements Cobol {
        String words;
        CobolLeftPadded<String> dot;
    }

    class AlphabetClause implements Cobol {
        String words;
        Identifier name;

        /**
         * At least one of this or {@link #literals} are non-null.
         * When the standard is CCSVERSION, literals must have a single element.
         */
        @Nullable
        CobolLeftPadded<String> standard;

        /**
         * At least one of {@link #standard} or this are non-null.
         */
        @Nullable
        CobolContainer<AlphabetLiteral> literals;
    }

    class AlphabetLiteral implements Cobol {
        Literal literal;

        @Nullable
        AlphabetThrough alphabetThrough;

        @Nullable
        CobolContainer<AlphabetAlso> alphabetAlso;
    }

    class AlphabetThrough implements Cobol {
        String words;
        Literal literal;
    }

    class AlphabetAlso implements Cobol {
        String words;
        CobolContainer<Literal> literals;
    }

    class AlterStatement implements Statement {
        String words;
        List<AlterProceedTo> alterProceedTo;
    }

    class AlterProceedTo implements Cobol {
        ProcedureName from;
        String words;
        ProcedureName to;
    }

    class ProcedureName implements Cobol {
        Name paragraphName;

        @Nullable
        InSection inSection;

        @Nullable
        Name sectionName;
    }

    class InData implements Cobol {
        String words;
        Name name;
    }

    class InFile implements Cobol {
        String words;
        Name name;
    }

    class InMnemonic implements Cobol {
        String words;
        Name name;
    }

    class InSection implements Cobol {
        String words;
        Name name;
    }

    class InLibrary implements Cobol {
        String words;
        Name name;
    }

    class InTable implements Cobol {
        String words;
        // TODO .. implement TableCall
    }

    class Call implements Statement {
        String call;
        Name identifier;

        @Nullable
        CallPhrase callUsingPhrase;

        @Nullable
        CallGivingPhrase callGivingPhrase;

        @Nullable
        StatementPhrase onOverflowPhrase;

        @Nullable
        StatementPhrase onExceptionClause;

        @Nullable
        StatementPhrase notOnExceptionClause;

        @Nullable
        CobolLeftPadded<String> endCall;
    }

    class CallPhrase implements Cobol {
        String words;
        CobolContainer<Cobol> parameters;
    }

    class CallGivingPhrase implements Cobol {
        String words;
        Name identifier;
    }

    class CallBy implements Cobol {
        String words;

        @Nullable
        Name identifier;
    }

    class Cancel implements Statement {
        String cancel;
        CobolContainer<CancelCall> cancelCalls;
    }

    class CancelCall implements Cobol {
        @Nullable
        Name libraryName;

        @Nullable
        String by;

        @Nullable
        Identifier identifier;

        @Nullable
        Literal literal;
    }

    class ChannelClause implements Cobol {
        String words;
        Literal literal;

        @Nullable
        CobolLeftPadded<String> is;

        Identifier mnemonicName;
    }

    class ClassClause implements Cobol {
        String words;
        Identifier className;
        CobolContainer<ClassClauseThrough> throughs;
    }

    class ClassClauseThrough implements Cobol {
        Name from;

        @Nullable
        CobolLeftPadded<String> through;

        @Nullable
        Name to;
    }

    class Close implements Statement {
        String close;
        CobolContainer<CloseFile> closeFiles;
    }

    class CloseFile implements Cobol {
        Name fileName;

        @Nullable
        Cobol closeStatement;
    }

    class CloseReelUnitStatement implements Cobol {
        String words;
    }

    class CloseRelativeStatement implements Cobol {
        String words;
    }

    class ClosePortFileIOStatement implements Cobol {
        String words;

        CobolContainer<Cobol> closePortFileIOUsing;
    }

    class ClosePortFileIOUsingCloseDisposition implements Cobol {
        String words;
    }

    class ClosePortFileIOUsingAssociatedData implements Cobol {
        String associatedData;

        Identifier identifier;
    }

    class ClosePortFileIOUsingAssociatedDataLength implements Cobol {
        String words;

        Identifier identifier;
    }

    class CollatingSequenceClause implements Cobol {
        String words;
        CobolContainer<Identifier> alphabetName;

        @Nullable
        CollatingSequenceAlphabet alphanumeric;

        @Nullable
        CollatingSequenceAlphabet national;
    }

    class CollatingSequenceAlphabet implements Cobol {
        String words;
        Identifier alphabetName;
    }

    class ConfigurationSection implements Cobol {
        String words;
        CobolContainer<Cobol> paragraphs;
    }

    class Continue implements Statement {
        String word;
    }

    class CurrencyClause implements Cobol {
        String words;
        Literal literal;

        @Nullable
        CobolLeftPadded<String> pictureSymbol;

        @Nullable
        Literal pictureSymbolLiteral;
    }

    class DataBaseSection implements Cobol {
        String words;

        CobolContainer<DataBaseSectionEntry> entries;
    }

    class DataBaseSectionEntry implements Cobol {
        String db;

        Literal from;
        String invoke;
        Literal to;
    }

    class DataDivision implements Cobol {
        String words;
        CobolContainer<DataDivisionSection> sections;
    }

    class DataDescriptionEntry implements Cobol {
        String level;

        @Nullable
        CobolLeftPadded<String> name;

        CobolContainer<Cobol> clauses;
    }

    class DataPictureClause implements Cobol {
        String words;
        CobolContainer<Picture> pictures;
    }

    class DecimalPointClause implements Cobol {
        String words;
    }

    class DefaultComputationalSignClause implements Cobol {
        String words;
    }

    class DefaultDisplaySignClause implements Cobol {
        String words;
    }

    class Delete implements Statement {
        String delete;
        Name fileName;

        @Nullable
        String record;

        @Nullable
        StatementPhrase invalidKey;

        @Nullable
        StatementPhrase notInvalidKey;

        @Nullable
        CobolLeftPadded<String> endDelete;
    }

    class Disable implements Statement {
        String disable;
        String type;
        Name cdName;

        @Nullable
        String with;

        String key;
        Name keyName;
    }

    class Display implements Statement {
        String display;
        List<Name> operands;
    }

    class Enable implements Statement {
        String enable;
        String type;
        Name cdName;

        @Nullable
        String with;

        String key;
        Name keyName;
    }

    class EndProgram implements Statement {
        String words;
        Name programName;
    }

    class Entry implements Statement {
        String entry;
        Literal literal;

        @Nullable
        CobolContainer<Identifier> identifiers;
    }

    class EnvironmentDivision implements Cobol {
        String words;
        CobolContainer<Cobol> body;
    }

    class Exhibit implements Statement {
        String words;
        CobolContainer<Identifier> operands;
    }

    class Exit implements Statement {
        String words;
    }

    class FileSection implements DataDivisionSection {
        String words;

        CobolContainer<FileDescriptionEntry> fileDescriptionEntry;
    }

    class Generate implements Statement {
        String generate;
        ReportName reportName;
    }

    class FileDescriptionEntry implements Cobol {
        String words;
        Identifier name;
        CobolContainer<Cobol> clauses;
        CobolContainer<DataDescriptionEntry> dataDescriptions;
    }

    class Identifier implements Name {
        String simpleName;
    }

    class Literal implements Name {
        Object value;
        String valueSource;

        @Override
        public String getSimpleName() {
            return value.toString();
        }
    }

    class LocalStorageSection implements Cobol {
        String words;

        String localData;
        Name localName;

        CobolContainer<DataDescriptionEntry> dataDescriptions;
    }

    class IdentificationDivision implements Cobol {
        String words;
        CobolLeftPadded<ProgramIdParagraph> programIdParagraph;
    }

    class LinkageSection implements Cobol {
        String words;
        CobolContainer<DataDescriptionEntry> dataDescriptions;
    }

    class ObjectComputer implements Cobol {
        CobolRightPadded<String> words;

        @Nullable
        CobolRightPadded<ObjectComputerDefinition> computer;
    }

    class ObjectComputerDefinition implements Cobol {
        String computerName;
        CobolContainer<Cobol> specifications;
    }

    class OdtClause implements Cobol {
        String words;
        Identifier mnemonicName;
    }

    class ProcedureDivision implements Cobol {
        String words;

        @Nullable
        ProcedureDivisionUsingClause procedureDivisionUsingClause;

        @Nullable
        ProcedureDivisionGivingClause procedureDivisionGivingClause;

        CobolLeftPadded<String> dot;

        @Nullable
        ProcedureDeclaratives procedureDeclaratives;

        CobolLeftPadded<ProcedureDivisionBody> body;
    }

    class ProcedureDeclaratives implements Cobol {
        String declaratives;
        CobolContainer<ProcedureDeclarative> procedureDeclarative;
        String endDeclaratives;
        CobolLeftPadded<String> dot;
    }

    class ProcedureDeclarative implements Cobol {
        ProcedureSectionHeader procedureSectionHeader;
        CobolLeftPadded<UseStatement> useStatement;
        CobolLeftPadded<Paragraphs> paragraphs;
    }

    class ProcedureSection implements Cobol {
        ProcedureSectionHeader procedureSectionHeader;
        CobolLeftPadded<String> dot;
        Paragraphs paragraphs;
    }

    class ProcedureSectionHeader implements Cobol {
        Name sectionName;
        String section;
        Name identifier;
    }

    class ProcedureDivisionGivingClause implements Cobol {
        String words;
        Name dataName;
    }

    class ProcedureDivisionUsingClause implements Cobol {
        String words;
        List<Cobol> procedureDivisionUsingParameter;
    }

    class ProcedureDivisionByReferencePhrase implements Cobol {
        @Nullable
        String words;

        List<ProcedureDivisionByReference> procedureDivisionByReference;
    }

    class ProcedureDivisionByReference implements Cobol {
        @Nullable
        String words;

        Name reference;
    }

    class ProcedureDivisionByValuePhrase implements Cobol {
        String words;

        List<Name> phrases;
    }

    class ProcedureDivisionBody implements Cobol {
        Paragraphs paragraphs;

        @Nullable
        CobolContainer<ProcedureSection> procedureSection;
    }

    class Paragraphs implements Cobol {
        CobolContainer<Sentence> sentences;

        CobolContainer<Paragraph> paragraphs;
    }

    class Paragraph implements Cobol {
        Name paragraphName;

        @Nullable
        CobolLeftPadded<String> dot;

        @Nullable
        AlteredGoTo alteredGoTo;

        @Nullable
        CobolContainer<Sentence> sentences;
    }

    class Picture implements Cobol {
        String chars;

        @Nullable
        CobolLeftPadded<String> cardinalitySource;

        @Nullable
        public String getCardinality() {
            return cardinalitySource == null ? null : cardinalitySource
                    .getElement()
                    .replace("(", "")
                    .replace(")", "")
                    .trim();
        }
    }

    class ProgramIdParagraph implements Cobol {
        String programId;
        CobolLeftPadded<Name> programName;

        @Nullable
        CobolLeftPadded<String> programAttributes;

        @Nullable
        CobolLeftPadded<String> dot;
    }

    class ProgramUnit implements Cobol {
        IdentificationDivision identificationDivision;

        @Nullable
        EnvironmentDivision environmentDivision;

        @Nullable
        DataDivision dataDivision;

        @Nullable
        ProcedureDivision procedureDivision;

        CobolContainer<ProgramUnit> programUnits;

        @Nullable
        CobolRightPadded<EndProgram> endProgram;
    }

    class QualifiedDataName implements Cobol {
        Cobol dataName;
    }

    class QualifiedDataNameFormat1 implements Cobol {
        Name name;

        @Nullable
        CobolContainer<Cobol> qualifiedInData;

        @Nullable
        InFile inFile;
    }

    class QualifiedDataNameFormat2 implements Cobol {
        Name paragraphName;
        InSection inSection;
    }

    class QualifiedDataNameFormat3 implements Cobol {
        Name textName;
        InLibrary inLibrary;
    }

    class QualifiedDataNameFormat4 implements Cobol {
        String linageCounter;
        InFile inFile;
    }

    class QualifiedInData implements Cobol {
        Cobol in;
    }

    class ReportName implements Cobol {
        QualifiedDataName qualifiedDataName;
    }

    class ReserveNetworkClause implements Cobol {
        String words;
    }

    class Rewrite implements Statement {
        String rewrite;

        @Nullable
        QualifiedDataName recordName;

        @Nullable
        StatementPhrase invalidKeyPhrase;

        @Nullable
        StatementPhrase notInvalidKeyPhrase;

        @Nullable
        CobolLeftPadded<String> endRewrite;
    }

    class RewriteFrom implements Cobol {
        String from;
        Name identifier;
    }

    class Roundable implements Name {
        Identifier identifier;

        @Nullable
        CobolLeftPadded<String> rounded;

        @Override
        public String getSimpleName() {
            return identifier.getSimpleName();
        }
    }

    class ScreenSection implements Cobol {
        String words;
        CobolContainer<ScreenDescriptionEntry> descriptions;
    }

    class ScreenDescriptionEntry implements Cobol {
        String words;

        @Nullable
        CobolLeftPadded<String> name;

        CobolContainer<Cobol> clauses;
    }

    class ScreenDescriptionBlankClause implements Cobol {
        String words;
    }

    class ScreenDescriptionControlClause implements Cobol {
        String words;
        Identifier identifier;
    }

    class ScreenDescriptionSizeClause implements Cobol {
        String words;
        Identifier identifier;
    }

    class ScreenDescriptionToClause implements Cobol {
        String words;
        Identifier identifier;
    }

    class ScreenDescriptionUsingClause implements Cobol {
        String words;
        Identifier identifier;
    }

    class Sentence implements Cobol {
        List<Statement> statements;
        CobolLeftPadded<String> dot;
    }

    class Set implements Statement {
        String set;

        @Nullable
        CobolContainer<SetTo> to;

        @Nullable
        SetUpDown upDown;
    }

    class SetTo implements Cobol {
        CobolContainer<Identifier> to;
        CobolContainer<Name> values;
    }

    class SetUpDown implements Cobol {
        CobolContainer<Identifier> to;
        CobolLeftPadded<String> operation;
        Name value;
    }

    class SourceComputer implements Cobol {
        CobolRightPadded<String> words;

        @Nullable
        CobolRightPadded<SourceComputerDefinition> computer;
    }

    class SourceComputerDefinition implements Cobol {
        String computerName;

        @Nullable
        CobolLeftPadded<String> debuggingMode;
    }

    class SpecialNames implements Cobol {
        String words;

        @Nullable
        CobolContainer<Cobol> clauses;
    }

    class StatementPhrase implements Cobol {
        String phrase;
        CobolContainer<Statement> statement;
    }

    class Stop implements Statement {
        String words;
        Cobol statement;
    }

    class SymbolicCharacter implements Cobol {
        CobolContainer<Identifier> symbols;
        CobolContainer<Literal> literals;
    }

    class SymbolicCharactersClause implements Cobol {
        String words;
        CobolContainer<SymbolicCharacter> symbols;

        @Nullable
        CobolLeftPadded<String> inAlphabet;

        @Nullable
        Identifier alphabetName;
    }

    class UseStatement implements Cobol {
        String use;
        Cobol clause;
    }

    class UseAfterClause implements Cobol {
        String words;
        UseAfterOn useAfterOn;
    }

    class UseAfterOn implements Cobol {
        @Nullable
        String afterOn;

        @Nullable
        CobolContainer<Name> fileNames;
    }

    class UseDebugClause implements Cobol {
        String words;

        CobolContainer<UseDebugOn> useDebugs;
    }

    class UseDebugOn implements Cobol {
        @Nullable
        String words;

        @Nullable
        Name name;

        @Nullable
        ProcedureName procedureName;
    }

    class ValuedObjectComputerClause implements Cobol {
        Type type;
        String words;

        @Nullable
        Cobol value;

        @Nullable
        CobolLeftPadded<String> units;

        public enum Type {
            Memory,
            Disk,
            SegmentLimit,
            CharacterSet
        }
    }

    class WorkingStorageSection implements DataDivisionSection {
        String words;
        CobolContainer<DataDescriptionEntry> dataDescriptions;
    }

    class Write implements Statement {
        String write;
        QualifiedDataName recordName;

        @Nullable
        WriteFromPhrase writeFromPhrase;

        @Nullable
        WriteAdvancingPhrase writeAdvancingPhrase;

        @Nullable
        StatementPhrase writeAtEndOfPagePhrase;

        @Nullable
        StatementPhrase writeNotAtEndOfPagePhrase;

        @Nullable
        StatementPhrase invalidKeyPhrase;

        @Nullable
        StatementPhrase notInvalidKeyPhrase;

        @Nullable
        CobolLeftPadded<String> endWrite;
    }

    class WriteFromPhrase implements Cobol {
        String from;
        Name name;
    }

    class WriteAdvancingPhrase implements Cobol {
        String words;
        Cobol writeBy;
    }

    class WriteAdvancingPage implements Cobol {
        String page;
    }

    class WriteAdvancingLines implements Cobol {
        Name name;
        String words;
    }

    class WriteAdvancingMnemonic implements Cobol {
        Name name;
    }
}
