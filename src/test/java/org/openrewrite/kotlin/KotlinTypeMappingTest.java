package org.openrewrite.kotlin;

import org.junit.jupiter.api.Disabled;
import org.openrewrite.InMemoryExecutionContext;
import org.openrewrite.internal.StringUtils;
import org.openrewrite.java.JavaTypeMappingTest;
import org.openrewrite.java.tree.JavaType;
import org.openrewrite.java.tree.TypeUtils;

@Disabled
public class KotlinTypeMappingTest implements JavaTypeMappingTest {
    private static final String goat = StringUtils.readFully(KotlinTypeMappingTest.class.getResourceAsStream("/KotlinTypeGoat.kt"));

    @Override
    public JavaType.FullyQualified classType(String fqn) {
        throw new UnsupportedOperationException("TODO");
    }

    @Override
    public JavaType.Parameterized goatType() {
        //noinspection ConstantConditions
        return TypeUtils.asParameterized(KotlinParser.builder()
          .logCompilationWarningsAndErrors(true)
          .build()
          .parse(new InMemoryExecutionContext(), goat)
          .get(0)
          .getClasses()
          .get(0)
          .getType()
        );
    }
}
