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

import java.util.List;

public class CobolPrinter<P> extends CobolVisitor<PrintOutputCapture<P>> {

    @Override
    public Space visitSpace(Space space, PrintOutputCapture<P> p) {
        p.append(space.getWhitespace());
        return space;
    }

    protected void visitLeftPadded(@Nullable String prefix, @Nullable CobolLeftPadded<?> leftPadded, PrintOutputCapture<P> p) {
        if (leftPadded != null) {
            visitSpace(leftPadded.getBefore(), p);
            if (prefix != null) {
                p.append(prefix);
            }
            if (leftPadded.getElement() instanceof Cobol) {
                visit((Cobol) leftPadded.getElement(), p);
            } else if (leftPadded.getElement() instanceof String) {
                p.append(((String) leftPadded.getElement()));
            }
        }
    }

    protected void visitRightPadded(@Nullable CobolRightPadded<?> rightPadded, @Nullable String suffix, PrintOutputCapture<P> p) {
        if (rightPadded != null) {
            if (rightPadded.getElement() instanceof Cobol) {
                visit((Cobol) rightPadded.getElement(), p);
            } else if (rightPadded.getElement() instanceof String) {
                p.append(((String) rightPadded.getElement()));
            }
            visitSpace(rightPadded.getAfter(), p);
            if (suffix != null) {
                p.append(suffix);
            }
        }
    }

    protected void visitRightPadded(List<? extends CobolRightPadded<? extends Cobol>> nodes, String suffixBetween, PrintOutputCapture<P> p) {
        for (int i = 0; i < nodes.size(); i++) {
            CobolRightPadded<? extends Cobol> node = nodes.get(i);
            visit(node.getElement(), p);
            visitSpace(node.getAfter(), p);
            visitMarkers(node.getMarkers(), p);
            if (i < nodes.size() - 1) {
                p.append(suffixBetween);
            }
        }
    }

    @SuppressWarnings("SameParameterValue")
    protected void visitContainer(String before, @Nullable CobolContainer<? extends Cobol> container,
                                  String suffixBetween, @Nullable String after, PrintOutputCapture<P> p) {
        if (container == null) {
            return;
        }
        visitSpace(container.getBefore(), p);
        p.append(before);
        visitRightPadded(container.getPadding().getElements(), suffixBetween, p);
        p.append(after == null ? "" : after);
    }

    @Override
    public Cobol visitDocument(Cobol.CompilationUnit compilationUnit, PrintOutputCapture<P> p) {
        visitSpace(compilationUnit.getPrefix(), p);
        visitMarkers(compilationUnit.getMarkers(), p);
        for (Cobol.ProgramUnit programUnit : compilationUnit.getProgramUnits()) {
            visit(programUnit, p);
        }
        return compilationUnit;
    }

    public Cobol visitDisplay(Cobol.Display display, PrintOutputCapture<P> p) {
        visitSpace(display.getPrefix(), p);
        visitMarkers(display.getMarkers(), p);
        p.append(display.getDisplay());
        for (Name d : display.getOperands()) {
            visit(d, p);
        }
        return display;
    }

    public Cobol visitIdentifier(Cobol.Identifier identifier, PrintOutputCapture<P> p) {
        visitSpace(identifier.getPrefix(), p);
        visitMarkers(identifier.getMarkers(), p);
        p.append(identifier.getSimpleName());
        return identifier;
    }

    public Cobol visitLiteral(Cobol.Literal literal, PrintOutputCapture<P> p) {
        visitSpace(literal.getPrefix(), p);
        visitMarkers(literal.getMarkers(), p);
        p.append(literal.getValueSource());
        return literal;
    }

