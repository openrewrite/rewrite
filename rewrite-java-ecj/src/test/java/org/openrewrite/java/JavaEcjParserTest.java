package org.openrewrite.java;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.openrewrite.InMemoryExecutionContext;
import org.openrewrite.java.tree.J;
import org.openrewrite.test.RewriteTest;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

public class JavaEcjParserTest implements RewriteTest {

    @Test
    void parseJavaSourceFromFile(@TempDir Path tempDir) throws Exception {
        File source = new File(tempDir.toFile(), "A.java");
        String initialSource = """
                    import java.io.Serializable;
                    public class A implements Serializable {
                        class B {
                        }
                    }
                """;
        Files.writeString(source.toPath(), initialSource);

        List<J.CompilationUnit> sources = JavaEcjParser.builder().build().parse(List.of(source.toPath()), tempDir, new InMemoryExecutionContext());

        assertThat(sources.get(0).printAll()).isEqualTo(initialSource);
    }
}
