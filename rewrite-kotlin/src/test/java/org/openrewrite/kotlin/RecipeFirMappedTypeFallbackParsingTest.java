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
package org.openrewrite.kotlin;

import org.junit.jupiter.api.Test;
import org.openrewrite.ExecutionContext;
import org.openrewrite.InMemoryExecutionContext;
import org.openrewrite.SourceFile;
import org.openrewrite.java.JavaIsoVisitor;
import org.openrewrite.java.JavaParser;
import org.openrewrite.java.tree.J;
import org.openrewrite.java.tree.JavaType;

import java.nio.file.Path;
import java.util.List;
import java.util.concurrent.atomic.AtomicReference;

import static java.util.Collections.emptyList;
import static org.assertj.core.api.Assertions.assertThat;

/**
 * Invariant guard: ordinary {@link KotlinParser} parsing must attribute calls to
 * Java&#8596;Kotlin mapped-class stdlib extensions (e.g. {@code String.toUpperCase},
 * {@code trim}, {@code substring}, {@code isEmpty}) to the canonical
 * {@code kotlin.text.StringsKt} owner — never the recipe-DSL plugin's synthetic
 * facade {@code __GENERATED__CALLABLES__Kt}.
 *
 * <p>The recipe-DSL compiler plugin ({@code RecipeFirMappedTypeFallbackExtension})
 * is NOT active during {@code KotlinParser} parsing, so parsing already resolves
 * the real {@code StringsKt} regardless of classpath — this test documents and
 * locks in that invariant for both an empty classpath and a real one
 * ({@link JavaParser#runtimeClasspath()}). The user-visible regression caused by
 * the plugin's over-broad stand-in generation manifested at recipe-DSL <em>authoring</em>
 * time (the generated {@code MethodMatcher} recorded the synthetic facade); that
 * path is covered decisively by
 * {@code org.openrewrite.kotlin.recipe.MappedTypeFallbackShadowingTest}.
 */
class RecipeFirMappedTypeFallbackParsingTest {

    private static String declaringType(List<Path> classpath, String source, String methodName) {
        ExecutionContext ctx = new InMemoryExecutionContext();
        SourceFile cu = KotlinParser.builder()
          .classpath(classpath)
          .build()
          .parse(ctx, source)
          .findFirst()
          .orElseThrow();

        AtomicReference<JavaType.Method> found = new AtomicReference<>();
        new JavaIsoVisitor<ExecutionContext>() {
            @Override
            public J.MethodInvocation visitMethodInvocation(J.MethodInvocation method, ExecutionContext p) {
                if (methodName.equals(method.getSimpleName()) && method.getMethodType() != null) {
                    found.compareAndSet(null, method.getMethodType());
                }
                return super.visitMethodInvocation(method, p);
            }
        }.visit(cu, ctx);

        JavaType.Method methodType = found.get();
        assertThat(methodType)
          .as("method invocation '%s' should have a resolved method type", methodName)
          .isNotNull();
        return methodType.getDeclaringType().toString();
    }

    private static void assertStringsKt(String source, String methodName) {
        assertThat(declaringType(emptyList(), source, methodName))
          .as("empty classpath: %s", methodName)
          .isEqualTo("kotlin.text.StringsKt");
        assertThat(declaringType(JavaParser.runtimeClasspath(), source, methodName))
          .as("runtime classpath: %s", methodName)
          .isEqualTo("kotlin.text.StringsKt");
    }

    @Test
    void toUpperCase() {
        assertStringsKt("val s: String = \"hello\".toUpperCase()", "toUpperCase");
    }

    @Test
    void trim() {
        assertStringsKt("fun f(s: String): String = s.trim()", "trim");
    }

    @Test
    void isEmpty() {
        assertStringsKt("fun f(s: String): Boolean = s.trim().isEmpty()", "isEmpty");
    }

    @Test
    void substring() {
        assertStringsKt("fun f(s: String, n: Int): String = s.substring(0, n)", "substring");
    }
}