    public Cobol visitIdentificationDivision(Cobol.IdentificationDivision identificationDivision, PrintOutputCapture<P> p) {
        visitSpace(identificationDivision.getPrefix(), p);
        visitMarkers(identificationDivision.getMarkers(), p);
        p.append(identificationDivision.getIdentification());
        visitLeftPadded("", identificationDivision.getPadding().getDivision(), p);
        visitLeftPadded(".", identificationDivision.getPadding().getProgramIdParagraph(), p);
        return identificationDivision;
    }

    public Cobol visitProcedureDivision(Cobol.ProcedureDivision procedureDivision, PrintOutputCapture<P> p) {
        visitSpace(procedureDivision.getPrefix(), p);
        visitMarkers(procedureDivision.getMarkers(), p);
        p.append(procedureDivision.getProcedure());
        visitLeftPadded("", procedureDivision.getPadding().getDivision(), p);
        visitLeftPadded(".", procedureDivision.getPadding().getBody(), p);
        return procedureDivision;
    }

    public Cobol visitProcedureDivisionBody(Cobol.ProcedureDivisionBody procedureDivisionBody, PrintOutputCapture<P> p) {
        visitSpace(procedureDivisionBody.getPrefix(), p);
        visitMarkers(procedureDivisionBody.getMarkers(), p);
        visit(procedureDivisionBody.getParagraphs(), p);
        return procedureDivisionBody;
    }

    public Cobol visitProgramIdParagraph(Cobol.ProgramIdParagraph programIdParagraph, PrintOutputCapture<P> p) {
        visitSpace(programIdParagraph.getPrefix(), p);
        visitMarkers(programIdParagraph.getMarkers(), p);
        p.append(programIdParagraph.getProgramId());
        visitLeftPadded(".", programIdParagraph.getPadding().getProgramName(), p);
        return programIdParagraph;
    }

    public Cobol visitStop(Cobol.Stop stop, PrintOutputCapture<P> p) {
        visitSpace(stop.getPrefix(), p);
        visitMarkers(stop.getMarkers(), p);
        p.append(stop.getStop());
        visitLeftPadded("", stop.getPadding().getRun(), p);
        visit(stop.getStatement(), p);
        return stop;
    }

    public Cobol visitParagraphs(Cobol.Paragraphs paragraphs, PrintOutputCapture<P> p) {
        visitSpace(paragraphs.getPrefix(), p);
        visitMarkers(paragraphs.getMarkers(), p);
        visitContainer("", paragraphs.getPadding().getSentences(), ".", ".", p);
        return paragraphs;
    }

    public Cobol visitSentence(Cobol.Sentence sentence, PrintOutputCapture<P> p) {
        visitSpace(sentence.getPrefix(), p);
        visitMarkers(sentence.getMarkers(), p);
        for (Statement s : sentence.getStatements()) {
            visit(s, p);
        }
        return sentence;
    }

    public Cobol visitDataDivision(Cobol.DataDivision dataDivision, PrintOutputCapture<P> p) {
        visitSpace(dataDivision.getPrefix(), p);
        visitMarkers(dataDivision.getMarkers(), p);
        p.append(dataDivision.getData());
        visitLeftPadded("", dataDivision.getPadding().getDivision(), p);
        visitContainer("", dataDivision.getPadding().getSections(), " ", "", p);
        return dataDivision;
    }

    public Cobol visitDataPictureClause(Cobol.DataPictureClause dataPictureClause, PrintOutputCapture<P> p) {
        visitSpace(dataPictureClause.getPrefix(), p);
        visitMarkers(dataPictureClause.getMarkers(), p);
        p.append(dataPictureClause.getPic());
        visitLeftPadded("", dataPictureClause.getPadding().getIs(), p);
        visitContainer("", dataPictureClause.getPadding().getPictures(), " ", "", p);
        return dataPictureClause;
    }

    public Cobol visitWorkingStorageSection(Cobol.WorkingStorageSection workingStorageSection, PrintOutputCapture<P> p) {
        visitSpace(workingStorageSection.getPrefix(), p);
        visitMarkers(workingStorageSection.getMarkers(), p);
        p.append(workingStorageSection.getWorkingStorage());
        visitLeftPadded("", workingStorageSection.getPadding().getSection(), p);
        visitContainer("", workingStorageSection.getPadding().getDataDescriptions(), " ", "", p);
        return workingStorageSection;
    }

