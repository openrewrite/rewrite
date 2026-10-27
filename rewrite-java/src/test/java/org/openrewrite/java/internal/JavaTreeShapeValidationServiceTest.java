/*
 * Copyright 2026 the original author or authors.
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
package org.openrewrite.java.internal;

import org.jspecify.annotations.Nullable;
import org.junit.jupiter.api.Test;
import org.openrewrite.InMemoryExecutionContext;
import org.openrewrite.Parser;
import org.openrewrite.java.JavaParser;
import org.openrewrite.java.tree.J;
import org.openrewrite.java.tree.JavaType;
import org.openrewrite.java.tree.TypeUtils;

import static java.util.Objects.requireNonNull;
import static org.assertj.core.api.Assertions.assertThat;

class JavaTreeShapeValidationServiceTest {

    private static final String SOURCE = "class A { org.jspecify.annotations.NullMarked m; b.B d; " +
                                         "Object o = java.util.Collections.emptyList(); }";

    @Test
    void untypedParserResolvesNoTypesWhileTheParserItCopiesStillDoes() {
        JavaParser.Builder<?, ?> typed = JavaParser.fromJavaVersion()
                .classpath("jspecify")
                .dependsOn("package b; public class B {}");
        J.ClassDeclaration untyped = parse(new JavaTreeShapeValidationService().untypedParser(typed));

        assertThat(TypeUtils.isOfClassType(fieldType(untyped, 0), "org.jspecify.annotations.NullMarked")).isFalse();
        assertThat(TypeUtils.isOfClassType(fieldType(untyped, 1), "b.B")).isFalse();
        assertThat(emptyList(untyped).getMethodType()).isNull();

        assertThat(TypeUtils.isOfClassType(fieldType(parse(typed), 0), "org.jspecify.annotations.NullMarked")).isTrue();
    }

    private static J.ClassDeclaration parse(Parser.Builder parser) {
        J.CompilationUnit cu = (J.CompilationUnit) parser.build()
                .parse(new InMemoryExecutionContext(), SOURCE)
                .findFirst()
                .orElseThrow(IllegalStateException::new);
        return cu.getClasses().get(0);
    }

    private static @Nullable JavaType fieldType(J.ClassDeclaration a, int index) {
        return ((J.VariableDeclarations) a.getBody().getStatements().get(index)).getVariables().get(0).getType();
    }

    private static J.MethodInvocation emptyList(J.ClassDeclaration a) {
        J.VariableDeclarations o = (J.VariableDeclarations) a.getBody().getStatements().get(2);
        return (J.MethodInvocation) requireNonNull(o.getVariables().get(0).getInitializer());
    }
}
