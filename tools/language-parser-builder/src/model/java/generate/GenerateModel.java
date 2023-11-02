/*
 * Copyright 2022 the original author or authors.
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
package generate;

import lombok.RequiredArgsConstructor;
import org.openrewrite.ExecutionContext;
import org.openrewrite.InMemoryExecutionContext;
import org.openrewrite.Result;
import org.openrewrite.internal.InMemoryLargeSourceSet;
import org.openrewrite.internal.ListUtils;
import org.openrewrite.java.JavaParser;
import org.openrewrite.java.tree.J;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;

import static java.util.Objects.requireNonNull;
import static java.util.stream.Collectors.toList;

@RequiredArgsConstructor
public class GenerateModel {
    static final ExecutionContext ctx = new InMemoryExecutionContext(Throwable::printStackTrace);

    static JavaParser jp() {
        return JavaParser.fromJavaVersion()
                .classpath(JavaParser.runtimeClasspath())
                .logCompilationWarningsAndErrors(true)
                .build();
    }

    final List<J.ClassDeclaration> modelClasses;

    public static void main(String[] args) {
        new GenerateModel(((J.CompilationUnit) jp()
                .parse(
                        List.of(
                                Paths.get("language-parser-builder/src/model/java/model/Ruby.java"),
                                Paths.get("language-parser-builder/src/model/java/model/Key.java"),
                                Paths.get("language-parser-builder/src/model/java/model/TValue.java")
                        ),
                        null,
                        ctx
                )
                .findFirst()
                .orElseThrow())
                .getClasses()
                .get(0)
                .getBody()
                .getStatements()
                .stream()
                .filter(J.ClassDeclaration.class::isInstance)
                .map(J.ClassDeclaration.class::cast)
                .toList()).generate();
    }

    public void generate() {
        List<Result> results = new ArrayList<>();

        Path rubyTreePath = Paths.get("language-parser-builder/src/main/java/org/openrewrite/ruby/tree/Ruby.java");

        List<Path> deps = List.of(
                Paths.get("language-parser-builder/src/main/java/org/openrewrite/ruby/RubyVisitor.java"),
                Paths.get("language-parser-builder/src/main/java/org/openrewrite/ruby/RubyIsoVisitor.java")
        );

        results.addAll(new WriteModel(modelClasses).run(new InMemoryLargeSourceSet(List.of(jp().parse(
                ListUtils.concat(rubyTreePath, deps), null, ctx).findFirst().orElseThrow())), ctx).getChangeset().getAllResults());
        results.addAll(new WriteVisitorMethods(modelClasses).run(new InMemoryLargeSourceSet(jp().parse(
                List.of(
                        Paths.get("language-parser-builder/src/main/java/org/openrewrite/ruby/RubyVisitor.java"),
                        Paths.get("language-parser-builder/src/main/java/org/openrewrite/ruby/RubyIsoVisitor.java")
                ),
                null,
                ctx
        ).collect(toList())), ctx).getChangeset().getAllResults());

        results.addAll(new WritePrinter(modelClasses).run(new InMemoryLargeSourceSet(jp().parse(
                List.of(
                        Paths.get("language-parser-builder/src/main/java/org/openrewrite/ruby/internal/RubyPrinter.java")
                ),
                null,
                ctx
        ).collect(toList())), ctx).getChangeset().getAllResults());

        writeResults(results);

        // TODO Unable to add accessors in the first phase due to some bug in JavaTemplate.
        writeResults(new WritePaddingAccessors().run(new InMemoryLargeSourceSet(jp().parse(List.of(rubyTreePath),
                null, ctx).collect(toList())), ctx).getChangeset().getAllResults());
    }

    private void writeResults(List<Result> results) {
        for (Result result : results) {
            try {
                Files.writeString(requireNonNull(result.getAfter()).getSourcePath(),
                        result.getAfter().printAll());
            } catch (IOException e) {
                throw new UncheckedIOException(e);
            }
        }
    }
}
