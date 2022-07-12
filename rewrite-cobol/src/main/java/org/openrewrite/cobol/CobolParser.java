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

import io.micrometer.core.instrument.Metrics;
import io.micrometer.core.instrument.Timer;
import org.antlr.v4.runtime.*;
import org.openrewrite.Cursor;
import org.openrewrite.ExecutionContext;
import org.openrewrite.InMemoryExecutionContext;
import org.openrewrite.Parser;
import org.openrewrite.cobol.internal.*;
import org.openrewrite.cobol.internal.grammar.CobolLexer;
import org.openrewrite.cobol.internal.params.CobolParserParams;
import org.openrewrite.cobol.internal.params.impl.CobolParserParamsImpl;
import org.openrewrite.cobol.internal.preprocessor.CobolPreprocessor;
import org.openrewrite.cobol.internal.preprocessor.impl.CobolPreprocessorImpl;
import org.openrewrite.cobol.internal.runner.impl.CobolTokenFactory;
import org.openrewrite.cobol.tree.Cobol;
import org.openrewrite.internal.EncodingDetectingInputStream;
import org.openrewrite.internal.MetricsHelper;
import org.openrewrite.internal.lang.Nullable;
import org.openrewrite.tree.ParsingEventListener;
import org.openrewrite.tree.ParsingExecutionContextView;
import org.openrewrite.cobol.internal.grammar.CobolParser.StartRuleContext;

import java.io.File;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;

import static java.util.stream.Collectors.toList;

public class CobolParser implements Parser<Cobol.CompilationUnit> {
    private static final List<String> COBOL_FILE_EXTENSIONS = Arrays.asList(".cbl", ".cpy");

    @Override
    public List<Cobol.CompilationUnit> parseInputs(Iterable<Input> sourceFiles, @Nullable Path relativeTo, ExecutionContext ctx) {
        ParsingEventListener parsingListener = ParsingExecutionContextView.view(ctx).getParsingListener();
        return acceptedInputs(sourceFiles).stream()
                .map(sourceFile -> {
                    Timer.Builder timer = Timer.builder("rewrite.parse")
                            .description("The time spent parsing a COBOL file")
                            .tag("file.type", "COBOL");
                    Timer.Sample sample = Timer.start();
                    try {
                        EncodingDetectingInputStream is = sourceFile.getSource();
                        String sourceStr = is.readFully();

                        CobolPreprocessor.CobolSourceFormatEnum format = CobolPreprocessor.CobolSourceFormatEnum.FIXED;
                        CobolParserParams params = new CobolParserParamsImpl();
                        params.setFormat(format);

                        StringWithOriginalPositions preProcessedInput = new CobolPreprocessorImpl().processWithOriginalPositions(sourceStr, params);

                        /////////////////

                        CobolLexer lexer = new CobolLexer(CharStreams.fromString(preProcessedInput.preprocessedText));
                        lexer.setTokenFactory(new CobolTokenFactory(preProcessedInput));

                        CobolTokenStream tokens = new CobolTokenStream(preProcessedInput.preprocessedText, lexer);

                        org.openrewrite.cobol.internal.grammar.CobolParser parser = new org.openrewrite.cobol.internal.grammar.CobolParser(tokens);

                        parser.removeErrorListeners();
                        parser.addErrorListener(new ForwardingErrorListener(sourceFile.getPath(), ctx));

                        StartRuleContext start = parser.startRule();

                        Cobol.CompilationUnit compilationUnit = new CobolParserVisitor(
                                sourceFile.getRelativePath(relativeTo),
                                sourceFile.getFileAttributes(),
                                preProcessedInput,
                                is.getCharset(),
                                is.isCharsetBomMarked()
                        ).visitStartRule(start);

                        tokens.removeTrailingWhitespace();

                        String test = compilationUnit.print(new Cursor(null, compilationUnit));
                        System.out.println("source= " + StringWithOriginalPositions.quote(sourceStr));
                        System.out.println("parsed= " + StringWithOriginalPositions.quote(test));
                        assert test.equals(sourceStr);

                        sample.stop(MetricsHelper.successTags(timer).register(Metrics.globalRegistry));
                        parsingListener.parsed(sourceFile, compilationUnit);
                        return compilationUnit;
                    } catch (Throwable t) {
                        sample.stop(MetricsHelper.errorTags(timer, t).register(Metrics.globalRegistry));
                        ctx.getOnError().accept(new IllegalStateException(sourceFile.getPath() + " " + t.getMessage(), t));
                        return null;
                    }
                })
                .filter(Objects::nonNull)
                .collect(toList());
    }

    @Override
    public List<Cobol.CompilationUnit> parse(String... sources) {
        return parse(new InMemoryExecutionContext(), sources);
    }

    @Override
    public boolean accept(Path path) {
        String s = path.toString().toLowerCase();
        for (String COBOL_FILE_EXTENSION : COBOL_FILE_EXTENSIONS) {
            if (s.endsWith(COBOL_FILE_EXTENSION)) {
                return true;
            }
        }
        return false;
    }

    @Override
    public Path sourcePathFromSourceText(Path prefix, String sourceCode) {
        return prefix.resolve("file.CBL");
    }

    private static class ForwardingErrorListener extends BaseErrorListener {
        private final Path sourcePath;
        private final ExecutionContext ctx;

        private ForwardingErrorListener(Path sourcePath, ExecutionContext ctx) {
            this.sourcePath = sourcePath;
            this.ctx = ctx;
        }

        @Override
        public void syntaxError(Recognizer<?, ?> recognizer, Object offendingSymbol,
                                int line, int charPositionInLine, String msg, RecognitionException e) {
            ctx.getOnError().accept(new CobolParsingException(sourcePath,
                    String.format("Syntax error in %s at line %d:%d %s.", sourcePath, line, charPositionInLine, msg), e));
        }
    }
}
