package model;

import lombok.Getter;
import lombok.With;
import org.openrewrite.cobol.tree.CobolLeftPadded;
import org.openrewrite.cobol.tree.CobolRightPadded;
import org.openrewrite.cobol.tree.Space;
import org.openrewrite.internal.lang.Nullable;

import java.util.List;

public interface Cobol {
    class Display implements Statement {
        /**
         * Either an {@link Identifier} or {@link Literal}.
         */
        List<String> operands;
    }

    class Identifier implements Cobol {
        String simpleName;
    }

    class Literal implements Cobol {
        Object value;
        String valueSource;
    }

    class IdentificationDivision implements Cobol {
        CobolRightPadded<IdKeyword> identification;
        CobolRightPadded<Space> division;
        CobolRightPadded<Space> dot;
        ProgramIdParagraph programIdParagraph;

        public enum IdKeyword {
            Identification,
            Id
        }
    }

    class ProcedureDivision implements Cobol {
        Space procedure;
        Space division;
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
        Space dot;
    }

    class ProgramIdParagraph implements Cobol {
        CobolRightPadded<Space> programId;
        CobolRightPadded<Space> dot1;
        String programName;
        CobolLeftPadded<Space> dot2;
    }

    class ProgramUnit implements Cobol {
        IdentificationDivision identificationDivision;

        @Nullable
        ProcedureDivision procedureDivision;
    }

    class Stop implements Statement {
        Space stop;
        Space run;
        Cobol statement;
    }
}
