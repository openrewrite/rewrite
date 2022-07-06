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
import org.openrewrite.cobol.tree.Cobol;

public class CobolPrinter<P> extends CobolVisitor<PrintOutputCapture<P>> {

    public Cobol visitDisplay(Cobol.Display display, PrintOutputCapture<P> p) {
        visitSpace(display.getPrefix(), p);    // List
        //visitLeftPadded(display.getPadding().getUpon(), p);
        return display;
    }

    @Override
    public Cobol visitDocument(Cobol.CompilationUnit compilationUnit, PrintOutputCapture<P> p) {
        for(Cobol.ProgramUnit pu : compilationUnit.getProgramUnits()) {
            visitProgramUnit(pu, p);
        }
        return compilationUnit;
    }

    public Cobol visitIdentifier(Cobol.Identifier identifier, PrintOutputCapture<P> p) {
        visitSpace(identifier.getPrefix(), p);
        p.append(identifier.getSimpleName());
        return identifier;
    }

    public Cobol visitLiteral(Cobol.Literal literal, PrintOutputCapture<P> p) {
        visitSpace(literal.getPrefix(), p);
        return literal;
    }

    public Cobol visitIdentificationDivision(Cobol.IdentificationDivision identificationDivision, PrintOutputCapture<P> p) {
        visitSpace(identificationDivision.getPrefix(), p);
        Cobol.IdentificationDivision.IdKeyword id = identificationDivision.getIdentification();
        p.append(id == Cobol.IdentificationDivision.IdKeyword.Id ? "ID" : "IDENTIFICATION");
        p.append(identificationDivision.getPadding().getIdentification().getAfter().toString());
        p.append("DIVISION");
        p.append(identificationDivision.getPadding().getDivision().getAfter().toString());
        p.append(".");
        p.append(identificationDivision.getPadding().getDot().getAfter().toString());
        visitProgramIdParagraph(identificationDivision.getProgramIdParagraph(), p);
        return identificationDivision;
    }

    public Cobol visitProcedureDivision(Cobol.ProcedureDivision procedureDivision, PrintOutputCapture<P> p) {
        visitSpace(procedureDivision.getPrefix(), p);
        return procedureDivision;
    }

    public Cobol visitProgramIdParagraph(Cobol.ProgramIdParagraph programIdParagraph, PrintOutputCapture<P> p) {
        visitSpace(programIdParagraph.getPrefix(), p);
        //visitRightPadded(programIdParagraph.getPadding().getProgramId(), p);
        p.append(programIdParagraph.getProgramName());
        return programIdParagraph;
    }

    public Cobol visitProgramUnit(Cobol.ProgramUnit programUnit, PrintOutputCapture<P> p) {
        visitSpace(programUnit.getPrefix(), p);
        visitIdentificationDivision(programUnit.getIdentificationDivision(), p);
        return programUnit;
    }

    public Cobol visitStop(Cobol.Stop stop, PrintOutputCapture<P> p) {
        visitSpace(stop.getPrefix(), p);
        visit(stop.getStatement(), p);
        return stop;
    }

}
