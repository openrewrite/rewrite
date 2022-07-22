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

    class OnExceptionClause implements Statement {
        String words;
        CobolContainer<Statement> statements;
    }

    class NotOnExceptionClause implements Statement {
        String words;
        CobolContainer<Statement> statements;
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

    class InSection implements Cobol {
        String words;
        Name name;
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

    class Display implements Statement {
        String display;
        List<Name> operands;
    }

    class EndProgram implements Statement {
        String words;
        Name programName;
    }

    class EnvironmentDivision implements Cobol {
        String words;
        CobolContainer<Cobol> body;
    }

    class FileSection implements DataDivisionSection {
        String words;

        CobolContainer<FileDescriptionEntry> fileDescriptionEntry;
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
        CobolLeftPadded<ProcedureDivisionBody> body;
    }

    class ProcedureDivisionBody implements Cobol {
        Paragraphs paragraphs;
    }

    class Paragraphs implements Cobol {
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

    class ReserveNetworkClause implements Cobol {
        String words;
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
}
