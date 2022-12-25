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
package org.openrewrite.java.internal;

import org.junit.jupiter.api.Test;
import org.openrewrite.Issue;
import org.openrewrite.java.tree.TypeUtils;
import org.openrewrite.test.RewriteTest;

import java.util.stream.Collectors;

import static org.assertj.core.api.Assertions.assertThat;
import static org.openrewrite.java.Assertions.java;

@SuppressWarnings("ConstantConditions")
class TypesInUseTest implements RewriteTest {

    @Issue("https://github.com/openrewrite/rewrite/issues/617")
    @Test
    void findAnnotationArgumentType() {
        rewriteRun(
          java(
            """
              package org.openrewrite.test;
                            
              public @interface YesOrNo {
                  Status status();
                  enum Status {
                      YES, NO
                  }
              }
              """
          ),
          java(
            """
              package org.openrewrite.test;
                            
              import static org.openrewrite.test.YesOrNo.Status.YES;
                            
              @YesOrNo(status = YES)
              public class Foo {}
              """,
            spec -> spec.afterRecipe(cu -> {
                var foundTypes = cu.getTypesInUse().getVariables().stream()
                  .map(v -> TypeUtils.asFullyQualified(v.getType()).getFullyQualifiedName())
                  .collect(Collectors.toList());
                assertThat(foundTypes).containsExactlyInAnyOrder("org.openrewrite.test.YesOrNo$Status");
            })
          )
        );
    }
}
