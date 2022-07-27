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

import org.openrewrite.cobol.tree.Cobol;
import org.openrewrite.internal.lang.Nullable;

public class CobolIsoVisitor<P> extends CobolVisitor<P> {

    @Override
    public Cobol.CompilationUnit visitCompilationUnit(Cobol.CompilationUnit compilationUnit, P p) {
        return (Cobol.CompilationUnit) super.visitCompilationUnit(compilationUnit, p);
    }

    @Override
    public Cobol.Display visitDisplay(Cobol.Display display, P p) {
        return (Cobol.Display) super.visitDisplay(display, p);
    }

    @Override
    public Cobol.Identifier visitIdentifier(Cobol.Identifier identifier, P p) {
        return (Cobol.Identifier) super.visitIdentifier(identifier, p);
    }

    @Override
    public Cobol.Literal visitLiteral(Cobol.Literal literal, P p) {
        return (Cobol.Literal) super.visitLiteral(literal, p);
    }

    @Override
    public Cobol.IdentificationDivision visitIdentificationDivision(Cobol.IdentificationDivision identificationDivision, P p) {
        return (Cobol.IdentificationDivision) super.visitIdentificationDivision(identificationDivision, p);
    }

    @Override
    public Cobol.ProcedureDivision visitProcedureDivision(Cobol.ProcedureDivision procedureDivision, P p) {
        return (Cobol.ProcedureDivision) super.visitProcedureDivision(procedureDivision, p);
    }

    @Override
    public Cobol.ProcedureDivisionBody visitProcedureDivisionBody(Cobol.ProcedureDivisionBody procedureDivisionBody, P p) {
        return (Cobol.ProcedureDivisionBody) super.visitProcedureDivisionBody(procedureDivisionBody, p);
    }

    @Override
    public Cobol.Paragraphs visitParagraphs(Cobol.Paragraphs paragraphs, P p) {
        return (Cobol.Paragraphs) super.visitParagraphs(paragraphs, p);
    }

    @Override
    public Cobol.Paragraph visitParagraph(Cobol.Paragraph paragraph, P p) {
        return (Cobol.Paragraph) super.visitParagraph(paragraph, p);
    }

    @Override
    public Cobol.Sentence visitSentence(Cobol.Sentence sentence, P p) {
        return (Cobol.Sentence) super.visitSentence(sentence, p);
    }

    @Override
    public Cobol.ProgramIdParagraph visitProgramIdParagraph(Cobol.ProgramIdParagraph programIdParagraph, P p) {
        return (Cobol.ProgramIdParagraph) super.visitProgramIdParagraph(programIdParagraph, p);
    }

    @Override
    public Cobol.ProgramUnit visitProgramUnit(Cobol.ProgramUnit programUnit, P p) {
        return (Cobol.ProgramUnit) super.visitProgramUnit(programUnit, p);
    }

    @Override
    public Cobol.Stop visitStop(Cobol.Stop stop, P p) {
        return (Cobol.Stop) super.visitStop(stop, p);
    }

    @Override
    public Cobol.DataDivision visitDataDivision(Cobol.DataDivision dataDivision, P p) {
        return (Cobol.DataDivision) super.visitDataDivision(dataDivision, p);
    }

    @Override
    public Cobol.DataDescriptionEntry visitDataDescriptionEntry(Cobol.DataDescriptionEntry dataDescriptionEntry, P p) {
        return (Cobol.DataDescriptionEntry) super.visitDataDescriptionEntry(dataDescriptionEntry, p);
    }

    @Override
    public Cobol.DataPictureClause visitDataPictureClause(Cobol.DataPictureClause dataPictureClause, P p) {
        return (Cobol.DataPictureClause) super.visitDataPictureClause(dataPictureClause, p);
    }

    @Override
    public Cobol.Picture visitPicture(Cobol.Picture picture, P p) {
        return (Cobol.Picture) super.visitPicture(picture, p);
    }

    @Override
    public Cobol.WorkingStorageSection visitWorkingStorageSection(Cobol.WorkingStorageSection workingStorageSection, P p) {
        return (Cobol.WorkingStorageSection) super.visitWorkingStorageSection(workingStorageSection, p);
    }

    @Override
    public Cobol.EndProgram visitEndProgram(Cobol.EndProgram endProgram, P p) {
        return (Cobol.EndProgram) super.visitEndProgram(endProgram, p);
    }

    @Override
    public Cobol.EnvironmentDivision visitEnvironmentDivision(Cobol.EnvironmentDivision environmentDivision, P p) {
        return (Cobol.EnvironmentDivision) super.visitEnvironmentDivision(environmentDivision, p);
    }

    @Override
    public Cobol.Set visitSet(Cobol.Set set, P p) {
        return (Cobol.Set) super.visitSet(set, p);
    }

    @Override
    public Cobol.SetTo visitSetTo(Cobol.SetTo setTo, P p) {
        return (Cobol.SetTo) super.visitSetTo(setTo, p);
    }

    @Override
    public Cobol.SetUpDown visitSetUpDown(Cobol.SetUpDown setUpDown, P p) {
        return (Cobol.SetUpDown) super.visitSetUpDown(setUpDown, p);
    }

    @Override
    public Cobol.Add visitAdd(Cobol.Add add, P p) {
        return (Cobol.Add) super.visitAdd(add, p);
    }

