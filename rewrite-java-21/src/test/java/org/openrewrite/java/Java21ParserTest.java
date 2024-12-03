/*
 * Copyright 2024 the original author or authors.
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

import org.junit.jupiter.api.Test;
import org.openrewrite.InMemoryExecutionContext;
import org.openrewrite.SourceFile;
import org.openrewrite.java.tree.J;
import org.openrewrite.java.tree.JavaType;
import org.openrewrite.test.RewriteTest;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class Java21ParserTest implements RewriteTest {

    @Test
    void shouldLoadResourceFromClasspath() throws IOException {
        Files.deleteIfExists(Paths.get(System.getProperty("user.home"), ".rewrite", "classpath", "jackson-annotations-2.17.1.jar"));
        rewriteRun(spec -> spec.parser(JavaParser.fromJavaVersion().classpathFromResources(new InMemoryExecutionContext(), "jackson-annotations")));
    }

    @Test
    void testPreserveAnnotationsFromClasspath() throws IOException {
        JavaParser p = JavaParser.fromJavaVersion().build();
        /**
         *     Using these annotations in core library for testing this feature:
         *
         *     @Deprecated(since="1.2", forRemoval=true)
         *     public final void stop()
         *
         *     @CallerSensitive
         *     public ClassLoader getContextClassLoader() {
         */
        List<SourceFile> sourceFiles = p.parse(
            """
              class Test {
                public void test() {
                  Thread.currentThread().stop();
                  Thread.currentThread().getContextClassLoader();
                }
              }
              """
          ).toList();
        J.CompilationUnit cu = (J.CompilationUnit) sourceFiles.get(0);

        J.MethodDeclaration md = (J.MethodDeclaration) cu.getClasses().get(0).getBody().getStatements().get(0);
        J.MethodInvocation mi = (J.MethodInvocation) md.getBody().getStatements().get(0);
        JavaType.Annotation annotation = (JavaType.Annotation) mi.getMethodType().getAnnotations().get(0);

        // Thread.currentThread().stop();
        assertEquals("java.lang.Deprecated" ,annotation.type.getFullyQualifiedName());
        assertEquals("since", annotation.values.get(0).getMethod().getName());
        assertEquals("1.2", annotation.values.get(0).getValue());
        assertEquals("forRemoval", annotation.values.get(1).getMethod().getName());
        assertEquals("true", annotation.values.get(1).getValue());

        // Thread.currentThread().getContextClassLoader();
        mi = (J.MethodInvocation) md.getBody().getStatements().get(1);
        annotation = (JavaType.Annotation) mi.getMethodType().getAnnotations().get(0);
        assertEquals("jdk.internal.reflect.CallerSensitive" ,annotation.type.getFullyQualifiedName());
        assertTrue(annotation.values.isEmpty());
    }
}
