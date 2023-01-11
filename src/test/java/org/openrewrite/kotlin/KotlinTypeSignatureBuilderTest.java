package org.openrewrite.kotlin;

import org.openrewrite.InMemoryExecutionContext;
import org.openrewrite.Parser;
import org.openrewrite.internal.StringUtils;
import org.openrewrite.java.JavaTypeSignatureBuilderTest;
import org.openrewrite.kotlin.tree.K;
import org.openrewrite.tree.ParsingExecutionContextView;

import java.io.ByteArrayInputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Paths;

import static java.util.Collections.singletonList;

public class KotlinTypeSignatureBuilderTest implements JavaTypeSignatureBuilderTest {
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

    public KotlinTypeSignatureBuilder signatureBuilder() {
        // FIX ME. this is an issue ... the mapping in Kotlin requires the FirSession.
        return new KotlinTypeSignatureBuilder(null);
    }

    public Object firstMethodParameter(String methodName) {
        throw new UnsupportedOperationException("TODO");
    }

    @Override
    public Object innerClassSignature(String innerClassSimpleName) {
        return null;
    }

    @Override
    public Object lastClassTypeParameter() {
        return null;
    }

    @Override
    public String fieldSignature(String field) {
        return null;
    }

    public String methodSignature(String methodName) {
        throw new UnsupportedOperationException("TODO");
    }

    @Override
    public String constructorSignature() {
        return null;
    }

    @Override
    public void constructor() {
        JavaTypeSignatureBuilderTest.super.constructor();
    }

    @Override
    public void parameterizedField() {
        JavaTypeSignatureBuilderTest.super.parameterizedField();
    }

    @Override
    public void array() {
        JavaTypeSignatureBuilderTest.super.array();
    }

    @Override
    public void classSignature() {
        JavaTypeSignatureBuilderTest.super.classSignature();
    }

    @Override
    public void primitive() {
        JavaTypeSignatureBuilderTest.super.primitive();
    }

    @Override
    public void parameterized() {
        JavaTypeSignatureBuilderTest.super.parameterized();
    }

    @Override
    public void parameterizedRecursive() {
        JavaTypeSignatureBuilderTest.super.parameterizedRecursive();
    }

    @Override
    public void generic() {
        JavaTypeSignatureBuilderTest.super.generic();
    }

    @Override
    public void genericT() {
        JavaTypeSignatureBuilderTest.super.genericT();
    }

    @Override
    public void genericContravariant() {
        JavaTypeSignatureBuilderTest.super.genericContravariant();
    }

    @Override
    public void genericRecursiveInClassDefinition() {
        JavaTypeSignatureBuilderTest.super.genericRecursiveInClassDefinition();
    }

    @Override
    public void genericRecursiveInMethodDeclaration() {
        JavaTypeSignatureBuilderTest.super.genericRecursiveInMethodDeclaration();
    }

    @Override
    public void genericUnbounded() {
        JavaTypeSignatureBuilderTest.super.genericUnbounded();
    }

    @Override
    public void innerClass() {
        JavaTypeSignatureBuilderTest.super.innerClass();
    }

    @Override
    public void inheritedJavaTypeGoat() {
        JavaTypeSignatureBuilderTest.super.inheritedJavaTypeGoat();
    }

    @Override
    public void extendsJavaTypeGoat() {
        JavaTypeSignatureBuilderTest.super.extendsJavaTypeGoat();
    }

    @Override
    public void genericIntersection() {
        JavaTypeSignatureBuilderTest.super.genericIntersection();
    }

    @Override
    public void recursiveIntersection() {
        JavaTypeSignatureBuilderTest.super.recursiveIntersection();
    }
}
