package org.openrewrite.kotlin;

import org.junit.jupiter.api.Test;
import org.openrewrite.InMemoryExecutionContext;
import org.openrewrite.internal.StringUtils;
import org.openrewrite.java.tree.JavaType;
import org.openrewrite.java.tree.TypeUtils;

import static java.util.Objects.requireNonNull;
import static org.assertj.core.api.Assertions.assertThat;

@SuppressWarnings("ConstantConditions")
public class KotlinTypeMappingTest {
    private static final String goat = StringUtils.readFully(KotlinTypeMappingTest.class.getResourceAsStream("/KotlinTypeGoat.kt"));

    public JavaType.FullyQualified classType(String fqn) {
        throw new UnsupportedOperationException("TODO");
    }

    public JavaType.Parameterized goatType() {
        //noinspection ConstantConditions
        return requireNonNull(TypeUtils.asParameterized(KotlinParser.builder()
               .logCompilationWarningsAndErrors(true)
               .build()
               .parse(new InMemoryExecutionContext(), goat)
               .get(0)
               .getClasses()
               .get(0)
               .getType()));
    }

    @Test
    void extendsKotlinAny() {
        assertThat(goatType().getSupertype().getFullyQualifiedName()).isEqualTo("kotlin.Any");
    }
}