    @Override
    public Cobol.AddTo visitAddTo(Cobol.AddTo addTo, P p) {
        return (Cobol.AddTo) super.visitAddTo(addTo, p);
    }

    @Override
    public Cobol.StatementPhrase visitStatementPhrase(Cobol.StatementPhrase statementPhrase, P p) {
        return (Cobol.StatementPhrase) super.visitStatementPhrase(statementPhrase, p);
    }

    @Override
    public Cobol.Roundable visitRoundable(Cobol.Roundable roundable, P p) {
        return (Cobol.Roundable) super.visitRoundable(roundable, p);
    }

    @Override
    public Cobol.ConfigurationSection visitConfigurationSection(Cobol.ConfigurationSection configurationSection, P p) {
        return (Cobol.ConfigurationSection) super.visitConfigurationSection(configurationSection, p);
    }

    @Override
    public Cobol.SourceComputer visitSourceComputer(Cobol.SourceComputer sourceComputer, P p) {
        return (Cobol.SourceComputer) super.visitSourceComputer(sourceComputer, p);
    }

    @Override
    public Cobol.SourceComputerDefinition visitSourceComputerDefinition(Cobol.SourceComputerDefinition sourceComputerDefinition, P p) {
        return (Cobol.SourceComputerDefinition) super.visitSourceComputerDefinition(sourceComputerDefinition, p);
    }

    @Override
    public Cobol.ObjectComputer visitObjectComputer(Cobol.ObjectComputer objectComputer, P p) {
        return (Cobol.ObjectComputer) super.visitObjectComputer(objectComputer, p);
    }

    @Override
    public Cobol.ObjectComputerDefinition visitObjectComputerDefinition(Cobol.ObjectComputerDefinition objectComputerDefinition, P p) {
        return (Cobol.ObjectComputerDefinition) super.visitObjectComputerDefinition(objectComputerDefinition, p);
    }

    @Override
    public Cobol.ValuedObjectComputerClause visitValuedObjectComputerClause(Cobol.ValuedObjectComputerClause valuedObjectComputerClause, P p) {
        return (Cobol.ValuedObjectComputerClause) super.visitValuedObjectComputerClause(valuedObjectComputerClause, p);
    }

    @Override
    public Cobol.CollatingSequenceClause visitCollatingSequenceClause(Cobol.CollatingSequenceClause collatingSequenceClause, P p) {
        return (Cobol.CollatingSequenceClause) super.visitCollatingSequenceClause(collatingSequenceClause, p);
    }

    @Override
    public Cobol.CollatingSequenceAlphabet visitCollatingSequenceAlphabet(Cobol.CollatingSequenceAlphabet collatingSequenceAlphabet, P p) {
        return (Cobol.CollatingSequenceAlphabet) super.visitCollatingSequenceAlphabet(collatingSequenceAlphabet, p);
    }

    @Override
    public Cobol.AlphabetClause visitAlphabetClause(Cobol.AlphabetClause alphabetClause, P p) {
        return (Cobol.AlphabetClause) super.visitAlphabetClause(alphabetClause, p);
    }

    @Override
    public Cobol.AlphabetLiteral visitAlphabetLiteral(Cobol.AlphabetLiteral alphabetLiteral, P p) {
        return (Cobol.AlphabetLiteral) super.visitAlphabetLiteral(alphabetLiteral, p);
    }

    @Override
    public Cobol.AlphabetThrough visitAlphabetThrough(Cobol.AlphabetThrough alphabetThrough, P p) {
        return (Cobol.AlphabetThrough) super.visitAlphabetThrough(alphabetThrough, p);
    }

    @Override
    public Cobol.AlphabetAlso visitAlphabetAlso(Cobol.AlphabetAlso alphabetAlso, P p) {
        return (Cobol.AlphabetAlso) super.visitAlphabetAlso(alphabetAlso, p);
    }

    @Override
    public Cobol.SpecialNames visitSpecialNames(Cobol.SpecialNames specialNames, P p) {
        return (Cobol.SpecialNames) super.visitSpecialNames(specialNames, p);
    }

    @Override
    public Cobol.ChannelClause visitChannelClause(Cobol.ChannelClause channelClause, P p) {
        return (Cobol.ChannelClause) super.visitChannelClause(channelClause, p);
    }

    @Override
    public Cobol.CurrencyClause visitCurrencyClause(Cobol.CurrencyClause currencyClause, P p) {
        return (Cobol.CurrencyClause) super.visitCurrencyClause(currencyClause, p);
    }

    @Override
    public Cobol.DecimalPointClause visitDecimalPointClause(Cobol.DecimalPointClause decimalPointClause, P p) {
        return (Cobol.DecimalPointClause) super.visitDecimalPointClause(decimalPointClause, p);
    }

    @Override
    public Cobol.DefaultComputationalSignClause visitDefaultComputationalSignClause(Cobol.DefaultComputationalSignClause defaultComputationalSignClause, P p) {
        return (Cobol.DefaultComputationalSignClause) super.visitDefaultComputationalSignClause(defaultComputationalSignClause, p);
    }

    @Override
    public Cobol.DefaultDisplaySignClause visitDefaultDisplaySignClause(Cobol.DefaultDisplaySignClause defaultDisplaySignClause, P p) {
        return (Cobol.DefaultDisplaySignClause) super.visitDefaultDisplaySignClause(defaultDisplaySignClause, p);
    }

    @Override
    public Cobol.ClassClause visitClassClause(Cobol.ClassClause classClause, P p) {
        return (Cobol.ClassClause) super.visitClassClause(classClause, p);
    }

