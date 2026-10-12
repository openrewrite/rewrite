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

import java.util.Collections;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Yarn's packages/plugin-npm/tests/NpmSemverResolver.test.ts.
 */
class NpmSemverResolverTest {

    @Test
    void getSatisfyingMatchesWhenTheReferenceContainsAnArchiveUrl() {
        Ident ident = StructUtils.makeIdent(null, "foo");
        Descriptor descriptor = StructUtils.makeDescriptor(ident, "npm:*");
        Locator locator = StructUtils.makeLocator(ident, "npm:1.0.0::__archiveUrl=foo.tgz");
        Resolver.Satisfying results = new NpmSemverResolver().getSatisfying(descriptor, Collections.emptyMap(),
                Collections.singletonList(locator), null);
        assertThat(results.getLocators()).hasSize(1);
        assertThat(results.getLocators().get(0).locatorHash).isEqualTo(locator.locatorHash);
    }
}
