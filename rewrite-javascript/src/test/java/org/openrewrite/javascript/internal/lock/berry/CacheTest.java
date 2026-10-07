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
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * The cache key and the {@code compressionLevel: 0} checksum, against what Yarn wrote: every registry entry of a
 * recorded lock whose checksum carries a {@code c0} key and whose tarball the scenario holds.
 */
class CacheTest {

    static Stream<Arguments> checksums() {
        List<Arguments> out = new ArrayList<>();
        Set<String> seen = new LinkedHashSet<>();
        for (BerryScenarios.Scenario scenario : BerryScenarios.discover()) {
            for (BerryScenarios.Lock lock : scenario.locks()) {
                Map<String, Object> parsed;
                try {
                    parsed = Syml.parseSyml(lock.getText());
                } catch (RuntimeException e) {
                    continue;
                }
                for (Object entry : parsed.values()) {
                    Object resolution = Js.get(entry, "resolution");
                    Object checksum = Js.get(entry, "checksum");
                    if (!(resolution instanceof String) || !(checksum instanceof String) ||
                        !((String) checksum).matches("\\d+c0/[0-9a-f]+")) {
                        continue;
                    }
                    Locator locator = StructUtils.parseLocator((String) resolution, true);
                    if (!locator.reference.matches("npm:[^:#]+")) {
                        continue;
                    }
                    String version = locator.reference.substring("npm:".length());
                    byte[] tgz = scenario.http(StructUtils.stringifyIdent(locator) + "-" + version + ".tgz");
                    if (tgz != null && seen.add(resolution + " " + checksum)) {
                        out.add(Arguments.of(scenario.getName() + " " + resolution, locator, tgz, checksum));
                    }
                }
            }
        }
        return out.stream();
    }

    @ParameterizedTest(name = "{0}")
    @MethodSource("checksums")
    void reproducesTheChecksumYarnWrote(String name, Locator locator, byte[] tgz, String checksum) {
        assertThat(Cache.checksum(tgz, locator, BerryOptions.defaults())).isEqualTo(checksum);
    }

    @Test
    void writesTheArchiveYarnCaches() throws IOException {
        BerryScenarios.Scenario scenario = scenario("open-tar-entry-types");
        byte[] tgz = scenario.http("lwb-tar-1.0.0.tgz");
        Path cached;
        try (Stream<Path> files = Files.list(scenario.getDir().resolve("cache-after"))) {
            cached = files.filter(f -> f.toString().endsWith(".zip")).findFirst()
                    .orElseThrow(IllegalStateException::new);
        }
        byte[] zip = TgzUtils.convertToZip(tgz, "node_modules/lwb-tar", 1).toByteArray();
        assertThat(zip).isEqualTo(Files.readAllBytes(cached));
    }

    @Test
    void cacheKeyFollowsTheCompressionLevel() {
        assertThat(Cache.getCacheKey(BerryOptions.defaults())).isEqualTo("10c0");
        assertThat(Cache.getCacheKey(options("compressionLevel: mixed\n"))).isEqualTo("10");
        assertThat(Cache.getCacheKey(options("compressionLevel: 9\n"))).isEqualTo("10c9");
    }

    @Test
    void anyOtherLevelIsSomethingTheEngineCannotKnow() {
        assertThatThrownBy(() -> Cache.checksum(new byte[0], StructUtils.makeIdent(null, "ms"),
                options("compressionLevel: mixed\n")))
                .isInstanceOf(CannotKnow.class)
                .hasMessageContaining("compressionLevel mixed");
    }

    private static BerryOptions options(String yarnrc) {
        return BerryOptions.load(Collections.singletonMap(".yarnrc.yml", yarnrc),
                Collections.emptyMap());
    }

    private static BerryScenarios.Scenario scenario(String name) {
        for (BerryScenarios.Scenario scenario : BerryScenarios.discover()) {
            if (scenario.getName().equals(name)) {
                return scenario;
            }
        }
        throw new IllegalStateException(name);
    }
}
