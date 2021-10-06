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

import org.gradle.configuration.DefaultImportsReader;
import org.openrewrite.ExecutionContext;
import org.openrewrite.Parser;
import org.openrewrite.groovy.GroovyParser;
import org.openrewrite.groovy.tree.G;
import org.openrewrite.internal.lang.Nullable;
import org.openrewrite.java.tree.J;

import java.io.ByteArrayInputStream;
import java.io.SequenceInputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.StreamSupport;

public class GradleParser implements Parser<G.CompilationUnit> {
    private final GroovyParser groovyParser;

    private final String preamble;

    public GradleParser(GroovyParser.Builder groovyParser) {
        this.groovyParser = groovyParser
                .classpath("gradle-api")
                .build();

        DefaultImportsReader reader = new DefaultImportsReader();
        preamble = Arrays.stream(reader.getImportPackages())
                .map(i -> "import " + i + ".*")
                .collect(Collectors.joining("\n", "", "\n" +
                        "interface PluginSpec {\n" +
                        "  Plugin id(String i)\n" +
                        "}\n" +
                        "interface Plugin {\n" +
                        "  Plugin version(String v)\n" +
                        "  Plugin apply(boolean a)\n" +
                        "}\n" +
                        "interface DependencyHandlerSpec {\n" +
                        "  org.gradle.api.artifacts.Dependency api(String dependencyNotation)\n" +
                        "}\n" +
                        "class RewriteGradleProject extends " +
                        "org.gradle.api.internal.project.DefaultProject {\n" +
                        "  void dependencies(@DelegatesTo(strategy=Closure.DELEGATE_ONLY, value=DependencyHandlerSpec) Closure cl) {}\n" +
                        "  void plugins(@DelegatesTo(strategy=Closure.DELEGATE_ONLY, value=PluginSpec) Closure cl) {}\n" +
                        "  void __script__() {")
                );
    }

    @Override
    public List<G.CompilationUnit> parseInputs(Iterable<Input> sources, @Nullable Path relativeTo, ExecutionContext ctx) {
        Iterable<Input> gradleWrapped = StreamSupport.stream(sources.spliterator(), false)
                .map(source ->
                        new Parser.Input(
                                source.getPath(),
                                () -> new SequenceInputStream(
                                        Collections.enumeration(Arrays.asList(
                                                new ByteArrayInputStream(preamble.getBytes(StandardCharsets.UTF_8)),
                                                source.getSource(),
                                                new ByteArrayInputStream(new byte[]{'}', '}'})
                                        ))
                                ),
                                source.isSynthetic()
                        )
                )
                .collect(Collectors.toList());

        return groovyParser.parseInputs(gradleWrapped, relativeTo, ctx).stream()
                .map(cu -> {
                    J.MethodDeclaration script = (J.MethodDeclaration) cu.getClasses()
                            .get(cu.getClasses().size() - 1)
                            .getBody().getStatements().get(1);
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
    public Path randomSourceName() {
        return Parser.super.randomSourceName().resolve("/build.gradle");
    }
}
