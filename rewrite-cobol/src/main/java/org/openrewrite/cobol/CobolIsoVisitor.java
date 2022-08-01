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
    public Cobol.Compute visitCompute(Cobol.Compute compute, P p) {
        return (Cobol.Compute) super.visitCompute(compute, p);
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

    @Override
    public Cobol.Purge visitPurge(Cobol.Purge purge, P p) {
        return (Cobol.Purge) super.visitPurge(purge, p);
    }

    @Override
    public Cobol.Read visitRead(Cobol.Read read, P p) {
        return (Cobol.Read) super.visitRead(read, p);
    }

    @Override
    public Cobol.ReadInto visitReadInto(Cobol.ReadInto readInto, P p) {
        return (Cobol.ReadInto) super.visitReadInto(readInto, p);
    }

    @Override
    public Cobol.ReadWith visitReadWith(Cobol.ReadWith readWith, P p) {
        return (Cobol.ReadWith) super.visitReadWith(readWith, p);
    }

    @Override
    public Cobol.ReadKey visitReadKey(Cobol.ReadKey readKey, P p) {
        return (Cobol.ReadKey) super.visitReadKey(readKey, p);
    }

    @Override
    public Cobol.Evaluate visitEvaluate(Cobol.Evaluate evaluate, P p) {
        return (Cobol.Evaluate) super.visitEvaluate(evaluate, p);
    }

    @Override
    public Cobol.EvaluateAlso visitEvaluateAlso(Cobol.EvaluateAlso evaluateAlso, P p) {
        return (Cobol.EvaluateAlso) super.visitEvaluateAlso(evaluateAlso, p);
    }

    @Override
    public Cobol.EvaluateAlsoCondition visitEvaluateAlsoCondition(Cobol.EvaluateAlsoCondition evaluateAlsoCondition, P p) {
        return (Cobol.EvaluateAlsoCondition) super.visitEvaluateAlsoCondition(evaluateAlsoCondition, p);
    }

    @Override
    public Cobol.EvaluateCondition visitEvaluateCondition(Cobol.EvaluateCondition evaluateCondition, P p) {
        return (Cobol.EvaluateCondition) super.visitEvaluateCondition(evaluateCondition, p);
    }

    @Override
    public Cobol.EvaluateThrough visitEvaluateThrough(Cobol.EvaluateThrough evaluateThrough, P p) {
        return (Cobol.EvaluateThrough) super.visitEvaluateThrough(evaluateThrough, p);
    }

    @Override
    public Cobol.EvaluateWhen visitEvaluateWhen(Cobol.EvaluateWhen evaluateWhen, P p) {
        return (Cobol.EvaluateWhen) super.visitEvaluateWhen(evaluateWhen, p);
    }

    @Override
    public Cobol.EvaluateWhenPhrase visitEvaluateWhenPhrase(Cobol.EvaluateWhenPhrase evaluateWhenPhrase, P p) {
        return (Cobol.EvaluateWhenPhrase) super.visitEvaluateWhenPhrase(evaluateWhenPhrase, p);
    }

    @Override
    public Cobol.EvaluateValueThrough visitEvaluateValueThrough(Cobol.EvaluateValueThrough evaluateValueThrough, P p) {
        return (Cobol.EvaluateValueThrough) super.visitEvaluateValueThrough(evaluateValueThrough, p);
    }

    @Override
    public Cobol.Abbreviation visitAbbreviation(Cobol.Abbreviation abbreviation, P p) {
        return (Cobol.Abbreviation) super.visitAbbreviation(abbreviation, p);
    }

    @Override
    public Cobol.AndOrCondition visitAndOrCondition(Cobol.AndOrCondition andOrCondition, P p) {
        return (Cobol.AndOrCondition) super.visitAndOrCondition(andOrCondition, p);
    }

    @Override
    public Cobol.ClassCondition visitClassCondition(Cobol.ClassCondition classCondition, P p) {
        return (Cobol.ClassCondition) super.visitClassCondition(classCondition, p);
    }

    @Override
    public Cobol.CombinableCondition visitCombinableCondition(Cobol.CombinableCondition combinableCondition, P p) {
        return (Cobol.CombinableCondition) super.visitCombinableCondition(combinableCondition, p);
    }

    @Override
    public Cobol.Condition visitCondition(Cobol.Condition condition, P p) {
        return (Cobol.Condition) super.visitCondition(condition, p);
    }

    @Override
    public Cobol.ConditionNameReference visitConditionNameReference(Cobol.ConditionNameReference conditionNameReference, P p) {
        return (Cobol.ConditionNameReference) super.visitConditionNameReference(conditionNameReference, p);
    }

    @Override
    public Cobol.RelationalOperator visitRelationalOperator(Cobol.RelationalOperator relationalOperator, P p) {
        return (Cobol.RelationalOperator) super.visitRelationalOperator(relationalOperator, p);
    }

    @Override
    public Cobol.RelationArithmeticComparison visitRelationArithmeticComparison(Cobol.RelationArithmeticComparison relationArithmeticComparison, P p) {
        return (Cobol.RelationArithmeticComparison) super.visitRelationArithmeticComparison(relationArithmeticComparison, p);
    }

    @Override
    public Cobol.RelationCombinedComparison visitRelationCombinedComparison(Cobol.RelationCombinedComparison relationCombinedComparison, P p) {
        return (Cobol.RelationCombinedComparison) super.visitRelationCombinedComparison(relationCombinedComparison, p);
    }

    @Override
    public Cobol.RelationCombinedCondition visitRelationCombinedCondition(Cobol.RelationCombinedCondition relationCombinedCondition, P p) {
        return (Cobol.RelationCombinedCondition) super.visitRelationCombinedCondition(relationCombinedCondition, p);
    }

    @Override
    public Cobol.RelationSignCondition visitRelationSignCondition(Cobol.RelationSignCondition relationSignCondition, P p) {
        return (Cobol.RelationSignCondition) super.visitRelationSignCondition(relationSignCondition, p);
    }

    @Override
    public Cobol.UnString visitUnString(Cobol.UnString unString, P p) {
        return (Cobol.UnString) super.visitUnString(unString, p);
    }

    @Override
    public Cobol.UnstringSendingPhrase visitUnstringSendingPhrase(Cobol.UnstringSendingPhrase unstringSendingPhrase, P p) {
        return (Cobol.UnstringSendingPhrase) super.visitUnstringSendingPhrase(unstringSendingPhrase, p);
    }

    @Override
    public Cobol.UnstringDelimitedByPhrase visitUnstringDelimitedByPhrase(Cobol.UnstringDelimitedByPhrase unstringDelimitedByPhrase, P p) {
        return (Cobol.UnstringDelimitedByPhrase) super.visitUnstringDelimitedByPhrase(unstringDelimitedByPhrase, p);
    }

    @Override
    public Cobol.UnstringOrAllPhrase visitUnstringOrAllPhrase(Cobol.UnstringOrAllPhrase unstringOrAllPhrase, P p) {
        return (Cobol.UnstringOrAllPhrase) super.visitUnstringOrAllPhrase(unstringOrAllPhrase, p);
    }

    @Override
    public Cobol.UnstringIntoPhrase visitUnstringIntoPhrase(Cobol.UnstringIntoPhrase unstringIntoPhrase, P p) {
        return (Cobol.UnstringIntoPhrase) super.visitUnstringIntoPhrase(unstringIntoPhrase, p);
    }

    @Override
    public Cobol.UnstringInto visitUnstringInto(Cobol.UnstringInto unstringInto, P p) {
        return (Cobol.UnstringInto) super.visitUnstringInto(unstringInto, p);
    }

    @Override
    public Cobol.UnstringDelimiterIn visitUnstringDelimiterIn(Cobol.UnstringDelimiterIn unstringDelimiterIn, P p) {
        return (Cobol.UnstringDelimiterIn) super.visitUnstringDelimiterIn(unstringDelimiterIn, p);
    }

    @Override
    public Cobol.UnstringCountIn visitUnstringCountIn(Cobol.UnstringCountIn unstringCountIn, P p) {
        return (Cobol.UnstringCountIn) super.visitUnstringCountIn(unstringCountIn, p);
    }

    @Override
    public Cobol.UnstringWithPointerPhrase visitUnstringWithPointerPhrase(Cobol.UnstringWithPointerPhrase unstringWithPointerPhrase, P p) {
        return (Cobol.UnstringWithPointerPhrase) super.visitUnstringWithPointerPhrase(unstringWithPointerPhrase, p);
    }

    @Override
    public Cobol.UnstringTallyingPhrase visitUnstringTallyingPhrase(Cobol.UnstringTallyingPhrase unstringTallyingPhrase, P p) {
        return (Cobol.UnstringTallyingPhrase) super.visitUnstringTallyingPhrase(unstringTallyingPhrase, p);
    }

    @Override
    public Cobol.ConditionNameSubscriptReference visitConditionNameSubscriptReference(Cobol.ConditionNameSubscriptReference conditionNameSubscriptReference, P p) {
        return (Cobol.ConditionNameSubscriptReference) super.visitConditionNameSubscriptReference(conditionNameSubscriptReference, p);
    }

    @Override
    public Cobol.Subscript visitSubscript(Cobol.Subscript subscript, P p) {
        return (Cobol.Subscript) super.visitSubscript(subscript, p);
    }

    @Override
    public Cobol.CobolWord visitCobolWord(Cobol.CobolWord cobolWord, P p) {
        return (Cobol.CobolWord) super.visitCobolWord(cobolWord, p);
    }

    @Override
    public Cobol.Receive visitReceive(Cobol.Receive receive, P p) {
        return (Cobol.Receive) super.visitReceive(receive, p);
    }

    @Override
    public Cobol.ReceiveFromStatement visitReceiveFromStatement(Cobol.ReceiveFromStatement receiveFromStatement, P p) {
        return (Cobol.ReceiveFromStatement) super.visitReceiveFromStatement(receiveFromStatement, p);
    }

    @Override
    public Cobol.ReceiveFrom visitReceiveFrom(Cobol.ReceiveFrom receiveFrom, P p) {
        return (Cobol.ReceiveFrom) super.visitReceiveFrom(receiveFrom, p);
    }

    @Override
    public Cobol.ReceiveIntoStatement visitReceiveIntoStatement(Cobol.ReceiveIntoStatement receiveIntoStatement, P p) {
        return (Cobol.ReceiveIntoStatement) super.visitReceiveIntoStatement(receiveIntoStatement, p);
    }

    @Override
    public Cobol.Receivable visitReceivable(Cobol.Receivable receivable, P p) {
        return (Cobol.Receivable) super.visitReceivable(receivable, p);
    }

    @Override
    public Cobol.Terminate visitTerminate(Cobol.Terminate terminate, P p) {
        return (Cobol.Terminate) super.visitTerminate(terminate, p);
    }

    @Override
    public Cobol.Subtract visitSubtract(Cobol.Subtract subtract, P p) {
        return (Cobol.Subtract) super.visitSubtract(subtract, p);
    }

    @Override
    public Cobol.SubtractFromStatement visitSubtractFromStatement(Cobol.SubtractFromStatement subtractFromStatement, P p) {
        return (Cobol.SubtractFromStatement) super.visitSubtractFromStatement(subtractFromStatement, p);
    }

    @Override
    public Cobol.SubtractFromGivingStatement visitSubtractFromGivingStatement(Cobol.SubtractFromGivingStatement subtractFromGivingStatement, P p) {
        return (Cobol.SubtractFromGivingStatement) super.visitSubtractFromGivingStatement(subtractFromGivingStatement, p);
    }

    @Override
    public Cobol.SubtractCorrespondingStatement visitSubtractCorrespondingStatement(Cobol.SubtractCorrespondingStatement subtractCorrespondingStatement, P p) {
        return (Cobol.SubtractCorrespondingStatement) super.visitSubtractCorrespondingStatement(subtractCorrespondingStatement, p);
    }

    @Override
    public Cobol.SubtractMinuendCorresponding visitSubtractMinuendCorresponding(Cobol.SubtractMinuendCorresponding subtractMinuendCorresponding, P p) {
        return (Cobol.SubtractMinuendCorresponding) super.visitSubtractMinuendCorresponding(subtractMinuendCorresponding, p);
    }

    @Override
    public Cobol.StringStatement visitStringStatement(Cobol.StringStatement stringStatement, P p) {
        return (Cobol.StringStatement) super.visitStringStatement(stringStatement, p);
    }

    @Override
    public Cobol.StringSendingPhrase visitStringSendingPhrase(Cobol.StringSendingPhrase stringSendingPhrase, P p) {
        return (Cobol.StringSendingPhrase) super.visitStringSendingPhrase(stringSendingPhrase, p);
    }

    @Override
    public Cobol.StringDelimitedByPhrase visitStringDelimitedByPhrase(Cobol.StringDelimitedByPhrase stringDelimitedByPhrase, P p) {
        return (Cobol.StringDelimitedByPhrase) super.visitStringDelimitedByPhrase(stringDelimitedByPhrase, p);
    }

    @Override
    public Cobol.StringForPhrase visitStringForPhrase(Cobol.StringForPhrase stringForPhrase, P p) {
        return (Cobol.StringForPhrase) super.visitStringForPhrase(stringForPhrase, p);
    }

    @Override
    public Cobol.StringIntoPhrase visitStringIntoPhrase(Cobol.StringIntoPhrase stringIntoPhrase, P p) {
        return (Cobol.StringIntoPhrase) super.visitStringIntoPhrase(stringIntoPhrase, p);
    }

    @Override
    public Cobol.StringWithPointerPhrase visitStringWithPointerPhrase(Cobol.StringWithPointerPhrase stringWithPointerPhrase, P p) {
        return (Cobol.StringWithPointerPhrase) super.visitStringWithPointerPhrase(stringWithPointerPhrase, p);
    }

    @Override
    public Cobol.ScreenDescriptionBellClause visitScreenDescriptionBellClause(Cobol.ScreenDescriptionBellClause screenDescriptionBellClause, P p) {
        return (Cobol.ScreenDescriptionBellClause) super.visitScreenDescriptionBellClause(screenDescriptionBellClause, p);
    }

    @Override
    public Cobol.ScreenDescriptionBlinkClause visitScreenDescriptionBlinkClause(Cobol.ScreenDescriptionBlinkClause screenDescriptionBlinkClause, P p) {
        return (Cobol.ScreenDescriptionBlinkClause) super.visitScreenDescriptionBlinkClause(screenDescriptionBlinkClause, p);
    }

    @Override
    public Cobol.ScreenDescriptionEraseClause visitScreenDescriptionEraseClause(Cobol.ScreenDescriptionEraseClause screenDescriptionEraseClause, P p) {
        return (Cobol.ScreenDescriptionEraseClause) super.visitScreenDescriptionEraseClause(screenDescriptionEraseClause, p);
    }

    @Override
    public Cobol.ScreenDescriptionLightClause visitScreenDescriptionLightClause(Cobol.ScreenDescriptionLightClause screenDescriptionLightClause, P p) {
        return (Cobol.ScreenDescriptionLightClause) super.visitScreenDescriptionLightClause(screenDescriptionLightClause, p);
    }

    @Override
    public Cobol.ScreenDescriptionGridClause visitScreenDescriptionGridClause(Cobol.ScreenDescriptionGridClause screenDescriptionGridClause, P p) {
        return (Cobol.ScreenDescriptionGridClause) super.visitScreenDescriptionGridClause(screenDescriptionGridClause, p);
    }

    @Override
    public Cobol.ScreenDescriptionReverseVideoClause visitScreenDescriptionReverseVideoClause(Cobol.ScreenDescriptionReverseVideoClause screenDescriptionReverseVideoClause, P p) {
        return (Cobol.ScreenDescriptionReverseVideoClause) super.visitScreenDescriptionReverseVideoClause(screenDescriptionReverseVideoClause, p);
    }

    @Override
    public Cobol.ScreenDescriptionUnderlineClause visitScreenDescriptionUnderlineClause(Cobol.ScreenDescriptionUnderlineClause screenDescriptionUnderlineClause, P p) {
        return (Cobol.ScreenDescriptionUnderlineClause) super.visitScreenDescriptionUnderlineClause(screenDescriptionUnderlineClause, p);
    }

    @Override
    public Cobol.ScreenDescriptionLineClause visitScreenDescriptionLineClause(Cobol.ScreenDescriptionLineClause screenDescriptionLineClause, P p) {
        return (Cobol.ScreenDescriptionLineClause) super.visitScreenDescriptionLineClause(screenDescriptionLineClause, p);
    }

    @Override
    public Cobol.ScreenDescriptionColumnClause visitScreenDescriptionColumnClause(Cobol.ScreenDescriptionColumnClause screenDescriptionColumnClause, P p) {
        return (Cobol.ScreenDescriptionColumnClause) super.visitScreenDescriptionColumnClause(screenDescriptionColumnClause, p);
    }

    @Override
    public Cobol.ScreenDescriptionForegroundColorClause visitScreenDescriptionForegroundColorClause(Cobol.ScreenDescriptionForegroundColorClause screenDescriptionForegroundColorClause, P p) {
        return (Cobol.ScreenDescriptionForegroundColorClause) super.visitScreenDescriptionForegroundColorClause(screenDescriptionForegroundColorClause, p);
    }

    @Override
    public Cobol.ScreenDescriptionBackgroundColorClause visitScreenDescriptionBackgroundColorClause(Cobol.ScreenDescriptionBackgroundColorClause screenDescriptionBackgroundColorClause, P p) {
        return (Cobol.ScreenDescriptionBackgroundColorClause) super.visitScreenDescriptionBackgroundColorClause(screenDescriptionBackgroundColorClause, p);
    }

    @Override
    public Cobol.ScreenDescriptionValueClause visitScreenDescriptionValueClause(Cobol.ScreenDescriptionValueClause screenDescriptionValueClause, P p) {
        return (Cobol.ScreenDescriptionValueClause) super.visitScreenDescriptionValueClause(screenDescriptionValueClause, p);
    }

    @Override
    public Cobol.ScreenDescriptionPictureClause visitScreenDescriptionPictureClause(Cobol.ScreenDescriptionPictureClause screenDescriptionPictureClause, P p) {
        return (Cobol.ScreenDescriptionPictureClause) super.visitScreenDescriptionPictureClause(screenDescriptionPictureClause, p);
    }

    @Override
    public Cobol.ScreenDescriptionFromClause visitScreenDescriptionFromClause(Cobol.ScreenDescriptionFromClause screenDescriptionFromClause, P p) {
        return (Cobol.ScreenDescriptionFromClause) super.visitScreenDescriptionFromClause(screenDescriptionFromClause, p);
    }

    @Override
    public Cobol.ScreenDescriptionUsageClause visitScreenDescriptionUsageClause(Cobol.ScreenDescriptionUsageClause screenDescriptionUsageClause, P p) {
        return (Cobol.ScreenDescriptionUsageClause) super.visitScreenDescriptionUsageClause(screenDescriptionUsageClause, p);
    }

    @Override
    public Cobol.ScreenDescriptionBlankWhenZeroClause visitScreenDescriptionBlankWhenZeroClause(Cobol.ScreenDescriptionBlankWhenZeroClause screenDescriptionBlankWhenZeroClause, P p) {
        return (Cobol.ScreenDescriptionBlankWhenZeroClause) super.visitScreenDescriptionBlankWhenZeroClause(screenDescriptionBlankWhenZeroClause, p);
    }

    @Override
    public Cobol.ScreenDescriptionJustifiedClause visitScreenDescriptionJustifiedClause(Cobol.ScreenDescriptionJustifiedClause screenDescriptionJustifiedClause, P p) {
        return (Cobol.ScreenDescriptionJustifiedClause) super.visitScreenDescriptionJustifiedClause(screenDescriptionJustifiedClause, p);
    }

    @Override
    public Cobol.ScreenDescriptionSignClause visitScreenDescriptionSignClause(Cobol.ScreenDescriptionSignClause screenDescriptionSignClause, P p) {
        return (Cobol.ScreenDescriptionSignClause) super.visitScreenDescriptionSignClause(screenDescriptionSignClause, p);
    }

    @Override
    public Cobol.ScreenDescriptionAutoClause visitScreenDescriptionAutoClause(Cobol.ScreenDescriptionAutoClause screenDescriptionAutoClause, P p) {
        return (Cobol.ScreenDescriptionAutoClause) super.visitScreenDescriptionAutoClause(screenDescriptionAutoClause, p);
    }

    @Override
    public Cobol.ScreenDescriptionRequiredClause visitScreenDescriptionRequiredClause(Cobol.ScreenDescriptionRequiredClause screenDescriptionRequiredClause, P p) {
        return (Cobol.ScreenDescriptionRequiredClause) super.visitScreenDescriptionRequiredClause(screenDescriptionRequiredClause, p);
    }

    @Override
    public Cobol.ScreenDescriptionPromptClause visitScreenDescriptionPromptClause(Cobol.ScreenDescriptionPromptClause screenDescriptionPromptClause, P p) {
        return (Cobol.ScreenDescriptionPromptClause) super.visitScreenDescriptionPromptClause(screenDescriptionPromptClause, p);
    }

    @Override
    public Cobol.ScreenDescriptionPromptOccursClause visitScreenDescriptionPromptOccursClause(Cobol.ScreenDescriptionPromptOccursClause screenDescriptionPromptOccursClause, P p) {
        return (Cobol.ScreenDescriptionPromptOccursClause) super.visitScreenDescriptionPromptOccursClause(screenDescriptionPromptOccursClause, p);
    }

    @Override
    public Cobol.ScreenDescriptionFullClause visitScreenDescriptionFullClause(Cobol.ScreenDescriptionFullClause screenDescriptionFullClause, P p) {
        return (Cobol.ScreenDescriptionFullClause) super.visitScreenDescriptionFullClause(screenDescriptionFullClause, p);
    }

    @Override
    public Cobol.ScreenDescriptionZeroFillClause visitScreenDescriptionZeroFillClause(Cobol.ScreenDescriptionZeroFillClause screenDescriptionZeroFillClause, P p) {
        return (Cobol.ScreenDescriptionZeroFillClause) super.visitScreenDescriptionZeroFillClause(screenDescriptionZeroFillClause, p);
    }

    @Override
    public Cobol.PictureString visitPictureString(Cobol.PictureString pictureString, P p) {
        return (Cobol.PictureString) super.visitPictureString(pictureString, p);
    }

    @Override
    public Cobol.Start visitStart(Cobol.Start start, P p) {
        return (Cobol.Start) super.visitStart(start, p);
    }

    @Override
    public Cobol.StartKey visitStartKey(Cobol.StartKey startKey, P p) {
        return (Cobol.StartKey) super.visitStartKey(startKey, p);
    }

    @Override
    public Cobol.ExecCicsStatement visitExecCicsStatement(Cobol.ExecCicsStatement execCicsStatement, P p) {
        return (Cobol.ExecCicsStatement) super.visitExecCicsStatement(execCicsStatement, p);
    }

    @Override
    public Cobol.ExecSqlStatement visitExecSqlStatement(Cobol.ExecSqlStatement execSqlStatement, P p) {
        return (Cobol.ExecSqlStatement) super.visitExecSqlStatement(execSqlStatement, p);
    }

    @Override
    public Cobol.ExecSqlImsStatement visitExecSqlImsStatement(Cobol.ExecSqlImsStatement execSqlImsStatement, P p) {
        return (Cobol.ExecSqlImsStatement) super.visitExecSqlImsStatement(execSqlImsStatement, p);
    }

    @Override
    public Cobol.GoBack visitGoBack(Cobol.GoBack goBack, P p) {
        return (Cobol.GoBack) super.visitGoBack(goBack, p);
    }

    @Override
    public Cobol.GoTo visitGoTo(Cobol.GoTo _goTo, P p) {
        return (Cobol.GoTo) super.visitGoTo(_goTo, p);
    }

    @Override
    public Cobol.GoToDependingOnStatement visitGoToDependingOnStatement(Cobol.GoToDependingOnStatement goToDependingOnStatement, P p) {
        return (Cobol.GoToDependingOnStatement) super.visitGoToDependingOnStatement(goToDependingOnStatement, p);
    }

    @Override
    public Cobol.If visitIf(Cobol.If _if, P p) {
        return (Cobol.If) super.visitIf(_if, p);
    }

    @Override
    public Cobol.IfThen visitIfThen(Cobol.IfThen ifThen, P p) {
        return (Cobol.IfThen) super.visitIfThen(ifThen, p);
    }

    @Override
    public Cobol.IfElse visitIfElse(Cobol.IfElse ifElse, P p) {
        return (Cobol.IfElse) super.visitIfElse(ifElse, p);
    }

    @Override
    public Cobol.Sort visitSort(Cobol.Sort sort, P p) {
        return (Cobol.Sort) super.visitSort(sort, p);
    }

    @Override
    public Cobol.SortCollatingSequencePhrase visitSortCollatingSequencePhrase(Cobol.SortCollatingSequencePhrase sortCollatingSequencePhrase, P p) {
        return (Cobol.SortCollatingSequencePhrase) super.visitSortCollatingSequencePhrase(sortCollatingSequencePhrase, p);
    }

    @Override
    public Cobol.SortProcedurePhrase visitSortProcedurePhrase(Cobol.SortProcedurePhrase sortProcedurePhrase, P p) {
        return (Cobol.SortProcedurePhrase) super.visitSortProcedurePhrase(sortProcedurePhrase, p);
    }

    @Override
    public Cobol.Sortable visitSortable(Cobol.Sortable sortable, P p) {
        return (Cobol.Sortable) super.visitSortable(sortable, p);
    }

    @Override
    public Cobol.SortGiving visitSortGiving(Cobol.SortGiving sortGiving, P p) {
        return (Cobol.SortGiving) super.visitSortGiving(sortGiving, p);
    }

    @Override
    public Cobol.Initialize visitInitialize(Cobol.Initialize initialize, P p) {
        return (Cobol.Initialize) super.visitInitialize(initialize, p);
    }

    @Override
    public Cobol.InitializeReplacingPhrase visitInitializeReplacingPhrase(Cobol.InitializeReplacingPhrase initializeReplacingPhrase, P p) {
        return (Cobol.InitializeReplacingPhrase) super.visitInitializeReplacingPhrase(initializeReplacingPhrase, p);
    }

    @Override
    public Cobol.InitializeReplacingBy visitInitializeReplacingBy(Cobol.InitializeReplacingBy initializeReplacingBy, P p) {
        return (Cobol.InitializeReplacingBy) super.visitInitializeReplacingBy(initializeReplacingBy, p);
    }

    @Override
    public Cobol.Initiate visitInitiate(Cobol.Initiate initiate, P p) {
        return (Cobol.Initiate) super.visitInitiate(initiate, p);
    }

    @Override
    public Cobol.Inspect visitInspect(Cobol.Inspect inspect, P p) {
        return (Cobol.Inspect) super.visitInspect(inspect, p);
    }

    @Override
    public Cobol.InspectAllLeading visitInspectAllLeading(Cobol.InspectAllLeading inspectAllLeading, P p) {
        return (Cobol.InspectAllLeading) super.visitInspectAllLeading(inspectAllLeading, p);
    }

    @Override
    public Cobol.InspectAllLeadings visitInspectAllLeadings(Cobol.InspectAllLeadings inspectAllLeadings, P p) {
        return (Cobol.InspectAllLeadings) super.visitInspectAllLeadings(inspectAllLeadings, p);
    }

    @Override
    public Cobol.InspectBeforeAfter visitInspectBeforeAfter(Cobol.InspectBeforeAfter inspectBeforeAfter, P p) {
        return (Cobol.InspectBeforeAfter) super.visitInspectBeforeAfter(inspectBeforeAfter, p);
    }

    @Override
    public Cobol.InspectBy visitInspectBy(Cobol.InspectBy inspectBy, P p) {
        return (Cobol.InspectBy) super.visitInspectBy(inspectBy, p);
    }

    @Override
    public Cobol.InspectCharacters visitInspectCharacters(Cobol.InspectCharacters inspectCharacters, P p) {
        return (Cobol.InspectCharacters) super.visitInspectCharacters(inspectCharacters, p);
    }

    @Override
    public Cobol.InspectConvertingPhrase visitInspectConvertingPhrase(Cobol.InspectConvertingPhrase inspectConvertingPhrase, P p) {
        return (Cobol.InspectConvertingPhrase) super.visitInspectConvertingPhrase(inspectConvertingPhrase, p);
    }

    @Override
    public Cobol.InspectFor visitInspectFor(Cobol.InspectFor inspectFor, P p) {
        return (Cobol.InspectFor) super.visitInspectFor(inspectFor, p);
    }

    @Override
    public Cobol.InspectReplacingAllLeadings visitInspectReplacingAllLeadings(Cobol.InspectReplacingAllLeadings inspectReplacingAllLeadings, P p) {
        return (Cobol.InspectReplacingAllLeadings) super.visitInspectReplacingAllLeadings(inspectReplacingAllLeadings, p);
    }

    @Override
    public Cobol.InspectReplacingAllLeading visitInspectReplacingAllLeading(Cobol.InspectReplacingAllLeading inspectReplacingAllLeading, P p) {
        return (Cobol.InspectReplacingAllLeading) super.visitInspectReplacingAllLeading(inspectReplacingAllLeading, p);
    }

    @Override
    public Cobol.InspectReplacingCharacters visitInspectReplacingCharacters(Cobol.InspectReplacingCharacters inspectReplacingCharacters, P p) {
        return (Cobol.InspectReplacingCharacters) super.visitInspectReplacingCharacters(inspectReplacingCharacters, p);
    }

    @Override
    public Cobol.InspectReplacingPhrase visitInspectReplacingPhrase(Cobol.InspectReplacingPhrase inspectReplacingPhrase, P p) {
        return (Cobol.InspectReplacingPhrase) super.visitInspectReplacingPhrase(inspectReplacingPhrase, p);
    }

    @Override
    public Cobol.InspectTallyingPhrase visitInspectTallyingPhrase(Cobol.InspectTallyingPhrase inspectTallyingPhrase, P p) {
        return (Cobol.InspectTallyingPhrase) super.visitInspectTallyingPhrase(inspectTallyingPhrase, p);
    }

    @Override
    public Cobol.InspectTallyingReplacingPhrase visitInspectTallyingReplacingPhrase(Cobol.InspectTallyingReplacingPhrase inspectTallyingReplacingPhrase, P p) {
        return (Cobol.InspectTallyingReplacingPhrase) super.visitInspectTallyingReplacingPhrase(inspectTallyingReplacingPhrase, p);
    }

    @Override
    public Cobol.InspectTo visitInspectTo(Cobol.InspectTo inspectTo, P p) {
        return (Cobol.InspectTo) super.visitInspectTo(inspectTo, p);
    }

    @Override
    public Cobol.TableCall visitTableCall(Cobol.TableCall tableCall, P p) {
        return (Cobol.TableCall) super.visitTableCall(tableCall, p);
    }

    @Override
    public Cobol.Parenthesized visitParenthesized(Cobol.Parenthesized parenthesized, P p) {
        return (Cobol.Parenthesized) super.visitParenthesized(parenthesized, p);
    }

    @Override
    public Cobol.ReferenceModifier visitReferenceModifier(Cobol.ReferenceModifier referenceModifier, P p) {
        return (Cobol.ReferenceModifier) super.visitReferenceModifier(referenceModifier, p);
    }

    @Override
    public Cobol.FunctionCall visitFunctionCall(Cobol.FunctionCall functionCall, P p) {
        return (Cobol.FunctionCall) super.visitFunctionCall(functionCall, p);
    }

    @Override
    public Cobol.CommitmentControlClause visitCommitmentControlClause(Cobol.CommitmentControlClause commitmentControlClause, P p) {
        return (Cobol.CommitmentControlClause) super.visitCommitmentControlClause(commitmentControlClause, p);
    }

    @Override
    public Cobol.FileControlParagraph visitFileControlParagraph(Cobol.FileControlParagraph fileControlParagraph, P p) {
        return (Cobol.FileControlParagraph) super.visitFileControlParagraph(fileControlParagraph, p);
    }

    @Override
    public Cobol.FileControlEntry visitFileControlEntry(Cobol.FileControlEntry fileControlEntry, P p) {
        return (Cobol.FileControlEntry) super.visitFileControlEntry(fileControlEntry, p);
    }

    @Override
    public Cobol.InputOutputSection visitInputOutputSection(Cobol.InputOutputSection inputOutputSection, P p) {
        return (Cobol.InputOutputSection) super.visitInputOutputSection(inputOutputSection, p);
    }

    @Override
    public Cobol.IoControlParagraph visitIoControlParagraph(Cobol.IoControlParagraph ioControlParagraph, P p) {
        return (Cobol.IoControlParagraph) super.visitIoControlParagraph(ioControlParagraph, p);
    }

    @Override
    public Cobol.MultipleFileClause visitMultipleFileClause(Cobol.MultipleFileClause multipleFileClause, P p) {
        return (Cobol.MultipleFileClause) super.visitMultipleFileClause(multipleFileClause, p);
    }

    @Override
    public Cobol.MultipleFilePosition visitMultipleFilePosition(Cobol.MultipleFilePosition multipleFilePosition, P p) {
        return (Cobol.MultipleFilePosition) super.visitMultipleFilePosition(multipleFilePosition, p);
    }

    @Override
    public Cobol.RerunClause visitRerunClause(Cobol.RerunClause rerunClause, P p) {
        return (Cobol.RerunClause) super.visitRerunClause(rerunClause, p);
    }

    @Override
    public Cobol.RerunEveryClock visitRerunEveryClock(Cobol.RerunEveryClock rerunEveryClock, P p) {
        return (Cobol.RerunEveryClock) super.visitRerunEveryClock(rerunEveryClock, p);
    }

    @Override
    public Cobol.RerunEveryOf visitRerunEveryOf(Cobol.RerunEveryOf rerunEveryOf, P p) {
        return (Cobol.RerunEveryOf) super.visitRerunEveryOf(rerunEveryOf, p);
    }

    @Override
    public Cobol.RerunEveryRecords visitRerunEveryRecords(Cobol.RerunEveryRecords rerunEveryRecords, P p) {
        return (Cobol.RerunEveryRecords) super.visitRerunEveryRecords(rerunEveryRecords, p);
    }

    @Override
    public Cobol.SameClause visitSameClause(Cobol.SameClause sameClause, P p) {
        return (Cobol.SameClause) super.visitSameClause(sameClause, p);
    }

    @Override
    public Cobol.CommunicationSection visitCommunicationSection(Cobol.CommunicationSection communicationSection, P p) {
        return (Cobol.CommunicationSection) super.visitCommunicationSection(communicationSection, p);
    }

    @Override
    public Cobol.CommunicationDescriptionEntryFormat1 visitCommunicationDescriptionEntryFormat1(Cobol.CommunicationDescriptionEntryFormat1 communicationDescriptionEntryFormat1, P p) {
        return (Cobol.CommunicationDescriptionEntryFormat1) super.visitCommunicationDescriptionEntryFormat1(communicationDescriptionEntryFormat1, p);
    }

    @Override
    public Cobol.CommunicationDescriptionEntryFormat2 visitCommunicationDescriptionEntryFormat2(Cobol.CommunicationDescriptionEntryFormat2 communicationDescriptionEntryFormat2, P p) {
        return (Cobol.CommunicationDescriptionEntryFormat2) super.visitCommunicationDescriptionEntryFormat2(communicationDescriptionEntryFormat2, p);
    }

    @Override
    public Cobol.CommunicationDescriptionEntryFormat3 visitCommunicationDescriptionEntryFormat3(Cobol.CommunicationDescriptionEntryFormat3 communicationDescriptionEntryFormat3, P p) {
        return (Cobol.CommunicationDescriptionEntryFormat3) super.visitCommunicationDescriptionEntryFormat3(communicationDescriptionEntryFormat3, p);
    }

    @Override
    public Cobol.ReportDescription visitReportDescription(Cobol.ReportDescription reportDescription, P p) {
        return (Cobol.ReportDescription) super.visitReportDescription(reportDescription, p);
    }

    @Override
    public Cobol.ReportDescriptionEntry visitReportDescriptionEntry(Cobol.ReportDescriptionEntry reportDescriptionEntry, P p) {
        return (Cobol.ReportDescriptionEntry) super.visitReportDescriptionEntry(reportDescriptionEntry, p);
    }

    @Override
    public Cobol.ReportGroupDescriptionEntryFormat1 visitReportGroupDescriptionEntryFormat1(Cobol.ReportGroupDescriptionEntryFormat1 reportGroupDescriptionEntryFormat1, P p) {
        return (Cobol.ReportGroupDescriptionEntryFormat1) super.visitReportGroupDescriptionEntryFormat1(reportGroupDescriptionEntryFormat1, p);
    }

    @Override
    public Cobol.ReportGroupDescriptionEntryFormat2 visitReportGroupDescriptionEntryFormat2(Cobol.ReportGroupDescriptionEntryFormat2 reportGroupDescriptionEntryFormat2, P p) {
        return (Cobol.ReportGroupDescriptionEntryFormat2) super.visitReportGroupDescriptionEntryFormat2(reportGroupDescriptionEntryFormat2, p);
    }

    @Override
    public Cobol.ReportGroupDescriptionEntryFormat3 visitReportGroupDescriptionEntryFormat3(Cobol.ReportGroupDescriptionEntryFormat3 reportGroupDescriptionEntryFormat3, P p) {
        return (Cobol.ReportGroupDescriptionEntryFormat3) super.visitReportGroupDescriptionEntryFormat3(reportGroupDescriptionEntryFormat3, p);
    }

    @Override
    public Cobol.ReportGroupLineNumberClause visitReportGroupLineNumberClause(Cobol.ReportGroupLineNumberClause reportGroupLineNumberClause, P p) {
        return (Cobol.ReportGroupLineNumberClause) super.visitReportGroupLineNumberClause(reportGroupLineNumberClause, p);
    }

    @Override
    public Cobol.ReportGroupLineNumberNextPage visitReportGroupLineNumberNextPage(Cobol.ReportGroupLineNumberNextPage reportGroupLineNumberNextPage, P p) {
        return (Cobol.ReportGroupLineNumberNextPage) super.visitReportGroupLineNumberNextPage(reportGroupLineNumberNextPage, p);
    }

    @Override
    public Cobol.ReportGroupLineNumberPlus visitReportGroupLineNumberPlus(Cobol.ReportGroupLineNumberPlus reportGroupLineNumberPlus, P p) {
        return (Cobol.ReportGroupLineNumberPlus) super.visitReportGroupLineNumberPlus(reportGroupLineNumberPlus, p);
    }

    @Override
    public Cobol.ReportGroupNextGroupClause visitReportGroupNextGroupClause(Cobol.ReportGroupNextGroupClause reportGroupNextGroupClause, P p) {
        return (Cobol.ReportGroupNextGroupClause) super.visitReportGroupNextGroupClause(reportGroupNextGroupClause, p);
    }

    @Override
    public Cobol.ReportGroupNextGroupNextPage visitReportGroupNextGroupNextPage(Cobol.ReportGroupNextGroupNextPage reportGroupNextGroupNextPage, P p) {
        return (Cobol.ReportGroupNextGroupNextPage) super.visitReportGroupNextGroupNextPage(reportGroupNextGroupNextPage, p);
    }

    @Override
    public Cobol.ReportGroupNextGroupPlus visitReportGroupNextGroupPlus(Cobol.ReportGroupNextGroupPlus reportGroupNextGroupPlus, P p) {
        return (Cobol.ReportGroupNextGroupPlus) super.visitReportGroupNextGroupPlus(reportGroupNextGroupPlus, p);
    }

    @Override
    public Cobol.ReportGroupTypeClause visitReportGroupTypeClause(Cobol.ReportGroupTypeClause reportGroupTypeClause, P p) {
        return (Cobol.ReportGroupTypeClause) super.visitReportGroupTypeClause(reportGroupTypeClause, p);
    }

    @Override
    public Cobol.ReportGroupUsageClause visitReportGroupUsageClause(Cobol.ReportGroupUsageClause reportGroupUsageClause, P p) {
        return (Cobol.ReportGroupUsageClause) super.visitReportGroupUsageClause(reportGroupUsageClause, p);
    }

    @Override
    public Cobol.ReportDescriptionGlobalClause visitReportDescriptionGlobalClause(Cobol.ReportDescriptionGlobalClause reportDescriptionGlobalClause, P p) {
        return (Cobol.ReportDescriptionGlobalClause) super.visitReportDescriptionGlobalClause(reportDescriptionGlobalClause, p);
    }

    @Override
    public Cobol.ReportSection visitReportSection(Cobol.ReportSection reportSection, P p) {
        return (Cobol.ReportSection) super.visitReportSection(reportSection, p);
    }

    @Override
    public Cobol.LibraryAttributeClauseFormat1 visitLibraryAttributeClauseFormat1(Cobol.LibraryAttributeClauseFormat1 libraryAttributeClauseFormat1, P p) {
        return (Cobol.LibraryAttributeClauseFormat1) super.visitLibraryAttributeClauseFormat1(libraryAttributeClauseFormat1, p);
    }

    @Override
    public Cobol.LibraryAttributeClauseFormat2 visitLibraryAttributeClauseFormat2(Cobol.LibraryAttributeClauseFormat2 libraryAttributeClauseFormat2, P p) {
        return (Cobol.LibraryAttributeClauseFormat2) super.visitLibraryAttributeClauseFormat2(libraryAttributeClauseFormat2, p);
    }

    @Override
    public Cobol.LibraryAttributeFunction visitLibraryAttributeFunction(Cobol.LibraryAttributeFunction libraryAttributeFunction, P p) {
        return (Cobol.LibraryAttributeFunction) super.visitLibraryAttributeFunction(libraryAttributeFunction, p);
    }

    @Override
    public Cobol.LibraryAttributeParameter visitLibraryAttributeParameter(Cobol.LibraryAttributeParameter libraryAttributeParameter, P p) {
        return (Cobol.LibraryAttributeParameter) super.visitLibraryAttributeParameter(libraryAttributeParameter, p);
    }

    @Override
    public Cobol.LibraryAttributeTitle visitLibraryAttributeTitle(Cobol.LibraryAttributeTitle libraryAttributeTitle, P p) {
        return (Cobol.LibraryAttributeTitle) super.visitLibraryAttributeTitle(libraryAttributeTitle, p);
    }

    @Override
    public Cobol.LibraryDescriptionEntryFormat1 visitLibraryDescriptionEntryFormat1(Cobol.LibraryDescriptionEntryFormat1 libraryDescriptionEntryFormat1, P p) {
        return (Cobol.LibraryDescriptionEntryFormat1) super.visitLibraryDescriptionEntryFormat1(libraryDescriptionEntryFormat1, p);
    }

    @Override
    public Cobol.LibraryDescriptionEntryFormat2 visitLibraryDescriptionEntryFormat2(Cobol.LibraryDescriptionEntryFormat2 libraryDescriptionEntryFormat2, P p) {
        return (Cobol.LibraryDescriptionEntryFormat2) super.visitLibraryDescriptionEntryFormat2(libraryDescriptionEntryFormat2, p);
    }

    @Override
    public Cobol.LibraryEntryProcedureClauseFormat1 visitLibraryEntryProcedureClauseFormat1(Cobol.LibraryEntryProcedureClauseFormat1 libraryEntryProcedureClauseFormat1, P p) {
        return (Cobol.LibraryEntryProcedureClauseFormat1) super.visitLibraryEntryProcedureClauseFormat1(libraryEntryProcedureClauseFormat1, p);
    }

    @Override
    public Cobol.LibraryEntryProcedureClauseFormat2 visitLibraryEntryProcedureClauseFormat2(Cobol.LibraryEntryProcedureClauseFormat2 libraryEntryProcedureClauseFormat2, P p) {
        return (Cobol.LibraryEntryProcedureClauseFormat2) super.visitLibraryEntryProcedureClauseFormat2(libraryEntryProcedureClauseFormat2, p);
    }

    @Override
    public Cobol.LibraryEntryProcedureForClause visitLibraryEntryProcedureForClause(Cobol.LibraryEntryProcedureForClause libraryEntryProcedureForClause, P p) {
        return (Cobol.LibraryEntryProcedureForClause) super.visitLibraryEntryProcedureForClause(libraryEntryProcedureForClause, p);
    }

    @Override
    public Cobol.LibraryEntryProcedureGivingClause visitLibraryEntryProcedureGivingClause(Cobol.LibraryEntryProcedureGivingClause libraryEntryProcedureGivingClause, P p) {
        return (Cobol.LibraryEntryProcedureGivingClause) super.visitLibraryEntryProcedureGivingClause(libraryEntryProcedureGivingClause, p);
    }

    @Override
    public Cobol.LibraryEntryProcedureUsingClause visitLibraryEntryProcedureUsingClause(Cobol.LibraryEntryProcedureUsingClause libraryEntryProcedureUsingClause, P p) {
        return (Cobol.LibraryEntryProcedureUsingClause) super.visitLibraryEntryProcedureUsingClause(libraryEntryProcedureUsingClause, p);
    }

    @Override
    public Cobol.LibraryEntryProcedureWithClause visitLibraryEntryProcedureWithClause(Cobol.LibraryEntryProcedureWithClause libraryEntryProcedureWithClause, P p) {
        return (Cobol.LibraryEntryProcedureWithClause) super.visitLibraryEntryProcedureWithClause(libraryEntryProcedureWithClause, p);
    }

    @Override
    public Cobol.LibraryIsCommonClause visitLibraryIsCommonClause(Cobol.LibraryIsCommonClause libraryIsCommonClause, P p) {
        return (Cobol.LibraryIsCommonClause) super.visitLibraryIsCommonClause(libraryIsCommonClause, p);
    }

    @Override
    public Cobol.LibraryIsGlobalClause visitLibraryIsGlobalClause(Cobol.LibraryIsGlobalClause libraryIsGlobalClause, P p) {
        return (Cobol.LibraryIsGlobalClause) super.visitLibraryIsGlobalClause(libraryIsGlobalClause, p);
    }

    @Override
    public Cobol.ProgramLibrarySection visitProgramLibrarySection(Cobol.ProgramLibrarySection programLibrarySection, P p) {
        return (Cobol.ProgramLibrarySection) super.visitProgramLibrarySection(programLibrarySection, p);
    }

    @Override
    public Cobol.Argument visitArgument(Cobol.Argument argument, P p) {
        return (Cobol.Argument) super.visitArgument(argument, p);
    }

    @Override
    public Cobol.SelectClause visitSelectClause(Cobol.SelectClause selectClause, P p) {
        return (Cobol.SelectClause) super.visitSelectClause(selectClause, p);
    }

    @Override
    public Cobol.AssignClause visitAssignClause(Cobol.AssignClause assignClause, P p) {
        return (Cobol.AssignClause) super.visitAssignClause(assignClause, p);
    }

    @Override
    public Cobol.ReserveClause visitReserveClause(Cobol.ReserveClause reserveClause, P p) {
        return (Cobol.ReserveClause) super.visitReserveClause(reserveClause, p);
    }

    @Override
    public Cobol.OrganizationClause visitOrganizationClause(Cobol.OrganizationClause organizationClause, P p) {
        return (Cobol.OrganizationClause) super.visitOrganizationClause(organizationClause, p);
    }

    @Override
    public Cobol.PaddingCharacterClause visitPaddingCharacterClause(Cobol.PaddingCharacterClause paddingCharacterClause, P p) {
        return (Cobol.PaddingCharacterClause) super.visitPaddingCharacterClause(paddingCharacterClause, p);
    }

    @Override
    public Cobol.RecordDelimiterClause visitRecordDelimiterClause(Cobol.RecordDelimiterClause recordDelimiterClause, P p) {
        return (Cobol.RecordDelimiterClause) super.visitRecordDelimiterClause(recordDelimiterClause, p);
    }

    @Override
    public Cobol.AccessModeClause visitAccessModeClause(Cobol.AccessModeClause accessModeClause, P p) {
        return (Cobol.AccessModeClause) super.visitAccessModeClause(accessModeClause, p);
    }
}