    @Override
    public Cobol.ClassClauseThrough visitClassClauseThrough(Cobol.ClassClauseThrough classClauseThrough, P p) {
        return (Cobol.ClassClauseThrough) super.visitClassClauseThrough(classClauseThrough, p);
    }

    @Override
    public Cobol.OdtClause visitOdtClause(Cobol.OdtClause odtClause, P p) {
        return (Cobol.OdtClause) super.visitOdtClause(odtClause, p);
    }

    @Override
    public Cobol.ReserveNetworkClause visitReserveNetworkClause(Cobol.ReserveNetworkClause reserveNetworkClause, P p) {
        return (Cobol.ReserveNetworkClause) super.visitReserveNetworkClause(reserveNetworkClause, p);
    }

    @Override
    public Cobol.SymbolicCharacter visitSymbolicCharacter(Cobol.SymbolicCharacter symbolicCharacter, P p) {
        return (Cobol.SymbolicCharacter) super.visitSymbolicCharacter(symbolicCharacter, p);
    }

    @Override
    public Cobol.SymbolicCharactersClause visitSymbolicCharactersClause(Cobol.SymbolicCharactersClause symbolicCharactersClause, P p) {
        return (Cobol.SymbolicCharactersClause) super.visitSymbolicCharactersClause(symbolicCharactersClause, p);
    }

    @Override
    public Cobol.FileSection visitFileSection(Cobol.FileSection fileSection, P p) {
        return (Cobol.FileSection) super.visitFileSection(fileSection, p);
    }

    @Override
    public Cobol.FileDescriptionEntry visitFileDescriptionEntry(Cobol.FileDescriptionEntry fileDescriptionEntry, P p) {
        return (Cobol.FileDescriptionEntry) super.visitFileDescriptionEntry(fileDescriptionEntry, p);
    }

    @Override
    public Cobol.LinkageSection visitLinkageSection(Cobol.LinkageSection linkageSection, P p) {
        return (Cobol.LinkageSection) super.visitLinkageSection(linkageSection, p);
    }

    @Override
    public Cobol.LocalStorageSection visitLocalStorageSection(Cobol.LocalStorageSection localStorageSection, P p) {
        return (Cobol.LocalStorageSection) super.visitLocalStorageSection(localStorageSection, p);
    }

    @Override
    public Cobol.DataBaseSection visitDataBaseSection(Cobol.DataBaseSection dataBaseSection, P p) {
        return (Cobol.DataBaseSection) super.visitDataBaseSection(dataBaseSection, p);
    }

    @Override
    public Cobol.DataBaseSectionEntry visitDataBaseSectionEntry(Cobol.DataBaseSectionEntry dataBaseSectionEntry, P p) {
        return (Cobol.DataBaseSectionEntry) super.visitDataBaseSectionEntry(dataBaseSectionEntry, p);
    }

    @Override
    public Cobol.ProcedureDivisionUsingClause visitProcedureDivisionUsingClause(Cobol.ProcedureDivisionUsingClause procedureDivisionUsingClause, P p) {
        return (Cobol.ProcedureDivisionUsingClause) super.visitProcedureDivisionUsingClause(procedureDivisionUsingClause, p);
    }

    @Override
    public Cobol.ProcedureDivisionByReferencePhrase visitProcedureDivisionByReferencePhrase(Cobol.ProcedureDivisionByReferencePhrase procedureDivisionByReferencePhrase, P p) {
        return (Cobol.ProcedureDivisionByReferencePhrase) super.visitProcedureDivisionByReferencePhrase(procedureDivisionByReferencePhrase, p);
    }

    @Override
    public Cobol.ProcedureDivisionByReference visitProcedureDivisionByReference(Cobol.ProcedureDivisionByReference procedureDivisionByReference, P p) {
        return (Cobol.ProcedureDivisionByReference) super.visitProcedureDivisionByReference(procedureDivisionByReference, p);
    }

    @Override
    public Cobol.ProcedureDivisionByValuePhrase visitProcedureDivisionByValuePhrase(Cobol.ProcedureDivisionByValuePhrase procedureDivisionByValuePhrase, P p) {
        return (Cobol.ProcedureDivisionByValuePhrase) super.visitProcedureDivisionByValuePhrase(procedureDivisionByValuePhrase, p);
    }

    @Override
    public Cobol.ScreenSection visitScreenSection(Cobol.ScreenSection screenSection, P p) {
        return (Cobol.ScreenSection) super.visitScreenSection(screenSection, p);
    }

    @Override
    public Cobol.ScreenDescriptionEntry visitScreenDescriptionEntry(Cobol.ScreenDescriptionEntry screenDescriptionEntry, P p) {
        return (Cobol.ScreenDescriptionEntry) super.visitScreenDescriptionEntry(screenDescriptionEntry, p);
    }

    @Override
    public Cobol.ScreenDescriptionBlankClause visitScreenDescriptionBlankClause(Cobol.ScreenDescriptionBlankClause screenDescriptionBlankClause, P p) {
        return (Cobol.ScreenDescriptionBlankClause) super.visitScreenDescriptionBlankClause(screenDescriptionBlankClause, p);
    }

    @Override
    public Cobol.ScreenDescriptionControlClause visitScreenDescriptionControlClause(Cobol.ScreenDescriptionControlClause screenDescriptionControlClause, P p) {
        return (Cobol.ScreenDescriptionControlClause) super.visitScreenDescriptionControlClause(screenDescriptionControlClause, p);
    }

