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
    public String visitString(String space, PrintOutputCapture<P> p) {
        p.append(space);
        return space;
    }

    @Override
    public <T> CobolLeftPadded<T> visitLeftPadded(@Nullable CobolLeftPadded<T> left, PrintOutputCapture<P> p) {
        if (left != null) {
            p.append(left.getBefore());
            p.append(left.getElement().toString());
        }
        return left;
    }

    @Override
    public <T> CobolRightPadded<T> visitRightPadded(@Nullable CobolRightPadded<T> right, PrintOutputCapture<P> p) {
        if (right != null) {
            p.append(right.getElement().toString());
            p.append(right.getAfter());
        }
        return right;
    }

    public Cobol visitDisplay(Cobol.Display display, PrintOutputCapture<P> p) {
        visitString(display.getPrefix(), p);
        p.append("DISPLAY");
        for (CobolLeftPadded<String> operand : display.getOperands()) {
            visitLeftPadded(operand, p);
        }
        return display;
    }

    @Override
    public Cobol visitDocument(Cobol.CompilationUnit compilationUnit, PrintOutputCapture<P> p) {
        for (Cobol.ProgramUnit pu : compilationUnit.getProgramUnits()) {
            visitProgramUnit(pu, p);
        }
        return compilationUnit;
    }

    public Cobol visitIdentifier(Cobol.Identifier identifier, PrintOutputCapture<P> p) {
        visitString(identifier.getPrefix(), p);
        p.append(identifier.getSimpleName());
        return identifier;
    }

    public Cobol visitLiteral(Cobol.Literal literal, PrintOutputCapture<P> p) {
        visitString(literal.getPrefix(), p);
        return literal;
    }

    public Cobol visitIdentificationDivision(Cobol.IdentificationDivision identificationDivision, PrintOutputCapture<P> p) {
        visitString(identificationDivision.getPrefix(), p);
        visitRightPadded(identificationDivision.getPadding().getIdentification(), p);
        visitRightPadded(identificationDivision.getPadding().getDivision(), p);
        p.append(identificationDivision.getDot());
        visitProgramIdParagraph(identificationDivision.getProgramIdParagraph(), p);
        return identificationDivision;
    }

    public Cobol visitProcedureDivision(Cobol.ProcedureDivision procedureDivision, PrintOutputCapture<P> p) {
        visitString(procedureDivision.getPrefix(), p);
        visitRightPadded(procedureDivision.getProcedure(), p);
        visitRightPadded(procedureDivision.getDivision(), p);
        p.append(procedureDivision.getDot());
        visitProcedureDivisionBody(procedureDivision.getBody(), p);
        return procedureDivision;
    }

    public Cobol visitProcedureDivisionBody(Cobol.ProcedureDivisionBody procedureDivisionBody, PrintOutputCapture<P> p) {
        visitString(procedureDivisionBody.getPrefix(), p);
        visitParagraphs(procedureDivisionBody.getParagraphs(), p);
        return procedureDivisionBody;
    }

    public Cobol visitProgramIdParagraph(Cobol.ProgramIdParagraph programIdParagraph, PrintOutputCapture<P> p) {
        visitString(programIdParagraph.getPrefix(), p);
        visitRightPadded(programIdParagraph.getPadding().getProgramId(), p);
        visitRightPadded(programIdParagraph.getPadding().getDot1(), p);
        p.append(programIdParagraph.getProgramName());
        visitLeftPadded(programIdParagraph.getPadding().getDot2(), p);
        return programIdParagraph;
    }

    public Cobol visitProgramUnit(Cobol.ProgramUnit programUnit, PrintOutputCapture<P> p) {
        visitString(programUnit.getPrefix(), p);
        visitIdentificationDivision(programUnit.getIdentificationDivision(), p);
        if (programUnit.getProcedureDivision() != null) {
            visitProcedureDivision(programUnit.getProcedureDivision(), p);
        }
        return programUnit;
    }

    public Cobol visitStop(Cobol.Stop stop, PrintOutputCapture<P> p) {
        visitString(stop.getPrefix(), p);
        visitString(stop.getStop(), p);
        if (stop.getRun() != null) visitString(stop.getRun(), p);
        return stop;
    }

    public Cobol visitParagraphs(Cobol.Paragraphs paragraphs, PrintOutputCapture<P> p) {
        visitString(paragraphs.getPrefix(), p);
        for (Cobol.Sentence sentence : paragraphs.getSentences()) {
            visitSentence(sentence, p);
        }
        return paragraphs;
    }

    public Cobol visitSentence(Cobol.Sentence sentence, PrintOutputCapture<P> p) {
        visitString(sentence.getPrefix(), p);
        for (Statement statement : sentence.getStatements()) {
            visit(statement, p);
        }
        visitLeftPadded(sentence.getDot(), p);
        return sentence;
    }

}
