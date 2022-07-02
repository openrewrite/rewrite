package model.openrewrite.cobol;

import org.openrewrite.cobol.tree.CobolLeftPadded;
import org.openrewrite.cobol.tree.CobolRightPadded;
import org.openrewrite.cobol.tree.DottedKeyword;
import org.openrewrite.cobol.tree.Space;
import org.openrewrite.internal.lang.Nullable;

import java.util.List;

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
        CobolRightPadded<IdKeyword> identification;
        DottedKeyword division;

        public enum IdKeyword {
            Identification,
            Id
        }
    }

    class ProcedureDivision implements Cobol {
    }

    class ProgramIdParagraph implements Cobol {
        DottedKeyword programId;
        Identifier programName;
    }

    class ProgramUnit implements Cobol {
    }

    class Stop implements Statement {
        Space stop;
        Cobol statement;
    }
}
