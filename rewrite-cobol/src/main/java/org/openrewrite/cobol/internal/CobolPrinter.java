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
    @Override
    public Cobol visitDisplay(Cobol.Display display, PrintOutputCapture<P> pPrintOutputCapture) {
        throw new UnsupportedOperationException("Implement me!");
    }

    @Override
    public Cobol visitDocument(Cobol.CompilationUnit compilationUnit, PrintOutputCapture<P> p) {
        visitSpace(compilationUnit.getPrefix(), p);
        visitMarkers(compilationUnit.getMarkers(), p);
//        visitStatements(document.getPadding().getBody(), p);
        visitSpace(compilationUnit.getEof(), p);
        return compilationUnit;
    }

    @Override
    public Cobol visitEmpty(Cobol.Empty empty, PrintOutputCapture<P> p) {
        visitSpace(empty.getPrefix(), p);
        visitMarkers(empty.getMarkers(), p);
        return empty;
    }

    @Override
    public Cobol visitIdentificationDivision(Cobol.IdentificationDivision identificationDivision, PrintOutputCapture<P> pPrintOutputCapture) {
        throw new UnsupportedOperationException("Implement me!");
    }

    @Override
    public Cobol visitLiteral(Cobol.Literal literal, PrintOutputCapture<P> pPrintOutputCapture) {
        throw new UnsupportedOperationException("Implement me!");
    }

    @Override
    public Cobol visitProcedureDivision(Cobol.ProcedureDivision procedureDivision, PrintOutputCapture<P> pPrintOutputCapture) {
        throw new UnsupportedOperationException("Implement me!");
    }

    @Override
    public Cobol visitProgramUnit(Cobol.ProgramUnit programUnit, PrintOutputCapture<P> pPrintOutputCapture) {
        throw new UnsupportedOperationException("Implement me!");
    }

    @Override
    public Cobol visitStop(Cobol.Stop stop, PrintOutputCapture<P> pPrintOutputCapture) {
        return super.visitStop(stop, pPrintOutputCapture);
    }
}
