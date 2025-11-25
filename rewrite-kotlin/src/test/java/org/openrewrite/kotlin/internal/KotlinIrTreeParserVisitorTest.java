package org.openrewrite.kotlin.internal;

import org.intellij.lang.annotations.Language;
import org.junit.jupiter.api.Test;
import org.openrewrite.*;
import org.openrewrite.internal.StringUtils;
import org.openrewrite.kotlin.KotlinParser;
import org.openrewrite.kotlin.tree.K;

import java.io.ByteArrayInputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;

import static java.util.Collections.singletonList;
import static org.junit.jupiter.api.Assertions.*;
import static org.openrewrite.ExecutionContext.REQUIRE_PRINT_EQUALS_INPUT;
import static org.openrewrite.test.RewriteTest.assertContentEquals;

class KotlinIrTreeParserVisitorTest {
    @Language("kotlin")
    private static final String goat = StringUtils.readFully(KotlinIrTreeParserVisitorTest.class.getResourceAsStream("/KotlinTypeGoat.kt"));
    private static final Parser.Input input = new Parser.Input(Path.of("KotlinTypeGoat.kt"), () -> new ByteArrayInputStream(goat.getBytes(StandardCharsets.UTF_8)));

    @Test
    void testParse() {
        KotlinParser psiKotlinParser = KotlinParser.builder().parsePsi(true).logCompilationWarningsAndErrors(true).build();
        InMemoryExecutionContext psiCtx = new InMemoryExecutionContext();
        psiCtx.putMessage(REQUIRE_PRINT_EQUALS_INPUT, false);
        SourceFile actualSourceFile = psiKotlinParser
          .parseInputs(singletonList(input), null, psiCtx)
          .findFirst()
          .orElseThrow();
        KotlinParser irKotlinParser = KotlinParser.builder().parsePsi(false).logCompilationWarningsAndErrors(true).build();
        InMemoryExecutionContext irCtx = new InMemoryExecutionContext();
        psiCtx.putMessage(REQUIRE_PRINT_EQUALS_INPUT, false);

        K.CompilationUnit actual = (K.CompilationUnit) irKotlinParser
          .parseInputs(singletonList(input), null, irCtx)
          .findFirst()
          .orElseThrow();

        assertEquals(
          (K.CompilationUnit) actualSourceFile,
          actual,
          "K.CompilationUnit of KotlinIrTreeParserVisitor does not match output of KotlinTreeParserVisitor"
        );
    }
}