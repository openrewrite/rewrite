package org.openrewrite.kotlin;

import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.openrewrite.InMemoryExecutionContext;
import org.openrewrite.Parser;
import org.openrewrite.internal.StringUtils;
import org.openrewrite.kotlin.tree.K;
import org.openrewrite.tree.ParsingExecutionContextView;

import java.io.ByteArrayInputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Paths;

import static java.util.Collections.singletonList;
import static org.assertj.core.api.Assertions.assertThat;

@Disabled
public class KotlinTypeSignatureBuilderTest {
    private static final String goat = StringUtils.readFully(KotlinTypeSignatureBuilderTest.class.getResourceAsStream("/KotlinTypeGoat.kt"));

    private static final K.CompilationUnit cu = KotlinParser.builder()
            .logCompilationWarningsAndErrors(true)
            .build()
            .parseInputs(
             singletonList(new Parser.Input(Paths.get("KotlinTypeGoat.kt"), () -> new ByteArrayInputStream(goat.getBytes(StandardCharsets.UTF_8)))),
             null,
             new ParsingExecutionContextView(new InMemoryExecutionContext(Throwable::printStackTrace)))
             .iterator()
             .next();

    @Test
    void classSignature() {
        assertThat(signatureBuilder().signature(firstMethodParameter("clazz")))
          .isEqualTo("org.openrewrite.java.C");
        assertThat(methodSignature("clazz"))
          .isEqualTo("org.openrewrite.java.JavaTypeGoat{name=clazz,return=void,parameters=[org.openrewrite.java.C]}");
    }

    public Object firstMethodParameter(String methodName) {
        throw new UnsupportedOperationException("TODO");
    }

    public String methodSignature(String methodName) {
        throw new UnsupportedOperationException("TODO");
    }

    public KotlinTypeSignatureBuilder signatureBuilder() {
        // FIX ME. this is an issue ... the mapping in Kotlin requires the FirSession.
        return new KotlinTypeSignatureBuilder(null);
    }
}