    @Override
    public Cobol.ScreenDescriptionSizeClause visitScreenDescriptionSizeClause(Cobol.ScreenDescriptionSizeClause screenDescriptionSizeClause, P p) {
        return (Cobol.ScreenDescriptionSizeClause) super.visitScreenDescriptionSizeClause(screenDescriptionSizeClause, p);
    }

    @Override
    public Cobol.ScreenDescriptionToClause visitScreenDescriptionToClause(Cobol.ScreenDescriptionToClause screenDescriptionToClause, P p) {
        return (Cobol.ScreenDescriptionToClause) super.visitScreenDescriptionToClause(screenDescriptionToClause, p);
    }

    @Override
    public Cobol.ScreenDescriptionUsingClause visitScreenDescriptionUsingClause(Cobol.ScreenDescriptionUsingClause screenDescriptionUsingClause, P p) {
        return (Cobol.ScreenDescriptionUsingClause) super.visitScreenDescriptionUsingClause(screenDescriptionUsingClause, p);
    }

    @Override
    public Cobol.Accept visitAccept(Cobol.Accept accept, P p) {
        return (Cobol.Accept) super.visitAccept(accept, p);
    }

    @Override
    public Cobol.AcceptFromDateStatement visitAcceptFromDateStatement(Cobol.AcceptFromDateStatement acceptFromDateStatement, P p) {
        return (Cobol.AcceptFromDateStatement) super.visitAcceptFromDateStatement(acceptFromDateStatement, p);
    }

    @Override
    public Cobol.AcceptFromMnemonicStatement visitAcceptFromMnemonicStatement(Cobol.AcceptFromMnemonicStatement acceptFromMnemonicStatement, P p) {
        return (Cobol.AcceptFromMnemonicStatement) super.visitAcceptFromMnemonicStatement(acceptFromMnemonicStatement, p);
    }

    @Override
    public Cobol.AcceptFromEscapeKeyStatement visitAcceptFromEscapeKeyStatement(Cobol.AcceptFromEscapeKeyStatement acceptFromEscapeKeyStatement, P p) {
        return (Cobol.AcceptFromEscapeKeyStatement) super.visitAcceptFromEscapeKeyStatement(acceptFromEscapeKeyStatement, p);
    }

    @Override
    public Cobol.AcceptMessageCountStatement visitAcceptMessageCountStatement(Cobol.AcceptMessageCountStatement acceptMessageCountStatement, P p) {
        return (Cobol.AcceptMessageCountStatement) super.visitAcceptMessageCountStatement(acceptMessageCountStatement, p);
    }

    @Override
    public Cobol.AlterStatement visitAlterStatement(Cobol.AlterStatement alterStatement, P p) {
        return (Cobol.AlterStatement) super.visitAlterStatement(alterStatement, p);
    }

    @Override
    public Cobol.AlterProceedTo visitAlterProceedTo(Cobol.AlterProceedTo alterProceedTo, P p) {
        return (Cobol.AlterProceedTo) super.visitAlterProceedTo(alterProceedTo, p);
    }

    @Override
    public Cobol.ProcedureName visitProcedureName(Cobol.ProcedureName procedureName, P p) {
        return (Cobol.ProcedureName) super.visitProcedureName(procedureName, p);
    }

    @Override
    public Cobol.InSection visitInSection(Cobol.InSection inSection, P p) {
        return (Cobol.InSection) super.visitInSection(inSection, p);
    }

    @Override
    public Cobol.Cancel visitCancel(Cobol.Cancel cancel, P p) {
        return (Cobol.Cancel) super.visitCancel(cancel, p);
    }

    @Override
    public Cobol.CancelCall visitCancelCall(Cobol.CancelCall cancelCall, P p) {
        return (Cobol.CancelCall) super.visitCancelCall(cancelCall, p);
    }

    @Override
    public Cobol.Close visitClose(Cobol.Close close, P p) {
        return (Cobol.Close) super.visitClose(close, p);
    }

    @Override
    public Cobol.CloseFile visitCloseFile(Cobol.CloseFile closeFile, P p) {
        return (Cobol.CloseFile) super.visitCloseFile(closeFile, p);
    }

    @Override
    public Cobol.CloseReelUnitStatement visitCloseReelUnitStatement(Cobol.CloseReelUnitStatement closeReelUnitStatement, P p) {
        return (Cobol.CloseReelUnitStatement) super.visitCloseReelUnitStatement(closeReelUnitStatement, p);
    }

    @Override
    public Cobol.CloseRelativeStatement visitCloseRelativeStatement(Cobol.CloseRelativeStatement closeRelativeStatement, P p) {
        return (Cobol.CloseRelativeStatement) super.visitCloseRelativeStatement(closeRelativeStatement, p);
    }

    @Override
    public Cobol.ClosePortFileIOStatement visitClosePortFileIOStatement(Cobol.ClosePortFileIOStatement closePortFileIOStatement, P p) {
        return (Cobol.ClosePortFileIOStatement) super.visitClosePortFileIOStatement(closePortFileIOStatement, p);
    }

    @Override
    public Cobol.ClosePortFileIOUsingCloseDisposition visitClosePortFileIOUsingCloseDisposition(Cobol.ClosePortFileIOUsingCloseDisposition closePortFileIOUsingCloseDisposition, P p) {
        return (Cobol.ClosePortFileIOUsingCloseDisposition) super.visitClosePortFileIOUsingCloseDisposition(closePortFileIOUsingCloseDisposition, p);
    }

