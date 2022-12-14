/*
 * Copyright 2022 the original author or authors.
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
package org.openrewrite.xml;

import org.junit.jupiter.api.Test;
import org.openrewrite.test.RewriteTest;

import static org.openrewrite.xml.Assertions.xml;

class ChangeTagNameTest implements RewriteTest {

    @Test
    void renamesWhitelistToAllowlist() {
        rewriteRun(
          spec -> spec.recipe(new ChangeTagName("/virtual-patches/enhanced-virtual-patch/whitelist-pattern", "allowlist-pattern", null)),
          xml(
            """
              <virtual-patches>
                  <enhanced-virtual-patch id="evp-name" path="/[request-path]" variable="request.parameters.[paramName]" message="alphabet validation failed" enableAntisamy="false">
                      <whitelist-pattern>^[a-zA-Z]+${'$'}</whitelist-pattern>
                  </enhanced-virtual-patch>
              </virtual-patches>
              """,
            """
              <virtual-patches>
                  <enhanced-virtual-patch id="evp-name" path="/[request-path]" variable="request.parameters.[paramName]" message="alphabet validation failed" enableAntisamy="false">
                      <allowlist-pattern>^[a-zA-Z]+${'$'}</allowlist-pattern>
                  </enhanced-virtual-patch>
              </virtual-patches>
              """
          )
        );
    }
}
