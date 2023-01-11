package org.openrewrite.kotlin;

import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.openrewrite.InMemoryExecutionContext;
import org.openrewrite.internal.StringUtils;
import org.openrewrite.java.tree.Flag;
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

    public JavaType.Method methodType(String methodName) {
        JavaType.Method type = goatType().getMethods().stream()
          .filter(m -> m.getName().equals(methodName))
          .findFirst()
          .orElseThrow(() -> new IllegalStateException("Expected to find matching method named " + methodName));
        assertThat(type.getDeclaringType().toString()).isEqualTo("org.openrewrite.kotlin.KotlinTypeGoat");
        return type;
    }

    JavaType firstMethodParameter(String methodName) {
        return methodType(methodName).getParameterTypes().get(0);
    }

    @Test
    void extendsKotlinAny() {
        assertThat(goatType().getSupertype().getFullyQualifiedName()).isEqualTo("kotlin.Any");
    }

    @Test
    void kotlinAnyHasNoSuperType() {
        assertThat(goatType().getSupertype().getSupertype()).isNull();
    }

    @Test
    void interfacesContainImplicitAbstractFlag() {
        JavaType.Class clazz = (JavaType.Class) firstMethodParameter("clazz");
        JavaType.Method methodType = methodType("clazz");
        assertThat(clazz.getFlags()).contains(Flag.Abstract);
        assertThat(methodType.getFlags()).contains(Flag.Abstract);
    }
}
