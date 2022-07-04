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
import java.util.*;
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
                                Paths.get("./rewrite-cobol/src/model/java/model/openrewrite/cobol/Cobol.java"),
                                Paths.get("./rewrite-cobol/src/model/java/model/openrewrite/cobol/Statement.java")
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
