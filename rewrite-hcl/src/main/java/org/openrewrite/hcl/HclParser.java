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
package org.openrewrite.hcl;

import org.antlr.v4.runtime.*;
import org.openrewrite.ExecutionContext;
import org.openrewrite.tree.ParseError;
import org.openrewrite.Parser;
import org.openrewrite.SourceFile;
import org.openrewrite.hcl.internal.HclParserVisitor;
import org.openrewrite.hcl.internal.grammar.HCLLexer;
import org.openrewrite.hcl.internal.grammar.HCLParser;
import org.openrewrite.hcl.tree.Hcl;
import org.openrewrite.internal.EncodingDetectingInputStream;
import org.openrewrite.internal.lang.Nullable;
import org.openrewrite.marker.Markers;
import org.openrewrite.style.NamedStyles;
import org.openrewrite.tree.ParsingEventListener;
import org.openrewrite.tree.ParsingExecutionContextView;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Stream;

public class HclParser implements Parser {
    private final List<NamedStyles> styles;

    private HclParser(List<NamedStyles> styles) {
        this.styles = styles;
    }

    @Override
    public Stream<SourceFile> parseInputs(Iterable<Input> sourceFiles, @Nullable Path relativeTo, ExecutionContext ctx) {
        ParsingEventListener parsingListener = ParsingExecutionContextView.view(ctx).getParsingListener();
        return acceptedInputs(sourceFiles).map(input -> {
            try {
                parsingListener.startedParsing(input);
                EncodingDetectingInputStream is = input.getSource(ctx);
                String sourceStr = is.readFully();

                HCLLexer lexer = new HCLLexer(CharStreams.fromString(sourceStr));
                lexer.removeErrorListeners();
                lexer.addErrorListener(new ForwardingErrorListener(input.getPath(), ctx));

                HCLParser parser = new HCLParser(new CommonTokenStream(lexer));
                parser.removeErrorListeners();
                parser.addErrorListener(new ForwardingErrorListener(input.getPath(), ctx));

                Hcl.ConfigFile configFile = (Hcl.ConfigFile) new HclParserVisitor(
                        input.getRelativePath(relativeTo),
                        sourceStr,
                        is.getCharset(),
                        is.isCharsetBomMarked(),
                        input.getFileAttributes()
                ).visitConfigFile(parser.configFile());

                configFile = configFile.withMarkers(Markers.build(styles));

                parsingListener.parsed(input, configFile);
                return requirePrintEqualsInput(configFile, input, relativeTo, ctx);
            } catch (Throwable t) {
                ctx.getOnError().accept(t);
                return ParseError.build(this, input, relativeTo, ctx, t);
            }
        });
    }

    @Override
    public boolean accept(Path path) {
        return path.toString().endsWith(".tf") || path.toString().endsWith(".tfvars");
    }

    @Override
    public Path sourcePathFromSourceText(Path prefix, String sourceCode) {
        return prefix.resolve("file.tf");
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
            ctx.getOnError().accept(new HclParsingException(sourcePath,
                    String.format("Syntax error in %s at line %d:%d %s.", sourcePath, line, charPositionInLine, msg), e));
        }
    }

    public static Builder builder() {
        return new Builder();
    }

    public static class Builder extends Parser.Builder {
        protected final List<NamedStyles> styles = new ArrayList<>();

        public Builder() {
            super(Hcl.ConfigFile.class);
        }

        public Builder styles(Iterable<? extends NamedStyles> styles) {
            for (NamedStyles style : styles) {
                this.styles.add(style);
            }
            return this;
        }

        public HclParser build() {
            return new HclParser(styles);
        }

        @Override
        public String getDslName() {
            return "hcl";
        }
    }
}
