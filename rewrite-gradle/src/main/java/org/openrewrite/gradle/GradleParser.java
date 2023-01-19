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
import org.openrewrite.gradle.internal.DefaultImportsCustomizer;
import org.openrewrite.groovy.GroovyParser;
import org.openrewrite.groovy.tree.G;
import org.openrewrite.internal.lang.Nullable;

import java.nio.file.Path;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.StreamSupport;

public class GradleParser implements Parser<G.CompilationUnit> {
    private final GroovyParser buildParser;
    private final GroovyParser settingsParser;

    public GradleParser(GroovyParser.Builder groovyParser) {
        GroovyParser.Builder base = groovyParser;
        try {
            base = groovyParser.classpath("gradle-core-api", "gradle-language-groovy", "gradle-language-java", "gradle-resources",
                    "gradle-testing-base", "gradle-testing-jvm", "gradle-enterprise-gradle-plugin");
        } catch (IllegalArgumentException e) {
            // when gradle API has been fatjared into the rewrite-gradle distribution
        }
        this.buildParser = GroovyParser.builder(base)
                .compilerCustomizers(
                        new DefaultImportsCustomizer(),
                        config -> config.setScriptBaseClass("RewriteGradleProject")
                )
                .build();
        this.settingsParser = GroovyParser.builder(base)
                .compilerCustomizers(
                        new DefaultImportsCustomizer(),
                        config -> config.setScriptBaseClass("RewriteSettings")
                )
                .build();
    }

    @Override
    public List<G.CompilationUnit> parseInputs(Iterable<Input> sources, @Nullable Path relativeTo, ExecutionContext ctx) {
        return StreamSupport.stream(sources.spliterator(), false)
              .flatMap(source -> {
                  if (source.getPath().endsWith("settings.gradle")) {
                      return settingsParser.parseInputs(Collections.singletonList(source), relativeTo, ctx).stream();
                  }
                  return buildParser.parseInputs(Collections.singletonList(source), relativeTo, ctx).stream();
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
