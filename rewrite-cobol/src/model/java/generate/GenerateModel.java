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
import org.openrewrite.java.JavaParser;
import org.openrewrite.java.tree.J;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

import static java.util.Objects.requireNonNull;

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
        new GenerateModel(jp()
                .parse(
                        Arrays.asList(
                                Paths.get("./rewrite-cobol/src/model/java/model/Cobol.java"),
                                Paths.get("./rewrite-cobol/src/model/java/model/Statement.java")
                        ),
                        null,
                        ctx
                )
                .get(0)
                .getClasses()
                .get(0)
                .getBody()
                .getStatements()
                .stream()
                .filter(J.ClassDeclaration.class::isInstance)
                .map(J.ClassDeclaration.class::cast)
                .collect(Collectors.toList())).generate();
    }

    public void generate() {
        List<Result> results = new ArrayList<>();

        Path cobolTreePath = Paths.get("./rewrite-cobol/src/main/java/org/openrewrite/cobol/tree/Cobol.java");

        results.addAll(new WriteModel(modelClasses).run(jp().parse(Collections.singletonList(cobolTreePath), null, ctx), ctx));
        results.addAll(new WriteVisitorMethods(modelClasses).run(jp().parse(
                Arrays.asList(
                        Paths.get("./rewrite-cobol/src/main/java/org/openrewrite/cobol/CobolVisitor.java"),
                        Paths.get("./rewrite-cobol/src/main/java/org/openrewrite/cobol/CobolIsoVisitor.java")
                ),
                null,
                ctx
        ), ctx));
        results.addAll(new WritePrinter(modelClasses).run(jp().parse(
                Collections.singletonList(
                        Paths.get("./rewrite-cobol/src/main/java/org/openrewrite/cobol/internal/CobolPrinter.java")
                ),
                null,
                ctx
        ), ctx));

        writeResults(results);

        // TODO Unable to add accessors in the first phase due to some bug in JavaTemplate.
        writeResults(new WritePaddingAccessors().run(jp().parse(Collections.singletonList(cobolTreePath), null, ctx), ctx));
    }

    private void writeResults(List<Result> results) {
        for (Result result : results) {
            try {
                Files.write(requireNonNull(result.getAfter()).getSourcePath(),
                        result.getAfter().printAllAsBytes());
            } catch (IOException e) {
                throw new UncheckedIOException(e);
            }
        }
    }
}
