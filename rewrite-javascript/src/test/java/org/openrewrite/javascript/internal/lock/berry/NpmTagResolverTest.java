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

import org.jspecify.annotations.Nullable;
import org.junit.jupiter.api.Test;
import org.openrewrite.javascript.internal.lock.LockScenario.Phase;
import org.openrewrite.javascript.internal.lock.RegistrySource;
import org.openrewrite.javascript.internal.LockFileRegeneration.Failure;
import org.openrewrite.javascript.internal.LockFileRegeneration.Kind;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * plugin-npm NpmTagResolver.ts over pickPackageMetadata, which keeps the {@code dist-tags} key of a document that
 * lacks it, so the tag lookup fails as on the null that open-tag-dist-tags-null records.
 */
class NpmTagResolverTest {

    @Test
    void aDocumentWithoutDistTagsFailsAsOneWhoseDistTagsIsNull() {
        BerryScenarios.Scenario scenario = null;
        for (BerryScenarios.Scenario s : BerryScenarios.discover()) {
            if ("open-tag-dist-tags-null".equals(s.getLabel())) {
                scenario = s;
            }
        }
        assertThat(scenario).isNotNull();
        String before = scenario.file("before");
        assertThat(before).isNotNull();
        BerryScenarios.ScenarioRegistry recorded = scenario.registry();
        RegistrySource withoutDistTags = new RegistrySource() {
            @Override
            public @Nullable String packument(String registry, String name, boolean full,
                                              @Nullable String authorization) {
                String text = recorded.packument(registry, name, full, authorization);
                if (text == null) {
                    return null;
                }
                Map<?, ?> document = (Map<?, ?>) Js.jsonParse(text);
                assertThat(document.remove("dist-tags")).isNull();
                assertThat(document.containsKey("dist-tags")).isFalse();
                return Js.jsonStringify(document);
            }

            @Override
            public byte @Nullable [] tarball(String url, String name, String version, @Nullable String authorization) {
                return recorded.tarball(url, name, version, authorization);
            }
        };
        Failure failure = new YarnBerryLockRegenerator(scenario.inputs(Phase.AFTER, before),
                withoutDistTags).regenerate().getFailure();
        assertThat(failure).isNotNull();
        assertThat(failure.getKind()).isEqualTo(Kind.PM_FAILS);
        assertThat(failure.getDetail()).isEqualTo("yarn fails on this edit: YN0001 TypeError: lwt-dist@npm:latest: " +
                                                  "Cannot convert undefined or null to object");
    }
}
