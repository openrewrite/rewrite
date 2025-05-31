/*
 * Copyright 2025 the original author or authors.
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
import org.openrewrite.ExecutionContext;
import org.openrewrite.java.tree.J;
import org.openrewrite.test.RewriteTest;

import static org.openrewrite.java.Assertions.java;
import static org.openrewrite.test.RewriteTest.toRecipe;

class JavaTemplateJavadocTest implements RewriteTest {

    @Test
    void methodInvocationReplacement() {
        // Extracted from org.openrewrite.java.migrate.net.MigrateURLDecoderDecode
        rewriteRun(
          spec -> spec.recipe(toRecipe(() -> new JavaIsoVisitor<>() {
              @Override
                public J.MethodInvocation visitMethodInvocation(J.MethodInvocation m, ExecutionContext ctx) {
                    m = JavaTemplate.builder("#{any(String)}, StandardCharsets.UTF_8")
                      .contextSensitive()
                      .imports("java.nio.charset.StandardCharsets")
                      .build().apply(
                        getCursor(),
                        m.getCoordinates().replaceArguments(),
                        m.getArguments().toArray());
                    return super.visitMethodInvocation(m, ctx);
                }
          })),
          java(
            """
            /**
             * @see java.net.URLDecoder#decode(String)
             */
            class A {}
            """
            // TODO ideally we should change the reference to: `@see java.net.URLDecoder#decode(String, Charset)`,
            // This requires changes to handling of Javadocs in JavaTemplate.
          )
        );
    }
}
