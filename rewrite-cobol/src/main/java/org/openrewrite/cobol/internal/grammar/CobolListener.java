// Generated from /Users/jon/Projects/github/openrewrite/rewrite/rewrite-cobol/src/main/antlr/Cobol.g4 by ANTLR 4.9.3
package org.openrewrite.cobol.internal.grammar;
import org.antlr.v4.runtime.tree.ParseTreeListener;

/**
 * This interface defines a complete listener for a parse tree produced by
 * {@link CobolParser}.
 */
public interface CobolListener extends ParseTreeListener {
	/**
	 * Enter a parse tree produced by {@link CobolParser#startRule}.
	 * @param ctx the parse tree
	 */
	void enterStartRule(CobolParser.StartRuleContext ctx);
	/**
	 * Exit a parse tree produced by {@link CobolParser#startRule}.
	 * @param ctx the parse tree
	 */
	void exitStartRule(CobolParser.StartRuleContext ctx);
	/**
	 * Enter a parse tree produced by {@link CobolParser#compilationUnit}.
	 * @param ctx the parse tree
	 */
	void enterCompilationUnit(CobolParser.CompilationUnitContext ctx);
	/**
	 * Exit a parse tree produced by {@link CobolParser#compilationUnit}.
	 * @param ctx the parse tree
	 */
	void exitCompilationUnit(CobolParser.CompilationUnitContext ctx);
	/**
	 * Enter a parse tree produced by {@link CobolParser#programUnit}.
	 * @param ctx the parse tree
	 */
	void enterProgramUnit(CobolParser.ProgramUnitContext ctx);
	/**
	 * Exit a parse tree produced by {@link CobolParser#programUnit}.
	 * @param ctx the parse tree
	 */
	void exitProgramUnit(CobolParser.ProgramUnitContext ctx);
	/**
	 * Enter a parse tree produced by {@link CobolParser#endProgramStatement}.
	 * @param ctx the parse tree
	 */
	void enterEndProgramStatement(CobolParser.EndProgramStatementContext ctx);
	/**
	 * Exit a parse tree produced by {@link CobolParser#endProgramStatement}.
	 * @param ctx the parse tree
	 */
	void exitEndProgramStatement(CobolParser.EndProgramStatementContext ctx);
	/**
	 * Enter a parse tree produced by {@link CobolParser#identificationDivision}.
	 * @param ctx the parse tree
	 */
	void enterIdentificationDivision(CobolParser.IdentificationDivisionContext ctx);
	/**
	 * Exit a parse tree produced by {@link CobolParser#identificationDivision}.
	 * @param ctx the parse tree
	 */
	void exitIdentificationDivision(CobolParser.IdentificationDivisionContext ctx);
	/**
	 * Enter a parse tree produced by {@link CobolParser#identificationDivisionBody}.
	 * @param ctx the parse tree
	 */
	void enterIdentificationDivisionBody(CobolParser.IdentificationDivisionBodyContext ctx);
	/**
	 * Exit a parse tree produced by {@link CobolParser#identificationDivisionBody}.
	 * @param ctx the parse tree
	 */
	void exitIdentificationDivisionBody(CobolParser.IdentificationDivisionBodyContext ctx);
	/**
	 * Enter a parse tree produced by {@link CobolParser#programIdParagraph}.
	 * @param ctx the parse tree
	 */
	void enterProgramIdParagraph(CobolParser.ProgramIdParagraphContext ctx);
	/**
	 * Exit a parse tree produced by {@link CobolParser#programIdParagraph}.
	 * @param ctx the parse tree
	 */
	void exitProgramIdParagraph(CobolParser.ProgramIdParagraphContext ctx);
	/**
	 * Enter a parse tree produced by {@link CobolParser#authorParagraph}.
	 * @param ctx the parse tree
	 */
	void enterAuthorParagraph(CobolParser.AuthorParagraphContext ctx);
	/**
	 * Exit a parse tree produced by {@link CobolParser#authorParagraph}.
	 * @param ctx the parse tree
	 */
	void exitAuthorParagraph(CobolParser.AuthorParagraphContext ctx);
	/**
	 * Enter a parse tree produced by {@link CobolParser#installationParagraph}.
	 * @param ctx the parse tree
	 */
	void enterInstallationParagraph(CobolParser.InstallationParagraphContext ctx);
	/**
	 * Exit a parse tree produced by {@link CobolParser#installationParagraph}.
	 * @param ctx the parse tree
	 */
	void exitInstallationParagraph(CobolParser.InstallationParagraphContext ctx);
	/**
	 * Enter a parse tree produced by {@link CobolParser#dateWrittenParagraph}.
	 * @param ctx the parse tree
	 */
	void enterDateWrittenParagraph(CobolParser.DateWrittenParagraphContext ctx);
	/**
	 * Exit a parse tree produced by {@link CobolParser#dateWrittenParagraph}.
	 * @param ctx the parse tree
	 */
	void exitDateWrittenParagraph(CobolParser.DateWrittenParagraphContext ctx);
	/**
	 * Enter a parse tree produced by {@link CobolParser#dateCompiledParagraph}.
	 * @param ctx the parse tree
	 */
	void enterDateCompiledParagraph(CobolParser.DateCompiledParagraphContext ctx);
	/**
	 * Exit a parse tree produced by {@link CobolParser#dateCompiledParagraph}.
	 * @param ctx the parse tree
	 */
	void exitDateCompiledParagraph(CobolParser.DateCompiledParagraphContext ctx);
	/**
	 * Enter a parse tree produced by {@link CobolParser#securityParagraph}.
	 * @param ctx the parse tree
	 */
	void enterSecurityParagraph(CobolParser.SecurityParagraphContext ctx);
	/**
	 * Exit a parse tree produced by {@link CobolParser#securityParagraph}.
	 * @param ctx the parse tree
	 */
	void exitSecurityParagraph(CobolParser.SecurityParagraphContext ctx);
	/**
	 * Enter a parse tree produced by {@link CobolParser#remarksParagraph}.
	 * @param ctx the parse tree
	 */
	void enterRemarksParagraph(CobolParser.RemarksParagraphContext ctx);
	/**
	 * Exit a parse tree produced by {@link CobolParser#remarksParagraph}.
	 * @param ctx the parse tree
	 */
	void exitRemarksParagraph(CobolParser.RemarksParagraphContext ctx);
	/**
	 * Enter a parse tree produced by {@link CobolParser#environmentDivision}.
	 * @param ctx the parse tree
	 */
	void enterEnvironmentDivision(CobolParser.EnvironmentDivisionContext ctx);
	/**
	 * Exit a parse tree produced by {@link CobolParser#environmentDivision}.
	 * @param ctx the parse tree
	 */
	void exitEnvironmentDivision(CobolParser.EnvironmentDivisionContext ctx);
	/**
	 * Enter a parse tree produced by {@link CobolParser#environmentDivisionBody}.
	 * @param ctx the parse tree
	 */
	void enterEnvironmentDivisionBody(CobolParser.EnvironmentDivisionBodyContext ctx);
	/**
	 * Exit a parse tree produced by {@link CobolParser#environmentDivisionBody}.
	 * @param ctx the parse tree
	 */
	void exitEnvironmentDivisionBody(CobolParser.EnvironmentDivisionBodyContext ctx);
	/**
	 * Enter a parse tree produced by {@link CobolParser#configurationSection}.
	 * @param ctx the parse tree
	 */
	void enterConfigurationSection(CobolParser.ConfigurationSectionContext ctx);
	/**
	 * Exit a parse tree produced by {@link CobolParser#configurationSection}.
	 * @param ctx the parse tree
	 */
	void exitConfigurationSection(CobolParser.ConfigurationSectionContext ctx);
	/**
	 * Enter a parse tree produced by {@link CobolParser#configurationSectionParagraph}.
	 * @param ctx the parse tree
	 */
	void enterConfigurationSectionParagraph(CobolParser.ConfigurationSectionParagraphContext ctx);
	/**
	 * Exit a parse tree produced by {@link CobolParser#configurationSectionParagraph}.
	 * @param ctx the parse tree
	 */
	void exitConfigurationSectionParagraph(CobolParser.ConfigurationSectionParagraphContext ctx);
	/**
	 * Enter a parse tree produced by {@link CobolParser#sourceComputerParagraph}.
	 * @param ctx the parse tree
	 */
	void enterSourceComputerParagraph(CobolParser.SourceComputerParagraphContext ctx);
	/**
	 * Exit a parse tree produced by {@link CobolParser#sourceComputerParagraph}.
	 * @param ctx the parse tree
	 */
	void exitSourceComputerParagraph(CobolParser.SourceComputerParagraphContext ctx);
	/**
	 * Enter a parse tree produced by {@link CobolParser#objectComputerParagraph}.
	 * @param ctx the parse tree
	 */
	void enterObjectComputerParagraph(CobolParser.ObjectComputerParagraphContext ctx);
	/**
	 * Exit a parse tree produced by {@link CobolParser#objectComputerParagraph}.
	 * @param ctx the parse tree
	 */
	void exitObjectComputerParagraph(CobolParser.ObjectComputerParagraphContext ctx);
	/**
	 * Enter a parse tree produced by {@link CobolParser#objectComputerClause}.
	 * @param ctx the parse tree
	 */
	void enterObjectComputerClause(CobolParser.ObjectComputerClauseContext ctx);
	/**
	 * Exit a parse tree produced by {@link CobolParser#objectComputerClause}.
	 * @param ctx the parse tree
	 */
	void exitObjectComputerClause(CobolParser.ObjectComputerClauseContext ctx);
	/**
	 * Enter a parse tree produced by {@link CobolParser#memorySizeClause}.
	 * @param ctx the parse tree
	 */
	void enterMemorySizeClause(CobolParser.MemorySizeClauseContext ctx);
	/**
	 * Exit a parse tree produced by {@link CobolParser#memorySizeClause}.
	 * @param ctx the parse tree
	 */
	void exitMemorySizeClause(CobolParser.MemorySizeClauseContext ctx);
	/**
	 * Enter a parse tree produced by {@link CobolParser#diskSizeClause}.
	 * @param ctx the parse tree
	 */
	void enterDiskSizeClause(CobolParser.DiskSizeClauseContext ctx);
	/**
	 * Exit a parse tree produced by {@link CobolParser#diskSizeClause}.
	 * @param ctx the parse tree
	 */
	void exitDiskSizeClause(CobolParser.DiskSizeClauseContext ctx);
	/**
	 * Enter a parse tree produced by {@link CobolParser#collatingSequenceClause}.
	 * @param ctx the parse tree
	 */
	void enterCollatingSequenceClause(CobolParser.CollatingSequenceClauseContext ctx);
	/**
	 * Exit a parse tree produced by {@link CobolParser#collatingSequenceClause}.
	 * @param ctx the parse tree
	 */
	void exitCollatingSequenceClause(CobolParser.CollatingSequenceClauseContext ctx);
	/**
	 * Enter a parse tree produced by {@link CobolParser#collatingSequenceClauseAlphanumeric}.
	 * @param ctx the parse tree
	 */
	void enterCollatingSequenceClauseAlphanumeric(CobolParser.CollatingSequenceClauseAlphanumericContext ctx);
	/**
	 * Exit a parse tree produced by {@link CobolParser#collatingSequenceClauseAlphanumeric}.
	 * @param ctx the parse tree
	 */
	void exitCollatingSequenceClauseAlphanumeric(CobolParser.CollatingSequenceClauseAlphanumericContext ctx);
	/**
	 * Enter a parse tree produced by {@link CobolParser#collatingSequenceClauseNational}.
	 * @param ctx the parse tree
	 */
	void enterCollatingSequenceClauseNational(CobolParser.CollatingSequenceClauseNationalContext ctx);
	/**
	 * Exit a parse tree produced by {@link CobolParser#collatingSequenceClauseNational}.
	 * @param ctx the parse tree
	 */
	void exitCollatingSequenceClauseNational(CobolParser.CollatingSequenceClauseNationalContext ctx);
	/**
	 * Enter a parse tree produced by {@link CobolParser#segmentLimitClause}.
	 * @param ctx the parse tree
	 */
	void enterSegmentLimitClause(CobolParser.SegmentLimitClauseContext ctx);
	/**
	 * Exit a parse tree produced by {@link CobolParser#segmentLimitClause}.
	 * @param ctx the parse tree
	 */
	void exitSegmentLimitClause(CobolParser.SegmentLimitClauseContext ctx);
	/**
	 * Enter a parse tree produced by {@link CobolParser#characterSetClause}.
	 * @param ctx the parse tree
	 */
	void enterCharacterSetClause(CobolParser.CharacterSetClauseContext ctx);
	/**
	 * Exit a parse tree produced by {@link CobolParser#characterSetClause}.
	 * @param ctx the parse tree
	 */
	void exitCharacterSetClause(CobolParser.CharacterSetClauseContext ctx);
	/**
	 * Enter a parse tree produced by {@link CobolParser#specialNamesParagraph}.
	 * @param ctx the parse tree
	 */
	void enterSpecialNamesParagraph(CobolParser.SpecialNamesParagraphContext ctx);
	/**
	 * Exit a parse tree produced by {@link CobolParser#specialNamesParagraph}.
	 * @param ctx the parse tree
	 */
	void exitSpecialNamesParagraph(CobolParser.SpecialNamesParagraphContext ctx);
	/**
	 * Enter a parse tree produced by {@link CobolParser#specialNameClause}.
	 * @param ctx the parse tree
	 */
	void enterSpecialNameClause(CobolParser.SpecialNameClauseContext ctx);
	/**
	 * Exit a parse tree produced by {@link CobolParser#specialNameClause}.
	 * @param ctx the parse tree
	 */
	void exitSpecialNameClause(CobolParser.SpecialNameClauseContext ctx);
	/**
	 * Enter a parse tree produced by {@link CobolParser#alphabetClause}.
	 * @param ctx the parse tree
	 */
	void enterAlphabetClause(CobolParser.AlphabetClauseContext ctx);
	/**
	 * Exit a parse tree produced by {@link CobolParser#alphabetClause}.
	 * @param ctx the parse tree
	 */
	void exitAlphabetClause(CobolParser.AlphabetClauseContext ctx);
	/**
	 * Enter a parse tree produced by {@link CobolParser#alphabetClauseFormat1}.
	 * @param ctx the parse tree
	 */
	void enterAlphabetClauseFormat1(CobolParser.AlphabetClauseFormat1Context ctx);
	/**
	 * Exit a parse tree produced by {@link CobolParser#alphabetClauseFormat1}.
	 * @param ctx the parse tree
	 */
	void exitAlphabetClauseFormat1(CobolParser.AlphabetClauseFormat1Context ctx);
	/**
	 * Enter a parse tree produced by {@link CobolParser#alphabetLiterals}.
	 * @param ctx the parse tree
	 */
	void enterAlphabetLiterals(CobolParser.AlphabetLiteralsContext ctx);
	/**
	 * Exit a parse tree produced by {@link CobolParser#alphabetLiterals}.
	 * @param ctx the parse tree
	 */
	void exitAlphabetLiterals(CobolParser.AlphabetLiteralsContext ctx);
	/**
	 * Enter a parse tree produced by {@link CobolParser#alphabetThrough}.
	 * @param ctx the parse tree
	 */
	void enterAlphabetThrough(CobolParser.AlphabetThroughContext ctx);
	/**
	 * Exit a parse tree produced by {@link CobolParser#alphabetThrough}.
	 * @param ctx the parse tree
	 */
	void exitAlphabetThrough(CobolParser.AlphabetThroughContext ctx);
	/**
	 * Enter a parse tree produced by {@link CobolParser#alphabetAlso}.
	 * @param ctx the parse tree
	 */
	void enterAlphabetAlso(CobolParser.AlphabetAlsoContext ctx);
	/**
	 * Exit a parse tree produced by {@link CobolParser#alphabetAlso}.
	 * @param ctx the parse tree
	 */
	void exitAlphabetAlso(CobolParser.AlphabetAlsoContext ctx);
	/**
	 * Enter a parse tree produced by {@link CobolParser#alphabetClauseFormat2}.
	 * @param ctx the parse tree
	 */
	void enterAlphabetClauseFormat2(CobolParser.AlphabetClauseFormat2Context ctx);
	/**
	 * Exit a parse tree produced by {@link CobolParser#alphabetClauseFormat2}.
	 * @param ctx the parse tree
	 */
	void exitAlphabetClauseFormat2(CobolParser.AlphabetClauseFormat2Context ctx);
	/**
	 * Enter a parse tree produced by {@link CobolParser#channelClause}.
	 * @param ctx the parse tree
	 */
	void enterChannelClause(CobolParser.ChannelClauseContext ctx);
	/**
	 * Exit a parse tree produced by {@link CobolParser#channelClause}.
	 * @param ctx the parse tree
	 */
	void exitChannelClause(CobolParser.ChannelClauseContext ctx);
	/**
	 * Enter a parse tree produced by {@link CobolParser#classClause}.
	 * @param ctx the parse tree
	 */
	void enterClassClause(CobolParser.ClassClauseContext ctx);
	/**
	 * Exit a parse tree produced by {@link CobolParser#classClause}.
	 * @param ctx the parse tree
	 */
	void exitClassClause(CobolParser.ClassClauseContext ctx);
	/**
	 * Enter a parse tree produced by {@link CobolParser#classClauseThrough}.
	 * @param ctx the parse tree
	 */
	void enterClassClauseThrough(CobolParser.ClassClauseThroughContext ctx);
	/**
	 * Exit a parse tree produced by {@link CobolParser#classClauseThrough}.
	 * @param ctx the parse tree
	 */
	void exitClassClauseThrough(CobolParser.ClassClauseThroughContext ctx);
	/**
	 * Enter a parse tree produced by {@link CobolParser#classClauseFrom}.
	 * @param ctx the parse tree
	 */
	void enterClassClauseFrom(CobolParser.ClassClauseFromContext ctx);
	/**
	 * Exit a parse tree produced by {@link CobolParser#classClauseFrom}.
	 * @param ctx the parse tree
	 */
	void exitClassClauseFrom(CobolParser.ClassClauseFromContext ctx);
	/**
	 * Enter a parse tree produced by {@link CobolParser#classClauseTo}.
	 * @param ctx the parse tree
	 */
	void enterClassClauseTo(CobolParser.ClassClauseToContext ctx);
	/**
	 * Exit a parse tree produced by {@link CobolParser#classClauseTo}.
	 * @param ctx the parse tree
	 */
	void exitClassClauseTo(CobolParser.ClassClauseToContext ctx);
	/**
	 * Enter a parse tree produced by {@link CobolParser#currencySignClause}.
	 * @param ctx the parse tree
	 */
	void enterCurrencySignClause(CobolParser.CurrencySignClauseContext ctx);
	/**
	 * Exit a parse tree produced by {@link CobolParser#currencySignClause}.
	 * @param ctx the parse tree
	 */
	void exitCurrencySignClause(CobolParser.CurrencySignClauseContext ctx);
	/**
	 * Enter a parse tree produced by {@link CobolParser#decimalPointClause}.
	 * @param ctx the parse tree
	 */
	void enterDecimalPointClause(CobolParser.DecimalPointClauseContext ctx);
	/**
	 * Exit a parse tree produced by {@link CobolParser#decimalPointClause}.
	 * @param ctx the parse tree
	 */
	void exitDecimalPointClause(CobolParser.DecimalPointClauseContext ctx);
	/**
	 * Enter a parse tree produced by {@link CobolParser#defaultComputationalSignClause}.
	 * @param ctx the parse tree
	 */
	void enterDefaultComputationalSignClause(CobolParser.DefaultComputationalSignClauseContext ctx);
	/**
	 * Exit a parse tree produced by {@link CobolParser#defaultComputationalSignClause}.
	 * @param ctx the parse tree
	 */
	void exitDefaultComputationalSignClause(CobolParser.DefaultComputationalSignClauseContext ctx);
	/**
	 * Enter a parse tree produced by {@link CobolParser#defaultDisplaySignClause}.
	 * @param ctx the parse tree
	 */
	void enterDefaultDisplaySignClause(CobolParser.DefaultDisplaySignClauseContext ctx);
	/**
	 * Exit a parse tree produced by {@link CobolParser#defaultDisplaySignClause}.
	 * @param ctx the parse tree
	 */
	void exitDefaultDisplaySignClause(CobolParser.DefaultDisplaySignClauseContext ctx);
	/**
	 * Enter a parse tree produced by {@link CobolParser#environmentSwitchNameClause}.
	 * @param ctx the parse tree
	 */
	void enterEnvironmentSwitchNameClause(CobolParser.EnvironmentSwitchNameClauseContext ctx);
	/**
	 * Exit a parse tree produced by {@link CobolParser#environmentSwitchNameClause}.
	 * @param ctx the parse tree
	 */
	void exitEnvironmentSwitchNameClause(CobolParser.EnvironmentSwitchNameClauseContext ctx);
	/**
	 * Enter a parse tree produced by {@link CobolParser#environmentSwitchNameSpecialNamesStatusPhrase}.
	 * @param ctx the parse tree
	 */
	void enterEnvironmentSwitchNameSpecialNamesStatusPhrase(CobolParser.EnvironmentSwitchNameSpecialNamesStatusPhraseContext ctx);
	/**
	 * Exit a parse tree produced by {@link CobolParser#environmentSwitchNameSpecialNamesStatusPhrase}.
	 * @param ctx the parse tree
	 */
	void exitEnvironmentSwitchNameSpecialNamesStatusPhrase(CobolParser.EnvironmentSwitchNameSpecialNamesStatusPhraseContext ctx);
	/**
	 * Enter a parse tree produced by {@link CobolParser#odtClause}.
	 * @param ctx the parse tree
	 */
	void enterOdtClause(CobolParser.OdtClauseContext ctx);
	/**
	 * Exit a parse tree produced by {@link CobolParser#odtClause}.
	 * @param ctx the parse tree
	 */
	void exitOdtClause(CobolParser.OdtClauseContext ctx);
	/**
	 * Enter a parse tree produced by {@link CobolParser#reserveNetworkClause}.
	 * @param ctx the parse tree
	 */
	void enterReserveNetworkClause(CobolParser.ReserveNetworkClauseContext ctx);
	/**
	 * Exit a parse tree produced by {@link CobolParser#reserveNetworkClause}.
	 * @param ctx the parse tree
	 */
	void exitReserveNetworkClause(CobolParser.ReserveNetworkClauseContext ctx);
	/**
	 * Enter a parse tree produced by {@link CobolParser#symbolicCharactersClause}.
	 * @param ctx the parse tree
	 */
	void enterSymbolicCharactersClause(CobolParser.SymbolicCharactersClauseContext ctx);
	/**
	 * Exit a parse tree produced by {@link CobolParser#symbolicCharactersClause}.
	 * @param ctx the parse tree
	 */
	void exitSymbolicCharactersClause(CobolParser.SymbolicCharactersClauseContext ctx);
	/**
	 * Enter a parse tree produced by {@link CobolParser#symbolicCharacters}.
	 * @param ctx the parse tree
	 */
	void enterSymbolicCharacters(CobolParser.SymbolicCharactersContext ctx);
	/**
	 * Exit a parse tree produced by {@link CobolParser#symbolicCharacters}.
	 * @param ctx the parse tree
	 */
	void exitSymbolicCharacters(CobolParser.SymbolicCharactersContext ctx);
	/**
	 * Enter a parse tree produced by {@link CobolParser#inputOutputSection}.
	 * @param ctx the parse tree
	 */
	void enterInputOutputSection(CobolParser.InputOutputSectionContext ctx);
	/**
	 * Exit a parse tree produced by {@link CobolParser#inputOutputSection}.
	 * @param ctx the parse tree
	 */
	void exitInputOutputSection(CobolParser.InputOutputSectionContext ctx);
	/**
	 * Enter a parse tree produced by {@link CobolParser#inputOutputSectionParagraph}.
	 * @param ctx the parse tree
	 */
	void enterInputOutputSectionParagraph(CobolParser.InputOutputSectionParagraphContext ctx);
	/**
	 * Exit a parse tree produced by {@link CobolParser#inputOutputSectionParagraph}.
	 * @param ctx the parse tree
	 */
	void exitInputOutputSectionParagraph(CobolParser.InputOutputSectionParagraphContext ctx);
	/**
	 * Enter a parse tree produced by {@link CobolParser#fileControlParagraph}.
	 * @param ctx the parse tree
	 */
	void enterFileControlParagraph(CobolParser.FileControlParagraphContext ctx);
	/**
	 * Exit a parse tree produced by {@link CobolParser#fileControlParagraph}.
	 * @param ctx the parse tree
	 */
	void exitFileControlParagraph(CobolParser.FileControlParagraphContext ctx);
	/**
	 * Enter a parse tree produced by {@link CobolParser#fileControlEntry}.
	 * @param ctx the parse tree
	 */
	void enterFileControlEntry(CobolParser.FileControlEntryContext ctx);
	/**
	 * Exit a parse tree produced by {@link CobolParser#fileControlEntry}.
	 * @param ctx the parse tree
	 */
	void exitFileControlEntry(CobolParser.FileControlEntryContext ctx);
	/**
	 * Enter a parse tree produced by {@link CobolParser#selectClause}.
	 * @param ctx the parse tree
	 */
	void enterSelectClause(CobolParser.SelectClauseContext ctx);
	/**
	 * Exit a parse tree produced by {@link CobolParser#selectClause}.
	 * @param ctx the parse tree
	 */
	void exitSelectClause(CobolParser.SelectClauseContext ctx);
	/**
	 * Enter a parse tree produced by {@link CobolParser#fileControlClause}.
	 * @param ctx the parse tree
	 */
	void enterFileControlClause(CobolParser.FileControlClauseContext ctx);
	/**
	 * Exit a parse tree produced by {@link CobolParser#fileControlClause}.
	 * @param ctx the parse tree
	 */
	void exitFileControlClause(CobolParser.FileControlClauseContext ctx);
	/**
	 * Enter a parse tree produced by {@link CobolParser#assignClause}.
	 * @param ctx the parse tree
	 */
	void enterAssignClause(CobolParser.AssignClauseContext ctx);
	/**
	 * Exit a parse tree produced by {@link CobolParser#assignClause}.
	 * @param ctx the parse tree
	 */
	void exitAssignClause(CobolParser.AssignClauseContext ctx);
	/**
	 * Enter a parse tree produced by {@link CobolParser#reserveClause}.
	 * @param ctx the parse tree
	 */
	void enterReserveClause(CobolParser.ReserveClauseContext ctx);
	/**
	 * Exit a parse tree produced by {@link CobolParser#reserveClause}.
	 * @param ctx the parse tree
	 */
	void exitReserveClause(CobolParser.ReserveClauseContext ctx);
	/**
	 * Enter a parse tree produced by {@link CobolParser#organizationClause}.
	 * @param ctx the parse tree
	 */
	void enterOrganizationClause(CobolParser.OrganizationClauseContext ctx);
	/**
	 * Exit a parse tree produced by {@link CobolParser#organizationClause}.
	 * @param ctx the parse tree
	 */
	void exitOrganizationClause(CobolParser.OrganizationClauseContext ctx);
	/**
	 * Enter a parse tree produced by {@link CobolParser#paddingCharacterClause}.
	 * @param ctx the parse tree
	 */
	void enterPaddingCharacterClause(CobolParser.PaddingCharacterClauseContext ctx);
	/**
	 * Exit a parse tree produced by {@link CobolParser#paddingCharacterClause}.
	 * @param ctx the parse tree
	 */
	void exitPaddingCharacterClause(CobolParser.PaddingCharacterClauseContext ctx);
	/**
	 * Enter a parse tree produced by {@link CobolParser#recordDelimiterClause}.
	 * @param ctx the parse tree
	 */
	void enterRecordDelimiterClause(CobolParser.RecordDelimiterClauseContext ctx);
	/**
	 * Exit a parse tree produced by {@link CobolParser#recordDelimiterClause}.
	 * @param ctx the parse tree
	 */
	void exitRecordDelimiterClause(CobolParser.RecordDelimiterClauseContext ctx);
	/**
	 * Enter a parse tree produced by {@link CobolParser#accessModeClause}.
	 * @param ctx the parse tree
	 */
	void enterAccessModeClause(CobolParser.AccessModeClauseContext ctx);
	/**
	 * Exit a parse tree produced by {@link CobolParser#accessModeClause}.
	 * @param ctx the parse tree
	 */
	void exitAccessModeClause(CobolParser.AccessModeClauseContext ctx);
	/**
	 * Enter a parse tree produced by {@link CobolParser#recordKeyClause}.
	 * @param ctx the parse tree
	 */
	void enterRecordKeyClause(CobolParser.RecordKeyClauseContext ctx);
	/**
	 * Exit a parse tree produced by {@link CobolParser#recordKeyClause}.
	 * @param ctx the parse tree
	 */
	void exitRecordKeyClause(CobolParser.RecordKeyClauseContext ctx);
	/**
	 * Enter a parse tree produced by {@link CobolParser#alternateRecordKeyClause}.
	 * @param ctx the parse tree
	 */
	void enterAlternateRecordKeyClause(CobolParser.AlternateRecordKeyClauseContext ctx);
	/**
	 * Exit a parse tree produced by {@link CobolParser#alternateRecordKeyClause}.
	 * @param ctx the parse tree
	 */
	void exitAlternateRecordKeyClause(CobolParser.AlternateRecordKeyClauseContext ctx);
	/**
	 * Enter a parse tree produced by {@link CobolParser#passwordClause}.
	 * @param ctx the parse tree
	 */
	void enterPasswordClause(CobolParser.PasswordClauseContext ctx);
	/**
	 * Exit a parse tree produced by {@link CobolParser#passwordClause}.
	 * @param ctx the parse tree
	 */
	void exitPasswordClause(CobolParser.PasswordClauseContext ctx);
	/**
	 * Enter a parse tree produced by {@link CobolParser#fileStatusClause}.
	 * @param ctx the parse tree
	 */
	void enterFileStatusClause(CobolParser.FileStatusClauseContext ctx);
	/**
	 * Exit a parse tree produced by {@link CobolParser#fileStatusClause}.
	 * @param ctx the parse tree
	 */
	void exitFileStatusClause(CobolParser.FileStatusClauseContext ctx);
	/**
	 * Enter a parse tree produced by {@link CobolParser#relativeKeyClause}.
	 * @param ctx the parse tree
	 */
	void enterRelativeKeyClause(CobolParser.RelativeKeyClauseContext ctx);
	/**
	 * Exit a parse tree produced by {@link CobolParser#relativeKeyClause}.
	 * @param ctx the parse tree
	 */
	void exitRelativeKeyClause(CobolParser.RelativeKeyClauseContext ctx);
	/**
	 * Enter a parse tree produced by {@link CobolParser#ioControlParagraph}.
	 * @param ctx the parse tree
	 */
	void enterIoControlParagraph(CobolParser.IoControlParagraphContext ctx);
	/**
	 * Exit a parse tree produced by {@link CobolParser#ioControlParagraph}.
	 * @param ctx the parse tree
	 */
	void exitIoControlParagraph(CobolParser.IoControlParagraphContext ctx);
	/**
	 * Enter a parse tree produced by {@link CobolParser#ioControlClause}.
	 * @param ctx the parse tree
	 */
	void enterIoControlClause(CobolParser.IoControlClauseContext ctx);
	/**
	 * Exit a parse tree produced by {@link CobolParser#ioControlClause}.
	 * @param ctx the parse tree
	 */
	void exitIoControlClause(CobolParser.IoControlClauseContext ctx);
	/**
	 * Enter a parse tree produced by {@link CobolParser#rerunClause}.
	 * @param ctx the parse tree
	 */
	void enterRerunClause(CobolParser.RerunClauseContext ctx);
	/**
	 * Exit a parse tree produced by {@link CobolParser#rerunClause}.
	 * @param ctx the parse tree
	 */
	void exitRerunClause(CobolParser.RerunClauseContext ctx);
	/**
	 * Enter a parse tree produced by {@link CobolParser#rerunEveryRecords}.
	 * @param ctx the parse tree
	 */
	void enterRerunEveryRecords(CobolParser.RerunEveryRecordsContext ctx);
	/**
	 * Exit a parse tree produced by {@link CobolParser#rerunEveryRecords}.
	 * @param ctx the parse tree
	 */
	void exitRerunEveryRecords(CobolParser.RerunEveryRecordsContext ctx);
	/**
	 * Enter a parse tree produced by {@link CobolParser#rerunEveryOf}.
	 * @param ctx the parse tree
	 */
	void enterRerunEveryOf(CobolParser.RerunEveryOfContext ctx);
	/**
	 * Exit a parse tree produced by {@link CobolParser#rerunEveryOf}.
	 * @param ctx the parse tree
	 */
	void exitRerunEveryOf(CobolParser.RerunEveryOfContext ctx);
	/**
	 * Enter a parse tree produced by {@link CobolParser#rerunEveryClock}.
	 * @param ctx the parse tree
	 */
	void enterRerunEveryClock(CobolParser.RerunEveryClockContext ctx);
	/**
	 * Exit a parse tree produced by {@link CobolParser#rerunEveryClock}.
	 * @param ctx the parse tree
	 */
	void exitRerunEveryClock(CobolParser.RerunEveryClockContext ctx);
	/**
	 * Enter a parse tree produced by {@link CobolParser#sameClause}.
	 * @param ctx the parse tree
	 */
	void enterSameClause(CobolParser.SameClauseContext ctx);
	/**
	 * Exit a parse tree produced by {@link CobolParser#sameClause}.
	 * @param ctx the parse tree
	 */
	void exitSameClause(CobolParser.SameClauseContext ctx);
	/**
	 * Enter a parse tree produced by {@link CobolParser#multipleFileClause}.
	 * @param ctx the parse tree
	 */
	void enterMultipleFileClause(CobolParser.MultipleFileClauseContext ctx);
	/**
	 * Exit a parse tree produced by {@link CobolParser#multipleFileClause}.
	 * @param ctx the parse tree
	 */
	void exitMultipleFileClause(CobolParser.MultipleFileClauseContext ctx);
	/**
	 * Enter a parse tree produced by {@link CobolParser#multipleFilePosition}.
	 * @param ctx the parse tree
	 */
	void enterMultipleFilePosition(CobolParser.MultipleFilePositionContext ctx);
	/**
	 * Exit a parse tree produced by {@link CobolParser#multipleFilePosition}.
	 * @param ctx the parse tree
	 */
	void exitMultipleFilePosition(CobolParser.MultipleFilePositionContext ctx);
	/**
	 * Enter a parse tree produced by {@link CobolParser#commitmentControlClause}.
	 * @param ctx the parse tree
	 */
	void enterCommitmentControlClause(CobolParser.CommitmentControlClauseContext ctx);
	/**
	 * Exit a parse tree produced by {@link CobolParser#commitmentControlClause}.
	 * @param ctx the parse tree
	 */
	void exitCommitmentControlClause(CobolParser.CommitmentControlClauseContext ctx);
	/**
	 * Enter a parse tree produced by {@link CobolParser#dataDivision}.
	 * @param ctx the parse tree
	 */
	void enterDataDivision(CobolParser.DataDivisionContext ctx);
	/**
	 * Exit a parse tree produced by {@link CobolParser#dataDivision}.
	 * @param ctx the parse tree
	 */
	void exitDataDivision(CobolParser.DataDivisionContext ctx);
	/**
	 * Enter a parse tree produced by {@link CobolParser#dataDivisionSection}.
	 * @param ctx the parse tree
	 */
	void enterDataDivisionSection(CobolParser.DataDivisionSectionContext ctx);
	/**
	 * Exit a parse tree produced by {@link CobolParser#dataDivisionSection}.
	 * @param ctx the parse tree
	 */
	void exitDataDivisionSection(CobolParser.DataDivisionSectionContext ctx);
	/**
	 * Enter a parse tree produced by {@link CobolParser#fileSection}.
	 * @param ctx the parse tree
	 */
	void enterFileSection(CobolParser.FileSectionContext ctx);
	/**
	 * Exit a parse tree produced by {@link CobolParser#fileSection}.
	 * @param ctx the parse tree
	 */
	void exitFileSection(CobolParser.FileSectionContext ctx);
	/**
	 * Enter a parse tree produced by {@link CobolParser#fileDescriptionEntry}.
	 * @param ctx the parse tree
	 */
	void enterFileDescriptionEntry(CobolParser.FileDescriptionEntryContext ctx);
	/**
	 * Exit a parse tree produced by {@link CobolParser#fileDescriptionEntry}.
	 * @param ctx the parse tree
	 */
	void exitFileDescriptionEntry(CobolParser.FileDescriptionEntryContext ctx);
	/**
	 * Enter a parse tree produced by {@link CobolParser#fileDescriptionEntryClause}.
	 * @param ctx the parse tree
	 */
	void enterFileDescriptionEntryClause(CobolParser.FileDescriptionEntryClauseContext ctx);
	/**
	 * Exit a parse tree produced by {@link CobolParser#fileDescriptionEntryClause}.
	 * @param ctx the parse tree
	 */
	void exitFileDescriptionEntryClause(CobolParser.FileDescriptionEntryClauseContext ctx);
	/**
	 * Enter a parse tree produced by {@link CobolParser#externalClause}.
	 * @param ctx the parse tree
	 */
	void enterExternalClause(CobolParser.ExternalClauseContext ctx);
	/**
	 * Exit a parse tree produced by {@link CobolParser#externalClause}.
	 * @param ctx the parse tree
	 */
	void exitExternalClause(CobolParser.ExternalClauseContext ctx);
	/**
	 * Enter a parse tree produced by {@link CobolParser#globalClause}.
	 * @param ctx the parse tree
	 */
	void enterGlobalClause(CobolParser.GlobalClauseContext ctx);
	/**
	 * Exit a parse tree produced by {@link CobolParser#globalClause}.
	 * @param ctx the parse tree
	 */
	void exitGlobalClause(CobolParser.GlobalClauseContext ctx);
	/**
	 * Enter a parse tree produced by {@link CobolParser#blockContainsClause}.
	 * @param ctx the parse tree
	 */
	void enterBlockContainsClause(CobolParser.BlockContainsClauseContext ctx);
	/**
	 * Exit a parse tree produced by {@link CobolParser#blockContainsClause}.
	 * @param ctx the parse tree
	 */
	void exitBlockContainsClause(CobolParser.BlockContainsClauseContext ctx);
	/**
	 * Enter a parse tree produced by {@link CobolParser#blockContainsTo}.
	 * @param ctx the parse tree
	 */
	void enterBlockContainsTo(CobolParser.BlockContainsToContext ctx);
	/**
	 * Exit a parse tree produced by {@link CobolParser#blockContainsTo}.
	 * @param ctx the parse tree
	 */
	void exitBlockContainsTo(CobolParser.BlockContainsToContext ctx);
	/**
	 * Enter a parse tree produced by {@link CobolParser#recordContainsClause}.
	 * @param ctx the parse tree
	 */
	void enterRecordContainsClause(CobolParser.RecordContainsClauseContext ctx);
	/**
	 * Exit a parse tree produced by {@link CobolParser#recordContainsClause}.
	 * @param ctx the parse tree
	 */
	void exitRecordContainsClause(CobolParser.RecordContainsClauseContext ctx);
	/**
	 * Enter a parse tree produced by {@link CobolParser#recordContainsClauseFormat1}.
	 * @param ctx the parse tree
	 */
	void enterRecordContainsClauseFormat1(CobolParser.RecordContainsClauseFormat1Context ctx);
	/**
	 * Exit a parse tree produced by {@link CobolParser#recordContainsClauseFormat1}.
	 * @param ctx the parse tree
	 */
	void exitRecordContainsClauseFormat1(CobolParser.RecordContainsClauseFormat1Context ctx);
	/**
	 * Enter a parse tree produced by {@link CobolParser#recordContainsClauseFormat2}.
	 * @param ctx the parse tree
	 */
	void enterRecordContainsClauseFormat2(CobolParser.RecordContainsClauseFormat2Context ctx);
	/**
	 * Exit a parse tree produced by {@link CobolParser#recordContainsClauseFormat2}.
	 * @param ctx the parse tree
	 */
	void exitRecordContainsClauseFormat2(CobolParser.RecordContainsClauseFormat2Context ctx);
	/**
	 * Enter a parse tree produced by {@link CobolParser#recordContainsClauseFormat3}.
	 * @param ctx the parse tree
	 */
	void enterRecordContainsClauseFormat3(CobolParser.RecordContainsClauseFormat3Context ctx);
	/**
	 * Exit a parse tree produced by {@link CobolParser#recordContainsClauseFormat3}.
	 * @param ctx the parse tree
	 */
	void exitRecordContainsClauseFormat3(CobolParser.RecordContainsClauseFormat3Context ctx);
	/**
	 * Enter a parse tree produced by {@link CobolParser#recordContainsTo}.
	 * @param ctx the parse tree
	 */
	void enterRecordContainsTo(CobolParser.RecordContainsToContext ctx);
	/**
	 * Exit a parse tree produced by {@link CobolParser#recordContainsTo}.
	 * @param ctx the parse tree
	 */
	void exitRecordContainsTo(CobolParser.RecordContainsToContext ctx);
	/**
	 * Enter a parse tree produced by {@link CobolParser#labelRecordsClause}.
	 * @param ctx the parse tree
	 */
	void enterLabelRecordsClause(CobolParser.LabelRecordsClauseContext ctx);
	/**
	 * Exit a parse tree produced by {@link CobolParser#labelRecordsClause}.
	 * @param ctx the parse tree
	 */
	void exitLabelRecordsClause(CobolParser.LabelRecordsClauseContext ctx);
	/**
	 * Enter a parse tree produced by {@link CobolParser#valueOfClause}.
	 * @param ctx the parse tree
	 */
	void enterValueOfClause(CobolParser.ValueOfClauseContext ctx);
	/**
	 * Exit a parse tree produced by {@link CobolParser#valueOfClause}.
	 * @param ctx the parse tree
	 */
	void exitValueOfClause(CobolParser.ValueOfClauseContext ctx);
	/**
	 * Enter a parse tree produced by {@link CobolParser#valuePair}.
	 * @param ctx the parse tree
	 */
	void enterValuePair(CobolParser.ValuePairContext ctx);
	/**
	 * Exit a parse tree produced by {@link CobolParser#valuePair}.
	 * @param ctx the parse tree
	 */
	void exitValuePair(CobolParser.ValuePairContext ctx);
	/**
	 * Enter a parse tree produced by {@link CobolParser#dataRecordsClause}.
	 * @param ctx the parse tree
	 */
	void enterDataRecordsClause(CobolParser.DataRecordsClauseContext ctx);
	/**
	 * Exit a parse tree produced by {@link CobolParser#dataRecordsClause}.
	 * @param ctx the parse tree
	 */
	void exitDataRecordsClause(CobolParser.DataRecordsClauseContext ctx);
	/**
	 * Enter a parse tree produced by {@link CobolParser#linageClause}.
	 * @param ctx the parse tree
	 */
	void enterLinageClause(CobolParser.LinageClauseContext ctx);
	/**
	 * Exit a parse tree produced by {@link CobolParser#linageClause}.
	 * @param ctx the parse tree
	 */
	void exitLinageClause(CobolParser.LinageClauseContext ctx);
	/**
	 * Enter a parse tree produced by {@link CobolParser#linageAt}.
	 * @param ctx the parse tree
	 */
	void enterLinageAt(CobolParser.LinageAtContext ctx);
	/**
	 * Exit a parse tree produced by {@link CobolParser#linageAt}.
	 * @param ctx the parse tree
	 */
	void exitLinageAt(CobolParser.LinageAtContext ctx);
	/**
	 * Enter a parse tree produced by {@link CobolParser#linageFootingAt}.
	 * @param ctx the parse tree
	 */
	void enterLinageFootingAt(CobolParser.LinageFootingAtContext ctx);
	/**
	 * Exit a parse tree produced by {@link CobolParser#linageFootingAt}.
	 * @param ctx the parse tree
	 */
	void exitLinageFootingAt(CobolParser.LinageFootingAtContext ctx);
	/**
	 * Enter a parse tree produced by {@link CobolParser#linageLinesAtTop}.
	 * @param ctx the parse tree
	 */
	void enterLinageLinesAtTop(CobolParser.LinageLinesAtTopContext ctx);
	/**
	 * Exit a parse tree produced by {@link CobolParser#linageLinesAtTop}.
	 * @param ctx the parse tree
	 */
	void exitLinageLinesAtTop(CobolParser.LinageLinesAtTopContext ctx);
	/**
	 * Enter a parse tree produced by {@link CobolParser#linageLinesAtBottom}.
	 * @param ctx the parse tree
	 */
	void enterLinageLinesAtBottom(CobolParser.LinageLinesAtBottomContext ctx);
	/**
	 * Exit a parse tree produced by {@link CobolParser#linageLinesAtBottom}.
	 * @param ctx the parse tree
	 */
	void exitLinageLinesAtBottom(CobolParser.LinageLinesAtBottomContext ctx);
	/**
	 * Enter a parse tree produced by {@link CobolParser#recordingModeClause}.
	 * @param ctx the parse tree
	 */
	void enterRecordingModeClause(CobolParser.RecordingModeClauseContext ctx);
	/**
	 * Exit a parse tree produced by {@link CobolParser#recordingModeClause}.
	 * @param ctx the parse tree
	 */
	void exitRecordingModeClause(CobolParser.RecordingModeClauseContext ctx);
	/**
	 * Enter a parse tree produced by {@link CobolParser#modeStatement}.
	 * @param ctx the parse tree
	 */
	void enterModeStatement(CobolParser.ModeStatementContext ctx);
	/**
	 * Exit a parse tree produced by {@link CobolParser#modeStatement}.
	 * @param ctx the parse tree
	 */
	void exitModeStatement(CobolParser.ModeStatementContext ctx);
	/**
	 * Enter a parse tree produced by {@link CobolParser#codeSetClause}.
	 * @param ctx the parse tree
	 */
	void enterCodeSetClause(CobolParser.CodeSetClauseContext ctx);
	/**
	 * Exit a parse tree produced by {@link CobolParser#codeSetClause}.
	 * @param ctx the parse tree
	 */
	void exitCodeSetClause(CobolParser.CodeSetClauseContext ctx);
	/**
	 * Enter a parse tree produced by {@link CobolParser#reportClause}.
	 * @param ctx the parse tree
	 */
	void enterReportClause(CobolParser.ReportClauseContext ctx);
	/**
	 * Exit a parse tree produced by {@link CobolParser#reportClause}.
	 * @param ctx the parse tree
	 */
	void exitReportClause(CobolParser.ReportClauseContext ctx);
	/**
	 * Enter a parse tree produced by {@link CobolParser#dataBaseSection}.
	 * @param ctx the parse tree
	 */
	void enterDataBaseSection(CobolParser.DataBaseSectionContext ctx);
	/**
	 * Exit a parse tree produced by {@link CobolParser#dataBaseSection}.
	 * @param ctx the parse tree
	 */
	void exitDataBaseSection(CobolParser.DataBaseSectionContext ctx);
	/**
	 * Enter a parse tree produced by {@link CobolParser#dataBaseSectionEntry}.
	 * @param ctx the parse tree
	 */
	void enterDataBaseSectionEntry(CobolParser.DataBaseSectionEntryContext ctx);
	/**
	 * Exit a parse tree produced by {@link CobolParser#dataBaseSectionEntry}.
	 * @param ctx the parse tree
	 */
	void exitDataBaseSectionEntry(CobolParser.DataBaseSectionEntryContext ctx);
	/**
	 * Enter a parse tree produced by {@link CobolParser#workingStorageSection}.
	 * @param ctx the parse tree
	 */
	void enterWorkingStorageSection(CobolParser.WorkingStorageSectionContext ctx);
	/**
	 * Exit a parse tree produced by {@link CobolParser#workingStorageSection}.
	 * @param ctx the parse tree
	 */
	void exitWorkingStorageSection(CobolParser.WorkingStorageSectionContext ctx);
	/**
	 * Enter a parse tree produced by {@link CobolParser#linkageSection}.
	 * @param ctx the parse tree
	 */
	void enterLinkageSection(CobolParser.LinkageSectionContext ctx);
	/**
	 * Exit a parse tree produced by {@link CobolParser#linkageSection}.
	 * @param ctx the parse tree
	 */
	void exitLinkageSection(CobolParser.LinkageSectionContext ctx);
	/**
	 * Enter a parse tree produced by {@link CobolParser#communicationSection}.
	 * @param ctx the parse tree
	 */
	void enterCommunicationSection(CobolParser.CommunicationSectionContext ctx);
	/**
	 * Exit a parse tree produced by {@link CobolParser#communicationSection}.
	 * @param ctx the parse tree
	 */
	void exitCommunicationSection(CobolParser.CommunicationSectionContext ctx);
	/**
	 * Enter a parse tree produced by {@link CobolParser#communicationDescriptionEntry}.
	 * @param ctx the parse tree
	 */
	void enterCommunicationDescriptionEntry(CobolParser.CommunicationDescriptionEntryContext ctx);
	/**
	 * Exit a parse tree produced by {@link CobolParser#communicationDescriptionEntry}.
	 * @param ctx the parse tree
	 */
	void exitCommunicationDescriptionEntry(CobolParser.CommunicationDescriptionEntryContext ctx);
	/**
	 * Enter a parse tree produced by {@link CobolParser#communicationDescriptionEntryFormat1}.
	 * @param ctx the parse tree
	 */
	void enterCommunicationDescriptionEntryFormat1(CobolParser.CommunicationDescriptionEntryFormat1Context ctx);
	/**
	 * Exit a parse tree produced by {@link CobolParser#communicationDescriptionEntryFormat1}.
	 * @param ctx the parse tree
	 */
	void exitCommunicationDescriptionEntryFormat1(CobolParser.CommunicationDescriptionEntryFormat1Context ctx);
	/**
	 * Enter a parse tree produced by {@link CobolParser#communicationDescriptionEntryFormat2}.
	 * @param ctx the parse tree
	 */
	void enterCommunicationDescriptionEntryFormat2(CobolParser.CommunicationDescriptionEntryFormat2Context ctx);
	/**
	 * Exit a parse tree produced by {@link CobolParser#communicationDescriptionEntryFormat2}.
	 * @param ctx the parse tree
	 */
	void exitCommunicationDescriptionEntryFormat2(CobolParser.CommunicationDescriptionEntryFormat2Context ctx);
	/**
	 * Enter a parse tree produced by {@link CobolParser#communicationDescriptionEntryFormat3}.
	 * @param ctx the parse tree
	 */
	void enterCommunicationDescriptionEntryFormat3(CobolParser.CommunicationDescriptionEntryFormat3Context ctx);
	/**
	 * Exit a parse tree produced by {@link CobolParser#communicationDescriptionEntryFormat3}.
	 * @param ctx the parse tree
	 */
	void exitCommunicationDescriptionEntryFormat3(CobolParser.CommunicationDescriptionEntryFormat3Context ctx);
	/**
	 * Enter a parse tree produced by {@link CobolParser#destinationCountClause}.
	 * @param ctx the parse tree
	 */
	void enterDestinationCountClause(CobolParser.DestinationCountClauseContext ctx);
	/**
	 * Exit a parse tree produced by {@link CobolParser#destinationCountClause}.
	 * @param ctx the parse tree
	 */
	void exitDestinationCountClause(CobolParser.DestinationCountClauseContext ctx);
	/**
	 * Enter a parse tree produced by {@link CobolParser#destinationTableClause}.
	 * @param ctx the parse tree
	 */
	void enterDestinationTableClause(CobolParser.DestinationTableClauseContext ctx);
	/**
	 * Exit a parse tree produced by {@link CobolParser#destinationTableClause}.
	 * @param ctx the parse tree
	 */
	void exitDestinationTableClause(CobolParser.DestinationTableClauseContext ctx);
	/**
	 * Enter a parse tree produced by {@link CobolParser#endKeyClause}.
	 * @param ctx the parse tree
	 */
	void enterEndKeyClause(CobolParser.EndKeyClauseContext ctx);
	/**
	 * Exit a parse tree produced by {@link CobolParser#endKeyClause}.
	 * @param ctx the parse tree
	 */
	void exitEndKeyClause(CobolParser.EndKeyClauseContext ctx);
	/**
	 * Enter a parse tree produced by {@link CobolParser#errorKeyClause}.
	 * @param ctx the parse tree
	 */
	void enterErrorKeyClause(CobolParser.ErrorKeyClauseContext ctx);
	/**
	 * Exit a parse tree produced by {@link CobolParser#errorKeyClause}.
	 * @param ctx the parse tree
	 */
	void exitErrorKeyClause(CobolParser.ErrorKeyClauseContext ctx);
	/**
	 * Enter a parse tree produced by {@link CobolParser#messageCountClause}.
	 * @param ctx the parse tree
	 */
	void enterMessageCountClause(CobolParser.MessageCountClauseContext ctx);
	/**
	 * Exit a parse tree produced by {@link CobolParser#messageCountClause}.
	 * @param ctx the parse tree
	 */
	void exitMessageCountClause(CobolParser.MessageCountClauseContext ctx);
	/**
	 * Enter a parse tree produced by {@link CobolParser#messageDateClause}.
	 * @param ctx the parse tree
	 */
	void enterMessageDateClause(CobolParser.MessageDateClauseContext ctx);
	/**
	 * Exit a parse tree produced by {@link CobolParser#messageDateClause}.
	 * @param ctx the parse tree
	 */
	void exitMessageDateClause(CobolParser.MessageDateClauseContext ctx);
	/**
	 * Enter a parse tree produced by {@link CobolParser#messageTimeClause}.
	 * @param ctx the parse tree
	 */
	void enterMessageTimeClause(CobolParser.MessageTimeClauseContext ctx);
	/**
	 * Exit a parse tree produced by {@link CobolParser#messageTimeClause}.
	 * @param ctx the parse tree
	 */
	void exitMessageTimeClause(CobolParser.MessageTimeClauseContext ctx);
	/**
	 * Enter a parse tree produced by {@link CobolParser#statusKeyClause}.
	 * @param ctx the parse tree
	 */
	void enterStatusKeyClause(CobolParser.StatusKeyClauseContext ctx);
	/**
	 * Exit a parse tree produced by {@link CobolParser#statusKeyClause}.
	 * @param ctx the parse tree
	 */
	void exitStatusKeyClause(CobolParser.StatusKeyClauseContext ctx);
	/**
	 * Enter a parse tree produced by {@link CobolParser#symbolicDestinationClause}.
	 * @param ctx the parse tree
	 */
	void enterSymbolicDestinationClause(CobolParser.SymbolicDestinationClauseContext ctx);
	/**
	 * Exit a parse tree produced by {@link CobolParser#symbolicDestinationClause}.
	 * @param ctx the parse tree
	 */
	void exitSymbolicDestinationClause(CobolParser.SymbolicDestinationClauseContext ctx);
	/**
	 * Enter a parse tree produced by {@link CobolParser#symbolicQueueClause}.
	 * @param ctx the parse tree
	 */
	void enterSymbolicQueueClause(CobolParser.SymbolicQueueClauseContext ctx);
	/**
	 * Exit a parse tree produced by {@link CobolParser#symbolicQueueClause}.
	 * @param ctx the parse tree
	 */
	void exitSymbolicQueueClause(CobolParser.SymbolicQueueClauseContext ctx);
	/**
	 * Enter a parse tree produced by {@link CobolParser#symbolicSourceClause}.
	 * @param ctx the parse tree
	 */
	void enterSymbolicSourceClause(CobolParser.SymbolicSourceClauseContext ctx);
	/**
	 * Exit a parse tree produced by {@link CobolParser#symbolicSourceClause}.
	 * @param ctx the parse tree
	 */
	void exitSymbolicSourceClause(CobolParser.SymbolicSourceClauseContext ctx);
	/**
	 * Enter a parse tree produced by {@link CobolParser#symbolicTerminalClause}.
	 * @param ctx the parse tree
	 */
	void enterSymbolicTerminalClause(CobolParser.SymbolicTerminalClauseContext ctx);
	/**
	 * Exit a parse tree produced by {@link CobolParser#symbolicTerminalClause}.
	 * @param ctx the parse tree
	 */
	void exitSymbolicTerminalClause(CobolParser.SymbolicTerminalClauseContext ctx);
	/**
	 * Enter a parse tree produced by {@link CobolParser#symbolicSubQueueClause}.
	 * @param ctx the parse tree
	 */
	void enterSymbolicSubQueueClause(CobolParser.SymbolicSubQueueClauseContext ctx);
	/**
	 * Exit a parse tree produced by {@link CobolParser#symbolicSubQueueClause}.
	 * @param ctx the parse tree
	 */
	void exitSymbolicSubQueueClause(CobolParser.SymbolicSubQueueClauseContext ctx);
	/**
	 * Enter a parse tree produced by {@link CobolParser#textLengthClause}.
	 * @param ctx the parse tree
	 */
	void enterTextLengthClause(CobolParser.TextLengthClauseContext ctx);
	/**
	 * Exit a parse tree produced by {@link CobolParser#textLengthClause}.
	 * @param ctx the parse tree
	 */
	void exitTextLengthClause(CobolParser.TextLengthClauseContext ctx);
	/**
	 * Enter a parse tree produced by {@link CobolParser#localStorageSection}.
	 * @param ctx the parse tree
	 */
	void enterLocalStorageSection(CobolParser.LocalStorageSectionContext ctx);
	/**
	 * Exit a parse tree produced by {@link CobolParser#localStorageSection}.
	 * @param ctx the parse tree
	 */
	void exitLocalStorageSection(CobolParser.LocalStorageSectionContext ctx);
	/**
	 * Enter a parse tree produced by {@link CobolParser#screenSection}.
	 * @param ctx the parse tree
	 */
	void enterScreenSection(CobolParser.ScreenSectionContext ctx);
	/**
	 * Exit a parse tree produced by {@link CobolParser#screenSection}.
	 * @param ctx the parse tree
	 */
	void exitScreenSection(CobolParser.ScreenSectionContext ctx);
	/**
	 * Enter a parse tree produced by {@link CobolParser#screenDescriptionEntry}.
	 * @param ctx the parse tree
	 */
	void enterScreenDescriptionEntry(CobolParser.ScreenDescriptionEntryContext ctx);
	/**
	 * Exit a parse tree produced by {@link CobolParser#screenDescriptionEntry}.
	 * @param ctx the parse tree
	 */
	void exitScreenDescriptionEntry(CobolParser.ScreenDescriptionEntryContext ctx);
	/**
	 * Enter a parse tree produced by {@link CobolParser#screenDescriptionBlankClause}.
	 * @param ctx the parse tree
	 */
	void enterScreenDescriptionBlankClause(CobolParser.ScreenDescriptionBlankClauseContext ctx);
	/**
	 * Exit a parse tree produced by {@link CobolParser#screenDescriptionBlankClause}.
	 * @param ctx the parse tree
	 */
	void exitScreenDescriptionBlankClause(CobolParser.ScreenDescriptionBlankClauseContext ctx);
	/**
	 * Enter a parse tree produced by {@link CobolParser#screenDescriptionBellClause}.
	 * @param ctx the parse tree
	 */
	void enterScreenDescriptionBellClause(CobolParser.ScreenDescriptionBellClauseContext ctx);
	/**
	 * Exit a parse tree produced by {@link CobolParser#screenDescriptionBellClause}.
	 * @param ctx the parse tree
	 */
	void exitScreenDescriptionBellClause(CobolParser.ScreenDescriptionBellClauseContext ctx);
	/**
	 * Enter a parse tree produced by {@link CobolParser#screenDescriptionBlinkClause}.
	 * @param ctx the parse tree
	 */
	void enterScreenDescriptionBlinkClause(CobolParser.ScreenDescriptionBlinkClauseContext ctx);
	/**
	 * Exit a parse tree produced by {@link CobolParser#screenDescriptionBlinkClause}.
	 * @param ctx the parse tree
	 */
	void exitScreenDescriptionBlinkClause(CobolParser.ScreenDescriptionBlinkClauseContext ctx);
	/**
	 * Enter a parse tree produced by {@link CobolParser#screenDescriptionEraseClause}.
	 * @param ctx the parse tree
	 */
	void enterScreenDescriptionEraseClause(CobolParser.ScreenDescriptionEraseClauseContext ctx);
	/**
	 * Exit a parse tree produced by {@link CobolParser#screenDescriptionEraseClause}.
	 * @param ctx the parse tree
	 */
	void exitScreenDescriptionEraseClause(CobolParser.ScreenDescriptionEraseClauseContext ctx);
	/**
	 * Enter a parse tree produced by {@link CobolParser#screenDescriptionLightClause}.
	 * @param ctx the parse tree
	 */
	void enterScreenDescriptionLightClause(CobolParser.ScreenDescriptionLightClauseContext ctx);
	/**
	 * Exit a parse tree produced by {@link CobolParser#screenDescriptionLightClause}.
	 * @param ctx the parse tree
	 */
	void exitScreenDescriptionLightClause(CobolParser.ScreenDescriptionLightClauseContext ctx);
	/**
	 * Enter a parse tree produced by {@link CobolParser#screenDescriptionGridClause}.
	 * @param ctx the parse tree
	 */
	void enterScreenDescriptionGridClause(CobolParser.ScreenDescriptionGridClauseContext ctx);
	/**
	 * Exit a parse tree produced by {@link CobolParser#screenDescriptionGridClause}.
	 * @param ctx the parse tree
	 */
	void exitScreenDescriptionGridClause(CobolParser.ScreenDescriptionGridClauseContext ctx);
	/**
	 * Enter a parse tree produced by {@link CobolParser#screenDescriptionReverseVideoClause}.
	 * @param ctx the parse tree
	 */
	void enterScreenDescriptionReverseVideoClause(CobolParser.ScreenDescriptionReverseVideoClauseContext ctx);
	/**
	 * Exit a parse tree produced by {@link CobolParser#screenDescriptionReverseVideoClause}.
	 * @param ctx the parse tree
	 */
	void exitScreenDescriptionReverseVideoClause(CobolParser.ScreenDescriptionReverseVideoClauseContext ctx);
	/**
	 * Enter a parse tree produced by {@link CobolParser#screenDescriptionUnderlineClause}.
	 * @param ctx the parse tree
	 */
	void enterScreenDescriptionUnderlineClause(CobolParser.ScreenDescriptionUnderlineClauseContext ctx);
	/**
	 * Exit a parse tree produced by {@link CobolParser#screenDescriptionUnderlineClause}.
	 * @param ctx the parse tree
	 */
	void exitScreenDescriptionUnderlineClause(CobolParser.ScreenDescriptionUnderlineClauseContext ctx);
	/**
	 * Enter a parse tree produced by {@link CobolParser#screenDescriptionSizeClause}.
	 * @param ctx the parse tree
	 */
	void enterScreenDescriptionSizeClause(CobolParser.ScreenDescriptionSizeClauseContext ctx);
	/**
	 * Exit a parse tree produced by {@link CobolParser#screenDescriptionSizeClause}.
	 * @param ctx the parse tree
	 */
	void exitScreenDescriptionSizeClause(CobolParser.ScreenDescriptionSizeClauseContext ctx);
	/**
	 * Enter a parse tree produced by {@link CobolParser#screenDescriptionLineClause}.
	 * @param ctx the parse tree
	 */
	void enterScreenDescriptionLineClause(CobolParser.ScreenDescriptionLineClauseContext ctx);
	/**
	 * Exit a parse tree produced by {@link CobolParser#screenDescriptionLineClause}.
	 * @param ctx the parse tree
	 */
	void exitScreenDescriptionLineClause(CobolParser.ScreenDescriptionLineClauseContext ctx);
	/**
	 * Enter a parse tree produced by {@link CobolParser#screenDescriptionColumnClause}.
	 * @param ctx the parse tree
	 */
	void enterScreenDescriptionColumnClause(CobolParser.ScreenDescriptionColumnClauseContext ctx);
	/**
	 * Exit a parse tree produced by {@link CobolParser#screenDescriptionColumnClause}.
	 * @param ctx the parse tree
	 */
	void exitScreenDescriptionColumnClause(CobolParser.ScreenDescriptionColumnClauseContext ctx);
	/**
	 * Enter a parse tree produced by {@link CobolParser#screenDescriptionForegroundColorClause}.
	 * @param ctx the parse tree
	 */
	void enterScreenDescriptionForegroundColorClause(CobolParser.ScreenDescriptionForegroundColorClauseContext ctx);
	/**
	 * Exit a parse tree produced by {@link CobolParser#screenDescriptionForegroundColorClause}.
	 * @param ctx the parse tree
	 */
	void exitScreenDescriptionForegroundColorClause(CobolParser.ScreenDescriptionForegroundColorClauseContext ctx);
	/**
	 * Enter a parse tree produced by {@link CobolParser#screenDescriptionBackgroundColorClause}.
	 * @param ctx the parse tree
	 */
	void enterScreenDescriptionBackgroundColorClause(CobolParser.ScreenDescriptionBackgroundColorClauseContext ctx);
	/**
	 * Exit a parse tree produced by {@link CobolParser#screenDescriptionBackgroundColorClause}.
	 * @param ctx the parse tree
	 */
	void exitScreenDescriptionBackgroundColorClause(CobolParser.ScreenDescriptionBackgroundColorClauseContext ctx);
	/**
	 * Enter a parse tree produced by {@link CobolParser#screenDescriptionControlClause}.
	 * @param ctx the parse tree
	 */
	void enterScreenDescriptionControlClause(CobolParser.ScreenDescriptionControlClauseContext ctx);
	/**
	 * Exit a parse tree produced by {@link CobolParser#screenDescriptionControlClause}.
	 * @param ctx the parse tree
	 */
	void exitScreenDescriptionControlClause(CobolParser.ScreenDescriptionControlClauseContext ctx);
	/**
	 * Enter a parse tree produced by {@link CobolParser#screenDescriptionValueClause}.
	 * @param ctx the parse tree
	 */
	void enterScreenDescriptionValueClause(CobolParser.ScreenDescriptionValueClauseContext ctx);
	/**
	 * Exit a parse tree produced by {@link CobolParser#screenDescriptionValueClause}.
	 * @param ctx the parse tree
	 */
	void exitScreenDescriptionValueClause(CobolParser.ScreenDescriptionValueClauseContext ctx);
	/**
	 * Enter a parse tree produced by {@link CobolParser#screenDescriptionPictureClause}.
	 * @param ctx the parse tree
	 */
	void enterScreenDescriptionPictureClause(CobolParser.ScreenDescriptionPictureClauseContext ctx);
	/**
	 * Exit a parse tree produced by {@link CobolParser#screenDescriptionPictureClause}.
	 * @param ctx the parse tree
	 */
	void exitScreenDescriptionPictureClause(CobolParser.ScreenDescriptionPictureClauseContext ctx);
	/**
	 * Enter a parse tree produced by {@link CobolParser#screenDescriptionFromClause}.
	 * @param ctx the parse tree
	 */
	void enterScreenDescriptionFromClause(CobolParser.ScreenDescriptionFromClauseContext ctx);
	/**
	 * Exit a parse tree produced by {@link CobolParser#screenDescriptionFromClause}.
	 * @param ctx the parse tree
	 */
	void exitScreenDescriptionFromClause(CobolParser.ScreenDescriptionFromClauseContext ctx);
	/**
	 * Enter a parse tree produced by {@link CobolParser#screenDescriptionToClause}.
	 * @param ctx the parse tree
	 */
	void enterScreenDescriptionToClause(CobolParser.ScreenDescriptionToClauseContext ctx);
	/**
	 * Exit a parse tree produced by {@link CobolParser#screenDescriptionToClause}.
	 * @param ctx the parse tree
	 */
	void exitScreenDescriptionToClause(CobolParser.ScreenDescriptionToClauseContext ctx);
	/**
	 * Enter a parse tree produced by {@link CobolParser#screenDescriptionUsingClause}.
	 * @param ctx the parse tree
	 */
	void enterScreenDescriptionUsingClause(CobolParser.ScreenDescriptionUsingClauseContext ctx);
	/**
	 * Exit a parse tree produced by {@link CobolParser#screenDescriptionUsingClause}.
	 * @param ctx the parse tree
	 */
	void exitScreenDescriptionUsingClause(CobolParser.ScreenDescriptionUsingClauseContext ctx);
	/**
	 * Enter a parse tree produced by {@link CobolParser#screenDescriptionUsageClause}.
	 * @param ctx the parse tree
	 */
	void enterScreenDescriptionUsageClause(CobolParser.ScreenDescriptionUsageClauseContext ctx);
	/**
	 * Exit a parse tree produced by {@link CobolParser#screenDescriptionUsageClause}.
	 * @param ctx the parse tree
	 */
	void exitScreenDescriptionUsageClause(CobolParser.ScreenDescriptionUsageClauseContext ctx);
	/**
	 * Enter a parse tree produced by {@link CobolParser#screenDescriptionBlankWhenZeroClause}.
	 * @param ctx the parse tree
	 */
	void enterScreenDescriptionBlankWhenZeroClause(CobolParser.ScreenDescriptionBlankWhenZeroClauseContext ctx);
	/**
	 * Exit a parse tree produced by {@link CobolParser#screenDescriptionBlankWhenZeroClause}.
	 * @param ctx the parse tree
	 */
	void exitScreenDescriptionBlankWhenZeroClause(CobolParser.ScreenDescriptionBlankWhenZeroClauseContext ctx);
	/**
	 * Enter a parse tree produced by {@link CobolParser#screenDescriptionJustifiedClause}.
	 * @param ctx the parse tree
	 */
	void enterScreenDescriptionJustifiedClause(CobolParser.ScreenDescriptionJustifiedClauseContext ctx);
	/**
	 * Exit a parse tree produced by {@link CobolParser#screenDescriptionJustifiedClause}.
	 * @param ctx the parse tree
	 */
	void exitScreenDescriptionJustifiedClause(CobolParser.ScreenDescriptionJustifiedClauseContext ctx);
	/**
	 * Enter a parse tree produced by {@link CobolParser#screenDescriptionSignClause}.
	 * @param ctx the parse tree
	 */
	void enterScreenDescriptionSignClause(CobolParser.ScreenDescriptionSignClauseContext ctx);
	/**
	 * Exit a parse tree produced by {@link CobolParser#screenDescriptionSignClause}.
	 * @param ctx the parse tree
	 */
	void exitScreenDescriptionSignClause(CobolParser.ScreenDescriptionSignClauseContext ctx);
	/**
	 * Enter a parse tree produced by {@link CobolParser#screenDescriptionAutoClause}.
	 * @param ctx the parse tree
	 */
	void enterScreenDescriptionAutoClause(CobolParser.ScreenDescriptionAutoClauseContext ctx);
	/**
	 * Exit a parse tree produced by {@link CobolParser#screenDescriptionAutoClause}.
	 * @param ctx the parse tree
	 */
	void exitScreenDescriptionAutoClause(CobolParser.ScreenDescriptionAutoClauseContext ctx);
	/**
	 * Enter a parse tree produced by {@link CobolParser#screenDescriptionSecureClause}.
	 * @param ctx the parse tree
	 */
	void enterScreenDescriptionSecureClause(CobolParser.ScreenDescriptionSecureClauseContext ctx);
	/**
	 * Exit a parse tree produced by {@link CobolParser#screenDescriptionSecureClause}.
	 * @param ctx the parse tree
	 */
	void exitScreenDescriptionSecureClause(CobolParser.ScreenDescriptionSecureClauseContext ctx);
	/**
	 * Enter a parse tree produced by {@link CobolParser#screenDescriptionRequiredClause}.
	 * @param ctx the parse tree
	 */
	void enterScreenDescriptionRequiredClause(CobolParser.ScreenDescriptionRequiredClauseContext ctx);
	/**
	 * Exit a parse tree produced by {@link CobolParser#screenDescriptionRequiredClause}.
	 * @param ctx the parse tree
	 */
	void exitScreenDescriptionRequiredClause(CobolParser.ScreenDescriptionRequiredClauseContext ctx);
	/**
	 * Enter a parse tree produced by {@link CobolParser#screenDescriptionPromptClause}.
	 * @param ctx the parse tree
	 */
	void enterScreenDescriptionPromptClause(CobolParser.ScreenDescriptionPromptClauseContext ctx);
	/**
	 * Exit a parse tree produced by {@link CobolParser#screenDescriptionPromptClause}.
	 * @param ctx the parse tree
	 */
	void exitScreenDescriptionPromptClause(CobolParser.ScreenDescriptionPromptClauseContext ctx);
	/**
	 * Enter a parse tree produced by {@link CobolParser#screenDescriptionPromptOccursClause}.
	 * @param ctx the parse tree
	 */
	void enterScreenDescriptionPromptOccursClause(CobolParser.ScreenDescriptionPromptOccursClauseContext ctx);
	/**
	 * Exit a parse tree produced by {@link CobolParser#screenDescriptionPromptOccursClause}.
	 * @param ctx the parse tree
	 */
	void exitScreenDescriptionPromptOccursClause(CobolParser.ScreenDescriptionPromptOccursClauseContext ctx);
	/**
	 * Enter a parse tree produced by {@link CobolParser#screenDescriptionFullClause}.
	 * @param ctx the parse tree
	 */
	void enterScreenDescriptionFullClause(CobolParser.ScreenDescriptionFullClauseContext ctx);
	/**
	 * Exit a parse tree produced by {@link CobolParser#screenDescriptionFullClause}.
	 * @param ctx the parse tree
	 */
	void exitScreenDescriptionFullClause(CobolParser.ScreenDescriptionFullClauseContext ctx);
	/**
	 * Enter a parse tree produced by {@link CobolParser#screenDescriptionZeroFillClause}.
	 * @param ctx the parse tree
	 */
	void enterScreenDescriptionZeroFillClause(CobolParser.ScreenDescriptionZeroFillClauseContext ctx);
	/**
	 * Exit a parse tree produced by {@link CobolParser#screenDescriptionZeroFillClause}.
	 * @param ctx the parse tree
	 */
	void exitScreenDescriptionZeroFillClause(CobolParser.ScreenDescriptionZeroFillClauseContext ctx);
	/**
	 * Enter a parse tree produced by {@link CobolParser#reportSection}.
	 * @param ctx the parse tree
	 */
	void enterReportSection(CobolParser.ReportSectionContext ctx);
	/**
	 * Exit a parse tree produced by {@link CobolParser#reportSection}.
	 * @param ctx the parse tree
	 */
	void exitReportSection(CobolParser.ReportSectionContext ctx);
	/**
	 * Enter a parse tree produced by {@link CobolParser#reportDescription}.
	 * @param ctx the parse tree
	 */
	void enterReportDescription(CobolParser.ReportDescriptionContext ctx);
	/**
	 * Exit a parse tree produced by {@link CobolParser#reportDescription}.
	 * @param ctx the parse tree
	 */
	void exitReportDescription(CobolParser.ReportDescriptionContext ctx);
	/**
	 * Enter a parse tree produced by {@link CobolParser#reportDescriptionEntry}.
	 * @param ctx the parse tree
	 */
	void enterReportDescriptionEntry(CobolParser.ReportDescriptionEntryContext ctx);
	/**
	 * Exit a parse tree produced by {@link CobolParser#reportDescriptionEntry}.
	 * @param ctx the parse tree
	 */
	void exitReportDescriptionEntry(CobolParser.ReportDescriptionEntryContext ctx);
	/**
	 * Enter a parse tree produced by {@link CobolParser#reportDescriptionGlobalClause}.
	 * @param ctx the parse tree
	 */
	void enterReportDescriptionGlobalClause(CobolParser.ReportDescriptionGlobalClauseContext ctx);
	/**
	 * Exit a parse tree produced by {@link CobolParser#reportDescriptionGlobalClause}.
	 * @param ctx the parse tree
	 */
	void exitReportDescriptionGlobalClause(CobolParser.ReportDescriptionGlobalClauseContext ctx);
	/**
	 * Enter a parse tree produced by {@link CobolParser#reportDescriptionPageLimitClause}.
	 * @param ctx the parse tree
	 */
	void enterReportDescriptionPageLimitClause(CobolParser.ReportDescriptionPageLimitClauseContext ctx);
	/**
	 * Exit a parse tree produced by {@link CobolParser#reportDescriptionPageLimitClause}.
	 * @param ctx the parse tree
	 */
	void exitReportDescriptionPageLimitClause(CobolParser.ReportDescriptionPageLimitClauseContext ctx);
	/**
	 * Enter a parse tree produced by {@link CobolParser#reportDescriptionHeadingClause}.
	 * @param ctx the parse tree
	 */
	void enterReportDescriptionHeadingClause(CobolParser.ReportDescriptionHeadingClauseContext ctx);
	/**
	 * Exit a parse tree produced by {@link CobolParser#reportDescriptionHeadingClause}.
	 * @param ctx the parse tree
	 */
	void exitReportDescriptionHeadingClause(CobolParser.ReportDescriptionHeadingClauseContext ctx);
	/**
	 * Enter a parse tree produced by {@link CobolParser#reportDescriptionFirstDetailClause}.
	 * @param ctx the parse tree
	 */
	void enterReportDescriptionFirstDetailClause(CobolParser.ReportDescriptionFirstDetailClauseContext ctx);
	/**
	 * Exit a parse tree produced by {@link CobolParser#reportDescriptionFirstDetailClause}.
	 * @param ctx the parse tree
	 */
	void exitReportDescriptionFirstDetailClause(CobolParser.ReportDescriptionFirstDetailClauseContext ctx);
	/**
	 * Enter a parse tree produced by {@link CobolParser#reportDescriptionLastDetailClause}.
	 * @param ctx the parse tree
	 */
	void enterReportDescriptionLastDetailClause(CobolParser.ReportDescriptionLastDetailClauseContext ctx);
	/**
	 * Exit a parse tree produced by {@link CobolParser#reportDescriptionLastDetailClause}.
	 * @param ctx the parse tree
	 */
	void exitReportDescriptionLastDetailClause(CobolParser.ReportDescriptionLastDetailClauseContext ctx);
	/**
	 * Enter a parse tree produced by {@link CobolParser#reportDescriptionFootingClause}.
	 * @param ctx the parse tree
	 */
	void enterReportDescriptionFootingClause(CobolParser.ReportDescriptionFootingClauseContext ctx);
	/**
	 * Exit a parse tree produced by {@link CobolParser#reportDescriptionFootingClause}.
	 * @param ctx the parse tree
	 */
	void exitReportDescriptionFootingClause(CobolParser.ReportDescriptionFootingClauseContext ctx);
	/**
	 * Enter a parse tree produced by {@link CobolParser#reportGroupDescriptionEntry}.
	 * @param ctx the parse tree
	 */
	void enterReportGroupDescriptionEntry(CobolParser.ReportGroupDescriptionEntryContext ctx);
	/**
	 * Exit a parse tree produced by {@link CobolParser#reportGroupDescriptionEntry}.
	 * @param ctx the parse tree
	 */
	void exitReportGroupDescriptionEntry(CobolParser.ReportGroupDescriptionEntryContext ctx);
	/**
	 * Enter a parse tree produced by {@link CobolParser#reportGroupDescriptionEntryFormat1}.
	 * @param ctx the parse tree
	 */
	void enterReportGroupDescriptionEntryFormat1(CobolParser.ReportGroupDescriptionEntryFormat1Context ctx);
	/**
	 * Exit a parse tree produced by {@link CobolParser#reportGroupDescriptionEntryFormat1}.
	 * @param ctx the parse tree
	 */
	void exitReportGroupDescriptionEntryFormat1(CobolParser.ReportGroupDescriptionEntryFormat1Context ctx);
	/**
	 * Enter a parse tree produced by {@link CobolParser#reportGroupDescriptionEntryFormat2}.
	 * @param ctx the parse tree
	 */
	void enterReportGroupDescriptionEntryFormat2(CobolParser.ReportGroupDescriptionEntryFormat2Context ctx);
	/**
	 * Exit a parse tree produced by {@link CobolParser#reportGroupDescriptionEntryFormat2}.
	 * @param ctx the parse tree
	 */
	void exitReportGroupDescriptionEntryFormat2(CobolParser.ReportGroupDescriptionEntryFormat2Context ctx);
	/**
	 * Enter a parse tree produced by {@link CobolParser#reportGroupDescriptionEntryFormat3}.
	 * @param ctx the parse tree
	 */
	void enterReportGroupDescriptionEntryFormat3(CobolParser.ReportGroupDescriptionEntryFormat3Context ctx);
	/**
	 * Exit a parse tree produced by {@link CobolParser#reportGroupDescriptionEntryFormat3}.
	 * @param ctx the parse tree
	 */
	void exitReportGroupDescriptionEntryFormat3(CobolParser.ReportGroupDescriptionEntryFormat3Context ctx);
	/**
	 * Enter a parse tree produced by {@link CobolParser#reportGroupBlankWhenZeroClause}.
	 * @param ctx the parse tree
	 */
	void enterReportGroupBlankWhenZeroClause(CobolParser.ReportGroupBlankWhenZeroClauseContext ctx);
	/**
	 * Exit a parse tree produced by {@link CobolParser#reportGroupBlankWhenZeroClause}.
	 * @param ctx the parse tree
	 */
	void exitReportGroupBlankWhenZeroClause(CobolParser.ReportGroupBlankWhenZeroClauseContext ctx);
	/**
	 * Enter a parse tree produced by {@link CobolParser#reportGroupColumnNumberClause}.
	 * @param ctx the parse tree
	 */
	void enterReportGroupColumnNumberClause(CobolParser.ReportGroupColumnNumberClauseContext ctx);
	/**
	 * Exit a parse tree produced by {@link CobolParser#reportGroupColumnNumberClause}.
	 * @param ctx the parse tree
	 */
	void exitReportGroupColumnNumberClause(CobolParser.ReportGroupColumnNumberClauseContext ctx);
	/**
	 * Enter a parse tree produced by {@link CobolParser#reportGroupIndicateClause}.
	 * @param ctx the parse tree
	 */
	void enterReportGroupIndicateClause(CobolParser.ReportGroupIndicateClauseContext ctx);
	/**
	 * Exit a parse tree produced by {@link CobolParser#reportGroupIndicateClause}.
	 * @param ctx the parse tree
	 */
	void exitReportGroupIndicateClause(CobolParser.ReportGroupIndicateClauseContext ctx);
	/**
	 * Enter a parse tree produced by {@link CobolParser#reportGroupJustifiedClause}.
	 * @param ctx the parse tree
	 */
	void enterReportGroupJustifiedClause(CobolParser.ReportGroupJustifiedClauseContext ctx);
	/**
	 * Exit a parse tree produced by {@link CobolParser#reportGroupJustifiedClause}.
	 * @param ctx the parse tree
	 */
	void exitReportGroupJustifiedClause(CobolParser.ReportGroupJustifiedClauseContext ctx);
	/**
	 * Enter a parse tree produced by {@link CobolParser#reportGroupLineNumberClause}.
	 * @param ctx the parse tree
	 */
	void enterReportGroupLineNumberClause(CobolParser.ReportGroupLineNumberClauseContext ctx);
	/**
	 * Exit a parse tree produced by {@link CobolParser#reportGroupLineNumberClause}.
	 * @param ctx the parse tree
	 */
	void exitReportGroupLineNumberClause(CobolParser.ReportGroupLineNumberClauseContext ctx);
	/**
	 * Enter a parse tree produced by {@link CobolParser#reportGroupLineNumberNextPage}.
	 * @param ctx the parse tree
	 */
	void enterReportGroupLineNumberNextPage(CobolParser.ReportGroupLineNumberNextPageContext ctx);
	/**
	 * Exit a parse tree produced by {@link CobolParser#reportGroupLineNumberNextPage}.
	 * @param ctx the parse tree
	 */
	void exitReportGroupLineNumberNextPage(CobolParser.ReportGroupLineNumberNextPageContext ctx);
	/**
	 * Enter a parse tree produced by {@link CobolParser#reportGroupLineNumberPlus}.
	 * @param ctx the parse tree
	 */
	void enterReportGroupLineNumberPlus(CobolParser.ReportGroupLineNumberPlusContext ctx);
	/**
	 * Exit a parse tree produced by {@link CobolParser#reportGroupLineNumberPlus}.
	 * @param ctx the parse tree
	 */
	void exitReportGroupLineNumberPlus(CobolParser.ReportGroupLineNumberPlusContext ctx);
	/**
	 * Enter a parse tree produced by {@link CobolParser#reportGroupNextGroupClause}.
	 * @param ctx the parse tree
	 */
	void enterReportGroupNextGroupClause(CobolParser.ReportGroupNextGroupClauseContext ctx);
	/**
	 * Exit a parse tree produced by {@link CobolParser#reportGroupNextGroupClause}.
	 * @param ctx the parse tree
	 */
	void exitReportGroupNextGroupClause(CobolParser.ReportGroupNextGroupClauseContext ctx);
	/**
	 * Enter a parse tree produced by {@link CobolParser#reportGroupNextGroupPlus}.
	 * @param ctx the parse tree
	 */
	void enterReportGroupNextGroupPlus(CobolParser.ReportGroupNextGroupPlusContext ctx);
	/**
	 * Exit a parse tree produced by {@link CobolParser#reportGroupNextGroupPlus}.
	 * @param ctx the parse tree
	 */
	void exitReportGroupNextGroupPlus(CobolParser.ReportGroupNextGroupPlusContext ctx);
	/**
	 * Enter a parse tree produced by {@link CobolParser#reportGroupNextGroupNextPage}.
	 * @param ctx the parse tree
	 */
	void enterReportGroupNextGroupNextPage(CobolParser.ReportGroupNextGroupNextPageContext ctx);
	/**
	 * Exit a parse tree produced by {@link CobolParser#reportGroupNextGroupNextPage}.
	 * @param ctx the parse tree
	 */
	void exitReportGroupNextGroupNextPage(CobolParser.ReportGroupNextGroupNextPageContext ctx);
	/**
	 * Enter a parse tree produced by {@link CobolParser#reportGroupPictureClause}.
	 * @param ctx the parse tree
	 */
	void enterReportGroupPictureClause(CobolParser.ReportGroupPictureClauseContext ctx);
	/**
	 * Exit a parse tree produced by {@link CobolParser#reportGroupPictureClause}.
	 * @param ctx the parse tree
	 */
	void exitReportGroupPictureClause(CobolParser.ReportGroupPictureClauseContext ctx);
	/**
	 * Enter a parse tree produced by {@link CobolParser#reportGroupResetClause}.
	 * @param ctx the parse tree
	 */
	void enterReportGroupResetClause(CobolParser.ReportGroupResetClauseContext ctx);
	/**
	 * Exit a parse tree produced by {@link CobolParser#reportGroupResetClause}.
	 * @param ctx the parse tree
	 */
	void exitReportGroupResetClause(CobolParser.ReportGroupResetClauseContext ctx);
	/**
	 * Enter a parse tree produced by {@link CobolParser#reportGroupSignClause}.
	 * @param ctx the parse tree
	 */
	void enterReportGroupSignClause(CobolParser.ReportGroupSignClauseContext ctx);
	/**
	 * Exit a parse tree produced by {@link CobolParser#reportGroupSignClause}.
	 * @param ctx the parse tree
	 */
	void exitReportGroupSignClause(CobolParser.ReportGroupSignClauseContext ctx);
	/**
	 * Enter a parse tree produced by {@link CobolParser#reportGroupSourceClause}.
	 * @param ctx the parse tree
	 */
	void enterReportGroupSourceClause(CobolParser.ReportGroupSourceClauseContext ctx);
	/**
	 * Exit a parse tree produced by {@link CobolParser#reportGroupSourceClause}.
	 * @param ctx the parse tree
	 */
	void exitReportGroupSourceClause(CobolParser.ReportGroupSourceClauseContext ctx);
	/**
	 * Enter a parse tree produced by {@link CobolParser#reportGroupSumClause}.
	 * @param ctx the parse tree
	 */
	void enterReportGroupSumClause(CobolParser.ReportGroupSumClauseContext ctx);
	/**
	 * Exit a parse tree produced by {@link CobolParser#reportGroupSumClause}.
	 * @param ctx the parse tree
	 */
	void exitReportGroupSumClause(CobolParser.ReportGroupSumClauseContext ctx);
	/**
	 * Enter a parse tree produced by {@link CobolParser#reportGroupTypeClause}.
	 * @param ctx the parse tree
	 */
	void enterReportGroupTypeClause(CobolParser.ReportGroupTypeClauseContext ctx);
	/**
	 * Exit a parse tree produced by {@link CobolParser#reportGroupTypeClause}.
	 * @param ctx the parse tree
	 */
	void exitReportGroupTypeClause(CobolParser.ReportGroupTypeClauseContext ctx);
	/**
	 * Enter a parse tree produced by {@link CobolParser#reportGroupTypeReportHeading}.
	 * @param ctx the parse tree
	 */
	void enterReportGroupTypeReportHeading(CobolParser.ReportGroupTypeReportHeadingContext ctx);
	/**
	 * Exit a parse tree produced by {@link CobolParser#reportGroupTypeReportHeading}.
	 * @param ctx the parse tree
	 */
	void exitReportGroupTypeReportHeading(CobolParser.ReportGroupTypeReportHeadingContext ctx);
	/**
	 * Enter a parse tree produced by {@link CobolParser#reportGroupTypePageHeading}.
	 * @param ctx the parse tree
	 */
	void enterReportGroupTypePageHeading(CobolParser.ReportGroupTypePageHeadingContext ctx);
	/**
	 * Exit a parse tree produced by {@link CobolParser#reportGroupTypePageHeading}.
	 * @param ctx the parse tree
	 */
	void exitReportGroupTypePageHeading(CobolParser.ReportGroupTypePageHeadingContext ctx);
	/**
	 * Enter a parse tree produced by {@link CobolParser#reportGroupTypeControlHeading}.
	 * @param ctx the parse tree
	 */
	void enterReportGroupTypeControlHeading(CobolParser.ReportGroupTypeControlHeadingContext ctx);
	/**
	 * Exit a parse tree produced by {@link CobolParser#reportGroupTypeControlHeading}.
	 * @param ctx the parse tree
	 */
	void exitReportGroupTypeControlHeading(CobolParser.ReportGroupTypeControlHeadingContext ctx);
	/**
	 * Enter a parse tree produced by {@link CobolParser#reportGroupTypeDetail}.
	 * @param ctx the parse tree
	 */
	void enterReportGroupTypeDetail(CobolParser.ReportGroupTypeDetailContext ctx);
	/**
	 * Exit a parse tree produced by {@link CobolParser#reportGroupTypeDetail}.
	 * @param ctx the parse tree
	 */
	void exitReportGroupTypeDetail(CobolParser.ReportGroupTypeDetailContext ctx);
	/**
	 * Enter a parse tree produced by {@link CobolParser#reportGroupTypeControlFooting}.
	 * @param ctx the parse tree
	 */
	void enterReportGroupTypeControlFooting(CobolParser.ReportGroupTypeControlFootingContext ctx);
	/**
	 * Exit a parse tree produced by {@link CobolParser#reportGroupTypeControlFooting}.
	 * @param ctx the parse tree
	 */
	void exitReportGroupTypeControlFooting(CobolParser.ReportGroupTypeControlFootingContext ctx);
	/**
	 * Enter a parse tree produced by {@link CobolParser#reportGroupUsageClause}.
	 * @param ctx the parse tree
	 */
	void enterReportGroupUsageClause(CobolParser.ReportGroupUsageClauseContext ctx);
	/**
	 * Exit a parse tree produced by {@link CobolParser#reportGroupUsageClause}.
	 * @param ctx the parse tree
	 */
	void exitReportGroupUsageClause(CobolParser.ReportGroupUsageClauseContext ctx);
	/**
	 * Enter a parse tree produced by {@link CobolParser#reportGroupTypePageFooting}.
	 * @param ctx the parse tree
	 */
	void enterReportGroupTypePageFooting(CobolParser.ReportGroupTypePageFootingContext ctx);
	/**
	 * Exit a parse tree produced by {@link CobolParser#reportGroupTypePageFooting}.
	 * @param ctx the parse tree
	 */
	void exitReportGroupTypePageFooting(CobolParser.ReportGroupTypePageFootingContext ctx);
	/**
	 * Enter a parse tree produced by {@link CobolParser#reportGroupTypeReportFooting}.
	 * @param ctx the parse tree
	 */
	void enterReportGroupTypeReportFooting(CobolParser.ReportGroupTypeReportFootingContext ctx);
	/**
	 * Exit a parse tree produced by {@link CobolParser#reportGroupTypeReportFooting}.
	 * @param ctx the parse tree
	 */
	void exitReportGroupTypeReportFooting(CobolParser.ReportGroupTypeReportFootingContext ctx);
	/**
	 * Enter a parse tree produced by {@link CobolParser#reportGroupValueClause}.
	 * @param ctx the parse tree
	 */
	void enterReportGroupValueClause(CobolParser.ReportGroupValueClauseContext ctx);
	/**
	 * Exit a parse tree produced by {@link CobolParser#reportGroupValueClause}.
	 * @param ctx the parse tree
	 */
	void exitReportGroupValueClause(CobolParser.ReportGroupValueClauseContext ctx);
	/**
	 * Enter a parse tree produced by {@link CobolParser#programLibrarySection}.
	 * @param ctx the parse tree
	 */
	void enterProgramLibrarySection(CobolParser.ProgramLibrarySectionContext ctx);
	/**
	 * Exit a parse tree produced by {@link CobolParser#programLibrarySection}.
	 * @param ctx the parse tree
	 */
	void exitProgramLibrarySection(CobolParser.ProgramLibrarySectionContext ctx);
	/**
	 * Enter a parse tree produced by {@link CobolParser#libraryDescriptionEntry}.
	 * @param ctx the parse tree
	 */
	void enterLibraryDescriptionEntry(CobolParser.LibraryDescriptionEntryContext ctx);
	/**
	 * Exit a parse tree produced by {@link CobolParser#libraryDescriptionEntry}.
	 * @param ctx the parse tree
	 */
	void exitLibraryDescriptionEntry(CobolParser.LibraryDescriptionEntryContext ctx);
	/**
	 * Enter a parse tree produced by {@link CobolParser#libraryDescriptionEntryFormat1}.
	 * @param ctx the parse tree
	 */
	void enterLibraryDescriptionEntryFormat1(CobolParser.LibraryDescriptionEntryFormat1Context ctx);
	/**
	 * Exit a parse tree produced by {@link CobolParser#libraryDescriptionEntryFormat1}.
	 * @param ctx the parse tree
	 */
	void exitLibraryDescriptionEntryFormat1(CobolParser.LibraryDescriptionEntryFormat1Context ctx);
	/**
	 * Enter a parse tree produced by {@link CobolParser#libraryDescriptionEntryFormat2}.
	 * @param ctx the parse tree
	 */
	void enterLibraryDescriptionEntryFormat2(CobolParser.LibraryDescriptionEntryFormat2Context ctx);
	/**
	 * Exit a parse tree produced by {@link CobolParser#libraryDescriptionEntryFormat2}.
	 * @param ctx the parse tree
	 */
	void exitLibraryDescriptionEntryFormat2(CobolParser.LibraryDescriptionEntryFormat2Context ctx);
	/**
	 * Enter a parse tree produced by {@link CobolParser#libraryAttributeClauseFormat1}.
	 * @param ctx the parse tree
	 */
	void enterLibraryAttributeClauseFormat1(CobolParser.LibraryAttributeClauseFormat1Context ctx);
	/**
	 * Exit a parse tree produced by {@link CobolParser#libraryAttributeClauseFormat1}.
	 * @param ctx the parse tree
	 */
	void exitLibraryAttributeClauseFormat1(CobolParser.LibraryAttributeClauseFormat1Context ctx);
	/**
	 * Enter a parse tree produced by {@link CobolParser#libraryAttributeClauseFormat2}.
	 * @param ctx the parse tree
	 */
	void enterLibraryAttributeClauseFormat2(CobolParser.LibraryAttributeClauseFormat2Context ctx);
	/**
	 * Exit a parse tree produced by {@link CobolParser#libraryAttributeClauseFormat2}.
	 * @param ctx the parse tree
	 */
	void exitLibraryAttributeClauseFormat2(CobolParser.LibraryAttributeClauseFormat2Context ctx);
	/**
	 * Enter a parse tree produced by {@link CobolParser#libraryAttributeFunction}.
	 * @param ctx the parse tree
	 */
	void enterLibraryAttributeFunction(CobolParser.LibraryAttributeFunctionContext ctx);
	/**
	 * Exit a parse tree produced by {@link CobolParser#libraryAttributeFunction}.
	 * @param ctx the parse tree
	 */
	void exitLibraryAttributeFunction(CobolParser.LibraryAttributeFunctionContext ctx);
	/**
	 * Enter a parse tree produced by {@link CobolParser#libraryAttributeParameter}.
	 * @param ctx the parse tree
	 */
	void enterLibraryAttributeParameter(CobolParser.LibraryAttributeParameterContext ctx);
	/**
	 * Exit a parse tree produced by {@link CobolParser#libraryAttributeParameter}.
	 * @param ctx the parse tree
	 */
	void exitLibraryAttributeParameter(CobolParser.LibraryAttributeParameterContext ctx);
	/**
	 * Enter a parse tree produced by {@link CobolParser#libraryAttributeTitle}.
	 * @param ctx the parse tree
	 */
	void enterLibraryAttributeTitle(CobolParser.LibraryAttributeTitleContext ctx);
	/**
	 * Exit a parse tree produced by {@link CobolParser#libraryAttributeTitle}.
	 * @param ctx the parse tree
	 */
	void exitLibraryAttributeTitle(CobolParser.LibraryAttributeTitleContext ctx);
	/**
	 * Enter a parse tree produced by {@link CobolParser#libraryEntryProcedureClauseFormat1}.
	 * @param ctx the parse tree
	 */
	void enterLibraryEntryProcedureClauseFormat1(CobolParser.LibraryEntryProcedureClauseFormat1Context ctx);
	/**
	 * Exit a parse tree produced by {@link CobolParser#libraryEntryProcedureClauseFormat1}.
	 * @param ctx the parse tree
	 */
	void exitLibraryEntryProcedureClauseFormat1(CobolParser.LibraryEntryProcedureClauseFormat1Context ctx);
	/**
	 * Enter a parse tree produced by {@link CobolParser#libraryEntryProcedureClauseFormat2}.
	 * @param ctx the parse tree
	 */
	void enterLibraryEntryProcedureClauseFormat2(CobolParser.LibraryEntryProcedureClauseFormat2Context ctx);
	/**
	 * Exit a parse tree produced by {@link CobolParser#libraryEntryProcedureClauseFormat2}.
	 * @param ctx the parse tree
	 */
	void exitLibraryEntryProcedureClauseFormat2(CobolParser.LibraryEntryProcedureClauseFormat2Context ctx);
	/**
	 * Enter a parse tree produced by {@link CobolParser#libraryEntryProcedureForClause}.
	 * @param ctx the parse tree
	 */
	void enterLibraryEntryProcedureForClause(CobolParser.LibraryEntryProcedureForClauseContext ctx);
	/**
	 * Exit a parse tree produced by {@link CobolParser#libraryEntryProcedureForClause}.
	 * @param ctx the parse tree
	 */
	void exitLibraryEntryProcedureForClause(CobolParser.LibraryEntryProcedureForClauseContext ctx);
	/**
	 * Enter a parse tree produced by {@link CobolParser#libraryEntryProcedureGivingClause}.
	 * @param ctx the parse tree
	 */
	void enterLibraryEntryProcedureGivingClause(CobolParser.LibraryEntryProcedureGivingClauseContext ctx);
	/**
	 * Exit a parse tree produced by {@link CobolParser#libraryEntryProcedureGivingClause}.
	 * @param ctx the parse tree
	 */
	void exitLibraryEntryProcedureGivingClause(CobolParser.LibraryEntryProcedureGivingClauseContext ctx);
	/**
	 * Enter a parse tree produced by {@link CobolParser#libraryEntryProcedureUsingClause}.
	 * @param ctx the parse tree
	 */
	void enterLibraryEntryProcedureUsingClause(CobolParser.LibraryEntryProcedureUsingClauseContext ctx);
	/**
	 * Exit a parse tree produced by {@link CobolParser#libraryEntryProcedureUsingClause}.
	 * @param ctx the parse tree
	 */
	void exitLibraryEntryProcedureUsingClause(CobolParser.LibraryEntryProcedureUsingClauseContext ctx);
	/**
	 * Enter a parse tree produced by {@link CobolParser#libraryEntryProcedureUsingName}.
	 * @param ctx the parse tree
	 */
	void enterLibraryEntryProcedureUsingName(CobolParser.LibraryEntryProcedureUsingNameContext ctx);
	/**
	 * Exit a parse tree produced by {@link CobolParser#libraryEntryProcedureUsingName}.
	 * @param ctx the parse tree
	 */
	void exitLibraryEntryProcedureUsingName(CobolParser.LibraryEntryProcedureUsingNameContext ctx);
	/**
	 * Enter a parse tree produced by {@link CobolParser#libraryEntryProcedureWithClause}.
	 * @param ctx the parse tree
	 */
	void enterLibraryEntryProcedureWithClause(CobolParser.LibraryEntryProcedureWithClauseContext ctx);
	/**
	 * Exit a parse tree produced by {@link CobolParser#libraryEntryProcedureWithClause}.
	 * @param ctx the parse tree
	 */
	void exitLibraryEntryProcedureWithClause(CobolParser.LibraryEntryProcedureWithClauseContext ctx);
	/**
	 * Enter a parse tree produced by {@link CobolParser#libraryEntryProcedureWithName}.
	 * @param ctx the parse tree
	 */
	void enterLibraryEntryProcedureWithName(CobolParser.LibraryEntryProcedureWithNameContext ctx);
	/**
	 * Exit a parse tree produced by {@link CobolParser#libraryEntryProcedureWithName}.
	 * @param ctx the parse tree
	 */
	void exitLibraryEntryProcedureWithName(CobolParser.LibraryEntryProcedureWithNameContext ctx);
	/**
	 * Enter a parse tree produced by {@link CobolParser#libraryIsCommonClause}.
	 * @param ctx the parse tree
	 */
	void enterLibraryIsCommonClause(CobolParser.LibraryIsCommonClauseContext ctx);
	/**
	 * Exit a parse tree produced by {@link CobolParser#libraryIsCommonClause}.
	 * @param ctx the parse tree
	 */
	void exitLibraryIsCommonClause(CobolParser.LibraryIsCommonClauseContext ctx);
	/**
	 * Enter a parse tree produced by {@link CobolParser#libraryIsGlobalClause}.
	 * @param ctx the parse tree
	 */
	void enterLibraryIsGlobalClause(CobolParser.LibraryIsGlobalClauseContext ctx);
	/**
	 * Exit a parse tree produced by {@link CobolParser#libraryIsGlobalClause}.
	 * @param ctx the parse tree
	 */
	void exitLibraryIsGlobalClause(CobolParser.LibraryIsGlobalClauseContext ctx);
	/**
	 * Enter a parse tree produced by {@link CobolParser#dataDescriptionEntry}.
	 * @param ctx the parse tree
	 */
	void enterDataDescriptionEntry(CobolParser.DataDescriptionEntryContext ctx);
	/**
	 * Exit a parse tree produced by {@link CobolParser#dataDescriptionEntry}.
	 * @param ctx the parse tree
	 */
	void exitDataDescriptionEntry(CobolParser.DataDescriptionEntryContext ctx);
	/**
	 * Enter a parse tree produced by {@link CobolParser#dataDescriptionEntryFormat1}.
	 * @param ctx the parse tree
	 */
	void enterDataDescriptionEntryFormat1(CobolParser.DataDescriptionEntryFormat1Context ctx);
	/**
	 * Exit a parse tree produced by {@link CobolParser#dataDescriptionEntryFormat1}.
	 * @param ctx the parse tree
	 */
	void exitDataDescriptionEntryFormat1(CobolParser.DataDescriptionEntryFormat1Context ctx);
	/**
	 * Enter a parse tree produced by {@link CobolParser#dataDescriptionEntryFormat1Clause}.
	 * @param ctx the parse tree
	 */
	void enterDataDescriptionEntryFormat1Clause(CobolParser.DataDescriptionEntryFormat1ClauseContext ctx);
	/**
	 * Exit a parse tree produced by {@link CobolParser#dataDescriptionEntryFormat1Clause}.
	 * @param ctx the parse tree
	 */
	void exitDataDescriptionEntryFormat1Clause(CobolParser.DataDescriptionEntryFormat1ClauseContext ctx);
	/**
	 * Enter a parse tree produced by {@link CobolParser#dataDescriptionEntryFormat2}.
	 * @param ctx the parse tree
	 */
	void enterDataDescriptionEntryFormat2(CobolParser.DataDescriptionEntryFormat2Context ctx);
	/**
	 * Exit a parse tree produced by {@link CobolParser#dataDescriptionEntryFormat2}.
	 * @param ctx the parse tree
	 */
	void exitDataDescriptionEntryFormat2(CobolParser.DataDescriptionEntryFormat2Context ctx);
	/**
	 * Enter a parse tree produced by {@link CobolParser#dataDescriptionEntryFormat3}.
	 * @param ctx the parse tree
	 */
	void enterDataDescriptionEntryFormat3(CobolParser.DataDescriptionEntryFormat3Context ctx);
	/**
	 * Exit a parse tree produced by {@link CobolParser#dataDescriptionEntryFormat3}.
	 * @param ctx the parse tree
	 */
	void exitDataDescriptionEntryFormat3(CobolParser.DataDescriptionEntryFormat3Context ctx);
	/**
	 * Enter a parse tree produced by {@link CobolParser#dataDescriptionEntryExecSql}.
	 * @param ctx the parse tree
	 */
	void enterDataDescriptionEntryExecSql(CobolParser.DataDescriptionEntryExecSqlContext ctx);
	/**
	 * Exit a parse tree produced by {@link CobolParser#dataDescriptionEntryExecSql}.
	 * @param ctx the parse tree
	 */
	void exitDataDescriptionEntryExecSql(CobolParser.DataDescriptionEntryExecSqlContext ctx);
	/**
	 * Enter a parse tree produced by {@link CobolParser#dataAlignedClause}.
	 * @param ctx the parse tree
	 */
	void enterDataAlignedClause(CobolParser.DataAlignedClauseContext ctx);
	/**
	 * Exit a parse tree produced by {@link CobolParser#dataAlignedClause}.
	 * @param ctx the parse tree
	 */
	void exitDataAlignedClause(CobolParser.DataAlignedClauseContext ctx);
	/**
	 * Enter a parse tree produced by {@link CobolParser#dataBlankWhenZeroClause}.
	 * @param ctx the parse tree
	 */
	void enterDataBlankWhenZeroClause(CobolParser.DataBlankWhenZeroClauseContext ctx);
	/**
	 * Exit a parse tree produced by {@link CobolParser#dataBlankWhenZeroClause}.
	 * @param ctx the parse tree
	 */
	void exitDataBlankWhenZeroClause(CobolParser.DataBlankWhenZeroClauseContext ctx);
	/**
	 * Enter a parse tree produced by {@link CobolParser#dataCommonOwnLocalClause}.
	 * @param ctx the parse tree
	 */
	void enterDataCommonOwnLocalClause(CobolParser.DataCommonOwnLocalClauseContext ctx);
	/**
	 * Exit a parse tree produced by {@link CobolParser#dataCommonOwnLocalClause}.
	 * @param ctx the parse tree
	 */
	void exitDataCommonOwnLocalClause(CobolParser.DataCommonOwnLocalClauseContext ctx);
	/**
	 * Enter a parse tree produced by {@link CobolParser#dataExternalClause}.
	 * @param ctx the parse tree
	 */
	void enterDataExternalClause(CobolParser.DataExternalClauseContext ctx);
	/**
	 * Exit a parse tree produced by {@link CobolParser#dataExternalClause}.
	 * @param ctx the parse tree
	 */
	void exitDataExternalClause(CobolParser.DataExternalClauseContext ctx);
	/**
	 * Enter a parse tree produced by {@link CobolParser#dataGlobalClause}.
	 * @param ctx the parse tree
	 */
	void enterDataGlobalClause(CobolParser.DataGlobalClauseContext ctx);
	/**
	 * Exit a parse tree produced by {@link CobolParser#dataGlobalClause}.
	 * @param ctx the parse tree
	 */
	void exitDataGlobalClause(CobolParser.DataGlobalClauseContext ctx);
	/**
	 * Enter a parse tree produced by {@link CobolParser#dataIntegerStringClause}.
	 * @param ctx the parse tree
	 */
	void enterDataIntegerStringClause(CobolParser.DataIntegerStringClauseContext ctx);
	/**
	 * Exit a parse tree produced by {@link CobolParser#dataIntegerStringClause}.
	 * @param ctx the parse tree
	 */
	void exitDataIntegerStringClause(CobolParser.DataIntegerStringClauseContext ctx);
	/**
	 * Enter a parse tree produced by {@link CobolParser#dataJustifiedClause}.
	 * @param ctx the parse tree
	 */
	void enterDataJustifiedClause(CobolParser.DataJustifiedClauseContext ctx);
	/**
	 * Exit a parse tree produced by {@link CobolParser#dataJustifiedClause}.
	 * @param ctx the parse tree
	 */
	void exitDataJustifiedClause(CobolParser.DataJustifiedClauseContext ctx);
	/**
	 * Enter a parse tree produced by {@link CobolParser#dataOccursClause}.
	 * @param ctx the parse tree
	 */
	void enterDataOccursClause(CobolParser.DataOccursClauseContext ctx);
	/**
	 * Exit a parse tree produced by {@link CobolParser#dataOccursClause}.
	 * @param ctx the parse tree
	 */
	void exitDataOccursClause(CobolParser.DataOccursClauseContext ctx);
	/**
	 * Enter a parse tree produced by {@link CobolParser#dataOccursTo}.
	 * @param ctx the parse tree
	 */
	void enterDataOccursTo(CobolParser.DataOccursToContext ctx);
	/**
	 * Exit a parse tree produced by {@link CobolParser#dataOccursTo}.
	 * @param ctx the parse tree
	 */
	void exitDataOccursTo(CobolParser.DataOccursToContext ctx);
	/**
	 * Enter a parse tree produced by {@link CobolParser#dataOccursDepending}.
	 * @param ctx the parse tree
	 */
	void enterDataOccursDepending(CobolParser.DataOccursDependingContext ctx);
	/**
	 * Exit a parse tree produced by {@link CobolParser#dataOccursDepending}.
	 * @param ctx the parse tree
	 */
	void exitDataOccursDepending(CobolParser.DataOccursDependingContext ctx);
	/**
	 * Enter a parse tree produced by {@link CobolParser#dataOccursSort}.
	 * @param ctx the parse tree
	 */
	void enterDataOccursSort(CobolParser.DataOccursSortContext ctx);
	/**
	 * Exit a parse tree produced by {@link CobolParser#dataOccursSort}.
	 * @param ctx the parse tree
	 */
	void exitDataOccursSort(CobolParser.DataOccursSortContext ctx);
	/**
	 * Enter a parse tree produced by {@link CobolParser#dataOccursIndexed}.
	 * @param ctx the parse tree
	 */
	void enterDataOccursIndexed(CobolParser.DataOccursIndexedContext ctx);
	/**
	 * Exit a parse tree produced by {@link CobolParser#dataOccursIndexed}.
	 * @param ctx the parse tree
	 */
	void exitDataOccursIndexed(CobolParser.DataOccursIndexedContext ctx);
	/**
	 * Enter a parse tree produced by {@link CobolParser#dataPictureClause}.
	 * @param ctx the parse tree
	 */
	void enterDataPictureClause(CobolParser.DataPictureClauseContext ctx);
	/**
	 * Exit a parse tree produced by {@link CobolParser#dataPictureClause}.
	 * @param ctx the parse tree
	 */
	void exitDataPictureClause(CobolParser.DataPictureClauseContext ctx);
	/**
	 * Enter a parse tree produced by {@link CobolParser#pictureString}.
	 * @param ctx the parse tree
	 */
	void enterPictureString(CobolParser.PictureStringContext ctx);
	/**
	 * Exit a parse tree produced by {@link CobolParser#pictureString}.
	 * @param ctx the parse tree
	 */
	void exitPictureString(CobolParser.PictureStringContext ctx);
	/**
	 * Enter a parse tree produced by {@link CobolParser#picture}.
	 * @param ctx the parse tree
	 */
	void enterPicture(CobolParser.PictureContext ctx);
	/**
	 * Exit a parse tree produced by {@link CobolParser#picture}.
	 * @param ctx the parse tree
	 */
	void exitPicture(CobolParser.PictureContext ctx);
	/**
	 * Enter a parse tree produced by {@link CobolParser#pictureChars}.
	 * @param ctx the parse tree
	 */
	void enterPictureChars(CobolParser.PictureCharsContext ctx);
	/**
	 * Exit a parse tree produced by {@link CobolParser#pictureChars}.
	 * @param ctx the parse tree
	 */
	void exitPictureChars(CobolParser.PictureCharsContext ctx);
	/**
	 * Enter a parse tree produced by {@link CobolParser#pictureCardinality}.
	 * @param ctx the parse tree
	 */
	void enterPictureCardinality(CobolParser.PictureCardinalityContext ctx);
	/**
	 * Exit a parse tree produced by {@link CobolParser#pictureCardinality}.
	 * @param ctx the parse tree
	 */
	void exitPictureCardinality(CobolParser.PictureCardinalityContext ctx);
	/**
	 * Enter a parse tree produced by {@link CobolParser#dataReceivedByClause}.
	 * @param ctx the parse tree
	 */
	void enterDataReceivedByClause(CobolParser.DataReceivedByClauseContext ctx);
	/**
	 * Exit a parse tree produced by {@link CobolParser#dataReceivedByClause}.
	 * @param ctx the parse tree
	 */
	void exitDataReceivedByClause(CobolParser.DataReceivedByClauseContext ctx);
	/**
	 * Enter a parse tree produced by {@link CobolParser#dataRecordAreaClause}.
	 * @param ctx the parse tree
	 */
	void enterDataRecordAreaClause(CobolParser.DataRecordAreaClauseContext ctx);
	/**
	 * Exit a parse tree produced by {@link CobolParser#dataRecordAreaClause}.
	 * @param ctx the parse tree
	 */
	void exitDataRecordAreaClause(CobolParser.DataRecordAreaClauseContext ctx);
	/**
	 * Enter a parse tree produced by {@link CobolParser#dataRedefinesClause}.
	 * @param ctx the parse tree
	 */
	void enterDataRedefinesClause(CobolParser.DataRedefinesClauseContext ctx);
	/**
	 * Exit a parse tree produced by {@link CobolParser#dataRedefinesClause}.
	 * @param ctx the parse tree
	 */
	void exitDataRedefinesClause(CobolParser.DataRedefinesClauseContext ctx);
	/**
	 * Enter a parse tree produced by {@link CobolParser#dataRenamesClause}.
	 * @param ctx the parse tree
	 */
	void enterDataRenamesClause(CobolParser.DataRenamesClauseContext ctx);
	/**
	 * Exit a parse tree produced by {@link CobolParser#dataRenamesClause}.
	 * @param ctx the parse tree
	 */
	void exitDataRenamesClause(CobolParser.DataRenamesClauseContext ctx);
	/**
	 * Enter a parse tree produced by {@link CobolParser#dataSignClause}.
	 * @param ctx the parse tree
	 */
	void enterDataSignClause(CobolParser.DataSignClauseContext ctx);
	/**
	 * Exit a parse tree produced by {@link CobolParser#dataSignClause}.
	 * @param ctx the parse tree
	 */
	void exitDataSignClause(CobolParser.DataSignClauseContext ctx);
	/**
	 * Enter a parse tree produced by {@link CobolParser#dataSynchronizedClause}.
	 * @param ctx the parse tree
	 */
	void enterDataSynchronizedClause(CobolParser.DataSynchronizedClauseContext ctx);
	/**
	 * Exit a parse tree produced by {@link CobolParser#dataSynchronizedClause}.
	 * @param ctx the parse tree
	 */
	void exitDataSynchronizedClause(CobolParser.DataSynchronizedClauseContext ctx);
	/**
	 * Enter a parse tree produced by {@link CobolParser#dataThreadLocalClause}.
	 * @param ctx the parse tree
	 */
	void enterDataThreadLocalClause(CobolParser.DataThreadLocalClauseContext ctx);
	/**
	 * Exit a parse tree produced by {@link CobolParser#dataThreadLocalClause}.
	 * @param ctx the parse tree
	 */
	void exitDataThreadLocalClause(CobolParser.DataThreadLocalClauseContext ctx);
	/**
	 * Enter a parse tree produced by {@link CobolParser#dataTypeClause}.
	 * @param ctx the parse tree
	 */
	void enterDataTypeClause(CobolParser.DataTypeClauseContext ctx);
	/**
	 * Exit a parse tree produced by {@link CobolParser#dataTypeClause}.
	 * @param ctx the parse tree
	 */
	void exitDataTypeClause(CobolParser.DataTypeClauseContext ctx);
	/**
	 * Enter a parse tree produced by {@link CobolParser#dataTypeDefClause}.
	 * @param ctx the parse tree
	 */
	void enterDataTypeDefClause(CobolParser.DataTypeDefClauseContext ctx);
	/**
	 * Exit a parse tree produced by {@link CobolParser#dataTypeDefClause}.
	 * @param ctx the parse tree
	 */
	void exitDataTypeDefClause(CobolParser.DataTypeDefClauseContext ctx);
	/**
	 * Enter a parse tree produced by {@link CobolParser#dataUsageClause}.
	 * @param ctx the parse tree
	 */
	void enterDataUsageClause(CobolParser.DataUsageClauseContext ctx);
	/**
	 * Exit a parse tree produced by {@link CobolParser#dataUsageClause}.
	 * @param ctx the parse tree
	 */
	void exitDataUsageClause(CobolParser.DataUsageClauseContext ctx);
	/**
	 * Enter a parse tree produced by {@link CobolParser#dataUsingClause}.
	 * @param ctx the parse tree
	 */
	void enterDataUsingClause(CobolParser.DataUsingClauseContext ctx);
	/**
	 * Exit a parse tree produced by {@link CobolParser#dataUsingClause}.
	 * @param ctx the parse tree
	 */
	void exitDataUsingClause(CobolParser.DataUsingClauseContext ctx);
	/**
	 * Enter a parse tree produced by {@link CobolParser#dataValueClause}.
	 * @param ctx the parse tree
	 */
	void enterDataValueClause(CobolParser.DataValueClauseContext ctx);
	/**
	 * Exit a parse tree produced by {@link CobolParser#dataValueClause}.
	 * @param ctx the parse tree
	 */
	void exitDataValueClause(CobolParser.DataValueClauseContext ctx);
	/**
	 * Enter a parse tree produced by {@link CobolParser#dataValueInterval}.
	 * @param ctx the parse tree
	 */
	void enterDataValueInterval(CobolParser.DataValueIntervalContext ctx);
	/**
	 * Exit a parse tree produced by {@link CobolParser#dataValueInterval}.
	 * @param ctx the parse tree
	 */
	void exitDataValueInterval(CobolParser.DataValueIntervalContext ctx);
	/**
	 * Enter a parse tree produced by {@link CobolParser#dataValueIntervalFrom}.
	 * @param ctx the parse tree
	 */
	void enterDataValueIntervalFrom(CobolParser.DataValueIntervalFromContext ctx);
	/**
	 * Exit a parse tree produced by {@link CobolParser#dataValueIntervalFrom}.
	 * @param ctx the parse tree
	 */
	void exitDataValueIntervalFrom(CobolParser.DataValueIntervalFromContext ctx);
	/**
	 * Enter a parse tree produced by {@link CobolParser#dataValueIntervalTo}.
	 * @param ctx the parse tree
	 */
	void enterDataValueIntervalTo(CobolParser.DataValueIntervalToContext ctx);
	/**
	 * Exit a parse tree produced by {@link CobolParser#dataValueIntervalTo}.
	 * @param ctx the parse tree
	 */
	void exitDataValueIntervalTo(CobolParser.DataValueIntervalToContext ctx);
	/**
	 * Enter a parse tree produced by {@link CobolParser#dataWithLowerBoundsClause}.
	 * @param ctx the parse tree
	 */
	void enterDataWithLowerBoundsClause(CobolParser.DataWithLowerBoundsClauseContext ctx);
	/**
	 * Exit a parse tree produced by {@link CobolParser#dataWithLowerBoundsClause}.
	 * @param ctx the parse tree
	 */
	void exitDataWithLowerBoundsClause(CobolParser.DataWithLowerBoundsClauseContext ctx);
	/**
	 * Enter a parse tree produced by {@link CobolParser#procedureDivision}.
	 * @param ctx the parse tree
	 */
	void enterProcedureDivision(CobolParser.ProcedureDivisionContext ctx);
	/**
	 * Exit a parse tree produced by {@link CobolParser#procedureDivision}.
	 * @param ctx the parse tree
	 */
	void exitProcedureDivision(CobolParser.ProcedureDivisionContext ctx);
	/**
	 * Enter a parse tree produced by {@link CobolParser#procedureDivisionUsingClause}.
	 * @param ctx the parse tree
	 */
	void enterProcedureDivisionUsingClause(CobolParser.ProcedureDivisionUsingClauseContext ctx);
	/**
	 * Exit a parse tree produced by {@link CobolParser#procedureDivisionUsingClause}.
	 * @param ctx the parse tree
	 */
	void exitProcedureDivisionUsingClause(CobolParser.ProcedureDivisionUsingClauseContext ctx);
	/**
	 * Enter a parse tree produced by {@link CobolParser#procedureDivisionGivingClause}.
	 * @param ctx the parse tree
	 */
	void enterProcedureDivisionGivingClause(CobolParser.ProcedureDivisionGivingClauseContext ctx);
	/**
	 * Exit a parse tree produced by {@link CobolParser#procedureDivisionGivingClause}.
	 * @param ctx the parse tree
	 */
	void exitProcedureDivisionGivingClause(CobolParser.ProcedureDivisionGivingClauseContext ctx);
	/**
	 * Enter a parse tree produced by {@link CobolParser#procedureDivisionUsingParameter}.
	 * @param ctx the parse tree
	 */
	void enterProcedureDivisionUsingParameter(CobolParser.ProcedureDivisionUsingParameterContext ctx);
	/**
	 * Exit a parse tree produced by {@link CobolParser#procedureDivisionUsingParameter}.
	 * @param ctx the parse tree
	 */
	void exitProcedureDivisionUsingParameter(CobolParser.ProcedureDivisionUsingParameterContext ctx);
	/**
	 * Enter a parse tree produced by {@link CobolParser#procedureDivisionByReferencePhrase}.
	 * @param ctx the parse tree
	 */
	void enterProcedureDivisionByReferencePhrase(CobolParser.ProcedureDivisionByReferencePhraseContext ctx);
	/**
	 * Exit a parse tree produced by {@link CobolParser#procedureDivisionByReferencePhrase}.
	 * @param ctx the parse tree
	 */
	void exitProcedureDivisionByReferencePhrase(CobolParser.ProcedureDivisionByReferencePhraseContext ctx);
	/**
	 * Enter a parse tree produced by {@link CobolParser#procedureDivisionByReference}.
	 * @param ctx the parse tree
	 */
	void enterProcedureDivisionByReference(CobolParser.ProcedureDivisionByReferenceContext ctx);
	/**
	 * Exit a parse tree produced by {@link CobolParser#procedureDivisionByReference}.
	 * @param ctx the parse tree
	 */
	void exitProcedureDivisionByReference(CobolParser.ProcedureDivisionByReferenceContext ctx);
	/**
	 * Enter a parse tree produced by {@link CobolParser#procedureDivisionByValuePhrase}.
	 * @param ctx the parse tree
	 */
	void enterProcedureDivisionByValuePhrase(CobolParser.ProcedureDivisionByValuePhraseContext ctx);
	/**
	 * Exit a parse tree produced by {@link CobolParser#procedureDivisionByValuePhrase}.
	 * @param ctx the parse tree
	 */
	void exitProcedureDivisionByValuePhrase(CobolParser.ProcedureDivisionByValuePhraseContext ctx);
	/**
	 * Enter a parse tree produced by {@link CobolParser#procedureDivisionByValue}.
	 * @param ctx the parse tree
	 */
	void enterProcedureDivisionByValue(CobolParser.ProcedureDivisionByValueContext ctx);
	/**
	 * Exit a parse tree produced by {@link CobolParser#procedureDivisionByValue}.
	 * @param ctx the parse tree
	 */
	void exitProcedureDivisionByValue(CobolParser.ProcedureDivisionByValueContext ctx);
	/**
	 * Enter a parse tree produced by {@link CobolParser#procedureDeclaratives}.
	 * @param ctx the parse tree
	 */
	void enterProcedureDeclaratives(CobolParser.ProcedureDeclarativesContext ctx);
	/**
	 * Exit a parse tree produced by {@link CobolParser#procedureDeclaratives}.
	 * @param ctx the parse tree
	 */
	void exitProcedureDeclaratives(CobolParser.ProcedureDeclarativesContext ctx);
	/**
	 * Enter a parse tree produced by {@link CobolParser#procedureDeclarative}.
	 * @param ctx the parse tree
	 */
	void enterProcedureDeclarative(CobolParser.ProcedureDeclarativeContext ctx);
	/**
	 * Exit a parse tree produced by {@link CobolParser#procedureDeclarative}.
	 * @param ctx the parse tree
	 */
	void exitProcedureDeclarative(CobolParser.ProcedureDeclarativeContext ctx);
	/**
	 * Enter a parse tree produced by {@link CobolParser#procedureSectionHeader}.
	 * @param ctx the parse tree
	 */
	void enterProcedureSectionHeader(CobolParser.ProcedureSectionHeaderContext ctx);
	/**
	 * Exit a parse tree produced by {@link CobolParser#procedureSectionHeader}.
	 * @param ctx the parse tree
	 */
	void exitProcedureSectionHeader(CobolParser.ProcedureSectionHeaderContext ctx);
	/**
	 * Enter a parse tree produced by {@link CobolParser#procedureDivisionBody}.
	 * @param ctx the parse tree
	 */
	void enterProcedureDivisionBody(CobolParser.ProcedureDivisionBodyContext ctx);
	/**
	 * Exit a parse tree produced by {@link CobolParser#procedureDivisionBody}.
	 * @param ctx the parse tree
	 */
	void exitProcedureDivisionBody(CobolParser.ProcedureDivisionBodyContext ctx);
	/**
	 * Enter a parse tree produced by {@link CobolParser#procedureSection}.
	 * @param ctx the parse tree
	 */
	void enterProcedureSection(CobolParser.ProcedureSectionContext ctx);
	/**
	 * Exit a parse tree produced by {@link CobolParser#procedureSection}.
	 * @param ctx the parse tree
	 */
	void exitProcedureSection(CobolParser.ProcedureSectionContext ctx);
	/**
	 * Enter a parse tree produced by {@link CobolParser#paragraphs}.
	 * @param ctx the parse tree
	 */
	void enterParagraphs(CobolParser.ParagraphsContext ctx);
	/**
	 * Exit a parse tree produced by {@link CobolParser#paragraphs}.
	 * @param ctx the parse tree
	 */
	void exitParagraphs(CobolParser.ParagraphsContext ctx);
	/**
	 * Enter a parse tree produced by {@link CobolParser#paragraph}.
	 * @param ctx the parse tree
	 */
	void enterParagraph(CobolParser.ParagraphContext ctx);
	/**
	 * Exit a parse tree produced by {@link CobolParser#paragraph}.
	 * @param ctx the parse tree
	 */
	void exitParagraph(CobolParser.ParagraphContext ctx);
	/**
	 * Enter a parse tree produced by {@link CobolParser#sentence}.
	 * @param ctx the parse tree
	 */
	void enterSentence(CobolParser.SentenceContext ctx);
	/**
	 * Exit a parse tree produced by {@link CobolParser#sentence}.
	 * @param ctx the parse tree
	 */
	void exitSentence(CobolParser.SentenceContext ctx);
	/**
	 * Enter a parse tree produced by {@link CobolParser#statement}.
	 * @param ctx the parse tree
	 */
	void enterStatement(CobolParser.StatementContext ctx);
	/**
	 * Exit a parse tree produced by {@link CobolParser#statement}.
	 * @param ctx the parse tree
	 */
	void exitStatement(CobolParser.StatementContext ctx);
	/**
	 * Enter a parse tree produced by {@link CobolParser#acceptStatement}.
	 * @param ctx the parse tree
	 */
	void enterAcceptStatement(CobolParser.AcceptStatementContext ctx);
	/**
	 * Exit a parse tree produced by {@link CobolParser#acceptStatement}.
	 * @param ctx the parse tree
	 */
	void exitAcceptStatement(CobolParser.AcceptStatementContext ctx);
	/**
	 * Enter a parse tree produced by {@link CobolParser#acceptFromDateStatement}.
	 * @param ctx the parse tree
	 */
	void enterAcceptFromDateStatement(CobolParser.AcceptFromDateStatementContext ctx);
	/**
	 * Exit a parse tree produced by {@link CobolParser#acceptFromDateStatement}.
	 * @param ctx the parse tree
	 */
	void exitAcceptFromDateStatement(CobolParser.AcceptFromDateStatementContext ctx);
	/**
	 * Enter a parse tree produced by {@link CobolParser#acceptFromMnemonicStatement}.
	 * @param ctx the parse tree
	 */
	void enterAcceptFromMnemonicStatement(CobolParser.AcceptFromMnemonicStatementContext ctx);
	/**
	 * Exit a parse tree produced by {@link CobolParser#acceptFromMnemonicStatement}.
	 * @param ctx the parse tree
	 */
	void exitAcceptFromMnemonicStatement(CobolParser.AcceptFromMnemonicStatementContext ctx);
	/**
	 * Enter a parse tree produced by {@link CobolParser#acceptFromEscapeKeyStatement}.
	 * @param ctx the parse tree
	 */
	void enterAcceptFromEscapeKeyStatement(CobolParser.AcceptFromEscapeKeyStatementContext ctx);
	/**
	 * Exit a parse tree produced by {@link CobolParser#acceptFromEscapeKeyStatement}.
	 * @param ctx the parse tree
	 */
	void exitAcceptFromEscapeKeyStatement(CobolParser.AcceptFromEscapeKeyStatementContext ctx);
	/**
	 * Enter a parse tree produced by {@link CobolParser#acceptMessageCountStatement}.
	 * @param ctx the parse tree
	 */
	void enterAcceptMessageCountStatement(CobolParser.AcceptMessageCountStatementContext ctx);
	/**
	 * Exit a parse tree produced by {@link CobolParser#acceptMessageCountStatement}.
	 * @param ctx the parse tree
	 */
	void exitAcceptMessageCountStatement(CobolParser.AcceptMessageCountStatementContext ctx);
	/**
	 * Enter a parse tree produced by {@link CobolParser#addStatement}.
	 * @param ctx the parse tree
	 */
	void enterAddStatement(CobolParser.AddStatementContext ctx);
	/**
	 * Exit a parse tree produced by {@link CobolParser#addStatement}.
	 * @param ctx the parse tree
	 */
	void exitAddStatement(CobolParser.AddStatementContext ctx);
	/**
	 * Enter a parse tree produced by {@link CobolParser#addToStatement}.
	 * @param ctx the parse tree
	 */
	void enterAddToStatement(CobolParser.AddToStatementContext ctx);
	/**
	 * Exit a parse tree produced by {@link CobolParser#addToStatement}.
	 * @param ctx the parse tree
	 */
	void exitAddToStatement(CobolParser.AddToStatementContext ctx);
	/**
	 * Enter a parse tree produced by {@link CobolParser#addToGivingStatement}.
	 * @param ctx the parse tree
	 */
	void enterAddToGivingStatement(CobolParser.AddToGivingStatementContext ctx);
	/**
	 * Exit a parse tree produced by {@link CobolParser#addToGivingStatement}.
	 * @param ctx the parse tree
	 */
	void exitAddToGivingStatement(CobolParser.AddToGivingStatementContext ctx);
	/**
	 * Enter a parse tree produced by {@link CobolParser#addCorrespondingStatement}.
	 * @param ctx the parse tree
	 */
	void enterAddCorrespondingStatement(CobolParser.AddCorrespondingStatementContext ctx);
	/**
	 * Exit a parse tree produced by {@link CobolParser#addCorrespondingStatement}.
	 * @param ctx the parse tree
	 */
	void exitAddCorrespondingStatement(CobolParser.AddCorrespondingStatementContext ctx);
	/**
	 * Enter a parse tree produced by {@link CobolParser#addFrom}.
	 * @param ctx the parse tree
	 */
	void enterAddFrom(CobolParser.AddFromContext ctx);
	/**
	 * Exit a parse tree produced by {@link CobolParser#addFrom}.
	 * @param ctx the parse tree
	 */
	void exitAddFrom(CobolParser.AddFromContext ctx);
	/**
	 * Enter a parse tree produced by {@link CobolParser#addTo}.
	 * @param ctx the parse tree
	 */
	void enterAddTo(CobolParser.AddToContext ctx);
	/**
	 * Exit a parse tree produced by {@link CobolParser#addTo}.
	 * @param ctx the parse tree
	 */
	void exitAddTo(CobolParser.AddToContext ctx);
	/**
	 * Enter a parse tree produced by {@link CobolParser#addToGiving}.
	 * @param ctx the parse tree
	 */
	void enterAddToGiving(CobolParser.AddToGivingContext ctx);
	/**
	 * Exit a parse tree produced by {@link CobolParser#addToGiving}.
	 * @param ctx the parse tree
	 */
	void exitAddToGiving(CobolParser.AddToGivingContext ctx);
	/**
	 * Enter a parse tree produced by {@link CobolParser#addGiving}.
	 * @param ctx the parse tree
	 */
	void enterAddGiving(CobolParser.AddGivingContext ctx);
	/**
	 * Exit a parse tree produced by {@link CobolParser#addGiving}.
	 * @param ctx the parse tree
	 */
	void exitAddGiving(CobolParser.AddGivingContext ctx);
	/**
	 * Enter a parse tree produced by {@link CobolParser#alteredGoTo}.
	 * @param ctx the parse tree
	 */
	void enterAlteredGoTo(CobolParser.AlteredGoToContext ctx);
	/**
	 * Exit a parse tree produced by {@link CobolParser#alteredGoTo}.
	 * @param ctx the parse tree
	 */
	void exitAlteredGoTo(CobolParser.AlteredGoToContext ctx);
	/**
	 * Enter a parse tree produced by {@link CobolParser#alterStatement}.
	 * @param ctx the parse tree
	 */
	void enterAlterStatement(CobolParser.AlterStatementContext ctx);
	/**
	 * Exit a parse tree produced by {@link CobolParser#alterStatement}.
	 * @param ctx the parse tree
	 */
	void exitAlterStatement(CobolParser.AlterStatementContext ctx);
	/**
	 * Enter a parse tree produced by {@link CobolParser#alterProceedTo}.
	 * @param ctx the parse tree
	 */
	void enterAlterProceedTo(CobolParser.AlterProceedToContext ctx);
	/**
	 * Exit a parse tree produced by {@link CobolParser#alterProceedTo}.
	 * @param ctx the parse tree
	 */
	void exitAlterProceedTo(CobolParser.AlterProceedToContext ctx);
	/**
	 * Enter a parse tree produced by {@link CobolParser#callStatement}.
	 * @param ctx the parse tree
	 */
	void enterCallStatement(CobolParser.CallStatementContext ctx);
	/**
	 * Exit a parse tree produced by {@link CobolParser#callStatement}.
	 * @param ctx the parse tree
	 */
	void exitCallStatement(CobolParser.CallStatementContext ctx);
	/**
	 * Enter a parse tree produced by {@link CobolParser#callUsingPhrase}.
	 * @param ctx the parse tree
	 */
	void enterCallUsingPhrase(CobolParser.CallUsingPhraseContext ctx);
	/**
	 * Exit a parse tree produced by {@link CobolParser#callUsingPhrase}.
	 * @param ctx the parse tree
	 */
	void exitCallUsingPhrase(CobolParser.CallUsingPhraseContext ctx);
	/**
	 * Enter a parse tree produced by {@link CobolParser#callUsingParameter}.
	 * @param ctx the parse tree
	 */
	void enterCallUsingParameter(CobolParser.CallUsingParameterContext ctx);
	/**
	 * Exit a parse tree produced by {@link CobolParser#callUsingParameter}.
	 * @param ctx the parse tree
	 */
	void exitCallUsingParameter(CobolParser.CallUsingParameterContext ctx);
	/**
	 * Enter a parse tree produced by {@link CobolParser#callByReferencePhrase}.
	 * @param ctx the parse tree
	 */
	void enterCallByReferencePhrase(CobolParser.CallByReferencePhraseContext ctx);
	/**
	 * Exit a parse tree produced by {@link CobolParser#callByReferencePhrase}.
	 * @param ctx the parse tree
	 */
	void exitCallByReferencePhrase(CobolParser.CallByReferencePhraseContext ctx);
	/**
	 * Enter a parse tree produced by {@link CobolParser#callByReference}.
	 * @param ctx the parse tree
	 */
	void enterCallByReference(CobolParser.CallByReferenceContext ctx);
	/**
	 * Exit a parse tree produced by {@link CobolParser#callByReference}.
	 * @param ctx the parse tree
	 */
	void exitCallByReference(CobolParser.CallByReferenceContext ctx);
	/**
	 * Enter a parse tree produced by {@link CobolParser#callByValuePhrase}.
	 * @param ctx the parse tree
	 */
	void enterCallByValuePhrase(CobolParser.CallByValuePhraseContext ctx);
	/**
	 * Exit a parse tree produced by {@link CobolParser#callByValuePhrase}.
	 * @param ctx the parse tree
	 */
	void exitCallByValuePhrase(CobolParser.CallByValuePhraseContext ctx);
	/**
	 * Enter a parse tree produced by {@link CobolParser#callByValue}.
	 * @param ctx the parse tree
	 */
	void enterCallByValue(CobolParser.CallByValueContext ctx);
	/**
	 * Exit a parse tree produced by {@link CobolParser#callByValue}.
	 * @param ctx the parse tree
	 */
	void exitCallByValue(CobolParser.CallByValueContext ctx);
	/**
	 * Enter a parse tree produced by {@link CobolParser#callByContentPhrase}.
	 * @param ctx the parse tree
	 */
	void enterCallByContentPhrase(CobolParser.CallByContentPhraseContext ctx);
	/**
	 * Exit a parse tree produced by {@link CobolParser#callByContentPhrase}.
	 * @param ctx the parse tree
	 */
	void exitCallByContentPhrase(CobolParser.CallByContentPhraseContext ctx);
	/**
	 * Enter a parse tree produced by {@link CobolParser#callByContent}.
	 * @param ctx the parse tree
	 */
	void enterCallByContent(CobolParser.CallByContentContext ctx);
	/**
	 * Exit a parse tree produced by {@link CobolParser#callByContent}.
	 * @param ctx the parse tree
	 */
	void exitCallByContent(CobolParser.CallByContentContext ctx);
	/**
	 * Enter a parse tree produced by {@link CobolParser#callGivingPhrase}.
	 * @param ctx the parse tree
	 */
	void enterCallGivingPhrase(CobolParser.CallGivingPhraseContext ctx);
	/**
	 * Exit a parse tree produced by {@link CobolParser#callGivingPhrase}.
	 * @param ctx the parse tree
	 */
	void exitCallGivingPhrase(CobolParser.CallGivingPhraseContext ctx);
	/**
	 * Enter a parse tree produced by {@link CobolParser#cancelStatement}.
	 * @param ctx the parse tree
	 */
	void enterCancelStatement(CobolParser.CancelStatementContext ctx);
	/**
	 * Exit a parse tree produced by {@link CobolParser#cancelStatement}.
	 * @param ctx the parse tree
	 */
	void exitCancelStatement(CobolParser.CancelStatementContext ctx);
	/**
	 * Enter a parse tree produced by {@link CobolParser#cancelCall}.
	 * @param ctx the parse tree
	 */
	void enterCancelCall(CobolParser.CancelCallContext ctx);
	/**
	 * Exit a parse tree produced by {@link CobolParser#cancelCall}.
	 * @param ctx the parse tree
	 */
	void exitCancelCall(CobolParser.CancelCallContext ctx);
	/**
	 * Enter a parse tree produced by {@link CobolParser#closeStatement}.
	 * @param ctx the parse tree
	 */
	void enterCloseStatement(CobolParser.CloseStatementContext ctx);
	/**
	 * Exit a parse tree produced by {@link CobolParser#closeStatement}.
	 * @param ctx the parse tree
	 */
	void exitCloseStatement(CobolParser.CloseStatementContext ctx);
	/**
	 * Enter a parse tree produced by {@link CobolParser#closeFile}.
	 * @param ctx the parse tree
	 */
	void enterCloseFile(CobolParser.CloseFileContext ctx);
	/**
	 * Exit a parse tree produced by {@link CobolParser#closeFile}.
	 * @param ctx the parse tree
	 */
	void exitCloseFile(CobolParser.CloseFileContext ctx);
	/**
	 * Enter a parse tree produced by {@link CobolParser#closeReelUnitStatement}.
	 * @param ctx the parse tree
	 */
	void enterCloseReelUnitStatement(CobolParser.CloseReelUnitStatementContext ctx);
	/**
	 * Exit a parse tree produced by {@link CobolParser#closeReelUnitStatement}.
	 * @param ctx the parse tree
	 */
	void exitCloseReelUnitStatement(CobolParser.CloseReelUnitStatementContext ctx);
	/**
	 * Enter a parse tree produced by {@link CobolParser#closeRelativeStatement}.
	 * @param ctx the parse tree
	 */
	void enterCloseRelativeStatement(CobolParser.CloseRelativeStatementContext ctx);
	/**
	 * Exit a parse tree produced by {@link CobolParser#closeRelativeStatement}.
	 * @param ctx the parse tree
	 */
	void exitCloseRelativeStatement(CobolParser.CloseRelativeStatementContext ctx);
	/**
	 * Enter a parse tree produced by {@link CobolParser#closePortFileIOStatement}.
	 * @param ctx the parse tree
	 */
	void enterClosePortFileIOStatement(CobolParser.ClosePortFileIOStatementContext ctx);
	/**
	 * Exit a parse tree produced by {@link CobolParser#closePortFileIOStatement}.
	 * @param ctx the parse tree
	 */
	void exitClosePortFileIOStatement(CobolParser.ClosePortFileIOStatementContext ctx);
	/**
	 * Enter a parse tree produced by {@link CobolParser#closePortFileIOUsing}.
	 * @param ctx the parse tree
	 */
	void enterClosePortFileIOUsing(CobolParser.ClosePortFileIOUsingContext ctx);
	/**
	 * Exit a parse tree produced by {@link CobolParser#closePortFileIOUsing}.
	 * @param ctx the parse tree
	 */
	void exitClosePortFileIOUsing(CobolParser.ClosePortFileIOUsingContext ctx);
	/**
	 * Enter a parse tree produced by {@link CobolParser#closePortFileIOUsingCloseDisposition}.
	 * @param ctx the parse tree
	 */
	void enterClosePortFileIOUsingCloseDisposition(CobolParser.ClosePortFileIOUsingCloseDispositionContext ctx);
	/**
	 * Exit a parse tree produced by {@link CobolParser#closePortFileIOUsingCloseDisposition}.
	 * @param ctx the parse tree
	 */
	void exitClosePortFileIOUsingCloseDisposition(CobolParser.ClosePortFileIOUsingCloseDispositionContext ctx);
	/**
	 * Enter a parse tree produced by {@link CobolParser#closePortFileIOUsingAssociatedData}.
	 * @param ctx the parse tree
	 */
	void enterClosePortFileIOUsingAssociatedData(CobolParser.ClosePortFileIOUsingAssociatedDataContext ctx);
	/**
	 * Exit a parse tree produced by {@link CobolParser#closePortFileIOUsingAssociatedData}.
	 * @param ctx the parse tree
	 */
	void exitClosePortFileIOUsingAssociatedData(CobolParser.ClosePortFileIOUsingAssociatedDataContext ctx);
	/**
	 * Enter a parse tree produced by {@link CobolParser#closePortFileIOUsingAssociatedDataLength}.
	 * @param ctx the parse tree
	 */
	void enterClosePortFileIOUsingAssociatedDataLength(CobolParser.ClosePortFileIOUsingAssociatedDataLengthContext ctx);
	/**
	 * Exit a parse tree produced by {@link CobolParser#closePortFileIOUsingAssociatedDataLength}.
	 * @param ctx the parse tree
	 */
	void exitClosePortFileIOUsingAssociatedDataLength(CobolParser.ClosePortFileIOUsingAssociatedDataLengthContext ctx);
	/**
	 * Enter a parse tree produced by {@link CobolParser#computeStatement}.
	 * @param ctx the parse tree
	 */
	void enterComputeStatement(CobolParser.ComputeStatementContext ctx);
	/**
	 * Exit a parse tree produced by {@link CobolParser#computeStatement}.
	 * @param ctx the parse tree
	 */
	void exitComputeStatement(CobolParser.ComputeStatementContext ctx);
	/**
	 * Enter a parse tree produced by {@link CobolParser#computeStore}.
	 * @param ctx the parse tree
	 */
	void enterComputeStore(CobolParser.ComputeStoreContext ctx);
	/**
	 * Exit a parse tree produced by {@link CobolParser#computeStore}.
	 * @param ctx the parse tree
	 */
	void exitComputeStore(CobolParser.ComputeStoreContext ctx);
	/**
	 * Enter a parse tree produced by {@link CobolParser#continueStatement}.
	 * @param ctx the parse tree
	 */
	void enterContinueStatement(CobolParser.ContinueStatementContext ctx);
	/**
	 * Exit a parse tree produced by {@link CobolParser#continueStatement}.
	 * @param ctx the parse tree
	 */
	void exitContinueStatement(CobolParser.ContinueStatementContext ctx);
	/**
	 * Enter a parse tree produced by {@link CobolParser#deleteStatement}.
	 * @param ctx the parse tree
	 */
	void enterDeleteStatement(CobolParser.DeleteStatementContext ctx);
	/**
	 * Exit a parse tree produced by {@link CobolParser#deleteStatement}.
	 * @param ctx the parse tree
	 */
	void exitDeleteStatement(CobolParser.DeleteStatementContext ctx);
	/**
	 * Enter a parse tree produced by {@link CobolParser#disableStatement}.
	 * @param ctx the parse tree
	 */
	void enterDisableStatement(CobolParser.DisableStatementContext ctx);
	/**
	 * Exit a parse tree produced by {@link CobolParser#disableStatement}.
	 * @param ctx the parse tree
	 */
	void exitDisableStatement(CobolParser.DisableStatementContext ctx);
	/**
	 * Enter a parse tree produced by {@link CobolParser#displayStatement}.
	 * @param ctx the parse tree
	 */
	void enterDisplayStatement(CobolParser.DisplayStatementContext ctx);
	/**
	 * Exit a parse tree produced by {@link CobolParser#displayStatement}.
	 * @param ctx the parse tree
	 */
	void exitDisplayStatement(CobolParser.DisplayStatementContext ctx);
	/**
	 * Enter a parse tree produced by {@link CobolParser#displayOperand}.
	 * @param ctx the parse tree
	 */
	void enterDisplayOperand(CobolParser.DisplayOperandContext ctx);
	/**
	 * Exit a parse tree produced by {@link CobolParser#displayOperand}.
	 * @param ctx the parse tree
	 */
	void exitDisplayOperand(CobolParser.DisplayOperandContext ctx);
	/**
	 * Enter a parse tree produced by {@link CobolParser#displayAt}.
	 * @param ctx the parse tree
	 */
	void enterDisplayAt(CobolParser.DisplayAtContext ctx);
	/**
	 * Exit a parse tree produced by {@link CobolParser#displayAt}.
	 * @param ctx the parse tree
	 */
	void exitDisplayAt(CobolParser.DisplayAtContext ctx);
	/**
	 * Enter a parse tree produced by {@link CobolParser#displayUpon}.
	 * @param ctx the parse tree
	 */
	void enterDisplayUpon(CobolParser.DisplayUponContext ctx);
	/**
	 * Exit a parse tree produced by {@link CobolParser#displayUpon}.
	 * @param ctx the parse tree
	 */
	void exitDisplayUpon(CobolParser.DisplayUponContext ctx);
	/**
	 * Enter a parse tree produced by {@link CobolParser#displayWith}.
	 * @param ctx the parse tree
	 */
	void enterDisplayWith(CobolParser.DisplayWithContext ctx);
	/**
	 * Exit a parse tree produced by {@link CobolParser#displayWith}.
	 * @param ctx the parse tree
	 */
	void exitDisplayWith(CobolParser.DisplayWithContext ctx);
	/**
	 * Enter a parse tree produced by {@link CobolParser#divideStatement}.
	 * @param ctx the parse tree
	 */
	void enterDivideStatement(CobolParser.DivideStatementContext ctx);
	/**
	 * Exit a parse tree produced by {@link CobolParser#divideStatement}.
	 * @param ctx the parse tree
	 */
	void exitDivideStatement(CobolParser.DivideStatementContext ctx);
	/**
	 * Enter a parse tree produced by {@link CobolParser#divideIntoStatement}.
	 * @param ctx the parse tree
	 */
	void enterDivideIntoStatement(CobolParser.DivideIntoStatementContext ctx);
	/**
	 * Exit a parse tree produced by {@link CobolParser#divideIntoStatement}.
	 * @param ctx the parse tree
	 */
	void exitDivideIntoStatement(CobolParser.DivideIntoStatementContext ctx);
	/**
	 * Enter a parse tree produced by {@link CobolParser#divideIntoGivingStatement}.
	 * @param ctx the parse tree
	 */
	void enterDivideIntoGivingStatement(CobolParser.DivideIntoGivingStatementContext ctx);
	/**
	 * Exit a parse tree produced by {@link CobolParser#divideIntoGivingStatement}.
	 * @param ctx the parse tree
	 */
	void exitDivideIntoGivingStatement(CobolParser.DivideIntoGivingStatementContext ctx);
	/**
	 * Enter a parse tree produced by {@link CobolParser#divideByGivingStatement}.
	 * @param ctx the parse tree
	 */
	void enterDivideByGivingStatement(CobolParser.DivideByGivingStatementContext ctx);
	/**
	 * Exit a parse tree produced by {@link CobolParser#divideByGivingStatement}.
	 * @param ctx the parse tree
	 */
	void exitDivideByGivingStatement(CobolParser.DivideByGivingStatementContext ctx);
	/**
	 * Enter a parse tree produced by {@link CobolParser#divideGivingPhrase}.
	 * @param ctx the parse tree
	 */
	void enterDivideGivingPhrase(CobolParser.DivideGivingPhraseContext ctx);
	/**
	 * Exit a parse tree produced by {@link CobolParser#divideGivingPhrase}.
	 * @param ctx the parse tree
	 */
	void exitDivideGivingPhrase(CobolParser.DivideGivingPhraseContext ctx);
	/**
	 * Enter a parse tree produced by {@link CobolParser#divideInto}.
	 * @param ctx the parse tree
	 */
	void enterDivideInto(CobolParser.DivideIntoContext ctx);
	/**
	 * Exit a parse tree produced by {@link CobolParser#divideInto}.
	 * @param ctx the parse tree
	 */
	void exitDivideInto(CobolParser.DivideIntoContext ctx);
	/**
	 * Enter a parse tree produced by {@link CobolParser#divideGiving}.
	 * @param ctx the parse tree
	 */
	void enterDivideGiving(CobolParser.DivideGivingContext ctx);
	/**
	 * Exit a parse tree produced by {@link CobolParser#divideGiving}.
	 * @param ctx the parse tree
	 */
	void exitDivideGiving(CobolParser.DivideGivingContext ctx);
	/**
	 * Enter a parse tree produced by {@link CobolParser#divideRemainder}.
	 * @param ctx the parse tree
	 */
	void enterDivideRemainder(CobolParser.DivideRemainderContext ctx);
	/**
	 * Exit a parse tree produced by {@link CobolParser#divideRemainder}.
	 * @param ctx the parse tree
	 */
	void exitDivideRemainder(CobolParser.DivideRemainderContext ctx);
	/**
	 * Enter a parse tree produced by {@link CobolParser#enableStatement}.
	 * @param ctx the parse tree
	 */
	void enterEnableStatement(CobolParser.EnableStatementContext ctx);
	/**
	 * Exit a parse tree produced by {@link CobolParser#enableStatement}.
	 * @param ctx the parse tree
	 */
	void exitEnableStatement(CobolParser.EnableStatementContext ctx);
	/**
	 * Enter a parse tree produced by {@link CobolParser#entryStatement}.
	 * @param ctx the parse tree
	 */
	void enterEntryStatement(CobolParser.EntryStatementContext ctx);
	/**
	 * Exit a parse tree produced by {@link CobolParser#entryStatement}.
	 * @param ctx the parse tree
	 */
	void exitEntryStatement(CobolParser.EntryStatementContext ctx);
	/**
	 * Enter a parse tree produced by {@link CobolParser#evaluateStatement}.
	 * @param ctx the parse tree
	 */
	void enterEvaluateStatement(CobolParser.EvaluateStatementContext ctx);
	/**
	 * Exit a parse tree produced by {@link CobolParser#evaluateStatement}.
	 * @param ctx the parse tree
	 */
	void exitEvaluateStatement(CobolParser.EvaluateStatementContext ctx);
	/**
	 * Enter a parse tree produced by {@link CobolParser#evaluateSelect}.
	 * @param ctx the parse tree
	 */
	void enterEvaluateSelect(CobolParser.EvaluateSelectContext ctx);
	/**
	 * Exit a parse tree produced by {@link CobolParser#evaluateSelect}.
	 * @param ctx the parse tree
	 */
	void exitEvaluateSelect(CobolParser.EvaluateSelectContext ctx);
	/**
	 * Enter a parse tree produced by {@link CobolParser#evaluateAlsoSelect}.
	 * @param ctx the parse tree
	 */
	void enterEvaluateAlsoSelect(CobolParser.EvaluateAlsoSelectContext ctx);
	/**
	 * Exit a parse tree produced by {@link CobolParser#evaluateAlsoSelect}.
	 * @param ctx the parse tree
	 */
	void exitEvaluateAlsoSelect(CobolParser.EvaluateAlsoSelectContext ctx);
	/**
	 * Enter a parse tree produced by {@link CobolParser#evaluateWhenPhrase}.
	 * @param ctx the parse tree
	 */
	void enterEvaluateWhenPhrase(CobolParser.EvaluateWhenPhraseContext ctx);
	/**
	 * Exit a parse tree produced by {@link CobolParser#evaluateWhenPhrase}.
	 * @param ctx the parse tree
	 */
	void exitEvaluateWhenPhrase(CobolParser.EvaluateWhenPhraseContext ctx);
	/**
	 * Enter a parse tree produced by {@link CobolParser#evaluateWhen}.
	 * @param ctx the parse tree
	 */
	void enterEvaluateWhen(CobolParser.EvaluateWhenContext ctx);
	/**
	 * Exit a parse tree produced by {@link CobolParser#evaluateWhen}.
	 * @param ctx the parse tree
	 */
	void exitEvaluateWhen(CobolParser.EvaluateWhenContext ctx);
	/**
	 * Enter a parse tree produced by {@link CobolParser#evaluateCondition}.
	 * @param ctx the parse tree
	 */
	void enterEvaluateCondition(CobolParser.EvaluateConditionContext ctx);
	/**
	 * Exit a parse tree produced by {@link CobolParser#evaluateCondition}.
	 * @param ctx the parse tree
	 */
	void exitEvaluateCondition(CobolParser.EvaluateConditionContext ctx);
	/**
	 * Enter a parse tree produced by {@link CobolParser#evaluateThrough}.
	 * @param ctx the parse tree
	 */
	void enterEvaluateThrough(CobolParser.EvaluateThroughContext ctx);
	/**
	 * Exit a parse tree produced by {@link CobolParser#evaluateThrough}.
	 * @param ctx the parse tree
	 */
	void exitEvaluateThrough(CobolParser.EvaluateThroughContext ctx);
	/**
	 * Enter a parse tree produced by {@link CobolParser#evaluateAlsoCondition}.
	 * @param ctx the parse tree
	 */
	void enterEvaluateAlsoCondition(CobolParser.EvaluateAlsoConditionContext ctx);
	/**
	 * Exit a parse tree produced by {@link CobolParser#evaluateAlsoCondition}.
	 * @param ctx the parse tree
	 */
	void exitEvaluateAlsoCondition(CobolParser.EvaluateAlsoConditionContext ctx);
	/**
	 * Enter a parse tree produced by {@link CobolParser#evaluateWhenOther}.
	 * @param ctx the parse tree
	 */
	void enterEvaluateWhenOther(CobolParser.EvaluateWhenOtherContext ctx);
	/**
	 * Exit a parse tree produced by {@link CobolParser#evaluateWhenOther}.
	 * @param ctx the parse tree
	 */
	void exitEvaluateWhenOther(CobolParser.EvaluateWhenOtherContext ctx);
	/**
	 * Enter a parse tree produced by {@link CobolParser#evaluateValue}.
	 * @param ctx the parse tree
	 */
	void enterEvaluateValue(CobolParser.EvaluateValueContext ctx);
	/**
	 * Exit a parse tree produced by {@link CobolParser#evaluateValue}.
	 * @param ctx the parse tree
	 */
	void exitEvaluateValue(CobolParser.EvaluateValueContext ctx);
	/**
	 * Enter a parse tree produced by {@link CobolParser#execCicsStatement}.
	 * @param ctx the parse tree
	 */
	void enterExecCicsStatement(CobolParser.ExecCicsStatementContext ctx);
	/**
	 * Exit a parse tree produced by {@link CobolParser#execCicsStatement}.
	 * @param ctx the parse tree
	 */
	void exitExecCicsStatement(CobolParser.ExecCicsStatementContext ctx);
	/**
	 * Enter a parse tree produced by {@link CobolParser#execSqlStatement}.
	 * @param ctx the parse tree
	 */
	void enterExecSqlStatement(CobolParser.ExecSqlStatementContext ctx);
	/**
	 * Exit a parse tree produced by {@link CobolParser#execSqlStatement}.
	 * @param ctx the parse tree
	 */
	void exitExecSqlStatement(CobolParser.ExecSqlStatementContext ctx);
	/**
	 * Enter a parse tree produced by {@link CobolParser#execSqlImsStatement}.
	 * @param ctx the parse tree
	 */
	void enterExecSqlImsStatement(CobolParser.ExecSqlImsStatementContext ctx);
	/**
	 * Exit a parse tree produced by {@link CobolParser#execSqlImsStatement}.
	 * @param ctx the parse tree
	 */
	void exitExecSqlImsStatement(CobolParser.ExecSqlImsStatementContext ctx);
	/**
	 * Enter a parse tree produced by {@link CobolParser#exhibitStatement}.
	 * @param ctx the parse tree
	 */
	void enterExhibitStatement(CobolParser.ExhibitStatementContext ctx);
	/**
	 * Exit a parse tree produced by {@link CobolParser#exhibitStatement}.
	 * @param ctx the parse tree
	 */
	void exitExhibitStatement(CobolParser.ExhibitStatementContext ctx);
	/**
	 * Enter a parse tree produced by {@link CobolParser#exhibitOperand}.
	 * @param ctx the parse tree
	 */
	void enterExhibitOperand(CobolParser.ExhibitOperandContext ctx);
	/**
	 * Exit a parse tree produced by {@link CobolParser#exhibitOperand}.
	 * @param ctx the parse tree
	 */
	void exitExhibitOperand(CobolParser.ExhibitOperandContext ctx);
	/**
	 * Enter a parse tree produced by {@link CobolParser#exitStatement}.
	 * @param ctx the parse tree
	 */
	void enterExitStatement(CobolParser.ExitStatementContext ctx);
	/**
	 * Exit a parse tree produced by {@link CobolParser#exitStatement}.
	 * @param ctx the parse tree
	 */
	void exitExitStatement(CobolParser.ExitStatementContext ctx);
	/**
	 * Enter a parse tree produced by {@link CobolParser#generateStatement}.
	 * @param ctx the parse tree
	 */
	void enterGenerateStatement(CobolParser.GenerateStatementContext ctx);
	/**
	 * Exit a parse tree produced by {@link CobolParser#generateStatement}.
	 * @param ctx the parse tree
	 */
	void exitGenerateStatement(CobolParser.GenerateStatementContext ctx);
	/**
	 * Enter a parse tree produced by {@link CobolParser#gobackStatement}.
	 * @param ctx the parse tree
	 */
	void enterGobackStatement(CobolParser.GobackStatementContext ctx);
	/**
	 * Exit a parse tree produced by {@link CobolParser#gobackStatement}.
	 * @param ctx the parse tree
	 */
	void exitGobackStatement(CobolParser.GobackStatementContext ctx);
	/**
	 * Enter a parse tree produced by {@link CobolParser#goToStatement}.
	 * @param ctx the parse tree
	 */
	void enterGoToStatement(CobolParser.GoToStatementContext ctx);
	/**
	 * Exit a parse tree produced by {@link CobolParser#goToStatement}.
	 * @param ctx the parse tree
	 */
	void exitGoToStatement(CobolParser.GoToStatementContext ctx);
	/**
	 * Enter a parse tree produced by {@link CobolParser#goToStatementSimple}.
	 * @param ctx the parse tree
	 */
	void enterGoToStatementSimple(CobolParser.GoToStatementSimpleContext ctx);
	/**
	 * Exit a parse tree produced by {@link CobolParser#goToStatementSimple}.
	 * @param ctx the parse tree
	 */
	void exitGoToStatementSimple(CobolParser.GoToStatementSimpleContext ctx);
	/**
	 * Enter a parse tree produced by {@link CobolParser#goToDependingOnStatement}.
	 * @param ctx the parse tree
	 */
	void enterGoToDependingOnStatement(CobolParser.GoToDependingOnStatementContext ctx);
	/**
	 * Exit a parse tree produced by {@link CobolParser#goToDependingOnStatement}.
	 * @param ctx the parse tree
	 */
	void exitGoToDependingOnStatement(CobolParser.GoToDependingOnStatementContext ctx);
	/**
	 * Enter a parse tree produced by {@link CobolParser#ifStatement}.
	 * @param ctx the parse tree
	 */
	void enterIfStatement(CobolParser.IfStatementContext ctx);
	/**
	 * Exit a parse tree produced by {@link CobolParser#ifStatement}.
	 * @param ctx the parse tree
	 */
	void exitIfStatement(CobolParser.IfStatementContext ctx);
	/**
	 * Enter a parse tree produced by {@link CobolParser#ifThen}.
	 * @param ctx the parse tree
	 */
	void enterIfThen(CobolParser.IfThenContext ctx);
	/**
	 * Exit a parse tree produced by {@link CobolParser#ifThen}.
	 * @param ctx the parse tree
	 */
	void exitIfThen(CobolParser.IfThenContext ctx);
	/**
	 * Enter a parse tree produced by {@link CobolParser#ifElse}.
	 * @param ctx the parse tree
	 */
	void enterIfElse(CobolParser.IfElseContext ctx);
	/**
	 * Exit a parse tree produced by {@link CobolParser#ifElse}.
	 * @param ctx the parse tree
	 */
	void exitIfElse(CobolParser.IfElseContext ctx);
	/**
	 * Enter a parse tree produced by {@link CobolParser#initializeStatement}.
	 * @param ctx the parse tree
	 */
	void enterInitializeStatement(CobolParser.InitializeStatementContext ctx);
	/**
	 * Exit a parse tree produced by {@link CobolParser#initializeStatement}.
	 * @param ctx the parse tree
	 */
	void exitInitializeStatement(CobolParser.InitializeStatementContext ctx);
	/**
	 * Enter a parse tree produced by {@link CobolParser#initializeReplacingPhrase}.
	 * @param ctx the parse tree
	 */
	void enterInitializeReplacingPhrase(CobolParser.InitializeReplacingPhraseContext ctx);
	/**
	 * Exit a parse tree produced by {@link CobolParser#initializeReplacingPhrase}.
	 * @param ctx the parse tree
	 */
	void exitInitializeReplacingPhrase(CobolParser.InitializeReplacingPhraseContext ctx);
	/**
	 * Enter a parse tree produced by {@link CobolParser#initializeReplacingBy}.
	 * @param ctx the parse tree
	 */
	void enterInitializeReplacingBy(CobolParser.InitializeReplacingByContext ctx);
	/**
	 * Exit a parse tree produced by {@link CobolParser#initializeReplacingBy}.
	 * @param ctx the parse tree
	 */
	void exitInitializeReplacingBy(CobolParser.InitializeReplacingByContext ctx);
	/**
	 * Enter a parse tree produced by {@link CobolParser#initiateStatement}.
	 * @param ctx the parse tree
	 */
	void enterInitiateStatement(CobolParser.InitiateStatementContext ctx);
	/**
	 * Exit a parse tree produced by {@link CobolParser#initiateStatement}.
	 * @param ctx the parse tree
	 */
	void exitInitiateStatement(CobolParser.InitiateStatementContext ctx);
	/**
	 * Enter a parse tree produced by {@link CobolParser#inspectStatement}.
	 * @param ctx the parse tree
	 */
	void enterInspectStatement(CobolParser.InspectStatementContext ctx);
	/**
	 * Exit a parse tree produced by {@link CobolParser#inspectStatement}.
	 * @param ctx the parse tree
	 */
	void exitInspectStatement(CobolParser.InspectStatementContext ctx);
	/**
	 * Enter a parse tree produced by {@link CobolParser#inspectTallyingPhrase}.
	 * @param ctx the parse tree
	 */
	void enterInspectTallyingPhrase(CobolParser.InspectTallyingPhraseContext ctx);
	/**
	 * Exit a parse tree produced by {@link CobolParser#inspectTallyingPhrase}.
	 * @param ctx the parse tree
	 */
	void exitInspectTallyingPhrase(CobolParser.InspectTallyingPhraseContext ctx);
	/**
	 * Enter a parse tree produced by {@link CobolParser#inspectReplacingPhrase}.
	 * @param ctx the parse tree
	 */
	void enterInspectReplacingPhrase(CobolParser.InspectReplacingPhraseContext ctx);
	/**
	 * Exit a parse tree produced by {@link CobolParser#inspectReplacingPhrase}.
	 * @param ctx the parse tree
	 */
	void exitInspectReplacingPhrase(CobolParser.InspectReplacingPhraseContext ctx);
	/**
	 * Enter a parse tree produced by {@link CobolParser#inspectTallyingReplacingPhrase}.
	 * @param ctx the parse tree
	 */
	void enterInspectTallyingReplacingPhrase(CobolParser.InspectTallyingReplacingPhraseContext ctx);
	/**
	 * Exit a parse tree produced by {@link CobolParser#inspectTallyingReplacingPhrase}.
	 * @param ctx the parse tree
	 */
	void exitInspectTallyingReplacingPhrase(CobolParser.InspectTallyingReplacingPhraseContext ctx);
	/**
	 * Enter a parse tree produced by {@link CobolParser#inspectConvertingPhrase}.
	 * @param ctx the parse tree
	 */
	void enterInspectConvertingPhrase(CobolParser.InspectConvertingPhraseContext ctx);
	/**
	 * Exit a parse tree produced by {@link CobolParser#inspectConvertingPhrase}.
	 * @param ctx the parse tree
	 */
	void exitInspectConvertingPhrase(CobolParser.InspectConvertingPhraseContext ctx);
	/**
	 * Enter a parse tree produced by {@link CobolParser#inspectFor}.
	 * @param ctx the parse tree
	 */
	void enterInspectFor(CobolParser.InspectForContext ctx);
	/**
	 * Exit a parse tree produced by {@link CobolParser#inspectFor}.
	 * @param ctx the parse tree
	 */
	void exitInspectFor(CobolParser.InspectForContext ctx);
	/**
	 * Enter a parse tree produced by {@link CobolParser#inspectCharacters}.
	 * @param ctx the parse tree
	 */
	void enterInspectCharacters(CobolParser.InspectCharactersContext ctx);
	/**
	 * Exit a parse tree produced by {@link CobolParser#inspectCharacters}.
	 * @param ctx the parse tree
	 */
	void exitInspectCharacters(CobolParser.InspectCharactersContext ctx);
	/**
	 * Enter a parse tree produced by {@link CobolParser#inspectReplacingCharacters}.
	 * @param ctx the parse tree
	 */
	void enterInspectReplacingCharacters(CobolParser.InspectReplacingCharactersContext ctx);
	/**
	 * Exit a parse tree produced by {@link CobolParser#inspectReplacingCharacters}.
	 * @param ctx the parse tree
	 */
	void exitInspectReplacingCharacters(CobolParser.InspectReplacingCharactersContext ctx);
	/**
	 * Enter a parse tree produced by {@link CobolParser#inspectAllLeadings}.
	 * @param ctx the parse tree
	 */
	void enterInspectAllLeadings(CobolParser.InspectAllLeadingsContext ctx);
	/**
	 * Exit a parse tree produced by {@link CobolParser#inspectAllLeadings}.
	 * @param ctx the parse tree
	 */
	void exitInspectAllLeadings(CobolParser.InspectAllLeadingsContext ctx);
	/**
	 * Enter a parse tree produced by {@link CobolParser#inspectReplacingAllLeadings}.
	 * @param ctx the parse tree
	 */
	void enterInspectReplacingAllLeadings(CobolParser.InspectReplacingAllLeadingsContext ctx);
	/**
	 * Exit a parse tree produced by {@link CobolParser#inspectReplacingAllLeadings}.
	 * @param ctx the parse tree
	 */
	void exitInspectReplacingAllLeadings(CobolParser.InspectReplacingAllLeadingsContext ctx);
	/**
	 * Enter a parse tree produced by {@link CobolParser#inspectAllLeading}.
	 * @param ctx the parse tree
	 */
	void enterInspectAllLeading(CobolParser.InspectAllLeadingContext ctx);
	/**
	 * Exit a parse tree produced by {@link CobolParser#inspectAllLeading}.
	 * @param ctx the parse tree
	 */
	void exitInspectAllLeading(CobolParser.InspectAllLeadingContext ctx);
	/**
	 * Enter a parse tree produced by {@link CobolParser#inspectReplacingAllLeading}.
	 * @param ctx the parse tree
	 */
	void enterInspectReplacingAllLeading(CobolParser.InspectReplacingAllLeadingContext ctx);
	/**
	 * Exit a parse tree produced by {@link CobolParser#inspectReplacingAllLeading}.
	 * @param ctx the parse tree
	 */
	void exitInspectReplacingAllLeading(CobolParser.InspectReplacingAllLeadingContext ctx);
	/**
	 * Enter a parse tree produced by {@link CobolParser#inspectBy}.
	 * @param ctx the parse tree
	 */
	void enterInspectBy(CobolParser.InspectByContext ctx);
	/**
	 * Exit a parse tree produced by {@link CobolParser#inspectBy}.
	 * @param ctx the parse tree
	 */
	void exitInspectBy(CobolParser.InspectByContext ctx);
	/**
	 * Enter a parse tree produced by {@link CobolParser#inspectTo}.
	 * @param ctx the parse tree
	 */
	void enterInspectTo(CobolParser.InspectToContext ctx);
	/**
	 * Exit a parse tree produced by {@link CobolParser#inspectTo}.
	 * @param ctx the parse tree
	 */
	void exitInspectTo(CobolParser.InspectToContext ctx);
	/**
	 * Enter a parse tree produced by {@link CobolParser#inspectBeforeAfter}.
	 * @param ctx the parse tree
	 */
	void enterInspectBeforeAfter(CobolParser.InspectBeforeAfterContext ctx);
	/**
	 * Exit a parse tree produced by {@link CobolParser#inspectBeforeAfter}.
	 * @param ctx the parse tree
	 */
	void exitInspectBeforeAfter(CobolParser.InspectBeforeAfterContext ctx);
	/**
	 * Enter a parse tree produced by {@link CobolParser#mergeStatement}.
	 * @param ctx the parse tree
	 */
	void enterMergeStatement(CobolParser.MergeStatementContext ctx);
	/**
	 * Exit a parse tree produced by {@link CobolParser#mergeStatement}.
	 * @param ctx the parse tree
	 */
	void exitMergeStatement(CobolParser.MergeStatementContext ctx);
	/**
	 * Enter a parse tree produced by {@link CobolParser#mergeOnKeyClause}.
	 * @param ctx the parse tree
	 */
	void enterMergeOnKeyClause(CobolParser.MergeOnKeyClauseContext ctx);
	/**
	 * Exit a parse tree produced by {@link CobolParser#mergeOnKeyClause}.
	 * @param ctx the parse tree
	 */
	void exitMergeOnKeyClause(CobolParser.MergeOnKeyClauseContext ctx);
	/**
	 * Enter a parse tree produced by {@link CobolParser#mergeCollatingSequencePhrase}.
	 * @param ctx the parse tree
	 */
	void enterMergeCollatingSequencePhrase(CobolParser.MergeCollatingSequencePhraseContext ctx);
	/**
	 * Exit a parse tree produced by {@link CobolParser#mergeCollatingSequencePhrase}.
	 * @param ctx the parse tree
	 */
	void exitMergeCollatingSequencePhrase(CobolParser.MergeCollatingSequencePhraseContext ctx);
	/**
	 * Enter a parse tree produced by {@link CobolParser#mergeCollatingAlphanumeric}.
	 * @param ctx the parse tree
	 */
	void enterMergeCollatingAlphanumeric(CobolParser.MergeCollatingAlphanumericContext ctx);
	/**
	 * Exit a parse tree produced by {@link CobolParser#mergeCollatingAlphanumeric}.
	 * @param ctx the parse tree
	 */
	void exitMergeCollatingAlphanumeric(CobolParser.MergeCollatingAlphanumericContext ctx);
	/**
	 * Enter a parse tree produced by {@link CobolParser#mergeCollatingNational}.
	 * @param ctx the parse tree
	 */
	void enterMergeCollatingNational(CobolParser.MergeCollatingNationalContext ctx);
	/**
	 * Exit a parse tree produced by {@link CobolParser#mergeCollatingNational}.
	 * @param ctx the parse tree
	 */
	void exitMergeCollatingNational(CobolParser.MergeCollatingNationalContext ctx);
	/**
	 * Enter a parse tree produced by {@link CobolParser#mergeUsing}.
	 * @param ctx the parse tree
	 */
	void enterMergeUsing(CobolParser.MergeUsingContext ctx);
	/**
	 * Exit a parse tree produced by {@link CobolParser#mergeUsing}.
	 * @param ctx the parse tree
	 */
	void exitMergeUsing(CobolParser.MergeUsingContext ctx);
	/**
	 * Enter a parse tree produced by {@link CobolParser#mergeOutputProcedurePhrase}.
	 * @param ctx the parse tree
	 */
	void enterMergeOutputProcedurePhrase(CobolParser.MergeOutputProcedurePhraseContext ctx);
	/**
	 * Exit a parse tree produced by {@link CobolParser#mergeOutputProcedurePhrase}.
	 * @param ctx the parse tree
	 */
	void exitMergeOutputProcedurePhrase(CobolParser.MergeOutputProcedurePhraseContext ctx);
	/**
	 * Enter a parse tree produced by {@link CobolParser#mergeOutputThrough}.
	 * @param ctx the parse tree
	 */
	void enterMergeOutputThrough(CobolParser.MergeOutputThroughContext ctx);
	/**
	 * Exit a parse tree produced by {@link CobolParser#mergeOutputThrough}.
	 * @param ctx the parse tree
	 */
	void exitMergeOutputThrough(CobolParser.MergeOutputThroughContext ctx);
	/**
	 * Enter a parse tree produced by {@link CobolParser#mergeGivingPhrase}.
	 * @param ctx the parse tree
	 */
	void enterMergeGivingPhrase(CobolParser.MergeGivingPhraseContext ctx);
	/**
	 * Exit a parse tree produced by {@link CobolParser#mergeGivingPhrase}.
	 * @param ctx the parse tree
	 */
	void exitMergeGivingPhrase(CobolParser.MergeGivingPhraseContext ctx);
	/**
	 * Enter a parse tree produced by {@link CobolParser#mergeGiving}.
	 * @param ctx the parse tree
	 */
	void enterMergeGiving(CobolParser.MergeGivingContext ctx);
	/**
	 * Exit a parse tree produced by {@link CobolParser#mergeGiving}.
	 * @param ctx the parse tree
	 */
	void exitMergeGiving(CobolParser.MergeGivingContext ctx);
	/**
	 * Enter a parse tree produced by {@link CobolParser#moveStatement}.
	 * @param ctx the parse tree
	 */
	void enterMoveStatement(CobolParser.MoveStatementContext ctx);
	/**
	 * Exit a parse tree produced by {@link CobolParser#moveStatement}.
	 * @param ctx the parse tree
	 */
	void exitMoveStatement(CobolParser.MoveStatementContext ctx);
	/**
	 * Enter a parse tree produced by {@link CobolParser#moveToStatement}.
	 * @param ctx the parse tree
	 */
	void enterMoveToStatement(CobolParser.MoveToStatementContext ctx);
	/**
	 * Exit a parse tree produced by {@link CobolParser#moveToStatement}.
	 * @param ctx the parse tree
	 */
	void exitMoveToStatement(CobolParser.MoveToStatementContext ctx);
	/**
	 * Enter a parse tree produced by {@link CobolParser#moveToSendingArea}.
	 * @param ctx the parse tree
	 */
	void enterMoveToSendingArea(CobolParser.MoveToSendingAreaContext ctx);
	/**
	 * Exit a parse tree produced by {@link CobolParser#moveToSendingArea}.
	 * @param ctx the parse tree
	 */
	void exitMoveToSendingArea(CobolParser.MoveToSendingAreaContext ctx);
	/**
	 * Enter a parse tree produced by {@link CobolParser#moveCorrespondingToStatement}.
	 * @param ctx the parse tree
	 */
	void enterMoveCorrespondingToStatement(CobolParser.MoveCorrespondingToStatementContext ctx);
	/**
	 * Exit a parse tree produced by {@link CobolParser#moveCorrespondingToStatement}.
	 * @param ctx the parse tree
	 */
	void exitMoveCorrespondingToStatement(CobolParser.MoveCorrespondingToStatementContext ctx);
	/**
	 * Enter a parse tree produced by {@link CobolParser#moveCorrespondingToSendingArea}.
	 * @param ctx the parse tree
	 */
	void enterMoveCorrespondingToSendingArea(CobolParser.MoveCorrespondingToSendingAreaContext ctx);
	/**
	 * Exit a parse tree produced by {@link CobolParser#moveCorrespondingToSendingArea}.
	 * @param ctx the parse tree
	 */
	void exitMoveCorrespondingToSendingArea(CobolParser.MoveCorrespondingToSendingAreaContext ctx);
	/**
	 * Enter a parse tree produced by {@link CobolParser#multiplyStatement}.
	 * @param ctx the parse tree
	 */
	void enterMultiplyStatement(CobolParser.MultiplyStatementContext ctx);
	/**
	 * Exit a parse tree produced by {@link CobolParser#multiplyStatement}.
	 * @param ctx the parse tree
	 */
	void exitMultiplyStatement(CobolParser.MultiplyStatementContext ctx);
	/**
	 * Enter a parse tree produced by {@link CobolParser#multiplyRegular}.
	 * @param ctx the parse tree
	 */
	void enterMultiplyRegular(CobolParser.MultiplyRegularContext ctx);
	/**
	 * Exit a parse tree produced by {@link CobolParser#multiplyRegular}.
	 * @param ctx the parse tree
	 */
	void exitMultiplyRegular(CobolParser.MultiplyRegularContext ctx);
	/**
	 * Enter a parse tree produced by {@link CobolParser#multiplyRegularOperand}.
	 * @param ctx the parse tree
	 */
	void enterMultiplyRegularOperand(CobolParser.MultiplyRegularOperandContext ctx);
	/**
	 * Exit a parse tree produced by {@link CobolParser#multiplyRegularOperand}.
	 * @param ctx the parse tree
	 */
	void exitMultiplyRegularOperand(CobolParser.MultiplyRegularOperandContext ctx);
	/**
	 * Enter a parse tree produced by {@link CobolParser#multiplyGiving}.
	 * @param ctx the parse tree
	 */
	void enterMultiplyGiving(CobolParser.MultiplyGivingContext ctx);
	/**
	 * Exit a parse tree produced by {@link CobolParser#multiplyGiving}.
	 * @param ctx the parse tree
	 */
	void exitMultiplyGiving(CobolParser.MultiplyGivingContext ctx);
	/**
	 * Enter a parse tree produced by {@link CobolParser#multiplyGivingOperand}.
	 * @param ctx the parse tree
	 */
	void enterMultiplyGivingOperand(CobolParser.MultiplyGivingOperandContext ctx);
	/**
	 * Exit a parse tree produced by {@link CobolParser#multiplyGivingOperand}.
	 * @param ctx the parse tree
	 */
	void exitMultiplyGivingOperand(CobolParser.MultiplyGivingOperandContext ctx);
	/**
	 * Enter a parse tree produced by {@link CobolParser#multiplyGivingResult}.
	 * @param ctx the parse tree
	 */
	void enterMultiplyGivingResult(CobolParser.MultiplyGivingResultContext ctx);
	/**
	 * Exit a parse tree produced by {@link CobolParser#multiplyGivingResult}.
	 * @param ctx the parse tree
	 */
	void exitMultiplyGivingResult(CobolParser.MultiplyGivingResultContext ctx);
	/**
	 * Enter a parse tree produced by {@link CobolParser#nextSentenceStatement}.
	 * @param ctx the parse tree
	 */
	void enterNextSentenceStatement(CobolParser.NextSentenceStatementContext ctx);
	/**
	 * Exit a parse tree produced by {@link CobolParser#nextSentenceStatement}.
	 * @param ctx the parse tree
	 */
	void exitNextSentenceStatement(CobolParser.NextSentenceStatementContext ctx);
	/**
	 * Enter a parse tree produced by {@link CobolParser#openStatement}.
	 * @param ctx the parse tree
	 */
	void enterOpenStatement(CobolParser.OpenStatementContext ctx);
	/**
	 * Exit a parse tree produced by {@link CobolParser#openStatement}.
	 * @param ctx the parse tree
	 */
	void exitOpenStatement(CobolParser.OpenStatementContext ctx);
	/**
	 * Enter a parse tree produced by {@link CobolParser#openInputStatement}.
	 * @param ctx the parse tree
	 */
	void enterOpenInputStatement(CobolParser.OpenInputStatementContext ctx);
	/**
	 * Exit a parse tree produced by {@link CobolParser#openInputStatement}.
	 * @param ctx the parse tree
	 */
	void exitOpenInputStatement(CobolParser.OpenInputStatementContext ctx);
	/**
	 * Enter a parse tree produced by {@link CobolParser#openInput}.
	 * @param ctx the parse tree
	 */
	void enterOpenInput(CobolParser.OpenInputContext ctx);
	/**
	 * Exit a parse tree produced by {@link CobolParser#openInput}.
	 * @param ctx the parse tree
	 */
	void exitOpenInput(CobolParser.OpenInputContext ctx);
	/**
	 * Enter a parse tree produced by {@link CobolParser#openOutputStatement}.
	 * @param ctx the parse tree
	 */
	void enterOpenOutputStatement(CobolParser.OpenOutputStatementContext ctx);
	/**
	 * Exit a parse tree produced by {@link CobolParser#openOutputStatement}.
	 * @param ctx the parse tree
	 */
	void exitOpenOutputStatement(CobolParser.OpenOutputStatementContext ctx);
	/**
	 * Enter a parse tree produced by {@link CobolParser#openOutput}.
	 * @param ctx the parse tree
	 */
	void enterOpenOutput(CobolParser.OpenOutputContext ctx);
	/**
	 * Exit a parse tree produced by {@link CobolParser#openOutput}.
	 * @param ctx the parse tree
	 */
	void exitOpenOutput(CobolParser.OpenOutputContext ctx);
	/**
	 * Enter a parse tree produced by {@link CobolParser#openIOStatement}.
	 * @param ctx the parse tree
	 */
	void enterOpenIOStatement(CobolParser.OpenIOStatementContext ctx);
	/**
	 * Exit a parse tree produced by {@link CobolParser#openIOStatement}.
	 * @param ctx the parse tree
	 */
	void exitOpenIOStatement(CobolParser.OpenIOStatementContext ctx);
	/**
	 * Enter a parse tree produced by {@link CobolParser#openExtendStatement}.
	 * @param ctx the parse tree
	 */
	void enterOpenExtendStatement(CobolParser.OpenExtendStatementContext ctx);
	/**
	 * Exit a parse tree produced by {@link CobolParser#openExtendStatement}.
	 * @param ctx the parse tree
	 */
	void exitOpenExtendStatement(CobolParser.OpenExtendStatementContext ctx);
	/**
	 * Enter a parse tree produced by {@link CobolParser#performStatement}.
	 * @param ctx the parse tree
	 */
	void enterPerformStatement(CobolParser.PerformStatementContext ctx);
	/**
	 * Exit a parse tree produced by {@link CobolParser#performStatement}.
	 * @param ctx the parse tree
	 */
	void exitPerformStatement(CobolParser.PerformStatementContext ctx);
	/**
	 * Enter a parse tree produced by {@link CobolParser#performInlineStatement}.
	 * @param ctx the parse tree
	 */
	void enterPerformInlineStatement(CobolParser.PerformInlineStatementContext ctx);
	/**
	 * Exit a parse tree produced by {@link CobolParser#performInlineStatement}.
	 * @param ctx the parse tree
	 */
	void exitPerformInlineStatement(CobolParser.PerformInlineStatementContext ctx);
	/**
	 * Enter a parse tree produced by {@link CobolParser#performProcedureStatement}.
	 * @param ctx the parse tree
	 */
	void enterPerformProcedureStatement(CobolParser.PerformProcedureStatementContext ctx);
	/**
	 * Exit a parse tree produced by {@link CobolParser#performProcedureStatement}.
	 * @param ctx the parse tree
	 */
	void exitPerformProcedureStatement(CobolParser.PerformProcedureStatementContext ctx);
	/**
	 * Enter a parse tree produced by {@link CobolParser#performType}.
	 * @param ctx the parse tree
	 */
	void enterPerformType(CobolParser.PerformTypeContext ctx);
	/**
	 * Exit a parse tree produced by {@link CobolParser#performType}.
	 * @param ctx the parse tree
	 */
	void exitPerformType(CobolParser.PerformTypeContext ctx);
	/**
	 * Enter a parse tree produced by {@link CobolParser#performTimes}.
	 * @param ctx the parse tree
	 */
	void enterPerformTimes(CobolParser.PerformTimesContext ctx);
	/**
	 * Exit a parse tree produced by {@link CobolParser#performTimes}.
	 * @param ctx the parse tree
	 */
	void exitPerformTimes(CobolParser.PerformTimesContext ctx);
	/**
	 * Enter a parse tree produced by {@link CobolParser#performUntil}.
	 * @param ctx the parse tree
	 */
	void enterPerformUntil(CobolParser.PerformUntilContext ctx);
	/**
	 * Exit a parse tree produced by {@link CobolParser#performUntil}.
	 * @param ctx the parse tree
	 */
	void exitPerformUntil(CobolParser.PerformUntilContext ctx);
	/**
	 * Enter a parse tree produced by {@link CobolParser#performVarying}.
	 * @param ctx the parse tree
	 */
	void enterPerformVarying(CobolParser.PerformVaryingContext ctx);
	/**
	 * Exit a parse tree produced by {@link CobolParser#performVarying}.
	 * @param ctx the parse tree
	 */
	void exitPerformVarying(CobolParser.PerformVaryingContext ctx);
	/**
	 * Enter a parse tree produced by {@link CobolParser#performVaryingClause}.
	 * @param ctx the parse tree
	 */
	void enterPerformVaryingClause(CobolParser.PerformVaryingClauseContext ctx);
	/**
	 * Exit a parse tree produced by {@link CobolParser#performVaryingClause}.
	 * @param ctx the parse tree
	 */
	void exitPerformVaryingClause(CobolParser.PerformVaryingClauseContext ctx);
	/**
	 * Enter a parse tree produced by {@link CobolParser#performVaryingPhrase}.
	 * @param ctx the parse tree
	 */
	void enterPerformVaryingPhrase(CobolParser.PerformVaryingPhraseContext ctx);
	/**
	 * Exit a parse tree produced by {@link CobolParser#performVaryingPhrase}.
	 * @param ctx the parse tree
	 */
	void exitPerformVaryingPhrase(CobolParser.PerformVaryingPhraseContext ctx);
	/**
	 * Enter a parse tree produced by {@link CobolParser#performAfter}.
	 * @param ctx the parse tree
	 */
	void enterPerformAfter(CobolParser.PerformAfterContext ctx);
	/**
	 * Exit a parse tree produced by {@link CobolParser#performAfter}.
	 * @param ctx the parse tree
	 */
	void exitPerformAfter(CobolParser.PerformAfterContext ctx);
	/**
	 * Enter a parse tree produced by {@link CobolParser#performFrom}.
	 * @param ctx the parse tree
	 */
	void enterPerformFrom(CobolParser.PerformFromContext ctx);
	/**
	 * Exit a parse tree produced by {@link CobolParser#performFrom}.
	 * @param ctx the parse tree
	 */
	void exitPerformFrom(CobolParser.PerformFromContext ctx);
	/**
	 * Enter a parse tree produced by {@link CobolParser#performBy}.
	 * @param ctx the parse tree
	 */
	void enterPerformBy(CobolParser.PerformByContext ctx);
	/**
	 * Exit a parse tree produced by {@link CobolParser#performBy}.
	 * @param ctx the parse tree
	 */
	void exitPerformBy(CobolParser.PerformByContext ctx);
	/**
	 * Enter a parse tree produced by {@link CobolParser#performTestClause}.
	 * @param ctx the parse tree
	 */
	void enterPerformTestClause(CobolParser.PerformTestClauseContext ctx);
	/**
	 * Exit a parse tree produced by {@link CobolParser#performTestClause}.
	 * @param ctx the parse tree
	 */
	void exitPerformTestClause(CobolParser.PerformTestClauseContext ctx);
	/**
	 * Enter a parse tree produced by {@link CobolParser#purgeStatement}.
	 * @param ctx the parse tree
	 */
	void enterPurgeStatement(CobolParser.PurgeStatementContext ctx);
	/**
	 * Exit a parse tree produced by {@link CobolParser#purgeStatement}.
	 * @param ctx the parse tree
	 */
	void exitPurgeStatement(CobolParser.PurgeStatementContext ctx);
	/**
	 * Enter a parse tree produced by {@link CobolParser#readStatement}.
	 * @param ctx the parse tree
	 */
	void enterReadStatement(CobolParser.ReadStatementContext ctx);
	/**
	 * Exit a parse tree produced by {@link CobolParser#readStatement}.
	 * @param ctx the parse tree
	 */
	void exitReadStatement(CobolParser.ReadStatementContext ctx);
	/**
	 * Enter a parse tree produced by {@link CobolParser#readInto}.
	 * @param ctx the parse tree
	 */
	void enterReadInto(CobolParser.ReadIntoContext ctx);
	/**
	 * Exit a parse tree produced by {@link CobolParser#readInto}.
	 * @param ctx the parse tree
	 */
	void exitReadInto(CobolParser.ReadIntoContext ctx);
	/**
	 * Enter a parse tree produced by {@link CobolParser#readWith}.
	 * @param ctx the parse tree
	 */
	void enterReadWith(CobolParser.ReadWithContext ctx);
	/**
	 * Exit a parse tree produced by {@link CobolParser#readWith}.
	 * @param ctx the parse tree
	 */
	void exitReadWith(CobolParser.ReadWithContext ctx);
	/**
	 * Enter a parse tree produced by {@link CobolParser#readKey}.
	 * @param ctx the parse tree
	 */
	void enterReadKey(CobolParser.ReadKeyContext ctx);
	/**
	 * Exit a parse tree produced by {@link CobolParser#readKey}.
	 * @param ctx the parse tree
	 */
	void exitReadKey(CobolParser.ReadKeyContext ctx);
	/**
	 * Enter a parse tree produced by {@link CobolParser#receiveStatement}.
	 * @param ctx the parse tree
	 */
	void enterReceiveStatement(CobolParser.ReceiveStatementContext ctx);
	/**
	 * Exit a parse tree produced by {@link CobolParser#receiveStatement}.
	 * @param ctx the parse tree
	 */
	void exitReceiveStatement(CobolParser.ReceiveStatementContext ctx);
	/**
	 * Enter a parse tree produced by {@link CobolParser#receiveFromStatement}.
	 * @param ctx the parse tree
	 */
	void enterReceiveFromStatement(CobolParser.ReceiveFromStatementContext ctx);
	/**
	 * Exit a parse tree produced by {@link CobolParser#receiveFromStatement}.
	 * @param ctx the parse tree
	 */
	void exitReceiveFromStatement(CobolParser.ReceiveFromStatementContext ctx);
	/**
	 * Enter a parse tree produced by {@link CobolParser#receiveFrom}.
	 * @param ctx the parse tree
	 */
	void enterReceiveFrom(CobolParser.ReceiveFromContext ctx);
	/**
	 * Exit a parse tree produced by {@link CobolParser#receiveFrom}.
	 * @param ctx the parse tree
	 */
	void exitReceiveFrom(CobolParser.ReceiveFromContext ctx);
	/**
	 * Enter a parse tree produced by {@link CobolParser#receiveIntoStatement}.
	 * @param ctx the parse tree
	 */
	void enterReceiveIntoStatement(CobolParser.ReceiveIntoStatementContext ctx);
	/**
	 * Exit a parse tree produced by {@link CobolParser#receiveIntoStatement}.
	 * @param ctx the parse tree
	 */
	void exitReceiveIntoStatement(CobolParser.ReceiveIntoStatementContext ctx);
	/**
	 * Enter a parse tree produced by {@link CobolParser#receiveNoData}.
	 * @param ctx the parse tree
	 */
	void enterReceiveNoData(CobolParser.ReceiveNoDataContext ctx);
	/**
	 * Exit a parse tree produced by {@link CobolParser#receiveNoData}.
	 * @param ctx the parse tree
	 */
	void exitReceiveNoData(CobolParser.ReceiveNoDataContext ctx);
	/**
	 * Enter a parse tree produced by {@link CobolParser#receiveWithData}.
	 * @param ctx the parse tree
	 */
	void enterReceiveWithData(CobolParser.ReceiveWithDataContext ctx);
	/**
	 * Exit a parse tree produced by {@link CobolParser#receiveWithData}.
	 * @param ctx the parse tree
	 */
	void exitReceiveWithData(CobolParser.ReceiveWithDataContext ctx);
	/**
	 * Enter a parse tree produced by {@link CobolParser#receiveBefore}.
	 * @param ctx the parse tree
	 */
	void enterReceiveBefore(CobolParser.ReceiveBeforeContext ctx);
	/**
	 * Exit a parse tree produced by {@link CobolParser#receiveBefore}.
	 * @param ctx the parse tree
	 */
	void exitReceiveBefore(CobolParser.ReceiveBeforeContext ctx);
	/**
	 * Enter a parse tree produced by {@link CobolParser#receiveWith}.
	 * @param ctx the parse tree
	 */
	void enterReceiveWith(CobolParser.ReceiveWithContext ctx);
	/**
	 * Exit a parse tree produced by {@link CobolParser#receiveWith}.
	 * @param ctx the parse tree
	 */
	void exitReceiveWith(CobolParser.ReceiveWithContext ctx);
	/**
	 * Enter a parse tree produced by {@link CobolParser#receiveThread}.
	 * @param ctx the parse tree
	 */
	void enterReceiveThread(CobolParser.ReceiveThreadContext ctx);
	/**
	 * Exit a parse tree produced by {@link CobolParser#receiveThread}.
	 * @param ctx the parse tree
	 */
	void exitReceiveThread(CobolParser.ReceiveThreadContext ctx);
	/**
	 * Enter a parse tree produced by {@link CobolParser#receiveSize}.
	 * @param ctx the parse tree
	 */
	void enterReceiveSize(CobolParser.ReceiveSizeContext ctx);
	/**
	 * Exit a parse tree produced by {@link CobolParser#receiveSize}.
	 * @param ctx the parse tree
	 */
	void exitReceiveSize(CobolParser.ReceiveSizeContext ctx);
	/**
	 * Enter a parse tree produced by {@link CobolParser#receiveStatus}.
	 * @param ctx the parse tree
	 */
	void enterReceiveStatus(CobolParser.ReceiveStatusContext ctx);
	/**
	 * Exit a parse tree produced by {@link CobolParser#receiveStatus}.
	 * @param ctx the parse tree
	 */
	void exitReceiveStatus(CobolParser.ReceiveStatusContext ctx);
	/**
	 * Enter a parse tree produced by {@link CobolParser#releaseStatement}.
	 * @param ctx the parse tree
	 */
	void enterReleaseStatement(CobolParser.ReleaseStatementContext ctx);
	/**
	 * Exit a parse tree produced by {@link CobolParser#releaseStatement}.
	 * @param ctx the parse tree
	 */
	void exitReleaseStatement(CobolParser.ReleaseStatementContext ctx);
	/**
	 * Enter a parse tree produced by {@link CobolParser#returnStatement}.
	 * @param ctx the parse tree
	 */
	void enterReturnStatement(CobolParser.ReturnStatementContext ctx);
	/**
	 * Exit a parse tree produced by {@link CobolParser#returnStatement}.
	 * @param ctx the parse tree
	 */
	void exitReturnStatement(CobolParser.ReturnStatementContext ctx);
	/**
	 * Enter a parse tree produced by {@link CobolParser#returnInto}.
	 * @param ctx the parse tree
	 */
	void enterReturnInto(CobolParser.ReturnIntoContext ctx);
	/**
	 * Exit a parse tree produced by {@link CobolParser#returnInto}.
	 * @param ctx the parse tree
	 */
	void exitReturnInto(CobolParser.ReturnIntoContext ctx);
	/**
	 * Enter a parse tree produced by {@link CobolParser#rewriteStatement}.
	 * @param ctx the parse tree
	 */
	void enterRewriteStatement(CobolParser.RewriteStatementContext ctx);
	/**
	 * Exit a parse tree produced by {@link CobolParser#rewriteStatement}.
	 * @param ctx the parse tree
	 */
	void exitRewriteStatement(CobolParser.RewriteStatementContext ctx);
	/**
	 * Enter a parse tree produced by {@link CobolParser#rewriteFrom}.
	 * @param ctx the parse tree
	 */
	void enterRewriteFrom(CobolParser.RewriteFromContext ctx);
	/**
	 * Exit a parse tree produced by {@link CobolParser#rewriteFrom}.
	 * @param ctx the parse tree
	 */
	void exitRewriteFrom(CobolParser.RewriteFromContext ctx);
	/**
	 * Enter a parse tree produced by {@link CobolParser#searchStatement}.
	 * @param ctx the parse tree
	 */
	void enterSearchStatement(CobolParser.SearchStatementContext ctx);
	/**
	 * Exit a parse tree produced by {@link CobolParser#searchStatement}.
	 * @param ctx the parse tree
	 */
	void exitSearchStatement(CobolParser.SearchStatementContext ctx);
	/**
	 * Enter a parse tree produced by {@link CobolParser#searchVarying}.
	 * @param ctx the parse tree
	 */
	void enterSearchVarying(CobolParser.SearchVaryingContext ctx);
	/**
	 * Exit a parse tree produced by {@link CobolParser#searchVarying}.
	 * @param ctx the parse tree
	 */
	void exitSearchVarying(CobolParser.SearchVaryingContext ctx);
	/**
	 * Enter a parse tree produced by {@link CobolParser#searchWhen}.
	 * @param ctx the parse tree
	 */
	void enterSearchWhen(CobolParser.SearchWhenContext ctx);
	/**
	 * Exit a parse tree produced by {@link CobolParser#searchWhen}.
	 * @param ctx the parse tree
	 */
	void exitSearchWhen(CobolParser.SearchWhenContext ctx);
	/**
	 * Enter a parse tree produced by {@link CobolParser#sendStatement}.
	 * @param ctx the parse tree
	 */
	void enterSendStatement(CobolParser.SendStatementContext ctx);
	/**
	 * Exit a parse tree produced by {@link CobolParser#sendStatement}.
	 * @param ctx the parse tree
	 */
	void exitSendStatement(CobolParser.SendStatementContext ctx);
	/**
	 * Enter a parse tree produced by {@link CobolParser#sendStatementSync}.
	 * @param ctx the parse tree
	 */
	void enterSendStatementSync(CobolParser.SendStatementSyncContext ctx);
	/**
	 * Exit a parse tree produced by {@link CobolParser#sendStatementSync}.
	 * @param ctx the parse tree
	 */
	void exitSendStatementSync(CobolParser.SendStatementSyncContext ctx);
	/**
	 * Enter a parse tree produced by {@link CobolParser#sendStatementAsync}.
	 * @param ctx the parse tree
	 */
	void enterSendStatementAsync(CobolParser.SendStatementAsyncContext ctx);
	/**
	 * Exit a parse tree produced by {@link CobolParser#sendStatementAsync}.
	 * @param ctx the parse tree
	 */
	void exitSendStatementAsync(CobolParser.SendStatementAsyncContext ctx);
	/**
	 * Enter a parse tree produced by {@link CobolParser#sendFromPhrase}.
	 * @param ctx the parse tree
	 */
	void enterSendFromPhrase(CobolParser.SendFromPhraseContext ctx);
	/**
	 * Exit a parse tree produced by {@link CobolParser#sendFromPhrase}.
	 * @param ctx the parse tree
	 */
	void exitSendFromPhrase(CobolParser.SendFromPhraseContext ctx);
	/**
	 * Enter a parse tree produced by {@link CobolParser#sendWithPhrase}.
	 * @param ctx the parse tree
	 */
	void enterSendWithPhrase(CobolParser.SendWithPhraseContext ctx);
	/**
	 * Exit a parse tree produced by {@link CobolParser#sendWithPhrase}.
	 * @param ctx the parse tree
	 */
	void exitSendWithPhrase(CobolParser.SendWithPhraseContext ctx);
	/**
	 * Enter a parse tree produced by {@link CobolParser#sendReplacingPhrase}.
	 * @param ctx the parse tree
	 */
	void enterSendReplacingPhrase(CobolParser.SendReplacingPhraseContext ctx);
	/**
	 * Exit a parse tree produced by {@link CobolParser#sendReplacingPhrase}.
	 * @param ctx the parse tree
	 */
	void exitSendReplacingPhrase(CobolParser.SendReplacingPhraseContext ctx);
	/**
	 * Enter a parse tree produced by {@link CobolParser#sendAdvancingPhrase}.
	 * @param ctx the parse tree
	 */
	void enterSendAdvancingPhrase(CobolParser.SendAdvancingPhraseContext ctx);
	/**
	 * Exit a parse tree produced by {@link CobolParser#sendAdvancingPhrase}.
	 * @param ctx the parse tree
	 */
	void exitSendAdvancingPhrase(CobolParser.SendAdvancingPhraseContext ctx);
	/**
	 * Enter a parse tree produced by {@link CobolParser#sendAdvancingPage}.
	 * @param ctx the parse tree
	 */
	void enterSendAdvancingPage(CobolParser.SendAdvancingPageContext ctx);
	/**
	 * Exit a parse tree produced by {@link CobolParser#sendAdvancingPage}.
	 * @param ctx the parse tree
	 */
	void exitSendAdvancingPage(CobolParser.SendAdvancingPageContext ctx);
	/**
	 * Enter a parse tree produced by {@link CobolParser#sendAdvancingLines}.
	 * @param ctx the parse tree
	 */
	void enterSendAdvancingLines(CobolParser.SendAdvancingLinesContext ctx);
	/**
	 * Exit a parse tree produced by {@link CobolParser#sendAdvancingLines}.
	 * @param ctx the parse tree
	 */
	void exitSendAdvancingLines(CobolParser.SendAdvancingLinesContext ctx);
	/**
	 * Enter a parse tree produced by {@link CobolParser#sendAdvancingMnemonic}.
	 * @param ctx the parse tree
	 */
	void enterSendAdvancingMnemonic(CobolParser.SendAdvancingMnemonicContext ctx);
	/**
	 * Exit a parse tree produced by {@link CobolParser#sendAdvancingMnemonic}.
	 * @param ctx the parse tree
	 */
	void exitSendAdvancingMnemonic(CobolParser.SendAdvancingMnemonicContext ctx);
	/**
	 * Enter a parse tree produced by {@link CobolParser#setStatement}.
	 * @param ctx the parse tree
	 */
	void enterSetStatement(CobolParser.SetStatementContext ctx);
	/**
	 * Exit a parse tree produced by {@link CobolParser#setStatement}.
	 * @param ctx the parse tree
	 */
	void exitSetStatement(CobolParser.SetStatementContext ctx);
	/**
	 * Enter a parse tree produced by {@link CobolParser#setToStatement}.
	 * @param ctx the parse tree
	 */
	void enterSetToStatement(CobolParser.SetToStatementContext ctx);
	/**
	 * Exit a parse tree produced by {@link CobolParser#setToStatement}.
	 * @param ctx the parse tree
	 */
	void exitSetToStatement(CobolParser.SetToStatementContext ctx);
	/**
	 * Enter a parse tree produced by {@link CobolParser#setUpDownByStatement}.
	 * @param ctx the parse tree
	 */
	void enterSetUpDownByStatement(CobolParser.SetUpDownByStatementContext ctx);
	/**
	 * Exit a parse tree produced by {@link CobolParser#setUpDownByStatement}.
	 * @param ctx the parse tree
	 */
	void exitSetUpDownByStatement(CobolParser.SetUpDownByStatementContext ctx);
	/**
	 * Enter a parse tree produced by {@link CobolParser#setTo}.
	 * @param ctx the parse tree
	 */
	void enterSetTo(CobolParser.SetToContext ctx);
	/**
	 * Exit a parse tree produced by {@link CobolParser#setTo}.
	 * @param ctx the parse tree
	 */
	void exitSetTo(CobolParser.SetToContext ctx);
	/**
	 * Enter a parse tree produced by {@link CobolParser#setToValue}.
	 * @param ctx the parse tree
	 */
	void enterSetToValue(CobolParser.SetToValueContext ctx);
	/**
	 * Exit a parse tree produced by {@link CobolParser#setToValue}.
	 * @param ctx the parse tree
	 */
	void exitSetToValue(CobolParser.SetToValueContext ctx);
	/**
	 * Enter a parse tree produced by {@link CobolParser#setByValue}.
	 * @param ctx the parse tree
	 */
	void enterSetByValue(CobolParser.SetByValueContext ctx);
	/**
	 * Exit a parse tree produced by {@link CobolParser#setByValue}.
	 * @param ctx the parse tree
	 */
	void exitSetByValue(CobolParser.SetByValueContext ctx);
	/**
	 * Enter a parse tree produced by {@link CobolParser#sortStatement}.
	 * @param ctx the parse tree
	 */
	void enterSortStatement(CobolParser.SortStatementContext ctx);
	/**
	 * Exit a parse tree produced by {@link CobolParser#sortStatement}.
	 * @param ctx the parse tree
	 */
	void exitSortStatement(CobolParser.SortStatementContext ctx);
	/**
	 * Enter a parse tree produced by {@link CobolParser#sortOnKeyClause}.
	 * @param ctx the parse tree
	 */
	void enterSortOnKeyClause(CobolParser.SortOnKeyClauseContext ctx);
	/**
	 * Exit a parse tree produced by {@link CobolParser#sortOnKeyClause}.
	 * @param ctx the parse tree
	 */
	void exitSortOnKeyClause(CobolParser.SortOnKeyClauseContext ctx);
	/**
	 * Enter a parse tree produced by {@link CobolParser#sortDuplicatesPhrase}.
	 * @param ctx the parse tree
	 */
	void enterSortDuplicatesPhrase(CobolParser.SortDuplicatesPhraseContext ctx);
	/**
	 * Exit a parse tree produced by {@link CobolParser#sortDuplicatesPhrase}.
	 * @param ctx the parse tree
	 */
	void exitSortDuplicatesPhrase(CobolParser.SortDuplicatesPhraseContext ctx);
	/**
	 * Enter a parse tree produced by {@link CobolParser#sortCollatingSequencePhrase}.
	 * @param ctx the parse tree
	 */
	void enterSortCollatingSequencePhrase(CobolParser.SortCollatingSequencePhraseContext ctx);
	/**
	 * Exit a parse tree produced by {@link CobolParser#sortCollatingSequencePhrase}.
	 * @param ctx the parse tree
	 */
	void exitSortCollatingSequencePhrase(CobolParser.SortCollatingSequencePhraseContext ctx);
	/**
	 * Enter a parse tree produced by {@link CobolParser#sortCollatingAlphanumeric}.
	 * @param ctx the parse tree
	 */
	void enterSortCollatingAlphanumeric(CobolParser.SortCollatingAlphanumericContext ctx);
	/**
	 * Exit a parse tree produced by {@link CobolParser#sortCollatingAlphanumeric}.
	 * @param ctx the parse tree
	 */
	void exitSortCollatingAlphanumeric(CobolParser.SortCollatingAlphanumericContext ctx);
	/**
	 * Enter a parse tree produced by {@link CobolParser#sortCollatingNational}.
	 * @param ctx the parse tree
	 */
	void enterSortCollatingNational(CobolParser.SortCollatingNationalContext ctx);
	/**
	 * Exit a parse tree produced by {@link CobolParser#sortCollatingNational}.
	 * @param ctx the parse tree
	 */
	void exitSortCollatingNational(CobolParser.SortCollatingNationalContext ctx);
	/**
	 * Enter a parse tree produced by {@link CobolParser#sortInputProcedurePhrase}.
	 * @param ctx the parse tree
	 */
	void enterSortInputProcedurePhrase(CobolParser.SortInputProcedurePhraseContext ctx);
	/**
	 * Exit a parse tree produced by {@link CobolParser#sortInputProcedurePhrase}.
	 * @param ctx the parse tree
	 */
	void exitSortInputProcedurePhrase(CobolParser.SortInputProcedurePhraseContext ctx);
	/**
	 * Enter a parse tree produced by {@link CobolParser#sortInputThrough}.
	 * @param ctx the parse tree
	 */
	void enterSortInputThrough(CobolParser.SortInputThroughContext ctx);
	/**
	 * Exit a parse tree produced by {@link CobolParser#sortInputThrough}.
	 * @param ctx the parse tree
	 */
	void exitSortInputThrough(CobolParser.SortInputThroughContext ctx);
	/**
	 * Enter a parse tree produced by {@link CobolParser#sortUsing}.
	 * @param ctx the parse tree
	 */
	void enterSortUsing(CobolParser.SortUsingContext ctx);
	/**
	 * Exit a parse tree produced by {@link CobolParser#sortUsing}.
	 * @param ctx the parse tree
	 */
	void exitSortUsing(CobolParser.SortUsingContext ctx);
	/**
	 * Enter a parse tree produced by {@link CobolParser#sortOutputProcedurePhrase}.
	 * @param ctx the parse tree
	 */
	void enterSortOutputProcedurePhrase(CobolParser.SortOutputProcedurePhraseContext ctx);
	/**
	 * Exit a parse tree produced by {@link CobolParser#sortOutputProcedurePhrase}.
	 * @param ctx the parse tree
	 */
	void exitSortOutputProcedurePhrase(CobolParser.SortOutputProcedurePhraseContext ctx);
	/**
	 * Enter a parse tree produced by {@link CobolParser#sortOutputThrough}.
	 * @param ctx the parse tree
	 */
	void enterSortOutputThrough(CobolParser.SortOutputThroughContext ctx);
	/**
	 * Exit a parse tree produced by {@link CobolParser#sortOutputThrough}.
	 * @param ctx the parse tree
	 */
	void exitSortOutputThrough(CobolParser.SortOutputThroughContext ctx);
	/**
	 * Enter a parse tree produced by {@link CobolParser#sortGivingPhrase}.
	 * @param ctx the parse tree
	 */
	void enterSortGivingPhrase(CobolParser.SortGivingPhraseContext ctx);
	/**
	 * Exit a parse tree produced by {@link CobolParser#sortGivingPhrase}.
	 * @param ctx the parse tree
	 */
	void exitSortGivingPhrase(CobolParser.SortGivingPhraseContext ctx);
	/**
	 * Enter a parse tree produced by {@link CobolParser#sortGiving}.
	 * @param ctx the parse tree
	 */
	void enterSortGiving(CobolParser.SortGivingContext ctx);
	/**
	 * Exit a parse tree produced by {@link CobolParser#sortGiving}.
	 * @param ctx the parse tree
	 */
	void exitSortGiving(CobolParser.SortGivingContext ctx);
	/**
	 * Enter a parse tree produced by {@link CobolParser#startStatement}.
	 * @param ctx the parse tree
	 */
	void enterStartStatement(CobolParser.StartStatementContext ctx);
	/**
	 * Exit a parse tree produced by {@link CobolParser#startStatement}.
	 * @param ctx the parse tree
	 */
	void exitStartStatement(CobolParser.StartStatementContext ctx);
	/**
	 * Enter a parse tree produced by {@link CobolParser#startKey}.
	 * @param ctx the parse tree
	 */
	void enterStartKey(CobolParser.StartKeyContext ctx);
	/**
	 * Exit a parse tree produced by {@link CobolParser#startKey}.
	 * @param ctx the parse tree
	 */
	void exitStartKey(CobolParser.StartKeyContext ctx);
	/**
	 * Enter a parse tree produced by {@link CobolParser#stopStatement}.
	 * @param ctx the parse tree
	 */
	void enterStopStatement(CobolParser.StopStatementContext ctx);
	/**
	 * Exit a parse tree produced by {@link CobolParser#stopStatement}.
	 * @param ctx the parse tree
	 */
	void exitStopStatement(CobolParser.StopStatementContext ctx);
	/**
	 * Enter a parse tree produced by {@link CobolParser#stopStatementGiving}.
	 * @param ctx the parse tree
	 */
	void enterStopStatementGiving(CobolParser.StopStatementGivingContext ctx);
	/**
	 * Exit a parse tree produced by {@link CobolParser#stopStatementGiving}.
	 * @param ctx the parse tree
	 */
	void exitStopStatementGiving(CobolParser.StopStatementGivingContext ctx);
	/**
	 * Enter a parse tree produced by {@link CobolParser#stringStatement}.
	 * @param ctx the parse tree
	 */
	void enterStringStatement(CobolParser.StringStatementContext ctx);
	/**
	 * Exit a parse tree produced by {@link CobolParser#stringStatement}.
	 * @param ctx the parse tree
	 */
	void exitStringStatement(CobolParser.StringStatementContext ctx);
	/**
	 * Enter a parse tree produced by {@link CobolParser#stringSendingPhrase}.
	 * @param ctx the parse tree
	 */
	void enterStringSendingPhrase(CobolParser.StringSendingPhraseContext ctx);
	/**
	 * Exit a parse tree produced by {@link CobolParser#stringSendingPhrase}.
	 * @param ctx the parse tree
	 */
	void exitStringSendingPhrase(CobolParser.StringSendingPhraseContext ctx);
	/**
	 * Enter a parse tree produced by {@link CobolParser#stringSending}.
	 * @param ctx the parse tree
	 */
	void enterStringSending(CobolParser.StringSendingContext ctx);
	/**
	 * Exit a parse tree produced by {@link CobolParser#stringSending}.
	 * @param ctx the parse tree
	 */
	void exitStringSending(CobolParser.StringSendingContext ctx);
	/**
	 * Enter a parse tree produced by {@link CobolParser#stringDelimitedByPhrase}.
	 * @param ctx the parse tree
	 */
	void enterStringDelimitedByPhrase(CobolParser.StringDelimitedByPhraseContext ctx);
	/**
	 * Exit a parse tree produced by {@link CobolParser#stringDelimitedByPhrase}.
	 * @param ctx the parse tree
	 */
	void exitStringDelimitedByPhrase(CobolParser.StringDelimitedByPhraseContext ctx);
	/**
	 * Enter a parse tree produced by {@link CobolParser#stringForPhrase}.
	 * @param ctx the parse tree
	 */
	void enterStringForPhrase(CobolParser.StringForPhraseContext ctx);
	/**
	 * Exit a parse tree produced by {@link CobolParser#stringForPhrase}.
	 * @param ctx the parse tree
	 */
	void exitStringForPhrase(CobolParser.StringForPhraseContext ctx);
	/**
	 * Enter a parse tree produced by {@link CobolParser#stringIntoPhrase}.
	 * @param ctx the parse tree
	 */
	void enterStringIntoPhrase(CobolParser.StringIntoPhraseContext ctx);
	/**
	 * Exit a parse tree produced by {@link CobolParser#stringIntoPhrase}.
	 * @param ctx the parse tree
	 */
	void exitStringIntoPhrase(CobolParser.StringIntoPhraseContext ctx);
	/**
	 * Enter a parse tree produced by {@link CobolParser#stringWithPointerPhrase}.
	 * @param ctx the parse tree
	 */
	void enterStringWithPointerPhrase(CobolParser.StringWithPointerPhraseContext ctx);
	/**
	 * Exit a parse tree produced by {@link CobolParser#stringWithPointerPhrase}.
	 * @param ctx the parse tree
	 */
	void exitStringWithPointerPhrase(CobolParser.StringWithPointerPhraseContext ctx);
	/**
	 * Enter a parse tree produced by {@link CobolParser#subtractStatement}.
	 * @param ctx the parse tree
	 */
	void enterSubtractStatement(CobolParser.SubtractStatementContext ctx);
	/**
	 * Exit a parse tree produced by {@link CobolParser#subtractStatement}.
	 * @param ctx the parse tree
	 */
	void exitSubtractStatement(CobolParser.SubtractStatementContext ctx);
	/**
	 * Enter a parse tree produced by {@link CobolParser#subtractFromStatement}.
	 * @param ctx the parse tree
	 */
	void enterSubtractFromStatement(CobolParser.SubtractFromStatementContext ctx);
	/**
	 * Exit a parse tree produced by {@link CobolParser#subtractFromStatement}.
	 * @param ctx the parse tree
	 */
	void exitSubtractFromStatement(CobolParser.SubtractFromStatementContext ctx);
	/**
	 * Enter a parse tree produced by {@link CobolParser#subtractFromGivingStatement}.
	 * @param ctx the parse tree
	 */
	void enterSubtractFromGivingStatement(CobolParser.SubtractFromGivingStatementContext ctx);
	/**
	 * Exit a parse tree produced by {@link CobolParser#subtractFromGivingStatement}.
	 * @param ctx the parse tree
	 */
	void exitSubtractFromGivingStatement(CobolParser.SubtractFromGivingStatementContext ctx);
	/**
	 * Enter a parse tree produced by {@link CobolParser#subtractCorrespondingStatement}.
	 * @param ctx the parse tree
	 */
	void enterSubtractCorrespondingStatement(CobolParser.SubtractCorrespondingStatementContext ctx);
	/**
	 * Exit a parse tree produced by {@link CobolParser#subtractCorrespondingStatement}.
	 * @param ctx the parse tree
	 */
	void exitSubtractCorrespondingStatement(CobolParser.SubtractCorrespondingStatementContext ctx);
	/**
	 * Enter a parse tree produced by {@link CobolParser#subtractSubtrahend}.
	 * @param ctx the parse tree
	 */
	void enterSubtractSubtrahend(CobolParser.SubtractSubtrahendContext ctx);
	/**
	 * Exit a parse tree produced by {@link CobolParser#subtractSubtrahend}.
	 * @param ctx the parse tree
	 */
	void exitSubtractSubtrahend(CobolParser.SubtractSubtrahendContext ctx);
	/**
	 * Enter a parse tree produced by {@link CobolParser#subtractMinuend}.
	 * @param ctx the parse tree
	 */
	void enterSubtractMinuend(CobolParser.SubtractMinuendContext ctx);
	/**
	 * Exit a parse tree produced by {@link CobolParser#subtractMinuend}.
	 * @param ctx the parse tree
	 */
	void exitSubtractMinuend(CobolParser.SubtractMinuendContext ctx);
	/**
	 * Enter a parse tree produced by {@link CobolParser#subtractMinuendGiving}.
	 * @param ctx the parse tree
	 */
	void enterSubtractMinuendGiving(CobolParser.SubtractMinuendGivingContext ctx);
	/**
	 * Exit a parse tree produced by {@link CobolParser#subtractMinuendGiving}.
	 * @param ctx the parse tree
	 */
	void exitSubtractMinuendGiving(CobolParser.SubtractMinuendGivingContext ctx);
	/**
	 * Enter a parse tree produced by {@link CobolParser#subtractGiving}.
	 * @param ctx the parse tree
	 */
	void enterSubtractGiving(CobolParser.SubtractGivingContext ctx);
	/**
	 * Exit a parse tree produced by {@link CobolParser#subtractGiving}.
	 * @param ctx the parse tree
	 */
	void exitSubtractGiving(CobolParser.SubtractGivingContext ctx);
	/**
	 * Enter a parse tree produced by {@link CobolParser#subtractMinuendCorresponding}.
	 * @param ctx the parse tree
	 */
	void enterSubtractMinuendCorresponding(CobolParser.SubtractMinuendCorrespondingContext ctx);
	/**
	 * Exit a parse tree produced by {@link CobolParser#subtractMinuendCorresponding}.
	 * @param ctx the parse tree
	 */
	void exitSubtractMinuendCorresponding(CobolParser.SubtractMinuendCorrespondingContext ctx);
	/**
	 * Enter a parse tree produced by {@link CobolParser#terminateStatement}.
	 * @param ctx the parse tree
	 */
	void enterTerminateStatement(CobolParser.TerminateStatementContext ctx);
	/**
	 * Exit a parse tree produced by {@link CobolParser#terminateStatement}.
	 * @param ctx the parse tree
	 */
	void exitTerminateStatement(CobolParser.TerminateStatementContext ctx);
	/**
	 * Enter a parse tree produced by {@link CobolParser#unstringStatement}.
	 * @param ctx the parse tree
	 */
	void enterUnstringStatement(CobolParser.UnstringStatementContext ctx);
	/**
	 * Exit a parse tree produced by {@link CobolParser#unstringStatement}.
	 * @param ctx the parse tree
	 */
	void exitUnstringStatement(CobolParser.UnstringStatementContext ctx);
	/**
	 * Enter a parse tree produced by {@link CobolParser#unstringSendingPhrase}.
	 * @param ctx the parse tree
	 */
	void enterUnstringSendingPhrase(CobolParser.UnstringSendingPhraseContext ctx);
	/**
	 * Exit a parse tree produced by {@link CobolParser#unstringSendingPhrase}.
	 * @param ctx the parse tree
	 */
	void exitUnstringSendingPhrase(CobolParser.UnstringSendingPhraseContext ctx);
	/**
	 * Enter a parse tree produced by {@link CobolParser#unstringDelimitedByPhrase}.
	 * @param ctx the parse tree
	 */
	void enterUnstringDelimitedByPhrase(CobolParser.UnstringDelimitedByPhraseContext ctx);
	/**
	 * Exit a parse tree produced by {@link CobolParser#unstringDelimitedByPhrase}.
	 * @param ctx the parse tree
	 */
	void exitUnstringDelimitedByPhrase(CobolParser.UnstringDelimitedByPhraseContext ctx);
	/**
	 * Enter a parse tree produced by {@link CobolParser#unstringOrAllPhrase}.
	 * @param ctx the parse tree
	 */
	void enterUnstringOrAllPhrase(CobolParser.UnstringOrAllPhraseContext ctx);
	/**
	 * Exit a parse tree produced by {@link CobolParser#unstringOrAllPhrase}.
	 * @param ctx the parse tree
	 */
	void exitUnstringOrAllPhrase(CobolParser.UnstringOrAllPhraseContext ctx);
	/**
	 * Enter a parse tree produced by {@link CobolParser#unstringIntoPhrase}.
	 * @param ctx the parse tree
	 */
	void enterUnstringIntoPhrase(CobolParser.UnstringIntoPhraseContext ctx);
	/**
	 * Exit a parse tree produced by {@link CobolParser#unstringIntoPhrase}.
	 * @param ctx the parse tree
	 */
	void exitUnstringIntoPhrase(CobolParser.UnstringIntoPhraseContext ctx);
	/**
	 * Enter a parse tree produced by {@link CobolParser#unstringInto}.
	 * @param ctx the parse tree
	 */
	void enterUnstringInto(CobolParser.UnstringIntoContext ctx);
	/**
	 * Exit a parse tree produced by {@link CobolParser#unstringInto}.
	 * @param ctx the parse tree
	 */
	void exitUnstringInto(CobolParser.UnstringIntoContext ctx);
	/**
	 * Enter a parse tree produced by {@link CobolParser#unstringDelimiterIn}.
	 * @param ctx the parse tree
	 */
	void enterUnstringDelimiterIn(CobolParser.UnstringDelimiterInContext ctx);
	/**
	 * Exit a parse tree produced by {@link CobolParser#unstringDelimiterIn}.
	 * @param ctx the parse tree
	 */
	void exitUnstringDelimiterIn(CobolParser.UnstringDelimiterInContext ctx);
	/**
	 * Enter a parse tree produced by {@link CobolParser#unstringCountIn}.
	 * @param ctx the parse tree
	 */
	void enterUnstringCountIn(CobolParser.UnstringCountInContext ctx);
	/**
	 * Exit a parse tree produced by {@link CobolParser#unstringCountIn}.
	 * @param ctx the parse tree
	 */
	void exitUnstringCountIn(CobolParser.UnstringCountInContext ctx);
	/**
	 * Enter a parse tree produced by {@link CobolParser#unstringWithPointerPhrase}.
	 * @param ctx the parse tree
	 */
	void enterUnstringWithPointerPhrase(CobolParser.UnstringWithPointerPhraseContext ctx);
	/**
	 * Exit a parse tree produced by {@link CobolParser#unstringWithPointerPhrase}.
	 * @param ctx the parse tree
	 */
	void exitUnstringWithPointerPhrase(CobolParser.UnstringWithPointerPhraseContext ctx);
	/**
	 * Enter a parse tree produced by {@link CobolParser#unstringTallyingPhrase}.
	 * @param ctx the parse tree
	 */
	void enterUnstringTallyingPhrase(CobolParser.UnstringTallyingPhraseContext ctx);
	/**
	 * Exit a parse tree produced by {@link CobolParser#unstringTallyingPhrase}.
	 * @param ctx the parse tree
	 */
	void exitUnstringTallyingPhrase(CobolParser.UnstringTallyingPhraseContext ctx);
	/**
	 * Enter a parse tree produced by {@link CobolParser#useStatement}.
	 * @param ctx the parse tree
	 */
	void enterUseStatement(CobolParser.UseStatementContext ctx);
	/**
	 * Exit a parse tree produced by {@link CobolParser#useStatement}.
	 * @param ctx the parse tree
	 */
	void exitUseStatement(CobolParser.UseStatementContext ctx);
	/**
	 * Enter a parse tree produced by {@link CobolParser#useAfterClause}.
	 * @param ctx the parse tree
	 */
	void enterUseAfterClause(CobolParser.UseAfterClauseContext ctx);
	/**
	 * Exit a parse tree produced by {@link CobolParser#useAfterClause}.
	 * @param ctx the parse tree
	 */
	void exitUseAfterClause(CobolParser.UseAfterClauseContext ctx);
	/**
	 * Enter a parse tree produced by {@link CobolParser#useAfterOn}.
	 * @param ctx the parse tree
	 */
	void enterUseAfterOn(CobolParser.UseAfterOnContext ctx);
	/**
	 * Exit a parse tree produced by {@link CobolParser#useAfterOn}.
	 * @param ctx the parse tree
	 */
	void exitUseAfterOn(CobolParser.UseAfterOnContext ctx);
	/**
	 * Enter a parse tree produced by {@link CobolParser#useDebugClause}.
	 * @param ctx the parse tree
	 */
	void enterUseDebugClause(CobolParser.UseDebugClauseContext ctx);
	/**
	 * Exit a parse tree produced by {@link CobolParser#useDebugClause}.
	 * @param ctx the parse tree
	 */
	void exitUseDebugClause(CobolParser.UseDebugClauseContext ctx);
	/**
	 * Enter a parse tree produced by {@link CobolParser#useDebugOn}.
	 * @param ctx the parse tree
	 */
	void enterUseDebugOn(CobolParser.UseDebugOnContext ctx);
	/**
	 * Exit a parse tree produced by {@link CobolParser#useDebugOn}.
	 * @param ctx the parse tree
	 */
	void exitUseDebugOn(CobolParser.UseDebugOnContext ctx);
	/**
	 * Enter a parse tree produced by {@link CobolParser#writeStatement}.
	 * @param ctx the parse tree
	 */
	void enterWriteStatement(CobolParser.WriteStatementContext ctx);
	/**
	 * Exit a parse tree produced by {@link CobolParser#writeStatement}.
	 * @param ctx the parse tree
	 */
	void exitWriteStatement(CobolParser.WriteStatementContext ctx);
	/**
	 * Enter a parse tree produced by {@link CobolParser#writeFromPhrase}.
	 * @param ctx the parse tree
	 */
	void enterWriteFromPhrase(CobolParser.WriteFromPhraseContext ctx);
	/**
	 * Exit a parse tree produced by {@link CobolParser#writeFromPhrase}.
	 * @param ctx the parse tree
	 */
	void exitWriteFromPhrase(CobolParser.WriteFromPhraseContext ctx);
	/**
	 * Enter a parse tree produced by {@link CobolParser#writeAdvancingPhrase}.
	 * @param ctx the parse tree
	 */
	void enterWriteAdvancingPhrase(CobolParser.WriteAdvancingPhraseContext ctx);
	/**
	 * Exit a parse tree produced by {@link CobolParser#writeAdvancingPhrase}.
	 * @param ctx the parse tree
	 */
	void exitWriteAdvancingPhrase(CobolParser.WriteAdvancingPhraseContext ctx);
	/**
	 * Enter a parse tree produced by {@link CobolParser#writeAdvancingPage}.
	 * @param ctx the parse tree
	 */
	void enterWriteAdvancingPage(CobolParser.WriteAdvancingPageContext ctx);
	/**
	 * Exit a parse tree produced by {@link CobolParser#writeAdvancingPage}.
	 * @param ctx the parse tree
	 */
	void exitWriteAdvancingPage(CobolParser.WriteAdvancingPageContext ctx);
	/**
	 * Enter a parse tree produced by {@link CobolParser#writeAdvancingLines}.
	 * @param ctx the parse tree
	 */
	void enterWriteAdvancingLines(CobolParser.WriteAdvancingLinesContext ctx);
	/**
	 * Exit a parse tree produced by {@link CobolParser#writeAdvancingLines}.
	 * @param ctx the parse tree
	 */
	void exitWriteAdvancingLines(CobolParser.WriteAdvancingLinesContext ctx);
	/**
	 * Enter a parse tree produced by {@link CobolParser#writeAdvancingMnemonic}.
	 * @param ctx the parse tree
	 */
	void enterWriteAdvancingMnemonic(CobolParser.WriteAdvancingMnemonicContext ctx);
	/**
	 * Exit a parse tree produced by {@link CobolParser#writeAdvancingMnemonic}.
	 * @param ctx the parse tree
	 */
	void exitWriteAdvancingMnemonic(CobolParser.WriteAdvancingMnemonicContext ctx);
	/**
	 * Enter a parse tree produced by {@link CobolParser#writeAtEndOfPagePhrase}.
	 * @param ctx the parse tree
	 */
	void enterWriteAtEndOfPagePhrase(CobolParser.WriteAtEndOfPagePhraseContext ctx);
	/**
	 * Exit a parse tree produced by {@link CobolParser#writeAtEndOfPagePhrase}.
	 * @param ctx the parse tree
	 */
	void exitWriteAtEndOfPagePhrase(CobolParser.WriteAtEndOfPagePhraseContext ctx);
	/**
	 * Enter a parse tree produced by {@link CobolParser#writeNotAtEndOfPagePhrase}.
	 * @param ctx the parse tree
	 */
	void enterWriteNotAtEndOfPagePhrase(CobolParser.WriteNotAtEndOfPagePhraseContext ctx);
	/**
	 * Exit a parse tree produced by {@link CobolParser#writeNotAtEndOfPagePhrase}.
	 * @param ctx the parse tree
	 */
	void exitWriteNotAtEndOfPagePhrase(CobolParser.WriteNotAtEndOfPagePhraseContext ctx);
	/**
	 * Enter a parse tree produced by {@link CobolParser#atEndPhrase}.
	 * @param ctx the parse tree
	 */
	void enterAtEndPhrase(CobolParser.AtEndPhraseContext ctx);
	/**
	 * Exit a parse tree produced by {@link CobolParser#atEndPhrase}.
	 * @param ctx the parse tree
	 */
	void exitAtEndPhrase(CobolParser.AtEndPhraseContext ctx);
	/**
	 * Enter a parse tree produced by {@link CobolParser#notAtEndPhrase}.
	 * @param ctx the parse tree
	 */
	void enterNotAtEndPhrase(CobolParser.NotAtEndPhraseContext ctx);
	/**
	 * Exit a parse tree produced by {@link CobolParser#notAtEndPhrase}.
	 * @param ctx the parse tree
	 */
	void exitNotAtEndPhrase(CobolParser.NotAtEndPhraseContext ctx);
	/**
	 * Enter a parse tree produced by {@link CobolParser#invalidKeyPhrase}.
	 * @param ctx the parse tree
	 */
	void enterInvalidKeyPhrase(CobolParser.InvalidKeyPhraseContext ctx);
	/**
	 * Exit a parse tree produced by {@link CobolParser#invalidKeyPhrase}.
	 * @param ctx the parse tree
	 */
	void exitInvalidKeyPhrase(CobolParser.InvalidKeyPhraseContext ctx);
	/**
	 * Enter a parse tree produced by {@link CobolParser#notInvalidKeyPhrase}.
	 * @param ctx the parse tree
	 */
	void enterNotInvalidKeyPhrase(CobolParser.NotInvalidKeyPhraseContext ctx);
	/**
	 * Exit a parse tree produced by {@link CobolParser#notInvalidKeyPhrase}.
	 * @param ctx the parse tree
	 */
	void exitNotInvalidKeyPhrase(CobolParser.NotInvalidKeyPhraseContext ctx);
	/**
	 * Enter a parse tree produced by {@link CobolParser#onOverflowPhrase}.
	 * @param ctx the parse tree
	 */
	void enterOnOverflowPhrase(CobolParser.OnOverflowPhraseContext ctx);
	/**
	 * Exit a parse tree produced by {@link CobolParser#onOverflowPhrase}.
	 * @param ctx the parse tree
	 */
	void exitOnOverflowPhrase(CobolParser.OnOverflowPhraseContext ctx);
	/**
	 * Enter a parse tree produced by {@link CobolParser#notOnOverflowPhrase}.
	 * @param ctx the parse tree
	 */
	void enterNotOnOverflowPhrase(CobolParser.NotOnOverflowPhraseContext ctx);
	/**
	 * Exit a parse tree produced by {@link CobolParser#notOnOverflowPhrase}.
	 * @param ctx the parse tree
	 */
	void exitNotOnOverflowPhrase(CobolParser.NotOnOverflowPhraseContext ctx);
	/**
	 * Enter a parse tree produced by {@link CobolParser#onSizeErrorPhrase}.
	 * @param ctx the parse tree
	 */
	void enterOnSizeErrorPhrase(CobolParser.OnSizeErrorPhraseContext ctx);
	/**
	 * Exit a parse tree produced by {@link CobolParser#onSizeErrorPhrase}.
	 * @param ctx the parse tree
	 */
	void exitOnSizeErrorPhrase(CobolParser.OnSizeErrorPhraseContext ctx);
	/**
	 * Enter a parse tree produced by {@link CobolParser#notOnSizeErrorPhrase}.
	 * @param ctx the parse tree
	 */
	void enterNotOnSizeErrorPhrase(CobolParser.NotOnSizeErrorPhraseContext ctx);
	/**
	 * Exit a parse tree produced by {@link CobolParser#notOnSizeErrorPhrase}.
	 * @param ctx the parse tree
	 */
	void exitNotOnSizeErrorPhrase(CobolParser.NotOnSizeErrorPhraseContext ctx);
	/**
	 * Enter a parse tree produced by {@link CobolParser#onExceptionClause}.
	 * @param ctx the parse tree
	 */
	void enterOnExceptionClause(CobolParser.OnExceptionClauseContext ctx);
	/**
	 * Exit a parse tree produced by {@link CobolParser#onExceptionClause}.
	 * @param ctx the parse tree
	 */
	void exitOnExceptionClause(CobolParser.OnExceptionClauseContext ctx);
	/**
	 * Enter a parse tree produced by {@link CobolParser#notOnExceptionClause}.
	 * @param ctx the parse tree
	 */
	void enterNotOnExceptionClause(CobolParser.NotOnExceptionClauseContext ctx);
	/**
	 * Exit a parse tree produced by {@link CobolParser#notOnExceptionClause}.
	 * @param ctx the parse tree
	 */
	void exitNotOnExceptionClause(CobolParser.NotOnExceptionClauseContext ctx);
	/**
	 * Enter a parse tree produced by {@link CobolParser#arithmeticExpression}.
	 * @param ctx the parse tree
	 */
	void enterArithmeticExpression(CobolParser.ArithmeticExpressionContext ctx);
	/**
	 * Exit a parse tree produced by {@link CobolParser#arithmeticExpression}.
	 * @param ctx the parse tree
	 */
	void exitArithmeticExpression(CobolParser.ArithmeticExpressionContext ctx);
	/**
	 * Enter a parse tree produced by {@link CobolParser#plusMinus}.
	 * @param ctx the parse tree
	 */
	void enterPlusMinus(CobolParser.PlusMinusContext ctx);
	/**
	 * Exit a parse tree produced by {@link CobolParser#plusMinus}.
	 * @param ctx the parse tree
	 */
	void exitPlusMinus(CobolParser.PlusMinusContext ctx);
	/**
	 * Enter a parse tree produced by {@link CobolParser#multDivs}.
	 * @param ctx the parse tree
	 */
	void enterMultDivs(CobolParser.MultDivsContext ctx);
	/**
	 * Exit a parse tree produced by {@link CobolParser#multDivs}.
	 * @param ctx the parse tree
	 */
	void exitMultDivs(CobolParser.MultDivsContext ctx);
	/**
	 * Enter a parse tree produced by {@link CobolParser#multDiv}.
	 * @param ctx the parse tree
	 */
	void enterMultDiv(CobolParser.MultDivContext ctx);
	/**
	 * Exit a parse tree produced by {@link CobolParser#multDiv}.
	 * @param ctx the parse tree
	 */
	void exitMultDiv(CobolParser.MultDivContext ctx);
	/**
	 * Enter a parse tree produced by {@link CobolParser#powers}.
	 * @param ctx the parse tree
	 */
	void enterPowers(CobolParser.PowersContext ctx);
	/**
	 * Exit a parse tree produced by {@link CobolParser#powers}.
	 * @param ctx the parse tree
	 */
	void exitPowers(CobolParser.PowersContext ctx);
	/**
	 * Enter a parse tree produced by {@link CobolParser#power}.
	 * @param ctx the parse tree
	 */
	void enterPower(CobolParser.PowerContext ctx);
	/**
	 * Exit a parse tree produced by {@link CobolParser#power}.
	 * @param ctx the parse tree
	 */
	void exitPower(CobolParser.PowerContext ctx);
	/**
	 * Enter a parse tree produced by {@link CobolParser#basis}.
	 * @param ctx the parse tree
	 */
	void enterBasis(CobolParser.BasisContext ctx);
	/**
	 * Exit a parse tree produced by {@link CobolParser#basis}.
	 * @param ctx the parse tree
	 */
	void exitBasis(CobolParser.BasisContext ctx);
	/**
	 * Enter a parse tree produced by {@link CobolParser#condition}.
	 * @param ctx the parse tree
	 */
	void enterCondition(CobolParser.ConditionContext ctx);
	/**
	 * Exit a parse tree produced by {@link CobolParser#condition}.
	 * @param ctx the parse tree
	 */
	void exitCondition(CobolParser.ConditionContext ctx);
	/**
	 * Enter a parse tree produced by {@link CobolParser#andOrCondition}.
	 * @param ctx the parse tree
	 */
	void enterAndOrCondition(CobolParser.AndOrConditionContext ctx);
	/**
	 * Exit a parse tree produced by {@link CobolParser#andOrCondition}.
	 * @param ctx the parse tree
	 */
	void exitAndOrCondition(CobolParser.AndOrConditionContext ctx);
	/**
	 * Enter a parse tree produced by {@link CobolParser#combinableCondition}.
	 * @param ctx the parse tree
	 */
	void enterCombinableCondition(CobolParser.CombinableConditionContext ctx);
	/**
	 * Exit a parse tree produced by {@link CobolParser#combinableCondition}.
	 * @param ctx the parse tree
	 */
	void exitCombinableCondition(CobolParser.CombinableConditionContext ctx);
	/**
	 * Enter a parse tree produced by {@link CobolParser#simpleCondition}.
	 * @param ctx the parse tree
	 */
	void enterSimpleCondition(CobolParser.SimpleConditionContext ctx);
	/**
	 * Exit a parse tree produced by {@link CobolParser#simpleCondition}.
	 * @param ctx the parse tree
	 */
	void exitSimpleCondition(CobolParser.SimpleConditionContext ctx);
	/**
	 * Enter a parse tree produced by {@link CobolParser#classCondition}.
	 * @param ctx the parse tree
	 */
	void enterClassCondition(CobolParser.ClassConditionContext ctx);
	/**
	 * Exit a parse tree produced by {@link CobolParser#classCondition}.
	 * @param ctx the parse tree
	 */
	void exitClassCondition(CobolParser.ClassConditionContext ctx);
	/**
	 * Enter a parse tree produced by {@link CobolParser#conditionNameReference}.
	 * @param ctx the parse tree
	 */
	void enterConditionNameReference(CobolParser.ConditionNameReferenceContext ctx);
	/**
	 * Exit a parse tree produced by {@link CobolParser#conditionNameReference}.
	 * @param ctx the parse tree
	 */
	void exitConditionNameReference(CobolParser.ConditionNameReferenceContext ctx);
	/**
	 * Enter a parse tree produced by {@link CobolParser#conditionNameSubscriptReference}.
	 * @param ctx the parse tree
	 */
	void enterConditionNameSubscriptReference(CobolParser.ConditionNameSubscriptReferenceContext ctx);
	/**
	 * Exit a parse tree produced by {@link CobolParser#conditionNameSubscriptReference}.
	 * @param ctx the parse tree
	 */
	void exitConditionNameSubscriptReference(CobolParser.ConditionNameSubscriptReferenceContext ctx);
	/**
	 * Enter a parse tree produced by {@link CobolParser#relationCondition}.
	 * @param ctx the parse tree
	 */
	void enterRelationCondition(CobolParser.RelationConditionContext ctx);
	/**
	 * Exit a parse tree produced by {@link CobolParser#relationCondition}.
	 * @param ctx the parse tree
	 */
	void exitRelationCondition(CobolParser.RelationConditionContext ctx);
	/**
	 * Enter a parse tree produced by {@link CobolParser#relationSignCondition}.
	 * @param ctx the parse tree
	 */
	void enterRelationSignCondition(CobolParser.RelationSignConditionContext ctx);
	/**
	 * Exit a parse tree produced by {@link CobolParser#relationSignCondition}.
	 * @param ctx the parse tree
	 */
	void exitRelationSignCondition(CobolParser.RelationSignConditionContext ctx);
	/**
	 * Enter a parse tree produced by {@link CobolParser#relationArithmeticComparison}.
	 * @param ctx the parse tree
	 */
	void enterRelationArithmeticComparison(CobolParser.RelationArithmeticComparisonContext ctx);
	/**
	 * Exit a parse tree produced by {@link CobolParser#relationArithmeticComparison}.
	 * @param ctx the parse tree
	 */
	void exitRelationArithmeticComparison(CobolParser.RelationArithmeticComparisonContext ctx);
	/**
	 * Enter a parse tree produced by {@link CobolParser#relationCombinedComparison}.
	 * @param ctx the parse tree
	 */
	void enterRelationCombinedComparison(CobolParser.RelationCombinedComparisonContext ctx);
	/**
	 * Exit a parse tree produced by {@link CobolParser#relationCombinedComparison}.
	 * @param ctx the parse tree
	 */
	void exitRelationCombinedComparison(CobolParser.RelationCombinedComparisonContext ctx);
	/**
	 * Enter a parse tree produced by {@link CobolParser#relationCombinedCondition}.
	 * @param ctx the parse tree
	 */
	void enterRelationCombinedCondition(CobolParser.RelationCombinedConditionContext ctx);
	/**
	 * Exit a parse tree produced by {@link CobolParser#relationCombinedCondition}.
	 * @param ctx the parse tree
	 */
	void exitRelationCombinedCondition(CobolParser.RelationCombinedConditionContext ctx);
	/**
	 * Enter a parse tree produced by {@link CobolParser#relationalOperator}.
	 * @param ctx the parse tree
	 */
	void enterRelationalOperator(CobolParser.RelationalOperatorContext ctx);
	/**
	 * Exit a parse tree produced by {@link CobolParser#relationalOperator}.
	 * @param ctx the parse tree
	 */
	void exitRelationalOperator(CobolParser.RelationalOperatorContext ctx);
	/**
	 * Enter a parse tree produced by {@link CobolParser#abbreviation}.
	 * @param ctx the parse tree
	 */
	void enterAbbreviation(CobolParser.AbbreviationContext ctx);
	/**
	 * Exit a parse tree produced by {@link CobolParser#abbreviation}.
	 * @param ctx the parse tree
	 */
	void exitAbbreviation(CobolParser.AbbreviationContext ctx);
	/**
	 * Enter a parse tree produced by {@link CobolParser#identifier}.
	 * @param ctx the parse tree
	 */
	void enterIdentifier(CobolParser.IdentifierContext ctx);
	/**
	 * Exit a parse tree produced by {@link CobolParser#identifier}.
	 * @param ctx the parse tree
	 */
	void exitIdentifier(CobolParser.IdentifierContext ctx);
	/**
	 * Enter a parse tree produced by {@link CobolParser#tableCall}.
	 * @param ctx the parse tree
	 */
	void enterTableCall(CobolParser.TableCallContext ctx);
	/**
	 * Exit a parse tree produced by {@link CobolParser#tableCall}.
	 * @param ctx the parse tree
	 */
	void exitTableCall(CobolParser.TableCallContext ctx);
	/**
	 * Enter a parse tree produced by {@link CobolParser#functionCall}.
	 * @param ctx the parse tree
	 */
	void enterFunctionCall(CobolParser.FunctionCallContext ctx);
	/**
	 * Exit a parse tree produced by {@link CobolParser#functionCall}.
	 * @param ctx the parse tree
	 */
	void exitFunctionCall(CobolParser.FunctionCallContext ctx);
	/**
	 * Enter a parse tree produced by {@link CobolParser#referenceModifier}.
	 * @param ctx the parse tree
	 */
	void enterReferenceModifier(CobolParser.ReferenceModifierContext ctx);
	/**
	 * Exit a parse tree produced by {@link CobolParser#referenceModifier}.
	 * @param ctx the parse tree
	 */
	void exitReferenceModifier(CobolParser.ReferenceModifierContext ctx);
	/**
	 * Enter a parse tree produced by {@link CobolParser#characterPosition}.
	 * @param ctx the parse tree
	 */
	void enterCharacterPosition(CobolParser.CharacterPositionContext ctx);
	/**
	 * Exit a parse tree produced by {@link CobolParser#characterPosition}.
	 * @param ctx the parse tree
	 */
	void exitCharacterPosition(CobolParser.CharacterPositionContext ctx);
	/**
	 * Enter a parse tree produced by {@link CobolParser#length}.
	 * @param ctx the parse tree
	 */
	void enterLength(CobolParser.LengthContext ctx);
	/**
	 * Exit a parse tree produced by {@link CobolParser#length}.
	 * @param ctx the parse tree
	 */
	void exitLength(CobolParser.LengthContext ctx);
	/**
	 * Enter a parse tree produced by {@link CobolParser#subscript}.
	 * @param ctx the parse tree
	 */
	void enterSubscript(CobolParser.SubscriptContext ctx);
	/**
	 * Exit a parse tree produced by {@link CobolParser#subscript}.
	 * @param ctx the parse tree
	 */
	void exitSubscript(CobolParser.SubscriptContext ctx);
	/**
	 * Enter a parse tree produced by {@link CobolParser#argument}.
	 * @param ctx the parse tree
	 */
	void enterArgument(CobolParser.ArgumentContext ctx);
	/**
	 * Exit a parse tree produced by {@link CobolParser#argument}.
	 * @param ctx the parse tree
	 */
	void exitArgument(CobolParser.ArgumentContext ctx);
	/**
	 * Enter a parse tree produced by {@link CobolParser#qualifiedDataName}.
	 * @param ctx the parse tree
	 */
	void enterQualifiedDataName(CobolParser.QualifiedDataNameContext ctx);
	/**
	 * Exit a parse tree produced by {@link CobolParser#qualifiedDataName}.
	 * @param ctx the parse tree
	 */
	void exitQualifiedDataName(CobolParser.QualifiedDataNameContext ctx);
	/**
	 * Enter a parse tree produced by {@link CobolParser#qualifiedDataNameFormat1}.
	 * @param ctx the parse tree
	 */
	void enterQualifiedDataNameFormat1(CobolParser.QualifiedDataNameFormat1Context ctx);
	/**
	 * Exit a parse tree produced by {@link CobolParser#qualifiedDataNameFormat1}.
	 * @param ctx the parse tree
	 */
	void exitQualifiedDataNameFormat1(CobolParser.QualifiedDataNameFormat1Context ctx);
	/**
	 * Enter a parse tree produced by {@link CobolParser#qualifiedDataNameFormat2}.
	 * @param ctx the parse tree
	 */
	void enterQualifiedDataNameFormat2(CobolParser.QualifiedDataNameFormat2Context ctx);
	/**
	 * Exit a parse tree produced by {@link CobolParser#qualifiedDataNameFormat2}.
	 * @param ctx the parse tree
	 */
	void exitQualifiedDataNameFormat2(CobolParser.QualifiedDataNameFormat2Context ctx);
	/**
	 * Enter a parse tree produced by {@link CobolParser#qualifiedDataNameFormat3}.
	 * @param ctx the parse tree
	 */
	void enterQualifiedDataNameFormat3(CobolParser.QualifiedDataNameFormat3Context ctx);
	/**
	 * Exit a parse tree produced by {@link CobolParser#qualifiedDataNameFormat3}.
	 * @param ctx the parse tree
	 */
	void exitQualifiedDataNameFormat3(CobolParser.QualifiedDataNameFormat3Context ctx);
	/**
	 * Enter a parse tree produced by {@link CobolParser#qualifiedDataNameFormat4}.
	 * @param ctx the parse tree
	 */
	void enterQualifiedDataNameFormat4(CobolParser.QualifiedDataNameFormat4Context ctx);
	/**
	 * Exit a parse tree produced by {@link CobolParser#qualifiedDataNameFormat4}.
	 * @param ctx the parse tree
	 */
	void exitQualifiedDataNameFormat4(CobolParser.QualifiedDataNameFormat4Context ctx);
	/**
	 * Enter a parse tree produced by {@link CobolParser#qualifiedInData}.
	 * @param ctx the parse tree
	 */
	void enterQualifiedInData(CobolParser.QualifiedInDataContext ctx);
	/**
	 * Exit a parse tree produced by {@link CobolParser#qualifiedInData}.
	 * @param ctx the parse tree
	 */
	void exitQualifiedInData(CobolParser.QualifiedInDataContext ctx);
	/**
	 * Enter a parse tree produced by {@link CobolParser#inData}.
	 * @param ctx the parse tree
	 */
	void enterInData(CobolParser.InDataContext ctx);
	/**
	 * Exit a parse tree produced by {@link CobolParser#inData}.
	 * @param ctx the parse tree
	 */
	void exitInData(CobolParser.InDataContext ctx);
	/**
	 * Enter a parse tree produced by {@link CobolParser#inFile}.
	 * @param ctx the parse tree
	 */
	void enterInFile(CobolParser.InFileContext ctx);
	/**
	 * Exit a parse tree produced by {@link CobolParser#inFile}.
	 * @param ctx the parse tree
	 */
	void exitInFile(CobolParser.InFileContext ctx);
	/**
	 * Enter a parse tree produced by {@link CobolParser#inMnemonic}.
	 * @param ctx the parse tree
	 */
	void enterInMnemonic(CobolParser.InMnemonicContext ctx);
	/**
	 * Exit a parse tree produced by {@link CobolParser#inMnemonic}.
	 * @param ctx the parse tree
	 */
	void exitInMnemonic(CobolParser.InMnemonicContext ctx);
	/**
	 * Enter a parse tree produced by {@link CobolParser#inSection}.
	 * @param ctx the parse tree
	 */
	void enterInSection(CobolParser.InSectionContext ctx);
	/**
	 * Exit a parse tree produced by {@link CobolParser#inSection}.
	 * @param ctx the parse tree
	 */
	void exitInSection(CobolParser.InSectionContext ctx);
	/**
	 * Enter a parse tree produced by {@link CobolParser#inLibrary}.
	 * @param ctx the parse tree
	 */
	void enterInLibrary(CobolParser.InLibraryContext ctx);
	/**
	 * Exit a parse tree produced by {@link CobolParser#inLibrary}.
	 * @param ctx the parse tree
	 */
	void exitInLibrary(CobolParser.InLibraryContext ctx);
	/**
	 * Enter a parse tree produced by {@link CobolParser#inTable}.
	 * @param ctx the parse tree
	 */
	void enterInTable(CobolParser.InTableContext ctx);
	/**
	 * Exit a parse tree produced by {@link CobolParser#inTable}.
	 * @param ctx the parse tree
	 */
	void exitInTable(CobolParser.InTableContext ctx);
	/**
	 * Enter a parse tree produced by {@link CobolParser#alphabetName}.
	 * @param ctx the parse tree
	 */
	void enterAlphabetName(CobolParser.AlphabetNameContext ctx);
	/**
	 * Exit a parse tree produced by {@link CobolParser#alphabetName}.
	 * @param ctx the parse tree
	 */
	void exitAlphabetName(CobolParser.AlphabetNameContext ctx);
	/**
	 * Enter a parse tree produced by {@link CobolParser#assignmentName}.
	 * @param ctx the parse tree
	 */
	void enterAssignmentName(CobolParser.AssignmentNameContext ctx);
	/**
	 * Exit a parse tree produced by {@link CobolParser#assignmentName}.
	 * @param ctx the parse tree
	 */
	void exitAssignmentName(CobolParser.AssignmentNameContext ctx);
	/**
	 * Enter a parse tree produced by {@link CobolParser#basisName}.
	 * @param ctx the parse tree
	 */
	void enterBasisName(CobolParser.BasisNameContext ctx);
	/**
	 * Exit a parse tree produced by {@link CobolParser#basisName}.
	 * @param ctx the parse tree
	 */
	void exitBasisName(CobolParser.BasisNameContext ctx);
	/**
	 * Enter a parse tree produced by {@link CobolParser#cdName}.
	 * @param ctx the parse tree
	 */
	void enterCdName(CobolParser.CdNameContext ctx);
	/**
	 * Exit a parse tree produced by {@link CobolParser#cdName}.
	 * @param ctx the parse tree
	 */
	void exitCdName(CobolParser.CdNameContext ctx);
	/**
	 * Enter a parse tree produced by {@link CobolParser#className}.
	 * @param ctx the parse tree
	 */
	void enterClassName(CobolParser.ClassNameContext ctx);
	/**
	 * Exit a parse tree produced by {@link CobolParser#className}.
	 * @param ctx the parse tree
	 */
	void exitClassName(CobolParser.ClassNameContext ctx);
	/**
	 * Enter a parse tree produced by {@link CobolParser#computerName}.
	 * @param ctx the parse tree
	 */
	void enterComputerName(CobolParser.ComputerNameContext ctx);
	/**
	 * Exit a parse tree produced by {@link CobolParser#computerName}.
	 * @param ctx the parse tree
	 */
	void exitComputerName(CobolParser.ComputerNameContext ctx);
	/**
	 * Enter a parse tree produced by {@link CobolParser#conditionName}.
	 * @param ctx the parse tree
	 */
	void enterConditionName(CobolParser.ConditionNameContext ctx);
	/**
	 * Exit a parse tree produced by {@link CobolParser#conditionName}.
	 * @param ctx the parse tree
	 */
	void exitConditionName(CobolParser.ConditionNameContext ctx);
	/**
	 * Enter a parse tree produced by {@link CobolParser#dataName}.
	 * @param ctx the parse tree
	 */
	void enterDataName(CobolParser.DataNameContext ctx);
	/**
	 * Exit a parse tree produced by {@link CobolParser#dataName}.
	 * @param ctx the parse tree
	 */
	void exitDataName(CobolParser.DataNameContext ctx);
	/**
	 * Enter a parse tree produced by {@link CobolParser#dataDescName}.
	 * @param ctx the parse tree
	 */
	void enterDataDescName(CobolParser.DataDescNameContext ctx);
	/**
	 * Exit a parse tree produced by {@link CobolParser#dataDescName}.
	 * @param ctx the parse tree
	 */
	void exitDataDescName(CobolParser.DataDescNameContext ctx);
	/**
	 * Enter a parse tree produced by {@link CobolParser#environmentName}.
	 * @param ctx the parse tree
	 */
	void enterEnvironmentName(CobolParser.EnvironmentNameContext ctx);
	/**
	 * Exit a parse tree produced by {@link CobolParser#environmentName}.
	 * @param ctx the parse tree
	 */
	void exitEnvironmentName(CobolParser.EnvironmentNameContext ctx);
	/**
	 * Enter a parse tree produced by {@link CobolParser#fileName}.
	 * @param ctx the parse tree
	 */
	void enterFileName(CobolParser.FileNameContext ctx);
	/**
	 * Exit a parse tree produced by {@link CobolParser#fileName}.
	 * @param ctx the parse tree
	 */
	void exitFileName(CobolParser.FileNameContext ctx);
	/**
	 * Enter a parse tree produced by {@link CobolParser#functionName}.
	 * @param ctx the parse tree
	 */
	void enterFunctionName(CobolParser.FunctionNameContext ctx);
	/**
	 * Exit a parse tree produced by {@link CobolParser#functionName}.
	 * @param ctx the parse tree
	 */
	void exitFunctionName(CobolParser.FunctionNameContext ctx);
	/**
	 * Enter a parse tree produced by {@link CobolParser#indexName}.
	 * @param ctx the parse tree
	 */
	void enterIndexName(CobolParser.IndexNameContext ctx);
	/**
	 * Exit a parse tree produced by {@link CobolParser#indexName}.
	 * @param ctx the parse tree
	 */
	void exitIndexName(CobolParser.IndexNameContext ctx);
	/**
	 * Enter a parse tree produced by {@link CobolParser#languageName}.
	 * @param ctx the parse tree
	 */
	void enterLanguageName(CobolParser.LanguageNameContext ctx);
	/**
	 * Exit a parse tree produced by {@link CobolParser#languageName}.
	 * @param ctx the parse tree
	 */
	void exitLanguageName(CobolParser.LanguageNameContext ctx);
	/**
	 * Enter a parse tree produced by {@link CobolParser#libraryName}.
	 * @param ctx the parse tree
	 */
	void enterLibraryName(CobolParser.LibraryNameContext ctx);
	/**
	 * Exit a parse tree produced by {@link CobolParser#libraryName}.
	 * @param ctx the parse tree
	 */
	void exitLibraryName(CobolParser.LibraryNameContext ctx);
	/**
	 * Enter a parse tree produced by {@link CobolParser#localName}.
	 * @param ctx the parse tree
	 */
	void enterLocalName(CobolParser.LocalNameContext ctx);
	/**
	 * Exit a parse tree produced by {@link CobolParser#localName}.
	 * @param ctx the parse tree
	 */
	void exitLocalName(CobolParser.LocalNameContext ctx);
	/**
	 * Enter a parse tree produced by {@link CobolParser#mnemonicName}.
	 * @param ctx the parse tree
	 */
	void enterMnemonicName(CobolParser.MnemonicNameContext ctx);
	/**
	 * Exit a parse tree produced by {@link CobolParser#mnemonicName}.
	 * @param ctx the parse tree
	 */
	void exitMnemonicName(CobolParser.MnemonicNameContext ctx);
	/**
	 * Enter a parse tree produced by {@link CobolParser#paragraphName}.
	 * @param ctx the parse tree
	 */
	void enterParagraphName(CobolParser.ParagraphNameContext ctx);
	/**
	 * Exit a parse tree produced by {@link CobolParser#paragraphName}.
	 * @param ctx the parse tree
	 */
	void exitParagraphName(CobolParser.ParagraphNameContext ctx);
	/**
	 * Enter a parse tree produced by {@link CobolParser#procedureName}.
	 * @param ctx the parse tree
	 */
	void enterProcedureName(CobolParser.ProcedureNameContext ctx);
	/**
	 * Exit a parse tree produced by {@link CobolParser#procedureName}.
	 * @param ctx the parse tree
	 */
	void exitProcedureName(CobolParser.ProcedureNameContext ctx);
	/**
	 * Enter a parse tree produced by {@link CobolParser#programName}.
	 * @param ctx the parse tree
	 */
	void enterProgramName(CobolParser.ProgramNameContext ctx);
	/**
	 * Exit a parse tree produced by {@link CobolParser#programName}.
	 * @param ctx the parse tree
	 */
	void exitProgramName(CobolParser.ProgramNameContext ctx);
	/**
	 * Enter a parse tree produced by {@link CobolParser#recordName}.
	 * @param ctx the parse tree
	 */
	void enterRecordName(CobolParser.RecordNameContext ctx);
	/**
	 * Exit a parse tree produced by {@link CobolParser#recordName}.
	 * @param ctx the parse tree
	 */
	void exitRecordName(CobolParser.RecordNameContext ctx);
	/**
	 * Enter a parse tree produced by {@link CobolParser#reportName}.
	 * @param ctx the parse tree
	 */
	void enterReportName(CobolParser.ReportNameContext ctx);
	/**
	 * Exit a parse tree produced by {@link CobolParser#reportName}.
	 * @param ctx the parse tree
	 */
	void exitReportName(CobolParser.ReportNameContext ctx);
	/**
	 * Enter a parse tree produced by {@link CobolParser#routineName}.
	 * @param ctx the parse tree
	 */
	void enterRoutineName(CobolParser.RoutineNameContext ctx);
	/**
	 * Exit a parse tree produced by {@link CobolParser#routineName}.
	 * @param ctx the parse tree
	 */
	void exitRoutineName(CobolParser.RoutineNameContext ctx);
	/**
	 * Enter a parse tree produced by {@link CobolParser#screenName}.
	 * @param ctx the parse tree
	 */
	void enterScreenName(CobolParser.ScreenNameContext ctx);
	/**
	 * Exit a parse tree produced by {@link CobolParser#screenName}.
	 * @param ctx the parse tree
	 */
	void exitScreenName(CobolParser.ScreenNameContext ctx);
	/**
	 * Enter a parse tree produced by {@link CobolParser#sectionName}.
	 * @param ctx the parse tree
	 */
	void enterSectionName(CobolParser.SectionNameContext ctx);
	/**
	 * Exit a parse tree produced by {@link CobolParser#sectionName}.
	 * @param ctx the parse tree
	 */
	void exitSectionName(CobolParser.SectionNameContext ctx);
	/**
	 * Enter a parse tree produced by {@link CobolParser#systemName}.
	 * @param ctx the parse tree
	 */
	void enterSystemName(CobolParser.SystemNameContext ctx);
	/**
	 * Exit a parse tree produced by {@link CobolParser#systemName}.
	 * @param ctx the parse tree
	 */
	void exitSystemName(CobolParser.SystemNameContext ctx);
	/**
	 * Enter a parse tree produced by {@link CobolParser#symbolicCharacter}.
	 * @param ctx the parse tree
	 */
	void enterSymbolicCharacter(CobolParser.SymbolicCharacterContext ctx);
	/**
	 * Exit a parse tree produced by {@link CobolParser#symbolicCharacter}.
	 * @param ctx the parse tree
	 */
	void exitSymbolicCharacter(CobolParser.SymbolicCharacterContext ctx);
	/**
	 * Enter a parse tree produced by {@link CobolParser#textName}.
	 * @param ctx the parse tree
	 */
	void enterTextName(CobolParser.TextNameContext ctx);
	/**
	 * Exit a parse tree produced by {@link CobolParser#textName}.
	 * @param ctx the parse tree
	 */
	void exitTextName(CobolParser.TextNameContext ctx);
	/**
	 * Enter a parse tree produced by {@link CobolParser#cobolWord}.
	 * @param ctx the parse tree
	 */
	void enterCobolWord(CobolParser.CobolWordContext ctx);
	/**
	 * Exit a parse tree produced by {@link CobolParser#cobolWord}.
	 * @param ctx the parse tree
	 */
	void exitCobolWord(CobolParser.CobolWordContext ctx);
	/**
	 * Enter a parse tree produced by {@link CobolParser#literal}.
	 * @param ctx the parse tree
	 */
	void enterLiteral(CobolParser.LiteralContext ctx);
	/**
	 * Exit a parse tree produced by {@link CobolParser#literal}.
	 * @param ctx the parse tree
	 */
	void exitLiteral(CobolParser.LiteralContext ctx);
	/**
	 * Enter a parse tree produced by {@link CobolParser#booleanLiteral}.
	 * @param ctx the parse tree
	 */
	void enterBooleanLiteral(CobolParser.BooleanLiteralContext ctx);
	/**
	 * Exit a parse tree produced by {@link CobolParser#booleanLiteral}.
	 * @param ctx the parse tree
	 */
	void exitBooleanLiteral(CobolParser.BooleanLiteralContext ctx);
	/**
	 * Enter a parse tree produced by {@link CobolParser#numericLiteral}.
	 * @param ctx the parse tree
	 */
	void enterNumericLiteral(CobolParser.NumericLiteralContext ctx);
	/**
	 * Exit a parse tree produced by {@link CobolParser#numericLiteral}.
	 * @param ctx the parse tree
	 */
	void exitNumericLiteral(CobolParser.NumericLiteralContext ctx);
	/**
	 * Enter a parse tree produced by {@link CobolParser#integerLiteral}.
	 * @param ctx the parse tree
	 */
	void enterIntegerLiteral(CobolParser.IntegerLiteralContext ctx);
	/**
	 * Exit a parse tree produced by {@link CobolParser#integerLiteral}.
	 * @param ctx the parse tree
	 */
	void exitIntegerLiteral(CobolParser.IntegerLiteralContext ctx);
	/**
	 * Enter a parse tree produced by {@link CobolParser#cicsDfhRespLiteral}.
	 * @param ctx the parse tree
	 */
	void enterCicsDfhRespLiteral(CobolParser.CicsDfhRespLiteralContext ctx);
	/**
	 * Exit a parse tree produced by {@link CobolParser#cicsDfhRespLiteral}.
	 * @param ctx the parse tree
	 */
	void exitCicsDfhRespLiteral(CobolParser.CicsDfhRespLiteralContext ctx);
	/**
	 * Enter a parse tree produced by {@link CobolParser#cicsDfhValueLiteral}.
	 * @param ctx the parse tree
	 */
	void enterCicsDfhValueLiteral(CobolParser.CicsDfhValueLiteralContext ctx);
	/**
	 * Exit a parse tree produced by {@link CobolParser#cicsDfhValueLiteral}.
	 * @param ctx the parse tree
	 */
	void exitCicsDfhValueLiteral(CobolParser.CicsDfhValueLiteralContext ctx);
	/**
	 * Enter a parse tree produced by {@link CobolParser#figurativeConstant}.
	 * @param ctx the parse tree
	 */
	void enterFigurativeConstant(CobolParser.FigurativeConstantContext ctx);
	/**
	 * Exit a parse tree produced by {@link CobolParser#figurativeConstant}.
	 * @param ctx the parse tree
	 */
	void exitFigurativeConstant(CobolParser.FigurativeConstantContext ctx);
	/**
	 * Enter a parse tree produced by {@link CobolParser#specialRegister}.
	 * @param ctx the parse tree
	 */
	void enterSpecialRegister(CobolParser.SpecialRegisterContext ctx);
	/**
	 * Exit a parse tree produced by {@link CobolParser#specialRegister}.
	 * @param ctx the parse tree
	 */
	void exitSpecialRegister(CobolParser.SpecialRegisterContext ctx);
	/**
	 * Enter a parse tree produced by {@link CobolParser#commentEntry}.
	 * @param ctx the parse tree
	 */
	void enterCommentEntry(CobolParser.CommentEntryContext ctx);
	/**
	 * Exit a parse tree produced by {@link CobolParser#commentEntry}.
	 * @param ctx the parse tree
	 */
	void exitCommentEntry(CobolParser.CommentEntryContext ctx);
}