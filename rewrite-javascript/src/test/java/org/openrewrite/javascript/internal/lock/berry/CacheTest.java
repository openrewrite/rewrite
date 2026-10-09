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
import org.openrewrite.javascript.internal.lock.CannotKnow;
import org.openrewrite.javascript.internal.lock.HostEnvironment;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.junit.jupiter.api.Assumptions.assumeTrue;

/**
 * The cache key and the {@code compressionLevel: 0} checksum, against what Yarn wrote: every registry entry of a
 * recorded lock whose checksum carries a {@code c0} key and whose tarball the scenario holds.
 */
class CacheTest {

    static Stream<Arguments> checksums() {
        List<Arguments> out = new ArrayList<>();
        Set<String> seen = new LinkedHashSet<>();
        for (BerryScenarios.Scenario scenario : BerryScenarios.discover()) {
            // a crafted start's checksums were given to Yarn, not computed by it
            Set<String> given = new LinkedHashSet<>();
            for (String input : new String[]{"lock-seed", "before-given"}) {
                String text = scenario.file(input);
                if (text != null) {
                    given.addAll(Arrays.asList(text.split("\n")));
                }
            }
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
                        !((String) checksum).matches("\\d+c0/[0-9a-f]+") ||
                        given.contains("  checksum: " + checksum)) {
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

    /**
     * The downloaded fixtures' registry tarballs against the checksum Yarn 4.5.3 wrote for each; skipped when the
     * fixtures are not on the classpath.
     */
    @ParameterizedTest(name = "{0}")
    @MethodSource("downloadedTarballs")
    void reproducesTheChecksumsOfTheDownloadedTarballs(String fixture, String name, String checksum)
            throws IOException {
        byte[] tgz;
        try (InputStream in = CacheTest.class.getClassLoader().getResourceAsStream("lock/yarn-berry/checksum/" +
                                                                                     fixture)) {
            assumeTrue(in != null, "the downloaded fixtures are not on the classpath");
            ByteArrayOutputStream out = new ByteArrayOutputStream();
            byte[] buf = new byte[8192];
            int n;
            while ((n = in.read(buf)) >= 0) {
                out.write(buf, 0, n);
            }
            tgz = out.toByteArray();
        }
        assertThat(Cache.checksum(tgz, StructUtils.parseIdent(name), BerryOptions.defaults())).isEqualTo(checksum);
    }

    static Stream<Arguments> downloadedTarballs() {
        return Stream.of(
                Arguments.of("ms-2.1.3.tgz", "ms", "10c0/d924b57e7312b3b63ad21fc5b3dc0af5e78d61a1fc7cfb5457edaf2632" +
                        "6bf62be5307cc87ffb6862ef1c2b33b0233cdb5d4f01c4c958cc0d660948b65a287a48"),
                Arguments.of("semver-7.6.3.tgz", "semver", "10c0/88f33e148b210c153873cb08cfe1e281d518aaa9a666d4d148a" +
                        "dd6560db5cd3c582f3a08ccb91f38d5f379ead256da9931234ed122057f40bb5766e65e58adaf"),
                Arguments.of("babel-code-frame-7.24.7.tgz", "@babel/code-frame", "10c0/ab0af539473a9f5aeaac7047e377" +
                        "cb4f4edd255a81d84a76058595f8540784cc3fbe8acf73f1e073981104562490aabfb23008cd66dc677a456a4" +
                        "ed5390fdde6"),
                Arguments.of("chalk-5.3.0.tgz", "chalk", "10c0/8297d436b2c0f95801103ff2ef67268d362021b8210daf8ddbe3" +
                        "49695333eb3610a71122172ff3b0272f1ef2cf7cc2c41fdaa4715f52e49ffe04c56340feed09"));
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
                HostEnvironment.of(Collections.emptyMap()));
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
