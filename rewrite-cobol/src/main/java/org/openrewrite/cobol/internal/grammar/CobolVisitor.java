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
// Generated from /Users/jon/Projects/github/openrewrite/rewrite/rewrite-cobol/src/main/antlr/Cobol.g4 by ANTLR 4.9.3
package org.openrewrite.cobol.internal.grammar;
import org.antlr.v4.runtime.tree.ParseTreeVisitor;

/**
 * This interface defines a complete generic visitor for a parse tree produced
 * by {@link CobolParser}.
 *
 * @param <T> The return type of the visit operation. Use {@link Void} for
 * operations with no return type.
 */
public interface CobolVisitor<T> extends ParseTreeVisitor<T> {
	/**
	 * Visit a parse tree produced by {@link CobolParser#startRule}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitStartRule(CobolParser.StartRuleContext ctx);
	/**
	 * Visit a parse tree produced by {@link CobolParser#compilationUnit}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitCompilationUnit(CobolParser.CompilationUnitContext ctx);
	/**
	 * Visit a parse tree produced by {@link CobolParser#programUnit}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitProgramUnit(CobolParser.ProgramUnitContext ctx);
	/**
	 * Visit a parse tree produced by {@link CobolParser#endProgramStatement}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitEndProgramStatement(CobolParser.EndProgramStatementContext ctx);
	/**
	 * Visit a parse tree produced by {@link CobolParser#identificationDivision}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitIdentificationDivision(CobolParser.IdentificationDivisionContext ctx);
	/**
	 * Visit a parse tree produced by {@link CobolParser#identificationDivisionBody}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitIdentificationDivisionBody(CobolParser.IdentificationDivisionBodyContext ctx);
	/**
	 * Visit a parse tree produced by {@link CobolParser#programIdParagraph}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitProgramIdParagraph(CobolParser.ProgramIdParagraphContext ctx);
	/**
	 * Visit a parse tree produced by {@link CobolParser#authorParagraph}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitAuthorParagraph(CobolParser.AuthorParagraphContext ctx);
	/**
	 * Visit a parse tree produced by {@link CobolParser#installationParagraph}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitInstallationParagraph(CobolParser.InstallationParagraphContext ctx);
	/**
	 * Visit a parse tree produced by {@link CobolParser#dateWrittenParagraph}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitDateWrittenParagraph(CobolParser.DateWrittenParagraphContext ctx);
	/**
	 * Visit a parse tree produced by {@link CobolParser#dateCompiledParagraph}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitDateCompiledParagraph(CobolParser.DateCompiledParagraphContext ctx);
	/**
	 * Visit a parse tree produced by {@link CobolParser#securityParagraph}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitSecurityParagraph(CobolParser.SecurityParagraphContext ctx);
	/**
	 * Visit a parse tree produced by {@link CobolParser#remarksParagraph}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitRemarksParagraph(CobolParser.RemarksParagraphContext ctx);
	/**
	 * Visit a parse tree produced by {@link CobolParser#environmentDivision}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitEnvironmentDivision(CobolParser.EnvironmentDivisionContext ctx);
	/**
	 * Visit a parse tree produced by {@link CobolParser#environmentDivisionBody}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitEnvironmentDivisionBody(CobolParser.EnvironmentDivisionBodyContext ctx);
	/**
	 * Visit a parse tree produced by {@link CobolParser#configurationSection}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitConfigurationSection(CobolParser.ConfigurationSectionContext ctx);
	/**
	 * Visit a parse tree produced by {@link CobolParser#configurationSectionParagraph}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitConfigurationSectionParagraph(CobolParser.ConfigurationSectionParagraphContext ctx);
	/**
	 * Visit a parse tree produced by {@link CobolParser#sourceComputerParagraph}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitSourceComputerParagraph(CobolParser.SourceComputerParagraphContext ctx);
	/**
	 * Visit a parse tree produced by {@link CobolParser#objectComputerParagraph}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitObjectComputerParagraph(CobolParser.ObjectComputerParagraphContext ctx);
	/**
	 * Visit a parse tree produced by {@link CobolParser#objectComputerClause}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitObjectComputerClause(CobolParser.ObjectComputerClauseContext ctx);
	/**
	 * Visit a parse tree produced by {@link CobolParser#memorySizeClause}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitMemorySizeClause(CobolParser.MemorySizeClauseContext ctx);
	/**
	 * Visit a parse tree produced by {@link CobolParser#diskSizeClause}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitDiskSizeClause(CobolParser.DiskSizeClauseContext ctx);
	/**
	 * Visit a parse tree produced by {@link CobolParser#collatingSequenceClause}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitCollatingSequenceClause(CobolParser.CollatingSequenceClauseContext ctx);
	/**
	 * Visit a parse tree produced by {@link CobolParser#collatingSequenceClauseAlphanumeric}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitCollatingSequenceClauseAlphanumeric(CobolParser.CollatingSequenceClauseAlphanumericContext ctx);
	/**
	 * Visit a parse tree produced by {@link CobolParser#collatingSequenceClauseNational}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitCollatingSequenceClauseNational(CobolParser.CollatingSequenceClauseNationalContext ctx);
	/**
	 * Visit a parse tree produced by {@link CobolParser#segmentLimitClause}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitSegmentLimitClause(CobolParser.SegmentLimitClauseContext ctx);
	/**
	 * Visit a parse tree produced by {@link CobolParser#characterSetClause}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitCharacterSetClause(CobolParser.CharacterSetClauseContext ctx);
	/**
	 * Visit a parse tree produced by {@link CobolParser#specialNamesParagraph}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitSpecialNamesParagraph(CobolParser.SpecialNamesParagraphContext ctx);
	/**
	 * Visit a parse tree produced by {@link CobolParser#specialNameClause}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitSpecialNameClause(CobolParser.SpecialNameClauseContext ctx);
	/**
	 * Visit a parse tree produced by {@link CobolParser#alphabetClause}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitAlphabetClause(CobolParser.AlphabetClauseContext ctx);
	/**
	 * Visit a parse tree produced by {@link CobolParser#alphabetClauseFormat1}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitAlphabetClauseFormat1(CobolParser.AlphabetClauseFormat1Context ctx);
	/**
	 * Visit a parse tree produced by {@link CobolParser#alphabetLiterals}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitAlphabetLiterals(CobolParser.AlphabetLiteralsContext ctx);
	/**
	 * Visit a parse tree produced by {@link CobolParser#alphabetThrough}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitAlphabetThrough(CobolParser.AlphabetThroughContext ctx);
	/**
	 * Visit a parse tree produced by {@link CobolParser#alphabetAlso}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitAlphabetAlso(CobolParser.AlphabetAlsoContext ctx);
	/**
	 * Visit a parse tree produced by {@link CobolParser#alphabetClauseFormat2}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitAlphabetClauseFormat2(CobolParser.AlphabetClauseFormat2Context ctx);
	/**
	 * Visit a parse tree produced by {@link CobolParser#channelClause}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitChannelClause(CobolParser.ChannelClauseContext ctx);
	/**
	 * Visit a parse tree produced by {@link CobolParser#classClause}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitClassClause(CobolParser.ClassClauseContext ctx);
	/**
	 * Visit a parse tree produced by {@link CobolParser#classClauseThrough}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitClassClauseThrough(CobolParser.ClassClauseThroughContext ctx);
	/**
	 * Visit a parse tree produced by {@link CobolParser#classClauseFrom}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitClassClauseFrom(CobolParser.ClassClauseFromContext ctx);
	/**
	 * Visit a parse tree produced by {@link CobolParser#classClauseTo}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitClassClauseTo(CobolParser.ClassClauseToContext ctx);
	/**
	 * Visit a parse tree produced by {@link CobolParser#currencySignClause}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitCurrencySignClause(CobolParser.CurrencySignClauseContext ctx);
	/**
	 * Visit a parse tree produced by {@link CobolParser#decimalPointClause}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitDecimalPointClause(CobolParser.DecimalPointClauseContext ctx);
	/**
	 * Visit a parse tree produced by {@link CobolParser#defaultComputationalSignClause}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitDefaultComputationalSignClause(CobolParser.DefaultComputationalSignClauseContext ctx);
	/**
	 * Visit a parse tree produced by {@link CobolParser#defaultDisplaySignClause}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitDefaultDisplaySignClause(CobolParser.DefaultDisplaySignClauseContext ctx);
	/**
	 * Visit a parse tree produced by {@link CobolParser#environmentSwitchNameClause}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitEnvironmentSwitchNameClause(CobolParser.EnvironmentSwitchNameClauseContext ctx);
	/**
	 * Visit a parse tree produced by {@link CobolParser#environmentSwitchNameSpecialNamesStatusPhrase}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitEnvironmentSwitchNameSpecialNamesStatusPhrase(CobolParser.EnvironmentSwitchNameSpecialNamesStatusPhraseContext ctx);
	/**
	 * Visit a parse tree produced by {@link CobolParser#odtClause}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitOdtClause(CobolParser.OdtClauseContext ctx);
	/**
	 * Visit a parse tree produced by {@link CobolParser#reserveNetworkClause}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitReserveNetworkClause(CobolParser.ReserveNetworkClauseContext ctx);
	/**
	 * Visit a parse tree produced by {@link CobolParser#symbolicCharactersClause}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitSymbolicCharactersClause(CobolParser.SymbolicCharactersClauseContext ctx);
	/**
	 * Visit a parse tree produced by {@link CobolParser#symbolicCharacters}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitSymbolicCharacters(CobolParser.SymbolicCharactersContext ctx);
	/**
	 * Visit a parse tree produced by {@link CobolParser#inputOutputSection}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitInputOutputSection(CobolParser.InputOutputSectionContext ctx);
	/**
	 * Visit a parse tree produced by {@link CobolParser#inputOutputSectionParagraph}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitInputOutputSectionParagraph(CobolParser.InputOutputSectionParagraphContext ctx);
	/**
	 * Visit a parse tree produced by {@link CobolParser#fileControlParagraph}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitFileControlParagraph(CobolParser.FileControlParagraphContext ctx);
	/**
	 * Visit a parse tree produced by {@link CobolParser#fileControlEntry}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitFileControlEntry(CobolParser.FileControlEntryContext ctx);
	/**
	 * Visit a parse tree produced by {@link CobolParser#selectClause}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitSelectClause(CobolParser.SelectClauseContext ctx);
	/**
	 * Visit a parse tree produced by {@link CobolParser#fileControlClause}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitFileControlClause(CobolParser.FileControlClauseContext ctx);
	/**
	 * Visit a parse tree produced by {@link CobolParser#assignClause}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitAssignClause(CobolParser.AssignClauseContext ctx);
	/**
	 * Visit a parse tree produced by {@link CobolParser#reserveClause}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitReserveClause(CobolParser.ReserveClauseContext ctx);
	/**
	 * Visit a parse tree produced by {@link CobolParser#organizationClause}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitOrganizationClause(CobolParser.OrganizationClauseContext ctx);
	/**
	 * Visit a parse tree produced by {@link CobolParser#paddingCharacterClause}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitPaddingCharacterClause(CobolParser.PaddingCharacterClauseContext ctx);
	/**
	 * Visit a parse tree produced by {@link CobolParser#recordDelimiterClause}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitRecordDelimiterClause(CobolParser.RecordDelimiterClauseContext ctx);
	/**
	 * Visit a parse tree produced by {@link CobolParser#accessModeClause}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitAccessModeClause(CobolParser.AccessModeClauseContext ctx);
	/**
	 * Visit a parse tree produced by {@link CobolParser#recordKeyClause}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitRecordKeyClause(CobolParser.RecordKeyClauseContext ctx);
	/**
	 * Visit a parse tree produced by {@link CobolParser#alternateRecordKeyClause}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitAlternateRecordKeyClause(CobolParser.AlternateRecordKeyClauseContext ctx);
	/**
	 * Visit a parse tree produced by {@link CobolParser#passwordClause}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitPasswordClause(CobolParser.PasswordClauseContext ctx);
	/**
	 * Visit a parse tree produced by {@link CobolParser#fileStatusClause}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitFileStatusClause(CobolParser.FileStatusClauseContext ctx);
	/**
	 * Visit a parse tree produced by {@link CobolParser#relativeKeyClause}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitRelativeKeyClause(CobolParser.RelativeKeyClauseContext ctx);
	/**
	 * Visit a parse tree produced by {@link CobolParser#ioControlParagraph}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitIoControlParagraph(CobolParser.IoControlParagraphContext ctx);
	/**
	 * Visit a parse tree produced by {@link CobolParser#ioControlClause}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitIoControlClause(CobolParser.IoControlClauseContext ctx);
	/**
	 * Visit a parse tree produced by {@link CobolParser#rerunClause}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitRerunClause(CobolParser.RerunClauseContext ctx);
	/**
	 * Visit a parse tree produced by {@link CobolParser#rerunEveryRecords}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitRerunEveryRecords(CobolParser.RerunEveryRecordsContext ctx);
	/**
	 * Visit a parse tree produced by {@link CobolParser#rerunEveryOf}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitRerunEveryOf(CobolParser.RerunEveryOfContext ctx);
	/**
	 * Visit a parse tree produced by {@link CobolParser#rerunEveryClock}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitRerunEveryClock(CobolParser.RerunEveryClockContext ctx);
	/**
	 * Visit a parse tree produced by {@link CobolParser#sameClause}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitSameClause(CobolParser.SameClauseContext ctx);
	/**
	 * Visit a parse tree produced by {@link CobolParser#multipleFileClause}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitMultipleFileClause(CobolParser.MultipleFileClauseContext ctx);
	/**
	 * Visit a parse tree produced by {@link CobolParser#multipleFilePosition}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitMultipleFilePosition(CobolParser.MultipleFilePositionContext ctx);
	/**
	 * Visit a parse tree produced by {@link CobolParser#commitmentControlClause}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitCommitmentControlClause(CobolParser.CommitmentControlClauseContext ctx);
	/**
	 * Visit a parse tree produced by {@link CobolParser#dataDivision}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitDataDivision(CobolParser.DataDivisionContext ctx);
	/**
	 * Visit a parse tree produced by {@link CobolParser#dataDivisionSection}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitDataDivisionSection(CobolParser.DataDivisionSectionContext ctx);
	/**
	 * Visit a parse tree produced by {@link CobolParser#fileSection}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitFileSection(CobolParser.FileSectionContext ctx);
	/**
	 * Visit a parse tree produced by {@link CobolParser#fileDescriptionEntry}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitFileDescriptionEntry(CobolParser.FileDescriptionEntryContext ctx);
	/**
	 * Visit a parse tree produced by {@link CobolParser#fileDescriptionEntryClause}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitFileDescriptionEntryClause(CobolParser.FileDescriptionEntryClauseContext ctx);
	/**
	 * Visit a parse tree produced by {@link CobolParser#externalClause}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitExternalClause(CobolParser.ExternalClauseContext ctx);
	/**
	 * Visit a parse tree produced by {@link CobolParser#globalClause}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitGlobalClause(CobolParser.GlobalClauseContext ctx);
	/**
	 * Visit a parse tree produced by {@link CobolParser#blockContainsClause}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitBlockContainsClause(CobolParser.BlockContainsClauseContext ctx);
	/**
	 * Visit a parse tree produced by {@link CobolParser#blockContainsTo}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitBlockContainsTo(CobolParser.BlockContainsToContext ctx);
	/**
	 * Visit a parse tree produced by {@link CobolParser#recordContainsClause}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitRecordContainsClause(CobolParser.RecordContainsClauseContext ctx);
	/**
	 * Visit a parse tree produced by {@link CobolParser#recordContainsClauseFormat1}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitRecordContainsClauseFormat1(CobolParser.RecordContainsClauseFormat1Context ctx);
	/**
	 * Visit a parse tree produced by {@link CobolParser#recordContainsClauseFormat2}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitRecordContainsClauseFormat2(CobolParser.RecordContainsClauseFormat2Context ctx);
	/**
	 * Visit a parse tree produced by {@link CobolParser#recordContainsClauseFormat3}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitRecordContainsClauseFormat3(CobolParser.RecordContainsClauseFormat3Context ctx);
	/**
	 * Visit a parse tree produced by {@link CobolParser#recordContainsTo}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitRecordContainsTo(CobolParser.RecordContainsToContext ctx);
	/**
	 * Visit a parse tree produced by {@link CobolParser#labelRecordsClause}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitLabelRecordsClause(CobolParser.LabelRecordsClauseContext ctx);
	/**
	 * Visit a parse tree produced by {@link CobolParser#valueOfClause}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitValueOfClause(CobolParser.ValueOfClauseContext ctx);
	/**
	 * Visit a parse tree produced by {@link CobolParser#valuePair}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitValuePair(CobolParser.ValuePairContext ctx);
	/**
	 * Visit a parse tree produced by {@link CobolParser#dataRecordsClause}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitDataRecordsClause(CobolParser.DataRecordsClauseContext ctx);
	/**
	 * Visit a parse tree produced by {@link CobolParser#linageClause}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitLinageClause(CobolParser.LinageClauseContext ctx);
	/**
	 * Visit a parse tree produced by {@link CobolParser#linageAt}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitLinageAt(CobolParser.LinageAtContext ctx);
	/**
	 * Visit a parse tree produced by {@link CobolParser#linageFootingAt}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitLinageFootingAt(CobolParser.LinageFootingAtContext ctx);
	/**
	 * Visit a parse tree produced by {@link CobolParser#linageLinesAtTop}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitLinageLinesAtTop(CobolParser.LinageLinesAtTopContext ctx);
	/**
	 * Visit a parse tree produced by {@link CobolParser#linageLinesAtBottom}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitLinageLinesAtBottom(CobolParser.LinageLinesAtBottomContext ctx);
	/**
	 * Visit a parse tree produced by {@link CobolParser#recordingModeClause}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitRecordingModeClause(CobolParser.RecordingModeClauseContext ctx);
	/**
	 * Visit a parse tree produced by {@link CobolParser#modeStatement}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitModeStatement(CobolParser.ModeStatementContext ctx);
	/**
	 * Visit a parse tree produced by {@link CobolParser#codeSetClause}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitCodeSetClause(CobolParser.CodeSetClauseContext ctx);
	/**
	 * Visit a parse tree produced by {@link CobolParser#reportClause}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitReportClause(CobolParser.ReportClauseContext ctx);
	/**
	 * Visit a parse tree produced by {@link CobolParser#dataBaseSection}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitDataBaseSection(CobolParser.DataBaseSectionContext ctx);
	/**
	 * Visit a parse tree produced by {@link CobolParser#dataBaseSectionEntry}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitDataBaseSectionEntry(CobolParser.DataBaseSectionEntryContext ctx);
	/**
	 * Visit a parse tree produced by {@link CobolParser#workingStorageSection}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitWorkingStorageSection(CobolParser.WorkingStorageSectionContext ctx);
	/**
	 * Visit a parse tree produced by {@link CobolParser#linkageSection}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitLinkageSection(CobolParser.LinkageSectionContext ctx);
	/**
	 * Visit a parse tree produced by {@link CobolParser#communicationSection}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitCommunicationSection(CobolParser.CommunicationSectionContext ctx);
	/**
	 * Visit a parse tree produced by {@link CobolParser#communicationDescriptionEntry}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitCommunicationDescriptionEntry(CobolParser.CommunicationDescriptionEntryContext ctx);
	/**
	 * Visit a parse tree produced by {@link CobolParser#communicationDescriptionEntryFormat1}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitCommunicationDescriptionEntryFormat1(CobolParser.CommunicationDescriptionEntryFormat1Context ctx);
	/**
	 * Visit a parse tree produced by {@link CobolParser#communicationDescriptionEntryFormat2}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitCommunicationDescriptionEntryFormat2(CobolParser.CommunicationDescriptionEntryFormat2Context ctx);
	/**
	 * Visit a parse tree produced by {@link CobolParser#communicationDescriptionEntryFormat3}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitCommunicationDescriptionEntryFormat3(CobolParser.CommunicationDescriptionEntryFormat3Context ctx);
	/**
	 * Visit a parse tree produced by {@link CobolParser#destinationCountClause}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitDestinationCountClause(CobolParser.DestinationCountClauseContext ctx);
	/**
	 * Visit a parse tree produced by {@link CobolParser#destinationTableClause}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitDestinationTableClause(CobolParser.DestinationTableClauseContext ctx);
	/**
	 * Visit a parse tree produced by {@link CobolParser#endKeyClause}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitEndKeyClause(CobolParser.EndKeyClauseContext ctx);
	/**
	 * Visit a parse tree produced by {@link CobolParser#errorKeyClause}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitErrorKeyClause(CobolParser.ErrorKeyClauseContext ctx);
	/**
	 * Visit a parse tree produced by {@link CobolParser#messageCountClause}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitMessageCountClause(CobolParser.MessageCountClauseContext ctx);
	/**
	 * Visit a parse tree produced by {@link CobolParser#messageDateClause}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitMessageDateClause(CobolParser.MessageDateClauseContext ctx);
	/**
	 * Visit a parse tree produced by {@link CobolParser#messageTimeClause}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitMessageTimeClause(CobolParser.MessageTimeClauseContext ctx);
	/**
	 * Visit a parse tree produced by {@link CobolParser#statusKeyClause}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitStatusKeyClause(CobolParser.StatusKeyClauseContext ctx);
	/**
	 * Visit a parse tree produced by {@link CobolParser#symbolicDestinationClause}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitSymbolicDestinationClause(CobolParser.SymbolicDestinationClauseContext ctx);
	/**
	 * Visit a parse tree produced by {@link CobolParser#symbolicQueueClause}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitSymbolicQueueClause(CobolParser.SymbolicQueueClauseContext ctx);
	/**
	 * Visit a parse tree produced by {@link CobolParser#symbolicSourceClause}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitSymbolicSourceClause(CobolParser.SymbolicSourceClauseContext ctx);
	/**
	 * Visit a parse tree produced by {@link CobolParser#symbolicTerminalClause}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitSymbolicTerminalClause(CobolParser.SymbolicTerminalClauseContext ctx);
	/**
	 * Visit a parse tree produced by {@link CobolParser#symbolicSubQueueClause}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitSymbolicSubQueueClause(CobolParser.SymbolicSubQueueClauseContext ctx);
	/**
	 * Visit a parse tree produced by {@link CobolParser#textLengthClause}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitTextLengthClause(CobolParser.TextLengthClauseContext ctx);
	/**
	 * Visit a parse tree produced by {@link CobolParser#localStorageSection}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitLocalStorageSection(CobolParser.LocalStorageSectionContext ctx);
	/**
	 * Visit a parse tree produced by {@link CobolParser#screenSection}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitScreenSection(CobolParser.ScreenSectionContext ctx);
	/**
	 * Visit a parse tree produced by {@link CobolParser#screenDescriptionEntry}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitScreenDescriptionEntry(CobolParser.ScreenDescriptionEntryContext ctx);
	/**
	 * Visit a parse tree produced by {@link CobolParser#screenDescriptionBlankClause}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitScreenDescriptionBlankClause(CobolParser.ScreenDescriptionBlankClauseContext ctx);
	/**
	 * Visit a parse tree produced by {@link CobolParser#screenDescriptionBellClause}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitScreenDescriptionBellClause(CobolParser.ScreenDescriptionBellClauseContext ctx);
	/**
	 * Visit a parse tree produced by {@link CobolParser#screenDescriptionBlinkClause}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitScreenDescriptionBlinkClause(CobolParser.ScreenDescriptionBlinkClauseContext ctx);
	/**
	 * Visit a parse tree produced by {@link CobolParser#screenDescriptionEraseClause}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitScreenDescriptionEraseClause(CobolParser.ScreenDescriptionEraseClauseContext ctx);
	/**
	 * Visit a parse tree produced by {@link CobolParser#screenDescriptionLightClause}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitScreenDescriptionLightClause(CobolParser.ScreenDescriptionLightClauseContext ctx);
	/**
	 * Visit a parse tree produced by {@link CobolParser#screenDescriptionGridClause}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitScreenDescriptionGridClause(CobolParser.ScreenDescriptionGridClauseContext ctx);
	/**
	 * Visit a parse tree produced by {@link CobolParser#screenDescriptionReverseVideoClause}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitScreenDescriptionReverseVideoClause(CobolParser.ScreenDescriptionReverseVideoClauseContext ctx);
	/**
	 * Visit a parse tree produced by {@link CobolParser#screenDescriptionUnderlineClause}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitScreenDescriptionUnderlineClause(CobolParser.ScreenDescriptionUnderlineClauseContext ctx);
	/**
	 * Visit a parse tree produced by {@link CobolParser#screenDescriptionSizeClause}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitScreenDescriptionSizeClause(CobolParser.ScreenDescriptionSizeClauseContext ctx);
	/**
	 * Visit a parse tree produced by {@link CobolParser#screenDescriptionLineClause}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitScreenDescriptionLineClause(CobolParser.ScreenDescriptionLineClauseContext ctx);
	/**
	 * Visit a parse tree produced by {@link CobolParser#screenDescriptionColumnClause}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitScreenDescriptionColumnClause(CobolParser.ScreenDescriptionColumnClauseContext ctx);
	/**
	 * Visit a parse tree produced by {@link CobolParser#screenDescriptionForegroundColorClause}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitScreenDescriptionForegroundColorClause(CobolParser.ScreenDescriptionForegroundColorClauseContext ctx);
	/**
	 * Visit a parse tree produced by {@link CobolParser#screenDescriptionBackgroundColorClause}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitScreenDescriptionBackgroundColorClause(CobolParser.ScreenDescriptionBackgroundColorClauseContext ctx);
	/**
	 * Visit a parse tree produced by {@link CobolParser#screenDescriptionControlClause}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitScreenDescriptionControlClause(CobolParser.ScreenDescriptionControlClauseContext ctx);
	/**
	 * Visit a parse tree produced by {@link CobolParser#screenDescriptionValueClause}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitScreenDescriptionValueClause(CobolParser.ScreenDescriptionValueClauseContext ctx);
	/**
	 * Visit a parse tree produced by {@link CobolParser#screenDescriptionPictureClause}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitScreenDescriptionPictureClause(CobolParser.ScreenDescriptionPictureClauseContext ctx);
	/**
	 * Visit a parse tree produced by {@link CobolParser#screenDescriptionFromClause}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitScreenDescriptionFromClause(CobolParser.ScreenDescriptionFromClauseContext ctx);
	/**
	 * Visit a parse tree produced by {@link CobolParser#screenDescriptionToClause}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitScreenDescriptionToClause(CobolParser.ScreenDescriptionToClauseContext ctx);
	/**
	 * Visit a parse tree produced by {@link CobolParser#screenDescriptionUsingClause}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitScreenDescriptionUsingClause(CobolParser.ScreenDescriptionUsingClauseContext ctx);
	/**
	 * Visit a parse tree produced by {@link CobolParser#screenDescriptionUsageClause}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitScreenDescriptionUsageClause(CobolParser.ScreenDescriptionUsageClauseContext ctx);
	/**
	 * Visit a parse tree produced by {@link CobolParser#screenDescriptionBlankWhenZeroClause}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitScreenDescriptionBlankWhenZeroClause(CobolParser.ScreenDescriptionBlankWhenZeroClauseContext ctx);
	/**
	 * Visit a parse tree produced by {@link CobolParser#screenDescriptionJustifiedClause}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitScreenDescriptionJustifiedClause(CobolParser.ScreenDescriptionJustifiedClauseContext ctx);
	/**
	 * Visit a parse tree produced by {@link CobolParser#screenDescriptionSignClause}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitScreenDescriptionSignClause(CobolParser.ScreenDescriptionSignClauseContext ctx);
	/**
	 * Visit a parse tree produced by {@link CobolParser#screenDescriptionAutoClause}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitScreenDescriptionAutoClause(CobolParser.ScreenDescriptionAutoClauseContext ctx);
	/**
	 * Visit a parse tree produced by {@link CobolParser#screenDescriptionSecureClause}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitScreenDescriptionSecureClause(CobolParser.ScreenDescriptionSecureClauseContext ctx);
	/**
	 * Visit a parse tree produced by {@link CobolParser#screenDescriptionRequiredClause}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitScreenDescriptionRequiredClause(CobolParser.ScreenDescriptionRequiredClauseContext ctx);
	/**
	 * Visit a parse tree produced by {@link CobolParser#screenDescriptionPromptClause}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitScreenDescriptionPromptClause(CobolParser.ScreenDescriptionPromptClauseContext ctx);
	/**
	 * Visit a parse tree produced by {@link CobolParser#screenDescriptionPromptOccursClause}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitScreenDescriptionPromptOccursClause(CobolParser.ScreenDescriptionPromptOccursClauseContext ctx);
	/**
	 * Visit a parse tree produced by {@link CobolParser#screenDescriptionFullClause}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitScreenDescriptionFullClause(CobolParser.ScreenDescriptionFullClauseContext ctx);
	/**
	 * Visit a parse tree produced by {@link CobolParser#screenDescriptionZeroFillClause}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitScreenDescriptionZeroFillClause(CobolParser.ScreenDescriptionZeroFillClauseContext ctx);
	/**
	 * Visit a parse tree produced by {@link CobolParser#reportSection}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitReportSection(CobolParser.ReportSectionContext ctx);
	/**
	 * Visit a parse tree produced by {@link CobolParser#reportDescription}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitReportDescription(CobolParser.ReportDescriptionContext ctx);
	/**
	 * Visit a parse tree produced by {@link CobolParser#reportDescriptionEntry}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitReportDescriptionEntry(CobolParser.ReportDescriptionEntryContext ctx);
	/**
	 * Visit a parse tree produced by {@link CobolParser#reportDescriptionGlobalClause}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitReportDescriptionGlobalClause(CobolParser.ReportDescriptionGlobalClauseContext ctx);
	/**
	 * Visit a parse tree produced by {@link CobolParser#reportDescriptionPageLimitClause}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitReportDescriptionPageLimitClause(CobolParser.ReportDescriptionPageLimitClauseContext ctx);
	/**
	 * Visit a parse tree produced by {@link CobolParser#reportDescriptionHeadingClause}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitReportDescriptionHeadingClause(CobolParser.ReportDescriptionHeadingClauseContext ctx);
	/**
	 * Visit a parse tree produced by {@link CobolParser#reportDescriptionFirstDetailClause}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitReportDescriptionFirstDetailClause(CobolParser.ReportDescriptionFirstDetailClauseContext ctx);
	/**
	 * Visit a parse tree produced by {@link CobolParser#reportDescriptionLastDetailClause}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitReportDescriptionLastDetailClause(CobolParser.ReportDescriptionLastDetailClauseContext ctx);
	/**
	 * Visit a parse tree produced by {@link CobolParser#reportDescriptionFootingClause}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitReportDescriptionFootingClause(CobolParser.ReportDescriptionFootingClauseContext ctx);
	/**
	 * Visit a parse tree produced by {@link CobolParser#reportGroupDescriptionEntry}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitReportGroupDescriptionEntry(CobolParser.ReportGroupDescriptionEntryContext ctx);
	/**
	 * Visit a parse tree produced by {@link CobolParser#reportGroupDescriptionEntryFormat1}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitReportGroupDescriptionEntryFormat1(CobolParser.ReportGroupDescriptionEntryFormat1Context ctx);
	/**
	 * Visit a parse tree produced by {@link CobolParser#reportGroupDescriptionEntryFormat2}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitReportGroupDescriptionEntryFormat2(CobolParser.ReportGroupDescriptionEntryFormat2Context ctx);
	/**
	 * Visit a parse tree produced by {@link CobolParser#reportGroupDescriptionEntryFormat3}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitReportGroupDescriptionEntryFormat3(CobolParser.ReportGroupDescriptionEntryFormat3Context ctx);
	/**
	 * Visit a parse tree produced by {@link CobolParser#reportGroupBlankWhenZeroClause}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitReportGroupBlankWhenZeroClause(CobolParser.ReportGroupBlankWhenZeroClauseContext ctx);
	/**
	 * Visit a parse tree produced by {@link CobolParser#reportGroupColumnNumberClause}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitReportGroupColumnNumberClause(CobolParser.ReportGroupColumnNumberClauseContext ctx);
	/**
	 * Visit a parse tree produced by {@link CobolParser#reportGroupIndicateClause}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitReportGroupIndicateClause(CobolParser.ReportGroupIndicateClauseContext ctx);
	/**
	 * Visit a parse tree produced by {@link CobolParser#reportGroupJustifiedClause}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitReportGroupJustifiedClause(CobolParser.ReportGroupJustifiedClauseContext ctx);
	/**
	 * Visit a parse tree produced by {@link CobolParser#reportGroupLineNumberClause}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitReportGroupLineNumberClause(CobolParser.ReportGroupLineNumberClauseContext ctx);
	/**
	 * Visit a parse tree produced by {@link CobolParser#reportGroupLineNumberNextPage}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitReportGroupLineNumberNextPage(CobolParser.ReportGroupLineNumberNextPageContext ctx);
	/**
	 * Visit a parse tree produced by {@link CobolParser#reportGroupLineNumberPlus}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitReportGroupLineNumberPlus(CobolParser.ReportGroupLineNumberPlusContext ctx);
	/**
	 * Visit a parse tree produced by {@link CobolParser#reportGroupNextGroupClause}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitReportGroupNextGroupClause(CobolParser.ReportGroupNextGroupClauseContext ctx);
	/**
	 * Visit a parse tree produced by {@link CobolParser#reportGroupNextGroupPlus}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitReportGroupNextGroupPlus(CobolParser.ReportGroupNextGroupPlusContext ctx);
	/**
	 * Visit a parse tree produced by {@link CobolParser#reportGroupNextGroupNextPage}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitReportGroupNextGroupNextPage(CobolParser.ReportGroupNextGroupNextPageContext ctx);
	/**
	 * Visit a parse tree produced by {@link CobolParser#reportGroupPictureClause}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitReportGroupPictureClause(CobolParser.ReportGroupPictureClauseContext ctx);
	/**
	 * Visit a parse tree produced by {@link CobolParser#reportGroupResetClause}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitReportGroupResetClause(CobolParser.ReportGroupResetClauseContext ctx);
	/**
	 * Visit a parse tree produced by {@link CobolParser#reportGroupSignClause}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitReportGroupSignClause(CobolParser.ReportGroupSignClauseContext ctx);
	/**
	 * Visit a parse tree produced by {@link CobolParser#reportGroupSourceClause}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitReportGroupSourceClause(CobolParser.ReportGroupSourceClauseContext ctx);
	/**
	 * Visit a parse tree produced by {@link CobolParser#reportGroupSumClause}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitReportGroupSumClause(CobolParser.ReportGroupSumClauseContext ctx);
	/**
	 * Visit a parse tree produced by {@link CobolParser#reportGroupTypeClause}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitReportGroupTypeClause(CobolParser.ReportGroupTypeClauseContext ctx);
	/**
	 * Visit a parse tree produced by {@link CobolParser#reportGroupTypeReportHeading}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitReportGroupTypeReportHeading(CobolParser.ReportGroupTypeReportHeadingContext ctx);
	/**
	 * Visit a parse tree produced by {@link CobolParser#reportGroupTypePageHeading}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitReportGroupTypePageHeading(CobolParser.ReportGroupTypePageHeadingContext ctx);
	/**
	 * Visit a parse tree produced by {@link CobolParser#reportGroupTypeControlHeading}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitReportGroupTypeControlHeading(CobolParser.ReportGroupTypeControlHeadingContext ctx);
	/**
	 * Visit a parse tree produced by {@link CobolParser#reportGroupTypeDetail}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitReportGroupTypeDetail(CobolParser.ReportGroupTypeDetailContext ctx);
	/**
	 * Visit a parse tree produced by {@link CobolParser#reportGroupTypeControlFooting}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitReportGroupTypeControlFooting(CobolParser.ReportGroupTypeControlFootingContext ctx);
	/**
	 * Visit a parse tree produced by {@link CobolParser#reportGroupUsageClause}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitReportGroupUsageClause(CobolParser.ReportGroupUsageClauseContext ctx);
	/**
	 * Visit a parse tree produced by {@link CobolParser#reportGroupTypePageFooting}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitReportGroupTypePageFooting(CobolParser.ReportGroupTypePageFootingContext ctx);
	/**
	 * Visit a parse tree produced by {@link CobolParser#reportGroupTypeReportFooting}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitReportGroupTypeReportFooting(CobolParser.ReportGroupTypeReportFootingContext ctx);
	/**
	 * Visit a parse tree produced by {@link CobolParser#reportGroupValueClause}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitReportGroupValueClause(CobolParser.ReportGroupValueClauseContext ctx);
	/**
	 * Visit a parse tree produced by {@link CobolParser#programLibrarySection}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitProgramLibrarySection(CobolParser.ProgramLibrarySectionContext ctx);
	/**
	 * Visit a parse tree produced by {@link CobolParser#libraryDescriptionEntry}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitLibraryDescriptionEntry(CobolParser.LibraryDescriptionEntryContext ctx);
	/**
	 * Visit a parse tree produced by {@link CobolParser#libraryDescriptionEntryFormat1}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitLibraryDescriptionEntryFormat1(CobolParser.LibraryDescriptionEntryFormat1Context ctx);
	/**
	 * Visit a parse tree produced by {@link CobolParser#libraryDescriptionEntryFormat2}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitLibraryDescriptionEntryFormat2(CobolParser.LibraryDescriptionEntryFormat2Context ctx);
	/**
	 * Visit a parse tree produced by {@link CobolParser#libraryAttributeClauseFormat1}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitLibraryAttributeClauseFormat1(CobolParser.LibraryAttributeClauseFormat1Context ctx);
	/**
	 * Visit a parse tree produced by {@link CobolParser#libraryAttributeClauseFormat2}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitLibraryAttributeClauseFormat2(CobolParser.LibraryAttributeClauseFormat2Context ctx);
	/**
	 * Visit a parse tree produced by {@link CobolParser#libraryAttributeFunction}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitLibraryAttributeFunction(CobolParser.LibraryAttributeFunctionContext ctx);
	/**
	 * Visit a parse tree produced by {@link CobolParser#libraryAttributeParameter}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitLibraryAttributeParameter(CobolParser.LibraryAttributeParameterContext ctx);
	/**
	 * Visit a parse tree produced by {@link CobolParser#libraryAttributeTitle}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitLibraryAttributeTitle(CobolParser.LibraryAttributeTitleContext ctx);
	/**
	 * Visit a parse tree produced by {@link CobolParser#libraryEntryProcedureClauseFormat1}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitLibraryEntryProcedureClauseFormat1(CobolParser.LibraryEntryProcedureClauseFormat1Context ctx);
	/**
	 * Visit a parse tree produced by {@link CobolParser#libraryEntryProcedureClauseFormat2}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitLibraryEntryProcedureClauseFormat2(CobolParser.LibraryEntryProcedureClauseFormat2Context ctx);
	/**
	 * Visit a parse tree produced by {@link CobolParser#libraryEntryProcedureForClause}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitLibraryEntryProcedureForClause(CobolParser.LibraryEntryProcedureForClauseContext ctx);
	/**
	 * Visit a parse tree produced by {@link CobolParser#libraryEntryProcedureGivingClause}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitLibraryEntryProcedureGivingClause(CobolParser.LibraryEntryProcedureGivingClauseContext ctx);
	/**
	 * Visit a parse tree produced by {@link CobolParser#libraryEntryProcedureUsingClause}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitLibraryEntryProcedureUsingClause(CobolParser.LibraryEntryProcedureUsingClauseContext ctx);
	/**
	 * Visit a parse tree produced by {@link CobolParser#libraryEntryProcedureUsingName}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitLibraryEntryProcedureUsingName(CobolParser.LibraryEntryProcedureUsingNameContext ctx);
	/**
	 * Visit a parse tree produced by {@link CobolParser#libraryEntryProcedureWithClause}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitLibraryEntryProcedureWithClause(CobolParser.LibraryEntryProcedureWithClauseContext ctx);
	/**
	 * Visit a parse tree produced by {@link CobolParser#libraryEntryProcedureWithName}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitLibraryEntryProcedureWithName(CobolParser.LibraryEntryProcedureWithNameContext ctx);
	/**
	 * Visit a parse tree produced by {@link CobolParser#libraryIsCommonClause}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitLibraryIsCommonClause(CobolParser.LibraryIsCommonClauseContext ctx);
	/**
	 * Visit a parse tree produced by {@link CobolParser#libraryIsGlobalClause}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitLibraryIsGlobalClause(CobolParser.LibraryIsGlobalClauseContext ctx);
	/**
	 * Visit a parse tree produced by {@link CobolParser#dataDescriptionEntry}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitDataDescriptionEntry(CobolParser.DataDescriptionEntryContext ctx);
	/**
	 * Visit a parse tree produced by {@link CobolParser#dataDescriptionEntryFormat1}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitDataDescriptionEntryFormat1(CobolParser.DataDescriptionEntryFormat1Context ctx);
	/**
	 * Visit a parse tree produced by {@link CobolParser#dataDescriptionEntryFormat2}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitDataDescriptionEntryFormat2(CobolParser.DataDescriptionEntryFormat2Context ctx);
	/**
	 * Visit a parse tree produced by {@link CobolParser#dataDescriptionEntryFormat3}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitDataDescriptionEntryFormat3(CobolParser.DataDescriptionEntryFormat3Context ctx);
	/**
	 * Visit a parse tree produced by {@link CobolParser#dataDescriptionEntryExecSql}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitDataDescriptionEntryExecSql(CobolParser.DataDescriptionEntryExecSqlContext ctx);
	/**
	 * Visit a parse tree produced by {@link CobolParser#dataAlignedClause}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitDataAlignedClause(CobolParser.DataAlignedClauseContext ctx);
	/**
	 * Visit a parse tree produced by {@link CobolParser#dataBlankWhenZeroClause}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitDataBlankWhenZeroClause(CobolParser.DataBlankWhenZeroClauseContext ctx);
	/**
	 * Visit a parse tree produced by {@link CobolParser#dataCommonOwnLocalClause}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitDataCommonOwnLocalClause(CobolParser.DataCommonOwnLocalClauseContext ctx);
	/**
	 * Visit a parse tree produced by {@link CobolParser#dataExternalClause}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitDataExternalClause(CobolParser.DataExternalClauseContext ctx);
	/**
	 * Visit a parse tree produced by {@link CobolParser#dataGlobalClause}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitDataGlobalClause(CobolParser.DataGlobalClauseContext ctx);
	/**
	 * Visit a parse tree produced by {@link CobolParser#dataIntegerStringClause}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitDataIntegerStringClause(CobolParser.DataIntegerStringClauseContext ctx);
	/**
	 * Visit a parse tree produced by {@link CobolParser#dataJustifiedClause}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitDataJustifiedClause(CobolParser.DataJustifiedClauseContext ctx);
	/**
	 * Visit a parse tree produced by {@link CobolParser#dataOccursClause}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitDataOccursClause(CobolParser.DataOccursClauseContext ctx);
	/**
	 * Visit a parse tree produced by {@link CobolParser#dataOccursTo}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitDataOccursTo(CobolParser.DataOccursToContext ctx);
	/**
	 * Visit a parse tree produced by {@link CobolParser#dataOccursDepending}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitDataOccursDepending(CobolParser.DataOccursDependingContext ctx);
	/**
	 * Visit a parse tree produced by {@link CobolParser#dataOccursSort}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitDataOccursSort(CobolParser.DataOccursSortContext ctx);
	/**
	 * Visit a parse tree produced by {@link CobolParser#dataOccursIndexed}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitDataOccursIndexed(CobolParser.DataOccursIndexedContext ctx);
	/**
	 * Visit a parse tree produced by {@link CobolParser#dataPictureClause}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitDataPictureClause(CobolParser.DataPictureClauseContext ctx);
	/**
	 * Visit a parse tree produced by {@link CobolParser#pictureString}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitPictureString(CobolParser.PictureStringContext ctx);
	/**
	 * Visit a parse tree produced by {@link CobolParser#pictureChars}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitPictureChars(CobolParser.PictureCharsContext ctx);
	/**
	 * Visit a parse tree produced by {@link CobolParser#pictureCardinality}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitPictureCardinality(CobolParser.PictureCardinalityContext ctx);
	/**
	 * Visit a parse tree produced by {@link CobolParser#dataReceivedByClause}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitDataReceivedByClause(CobolParser.DataReceivedByClauseContext ctx);
	/**
	 * Visit a parse tree produced by {@link CobolParser#dataRecordAreaClause}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitDataRecordAreaClause(CobolParser.DataRecordAreaClauseContext ctx);
	/**
	 * Visit a parse tree produced by {@link CobolParser#dataRedefinesClause}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitDataRedefinesClause(CobolParser.DataRedefinesClauseContext ctx);
	/**
	 * Visit a parse tree produced by {@link CobolParser#dataRenamesClause}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitDataRenamesClause(CobolParser.DataRenamesClauseContext ctx);
	/**
	 * Visit a parse tree produced by {@link CobolParser#dataSignClause}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitDataSignClause(CobolParser.DataSignClauseContext ctx);
	/**
	 * Visit a parse tree produced by {@link CobolParser#dataSynchronizedClause}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitDataSynchronizedClause(CobolParser.DataSynchronizedClauseContext ctx);
	/**
	 * Visit a parse tree produced by {@link CobolParser#dataThreadLocalClause}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitDataThreadLocalClause(CobolParser.DataThreadLocalClauseContext ctx);
	/**
	 * Visit a parse tree produced by {@link CobolParser#dataTypeClause}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitDataTypeClause(CobolParser.DataTypeClauseContext ctx);
	/**
	 * Visit a parse tree produced by {@link CobolParser#dataTypeDefClause}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitDataTypeDefClause(CobolParser.DataTypeDefClauseContext ctx);
	/**
	 * Visit a parse tree produced by {@link CobolParser#dataUsageClause}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitDataUsageClause(CobolParser.DataUsageClauseContext ctx);
	/**
	 * Visit a parse tree produced by {@link CobolParser#dataUsingClause}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitDataUsingClause(CobolParser.DataUsingClauseContext ctx);
	/**
	 * Visit a parse tree produced by {@link CobolParser#dataValueClause}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitDataValueClause(CobolParser.DataValueClauseContext ctx);
	/**
	 * Visit a parse tree produced by {@link CobolParser#dataValueInterval}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitDataValueInterval(CobolParser.DataValueIntervalContext ctx);
	/**
	 * Visit a parse tree produced by {@link CobolParser#dataValueIntervalFrom}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitDataValueIntervalFrom(CobolParser.DataValueIntervalFromContext ctx);
	/**
	 * Visit a parse tree produced by {@link CobolParser#dataValueIntervalTo}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitDataValueIntervalTo(CobolParser.DataValueIntervalToContext ctx);
	/**
	 * Visit a parse tree produced by {@link CobolParser#dataWithLowerBoundsClause}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitDataWithLowerBoundsClause(CobolParser.DataWithLowerBoundsClauseContext ctx);
	/**
	 * Visit a parse tree produced by {@link CobolParser#procedureDivision}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitProcedureDivision(CobolParser.ProcedureDivisionContext ctx);
	/**
	 * Visit a parse tree produced by {@link CobolParser#procedureDivisionUsingClause}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitProcedureDivisionUsingClause(CobolParser.ProcedureDivisionUsingClauseContext ctx);
	/**
	 * Visit a parse tree produced by {@link CobolParser#procedureDivisionGivingClause}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitProcedureDivisionGivingClause(CobolParser.ProcedureDivisionGivingClauseContext ctx);
	/**
	 * Visit a parse tree produced by {@link CobolParser#procedureDivisionUsingParameter}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitProcedureDivisionUsingParameter(CobolParser.ProcedureDivisionUsingParameterContext ctx);
	/**
	 * Visit a parse tree produced by {@link CobolParser#procedureDivisionByReferencePhrase}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitProcedureDivisionByReferencePhrase(CobolParser.ProcedureDivisionByReferencePhraseContext ctx);
	/**
	 * Visit a parse tree produced by {@link CobolParser#procedureDivisionByReference}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitProcedureDivisionByReference(CobolParser.ProcedureDivisionByReferenceContext ctx);
	/**
	 * Visit a parse tree produced by {@link CobolParser#procedureDivisionByValuePhrase}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitProcedureDivisionByValuePhrase(CobolParser.ProcedureDivisionByValuePhraseContext ctx);
	/**
	 * Visit a parse tree produced by {@link CobolParser#procedureDivisionByValue}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitProcedureDivisionByValue(CobolParser.ProcedureDivisionByValueContext ctx);
	/**
	 * Visit a parse tree produced by {@link CobolParser#procedureDeclaratives}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitProcedureDeclaratives(CobolParser.ProcedureDeclarativesContext ctx);
	/**
	 * Visit a parse tree produced by {@link CobolParser#procedureDeclarative}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitProcedureDeclarative(CobolParser.ProcedureDeclarativeContext ctx);
	/**
	 * Visit a parse tree produced by {@link CobolParser#procedureSectionHeader}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitProcedureSectionHeader(CobolParser.ProcedureSectionHeaderContext ctx);
	/**
	 * Visit a parse tree produced by {@link CobolParser#procedureDivisionBody}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitProcedureDivisionBody(CobolParser.ProcedureDivisionBodyContext ctx);
	/**
	 * Visit a parse tree produced by {@link CobolParser#procedureSection}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitProcedureSection(CobolParser.ProcedureSectionContext ctx);
	/**
	 * Visit a parse tree produced by {@link CobolParser#paragraphs}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitParagraphs(CobolParser.ParagraphsContext ctx);
	/**
	 * Visit a parse tree produced by {@link CobolParser#paragraph}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitParagraph(CobolParser.ParagraphContext ctx);
	/**
	 * Visit a parse tree produced by {@link CobolParser#sentence}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitSentence(CobolParser.SentenceContext ctx);
	/**
	 * Visit a parse tree produced by {@link CobolParser#statement}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitStatement(CobolParser.StatementContext ctx);
	/**
	 * Visit a parse tree produced by {@link CobolParser#acceptStatement}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitAcceptStatement(CobolParser.AcceptStatementContext ctx);
	/**
	 * Visit a parse tree produced by {@link CobolParser#acceptFromDateStatement}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitAcceptFromDateStatement(CobolParser.AcceptFromDateStatementContext ctx);
	/**
	 * Visit a parse tree produced by {@link CobolParser#acceptFromMnemonicStatement}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitAcceptFromMnemonicStatement(CobolParser.AcceptFromMnemonicStatementContext ctx);
	/**
	 * Visit a parse tree produced by {@link CobolParser#acceptFromEscapeKeyStatement}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitAcceptFromEscapeKeyStatement(CobolParser.AcceptFromEscapeKeyStatementContext ctx);
	/**
	 * Visit a parse tree produced by {@link CobolParser#acceptMessageCountStatement}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitAcceptMessageCountStatement(CobolParser.AcceptMessageCountStatementContext ctx);
	/**
	 * Visit a parse tree produced by {@link CobolParser#addStatement}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitAddStatement(CobolParser.AddStatementContext ctx);
	/**
	 * Visit a parse tree produced by {@link CobolParser#addToStatement}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitAddToStatement(CobolParser.AddToStatementContext ctx);
	/**
	 * Visit a parse tree produced by {@link CobolParser#addToGivingStatement}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitAddToGivingStatement(CobolParser.AddToGivingStatementContext ctx);
	/**
	 * Visit a parse tree produced by {@link CobolParser#addCorrespondingStatement}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitAddCorrespondingStatement(CobolParser.AddCorrespondingStatementContext ctx);
	/**
	 * Visit a parse tree produced by {@link CobolParser#addFrom}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitAddFrom(CobolParser.AddFromContext ctx);
	/**
	 * Visit a parse tree produced by {@link CobolParser#addTo}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitAddTo(CobolParser.AddToContext ctx);
	/**
	 * Visit a parse tree produced by {@link CobolParser#addToGiving}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitAddToGiving(CobolParser.AddToGivingContext ctx);
	/**
	 * Visit a parse tree produced by {@link CobolParser#addGiving}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitAddGiving(CobolParser.AddGivingContext ctx);
	/**
	 * Visit a parse tree produced by {@link CobolParser#alteredGoTo}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitAlteredGoTo(CobolParser.AlteredGoToContext ctx);
	/**
	 * Visit a parse tree produced by {@link CobolParser#alterStatement}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitAlterStatement(CobolParser.AlterStatementContext ctx);
	/**
	 * Visit a parse tree produced by {@link CobolParser#alterProceedTo}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitAlterProceedTo(CobolParser.AlterProceedToContext ctx);
	/**
	 * Visit a parse tree produced by {@link CobolParser#callStatement}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitCallStatement(CobolParser.CallStatementContext ctx);
	/**
	 * Visit a parse tree produced by {@link CobolParser#callUsingPhrase}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitCallUsingPhrase(CobolParser.CallUsingPhraseContext ctx);
	/**
	 * Visit a parse tree produced by {@link CobolParser#callUsingParameter}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitCallUsingParameter(CobolParser.CallUsingParameterContext ctx);
	/**
	 * Visit a parse tree produced by {@link CobolParser#callByReferencePhrase}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitCallByReferencePhrase(CobolParser.CallByReferencePhraseContext ctx);
	/**
	 * Visit a parse tree produced by {@link CobolParser#callByReference}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitCallByReference(CobolParser.CallByReferenceContext ctx);
	/**
	 * Visit a parse tree produced by {@link CobolParser#callByValuePhrase}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitCallByValuePhrase(CobolParser.CallByValuePhraseContext ctx);
	/**
	 * Visit a parse tree produced by {@link CobolParser#callByValue}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitCallByValue(CobolParser.CallByValueContext ctx);
	/**
	 * Visit a parse tree produced by {@link CobolParser#callByContentPhrase}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitCallByContentPhrase(CobolParser.CallByContentPhraseContext ctx);
	/**
	 * Visit a parse tree produced by {@link CobolParser#callByContent}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitCallByContent(CobolParser.CallByContentContext ctx);
	/**
	 * Visit a parse tree produced by {@link CobolParser#callGivingPhrase}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitCallGivingPhrase(CobolParser.CallGivingPhraseContext ctx);
	/**
	 * Visit a parse tree produced by {@link CobolParser#cancelStatement}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitCancelStatement(CobolParser.CancelStatementContext ctx);
	/**
	 * Visit a parse tree produced by {@link CobolParser#cancelCall}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitCancelCall(CobolParser.CancelCallContext ctx);
	/**
	 * Visit a parse tree produced by {@link CobolParser#closeStatement}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitCloseStatement(CobolParser.CloseStatementContext ctx);
	/**
	 * Visit a parse tree produced by {@link CobolParser#closeFile}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitCloseFile(CobolParser.CloseFileContext ctx);
	/**
	 * Visit a parse tree produced by {@link CobolParser#closeReelUnitStatement}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitCloseReelUnitStatement(CobolParser.CloseReelUnitStatementContext ctx);
	/**
	 * Visit a parse tree produced by {@link CobolParser#closeRelativeStatement}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitCloseRelativeStatement(CobolParser.CloseRelativeStatementContext ctx);
	/**
	 * Visit a parse tree produced by {@link CobolParser#closePortFileIOStatement}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitClosePortFileIOStatement(CobolParser.ClosePortFileIOStatementContext ctx);
	/**
	 * Visit a parse tree produced by {@link CobolParser#closePortFileIOUsing}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitClosePortFileIOUsing(CobolParser.ClosePortFileIOUsingContext ctx);
	/**
	 * Visit a parse tree produced by {@link CobolParser#closePortFileIOUsingCloseDisposition}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitClosePortFileIOUsingCloseDisposition(CobolParser.ClosePortFileIOUsingCloseDispositionContext ctx);
	/**
	 * Visit a parse tree produced by {@link CobolParser#closePortFileIOUsingAssociatedData}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitClosePortFileIOUsingAssociatedData(CobolParser.ClosePortFileIOUsingAssociatedDataContext ctx);
	/**
	 * Visit a parse tree produced by {@link CobolParser#closePortFileIOUsingAssociatedDataLength}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitClosePortFileIOUsingAssociatedDataLength(CobolParser.ClosePortFileIOUsingAssociatedDataLengthContext ctx);
	/**
	 * Visit a parse tree produced by {@link CobolParser#computeStatement}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitComputeStatement(CobolParser.ComputeStatementContext ctx);
	/**
	 * Visit a parse tree produced by {@link CobolParser#computeStore}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitComputeStore(CobolParser.ComputeStoreContext ctx);
	/**
	 * Visit a parse tree produced by {@link CobolParser#continueStatement}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitContinueStatement(CobolParser.ContinueStatementContext ctx);
	/**
	 * Visit a parse tree produced by {@link CobolParser#deleteStatement}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitDeleteStatement(CobolParser.DeleteStatementContext ctx);
	/**
	 * Visit a parse tree produced by {@link CobolParser#disableStatement}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitDisableStatement(CobolParser.DisableStatementContext ctx);
	/**
	 * Visit a parse tree produced by {@link CobolParser#displayStatement}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitDisplayStatement(CobolParser.DisplayStatementContext ctx);
	/**
	 * Visit a parse tree produced by {@link CobolParser#displayOperand}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitDisplayOperand(CobolParser.DisplayOperandContext ctx);
	/**
	 * Visit a parse tree produced by {@link CobolParser#displayAt}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitDisplayAt(CobolParser.DisplayAtContext ctx);
	/**
	 * Visit a parse tree produced by {@link CobolParser#displayUpon}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitDisplayUpon(CobolParser.DisplayUponContext ctx);
	/**
	 * Visit a parse tree produced by {@link CobolParser#displayWith}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitDisplayWith(CobolParser.DisplayWithContext ctx);
	/**
	 * Visit a parse tree produced by {@link CobolParser#divideStatement}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitDivideStatement(CobolParser.DivideStatementContext ctx);
	/**
	 * Visit a parse tree produced by {@link CobolParser#divideIntoStatement}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitDivideIntoStatement(CobolParser.DivideIntoStatementContext ctx);
	/**
	 * Visit a parse tree produced by {@link CobolParser#divideIntoGivingStatement}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitDivideIntoGivingStatement(CobolParser.DivideIntoGivingStatementContext ctx);
	/**
	 * Visit a parse tree produced by {@link CobolParser#divideByGivingStatement}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitDivideByGivingStatement(CobolParser.DivideByGivingStatementContext ctx);
	/**
	 * Visit a parse tree produced by {@link CobolParser#divideGivingPhrase}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitDivideGivingPhrase(CobolParser.DivideGivingPhraseContext ctx);
	/**
	 * Visit a parse tree produced by {@link CobolParser#divideInto}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitDivideInto(CobolParser.DivideIntoContext ctx);
	/**
	 * Visit a parse tree produced by {@link CobolParser#divideGiving}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitDivideGiving(CobolParser.DivideGivingContext ctx);
	/**
	 * Visit a parse tree produced by {@link CobolParser#divideRemainder}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitDivideRemainder(CobolParser.DivideRemainderContext ctx);
	/**
	 * Visit a parse tree produced by {@link CobolParser#enableStatement}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitEnableStatement(CobolParser.EnableStatementContext ctx);
	/**
	 * Visit a parse tree produced by {@link CobolParser#entryStatement}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitEntryStatement(CobolParser.EntryStatementContext ctx);
	/**
	 * Visit a parse tree produced by {@link CobolParser#evaluateStatement}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitEvaluateStatement(CobolParser.EvaluateStatementContext ctx);
	/**
	 * Visit a parse tree produced by {@link CobolParser#evaluateSelect}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitEvaluateSelect(CobolParser.EvaluateSelectContext ctx);
	/**
	 * Visit a parse tree produced by {@link CobolParser#evaluateAlsoSelect}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitEvaluateAlsoSelect(CobolParser.EvaluateAlsoSelectContext ctx);
	/**
	 * Visit a parse tree produced by {@link CobolParser#evaluateWhenPhrase}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitEvaluateWhenPhrase(CobolParser.EvaluateWhenPhraseContext ctx);
	/**
	 * Visit a parse tree produced by {@link CobolParser#evaluateWhen}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitEvaluateWhen(CobolParser.EvaluateWhenContext ctx);
	/**
	 * Visit a parse tree produced by {@link CobolParser#evaluateCondition}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitEvaluateCondition(CobolParser.EvaluateConditionContext ctx);
	/**
	 * Visit a parse tree produced by {@link CobolParser#evaluateThrough}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitEvaluateThrough(CobolParser.EvaluateThroughContext ctx);
	/**
	 * Visit a parse tree produced by {@link CobolParser#evaluateAlsoCondition}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitEvaluateAlsoCondition(CobolParser.EvaluateAlsoConditionContext ctx);
	/**
	 * Visit a parse tree produced by {@link CobolParser#evaluateWhenOther}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitEvaluateWhenOther(CobolParser.EvaluateWhenOtherContext ctx);
	/**
	 * Visit a parse tree produced by {@link CobolParser#evaluateValue}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitEvaluateValue(CobolParser.EvaluateValueContext ctx);
	/**
	 * Visit a parse tree produced by {@link CobolParser#execCicsStatement}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitExecCicsStatement(CobolParser.ExecCicsStatementContext ctx);
	/**
	 * Visit a parse tree produced by {@link CobolParser#execSqlStatement}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitExecSqlStatement(CobolParser.ExecSqlStatementContext ctx);
	/**
	 * Visit a parse tree produced by {@link CobolParser#execSqlImsStatement}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitExecSqlImsStatement(CobolParser.ExecSqlImsStatementContext ctx);
	/**
	 * Visit a parse tree produced by {@link CobolParser#exhibitStatement}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitExhibitStatement(CobolParser.ExhibitStatementContext ctx);
	/**
	 * Visit a parse tree produced by {@link CobolParser#exhibitOperand}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitExhibitOperand(CobolParser.ExhibitOperandContext ctx);
	/**
	 * Visit a parse tree produced by {@link CobolParser#exitStatement}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitExitStatement(CobolParser.ExitStatementContext ctx);
	/**
	 * Visit a parse tree produced by {@link CobolParser#generateStatement}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitGenerateStatement(CobolParser.GenerateStatementContext ctx);
	/**
	 * Visit a parse tree produced by {@link CobolParser#gobackStatement}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitGobackStatement(CobolParser.GobackStatementContext ctx);
	/**
	 * Visit a parse tree produced by {@link CobolParser#goToStatement}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitGoToStatement(CobolParser.GoToStatementContext ctx);
	/**
	 * Visit a parse tree produced by {@link CobolParser#goToStatementSimple}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitGoToStatementSimple(CobolParser.GoToStatementSimpleContext ctx);
	/**
	 * Visit a parse tree produced by {@link CobolParser#goToDependingOnStatement}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitGoToDependingOnStatement(CobolParser.GoToDependingOnStatementContext ctx);
	/**
	 * Visit a parse tree produced by {@link CobolParser#ifStatement}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitIfStatement(CobolParser.IfStatementContext ctx);
	/**
	 * Visit a parse tree produced by {@link CobolParser#ifThen}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitIfThen(CobolParser.IfThenContext ctx);
	/**
	 * Visit a parse tree produced by {@link CobolParser#ifElse}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitIfElse(CobolParser.IfElseContext ctx);
	/**
	 * Visit a parse tree produced by {@link CobolParser#initializeStatement}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitInitializeStatement(CobolParser.InitializeStatementContext ctx);
	/**
	 * Visit a parse tree produced by {@link CobolParser#initializeReplacingPhrase}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitInitializeReplacingPhrase(CobolParser.InitializeReplacingPhraseContext ctx);
	/**
	 * Visit a parse tree produced by {@link CobolParser#initializeReplacingBy}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitInitializeReplacingBy(CobolParser.InitializeReplacingByContext ctx);
	/**
	 * Visit a parse tree produced by {@link CobolParser#initiateStatement}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitInitiateStatement(CobolParser.InitiateStatementContext ctx);
	/**
	 * Visit a parse tree produced by {@link CobolParser#inspectStatement}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitInspectStatement(CobolParser.InspectStatementContext ctx);
	/**
	 * Visit a parse tree produced by {@link CobolParser#inspectTallyingPhrase}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitInspectTallyingPhrase(CobolParser.InspectTallyingPhraseContext ctx);
	/**
	 * Visit a parse tree produced by {@link CobolParser#inspectReplacingPhrase}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitInspectReplacingPhrase(CobolParser.InspectReplacingPhraseContext ctx);
	/**
	 * Visit a parse tree produced by {@link CobolParser#inspectTallyingReplacingPhrase}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitInspectTallyingReplacingPhrase(CobolParser.InspectTallyingReplacingPhraseContext ctx);
	/**
	 * Visit a parse tree produced by {@link CobolParser#inspectConvertingPhrase}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitInspectConvertingPhrase(CobolParser.InspectConvertingPhraseContext ctx);
	/**
	 * Visit a parse tree produced by {@link CobolParser#inspectFor}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitInspectFor(CobolParser.InspectForContext ctx);
	/**
	 * Visit a parse tree produced by {@link CobolParser#inspectCharacters}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitInspectCharacters(CobolParser.InspectCharactersContext ctx);
	/**
	 * Visit a parse tree produced by {@link CobolParser#inspectReplacingCharacters}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitInspectReplacingCharacters(CobolParser.InspectReplacingCharactersContext ctx);
	/**
	 * Visit a parse tree produced by {@link CobolParser#inspectAllLeadings}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitInspectAllLeadings(CobolParser.InspectAllLeadingsContext ctx);
	/**
	 * Visit a parse tree produced by {@link CobolParser#inspectReplacingAllLeadings}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitInspectReplacingAllLeadings(CobolParser.InspectReplacingAllLeadingsContext ctx);
	/**
	 * Visit a parse tree produced by {@link CobolParser#inspectAllLeading}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitInspectAllLeading(CobolParser.InspectAllLeadingContext ctx);
	/**
	 * Visit a parse tree produced by {@link CobolParser#inspectReplacingAllLeading}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitInspectReplacingAllLeading(CobolParser.InspectReplacingAllLeadingContext ctx);
	/**
	 * Visit a parse tree produced by {@link CobolParser#inspectBy}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitInspectBy(CobolParser.InspectByContext ctx);
	/**
	 * Visit a parse tree produced by {@link CobolParser#inspectTo}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitInspectTo(CobolParser.InspectToContext ctx);
	/**
	 * Visit a parse tree produced by {@link CobolParser#inspectBeforeAfter}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitInspectBeforeAfter(CobolParser.InspectBeforeAfterContext ctx);
	/**
	 * Visit a parse tree produced by {@link CobolParser#mergeStatement}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitMergeStatement(CobolParser.MergeStatementContext ctx);
	/**
	 * Visit a parse tree produced by {@link CobolParser#mergeOnKeyClause}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitMergeOnKeyClause(CobolParser.MergeOnKeyClauseContext ctx);
	/**
	 * Visit a parse tree produced by {@link CobolParser#mergeCollatingSequencePhrase}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitMergeCollatingSequencePhrase(CobolParser.MergeCollatingSequencePhraseContext ctx);
	/**
	 * Visit a parse tree produced by {@link CobolParser#mergeCollatingAlphanumeric}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitMergeCollatingAlphanumeric(CobolParser.MergeCollatingAlphanumericContext ctx);
	/**
	 * Visit a parse tree produced by {@link CobolParser#mergeCollatingNational}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitMergeCollatingNational(CobolParser.MergeCollatingNationalContext ctx);
	/**
	 * Visit a parse tree produced by {@link CobolParser#mergeUsing}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitMergeUsing(CobolParser.MergeUsingContext ctx);
	/**
	 * Visit a parse tree produced by {@link CobolParser#mergeOutputProcedurePhrase}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitMergeOutputProcedurePhrase(CobolParser.MergeOutputProcedurePhraseContext ctx);
	/**
	 * Visit a parse tree produced by {@link CobolParser#mergeOutputThrough}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitMergeOutputThrough(CobolParser.MergeOutputThroughContext ctx);
	/**
	 * Visit a parse tree produced by {@link CobolParser#mergeGivingPhrase}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitMergeGivingPhrase(CobolParser.MergeGivingPhraseContext ctx);
	/**
	 * Visit a parse tree produced by {@link CobolParser#mergeGiving}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitMergeGiving(CobolParser.MergeGivingContext ctx);
	/**
	 * Visit a parse tree produced by {@link CobolParser#moveStatement}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitMoveStatement(CobolParser.MoveStatementContext ctx);
	/**
	 * Visit a parse tree produced by {@link CobolParser#moveToStatement}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitMoveToStatement(CobolParser.MoveToStatementContext ctx);
	/**
	 * Visit a parse tree produced by {@link CobolParser#moveToSendingArea}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitMoveToSendingArea(CobolParser.MoveToSendingAreaContext ctx);
	/**
	 * Visit a parse tree produced by {@link CobolParser#moveCorrespondingToStatement}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitMoveCorrespondingToStatement(CobolParser.MoveCorrespondingToStatementContext ctx);
	/**
	 * Visit a parse tree produced by {@link CobolParser#moveCorrespondingToSendingArea}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitMoveCorrespondingToSendingArea(CobolParser.MoveCorrespondingToSendingAreaContext ctx);
	/**
	 * Visit a parse tree produced by {@link CobolParser#multiplyStatement}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitMultiplyStatement(CobolParser.MultiplyStatementContext ctx);
	/**
	 * Visit a parse tree produced by {@link CobolParser#multiplyRegular}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitMultiplyRegular(CobolParser.MultiplyRegularContext ctx);
	/**
	 * Visit a parse tree produced by {@link CobolParser#multiplyRegularOperand}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitMultiplyRegularOperand(CobolParser.MultiplyRegularOperandContext ctx);
	/**
	 * Visit a parse tree produced by {@link CobolParser#multiplyGiving}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitMultiplyGiving(CobolParser.MultiplyGivingContext ctx);
	/**
	 * Visit a parse tree produced by {@link CobolParser#multiplyGivingOperand}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitMultiplyGivingOperand(CobolParser.MultiplyGivingOperandContext ctx);
	/**
	 * Visit a parse tree produced by {@link CobolParser#multiplyGivingResult}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitMultiplyGivingResult(CobolParser.MultiplyGivingResultContext ctx);
	/**
	 * Visit a parse tree produced by {@link CobolParser#nextSentenceStatement}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitNextSentenceStatement(CobolParser.NextSentenceStatementContext ctx);
	/**
	 * Visit a parse tree produced by {@link CobolParser#openStatement}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitOpenStatement(CobolParser.OpenStatementContext ctx);
	/**
	 * Visit a parse tree produced by {@link CobolParser#openInputStatement}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitOpenInputStatement(CobolParser.OpenInputStatementContext ctx);
	/**
	 * Visit a parse tree produced by {@link CobolParser#openInput}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitOpenInput(CobolParser.OpenInputContext ctx);
	/**
	 * Visit a parse tree produced by {@link CobolParser#openOutputStatement}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitOpenOutputStatement(CobolParser.OpenOutputStatementContext ctx);
	/**
	 * Visit a parse tree produced by {@link CobolParser#openOutput}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitOpenOutput(CobolParser.OpenOutputContext ctx);
	/**
	 * Visit a parse tree produced by {@link CobolParser#openIOStatement}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitOpenIOStatement(CobolParser.OpenIOStatementContext ctx);
	/**
	 * Visit a parse tree produced by {@link CobolParser#openExtendStatement}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitOpenExtendStatement(CobolParser.OpenExtendStatementContext ctx);
	/**
	 * Visit a parse tree produced by {@link CobolParser#performStatement}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitPerformStatement(CobolParser.PerformStatementContext ctx);
	/**
	 * Visit a parse tree produced by {@link CobolParser#performInlineStatement}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitPerformInlineStatement(CobolParser.PerformInlineStatementContext ctx);
	/**
	 * Visit a parse tree produced by {@link CobolParser#performProcedureStatement}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitPerformProcedureStatement(CobolParser.PerformProcedureStatementContext ctx);
	/**
	 * Visit a parse tree produced by {@link CobolParser#performType}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitPerformType(CobolParser.PerformTypeContext ctx);
	/**
	 * Visit a parse tree produced by {@link CobolParser#performTimes}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitPerformTimes(CobolParser.PerformTimesContext ctx);
	/**
	 * Visit a parse tree produced by {@link CobolParser#performUntil}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitPerformUntil(CobolParser.PerformUntilContext ctx);
	/**
	 * Visit a parse tree produced by {@link CobolParser#performVarying}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitPerformVarying(CobolParser.PerformVaryingContext ctx);
	/**
	 * Visit a parse tree produced by {@link CobolParser#performVaryingClause}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitPerformVaryingClause(CobolParser.PerformVaryingClauseContext ctx);
	/**
	 * Visit a parse tree produced by {@link CobolParser#performVaryingPhrase}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitPerformVaryingPhrase(CobolParser.PerformVaryingPhraseContext ctx);
	/**
	 * Visit a parse tree produced by {@link CobolParser#performAfter}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitPerformAfter(CobolParser.PerformAfterContext ctx);
	/**
	 * Visit a parse tree produced by {@link CobolParser#performFrom}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitPerformFrom(CobolParser.PerformFromContext ctx);
	/**
	 * Visit a parse tree produced by {@link CobolParser#performBy}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitPerformBy(CobolParser.PerformByContext ctx);
	/**
	 * Visit a parse tree produced by {@link CobolParser#performTestClause}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitPerformTestClause(CobolParser.PerformTestClauseContext ctx);
	/**
	 * Visit a parse tree produced by {@link CobolParser#purgeStatement}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitPurgeStatement(CobolParser.PurgeStatementContext ctx);
	/**
	 * Visit a parse tree produced by {@link CobolParser#readStatement}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitReadStatement(CobolParser.ReadStatementContext ctx);
	/**
	 * Visit a parse tree produced by {@link CobolParser#readInto}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitReadInto(CobolParser.ReadIntoContext ctx);
	/**
	 * Visit a parse tree produced by {@link CobolParser#readWith}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitReadWith(CobolParser.ReadWithContext ctx);
	/**
	 * Visit a parse tree produced by {@link CobolParser#readKey}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitReadKey(CobolParser.ReadKeyContext ctx);
	/**
	 * Visit a parse tree produced by {@link CobolParser#receiveStatement}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitReceiveStatement(CobolParser.ReceiveStatementContext ctx);
	/**
	 * Visit a parse tree produced by {@link CobolParser#receiveFromStatement}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitReceiveFromStatement(CobolParser.ReceiveFromStatementContext ctx);
	/**
	 * Visit a parse tree produced by {@link CobolParser#receiveFrom}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitReceiveFrom(CobolParser.ReceiveFromContext ctx);
	/**
	 * Visit a parse tree produced by {@link CobolParser#receiveIntoStatement}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitReceiveIntoStatement(CobolParser.ReceiveIntoStatementContext ctx);
	/**
	 * Visit a parse tree produced by {@link CobolParser#receiveNoData}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitReceiveNoData(CobolParser.ReceiveNoDataContext ctx);
	/**
	 * Visit a parse tree produced by {@link CobolParser#receiveWithData}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitReceiveWithData(CobolParser.ReceiveWithDataContext ctx);
	/**
	 * Visit a parse tree produced by {@link CobolParser#receiveBefore}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitReceiveBefore(CobolParser.ReceiveBeforeContext ctx);
	/**
	 * Visit a parse tree produced by {@link CobolParser#receiveWith}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitReceiveWith(CobolParser.ReceiveWithContext ctx);
	/**
	 * Visit a parse tree produced by {@link CobolParser#receiveThread}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitReceiveThread(CobolParser.ReceiveThreadContext ctx);
	/**
	 * Visit a parse tree produced by {@link CobolParser#receiveSize}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitReceiveSize(CobolParser.ReceiveSizeContext ctx);
	/**
	 * Visit a parse tree produced by {@link CobolParser#receiveStatus}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitReceiveStatus(CobolParser.ReceiveStatusContext ctx);
	/**
	 * Visit a parse tree produced by {@link CobolParser#releaseStatement}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitReleaseStatement(CobolParser.ReleaseStatementContext ctx);
	/**
	 * Visit a parse tree produced by {@link CobolParser#returnStatement}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitReturnStatement(CobolParser.ReturnStatementContext ctx);
	/**
	 * Visit a parse tree produced by {@link CobolParser#returnInto}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitReturnInto(CobolParser.ReturnIntoContext ctx);
	/**
	 * Visit a parse tree produced by {@link CobolParser#rewriteStatement}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitRewriteStatement(CobolParser.RewriteStatementContext ctx);
	/**
	 * Visit a parse tree produced by {@link CobolParser#rewriteFrom}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitRewriteFrom(CobolParser.RewriteFromContext ctx);
	/**
	 * Visit a parse tree produced by {@link CobolParser#searchStatement}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitSearchStatement(CobolParser.SearchStatementContext ctx);
	/**
	 * Visit a parse tree produced by {@link CobolParser#searchVarying}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitSearchVarying(CobolParser.SearchVaryingContext ctx);
	/**
	 * Visit a parse tree produced by {@link CobolParser#searchWhen}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitSearchWhen(CobolParser.SearchWhenContext ctx);
	/**
	 * Visit a parse tree produced by {@link CobolParser#sendStatement}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitSendStatement(CobolParser.SendStatementContext ctx);
	/**
	 * Visit a parse tree produced by {@link CobolParser#sendStatementSync}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitSendStatementSync(CobolParser.SendStatementSyncContext ctx);
	/**
	 * Visit a parse tree produced by {@link CobolParser#sendStatementAsync}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitSendStatementAsync(CobolParser.SendStatementAsyncContext ctx);
	/**
	 * Visit a parse tree produced by {@link CobolParser#sendFromPhrase}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitSendFromPhrase(CobolParser.SendFromPhraseContext ctx);
	/**
	 * Visit a parse tree produced by {@link CobolParser#sendWithPhrase}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitSendWithPhrase(CobolParser.SendWithPhraseContext ctx);
	/**
	 * Visit a parse tree produced by {@link CobolParser#sendReplacingPhrase}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitSendReplacingPhrase(CobolParser.SendReplacingPhraseContext ctx);
	/**
	 * Visit a parse tree produced by {@link CobolParser#sendAdvancingPhrase}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitSendAdvancingPhrase(CobolParser.SendAdvancingPhraseContext ctx);
	/**
	 * Visit a parse tree produced by {@link CobolParser#sendAdvancingPage}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitSendAdvancingPage(CobolParser.SendAdvancingPageContext ctx);
	/**
	 * Visit a parse tree produced by {@link CobolParser#sendAdvancingLines}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitSendAdvancingLines(CobolParser.SendAdvancingLinesContext ctx);
	/**
	 * Visit a parse tree produced by {@link CobolParser#sendAdvancingMnemonic}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitSendAdvancingMnemonic(CobolParser.SendAdvancingMnemonicContext ctx);
	/**
	 * Visit a parse tree produced by {@link CobolParser#setStatement}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitSetStatement(CobolParser.SetStatementContext ctx);
	/**
	 * Visit a parse tree produced by {@link CobolParser#setToStatement}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitSetToStatement(CobolParser.SetToStatementContext ctx);
	/**
	 * Visit a parse tree produced by {@link CobolParser#setUpDownByStatement}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitSetUpDownByStatement(CobolParser.SetUpDownByStatementContext ctx);
	/**
	 * Visit a parse tree produced by {@link CobolParser#setTo}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitSetTo(CobolParser.SetToContext ctx);
	/**
	 * Visit a parse tree produced by {@link CobolParser#setToValue}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitSetToValue(CobolParser.SetToValueContext ctx);
	/**
	 * Visit a parse tree produced by {@link CobolParser#setByValue}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitSetByValue(CobolParser.SetByValueContext ctx);
	/**
	 * Visit a parse tree produced by {@link CobolParser#sortStatement}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitSortStatement(CobolParser.SortStatementContext ctx);
	/**
	 * Visit a parse tree produced by {@link CobolParser#sortOnKeyClause}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitSortOnKeyClause(CobolParser.SortOnKeyClauseContext ctx);
	/**
	 * Visit a parse tree produced by {@link CobolParser#sortDuplicatesPhrase}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitSortDuplicatesPhrase(CobolParser.SortDuplicatesPhraseContext ctx);
	/**
	 * Visit a parse tree produced by {@link CobolParser#sortCollatingSequencePhrase}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitSortCollatingSequencePhrase(CobolParser.SortCollatingSequencePhraseContext ctx);
	/**
	 * Visit a parse tree produced by {@link CobolParser#sortCollatingAlphanumeric}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitSortCollatingAlphanumeric(CobolParser.SortCollatingAlphanumericContext ctx);
	/**
	 * Visit a parse tree produced by {@link CobolParser#sortCollatingNational}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitSortCollatingNational(CobolParser.SortCollatingNationalContext ctx);
	/**
	 * Visit a parse tree produced by {@link CobolParser#sortInputProcedurePhrase}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitSortInputProcedurePhrase(CobolParser.SortInputProcedurePhraseContext ctx);
	/**
	 * Visit a parse tree produced by {@link CobolParser#sortInputThrough}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitSortInputThrough(CobolParser.SortInputThroughContext ctx);
	/**
	 * Visit a parse tree produced by {@link CobolParser#sortUsing}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitSortUsing(CobolParser.SortUsingContext ctx);
	/**
	 * Visit a parse tree produced by {@link CobolParser#sortOutputProcedurePhrase}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitSortOutputProcedurePhrase(CobolParser.SortOutputProcedurePhraseContext ctx);
	/**
	 * Visit a parse tree produced by {@link CobolParser#sortOutputThrough}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitSortOutputThrough(CobolParser.SortOutputThroughContext ctx);
	/**
	 * Visit a parse tree produced by {@link CobolParser#sortGivingPhrase}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitSortGivingPhrase(CobolParser.SortGivingPhraseContext ctx);
	/**
	 * Visit a parse tree produced by {@link CobolParser#sortGiving}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitSortGiving(CobolParser.SortGivingContext ctx);
	/**
	 * Visit a parse tree produced by {@link CobolParser#startStatement}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitStartStatement(CobolParser.StartStatementContext ctx);
	/**
	 * Visit a parse tree produced by {@link CobolParser#startKey}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitStartKey(CobolParser.StartKeyContext ctx);
	/**
	 * Visit a parse tree produced by {@link CobolParser#stopStatement}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitStopStatement(CobolParser.StopStatementContext ctx);
	/**
	 * Visit a parse tree produced by {@link CobolParser#stopStatementGiving}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitStopStatementGiving(CobolParser.StopStatementGivingContext ctx);
	/**
	 * Visit a parse tree produced by {@link CobolParser#stringStatement}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitStringStatement(CobolParser.StringStatementContext ctx);
	/**
	 * Visit a parse tree produced by {@link CobolParser#stringSendingPhrase}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitStringSendingPhrase(CobolParser.StringSendingPhraseContext ctx);
	/**
	 * Visit a parse tree produced by {@link CobolParser#stringSending}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitStringSending(CobolParser.StringSendingContext ctx);
	/**
	 * Visit a parse tree produced by {@link CobolParser#stringDelimitedByPhrase}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitStringDelimitedByPhrase(CobolParser.StringDelimitedByPhraseContext ctx);
	/**
	 * Visit a parse tree produced by {@link CobolParser#stringForPhrase}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitStringForPhrase(CobolParser.StringForPhraseContext ctx);
	/**
	 * Visit a parse tree produced by {@link CobolParser#stringIntoPhrase}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitStringIntoPhrase(CobolParser.StringIntoPhraseContext ctx);
	/**
	 * Visit a parse tree produced by {@link CobolParser#stringWithPointerPhrase}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitStringWithPointerPhrase(CobolParser.StringWithPointerPhraseContext ctx);
	/**
	 * Visit a parse tree produced by {@link CobolParser#subtractStatement}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitSubtractStatement(CobolParser.SubtractStatementContext ctx);
	/**
	 * Visit a parse tree produced by {@link CobolParser#subtractFromStatement}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitSubtractFromStatement(CobolParser.SubtractFromStatementContext ctx);
	/**
	 * Visit a parse tree produced by {@link CobolParser#subtractFromGivingStatement}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitSubtractFromGivingStatement(CobolParser.SubtractFromGivingStatementContext ctx);
	/**
	 * Visit a parse tree produced by {@link CobolParser#subtractCorrespondingStatement}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitSubtractCorrespondingStatement(CobolParser.SubtractCorrespondingStatementContext ctx);
	/**
	 * Visit a parse tree produced by {@link CobolParser#subtractSubtrahend}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitSubtractSubtrahend(CobolParser.SubtractSubtrahendContext ctx);
	/**
	 * Visit a parse tree produced by {@link CobolParser#subtractMinuend}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitSubtractMinuend(CobolParser.SubtractMinuendContext ctx);
	/**
	 * Visit a parse tree produced by {@link CobolParser#subtractMinuendGiving}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitSubtractMinuendGiving(CobolParser.SubtractMinuendGivingContext ctx);
	/**
	 * Visit a parse tree produced by {@link CobolParser#subtractGiving}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitSubtractGiving(CobolParser.SubtractGivingContext ctx);
	/**
	 * Visit a parse tree produced by {@link CobolParser#subtractMinuendCorresponding}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitSubtractMinuendCorresponding(CobolParser.SubtractMinuendCorrespondingContext ctx);
	/**
	 * Visit a parse tree produced by {@link CobolParser#terminateStatement}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitTerminateStatement(CobolParser.TerminateStatementContext ctx);
	/**
	 * Visit a parse tree produced by {@link CobolParser#unstringStatement}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitUnstringStatement(CobolParser.UnstringStatementContext ctx);
	/**
	 * Visit a parse tree produced by {@link CobolParser#unstringSendingPhrase}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitUnstringSendingPhrase(CobolParser.UnstringSendingPhraseContext ctx);
	/**
	 * Visit a parse tree produced by {@link CobolParser#unstringDelimitedByPhrase}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitUnstringDelimitedByPhrase(CobolParser.UnstringDelimitedByPhraseContext ctx);
	/**
	 * Visit a parse tree produced by {@link CobolParser#unstringOrAllPhrase}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitUnstringOrAllPhrase(CobolParser.UnstringOrAllPhraseContext ctx);
	/**
	 * Visit a parse tree produced by {@link CobolParser#unstringIntoPhrase}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitUnstringIntoPhrase(CobolParser.UnstringIntoPhraseContext ctx);
	/**
	 * Visit a parse tree produced by {@link CobolParser#unstringInto}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitUnstringInto(CobolParser.UnstringIntoContext ctx);
	/**
	 * Visit a parse tree produced by {@link CobolParser#unstringDelimiterIn}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitUnstringDelimiterIn(CobolParser.UnstringDelimiterInContext ctx);
	/**
	 * Visit a parse tree produced by {@link CobolParser#unstringCountIn}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitUnstringCountIn(CobolParser.UnstringCountInContext ctx);
	/**
	 * Visit a parse tree produced by {@link CobolParser#unstringWithPointerPhrase}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitUnstringWithPointerPhrase(CobolParser.UnstringWithPointerPhraseContext ctx);
	/**
	 * Visit a parse tree produced by {@link CobolParser#unstringTallyingPhrase}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitUnstringTallyingPhrase(CobolParser.UnstringTallyingPhraseContext ctx);
	/**
	 * Visit a parse tree produced by {@link CobolParser#useStatement}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitUseStatement(CobolParser.UseStatementContext ctx);
	/**
	 * Visit a parse tree produced by {@link CobolParser#useAfterClause}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitUseAfterClause(CobolParser.UseAfterClauseContext ctx);
	/**
	 * Visit a parse tree produced by {@link CobolParser#useAfterOn}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitUseAfterOn(CobolParser.UseAfterOnContext ctx);
	/**
	 * Visit a parse tree produced by {@link CobolParser#useDebugClause}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitUseDebugClause(CobolParser.UseDebugClauseContext ctx);
	/**
	 * Visit a parse tree produced by {@link CobolParser#useDebugOn}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitUseDebugOn(CobolParser.UseDebugOnContext ctx);
	/**
	 * Visit a parse tree produced by {@link CobolParser#writeStatement}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitWriteStatement(CobolParser.WriteStatementContext ctx);
	/**
	 * Visit a parse tree produced by {@link CobolParser#writeFromPhrase}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitWriteFromPhrase(CobolParser.WriteFromPhraseContext ctx);
	/**
	 * Visit a parse tree produced by {@link CobolParser#writeAdvancingPhrase}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitWriteAdvancingPhrase(CobolParser.WriteAdvancingPhraseContext ctx);
	/**
	 * Visit a parse tree produced by {@link CobolParser#writeAdvancingPage}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitWriteAdvancingPage(CobolParser.WriteAdvancingPageContext ctx);
	/**
	 * Visit a parse tree produced by {@link CobolParser#writeAdvancingLines}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitWriteAdvancingLines(CobolParser.WriteAdvancingLinesContext ctx);
	/**
	 * Visit a parse tree produced by {@link CobolParser#writeAdvancingMnemonic}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitWriteAdvancingMnemonic(CobolParser.WriteAdvancingMnemonicContext ctx);
	/**
	 * Visit a parse tree produced by {@link CobolParser#writeAtEndOfPagePhrase}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitWriteAtEndOfPagePhrase(CobolParser.WriteAtEndOfPagePhraseContext ctx);
	/**
	 * Visit a parse tree produced by {@link CobolParser#writeNotAtEndOfPagePhrase}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitWriteNotAtEndOfPagePhrase(CobolParser.WriteNotAtEndOfPagePhraseContext ctx);
	/**
	 * Visit a parse tree produced by {@link CobolParser#atEndPhrase}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitAtEndPhrase(CobolParser.AtEndPhraseContext ctx);
	/**
	 * Visit a parse tree produced by {@link CobolParser#notAtEndPhrase}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitNotAtEndPhrase(CobolParser.NotAtEndPhraseContext ctx);
	/**
	 * Visit a parse tree produced by {@link CobolParser#invalidKeyPhrase}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitInvalidKeyPhrase(CobolParser.InvalidKeyPhraseContext ctx);
	/**
	 * Visit a parse tree produced by {@link CobolParser#notInvalidKeyPhrase}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitNotInvalidKeyPhrase(CobolParser.NotInvalidKeyPhraseContext ctx);
	/**
	 * Visit a parse tree produced by {@link CobolParser#onOverflowPhrase}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitOnOverflowPhrase(CobolParser.OnOverflowPhraseContext ctx);
	/**
	 * Visit a parse tree produced by {@link CobolParser#notOnOverflowPhrase}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitNotOnOverflowPhrase(CobolParser.NotOnOverflowPhraseContext ctx);
	/**
	 * Visit a parse tree produced by {@link CobolParser#onSizeErrorPhrase}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitOnSizeErrorPhrase(CobolParser.OnSizeErrorPhraseContext ctx);
	/**
	 * Visit a parse tree produced by {@link CobolParser#notOnSizeErrorPhrase}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitNotOnSizeErrorPhrase(CobolParser.NotOnSizeErrorPhraseContext ctx);
	/**
	 * Visit a parse tree produced by {@link CobolParser#onExceptionClause}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitOnExceptionClause(CobolParser.OnExceptionClauseContext ctx);
	/**
	 * Visit a parse tree produced by {@link CobolParser#notOnExceptionClause}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitNotOnExceptionClause(CobolParser.NotOnExceptionClauseContext ctx);
	/**
	 * Visit a parse tree produced by {@link CobolParser#arithmeticExpression}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitArithmeticExpression(CobolParser.ArithmeticExpressionContext ctx);
	/**
	 * Visit a parse tree produced by {@link CobolParser#plusMinus}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitPlusMinus(CobolParser.PlusMinusContext ctx);
	/**
	 * Visit a parse tree produced by {@link CobolParser#multDivs}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitMultDivs(CobolParser.MultDivsContext ctx);
	/**
	 * Visit a parse tree produced by {@link CobolParser#multDiv}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitMultDiv(CobolParser.MultDivContext ctx);
	/**
	 * Visit a parse tree produced by {@link CobolParser#powers}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitPowers(CobolParser.PowersContext ctx);
	/**
	 * Visit a parse tree produced by {@link CobolParser#power}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitPower(CobolParser.PowerContext ctx);
	/**
	 * Visit a parse tree produced by {@link CobolParser#basis}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitBasis(CobolParser.BasisContext ctx);
	/**
	 * Visit a parse tree produced by {@link CobolParser#condition}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitCondition(CobolParser.ConditionContext ctx);
	/**
	 * Visit a parse tree produced by {@link CobolParser#andOrCondition}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitAndOrCondition(CobolParser.AndOrConditionContext ctx);
	/**
	 * Visit a parse tree produced by {@link CobolParser#combinableCondition}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitCombinableCondition(CobolParser.CombinableConditionContext ctx);
	/**
	 * Visit a parse tree produced by {@link CobolParser#simpleCondition}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitSimpleCondition(CobolParser.SimpleConditionContext ctx);
	/**
	 * Visit a parse tree produced by {@link CobolParser#classCondition}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitClassCondition(CobolParser.ClassConditionContext ctx);
	/**
	 * Visit a parse tree produced by {@link CobolParser#conditionNameReference}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitConditionNameReference(CobolParser.ConditionNameReferenceContext ctx);
	/**
	 * Visit a parse tree produced by {@link CobolParser#conditionNameSubscriptReference}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitConditionNameSubscriptReference(CobolParser.ConditionNameSubscriptReferenceContext ctx);
	/**
	 * Visit a parse tree produced by {@link CobolParser#relationCondition}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitRelationCondition(CobolParser.RelationConditionContext ctx);
	/**
	 * Visit a parse tree produced by {@link CobolParser#relationSignCondition}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitRelationSignCondition(CobolParser.RelationSignConditionContext ctx);
	/**
	 * Visit a parse tree produced by {@link CobolParser#relationArithmeticComparison}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitRelationArithmeticComparison(CobolParser.RelationArithmeticComparisonContext ctx);
	/**
	 * Visit a parse tree produced by {@link CobolParser#relationCombinedComparison}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitRelationCombinedComparison(CobolParser.RelationCombinedComparisonContext ctx);
	/**
	 * Visit a parse tree produced by {@link CobolParser#relationCombinedCondition}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitRelationCombinedCondition(CobolParser.RelationCombinedConditionContext ctx);
	/**
	 * Visit a parse tree produced by {@link CobolParser#relationalOperator}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitRelationalOperator(CobolParser.RelationalOperatorContext ctx);
	/**
	 * Visit a parse tree produced by {@link CobolParser#abbreviation}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitAbbreviation(CobolParser.AbbreviationContext ctx);
	/**
	 * Visit a parse tree produced by {@link CobolParser#identifier}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitIdentifier(CobolParser.IdentifierContext ctx);
	/**
	 * Visit a parse tree produced by {@link CobolParser#tableCall}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitTableCall(CobolParser.TableCallContext ctx);
	/**
	 * Visit a parse tree produced by {@link CobolParser#functionCall}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitFunctionCall(CobolParser.FunctionCallContext ctx);
	/**
	 * Visit a parse tree produced by {@link CobolParser#referenceModifier}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitReferenceModifier(CobolParser.ReferenceModifierContext ctx);
	/**
	 * Visit a parse tree produced by {@link CobolParser#characterPosition}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitCharacterPosition(CobolParser.CharacterPositionContext ctx);
	/**
	 * Visit a parse tree produced by {@link CobolParser#length}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitLength(CobolParser.LengthContext ctx);
	/**
	 * Visit a parse tree produced by {@link CobolParser#subscript}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitSubscript(CobolParser.SubscriptContext ctx);
	/**
	 * Visit a parse tree produced by {@link CobolParser#argument}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitArgument(CobolParser.ArgumentContext ctx);
	/**
	 * Visit a parse tree produced by {@link CobolParser#qualifiedDataName}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitQualifiedDataName(CobolParser.QualifiedDataNameContext ctx);
	/**
	 * Visit a parse tree produced by {@link CobolParser#qualifiedDataNameFormat1}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitQualifiedDataNameFormat1(CobolParser.QualifiedDataNameFormat1Context ctx);
	/**
	 * Visit a parse tree produced by {@link CobolParser#qualifiedDataNameFormat2}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitQualifiedDataNameFormat2(CobolParser.QualifiedDataNameFormat2Context ctx);
	/**
	 * Visit a parse tree produced by {@link CobolParser#qualifiedDataNameFormat3}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitQualifiedDataNameFormat3(CobolParser.QualifiedDataNameFormat3Context ctx);
	/**
	 * Visit a parse tree produced by {@link CobolParser#qualifiedDataNameFormat4}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitQualifiedDataNameFormat4(CobolParser.QualifiedDataNameFormat4Context ctx);
	/**
	 * Visit a parse tree produced by {@link CobolParser#qualifiedInData}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitQualifiedInData(CobolParser.QualifiedInDataContext ctx);
	/**
	 * Visit a parse tree produced by {@link CobolParser#inData}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitInData(CobolParser.InDataContext ctx);
	/**
	 * Visit a parse tree produced by {@link CobolParser#inFile}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitInFile(CobolParser.InFileContext ctx);
	/**
	 * Visit a parse tree produced by {@link CobolParser#inMnemonic}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitInMnemonic(CobolParser.InMnemonicContext ctx);
	/**
	 * Visit a parse tree produced by {@link CobolParser#inSection}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitInSection(CobolParser.InSectionContext ctx);
	/**
	 * Visit a parse tree produced by {@link CobolParser#inLibrary}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitInLibrary(CobolParser.InLibraryContext ctx);
	/**
	 * Visit a parse tree produced by {@link CobolParser#inTable}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitInTable(CobolParser.InTableContext ctx);
	/**
	 * Visit a parse tree produced by {@link CobolParser#alphabetName}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitAlphabetName(CobolParser.AlphabetNameContext ctx);
	/**
	 * Visit a parse tree produced by {@link CobolParser#assignmentName}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitAssignmentName(CobolParser.AssignmentNameContext ctx);
	/**
	 * Visit a parse tree produced by {@link CobolParser#basisName}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitBasisName(CobolParser.BasisNameContext ctx);
	/**
	 * Visit a parse tree produced by {@link CobolParser#cdName}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitCdName(CobolParser.CdNameContext ctx);
	/**
	 * Visit a parse tree produced by {@link CobolParser#className}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitClassName(CobolParser.ClassNameContext ctx);
	/**
	 * Visit a parse tree produced by {@link CobolParser#computerName}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitComputerName(CobolParser.ComputerNameContext ctx);
	/**
	 * Visit a parse tree produced by {@link CobolParser#conditionName}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitConditionName(CobolParser.ConditionNameContext ctx);
	/**
	 * Visit a parse tree produced by {@link CobolParser#dataName}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitDataName(CobolParser.DataNameContext ctx);
	/**
	 * Visit a parse tree produced by {@link CobolParser#dataDescName}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitDataDescName(CobolParser.DataDescNameContext ctx);
	/**
	 * Visit a parse tree produced by {@link CobolParser#environmentName}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitEnvironmentName(CobolParser.EnvironmentNameContext ctx);
	/**
	 * Visit a parse tree produced by {@link CobolParser#fileName}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitFileName(CobolParser.FileNameContext ctx);
	/**
	 * Visit a parse tree produced by {@link CobolParser#functionName}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitFunctionName(CobolParser.FunctionNameContext ctx);
	/**
	 * Visit a parse tree produced by {@link CobolParser#indexName}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitIndexName(CobolParser.IndexNameContext ctx);
	/**
	 * Visit a parse tree produced by {@link CobolParser#languageName}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitLanguageName(CobolParser.LanguageNameContext ctx);
	/**
	 * Visit a parse tree produced by {@link CobolParser#libraryName}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitLibraryName(CobolParser.LibraryNameContext ctx);
	/**
	 * Visit a parse tree produced by {@link CobolParser#localName}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitLocalName(CobolParser.LocalNameContext ctx);
	/**
	 * Visit a parse tree produced by {@link CobolParser#mnemonicName}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitMnemonicName(CobolParser.MnemonicNameContext ctx);
	/**
	 * Visit a parse tree produced by {@link CobolParser#paragraphName}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitParagraphName(CobolParser.ParagraphNameContext ctx);
	/**
	 * Visit a parse tree produced by {@link CobolParser#procedureName}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitProcedureName(CobolParser.ProcedureNameContext ctx);
	/**
	 * Visit a parse tree produced by {@link CobolParser#programName}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitProgramName(CobolParser.ProgramNameContext ctx);
	/**
	 * Visit a parse tree produced by {@link CobolParser#recordName}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitRecordName(CobolParser.RecordNameContext ctx);
	/**
	 * Visit a parse tree produced by {@link CobolParser#reportName}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitReportName(CobolParser.ReportNameContext ctx);
	/**
	 * Visit a parse tree produced by {@link CobolParser#routineName}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitRoutineName(CobolParser.RoutineNameContext ctx);
	/**
	 * Visit a parse tree produced by {@link CobolParser#screenName}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitScreenName(CobolParser.ScreenNameContext ctx);
	/**
	 * Visit a parse tree produced by {@link CobolParser#sectionName}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitSectionName(CobolParser.SectionNameContext ctx);
	/**
	 * Visit a parse tree produced by {@link CobolParser#systemName}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitSystemName(CobolParser.SystemNameContext ctx);
	/**
	 * Visit a parse tree produced by {@link CobolParser#symbolicCharacter}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitSymbolicCharacter(CobolParser.SymbolicCharacterContext ctx);
	/**
	 * Visit a parse tree produced by {@link CobolParser#textName}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitTextName(CobolParser.TextNameContext ctx);
	/**
	 * Visit a parse tree produced by {@link CobolParser#cobolWord}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitCobolWord(CobolParser.CobolWordContext ctx);
	/**
	 * Visit a parse tree produced by {@link CobolParser#literal}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitLiteral(CobolParser.LiteralContext ctx);
	/**
	 * Visit a parse tree produced by {@link CobolParser#booleanLiteral}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitBooleanLiteral(CobolParser.BooleanLiteralContext ctx);
	/**
	 * Visit a parse tree produced by {@link CobolParser#numericLiteral}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitNumericLiteral(CobolParser.NumericLiteralContext ctx);
	/**
	 * Visit a parse tree produced by {@link CobolParser#integerLiteral}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitIntegerLiteral(CobolParser.IntegerLiteralContext ctx);
	/**
	 * Visit a parse tree produced by {@link CobolParser#cicsDfhRespLiteral}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitCicsDfhRespLiteral(CobolParser.CicsDfhRespLiteralContext ctx);
	/**
	 * Visit a parse tree produced by {@link CobolParser#cicsDfhValueLiteral}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitCicsDfhValueLiteral(CobolParser.CicsDfhValueLiteralContext ctx);
	/**
	 * Visit a parse tree produced by {@link CobolParser#figurativeConstant}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitFigurativeConstant(CobolParser.FigurativeConstantContext ctx);
	/**
	 * Visit a parse tree produced by {@link CobolParser#specialRegister}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitSpecialRegister(CobolParser.SpecialRegisterContext ctx);
	/**
	 * Visit a parse tree produced by {@link CobolParser#commentEntry}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitCommentEntry(CobolParser.CommentEntryContext ctx);
}