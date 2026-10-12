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
package org.openrewrite.javascript.internal.lock.pnpm;

import org.junit.jupiter.api.Test;

import java.time.Instant;

import static org.assertj.core.api.Assertions.assertThat;
import static org.openrewrite.javascript.internal.lock.pnpm.PnpmTestSupport.obj;

/**
 * resolving/npm-resolver/test/distTagsByDate.test.ts of pnpm 11.2.2: resolving {@code latest} under a
 * {@code publishedBy} cutoff, the packument picked from as the resolver does once it has the document.
 */
class PickPackageTest {

    private static final String REGISTRY = "https://registry.npmjs.org/";

    private static String versions(String name, String... versions) {
        StringBuilder out = new StringBuilder();
        for (String version : versions) {
            String deprecated = version.endsWith("!") ? ",'deprecated':'This version is deprecated'" : "";
            version = version.replace("!", "");
            if (out.length() > 0) {
                out.append(',');
            }
            out.append("'").append(version).append("':{'name':'").append(name).append("','version':'")
              .append(version).append("','dist':{'tarball':'").append(REGISTRY).append(name).append("/-/")
              .append(name).append('-').append(version).append(".tgz'}").append(deprecated).append('}');
        }
        return "{" + out + "}";
    }

    private static String resolveLatest(String name, String meta, String cutoff) {
        PickPackage.PickerOptions options = PickPackage.PickerOptions.builder()
          .publishedBy(Instant.parse(cutoff)).build();
        PickPackageFromMeta.RegistryPackageSpec spec = PickPackageFromMeta.parseBareSpecifier("latest", name,
          "latest", REGISTRY);
        return name + "@" + PickPackage.pickMatchingVersionFinal(options, spec, obj(meta)).get("version");
    }

    @Test
    void repopulateDistTagToHighestSameMajorVersionWithinTheDateCutoff() {
        String meta = "{'name':'dist-tag-date','versions':" +
                      versions("dist-tag-date", "3.0.0", "3.1.0", "3.2.0", "2.9.9") +
                      ",'dist-tags':{'latest':'3.2.0'},'time':{'2.9.9':'2020-01-01T00:00:00.000Z'," +
                      "'3.0.0':'2020-02-01T00:00:00.000Z','3.1.0':'2020-03-01T00:00:00.000Z'," +
                      "'3.2.0':'2020-05-01T00:00:00.000Z'}}";
        assertThat(resolveLatest("dist-tag-date", meta, "2020-04-01T00:00:00.000Z")).isEqualTo("dist-tag-date@3.1.0");
    }

    @Test
    void repopulateDistTagToHighestSameMajorVersionWithinTheDateCutoffPreferNonDeprecatedVersion() {
        String meta = "{'name':'dist-tag-date','versions':" +
                      versions("dist-tag-date", "3.0.0", "3.1.0!", "3.2.0", "2.9.9") +
                      ",'dist-tags':{'latest':'3.2.0'},'time':{'2.9.9':'2020-01-01T00:00:00.000Z'," +
                      "'3.0.0':'2020-02-01T00:00:00.000Z','3.1.0':'2020-03-01T00:00:00.000Z'," +
                      "'3.2.0':'2020-05-01T00:00:00.000Z'}}";
        assertThat(resolveLatest("dist-tag-date", meta, "2020-04-01T00:00:00.000Z")).isEqualTo("dist-tag-date@3.0.0");
    }

    @Test
    void repopulateDistTagToHighestNonPrereleaseSameMajorVersionWithinTheDateCutoff() {
        String meta = "{'name':'dist-tag-date','versions':" +
                      versions("dist-tag-date", "3.0.0", "3.1.0-alpha.0", "3.2.0", "2.9.9") +
                      ",'dist-tags':{'latest':'3.2.0'},'time':{'2.9.9':'2020-01-01T00:00:00.000Z'," +
                      "'3.0.0':'2020-02-01T00:00:00.000Z','3.1.0-alpha.0':'2020-03-01T00:00:00.000Z'," +
                      "'3.2.0':'2020-05-01T00:00:00.000Z'}}";
        assertThat(resolveLatest("dist-tag-date", meta, "2020-04-01T00:00:00.000Z")).isEqualTo("dist-tag-date@3.0.0");
    }

    @Test
    void repopulateDistTagToHighestPrereleaseSameMajorVersionWithinTheDateCutoff() {
        String meta = "{'name':'dist-tag-date','versions':" +
                      versions("dist-tag-date", "3.0.0-alpha.0", "3.0.0-alpha.1", "3.0.0-alpha.2", "3.2.0",
                        "2.9.9") +
                      ",'dist-tags':{'latest':'3.0.0-alpha.2'},'time':{'2.9.9':'2020-01-01T00:00:00.000Z'," +
                      "'3.0.0-alpha.0':'2020-02-01T00:00:00.000Z','3.0.0-alpha.1':'2020-03-01T00:00:00.000Z'," +
                      "'3.0.0-alpha.2':'2020-05-01T00:00:00.000Z','3.2.0':'2020-05-01T00:00:00.000Z'}}";
        assertThat(resolveLatest("dist-tag-date", meta, "2020-04-01T00:00:00.000Z"))
          .isEqualTo("dist-tag-date@3.0.0-alpha.1");
    }

    @Test
    void keepDistTagIfOriginalVersionIsWithinTheDateCutoff() {
        String meta = "{'name':'dist-tag-date-keep','versions':" + versions("dist-tag-date-keep", "1.0.0") +
                      ",'dist-tags':{'latest':'1.0.0'},'time':{'1.0.0':'2020-01-01T00:00:00.000Z'}}";
        assertThat(resolveLatest("dist-tag-date-keep", meta, "2020-02-01T00:00:00.000Z"))
          .isEqualTo("dist-tag-date-keep@1.0.0");
    }
}
