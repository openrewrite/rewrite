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

import org.openrewrite.PrintOutputCapture;
import org.openrewrite.cobol.CobolVisitor;
import org.openrewrite.cobol.tree.*;
import org.openrewrite.internal.lang.Nullable;

public class CobolPrinter<P> extends CobolVisitor<PrintOutputCapture<P>> {

    @Override
    public Space visitSpace(Space space, PrintOutputCapture<P> p) {
        p.append(space.getWhitespace());
        return space;
    }

    @Override
    public <T> CobolLeftPadded<T> visitLeftPadded(@Nullable CobolLeftPadded<T> left, PrintOutputCapture<P> p) {
        if (left != null) {
            p.append(left.getBefore().getWhitespace());
            p.append(left.getElement().toString());
        }
        //noinspection ConstantConditions
        return left;
    }

    @Override
    public <T> CobolRightPadded<T> visitRightPadded(@Nullable CobolRightPadded<T> right, PrintOutputCapture<P> p) {
        if (right != null) {
            p.append(right.getElement().toString());
            p.append(right.getAfter().getWhitespace());
        }
        //noinspection ConstantConditions
        return right;
    }

    public Cobol visitIdentifier(Cobol.Identifier identifier, PrintOutputCapture<P> p) {
        visitSpace(identifier.getPrefix(), p);
        return identifier;
    }

    public Cobol visitLiteral(Cobol.Literal literal, PrintOutputCapture<P> p) {
        visitSpace(literal.getPrefix(), p);
        return literal;
    }

    public Cobol visitIdentificationDivision(Cobol.IdentificationDivision identificationDivision, PrintOutputCapture<P> p) {
        visitSpace(identificationDivision.getPrefix(), p);
        visitLeftPadded(identificationDivision.getPadding().getDivision(), p);
        visitLeftPadded(identificationDivision.getPadding().getProgramIdParagraph(), p);
        return identificationDivision;
    }

    public Cobol visitProcedureDivision(Cobol.ProcedureDivision procedureDivision, PrintOutputCapture<P> p) {
        visitSpace(procedureDivision.getPrefix(), p);
        visitLeftPadded(procedureDivision.getPadding().getDivision(), p);
        visitLeftPadded(procedureDivision.getPadding().getBody(), p);
        return procedureDivision;
    }

    public Cobol visitProcedureDivisionBody(Cobol.ProcedureDivisionBody procedureDivisionBody, PrintOutputCapture<P> p) {
        visitSpace(procedureDivisionBody.getPrefix(), p);
        visit(procedureDivisionBody.getParagraphs(), p);
        return procedureDivisionBody;
    }

    public Cobol visitProgramIdParagraph(Cobol.ProgramIdParagraph programIdParagraph, PrintOutputCapture<P> p) {
        visitSpace(programIdParagraph.getPrefix(), p);
        visitLeftPadded(programIdParagraph.getPadding().getProgramName(), p);
        return programIdParagraph;
    }

    public Cobol visitProgramUnit(Cobol.ProgramUnit programUnit, PrintOutputCapture<P> p) {
        visitSpace(programUnit.getPrefix(), p);
        visit(programUnit.getIdentificationDivision(), p);
        visit(programUnit.getProcedureDivision(), p);
        return programUnit;
    }

    public Cobol visitStop(Cobol.Stop stop, PrintOutputCapture<P> p) {
        visitSpace(stop.getPrefix(), p);
        visitLeftPadded(stop.getPadding().getRun(), p);
        visit(stop.getStatement(), p);
        return stop;
    }
}
