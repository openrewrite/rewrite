package org.openrewrite.kotlin;

import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.openrewrite.InMemoryExecutionContext;
import org.openrewrite.internal.StringUtils;
import org.openrewrite.java.tree.J;
import org.openrewrite.java.tree.JavaType;
import org.openrewrite.java.tree.TypeUtils;
import org.openrewrite.kotlin.tree.K;

import java.util.List;

import static java.util.Objects.requireNonNull;
import static org.assertj.core.api.Assertions.assertThat;

@SuppressWarnings("ConstantConditions")
public class KotlinTypeSignatureBuilderTest {
    private static final String goat = StringUtils.readFully(KotlinTypeSignatureBuilderTest.class.getResourceAsStream("/KotlinTypeGoat.kt"));

    private static final K.CompilationUnit cu = KotlinParser.builder()
      .logCompilationWarningsAndErrors(true)
      .build()
      .parse(new InMemoryExecutionContext(), goat)
      .get(0);

    public JavaType.Parameterized goatType() {
        return requireNonNull(TypeUtils.asParameterized(cu
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

    public KotlinTypeSignatureBuilder signatureBuilder() {
        // FIX ME. this is an issue ... the mapping in Kotlin requires the FirSession.
        return new KotlinTypeSignatureBuilder(null);
    }

    public Object firstMethodParameter(String methodName) {
        return methodType(methodName).getParameterTypes().get(0);
    }

    public Object firstMethodParameterSignature(String methodName) {
        return firstMethodParameter(methodName).toString();
    }

    public Object innerClassSignature(String innerClassSimpleName) {
        return cu.getClasses().stream()
                .flatMap(it -> it.getBody().getStatements().stream())
                .filter(it -> it instanceof J.ClassDeclaration)
                .map(it -> (J.ClassDeclaration) it)
                .filter(it -> it.getSimpleName().equals(innerClassSimpleName))
                .findAny()
                .orElseThrow()
                .getType()
                .toString();
    }

    public Object lastClassTypeParameter() {
        List<JavaType> typeParameters = goatType().getTypeParameters();
        return typeParameters.get(typeParameters.size() - 1).toString();
    }

    public String fieldSignature(String field) {
        JavaType.Variable type = goatType().getType().getMembers().stream()
               .filter(m -> m.getName().equals(field))
               .findFirst()
               .orElseThrow(() -> new IllegalStateException("Expected to find matching field named " + field));
        return type.toString();
    }

    public String methodSignature(String methodName) {
        JavaType.Method type = goatType().getType().getMethods().stream()
          .filter(m -> m.getName().equals(methodName))
          .findAny()
          .orElseThrow();
        return type.toString();
    }

    public String constructorSignature() {
        JavaType.Method ctor = methodType("<constructor>");
        return ctor.toString();
    }

    @Test
    public void constructor() {
        assertThat(constructorSignature())
          .isEqualTo("org.openrewrite.kotlin.KotlinTypeGoat{name=<constructor>,return=org.openrewrite.kotlin.KotlinTypeGoat,parameters=[]}");
    }

    @Test
    public void parameterizedField() {
        assertThat(fieldSignature("parameterizedField"))
          .isEqualTo("org.openrewrite.kotlin.KotlinTypeGoat{name=parameterizedField,type=org.openrewrite.kotlin.PT<org.openrewrite.kotlin.KotlinTypeGoat$TypeA>}");
    }

    @Test
    public void classSignature() {
        assertThat(firstMethodParameterSignature("clazz"))
          .isEqualTo("org.openrewrite.kotlin.C");
        assertThat(methodSignature("clazz"))
          .isEqualTo("org.openrewrite.kotlin.KotlinTypeGoat{name=clazz,return=kotlin.Unit,parameters=[org.openrewrite.kotlin.C]}");
    }

    @Test
    public void parameterized() {
        assertThat(firstMethodParameterSignature("parameterized"))
          .isEqualTo("org.openrewrite.kotlin.PT<org.openrewrite.kotlin.C>");
        assertThat(methodSignature("parameterized"))
          .isEqualTo("org.openrewrite.kotlin.KotlinTypeGoat{name=parameterized,return=org.openrewrite.kotlin.PT<org.openrewrite.kotlin.C>,parameters=[org.openrewrite.kotlin.PT<org.openrewrite.kotlin.C>]}");
    }

    @Test
    public void parameterizedRecursive() {
        assertThat(firstMethodParameterSignature("parameterizedRecursive"))
          .isEqualTo("org.openrewrite.kotlin.PT<org.openrewrite.kotlin.PT<org.openrewrite.kotlin.C>>");
        assertThat(methodSignature("parameterizedRecursive"))
          .isEqualTo("org.openrewrite.kotlin.KotlinTypeGoat{name=parameterizedRecursive,return=org.openrewrite.kotlin.PT<org.openrewrite.kotlin.PT<org.openrewrite.kotlin.C>>,parameters=[org.openrewrite.kotlin.PT<org.openrewrite.kotlin.PT<org.openrewrite.kotlin.C>>]}");
    }

    @Test
    public void generic() {
        assertThat(firstMethodParameterSignature("generic"))
          .isEqualTo("org.openrewrite.kotlin.PT<Generic{ extends org.openrewrite.kotlin.C}>");
        assertThat(methodSignature("generic"))
          .isEqualTo("org.openrewrite.kotlin.KotlinTypeGoat{name=generic,return=org.openrewrite.kotlin.PT<Generic{ extends org.openrewrite.kotlin.C}>,parameters=[org.openrewrite.kotlin.PT<Generic{ extends org.openrewrite.kotlin.C}>]}");
    }

    @Test
    public void genericT() {
        assertThat(firstMethodParameterSignature("genericT"))
          .isEqualTo("Generic{T}");
        assertThat(methodSignature("genericT"))
          .isEqualTo("org.openrewrite.kotlin.KotlinTypeGoat{name=genericT,return=Generic{T},parameters=[Generic{T}]}");
    }

    @Test
    public void genericContravariant() {
        assertThat(firstMethodParameterSignature("genericContravariant"))
          .isEqualTo("org.openrewrite.kotlin.PT<Generic{ super org.openrewrite.kotlin.C}>");
        assertThat(methodSignature("genericContravariant"))
          .isEqualTo("org.openrewrite.kotlin.KotlinTypeGoat{name=genericContravariant,return=org.openrewrite.kotlin.PT<Generic{ super org.openrewrite.kotlin.C}>,parameters=[org.openrewrite.kotlin.PT<Generic{ super org.openrewrite.kotlin.C}>]}");
    }

    @Disabled("Requires parsing intersection types")
    @Test
    public void genericRecursiveInClassDefinition() {
        assertThat(lastClassTypeParameter())
          .isEqualTo("Generic{S extends org.openrewrite.kotlin.PT<Generic{S}> & org.openrewrite.kotlin.C}");
    }

    @Disabled
    @Test
    public void genericRecursiveInMethodDeclaration() {
        // <U : KotlinTypeGoat<U, *>> genericRecursive(n: KotlinTypeGoat<out Array<U>, *>): KotlinTypeGoat<out Array<U>, *>
        assertThat(firstMethodParameterSignature("genericRecursive"))
          .isEqualTo("org.openrewrite.kotlin.KotlinTypeGoat<Generic{ extends kotlin.Array<Generic{U}>, Generic{*}>");
//        assertThat(methodSignature("genericRecursive"))
//          .isEqualTo("org.openrewrite.kotlin.KotlinTypeGoat{name=genericRecursive,return=org.openrewrite.kotlin.KotlinTypeGoat<Generic{? extends Generic{U extends org.openrewrite.kotlin.KotlinTypeGoat<Generic{U}, Generic{?}>}[]}, Generic{?}>,parameters=[org.openrewrite.kotlin.KotlinTypeGoat<Generic{? extends Generic{U extends org.openrewrite.kotlin.KotlinTypeGoat<Generic{U}, Generic{?}>}[]}, Generic{?}>]}");
    }

    @Test
    public void genericUnbounded() {
        assertThat(firstMethodParameterSignature("genericUnbounded"))
          .isEqualTo("org.openrewrite.kotlin.PT<Generic{U}>");
        assertThat(methodSignature("genericUnbounded"))
          .isEqualTo("org.openrewrite.kotlin.KotlinTypeGoat{name=genericUnbounded,return=org.openrewrite.kotlin.PT<Generic{U}>,parameters=[org.openrewrite.kotlin.PT<Generic{U}>]}");
    }

    @Test
    public void innerClass() {
        assertThat(firstMethodParameterSignature("inner"))
          .isEqualTo("org.openrewrite.kotlin.C$Inner");
        assertThat(methodSignature("inner"))
          .isEqualTo("org.openrewrite.kotlin.KotlinTypeGoat{name=inner,return=kotlin.Unit,parameters=[org.openrewrite.kotlin.C$Inner]}");
    }

    @Disabled("Requires parsing intersection types")
    @Test
    public void inheritedJavaTypeGoat() {
        assertThat(firstMethodParameterSignature("inheritedJavaTypeGoat"))
          .isEqualTo("org.openrewrite.java.JavaTypeGoat$InheritedJavaTypeGoat<Generic{T}, Generic{U extends org.openrewrite.java.PT<Generic{U}> & org.openrewrite.java.C}>");
        assertThat(methodSignature("inheritedJavaTypeGoat"))
          .isEqualTo("org.openrewrite.java.JavaTypeGoat{name=inheritedJavaTypeGoat,return=org.openrewrite.java.JavaTypeGoat$InheritedJavaTypeGoat<Generic{T}, Generic{U extends org.openrewrite.java.PT<Generic{U}> & org.openrewrite.java.C}>,parameters=[org.openrewrite.java.JavaTypeGoat$InheritedJavaTypeGoat<Generic{T}, Generic{U extends org.openrewrite.java.PT<Generic{U}> & org.openrewrite.java.C}>]}");
    }

    @Disabled("Requires reference of type params from parent class")
    @Test
    public void extendsJavaTypeGoat() {
        assertThat(innerClassSignature("ExtendsKotlinTypeGoat"))
          .isEqualTo("org.openrewrite.kotlin.KotlinTypeGoat$ExtendsKotlinTypeGoat");
    }

    @Disabled("Requires parsing intersection types")
    @Test
    public void genericIntersection() {
        assertThat(signatureBuilder().signature(firstMethodParameter("genericIntersection")))
          .isEqualTo("Generic{U extends org.openrewrite.java.JavaTypeGoat$TypeA & org.openrewrite.java.PT<Generic{U}> & org.openrewrite.java.C}");
        assertThat(methodSignature("genericIntersection"))
          .isEqualTo("org.openrewrite.java.JavaTypeGoat{name=genericIntersection,return=Generic{U extends org.openrewrite.java.JavaTypeGoat$TypeA & org.openrewrite.java.PT<Generic{U}> & org.openrewrite.java.C},parameters=[Generic{U extends org.openrewrite.java.JavaTypeGoat$TypeA & org.openrewrite.java.PT<Generic{U}> & org.openrewrite.java.C}]}");
    }

    @Disabled("Requires parsing intersection types")
    @Test
    public void recursiveIntersection() {
        assertThat(signatureBuilder().signature(firstMethodParameter("recursiveIntersection")))
          .isEqualTo("Generic{U extends org.openrewrite.java.JavaTypeGoat$Extension<Generic{U}> & org.openrewrite.java.Intersection<Generic{U}>}");
        assertThat(methodSignature("recursiveIntersection"))
          .isEqualTo("org.openrewrite.java.JavaTypeGoat{name=recursiveIntersection,return=void,parameters=[Generic{U extends org.openrewrite.java.JavaTypeGoat$Extension<Generic{U}> & org.openrewrite.java.Intersection<Generic{U}>}]}");
    }
}
