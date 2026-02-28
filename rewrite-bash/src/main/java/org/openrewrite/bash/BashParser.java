/*
 * Copyright 2026 the original author or authors.
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
package org.openrewrite.bash;

import org.intellij.lang.annotations.Language;
import org.jspecify.annotations.Nullable;
import org.openrewrite.ExecutionContext;
import org.openrewrite.InMemoryExecutionContext;
import org.openrewrite.Parser;
import org.openrewrite.SourceFile;
import org.openrewrite.bash.internal.BashParserVisitor;
import org.openrewrite.bash.tree.Bash;
import org.openrewrite.tree.ParseError;
import org.openrewrite.tree.ParsingEventListener;
import org.openrewrite.tree.ParsingExecutionContextView;

import java.io.InputStream;
import java.nio.file.Path;
import java.util.regex.Pattern;
import java.util.stream.Stream;

public class BashParser implements Parser {
    @Override
    public Stream<SourceFile> parseInputs(Iterable<Input> sourceFiles, @Nullable Path relativeTo, ExecutionContext ctx) {
        ParsingEventListener parsingListener = ParsingExecutionContextView.view(ctx).getParsingListener();
        return acceptedInputs(sourceFiles).map(input -> {
            parsingListener.startedParsing(input);
            try (InputStream sourceStream = input.getSource(ctx)) {
                Bash.Script script = new BashParserVisitor(
                        input.getRelativePath(relativeTo),
                        input.getFileAttributes(),
                        input.getSource(ctx)
                ).parse();

                parsingListener.parsed(input, script);
                return requirePrintEqualsInput(script, input, relativeTo, ctx);
            } catch (Throwable t) {
                ctx.getOnError().accept(t);
                return ParseError.build(this, input, relativeTo, ctx, t);
            }
        });
    }

    @Override
    public Stream<SourceFile> parse(@Language("bash") String... sources) {
        return parse(new InMemoryExecutionContext(), sources);
    }

    private static final Pattern PATTERN = Pattern.compile("\\.sh$|\\.bash$");

    @Override
    public boolean accept(Path path) {
        return PATTERN.matcher(path.getFileName().toString().toLowerCase()).find();
    }

    @Override
    public Path sourcePathFromSourceText(Path prefix, String sourceCode) {
        return prefix.resolve("script.sh");
    }

    public static Builder builder() {
        return new Builder();
    }

    public static class Builder extends Parser.Builder {

        public Builder() {
            super(Bash.Script.class);
        }

        @Override
        public BashParser build() {
            return new BashParser();
        }

        @Override
        public String getDslName() {
            return "bash";
        }
    }
}
