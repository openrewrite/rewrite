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
        visit(procedureDivision.getProcedureDivisionUsingClause(), p);
        visit(procedureDivision.getProcedureDivisionGivingClause(), p);
        visitLeftPadded("", procedureDivision.getPadding().getDot(), p);
        visit(procedureDivision.getProcedureDeclaratives(), p);
        visitLeftPadded("", procedureDivision.getPadding().getBody(), p);
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
        for (Cobol pp : procedureDivisionUsingClause.getProcedureDivisionUsingParameter()) {
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
        visitContainer("", procedureDivisionBody.getPadding().getProcedureSection(), "", "", p);
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
        visitContainer("", paragraphs.getPadding().getSentences(), "", "", p);
        visitContainer("", paragraphs.getPadding().getParagraphs(), "", "", p);
        return paragraphs;
    }

    public Cobol visitParagraph(Cobol.Paragraph paragraph, PrintOutputCapture<P> p) {
        visitSpace(paragraph.getPrefix(), p);
        visitMarkers(paragraph.getMarkers(), p);
        visit(paragraph.getParagraphName(), p);
        visitLeftPadded("", paragraph.getPadding().getDot(), p);
        visit(paragraph.getAlteredGoTo(), p);
        visitContainer("", paragraph.getPadding().getSentences(), "", "", p);
        return paragraph;
    }

    public Cobol visitSentence(Cobol.Sentence sentence, PrintOutputCapture<P> p) {
        visitSpace(sentence.getPrefix(), p);
        visitMarkers(sentence.getMarkers(), p);
        for (Statement s : sentence.getStatements()) {
            visit(s, p);
        }
        visitLeftPadded("", sentence.getPadding().getDot(), p);
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
            visit(localStorageSection.getLocalName(), p);
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
        visit(cancelCall.getLibraryName(), p);
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
        visit(closeFile.getFileName(), p);
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
        visit(inData.getName(), p);
        return inData;
    }

    public Cobol visitInFile(Cobol.InFile inFile, PrintOutputCapture<P> p) {
        visitSpace(inFile.getPrefix(), p);
        visitMarkers(inFile.getMarkers(), p);
        p.append(inFile.getWords());
        visit(inFile.getName(), p);
        return inFile;
    }

    public Cobol visitInMnemonic(Cobol.InMnemonic inMnemonic, PrintOutputCapture<P> p) {
        visitSpace(inMnemonic.getPrefix(), p);
        visitMarkers(inMnemonic.getMarkers(), p);
        p.append(inMnemonic.getWords());
        visit(inMnemonic.getName(), p);
        return inMnemonic;
    }

    public Cobol visitInLibrary(Cobol.InLibrary inLibrary, PrintOutputCapture<P> p) {
        visitSpace(inLibrary.getPrefix(), p);
        visitMarkers(inLibrary.getMarkers(), p);
        p.append(inLibrary.getWords());
        visit(inLibrary.getName(), p);
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
        visit(qualifiedDataNameFormat1.getName(), p);
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

    public Cobol visitAlteredGoTo(Cobol.AlteredGoTo alteredGoTo, PrintOutputCapture<P> p) {
        visitSpace(alteredGoTo.getPrefix(), p);
        visitMarkers(alteredGoTo.getMarkers(), p);
        p.append(alteredGoTo.getWords());
        visitLeftPadded("", alteredGoTo.getPadding().getDot(), p);
        return alteredGoTo;
    }

    public Cobol visitProcedureDeclaratives(Cobol.ProcedureDeclaratives procedureDeclaratives, PrintOutputCapture<P> p) {
        visitSpace(procedureDeclaratives.getPrefix(), p);
        visitMarkers(procedureDeclaratives.getMarkers(), p);
        p.append(procedureDeclaratives.getDeclaratives());
        visitContainer(".", procedureDeclaratives.getPadding().getProcedureDeclarative(), "", "", p);
        p.append(procedureDeclaratives.getEndDeclaratives());
        visitLeftPadded("", procedureDeclaratives.getPadding().getDot(), p);
        return procedureDeclaratives;
    }

    public Cobol visitProcedureDeclarative(Cobol.ProcedureDeclarative procedureDeclarative, PrintOutputCapture<P> p) {
        visitSpace(procedureDeclarative.getPrefix(), p);
        visitMarkers(procedureDeclarative.getMarkers(), p);
        visit(procedureDeclarative.getProcedureSectionHeader(), p);
        visitLeftPadded(".", procedureDeclarative.getPadding().getUseStatement(), p);
        visitLeftPadded(".", procedureDeclarative.getPadding().getParagraphs(), p);
        return procedureDeclarative;
    }

    public Cobol visitProcedureSection(Cobol.ProcedureSection procedureSection, PrintOutputCapture<P> p) {
        visitSpace(procedureSection.getPrefix(), p);
        visitMarkers(procedureSection.getMarkers(), p);
        visit(procedureSection.getProcedureSectionHeader(), p);
        visitLeftPadded("", procedureSection.getPadding().getDot(), p);
        visit(procedureSection.getParagraphs(), p);
        return procedureSection;
    }

    public Cobol visitProcedureSectionHeader(Cobol.ProcedureSectionHeader procedureSectionHeader, PrintOutputCapture<P> p) {
        visitSpace(procedureSectionHeader.getPrefix(), p);
        visitMarkers(procedureSectionHeader.getMarkers(), p);
        visit(procedureSectionHeader.getSectionName(), p);
        p.append(procedureSectionHeader.getSection());
        visit(procedureSectionHeader.getIdentifier(), p);
        return procedureSectionHeader;
    }

    public Cobol visitProcedureDivisionGivingClause(Cobol.ProcedureDivisionGivingClause procedureDivisionGivingClause, PrintOutputCapture<P> p) {
        visitSpace(procedureDivisionGivingClause.getPrefix(), p);
        visitMarkers(procedureDivisionGivingClause.getMarkers(), p);
        p.append(procedureDivisionGivingClause.getWords());
        visit(procedureDivisionGivingClause.getDataName(), p);
        return procedureDivisionGivingClause;
    }

    public Cobol visitUseStatement(Cobol.UseStatement useStatement, PrintOutputCapture<P> p) {
        visitSpace(useStatement.getPrefix(), p);
        visitMarkers(useStatement.getMarkers(), p);
        p.append(useStatement.getUse());
        visit(useStatement.getClause(), p);
        return useStatement;
    }

    public Cobol visitUseAfterClause(Cobol.UseAfterClause useAfterClause, PrintOutputCapture<P> p) {
        visitSpace(useAfterClause.getPrefix(), p);
        visitMarkers(useAfterClause.getMarkers(), p);
        p.append(useAfterClause.getWords());
        visit(useAfterClause.getUseAfterOn(), p);
        return useAfterClause;
    }

    public Cobol visitUseAfterOn(Cobol.UseAfterOn useAfterOn, PrintOutputCapture<P> p) {
        visitSpace(useAfterOn.getPrefix(), p);
        visitMarkers(useAfterOn.getMarkers(), p);
        p.append(useAfterOn.getAfterOn());
        visitContainer("", useAfterOn.getPadding().getFileNames(), "", "", p);
        return useAfterOn;
    }

    public Cobol visitUseDebugClause(Cobol.UseDebugClause useDebugClause, PrintOutputCapture<P> p) {
        visitSpace(useDebugClause.getPrefix(), p);
        visitMarkers(useDebugClause.getMarkers(), p);
        p.append(useDebugClause.getWords());
        visitContainer("", useDebugClause.getPadding().getUseDebugs(), "", "", p);
        return useDebugClause;
    }

    public Cobol visitUseDebugOn(Cobol.UseDebugOn useDebugOn, PrintOutputCapture<P> p) {
        visitSpace(useDebugOn.getPrefix(), p);
        visitMarkers(useDebugOn.getMarkers(), p);
        p.append(useDebugOn.getWords());
        visit(useDebugOn.getProcedureName(), p);
        return useDebugOn;
    }

    public Cobol visitRewrite(Cobol.Rewrite rewrite, PrintOutputCapture<P> p) {
        visitSpace(rewrite.getPrefix(), p);
        visitMarkers(rewrite.getMarkers(), p);
        p.append(rewrite.getRewrite());
        visit(rewrite.getRecordName(), p);
        visit(rewrite.getInvalidKeyPhrase(), p);
        visit(rewrite.getNotInvalidKeyPhrase(), p);
        visitLeftPadded("", rewrite.getPadding().getEndRewrite(), p);
        return rewrite;
    }

    public Cobol visitRewriteFrom(Cobol.RewriteFrom rewriteFrom, PrintOutputCapture<P> p) {
        visitSpace(rewriteFrom.getPrefix(), p);
        visitMarkers(rewriteFrom.getMarkers(), p);
        p.append(rewriteFrom.getFrom());
        return rewriteFrom;
    }

    public Cobol visitCall(Cobol.Call call, PrintOutputCapture<P> p) {
        visitSpace(call.getPrefix(), p);
        visitMarkers(call.getMarkers(), p);
        p.append(call.getCall());
        visit(call.getIdentifier(), p);
        visit(call.getCallUsingPhrase(), p);
        visit(call.getCallGivingPhrase(), p);
        visit(call.getOnOverflowPhrase(), p);
        visit(call.getOnExceptionClause(), p);
        visit(call.getNotOnExceptionClause(), p);
        visitLeftPadded("", call.getPadding().getEndCall(), p);
        return call;
    }

    public Cobol visitCallPhrase(Cobol.CallPhrase callPhrase, PrintOutputCapture<P> p) {
        visitSpace(callPhrase.getPrefix(), p);
        visitMarkers(callPhrase.getMarkers(), p);
        p.append(callPhrase.getWords());
        visitContainer("", callPhrase.getPadding().getParameters(), "", "", p);
        return callPhrase;
    }

    public Cobol visitCallBy(Cobol.CallBy callBy, PrintOutputCapture<P> p) {
        visitSpace(callBy.getPrefix(), p);
        visitMarkers(callBy.getMarkers(), p);
        p.append(callBy.getWords());
        visit(callBy.getIdentifier(), p);
        return callBy;
    }

    public Cobol visitCallGivingPhrase(Cobol.CallGivingPhrase callGivingPhrase, PrintOutputCapture<P> p) {
        visitSpace(callGivingPhrase.getPrefix(), p);
        visitMarkers(callGivingPhrase.getMarkers(), p);
        p.append(callGivingPhrase.getWords());
        visit(callGivingPhrase.getIdentifier(), p);
        return callGivingPhrase;
    }

    public Cobol visitMoveStatement(Cobol.MoveStatement moveStatement, PrintOutputCapture<P> p) {
        visitSpace(moveStatement.getPrefix(), p);
        visitMarkers(moveStatement.getMarkers(), p);
        p.append(moveStatement.getWords());
        visit(moveStatement.getMoveToStatement(), p);
        return moveStatement;
    }

    public Cobol visitMoveToStatement(Cobol.MoveToStatement moveToStatement, PrintOutputCapture<P> p) {
        visitSpace(moveToStatement.getPrefix(), p);
        visitMarkers(moveToStatement.getMarkers(), p);
        visit(moveToStatement.getFrom(), p);
        visitContainer("", moveToStatement.getPadding().getTo(), "", "", p);
        return moveToStatement;
    }

    public Cobol visitMoveCorrespondingToStatement(Cobol.MoveCorrespondingToStatement moveCorrespondingToStatement, PrintOutputCapture<P> p) {
        visitSpace(moveCorrespondingToStatement.getPrefix(), p);
        visitMarkers(moveCorrespondingToStatement.getMarkers(), p);
        p.append(moveCorrespondingToStatement.getWords());
        visit(moveCorrespondingToStatement.getMoveCorrespondingToSendingArea(), p);
        visitContainer("", moveCorrespondingToStatement.getPadding().getTo(), "", "", p);
        return moveCorrespondingToStatement;
    }

    public Cobol visitWrite(Cobol.Write write, PrintOutputCapture<P> p) {
        visitSpace(write.getPrefix(), p);
        visitMarkers(write.getMarkers(), p);
        p.append(write.getWrite());
        visit(write.getRecordName(), p);
        visit(write.getWriteFromPhrase(), p);
        visit(write.getWriteAdvancingPhrase(), p);
        visit(write.getWriteAtEndOfPagePhrase(), p);
        visit(write.getWriteNotAtEndOfPagePhrase(), p);
        visit(write.getInvalidKeyPhrase(), p);
        visit(write.getNotInvalidKeyPhrase(), p);
        visitLeftPadded("", write.getPadding().getEndWrite(), p);
        return write;
    }

    public Cobol visitWriteFromPhrase(Cobol.WriteFromPhrase writeFromPhrase, PrintOutputCapture<P> p) {
        visitSpace(writeFromPhrase.getPrefix(), p);
        visitMarkers(writeFromPhrase.getMarkers(), p);
        p.append(writeFromPhrase.getFrom());
        visit(writeFromPhrase.getName(), p);
        return writeFromPhrase;
    }

    public Cobol visitWriteAdvancingPhrase(Cobol.WriteAdvancingPhrase writeAdvancingPhrase, PrintOutputCapture<P> p) {
        visitSpace(writeAdvancingPhrase.getPrefix(), p);
        visitMarkers(writeAdvancingPhrase.getMarkers(), p);
        p.append(writeAdvancingPhrase.getWords());
        visit(writeAdvancingPhrase.getWriteBy(), p);
        return writeAdvancingPhrase;
    }

    public Cobol visitWriteAdvancingPage(Cobol.WriteAdvancingPage writeAdvancingPage, PrintOutputCapture<P> p) {
        visitSpace(writeAdvancingPage.getPrefix(), p);
        visitMarkers(writeAdvancingPage.getMarkers(), p);
        p.append(writeAdvancingPage.getPage());
        return writeAdvancingPage;
    }

    public Cobol visitWriteAdvancingLines(Cobol.WriteAdvancingLines writeAdvancingLines, PrintOutputCapture<P> p) {
        visitSpace(writeAdvancingLines.getPrefix(), p);
        visitMarkers(writeAdvancingLines.getMarkers(), p);
        visit(writeAdvancingLines.getName(), p);
        p.append(writeAdvancingLines.getWords());
        return writeAdvancingLines;
    }

    public Cobol visitWriteAdvancingMnemonic(Cobol.WriteAdvancingMnemonic writeAdvancingMnemonic, PrintOutputCapture<P> p) {
        visitSpace(writeAdvancingMnemonic.getPrefix(), p);
        visitMarkers(writeAdvancingMnemonic.getMarkers(), p);
        visit(writeAdvancingMnemonic.getName(), p);
        return writeAdvancingMnemonic;
    }

    public Cobol visitArithmeticExpression(Cobol.ArithmeticExpression arithmeticExpression, PrintOutputCapture<P> p) {
        visitSpace(arithmeticExpression.getPrefix(), p);
        visitMarkers(arithmeticExpression.getMarkers(), p);
        visit(arithmeticExpression.getMultDivs(), p);
        visitContainer("", arithmeticExpression.getPadding().getPlusMinuses(), "", "", p);
        return arithmeticExpression;
    }

    public Cobol visitBasis(Cobol.Basis basis, PrintOutputCapture<P> p) {
        visitSpace(basis.getPrefix(), p);
        visitMarkers(basis.getMarkers(), p);
        p.append(basis.getLParen());
        visit(basis.getArithmeticExpression(), p);
        p.append(basis.getRParen());
        visit(basis.getIdentifier(), p);
        visit(basis.getLiteral(), p);
        return basis;
    }

    public Cobol visitComputeStatement(Cobol.ComputeStatement computeStatement, PrintOutputCapture<P> p) {
        visitSpace(computeStatement.getPrefix(), p);
        visitMarkers(computeStatement.getMarkers(), p);
        p.append(computeStatement.getWords());
        visitContainer("", computeStatement.getPadding().getComputeStores(), "", "", p);
        p.append(computeStatement.getEqualWord());
        visit(computeStatement.getArithmeticExpression(), p);
        visit(computeStatement.getOnSizeErrorPhrase(), p);
        visit(computeStatement.getNotOnSizeErrorPhrase(), p);
        return computeStatement;
    }

    public Cobol visitComputeStore(Cobol.ComputeStore computeStore, PrintOutputCapture<P> p) {
        visitSpace(computeStore.getPrefix(), p);
        visitMarkers(computeStore.getMarkers(), p);
        visit(computeStore.getRoundable(), p);
        return computeStore;
    }

    public Cobol visitMultDivs(Cobol.MultDivs multDivs, PrintOutputCapture<P> p) {
        visitSpace(multDivs.getPrefix(), p);
        visitMarkers(multDivs.getMarkers(), p);
        visit(multDivs.getPowers(), p);
        visitContainer("", multDivs.getPadding().getMultDivs(), "", "", p);
        return multDivs;
    }

    public Cobol visitMultDiv(Cobol.MultDiv multDiv, PrintOutputCapture<P> p) {
        visitSpace(multDiv.getPrefix(), p);
        visitMarkers(multDiv.getMarkers(), p);
        p.append(multDiv.getWords());
        visit(multDiv.getPowers(), p);
        return multDiv;
    }

    public Cobol visitPowers(Cobol.Powers powers, PrintOutputCapture<P> p) {
        visitSpace(powers.getPrefix(), p);
        visitMarkers(powers.getMarkers(), p);
        p.append(powers.getWords());
        visit(powers.getBasis(), p);
        visitContainer("", powers.getPadding().getPowers(), "", "", p);
        return powers;
    }

    public Cobol visitPower(Cobol.Power power, PrintOutputCapture<P> p) {
        visitSpace(power.getPrefix(), p);
        visitMarkers(power.getMarkers(), p);
        p.append(power.getWords());
        visit(power.getBasis(), p);
        return power;
    }

    public Cobol visitPlusMinus(Cobol.PlusMinus plusMinus, PrintOutputCapture<P> p) {
        visitSpace(plusMinus.getPrefix(), p);
        visitMarkers(plusMinus.getMarkers(), p);
        p.append(plusMinus.getWords());
        visit(plusMinus.getMultDivs(), p);
        return plusMinus;
    }

    public Cobol visitDivide(Cobol.Divide divide, PrintOutputCapture<P> p) {
        visitSpace(divide.getPrefix(), p);
        visitMarkers(divide.getMarkers(), p);
        p.append(divide.getDivide());
        visit(divide.getName(), p);
        visit(divide.getAction(), p);
        visit(divide.getDivideRemainder(), p);
        visit(divide.getOnSizeErrorPhrase(), p);
        visit(divide.getNotOnSizeErrorPhrase(), p);
        visitLeftPadded("", divide.getPadding().getEndDivide(), p);
        return divide;
    }

    public Cobol visitDivideInto(Cobol.DivideInto divideInto, PrintOutputCapture<P> p) {
        visitSpace(divideInto.getPrefix(), p);
        visitMarkers(divideInto.getMarkers(), p);
        p.append(divideInto.getInto());
        visitContainer("", divideInto.getPadding().getRoundable(), "", "", p);
        return divideInto;
    }

    public Cobol visitDivideGiving(Cobol.DivideGiving divideGiving, PrintOutputCapture<P> p) {
        visitSpace(divideGiving.getPrefix(), p);
        visitMarkers(divideGiving.getMarkers(), p);
        p.append(divideGiving.getWord());
        visit(divideGiving.getName(), p);
        visit(divideGiving.getDivideGivingPhrase(), p);
        return divideGiving;
    }

    public Cobol visitDivideGivingPhrase(Cobol.DivideGivingPhrase divideGivingPhrase, PrintOutputCapture<P> p) {
        visitSpace(divideGivingPhrase.getPrefix(), p);
        visitMarkers(divideGivingPhrase.getMarkers(), p);
        p.append(divideGivingPhrase.getGiving());
        visitContainer("", divideGivingPhrase.getPadding().getRoundable(), "", "", p);
        return divideGivingPhrase;
    }

    public Cobol visitDivideRemainder(Cobol.DivideRemainder divideRemainder, PrintOutputCapture<P> p) {
        visitSpace(divideRemainder.getPrefix(), p);
        visitMarkers(divideRemainder.getMarkers(), p);
        p.append(divideRemainder.getRemainder());
        visit(divideRemainder.getName(), p);
        return divideRemainder;
    }

    public Cobol visitMerge(Cobol.Merge merge, PrintOutputCapture<P> p) {
        visitSpace(merge.getPrefix(), p);
        visitMarkers(merge.getMarkers(), p);
        p.append(merge.getWords());
        visit(merge.getFileName(), p);
        visitContainer("", merge.getPadding().getMergeOnKeyClause(), "", "", p);
        visit(merge.getMergeCollatingSequencePhrase(), p);
        visitContainer("", merge.getPadding().getMergeUsing(), "", "", p);
        visit(merge.getMergeOutputProcedurePhrase(), p);
        visitContainer("", merge.getPadding().getMergeGivingPhrase(), "", "", p);
        return merge;
    }

    public Cobol visitMergeOnKeyClause(Cobol.MergeOnKeyClause mergeOnKeyClause, PrintOutputCapture<P> p) {
        visitSpace(mergeOnKeyClause.getPrefix(), p);
        visitMarkers(mergeOnKeyClause.getMarkers(), p);
        p.append(mergeOnKeyClause.getWords());
        visitContainer("", mergeOnKeyClause.getPadding().getQualifiedDataName(), "", "", p);
        return mergeOnKeyClause;
    }

    public Cobol visitMergeCollatingSequencePhrase(Cobol.MergeCollatingSequencePhrase mergeCollatingSequencePhrase, PrintOutputCapture<P> p) {
        visitSpace(mergeCollatingSequencePhrase.getPrefix(), p);
        visitMarkers(mergeCollatingSequencePhrase.getMarkers(), p);
        p.append(mergeCollatingSequencePhrase.getWords());
        visit(mergeCollatingSequencePhrase.getMergeCollatingAlphanumeric(), p);
        visit(mergeCollatingSequencePhrase.getMergeCollatingNational(), p);
        return mergeCollatingSequencePhrase;
    }

    public Cobol visitMergeable(Cobol.Mergeable mergeable, PrintOutputCapture<P> p) {
        visitSpace(mergeable.getPrefix(), p);
        visitMarkers(mergeable.getMarkers(), p);
        p.append(mergeable.getWords());
        return mergeable;
    }

    public Cobol visitMergeOutputProcedurePhrase(Cobol.MergeOutputProcedurePhrase mergeOutputProcedurePhrase, PrintOutputCapture<P> p) {
        visitSpace(mergeOutputProcedurePhrase.getPrefix(), p);
        visitMarkers(mergeOutputProcedurePhrase.getMarkers(), p);
        p.append(mergeOutputProcedurePhrase.getWords());
        visit(mergeOutputProcedurePhrase.getProcedureName(), p);
        visit(mergeOutputProcedurePhrase.getMergeOutputThrough(), p);
        return mergeOutputProcedurePhrase;
    }

    public Cobol visitMergeGivingPhrase(Cobol.MergeGivingPhrase mergeGivingPhrase, PrintOutputCapture<P> p) {
        visitSpace(mergeGivingPhrase.getPrefix(), p);
        visitMarkers(mergeGivingPhrase.getMarkers(), p);
        p.append(mergeGivingPhrase.getWords());
        visitContainer("", mergeGivingPhrase.getPadding().getMergeGiving(), "", "", p);
        return mergeGivingPhrase;
    }

    public Cobol visitMergeGiving(Cobol.MergeGiving mergeGiving, PrintOutputCapture<P> p) {
        visitSpace(mergeGiving.getPrefix(), p);
        visitMarkers(mergeGiving.getMarkers(), p);
        p.append(mergeGiving.getWords());
        return mergeGiving;
    }

    @Override
    public Cobol visitMergeUsing(Cobol.MergeUsing mergeUsing, PrintOutputCapture<P> p) {
        visitSpace(mergeUsing.getPrefix(), p);
        visitMarkers(mergeUsing.getMarkers(), p);
        p.append(mergeUsing.getWords());
        visit(mergeUsing.getFileNames(), p);
        return mergeUsing;
    }

    @Override
    public @Nullable Cobol visitMergeOutputThrough(Cobol.MergeOutputThrough mergeOutputThrough, PrintOutputCapture<P> p) {
        visitSpace(mergeOutputThrough.getPrefix(), p);
        visitMarkers(mergeOutputThrough.getMarkers(), p);
        p.append(mergeOutputThrough.getWords());
        return mergeOutputThrough;
    }

    public Cobol visitMultiply(Cobol.Multiply multiply, PrintOutputCapture<P> p) {
        visitSpace(multiply.getPrefix(), p);
        visitMarkers(multiply.getMarkers(), p);
        p.append(multiply.getWords());
        visit(multiply.getMultiplicand(), p);
        p.append(multiply.getBy());
        visit(multiply.getMultiply(), p);
        visit(multiply.getOnSizeErrorPhrase(), p);
        visit(multiply.getNotOnSizeErrorPhrase(), p);
        p.append(multiply.getEndMultiply());
        return multiply;
    }

    public Cobol visitMultiplyRegular(Cobol.MultiplyRegular multiplyRegular, PrintOutputCapture<P> p) {
        visitSpace(multiplyRegular.getPrefix(), p);
        visitMarkers(multiplyRegular.getMarkers(), p);
        visitContainer("", multiplyRegular.getPadding().getOperand(), "", "", p);
        return multiplyRegular;
    }

    public Cobol visitMultiplyGiving(Cobol.MultiplyGiving multiplyGiving, PrintOutputCapture<P> p) {
        visitSpace(multiplyGiving.getPrefix(), p);
        visitMarkers(multiplyGiving.getMarkers(), p);
        visitContainer("", multiplyGiving.getPadding().getResult(), "", "", p);
        return multiplyGiving;
    }

    public Cobol visitNextSentence(Cobol.NextSentence nextSentence, PrintOutputCapture<P> p) {
        visitSpace(nextSentence.getPrefix(), p);
        visitMarkers(nextSentence.getMarkers(), p);
        p.append(nextSentence.getWords());
        return nextSentence;
    }

    public Cobol visitOpen(Cobol.Open open, PrintOutputCapture<P> p) {
        visitSpace(open.getPrefix(), p);
        visitMarkers(open.getMarkers(), p);
        p.append(open.getWords());
        visitContainer("", open.getPadding().getOpen(), "", "", p);
        return open;
    }

    public Cobol visitOpenInputOutputStatement(Cobol.OpenInputOutputStatement openInputOutputStatement, PrintOutputCapture<P> p) {
        visitSpace(openInputOutputStatement.getPrefix(), p);
        visitMarkers(openInputOutputStatement.getMarkers(), p);
        p.append(openInputOutputStatement.getWords());
        visitContainer("", openInputOutputStatement.getPadding().getOpenInput(), "", "", p);
        return openInputOutputStatement;
    }

    public Cobol visitOpenable(Cobol.Openable openable, PrintOutputCapture<P> p) {
        visitSpace(openable.getPrefix(), p);
        visitMarkers(openable.getMarkers(), p);
        visit(openable.getFileName(), p);
        p.append(openable.getWords());
        return openable;
    }

    public Cobol visitOpenIOExtendStatement(Cobol.OpenIOExtendStatement openIOExtendStatement, PrintOutputCapture<P> p) {
        visitSpace(openIOExtendStatement.getPrefix(), p);
        visitMarkers(openIOExtendStatement.getMarkers(), p);
        p.append(openIOExtendStatement.getWords());
        visitContainer("", openIOExtendStatement.getPadding().getFileNames(), "", "", p);
        return openIOExtendStatement;
    }

    public Cobol visitPerform(Cobol.Perform perform, PrintOutputCapture<P> p) {
        visitSpace(perform.getPrefix(), p);
        visitMarkers(perform.getMarkers(), p);
        p.append(perform.getWords());
        visit(perform.getStatement(), p);
        return perform;
    }

    public Cobol visitPerformInlineStatement(Cobol.PerformInlineStatement performInlineStatement, PrintOutputCapture<P> p) {
        visitSpace(performInlineStatement.getPrefix(), p);
        visitMarkers(performInlineStatement.getMarkers(), p);
        visit(performInlineStatement.getPerformType(), p);
        visitContainer("", performInlineStatement.getPadding().getStatements(), "", "", p);
        p.append(performInlineStatement.getWords());
        return performInlineStatement;
    }

    public Cobol visitPerformProcedureStatement(Cobol.PerformProcedureStatement performProcedureStatement, PrintOutputCapture<P> p) {
        visitSpace(performProcedureStatement.getPrefix(), p);
        visitMarkers(performProcedureStatement.getMarkers(), p);
        visit(performProcedureStatement.getProcedureName(), p);
        p.append(performProcedureStatement.getWords());
        visit(performProcedureStatement.getThroughProcedure(), p);
        visit(performProcedureStatement.getPerformType(), p);
        return performProcedureStatement;
    }

    public Cobol visitPerformTimes(Cobol.PerformTimes performTimes, PrintOutputCapture<P> p) {
        visitSpace(performTimes.getPrefix(), p);
        visitMarkers(performTimes.getMarkers(), p);
        visit(performTimes.getValue(), p);
        p.append(performTimes.getWords());
        return performTimes;
    }

    public Cobol visitPerformUntil(Cobol.PerformUntil performUntil, PrintOutputCapture<P> p) {
        visitSpace(performUntil.getPrefix(), p);
        visitMarkers(performUntil.getMarkers(), p);
        visit(performUntil.getPerformTestClause(), p);
        p.append(performUntil.getWords());
        visit(performUntil.getCondition(), p);
        return performUntil;
    }

    public Cobol visitPerformVarying(Cobol.PerformVarying performVarying, PrintOutputCapture<P> p) {
        visitSpace(performVarying.getPrefix(), p);
        visitMarkers(performVarying.getMarkers(), p);
        visit(performVarying.getFirst(), p);
        visit(performVarying.getSecond(), p);
        return performVarying;
    }

    public Cobol visitPerformVaryingClause(Cobol.PerformVaryingClause performVaryingClause, PrintOutputCapture<P> p) {
        visitSpace(performVaryingClause.getPrefix(), p);
        visitMarkers(performVaryingClause.getMarkers(), p);
        p.append(performVaryingClause.getWords());
        visit(performVaryingClause.getPerformVaryingPhrase(), p);
        visitContainer("", performVaryingClause.getPadding().getPerformAfter(), "", "", p);
        return performVaryingClause;
    }

    public Cobol visitPerformVaryingPhrase(Cobol.PerformVaryingPhrase performVaryingPhrase, PrintOutputCapture<P> p) {
        visitSpace(performVaryingPhrase.getPrefix(), p);
        visitMarkers(performVaryingPhrase.getMarkers(), p);
        visit(performVaryingPhrase.getFrom(), p);
        visit(performVaryingPhrase.getBy(), p);
        visit(performVaryingPhrase.getUntil(), p);
        return performVaryingPhrase;
    }

    public Cobol visitPerformable(Cobol.Performable performable, PrintOutputCapture<P> p) {
        visitSpace(performable.getPrefix(), p);
        visitMarkers(performable.getMarkers(), p);
        p.append(performable.getWords());
        visit(performable.getExpression(), p);
        return performable;
    }

    public Cobol visitPerformFrom(Cobol.PerformFrom performFrom, PrintOutputCapture<P> p) {
        visitSpace(performFrom.getPrefix(), p);
        visitMarkers(performFrom.getMarkers(), p);
        p.append(performFrom.getWords());
        visit(performFrom.getFrom(), p);
        return performFrom;
    }

    public Cobol visitPerformTestClause(Cobol.PerformTestClause performTestClause, PrintOutputCapture<P> p) {
        visitSpace(performTestClause.getPrefix(), p);
        visitMarkers(performTestClause.getMarkers(), p);
        p.append(performTestClause.getWords());
        return performTestClause;
    }

    public Cobol visitPurge(Cobol.Purge purge, PrintOutputCapture<P> p) {
        visitSpace(purge.getPrefix(), p);
        visitMarkers(purge.getMarkers(), p);
        visitContainer("", purge.getPadding().getNames(), "", "", p);
        return purge;
    }

    public Cobol visitRead(Cobol.Read read, PrintOutputCapture<P> p) {
        visitSpace(read.getPrefix(), p);
        visitMarkers(read.getMarkers(), p);
        p.append(read.getWords());
        visit(read.getFileName(), p);
        p.append(read.getNextRecord());
        visit(read.getReadInto(), p);
        visit(read.getReadWith(), p);
        visit(read.getReadKey(), p);
        visit(read.getInvalidKeyPhrase(), p);
        visit(read.getNotInvalidKeyPhrase(), p);
        visit(read.getAtEndPhrase(), p);
        visit(read.getNotAtEndPhrase(), p);
        p.append(read.getEndRead());
        return read;
    }

    public Cobol visitReadInto(Cobol.ReadInto readInto, PrintOutputCapture<P> p) {
        visitSpace(readInto.getPrefix(), p);
        visitMarkers(readInto.getMarkers(), p);
        p.append(readInto.getWords());
        visit(readInto.getIdentifier(), p);
        return readInto;
    }

    public Cobol visitReadWith(Cobol.ReadWith readWith, PrintOutputCapture<P> p) {
        visitSpace(readWith.getPrefix(), p);
        visitMarkers(readWith.getMarkers(), p);
        p.append(readWith.getWords());
        return readWith;
    }

    public Cobol visitReadKey(Cobol.ReadKey readKey, PrintOutputCapture<P> p) {
        visitSpace(readKey.getPrefix(), p);
        visitMarkers(readKey.getMarkers(), p);
        p.append(readKey.getWords());
        visit(readKey.getQualifiedDataName(), p);
        return readKey;
    }

    public Cobol visitEvaluate(Cobol.Evaluate evaluate, PrintOutputCapture<P> p) {
        visitSpace(evaluate.getPrefix(), p);
        visitMarkers(evaluate.getMarkers(), p);
        p.append(evaluate.getEvaluate());
        visit(evaluate.getSelect(), p);
        visitContainer("", evaluate.getPadding().getAlsoSelect(), "", "", p);
        visitContainer("", evaluate.getPadding().getWhenPhrase(), "", "", p);
        visit(evaluate.getWhenOther(), p);
        visitLeftPadded("", evaluate.getPadding().getEndPhrase(), p);
        return evaluate;
    }

    public Cobol visitEvaluateAlso(Cobol.EvaluateAlso evaluateAlso, PrintOutputCapture<P> p) {
        visitSpace(evaluateAlso.getPrefix(), p);
        visitMarkers(evaluateAlso.getMarkers(), p);
        p.append(evaluateAlso.getAlso());
        visit(evaluateAlso.getSelect(), p);
        return evaluateAlso;
    }

    public Cobol visitEvaluateAlsoCondition(Cobol.EvaluateAlsoCondition evaluateAlsoCondition, PrintOutputCapture<P> p) {
        visitSpace(evaluateAlsoCondition.getPrefix(), p);
        visitMarkers(evaluateAlsoCondition.getMarkers(), p);
        p.append(evaluateAlsoCondition.getAlso());
        visit(evaluateAlsoCondition.getCondition(), p);
        return evaluateAlsoCondition;
    }

    public Cobol visitEvaluateCondition(Cobol.EvaluateCondition evaluateCondition, PrintOutputCapture<P> p) {
        visitSpace(evaluateCondition.getPrefix(), p);
        visitMarkers(evaluateCondition.getMarkers(), p);
        p.append(evaluateCondition.getWords());
        visit(evaluateCondition.getCondition(), p);
        visit(evaluateCondition.getEvaluateThrough(), p);
        return evaluateCondition;
    }

    public Cobol visitEvaluateThrough(Cobol.EvaluateThrough evaluateThrough, PrintOutputCapture<P> p) {
        visitSpace(evaluateThrough.getPrefix(), p);
        visitMarkers(evaluateThrough.getMarkers(), p);
        p.append(evaluateThrough.getThrough());
        visit(evaluateThrough.getValue(), p);
        return evaluateThrough;
    }

    public Cobol visitEvaluateWhen(Cobol.EvaluateWhen evaluateWhen, PrintOutputCapture<P> p) {
        visitSpace(evaluateWhen.getPrefix(), p);
        visitMarkers(evaluateWhen.getMarkers(), p);
        p.append(evaluateWhen.getWhen());
        visit(evaluateWhen.getCondition(), p);
        visitContainer("", evaluateWhen.getPadding().getAlsoCondition(), "", "", p);
        return evaluateWhen;
    }

    public Cobol visitEvaluateWhenPhrase(Cobol.EvaluateWhenPhrase evaluateWhenPhrase, PrintOutputCapture<P> p) {
        visitSpace(evaluateWhenPhrase.getPrefix(), p);
        visitMarkers(evaluateWhenPhrase.getMarkers(), p);
        visitContainer("", evaluateWhenPhrase.getPadding().getWhens(), "", "", p);
        visitContainer("", evaluateWhenPhrase.getPadding().getStatements(), "", "", p);
        return evaluateWhenPhrase;
    }

    public Cobol visitEvaluateValueThrough(Cobol.EvaluateValueThrough evaluateValueThrough, PrintOutputCapture<P> p) {
        visitSpace(evaluateValueThrough.getPrefix(), p);
        visitMarkers(evaluateValueThrough.getMarkers(), p);
        p.append(evaluateValueThrough.getNot());
        visit(evaluateValueThrough.getValue(), p);
        visit(evaluateValueThrough.getEvaluateThrough(), p);
        return evaluateValueThrough;
    }

    public Cobol visitAbbreviation(Cobol.Abbreviation abbreviation, PrintOutputCapture<P> p) {
        visitSpace(abbreviation.getPrefix(), p);
        visitMarkers(abbreviation.getMarkers(), p);
        p.append(abbreviation.getNot());
        visit(abbreviation.getRelationalOperator(), p);
        if (abbreviation.getLeftParen() != null) {
            p.append(abbreviation.getLeftParen());
        }
        visit(abbreviation.getArithmeticExpression(), p);
        visit(abbreviation.getAbbreviation(), p);
        if (abbreviation.getRightParen() != null) {
            p.append(abbreviation.getRightParen());
        }
        return abbreviation;
    }

    public Cobol visitAndOrCondition(Cobol.AndOrCondition andOrCondition, PrintOutputCapture<P> p) {
        visitSpace(andOrCondition.getPrefix(), p);
        visitMarkers(andOrCondition.getMarkers(), p);
        p.append(andOrCondition.getLogicalOperator());
        visit(andOrCondition.getCombinableCondition(), p);
        visitContainer("", andOrCondition.getPadding().getAbbreviations(), "", "", p);
        return andOrCondition;
    }

    public Cobol visitClassCondition(Cobol.ClassCondition classCondition, PrintOutputCapture<P> p) {
        visitSpace(classCondition.getPrefix(), p);
        visitMarkers(classCondition.getMarkers(), p);
        p.append(classCondition.getWords());
        return classCondition;
    }

    public Cobol visitCombinableCondition(Cobol.CombinableCondition combinableCondition, PrintOutputCapture<P> p) {
        visitSpace(combinableCondition.getPrefix(), p);
        visitMarkers(combinableCondition.getMarkers(), p);
        p.append(combinableCondition.getNot());
        visit(combinableCondition.getSimpleCondition(), p);
        return combinableCondition;
    }

    public Cobol visitConditionNameReference(Cobol.ConditionNameReference conditionNameReference, PrintOutputCapture<P> p) {
        visitSpace(conditionNameReference.getPrefix(), p);
        visitMarkers(conditionNameReference.getMarkers(), p);
        visit(conditionNameReference.getName(), p);
        visitContainer("", conditionNameReference.getPadding().getInDatas(), "", "", p);
        visit(conditionNameReference.getInFile(), p);
        visitContainer("", conditionNameReference.getPadding().getReferences(), "", "", p);
        visitContainer("", conditionNameReference.getPadding().getInMnemonics(), "", "", p);
        return conditionNameReference;
    }

    public Cobol visitParenExpression(Cobol.ParenExpression parenExpression, PrintOutputCapture<P> p) {
        visitSpace(parenExpression.getPrefix(), p);
        visitMarkers(parenExpression.getMarkers(), p);
        p.append(parenExpression.getLeftParen());
        visit(parenExpression.getExpression(), p);
        p.append(parenExpression.getRightParen());
        return parenExpression;
    }

    public Cobol visitRelationalOperator(Cobol.RelationalOperator relationalOperator, PrintOutputCapture<P> p) {
        visitSpace(relationalOperator.getPrefix(), p);
        visitMarkers(relationalOperator.getMarkers(), p);
        p.append(relationalOperator.getWords());
        return relationalOperator;
    }

    public Cobol visitRelationArithmeticComparison(Cobol.RelationArithmeticComparison relationArithmeticComparison, PrintOutputCapture<P> p) {
        visitSpace(relationArithmeticComparison.getPrefix(), p);
        visitMarkers(relationArithmeticComparison.getMarkers(), p);
        visit(relationArithmeticComparison.getArithmeticExpressionA(), p);
        visit(relationArithmeticComparison.getRelationalOperator(), p);
        visit(relationArithmeticComparison.getArithmeticExpressionB(), p);
        return relationArithmeticComparison;
    }

    public Cobol visitRelationCombinedComparison(Cobol.RelationCombinedComparison relationCombinedComparison, PrintOutputCapture<P> p) {
        visitSpace(relationCombinedComparison.getPrefix(), p);
        visitMarkers(relationCombinedComparison.getMarkers(), p);
        visit(relationCombinedComparison.getArithmeticExpression(), p);
        visit(relationCombinedComparison.getRelationalOperator(), p);
        visit(relationCombinedComparison.getCombinedCondition(), p);
        return relationCombinedComparison;
    }

    public Cobol visitRelationCombinedCondition(Cobol.RelationCombinedCondition relationCombinedCondition, PrintOutputCapture<P> p) {
        visitSpace(relationCombinedCondition.getPrefix(), p);
        visitMarkers(relationCombinedCondition.getMarkers(), p);
        for (Cobol c : relationCombinedCondition.getRelationalArithmeticExpressions()) {
            visit(c, p);
        }
        return relationCombinedCondition;
    }

    public Cobol visitRelationSignCondition(Cobol.RelationSignCondition relationSignCondition, PrintOutputCapture<P> p) {
        visitSpace(relationSignCondition.getPrefix(), p);
        visitMarkers(relationSignCondition.getMarkers(), p);
        visit(relationSignCondition.getArithmeticExpression(), p);
        p.append(relationSignCondition.getWords());
        return relationSignCondition;
    }

    public Cobol visitUnString(Cobol.UnString unString, PrintOutputCapture<P> p) {
        visitSpace(unString.getPrefix(), p);
        visitMarkers(unString.getMarkers(), p);
        p.append(unString.getUnstring());
        visit(unString.getUnstringSendingPhrase(), p);
        visit(unString.getUnstringIntoPhrase(), p);
        visit(unString.getUnstringWithPointerPhrase(), p);
        visit(unString.getUnstringTallyingPhrase(), p);
        visit(unString.getOnOverflowPhrase(), p);
        visit(unString.getNotOnOverflowPhrase(), p);
        visitLeftPadded("", unString.getPadding().getEndUnstring(), p);
        return unString;
    }

    public Cobol visitUnstringSendingPhrase(Cobol.UnstringSendingPhrase unstringSendingPhrase, PrintOutputCapture<P> p) {
        visitSpace(unstringSendingPhrase.getPrefix(), p);
        visitMarkers(unstringSendingPhrase.getMarkers(), p);
        visit(unstringSendingPhrase.getIdentifier(), p);
        visit(unstringSendingPhrase.getUnstringDelimitedByPhrase(), p);
        visitContainer("", unstringSendingPhrase.getPadding().getUnstringOrAllPhrases(), "", "", p);
        return unstringSendingPhrase;
    }

    public Cobol visitUnstringDelimitedByPhrase(Cobol.UnstringDelimitedByPhrase unstringDelimitedByPhrase, PrintOutputCapture<P> p) {
        visitSpace(unstringDelimitedByPhrase.getPrefix(), p);
        visitMarkers(unstringDelimitedByPhrase.getMarkers(), p);
        p.append(unstringDelimitedByPhrase.getWords());
        visit(unstringDelimitedByPhrase.getName(), p);
        return unstringDelimitedByPhrase;
    }

    public Cobol visitUnstringOrAllPhrase(Cobol.UnstringOrAllPhrase unstringOrAllPhrase, PrintOutputCapture<P> p) {
        visitSpace(unstringOrAllPhrase.getPrefix(), p);
        visitMarkers(unstringOrAllPhrase.getMarkers(), p);
        p.append(unstringOrAllPhrase.getWords());
        visit(unstringOrAllPhrase.getName(), p);
        return unstringOrAllPhrase;
    }

    public Cobol visitUnstringIntoPhrase(Cobol.UnstringIntoPhrase unstringIntoPhrase, PrintOutputCapture<P> p) {
        visitSpace(unstringIntoPhrase.getPrefix(), p);
        visitMarkers(unstringIntoPhrase.getMarkers(), p);
        p.append(unstringIntoPhrase.getInto());
        visitContainer("", unstringIntoPhrase.getPadding().getUnstringIntos(), "", "", p);
        return unstringIntoPhrase;
    }

    public Cobol visitUnstringInto(Cobol.UnstringInto unstringInto, PrintOutputCapture<P> p) {
        visitSpace(unstringInto.getPrefix(), p);
        visitMarkers(unstringInto.getMarkers(), p);
        visit(unstringInto.getIdentifier(), p);
        visit(unstringInto.getUnstringDelimiterIn(), p);
        visit(unstringInto.getUnstringCountIn(), p);
        return unstringInto;
    }

    public Cobol visitUnstringDelimiterIn(Cobol.UnstringDelimiterIn unstringDelimiterIn, PrintOutputCapture<P> p) {
        visitSpace(unstringDelimiterIn.getPrefix(), p);
        visitMarkers(unstringDelimiterIn.getMarkers(), p);
        p.append(unstringDelimiterIn.getWords());
        visit(unstringDelimiterIn.getIdentifier(), p);
        return unstringDelimiterIn;
    }

    public Cobol visitUnstringCountIn(Cobol.UnstringCountIn unstringCountIn, PrintOutputCapture<P> p) {
        visitSpace(unstringCountIn.getPrefix(), p);
        visitMarkers(unstringCountIn.getMarkers(), p);
        p.append(unstringCountIn.getWords());
        visit(unstringCountIn.getIdentifier(), p);
        return unstringCountIn;
    }

    public Cobol visitUnstringWithPointerPhrase(Cobol.UnstringWithPointerPhrase unstringWithPointerPhrase, PrintOutputCapture<P> p) {
        visitSpace(unstringWithPointerPhrase.getPrefix(), p);
        visitMarkers(unstringWithPointerPhrase.getMarkers(), p);
        p.append(unstringWithPointerPhrase.getWords());
        visit(unstringWithPointerPhrase.getQualifiedDataName(), p);
        return unstringWithPointerPhrase;
    }

    public Cobol visitUnstringTallyingPhrase(Cobol.UnstringTallyingPhrase unstringTallyingPhrase, PrintOutputCapture<P> p) {
        visitSpace(unstringTallyingPhrase.getPrefix(), p);
        visitMarkers(unstringTallyingPhrase.getMarkers(), p);
        p.append(unstringTallyingPhrase.getWords());
        visit(unstringTallyingPhrase.getQualifiedDataName(), p);
        return unstringTallyingPhrase;
    }

    public Cobol visitConditionNameSubscriptReference(Cobol.ConditionNameSubscriptReference conditionNameSubscriptReference, PrintOutputCapture<P> p) {
        visitSpace(conditionNameSubscriptReference.getPrefix(), p);
        visitMarkers(conditionNameSubscriptReference.getMarkers(), p);
        p.append(conditionNameSubscriptReference.getLeftParen());
        for(Cobol c : conditionNameSubscriptReference.getSubscripts()) {
            visit(c, p);
        }
        p.append(conditionNameSubscriptReference.getRightParen());
        return conditionNameSubscriptReference;
    }

    public Cobol visitSubscript(Cobol.Subscript subscript, PrintOutputCapture<P> p) {
        visitSpace(subscript.getPrefix(), p);
        visitMarkers(subscript.getMarkers(), p);
        visit(subscript.getFirst(), p);
        visit(subscript.getIntegerLiteral(), p);
        return subscript;
    }

    public Cobol visitCobolWord(Cobol.CobolWord cobolWord, PrintOutputCapture<P> p) {
        visitSpace(cobolWord.getPrefix(), p);
        visitMarkers(cobolWord.getMarkers(), p);
        p.append(cobolWord.getWord());
        return cobolWord;
    }

    public Cobol visitReceive(Cobol.Receive receive, PrintOutputCapture<P> p) {
        visitSpace(receive.getPrefix(), p);
        visitMarkers(receive.getMarkers(), p);
        p.append(receive.getReceive());
        visit(receive.getFromOrInto(), p);
        visit(receive.getOnExceptionClause(), p);
        visit(receive.getNotOnExceptionClause(), p);
        p.append(receive.getEndReceive());
        return receive;
    }

    public Cobol visitReceiveFromStatement(Cobol.ReceiveFromStatement receiveFromStatement, PrintOutputCapture<P> p) {
        visitSpace(receiveFromStatement.getPrefix(), p);
        visitMarkers(receiveFromStatement.getMarkers(), p);
        visit(receiveFromStatement.getDataName(), p);
        p.append(receiveFromStatement.getFrom());
        visit(receiveFromStatement.getReceiveFrom(), p);
        visitContainer("", receiveFromStatement.getPadding().getBeforeWithThreadSizeStatus(), "", "", p);
        return receiveFromStatement;
    }

    public Cobol visitReceiveFrom(Cobol.ReceiveFrom receiveFrom, PrintOutputCapture<P> p) {
        visitSpace(receiveFrom.getPrefix(), p);
        visitMarkers(receiveFrom.getMarkers(), p);
        p.append(receiveFrom.getWords());
        visit(receiveFrom.getDataName(), p);
        return receiveFrom;
    }

    public Cobol visitReceiveIntoStatement(Cobol.ReceiveIntoStatement receiveIntoStatement, PrintOutputCapture<P> p) {
        visitSpace(receiveIntoStatement.getPrefix(), p);
        visitMarkers(receiveIntoStatement.getMarkers(), p);
        visit(receiveIntoStatement.getCdName(), p);
        p.append(receiveIntoStatement.getWords());
        visit(receiveIntoStatement.getIdentifier(), p);
        visit(receiveIntoStatement.getReceiveNoData(), p);
        visit(receiveIntoStatement.getReceiveWithData(), p);
        return receiveIntoStatement;
    }

    public Cobol visitReceivable(Cobol.Receivable receivable, PrintOutputCapture<P> p) {
        visitSpace(receivable.getPrefix(), p);
        visitMarkers(receivable.getMarkers(), p);
        p.append(receivable.getWords());
        return receivable;
    }

    public Cobol visitTerminate(Cobol.Terminate terminate, PrintOutputCapture<P> p) {
        visitSpace(terminate.getPrefix(), p);
        visitMarkers(terminate.getMarkers(), p);
        visit(terminate.getTerminate(), p);
        visit(terminate.getReportName(), p);
        return terminate;
    }

    public Cobol visitSubtract(Cobol.Subtract subtract, PrintOutputCapture<P> p) {
        visitSpace(subtract.getPrefix(), p);
        visitMarkers(subtract.getMarkers(), p);
        visit(subtract.getSubstract(), p);
        visit(subtract.getOperation(), p);
        visit(subtract.getOnSizeErrorPhrase(), p);
        visit(subtract.getNotOnSizeErrorPhrase(), p);
        visitLeftPadded("", subtract.getPadding().getEndSubtract(), p);
        return subtract;
    }

    public Cobol visitSubtractFromStatement(Cobol.SubtractFromStatement subtractFromStatement, PrintOutputCapture<P> p) {
        visitSpace(subtractFromStatement.getPrefix(), p);
        visitMarkers(subtractFromStatement.getMarkers(), p);
        visitContainer("", subtractFromStatement.getPadding().getSubtractSubtrahend(), "", "", p);
        visit(subtractFromStatement.getFrom(), p);
        visitContainer("", subtractFromStatement.getPadding().getSubtractMinuend(), "", "", p);
        return subtractFromStatement;
    }

    public Cobol visitSubtractFromGivingStatement(Cobol.SubtractFromGivingStatement subtractFromGivingStatement, PrintOutputCapture<P> p) {
        visitSpace(subtractFromGivingStatement.getPrefix(), p);
        visitMarkers(subtractFromGivingStatement.getMarkers(), p);
        visitContainer("", subtractFromGivingStatement.getPadding().getSubtractSubtrahend(), "", "", p);
        visit(subtractFromGivingStatement.getFrom(), p);
        visit(subtractFromGivingStatement.getSubtractMinuendGiving(), p);
        visit(subtractFromGivingStatement.getGiving(), p);
        visitContainer("", subtractFromGivingStatement.getPadding().getSubtractGiving(), "", "", p);
        return subtractFromGivingStatement;
    }

    public Cobol visitSubtractCorrespondingStatement(Cobol.SubtractCorrespondingStatement subtractCorrespondingStatement, PrintOutputCapture<P> p) {
        visitSpace(subtractCorrespondingStatement.getPrefix(), p);
        visitMarkers(subtractCorrespondingStatement.getMarkers(), p);
        visit(subtractCorrespondingStatement.getCorresponding(), p);
        visit(subtractCorrespondingStatement.getQualifiedDataName(), p);
        visit(subtractCorrespondingStatement.getGiving(), p);
        visit(subtractCorrespondingStatement.getSubtractMinuendCorresponding(), p);
        return subtractCorrespondingStatement;
    }

    public Cobol visitSubtractMinuendCorresponding(Cobol.SubtractMinuendCorresponding subtractMinuendCorresponding, PrintOutputCapture<P> p) {
        visitSpace(subtractMinuendCorresponding.getPrefix(), p);
        visitMarkers(subtractMinuendCorresponding.getMarkers(), p);
        visit(subtractMinuendCorresponding.getQualifiedDataName(), p);
        visit(subtractMinuendCorresponding.getRounded(), p);
        return subtractMinuendCorresponding;
    }
}
