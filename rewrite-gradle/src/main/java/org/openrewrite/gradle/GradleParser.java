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
package org.openrewrite.gradle;

import lombok.Getter;
import lombok.Setter;
import lombok.experimental.Accessors;
import org.openrewrite.ExecutionContext;
import org.openrewrite.Parser;
import org.openrewrite.groovy.GroovyParser;
import org.openrewrite.groovy.tree.G;
import org.openrewrite.internal.StringUtils;
import org.openrewrite.internal.lang.Nullable;
import org.openrewrite.java.tree.J;
import org.openrewrite.java.tree.Statement;

import java.io.ByteArrayInputStream;
import java.io.SequenceInputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.StreamSupport;

import static java.util.Arrays.asList;

public class GradleParser implements Parser<G.CompilationUnit> {
    @SuppressWarnings("ConstantConditions")
    private static final byte[] PREAMBLE = StringUtils.readFully(GroovyParser.class.getResourceAsStream("/RewriteGradleProject.groovy"), StandardCharsets.UTF_8)
            .trim()
            .replaceAll("\\n}}$", "")
            .getBytes(StandardCharsets.UTF_8);

    @SuppressWarnings("ConstantConditions")
    private static final byte[] SETTINGS_PREAMBLE = StringUtils.readFully(GroovyParser.class.getResourceAsStream("/RewriteSettings.groovy"), StandardCharsets.UTF_8)
            .trim()
            .replaceAll("\\n}}$", "")
            .getBytes(StandardCharsets.UTF_8);

    private GroovyParser groovyParser;

    public GradleParser(GroovyParser.Builder groovyParser) {
        try {
            this.groovyParser = groovyParser
                    .classpath("gradle-core-api", "gradle-language-groovy", "gradle-language-java", "gradle-resources",
                            "gradle-testing-base", "gradle-testing-jvm", "gradle-enterprise-gradle-plugin")
                    .build();
        } catch (IllegalArgumentException e) {
            // when gradle API has been fatjared into the rewrite-gradle distribution
            this.groovyParser = groovyParser.build();
        }
    }

    @Override
    public List<G.CompilationUnit> parseInputs(Iterable<Input> sources, @Nullable Path relativeTo, ExecutionContext ctx) {
        Iterable<Input> gradleWrapped = StreamSupport.stream(sources.spliterator(), false)
                .map(source ->
                        new Parser.Input(
                                source.getPath(), source.getFileAttributes(),
                                () -> new SequenceInputStream(
                                        Collections.enumeration(asList(
                                                new ByteArrayInputStream(source.getPath().endsWith(Paths.get("settings.gradle")) ?
                                                        SETTINGS_PREAMBLE : PREAMBLE),
                                                source.getSource(ctx),
                                                new ByteArrayInputStream(new byte[]{'}', '}'})
                                        ))
                                ),
                                source.isSynthetic()
                        )
                )
                .collect(Collectors.toList());

        return groovyParser.parseInputs(gradleWrapped, relativeTo, ctx).stream()
                .map(cu -> {
                    List<Statement> projectBody = cu.getClasses()
                            .get(cu.getClasses().size() - 1)
                            .getBody().getStatements();
                    J.MethodDeclaration script = (J.MethodDeclaration) projectBody.get(projectBody.size() - 1);
                    assert script.getBody() != null;
                    return cu.withStatements(script.getBody().getStatements());
                })
                .collect(Collectors.toList());
    }

    @Override
    public boolean accept(Path path) {
        return path.toString().endsWith(".gradle");
    }

    @Override
    public Path sourcePathFromSourceText(Path prefix, String sourceCode) {
        return prefix.resolve("build.gradle");
    }

    public static Builder builder() {
        return new Builder();
    }

    @Accessors(chain=true)
    @Setter
    @Getter
    public static class Builder extends Parser.Builder {
        protected GroovyParser.Builder groovyParser = GroovyParser.builder();

        public Builder() {
            super(G.CompilationUnit.class);
        }

        public GradleParser build() {
            return new GradleParser(groovyParser);
        }

        @Override
        public String getDslName() {
            return "gradle";
        }
    }
}
