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
package org.openrewrite.javascript.internal.lock.berry;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * plugin-jsr helpers.ts: a jsr package is the npm package of its name wrapped into the {@code jsr} scope.
 */
class JsrResolverTest {

    @Test
    void wrapsAJsrPackageIntoItsNpmScope() {
        Descriptor scoped = StructUtils.parseDescriptor("@std/path@jsr:^1.0.0");
        assertThat(StructUtils.stringifyDescriptor(JsrResolver.convertDescriptorFromJsrToNpm(scoped)))
                .isEqualTo("@std/path@npm:@jsr/std__path@^1.0.0");
        Locator npm = StructUtils.parseLocator("@jsr/std__path@npm:1.0.8");
        assertThat(StructUtils.stringifyLocator(JsrResolver.convertLocatorFromNpmToJsr(npm)))
                .isEqualTo("@std/path@jsr:1.0.8");
        assertThat(StructUtils.stringifyLocator(JsrResolver.convertLocatorFromJsrToNpm(
                StructUtils.parseLocator("@std/path@jsr:1.0.8")))).isEqualTo("@jsr/std__path@npm:1.0.8");
    }
}
