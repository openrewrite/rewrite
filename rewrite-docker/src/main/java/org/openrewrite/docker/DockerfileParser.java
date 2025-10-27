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
package org.openrewrite.docker;

import org.antlr.v4.runtime.*;
import org.intellij.lang.annotations.Language;
import org.jspecify.annotations.Nullable;
import org.openrewrite.ExecutionContext;
import org.openrewrite.InMemoryExecutionContext;
import org.openrewrite.Parser;
import org.openrewrite.SourceFile;
import org.openrewrite.docker.internal.DockerfileParserVisitor;
import org.openrewrite.docker.internal.grammar.DockerfileLexer;
import org.openrewrite.docker.tree.Dockerfile;
import org.openrewrite.tree.ParseError;
import org.openrewrite.tree.ParsingEventListener;
import org.openrewrite.tree.ParsingExecutionContextView;

import java.io.InputStream;
import java.nio.file.Path;
import java.util.regex.Pattern;
import java.util.stream.Stream;

public class DockerfileParser implements Parser {
    private static final Pattern DOCKERFILE_PATTERN = Pattern.compile("(?i)(dockerfile|containerfile).*");

    @Override
    public Stream<SourceFile> parseInputs(Iterable<Input> sourceFiles, @Nullable Path relativeTo, ExecutionContext ctx) {
        ParsingEventListener parsingListener = ParsingExecutionContextView.view(ctx).getParsingListener();
        return acceptedInputs(sourceFiles).map(input -> {
            parsingListener.startedParsing(input);
            try (InputStream sourceStream = input.getSource(ctx)) {
                DockerfileLexer lexer = new DockerfileLexer(CharStreams.fromStream(sourceStream));
                lexer.removeErrorListeners();
                lexer.addErrorListener(new ForwardingErrorListener(input.getPath(), ctx));

                org.openrewrite.docker.internal.grammar.DockerfileParser parser =
                    new org.openrewrite.docker.internal.grammar.DockerfileParser(new CommonTokenStream(lexer));
                parser.removeErrorListeners();
                parser.addErrorListener(new ForwardingErrorListener(input.getPath(), ctx));

                Dockerfile.Document document = new DockerfileParserVisitor(
                        input.getRelativePath(relativeTo),
                        input.getFileAttributes(),
                        input.getSource(ctx)
                ).visitDockerfile(parser.dockerfile());

                parsingListener.parsed(input, document);
                return requirePrintEqualsInput(document, input, relativeTo, ctx);
            } catch (Throwable t) {
                ctx.getOnError().accept(t);
                return ParseError.build(this, input, relativeTo, ctx, t);
            }
        });
    }

    @Override
    public Stream<SourceFile> parse(@Language("dockerfile") String... sources) {
        return parse(new InMemoryExecutionContext(), sources);
    }

    @Override
    public boolean accept(Path path) {
        String fileName = path.getFileName().toString();
        return DOCKERFILE_PATTERN.matcher(fileName).matches() ||
               fileName.endsWith(".dockerfile") ||
               fileName.endsWith(".containerfile");
    }

    @Override
    public Path sourcePathFromSourceText(Path prefix, String sourceCode) {
        return prefix.resolve("Dockerfile");
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
            ctx.getOnError().accept(new DockerfileParsingException(sourcePath,
                    String.format("Syntax error in %s at line %d:%d %s.", sourcePath, line, charPositionInLine, msg), e));
        }
    }

    public static Builder builder() {
        return new Builder();
    }

    public static class Builder extends Parser.Builder {

        public Builder() {
            super(Dockerfile.Document.class);
        }

        @Override
        public DockerfileParser build() {
            return new DockerfileParser();
        }

        @Override
        public String getDslName() {
            return "dockerfile";
        }
    }
}