    @Override
    public Cobol.ClosePortFileIOUsingAssociatedData visitClosePortFileIOUsingAssociatedData(Cobol.ClosePortFileIOUsingAssociatedData closePortFileIOUsingAssociatedData, P p) {
        return (Cobol.ClosePortFileIOUsingAssociatedData) super.visitClosePortFileIOUsingAssociatedData(closePortFileIOUsingAssociatedData, p);
    }

    @Override
    public Cobol.ClosePortFileIOUsingAssociatedDataLength visitClosePortFileIOUsingAssociatedDataLength(Cobol.ClosePortFileIOUsingAssociatedDataLength closePortFileIOUsingAssociatedDataLength, P p) {
        return (Cobol.ClosePortFileIOUsingAssociatedDataLength) super.visitClosePortFileIOUsingAssociatedDataLength(closePortFileIOUsingAssociatedDataLength, p);
    }

    @Override
    public Cobol.InData visitInData(Cobol.InData inData, P p) {
        return (Cobol.InData) super.visitInData(inData, p);
    }

    @Override
    public Cobol.InFile visitInFile(Cobol.InFile inFile, P p) {
        return (Cobol.InFile) super.visitInFile(inFile, p);
    }

    @Override
    public Cobol.InMnemonic visitInMnemonic(Cobol.InMnemonic inMnemonic, P p) {
        return (Cobol.InMnemonic) super.visitInMnemonic(inMnemonic, p);
    }

    @Override
    public Cobol.InLibrary visitInLibrary(Cobol.InLibrary inLibrary, P p) {
        return (Cobol.InLibrary) super.visitInLibrary(inLibrary, p);
    }

    @Override
    public Cobol.InTable visitInTable(Cobol.InTable inTable, P p) {
        return (Cobol.InTable) super.visitInTable(inTable, p);
    }

    @Override
    public Cobol.Continue visitContinue(Cobol.Continue continuez, P p) {
        return (Cobol.Continue) super.visitContinue(continuez, p);
    }

    @Override
    public Cobol.Delete visitDelete(Cobol.Delete delete, P p) {
        return (Cobol.Delete) super.visitDelete(delete, p);
    }

    @Override
    public Cobol.Disable visitDisable(Cobol.Disable disable, P p) {
        return (Cobol.Disable) super.visitDisable(disable, p);
    }

    @Override
    public Cobol.Enable visitEnable(Cobol.Enable enable, P p) {
        return (Cobol.Enable) super.visitEnable(enable, p);
    }

    @Override
    public Cobol.Entry visitEntry(Cobol.Entry entry, P p) {
        return super.visitEntry(entry, p);
    }

    @Override
    public Cobol.Exhibit visitExhibit(Cobol.Exhibit exhibit, P p) {
        return (Cobol.Exhibit) super.visitExhibit(exhibit, p);
    }

    @Override
    public Cobol.Exit visitExit(Cobol.Exit exit, P p) {
        return (Cobol.Exit) super.visitExit(exit, p);
    }

    @Override
    public Cobol.Generate visitGenerate(Cobol.Generate generate, P p) {
        return (Cobol.Generate) super.visitGenerate(generate, p);
    }

    @Override
    public Cobol.QualifiedDataName visitQualifiedDataName(Cobol.QualifiedDataName qualifiedDataName, P p) {
        return (Cobol.QualifiedDataName) super.visitQualifiedDataName(qualifiedDataName, p);
    }

    @Override
    public Cobol.QualifiedDataNameFormat1 visitQualifiedDataNameFormat1(Cobol.QualifiedDataNameFormat1 qualifiedDataNameFormat1, P p) {
        return (Cobol.QualifiedDataNameFormat1) super.visitQualifiedDataNameFormat1(qualifiedDataNameFormat1, p);
    }

    @Override
    public Cobol.QualifiedDataNameFormat2 visitQualifiedDataNameFormat2(Cobol.QualifiedDataNameFormat2 qualifiedDataNameFormat2, P p) {
        return (Cobol.QualifiedDataNameFormat2) super.visitQualifiedDataNameFormat2(qualifiedDataNameFormat2, p);
    }

    @Override
    public Cobol.QualifiedDataNameFormat3 visitQualifiedDataNameFormat3(Cobol.QualifiedDataNameFormat3 qualifiedDataNameFormat3, P p) {
        return (Cobol.QualifiedDataNameFormat3) super.visitQualifiedDataNameFormat3(qualifiedDataNameFormat3, p);
    }

    @Override
    public Cobol.QualifiedDataNameFormat4 visitQualifiedDataNameFormat4(Cobol.QualifiedDataNameFormat4 qualifiedDataNameFormat4, P p) {
        return (Cobol.QualifiedDataNameFormat4) super.visitQualifiedDataNameFormat4(qualifiedDataNameFormat4, p);
    }

    @Override
    public Cobol.QualifiedInData visitQualifiedInData(Cobol.QualifiedInData qualifiedInData, P p) {
        return (Cobol.QualifiedInData) super.visitQualifiedInData(qualifiedInData, p);
    }

    @Override
    public Cobol.ReportName visitReportName(Cobol.ReportName reportName, P p) {
        return (Cobol.ReportName) super.visitReportName(reportName, p);
    }

