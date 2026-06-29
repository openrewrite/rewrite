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
package org.openrewrite.groovy;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.openrewrite.InMemoryExecutionContext;
import org.openrewrite.Parser;
import org.openrewrite.SourceFile;
import org.openrewrite.tree.ParseError;

import java.io.ByteArrayInputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import static java.util.Collections.singletonList;
import static org.assertj.core.api.Assertions.assertThat;

/**
 * Guards against the failure mode behind "Failed to parse build.gradle at cursor position N" in
 * {@code UpgradeTransitiveDependencyVersion} (and openrewrite/rewrite#7870): a <em>global</em> Groovy AST
 * transformation on the classpath rewrites the AST away from the source, breaking the position-coupled
 * {@link GroovyParserVisitor}.
 *
 * <p>{@code GroovyParser} discovers global AST transformations through a classpath-free loader, so one
 * registered only on the compile classpath is never discovered and cannot run. This doubles as a guard that
 * the per-source {@code transformLoader} stays classpath-free: were it to carry the compile classpath again,
 * the transformation below would run.
 */
class GradleSnippetGlobalTransformReproTest {

    // The exact snippet UpgradeTransitiveDependencyVersion.parseAsGradle() compiles at runtime.
    private static final String SNIPPET =
            "plugins { id 'java' }\n" +
            "dependencies {\n" +
            "    constraints {\n" +
            "    }\n" +
            "}\n";

    @Test
    void parsesCleanlyWithNoGlobalTransformOnClasspath() {
        SourceFile parsed = parse(null);
        assertThat(parsed).isNotInstanceOf(ParseError.class);
    }

    @Test
    void parsesCleanlyEvenWhenAnAstMutatingGlobalTransformIsOnTheClasspath(@TempDir Path tmp) throws Exception {
        // Register a global AST transformation exactly as a real library (Spock, a Gradle plugin, ...) would.
        Path services = tmp.resolve("META-INF/services");
        Files.createDirectories(services);
        Files.write(services.resolve("org.codehaus.groovy.transform.ASTTransformation"),
                InjectClosureStatementTransformation.class.getName().getBytes(StandardCharsets.UTF_8));
        // Sanity: the registration is wired up, so this test cannot pass vacuously.
        assertThat(Files.readString(services.resolve("org.codehaus.groovy.transform.ASTTransformation")))
                .isEqualTo(InjectClosureStatementTransformation.class.getName());

        InjectClosureStatementTransformation.executed = false;

        SourceFile parsed = parse(tmp);

        // An AST-mutating global transform on the classpath must not yield a ParseError
        // ("Failed to parse build.gradle at cursor position N").
        assertThat(parsed).isNotInstanceOf(ParseError.class);
        // Registered only on the compile classpath, which the classpath-free transformLoader never scans.
        assertThat(InjectClosureStatementTransformation.executed)
                .as("a global AST transformation registered only on the compile classpath must not run")
                .isFalse();
    }

    private static SourceFile parse(@org.jspecify.annotations.Nullable Path classpathEntry) {
        GroovyParser.Builder builder = GroovyParser.builder();
        if (classpathEntry != null) {
            builder = builder.classpath(singletonList(classpathEntry));
        }
        List<SourceFile> result = builder.build()
                .parseInputs(singletonList(new Parser.Input(
                        Path.of("build.gradle"),
                        () -> new ByteArrayInputStream(SNIPPET.getBytes(StandardCharsets.UTF_8)))),
                        null,
                        new InMemoryExecutionContext())
                .toList();
        return result.get(0);
    }
}
