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

import org.openrewrite.cobol.tree.CobolLeftPadded;
import org.openrewrite.cobol.tree.CobolRightPadded;
import org.openrewrite.internal.lang.Nullable;

import java.util.List;

public interface Cobol {
    class Display implements Statement {
        /**
         * Either an {@link Identifier} or {@link Literal}.
         */
        List<CobolLeftPadded<String>> operands;
    }

    class Identifier implements Cobol {
        String simpleName;
    }

    class Literal implements Cobol {
        Object value;
        String valueSource;
    }

    class IdentificationDivision implements Cobol {
        CobolRightPadded<String> identification;
        CobolRightPadded<String> division;
        String dot;
        ProgramIdParagraph programIdParagraph;
    }

    class ProcedureDivision implements Cobol {
        CobolRightPadded<String> procedure;
        CobolRightPadded<String> division;
        String dot;
        ProcedureDivisionBody body;
    }

    class ProcedureDivisionBody implements Cobol {
        Paragraphs paragraphs;
    }

    class Paragraphs implements Cobol {
        List<Sentence> sentences;
    }

    class Sentence implements Cobol {
        List<Statement> statements;
        CobolLeftPadded<String> dot;
    }

    class ProgramIdParagraph implements Cobol {
        CobolRightPadded<String> programId;
        CobolRightPadded<String> dot1;
        String programName;
        CobolLeftPadded<String> dot2;
    }

    class ProgramUnit implements Cobol {
        IdentificationDivision identificationDivision;

        @Nullable
        ProcedureDivision procedureDivision;
    }

    class Stop implements Statement {
        String stop;
        String run;
        Cobol statement;
    }
}
