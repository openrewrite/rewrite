/*
 * Copyright 2026 the original author or authors.
 * <p>
 * Licensed under the Moderne Source Available License (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * <p>
 * https://docs.moderne.io/licensing/moderne-source-available-license
 * <p>
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.openrewrite.python;

import org.jspecify.annotations.Nullable;
import org.openrewrite.ExecutionContext;
import org.openrewrite.Parser;
import org.openrewrite.SourceFile;
import org.openrewrite.python.marker.PythonResolutionResult;
import org.openrewrite.python.marker.PythonResolutionResult.Dependency;
import org.openrewrite.python.marker.PythonResolutionResult.PackageManager;
import org.openrewrite.python.marker.PythonResolutionResult.ResolvedDependency;
import org.openrewrite.text.PlainText;
import org.openrewrite.text.PlainTextParser;

import java.nio.file.Path;
import java.util.Collections;
import java.util.List;
import java.util.stream.Stream;

import static org.openrewrite.Tree.randomId;

/**
 * Parser for setup.cfg files that delegates to {@link PlainTextParser} and attaches a
 * {@link PythonResolutionResult} marker with dependency metadata resolved via
 * {@code uv pip install <projectDir>} and {@code uv pip freeze}.
 */
public class SetupCfgParser implements Parser {

    private final PlainTextParser plainTextParser = new PlainTextParser();

    @Override
    public Stream<SourceFile> parseInputs(Iterable<Input> sources, @Nullable Path relativeTo, ExecutionContext ctx) {
        return plainTextParser.parseInputs(sources, relativeTo, ctx).map(sf -> {
            if (!(sf instanceof PlainText)) {
                return sf;
            }
            PlainText text = (PlainText) sf;

            @Nullable Path projectDir = null;
            if (relativeTo != null) {
                Path filePath = relativeTo.resolve(text.getSourcePath());
                projectDir = filePath.getParent();
            }
            Path workspace = DependencyWorkspace.getOrCreateSetuptoolsWorkspace(
                    text.getText(), projectDir);
            if (workspace == null) {
                return sf;
            }

            List<ResolvedDependency> resolvedDeps = RequirementsTxtParser.parseFreezeOutput(workspace);
            if (resolvedDeps.isEmpty()) {
                return sf;
            }

            List<Dependency> deps = RequirementsTxtParser.dependenciesFromResolved(resolvedDeps);

            PythonResolutionResult marker = new PythonResolutionResult(
                    randomId(),
                    null,
                    null,
                    null,
                    null,
                    text.getSourcePath().toString(),
                    null,
                    null,
                    Collections.emptyList(),
                    deps,
                    Collections.emptyMap(),
                    Collections.emptyMap(),
                    Collections.emptyList(),
                    Collections.emptyList(),
                    resolvedDeps,
                    PackageManager.Uv,
                    null
            );

            return text.withMarkers(text.getMarkers().addIfAbsent(marker));
        });
    }

    @Override
    public boolean accept(Path path) {
        return "setup.cfg".equals(path.getFileName().toString());
    }

    @Override
    public Path sourcePathFromSourceText(Path prefix, String sourceCode) {
        return prefix.resolve("setup.cfg");
    }

    public static Builder builder() {
        return new Builder();
    }

    public static class Builder extends Parser.Builder {

        Builder() {
            super(PlainText.class);
        }

        @Override
        public SetupCfgParser build() {
            return new SetupCfgParser();
        }

        @Override
        public String getDslName() {
            return "setup.cfg";
        }
    }
}
