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
        p.append(space.toString());
        return space;
    }

    @Override
    public <T> CobolLeftPadded<T> visitLeftPadded(CobolLeftPadded<T> left, PrintOutputCapture<P> p) {
        p.append(left.getBefore().toString());
        p.append(left.getElement().toString());
        return left;
    }

    @Override
    public <T> CobolRightPadded<T> visitRightPadded(@Nullable CobolRightPadded<T> right, PrintOutputCapture<P> p) {
        p.append(right.getElement().toString());
        p.append(right.getAfter().toString());
        return right;
    }

    public Cobol visitDisplay(Cobol.Display display, PrintOutputCapture<P> p) {
        visitSpace(display.getPrefix(), p);
        p.append("DISPLAY");
        for(CobolLeftPadded<String> operand : display.getOperands()) {
            visitLeftPadded(operand, p);
        }
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
        visitSpace(identificationDivision.getDot(), p);
        visitProgramIdParagraph(identificationDivision.getProgramIdParagraph(), p);
        return identificationDivision;
    }

    public Cobol visitProcedureDivision(Cobol.ProcedureDivision procedureDivision, PrintOutputCapture<P> p) {
        visitSpace(procedureDivision.getPrefix(), p);
        visitSpace(procedureDivision.getProcedure(), p);
        visitSpace(procedureDivision.getDivision(), p);
        visitProcedureDivisionBody(procedureDivision.getBody(), p);
        return procedureDivision;
    }

    public Cobol visitProcedureDivisionBody(Cobol.ProcedureDivisionBody procedureDivisionBody, PrintOutputCapture<P> p) {
        visitSpace(procedureDivisionBody.getPrefix(), p);
        visitParagraphs(procedureDivisionBody.getParagraphs(), p);
        return procedureDivisionBody;
    }

    public Cobol visitProgramIdParagraph(Cobol.ProgramIdParagraph programIdParagraph, PrintOutputCapture<P> p) {
        visitSpace(programIdParagraph.getPrefix(), p);
        visitRightPadded(programIdParagraph.getPadding().getProgramId(), p);
        visitRightPadded(programIdParagraph.getPadding().getDot1(), p);
        p.append(programIdParagraph.getProgramName());
        visitRightPadded(programIdParagraph.getPadding().getDot1(), p);
        return programIdParagraph;
    }

    public Cobol visitProgramUnit(Cobol.ProgramUnit programUnit, PrintOutputCapture<P> p) {
        visitSpace(programUnit.getPrefix(), p);
        visitIdentificationDivision(programUnit.getIdentificationDivision(), p);
        if(programUnit.getProcedureDivision().isPresent()) {
            visitProcedureDivision(programUnit.getProcedureDivision().get(), p);
        }
        return programUnit;
    }

    public Cobol visitStop(Cobol.Stop stop, PrintOutputCapture<P> p) {
        visitSpace(stop.getPrefix(), p);
        visitSpace(stop.getStop(), p);
        if(stop.getRun() != null) visitSpace(stop.getRun(), p);
        return stop;
    }

    public Cobol visitParagraphs(Cobol.Paragraphs paragraphs, PrintOutputCapture<P> p) {
        visitSpace(paragraphs.getPrefix(), p);
        for(Cobol.Sentence sentence : paragraphs.getSentences()) {
            visitSentence(sentence, p);
        }
        return paragraphs;
    }

    public Cobol visitSentence(Cobol.Sentence sentence, PrintOutputCapture<P> p) {
        visitSpace(sentence.getPrefix(), p);
        for(Statement statement : sentence.getStatements()) {
            visit(statement, p);
        }
        visitSpace(sentence.getDot(), p);
        return sentence;
    }

}
