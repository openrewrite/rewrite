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
package org.openrewrite.javascript.internal.lock.yarnclassic;

import org.junit.jupiter.api.Test;

import static java.util.Arrays.asList;
import static org.assertj.core.api.Assertions.assertThat;

/**
 * yarn 1.22.22's __tests__/package-resolver.js: the case that needs no network. Its {@code resolve <pattern>}
 * cases resolve live git repositories, tarballs and registry packages into a cache directory.
 */
class PackageResolverTest {

    @Test
    void addPatternDoesNotAddDuplicates() {
        PackageResolver resolver = YarnClassicScenarios.resolver(YarnClassicOptions.defaults(), new Lockfile());
        Manifest manifest = new Manifest();
        manifest.put("name", "name");
        resolver.addPattern("patternOne", manifest);
        resolver.addPattern("patternTwo", manifest);
        resolver.addPattern("patternOne", manifest);
        assertThat(resolver.patternsByPackage.get("name")).isEqualTo(asList("patternOne", "patternTwo"));
    }
}
