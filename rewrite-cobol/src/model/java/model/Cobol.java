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
        CobolContainer<Name> to;

        @Nullable
        CobolContainer<Name> giving;
    }

    class DataDivision implements Statement {
        String words;
        CobolContainer<DataDivisionSection> sections;
    }

    class DataDescriptionEntry implements Cobol {
        Integer level;

        @Nullable
        CobolLeftPadded<String> name;

        CobolContainer<Cobol> clauses;
    }

    class DataPictureClause implements Cobol {
        String pic;

        @Nullable
        CobolLeftPadded<String> is;

        CobolContainer<Picture> pictures;
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

    class IdentificationDivision implements Cobol {
        String words;
        CobolLeftPadded<ProgramIdParagraph> programIdParagraph;
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

    class Sentence implements Cobol {
        List<Statement> statements;
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

    class StatementPhrase implements Cobol {
        String phrase;
        CobolContainer<Statement> statement;
    }

    class Stop implements Statement {
        String words;
        Cobol statement;
    }

    class WorkingStorageSection implements DataDivisionSection {
        String words;
        CobolContainer<DataDescriptionEntry> dataDescriptions;
    }
}
