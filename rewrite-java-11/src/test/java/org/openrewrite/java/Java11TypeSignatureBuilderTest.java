package org.openrewrite.java;

import com.sun.source.tree.MethodTree;
import com.sun.source.util.TreeScanner;
import com.sun.tools.javac.code.Type;
import com.sun.tools.javac.tree.JCTree;
import org.intellij.lang.annotations.Language;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.openrewrite.InMemoryExecutionContext;
import org.openrewrite.Parser;
import org.openrewrite.internal.StringUtils;

import java.io.ByteArrayInputStream;
import java.nio.file.Paths;

import static java.util.Collections.singletonList;
import static org.assertj.core.api.Assertions.assertThat;

class Java11TypeSignatureBuilderTest implements JavaTypeSignatureBuilderTest {
    private final Java11Parser javaParser = Java11Parser.builder().build();
    private final Java11TypeSignatureBuilder signatureBuilder = new Java11TypeSignatureBuilder();

    @BeforeEach
    void before() {
        javaParser.reset();
    }

    @Override
    @Test
    public void arraySignature() {
        methodReturnTypeSignatureEquals("" +
                        "interface Test {\n" +
                        "  Integer[] test();\n" +
                        "}",
                "java.lang.Integer[]"
        );
    }

    @Override
    @Test
    public void classSignature() {
        methodReturnTypeSignatureEquals("" +
                        "import java.io.File;\n" +
                        "interface Test {\n" +
                        "    File test();\n" +
                        "}",
                "java.io.File"
        );
    }

    @Override
    @Test
    public void parameterizedSignature() {
        methodReturnTypeSignatureEquals("" +
                        "import java.util.List;\n" +
                        "interface Test {\n" +
                        "    List<String> test();\n" +
                        "}",
                "java.util.List<java.lang.String>"
        );
    }

    @Override
    @Test
    public void genericTypeVariable() {
        methodReturnTypeSignatureEquals("" +
                        "import java.util.List;\n" +
                        "interface Test {\n" +
                        "    List<? extends String> test();\n" +
                        "}",
                "java.util.List<? extends java.lang.String>"
        );
    }

    @Override
    @Test
    public void unboundedGenericTypeVariable() {
        methodReturnTypeSignatureEquals("" +
                        "import java.util.List;\n" +
                        "interface Test {\n" +
                        "    <T> List<T> test();\n" +
                        "}",
                "java.util.List<T>"
        );
    }

    private void methodReturnTypeSignatureEquals(@Language("java") String source, String signature) {
        assertThat(signatureBuilder.signature(methodReturnType(source))).isEqualTo(signature);
    }

    private Type methodReturnType(@Language("java") String source) {
        JCTree.JCCompilationUnit cu = compilationUnit(source);
        return new TreeScanner<Type, Integer>() {
            @Override
            public Type visitMethod(MethodTree node, Integer integer) {
                return ((JCTree.JCMethodDecl) node).type.getReturnType();
            }
        }.scan(cu, 0);
    }

    private JCTree.JCCompilationUnit compilationUnit(@Language("java") String source) {
        return javaParser.parseInputsToCompilerAst(
                singletonList(new Parser.Input(Paths.get("Test.java"), () -> new ByteArrayInputStream(StringUtils.trimIndent(source).getBytes()))),
                new InMemoryExecutionContext(Throwable::printStackTrace)).entrySet().iterator().next().getValue();
    }
}