    @Override
    public Cobol.AlteredGoTo visitAlteredGoTo(Cobol.AlteredGoTo alteredGoTo, P p) {
        return (Cobol.AlteredGoTo) super.visitAlteredGoTo(alteredGoTo, p);
    }

    @Override
    public Cobol.ProcedureDeclaratives visitProcedureDeclaratives(Cobol.ProcedureDeclaratives procedureDeclaratives, P p) {
        return (Cobol.ProcedureDeclaratives) super.visitProcedureDeclaratives(procedureDeclaratives, p);
    }

    @Override
    public Cobol.ProcedureDeclarative visitProcedureDeclarative(Cobol.ProcedureDeclarative procedureDeclarative, P p) {
        return (Cobol.ProcedureDeclarative) super.visitProcedureDeclarative(procedureDeclarative, p);
    }

    @Override
    public Cobol.ProcedureSection visitProcedureSection(Cobol.ProcedureSection procedureSection, P p) {
        return (Cobol.ProcedureSection) super.visitProcedureSection(procedureSection, p);
    }

    @Override
    public Cobol.ProcedureSectionHeader visitProcedureSectionHeader(Cobol.ProcedureSectionHeader procedureSectionHeader, P p) {
        return (Cobol.ProcedureSectionHeader) super.visitProcedureSectionHeader(procedureSectionHeader, p);
    }

    @Override
    public Cobol.ProcedureDivisionGivingClause visitProcedureDivisionGivingClause(Cobol.ProcedureDivisionGivingClause procedureDivisionGivingClause, P p) {
        return (Cobol.ProcedureDivisionGivingClause) super.visitProcedureDivisionGivingClause(procedureDivisionGivingClause, p);
    }

    @Override
    public Cobol.UseStatement visitUseStatement(Cobol.UseStatement useStatement, P p) {
        return (Cobol.UseStatement) super.visitUseStatement(useStatement, p);
    }

    @Override
    public Cobol.UseAfterClause visitUseAfterClause(Cobol.UseAfterClause useAfterClause, P p) {
        return (Cobol.UseAfterClause) super.visitUseAfterClause(useAfterClause, p);
    }

    @Override
    public Cobol.UseAfterOn visitUseAfterOn(Cobol.UseAfterOn useAfterOn, P p) {
        return (Cobol.UseAfterOn) super.visitUseAfterOn(useAfterOn, p);
    }

    @Override
    public Cobol.UseDebugClause visitUseDebugClause(Cobol.UseDebugClause useDebugClause, P p) {
        return (Cobol.UseDebugClause) super.visitUseDebugClause(useDebugClause, p);
    }

    @Override
    public Cobol.UseDebugOn visitUseDebugOn(Cobol.UseDebugOn useDebugOn, P p) {
        return (Cobol.UseDebugOn) super.visitUseDebugOn(useDebugOn, p);
    }

    @Override
    public Cobol.Rewrite visitRewrite(Cobol.Rewrite rewrite, P p) {
        return (Cobol.Rewrite) super.visitRewrite(rewrite, p);
    }

    @Override
    public Cobol.RewriteFrom visitRewriteFrom(Cobol.RewriteFrom rewriteFrom, P p) {
        return (Cobol.RewriteFrom) super.visitRewriteFrom(rewriteFrom, p);
    }

    public Cobol.Call visitCall(Cobol.Call call, P p) {
        return (Cobol.Call) super.visitCall(call, p);
    }

    @Override
    public Cobol.CallPhrase visitCallPhrase(Cobol.CallPhrase callPhrase, P p) {
        return (Cobol.CallPhrase) super.visitCallPhrase(callPhrase, p);
    }

    @Override
    public Cobol.CallBy visitCallBy(Cobol.CallBy callBy, P p) {
        return (Cobol.CallBy) super.visitCallBy(callBy, p);
    }

    @Override
    public Cobol.CallGivingPhrase visitCallGivingPhrase(Cobol.CallGivingPhrase callGivingPhrase, P p) {
        return (Cobol.CallGivingPhrase) super.visitCallGivingPhrase(callGivingPhrase, p);
    }

    @Override
    public Cobol.MoveStatement visitMoveStatement(Cobol.MoveStatement moveStatement, P p) {
        return (Cobol.MoveStatement) super.visitMoveStatement(moveStatement, p);
    }

    @Override
    public Cobol.MoveToStatement visitMoveToStatement(Cobol.MoveToStatement moveToStatement, P p) {
        return (Cobol.MoveToStatement) super.visitMoveToStatement(moveToStatement, p);
    }

    @Override
    public Cobol.MoveCorrespondingToStatement visitMoveCorrespondingToStatement(Cobol.MoveCorrespondingToStatement moveCorrespondingToStatement, P p) {
        return (Cobol.MoveCorrespondingToStatement) super.visitMoveCorrespondingToStatement(moveCorrespondingToStatement, p);
    }

    @Override
    public Cobol.Write visitWrite(Cobol.Write write, P p) {
        return (Cobol.Write) super.visitWrite(write, p);
    }

    @Override
    public Cobol.WriteFromPhrase visitWriteFromPhrase(Cobol.WriteFromPhrase writeFromPhrase, P p) {
        return (Cobol.WriteFromPhrase) super.visitWriteFromPhrase(writeFromPhrase, p);
    }

