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
package org.openrewrite.java;

import com.sun.source.tree.CompilationUnitTree;
import com.sun.source.tree.MethodTree;
import com.sun.source.util.TreeScanner;
import com.sun.tools.javac.code.Type;
import com.sun.tools.javac.tree.JCTree;
import com.sun.tools.javac.util.List;
import org.openrewrite.InMemoryExecutionContext;
import org.openrewrite.Parser;
import org.openrewrite.internal.lang.Nullable;
import org.openrewrite.java.internal.JavaTypeSignatureBuilder;

import java.io.ByteArrayInputStream;
import java.nio.file.Paths;

import static java.util.Collections.singletonList;

public class Java11TypeSignatureBuilderTest implements JavaTypeSignatureBuilderTest {
    private static final String goat = "" +
            "package org.openrewrite.java;" +
            "public interface JavaTypeGoat<T extends JavaTypeGoat<? extends T> & C> {" +
            "    void clazz(C n);" +
            "    void primitive(int n);" +
            "    void array(C[] n);" +
            "    void parameterized(PT<C> n);" +
            "    void generic(PT<? extends C> n);" +
            "    void genericContravariant(PT<? super C> n);" +
            "    <U> void genericUnbounded(PT<U> n);" +
            "}" +
            "interface C {}" +
            "interface PT<T> {}";

    private static final JCTree.JCCompilationUnit cu = Java11Parser.builder()
            .logCompilationWarningsAndErrors(true)
            .build()
            .parseInputsToCompilerAst(
                    singletonList(new Parser.Input(Paths.get("JavaTypeGoat.java"), () -> new ByteArrayInputStream(goat.getBytes()))),
                    new InMemoryExecutionContext(Throwable::printStackTrace))
            .entrySet()
            .iterator()
            .next()
            .getValue();

    @Override
    public Object firstMethodParameter(String methodName) {
        return new TreeScanner<Type, Integer>() {
            @Override
            public Type visitMethod(MethodTree node, Integer p) {
                JCTree.JCMethodDecl method = (JCTree.JCMethodDecl) node;
                if(method.getName().toString().equals(methodName)) {
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
    public Object classTypeParameter() {
        return new TreeScanner<Type, Integer>() {
            @Override
            public Type visitCompilationUnit(CompilationUnitTree node, Integer integer) {
                JCTree.JCClassDecl classDecl = (JCTree.JCClassDecl) ((JCTree.JCCompilationUnit) node).getTypeDecls().get(0);
                return classDecl.getTypeParameters().get(0).type;
            }
        }.scan(cu, 0);
    }

    @Override
    public JavaTypeSignatureBuilder signatureBuilder() {
        return new Java11TypeSignatureBuilder();
    }
}
