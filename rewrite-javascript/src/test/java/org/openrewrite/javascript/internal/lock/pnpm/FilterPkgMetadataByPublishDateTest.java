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
import static org.openrewrite.javascript.internal.lock.pnpm.PnpmTestSupport.json;
import static org.openrewrite.javascript.internal.lock.pnpm.PnpmTestSupport.obj;
import static org.openrewrite.javascript.internal.lock.pnpm.PnpmTestSupport.sortedJson;

/**
 * resolving/registry/pkg-metadata-filter/test/index.ts of pnpm 11.2.2 and its snapshots (keys sorted, as Jest
 * prints them).
 */
class FilterPkgMetadataByPublishDateTest {

    private static String version(String version, String extra) {
        return "'" + version + "':{'name':'dist-tag-date','version':'" + version + "','dist':{'tarball':" +
               "'https://registry.npmjs.org/dist-tag-date/-/dist-tag-date-" + version + ".tgz','shasum':''}" + extra +
               "}";
    }

    private static String filter(String doc) {
        return sortedJson(json(PickPackageFromMeta.filterPkgMetadataByPublishDate(obj(doc),
          Instant.parse("2020-04-01T00:00:00.000Z"), null)));
    }

    @Test
    void filterPkgMetadataByPublishDate() {
        assertThat(filter("{'name':'dist-tag-date','versions':{" + version("3.0.0", "") + "," +
                          version("3.1.0", ",'deprecated':'This version is deprecated'") + "," +
                          version("3.2.0", "") + "," + version("2.9.9", "") + "}," +
                          "'dist-tags':{'latest':'3.2.0'},'time':{'2.9.9':'2020-01-01T00:00:00.000Z'," +
                          "'3.0.0':'2020-02-01T00:00:00.000Z','3.1.0':'2020-03-01T00:00:00.000Z'," +
                          "'3.2.0':'2020-05-01T00:00:00.000Z'}}"))
          .isEqualTo(sortedJson("{'dist-tags':{'latest':'3.0.0'},'name':'dist-tag-date','time':{" +
                                "'2.9.9':'2020-01-01T00:00:00.000Z','3.0.0':'2020-02-01T00:00:00.000Z'," +
                                "'3.1.0':'2020-03-01T00:00:00.000Z','3.2.0':'2020-05-01T00:00:00.000Z'}," +
                                "'versions':{" + version("2.9.9", "") + "," + version("3.0.0", "") + "," +
                                version("3.1.0", ",'deprecated':'This version is deprecated'") + "}}"));

        assertThat(filter("{'name':'dist-tag-date','versions':{" + version("3.0.0", "") + "," +
                          version("2.9.9", "") + "},'dist-tags':{'latest':'3.0.0','stable':'3.0.0'}," +
                          "'time':{'2.9.9':'2020-03-01T00:00:00.000Z','3.0.0':'2020-05-01T00:00:00.000Z'}}"))
          .isEqualTo(sortedJson("{'dist-tags':{'latest':'2.9.9'},'name':'dist-tag-date','time':{" +
                                "'2.9.9':'2020-03-01T00:00:00.000Z','3.0.0':'2020-05-01T00:00:00.000Z'}," +
                                "'versions':{" + version("2.9.9", "") + "}}"));
    }
}