    @Override
    public Cobol.WriteAdvancingPhrase visitWriteAdvancingPhrase(Cobol.WriteAdvancingPhrase writeAdvancingPhrase, P p) {
        return (Cobol.WriteAdvancingPhrase) super.visitWriteAdvancingPhrase(writeAdvancingPhrase, p);
    }

    @Override
    public Cobol.WriteAdvancingPage visitWriteAdvancingPage(Cobol.WriteAdvancingPage writeAdvancingPage, P p) {
        return (Cobol.WriteAdvancingPage) super.visitWriteAdvancingPage(writeAdvancingPage, p);
    }

    @Override
    public Cobol.WriteAdvancingLines visitWriteAdvancingLines(Cobol.WriteAdvancingLines writeAdvancingLines, P p) {
        return (Cobol.WriteAdvancingLines) super.visitWriteAdvancingLines(writeAdvancingLines, p);
    }

    @Override
    public Cobol.WriteAdvancingMnemonic visitWriteAdvancingMnemonic(Cobol.WriteAdvancingMnemonic writeAdvancingMnemonic, P p) {
        return (Cobol.WriteAdvancingMnemonic) super.visitWriteAdvancingMnemonic(writeAdvancingMnemonic, p);
    }

    @Override
    public Cobol.ArithmeticExpression visitArithmeticExpression(Cobol.ArithmeticExpression arithmeticExpression, P p) {
        return (Cobol.ArithmeticExpression) super.visitArithmeticExpression(arithmeticExpression, p);
    }

    @Override
    public Cobol.Basis visitBasis(Cobol.Basis basis, P p) {
        return (Cobol.Basis) super.visitBasis(basis, p);
    }

    @Override
    public Cobol.ComputeStatement visitComputeStatement(Cobol.ComputeStatement computeStatement, P p) {
        return (Cobol.ComputeStatement) super.visitComputeStatement(computeStatement, p);
    }

    @Override
    public Cobol.ComputeStore visitComputeStore(Cobol.ComputeStore computeStore, P p) {
        return (Cobol.ComputeStore) super.visitComputeStore(computeStore, p);
    }

    @Override
    public Cobol.MultDivs visitMultDivs(Cobol.MultDivs multDivs, P p) {
        return (Cobol.MultDivs) super.visitMultDivs(multDivs, p);
    }

    @Override
    public Cobol.MultDiv visitMultDiv(Cobol.MultDiv multDiv, P p) {
        return (Cobol.MultDiv) super.visitMultDiv(multDiv, p);
    }

    @Override
    public Cobol.Powers visitPowers(Cobol.Powers powers, P p) {
        return (Cobol.Powers) super.visitPowers(powers, p);
    }

    @Override
    public Cobol.Power visitPower(Cobol.Power power, P p) {
        return (Cobol.Power) super.visitPower(power, p);
    }

    @Override
    public Cobol.PlusMinus visitPlusMinus(Cobol.PlusMinus plusMinus, P p) {
        return (Cobol.PlusMinus) super.visitPlusMinus(plusMinus, p);
    }

    @Override
    public Cobol.Divide visitDivide(Cobol.Divide divide, P p) {
        return (Cobol.Divide) super.visitDivide(divide, p);
    }

    @Override
    public Cobol.DivideInto visitDivideInto(Cobol.DivideInto divideInto, P p) {
        return (Cobol.DivideInto) super.visitDivideInto(divideInto, p);
    }

    @Override
    public Cobol.DivideGiving visitDivideGiving(Cobol.DivideGiving divideGiving, P p) {
        return (Cobol.DivideGiving) super.visitDivideGiving(divideGiving, p);
    }

    @Override
    public Cobol.DivideGivingPhrase visitDivideGivingPhrase(Cobol.DivideGivingPhrase divideGivingPhrase, P p) {
        return (Cobol.DivideGivingPhrase) super.visitDivideGivingPhrase(divideGivingPhrase, p);
    }

    @Override
    public Cobol.DivideRemainder visitDivideRemainder(Cobol.DivideRemainder divideRemainder, P p) {
        return (Cobol.DivideRemainder) super.visitDivideRemainder(divideRemainder, p);
    }

    @Override
    public Cobol.Merge visitMerge(Cobol.Merge merge, P p) {
        return (Cobol.Merge) super.visitMerge(merge, p);
    }

    @Override
    public Cobol.MergeOnKeyClause visitMergeOnKeyClause(Cobol.MergeOnKeyClause mergeOnKeyClause, P p) {
        return (Cobol.MergeOnKeyClause) super.visitMergeOnKeyClause(mergeOnKeyClause, p);
    }

    @Override
    public Cobol.MergeCollatingSequencePhrase visitMergeCollatingSequencePhrase(Cobol.MergeCollatingSequencePhrase mergeCollatingSequencePhrase, P p) {
        return (Cobol.MergeCollatingSequencePhrase) super.visitMergeCollatingSequencePhrase(mergeCollatingSequencePhrase, p);
    }

    @Override
    public Cobol.Mergeable visitMergeable(Cobol.Mergeable mergeable, P p) {
        return (Cobol.Mergeable) super.visitMergeable(mergeable, p);
    }

    @Override
    public Cobol.MergeOutputProcedurePhrase visitMergeOutputProcedurePhrase(Cobol.MergeOutputProcedurePhrase mergeOutputProcedurePhrase, P p) {
        return (Cobol.MergeOutputProcedurePhrase) super.visitMergeOutputProcedurePhrase(mergeOutputProcedurePhrase, p);
    }

