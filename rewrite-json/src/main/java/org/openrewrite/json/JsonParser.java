/*
 * Copyright 2021 the original author or authors.
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
package org.openrewrite.json;

import org.antlr.v4.runtime.*;
import org.intellij.lang.annotations.Language;
import org.openrewrite.Parser;
import org.openrewrite.*;
import org.openrewrite.internal.lang.Nullable;
import org.openrewrite.json.internal.JsonParserVisitor;
import org.openrewrite.json.internal.grammar.JSON5Lexer;
import org.openrewrite.json.internal.grammar.JSON5Parser;
import org.openrewrite.json.tree.Json;
import org.openrewrite.tree.ParseError;
import org.openrewrite.tree.ParsingEventListener;
import org.openrewrite.tree.ParsingExecutionContextView;

import java.io.InputStream;
import java.nio.file.Path;
import java.util.stream.Stream;

public class JsonParser implements Parser {
    @Override
    public Stream<SourceFile> parseInputs(Iterable<Input> sourceFiles, @Nullable Path relativeTo, ExecutionContext ctx) {
        ParsingEventListener parsingListener = ParsingExecutionContextView.view(ctx).getParsingListener();
        return acceptedInputs(sourceFiles).map(input -> {
            parsingListener.startedParsing(input);
            try (InputStream sourceStream = input.getSource(ctx)) {
                JSON5Parser parser = new JSON5Parser(new CommonTokenStream(new JSON5Lexer(
                        CharStreams.fromStream(sourceStream))));

                parser.removeErrorListeners();
                parser.addErrorListener(new ForwardingErrorListener(input.getPath(), ctx));

                Json.Document document = new JsonParserVisitor(
                        input.getRelativePath(relativeTo),
                        input.getFileAttributes(),
                        input.getSource(ctx)
                ).visitJson5(parser.json5());
                parsingListener.parsed(input, document);
                return requirePrintEqualsInput(document, input, relativeTo, ctx);
            } catch (Throwable t) {
                ctx.getOnError().accept(t);
                return ParseError.build(this, input, relativeTo, ctx, t);
            }
        });
    }

    @Override
    public Stream<SourceFile> parse(@Language("Json") String... sources) {
        return parse(new InMemoryExecutionContext(), sources);
    }

    @Override
    public boolean accept(Path path) {
        return path.toString().endsWith(".json");
    }

    @Override
    public Path sourcePathFromSourceText(Path prefix, String sourceCode) {
        return prefix.resolve("file.json");
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
            ctx.getOnError().accept(new JsonParsingException(sourcePath,
                    String.format("Syntax error in %s at line %d:%d %s.", sourcePath, line, charPositionInLine, msg), e));
        }
    }

    public static Builder builder() {
        return new Builder();
    }

    public static class Builder extends org.openrewrite.Parser.Builder {

        public Builder() {
            super(Json.Document.class);
        }

        @Override
        public JsonParser build() {
            return new JsonParser();
        }

        @Override
        public String getDslName() {
            return "json";
        }
    }
}
