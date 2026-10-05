/*
 * Copyright 2026 the original author or authors.
 * <p>
 * Licensed under the Moderne Source Available License (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * <p>
 * https://docs.moderne.io/licensing/moderne-source-available-license
 * <p>
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.openrewrite.golang.internal;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class ModuleCacheTest {

    @Test
    void uppercaseIsEscapedInBothHalvesOfACoordinate() {
        // A miss here is silent: the cached module reads as absent and gets downloaded again.
        assertThat(ModuleCache.directoryName("github.com/BurntSushi/toml", "v1.0.0-RC1"))
          .isEqualTo("github.com/!burnt!sushi/toml@v1.0.0-!r!c1");

        assertThat(ModuleCache.directoryName("github.com/gin-gonic/gin", "v1.10.0"))
          .isEqualTo("github.com/gin-gonic/gin@v1.10.0");
    }
}
