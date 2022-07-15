/*
 * Copyright 2021 the original author or authors.
 * <p>
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * <p>
 * https://www.apache.org/licenses/LICENSE-2.0
 * <p>
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.openrewrite.java.isolated;

import com.sun.source.tree.ClassTree;
import com.sun.source.tree.CompilationUnitTree;
import com.sun.source.tree.MethodTree;
import com.sun.source.tree.VariableTree;
import com.sun.source.util.TreeScanner;
import com.sun.tools.javac.code.Type;
import com.sun.tools.javac.tree.JCTree;
import com.sun.tools.javac.util.List;
import org.junit.jupiter.api.Disabled;
import org.openrewrite.InMemoryExecutionContext;
import org.openrewrite.Parser;
import org.openrewrite.internal.StringUtils;
import org.openrewrite.internal.lang.Nullable;
import org.openrewrite.java.JavaTypeSignatureBuilderTest;

import java.io.ByteArrayInputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Paths;

import static java.util.Collections.singletonList;

@Disabled("Test disabled until we can solve how to load the tests in a classloader that allows access to internal types.")
public class Java17TypeSignatureBuilderTest implements JavaTypeSignatureBuilderTest {
    private static final String goat = StringUtils.readFully(
            Java17TypeSignatureBuilderTest.class.getResourceAsStream("/JavaTypeGoat.java"));

    private static final JCTree.JCCompilationUnit cu = ReloadableJava17Parser.builder()
            .logCompilationWarningsAndErrors(true)
            .build()
            .parseInputsToCompilerAst(
                    singletonList(new Parser.Input(Paths.get("JavaTypeGoat.java"), () -> new ByteArrayInputStream(goat.getBytes(StandardCharsets.UTF_8)))),
                    new InMemoryExecutionContext(Throwable::printStackTrace))
            .entrySet()
            .iterator()
            .next()
            .getValue();

    @Override
    public String fieldSignature(String field) {
        return new TreeScanner<String, Integer>() {
            @Override
            public String visitVariable(VariableTree node, Integer integer) {
                if (node.getName().toString().equals(field)) {
                    return signatureBuilder().variableSignature(((JCTree.JCVariableDecl) node).sym);
                }
                //noinspection ConstantConditions
                return null;
            }

            @Nullable
            @Override
            public String reduce(@Nullable String r1, @Nullable String r2) {
                return r1 == null ? r2 : r1;
            }
        }.scan(cu, 0);
    }

    @Override
    public String methodSignature(String methodName) {
        return new TreeScanner<String, Integer>() {
            @Override
            public String visitMethod(MethodTree node, Integer p) {
                JCTree.JCMethodDecl method = (JCTree.JCMethodDecl) node;
                if (method.getName().toString().equals(methodName)) {
                    return signatureBuilder().methodSignature(method.type, method.sym);
                }
                //noinspection ConstantConditions
                return null;
            }

            @Nullable
            @Override
            public String reduce(@Nullable String r1, @Nullable String r2) {
                return r1 == null ? r2 : r1;
            }
        }.scan(cu, 0);
    }

    @Override
    public String constructorSignature() {
        return new TreeScanner<String, Integer>() {
            @Override
            public String visitMethod(MethodTree node, Integer p) {
                JCTree.JCMethodDecl method = (JCTree.JCMethodDecl) node;
                if (method.name.toString().equals("<init>")) {
                    return signatureBuilder().methodSignature(method.type, method.sym);
                }
                //noinspection ConstantConditions
                return null;
            }

            @Nullable
            @Override
            public String reduce(@Nullable String r1, @Nullable String r2) {
                return r2 == null ? r1 : r2;
            }
        }.scan(cu, 0);
    }

    @Override
    public Type firstMethodParameter(String methodName) {
        return new TreeScanner<Type, Integer>() {
            @Override
            public Type visitMethod(MethodTree node, Integer p) {
                JCTree.JCMethodDecl method = (JCTree.JCMethodDecl) node;
                if (method.getName().toString().equals(methodName)) {
                    List<JCTree.JCVariableDecl> params = method.getParameters();
                    return params.iterator().next().type;
                }
                //noinspection ConstantConditions
                return null;
            }

            @Nullable
            @Override
            public Type reduce(@Nullable Type r1, @Nullable Type r2) {
                return r1 == null ? r2 : r1;
            }
        }.scan(cu, 0);
    }

    @Override
    public Object innerClassSignature(String innerClassSimpleName) {
        return new TreeScanner<Type, Integer>() {
            @Override
            public Type visitClass(ClassTree node, Integer integer) {
                JCTree.JCClassDecl clazz = (JCTree.JCClassDecl) node;
                if (innerClassSimpleName.equals(clazz.getSimpleName().toString())) {
                    return clazz.type;
                }
                return super.visitClass(node, integer);
            }

            @Nullable
            @Override
            public Type reduce(@Nullable Type r1, @Nullable Type r2) {
                return r1 == null ? r2 : r1;
            }
        }.scan(cu, 0);
    }

    @Override
    public Object lastClassTypeParameter() {
        return new TreeScanner<Type, Integer>() {
            @Override
            public Type visitCompilationUnit(CompilationUnitTree node, Integer integer) {
                JCTree.JCClassDecl classDecl = (JCTree.JCClassDecl) ((JCTree.JCCompilationUnit) node).getTypeDecls().get(0);
                return classDecl.getTypeParameters().get(1).type;
            }
        }.scan(cu, 0);
    }

    @Override
    public ReloadableJava17TypeSignatureBuilder signatureBuilder() {
        return new ReloadableJava17TypeSignatureBuilder();
    }
}
