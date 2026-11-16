/*
 * Copyright 2025 the original author or authors.
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
package org.openrewrite.toml;

import org.antlr.v4.runtime.*;
import org.intellij.lang.annotations.Language;
import org.jspecify.annotations.Nullable;
import org.openrewrite.ExecutionContext;
import org.openrewrite.InMemoryExecutionContext;
import org.openrewrite.ParseExceptionResult;
import org.openrewrite.Parser;
import org.openrewrite.SourceFile;
import org.openrewrite.internal.EncodingDetectingInputStream;
import org.openrewrite.toml.internal.TomlParserVisitor;
import org.openrewrite.toml.internal.grammar.TomlLexer;
import org.openrewrite.toml.tree.Toml;
import org.openrewrite.tree.ParseError;
import org.openrewrite.tree.ParsingEventListener;
import org.openrewrite.tree.ParsingExecutionContextView;

import java.nio.file.Path;
import java.util.stream.Stream;

public class TomlParser implements Parser {
    @Override
    public Stream<SourceFile> parseInputs(Iterable<Input> sourceFiles, @Nullable Path relativeTo, ExecutionContext ctx) {
        ParsingEventListener parsingListener = ParsingExecutionContextView.view(ctx).getParsingListener();
        return acceptedInputs(sourceFiles).map(input -> {
            parsingListener.startedParsing(input);
            ForwardingErrorListener errorListener = new ForwardingErrorListener(input.getPath(), ctx);
            try (EncodingDetectingInputStream sourceStream = input.getSource(ctx)) {
                // Lex the same decoded text the visitor reads, or a file that is not UTF-8 lexes to different text
                String source = sourceStream.readFully();
                TomlLexer lexer = new TomlLexer(CharStreams.fromString(source));
                lexer.removeErrorListeners();
                lexer.addErrorListener(errorListener);

                org.openrewrite.toml.internal.grammar.TomlParser parser = new org.openrewrite.toml.internal.grammar.TomlParser(new CommonTokenStream(lexer));
                parser.removeErrorListeners();
                parser.addErrorListener(errorListener);

                Toml.Document document = new TomlParserVisitor(
                        input.getRelativePath(relativeTo),
                        input.getFileAttributes(),
                        source,
                        sourceStream.getCharset(),
                        sourceStream.isCharsetBomMarked()
                ).visitDocument(parser.document());
                if (errorListener.firstError != null) {
                    // recovered: prints as written, but the tree may not mean what the file does
                    document = document.withMarkers(document.getMarkers().add(ParseExceptionResult.build(this, errorListener.firstError)));
                }
                parsingListener.parsed(input, document);
                return requirePrintEqualsInput(document, input, relativeTo, ctx);
            } catch (Throwable t) {
                // report the syntax error, not the visitor's failure on what recovery left
                TomlParsingException syntaxError = errorListener.firstError;
                if (syntaxError != null) {
                    syntaxError.addSuppressed(t);
                    t = syntaxError;
                }
                ctx.getOnError().accept(t);
                return ParseError.build(this, input, relativeTo, ctx, t);
            }
        });
    }

    @Override
    public Stream<SourceFile> parse(@Language("toml") String... sources) {
        return parse(new InMemoryExecutionContext(), sources);
    }

    @Override
    public boolean accept(Path path) {
        String fileName = path.getFileName().toString();
        return fileName.endsWith(".toml") ||
               "uv.lock".equals(fileName) ||
               "poetry.lock".equals(fileName) ||
               "pdm.lock".equals(fileName) ||
               // Not yet needed by any recipe, and would grow every Rust repository's LST
               // "Cargo.lock".equals(fileName) ||
               "Pipfile".equals(fileName) ||
               ".xmake.cfg".equals(fileName);
    }

    @Override
    public Path sourcePathFromSourceText(Path prefix, String sourceCode) {
        return prefix.resolve("file.toml");
    }

    private static class ForwardingErrorListener extends BaseErrorListener {
        private final Path sourcePath;
        private final ExecutionContext ctx;

        @Nullable
        TomlParsingException firstError;

        private ForwardingErrorListener(Path sourcePath, ExecutionContext ctx) {
            this.sourcePath = sourcePath;
            this.ctx = ctx;
        }

        @Override
        public void syntaxError(Recognizer<?, ?> recognizer, Object offendingSymbol,
                                int line, int charPositionInLine, String msg, RecognitionException e) {
            TomlParsingException error = new TomlParsingException(sourcePath,
                    String.format("Syntax error in %s at line %d:%d %s.", sourcePath, line, charPositionInLine, msg), e);
            if (firstError == null) {
                firstError = error;
            }
            ctx.getOnError().accept(error);
        }
    }

    public static Builder builder() {
        return new Builder();
    }

    public static class Builder extends Parser.Builder {

        public Builder() {
            super(Toml.Document.class);
        }

        @Override
        public TomlParser build() {
            return new TomlParser();
        }

        @Override
        public String getDslName() {
            return "toml";
        }
    }
}