    public Cobol visitPicture(Cobol.Picture picture, PrintOutputCapture<P> p) {
        visitSpace(picture.getPrefix(), p);
        visitMarkers(picture.getMarkers(), p);
        p.append(picture.getChars());
        visitLeftPadded("", picture.getPadding().getCardinalitySource(), p);
        return picture;
    }

    public Cobol visitDataDescriptionEntry(Cobol.DataDescriptionEntry dataDescriptionEntry, PrintOutputCapture<P> p) {
        visitSpace(dataDescriptionEntry.getPrefix(), p);
        visitMarkers(dataDescriptionEntry.getMarkers(), p);
        p.append(dataDescriptionEntry.getLevel().toString());
        visitLeftPadded("", dataDescriptionEntry.getPadding().getName(), p);
        visitContainer("", dataDescriptionEntry.getPadding().getClauses(), ".", ".", p);
        return dataDescriptionEntry;
    }

    public Cobol visitEnvironmentDivision(Cobol.EnvironmentDivision environmentDivision, PrintOutputCapture<P> p) {
        visitSpace(environmentDivision.getPrefix(), p);
        visitMarkers(environmentDivision.getMarkers(), p);
        p.append(environmentDivision.getEnvironment());
        visitLeftPadded("", environmentDivision.getPadding().getDivision(), p);
        visitContainer("", environmentDivision.getPadding().getBody(), " ", "", p);
        return environmentDivision;
    }

    public Cobol visitProgramUnit(Cobol.ProgramUnit programUnit, PrintOutputCapture<P> p) {
        visitSpace(programUnit.getPrefix(), p);
        visitMarkers(programUnit.getMarkers(), p);
        visit(programUnit.getIdentificationDivision(), p);
        visit(programUnit.getDataDivision(), p);
        visit(programUnit.getEnvironmentDivision(), p);
        visit(programUnit.getProcedureDivision(), p);
        visitContainer("", programUnit.getPadding().getProgramUnits(), " ", "", p);
        visitRightPadded(programUnit.getPadding().getEndProgram(), ".", p);
        return programUnit;
    }

    public Cobol visitEndProgram(Cobol.EndProgram endProgram, PrintOutputCapture<P> p) {
        visitSpace(endProgram.getPrefix(), p);
        visitMarkers(endProgram.getMarkers(), p);
        p.append(endProgram.getEnd());
        visitLeftPadded("", endProgram.getPadding().getProgram(), p);
        return endProgram;
    }

    public Cobol visitSetTo(Cobol.SetTo setTo, PrintOutputCapture<P> p) {
        visitSpace(setTo.getPrefix(), p);
        visitMarkers(setTo.getMarkers(), p);
        visitContainer("", setTo.getPadding().getTo(), "", "", p);
        visitContainer("TO", setTo.getPadding().getValues(), "", "", p);
        return setTo;
    }

    public Cobol visitSetUpDown(Cobol.SetUpDown setUpDown, PrintOutputCapture<P> p) {
        visitSpace(setUpDown.getPrefix(), p);
        visitMarkers(setUpDown.getMarkers(), p);
        visitContainer("", setUpDown.getPadding().getTo(), "", "", p);
        visitLeftPadded("", setUpDown.getPadding().getOperation(), p);
        return setUpDown;
    }

    public Cobol visitSet(Cobol.Set set, PrintOutputCapture<P> p) {
        visitSpace(set.getPrefix(), p);
        visitMarkers(set.getMarkers(), p);
        p.append(set.getSet());
        visitContainer("", set.getPadding().getTo(), "", "", p);
        visit(set.getUpDown(), p);
        return set;
    }
}
