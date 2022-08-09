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

    public Cobol visitAbbreviation(Cobol.Abbreviation abbreviation, PrintOutputCapture<P> p) {
        visitSpace(abbreviation.getPrefix(), p);
        visitMarkers(abbreviation.getMarkers(), p);
        visit(abbreviation.getNot(), p);
        visit(abbreviation.getRelationalOperator(), p);
        if (abbreviation.getLeftParen() != null) {
            visit(abbreviation.getLeftParen(), p);
        }
        visit(abbreviation.getArithmeticExpression(), p);
        visit(abbreviation.getAbbreviation(), p);
        if (abbreviation.getRightParen() != null) {
            visit(abbreviation.getRightParen(), p);
        }
        return abbreviation;
    }

    public Cobol visitAccept(Cobol.Accept accept, PrintOutputCapture<P> p) {
        visitSpace(accept.getPrefix(), p);
        visitMarkers(accept.getMarkers(), p);
        visit(accept.getAccept(), p);
        visit(accept.getIdentifier(), p);
        visit(accept.getOperation(), p);
        visit(accept.getOnExceptionClause(), p);
        visit(accept.getNotOnExceptionClause(), p);
        visit(accept.getEndAccept(), p);
        return accept;
    }

    public Cobol visitAcceptFromDateStatement(Cobol.AcceptFromDateStatement acceptFromDateStatement, PrintOutputCapture<P> p) {
        visitSpace(acceptFromDateStatement.getPrefix(), p);
        visitMarkers(acceptFromDateStatement.getMarkers(), p);
        visit(acceptFromDateStatement.getWords(), p);
        return acceptFromDateStatement;
    }

    public Cobol visitAcceptFromEscapeKeyStatement(Cobol.AcceptFromEscapeKeyStatement acceptFromEscapeKeyStatement, PrintOutputCapture<P> p) {
        visitSpace(acceptFromEscapeKeyStatement.getPrefix(), p);
        visitMarkers(acceptFromEscapeKeyStatement.getMarkers(), p);
        visit(acceptFromEscapeKeyStatement.getWords(), p);
        return acceptFromEscapeKeyStatement;
    }

    public Cobol visitAcceptFromMnemonicStatement(Cobol.AcceptFromMnemonicStatement acceptFromMnemonicStatement, PrintOutputCapture<P> p) {
        visitSpace(acceptFromMnemonicStatement.getPrefix(), p);
        visitMarkers(acceptFromMnemonicStatement.getMarkers(), p);
        visit(acceptFromMnemonicStatement.getFrom(), p);
        visit(acceptFromMnemonicStatement.getMnemonicName(), p);
        return acceptFromMnemonicStatement;
    }

    public Cobol visitAcceptMessageCountStatement(Cobol.AcceptMessageCountStatement acceptMessageCountStatement, PrintOutputCapture<P> p) {
        visitSpace(acceptMessageCountStatement.getPrefix(), p);
        visitMarkers(acceptMessageCountStatement.getMarkers(), p);
        visit(acceptMessageCountStatement.getWords(), p);
        return acceptMessageCountStatement;
    }

    public Cobol visitAccessModeClause(Cobol.AccessModeClause accessModeClause, PrintOutputCapture<P> p) {
        visitSpace(accessModeClause.getPrefix(), p);
        visitMarkers(accessModeClause.getMarkers(), p);
        visit(accessModeClause.getWords(), p);
        return accessModeClause;
    }

    public Cobol visitAdd(Cobol.Add add, PrintOutputCapture<P> p) {
        visitSpace(add.getPrefix(), p);
        visitMarkers(add.getMarkers(), p);
        visit(add.getAdd(), p);
        visit(add.getOperation(), p);
        visit(add.getOnSizeError(), p);
        visit(add.getEndAdd(), p);
        return add;
    }

    public Cobol visitAddTo(Cobol.AddTo addTo, PrintOutputCapture<P> p) {
        visitSpace(addTo.getPrefix(), p);
        visitMarkers(addTo.getMarkers(), p);
        visitContainer("", addTo.getPadding().getFrom(), "", "", p);
        visitContainer("", addTo.getPadding().getTo(), "", "", p);
        visitContainer("", addTo.getPadding().getGiving(), "", "", p);
        return addTo;
    }

    public Cobol visitAlphabetAlso(Cobol.AlphabetAlso alphabetAlso, PrintOutputCapture<P> p) {
        visitSpace(alphabetAlso.getPrefix(), p);
        visitMarkers(alphabetAlso.getMarkers(), p);
        visit(alphabetAlso.getWords(), p);
        visitContainer("", alphabetAlso.getPadding().getLiterals(), "", "", p);
        return alphabetAlso;
    }

    public Cobol visitAlphabetClause(Cobol.AlphabetClause alphabetClause, PrintOutputCapture<P> p) {
        visitSpace(alphabetClause.getPrefix(), p);
        visitMarkers(alphabetClause.getMarkers(), p);
        visit(alphabetClause.getAlphabet(), p);
        visit(alphabetClause.getName(), p);
        if (alphabetClause.getWords() != null) {
            for (Cobol c : alphabetClause.getWords()) {
                visit(c, p);
            }
        }
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
        visit(alphabetThrough.getWords(), p);
        visit(alphabetThrough.getLiteral(), p);
        return alphabetThrough;
    }

    public Cobol visitAlterProceedTo(Cobol.AlterProceedTo alterProceedTo, PrintOutputCapture<P> p) {
        visitSpace(alterProceedTo.getPrefix(), p);
        visitMarkers(alterProceedTo.getMarkers(), p);
        visit(alterProceedTo.getFrom(), p);
        visit(alterProceedTo.getWords(), p);
        visit(alterProceedTo.getTo(), p);
        return alterProceedTo;
    }

    public Cobol visitAlterStatement(Cobol.AlterStatement alterStatement, PrintOutputCapture<P> p) {
        visitSpace(alterStatement.getPrefix(), p);
        visitMarkers(alterStatement.getMarkers(), p);
        visit(alterStatement.getWords(), p);
        for (Cobol.AlterProceedTo a : alterStatement.getAlterProceedTo()) {
            visitAlterProceedTo(a, p);
        }
        return alterStatement;
    }

    public Cobol visitAlteredGoTo(Cobol.AlteredGoTo alteredGoTo, PrintOutputCapture<P> p) {
        visitSpace(alteredGoTo.getPrefix(), p);
        visitMarkers(alteredGoTo.getMarkers(), p);
        visit(alteredGoTo.getWords(), p);
        visitLeftPadded("", alteredGoTo.getPadding().getDot(), p);
        return alteredGoTo;
    }

    public Cobol visitAlternateRecordKeyClause(Cobol.AlternateRecordKeyClause alternateRecordKeyClause, PrintOutputCapture<P> p) {
        visitSpace(alternateRecordKeyClause.getPrefix(), p);
        visitMarkers(alternateRecordKeyClause.getMarkers(), p);
        visit(alternateRecordKeyClause.getAlternateWords(), p);
        visit(alternateRecordKeyClause.getQualifiedDataName(), p);
        visit(alternateRecordKeyClause.getPasswordClause(), p);
        visit(alternateRecordKeyClause.getDuplicates(), p);
        return alternateRecordKeyClause;
    }

    public Cobol visitAndOrCondition(Cobol.AndOrCondition andOrCondition, PrintOutputCapture<P> p) {
        visitSpace(andOrCondition.getPrefix(), p);
        visitMarkers(andOrCondition.getMarkers(), p);
        visit(andOrCondition.getLogicalOperator(), p);
        visit(andOrCondition.getCombinableCondition(), p);
        visitContainer("", andOrCondition.getPadding().getAbbreviations(), "", "", p);
        return andOrCondition;
    }

    public Cobol visitArgument(Cobol.Argument argument, PrintOutputCapture<P> p) {
        visitSpace(argument.getPrefix(), p);
        visitMarkers(argument.getMarkers(), p);
        visit(argument.getFirst(), p);
        visit(argument.getIntegerLiteral(), p);
        return argument;
    }

    public Cobol visitArithmeticExpression(Cobol.ArithmeticExpression arithmeticExpression, PrintOutputCapture<P> p) {
        visitSpace(arithmeticExpression.getPrefix(), p);
        visitMarkers(arithmeticExpression.getMarkers(), p);
        visit(arithmeticExpression.getMultDivs(), p);
        visitContainer("", arithmeticExpression.getPadding().getPlusMinuses(), "", "", p);
        return arithmeticExpression;
    }

    public Cobol visitAssignClause(Cobol.AssignClause assignClause, PrintOutputCapture<P> p) {
        visitSpace(assignClause.getPrefix(), p);
        visitMarkers(assignClause.getMarkers(), p);
        visit(assignClause.getWords(), p);
        visit(assignClause.getName(), p);
        return assignClause;
    }

    public Cobol visitBlockContainsClause(Cobol.BlockContainsClause blockContainsClause, PrintOutputCapture<P> p) {
        visitSpace(blockContainsClause.getPrefix(), p);
        visitMarkers(blockContainsClause.getMarkers(), p);
        visit(blockContainsClause.getFirstWords(), p);
        visit(blockContainsClause.getIntegerLiteral(), p);
        visit(blockContainsClause.getBlockContainsTo(), p);
        visit(blockContainsClause.getLastWords(), p);
        return blockContainsClause;
    }

    public Cobol visitBlockContainsTo(Cobol.BlockContainsTo blockContainsTo, PrintOutputCapture<P> p) {
        visitSpace(blockContainsTo.getPrefix(), p);
        visitMarkers(blockContainsTo.getMarkers(), p);
        visit(blockContainsTo.getTo(), p);
        visit(blockContainsTo.getIntegerLiteral(), p);
        return blockContainsTo;
    }

    public Cobol visitCall(Cobol.Call call, PrintOutputCapture<P> p) {
        visitSpace(call.getPrefix(), p);
        visitMarkers(call.getMarkers(), p);
        visit(call.getCall(), p);
        visit(call.getIdentifier(), p);
        visit(call.getCallUsingPhrase(), p);
        visit(call.getCallGivingPhrase(), p);
        visit(call.getOnOverflowPhrase(), p);
        visit(call.getOnExceptionClause(), p);
        visit(call.getNotOnExceptionClause(), p);
        visit(call.getEndCall(), p);
        return call;
    }

    public Cobol visitCallBy(Cobol.CallBy callBy, PrintOutputCapture<P> p) {
        visitSpace(callBy.getPrefix(), p);
        visitMarkers(callBy.getMarkers(), p);
        visit(callBy.getWords(), p);
        visit(callBy.getIdentifier(), p);
        return callBy;
    }

    public Cobol visitCallGivingPhrase(Cobol.CallGivingPhrase callGivingPhrase, PrintOutputCapture<P> p) {
        visitSpace(callGivingPhrase.getPrefix(), p);
        visitMarkers(callGivingPhrase.getMarkers(), p);
        visit(callGivingPhrase.getWords(), p);
        visit(callGivingPhrase.getIdentifier(), p);
        return callGivingPhrase;
    }

    public Cobol visitCallPhrase(Cobol.CallPhrase callPhrase, PrintOutputCapture<P> p) {
        visitSpace(callPhrase.getPrefix(), p);
        visitMarkers(callPhrase.getMarkers(), p);
        visit(callPhrase.getWords(), p);
        visitContainer("", callPhrase.getPadding().getParameters(), "", "", p);
        return callPhrase;
    }

    public Cobol visitCancel(Cobol.Cancel cancel, PrintOutputCapture<P> p) {
        visitSpace(cancel.getPrefix(), p);
        visitMarkers(cancel.getMarkers(), p);
        visit(cancel.getCancel(), p);
        visitContainer("", cancel.getPadding().getCancelCalls(), "", "", p);
        return cancel;
    }

    public Cobol visitCancelCall(Cobol.CancelCall cancelCall, PrintOutputCapture<P> p) {
        visitSpace(cancelCall.getPrefix(), p);
        visitMarkers(cancelCall.getMarkers(), p);
        visit(cancelCall.getLibraryName(), p);
        visit(cancelCall.getBy(), p);
        visit(cancelCall.getIdentifier(), p);
        visit(cancelCall.getLiteral(), p);
        return cancelCall;
    }

    public Cobol visitChannelClause(Cobol.ChannelClause channelClause, PrintOutputCapture<P> p) {
        visitSpace(channelClause.getPrefix(), p);
        visitMarkers(channelClause.getMarkers(), p);
        visit(channelClause.getWords(), p);
        visit(channelClause.getLiteral(), p);
        visit(channelClause.getIs(), p);
        visit(channelClause.getMnemonicName(), p);
        return channelClause;
    }

    public Cobol visitClassClause(Cobol.ClassClause classClause, PrintOutputCapture<P> p) {
        visitSpace(classClause.getPrefix(), p);
        visitMarkers(classClause.getMarkers(), p);
        visit(classClause.getClazz(), p);
        visit(classClause.getClassName(), p);
        visit(classClause.getWords(), p);
        visitContainer("", classClause.getPadding().getThroughs(), "", "", p);
        return classClause;
    }

    public Cobol visitClassClauseThrough(Cobol.ClassClauseThrough classClauseThrough, PrintOutputCapture<P> p) {
        visitSpace(classClauseThrough.getPrefix(), p);
        visitMarkers(classClauseThrough.getMarkers(), p);
        visit(classClauseThrough.getFrom(), p);
        visit(classClauseThrough.getThrough(), p);
        visit(classClauseThrough.getTo(), p);
        return classClauseThrough;
    }

    public Cobol visitClassCondition(Cobol.ClassCondition classCondition, PrintOutputCapture<P> p) {
        visitSpace(classCondition.getPrefix(), p);
        visitMarkers(classCondition.getMarkers(), p);
        visit(classCondition.getName(), p);
        visit(classCondition.getWords(), p);
        visit(classCondition.getClassName(), p);
        return classCondition;
    }

    public Cobol visitClose(Cobol.Close close, PrintOutputCapture<P> p) {
        visitSpace(close.getPrefix(), p);
        visitMarkers(close.getMarkers(), p);
        visit(close.getClose(), p);
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

    public Cobol visitClosePortFileIOStatement(Cobol.ClosePortFileIOStatement closePortFileIOStatement, PrintOutputCapture<P> p) {
        visitSpace(closePortFileIOStatement.getPrefix(), p);
        visitMarkers(closePortFileIOStatement.getMarkers(), p);
        visit(closePortFileIOStatement.getWords(), p);
        visitContainer("", closePortFileIOStatement.getPadding().getClosePortFileIOUsing(), "", "", p);
        return closePortFileIOStatement;
    }

    public Cobol visitClosePortFileIOUsingAssociatedData(Cobol.ClosePortFileIOUsingAssociatedData closePortFileIOUsingAssociatedData, PrintOutputCapture<P> p) {
        visitSpace(closePortFileIOUsingAssociatedData.getPrefix(), p);
        visitMarkers(closePortFileIOUsingAssociatedData.getMarkers(), p);
        visit(closePortFileIOUsingAssociatedData.getAssociatedData(), p);
        visit(closePortFileIOUsingAssociatedData.getIdentifier(), p);
        return closePortFileIOUsingAssociatedData;
    }

    public Cobol visitClosePortFileIOUsingAssociatedDataLength(Cobol.ClosePortFileIOUsingAssociatedDataLength closePortFileIOUsingAssociatedDataLength, PrintOutputCapture<P> p) {
        visitSpace(closePortFileIOUsingAssociatedDataLength.getPrefix(), p);
        visitMarkers(closePortFileIOUsingAssociatedDataLength.getMarkers(), p);
        visit(closePortFileIOUsingAssociatedDataLength.getWords(), p);
        visit(closePortFileIOUsingAssociatedDataLength.getIdentifier(), p);
        return closePortFileIOUsingAssociatedDataLength;
    }

    public Cobol visitClosePortFileIOUsingCloseDisposition(Cobol.ClosePortFileIOUsingCloseDisposition closePortFileIOUsingCloseDisposition, PrintOutputCapture<P> p) {
        visitSpace(closePortFileIOUsingCloseDisposition.getPrefix(), p);
        visitMarkers(closePortFileIOUsingCloseDisposition.getMarkers(), p);
        visit(closePortFileIOUsingCloseDisposition.getWords(), p);
        return closePortFileIOUsingCloseDisposition;
    }

    public Cobol visitCloseReelUnitStatement(Cobol.CloseReelUnitStatement closeReelUnitStatement, PrintOutputCapture<P> p) {
        visitSpace(closeReelUnitStatement.getPrefix(), p);
        visitMarkers(closeReelUnitStatement.getMarkers(), p);
        visit(closeReelUnitStatement.getWords(), p);
        return closeReelUnitStatement;
    }

    public Cobol visitCloseRelativeStatement(Cobol.CloseRelativeStatement closeRelativeStatement, PrintOutputCapture<P> p) {
        visitSpace(closeRelativeStatement.getPrefix(), p);
        visitMarkers(closeRelativeStatement.getMarkers(), p);
        visit(closeRelativeStatement.getWords(), p);
        return closeRelativeStatement;
    }

    public Cobol visitCobolWord(Cobol.CobolWord cobolWord, PrintOutputCapture<P> p) {
        visitSpace(cobolWord.getPrefix(), p);
        visitMarkers(cobolWord.getMarkers(), p);
        p.append(cobolWord.getWord());
        return cobolWord;
    }

    public Cobol visitCodeSetClause(Cobol.CodeSetClause codeSetClause, PrintOutputCapture<P> p) {
        visitSpace(codeSetClause.getPrefix(), p);
        visitMarkers(codeSetClause.getMarkers(), p);
        visit(codeSetClause.getWords(), p);
        visit(codeSetClause.getAlphabetName(), p);
        return codeSetClause;
    }

    public Cobol visitCollatingSequenceAlphabet(Cobol.CollatingSequenceAlphabet collatingSequenceAlphabet, PrintOutputCapture<P> p) {
        visitSpace(collatingSequenceAlphabet.getPrefix(), p);
        visitMarkers(collatingSequenceAlphabet.getMarkers(), p);
        visit(collatingSequenceAlphabet.getWords(), p);
        visit(collatingSequenceAlphabet.getAlphabetName(), p);
        return collatingSequenceAlphabet;
    }

    public Cobol visitCollatingSequenceClause(Cobol.CollatingSequenceClause collatingSequenceClause, PrintOutputCapture<P> p) {
        visitSpace(collatingSequenceClause.getPrefix(), p);
        visitMarkers(collatingSequenceClause.getMarkers(), p);
        visit(collatingSequenceClause.getWords(), p);
        visitContainer("", collatingSequenceClause.getPadding().getAlphabetName(), "", "", p);
        visit(collatingSequenceClause.getAlphanumeric(), p);
        visit(collatingSequenceClause.getNational(), p);
        return collatingSequenceClause;
    }

    public Cobol visitCombinableCondition(Cobol.CombinableCondition combinableCondition, PrintOutputCapture<P> p) {
        visitSpace(combinableCondition.getPrefix(), p);
        visitMarkers(combinableCondition.getMarkers(), p);
        visit(combinableCondition.getNot(), p);
        visit(combinableCondition.getSimpleCondition(), p);
        return combinableCondition;
    }

    public Cobol visitCommitmentControlClause(Cobol.CommitmentControlClause commitmentControlClause, PrintOutputCapture<P> p) {
        visitSpace(commitmentControlClause.getPrefix(), p);
        visitMarkers(commitmentControlClause.getMarkers(), p);
        visit(commitmentControlClause.getWords(), p);
        visit(commitmentControlClause.getFileName(), p);
        return commitmentControlClause;
    }

    public Cobol visitCommunicationDescriptionEntryFormat1(Cobol.CommunicationDescriptionEntryFormat1 communicationDescriptionEntryFormat1, PrintOutputCapture<P> p) {
        visitSpace(communicationDescriptionEntryFormat1.getPrefix(), p);
        visitMarkers(communicationDescriptionEntryFormat1.getMarkers(), p);
        visit(communicationDescriptionEntryFormat1.getCd(), p);
        visit(communicationDescriptionEntryFormat1.getName(), p);
        visit(communicationDescriptionEntryFormat1.getWords(), p);
        visitContainer("", communicationDescriptionEntryFormat1.getPadding().getInputs(), "", ".", p);
        return communicationDescriptionEntryFormat1;
    }

    public Cobol visitCommunicationDescriptionEntryFormat2(Cobol.CommunicationDescriptionEntryFormat2 communicationDescriptionEntryFormat2, PrintOutputCapture<P> p) {
        visitSpace(communicationDescriptionEntryFormat2.getPrefix(), p);
        visitMarkers(communicationDescriptionEntryFormat2.getMarkers(), p);
        visit(communicationDescriptionEntryFormat2.getCd(), p);
        visit(communicationDescriptionEntryFormat2.getName(), p);
        visit(communicationDescriptionEntryFormat2.getWords(), p);
        visitContainer("", communicationDescriptionEntryFormat2.getPadding().getOutputs(), "", ".", p);
        return communicationDescriptionEntryFormat2;
    }

    public Cobol visitCommunicationDescriptionEntryFormat3(Cobol.CommunicationDescriptionEntryFormat3 communicationDescriptionEntryFormat3, PrintOutputCapture<P> p) {
        visitSpace(communicationDescriptionEntryFormat3.getPrefix(), p);
        visitMarkers(communicationDescriptionEntryFormat3.getMarkers(), p);
        visit(communicationDescriptionEntryFormat3.getCd(), p);
        visit(communicationDescriptionEntryFormat3.getName(), p);
        visit(communicationDescriptionEntryFormat3.getWords(), p);
        visitContainer("", communicationDescriptionEntryFormat3.getPadding().getInitialIOs(), "", ".", p);
        return communicationDescriptionEntryFormat3;
    }

    public Cobol visitCommunicationSection(Cobol.CommunicationSection communicationSection, PrintOutputCapture<P> p) {
        visitSpace(communicationSection.getPrefix(), p);
        visitMarkers(communicationSection.getMarkers(), p);
        visit(communicationSection.getWords(), p);
        visitContainer("", communicationSection.getPadding().getEntries(), "", "", p);
        return communicationSection;
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

    public Cobol visitCompute(Cobol.Compute compute, PrintOutputCapture<P> p) {
        visitSpace(compute.getPrefix(), p);
        visitMarkers(compute.getMarkers(), p);
        visit(compute.getCompute(), p);
        visitContainer("", compute.getPadding().getRoundables(), "", "", p);
        visit(compute.getEqualWord(), p);
        visit(compute.getArithmeticExpression(), p);
        visit(compute.getOnSizeErrorPhrase(), p);
        visit(compute.getNotOnSizeErrorPhrase(), p);
        visit(compute.getEndCompute(), p);
        return compute;
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

    public Cobol visitConditionNameSubscriptReference(Cobol.ConditionNameSubscriptReference conditionNameSubscriptReference, PrintOutputCapture<P> p) {
        visitSpace(conditionNameSubscriptReference.getPrefix(), p);
        visitMarkers(conditionNameSubscriptReference.getMarkers(), p);
        visit(conditionNameSubscriptReference.getLeftParen(), p);
        for(Cobol c : conditionNameSubscriptReference.getSubscripts()) {
            visit(c, p);
        }
        visit(conditionNameSubscriptReference.getRightParen(), p);
        return conditionNameSubscriptReference;
    }

    public Cobol visitConfigurationSection(Cobol.ConfigurationSection configurationSection, PrintOutputCapture<P> p) {
        visitSpace(configurationSection.getPrefix(), p);
        visitMarkers(configurationSection.getMarkers(), p);
        visit(configurationSection.getWords(), p);
        visitContainer(".", configurationSection.getPadding().getParagraphs(), "", "", p);
        return configurationSection;
    }

    public Cobol visitContinue(Cobol.Continue continuez, PrintOutputCapture<P> p) {
        visitSpace(continuez.getPrefix(), p);
        visitMarkers(continuez.getMarkers(), p);
        visit(continuez.getWord(), p);
        return continuez;
    }

    public Cobol visitCurrencyClause(Cobol.CurrencyClause currencyClause, PrintOutputCapture<P> p) {
        visitSpace(currencyClause.getPrefix(), p);
        visitMarkers(currencyClause.getMarkers(), p);
        visit(currencyClause.getWords(), p);
        visit(currencyClause.getLiteral(), p);
        visitLeftPadded("", currencyClause.getPadding().getPictureSymbol(), p);
        visit(currencyClause.getPictureSymbolLiteral(), p);
        return currencyClause;
    }

    public Cobol visitDataAlignedClause(Cobol.DataAlignedClause dataAlignedClause, PrintOutputCapture<P> p) {
        visitSpace(dataAlignedClause.getPrefix(), p);
        visitMarkers(dataAlignedClause.getMarkers(), p);
        visit(dataAlignedClause.getAligned(), p);
        return dataAlignedClause;
    }

    public Cobol visitDataBaseSection(Cobol.DataBaseSection dataBaseSection, PrintOutputCapture<P> p) {
        visitSpace(dataBaseSection.getPrefix(), p);
        visitMarkers(dataBaseSection.getMarkers(), p);
        visit(dataBaseSection.getWords(), p);
        visitContainer(".", dataBaseSection.getPadding().getEntries(), "", "", p);
        return dataBaseSection;
    }

    public Cobol visitDataBaseSectionEntry(Cobol.DataBaseSectionEntry dataBaseSectionEntry, PrintOutputCapture<P> p) {
        visitSpace(dataBaseSectionEntry.getPrefix(), p);
        visitMarkers(dataBaseSectionEntry.getMarkers(), p);
        visit(dataBaseSectionEntry.getDb(), p);
        visit(dataBaseSectionEntry.getFrom(), p);
        visit(dataBaseSectionEntry.getInvoke(), p);
        visit(dataBaseSectionEntry.getTo(), p);
        return dataBaseSectionEntry;
    }

    public Cobol visitDataBlankWhenZeroClause(Cobol.DataBlankWhenZeroClause dataBlankWhenZeroClause, PrintOutputCapture<P> p) {
        visitSpace(dataBlankWhenZeroClause.getPrefix(), p);
        visitMarkers(dataBlankWhenZeroClause.getMarkers(), p);
        visit(dataBlankWhenZeroClause.getWords(), p);
        return dataBlankWhenZeroClause;
    }

    public Cobol visitDataCommonOwnLocalClause(Cobol.DataCommonOwnLocalClause dataCommonOwnLocalClause, PrintOutputCapture<P> p) {
        visitSpace(dataCommonOwnLocalClause.getPrefix(), p);
        visitMarkers(dataCommonOwnLocalClause.getMarkers(), p);
        visit(dataCommonOwnLocalClause.getWords(), p);
        return dataCommonOwnLocalClause;
    }

    public Cobol visitDataDescriptionEntry(Cobol.DataDescriptionEntry dataDescriptionEntry, PrintOutputCapture<P> p) {
        visitSpace(dataDescriptionEntry.getPrefix(), p);
        visitMarkers(dataDescriptionEntry.getMarkers(), p);
        visit(dataDescriptionEntry.getLevel(), p);
        visit(dataDescriptionEntry.getName(), p);
        visitContainer("", dataDescriptionEntry.getPadding().getClauses(), "", ".", p);
        return dataDescriptionEntry;
    }

    public Cobol visitDataDivision(Cobol.DataDivision dataDivision, PrintOutputCapture<P> p) {
        visitSpace(dataDivision.getPrefix(), p);
        visitMarkers(dataDivision.getMarkers(), p);
        visit(dataDivision.getWords(), p);
        visitContainer(".", dataDivision.getPadding().getSections(), "", "", p);
        return dataDivision;
    }

    public Cobol visitDataExternalClause(Cobol.DataExternalClause dataExternalClause, PrintOutputCapture<P> p) {
        visitSpace(dataExternalClause.getPrefix(), p);
        visitMarkers(dataExternalClause.getMarkers(), p);
        visit(dataExternalClause.getRedefines(), p);
        return dataExternalClause;
    }

    public Cobol visitDataGlobalClause(Cobol.DataGlobalClause dataGlobalClause, PrintOutputCapture<P> p) {
        visitSpace(dataGlobalClause.getPrefix(), p);
        visitMarkers(dataGlobalClause.getMarkers(), p);
        visit(dataGlobalClause.getWords(), p);
        return dataGlobalClause;
    }

    public Cobol visitDataIntegerStringClause(Cobol.DataIntegerStringClause dataIntegerStringClause, PrintOutputCapture<P> p) {
        visitSpace(dataIntegerStringClause.getPrefix(), p);
        visitMarkers(dataIntegerStringClause.getMarkers(), p);
        visit(dataIntegerStringClause.getRedefines(), p);
        return dataIntegerStringClause;
    }

    public Cobol visitDataJustifiedClause(Cobol.DataJustifiedClause dataJustifiedClause, PrintOutputCapture<P> p) {
        visitSpace(dataJustifiedClause.getPrefix(), p);
        visitMarkers(dataJustifiedClause.getMarkers(), p);
        visit(dataJustifiedClause.getWords(), p);
        return dataJustifiedClause;
    }

    public Cobol visitDataOccursClause(Cobol.DataOccursClause dataOccursClause, PrintOutputCapture<P> p) {
        visitSpace(dataOccursClause.getPrefix(), p);
        visitMarkers(dataOccursClause.getMarkers(), p);
        visit(dataOccursClause.getOccurs(), p);
        visit(dataOccursClause.getDataOccursTo(), p);
        visit(dataOccursClause.getTimes(), p);
        visit(dataOccursClause.getDataOccursDepending(), p);
        visitContainer("", dataOccursClause.getPadding().getSortIndexed(), "", "", p);
        return dataOccursClause;
    }

    public Cobol visitDataOccursDepending(Cobol.DataOccursDepending dataOccursDepending, PrintOutputCapture<P> p) {
        visitSpace(dataOccursDepending.getPrefix(), p);
        visitMarkers(dataOccursDepending.getMarkers(), p);
        visit(dataOccursDepending.getWords(), p);
        visit(dataOccursDepending.getQualifiedDataName(), p);
        return dataOccursDepending;
    }

    public Cobol visitDataOccursIndexed(Cobol.DataOccursIndexed dataOccursIndexed, PrintOutputCapture<P> p) {
        visitSpace(dataOccursIndexed.getPrefix(), p);
        visitMarkers(dataOccursIndexed.getMarkers(), p);
        visit(dataOccursIndexed.getWords(), p);
        visitContainer("", dataOccursIndexed.getPadding().getIndexNames(), "", "", p);
        return dataOccursIndexed;
    }

    public Cobol visitDataOccursSort(Cobol.DataOccursSort dataOccursSort, PrintOutputCapture<P> p) {
        visitSpace(dataOccursSort.getPrefix(), p);
        visitMarkers(dataOccursSort.getMarkers(), p);
        visit(dataOccursSort.getWords(), p);
        visitContainer("", dataOccursSort.getPadding().getQualifiedDataNames(), "", "", p);
        return dataOccursSort;
    }

    public Cobol visitDataOccursTo(Cobol.DataOccursTo dataOccursTo, PrintOutputCapture<P> p) {
        visitSpace(dataOccursTo.getPrefix(), p);
        visitMarkers(dataOccursTo.getMarkers(), p);
        visit(dataOccursTo.getTo(), p);
        visit(dataOccursTo.getIntegerLiteral(), p);
        return dataOccursTo;
    }

    public Cobol visitDataPictureClause(Cobol.DataPictureClause dataPictureClause, PrintOutputCapture<P> p) {
        visitSpace(dataPictureClause.getPrefix(), p);
        visitMarkers(dataPictureClause.getMarkers(), p);
        visit(dataPictureClause.getWords(), p);
        visitContainer("", dataPictureClause.getPadding().getPictures(), "", "", p);
        return dataPictureClause;
    }

    public Cobol visitDataReceivedByClause(Cobol.DataReceivedByClause dataReceivedByClause, PrintOutputCapture<P> p) {
        visitSpace(dataReceivedByClause.getPrefix(), p);
        visitMarkers(dataReceivedByClause.getMarkers(), p);
        visit(dataReceivedByClause.getWords(), p);
        return dataReceivedByClause;
    }

    public Cobol visitDataRecordAreaClause(Cobol.DataRecordAreaClause dataRecordAreaClause, PrintOutputCapture<P> p) {
        visitSpace(dataRecordAreaClause.getPrefix(), p);
        visitMarkers(dataRecordAreaClause.getMarkers(), p);
        visit(dataRecordAreaClause.getWords(), p);
        return dataRecordAreaClause;
    }

    public Cobol visitDataRecordsClause(Cobol.DataRecordsClause dataRecordsClause, PrintOutputCapture<P> p) {
        visitSpace(dataRecordsClause.getPrefix(), p);
        visitMarkers(dataRecordsClause.getMarkers(), p);
        visit(dataRecordsClause.getWords(), p);
        visitContainer("", dataRecordsClause.getPadding().getDataName(), "", "", p);
        return dataRecordsClause;
    }

    public Cobol visitDataRedefinesClause(Cobol.DataRedefinesClause dataRedefinesClause, PrintOutputCapture<P> p) {
        visitSpace(dataRedefinesClause.getPrefix(), p);
        visitMarkers(dataRedefinesClause.getMarkers(), p);
        visit(dataRedefinesClause.getRedefines(), p);
        visit(dataRedefinesClause.getDataName(), p);
        return dataRedefinesClause;
    }

    public Cobol visitDataRenamesClause(Cobol.DataRenamesClause dataRenamesClause, PrintOutputCapture<P> p) {
        visitSpace(dataRenamesClause.getPrefix(), p);
        visitMarkers(dataRenamesClause.getMarkers(), p);
        visit(dataRenamesClause.getRenames(), p);
        visit(dataRenamesClause.getFromName(), p);
        visit(dataRenamesClause.getThrough(), p);
        visit(dataRenamesClause.getToName(), p);
        return dataRenamesClause;
    }

    public Cobol visitDataSignClause(Cobol.DataSignClause dataSignClause, PrintOutputCapture<P> p) {
        visitSpace(dataSignClause.getPrefix(), p);
        visitMarkers(dataSignClause.getMarkers(), p);
        visit(dataSignClause.getWords(), p);
        return dataSignClause;
    }

    public Cobol visitDataSynchronizedClause(Cobol.DataSynchronizedClause dataSynchronizedClause, PrintOutputCapture<P> p) {
        visitSpace(dataSynchronizedClause.getPrefix(), p);
        visitMarkers(dataSynchronizedClause.getMarkers(), p);
        visit(dataSynchronizedClause.getWords(), p);
        return dataSynchronizedClause;
    }

    public Cobol visitDataThreadLocalClause(Cobol.DataThreadLocalClause dataThreadLocalClause, PrintOutputCapture<P> p) {
        visitSpace(dataThreadLocalClause.getPrefix(), p);
        visitMarkers(dataThreadLocalClause.getMarkers(), p);
        visit(dataThreadLocalClause.getWords(), p);
        return dataThreadLocalClause;
    }

    public Cobol visitDataTypeClause(Cobol.DataTypeClause dataTypeClause, PrintOutputCapture<P> p) {
        visitSpace(dataTypeClause.getPrefix(), p);
        visitMarkers(dataTypeClause.getMarkers(), p);
        visit(dataTypeClause.getWords(), p);
        visit(dataTypeClause.getParenthesized(), p);
        return dataTypeClause;
    }

    public Cobol visitDataTypeDefClause(Cobol.DataTypeDefClause dataTypeDefClause, PrintOutputCapture<P> p) {
        visitSpace(dataTypeDefClause.getPrefix(), p);
        visitMarkers(dataTypeDefClause.getMarkers(), p);
        visit(dataTypeDefClause.getWords(), p);
        return dataTypeDefClause;
    }

    public Cobol visitDataUsageClause(Cobol.DataUsageClause dataUsageClause, PrintOutputCapture<P> p) {
        visitSpace(dataUsageClause.getPrefix(), p);
        visitMarkers(dataUsageClause.getMarkers(), p);
        visit(dataUsageClause.getWords(), p);
        return dataUsageClause;
    }

    public Cobol visitDataUsingClause(Cobol.DataUsingClause dataUsingClause, PrintOutputCapture<P> p) {
        visitSpace(dataUsingClause.getPrefix(), p);
        visitMarkers(dataUsingClause.getMarkers(), p);
        visit(dataUsingClause.getWords(), p);
        return dataUsingClause;
    }

    public Cobol visitDataValueClause(Cobol.DataValueClause dataValueClause, PrintOutputCapture<P> p) {
        visitSpace(dataValueClause.getPrefix(), p);
        visitMarkers(dataValueClause.getMarkers(), p);
        visit(dataValueClause.getWords(), p);
        for (Cobol c : dataValueClause.getCobols()) {
            visit(c, p);
        }
        return dataValueClause;
    }

    public Cobol visitDataWithLowerBoundsClause(Cobol.DataWithLowerBoundsClause dataWithLowerBoundsClause, PrintOutputCapture<P> p) {
        visitSpace(dataWithLowerBoundsClause.getPrefix(), p);
        visitMarkers(dataWithLowerBoundsClause.getMarkers(), p);
        visit(dataWithLowerBoundsClause.getWords(), p);
        return dataWithLowerBoundsClause;
    }

    public Cobol visitDecimalPointClause(Cobol.DecimalPointClause decimalPointClause, PrintOutputCapture<P> p) {
        visitSpace(decimalPointClause.getPrefix(), p);
        visitMarkers(decimalPointClause.getMarkers(), p);
        visit(decimalPointClause.getWords(), p);
        return decimalPointClause;
    }

    public Cobol visitDefaultComputationalSignClause(Cobol.DefaultComputationalSignClause defaultComputationalSignClause, PrintOutputCapture<P> p) {
        visitSpace(defaultComputationalSignClause.getPrefix(), p);
        visitMarkers(defaultComputationalSignClause.getMarkers(), p);
        visit(defaultComputationalSignClause.getWords(), p);
        return defaultComputationalSignClause;
    }

    public Cobol visitDefaultDisplaySignClause(Cobol.DefaultDisplaySignClause defaultDisplaySignClause, PrintOutputCapture<P> p) {
        visitSpace(defaultDisplaySignClause.getPrefix(), p);
        visitMarkers(defaultDisplaySignClause.getMarkers(), p);
        visit(defaultDisplaySignClause.getWords(), p);
        return defaultDisplaySignClause;
    }

    public Cobol visitDelete(Cobol.Delete delete, PrintOutputCapture<P> p) {
        visitSpace(delete.getPrefix(), p);
        visitMarkers(delete.getMarkers(), p);
        visit(delete.getDelete(), p);
        visit(delete.getRecord(), p);
        visit(delete.getInvalidKey(), p);
        visit(delete.getNotInvalidKey(), p);
        visit(delete.getEndDelete(), p);
        return delete;
    }

    public Cobol visitDestinationCountClause(Cobol.DestinationCountClause destinationCountClause, PrintOutputCapture<P> p) {
        visitSpace(destinationCountClause.getPrefix(), p);
        visitMarkers(destinationCountClause.getMarkers(), p);
        visit(destinationCountClause.getWords(), p);
        visit(destinationCountClause.getDataDescName(), p);
        return destinationCountClause;
    }

    public Cobol visitDestinationTableClause(Cobol.DestinationTableClause destinationTableClause, PrintOutputCapture<P> p) {
        visitSpace(destinationTableClause.getPrefix(), p);
        visitMarkers(destinationTableClause.getMarkers(), p);
        visit(destinationTableClause.getFirstWords(), p);
        visit(destinationTableClause.getIntegerLiteral(), p);
        visit(destinationTableClause.getSecondWords(), p);
        visitContainer("", destinationTableClause.getPadding().getIndexNames(), "", "", p);
        return destinationTableClause;
    }

    public Cobol visitDisable(Cobol.Disable disable, PrintOutputCapture<P> p) {
        visitSpace(disable.getPrefix(), p);
        visitMarkers(disable.getMarkers(), p);
        visit(disable.getDisable(), p);
        visit(disable.getType(), p);
        visit(disable.getWith(), p);
        visit(disable.getKey(), p);
        return disable;
    }

    public Cobol visitDisplay(Cobol.Display display, PrintOutputCapture<P> p) {
        visitSpace(display.getPrefix(), p);
        visitMarkers(display.getMarkers(), p);
        visit(display.getDisplay(), p);
        for (Name d : display.getOperands()) {
            visit(d, p);
        }
        return display;
    }

    public Cobol visitDivide(Cobol.Divide divide, PrintOutputCapture<P> p) {
        visitSpace(divide.getPrefix(), p);
        visitMarkers(divide.getMarkers(), p);
        visit(divide.getDivide(), p);
        visit(divide.getName(), p);
        visit(divide.getAction(), p);
        visit(divide.getDivideRemainder(), p);
        visit(divide.getOnSizeErrorPhrase(), p);
        visit(divide.getNotOnSizeErrorPhrase(), p);
        visit(divide.getEndDivide(), p);
        return divide;
    }

    public Cobol visitDivideGiving(Cobol.DivideGiving divideGiving, PrintOutputCapture<P> p) {
        visitSpace(divideGiving.getPrefix(), p);
        visitMarkers(divideGiving.getMarkers(), p);
        visit(divideGiving.getWord(), p);
        visit(divideGiving.getName(), p);
        visit(divideGiving.getDivideGivingPhrase(), p);
        return divideGiving;
    }

    public Cobol visitDivideGivingPhrase(Cobol.DivideGivingPhrase divideGivingPhrase, PrintOutputCapture<P> p) {
        visitSpace(divideGivingPhrase.getPrefix(), p);
        visitMarkers(divideGivingPhrase.getMarkers(), p);
        visit(divideGivingPhrase.getGiving(), p);
        visitContainer("", divideGivingPhrase.getPadding().getRoundable(), "", "", p);
        return divideGivingPhrase;
    }

    public Cobol visitDivideInto(Cobol.DivideInto divideInto, PrintOutputCapture<P> p) {
        visitSpace(divideInto.getPrefix(), p);
        visitMarkers(divideInto.getMarkers(), p);
        visit(divideInto.getInto(), p);
        visitContainer("", divideInto.getPadding().getRoundable(), "", "", p);
        return divideInto;
    }

    public Cobol visitDivideRemainder(Cobol.DivideRemainder divideRemainder, PrintOutputCapture<P> p) {
        visitSpace(divideRemainder.getPrefix(), p);
        visitMarkers(divideRemainder.getMarkers(), p);
        visit(divideRemainder.getRemainder(), p);
        visit(divideRemainder.getName(), p);
        return divideRemainder;
    }

    public Cobol visitEnable(Cobol.Enable enable, PrintOutputCapture<P> p) {
        visitSpace(enable.getPrefix(), p);
        visitMarkers(enable.getMarkers(), p);
        visit(enable.getEnable(), p);
        visit(enable.getType(), p);
        visit(enable.getWith(), p);
        visit(enable.getKey(), p);
        return enable;
    }

    public Cobol visitEndProgram(Cobol.EndProgram endProgram, PrintOutputCapture<P> p) {
        visitSpace(endProgram.getPrefix(), p);
        visitMarkers(endProgram.getMarkers(), p);
        visit(endProgram.getWords(), p);
        return endProgram;
    }

    public Cobol visitEnvironmentDivision(Cobol.EnvironmentDivision environmentDivision, PrintOutputCapture<P> p) {
        visitSpace(environmentDivision.getPrefix(), p);
        visitMarkers(environmentDivision.getMarkers(), p);
        visit(environmentDivision.getWords(), p);
        visitContainer(".", environmentDivision.getPadding().getBody(), "", "", p);
        return environmentDivision;
    }

    public Cobol visitEvaluate(Cobol.Evaluate evaluate, PrintOutputCapture<P> p) {
        visitSpace(evaluate.getPrefix(), p);
        visitMarkers(evaluate.getMarkers(), p);
        visit(evaluate.getEvaluate(), p);
        visit(evaluate.getSelect(), p);
        visitContainer("", evaluate.getPadding().getAlsoSelect(), "", "", p);
        visitContainer("", evaluate.getPadding().getWhenPhrase(), "", "", p);
        visit(evaluate.getWhenOther(), p);
        visit(evaluate.getEndPhrase(), p);
        return evaluate;
    }

    public Cobol visitEvaluateAlso(Cobol.EvaluateAlso evaluateAlso, PrintOutputCapture<P> p) {
        visitSpace(evaluateAlso.getPrefix(), p);
        visitMarkers(evaluateAlso.getMarkers(), p);
        visit(evaluateAlso.getAlso(), p);
        visit(evaluateAlso.getSelect(), p);
        return evaluateAlso;
    }

    public Cobol visitEvaluateAlsoCondition(Cobol.EvaluateAlsoCondition evaluateAlsoCondition, PrintOutputCapture<P> p) {
        visitSpace(evaluateAlsoCondition.getPrefix(), p);
        visitMarkers(evaluateAlsoCondition.getMarkers(), p);
        visit(evaluateAlsoCondition.getAlso(), p);
        visit(evaluateAlsoCondition.getCondition(), p);
        return evaluateAlsoCondition;
    }

    public Cobol visitEvaluateCondition(Cobol.EvaluateCondition evaluateCondition, PrintOutputCapture<P> p) {
        visitSpace(evaluateCondition.getPrefix(), p);
        visitMarkers(evaluateCondition.getMarkers(), p);
        visit(evaluateCondition.getWords(), p);
        visit(evaluateCondition.getCondition(), p);
        visit(evaluateCondition.getEvaluateThrough(), p);
        return evaluateCondition;
    }

    public Cobol visitEvaluateThrough(Cobol.EvaluateThrough evaluateThrough, PrintOutputCapture<P> p) {
        visitSpace(evaluateThrough.getPrefix(), p);
        visitMarkers(evaluateThrough.getMarkers(), p);
        visit(evaluateThrough.getThrough(), p);
        visit(evaluateThrough.getValue(), p);
        return evaluateThrough;
    }

    public Cobol visitEvaluateValueThrough(Cobol.EvaluateValueThrough evaluateValueThrough, PrintOutputCapture<P> p) {
        visitSpace(evaluateValueThrough.getPrefix(), p);
        visitMarkers(evaluateValueThrough.getMarkers(), p);
        visit(evaluateValueThrough.getNot(), p);
        visit(evaluateValueThrough.getValue(), p);
        visit(evaluateValueThrough.getEvaluateThrough(), p);
        return evaluateValueThrough;
    }

    public Cobol visitEvaluateWhen(Cobol.EvaluateWhen evaluateWhen, PrintOutputCapture<P> p) {
        visitSpace(evaluateWhen.getPrefix(), p);
        visitMarkers(evaluateWhen.getMarkers(), p);
        visit(evaluateWhen.getWhen(), p);
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

    public Cobol visitExecCicsStatement(Cobol.ExecCicsStatement execCicsStatement, PrintOutputCapture<P> p) {
        visitSpace(execCicsStatement.getPrefix(), p);
        visitMarkers(execCicsStatement.getMarkers(), p);
        visitContainer("", execCicsStatement.getPadding().getExecCicsLines(), "", "", p);
        return execCicsStatement;
    }

    public Cobol visitExecSqlImsStatement(Cobol.ExecSqlImsStatement execSqlImsStatement, PrintOutputCapture<P> p) {
        visitSpace(execSqlImsStatement.getPrefix(), p);
        visitMarkers(execSqlImsStatement.getMarkers(), p);
        visitContainer("", execSqlImsStatement.getPadding().getExecSqlLmsLines(), "", "", p);
        return execSqlImsStatement;
    }

    public Cobol visitExecSqlStatement(Cobol.ExecSqlStatement execSqlStatement, PrintOutputCapture<P> p) {
        visitSpace(execSqlStatement.getPrefix(), p);
        visitMarkers(execSqlStatement.getMarkers(), p);
        visitContainer("", execSqlStatement.getPadding().getExecSqlLines(), "", "", p);
        return execSqlStatement;
    }

    public Cobol visitExhibit(Cobol.Exhibit exhibit, PrintOutputCapture<P> p) {
        visitSpace(exhibit.getPrefix(), p);
        visitMarkers(exhibit.getMarkers(), p);
        visit(exhibit.getWords(), p);
        visitContainer("", exhibit.getPadding().getOperands(), "", "", p);
        return exhibit;
    }

    public Cobol visitExit(Cobol.Exit exit, PrintOutputCapture<P> p) {
        visitSpace(exit.getPrefix(), p);
        visitMarkers(exit.getMarkers(), p);
        visit(exit.getWords(), p);
        return exit;
    }

    public Cobol visitExternalClause(Cobol.ExternalClause externalClause, PrintOutputCapture<P> p) {
        visitSpace(externalClause.getPrefix(), p);
        visitMarkers(externalClause.getMarkers(), p);
        visit(externalClause.getWords(), p);
        return externalClause;
    }

    public Cobol visitFileControlEntry(Cobol.FileControlEntry fileControlEntry, PrintOutputCapture<P> p) {
        visitSpace(fileControlEntry.getPrefix(), p);
        visitMarkers(fileControlEntry.getMarkers(), p);
        visit(fileControlEntry.getSelectClause(), p);
        visitContainer("", fileControlEntry.getPadding().getControlClauses(), "", "", p);
        return fileControlEntry;
    }

    public Cobol visitFileControlParagraph(Cobol.FileControlParagraph fileControlParagraph, PrintOutputCapture<P> p) {
        visitSpace(fileControlParagraph.getPrefix(), p);
        visitMarkers(fileControlParagraph.getMarkers(), p);
        visit(fileControlParagraph.getFileControl(), p);
        if (fileControlParagraph.getControlEntries() != null) {
            for (Cobol c : fileControlParagraph.getControlEntries()) {
                visit(c, p);
            }
        }
        visit(fileControlParagraph.getDot(), p);
        return fileControlParagraph;
    }

    public Cobol visitFileDescriptionEntry(Cobol.FileDescriptionEntry fileDescriptionEntry, PrintOutputCapture<P> p) {
        visitSpace(fileDescriptionEntry.getPrefix(), p);
        visitMarkers(fileDescriptionEntry.getMarkers(), p);
        visit(fileDescriptionEntry.getWords(), p);
        visit(fileDescriptionEntry.getName(), p);
        if (fileDescriptionEntry.getClauses() != null) {
            for (Cobol c : fileDescriptionEntry.getClauses()) {
                visit(c, p);
            }
        }
        visitContainer(".", fileDescriptionEntry.getPadding().getDataDescriptions(), "", "", p);
        return fileDescriptionEntry;
    }

    public Cobol visitFileSection(Cobol.FileSection fileSection, PrintOutputCapture<P> p) {
        visitSpace(fileSection.getPrefix(), p);
        visitMarkers(fileSection.getMarkers(), p);
        visit(fileSection.getWords(), p);
        visitContainer(".", fileSection.getPadding().getFileDescriptionEntry(), "", "", p);
        return fileSection;
    }

    public Cobol visitFileStatusClause(Cobol.FileStatusClause fileStatusClause, PrintOutputCapture<P> p) {
        visitSpace(fileStatusClause.getPrefix(), p);
        visitMarkers(fileStatusClause.getMarkers(), p);
        visit(fileStatusClause.getWords(), p);
        visitContainer("", fileStatusClause.getPadding().getQualifiedDataNames(), "", "", p);
        return fileStatusClause;
    }

    public Cobol visitFunctionCall(Cobol.FunctionCall functionCall, PrintOutputCapture<P> p) {
        visitSpace(functionCall.getPrefix(), p);
        visitMarkers(functionCall.getMarkers(), p);
        visit(functionCall.getFunction(), p);
        visit(functionCall.getFunctionName(), p);
        visitContainer("", functionCall.getPadding().getArguments(), "", "", p);
        visit(functionCall.getReferenceModifier(), p);
        return functionCall;
    }

    public Cobol visitGenerate(Cobol.Generate generate, PrintOutputCapture<P> p) {
        visitSpace(generate.getPrefix(), p);
        visitMarkers(generate.getMarkers(), p);
        visit(generate.getGenerate(), p);
        visit(generate.getReportName(), p);
        return generate;
    }

    public Cobol visitGlobalClause(Cobol.GlobalClause globalClause, PrintOutputCapture<P> p) {
        visitSpace(globalClause.getPrefix(), p);
        visitMarkers(globalClause.getMarkers(), p);
        visit(globalClause.getWords(), p);
        return globalClause;
    }

    public Cobol visitGoBack(Cobol.GoBack goBack, PrintOutputCapture<P> p) {
        visitSpace(goBack.getPrefix(), p);
        visitMarkers(goBack.getMarkers(), p);
        visit(goBack.getGoBack(), p);
        return goBack;
    }

    public Cobol visitGoTo(Cobol.GoTo _goTo, PrintOutputCapture<P> p) {
        visitSpace(_goTo.getPrefix(), p);
        visitMarkers(_goTo.getMarkers(), p);
        visit(_goTo.getWords(), p);
        visit(_goTo.getStatement(), p);
        return _goTo;
    }

    public Cobol visitGoToDependingOnStatement(Cobol.GoToDependingOnStatement goToDependingOnStatement, PrintOutputCapture<P> p) {
        visitSpace(goToDependingOnStatement.getPrefix(), p);
        visitMarkers(goToDependingOnStatement.getMarkers(), p);
        visitContainer("", goToDependingOnStatement.getPadding().getProcedureNames(), "", "", p);
        visit(goToDependingOnStatement.getWords(), p);
        visit(goToDependingOnStatement.getIdentifier(), p);
        return goToDependingOnStatement;
    }

    public Cobol visitIdentificationDivision(Cobol.IdentificationDivision identificationDivision, PrintOutputCapture<P> p) {
        visitSpace(identificationDivision.getPrefix(), p);
        visitMarkers(identificationDivision.getMarkers(), p);
        visit(identificationDivision.getWords(), p);
        visit(identificationDivision.getProgramIdParagraph(), p);
        return identificationDivision;
    }

    public Cobol visitIf(Cobol.If _if, PrintOutputCapture<P> p) {
        visitSpace(_if.getPrefix(), p);
        visitMarkers(_if.getMarkers(), p);
        visit(_if.getWord(), p);
        visit(_if.getCondition(), p);
        visit(_if.getIfThen(), p);
        visit(_if.getIfElse(), p);
        visit(_if.getEndIf(), p);
        return _if;
    }

    public Cobol visitIfElse(Cobol.IfElse ifElse, PrintOutputCapture<P> p) {
        visitSpace(ifElse.getPrefix(), p);
        visitMarkers(ifElse.getMarkers(), p);
        visit(ifElse.getWord(), p);
        visit(ifElse.getNextSentence(), p);
        visitContainer("", ifElse.getPadding().getStatements(), "", "", p);
        return ifElse;
    }

    public Cobol visitIfThen(Cobol.IfThen ifThen, PrintOutputCapture<P> p) {
        visitSpace(ifThen.getPrefix(), p);
        visitMarkers(ifThen.getMarkers(), p);
        visit(ifThen.getWord(), p);
        visit(ifThen.getNextSentence(), p);
        visitContainer("", ifThen.getPadding().getStatements(), "", "", p);
        return ifThen;
    }

    public Cobol visitInData(Cobol.InData inData, PrintOutputCapture<P> p) {
        visitSpace(inData.getPrefix(), p);
        visitMarkers(inData.getMarkers(), p);
        visit(inData.getWords(), p);
        visit(inData.getName(), p);
        return inData;
    }

    public Cobol visitInFile(Cobol.InFile inFile, PrintOutputCapture<P> p) {
        visitSpace(inFile.getPrefix(), p);
        visitMarkers(inFile.getMarkers(), p);
        visit(inFile.getWords(), p);
        visit(inFile.getName(), p);
        return inFile;
    }

    public Cobol visitInLibrary(Cobol.InLibrary inLibrary, PrintOutputCapture<P> p) {
        visitSpace(inLibrary.getPrefix(), p);
        visitMarkers(inLibrary.getMarkers(), p);
        visit(inLibrary.getWords(), p);
        visit(inLibrary.getName(), p);
        return inLibrary;
    }

    public Cobol visitInMnemonic(Cobol.InMnemonic inMnemonic, PrintOutputCapture<P> p) {
        visitSpace(inMnemonic.getPrefix(), p);
        visitMarkers(inMnemonic.getMarkers(), p);
        visit(inMnemonic.getWords(), p);
        visit(inMnemonic.getName(), p);
        return inMnemonic;
    }

    public Cobol visitInSection(Cobol.InSection inSection, PrintOutputCapture<P> p) {
        visitSpace(inSection.getPrefix(), p);
        visitMarkers(inSection.getMarkers(), p);
        visit(inSection.getWords(), p);
        visit(inSection.getName(), p);
        return inSection;
    }

    public Cobol visitInTable(Cobol.InTable inTable, PrintOutputCapture<P> p) {
        visitSpace(inTable.getPrefix(), p);
        visitMarkers(inTable.getMarkers(), p);
        visit(inTable.getWords(), p);
        return inTable;
    }

    public Cobol visitInitialize(Cobol.Initialize initialize, PrintOutputCapture<P> p) {
        visitSpace(initialize.getPrefix(), p);
        visitMarkers(initialize.getMarkers(), p);
        visit(initialize.getInitialize(), p);
        visitContainer("", initialize.getPadding().getIdentifiers(), "", "", p);
        visit(initialize.getInitializeReplacingPhrase(), p);
        return initialize;
    }

    public Cobol visitInitializeReplacingBy(Cobol.InitializeReplacingBy initializeReplacingBy, PrintOutputCapture<P> p) {
        visitSpace(initializeReplacingBy.getPrefix(), p);
        visitMarkers(initializeReplacingBy.getMarkers(), p);
        visit(initializeReplacingBy.getWords(), p);
        visit(initializeReplacingBy.getIdentifier(), p);
        return initializeReplacingBy;
    }

    public Cobol visitInitializeReplacingPhrase(Cobol.InitializeReplacingPhrase initializeReplacingPhrase, PrintOutputCapture<P> p) {
        visitSpace(initializeReplacingPhrase.getPrefix(), p);
        visitMarkers(initializeReplacingPhrase.getMarkers(), p);
        visit(initializeReplacingPhrase.getReplacing(), p);
        visitContainer("", initializeReplacingPhrase.getPadding().getInitializeReplacingBy(), "", "", p);
        return initializeReplacingPhrase;
    }

    public Cobol visitInitiate(Cobol.Initiate initiate, PrintOutputCapture<P> p) {
        visitSpace(initiate.getPrefix(), p);
        visitMarkers(initiate.getMarkers(), p);
        visit(initiate.getInitiate(), p);
        visitContainer("", initiate.getPadding().getReportNames(), "", "", p);
        return initiate;
    }

    public Cobol visitInputOutputSection(Cobol.InputOutputSection inputOutputSection, PrintOutputCapture<P> p) {
        visitSpace(inputOutputSection.getPrefix(), p);
        visitMarkers(inputOutputSection.getMarkers(), p);
        visit(inputOutputSection.getWords(), p);
        visitContainer("", inputOutputSection.getPadding().getParagraphs(), "", "", p);
        return inputOutputSection;
    }

    public Cobol visitInspect(Cobol.Inspect inspect, PrintOutputCapture<P> p) {
        visitSpace(inspect.getPrefix(), p);
        visitMarkers(inspect.getMarkers(), p);
        visit(inspect.getInspect(), p);
        visit(inspect.getIdentifier(), p);
        visit(inspect.getPhrase(), p);
        return inspect;
    }

    public Cobol visitInspectAllLeading(Cobol.InspectAllLeading inspectAllLeading, PrintOutputCapture<P> p) {
        visitSpace(inspectAllLeading.getPrefix(), p);
        visitMarkers(inspectAllLeading.getMarkers(), p);
        visit(inspectAllLeading.getWord(), p);
        visitContainer("", inspectAllLeading.getPadding().getInspections(), "", "", p);
        return inspectAllLeading;
    }

    public Cobol visitInspectAllLeadings(Cobol.InspectAllLeadings inspectAllLeadings, PrintOutputCapture<P> p) {
        visitSpace(inspectAllLeadings.getPrefix(), p);
        visitMarkers(inspectAllLeadings.getMarkers(), p);
        visit(inspectAllLeadings.getWord(), p);
        visitContainer("", inspectAllLeadings.getPadding().getLeadings(), "", "", p);
        return inspectAllLeadings;
    }

    public Cobol visitInspectBeforeAfter(Cobol.InspectBeforeAfter inspectBeforeAfter, PrintOutputCapture<P> p) {
        visitSpace(inspectBeforeAfter.getPrefix(), p);
        visitMarkers(inspectBeforeAfter.getMarkers(), p);
        visit(inspectBeforeAfter.getWords(), p);
        visit(inspectBeforeAfter.getIdentifier(), p);
        return inspectBeforeAfter;
    }

    public Cobol visitInspectBy(Cobol.InspectBy inspectBy, PrintOutputCapture<P> p) {
        visitSpace(inspectBy.getPrefix(), p);
        visitMarkers(inspectBy.getMarkers(), p);
        visit(inspectBy.getBy(), p);
        visit(inspectBy.getIdentifier(), p);
        return inspectBy;
    }

    public Cobol visitInspectCharacters(Cobol.InspectCharacters inspectCharacters, PrintOutputCapture<P> p) {
        visitSpace(inspectCharacters.getPrefix(), p);
        visitMarkers(inspectCharacters.getMarkers(), p);
        visit(inspectCharacters.getCharacter(), p);
        visitContainer("", inspectCharacters.getPadding().getInspections(), "", "", p);
        return inspectCharacters;
    }

    public Cobol visitInspectConvertingPhrase(Cobol.InspectConvertingPhrase inspectConvertingPhrase, PrintOutputCapture<P> p) {
        visitSpace(inspectConvertingPhrase.getPrefix(), p);
        visitMarkers(inspectConvertingPhrase.getMarkers(), p);
        visit(inspectConvertingPhrase.getConverting(), p);
        visit(inspectConvertingPhrase.getIdentifier(), p);
        visit(inspectConvertingPhrase.getInspectTo(), p);
        visitContainer("", inspectConvertingPhrase.getPadding().getInspections(), "", "", p);
        return inspectConvertingPhrase;
    }

    public Cobol visitInspectFor(Cobol.InspectFor inspectFor, PrintOutputCapture<P> p) {
        visitSpace(inspectFor.getPrefix(), p);
        visitMarkers(inspectFor.getMarkers(), p);
        visit(inspectFor.getIdentifier(), p);
        visit(inspectFor.getWord(), p);
        visitContainer("", inspectFor.getPadding().getInspects(), "", "", p);
        return inspectFor;
    }

    public Cobol visitInspectReplacingAllLeading(Cobol.InspectReplacingAllLeading inspectReplacingAllLeading, PrintOutputCapture<P> p) {
        visitSpace(inspectReplacingAllLeading.getPrefix(), p);
        visitMarkers(inspectReplacingAllLeading.getMarkers(), p);
        visit(inspectReplacingAllLeading.getIdentifier(), p);
        visit(inspectReplacingAllLeading.getInspectBy(), p);
        visitContainer("", inspectReplacingAllLeading.getPadding().getInspections(), "", "", p);
        return inspectReplacingAllLeading;
    }

    public Cobol visitInspectReplacingAllLeadings(Cobol.InspectReplacingAllLeadings inspectReplacingAllLeadings, PrintOutputCapture<P> p) {
        visitSpace(inspectReplacingAllLeadings.getPrefix(), p);
        visitMarkers(inspectReplacingAllLeadings.getMarkers(), p);
        visit(inspectReplacingAllLeadings.getWord(), p);
        visitContainer("", inspectReplacingAllLeadings.getPadding().getInspections(), "", "", p);
        return inspectReplacingAllLeadings;
    }

    public Cobol visitInspectReplacingCharacters(Cobol.InspectReplacingCharacters inspectReplacingCharacters, PrintOutputCapture<P> p) {
        visitSpace(inspectReplacingCharacters.getPrefix(), p);
        visitMarkers(inspectReplacingCharacters.getMarkers(), p);
        visit(inspectReplacingCharacters.getWord(), p);
        visit(inspectReplacingCharacters.getInspectBy(), p);
        visitContainer("", inspectReplacingCharacters.getPadding().getInspections(), "", "", p);
        return inspectReplacingCharacters;
    }

    public Cobol visitInspectReplacingPhrase(Cobol.InspectReplacingPhrase inspectReplacingPhrase, PrintOutputCapture<P> p) {
        visitSpace(inspectReplacingPhrase.getPrefix(), p);
        visitMarkers(inspectReplacingPhrase.getMarkers(), p);
        visit(inspectReplacingPhrase.getWord(), p);
        visitContainer("", inspectReplacingPhrase.getPadding().getInspections(), "", "", p);
        return inspectReplacingPhrase;
    }

    public Cobol visitInspectTallyingPhrase(Cobol.InspectTallyingPhrase inspectTallyingPhrase, PrintOutputCapture<P> p) {
        visitSpace(inspectTallyingPhrase.getPrefix(), p);
        visitMarkers(inspectTallyingPhrase.getMarkers(), p);
        visit(inspectTallyingPhrase.getTallying(), p);
        visitContainer("", inspectTallyingPhrase.getPadding().getInspectFors(), "", "", p);
        return inspectTallyingPhrase;
    }

    public Cobol visitInspectTallyingReplacingPhrase(Cobol.InspectTallyingReplacingPhrase inspectTallyingReplacingPhrase, PrintOutputCapture<P> p) {
        visitSpace(inspectTallyingReplacingPhrase.getPrefix(), p);
        visitMarkers(inspectTallyingReplacingPhrase.getMarkers(), p);
        visit(inspectTallyingReplacingPhrase.getTallying(), p);
        visitContainer("", inspectTallyingReplacingPhrase.getPadding().getInspectFors(), "", "", p);
        visitContainer("", inspectTallyingReplacingPhrase.getPadding().getReplacingPhrases(), "", "", p);
        return inspectTallyingReplacingPhrase;
    }

    public Cobol visitInspectTo(Cobol.InspectTo inspectTo, PrintOutputCapture<P> p) {
        visitSpace(inspectTo.getPrefix(), p);
        visitMarkers(inspectTo.getMarkers(), p);
        visit(inspectTo.getTo(), p);
        visit(inspectTo.getIdentifier(), p);
        return inspectTo;
    }

    public Cobol visitIoControlParagraph(Cobol.IoControlParagraph ioControlParagraph, PrintOutputCapture<P> p) {
        visitSpace(ioControlParagraph.getPrefix(), p);
        visitMarkers(ioControlParagraph.getMarkers(), p);
        visit(ioControlParagraph.getIOControl(), p);
        visit(ioControlParagraph.getDot(), p);
        visit(ioControlParagraph.getFileName(), p);
        visit(ioControlParagraph.getFileNameDot(), p);
        visitContainer("", ioControlParagraph.getPadding().getClauses(), "", ".", p);
        return ioControlParagraph;
    }

    public Cobol visitLabelRecordsClause(Cobol.LabelRecordsClause labelRecordsClause, PrintOutputCapture<P> p) {
        visitSpace(labelRecordsClause.getPrefix(), p);
        visitMarkers(labelRecordsClause.getMarkers(), p);
        visit(labelRecordsClause.getWords(), p);
        visitContainer("", labelRecordsClause.getPadding().getDataNames(), "", "", p);
        return labelRecordsClause;
    }

    public Cobol visitLibraryAttributeClauseFormat1(Cobol.LibraryAttributeClauseFormat1 libraryAttributeClauseFormat1, PrintOutputCapture<P> p) {
        visitSpace(libraryAttributeClauseFormat1.getPrefix(), p);
        visitMarkers(libraryAttributeClauseFormat1.getMarkers(), p);
        visit(libraryAttributeClauseFormat1.getWords(), p);
        return libraryAttributeClauseFormat1;
    }

    public Cobol visitLibraryAttributeClauseFormat2(Cobol.LibraryAttributeClauseFormat2 libraryAttributeClauseFormat2, PrintOutputCapture<P> p) {
        visitSpace(libraryAttributeClauseFormat2.getPrefix(), p);
        visitMarkers(libraryAttributeClauseFormat2.getMarkers(), p);
        visit(libraryAttributeClauseFormat2.getAttribute(), p);
        visit(libraryAttributeClauseFormat2.getLibraryAttributeFunction(), p);
        visit(libraryAttributeClauseFormat2.getWords(), p);
        visit(libraryAttributeClauseFormat2.getLibraryAttributeParameter(), p);
        visit(libraryAttributeClauseFormat2.getLibraryAttributeTitle(), p);
        return libraryAttributeClauseFormat2;
    }

    public Cobol visitLibraryAttributeFunction(Cobol.LibraryAttributeFunction libraryAttributeFunction, PrintOutputCapture<P> p) {
        visitSpace(libraryAttributeFunction.getPrefix(), p);
        visitMarkers(libraryAttributeFunction.getMarkers(), p);
        visit(libraryAttributeFunction.getWords(), p);
        visit(libraryAttributeFunction.getLiteral(), p);
        return libraryAttributeFunction;
    }

    public Cobol visitLibraryAttributeParameter(Cobol.LibraryAttributeParameter libraryAttributeParameter, PrintOutputCapture<P> p) {
        visitSpace(libraryAttributeParameter.getPrefix(), p);
        visitMarkers(libraryAttributeParameter.getMarkers(), p);
        visit(libraryAttributeParameter.getWords(), p);
        visit(libraryAttributeParameter.getLiteral(), p);
        return libraryAttributeParameter;
    }

    public Cobol visitLibraryAttributeTitle(Cobol.LibraryAttributeTitle libraryAttributeTitle, PrintOutputCapture<P> p) {
        visitSpace(libraryAttributeTitle.getPrefix(), p);
        visitMarkers(libraryAttributeTitle.getMarkers(), p);
        visit(libraryAttributeTitle.getWords(), p);
        visit(libraryAttributeTitle.getLiteral(), p);
        return libraryAttributeTitle;
    }

    public Cobol visitLibraryDescriptionEntryFormat1(Cobol.LibraryDescriptionEntryFormat1 libraryDescriptionEntryFormat1, PrintOutputCapture<P> p) {
        visitSpace(libraryDescriptionEntryFormat1.getPrefix(), p);
        visitMarkers(libraryDescriptionEntryFormat1.getMarkers(), p);
        visit(libraryDescriptionEntryFormat1.getLd(), p);
        visit(libraryDescriptionEntryFormat1.getLibraryName(), p);
        visit(libraryDescriptionEntryFormat1.getExport(), p);
        visit(libraryDescriptionEntryFormat1.getLibraryAttributeClauseFormat1(), p);
        visit(libraryDescriptionEntryFormat1.getLibraryEntryProcedureClauseFormat1(), p);
        return libraryDescriptionEntryFormat1;
    }

    public Cobol visitLibraryDescriptionEntryFormat2(Cobol.LibraryDescriptionEntryFormat2 libraryDescriptionEntryFormat2, PrintOutputCapture<P> p) {
        visitSpace(libraryDescriptionEntryFormat2.getPrefix(), p);
        visitMarkers(libraryDescriptionEntryFormat2.getMarkers(), p);
        visit(libraryDescriptionEntryFormat2.getLb(), p);
        visit(libraryDescriptionEntryFormat2.getLibraryName(), p);
        visit(libraryDescriptionEntryFormat2.getExport(), p);
        visit(libraryDescriptionEntryFormat2.getLibraryIsGlobalClause(), p);
        visit(libraryDescriptionEntryFormat2.getLibraryIsCommonClause(), p);
        visitContainer("", libraryDescriptionEntryFormat2.getPadding().getClauseFormats(), "", "", p);
        return libraryDescriptionEntryFormat2;
    }

    public Cobol visitLibraryEntryProcedureClauseFormat1(Cobol.LibraryEntryProcedureClauseFormat1 libraryEntryProcedureClauseFormat1, PrintOutputCapture<P> p) {
        visitSpace(libraryEntryProcedureClauseFormat1.getPrefix(), p);
        visitMarkers(libraryEntryProcedureClauseFormat1.getMarkers(), p);
        visit(libraryEntryProcedureClauseFormat1.getEntryProcedure(), p);
        visit(libraryEntryProcedureClauseFormat1.getProgramName(), p);
        visit(libraryEntryProcedureClauseFormat1.getLibraryEntryProcedureForClause(), p);
        return libraryEntryProcedureClauseFormat1;
    }

    public Cobol visitLibraryEntryProcedureClauseFormat2(Cobol.LibraryEntryProcedureClauseFormat2 libraryEntryProcedureClauseFormat2, PrintOutputCapture<P> p) {
        visitSpace(libraryEntryProcedureClauseFormat2.getPrefix(), p);
        visitMarkers(libraryEntryProcedureClauseFormat2.getMarkers(), p);
        visit(libraryEntryProcedureClauseFormat2.getEntryProcedure(), p);
        visit(libraryEntryProcedureClauseFormat2.getProgramName(), p);
        visit(libraryEntryProcedureClauseFormat2.getLibraryEntryProcedureForClause(), p);
        visit(libraryEntryProcedureClauseFormat2.getLibraryEntryProcedureWithClause(), p);
        visit(libraryEntryProcedureClauseFormat2.getLibraryEntryProcedureUsingClause(), p);
        visit(libraryEntryProcedureClauseFormat2.getLibraryEntryProcedureGivingClause(), p);
        return libraryEntryProcedureClauseFormat2;
    }

    public Cobol visitLibraryEntryProcedureForClause(Cobol.LibraryEntryProcedureForClause libraryEntryProcedureForClause, PrintOutputCapture<P> p) {
        visitSpace(libraryEntryProcedureForClause.getPrefix(), p);
        visitMarkers(libraryEntryProcedureForClause.getMarkers(), p);
        visit(libraryEntryProcedureForClause.getWord(), p);
        visit(libraryEntryProcedureForClause.getLiteral(), p);
        return libraryEntryProcedureForClause;
    }

    public Cobol visitLibraryEntryProcedureGivingClause(Cobol.LibraryEntryProcedureGivingClause libraryEntryProcedureGivingClause, PrintOutputCapture<P> p) {
        visitSpace(libraryEntryProcedureGivingClause.getPrefix(), p);
        visitMarkers(libraryEntryProcedureGivingClause.getMarkers(), p);
        visit(libraryEntryProcedureGivingClause.getGiving(), p);
        visit(libraryEntryProcedureGivingClause.getDataName(), p);
        return libraryEntryProcedureGivingClause;
    }

    public Cobol visitLibraryEntryProcedureUsingClause(Cobol.LibraryEntryProcedureUsingClause libraryEntryProcedureUsingClause, PrintOutputCapture<P> p) {
        visitSpace(libraryEntryProcedureUsingClause.getPrefix(), p);
        visitMarkers(libraryEntryProcedureUsingClause.getMarkers(), p);
        visit(libraryEntryProcedureUsingClause.getUsing(), p);
        visitContainer("", libraryEntryProcedureUsingClause.getPadding().getNames(), "", "", p);
        return libraryEntryProcedureUsingClause;
    }

    public Cobol visitLibraryEntryProcedureWithClause(Cobol.LibraryEntryProcedureWithClause libraryEntryProcedureWithClause, PrintOutputCapture<P> p) {
        visitSpace(libraryEntryProcedureWithClause.getPrefix(), p);
        visitMarkers(libraryEntryProcedureWithClause.getMarkers(), p);
        visit(libraryEntryProcedureWithClause.getWith(), p);
        visitContainer("", libraryEntryProcedureWithClause.getPadding().getNames(), "", "", p);
        return libraryEntryProcedureWithClause;
    }

    public Cobol visitLibraryIsCommonClause(Cobol.LibraryIsCommonClause libraryIsCommonClause, PrintOutputCapture<P> p) {
        visitSpace(libraryIsCommonClause.getPrefix(), p);
        visitMarkers(libraryIsCommonClause.getMarkers(), p);
        visit(libraryIsCommonClause.getWords(), p);
        return libraryIsCommonClause;
    }

    public Cobol visitLibraryIsGlobalClause(Cobol.LibraryIsGlobalClause libraryIsGlobalClause, PrintOutputCapture<P> p) {
        visitSpace(libraryIsGlobalClause.getPrefix(), p);
        visitMarkers(libraryIsGlobalClause.getMarkers(), p);
        visit(libraryIsGlobalClause.getWords(), p);
        return libraryIsGlobalClause;
    }

    public Cobol visitLinageClause(Cobol.LinageClause linageClause, PrintOutputCapture<P> p) {
        visitSpace(linageClause.getPrefix(), p);
        visitMarkers(linageClause.getMarkers(), p);
        visit(linageClause.getWords(), p);
        visit(linageClause.getName(), p);
        visit(linageClause.getLines(), p);
        visitContainer("", linageClause.getPadding().getLinageAt(), "", "", p);
        return linageClause;
    }

    public Cobol visitLinageFootingAt(Cobol.LinageFootingAt linageFootingAt, PrintOutputCapture<P> p) {
        visitSpace(linageFootingAt.getPrefix(), p);
        visitMarkers(linageFootingAt.getMarkers(), p);
        visit(linageFootingAt.getWords(), p);
        visit(linageFootingAt.getName(), p);
        return linageFootingAt;
    }

    public Cobol visitLinageLinesAtBottom(Cobol.LinageLinesAtBottom linageLinesAtBottom, PrintOutputCapture<P> p) {
        visitSpace(linageLinesAtBottom.getPrefix(), p);
        visitMarkers(linageLinesAtBottom.getMarkers(), p);
        visit(linageLinesAtBottom.getWords(), p);
        visit(linageLinesAtBottom.getName(), p);
        return linageLinesAtBottom;
    }

    public Cobol visitLinageLinesAtTop(Cobol.LinageLinesAtTop linageLinesAtTop, PrintOutputCapture<P> p) {
        visitSpace(linageLinesAtTop.getPrefix(), p);
        visitMarkers(linageLinesAtTop.getMarkers(), p);
        visit(linageLinesAtTop.getWords(), p);
        visit(linageLinesAtTop.getName(), p);
        return linageLinesAtTop;
    }

    public Cobol visitLinkageSection(Cobol.LinkageSection linkageSection, PrintOutputCapture<P> p) {
        visitSpace(linkageSection.getPrefix(), p);
        visitMarkers(linkageSection.getMarkers(), p);
        visit(linkageSection.getWords(), p);
        visitContainer(".", linkageSection.getPadding().getDataDescriptions(), "", "", p);
        return linkageSection;
    }

    public Cobol visitLocalStorageSection(Cobol.LocalStorageSection localStorageSection, PrintOutputCapture<P> p) {
        visitSpace(localStorageSection.getPrefix(), p);
        visitMarkers(localStorageSection.getMarkers(), p);
        visit(localStorageSection.getWords(), p);

        if (localStorageSection.getLocalName() != null) {
            visit(localStorageSection.getLocalData(), p);
            visit(localStorageSection.getLocalName(), p);
        }
        visitContainer(".", localStorageSection.getPadding().getDataDescriptions(), "", "", p);
        return localStorageSection;
    }

    public Cobol visitMerge(Cobol.Merge merge, PrintOutputCapture<P> p) {
        visitSpace(merge.getPrefix(), p);
        visitMarkers(merge.getMarkers(), p);
        visit(merge.getWords(), p);
        visit(merge.getFileName(), p);
        visitContainer("", merge.getPadding().getMergeOnKeyClause(), "", "", p);
        visit(merge.getMergeCollatingSequencePhrase(), p);
        visitContainer("", merge.getPadding().getMergeUsing(), "", "", p);
        visit(merge.getMergeOutputProcedurePhrase(), p);
        visitContainer("", merge.getPadding().getMergeGivingPhrase(), "", "", p);
        return merge;
    }

    public Cobol visitMergeCollatingSequencePhrase(Cobol.MergeCollatingSequencePhrase mergeCollatingSequencePhrase, PrintOutputCapture<P> p) {
        visitSpace(mergeCollatingSequencePhrase.getPrefix(), p);
        visitMarkers(mergeCollatingSequencePhrase.getMarkers(), p);
        visit(mergeCollatingSequencePhrase.getWords(), p);
        visit(mergeCollatingSequencePhrase.getMergeCollatingAlphanumeric(), p);
        visit(mergeCollatingSequencePhrase.getMergeCollatingNational(), p);
        return mergeCollatingSequencePhrase;
    }

    public Cobol visitMergeGiving(Cobol.MergeGiving mergeGiving, PrintOutputCapture<P> p) {
        visitSpace(mergeGiving.getPrefix(), p);
        visitMarkers(mergeGiving.getMarkers(), p);
        visit(mergeGiving.getWords(), p);
        return mergeGiving;
    }

    public Cobol visitMergeGivingPhrase(Cobol.MergeGivingPhrase mergeGivingPhrase, PrintOutputCapture<P> p) {
        visitSpace(mergeGivingPhrase.getPrefix(), p);
        visitMarkers(mergeGivingPhrase.getMarkers(), p);
        visit(mergeGivingPhrase.getWords(), p);
        visitContainer("", mergeGivingPhrase.getPadding().getMergeGiving(), "", "", p);
        return mergeGivingPhrase;
    }

    public Cobol visitMergeOnKeyClause(Cobol.MergeOnKeyClause mergeOnKeyClause, PrintOutputCapture<P> p) {
        visitSpace(mergeOnKeyClause.getPrefix(), p);
        visitMarkers(mergeOnKeyClause.getMarkers(), p);
        visit(mergeOnKeyClause.getWords(), p);
        visitContainer("", mergeOnKeyClause.getPadding().getQualifiedDataName(), "", "", p);
        return mergeOnKeyClause;
    }

    public Cobol visitMergeOutputProcedurePhrase(Cobol.MergeOutputProcedurePhrase mergeOutputProcedurePhrase, PrintOutputCapture<P> p) {
        visitSpace(mergeOutputProcedurePhrase.getPrefix(), p);
        visitMarkers(mergeOutputProcedurePhrase.getMarkers(), p);
        visit(mergeOutputProcedurePhrase.getWords(), p);
        visit(mergeOutputProcedurePhrase.getProcedureName(), p);
        visit(mergeOutputProcedurePhrase.getMergeOutputThrough(), p);
        return mergeOutputProcedurePhrase;
    }

    @Override
    public @Nullable Cobol visitMergeOutputThrough(Cobol.MergeOutputThrough mergeOutputThrough, PrintOutputCapture<P> p) {
        visitSpace(mergeOutputThrough.getPrefix(), p);
        visitMarkers(mergeOutputThrough.getMarkers(), p);
        visit(mergeOutputThrough.getWords(), p);
        return mergeOutputThrough;
    }

    @Override
    public Cobol visitMergeUsing(Cobol.MergeUsing mergeUsing, PrintOutputCapture<P> p) {
        visitSpace(mergeUsing.getPrefix(), p);
        visitMarkers(mergeUsing.getMarkers(), p);
        visit(mergeUsing.getWords(), p);
        visit(mergeUsing.getFileNames(), p);
        return mergeUsing;
    }

    public Cobol visitMergeable(Cobol.Mergeable mergeable, PrintOutputCapture<P> p) {
        visitSpace(mergeable.getPrefix(), p);
        visitMarkers(mergeable.getMarkers(), p);
        visit(mergeable.getWords(), p);
        return mergeable;
    }

    public Cobol visitMessageCountClause(Cobol.MessageCountClause messageCountClause, PrintOutputCapture<P> p) {
        visitSpace(messageCountClause.getPrefix(), p);
        visitMarkers(messageCountClause.getMarkers(), p);
        visit(messageCountClause.getWords(), p);
        visit(messageCountClause.getDataDescName(), p);
        return messageCountClause;
    }

    public Cobol visitMessageDateClause(Cobol.MessageDateClause messageDateClause, PrintOutputCapture<P> p) {
        visitSpace(messageDateClause.getPrefix(), p);
        visitMarkers(messageDateClause.getMarkers(), p);
        visit(messageDateClause.getWords(), p);
        visit(messageDateClause.getDataDescName(), p);
        return messageDateClause;
    }

    public Cobol visitMessageTimeClause(Cobol.MessageTimeClause messageTimeClause, PrintOutputCapture<P> p) {
        visitSpace(messageTimeClause.getPrefix(), p);
        visitMarkers(messageTimeClause.getMarkers(), p);
        visit(messageTimeClause.getWords(), p);
        visit(messageTimeClause.getDataDescName(), p);
        return messageTimeClause;
    }

    public Cobol visitMoveCorrespondingToStatement(Cobol.MoveCorrespondingToStatement moveCorrespondingToStatement, PrintOutputCapture<P> p) {
        visitSpace(moveCorrespondingToStatement.getPrefix(), p);
        visitMarkers(moveCorrespondingToStatement.getMarkers(), p);
        visit(moveCorrespondingToStatement.getWords(), p);
        visit(moveCorrespondingToStatement.getMoveCorrespondingToSendingArea(), p);
        visitContainer("", moveCorrespondingToStatement.getPadding().getTo(), "", "", p);
        return moveCorrespondingToStatement;
    }

    public Cobol visitMoveStatement(Cobol.MoveStatement moveStatement, PrintOutputCapture<P> p) {
        visitSpace(moveStatement.getPrefix(), p);
        visitMarkers(moveStatement.getMarkers(), p);
        visit(moveStatement.getWords(), p);
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

    public Cobol visitMultDiv(Cobol.MultDiv multDiv, PrintOutputCapture<P> p) {
        visitSpace(multDiv.getPrefix(), p);
        visitMarkers(multDiv.getMarkers(), p);
        visit(multDiv.getWords(), p);
        visit(multDiv.getPowers(), p);
        return multDiv;
    }

    public Cobol visitMultDivs(Cobol.MultDivs multDivs, PrintOutputCapture<P> p) {
        visitSpace(multDivs.getPrefix(), p);
        visitMarkers(multDivs.getMarkers(), p);
        visit(multDivs.getPowers(), p);
        visitContainer("", multDivs.getPadding().getMultDivs(), "", "", p);
        return multDivs;
    }

    public Cobol visitMultipleFileClause(Cobol.MultipleFileClause multipleFileClause, PrintOutputCapture<P> p) {
        visitSpace(multipleFileClause.getPrefix(), p);
        visitMarkers(multipleFileClause.getMarkers(), p);
        visit(multipleFileClause.getWords(), p);
        visitContainer("", multipleFileClause.getPadding().getFilePositions(), "", "", p);
        return multipleFileClause;
    }

    public Cobol visitMultipleFilePosition(Cobol.MultipleFilePosition multipleFilePosition, PrintOutputCapture<P> p) {
        visitSpace(multipleFilePosition.getPrefix(), p);
        visitMarkers(multipleFilePosition.getMarkers(), p);
        visit(multipleFilePosition.getFileName(), p);
        visit(multipleFilePosition.getPosition(), p);
        visit(multipleFilePosition.getIntegerLiteral(), p);
        return multipleFilePosition;
    }

    public Cobol visitMultiply(Cobol.Multiply multiply, PrintOutputCapture<P> p) {
        visitSpace(multiply.getPrefix(), p);
        visitMarkers(multiply.getMarkers(), p);
        visit(multiply.getWords(), p);
        visit(multiply.getMultiplicand(), p);
        visit(multiply.getBy(), p);
        visit(multiply.getMultiply(), p);
        visit(multiply.getOnSizeErrorPhrase(), p);
        visit(multiply.getNotOnSizeErrorPhrase(), p);
        visit(multiply.getEndMultiply(), p);
        return multiply;
    }

    public Cobol visitMultiplyGiving(Cobol.MultiplyGiving multiplyGiving, PrintOutputCapture<P> p) {
        visitSpace(multiplyGiving.getPrefix(), p);
        visitMarkers(multiplyGiving.getMarkers(), p);
        visitContainer("", multiplyGiving.getPadding().getResult(), "", "", p);
        return multiplyGiving;
    }

    public Cobol visitMultiplyRegular(Cobol.MultiplyRegular multiplyRegular, PrintOutputCapture<P> p) {
        visitSpace(multiplyRegular.getPrefix(), p);
        visitMarkers(multiplyRegular.getMarkers(), p);
        visitContainer("", multiplyRegular.getPadding().getOperand(), "", "", p);
        return multiplyRegular;
    }

    public Cobol visitNextSentence(Cobol.NextSentence nextSentence, PrintOutputCapture<P> p) {
        visitSpace(nextSentence.getPrefix(), p);
        visitMarkers(nextSentence.getMarkers(), p);
        visit(nextSentence.getWords(), p);
        return nextSentence;
    }

    public Cobol visitObjectComputer(Cobol.ObjectComputer objectComputer, PrintOutputCapture<P> p) {
        visitSpace(objectComputer.getPrefix(), p);
        visitMarkers(objectComputer.getMarkers(), p);
        visit(objectComputer.getWords(), p);
        visit(objectComputer.getComputer(), p);
        visit(objectComputer.getDot(), p);
        return objectComputer;
    }

    public Cobol visitObjectComputerDefinition(Cobol.ObjectComputerDefinition objectComputerDefinition, PrintOutputCapture<P> p) {
        visitSpace(objectComputerDefinition.getPrefix(), p);
        visitMarkers(objectComputerDefinition.getMarkers(), p);
        visit(objectComputerDefinition.getComputerName(), p);
        visitContainer("", objectComputerDefinition.getPadding().getSpecifications(), "", "", p);
        return objectComputerDefinition;
    }

    public Cobol visitOdtClause(Cobol.OdtClause odtClause, PrintOutputCapture<P> p) {
        visitSpace(odtClause.getPrefix(), p);
        visitMarkers(odtClause.getMarkers(), p);
        visit(odtClause.getWords(), p);
        visit(odtClause.getMnemonicName(), p);
        return odtClause;
    }

    public Cobol visitOpen(Cobol.Open open, PrintOutputCapture<P> p) {
        visitSpace(open.getPrefix(), p);
        visitMarkers(open.getMarkers(), p);
        visit(open.getWords(), p);
        visitContainer("", open.getPadding().getOpen(), "", "", p);
        return open;
    }

    public Cobol visitOpenIOExtendStatement(Cobol.OpenIOExtendStatement openIOExtendStatement, PrintOutputCapture<P> p) {
        visitSpace(openIOExtendStatement.getPrefix(), p);
        visitMarkers(openIOExtendStatement.getMarkers(), p);
        visit(openIOExtendStatement.getWords(), p);
        visitContainer("", openIOExtendStatement.getPadding().getFileNames(), "", "", p);
        return openIOExtendStatement;
    }

    public Cobol visitOpenInputOutputStatement(Cobol.OpenInputOutputStatement openInputOutputStatement, PrintOutputCapture<P> p) {
        visitSpace(openInputOutputStatement.getPrefix(), p);
        visitMarkers(openInputOutputStatement.getMarkers(), p);
        visit(openInputOutputStatement.getWords(), p);
        visitContainer("", openInputOutputStatement.getPadding().getOpenInput(), "", "", p);
        return openInputOutputStatement;
    }

    public Cobol visitOpenable(Cobol.Openable openable, PrintOutputCapture<P> p) {
        visitSpace(openable.getPrefix(), p);
        visitMarkers(openable.getMarkers(), p);
        visit(openable.getFileName(), p);
        visit(openable.getWords(), p);
        return openable;
    }

    public Cobol visitOrganizationClause(Cobol.OrganizationClause organizationClause, PrintOutputCapture<P> p) {
        visitSpace(organizationClause.getPrefix(), p);
        visitMarkers(organizationClause.getMarkers(), p);
        visit(organizationClause.getWords(), p);
        return organizationClause;
    }

    public Cobol visitPaddingCharacterClause(Cobol.PaddingCharacterClause paddingCharacterClause, PrintOutputCapture<P> p) {
        visitSpace(paddingCharacterClause.getPrefix(), p);
        visitMarkers(paddingCharacterClause.getMarkers(), p);
        visit(paddingCharacterClause.getWords(), p);
        visit(paddingCharacterClause.getName(), p);
        return paddingCharacterClause;
    }

    public Cobol visitParagraph(Cobol.Paragraph paragraph, PrintOutputCapture<P> p) {
        visitSpace(paragraph.getPrefix(), p);
        visitMarkers(paragraph.getMarkers(), p);
        visit(paragraph.getParagraphName(), p);
        visit(paragraph.getDot(), p);
        visit(paragraph.getAlteredGoTo(), p);
        visitContainer("", paragraph.getPadding().getSentences(), "", "", p);
        return paragraph;
    }

    public Cobol visitParagraphs(Cobol.Paragraphs paragraphs, PrintOutputCapture<P> p) {
        visitSpace(paragraphs.getPrefix(), p);
        visitMarkers(paragraphs.getMarkers(), p);
        visitContainer("", paragraphs.getPadding().getSentences(), "", "", p);
        visitContainer("", paragraphs.getPadding().getParagraphs(), "", "", p);
        return paragraphs;
    }

    public Cobol visitParenthesized(Cobol.Parenthesized parenthesized, PrintOutputCapture<P> p) {
        visitSpace(parenthesized.getPrefix(), p);
        visitMarkers(parenthesized.getMarkers(), p);
        visit(parenthesized.getLeftParen(), p);
        for(Cobol c : parenthesized.getContents()) {
            visit(c, p);
        }
        visit(parenthesized.getRightParen(), p);
        return parenthesized;
    }

    public Cobol visitPasswordClause(Cobol.PasswordClause passwordClause, PrintOutputCapture<P> p) {
        visitSpace(passwordClause.getPrefix(), p);
        visitMarkers(passwordClause.getMarkers(), p);
        visit(passwordClause.getWords(), p);
        visit(passwordClause.getDataName(), p);
        return passwordClause;
    }

    public Cobol visitPerform(Cobol.Perform perform, PrintOutputCapture<P> p) {
        visitSpace(perform.getPrefix(), p);
        visitMarkers(perform.getMarkers(), p);
        visit(perform.getWords(), p);
        visit(perform.getStatement(), p);
        return perform;
    }

    public Cobol visitPerformFrom(Cobol.PerformFrom performFrom, PrintOutputCapture<P> p) {
        visitSpace(performFrom.getPrefix(), p);
        visitMarkers(performFrom.getMarkers(), p);
        visit(performFrom.getWords(), p);
        visit(performFrom.getFrom(), p);
        return performFrom;
    }

    public Cobol visitPerformInlineStatement(Cobol.PerformInlineStatement performInlineStatement, PrintOutputCapture<P> p) {
        visitSpace(performInlineStatement.getPrefix(), p);
        visitMarkers(performInlineStatement.getMarkers(), p);
        visit(performInlineStatement.getPerformType(), p);
        visitContainer("", performInlineStatement.getPadding().getStatements(), "", "", p);
        visit(performInlineStatement.getWords(), p);
        return performInlineStatement;
    }

    public Cobol visitPerformProcedureStatement(Cobol.PerformProcedureStatement performProcedureStatement, PrintOutputCapture<P> p) {
        visitSpace(performProcedureStatement.getPrefix(), p);
        visitMarkers(performProcedureStatement.getMarkers(), p);
        visit(performProcedureStatement.getProcedureName(), p);
        visit(performProcedureStatement.getWords(), p);
        visit(performProcedureStatement.getThroughProcedure(), p);
        visit(performProcedureStatement.getPerformType(), p);
        return performProcedureStatement;
    }

    public Cobol visitPerformTestClause(Cobol.PerformTestClause performTestClause, PrintOutputCapture<P> p) {
        visitSpace(performTestClause.getPrefix(), p);
        visitMarkers(performTestClause.getMarkers(), p);
        visit(performTestClause.getWords(), p);
        return performTestClause;
    }

    public Cobol visitPerformTimes(Cobol.PerformTimes performTimes, PrintOutputCapture<P> p) {
        visitSpace(performTimes.getPrefix(), p);
        visitMarkers(performTimes.getMarkers(), p);
        visit(performTimes.getValue(), p);
        visit(performTimes.getWords(), p);
        return performTimes;
    }

    public Cobol visitPerformUntil(Cobol.PerformUntil performUntil, PrintOutputCapture<P> p) {
        visitSpace(performUntil.getPrefix(), p);
        visitMarkers(performUntil.getMarkers(), p);
        visit(performUntil.getPerformTestClause(), p);
        visit(performUntil.getWords(), p);
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
        visit(performVaryingClause.getWords(), p);
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
        visit(performable.getWords(), p);
        visit(performable.getExpression(), p);
        return performable;
    }

    public Cobol visitPicture(Cobol.Picture picture, PrintOutputCapture<P> p) {
        visitSpace(picture.getPrefix(), p);
        visitMarkers(picture.getMarkers(), p);
        visitContainer("", picture.getPadding().getWords(), "", "", p);
        visit(picture.getParenthesized(), p);
        return picture;
    }

    public Cobol visitPictureString(Cobol.PictureString pictureString, PrintOutputCapture<P> p) {
        visitSpace(pictureString.getPrefix(), p);
        visitMarkers(pictureString.getMarkers(), p);
        visitContainer("", pictureString.getPadding().getPictures(), "", "", p);
        return pictureString;
    }

    public Cobol visitPlusMinus(Cobol.PlusMinus plusMinus, PrintOutputCapture<P> p) {
        visitSpace(plusMinus.getPrefix(), p);
        visitMarkers(plusMinus.getMarkers(), p);
        visit(plusMinus.getWords(), p);
        visit(plusMinus.getMultDivs(), p);
        return plusMinus;
    }

    public Cobol visitPower(Cobol.Power power, PrintOutputCapture<P> p) {
        visitSpace(power.getPrefix(), p);
        visitMarkers(power.getMarkers(), p);
        visit(power.getPower(), p);
        visit(power.getExpression(), p);
        return power;
    }

    public Cobol visitPowers(Cobol.Powers powers, PrintOutputCapture<P> p) {
        visitSpace(powers.getPrefix(), p);
        visitMarkers(powers.getMarkers(), p);
        visit(powers.getPlusMinusChar(), p);
        visit(powers.getExpression(), p);
        visitContainer("", powers.getPadding().getPowers(), "", "", p);
        return powers;
    }

    public Cobol visitProcedureDeclarative(Cobol.ProcedureDeclarative procedureDeclarative, PrintOutputCapture<P> p) {
        visitSpace(procedureDeclarative.getPrefix(), p);
        visitMarkers(procedureDeclarative.getMarkers(), p);
        visit(procedureDeclarative.getProcedureSectionHeader(), p);
        visitLeftPadded(".", procedureDeclarative.getPadding().getUseStatement(), p);
        visitLeftPadded(".", procedureDeclarative.getPadding().getParagraphs(), p);
        return procedureDeclarative;
    }

    public Cobol visitProcedureDeclaratives(Cobol.ProcedureDeclaratives procedureDeclaratives, PrintOutputCapture<P> p) {
        visitSpace(procedureDeclaratives.getPrefix(), p);
        visitMarkers(procedureDeclaratives.getMarkers(), p);
        visit(procedureDeclaratives.getDeclaratives(), p);
        visitContainer(".", procedureDeclaratives.getPadding().getProcedureDeclarative(), "", "", p);
        visit(procedureDeclaratives.getEndDeclaratives(), p);
        visit(procedureDeclaratives.getDot(), p);
        return procedureDeclaratives;
    }

    public Cobol visitProcedureDivision(Cobol.ProcedureDivision procedureDivision, PrintOutputCapture<P> p) {
        visitSpace(procedureDivision.getPrefix(), p);
        visitMarkers(procedureDivision.getMarkers(), p);
        visit(procedureDivision.getWords(), p);
        visit(procedureDivision.getProcedureDivisionUsingClause(), p);
        visit(procedureDivision.getProcedureDivisionGivingClause(), p);
        visit(procedureDivision.getDot(), p);
        visit(procedureDivision.getProcedureDeclaratives(), p);
        visitLeftPadded("", procedureDivision.getPadding().getBody(), p);
        return procedureDivision;
    }

    public Cobol visitProcedureDivisionBody(Cobol.ProcedureDivisionBody procedureDivisionBody, PrintOutputCapture<P> p) {
        visitSpace(procedureDivisionBody.getPrefix(), p);
        visitMarkers(procedureDivisionBody.getMarkers(), p);
        visit(procedureDivisionBody.getParagraphs(), p);
        visitContainer("", procedureDivisionBody.getPadding().getProcedureSection(), "", "", p);
        return procedureDivisionBody;
    }

    public Cobol visitProcedureDivisionByReference(Cobol.ProcedureDivisionByReference procedureDivisionByReference, PrintOutputCapture<P> p) {
        visitSpace(procedureDivisionByReference.getPrefix(), p);
        visitMarkers(procedureDivisionByReference.getMarkers(), p);
        visit(procedureDivisionByReference.getWords(), p);
        visit(procedureDivisionByReference.getReference(), p);
        return procedureDivisionByReference;
    }

    public Cobol visitProcedureDivisionByReferencePhrase(Cobol.ProcedureDivisionByReferencePhrase procedureDivisionByReferencePhrase, PrintOutputCapture<P> p) {
        visitSpace(procedureDivisionByReferencePhrase.getPrefix(), p);
        visitMarkers(procedureDivisionByReferencePhrase.getMarkers(), p);
        visit(procedureDivisionByReferencePhrase.getWords(), p);
        for (Cobol.ProcedureDivisionByReference pp : procedureDivisionByReferencePhrase.getProcedureDivisionByReference()) {
            visit(pp, p);
        }
        return procedureDivisionByReferencePhrase;
    }

    public Cobol visitProcedureDivisionByValuePhrase(Cobol.ProcedureDivisionByValuePhrase procedureDivisionByValuePhrase, PrintOutputCapture<P> p) {
        visitSpace(procedureDivisionByValuePhrase.getPrefix(), p);
        visitMarkers(procedureDivisionByValuePhrase.getMarkers(), p);
        visit(procedureDivisionByValuePhrase.getWords(), p);
        for (Name pp : procedureDivisionByValuePhrase.getPhrases()) {
            visit(pp, p);
        }
        return procedureDivisionByValuePhrase;
    }

    public Cobol visitProcedureDivisionGivingClause(Cobol.ProcedureDivisionGivingClause procedureDivisionGivingClause, PrintOutputCapture<P> p) {
        visitSpace(procedureDivisionGivingClause.getPrefix(), p);
        visitMarkers(procedureDivisionGivingClause.getMarkers(), p);
        visit(procedureDivisionGivingClause.getWords(), p);
        visit(procedureDivisionGivingClause.getDataName(), p);
        return procedureDivisionGivingClause;
    }

    @SuppressWarnings("NullableProblems")
    @Nullable
    public Cobol visitProcedureDivisionUsingClause(@Nullable Cobol.ProcedureDivisionUsingClause procedureDivisionUsingClause, PrintOutputCapture<P> p) {
        if(procedureDivisionUsingClause == null) {
            return null;
        }
        visitSpace(procedureDivisionUsingClause.getPrefix(), p);
        visitMarkers(procedureDivisionUsingClause.getMarkers(), p);
        visit(procedureDivisionUsingClause.getWords(), p);
        for (Cobol pp : procedureDivisionUsingClause.getProcedureDivisionUsingParameter()) {
            visit(pp, p);
        }
        return procedureDivisionUsingClause;
    }

    public Cobol visitProcedureName(Cobol.ProcedureName procedureName, PrintOutputCapture<P> p) {
        visitSpace(procedureName.getPrefix(), p);
        visitMarkers(procedureName.getMarkers(), p);
        visit(procedureName.getParagraphName(), p);
        visit(procedureName.getInSection(), p);
        visit(procedureName.getSectionName(), p);
        return procedureName;
    }

    public Cobol visitProcedureSection(Cobol.ProcedureSection procedureSection, PrintOutputCapture<P> p) {
        visitSpace(procedureSection.getPrefix(), p);
        visitMarkers(procedureSection.getMarkers(), p);
        visit(procedureSection.getProcedureSectionHeader(), p);
        visit(procedureSection.getDot(), p);
        visit(procedureSection.getParagraphs(), p);
        return procedureSection;
    }

    public Cobol visitProcedureSectionHeader(Cobol.ProcedureSectionHeader procedureSectionHeader, PrintOutputCapture<P> p) {
        visitSpace(procedureSectionHeader.getPrefix(), p);
        visitMarkers(procedureSectionHeader.getMarkers(), p);
        visit(procedureSectionHeader.getSectionName(), p);
        visit(procedureSectionHeader.getSection(), p);
        visit(procedureSectionHeader.getIdentifier(), p);
        return procedureSectionHeader;
    }

    public Cobol visitProgramIdParagraph(Cobol.ProgramIdParagraph programIdParagraph, PrintOutputCapture<P> p) {
        visitSpace(programIdParagraph.getPrefix(), p);
        visitMarkers(programIdParagraph.getMarkers(), p);
        visit(programIdParagraph.getProgramId(), p);
        visitLeftPadded(".", programIdParagraph.getPadding().getProgramName(), p);
        visit(programIdParagraph.getProgramAttributes(), p);
        visit(programIdParagraph.getDot(), p);
        return programIdParagraph;
    }

    public Cobol visitProgramLibrarySection(Cobol.ProgramLibrarySection programLibrarySection, PrintOutputCapture<P> p) {
        visitSpace(programLibrarySection.getPrefix(), p);
        visitMarkers(programLibrarySection.getMarkers(), p);
        visit(programLibrarySection.getWords(), p);
        visitContainer("", programLibrarySection.getPadding().getLibraryDescriptionEntries(), "", "", p);
        return programLibrarySection;
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

    public Cobol visitPurge(Cobol.Purge purge, PrintOutputCapture<P> p) {
        visitSpace(purge.getPrefix(), p);
        visitMarkers(purge.getMarkers(), p);
        visitContainer("", purge.getPadding().getNames(), "", "", p);
        return purge;
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
        visit(qualifiedDataNameFormat4.getLinageCounter(), p);
        visit(qualifiedDataNameFormat4.getInFile(), p);
        return qualifiedDataNameFormat4;
    }

    public Cobol visitQualifiedInData(Cobol.QualifiedInData qualifiedInData, PrintOutputCapture<P> p) {
        visitSpace(qualifiedInData.getPrefix(), p);
        visitMarkers(qualifiedInData.getMarkers(), p);
        visit(qualifiedInData.getIn(), p);
        return qualifiedInData;
    }

    public Cobol visitRead(Cobol.Read read, PrintOutputCapture<P> p) {
        visitSpace(read.getPrefix(), p);
        visitMarkers(read.getMarkers(), p);
        visit(read.getWords(), p);
        visit(read.getFileName(), p);
        visit(read.getNextRecord(), p);
        visit(read.getReadInto(), p);
        visit(read.getReadWith(), p);
        visit(read.getReadKey(), p);
        visit(read.getInvalidKeyPhrase(), p);
        visit(read.getNotInvalidKeyPhrase(), p);
        visit(read.getAtEndPhrase(), p);
        visit(read.getNotAtEndPhrase(), p);
        visit(read.getEndRead(), p);
        return read;
    }

    public Cobol visitReadInto(Cobol.ReadInto readInto, PrintOutputCapture<P> p) {
        visitSpace(readInto.getPrefix(), p);
        visitMarkers(readInto.getMarkers(), p);
        visit(readInto.getWords(), p);
        visit(readInto.getIdentifier(), p);
        return readInto;
    }

    public Cobol visitReadKey(Cobol.ReadKey readKey, PrintOutputCapture<P> p) {
        visitSpace(readKey.getPrefix(), p);
        visitMarkers(readKey.getMarkers(), p);
        visit(readKey.getWords(), p);
        visit(readKey.getQualifiedDataName(), p);
        return readKey;
    }

    public Cobol visitReadWith(Cobol.ReadWith readWith, PrintOutputCapture<P> p) {
        visitSpace(readWith.getPrefix(), p);
        visitMarkers(readWith.getMarkers(), p);
        visit(readWith.getWords(), p);
        return readWith;
    }

    public Cobol visitReceivable(Cobol.Receivable receivable, PrintOutputCapture<P> p) {
        visitSpace(receivable.getPrefix(), p);
        visitMarkers(receivable.getMarkers(), p);
        visit(receivable.getWords(), p);
        return receivable;
    }

    public Cobol visitReceive(Cobol.Receive receive, PrintOutputCapture<P> p) {
        visitSpace(receive.getPrefix(), p);
        visitMarkers(receive.getMarkers(), p);
        visit(receive.getReceive(), p);
        visit(receive.getFromOrInto(), p);
        visit(receive.getOnExceptionClause(), p);
        visit(receive.getNotOnExceptionClause(), p);
        visit(receive.getEndReceive(), p);
        return receive;
    }

    public Cobol visitReceiveFrom(Cobol.ReceiveFrom receiveFrom, PrintOutputCapture<P> p) {
        visitSpace(receiveFrom.getPrefix(), p);
        visitMarkers(receiveFrom.getMarkers(), p);
        visit(receiveFrom.getWords(), p);
        visit(receiveFrom.getDataName(), p);
        return receiveFrom;
    }

    public Cobol visitReceiveFromStatement(Cobol.ReceiveFromStatement receiveFromStatement, PrintOutputCapture<P> p) {
        visitSpace(receiveFromStatement.getPrefix(), p);
        visitMarkers(receiveFromStatement.getMarkers(), p);
        visit(receiveFromStatement.getDataName(), p);
        visit(receiveFromStatement.getFrom(), p);
        visit(receiveFromStatement.getReceiveFrom(), p);
        visitContainer("", receiveFromStatement.getPadding().getBeforeWithThreadSizeStatus(), "", "", p);
        return receiveFromStatement;
    }

    public Cobol visitReceiveIntoStatement(Cobol.ReceiveIntoStatement receiveIntoStatement, PrintOutputCapture<P> p) {
        visitSpace(receiveIntoStatement.getPrefix(), p);
        visitMarkers(receiveIntoStatement.getMarkers(), p);
        visit(receiveIntoStatement.getCdName(), p);
        visit(receiveIntoStatement.getWords(), p);
        visit(receiveIntoStatement.getIdentifier(), p);
        visit(receiveIntoStatement.getReceiveNoData(), p);
        visit(receiveIntoStatement.getReceiveWithData(), p);
        return receiveIntoStatement;
    }

    public Cobol visitRecordContainsClause(Cobol.RecordContainsClause recordContainsClause, PrintOutputCapture<P> p) {
        visitSpace(recordContainsClause.getPrefix(), p);
        visitMarkers(recordContainsClause.getMarkers(), p);
        visit(recordContainsClause.getRecord(), p);
        visit(recordContainsClause.getClause(), p);
        return recordContainsClause;
    }

    public Cobol visitRecordContainsClauseFormat1(Cobol.RecordContainsClauseFormat1 recordContainsClauseFormat1, PrintOutputCapture<P> p) {
        visitSpace(recordContainsClauseFormat1.getPrefix(), p);
        visitMarkers(recordContainsClauseFormat1.getMarkers(), p);
        visit(recordContainsClauseFormat1.getContains(), p);
        visit(recordContainsClauseFormat1.getIntegerLiteral(), p);
        visit(recordContainsClauseFormat1.getCharacters(), p);
        return recordContainsClauseFormat1;
    }

    public Cobol visitRecordContainsClauseFormat2(Cobol.RecordContainsClauseFormat2 recordContainsClauseFormat2, PrintOutputCapture<P> p) {
        visitSpace(recordContainsClauseFormat2.getPrefix(), p);
        visitMarkers(recordContainsClauseFormat2.getMarkers(), p);
        visit(recordContainsClauseFormat2.getWords(), p);
        for (Cobol c : recordContainsClauseFormat2.getFromClause()) {
            visit(c, p);
        }
        for (Cobol c : recordContainsClauseFormat2.getQualifiedDataName()) {
            visit(c, p);
        }
        return recordContainsClauseFormat2;
    }

    public Cobol visitRecordContainsClauseFormat3(Cobol.RecordContainsClauseFormat3 recordContainsClauseFormat3, PrintOutputCapture<P> p) {
        visitSpace(recordContainsClauseFormat3.getPrefix(), p);
        visitMarkers(recordContainsClauseFormat3.getMarkers(), p);
        visit(recordContainsClauseFormat3.getContains(), p);
        visit(recordContainsClauseFormat3.getIntegerLiteral(), p);
        visit(recordContainsClauseFormat3.getRecordContainsTo(), p);
        visit(recordContainsClauseFormat3.getCharacters(), p);
        return recordContainsClauseFormat3;
    }

    public Cobol visitRecordContainsTo(Cobol.RecordContainsTo recordContainsTo, PrintOutputCapture<P> p) {
        visitSpace(recordContainsTo.getPrefix(), p);
        visitMarkers(recordContainsTo.getMarkers(), p);
        visit(recordContainsTo.getTo(), p);
        visit(recordContainsTo.getIntegerLiteral(), p);
        return recordContainsTo;
    }

    public Cobol visitRecordDelimiterClause(Cobol.RecordDelimiterClause recordDelimiterClause, PrintOutputCapture<P> p) {
        visitSpace(recordDelimiterClause.getPrefix(), p);
        visitMarkers(recordDelimiterClause.getMarkers(), p);
        visit(recordDelimiterClause.getWords(), p);
        visit(recordDelimiterClause.getName(), p);
        return recordDelimiterClause;
    }

    public Cobol visitRecordKeyClause(Cobol.RecordKeyClause recordKeyClause, PrintOutputCapture<P> p) {
        visitSpace(recordKeyClause.getPrefix(), p);
        visitMarkers(recordKeyClause.getMarkers(), p);
        visit(recordKeyClause.getRecordWords(), p);
        visit(recordKeyClause.getQualifiedDataName(), p);
        visit(recordKeyClause.getPasswordClause(), p);
        visit(recordKeyClause.getDuplicates(), p);
        return recordKeyClause;
    }

    public Cobol visitRecordingModeClause(Cobol.RecordingModeClause recordingModeClause, PrintOutputCapture<P> p) {
        visitSpace(recordingModeClause.getPrefix(), p);
        visitMarkers(recordingModeClause.getMarkers(), p);
        visit(recordingModeClause.getWords(), p);
        visit(recordingModeClause.getMode(), p);
        return recordingModeClause;
    }

    public Cobol visitReferenceModifier(Cobol.ReferenceModifier referenceModifier, PrintOutputCapture<P> p) {
        visitSpace(referenceModifier.getPrefix(), p);
        visitMarkers(referenceModifier.getMarkers(), p);
        visit(referenceModifier.getLeftParen(), p);
        visit(referenceModifier.getCharacterPosition(), p);
        visit(referenceModifier.getColon(), p);
        visit(referenceModifier.getLength(), p);
        visit(referenceModifier.getRightParen(), p);
        return referenceModifier;
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
        visit(relationSignCondition.getWords(), p);
        return relationSignCondition;
    }

    public Cobol visitRelationalOperator(Cobol.RelationalOperator relationalOperator, PrintOutputCapture<P> p) {
        visitSpace(relationalOperator.getPrefix(), p);
        visitMarkers(relationalOperator.getMarkers(), p);
        visit(relationalOperator.getWords(), p);
        return relationalOperator;
    }

    public Cobol visitRelativeKeyClause(Cobol.RelativeKeyClause relativeKeyClause, PrintOutputCapture<P> p) {
        visitSpace(relativeKeyClause.getPrefix(), p);
        visitMarkers(relativeKeyClause.getMarkers(), p);
        visit(relativeKeyClause.getWords(), p);
        visit(relativeKeyClause.getQualifiedDataName(), p);
        return relativeKeyClause;
    }

    public Cobol visitReportClause(Cobol.ReportClause reportClause, PrintOutputCapture<P> p) {
        visitSpace(reportClause.getPrefix(), p);
        visitMarkers(reportClause.getMarkers(), p);
        visit(reportClause.getWords(), p);
        visitContainer("", reportClause.getPadding().getReportName(), "", "", p);
        return reportClause;
    }

    public Cobol visitReportDescription(Cobol.ReportDescription reportDescription, PrintOutputCapture<P> p) {
        visitSpace(reportDescription.getPrefix(), p);
        visitMarkers(reportDescription.getMarkers(), p);
        visit(reportDescription.getReportDescriptionEntry(), p);
        visitContainer("", reportDescription.getPadding().getGroupDescriptionEntries(), "", "", p);
        return reportDescription;
    }

    public Cobol visitReportDescriptionEntry(Cobol.ReportDescriptionEntry reportDescriptionEntry, PrintOutputCapture<P> p) {
        visitSpace(reportDescriptionEntry.getPrefix(), p);
        visitMarkers(reportDescriptionEntry.getMarkers(), p);
        visit(reportDescriptionEntry.getRd(), p);
        visit(reportDescriptionEntry.getQualifiedDataName(), p);
        visit(reportDescriptionEntry.getReportDescriptionGlobalClause(), p);
        visit(reportDescriptionEntry.getReportDescriptionPageLimitClause(), p);
        visit(reportDescriptionEntry.getReportDescriptionHeadingClause(), p);
        visit(reportDescriptionEntry.getReportDescriptionFirstDetailClause(), p);
        visit(reportDescriptionEntry.getReportDescriptionLastDetailClause(), p);
        visit(reportDescriptionEntry.getReportDescriptionFootingClause(), p);
        visit(reportDescriptionEntry.getDot(), p);
        return reportDescriptionEntry;
    }

    public Cobol visitReportDescriptionFirstDetailClause(Cobol.ReportDescriptionFirstDetailClause reportDescriptionFirstDetailClause, PrintOutputCapture<P> p) {
        visitSpace(reportDescriptionFirstDetailClause.getPrefix(), p);
        visitMarkers(reportDescriptionFirstDetailClause.getMarkers(), p);
        visit(reportDescriptionFirstDetailClause.getWords(), p);
        return reportDescriptionFirstDetailClause;
    }

    public Cobol visitReportDescriptionFootingClause(Cobol.ReportDescriptionFootingClause reportDescriptionFootingClause, PrintOutputCapture<P> p) {
        visitSpace(reportDescriptionFootingClause.getPrefix(), p);
        visitMarkers(reportDescriptionFootingClause.getMarkers(), p);
        visit(reportDescriptionFootingClause.getWords(), p);
        return reportDescriptionFootingClause;
    }

    public Cobol visitReportDescriptionGlobalClause(Cobol.ReportDescriptionGlobalClause reportDescriptionGlobalClause, PrintOutputCapture<P> p) {
        visitSpace(reportDescriptionGlobalClause.getPrefix(), p);
        visitMarkers(reportDescriptionGlobalClause.getMarkers(), p);
        visit(reportDescriptionGlobalClause.getWords(), p);
        return reportDescriptionGlobalClause;
    }

    public Cobol visitReportDescriptionHeadingClause(Cobol.ReportDescriptionHeadingClause reportDescriptionHeadingClause, PrintOutputCapture<P> p) {
        visitSpace(reportDescriptionHeadingClause.getPrefix(), p);
        visitMarkers(reportDescriptionHeadingClause.getMarkers(), p);
        visit(reportDescriptionHeadingClause.getWords(), p);
        return reportDescriptionHeadingClause;
    }

    public Cobol visitReportDescriptionLastDetailClause(Cobol.ReportDescriptionLastDetailClause reportDescriptionLastDetailClause, PrintOutputCapture<P> p) {
        visitSpace(reportDescriptionLastDetailClause.getPrefix(), p);
        visitMarkers(reportDescriptionLastDetailClause.getMarkers(), p);
        visit(reportDescriptionLastDetailClause.getWords(), p);
        return reportDescriptionLastDetailClause;
    }

    public Cobol visitReportDescriptionPageLimitClause(Cobol.ReportDescriptionPageLimitClause reportDescriptionPageLimitClause, PrintOutputCapture<P> p) {
        visitSpace(reportDescriptionPageLimitClause.getPrefix(), p);
        visitMarkers(reportDescriptionPageLimitClause.getMarkers(), p);
        visit(reportDescriptionPageLimitClause.getFirstWords(), p);
        visit(reportDescriptionPageLimitClause.getSecondWords(), p);
        return reportDescriptionPageLimitClause;
    }

    public Cobol visitReportGroupBlankWhenZeroClause(Cobol.ReportGroupBlankWhenZeroClause reportGroupBlankWhenZeroClause, PrintOutputCapture<P> p) {
        visitSpace(reportGroupBlankWhenZeroClause.getPrefix(), p);
        visitMarkers(reportGroupBlankWhenZeroClause.getMarkers(), p);
        visit(reportGroupBlankWhenZeroClause.getWords(), p);
        return reportGroupBlankWhenZeroClause;
    }

    public Cobol visitReportGroupColumnNumberClause(Cobol.ReportGroupColumnNumberClause reportGroupColumnNumberClause, PrintOutputCapture<P> p) {
        visitSpace(reportGroupColumnNumberClause.getPrefix(), p);
        visitMarkers(reportGroupColumnNumberClause.getMarkers(), p);
        visit(reportGroupColumnNumberClause.getWords(), p);
        return reportGroupColumnNumberClause;
    }

    public Cobol visitReportGroupDescriptionEntryFormat1(Cobol.ReportGroupDescriptionEntryFormat1 reportGroupDescriptionEntryFormat1, PrintOutputCapture<P> p) {
        visitSpace(reportGroupDescriptionEntryFormat1.getPrefix(), p);
        visitMarkers(reportGroupDescriptionEntryFormat1.getMarkers(), p);
        visit(reportGroupDescriptionEntryFormat1.getIntegerLiteral(), p);
        visit(reportGroupDescriptionEntryFormat1.getDataName(), p);
        visit(reportGroupDescriptionEntryFormat1.getGroupLineNumberClause(), p);
        visit(reportGroupDescriptionEntryFormat1.getGroupNextGroupClause(), p);
        visit(reportGroupDescriptionEntryFormat1.getGroupTypeClause(), p);
        visit(reportGroupDescriptionEntryFormat1.getGroupUsageClause(), p);
        visit(reportGroupDescriptionEntryFormat1.getDot(), p);
        return reportGroupDescriptionEntryFormat1;
    }

    public Cobol visitReportGroupDescriptionEntryFormat2(Cobol.ReportGroupDescriptionEntryFormat2 reportGroupDescriptionEntryFormat2, PrintOutputCapture<P> p) {
        visitSpace(reportGroupDescriptionEntryFormat2.getPrefix(), p);
        visitMarkers(reportGroupDescriptionEntryFormat2.getMarkers(), p);
        visit(reportGroupDescriptionEntryFormat2.getIntegerLiteral(), p);
        visit(reportGroupDescriptionEntryFormat2.getDataName(), p);
        visit(reportGroupDescriptionEntryFormat2.getReportGroupLineNumberClause(), p);
        visit(reportGroupDescriptionEntryFormat2.getGroupUsageClause(), p);
        visit(reportGroupDescriptionEntryFormat2.getDot(), p);
        return reportGroupDescriptionEntryFormat2;
    }

    public Cobol visitReportGroupDescriptionEntryFormat3(Cobol.ReportGroupDescriptionEntryFormat3 reportGroupDescriptionEntryFormat3, PrintOutputCapture<P> p) {
        visitSpace(reportGroupDescriptionEntryFormat3.getPrefix(), p);
        visitMarkers(reportGroupDescriptionEntryFormat3.getMarkers(), p);
        visit(reportGroupDescriptionEntryFormat3.getIntegerLiteral(), p);
        visit(reportGroupDescriptionEntryFormat3.getDataName(), p);
        visitContainer("", reportGroupDescriptionEntryFormat3.getPadding().getClauses(), "", "", p);
        visit(reportGroupDescriptionEntryFormat3.getDot(), p);
        return reportGroupDescriptionEntryFormat3;
    }

    public Cobol visitReportGroupIndicateClause(Cobol.ReportGroupIndicateClause reportGroupIndicateClause, PrintOutputCapture<P> p) {
        visitSpace(reportGroupIndicateClause.getPrefix(), p);
        visitMarkers(reportGroupIndicateClause.getMarkers(), p);
        visit(reportGroupIndicateClause.getWords(), p);
        return reportGroupIndicateClause;
    }

    public Cobol visitReportGroupJustifiedClause(Cobol.ReportGroupJustifiedClause reportGroupJustifiedClause, PrintOutputCapture<P> p) {
        visitSpace(reportGroupJustifiedClause.getPrefix(), p);
        visitMarkers(reportGroupJustifiedClause.getMarkers(), p);
        visit(reportGroupJustifiedClause.getWords(), p);
        return reportGroupJustifiedClause;
    }

    public Cobol visitReportGroupLineNumberClause(Cobol.ReportGroupLineNumberClause reportGroupLineNumberClause, PrintOutputCapture<P> p) {
        visitSpace(reportGroupLineNumberClause.getPrefix(), p);
        visitMarkers(reportGroupLineNumberClause.getMarkers(), p);
        visit(reportGroupLineNumberClause.getWords(), p);
        visit(reportGroupLineNumberClause.getClause(), p);
        return reportGroupLineNumberClause;
    }

    public Cobol visitReportGroupLineNumberNextPage(Cobol.ReportGroupLineNumberNextPage reportGroupLineNumberNextPage, PrintOutputCapture<P> p) {
        visitSpace(reportGroupLineNumberNextPage.getPrefix(), p);
        visitMarkers(reportGroupLineNumberNextPage.getMarkers(), p);
        visit(reportGroupLineNumberNextPage.getIntegerLiteral(), p);
        visit(reportGroupLineNumberNextPage.getWords(), p);
        return reportGroupLineNumberNextPage;
    }

    public Cobol visitReportGroupLineNumberPlus(Cobol.ReportGroupLineNumberPlus reportGroupLineNumberPlus, PrintOutputCapture<P> p) {
        visitSpace(reportGroupLineNumberPlus.getPrefix(), p);
        visitMarkers(reportGroupLineNumberPlus.getMarkers(), p);
        visit(reportGroupLineNumberPlus.getPlus(), p);
        visit(reportGroupLineNumberPlus.getIntegerLiteral(), p);
        return reportGroupLineNumberPlus;
    }

    public Cobol visitReportGroupNextGroupClause(Cobol.ReportGroupNextGroupClause reportGroupNextGroupClause, PrintOutputCapture<P> p) {
        visitSpace(reportGroupNextGroupClause.getPrefix(), p);
        visitMarkers(reportGroupNextGroupClause.getMarkers(), p);
        visit(reportGroupNextGroupClause.getWords(), p);
        visit(reportGroupNextGroupClause.getClause(), p);
        return reportGroupNextGroupClause;
    }

    public Cobol visitReportGroupNextGroupNextPage(Cobol.ReportGroupNextGroupNextPage reportGroupNextGroupNextPage, PrintOutputCapture<P> p) {
        visitSpace(reportGroupNextGroupNextPage.getPrefix(), p);
        visitMarkers(reportGroupNextGroupNextPage.getMarkers(), p);
        visit(reportGroupNextGroupNextPage.getNextPage(), p);
        return reportGroupNextGroupNextPage;
    }

    public Cobol visitReportGroupNextGroupPlus(Cobol.ReportGroupNextGroupPlus reportGroupNextGroupPlus, PrintOutputCapture<P> p) {
        visitSpace(reportGroupNextGroupPlus.getPrefix(), p);
        visitMarkers(reportGroupNextGroupPlus.getMarkers(), p);
        visit(reportGroupNextGroupPlus.getPlus(), p);
        visit(reportGroupNextGroupPlus.getIntegerLiteral(), p);
        return reportGroupNextGroupPlus;
    }

    public Cobol visitReportGroupPictureClause(Cobol.ReportGroupPictureClause reportGroupPictureClause, PrintOutputCapture<P> p) {
        visitSpace(reportGroupPictureClause.getPrefix(), p);
        visitMarkers(reportGroupPictureClause.getMarkers(), p);
        visit(reportGroupPictureClause.getWords(), p);
        visit(reportGroupPictureClause.getPictureString(), p);
        return reportGroupPictureClause;
    }

    public Cobol visitReportGroupResetClause(Cobol.ReportGroupResetClause reportGroupResetClause, PrintOutputCapture<P> p) {
        visitSpace(reportGroupResetClause.getPrefix(), p);
        visitMarkers(reportGroupResetClause.getMarkers(), p);
        visit(reportGroupResetClause.getWords(), p);
        return reportGroupResetClause;
    }

    public Cobol visitReportGroupSignClause(Cobol.ReportGroupSignClause reportGroupSignClause, PrintOutputCapture<P> p) {
        visitSpace(reportGroupSignClause.getPrefix(), p);
        visitMarkers(reportGroupSignClause.getMarkers(), p);
        visit(reportGroupSignClause.getWords(), p);
        return reportGroupSignClause;
    }

    public Cobol visitReportGroupSourceClause(Cobol.ReportGroupSourceClause reportGroupSourceClause, PrintOutputCapture<P> p) {
        visitSpace(reportGroupSourceClause.getPrefix(), p);
        visitMarkers(reportGroupSourceClause.getMarkers(), p);
        visit(reportGroupSourceClause.getWords(), p);
        return reportGroupSourceClause;
    }

    public Cobol visitReportGroupSumClause(Cobol.ReportGroupSumClause reportGroupSumClause, PrintOutputCapture<P> p) {
        visitSpace(reportGroupSumClause.getPrefix(), p);
        visitMarkers(reportGroupSumClause.getMarkers(), p);
        visit(reportGroupSumClause.getWords(), p);
        for (Cobol c : reportGroupSumClause.getIdentifiers()) {
            visit(c, p);
        }
        visit(reportGroupSumClause.getUpon(), p);
        for (Cobol c : reportGroupSumClause.getDataNames()) {
            visit(c, p);
        }
        return reportGroupSumClause;
    }

    public Cobol visitReportGroupTypeClause(Cobol.ReportGroupTypeClause reportGroupTypeClause, PrintOutputCapture<P> p) {
        visitSpace(reportGroupTypeClause.getPrefix(), p);
        visitMarkers(reportGroupTypeClause.getMarkers(), p);
        visit(reportGroupTypeClause.getWords(), p);
        visit(reportGroupTypeClause.getType(), p);
        return reportGroupTypeClause;
    }

    public Cobol visitReportGroupTypeControlFooting(Cobol.ReportGroupTypeControlFooting reportGroupTypeControlFooting, PrintOutputCapture<P> p) {
        visitSpace(reportGroupTypeControlFooting.getPrefix(), p);
        visitMarkers(reportGroupTypeControlFooting.getMarkers(), p);
        visit(reportGroupTypeControlFooting.getWords(), p);
        return reportGroupTypeControlFooting;
    }

    public Cobol visitReportGroupTypeControlHeading(Cobol.ReportGroupTypeControlHeading reportGroupTypeControlHeading, PrintOutputCapture<P> p) {
        visitSpace(reportGroupTypeControlHeading.getPrefix(), p);
        visitMarkers(reportGroupTypeControlHeading.getMarkers(), p);
        visit(reportGroupTypeControlHeading.getWords(), p);
        return reportGroupTypeControlHeading;
    }

    public Cobol visitReportGroupTypeDetail(Cobol.ReportGroupTypeDetail reportGroupTypeDetail, PrintOutputCapture<P> p) {
        visitSpace(reportGroupTypeDetail.getPrefix(), p);
        visitMarkers(reportGroupTypeDetail.getMarkers(), p);
        visit(reportGroupTypeDetail.getWords(), p);
        return reportGroupTypeDetail;
    }

    public Cobol visitReportGroupTypePageFooting(Cobol.ReportGroupTypePageFooting reportGroupTypePageFooting, PrintOutputCapture<P> p) {
        visitSpace(reportGroupTypePageFooting.getPrefix(), p);
        visitMarkers(reportGroupTypePageFooting.getMarkers(), p);
        visit(reportGroupTypePageFooting.getWords(), p);
        return reportGroupTypePageFooting;
    }

    public Cobol visitReportGroupTypeReportFooting(Cobol.ReportGroupTypeReportFooting reportGroupTypeReportFooting, PrintOutputCapture<P> p) {
        visitSpace(reportGroupTypeReportFooting.getPrefix(), p);
        visitMarkers(reportGroupTypeReportFooting.getMarkers(), p);
        visit(reportGroupTypeReportFooting.getWords(), p);
        return reportGroupTypeReportFooting;
    }

    public Cobol visitReportGroupTypeReportHeading(Cobol.ReportGroupTypeReportHeading reportGroupTypeReportHeading, PrintOutputCapture<P> p) {
        visitSpace(reportGroupTypeReportHeading.getPrefix(), p);
        visitMarkers(reportGroupTypeReportHeading.getMarkers(), p);
        visit(reportGroupTypeReportHeading.getWords(), p);
        return reportGroupTypeReportHeading;
    }

    public Cobol visitReportGroupUsageClause(Cobol.ReportGroupUsageClause reportGroupUsageClause, PrintOutputCapture<P> p) {
        visitSpace(reportGroupUsageClause.getPrefix(), p);
        visitMarkers(reportGroupUsageClause.getMarkers(), p);
        visit(reportGroupUsageClause.getWords(), p);
        return reportGroupUsageClause;
    }

    public Cobol visitReportGroupValueClause(Cobol.ReportGroupValueClause reportGroupValueClause, PrintOutputCapture<P> p) {
        visitSpace(reportGroupValueClause.getPrefix(), p);
        visitMarkers(reportGroupValueClause.getMarkers(), p);
        visit(reportGroupValueClause.getWords(), p);
        return reportGroupValueClause;
    }

    public Cobol visitReportName(Cobol.ReportName reportName, PrintOutputCapture<P> p) {
        visitSpace(reportName.getPrefix(), p);
        visitMarkers(reportName.getMarkers(), p);
        visit(reportName.getQualifiedDataName(), p);
        return reportName;
    }

    public Cobol visitReportSection(Cobol.ReportSection reportSection, PrintOutputCapture<P> p) {
        visitSpace(reportSection.getPrefix(), p);
        visitMarkers(reportSection.getMarkers(), p);
        visit(reportSection.getWords(), p);
        visitContainer("", reportSection.getPadding().getDescriptions(), "", "", p);
        return reportSection;
    }

    public Cobol visitRerunClause(Cobol.RerunClause rerunClause, PrintOutputCapture<P> p) {
        visitSpace(rerunClause.getPrefix(), p);
        visitMarkers(rerunClause.getMarkers(), p);
        visit(rerunClause.getRerun(), p);
        visit(rerunClause.getOn(), p);
        visit(rerunClause.getName(), p);
        visit(rerunClause.getEvery(), p);
        visit(rerunClause.getAction(), p);
        return rerunClause;
    }

    public Cobol visitRerunEveryClock(Cobol.RerunEveryClock rerunEveryClock, PrintOutputCapture<P> p) {
        visitSpace(rerunEveryClock.getPrefix(), p);
        visitMarkers(rerunEveryClock.getMarkers(), p);
        visit(rerunEveryClock.getIntegerLiteral(), p);
        visit(rerunEveryClock.getClockUnits(), p);
        return rerunEveryClock;
    }

    public Cobol visitRerunEveryOf(Cobol.RerunEveryOf rerunEveryOf, PrintOutputCapture<P> p) {
        visitSpace(rerunEveryOf.getPrefix(), p);
        visitMarkers(rerunEveryOf.getMarkers(), p);
        visit(rerunEveryOf.getRecords(), p);
        visit(rerunEveryOf.getFileName(), p);
        return rerunEveryOf;
    }

    public Cobol visitRerunEveryRecords(Cobol.RerunEveryRecords rerunEveryRecords, PrintOutputCapture<P> p) {
        visitSpace(rerunEveryRecords.getPrefix(), p);
        visitMarkers(rerunEveryRecords.getMarkers(), p);
        visit(rerunEveryRecords.getIntegerLiteral(), p);
        visit(rerunEveryRecords.getRecords(), p);
        return rerunEveryRecords;
    }

    public Cobol visitReserveClause(Cobol.ReserveClause reserveClause, PrintOutputCapture<P> p) {
        visitSpace(reserveClause.getPrefix(), p);
        visitMarkers(reserveClause.getMarkers(), p);
        for (Cobol c : reserveClause.getWords()) {
            visit(c, p);
        }
        return reserveClause;
    }

    public Cobol visitReserveNetworkClause(Cobol.ReserveNetworkClause reserveNetworkClause, PrintOutputCapture<P> p) {
        visitSpace(reserveNetworkClause.getPrefix(), p);
        visitMarkers(reserveNetworkClause.getMarkers(), p);
        visit(reserveNetworkClause.getWords(), p);
        return reserveNetworkClause;
    }

    @Override
    public Cobol visitReturn(Cobol.Return r, PrintOutputCapture<P> p) {
        visitSpace(r.getPrefix(), p);
        visitMarkers(r.getMarkers(), p);
        visit(r.getWords(), p);
        visit(r.getFileName(), p);
        visit(r.getRecord(), p);
        visit(r.getInto(), p);
        visit(r.getAtEndPhrase(), p);
        visit(r.getNotAtEndPhrase(), p);
        visit(r.getEndReturn(), p);
        return r;
    }

    @Override
    public Cobol visitReturnInto(Cobol.ReturnInto r, PrintOutputCapture<P> p) {
        visitSpace(r.getPrefix(), p);
        visitMarkers(r.getMarkers(), p);
        visit(r.getInto(), p);
        visit(r.getQualifiedDataName(), p);
        return r;
    }

    public Cobol visitRewrite(Cobol.Rewrite rewrite, PrintOutputCapture<P> p) {
        visitSpace(rewrite.getPrefix(), p);
        visitMarkers(rewrite.getMarkers(), p);
        visit(rewrite.getRewrite(), p);
        visit(rewrite.getRecordName(), p);
        visit(rewrite.getInvalidKeyPhrase(), p);
        visit(rewrite.getNotInvalidKeyPhrase(), p);
        visit(rewrite.getEndRewrite(), p);
        return rewrite;
    }

    public Cobol visitRewriteFrom(Cobol.RewriteFrom rewriteFrom, PrintOutputCapture<P> p) {
        visitSpace(rewriteFrom.getPrefix(), p);
        visitMarkers(rewriteFrom.getMarkers(), p);
        visit(rewriteFrom.getFrom(), p);
        return rewriteFrom;
    }

    public Cobol visitRoundable(Cobol.Roundable roundable, PrintOutputCapture<P> p) {
        visitSpace(roundable.getPrefix(), p);
        visitMarkers(roundable.getMarkers(), p);
        visit(roundable.getIdentifier(), p);
        visit(roundable.getRounded(), p);
        return roundable;
    }

    public Cobol visitSameClause(Cobol.SameClause sameClause, PrintOutputCapture<P> p) {
        visitSpace(sameClause.getPrefix(), p);
        visitMarkers(sameClause.getMarkers(), p);
        visit(sameClause.getWords(), p);
        visitContainer("", sameClause.getPadding().getFileNames(), "", "", p);
        return sameClause;
    }

    public Cobol visitScreenDescriptionAutoClause(Cobol.ScreenDescriptionAutoClause screenDescriptionAutoClause, PrintOutputCapture<P> p) {
        visitSpace(screenDescriptionAutoClause.getPrefix(), p);
        visitMarkers(screenDescriptionAutoClause.getMarkers(), p);
        visit(screenDescriptionAutoClause.getAuto(), p);
        return screenDescriptionAutoClause;
    }

    public Cobol visitScreenDescriptionBackgroundColorClause(Cobol.ScreenDescriptionBackgroundColorClause screenDescriptionBackgroundColorClause, PrintOutputCapture<P> p) {
        visitSpace(screenDescriptionBackgroundColorClause.getPrefix(), p);
        visitMarkers(screenDescriptionBackgroundColorClause.getMarkers(), p);
        visit(screenDescriptionBackgroundColorClause.getBackground(), p);
        visit(screenDescriptionBackgroundColorClause.getIs(), p);
        visit(screenDescriptionBackgroundColorClause.getValue(), p);
        return screenDescriptionBackgroundColorClause;
    }

    public Cobol visitScreenDescriptionBellClause(Cobol.ScreenDescriptionBellClause screenDescriptionBellClause, PrintOutputCapture<P> p) {
        visitSpace(screenDescriptionBellClause.getPrefix(), p);
        visitMarkers(screenDescriptionBellClause.getMarkers(), p);
        visit(screenDescriptionBellClause.getBell(), p);
        return screenDescriptionBellClause;
    }

    public Cobol visitScreenDescriptionBlankClause(Cobol.ScreenDescriptionBlankClause screenDescriptionBlankClause, PrintOutputCapture<P> p) {
        visitSpace(screenDescriptionBlankClause.getPrefix(), p);
        visitMarkers(screenDescriptionBlankClause.getMarkers(), p);
        visit(screenDescriptionBlankClause.getWords(), p);
        return screenDescriptionBlankClause;
    }

    public Cobol visitScreenDescriptionBlankWhenZeroClause(Cobol.ScreenDescriptionBlankWhenZeroClause screenDescriptionBlankWhenZeroClause, PrintOutputCapture<P> p) {
        visitSpace(screenDescriptionBlankWhenZeroClause.getPrefix(), p);
        visitMarkers(screenDescriptionBlankWhenZeroClause.getMarkers(), p);
        visit(screenDescriptionBlankWhenZeroClause.getWords(), p);
        return screenDescriptionBlankWhenZeroClause;
    }

    public Cobol visitScreenDescriptionBlinkClause(Cobol.ScreenDescriptionBlinkClause screenDescriptionBlinkClause, PrintOutputCapture<P> p) {
        visitSpace(screenDescriptionBlinkClause.getPrefix(), p);
        visitMarkers(screenDescriptionBlinkClause.getMarkers(), p);
        visit(screenDescriptionBlinkClause.getBlink(), p);
        return screenDescriptionBlinkClause;
    }

    public Cobol visitScreenDescriptionColumnClause(Cobol.ScreenDescriptionColumnClause screenDescriptionColumnClause, PrintOutputCapture<P> p) {
        visitSpace(screenDescriptionColumnClause.getPrefix(), p);
        visitMarkers(screenDescriptionColumnClause.getMarkers(), p);
        visit(screenDescriptionColumnClause.getWords(), p);
        visit(screenDescriptionColumnClause.getValue(), p);
        return screenDescriptionColumnClause;
    }

    public Cobol visitScreenDescriptionControlClause(Cobol.ScreenDescriptionControlClause screenDescriptionControlClause, PrintOutputCapture<P> p) {
        visitSpace(screenDescriptionControlClause.getPrefix(), p);
        visitMarkers(screenDescriptionControlClause.getMarkers(), p);
        visit(screenDescriptionControlClause.getWords(), p);
        visit(screenDescriptionControlClause.getValue(), p);
        return screenDescriptionControlClause;
    }

    public Cobol visitScreenDescriptionEntry(Cobol.ScreenDescriptionEntry screenDescriptionEntry, PrintOutputCapture<P> p) {
        visitSpace(screenDescriptionEntry.getPrefix(), p);
        visitMarkers(screenDescriptionEntry.getMarkers(), p);
        visit(screenDescriptionEntry.getWords(), p);
        visit(screenDescriptionEntry.getName(), p);
        visitContainer("", screenDescriptionEntry.getPadding().getClauses(), "", ".", p);
        return screenDescriptionEntry;
    }

    public Cobol visitScreenDescriptionEraseClause(Cobol.ScreenDescriptionEraseClause screenDescriptionEraseClause, PrintOutputCapture<P> p) {
        visitSpace(screenDescriptionEraseClause.getPrefix(), p);
        visitMarkers(screenDescriptionEraseClause.getMarkers(), p);
        visit(screenDescriptionEraseClause.getWords(), p);
        return screenDescriptionEraseClause;
    }

    public Cobol visitScreenDescriptionForegroundColorClause(Cobol.ScreenDescriptionForegroundColorClause screenDescriptionForegroundColorClause, PrintOutputCapture<P> p) {
        visitSpace(screenDescriptionForegroundColorClause.getPrefix(), p);
        visitMarkers(screenDescriptionForegroundColorClause.getMarkers(), p);
        visit(screenDescriptionForegroundColorClause.getWords(), p);
        visit(screenDescriptionForegroundColorClause.getValue(), p);
        return screenDescriptionForegroundColorClause;
    }

    public Cobol visitScreenDescriptionFromClause(Cobol.ScreenDescriptionFromClause screenDescriptionFromClause, PrintOutputCapture<P> p) {
        visitSpace(screenDescriptionFromClause.getPrefix(), p);
        visitMarkers(screenDescriptionFromClause.getMarkers(), p);
        visit(screenDescriptionFromClause.getFrom(), p);
        visit(screenDescriptionFromClause.getValue(), p);
        visit(screenDescriptionFromClause.getScreenDescriptionToClause(), p);
        return screenDescriptionFromClause;
    }

    public Cobol visitScreenDescriptionFullClause(Cobol.ScreenDescriptionFullClause screenDescriptionFullClause, PrintOutputCapture<P> p) {
        visitSpace(screenDescriptionFullClause.getPrefix(), p);
        visitMarkers(screenDescriptionFullClause.getMarkers(), p);
        visit(screenDescriptionFullClause.getWord(), p);
        return screenDescriptionFullClause;
    }

    public Cobol visitScreenDescriptionGridClause(Cobol.ScreenDescriptionGridClause screenDescriptionGridClause, PrintOutputCapture<P> p) {
        visitSpace(screenDescriptionGridClause.getPrefix(), p);
        visitMarkers(screenDescriptionGridClause.getMarkers(), p);
        visit(screenDescriptionGridClause.getWord(), p);
        return screenDescriptionGridClause;
    }

    public Cobol visitScreenDescriptionJustifiedClause(Cobol.ScreenDescriptionJustifiedClause screenDescriptionJustifiedClause, PrintOutputCapture<P> p) {
        visitSpace(screenDescriptionJustifiedClause.getPrefix(), p);
        visitMarkers(screenDescriptionJustifiedClause.getMarkers(), p);
        visit(screenDescriptionJustifiedClause.getWords(), p);
        return screenDescriptionJustifiedClause;
    }

    public Cobol visitScreenDescriptionLightClause(Cobol.ScreenDescriptionLightClause screenDescriptionLightClause, PrintOutputCapture<P> p) {
        visitSpace(screenDescriptionLightClause.getPrefix(), p);
        visitMarkers(screenDescriptionLightClause.getMarkers(), p);
        visit(screenDescriptionLightClause.getLight(), p);
        return screenDescriptionLightClause;
    }

    public Cobol visitScreenDescriptionLineClause(Cobol.ScreenDescriptionLineClause screenDescriptionLineClause, PrintOutputCapture<P> p) {
        visitSpace(screenDescriptionLineClause.getPrefix(), p);
        visitMarkers(screenDescriptionLineClause.getMarkers(), p);
        visit(screenDescriptionLineClause.getWords(), p);
        visit(screenDescriptionLineClause.getValue(), p);
        return screenDescriptionLineClause;
    }

    public Cobol visitScreenDescriptionPictureClause(Cobol.ScreenDescriptionPictureClause screenDescriptionPictureClause, PrintOutputCapture<P> p) {
        visitSpace(screenDescriptionPictureClause.getPrefix(), p);
        visitMarkers(screenDescriptionPictureClause.getMarkers(), p);
        visit(screenDescriptionPictureClause.getWords(), p);
        visit(screenDescriptionPictureClause.getPictureString(), p);
        return screenDescriptionPictureClause;
    }

    public Cobol visitScreenDescriptionPromptClause(Cobol.ScreenDescriptionPromptClause screenDescriptionPromptClause, PrintOutputCapture<P> p) {
        visitSpace(screenDescriptionPromptClause.getPrefix(), p);
        visitMarkers(screenDescriptionPromptClause.getMarkers(), p);
        visit(screenDescriptionPromptClause.getWords(), p);
        visit(screenDescriptionPromptClause.getName(), p);
        visit(screenDescriptionPromptClause.getScreenDescriptionPromptOccursClause(), p);
        return screenDescriptionPromptClause;
    }

    public Cobol visitScreenDescriptionPromptOccursClause(Cobol.ScreenDescriptionPromptOccursClause screenDescriptionPromptOccursClause, PrintOutputCapture<P> p) {
        visitSpace(screenDescriptionPromptOccursClause.getPrefix(), p);
        visitMarkers(screenDescriptionPromptOccursClause.getMarkers(), p);
        visit(screenDescriptionPromptOccursClause.getOccurs(), p);
        visit(screenDescriptionPromptOccursClause.getInteger(), p);
        visit(screenDescriptionPromptOccursClause.getTimes(), p);
        return screenDescriptionPromptOccursClause;
    }

    public Cobol visitScreenDescriptionRequiredClause(Cobol.ScreenDescriptionRequiredClause screenDescriptionRequiredClause, PrintOutputCapture<P> p) {
        visitSpace(screenDescriptionRequiredClause.getPrefix(), p);
        visitMarkers(screenDescriptionRequiredClause.getMarkers(), p);
        visit(screenDescriptionRequiredClause.getRequired(), p);
        return screenDescriptionRequiredClause;
    }

    public Cobol visitScreenDescriptionReverseVideoClause(Cobol.ScreenDescriptionReverseVideoClause screenDescriptionReverseVideoClause, PrintOutputCapture<P> p) {
        visitSpace(screenDescriptionReverseVideoClause.getPrefix(), p);
        visitMarkers(screenDescriptionReverseVideoClause.getMarkers(), p);
        visit(screenDescriptionReverseVideoClause.getWord(), p);
        return screenDescriptionReverseVideoClause;
    }

    public Cobol visitScreenDescriptionSignClause(Cobol.ScreenDescriptionSignClause screenDescriptionSignClause, PrintOutputCapture<P> p) {
        visitSpace(screenDescriptionSignClause.getPrefix(), p);
        visitMarkers(screenDescriptionSignClause.getMarkers(), p);
        visit(screenDescriptionSignClause.getWords(), p);
        return screenDescriptionSignClause;
    }

    public Cobol visitScreenDescriptionSizeClause(Cobol.ScreenDescriptionSizeClause screenDescriptionSizeClause, PrintOutputCapture<P> p) {
        visitSpace(screenDescriptionSizeClause.getPrefix(), p);
        visitMarkers(screenDescriptionSizeClause.getMarkers(), p);
        visit(screenDescriptionSizeClause.getWords(), p);
        visit(screenDescriptionSizeClause.getValue(), p);
        return screenDescriptionSizeClause;
    }

    public Cobol visitScreenDescriptionToClause(Cobol.ScreenDescriptionToClause screenDescriptionToClause, PrintOutputCapture<P> p) {
        visitSpace(screenDescriptionToClause.getPrefix(), p);
        visitMarkers(screenDescriptionToClause.getMarkers(), p);
        visit(screenDescriptionToClause.getTo(), p);
        visit(screenDescriptionToClause.getIdentifier(), p);
        return screenDescriptionToClause;
    }

    public Cobol visitScreenDescriptionUnderlineClause(Cobol.ScreenDescriptionUnderlineClause screenDescriptionUnderlineClause, PrintOutputCapture<P> p) {
        visitSpace(screenDescriptionUnderlineClause.getPrefix(), p);
        visitMarkers(screenDescriptionUnderlineClause.getMarkers(), p);
        visit(screenDescriptionUnderlineClause.getUnderline(), p);
        return screenDescriptionUnderlineClause;
    }

    public Cobol visitScreenDescriptionUsageClause(Cobol.ScreenDescriptionUsageClause screenDescriptionUsageClause, PrintOutputCapture<P> p) {
        visitSpace(screenDescriptionUsageClause.getPrefix(), p);
        visitMarkers(screenDescriptionUsageClause.getMarkers(), p);
        visit(screenDescriptionUsageClause.getWords(), p);
        return screenDescriptionUsageClause;
    }

    public Cobol visitScreenDescriptionUsingClause(Cobol.ScreenDescriptionUsingClause screenDescriptionUsingClause, PrintOutputCapture<P> p) {
        visitSpace(screenDescriptionUsingClause.getPrefix(), p);
        visitMarkers(screenDescriptionUsingClause.getMarkers(), p);
        visit(screenDescriptionUsingClause.getUsing(), p);
        visit(screenDescriptionUsingClause.getIdentifier(), p);
        return screenDescriptionUsingClause;
    }

    public Cobol visitScreenDescriptionValueClause(Cobol.ScreenDescriptionValueClause screenDescriptionValueClause, PrintOutputCapture<P> p) {
        visitSpace(screenDescriptionValueClause.getPrefix(), p);
        visitMarkers(screenDescriptionValueClause.getMarkers(), p);
        visit(screenDescriptionValueClause.getWords(), p);
        visit(screenDescriptionValueClause.getValue(), p);
        return screenDescriptionValueClause;
    }

    public Cobol visitScreenDescriptionZeroFillClause(Cobol.ScreenDescriptionZeroFillClause screenDescriptionZeroFillClause, PrintOutputCapture<P> p) {
        visitSpace(screenDescriptionZeroFillClause.getPrefix(), p);
        visitMarkers(screenDescriptionZeroFillClause.getMarkers(), p);
        visit(screenDescriptionZeroFillClause.getWord(), p);
        return screenDescriptionZeroFillClause;
    }

    public Cobol visitScreenSection(Cobol.ScreenSection screenSection, PrintOutputCapture<P> p) {
        visitSpace(screenSection.getPrefix(), p);
        visitMarkers(screenSection.getMarkers(), p);
        visit(screenSection.getWords(), p);
        visitContainer(".", screenSection.getPadding().getDescriptions(), "", "", p);
        return screenSection;
    }

    @Override
    public Cobol visitSearch(Cobol.Search s, PrintOutputCapture<P> p) {
        visitSpace(s.getPrefix(), p);
        visitMarkers(s.getMarkers(), p);
        visit(s.getWords(), p);
        visit(s.getQualifiedDataName(), p);
        visit(s.getSearchVarying(), p);
        visit(s.getAtEndPhrase(), p);
        for(Cobol.SearchWhen sw : s.getSearchWhen()) {
            visit(sw, p);
        }
        visit(s.getEndSearch(), p);
        return s;
    }

    @Override
    public Cobol visitSearchVarying(Cobol.SearchVarying s, PrintOutputCapture<P> p) {
        visitSpace(s.getPrefix(), p);
        visitMarkers(s.getMarkers(), p);
        visit(s.getVarying(), p);
        visit(s.getQualifiedDataName(), p);
        return s;
    }

    @Override
    public Cobol visitSearchWhen(Cobol.SearchWhen s, PrintOutputCapture<P> p) {
        visitSpace(s.getPrefix(), p);
        visitMarkers(s.getMarkers(), p);
        visit(s.getWhen(), p);
        visit(s.getCondition(), p);
        visit(s.getNextSentence(), p);
        for(Statement st : s.getStatements()) {
            visit(st, p);
        }
        return s;
    }

    public Cobol visitSelectClause(Cobol.SelectClause selectClause, PrintOutputCapture<P> p) {
        visitSpace(selectClause.getPrefix(), p);
        visitMarkers(selectClause.getMarkers(), p);
        visit(selectClause.getWords(), p);
        visit(selectClause.getFileName(), p);
        return selectClause;
    }

    @Override
    public Cobol visitSend(Cobol.Send s, PrintOutputCapture<P> p) {
        visitSpace(s.getPrefix(), p);
        visitMarkers(s.getMarkers(), p);
        visit(s.getSend(), p);
        visit(s.getStatement(), p);
        visit(s.getOnExceptionClause(), p);
        visit(s.getNotOnExceptionClause(), p);
        return s;
    }

    @Override
    public Cobol visitSendPhrase(Cobol.SendPhrase s, PrintOutputCapture<P> p) {
        visitSpace(s.getPrefix(), p);
        visitMarkers(s.getMarkers(), p);
        visit(s.getWords(), p);
        visit(s.getTarget(), p);
        return s;
    }

    @Override
    public Cobol visitSendStatementSync(Cobol.SendStatementSync s, PrintOutputCapture<P> p) {
        visitSpace(s.getPrefix(), p);
        visitMarkers(s.getMarkers(), p);
        visit(s.getName(), p);
        visit(s.getSendFromPhrase(), p);
        visit(s.getSendWithPhrase(), p);
        visit(s.getSendReplacingPhrase(), p);
        visit(s.getSendAdvancingPhrase(), p);
        return s;
    }

    public Cobol visitSentence(Cobol.Sentence sentence, PrintOutputCapture<P> p) {
        visitSpace(sentence.getPrefix(), p);
        visitMarkers(sentence.getMarkers(), p);
        for (Statement s : sentence.getStatements()) {
            visit(s, p);
        }
        visit(sentence.getDot(), p);
        return sentence;
    }

    public Cobol visitSet(Cobol.Set set, PrintOutputCapture<P> p) {
        visitSpace(set.getPrefix(), p);
        visitMarkers(set.getMarkers(), p);
        visit(set.getSet(), p);
        visitContainer("", set.getPadding().getTo(), "", "", p);
        visit(set.getUpDown(), p);
        return set;
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
        visit(setUpDown.getOperation(), p);
        return setUpDown;
    }

    public Cobol visitSort(Cobol.Sort sort, PrintOutputCapture<P> p) {
        visitSpace(sort.getPrefix(), p);
        visitMarkers(sort.getMarkers(), p);
        visit(sort.getSort(), p);
        visit(sort.getFileName(), p);
        visitContainer("", sort.getPadding().getSortOnKeyClause(), "", "", p);
        visit(sort.getSortDuplicatesPhrase(), p);
        visit(sort.getSortCollatingSequencePhrase(), p);
        visit(sort.getSortInputProcedurePhrase(), p);
        visitContainer("", sort.getPadding().getSortUsing(), "", "", p);
        visit(sort.getSortOutputProcedurePhrase(), p);
        visitContainer("", sort.getPadding().getSortGiving(), "", "", p);
        return sort;
    }

    public Cobol visitSortCollatingSequencePhrase(Cobol.SortCollatingSequencePhrase sortCollatingSequencePhrase, PrintOutputCapture<P> p) {
        visitSpace(sortCollatingSequencePhrase.getPrefix(), p);
        visitMarkers(sortCollatingSequencePhrase.getMarkers(), p);
        visit(sortCollatingSequencePhrase.getWords(), p);
        visitContainer("", sortCollatingSequencePhrase.getPadding().getAlphabetNames(), "", "", p);
        visit(sortCollatingSequencePhrase.getSortCollatingAlphanumeric(), p);
        visit(sortCollatingSequencePhrase.getSortCollatingNational(), p);
        return sortCollatingSequencePhrase;
    }

    public Cobol visitSortGiving(Cobol.SortGiving sortGiving, PrintOutputCapture<P> p) {
        visitSpace(sortGiving.getPrefix(), p);
        visitMarkers(sortGiving.getMarkers(), p);
        visit(sortGiving.getFileName(), p);
        visit(sortGiving.getWords(), p);
        return sortGiving;
    }

    public Cobol visitSortProcedurePhrase(Cobol.SortProcedurePhrase sortProcedurePhrase, PrintOutputCapture<P> p) {
        visitSpace(sortProcedurePhrase.getPrefix(), p);
        visitMarkers(sortProcedurePhrase.getMarkers(), p);
        visit(sortProcedurePhrase.getWords(), p);
        visit(sortProcedurePhrase.getProcedureName(), p);
        visit(sortProcedurePhrase.getSortInputThrough(), p);
        return sortProcedurePhrase;
    }

    public Cobol visitSortable(Cobol.Sortable sortable, PrintOutputCapture<P> p) {
        visitSpace(sortable.getPrefix(), p);
        visitMarkers(sortable.getMarkers(), p);
        visit(sortable.getWords(), p);
        visitContainer("", sortable.getPadding().getNames(), "", "", p);
        return sortable;
    }

    public Cobol visitSourceComputer(Cobol.SourceComputer sourceComputer, PrintOutputCapture<P> p) {
        visitSpace(sourceComputer.getPrefix(), p);
        visitMarkers(sourceComputer.getMarkers(), p);
        visit(sourceComputer.getWords(), p);
        visit(sourceComputer.getComputer(), p);
        visit(sourceComputer.getDot(), p);
        return sourceComputer;
    }

    public Cobol visitSourceComputerDefinition(Cobol.SourceComputerDefinition sourceComputerDefinition, PrintOutputCapture<P> p) {
        visitSpace(sourceComputerDefinition.getPrefix(), p);
        visitMarkers(sourceComputerDefinition.getMarkers(), p);
        visit(sourceComputerDefinition.getComputerName(), p);
        visit(sourceComputerDefinition.getDebuggingMode(), p);
        return sourceComputerDefinition;
    }

    @Override
    public Space visitSpace(Space space, PrintOutputCapture<P> p) {
        p.append(space.getWhitespace());
        return space;
    }

    public Cobol visitSpecialNames(Cobol.SpecialNames specialNames, PrintOutputCapture<P> p) {
        visitSpace(specialNames.getPrefix(), p);
        visitMarkers(specialNames.getMarkers(), p);
        visit(specialNames.getWords(), p);
        visitContainer(".", specialNames.getPadding().getClauses(), "", "", p);
        if (!specialNames.getClauses().isEmpty()) {
            p.append('.');
        }
        return specialNames;
    }

    public Cobol visitStart(Cobol.Start start, PrintOutputCapture<P> p) {
        visitSpace(start.getPrefix(), p);
        visitMarkers(start.getMarkers(), p);
        visit(start.getStart(), p);
        visit(start.getFileName(), p);
        visit(start.getStartKey(), p);
        visit(start.getInvalidKeyPhrase(), p);
        visit(start.getNotInvalidKeyPhrase(), p);
        visit(start.getEndStart(), p);
        return start;
    }

    public Cobol visitStartKey(Cobol.StartKey startKey, PrintOutputCapture<P> p) {
        visitSpace(startKey.getPrefix(), p);
        visitMarkers(startKey.getMarkers(), p);
        visit(startKey.getWords(), p);
        visit(startKey.getQualifiedDataName(), p);
        return startKey;
    }

    public Cobol visitStatementPhrase(Cobol.StatementPhrase statementPhrase, PrintOutputCapture<P> p) {
        visitSpace(statementPhrase.getPrefix(), p);
        visitMarkers(statementPhrase.getMarkers(), p);
        visit(statementPhrase.getPhrase(), p);
        visitContainer("", statementPhrase.getPadding().getStatement(), "", "", p);
        return statementPhrase;
    }

    public Cobol visitStop(Cobol.Stop stop, PrintOutputCapture<P> p) {
        visitSpace(stop.getPrefix(), p);
        visitMarkers(stop.getMarkers(), p);
        visit(stop.getWords(), p);
        visit(stop.getStatement(), p);
        return stop;
    }

    public Cobol visitStopStatementGiving(Cobol.StopStatementGiving stopStatementGiving, PrintOutputCapture<P> p) {
        visitSpace(stopStatementGiving.getPrefix(), p);
        visitMarkers(stopStatementGiving.getMarkers(), p);
        visit(stopStatementGiving.getWords(), p);
        visit(stopStatementGiving.getName(), p);
        return stopStatementGiving;
    }

    public Cobol visitStringDelimitedByPhrase(Cobol.StringDelimitedByPhrase stringDelimitedByPhrase, PrintOutputCapture<P> p) {
        visitSpace(stringDelimitedByPhrase.getPrefix(), p);
        visitMarkers(stringDelimitedByPhrase.getMarkers(), p);
        visit(stringDelimitedByPhrase.getWord(), p);
        visit(stringDelimitedByPhrase.getIdentifier(), p);
        return stringDelimitedByPhrase;
    }

    public Cobol visitStringForPhrase(Cobol.StringForPhrase stringForPhrase, PrintOutputCapture<P> p) {
        visitSpace(stringForPhrase.getPrefix(), p);
        visitMarkers(stringForPhrase.getMarkers(), p);
        visit(stringForPhrase.getWord(), p);
        visit(stringForPhrase.getIdentifier(), p);
        return stringForPhrase;
    }

    public Cobol visitStringIntoPhrase(Cobol.StringIntoPhrase stringIntoPhrase, PrintOutputCapture<P> p) {
        visitSpace(stringIntoPhrase.getPrefix(), p);
        visitMarkers(stringIntoPhrase.getMarkers(), p);
        visit(stringIntoPhrase.getInto(), p);
        visit(stringIntoPhrase.getIdentifier(), p);
        return stringIntoPhrase;
    }

    public Cobol visitStringSendingPhrase(Cobol.StringSendingPhrase stringSendingPhrase, PrintOutputCapture<P> p) {
        visitSpace(stringSendingPhrase.getPrefix(), p);
        visitMarkers(stringSendingPhrase.getMarkers(), p);
        for (Cobol s : stringSendingPhrase.getSendings()) {
            visit(s, p);
        }
        visit(stringSendingPhrase.getPhrase(), p);
        return stringSendingPhrase;
    }

    public Cobol visitStringStatement(Cobol.StringStatement stringStatement, PrintOutputCapture<P> p) {
        visitSpace(stringStatement.getPrefix(), p);
        visitMarkers(stringStatement.getMarkers(), p);
        visit(stringStatement.getString(), p);
        visitContainer("", stringStatement.getPadding().getStringSendingPhrases(), "", "", p);
        visit(stringStatement.getStringIntoPhrase(), p);
        visit(stringStatement.getStringWithPointerPhrase(), p);
        visit(stringStatement.getOnOverflowPhrase(), p);
        visit(stringStatement.getNotOnOverflowPhrase(), p);
        visit(stringStatement.getEndString(), p);
        return stringStatement;
    }

    public Cobol visitStringWithPointerPhrase(Cobol.StringWithPointerPhrase stringWithPointerPhrase, PrintOutputCapture<P> p) {
        visitSpace(stringWithPointerPhrase.getPrefix(), p);
        visitMarkers(stringWithPointerPhrase.getMarkers(), p);
        visit(stringWithPointerPhrase.getWords(), p);
        visit(stringWithPointerPhrase.getQualifiedDataName(), p);
        return stringWithPointerPhrase;
    }

    public Cobol visitSubscript(Cobol.Subscript subscript, PrintOutputCapture<P> p) {
        visitSpace(subscript.getPrefix(), p);
        visitMarkers(subscript.getMarkers(), p);
        visit(subscript.getFirst(), p);
        visit(subscript.getIntegerLiteral(), p);
        return subscript;
    }

    public Cobol visitSubtract(Cobol.Subtract subtract, PrintOutputCapture<P> p) {
        visitSpace(subtract.getPrefix(), p);
        visitMarkers(subtract.getMarkers(), p);
        visit(subtract.getSubstract(), p);
        visit(subtract.getOperation(), p);
        visit(subtract.getOnSizeErrorPhrase(), p);
        visit(subtract.getNotOnSizeErrorPhrase(), p);
        visit(subtract.getEndSubtract(), p);
        return subtract;
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

    public Cobol visitSubtractFromStatement(Cobol.SubtractFromStatement subtractFromStatement, PrintOutputCapture<P> p) {
        visitSpace(subtractFromStatement.getPrefix(), p);
        visitMarkers(subtractFromStatement.getMarkers(), p);
        visitContainer("", subtractFromStatement.getPadding().getSubtractSubtrahend(), "", "", p);
        visit(subtractFromStatement.getFrom(), p);
        visitContainer("", subtractFromStatement.getPadding().getSubtractMinuend(), "", "", p);
        return subtractFromStatement;
    }

    public Cobol visitSubtractMinuendCorresponding(Cobol.SubtractMinuendCorresponding subtractMinuendCorresponding, PrintOutputCapture<P> p) {
        visitSpace(subtractMinuendCorresponding.getPrefix(), p);
        visitMarkers(subtractMinuendCorresponding.getMarkers(), p);
        visit(subtractMinuendCorresponding.getQualifiedDataName(), p);
        visit(subtractMinuendCorresponding.getRounded(), p);
        return subtractMinuendCorresponding;
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
        visit(symbolicCharactersClause.getWords(), p);
        visitContainer("", symbolicCharactersClause.getPadding().getSymbols(), "", "", p);
        visit(symbolicCharactersClause.getInAlphabet(), p);
        visit(symbolicCharactersClause.getAlphabetName(), p);
        return symbolicCharactersClause;
    }

    public Cobol visitSymbolicDestinationClause(Cobol.SymbolicDestinationClause symbolicDestinationClause, PrintOutputCapture<P> p) {
        visitSpace(symbolicDestinationClause.getPrefix(), p);
        visitMarkers(symbolicDestinationClause.getMarkers(), p);
        visit(symbolicDestinationClause.getWords(), p);
        visit(symbolicDestinationClause.getDataDescName(), p);
        return symbolicDestinationClause;
    }

    public Cobol visitSymbolicQueueClause(Cobol.SymbolicQueueClause symbolicQueueClause, PrintOutputCapture<P> p) {
        visitSpace(symbolicQueueClause.getPrefix(), p);
        visitMarkers(symbolicQueueClause.getMarkers(), p);
        visit(symbolicQueueClause.getWords(), p);
        visit(symbolicQueueClause.getDataDescName(), p);
        return symbolicQueueClause;
    }

    public Cobol visitSymbolicSourceClause(Cobol.SymbolicSourceClause symbolicSourceClause, PrintOutputCapture<P> p) {
        visitSpace(symbolicSourceClause.getPrefix(), p);
        visitMarkers(symbolicSourceClause.getMarkers(), p);
        visit(symbolicSourceClause.getWords(), p);
        visit(symbolicSourceClause.getDataDescName(), p);
        return symbolicSourceClause;
    }

    public Cobol visitSymbolicSubQueueClause(Cobol.SymbolicSubQueueClause symbolicSubQueueClause, PrintOutputCapture<P> p) {
        visitSpace(symbolicSubQueueClause.getPrefix(), p);
        visitMarkers(symbolicSubQueueClause.getMarkers(), p);
        visit(symbolicSubQueueClause.getWords(), p);
        visit(symbolicSubQueueClause.getDataDescName(), p);
        return symbolicSubQueueClause;
    }

    public Cobol visitSymbolicTerminalClause(Cobol.SymbolicTerminalClause symbolicTerminalClause, PrintOutputCapture<P> p) {
        visitSpace(symbolicTerminalClause.getPrefix(), p);
        visitMarkers(symbolicTerminalClause.getMarkers(), p);
        visit(symbolicTerminalClause.getWords(), p);
        visit(symbolicTerminalClause.getDataDescName(), p);
        return symbolicTerminalClause;
    }

    public Cobol visitTableCall(Cobol.TableCall tableCall, PrintOutputCapture<P> p) {
        visitSpace(tableCall.getPrefix(), p);
        visitMarkers(tableCall.getMarkers(), p);
        visit(tableCall.getQualifiedDataName(), p);
        visitContainer("", tableCall.getPadding().getSubscripts(), "", "", p);
        visit(tableCall.getReferenceModifier(), p);
        return tableCall;
    }

    public Cobol visitTerminate(Cobol.Terminate terminate, PrintOutputCapture<P> p) {
        visitSpace(terminate.getPrefix(), p);
        visitMarkers(terminate.getMarkers(), p);
        visit(terminate.getTerminate(), p);
        visit(terminate.getReportName(), p);
        return terminate;
    }

    public Cobol visitTextLengthClause(Cobol.TextLengthClause textLengthClause, PrintOutputCapture<P> p) {
        visitSpace(textLengthClause.getPrefix(), p);
        visitMarkers(textLengthClause.getMarkers(), p);
        visit(textLengthClause.getWords(), p);
        visit(textLengthClause.getDataDescName(), p);
        return textLengthClause;
    }

    public Cobol visitUnString(Cobol.UnString unString, PrintOutputCapture<P> p) {
        visitSpace(unString.getPrefix(), p);
        visitMarkers(unString.getMarkers(), p);
        visit(unString.getUnstring(), p);
        visit(unString.getUnstringSendingPhrase(), p);
        visit(unString.getUnstringIntoPhrase(), p);
        visit(unString.getUnstringWithPointerPhrase(), p);
        visit(unString.getUnstringTallyingPhrase(), p);
        visit(unString.getOnOverflowPhrase(), p);
        visit(unString.getNotOnOverflowPhrase(), p);
        visit(unString.getEndUnstring(), p);
        return unString;
    }

    public Cobol visitUnstringCountIn(Cobol.UnstringCountIn unstringCountIn, PrintOutputCapture<P> p) {
        visitSpace(unstringCountIn.getPrefix(), p);
        visitMarkers(unstringCountIn.getMarkers(), p);
        visit(unstringCountIn.getWords(), p);
        visit(unstringCountIn.getIdentifier(), p);
        return unstringCountIn;
    }

    public Cobol visitUnstringDelimitedByPhrase(Cobol.UnstringDelimitedByPhrase unstringDelimitedByPhrase, PrintOutputCapture<P> p) {
        visitSpace(unstringDelimitedByPhrase.getPrefix(), p);
        visitMarkers(unstringDelimitedByPhrase.getMarkers(), p);
        visit(unstringDelimitedByPhrase.getWords(), p);
        visit(unstringDelimitedByPhrase.getName(), p);
        return unstringDelimitedByPhrase;
    }

    public Cobol visitUnstringDelimiterIn(Cobol.UnstringDelimiterIn unstringDelimiterIn, PrintOutputCapture<P> p) {
        visitSpace(unstringDelimiterIn.getPrefix(), p);
        visitMarkers(unstringDelimiterIn.getMarkers(), p);
        visit(unstringDelimiterIn.getWords(), p);
        visit(unstringDelimiterIn.getIdentifier(), p);
        return unstringDelimiterIn;
    }

    public Cobol visitUnstringInto(Cobol.UnstringInto unstringInto, PrintOutputCapture<P> p) {
        visitSpace(unstringInto.getPrefix(), p);
        visitMarkers(unstringInto.getMarkers(), p);
        visit(unstringInto.getIdentifier(), p);
        visit(unstringInto.getUnstringDelimiterIn(), p);
        visit(unstringInto.getUnstringCountIn(), p);
        return unstringInto;
    }

    public Cobol visitUnstringIntoPhrase(Cobol.UnstringIntoPhrase unstringIntoPhrase, PrintOutputCapture<P> p) {
        visitSpace(unstringIntoPhrase.getPrefix(), p);
        visitMarkers(unstringIntoPhrase.getMarkers(), p);
        visit(unstringIntoPhrase.getInto(), p);
        visitContainer("", unstringIntoPhrase.getPadding().getUnstringIntos(), "", "", p);
        return unstringIntoPhrase;
    }

    public Cobol visitUnstringOrAllPhrase(Cobol.UnstringOrAllPhrase unstringOrAllPhrase, PrintOutputCapture<P> p) {
        visitSpace(unstringOrAllPhrase.getPrefix(), p);
        visitMarkers(unstringOrAllPhrase.getMarkers(), p);
        visit(unstringOrAllPhrase.getWords(), p);
        visit(unstringOrAllPhrase.getName(), p);
        return unstringOrAllPhrase;
    }

    public Cobol visitUnstringSendingPhrase(Cobol.UnstringSendingPhrase unstringSendingPhrase, PrintOutputCapture<P> p) {
        visitSpace(unstringSendingPhrase.getPrefix(), p);
        visitMarkers(unstringSendingPhrase.getMarkers(), p);
        visit(unstringSendingPhrase.getIdentifier(), p);
        visit(unstringSendingPhrase.getUnstringDelimitedByPhrase(), p);
        visitContainer("", unstringSendingPhrase.getPadding().getUnstringOrAllPhrases(), "", "", p);
        return unstringSendingPhrase;
    }

    public Cobol visitUnstringTallyingPhrase(Cobol.UnstringTallyingPhrase unstringTallyingPhrase, PrintOutputCapture<P> p) {
        visitSpace(unstringTallyingPhrase.getPrefix(), p);
        visitMarkers(unstringTallyingPhrase.getMarkers(), p);
        visit(unstringTallyingPhrase.getWords(), p);
        visit(unstringTallyingPhrase.getQualifiedDataName(), p);
        return unstringTallyingPhrase;
    }

    public Cobol visitUnstringWithPointerPhrase(Cobol.UnstringWithPointerPhrase unstringWithPointerPhrase, PrintOutputCapture<P> p) {
        visitSpace(unstringWithPointerPhrase.getPrefix(), p);
        visitMarkers(unstringWithPointerPhrase.getMarkers(), p);
        visit(unstringWithPointerPhrase.getWords(), p);
        visit(unstringWithPointerPhrase.getQualifiedDataName(), p);
        return unstringWithPointerPhrase;
    }

    public Cobol visitUseAfterClause(Cobol.UseAfterClause useAfterClause, PrintOutputCapture<P> p) {
        visitSpace(useAfterClause.getPrefix(), p);
        visitMarkers(useAfterClause.getMarkers(), p);
        visit(useAfterClause.getWords(), p);
        visit(useAfterClause.getUseAfterOn(), p);
        return useAfterClause;
    }

    public Cobol visitUseAfterOn(Cobol.UseAfterOn useAfterOn, PrintOutputCapture<P> p) {
        visitSpace(useAfterOn.getPrefix(), p);
        visitMarkers(useAfterOn.getMarkers(), p);
        visit(useAfterOn.getAfterOn(), p);
        visitContainer("", useAfterOn.getPadding().getFileNames(), "", "", p);
        return useAfterOn;
    }

    public Cobol visitUseDebugClause(Cobol.UseDebugClause useDebugClause, PrintOutputCapture<P> p) {
        visitSpace(useDebugClause.getPrefix(), p);
        visitMarkers(useDebugClause.getMarkers(), p);
        visit(useDebugClause.getWords(), p);
        visitContainer("", useDebugClause.getPadding().getUseDebugs(), "", "", p);
        return useDebugClause;
    }

    public Cobol visitUseDebugOn(Cobol.UseDebugOn useDebugOn, PrintOutputCapture<P> p) {
        visitSpace(useDebugOn.getPrefix(), p);
        visitMarkers(useDebugOn.getMarkers(), p);
        visit(useDebugOn.getWords(), p);
        visit(useDebugOn.getProcedureName(), p);
        return useDebugOn;
    }

    public Cobol visitUseStatement(Cobol.UseStatement useStatement, PrintOutputCapture<P> p) {
        visitSpace(useStatement.getPrefix(), p);
        visitMarkers(useStatement.getMarkers(), p);
        visit(useStatement.getUse(), p);
        visit(useStatement.getClause(), p);
        return useStatement;
    }

    public Cobol visitValueOfClause(Cobol.ValueOfClause valueOfClause, PrintOutputCapture<P> p) {
        visitSpace(valueOfClause.getPrefix(), p);
        visitMarkers(valueOfClause.getMarkers(), p);
        visit(valueOfClause.getValueOf(), p);
        visitContainer("", valueOfClause.getPadding().getValuePairs(), "", "", p);
        return valueOfClause;
    }

    public Cobol visitValuePair(Cobol.ValuePair valuePair, PrintOutputCapture<P> p) {
        visitSpace(valuePair.getPrefix(), p);
        visitMarkers(valuePair.getMarkers(), p);
        visit(valuePair.getSystemName(), p);
        visit(valuePair.getIs(), p);
        visit(valuePair.getName(), p);
        return valuePair;
    }

    public Cobol visitValuedObjectComputerClause(Cobol.ValuedObjectComputerClause valuedObjectComputerClause, PrintOutputCapture<P> p) {
        visitSpace(valuedObjectComputerClause.getPrefix(), p);
        visitMarkers(valuedObjectComputerClause.getMarkers(), p);
        visit(valuedObjectComputerClause.getWords(), p);
        visit(valuedObjectComputerClause.getValue(), p);
        visit(valuedObjectComputerClause.getUnits(), p);
        return valuedObjectComputerClause;
    }

    public Cobol visitWorkingStorageSection(Cobol.WorkingStorageSection workingStorageSection, PrintOutputCapture<P> p) {
        visitSpace(workingStorageSection.getPrefix(), p);
        visitMarkers(workingStorageSection.getMarkers(), p);
        visit(workingStorageSection.getWords(), p);
        visitContainer(".", workingStorageSection.getPadding().getDataDescriptions(), "", "", p);
        return workingStorageSection;
    }

    public Cobol visitWrite(Cobol.Write write, PrintOutputCapture<P> p) {
        visitSpace(write.getPrefix(), p);
        visitMarkers(write.getMarkers(), p);
        visit(write.getWrite(), p);
        visit(write.getRecordName(), p);
        visit(write.getWriteFromPhrase(), p);
        visit(write.getWriteAdvancingPhrase(), p);
        visit(write.getWriteAtEndOfPagePhrase(), p);
        visit(write.getWriteNotAtEndOfPagePhrase(), p);
        visit(write.getInvalidKeyPhrase(), p);
        visit(write.getNotInvalidKeyPhrase(), p);
        visit(write.getEndWrite(), p);
        return write;
    }

    public Cobol visitWriteAdvancingLines(Cobol.WriteAdvancingLines writeAdvancingLines, PrintOutputCapture<P> p) {
        visitSpace(writeAdvancingLines.getPrefix(), p);
        visitMarkers(writeAdvancingLines.getMarkers(), p);
        visit(writeAdvancingLines.getName(), p);
        visit(writeAdvancingLines.getWords(), p);
        return writeAdvancingLines;
    }

    public Cobol visitWriteAdvancingMnemonic(Cobol.WriteAdvancingMnemonic writeAdvancingMnemonic, PrintOutputCapture<P> p) {
        visitSpace(writeAdvancingMnemonic.getPrefix(), p);
        visitMarkers(writeAdvancingMnemonic.getMarkers(), p);
        visit(writeAdvancingMnemonic.getName(), p);
        return writeAdvancingMnemonic;
    }

    public Cobol visitWriteAdvancingPage(Cobol.WriteAdvancingPage writeAdvancingPage, PrintOutputCapture<P> p) {
        visitSpace(writeAdvancingPage.getPrefix(), p);
        visitMarkers(writeAdvancingPage.getMarkers(), p);
        visit(writeAdvancingPage.getPage(), p);
        return writeAdvancingPage;
    }

    public Cobol visitWriteAdvancingPhrase(Cobol.WriteAdvancingPhrase writeAdvancingPhrase, PrintOutputCapture<P> p) {
        visitSpace(writeAdvancingPhrase.getPrefix(), p);
        visitMarkers(writeAdvancingPhrase.getMarkers(), p);
        visit(writeAdvancingPhrase.getWords(), p);
        visit(writeAdvancingPhrase.getWriteBy(), p);
        return writeAdvancingPhrase;
    }

    public Cobol visitWriteFromPhrase(Cobol.WriteFromPhrase writeFromPhrase, PrintOutputCapture<P> p) {
        visitSpace(writeFromPhrase.getPrefix(), p);
        visitMarkers(writeFromPhrase.getMarkers(), p);
        visit(writeFromPhrase.getFrom(), p);
        visit(writeFromPhrase.getName(), p);
        return writeFromPhrase;
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
}