    @Override
    public Cobol.MergeGivingPhrase visitMergeGivingPhrase(Cobol.MergeGivingPhrase mergeGivingPhrase, P p) {
        return (Cobol.MergeGivingPhrase) super.visitMergeGivingPhrase(mergeGivingPhrase, p);
    }

    @Override
    public Cobol.MergeGiving visitMergeGiving(Cobol.MergeGiving mergeGiving, P p) {
        return (Cobol.MergeGiving) super.visitMergeGiving(mergeGiving, p);
    }

    @Override
    public @Nullable Cobol.MergeUsing visitMergeUsing(Cobol.MergeUsing mergeUsing, P p) {
        return (Cobol.MergeUsing) super.visitMergeUsing(mergeUsing, p);
    }

    @Override
    public @Nullable Cobol.MergeOutputThrough visitMergeOutputThrough(Cobol.MergeOutputThrough mergeOutputThrough, P p) {
        return (Cobol.MergeOutputThrough) super.visitMergeOutputThrough(mergeOutputThrough, p);
    }

    @Override
    public Cobol.Multiply visitMultiply(Cobol.Multiply multiply, P p) {
        return (Cobol.Multiply) super.visitMultiply(multiply, p);
    }

    @Override
    public Cobol.MultiplyRegular visitMultiplyRegular(Cobol.MultiplyRegular multiplyRegular, P p) {
        return (Cobol.MultiplyRegular) super.visitMultiplyRegular(multiplyRegular, p);
    }

    @Override
    public Cobol.MultiplyGiving visitMultiplyGiving(Cobol.MultiplyGiving multiplyGiving, P p) {
        return (Cobol.MultiplyGiving) super.visitMultiplyGiving(multiplyGiving, p);
    }

    @Override
    public Cobol.NextSentence visitNextSentence(Cobol.NextSentence nextSentence, P p) {
        return (Cobol.NextSentence) super.visitNextSentence(nextSentence, p);
    }

    @Override
    public Cobol.Open visitOpen(Cobol.Open open, P p) {
        return (Cobol.Open) super.visitOpen(open, p);
    }

    @Override
    public Cobol.OpenInputOutputStatement visitOpenInputOutputStatement(Cobol.OpenInputOutputStatement openInputOutputStatement, P p) {
        return (Cobol.OpenInputOutputStatement) super.visitOpenInputOutputStatement(openInputOutputStatement, p);
    }

    @Override
    public Cobol.Openable visitOpenable(Cobol.Openable openable, P p) {
        return (Cobol.Openable) super.visitOpenable(openable, p);
    }

    @Override
    public Cobol.OpenIOExtendStatement visitOpenIOExtendStatement(Cobol.OpenIOExtendStatement openIOExtendStatement, P p) {
        return (Cobol.OpenIOExtendStatement) super.visitOpenIOExtendStatement(openIOExtendStatement, p);
    }

    @Override
    public Cobol.Perform visitPerform(Cobol.Perform perform, P p) {
        return (Cobol.Perform) super.visitPerform(perform, p);
    }

    @Override
    public Cobol.PerformInlineStatement visitPerformInlineStatement(Cobol.PerformInlineStatement performInlineStatement, P p) {
        return (Cobol.PerformInlineStatement) super.visitPerformInlineStatement(performInlineStatement, p);
    }

    @Override
    public Cobol.PerformProcedureStatement visitPerformProcedureStatement(Cobol.PerformProcedureStatement performProcedureStatement, P p) {
        return (Cobol.PerformProcedureStatement) super.visitPerformProcedureStatement(performProcedureStatement, p);
    }

    @Override
    public Cobol.PerformTimes visitPerformTimes(Cobol.PerformTimes performTimes, P p) {
        return (Cobol.PerformTimes) super.visitPerformTimes(performTimes, p);
    }

    @Override
    public Cobol.PerformUntil visitPerformUntil(Cobol.PerformUntil performUntil, P p) {
        return (Cobol.PerformUntil) super.visitPerformUntil(performUntil, p);
    }

    @Override
    public Cobol.PerformVarying visitPerformVarying(Cobol.PerformVarying performVarying, P p) {
        return (Cobol.PerformVarying) super.visitPerformVarying(performVarying, p);
    }

    @Override
    public Cobol.PerformVaryingClause visitPerformVaryingClause(Cobol.PerformVaryingClause performVaryingClause, P p) {
        return (Cobol.PerformVaryingClause) super.visitPerformVaryingClause(performVaryingClause, p);
    }

    @Override
    public Cobol.PerformVaryingPhrase visitPerformVaryingPhrase(Cobol.PerformVaryingPhrase performVaryingPhrase, P p) {
        return (Cobol.PerformVaryingPhrase) super.visitPerformVaryingPhrase(performVaryingPhrase, p);
    }

    @Override
    public Cobol.Performable visitPerformable(Cobol.Performable performable, P p) {
        return (Cobol.Performable) super.visitPerformable(performable, p);
    }

    @Override
    public Cobol.PerformFrom visitPerformFrom(Cobol.PerformFrom performFrom, P p) {
        return (Cobol.PerformFrom) super.visitPerformFrom(performFrom, p);
    }

    @Override
    public Cobol.PerformTestClause visitPerformTestClause(Cobol.PerformTestClause performTestClause, P p) {
        return (Cobol.PerformTestClause) super.visitPerformTestClause(performTestClause, p);
    }
}
