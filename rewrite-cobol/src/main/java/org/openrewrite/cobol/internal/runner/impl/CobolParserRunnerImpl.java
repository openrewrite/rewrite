/*
 * Copyright (C) 2017, Ulrich Wolffgang <ulrich.wolffgang@proleap.io>
 * All rights reserved.
 *
 * This software may be modified and distributed under the terms
 * of the MIT license. See the LICENSE file for details.
 */

package org.openrewrite.cobol.internal.runner.impl;

import org.antlr.v4.runtime.CharStreams;
import org.antlr.v4.runtime.CommonTokenStream;
import org.openrewrite.cobol.CompilationUnit;
import org.openrewrite.cobol.ProgramImpl;
import org.openrewrite.cobol.internal.CobolParserException;
import org.openrewrite.cobol.internal.Program;
import org.openrewrite.cobol.internal.StringWithOriginalPositions;
import org.openrewrite.cobol.internal.grammar.CobolLexer;
import org.openrewrite.cobol.internal.grammar.CobolParser;
import org.openrewrite.cobol.internal.params.CobolParserParams;
import org.openrewrite.cobol.internal.params.impl.CobolParserParamsImpl;
import org.openrewrite.cobol.internal.preprocessor.CobolPreprocessor;
import org.openrewrite.cobol.internal.preprocessor.impl.CobolPreprocessorImpl;
import org.openrewrite.cobol.internal.runner.CobolParserRunner;
import org.openrewrite.cobol.internal.runner.ThrowingErrorListener;
import org.openrewrite.cobol.internal.util.FilenameUtils;

import java.io.File;
import java.io.IOException;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Scanner;

public class CobolParserRunnerImpl implements CobolParserRunner {

	// private final static Logger LOG = LoggerFactory.getLogger(CobolParserRunnerImpl.class);

	protected void analyze(final Program program) {
		throw new Error();
//		analyzeProgramUnits(program);
//
//		analyzeDataDivisionsStep1(program);
//		analyzeDataDivisionsStep2(program);
//
//		analyzeFileControlClauses(program);
//		analyzeFileDescriptionEntriesClauses(program);
//
//		analyzeProcedureDivisions(program);
//		analyzeProcedureStatements(program);
	}

	@Override
	public Program analyzeCode(final String cobolCode, final String compilationUnitName, final CobolParserParams params)
			throws IOException {
		throw new Error();
//		final Program program = new ProgramImpl();
//
//		parseCode(cobolCode, compilationUnitName, program, params);
//		analyze(program);
//
//		return program;
	}

	protected void analyzeDataDivisionsStep1(final Program program) {
		throw new Error();
//		for (final CompilationUnit compilationUnit : program.getCompilationUnits()) {
//			final ParserVisitor visitor = new CobolDataDivisionStep1VisitorImpl(program);
//
//			// LOG.info("Analyzing data divisions of compilation unit {} in step 1.", compilationUnit.getName());
//			visitor.visit(compilationUnit.getCtx());
//		}
	}

	protected void analyzeDataDivisionsStep2(final Program program) {
		throw new Error();
//		for (final CompilationUnit compilationUnit : program.getCompilationUnits()) {
//			final ParserVisitor visitor = new CobolDataDivisionStep2VisitorImpl(program);
//
//			LOG.info("Analyzing data divisions of compilation unit {} in step 2.", compilationUnit.getName());
//			visitor.visit(compilationUnit.getCtx());
//		}
	}

	@Override
	public Program analyzeFile(final File inputFile, final CobolParserParams params) throws IOException {
		throw new Error();
//		final Program program = new ProgramImpl();
//
//		parseFile(inputFile, program, params);
//		analyze(program);
//
//		return program;
	}

	@Override
	public Program analyzeFile(final File cobolFile, final CobolPreprocessor.CobolSourceFormatEnum format) throws IOException {
		throw new Error();
//		final CobolParserParams params = createDefaultParams(format, cobolFile);
//		return analyzeFile(cobolFile, params);
	}

	protected void analyzeFileControlClauses(final Program program) {
		throw new Error();
//		for (final CompilationUnit compilationUnit : program.getCompilationUnits()) {
//			final ParserVisitor visitor = new CobolFileControlClauseVisitorImpl(program);
//
//			LOG.info("Analyzing file control clauses of compilation unit {}.", compilationUnit.getName());
//			visitor.visit(compilationUnit.getCtx());
//		}
	}

	protected void analyzeFileDescriptionEntriesClauses(final Program program) {
		throw new Error();
//		for (final CompilationUnit compilationUnit : program.getCompilationUnits()) {
//			final ParserVisitor visitor = new CobolFileDescriptionEntryClauseVisitorImpl(program);
//
//			LOG.info("Analyzing file description entries of compilation unit {}.", compilationUnit.getName());
//			visitor.visit(compilationUnit.getCtx());
//		}
	}

