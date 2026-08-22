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
package org.openrewrite.groovy;

import org.junit.jupiter.api.Test;
import org.openrewrite.java.tree.Flag;
import org.openrewrite.java.tree.J;
import org.openrewrite.java.tree.JavaType;
import org.openrewrite.test.RewriteTest;

import java.util.concurrent.atomic.AtomicReference;

import static org.assertj.core.api.Assertions.assertThat;
import static org.openrewrite.groovy.Assertions.groovy;

class GroovyVarargsTypeMappingTest implements RewriteTest {

    @Test
    void varargsMethodHasVarargsFlag() {
        // Groovy resolves library methods from byte code, where varargs is encoded as
        // ACC_VARARGS (0x80) in MethodNode.getModifiers(). That bit collides with
        // Flag.Transient, so without remapping the call would be flagged Transient and
        // lose Varargs -- the upstream cause of UseDiamondOperator's AIOOBE.
        rewriteRun(
          groovy(
            """
              java.util.Arrays.asList("a", "b", "c")
              """,
            spec -> spec.afterRecipe(cu -> {
                AtomicReference<JavaType.Method> ref = new AtomicReference<>();
                new GroovyIsoVisitor<Integer>() {
                    @Override
                    public J.MethodInvocation visitMethodInvocation(J.MethodInvocation method, Integer p) {
                        if (method.getMethodType() != null && "asList".equals(method.getMethodType().getName())) {
                            ref.set(method.getMethodType());
                        }
                        return super.visitMethodInvocation(method, p);
                    }
                }.visit(cu, 0);

                assertThat(ref.get()).as("asList method type resolved").isNotNull();
                assertThat(ref.get().getFlags())
                  .contains(Flag.Varargs)
                  .doesNotContain(Flag.Transient);
            })
          )
        );
    }
}
