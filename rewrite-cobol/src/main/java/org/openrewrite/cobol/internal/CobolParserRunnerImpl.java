/*
 * Copyright (C) 2017, Ulrich Wolffgang <ulrich.wolffgang@proleap.io>
 * All rights reserved.
 *
 * This software may be modified and distributed under the terms
 * of the MIT license. See the LICENSE file for details.
 */

package org.openrewrite.cobol.internal;

import org.antlr.v4.runtime.CharStreams;
import org.antlr.v4.runtime.CommonTokenStream;
import org.openrewrite.cobol.internal.grammar.CobolLexer;
import org.openrewrite.cobol.internal.grammar.CobolParser;
import org.openrewrite.cobol.internal.params.CobolParserParams;
import org.openrewrite.cobol.internal.params.impl.CobolParserParamsImpl;
import org.openrewrite.cobol.internal.preprocessor.CobolPreprocessor;
import org.openrewrite.cobol.internal.preprocessor.impl.CobolPreprocessorImpl;
import org.openrewrite.cobol.internal.runner.ThrowingErrorListener;
import org.openrewrite.cobol.internal.runner.impl.CobolTokenFactory;
import org.openrewrite.cobol.internal.util.FilenameUtils;

import java.io.File;
import java.io.IOException;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Scanner;

public class CobolParserRunnerImpl {

    // private final static Logger LOG = LoggerFactory.getLogger(CobolParserRunnerImpl.class);

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
        //LOG.trace("Parsing compilation unit {}.", compilationUnitName);

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

            //LOG.info("Parsing compilation unit {}.", compilationUnitName);

            // preprocess input stream
            final Charset charset = params.getCharset();
            //LOG.info("Preprocessing file {} with line format {} and charset {}.", cobolFile.getName(), params.getFormat(),
            //		charset);
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
