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
package org.openrewrite.cobol;

import org.openrewrite.Cursor;
import org.openrewrite.TreeVisitor;
import org.openrewrite.cobol.tree.*;
import org.openrewrite.internal.ListUtils;
import org.openrewrite.internal.lang.Nullable;

import java.util.List;

public class CobolVisitor<P> extends TreeVisitor<Cobol, P> {

    public Cobol visitDocument(Cobol.CompilationUnit compilationUnit, P p) {
        Cobol.CompilationUnit d = compilationUnit;
        d = d.withPrefix(visitSpace(d.getPrefix(), p));
        d = d.withMarkers(visitMarkers(d.getMarkers(), p));
        d = d.getPadding().withProgramUnits(ListUtils.map(d.getPadding().getProgramUnits(), it -> visitRightPadded(it, p)));
        return d;
    }

    public Space visitSpace(Space space, P p) {
        return space;
    }

    public <P2 extends Cobol> CobolContainer<P2> visitContainer(@Nullable CobolContainer<P2> container, P p) {
        if (container == null) {
            //noinspection ConstantConditions
            return null;
        }

        setCursor(new Cursor(getCursor(), container));

        Space before = visitSpace(container.getBefore(), p);
        List<CobolRightPadded<P2>> ps = ListUtils.map(container.getPadding().getElements(), t -> visitRightPadded(t, p));

        setCursor(getCursor().getParent());

        return ps == container.getPadding().getElements() && before == container.getBefore() ?
                container :
                CobolContainer.build(before, ps, container.getMarkers());
    }

    public <T> CobolLeftPadded<T> visitLeftPadded(CobolLeftPadded<T> left, P p) {
        setCursor(new Cursor(getCursor(), left));

        Space before = visitSpace(left.getBefore(), p);
        T t = left.getElement();

        if (t instanceof Cobol) {
            //noinspection unchecked
            t = visitAndCast((Cobol) left.getElement(), p);
        }

        setCursor(getCursor().getParent());
        if (t == null) {
            //noinspection ConstantConditions
            return null;
        }

        return (before == left.getBefore() && t == left.getElement()) ? left : new CobolLeftPadded<>(before, t, left.getMarkers());
    }

    @SuppressWarnings("ConstantConditions")
    public <T> CobolRightPadded<T> visitRightPadded(@Nullable CobolRightPadded<T> right, P p) {
        if (right == null) {
            //noinspection ConstantConditions
            return null;
        }

        setCursor(new Cursor(getCursor(), right));

        T t = right.getElement();
        if (t instanceof Cobol) {
            //noinspection unchecked
            t = (T) visit((Cobol) right.getElement(), p);
        }

        setCursor(getCursor().getParent());
        if (t == null) {
            //noinspection ConstantConditions
            return null;
        }
        Space after = visitSpace(right.getAfter(), p);
        return (after == right.getAfter() && t == right.getElement()) ? right : new CobolRightPadded<>(t, after, right.getMarkers());
    }

    public Cobol visitDataDivision(Cobol.DataDivision dataDivision, P p) {
        Cobol.DataDivision d = dataDivision;
        d = d.withPrefix(visitSpace(d.getPrefix(), p));
        d = d.withMarkers(visitMarkers(d.getMarkers(), p));
        d = d.getPadding().withDivision(visitLeftPadded(d.getPadding().getDivision(), p));
        d = d.getPadding().withSections(visitContainer(d.getPadding().getSections(), p));
        return d;
    }

    public Cobol visitDataDescriptionEntry(Cobol.DataDescriptionEntry dataDescriptionEntry, P p) {
        Cobol.DataDescriptionEntry d = dataDescriptionEntry;
        d = d.withPrefix(visitSpace(d.getPrefix(), p));
        d = d.withMarkers(visitMarkers(d.getMarkers(), p));
        if (d.getPadding().getName() != null) {
            d = d.getPadding().withName(visitLeftPadded(d.getPadding().getName(), p));
        }
        d = d.getPadding().withClauses(visitContainer(d.getPadding().getClauses(), p));
        return d;
    }

