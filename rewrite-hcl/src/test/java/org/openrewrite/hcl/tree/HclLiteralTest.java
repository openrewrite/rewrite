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
package org.openrewrite.hcl.tree;

import org.junit.jupiter.api.Test;
import org.openrewrite.test.RewriteTest;

import static org.assertj.core.api.Assertions.assertThat;
import static org.openrewrite.hcl.Assertions.hcl;

class HclLiteralTest implements RewriteTest {

    @Test
    void nullValue() {
        rewriteRun(
          hcl(
            """
              foo {
                  default = null
              }
              """,
            spec -> spec.afterRecipe(conf -> {
                Hcl.Block block = (Hcl.Block) conf.getBody().get(0);
                Hcl.Attribute default_ = (Hcl.Attribute) block.getBody().get(0);
                Expression val = default_.getValue();
                assertThat(val).isInstanceOf(Hcl.Literal.class);
                assertThat(((Hcl.Literal) val).getValueSource()).isEqualTo("null");
                assertThat(((Hcl.Literal) val).getValue()).isNull();
            })
          )
        );
    }
}
