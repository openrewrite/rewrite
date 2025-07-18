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
package org.openrewrite.graphql;

import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import org.antlr.v4.runtime.*;
import org.jspecify.annotations.Nullable;
import org.openrewrite.ExecutionContext;
import org.openrewrite.Parser;
import org.openrewrite.SourceFile;
import org.openrewrite.graphql.internal.GraphQlParserVisitor;
import org.openrewrite.graphql.internal.grammar.GraphQLLexer;
import org.openrewrite.graphql.internal.grammar.GraphQLParser;
import org.openrewrite.graphql.tree.GraphQl;
import org.openrewrite.internal.EncodingDetectingInputStream;
import org.openrewrite.tree.ParseError;
import org.openrewrite.tree.ParsingEventListener;
import org.openrewrite.tree.ParsingExecutionContextView;

import java.nio.charset.Charset;
import java.nio.file.Path;
import java.util.stream.Stream;

@RequiredArgsConstructor(access = AccessLevel.PRIVATE)
public class GraphQlParser implements Parser {
    private final boolean skipMalformed;

    @Override
    public Stream<SourceFile> parseInputs(Iterable<Input> inputs, @Nullable Path relativeTo, ExecutionContext ctx) {
        ParsingEventListener parsingListener = ParsingExecutionContextView.view(ctx).getParsingListener();
        return acceptedInputs(inputs).map(input -> {
            parsingListener.startedParsing(input);
            try (EncodingDetectingInputStream is = input.getSource(ctx)) {
                String sourceStr = is.readFully();
                GraphQl.Document document = parseGraphQl(input.getRelativePath(relativeTo), sourceStr, is.getCharset());
                parsingListener.parsed(input, document);
                return requirePrintEqualsInput(document, input, relativeTo, ctx);
            } catch (Throwable t) {
                ctx.getOnError().accept(t);
                return ParseError.build(this, input, relativeTo, ctx, t);
            }
        });
    }

    private GraphQl.Document parseGraphQl(Path path, String source, @Nullable Charset charset) {
        CharStream charStream = CharStreams.fromString(source);
        GraphQLLexer lexer = new GraphQLLexer(charStream);
        lexer.removeErrorListeners();
        lexer.addErrorListener(new ForwardingErrorListener());

        CommonTokenStream tokens = new CommonTokenStream(lexer);
        GraphQLParser parser = new GraphQLParser(tokens);
        parser.removeErrorListeners();
        parser.addErrorListener(new ForwardingErrorListener());

        GraphQLParser.DocumentContext documentContext = parser.document();
        GraphQlParserVisitor visitor = new GraphQlParserVisitor(path, charset, source);
        return (GraphQl.Document) visitor.visitDocument(documentContext);
    }

    @Override
    public boolean accept(Path path) {
        String name = path.toFile().getName().toLowerCase();
        return name.endsWith(".graphql") || name.endsWith(".gql") || name.endsWith(".graphqls");
    }

    @Override
    public Path sourcePathFromSourceText(Path prefix, String sourceCode) {
        return prefix.resolve("file.graphql");
    }

    public static Builder builder() {
        return new Builder();
    }

    public static class Builder extends Parser.Builder {
        private boolean skipMalformed = false;

        public Builder() {
            super(GraphQl.Document.class);
        }

        public Builder skipMalformed(boolean skipMalformed) {
            this.skipMalformed = skipMalformed;
            return this;
        }

        @Override
        public GraphQlParser build() {
            return new GraphQlParser(skipMalformed);
        }

        @Override
        public String getDslName() {
            return "graphql";
        }
    }

    private static class ForwardingErrorListener extends BaseErrorListener {
        @Override
        public void syntaxError(Recognizer<?, ?> recognizer, @Nullable Object offendingSymbol,
                                int line, int charPositionInLine, String msg, @Nullable RecognitionException e) {
            throw new GraphQlParsingException(String.format("Syntax error at line %d:%d %s", line, charPositionInLine, msg), e);
        }
    }

    public static class GraphQlParsingException extends RuntimeException {
        public GraphQlParsingException(String message, @Nullable Throwable cause) {
            super(message, cause);
        }
    }
}