    public Cobol visitDataPictureClause(Cobol.DataPictureClause dataPictureClause, P p) {
        Cobol.DataPictureClause d = dataPictureClause;
        d = d.withPrefix(visitSpace(d.getPrefix(), p));
        d = d.withMarkers(visitMarkers(d.getMarkers(), p));
        if (d.getPadding().getIs() != null) {
            d = d.getPadding().withIs(visitLeftPadded(d.getPadding().getIs(), p));
        }
        d = d.getPadding().withPictures(visitContainer(d.getPadding().getPictures(), p));
        return d;
    }

    public Cobol visitPicture(Cobol.Picture picture, P p) {
        Cobol.Picture pp = picture;
        pp = pp.withPrefix(visitSpace(pp.getPrefix(), p));
        pp = pp.withMarkers(visitMarkers(pp.getMarkers(), p));
        if (pp.getPadding().getCardinalitySource() != null) {
            pp = pp.getPadding().withCardinalitySource(visitLeftPadded(pp.getPadding().getCardinalitySource(), p));
        }
        return pp;
    }

    public Cobol visitDisplay(Cobol.Display display, P p) {
        Cobol.Display d = display;
        d = d.withPrefix(visitSpace(d.getPrefix(), p));
        d = d.withMarkers(visitMarkers(d.getMarkers(), p));
        d = d.withOperands(ListUtils.map(d.getOperands(), t -> (Name) visit(t, p)));
        return d;
    }

    public Cobol visitEndProgram(Cobol.EndProgram endProgram, P p) {
        Cobol.EndProgram e = endProgram;
        e = e.withPrefix(visitSpace(e.getPrefix(), p));
        e = e.withMarkers(visitMarkers(e.getMarkers(), p));
        e = e.getPadding().withProgram(visitLeftPadded(e.getPadding().getProgram(), p));
        return e;
    }

    public Cobol visitEnvironmentDivision(Cobol.EnvironmentDivision environmentDivision, P p) {
        Cobol.EnvironmentDivision e = environmentDivision;
        e = e.withPrefix(visitSpace(e.getPrefix(), p));
        e = e.withMarkers(visitMarkers(e.getMarkers(), p));
        e = e.getPadding().withDivision(visitLeftPadded(e.getPadding().getDivision(), p));
        e = e.getPadding().withBody(visitContainer(e.getPadding().getBody(), p));
        return e;
    }

    public Cobol visitIdentifier(Cobol.Identifier identifier, P p) {
        Cobol.Identifier i = identifier;
        i = i.withPrefix(visitSpace(i.getPrefix(), p));
        i = i.withMarkers(visitMarkers(i.getMarkers(), p));
        return i;
    }

    public Cobol visitLiteral(Cobol.Literal literal, P p) {
        Cobol.Literal l = literal;
        l = l.withPrefix(visitSpace(l.getPrefix(), p));
        l = l.withMarkers(visitMarkers(l.getMarkers(), p));
        return l;
    }

    public Cobol visitIdentificationDivision(Cobol.IdentificationDivision identificationDivision, P p) {
        Cobol.IdentificationDivision i = identificationDivision;
        i = i.withPrefix(visitSpace(i.getPrefix(), p));
        i = i.withMarkers(visitMarkers(i.getMarkers(), p));
        i = i.getPadding().withDivision(visitLeftPadded(i.getPadding().getDivision(), p));
        i = i.getPadding().withProgramIdParagraph(visitLeftPadded(i.getPadding().getProgramIdParagraph(), p));
        return i;
    }

    public Cobol visitProcedureDivision(Cobol.ProcedureDivision procedureDivision, P p) {
        Cobol.ProcedureDivision pp = procedureDivision;
        pp = pp.withPrefix(visitSpace(pp.getPrefix(), p));
        pp = pp.withMarkers(visitMarkers(pp.getMarkers(), p));
        pp = pp.getPadding().withDivision(visitLeftPadded(pp.getPadding().getDivision(), p));
        pp = pp.getPadding().withBody(visitLeftPadded(pp.getPadding().getBody(), p));
        return pp;
    }