	protected void analyzeProcedureDivisions(final Program program) {
		throw new Error();
//		for (final CompilationUnit compilationUnit : program.getCompilationUnits()) {
//			final ParserVisitor visitor = new CobolProcedureDivisionVisitorImpl(program);
//
//			LOG.info("Analyzing procedure divisions of compilation unit {}.", compilationUnit.getName());
//			visitor.visit(compilationUnit.getCtx());
//		}
	}

	protected void analyzeProcedureStatements(final Program program) {
		throw new Error();
//		for (final CompilationUnit compilationUnit : program.getCompilationUnits()) {
//			final ParserVisitor visitor = new CobolProcedureStatementVisitorImpl(program);
//
//			LOG.info("Analyzing statements of compilation unit {}.", compilationUnit.getName());
//			visitor.visit(compilationUnit.getCtx());
//		}
	}

	protected void analyzeProgramUnits(final Program program) {
		throw new Error();
//		for (final CompilationUnit compilationUnit : program.getCompilationUnits()) {
//			final ParserVisitor visitor = new CobolProgramUnitVisitorImpl(compilationUnit);
//
//			LOG.info("Analyzing program units of compilation unit {}.", compilationUnit.getName());
//			visitor.visit(compilationUnit.getCtx());
//		}
	}

	protected String capitalize(final String line) {
		return Character.toUpperCase(line.charAt(0)) + line.substring(1);
	}

	protected CobolParserParams createDefaultParams() {
		return new CobolParserParamsImpl();
	}

	protected CobolParserParams createDefaultParams(final CobolPreprocessor.CobolSourceFormatEnum format, final File cobolFile) {
		final CobolParserParams result = createDefaultParams();
		result.setFormat(format);

		final File copyBooksDirectory = cobolFile.getParentFile();
		result.setCopyBookDirectories(Arrays.asList(copyBooksDirectory));

		return result;
	}

	protected String getCompilationUnitName(final File cobolFile) {
		return capitalize(FilenameUtils.removeExtension(cobolFile.getName()));
	}

	protected void parseCode(final String cobolCode, final String compilationUnitName, final Program program,
			final CobolParserParams params) throws IOException {
		// LOG.info("Parsing compilation unit {}.", compilationUnitName);

		// preprocess input stream
		final StringWithOriginalPositions preProcessedInput = new CobolPreprocessorImpl().processWithOriginalPositions(cobolCode, params);

		parsePreprocessInput(preProcessedInput, compilationUnitName, program, params);
	}

	protected void parseFile(final File cobolFile, final Program program, final CobolParserParams params)
			throws IOException {
		if (!cobolFile.isFile()) {
			throw new CobolParserException("Could not find file " + cobolFile.getAbsolutePath());
		} else {
			// determine the copy book name
			final String compilationUnitName = getCompilationUnitName(cobolFile);

			// LOG.info("Parsing compilation unit {}.", compilationUnitName);

			// preprocess input stream
			final Charset charset = params.getCharset();
			// LOG.info("Preprocessing file {} with line format {} and charset {}.", cobolFile.getName(), params.getFormat(),
			// 		charset);
			//final String cobolFileContent = Files.readString(cobolFile.toPath(), charset);
			final String cobolFileContent = new String(Files.readAllBytes(cobolFile.toPath()), charset);

			final StringWithOriginalPositions preProcessedInput = new CobolPreprocessorImpl().processWithOriginalPositions(cobolFileContent, params);

			parsePreprocessInput(preProcessedInput, compilationUnitName, program, params);
		}
	}

	public CobolLexer lexer;
	public CommonTokenStream tokens;
	public CobolParser parser;
	
	protected void parsePreprocessInput(final StringWithOriginalPositions preProcessedInput, final String compilationUnitName,
			final Program program, final CobolParserParams params) throws IOException {
		// run the lexer
		lexer = new CobolLexer(CharStreams.fromString(preProcessedInput.preprocessedText));

		lexer.setTokenFactory(new CobolTokenFactory(preProcessedInput));
		
		if (!params.getIgnoreSyntaxErrors()) {
			// register an error listener, so that preprocessing stops on errors
			lexer.removeErrorListeners();
			lexer.addErrorListener(new ThrowingErrorListener());
		}

		// get a list of matched tokens
		tokens = new CommonTokenStream(lexer);

		// pass the tokens to the parser
		parser = new CobolParser(tokens);

		if (!params.getIgnoreSyntaxErrors()) {
			// register an error listener, so that preprocessing stops on errors
			parser.removeErrorListeners();
			parser.addErrorListener(new ThrowingErrorListener());
		}

		// specify our entry point
		final CobolParser.StartRuleContext ctx = parser.startRule();

		// analyze contained compilation units
//		final List<String> lines = splitLines(preProcessedInput.preprocessedText);
//		final ParserVisitor visitor = new CobolCompilationUnitVisitorImpl(compilationUnitName, lines, tokens, program);
//
//		visitor.visit(ctx);
	}

	protected List<String> splitLines(final String preProcessedInput) {
		final Scanner scanner = new Scanner(preProcessedInput);
		final List<String> result = new ArrayList<String>();

		while (scanner.hasNextLine()) {
			result.add(scanner.nextLine());
		}

		scanner.close();
		return result;
	}
}
