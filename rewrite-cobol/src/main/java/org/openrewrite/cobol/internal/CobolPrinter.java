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
        visitLeftPadded("", container.getPreposition(), p);
        p.append(before);
        visitRightPadded(container.getPadding().getElements(), suffixBetween, p);
        p.append(after == null ? "" : after);
    }

    @Override
    public Cobol visitCompilationUnit(Cobol.CompilationUnit compilationUnit, PrintOutputCapture<P> p) {
        visitSpace(compilationUnit.getPrefix(), p);
        visitMarkers(compilationUnit.getMarkers(), p);
        for (Cobol.ProgramUnit programUnit : compilationUnit.getProgramUnits()) {
            visit(programUnit, p);
        }
        p.append(compilationUnit.getEof());
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
        p.append(identificationDivision.getWords());
        visitLeftPadded(".", identificationDivision.getPadding().getProgramIdParagraph(), p);
        return identificationDivision;
    }

    public Cobol visitProcedureDivision(Cobol.ProcedureDivision procedureDivision, PrintOutputCapture<P> p) {
        visitSpace(procedureDivision.getPrefix(), p);
        visitMarkers(procedureDivision.getMarkers(), p);
        p.append(procedureDivision.getWords());
        visitProcedureDivisionUsingClause(procedureDivision.getUsing(), p);
        visitLeftPadded(".", procedureDivision.getPadding().getBody(), p);
        return procedureDivision;
    }

    @SuppressWarnings("NullableProblems")
    @Nullable
    public Cobol visitProcedureDivisionUsingClause(@Nullable Cobol.ProcedureDivisionUsingClause procedureDivisionUsingClause, PrintOutputCapture<P> p) {
        if(procedureDivisionUsingClause == null) {
            return null;
        }
        visitSpace(procedureDivisionUsingClause.getPrefix(), p);
        visitMarkers(procedureDivisionUsingClause.getMarkers(), p);
        p.append(procedureDivisionUsingClause.getWords());
        for (Cobol.ProcedureDivisionUsingParameter pp : procedureDivisionUsingClause.getProcedureDivisionUsingParameter()) {
            visit(pp, p);
        }
        return procedureDivisionUsingClause;
    }

    public Cobol visitProcedureDivisionByReferencePhrase(Cobol.ProcedureDivisionByReferencePhrase procedureDivisionByReferencePhrase, PrintOutputCapture<P> p) {
        visitSpace(procedureDivisionByReferencePhrase.getPrefix(), p);
        visitMarkers(procedureDivisionByReferencePhrase.getMarkers(), p);
        p.append(procedureDivisionByReferencePhrase.getWords());
        for (Cobol.ProcedureDivisionByReference pp : procedureDivisionByReferencePhrase.getProcedureDivisionByReference()) {
            visit(pp, p);
        }
        return procedureDivisionByReferencePhrase;
    }

    public Cobol visitProcedureDivisionByReference(Cobol.ProcedureDivisionByReference procedureDivisionByReference, PrintOutputCapture<P> p) {
        visitSpace(procedureDivisionByReference.getPrefix(), p);
        visitMarkers(procedureDivisionByReference.getMarkers(), p);
        p.append(procedureDivisionByReference.getWords());
        visit(procedureDivisionByReference.getReference(), p);
        return procedureDivisionByReference;
    }

    public Cobol visitProcedureDivisionByValuePhrase(Cobol.ProcedureDivisionByValuePhrase procedureDivisionByValuePhrase, PrintOutputCapture<P> p) {
        visitSpace(procedureDivisionByValuePhrase.getPrefix(), p);
        visitMarkers(procedureDivisionByValuePhrase.getMarkers(), p);
        p.append(procedureDivisionByValuePhrase.getWords());
        for (Name pp : procedureDivisionByValuePhrase.getPhrases()) {
            visit(pp, p);
        }
        return procedureDivisionByValuePhrase;
    }

    public Cobol visitProcedureDivisionBody(Cobol.ProcedureDivisionBody procedureDivisionBody, PrintOutputCapture<P> p) {
        visitSpace(procedureDivisionBody.getPrefix(), p);
        visitMarkers(procedureDivisionBody.getMarkers(), p);
        visit(procedureDivisionBody.getParagraphs(), p);
        return procedureDivisionBody;
    }

    public Cobol visitStop(Cobol.Stop stop, PrintOutputCapture<P> p) {
        visitSpace(stop.getPrefix(), p);
        visitMarkers(stop.getMarkers(), p);
        p.append(stop.getWords());
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

    public Cobol visitDataPictureClause(Cobol.DataPictureClause dataPictureClause, PrintOutputCapture<P> p) {
        visitSpace(dataPictureClause.getPrefix(), p);
        visitMarkers(dataPictureClause.getMarkers(), p);
        p.append(dataPictureClause.getWords());
        visitContainer("", dataPictureClause.getPadding().getPictures(), "", "", p);
        return dataPictureClause;
    }

    public Cobol visitWorkingStorageSection(Cobol.WorkingStorageSection workingStorageSection, PrintOutputCapture<P> p) {
        visitSpace(workingStorageSection.getPrefix(), p);
        visitMarkers(workingStorageSection.getMarkers(), p);
        p.append(workingStorageSection.getWords());
        visitContainer(".", workingStorageSection.getPadding().getDataDescriptions(), "", "", p);
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
        p.append(dataDescriptionEntry.getLevel());
        visitLeftPadded("", dataDescriptionEntry.getPadding().getName(), p);
        visitContainer("", dataDescriptionEntry.getPadding().getClauses(), ".", ".", p);
        return dataDescriptionEntry;
    }

    public Cobol visitEnvironmentDivision(Cobol.EnvironmentDivision environmentDivision, PrintOutputCapture<P> p) {
        visitSpace(environmentDivision.getPrefix(), p);
        visitMarkers(environmentDivision.getMarkers(), p);
        p.append(environmentDivision.getWords());
        visitContainer(".", environmentDivision.getPadding().getBody(), "", "", p);
        return environmentDivision;
    }

    public Cobol visitProgramUnit(Cobol.ProgramUnit programUnit, PrintOutputCapture<P> p) {
        visitSpace(programUnit.getPrefix(), p);
        visitMarkers(programUnit.getMarkers(), p);
        visit(programUnit.getIdentificationDivision(), p);
        visit(programUnit.getDataDivision(), p);
        visit(programUnit.getEnvironmentDivision(), p);
        visit(programUnit.getProcedureDivision(), p);
        visitContainer("", programUnit.getPadding().getProgramUnits(), "", "", p);
        visitRightPadded(programUnit.getPadding().getEndProgram(), ".", p);
        return programUnit;
    }

    public Cobol visitSetTo(Cobol.SetTo setTo, PrintOutputCapture<P> p) {
        visitSpace(setTo.getPrefix(), p);
        visitMarkers(setTo.getMarkers(), p);
        visitContainer("", setTo.getPadding().getTo(), "", "", p);
        visitContainer("", setTo.getPadding().getValues(), "", "", p);
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

    public Cobol visitAdd(Cobol.Add add, PrintOutputCapture<P> p) {
        visitSpace(add.getPrefix(), p);
        visitMarkers(add.getMarkers(), p);
        p.append(add.getAdd());
        visit(add.getOperation(), p);
        visit(add.getOnSizeError(), p);
        visitLeftPadded("", add.getPadding().getEndAdd(), p);
        return add;
    }

    public Cobol visitDataDivision(Cobol.DataDivision dataDivision, PrintOutputCapture<P> p) {
        visitSpace(dataDivision.getPrefix(), p);
        visitMarkers(dataDivision.getMarkers(), p);
        p.append(dataDivision.getWords());
        visitContainer(".", dataDivision.getPadding().getSections(), "", "", p);
        return dataDivision;
    }

    public Cobol visitEndProgram(Cobol.EndProgram endProgram, PrintOutputCapture<P> p) {
        visitSpace(endProgram.getPrefix(), p);
        visitMarkers(endProgram.getMarkers(), p);
        p.append(endProgram.getWords());
        return endProgram;
    }

    public Cobol visitStatementPhrase(Cobol.StatementPhrase statementPhrase, PrintOutputCapture<P> p) {
        visitSpace(statementPhrase.getPrefix(), p);
        visitMarkers(statementPhrase.getMarkers(), p);
        p.append(statementPhrase.getPhrase());
        visitContainer("", statementPhrase.getPadding().getStatement(), "", "", p);
        return statementPhrase;
    }

    public Cobol visitProgramIdParagraph(Cobol.ProgramIdParagraph programIdParagraph, PrintOutputCapture<P> p) {
        visitSpace(programIdParagraph.getPrefix(), p);
        visitMarkers(programIdParagraph.getMarkers(), p);
        p.append(programIdParagraph.getProgramId());
        visitLeftPadded(".", programIdParagraph.getPadding().getProgramName(), p);
        visitLeftPadded("", programIdParagraph.getPadding().getProgramAttributes(), p);
        visitLeftPadded("", programIdParagraph.getPadding().getDot(), p);
        return programIdParagraph;
    }

    public Cobol visitAddTo(Cobol.AddTo addTo, PrintOutputCapture<P> p) {
        visitSpace(addTo.getPrefix(), p);
        visitMarkers(addTo.getMarkers(), p);
        visitContainer("", addTo.getPadding().getFrom(), "", "", p);
        visitContainer("", addTo.getPadding().getTo(), "", "", p);
        visitContainer("", addTo.getPadding().getGiving(), "", "", p);
        return addTo;
    }

    public Cobol visitRoundable(Cobol.Roundable roundable, PrintOutputCapture<P> p) {
        visitSpace(roundable.getPrefix(), p);
        visitMarkers(roundable.getMarkers(), p);
        visit(roundable.getIdentifier(), p);
        visitLeftPadded("", roundable.getPadding().getRounded(), p);
        return roundable;
    }

    public Cobol visitConfigurationSection(Cobol.ConfigurationSection configurationSection, PrintOutputCapture<P> p) {
        visitSpace(configurationSection.getPrefix(), p);
        visitMarkers(configurationSection.getMarkers(), p);
        p.append(configurationSection.getWords());
        visitContainer(".", configurationSection.getPadding().getParagraphs(), "", "", p);
        return configurationSection;
    }

    public Cobol visitSourceComputerDefinition(Cobol.SourceComputerDefinition sourceComputerDefinition, PrintOutputCapture<P> p) {
        visitSpace(sourceComputerDefinition.getPrefix(), p);
        visitMarkers(sourceComputerDefinition.getMarkers(), p);
        p.append(sourceComputerDefinition.getComputerName());
        visitLeftPadded("", sourceComputerDefinition.getPadding().getDebuggingMode(), p);
        return sourceComputerDefinition;
    }

    public Cobol visitObjectComputer(Cobol.ObjectComputer objectComputer, PrintOutputCapture<P> p) {
        visitSpace(objectComputer.getPrefix(), p);
        visitMarkers(objectComputer.getMarkers(), p);
        visitRightPadded(objectComputer.getPadding().getWords(), ".", p);
        visitRightPadded(objectComputer.getPadding().getComputer(), ".", p);
        return objectComputer;
    }

    public Cobol visitObjectComputerDefinition(Cobol.ObjectComputerDefinition objectComputerDefinition, PrintOutputCapture<P> p) {
        visitSpace(objectComputerDefinition.getPrefix(), p);
        visitMarkers(objectComputerDefinition.getMarkers(), p);
        p.append(objectComputerDefinition.getComputerName());
        visitContainer("", objectComputerDefinition.getPadding().getSpecifications(), "", "", p);
        return objectComputerDefinition;
    }

    public Cobol visitCollatingSequenceClause(Cobol.CollatingSequenceClause collatingSequenceClause, PrintOutputCapture<P> p) {
        visitSpace(collatingSequenceClause.getPrefix(), p);
        visitMarkers(collatingSequenceClause.getMarkers(), p);
        p.append(collatingSequenceClause.getWords());
        visitContainer("", collatingSequenceClause.getPadding().getAlphabetName(), "", "", p);
        visit(collatingSequenceClause.getAlphanumeric(), p);
        visit(collatingSequenceClause.getNational(), p);
        return collatingSequenceClause;
    }

    public Cobol visitCollatingSequenceAlphabet(Cobol.CollatingSequenceAlphabet collatingSequenceAlphabet, PrintOutputCapture<P> p) {
        visitSpace(collatingSequenceAlphabet.getPrefix(), p);
        visitMarkers(collatingSequenceAlphabet.getMarkers(), p);
        p.append(collatingSequenceAlphabet.getWords());
        visit(collatingSequenceAlphabet.getAlphabetName(), p);
        return collatingSequenceAlphabet;
    }

    public Cobol visitSourceComputer(Cobol.SourceComputer sourceComputer, PrintOutputCapture<P> p) {
        visitSpace(sourceComputer.getPrefix(), p);
        visitMarkers(sourceComputer.getMarkers(), p);
        visitRightPadded(sourceComputer.getPadding().getWords(), ".", p);
        visitRightPadded(sourceComputer.getPadding().getComputer(), ".", p);
        return sourceComputer;
    }

    public Cobol visitValuedObjectComputerClause(Cobol.ValuedObjectComputerClause valuedObjectComputerClause, PrintOutputCapture<P> p) {
        visitSpace(valuedObjectComputerClause.getPrefix(), p);
        visitMarkers(valuedObjectComputerClause.getMarkers(), p);
        p.append(valuedObjectComputerClause.getWords());
        visit(valuedObjectComputerClause.getValue(), p);
        visitLeftPadded("", valuedObjectComputerClause.getPadding().getUnits(), p);
        return valuedObjectComputerClause;
    }

    public Cobol visitAlphabetClause(Cobol.AlphabetClause alphabetClause, PrintOutputCapture<P> p) {
        visitSpace(alphabetClause.getPrefix(), p);
        visitMarkers(alphabetClause.getMarkers(), p);
        p.append(alphabetClause.getWords());
        visit(alphabetClause.getName(), p);
        visitLeftPadded("", alphabetClause.getPadding().getStandard(), p);
        visitContainer("", alphabetClause.getPadding().getLiterals(), "", "", p);
        return alphabetClause;
    }

    public Cobol visitAlphabetLiteral(Cobol.AlphabetLiteral alphabetLiteral, PrintOutputCapture<P> p) {
        visitSpace(alphabetLiteral.getPrefix(), p);
        visitMarkers(alphabetLiteral.getMarkers(), p);
        visit(alphabetLiteral.getLiteral(), p);
        visit(alphabetLiteral.getAlphabetThrough(), p);
        visitContainer("", alphabetLiteral.getPadding().getAlphabetAlso(), "", "", p);
        return alphabetLiteral;
    }

    public Cobol visitAlphabetThrough(Cobol.AlphabetThrough alphabetThrough, PrintOutputCapture<P> p) {
        visitSpace(alphabetThrough.getPrefix(), p);
        visitMarkers(alphabetThrough.getMarkers(), p);
        p.append(alphabetThrough.getWords());
        visit(alphabetThrough.getLiteral(), p);
        return alphabetThrough;
    }

    public Cobol visitAlphabetAlso(Cobol.AlphabetAlso alphabetAlso, PrintOutputCapture<P> p) {
        visitSpace(alphabetAlso.getPrefix(), p);
        visitMarkers(alphabetAlso.getMarkers(), p);
        p.append(alphabetAlso.getWords());
        visitContainer("", alphabetAlso.getPadding().getLiterals(), "", "", p);
        return alphabetAlso;
    }

    public Cobol visitSpecialNames(Cobol.SpecialNames specialNames, PrintOutputCapture<P> p) {
        visitSpace(specialNames.getPrefix(), p);
        visitMarkers(specialNames.getMarkers(), p);
        p.append(specialNames.getWords());
        visitContainer(".", specialNames.getPadding().getClauses(), "", "", p);
        if (!specialNames.getClauses().isEmpty()) {
            p.append('.');
        }
        return specialNames;
    }

    public Cobol visitChannelClause(Cobol.ChannelClause channelClause, PrintOutputCapture<P> p) {
        visitSpace(channelClause.getPrefix(), p);
        visitMarkers(channelClause.getMarkers(), p);
        p.append(channelClause.getWords());
        visit(channelClause.getLiteral(), p);
        visitLeftPadded("", channelClause.getPadding().getIs(), p);
        visit(channelClause.getMnemonicName(), p);
        return channelClause;
    }

    public Cobol visitCurrencyClause(Cobol.CurrencyClause currencyClause, PrintOutputCapture<P> p) {
        visitSpace(currencyClause.getPrefix(), p);
        visitMarkers(currencyClause.getMarkers(), p);
        p.append(currencyClause.getWords());
        visit(currencyClause.getLiteral(), p);
        visitLeftPadded("", currencyClause.getPadding().getPictureSymbol(), p);
        visit(currencyClause.getPictureSymbolLiteral(), p);
        return currencyClause;
    }

    public Cobol visitDecimalPointClause(Cobol.DecimalPointClause decimalPointClause, PrintOutputCapture<P> p) {
        visitSpace(decimalPointClause.getPrefix(), p);
        visitMarkers(decimalPointClause.getMarkers(), p);
        p.append(decimalPointClause.getWords());
        return decimalPointClause;
    }

    public Cobol visitDefaultComputationalSignClause(Cobol.DefaultComputationalSignClause defaultComputationalSignClause, PrintOutputCapture<P> p) {
        visitSpace(defaultComputationalSignClause.getPrefix(), p);
        visitMarkers(defaultComputationalSignClause.getMarkers(), p);
        p.append(defaultComputationalSignClause.getWords());
        return defaultComputationalSignClause;
    }

    public Cobol visitDefaultDisplaySignClause(Cobol.DefaultDisplaySignClause defaultDisplaySignClause, PrintOutputCapture<P> p) {
        visitSpace(defaultDisplaySignClause.getPrefix(), p);
        visitMarkers(defaultDisplaySignClause.getMarkers(), p);
        p.append(defaultDisplaySignClause.getWords());
        return defaultDisplaySignClause;
    }

    public Cobol visitClassClause(Cobol.ClassClause classClause, PrintOutputCapture<P> p) {
        visitSpace(classClause.getPrefix(), p);
        visitMarkers(classClause.getMarkers(), p);
        p.append(classClause.getWords());
        visit(classClause.getClassName(), p);
        visitContainer("", classClause.getPadding().getThroughs(), "", "", p);
        return classClause;
    }

    public Cobol visitClassClauseThrough(Cobol.ClassClauseThrough classClauseThrough, PrintOutputCapture<P> p) {
        visitSpace(classClauseThrough.getPrefix(), p);
        visitMarkers(classClauseThrough.getMarkers(), p);
        visitLeftPadded("", classClauseThrough.getPadding().getThrough(), p);
        return classClauseThrough;
    }

    public Cobol visitOdtClause(Cobol.OdtClause odtClause, PrintOutputCapture<P> p) {
        visitSpace(odtClause.getPrefix(), p);
        visitMarkers(odtClause.getMarkers(), p);
        p.append(odtClause.getWords());
        visit(odtClause.getMnemonicName(), p);
        return odtClause;
    }

    public Cobol visitReserveNetworkClause(Cobol.ReserveNetworkClause reserveNetworkClause, PrintOutputCapture<P> p) {
        visitSpace(reserveNetworkClause.getPrefix(), p);
        visitMarkers(reserveNetworkClause.getMarkers(), p);
        p.append(reserveNetworkClause.getWords());
        return reserveNetworkClause;
    }

    public Cobol visitSymbolicCharacter(Cobol.SymbolicCharacter symbolicCharacter, PrintOutputCapture<P> p) {
        visitSpace(symbolicCharacter.getPrefix(), p);
        visitMarkers(symbolicCharacter.getMarkers(), p);
        visitContainer("", symbolicCharacter.getPadding().getSymbols(), "", "", p);
        visitContainer("", symbolicCharacter.getPadding().getLiterals(), "", "", p);
        return symbolicCharacter;
    }

    public Cobol visitSymbolicCharactersClause(Cobol.SymbolicCharactersClause symbolicCharactersClause, PrintOutputCapture<P> p) {
        visitSpace(symbolicCharactersClause.getPrefix(), p);
        visitMarkers(symbolicCharactersClause.getMarkers(), p);
        p.append(symbolicCharactersClause.getWords());
        visitContainer("", symbolicCharactersClause.getPadding().getSymbols(), "", "", p);
        visitLeftPadded("", symbolicCharactersClause.getPadding().getInAlphabet(), p);
        visit(symbolicCharactersClause.getAlphabetName(), p);
        return symbolicCharactersClause;
    }

    public Cobol visitFileSection(Cobol.FileSection fileSection, PrintOutputCapture<P> p) {
        visitSpace(fileSection.getPrefix(), p);
        visitMarkers(fileSection.getMarkers(), p);
        p.append(fileSection.getWords());
        visitContainer(".", fileSection.getPadding().getFileDescriptionEntry(), "", "", p);
        return fileSection;
    }

    public Cobol visitFileDescriptionEntry(Cobol.FileDescriptionEntry fileDescriptionEntry, PrintOutputCapture<P> p) {
        visitSpace(fileDescriptionEntry.getPrefix(), p);
        visitMarkers(fileDescriptionEntry.getMarkers(), p);
        p.append(fileDescriptionEntry.getWords());
        visit(fileDescriptionEntry.getName(), p);
        visitContainer("", fileDescriptionEntry.getPadding().getClauses(), "", "", p);
        visitContainer(".", fileDescriptionEntry.getPadding().getDataDescriptions(), "", "", p);
        return fileDescriptionEntry;
    }

    public Cobol visitLinkageSection(Cobol.LinkageSection linkageSection, PrintOutputCapture<P> p) {
        visitSpace(linkageSection.getPrefix(), p);
        visitMarkers(linkageSection.getMarkers(), p);
        p.append(linkageSection.getWords());
        visitContainer(".", linkageSection.getPadding().getDataDescriptions(), "", "", p);
        return linkageSection;
    }

    public Cobol visitLocalStorageSection(Cobol.LocalStorageSection localStorageSection, PrintOutputCapture<P> p) {
        visitSpace(localStorageSection.getPrefix(), p);
        visitMarkers(localStorageSection.getMarkers(), p);
        p.append(localStorageSection.getWords());
        if (localStorageSection.getLocalName() != null) {
            p.append(localStorageSection.getLocalData());
            p.append(localStorageSection.getLocalName().getSimpleName());
        }
        visitContainer(".", localStorageSection.getPadding().getDataDescriptions(), "", "", p);
        return localStorageSection;
    }

    public Cobol visitDataBaseSection(Cobol.DataBaseSection dataBaseSection, PrintOutputCapture<P> p) {
        visitSpace(dataBaseSection.getPrefix(), p);
        visitMarkers(dataBaseSection.getMarkers(), p);
        p.append(dataBaseSection.getWords());
        visitContainer(".", dataBaseSection.getPadding().getEntries(), "", "", p);
        return dataBaseSection;
    }

    public Cobol visitDataBaseSectionEntry(Cobol.DataBaseSectionEntry dataBaseSectionEntry, PrintOutputCapture<P> p) {
        visitSpace(dataBaseSectionEntry.getPrefix(), p);
        visitMarkers(dataBaseSectionEntry.getMarkers(), p);
        p.append(dataBaseSectionEntry.getDb());
        visit(dataBaseSectionEntry.getFrom(), p);
        p.append(dataBaseSectionEntry.getInvoke());
        visit(dataBaseSectionEntry.getTo(), p);
        return dataBaseSectionEntry;
    }

    public Cobol visitScreenSection(Cobol.ScreenSection screenSection, PrintOutputCapture<P> p) {
        visitSpace(screenSection.getPrefix(), p);
        visitMarkers(screenSection.getMarkers(), p);
        p.append(screenSection.getWords());
        visitContainer(".", screenSection.getPadding().getDescriptions(), "", "", p);
        return screenSection;
    }

    public Cobol visitScreenDescriptionEntry(Cobol.ScreenDescriptionEntry screenDescriptionEntry, PrintOutputCapture<P> p) {
        visitSpace(screenDescriptionEntry.getPrefix(), p);
        visitMarkers(screenDescriptionEntry.getMarkers(), p);
        p.append(screenDescriptionEntry.getWords());
        visitLeftPadded("", screenDescriptionEntry.getPadding().getName(), p);
        visitContainer("", screenDescriptionEntry.getPadding().getClauses(), "", ".", p);
        return screenDescriptionEntry;
    }

    public Cobol visitScreenDescriptionBlankClause(Cobol.ScreenDescriptionBlankClause screenDescriptionBlankClause, PrintOutputCapture<P> p) {
        visitSpace(screenDescriptionBlankClause.getPrefix(), p);
        visitMarkers(screenDescriptionBlankClause.getMarkers(), p);
        p.append(screenDescriptionBlankClause.getWords());
        return screenDescriptionBlankClause;
    }

    public Cobol visitScreenDescriptionControlClause(Cobol.ScreenDescriptionControlClause screenDescriptionControlClause, PrintOutputCapture<P> p) {
        visitSpace(screenDescriptionControlClause.getPrefix(), p);
        visitMarkers(screenDescriptionControlClause.getMarkers(), p);
        p.append(screenDescriptionControlClause.getWords());
        visit(screenDescriptionControlClause.getIdentifier(), p);
        return screenDescriptionControlClause;
    }

    public Cobol visitScreenDescriptionSizeClause(Cobol.ScreenDescriptionSizeClause screenDescriptionSizeClause, PrintOutputCapture<P> p) {
        visitSpace(screenDescriptionSizeClause.getPrefix(), p);
        visitMarkers(screenDescriptionSizeClause.getMarkers(), p);
        p.append(screenDescriptionSizeClause.getWords());
        visit(screenDescriptionSizeClause.getIdentifier(), p);
        return screenDescriptionSizeClause;
    }

    public Cobol visitScreenDescriptionToClause(Cobol.ScreenDescriptionToClause screenDescriptionToClause, PrintOutputCapture<P> p) {
        visitSpace(screenDescriptionToClause.getPrefix(), p);
        visitMarkers(screenDescriptionToClause.getMarkers(), p);
        p.append(screenDescriptionToClause.getWords());
        visit(screenDescriptionToClause.getIdentifier(), p);
        return screenDescriptionToClause;
    }

    public Cobol visitScreenDescriptionUsingClause(Cobol.ScreenDescriptionUsingClause screenDescriptionUsingClause, PrintOutputCapture<P> p) {
        visitSpace(screenDescriptionUsingClause.getPrefix(), p);
        visitMarkers(screenDescriptionUsingClause.getMarkers(), p);
        p.append(screenDescriptionUsingClause.getWords());
        visit(screenDescriptionUsingClause.getIdentifier(), p);
        return screenDescriptionUsingClause;
    }

    public Cobol visitAccept(Cobol.Accept accept, PrintOutputCapture<P> p) {
        visitSpace(accept.getPrefix(), p);
        visitMarkers(accept.getMarkers(), p);
        p.append(accept.getAccept());
        visit(accept.getIdentifier(), p);
        visit(accept.getOperation(), p);
        visit(accept.getOnExceptionClause(), p);
        visit(accept.getNotOnExceptionClause(), p);
        visitLeftPadded("", accept.getPadding().getEndAccept(), p);
        return accept;
    }

    public Cobol visitAcceptFromDateStatement(Cobol.AcceptFromDateStatement acceptFromDateStatement, PrintOutputCapture<P> p) {
        visitSpace(acceptFromDateStatement.getPrefix(), p);
        visitMarkers(acceptFromDateStatement.getMarkers(), p);
        p.append(acceptFromDateStatement.getWords());
        return acceptFromDateStatement;
    }

    public Cobol visitAcceptFromMnemonicStatement(Cobol.AcceptFromMnemonicStatement acceptFromMnemonicStatement, PrintOutputCapture<P> p) {
        visitSpace(acceptFromMnemonicStatement.getPrefix(), p);
        visitMarkers(acceptFromMnemonicStatement.getMarkers(), p);
        p.append(acceptFromMnemonicStatement.getFrom());
        visit(acceptFromMnemonicStatement.getMnemonicName(), p);
        return acceptFromMnemonicStatement;
    }

    public Cobol visitAcceptFromEscapeKeyStatement(Cobol.AcceptFromEscapeKeyStatement acceptFromEscapeKeyStatement, PrintOutputCapture<P> p) {
        visitSpace(acceptFromEscapeKeyStatement.getPrefix(), p);
        visitMarkers(acceptFromEscapeKeyStatement.getMarkers(), p);
        p.append(acceptFromEscapeKeyStatement.getWords());
        return acceptFromEscapeKeyStatement;
    }

    public Cobol visitAcceptMessageCountStatement(Cobol.AcceptMessageCountStatement acceptMessageCountStatement, PrintOutputCapture<P> p) {
        visitSpace(acceptMessageCountStatement.getPrefix(), p);
        visitMarkers(acceptMessageCountStatement.getMarkers(), p);
        p.append(acceptMessageCountStatement.getWords());
        return acceptMessageCountStatement;
    }

    public Cobol visitOnExceptionClause(Cobol.OnExceptionClause onExceptionClause, PrintOutputCapture<P> p) {
        visitSpace(onExceptionClause.getPrefix(), p);
        visitMarkers(onExceptionClause.getMarkers(), p);
        p.append(onExceptionClause.getWords());
        return onExceptionClause;
    }

    public Cobol visitNotOnExceptionClause(Cobol.NotOnExceptionClause notOnExceptionClause, PrintOutputCapture<P> p) {
        visitSpace(notOnExceptionClause.getPrefix(), p);
        visitMarkers(notOnExceptionClause.getMarkers(), p);
        p.append(notOnExceptionClause.getWords());
        return notOnExceptionClause;
    }

    public Cobol visitAlterStatement(Cobol.AlterStatement alterStatement, PrintOutputCapture<P> p) {
        visitSpace(alterStatement.getPrefix(), p);
        visitMarkers(alterStatement.getMarkers(), p);
        p.append(alterStatement.getWords());
        for (Cobol.AlterProceedTo a : alterStatement.getAlterProceedTo()) {
            visitAlterProceedTo(a, p);
        }
        return alterStatement;
    }

    public Cobol visitAlterProceedTo(Cobol.AlterProceedTo alterProceedTo, PrintOutputCapture<P> p) {
        visitSpace(alterProceedTo.getPrefix(), p);
        visitMarkers(alterProceedTo.getMarkers(), p);
        visit(alterProceedTo.getFrom(), p);
        p.append(alterProceedTo.getWords());
        visit(alterProceedTo.getTo(), p);
        return alterProceedTo;
    }

    public Cobol visitProcedureName(Cobol.ProcedureName procedureName, PrintOutputCapture<P> p) {
        visitSpace(procedureName.getPrefix(), p);
        visitMarkers(procedureName.getMarkers(), p);
        visit(procedureName.getParagraphName(), p);
        visit(procedureName.getInSection(), p);
        visit(procedureName.getSectionName(), p);
        return procedureName;
    }

    public Cobol visitInSection(Cobol.InSection inSection, PrintOutputCapture<P> p) {
        visitSpace(inSection.getPrefix(), p);
        visitMarkers(inSection.getMarkers(), p);
        p.append(inSection.getWords());
        visit(inSection.getName(), p);
        return inSection;
    }

    public Cobol visitCancel(Cobol.Cancel cancel, PrintOutputCapture<P> p) {
        visitSpace(cancel.getPrefix(), p);
        visitMarkers(cancel.getMarkers(), p);
        p.append(cancel.getCancel());
        visitContainer("", cancel.getPadding().getCancelCalls(), "", "", p);
        return cancel;
    }

    public Cobol visitCancelCall(Cobol.CancelCall cancelCall, PrintOutputCapture<P> p) {
        visitSpace(cancelCall.getPrefix(), p);
        visitMarkers(cancelCall.getMarkers(), p);
        p.append(cancelCall.getBy());
        visit(cancelCall.getIdentifier(), p);
        visit(cancelCall.getLiteral(), p);
        return cancelCall;
    }

    public Cobol visitClose(Cobol.Close close, PrintOutputCapture<P> p) {
        visitSpace(close.getPrefix(), p);
        visitMarkers(close.getMarkers(), p);
        p.append(close.getClose());
        visitContainer("", close.getPadding().getCloseFiles(), "", "", p);
        return close;
    }

    public Cobol visitCloseFile(Cobol.CloseFile closeFile, PrintOutputCapture<P> p) {
        visitSpace(closeFile.getPrefix(), p);
        visitMarkers(closeFile.getMarkers(), p);
        visit(closeFile.getCloseStatement(), p);
        return closeFile;
    }

    public Cobol visitCloseReelUnitStatement(Cobol.CloseReelUnitStatement closeReelUnitStatement, PrintOutputCapture<P> p) {
        visitSpace(closeReelUnitStatement.getPrefix(), p);
        visitMarkers(closeReelUnitStatement.getMarkers(), p);
        p.append(closeReelUnitStatement.getWords());
        return closeReelUnitStatement;
    }

    public Cobol visitCloseRelativeStatement(Cobol.CloseRelativeStatement closeRelativeStatement, PrintOutputCapture<P> p) {
        visitSpace(closeRelativeStatement.getPrefix(), p);
        visitMarkers(closeRelativeStatement.getMarkers(), p);
        p.append(closeRelativeStatement.getWords());
        return closeRelativeStatement;
    }

    public Cobol visitClosePortFileIOStatement(Cobol.ClosePortFileIOStatement closePortFileIOStatement, PrintOutputCapture<P> p) {
        visitSpace(closePortFileIOStatement.getPrefix(), p);
        visitMarkers(closePortFileIOStatement.getMarkers(), p);
        p.append(closePortFileIOStatement.getWords());
        visitContainer("", closePortFileIOStatement.getPadding().getClosePortFileIOUsing(), "", "", p);
        return closePortFileIOStatement;
    }

    public Cobol visitClosePortFileIOUsingCloseDisposition(Cobol.ClosePortFileIOUsingCloseDisposition closePortFileIOUsingCloseDisposition, PrintOutputCapture<P> p) {
        visitSpace(closePortFileIOUsingCloseDisposition.getPrefix(), p);
        visitMarkers(closePortFileIOUsingCloseDisposition.getMarkers(), p);
        p.append(closePortFileIOUsingCloseDisposition.getWords());
        return closePortFileIOUsingCloseDisposition;
    }

    public Cobol visitClosePortFileIOUsingAssociatedData(Cobol.ClosePortFileIOUsingAssociatedData closePortFileIOUsingAssociatedData, PrintOutputCapture<P> p) {
        visitSpace(closePortFileIOUsingAssociatedData.getPrefix(), p);
        visitMarkers(closePortFileIOUsingAssociatedData.getMarkers(), p);
        p.append(closePortFileIOUsingAssociatedData.getAssociatedData());
        visit(closePortFileIOUsingAssociatedData.getIdentifier(), p);
        return closePortFileIOUsingAssociatedData;
    }

    public Cobol visitClosePortFileIOUsingAssociatedDataLength(Cobol.ClosePortFileIOUsingAssociatedDataLength closePortFileIOUsingAssociatedDataLength, PrintOutputCapture<P> p) {
        visitSpace(closePortFileIOUsingAssociatedDataLength.getPrefix(), p);
        visitMarkers(closePortFileIOUsingAssociatedDataLength.getMarkers(), p);
        p.append(closePortFileIOUsingAssociatedDataLength.getWords());
        visit(closePortFileIOUsingAssociatedDataLength.getIdentifier(), p);
        return closePortFileIOUsingAssociatedDataLength;
    }

    public Cobol visitInData(Cobol.InData inData, PrintOutputCapture<P> p) {
        visitSpace(inData.getPrefix(), p);
        visitMarkers(inData.getMarkers(), p);
        p.append(inData.getWords());
        return inData;
    }

    public Cobol visitInFile(Cobol.InFile inFile, PrintOutputCapture<P> p) {
        visitSpace(inFile.getPrefix(), p);
        visitMarkers(inFile.getMarkers(), p);
        p.append(inFile.getWords());
        return inFile;
    }

    public Cobol visitInMnemonic(Cobol.InMnemonic inMnemonic, PrintOutputCapture<P> p) {
        visitSpace(inMnemonic.getPrefix(), p);
        visitMarkers(inMnemonic.getMarkers(), p);
        p.append(inMnemonic.getWords());
        return inMnemonic;
    }

    public Cobol visitInLibrary(Cobol.InLibrary inLibrary, PrintOutputCapture<P> p) {
        visitSpace(inLibrary.getPrefix(), p);
        visitMarkers(inLibrary.getMarkers(), p);
        p.append(inLibrary.getWords());
        return inLibrary;
    }

    public Cobol visitInTable(Cobol.InTable inTable, PrintOutputCapture<P> p) {
        visitSpace(inTable.getPrefix(), p);
        visitMarkers(inTable.getMarkers(), p);
        p.append(inTable.getWords());
        return inTable;
    }

    public Cobol visitContinue(Cobol.Continue continuez, PrintOutputCapture<P> p) {
        visitSpace(continuez.getPrefix(), p);
        visitMarkers(continuez.getMarkers(), p);
        p.append(continuez.getWord());
        return continuez;
    }

    public Cobol visitDelete(Cobol.Delete delete, PrintOutputCapture<P> p) {
        visitSpace(delete.getPrefix(), p);
        visitMarkers(delete.getMarkers(), p);
        p.append(delete.getDelete());
        p.append(delete.getRecord());
        visit(delete.getInvalidKey(), p);
        visit(delete.getNotInvalidKey(), p);
        visitLeftPadded("", delete.getPadding().getEndDelete(), p);
        return delete;
    }

    public Cobol visitDisable(Cobol.Disable disable, PrintOutputCapture<P> p) {
        visitSpace(disable.getPrefix(), p);
        visitMarkers(disable.getMarkers(), p);
        p.append(disable.getDisable());
        p.append(disable.getType());
        p.append(disable.getWith());
        p.append(disable.getKey());
        return disable;
    }

    public Cobol visitEnable(Cobol.Enable enable, PrintOutputCapture<P> p) {
        visitSpace(enable.getPrefix(), p);
        visitMarkers(enable.getMarkers(), p);
        p.append(enable.getEnable());
        p.append(enable.getType());
        p.append(enable.getWith());
        p.append(enable.getKey());
        return enable;
    }

    public Cobol visitExhibit(Cobol.Exhibit exhibit, PrintOutputCapture<P> p) {
        visitSpace(exhibit.getPrefix(), p);
        visitMarkers(exhibit.getMarkers(), p);
        p.append(exhibit.getWords());
        visitContainer("", exhibit.getPadding().getOperands(), "", "", p);
        return exhibit;
    }

    public Cobol visitExit(Cobol.Exit exit, PrintOutputCapture<P> p) {
        visitSpace(exit.getPrefix(), p);
        visitMarkers(exit.getMarkers(), p);
        p.append(exit.getWords());
        return exit;
    }

    public Cobol visitGenerate(Cobol.Generate generate, PrintOutputCapture<P> p) {
        visitSpace(generate.getPrefix(), p);
        visitMarkers(generate.getMarkers(), p);
        p.append(generate.getGenerate());
        visit(generate.getReportName(), p);
        return generate;
    }

    public Cobol visitQualifiedDataName(Cobol.QualifiedDataName qualifiedDataName, PrintOutputCapture<P> p) {
        visitSpace(qualifiedDataName.getPrefix(), p);
        visitMarkers(qualifiedDataName.getMarkers(), p);
        visit(qualifiedDataName.getDataName(), p);
        return qualifiedDataName;
    }

    public Cobol visitQualifiedDataNameFormat1(Cobol.QualifiedDataNameFormat1 qualifiedDataNameFormat1, PrintOutputCapture<P> p) {
        visitSpace(qualifiedDataNameFormat1.getPrefix(), p);
        visitMarkers(qualifiedDataNameFormat1.getMarkers(), p);
        visitContainer("", qualifiedDataNameFormat1.getPadding().getQualifiedInData(), "", "", p);
        visit(qualifiedDataNameFormat1.getInFile(), p);
        return qualifiedDataNameFormat1;
    }

    public Cobol visitQualifiedDataNameFormat2(Cobol.QualifiedDataNameFormat2 qualifiedDataNameFormat2, PrintOutputCapture<P> p) {
        visitSpace(qualifiedDataNameFormat2.getPrefix(), p);
        visitMarkers(qualifiedDataNameFormat2.getMarkers(), p);
        visit(qualifiedDataNameFormat2.getInSection(), p);
        return qualifiedDataNameFormat2;
    }

    public Cobol visitQualifiedDataNameFormat3(Cobol.QualifiedDataNameFormat3 qualifiedDataNameFormat3, PrintOutputCapture<P> p) {
        visitSpace(qualifiedDataNameFormat3.getPrefix(), p);
        visitMarkers(qualifiedDataNameFormat3.getMarkers(), p);
        visit(qualifiedDataNameFormat3.getInLibrary(), p);
        return qualifiedDataNameFormat3;
    }

    public Cobol visitQualifiedDataNameFormat4(Cobol.QualifiedDataNameFormat4 qualifiedDataNameFormat4, PrintOutputCapture<P> p) {
        visitSpace(qualifiedDataNameFormat4.getPrefix(), p);
        visitMarkers(qualifiedDataNameFormat4.getMarkers(), p);
        p.append(qualifiedDataNameFormat4.getLinageCounter());
        visit(qualifiedDataNameFormat4.getInFile(), p);
        return qualifiedDataNameFormat4;
    }

    public Cobol visitQualifiedInData(Cobol.QualifiedInData qualifiedInData, PrintOutputCapture<P> p) {
        visitSpace(qualifiedInData.getPrefix(), p);
        visitMarkers(qualifiedInData.getMarkers(), p);
        visit(qualifiedInData.getIn(), p);
        return qualifiedInData;
    }

    public Cobol visitReportName(Cobol.ReportName reportName, PrintOutputCapture<P> p) {
        visitSpace(reportName.getPrefix(), p);
        visitMarkers(reportName.getMarkers(), p);
        visit(reportName.getQualifiedDataName(), p);
        return reportName;
    }
}