    public Cobol visitProcedureDivisionBody(Cobol.ProcedureDivisionBody procedureDivisionBody, P p) {
        Cobol.ProcedureDivisionBody pp = procedureDivisionBody;
        pp = pp.withPrefix(visitSpace(pp.getPrefix(), p));
        pp = pp.withMarkers(visitMarkers(pp.getMarkers(), p));
        pp = pp.withParagraphs((Cobol.Paragraphs) visit(pp.getParagraphs(), p));
        return pp;
    }

    public Cobol visitParagraphs(Cobol.Paragraphs paragraphs, P p) {
        Cobol.Paragraphs pp = paragraphs;
        pp = pp.withPrefix(visitSpace(pp.getPrefix(), p));
        pp = pp.withMarkers(visitMarkers(pp.getMarkers(), p));
        pp = pp.getPadding().withSentences(visitContainer(pp.getPadding().getSentences(), p));
        return pp;
    }

    public Cobol visitSentence(Cobol.Sentence sentence, P p) {
        Cobol.Sentence s = sentence;
        s = s.withPrefix(visitSpace(s.getPrefix(), p));
        s = s.withMarkers(visitMarkers(s.getMarkers(), p));
        s = s.withStatements(ListUtils.map(s.getStatements(), t -> (Statement) visit(t, p)));
        return s;
    }

    public Cobol visitProgramIdParagraph(Cobol.ProgramIdParagraph programIdParagraph, P p) {
        Cobol.ProgramIdParagraph pp = programIdParagraph;
        pp = pp.withPrefix(visitSpace(pp.getPrefix(), p));
        pp = pp.withMarkers(visitMarkers(pp.getMarkers(), p));
        pp = pp.getPadding().withProgramName(visitLeftPadded(pp.getPadding().getProgramName(), p));
        return pp;
    }

    public Cobol visitProgramUnit(Cobol.ProgramUnit programUnit, P p) {
        Cobol.ProgramUnit pp = programUnit;
        pp = pp.withPrefix(visitSpace(pp.getPrefix(), p));
        pp = pp.withMarkers(visitMarkers(pp.getMarkers(), p));
        pp = pp.withIdentificationDivision((Cobol.IdentificationDivision) visit(pp.getIdentificationDivision(), p));
        pp = pp.withEnvironmentDivision((Cobol.EnvironmentDivision) visit(pp.getEnvironmentDivision(), p));
        pp = pp.withDataDivision((Cobol.DataDivision) visit(pp.getDataDivision(), p));
        pp = pp.withProcedureDivision((Cobol.ProcedureDivision) visit(pp.getProcedureDivision(), p));
        pp = pp.getPadding().withProgramUnits(visitContainer(pp.getPadding().getProgramUnits(), p));
        if (pp.getPadding().getEndProgram() != null) {
            pp = pp.getPadding().withEndProgram(visitRightPadded(pp.getPadding().getEndProgram(), p));
        }
        return pp;
    }

    public Cobol visitStop(Cobol.Stop stop, P p) {
        Cobol.Stop s = stop;
        s = s.withPrefix(visitSpace(s.getPrefix(), p));
        s = s.withMarkers(visitMarkers(s.getMarkers(), p));
        s = s.getPadding().withRun(visitLeftPadded(s.getPadding().getRun(), p));
        s = s.withStatement((Cobol) visit(s.getStatement(), p));
        return s;
    }

    public Cobol visitWorkingStorageSection(Cobol.WorkingStorageSection workingStorageSection, P p) {
        Cobol.WorkingStorageSection w = workingStorageSection;
        w = w.withPrefix(visitSpace(w.getPrefix(), p));
        w = w.withMarkers(visitMarkers(w.getMarkers(), p));
        w = w.getPadding().withSection(visitLeftPadded(w.getPadding().getSection(), p));
        w = w.getPadding().withDataDescriptions(visitContainer(w.getPadding().getDataDescriptions(), p));
        return w;
    }
}
