package model;

import org.openrewrite.cobol.tree.CobolLeftPadded;
import org.openrewrite.cobol.tree.CobolRightPadded;
import org.openrewrite.cobol.tree.Space;
import org.openrewrite.internal.lang.Nullable;

import java.util.List;
import java.util.Optional;

public interface Cobol {
    class Display implements Statement {
        /**
         * Either an {@link Identifier} or {@link Literal}.
         */
        List<Cobol> operands;

        @Nullable
        CobolLeftPadded<Identifier> upon;
    }

    class Identifier implements Cobol {
        String simpleName;
    }

    class Literal implements Cobol {
        Object value;
        String valueSource;
    }

    class IdentificationDivision implements Cobol {

        // identificationDivision
        //   : (IDENTIFICATION | ID) DIVISION DOT_FS programIdParagraph identificationDivisionBody*
        //   ;

        CobolRightPadded<IdKeyword> identification;
        CobolRightPadded<Space> division;
        CobolRightPadded<Space> dot;

        public enum IdKeyword {
            Identification,
            Id
        }

        ProgramIdParagraph programIdParagraph;
    }

    class ProcedureDivision implements Cobol {
        // procedureDivision
        //   : PROCEDURE DIVISION procedureDivisionUsingClause? procedureDivisionGivingClause? DOT_FS procedureDeclaratives? procedureDivisionBody
        //   ;
        ProcedureDivisionBody body;
    }

    class ProcedureDivisionBody implements Cobol {
        Paragraphs paragraphs;
    }

    class Paragraphs implements Cobol {
        // paragraphs
        //   : sentence* paragraph*
        //   ;
        List<Sentence> sentences;
    }

    class Sentence implements Cobol {
        // sentence
        //   : statement* DOT_FS
        //   ;
        List<Statement> statements;
    }

    class ProgramIdParagraph implements Cobol {
        CobolRightPadded<Space> programId;
        String programName;
    }

    class ProgramUnit implements Cobol {
        // programUnit
        //   : identificationDivision environmentDivision? dataDivision? procedureDivision? programUnit* endProgramStatement?
        //   ;
        IdentificationDivision identificationDivision;
        Optional<ProcedureDivision> procedureDivision;
    }

    class Stop implements Statement {
        Space stop;
        Cobol statement;
    }
}
