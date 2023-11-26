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
package org.openrewrite.protobuf;

import org.antlr.v4.runtime.*;
import org.intellij.lang.annotations.Language;
import org.openrewrite.ExecutionContext;
import org.openrewrite.InMemoryExecutionContext;
import org.openrewrite.Parser;
import org.openrewrite.SourceFile;
import org.openrewrite.internal.EncodingDetectingInputStream;
import org.openrewrite.internal.lang.Nullable;
import org.openrewrite.marker.Markers;
import org.openrewrite.protobuf.internal.ProtoParserVisitor;
import org.openrewrite.protobuf.internal.grammar.Protobuf2Lexer;
import org.openrewrite.protobuf.internal.grammar.Protobuf2Parser;
import org.openrewrite.protobuf.tree.Proto;
import org.openrewrite.text.PlainText;
import org.openrewrite.tree.ParseError;
import org.openrewrite.tree.ParsingEventListener;
import org.openrewrite.tree.ParsingExecutionContextView;

import java.nio.file.Path;
import java.util.stream.Stream;

import static org.openrewrite.Tree.randomId;

public class ProtoParser implements Parser {

    @Override
    public Stream<SourceFile> parseInputs(Iterable<Input> sourceFiles, @Nullable Path relativeTo, ExecutionContext ctx) {
        ParsingEventListener parsingListener = ParsingExecutionContextView.view(ctx).getParsingListener();
        return acceptedInputs(sourceFiles).map(input -> {
                    parsingListener.startedParsing(input);
                    Path path = input.getRelativePath(relativeTo);
                    try {
                        EncodingDetectingInputStream is = input.getSource(ctx);
                        String sourceStr = is.readFully();
                        Protobuf2Parser parser = new Protobuf2Parser(new CommonTokenStream(new Protobuf2Lexer(
                                CharStreams.fromString(sourceStr))));

                        parser.removeErrorListeners();
                        parser.addErrorListener(new ForwardingErrorListener(input.getPath(), ctx));

                        if (sourceStr.contains("proto3")) {
                            // Pending Proto3 support, the best we can do is plain text & not skip files
                            return new PlainText(
                                    randomId(),
                                    path,
                                    Markers.EMPTY,
                                    is.getCharset().name(),
                                    is.isCharsetBomMarked(),
                                    input.getFileAttributes(),
                                    null,
                                    sourceStr,
                                    null
                            );
                        }

                        Proto.Document document = new ProtoParserVisitor(
                                path,
                                input.getFileAttributes(),
                                sourceStr,
                                is.getCharset(),
                                is.isCharsetBomMarked()
                        ).visitProto(parser.proto());
                        parsingListener.parsed(input, document);
                        return requirePrintEqualsInput(document, input, relativeTo, ctx);
                    } catch (Throwable t) {
                        ctx.getOnError().accept(t);
                        return ParseError.build(this, input, relativeTo, ctx, t);
                    }
                });
    }

    @Override
    public Stream<SourceFile> parse(@Language("protobuf") String... sources) {
        return parse(new InMemoryExecutionContext(), sources);
    }

    @Override
    public boolean accept(Path path) {
        return path.toString().endsWith(".proto");
    }

    @Override
    public Path sourcePathFromSourceText(Path prefix, String sourceCode) {
        return prefix.resolve("file.proto");
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
            ctx.getOnError().accept(new ProtoParsingException(sourcePath,
                    String.format("Syntax error in %s at line %d:%d %s.", sourcePath, line, charPositionInLine, msg), e));
        }
    }


    public static Builder builder() {
        return new Builder();
    }

    public static class Builder extends Parser.Builder {

        public Builder() {
            super(Proto.Document.class);
        }

        @Override
        public ProtoParser build() {
            return new ProtoParser();
        }

        @Override
        public String getDslName() {
            return "proto";
        }
    }
}
