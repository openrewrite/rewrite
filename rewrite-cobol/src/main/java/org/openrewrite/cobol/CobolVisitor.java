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
import org.openrewrite.marker.Markers;

import java.util.List;

public class CobolVisitor<P> extends TreeVisitor<Cobol, P> {

    public Cobol visitCompilationUnit(Cobol.CompilationUnit compilationUnit, P p) {
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
        CobolLeftPadded<String> preposition = visitLeftPadded(container.getPreposition(), p);
        List<CobolRightPadded<P2>> ps = ListUtils.map(container.getPadding().getElements(), t -> visitRightPadded(t, p));
        Markers markers = visitMarkers(container.getMarkers(), p);

        setCursor(getCursor().getParent());

        return (ps == container.getPadding().getElements() && before == container.getBefore() && preposition == container.getPreposition() &&
                markers == container.getMarkers()) ?
                container :
                CobolContainer.build(before, preposition, ps, markers);
    }

    public <T> CobolLeftPadded<T> visitLeftPadded(@Nullable CobolLeftPadded<T> left, P p) {
        if (left == null) {
            //noinspection ConstantConditions
            return null;
        }

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

    public Cobol visitAdd(Cobol.Add add, P p) {
        Cobol.Add a = add;
        a = a.withPrefix(visitSpace(a.getPrefix(), p));
        a = a.withMarkers(visitMarkers(a.getMarkers(), p));
        a = a.withOperation(visit(a.getOperation(), p));
        a = a.withOnSizeError((Cobol.StatementPhrase) visit(a.getOnSizeError(), p));
        if (a.getPadding().getEndAdd() != null) {
            a = a.getPadding().withEndAdd(visitLeftPadded(a.getPadding().getEndAdd(), p));
        }
        return a;
    }

    public Cobol visitAddTo(Cobol.AddTo addTo, P p) {
        Cobol.AddTo a = addTo;
        a = a.withPrefix(visitSpace(a.getPrefix(), p));
        a = a.withMarkers(visitMarkers(a.getMarkers(), p));
        a = a.getPadding().withFrom(visitContainer(a.getPadding().getFrom(), p));
        a = a.getPadding().withTo(visitContainer(a.getPadding().getTo(), p));
        a = a.getPadding().withGiving(visitContainer(a.getPadding().getGiving(), p));
        return a;
    }

    public Cobol visitConfigurationSection(Cobol.ConfigurationSection configurationSection, P p) {
        Cobol.ConfigurationSection c = configurationSection;
        c = c.withPrefix(visitSpace(c.getPrefix(), p));
        c = c.withMarkers(visitMarkers(c.getMarkers(), p));
        c = c.getPadding().withParagraphs(visitContainer(c.getPadding().getParagraphs(), p));
        return c;
    }

    public Cobol visitDataDivision(Cobol.DataDivision dataDivision, P p) {
        Cobol.DataDivision d = dataDivision;
        d = d.withPrefix(visitSpace(d.getPrefix(), p));
        d = d.withMarkers(visitMarkers(d.getMarkers(), p));
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
        return e;
    }

    public Cobol visitEnvironmentDivision(Cobol.EnvironmentDivision environmentDivision, P p) {
        Cobol.EnvironmentDivision e = environmentDivision;
        e = e.withPrefix(visitSpace(e.getPrefix(), p));
        e = e.withMarkers(visitMarkers(e.getMarkers(), p));
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
        i = i.getPadding().withProgramIdParagraph(visitLeftPadded(i.getPadding().getProgramIdParagraph(), p));
        return i;
    }

    public Cobol visitProcedureDivision(Cobol.ProcedureDivision procedureDivision, P p) {
        Cobol.ProcedureDivision pp = procedureDivision;
        pp = pp.withPrefix(visitSpace(pp.getPrefix(), p));
        pp = pp.withMarkers(visitMarkers(pp.getMarkers(), p));
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

    public Cobol visitRoundable(Cobol.Roundable roundable, P p) {
        Cobol.Roundable r = roundable;
        r = r.withPrefix(visitSpace(r.getPrefix(), p));
        r = r.withMarkers(visitMarkers(r.getMarkers(), p));
        r = r.withIdentifier((Cobol.Identifier) visit(r.getIdentifier(), p));
        if (r.getPadding().getRounded() != null) {
            r = r.getPadding().withRounded(visitLeftPadded(r.getPadding().getRounded(), p));
        }
        return r;
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
        if (pp.getPadding().getProgramAttributes() != null) {
            pp = pp.getPadding().withProgramAttributes(visitLeftPadded(pp.getPadding().getProgramAttributes(), p));
        }
        if (pp.getPadding().getDot() != null) {
            pp = pp.getPadding().withDot(visitLeftPadded(pp.getPadding().getDot(), p));
        }
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

    public Cobol visitSet(Cobol.Set set, P p) {
        Cobol.Set s = set;
        s = s.withPrefix(visitSpace(s.getPrefix(), p));
        s = s.withMarkers(visitMarkers(s.getMarkers(), p));
        s = s.getPadding().withTo(visitContainer(s.getPadding().getTo(), p));
        s = s.withUpDown((Cobol.SetUpDown) visit(s.getUpDown(), p));
        return s;
    }

    public Cobol visitSetTo(Cobol.SetTo setTo, P p) {
        Cobol.SetTo s = setTo;
        s = s.withPrefix(visitSpace(s.getPrefix(), p));
        s = s.withMarkers(visitMarkers(s.getMarkers(), p));
        s = s.getPadding().withTo(visitContainer(s.getPadding().getTo(), p));
        s = s.getPadding().withValues(visitContainer(s.getPadding().getValues(), p));
        return s;
    }

    public Cobol visitSetUpDown(Cobol.SetUpDown setUpDown, P p) {
        Cobol.SetUpDown s = setUpDown;
        s = s.withPrefix(visitSpace(s.getPrefix(), p));
        s = s.withMarkers(visitMarkers(s.getMarkers(), p));
        s = s.getPadding().withTo(visitContainer(s.getPadding().getTo(), p));
        s = s.getPadding().withOperation(visitLeftPadded(s.getPadding().getOperation(), p));
        return s;
    }

    public Cobol visitSourceComputer(Cobol.SourceComputer sourceComputer, P p) {
        Cobol.SourceComputer s = sourceComputer;
        s = s.withPrefix(visitSpace(s.getPrefix(), p));
        s = s.withMarkers(visitMarkers(s.getMarkers(), p));
        s = s.getPadding().withWords(visitRightPadded(s.getPadding().getWords(), p));
        if (s.getPadding().getComputer() != null) {
            s = s.getPadding().withComputer(visitRightPadded(s.getPadding().getComputer(), p));
        }
        return s;
    }

    public Cobol visitSourceComputerDefinition(Cobol.SourceComputerDefinition sourceComputerDefinition, P p) {
        Cobol.SourceComputerDefinition s = sourceComputerDefinition;
        s = s.withPrefix(visitSpace(s.getPrefix(), p));
        s = s.withMarkers(visitMarkers(s.getMarkers(), p));
        if (s.getPadding().getDebuggingMode() != null) {
            s = s.getPadding().withDebuggingMode(visitLeftPadded(s.getPadding().getDebuggingMode(), p));
        }
        return s;
    }

    public Cobol visitObjectComputer(Cobol.ObjectComputer objectComputer, P p) {
        Cobol.ObjectComputer o = objectComputer;
        o = o.withPrefix(visitSpace(o.getPrefix(), p));
        o = o.withMarkers(visitMarkers(o.getMarkers(), p));
        o = o.getPadding().withWords(visitRightPadded(o.getPadding().getWords(), p));
        if (o.getPadding().getComputer() != null) {
            o = o.getPadding().withComputer(visitRightPadded(o.getPadding().getComputer(), p));
        }
        return o;
    }

    public Cobol visitObjectComputerDefinition(Cobol.ObjectComputerDefinition objectComputerDefinition, P p) {
        Cobol.ObjectComputerDefinition o = objectComputerDefinition;
        o = o.withPrefix(visitSpace(o.getPrefix(), p));
        o = o.withMarkers(visitMarkers(o.getMarkers(), p));
        o = o.getPadding().withSpecifications(visitContainer(o.getPadding().getSpecifications(), p));
        return o;
    }

    public Cobol visitValuedObjectComputerClause(Cobol.ValuedObjectComputerClause valuedObjectComputerClause, P p) {
        Cobol.ValuedObjectComputerClause v = valuedObjectComputerClause;
        v = v.withPrefix(visitSpace(v.getPrefix(), p));
        v = v.withMarkers(visitMarkers(v.getMarkers(), p));
        v = v.withValue(visit(v.getValue(), p));
        if (v.getPadding().getUnits() != null) {
            v = v.getPadding().withUnits(visitLeftPadded(v.getPadding().getUnits(), p));
        }
        return v;
    }

    public Cobol visitCollatingSequenceClause(Cobol.CollatingSequenceClause collatingSequenceClause, P p) {
        Cobol.CollatingSequenceClause c = collatingSequenceClause;
        c = c.withPrefix(visitSpace(c.getPrefix(), p));
        c = c.withMarkers(visitMarkers(c.getMarkers(), p));
        c = c.getPadding().withAlphabetName(visitContainer(c.getPadding().getAlphabetName(), p));
        c = c.withAlphanumeric((Cobol.CollatingSequenceAlphabet) visit(c.getAlphanumeric(), p));
        c = c.withNational((Cobol.CollatingSequenceAlphabet) visit(c.getNational(), p));
        return c;
    }

    public Cobol visitCollatingSequenceAlphabet(Cobol.CollatingSequenceAlphabet collatingSequenceAlphabet, P p) {
        Cobol.CollatingSequenceAlphabet c = collatingSequenceAlphabet;
        c = c.withPrefix(visitSpace(c.getPrefix(), p));
        c = c.withMarkers(visitMarkers(c.getMarkers(), p));
        c = c.withAlphabetName((Cobol.Identifier) visit(c.getAlphabetName(), p));
        return c;
    }

    public Cobol visitStatementPhrase(Cobol.StatementPhrase statementPhrase, P p) {
        Cobol.StatementPhrase s = statementPhrase;
        s = s.withPrefix(visitSpace(s.getPrefix(), p));
        s = s.withMarkers(visitMarkers(s.getMarkers(), p));
        s = s.getPadding().withStatement(visitContainer(s.getPadding().getStatement(), p));
        return s;
    }

    public Cobol visitStop(Cobol.Stop stop, P p) {
        Cobol.Stop s = stop;
        s = s.withPrefix(visitSpace(s.getPrefix(), p));
        s = s.withMarkers(visitMarkers(s.getMarkers(), p));
        s = s.withStatement(visit(s.getStatement(), p));
        return s;
    }

    public Cobol visitWorkingStorageSection(Cobol.WorkingStorageSection workingStorageSection, P p) {
        Cobol.WorkingStorageSection w = workingStorageSection;
        w = w.withPrefix(visitSpace(w.getPrefix(), p));
        w = w.withMarkers(visitMarkers(w.getMarkers(), p));
        w = w.getPadding().withDataDescriptions(visitContainer(w.getPadding().getDataDescriptions(), p));
        return w;
    }

    public Cobol visitAlphabetClause(Cobol.AlphabetClause alphabetClause, P p) {
        Cobol.AlphabetClause a = alphabetClause;
        a = a.withPrefix(visitSpace(a.getPrefix(), p));
        a = a.withMarkers(visitMarkers(a.getMarkers(), p));
        a = a.withName((Cobol.Identifier) visit(a.getName(), p));
        if (a.getPadding().getStandard() != null) {
            a = a.getPadding().withStandard(visitLeftPadded(a.getPadding().getStandard(), p));
        }
        a = a.getPadding().withLiterals(visitContainer(a.getPadding().getLiterals(), p));
        return a;
    }

    public Cobol visitAlphabetLiteral(Cobol.AlphabetLiteral alphabetLiteral, P p) {
        Cobol.AlphabetLiteral a = alphabetLiteral;
        a = a.withPrefix(visitSpace(a.getPrefix(), p));
        a = a.withMarkers(visitMarkers(a.getMarkers(), p));
        a = a.withLiteral((Cobol.Literal) visit(a.getLiteral(), p));
        a = a.withAlphabetThrough((Cobol.AlphabetThrough) visit(a.getAlphabetThrough(), p));
        a = a.getPadding().withAlphabetAlso(visitContainer(a.getPadding().getAlphabetAlso(), p));
        return a;
    }

    public Cobol visitAlphabetThrough(Cobol.AlphabetThrough alphabetThrough, P p) {
        Cobol.AlphabetThrough a = alphabetThrough;
        a = a.withPrefix(visitSpace(a.getPrefix(), p));
        a = a.withMarkers(visitMarkers(a.getMarkers(), p));
        a = a.withLiteral((Cobol.Literal) visit(a.getLiteral(), p));
        return a;
    }

    public Cobol visitAlphabetAlso(Cobol.AlphabetAlso alphabetAlso, P p) {
        Cobol.AlphabetAlso a = alphabetAlso;
        a = a.withPrefix(visitSpace(a.getPrefix(), p));
        a = a.withMarkers(visitMarkers(a.getMarkers(), p));
        a = a.getPadding().withLiterals(visitContainer(a.getPadding().getLiterals(), p));
        return a;
    }

    public Cobol visitSpecialNames(Cobol.SpecialNames specialNames, P p) {
        Cobol.SpecialNames s = specialNames;
        s = s.withPrefix(visitSpace(s.getPrefix(), p));
        s = s.withMarkers(visitMarkers(s.getMarkers(), p));
        s = s.getPadding().withClauses(visitContainer(s.getPadding().getClauses(), p));
        return s;
    }

    public Cobol visitChannelClause(Cobol.ChannelClause channelClause, P p) {
        Cobol.ChannelClause c = channelClause;
        c = c.withPrefix(visitSpace(c.getPrefix(), p));
        c = c.withMarkers(visitMarkers(c.getMarkers(), p));
        c = c.withLiteral((Cobol.Literal) visit(c.getLiteral(), p));
        if (c.getPadding().getIs() != null) {
            c = c.getPadding().withIs(visitLeftPadded(c.getPadding().getIs(), p));
        }
        c = c.withMnemonicName((Cobol.Identifier) visit(c.getMnemonicName(), p));
        return c;
    }

    public Cobol visitCurrencyClause(Cobol.CurrencyClause currencyClause, P p) {
        Cobol.CurrencyClause c = currencyClause;
        c = c.withPrefix(visitSpace(c.getPrefix(), p));
        c = c.withMarkers(visitMarkers(c.getMarkers(), p));
        c = c.withLiteral((Cobol.Literal) visit(c.getLiteral(), p));
        if (c.getPadding().getPictureSymbol() != null) {
            c = c.getPadding().withPictureSymbol(visitLeftPadded(c.getPadding().getPictureSymbol(), p));
        }
        c = c.withPictureSymbolLiteral((Cobol.Literal) visit(c.getPictureSymbolLiteral(), p));
        return c;
    }

    public Cobol visitDecimalPointClause(Cobol.DecimalPointClause decimalPointClause, P p) {
        Cobol.DecimalPointClause d = decimalPointClause;
        d = d.withPrefix(visitSpace(d.getPrefix(), p));
        d = d.withMarkers(visitMarkers(d.getMarkers(), p));
        return d;
    }

    public Cobol visitDefaultComputationalSignClause(Cobol.DefaultComputationalSignClause defaultComputationalSignClause, P p) {
        Cobol.DefaultComputationalSignClause d = defaultComputationalSignClause;
        d = d.withPrefix(visitSpace(d.getPrefix(), p));
        d = d.withMarkers(visitMarkers(d.getMarkers(), p));
        return d;
    }

    public Cobol visitDefaultDisplaySignClause(Cobol.DefaultDisplaySignClause defaultDisplaySignClause, P p) {
        Cobol.DefaultDisplaySignClause d = defaultDisplaySignClause;
        d = d.withPrefix(visitSpace(d.getPrefix(), p));
        d = d.withMarkers(visitMarkers(d.getMarkers(), p));
        return d;
    }

    public Cobol visitClassClause(Cobol.ClassClause classClause, P p) {
        Cobol.ClassClause c = classClause;
        c = c.withPrefix(visitSpace(c.getPrefix(), p));
        c = c.withMarkers(visitMarkers(c.getMarkers(), p));
        c = c.withClassName((Cobol.Identifier) visit(c.getClassName(), p));
        c = c.getPadding().withThroughs(visitContainer(c.getPadding().getThroughs(), p));
        return c;
    }

    public Cobol visitClassClauseThrough(Cobol.ClassClauseThrough classClauseThrough, P p) {
        Cobol.ClassClauseThrough c = classClauseThrough;
        c = c.withPrefix(visitSpace(c.getPrefix(), p));
        c = c.withMarkers(visitMarkers(c.getMarkers(), p));
        if (c.getPadding().getThrough() != null) {
            c = c.getPadding().withThrough(visitLeftPadded(c.getPadding().getThrough(), p));
        }
        return c;
    }

    public Cobol visitOdtClause(Cobol.OdtClause odtClause, P p) {
        Cobol.OdtClause o = odtClause;
        o = o.withPrefix(visitSpace(o.getPrefix(), p));
        o = o.withMarkers(visitMarkers(o.getMarkers(), p));
        o = o.withMnemonicName((Cobol.Identifier) visit(o.getMnemonicName(), p));
        return o;
    }

    public Cobol visitReserveNetworkClause(Cobol.ReserveNetworkClause reserveNetworkClause, P p) {
        Cobol.ReserveNetworkClause r = reserveNetworkClause;
        r = r.withPrefix(visitSpace(r.getPrefix(), p));
        r = r.withMarkers(visitMarkers(r.getMarkers(), p));
        return r;
    }

    public Cobol visitSymbolicCharacter(Cobol.SymbolicCharacter symbolicCharacter, P p) {
        Cobol.SymbolicCharacter s = symbolicCharacter;
        s = s.withPrefix(visitSpace(s.getPrefix(), p));
        s = s.withMarkers(visitMarkers(s.getMarkers(), p));
        s = s.getPadding().withSymbols(visitContainer(s.getPadding().getSymbols(), p));
        s = s.getPadding().withLiterals(visitContainer(s.getPadding().getLiterals(), p));
        return s;
    }

    public Cobol visitSymbolicCharactersClause(Cobol.SymbolicCharactersClause symbolicCharactersClause, P p) {
        Cobol.SymbolicCharactersClause s = symbolicCharactersClause;
        s = s.withPrefix(visitSpace(s.getPrefix(), p));
        s = s.withMarkers(visitMarkers(s.getMarkers(), p));
        s = s.getPadding().withSymbols(visitContainer(s.getPadding().getSymbols(), p));
        if (s.getPadding().getInAlphabet() != null) {
            s = s.getPadding().withInAlphabet(visitLeftPadded(s.getPadding().getInAlphabet(), p));
        }
        s = s.withAlphabetName((Cobol.Identifier) visit(s.getAlphabetName(), p));
        return s;
    }

    public Cobol visitFileSection(Cobol.FileSection fileSection, P p) {
        Cobol.FileSection f = fileSection;
        f = f.withPrefix(visitSpace(f.getPrefix(), p));
        f = f.withMarkers(visitMarkers(f.getMarkers(), p));
        return f;
    }

    public Cobol visitFileDescriptionEntry(Cobol.FileDescriptionEntry fileDescriptionEntry, P p) {
        Cobol.FileDescriptionEntry f = fileDescriptionEntry;
        f = f.withPrefix(visitSpace(f.getPrefix(), p));
        f = f.withMarkers(visitMarkers(f.getMarkers(), p));
        return f;
    }

    public Cobol visitLinkageSection(Cobol.LinkageSection linkageSection, P p) {
        Cobol.LinkageSection l = linkageSection;
        l = l.withPrefix(visitSpace(l.getPrefix(), p));
        l = l.withMarkers(visitMarkers(l.getMarkers(), p));
        l = l.getPadding().withDataDescriptions(visitContainer(l.getPadding().getDataDescriptions(), p));
        return l;
    }

    public Cobol visitLocalStorageSection(Cobol.LocalStorageSection localStorageSection, P p) {
        Cobol.LocalStorageSection l = localStorageSection;
        l = l.withPrefix(visitSpace(l.getPrefix(), p));
        l = l.withMarkers(visitMarkers(l.getMarkers(), p));
        l = l.getPadding().withDataDescriptions(visitContainer(l.getPadding().getDataDescriptions(), p));
        return l;
    }

    public Cobol visitDataBaseSection(Cobol.DataBaseSection dataBaseSection, P p) {
        Cobol.DataBaseSection d = dataBaseSection;
        d = d.withPrefix(visitSpace(d.getPrefix(), p));
        d = d.withMarkers(visitMarkers(d.getMarkers(), p));
        d = d.getPadding().withEntries(visitContainer(d.getPadding().getEntries(), p));
        return d;
    }

    public Cobol visitDataBaseSectionEntry(Cobol.DataBaseSectionEntry dataBaseSectionEntry, P p) {
        Cobol.DataBaseSectionEntry d = dataBaseSectionEntry;
        d = d.withPrefix(visitSpace(d.getPrefix(), p));
        d = d.withMarkers(visitMarkers(d.getMarkers(), p));
        d = d.withFrom((Cobol.Literal) visit(d.getFrom(), p));
        d = d.withTo((Cobol.Literal) visit(d.getTo(), p));
        return d;
    }

    public Cobol visitProcedureDivisionUsingClause(Cobol.ProcedureDivisionUsingClause procedureDivisionUsingClause, P p) {
        Cobol.ProcedureDivisionUsingClause pp = procedureDivisionUsingClause;
        pp = pp.withPrefix(visitSpace(pp.getPrefix(), p));
        pp = pp.withMarkers(visitMarkers(pp.getMarkers(), p));
        pp = pp.withProcedureDivisionUsingParameter(ListUtils.map(pp.getProcedureDivisionUsingParameter(), t -> visit(t, p)));
        return pp;
    }

    public Cobol visitProcedureDivisionByReferencePhrase(Cobol.ProcedureDivisionByReferencePhrase procedureDivisionByReferencePhrase, P p) {
        Cobol.ProcedureDivisionByReferencePhrase pp = procedureDivisionByReferencePhrase;
        pp = pp.withPrefix(visitSpace(pp.getPrefix(), p));
        pp = pp.withMarkers(visitMarkers(pp.getMarkers(), p));
        pp = pp.withProcedureDivisionByReference(ListUtils.map(pp.getProcedureDivisionByReference(), t -> (Cobol.ProcedureDivisionByReference) visit(t, p)));
        return pp;
    }

    public Cobol visitProcedureDivisionByReference(Cobol.ProcedureDivisionByReference procedureDivisionByReference, P p) {
        Cobol.ProcedureDivisionByReference pp = procedureDivisionByReference;
        pp = pp.withPrefix(visitSpace(pp.getPrefix(), p));
        pp = pp.withMarkers(visitMarkers(pp.getMarkers(), p));
        return pp;
    }

    public Cobol visitProcedureDivisionByValuePhrase(Cobol.ProcedureDivisionByValuePhrase procedureDivisionByValuePhrase, P p) {
        Cobol.ProcedureDivisionByValuePhrase pp = procedureDivisionByValuePhrase;
        pp = pp.withPrefix(visitSpace(pp.getPrefix(), p));
        pp = pp.withMarkers(visitMarkers(pp.getMarkers(), p));
        pp = pp.withPhrases(ListUtils.map(pp.getPhrases(), t -> (Name) visit(t, p)));
        return pp;
    }

    public Cobol visitScreenSection(Cobol.ScreenSection screenSection, P p) {
        Cobol.ScreenSection s = screenSection;
        s = s.withPrefix(visitSpace(s.getPrefix(), p));
        s = s.withMarkers(visitMarkers(s.getMarkers(), p));
        s = s.getPadding().withDescriptions(visitContainer(s.getPadding().getDescriptions(), p));
        return s;
    }

    public Cobol visitScreenDescriptionEntry(Cobol.ScreenDescriptionEntry screenDescriptionEntry, P p) {
        Cobol.ScreenDescriptionEntry s = screenDescriptionEntry;
        s = s.withPrefix(visitSpace(s.getPrefix(), p));
        s = s.withMarkers(visitMarkers(s.getMarkers(), p));
        if (s.getPadding().getName() != null) {
            s = s.getPadding().withName(visitLeftPadded(s.getPadding().getName(), p));
        }
        s = s.getPadding().withClauses(visitContainer(s.getPadding().getClauses(), p));
        return s;
    }

    public Cobol visitScreenDescriptionBlankClause(Cobol.ScreenDescriptionBlankClause screenDescriptionBlankClause, P p) {
        Cobol.ScreenDescriptionBlankClause s = screenDescriptionBlankClause;
        s = s.withPrefix(visitSpace(s.getPrefix(), p));
        s = s.withMarkers(visitMarkers(s.getMarkers(), p));
        return s;
    }

    public Cobol visitScreenDescriptionControlClause(Cobol.ScreenDescriptionControlClause screenDescriptionControlClause, P p) {
        Cobol.ScreenDescriptionControlClause s = screenDescriptionControlClause;
        s = s.withPrefix(visitSpace(s.getPrefix(), p));
        s = s.withMarkers(visitMarkers(s.getMarkers(), p));
        s = s.withIdentifier((Cobol.Identifier) visit(s.getIdentifier(), p));
        return s;
    }

    public Cobol visitScreenDescriptionSizeClause(Cobol.ScreenDescriptionSizeClause screenDescriptionSizeClause, P p) {
        Cobol.ScreenDescriptionSizeClause s = screenDescriptionSizeClause;
        s = s.withPrefix(visitSpace(s.getPrefix(), p));
        s = s.withMarkers(visitMarkers(s.getMarkers(), p));
        s = s.withIdentifier((Cobol.Identifier) visit(s.getIdentifier(), p));
        return s;
    }

    public Cobol visitScreenDescriptionToClause(Cobol.ScreenDescriptionToClause screenDescriptionToClause, P p) {
        Cobol.ScreenDescriptionToClause s = screenDescriptionToClause;
        s = s.withPrefix(visitSpace(s.getPrefix(), p));
        s = s.withMarkers(visitMarkers(s.getMarkers(), p));
        s = s.withIdentifier((Cobol.Identifier) visit(s.getIdentifier(), p));
        return s;
    }

    public Cobol visitScreenDescriptionUsingClause(Cobol.ScreenDescriptionUsingClause screenDescriptionUsingClause, P p) {
        Cobol.ScreenDescriptionUsingClause s = screenDescriptionUsingClause;
        s = s.withPrefix(visitSpace(s.getPrefix(), p));
        s = s.withMarkers(visitMarkers(s.getMarkers(), p));
        s = s.withIdentifier((Cobol.Identifier) visit(s.getIdentifier(), p));
        return s;
    }

    public Cobol visitAccept(Cobol.Accept acceptStatement, P p) {
        Cobol.Accept a = acceptStatement;
        a = a.withPrefix(visitSpace(a.getPrefix(), p));
        a = a.withMarkers(visitMarkers(a.getMarkers(), p));
        a = a.withIdentifier((Cobol.Identifier) visit(a.getIdentifier(), p));
        a = a.withOperation((Cobol) visit(a.getOperation(), p));
        a = a.withOnExceptionClause((Cobol.StatementPhrase) visit(a.getOnExceptionClause(), p));
        a = a.withNotOnExceptionClause((Cobol.StatementPhrase) visit(a.getNotOnExceptionClause(), p));
        if (a.getPadding().getEndAccept() != null) {
            a = a.getPadding().withEndAccept(visitLeftPadded(a.getPadding().getEndAccept(), p));
        }
        return a;
    }

    public Cobol visitAcceptFromDateStatement(Cobol.AcceptFromDateStatement acceptFromDateStatement, P p) {
        Cobol.AcceptFromDateStatement a = acceptFromDateStatement;
        a = a.withPrefix(visitSpace(a.getPrefix(), p));
        a = a.withMarkers(visitMarkers(a.getMarkers(), p));
        return a;
    }

    public Cobol visitAcceptFromMnemonicStatement(Cobol.AcceptFromMnemonicStatement acceptFromMnemonicStatement, P p) {
        Cobol.AcceptFromMnemonicStatement a = acceptFromMnemonicStatement;
        a = a.withPrefix(visitSpace(a.getPrefix(), p));
        a = a.withMarkers(visitMarkers(a.getMarkers(), p));
        a = a.withMnemonicName((Cobol.Identifier) visit(a.getMnemonicName(), p));
        return a;
    }

    public Cobol visitAcceptFromEscapeKeyStatement(Cobol.AcceptFromEscapeKeyStatement acceptFromEscapeKeyStatement, P p) {
        Cobol.AcceptFromEscapeKeyStatement a = acceptFromEscapeKeyStatement;
        a = a.withPrefix(visitSpace(a.getPrefix(), p));
        a = a.withMarkers(visitMarkers(a.getMarkers(), p));
        return a;
    }

    public Cobol visitAcceptMessageCountStatement(Cobol.AcceptMessageCountStatement acceptMessageCountStatement, P p) {
        Cobol.AcceptMessageCountStatement a = acceptMessageCountStatement;
        a = a.withPrefix(visitSpace(a.getPrefix(), p));
        a = a.withMarkers(visitMarkers(a.getMarkers(), p));
        return a;
    }

    public Cobol visitOnExceptionClause(Cobol.OnExceptionClause onExceptionClause, P p) {
        Cobol.OnExceptionClause o = onExceptionClause;
        o = o.withPrefix(visitSpace(o.getPrefix(), p));
        o = o.withMarkers(visitMarkers(o.getMarkers(), p));
        return o;
    }

    public Cobol visitNotOnExceptionClause(Cobol.NotOnExceptionClause notOnExceptionClause, P p) {
        Cobol.NotOnExceptionClause n = notOnExceptionClause;
        n = n.withPrefix(visitSpace(n.getPrefix(), p));
        n = n.withMarkers(visitMarkers(n.getMarkers(), p));
        return n;
    }

    public Cobol visitAlterStatement(Cobol.AlterStatement alterStatement, P p) {
        Cobol.AlterStatement a = alterStatement;
        a = a.withPrefix(visitSpace(a.getPrefix(), p));
        a = a.withMarkers(visitMarkers(a.getMarkers(), p));
        a = a.withAlterProceedTo(ListUtils.map(a.getAlterProceedTo(), t -> (Cobol.AlterProceedTo) visit(t, p)));
        return a;
    }

    public Cobol visitAlterProceedTo(Cobol.AlterProceedTo alterProceedTo, P p) {
        Cobol.AlterProceedTo a = alterProceedTo;
        a = a.withPrefix(visitSpace(a.getPrefix(), p));
        a = a.withMarkers(visitMarkers(a.getMarkers(), p));
        a = a.withFrom((Cobol.ProcedureName) visit(a.getFrom(), p));
        a = a.withTo((Cobol.ProcedureName) visit(a.getTo(), p));
        return a;
    }

    public Cobol visitProcedureName(Cobol.ProcedureName procedureName, P p) {
        Cobol.ProcedureName pp = procedureName;
        pp = pp.withPrefix(visitSpace(pp.getPrefix(), p));
        pp = pp.withMarkers(visitMarkers(pp.getMarkers(), p));
        pp = pp.withInSection((Cobol.InSection) visit(pp.getInSection(), p));
        return pp;
    }

    public Cobol visitInSection(Cobol.InSection inSection, P p) {
        Cobol.InSection i = inSection;
        i = i.withPrefix(visitSpace(i.getPrefix(), p));
        i = i.withMarkers(visitMarkers(i.getMarkers(), p));
        return i;
    }

    public Cobol visitCancel(Cobol.Cancel cancel, P p) {
        Cobol.Cancel c = cancel;
        c = c.withPrefix(visitSpace(c.getPrefix(), p));
        c = c.withMarkers(visitMarkers(c.getMarkers(), p));
        c = c.getPadding().withCancelCalls(visitContainer(c.getPadding().getCancelCalls(), p));
        return c;
    }

    public Cobol visitCancelCall(Cobol.CancelCall cancelCall, P p) {
        Cobol.CancelCall c = cancelCall;
        c = c.withPrefix(visitSpace(c.getPrefix(), p));
        c = c.withMarkers(visitMarkers(c.getMarkers(), p));
        c = c.withIdentifier((Cobol.Identifier) visit(c.getIdentifier(), p));
        c = c.withLiteral((Cobol.Literal) visit(c.getLiteral(), p));
        return c;
    }

    public Cobol visitClose(Cobol.Close close, P p) {
        Cobol.Close c = close;
        c = c.withPrefix(visitSpace(c.getPrefix(), p));
        c = c.withMarkers(visitMarkers(c.getMarkers(), p));
        c = c.getPadding().withCloseFiles(visitContainer(c.getPadding().getCloseFiles(), p));
        return c;
    }

    public Cobol visitCloseFile(Cobol.CloseFile closeFile, P p) {
        Cobol.CloseFile c = closeFile;
        c = c.withPrefix(visitSpace(c.getPrefix(), p));
        c = c.withMarkers(visitMarkers(c.getMarkers(), p));
        c = c.withCloseStatement((Cobol) visit(c.getCloseStatement(), p));
        return c;
    }

    public Cobol visitCloseReelUnitStatement(Cobol.CloseReelUnitStatement closeReelUnitStatement, P p) {
        Cobol.CloseReelUnitStatement c = closeReelUnitStatement;
        c = c.withPrefix(visitSpace(c.getPrefix(), p));
        c = c.withMarkers(visitMarkers(c.getMarkers(), p));
        return c;
    }

    public Cobol visitCloseRelativeStatement(Cobol.CloseRelativeStatement closeRelativeStatement, P p) {
        Cobol.CloseRelativeStatement c = closeRelativeStatement;
        c = c.withPrefix(visitSpace(c.getPrefix(), p));
        c = c.withMarkers(visitMarkers(c.getMarkers(), p));
        return c;
    }

    public Cobol visitClosePortFileIOStatement(Cobol.ClosePortFileIOStatement closePortFileIOStatement, P p) {
        Cobol.ClosePortFileIOStatement c = closePortFileIOStatement;
        c = c.withPrefix(visitSpace(c.getPrefix(), p));
        c = c.withMarkers(visitMarkers(c.getMarkers(), p));
        c = c.getPadding().withClosePortFileIOUsing(visitContainer(c.getPadding().getClosePortFileIOUsing(), p));
        return c;
    }

    public Cobol visitClosePortFileIOUsingCloseDisposition(Cobol.ClosePortFileIOUsingCloseDisposition closePortFileIOUsingCloseDisposition, P p) {
        Cobol.ClosePortFileIOUsingCloseDisposition c = closePortFileIOUsingCloseDisposition;
        c = c.withPrefix(visitSpace(c.getPrefix(), p));
        c = c.withMarkers(visitMarkers(c.getMarkers(), p));
        return c;
    }

    public Cobol visitClosePortFileIOUsingAssociatedData(Cobol.ClosePortFileIOUsingAssociatedData closePortFileIOUsingAssociatedData, P p) {
        Cobol.ClosePortFileIOUsingAssociatedData c = closePortFileIOUsingAssociatedData;
        c = c.withPrefix(visitSpace(c.getPrefix(), p));
        c = c.withMarkers(visitMarkers(c.getMarkers(), p));
        c = c.withIdentifier((Cobol.Identifier) visit(c.getIdentifier(), p));
        return c;
    }

    public Cobol visitClosePortFileIOUsingAssociatedDataLength(Cobol.ClosePortFileIOUsingAssociatedDataLength closePortFileIOUsingAssociatedDataLength, P p) {
        Cobol.ClosePortFileIOUsingAssociatedDataLength c = closePortFileIOUsingAssociatedDataLength;
        c = c.withPrefix(visitSpace(c.getPrefix(), p));
        c = c.withMarkers(visitMarkers(c.getMarkers(), p));
        c = c.withIdentifier((Cobol.Identifier) visit(c.getIdentifier(), p));
        return c;
    }

    public Cobol visitInData(Cobol.InData inData, P p) {
        Cobol.InData i = inData;
        i = i.withPrefix(visitSpace(i.getPrefix(), p));
        i = i.withMarkers(visitMarkers(i.getMarkers(), p));
        return i;
    }

    public Cobol visitInFile(Cobol.InFile inFile, P p) {
        Cobol.InFile i = inFile;
        i = i.withPrefix(visitSpace(i.getPrefix(), p));
        i = i.withMarkers(visitMarkers(i.getMarkers(), p));
        return i;
    }

    public Cobol visitInMnemonic(Cobol.InMnemonic inMnemonic, P p) {
        Cobol.InMnemonic i = inMnemonic;
        i = i.withPrefix(visitSpace(i.getPrefix(), p));
        i = i.withMarkers(visitMarkers(i.getMarkers(), p));
        return i;
    }

    public Cobol visitInLibrary(Cobol.InLibrary inLibrary, P p) {
        Cobol.InLibrary i = inLibrary;
        i = i.withPrefix(visitSpace(i.getPrefix(), p));
        i = i.withMarkers(visitMarkers(i.getMarkers(), p));
        return i;
    }

    public Cobol visitInTable(Cobol.InTable inTable, P p) {
        Cobol.InTable i = inTable;
        i = i.withPrefix(visitSpace(i.getPrefix(), p));
        i = i.withMarkers(visitMarkers(i.getMarkers(), p));
        return i;
    }

    public Cobol visitContinue(Cobol.Continue continuez, P p) {
        Cobol.Continue c = continuez;
        c = c.withPrefix(visitSpace(c.getPrefix(), p));
        c = c.withMarkers(visitMarkers(c.getMarkers(), p));
        return c;
    }

    public Cobol visitDelete(Cobol.Delete delete, P p) {
        Cobol.Delete d = delete;
        d = d.withPrefix(visitSpace(d.getPrefix(), p));
        d = d.withMarkers(visitMarkers(d.getMarkers(), p));
        d = d.withInvalidKey((Cobol.StatementPhrase) visit(d.getInvalidKey(), p));
        d = d.withNotInvalidKey((Cobol.StatementPhrase) visit(d.getNotInvalidKey(), p));
        if (d.getPadding().getEndDelete() != null) {
            d = d.getPadding().withEndDelete(visitLeftPadded(d.getPadding().getEndDelete(), p));
        }
        return d;
    }

    public Cobol visitDisable(Cobol.Disable disable, P p) {
        Cobol.Disable d = disable;
        d = d.withPrefix(visitSpace(d.getPrefix(), p));
        d = d.withMarkers(visitMarkers(d.getMarkers(), p));
        return d;
    }

    public Cobol visitEnable(Cobol.Enable enable, P p) {
        Cobol.Enable e = enable;
        e = e.withPrefix(visitSpace(e.getPrefix(), p));
        e = e.withMarkers(visitMarkers(e.getMarkers(), p));
        return e;
    }

    public Cobol.Entry visitEntry(Cobol.Entry entry, P p) {
        Cobol.Entry e = entry;
        e = e.withPrefix(visitSpace(e.getPrefix(), p));
        e = e.withMarkers(visitMarkers(e.getMarkers(), p));
        return e;
    }

    public Cobol visitExhibit(Cobol.Exhibit exhibit, P p) {
        Cobol.Exhibit e = exhibit;
        e = e.withPrefix(visitSpace(e.getPrefix(), p));
        e = e.withMarkers(visitMarkers(e.getMarkers(), p));
        e = e.getPadding().withOperands(visitContainer(e.getPadding().getOperands(), p));
        return e;
    }

    public Cobol visitExit(Cobol.Exit exit, P p) {
        Cobol.Exit e = exit;
        e = e.withPrefix(visitSpace(e.getPrefix(), p));
        e = e.withMarkers(visitMarkers(e.getMarkers(), p));
        return e;
    }

    public Cobol visitGenerate(Cobol.Generate generate, P p) {
        Cobol.Generate g = generate;
        g = g.withPrefix(visitSpace(g.getPrefix(), p));
        g = g.withMarkers(visitMarkers(g.getMarkers(), p));
        g = g.withReportName((Cobol.ReportName) visit(g.getReportName(), p));
        return g;
    }

    public Cobol visitQualifiedDataName(Cobol.QualifiedDataName qualifiedDataName, P p) {
        Cobol.QualifiedDataName q = qualifiedDataName;
        q = q.withPrefix(visitSpace(q.getPrefix(), p));
        q = q.withMarkers(visitMarkers(q.getMarkers(), p));
        q = q.withDataName((Cobol) visit(q.getDataName(), p));
        return q;
    }

    public Cobol visitQualifiedDataNameFormat1(Cobol.QualifiedDataNameFormat1 qualifiedDataNameFormat1, P p) {
        Cobol.QualifiedDataNameFormat1 q = qualifiedDataNameFormat1;
        q = q.withPrefix(visitSpace(q.getPrefix(), p));
        q = q.withMarkers(visitMarkers(q.getMarkers(), p));
        q = q.getPadding().withQualifiedInData(visitContainer(q.getPadding().getQualifiedInData(), p));
        q = q.withInFile((Cobol.InFile) visit(q.getInFile(), p));
        return q;
    }

    public Cobol visitQualifiedDataNameFormat2(Cobol.QualifiedDataNameFormat2 qualifiedDataNameFormat2, P p) {
        Cobol.QualifiedDataNameFormat2 q = qualifiedDataNameFormat2;
        q = q.withPrefix(visitSpace(q.getPrefix(), p));
        q = q.withMarkers(visitMarkers(q.getMarkers(), p));
        q = q.withInSection((Cobol.InSection) visit(q.getInSection(), p));
        return q;
    }

    public Cobol visitQualifiedDataNameFormat3(Cobol.QualifiedDataNameFormat3 qualifiedDataNameFormat3, P p) {
        Cobol.QualifiedDataNameFormat3 q = qualifiedDataNameFormat3;
        q = q.withPrefix(visitSpace(q.getPrefix(), p));
        q = q.withMarkers(visitMarkers(q.getMarkers(), p));
        q = q.withInLibrary((Cobol.InLibrary) visit(q.getInLibrary(), p));
        return q;
    }

    public Cobol visitQualifiedDataNameFormat4(Cobol.QualifiedDataNameFormat4 qualifiedDataNameFormat4, P p) {
        Cobol.QualifiedDataNameFormat4 q = qualifiedDataNameFormat4;
        q = q.withPrefix(visitSpace(q.getPrefix(), p));
        q = q.withMarkers(visitMarkers(q.getMarkers(), p));
        q = q.withInFile((Cobol.InFile) visit(q.getInFile(), p));
        return q;
    }

    public Cobol visitQualifiedInData(Cobol.QualifiedInData qualifiedInData, P p) {
        Cobol.QualifiedInData q = qualifiedInData;
        q = q.withPrefix(visitSpace(q.getPrefix(), p));
        q = q.withMarkers(visitMarkers(q.getMarkers(), p));
        q = q.withIn((Cobol) visit(q.getIn(), p));
        return q;
    }

    public Cobol visitReportName(Cobol.ReportName reportName, P p) {
        Cobol.ReportName r = reportName;
        r = r.withPrefix(visitSpace(r.getPrefix(), p));
        r = r.withMarkers(visitMarkers(r.getMarkers(), p));
        r = r.withQualifiedDataName((Cobol.QualifiedDataName) visit(r.getQualifiedDataName(), p));
        return r;
    }
}
