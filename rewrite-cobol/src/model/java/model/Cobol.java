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
import org.openrewrite.internal.lang.Nullable;

import java.util.List;

public interface Cobol {
    class Display implements Statement {
        List<Name> operands;
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
        String identification;
        CobolLeftPadded<String> division;
        CobolLeftPadded<ProgramIdParagraph> programIdParagraph;
    }

    class ProcedureDivision implements Cobol {
        String procedure;
        CobolLeftPadded<String> division;
        CobolLeftPadded<ProcedureDivisionBody> body;
    }

    class ProcedureDivisionBody implements Cobol {
        Paragraphs paragraphs;
    }

    class Paragraphs implements Cobol {
        List<Sentence> sentences;
    }

    class Sentence implements Cobol {
        List<Statement> statements;
    }

    class ProgramIdParagraph implements Cobol {
        String programId;
        CobolLeftPadded<Name> programName;
    }

    class ProgramUnit implements Cobol {
        IdentificationDivision identificationDivision;

        @Nullable
        ProcedureDivision procedureDivision;
    }

    class Stop implements Statement {
        String stop;
        CobolLeftPadded<String> run;
        Cobol statement;
    }
}
