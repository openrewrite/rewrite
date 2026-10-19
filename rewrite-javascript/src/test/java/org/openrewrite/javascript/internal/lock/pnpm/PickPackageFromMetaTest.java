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

import org.jspecify.annotations.Nullable;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.openrewrite.javascript.internal.lock.pnpm.PnpmTestSupport.json;
import static org.openrewrite.javascript.internal.lock.pnpm.PnpmTestSupport.obj;
import static org.openrewrite.javascript.internal.lock.pnpm.PnpmTestSupport.sortedJson;

/**
 * The version-choice cases of pnpm 11.2.2's resolving/npm-resolver/test/index.ts as resolveNpm runs them, and
 * resolving/registry/pkg-metadata-filter/test/index.ts with its snapshots (keys sorted, as Jest prints them).
 */
class PickPackageFromMetaTest {

    private static final String REGISTRY = "https://registry.npmjs.org/";

    private static String meta(String distTags, String deprecated) {
        StringBuilder versions = new StringBuilder();
        for (String v : new String[]{"1.0.0", "2.0.0", "3.0.0", "3.1.0"}) {
            if (versions.length() > 0) {
                versions.append(',');
            }
            versions.append("'").append(v).append("':{'name':'is-positive','version':'").append(v).append("'")
              .append(deprecated.contains(v) ? ",'deprecated':'This version is deprecated'" : "").append('}');
        }
        return "{'name':'is-positive','versions':{" + versions + "},'dist-tags':" + distTags + "}";
    }

    private static final String IS_POSITIVE = meta("{'stable':'3.0.0','latest':'3.1.0'}", "");
    private static final String WITH_DEPRECATED = meta("{'stable':'2.0.0','latest':'2.0.0'}", "2.0.0 3.1.0");

    private static @Nullable String resolve(String meta, String alias, @Nullable String bareSpecifier,
                                            String defaultTag,
                                            @Nullable Map<String, PreferredVersions.Selector> preferred) {
        PickPackageFromMeta.RegistryPackageSpec spec = PickPackageFromMeta.parseBareSpecifier(
          bareSpecifier == null ? defaultTag : bareSpecifier, alias, defaultTag, REGISTRY);
        Map<String, @Nullable Object> manifest = PickPackageFromMeta.pickPackageFromMeta(
          PickPackageFromMeta::pickVersionByVersionRange, preferred, null, null, obj(meta), spec);
        return manifest == null ? null : manifest.get("name") + "@" + manifest.get("version");
    }

    private static Map<String, PreferredVersions.Selector> prefer(Object... selectors) {
        Map<String, PreferredVersions.Selector> out = new LinkedHashMap<>();
        for (int i = 0; i < selectors.length; i += 2) {
            out.put((String) selectors[i], selectors[i + 1] instanceof String ?
              PreferredVersions.Selector.of((String) selectors[i + 1]) : (PreferredVersions.Selector) selectors[i + 1]);
        }
        return out;
    }

    @Test
    void resolveToLatestOrTheDefaultTagWhenNoBareSpecifierSpecified() {
        assertThat(resolve(IS_POSITIVE, "is-positive", null, "latest", null)).isEqualTo("is-positive@3.1.0");
        assertThat(resolve(IS_POSITIVE, "is-positive", null, "stable", null)).isEqualTo("is-positive@3.0.0");
    }

    @Test
    void deprecatedVersions() {
        assertThat(resolve(WITH_DEPRECATED, "is-positive", "3", "latest", null)).isEqualTo("is-positive@3.0.0");
        assertThat(resolve(WITH_DEPRECATED, "is-positive", "2", "latest", null)).isEqualTo("is-positive@2.0.0");
    }

    @Test
    void aliasedDependencies() {
        assertThat(resolve(IS_POSITIVE, "positive", "npm:is-positive@1.0.0", "latest", null))
          .isEqualTo("is-positive@1.0.0");
        assertThat(resolve(IS_POSITIVE, "positive", "npm:is-positive", "latest", null)).isEqualTo("is-positive@3.1.0");
        assertThat(resolve(IS_POSITIVE, "positive", "npm:is-positive", "stable", null)).isEqualTo("is-positive@3.0.0");
    }

    @Test
    void versionsPrefixedWithVOrLoose() {
        assertThat(resolve(IS_POSITIVE, "is-positive", "v1.0.0", "latest", null)).isEqualTo("is-positive@1.0.0");
        assertThat(resolve(IS_POSITIVE, "is-positive", "= 1.0.0", "latest", null)).isEqualTo("is-positive@1.0.0");
    }

    @Test
    void latestInsideTheWantedOrPreferredRange() {
        String latest300 = meta("{'latest':'3.0.0'}", "");
        assertThat(resolve(latest300, "is-positive", "^3.0.0", "latest", null)).isEqualTo("is-positive@3.0.0");
        assertThat(resolve(latest300, "is-positive", "^3.0.0", "latest", prefer("^3.0.0", "range")))
          .isEqualTo("is-positive@3.0.0");
        assertThat(resolve(meta("{'latest':'2.0.0'}", ""), "is-positive", "^3.0.0", "latest",
          prefer("^2.0.0", "range"))).isEqualTo("is-positive@3.1.0");
    }

    @Test
    void preferredVersionsAndRanges() {
        String latest310 = meta("{'latest':'3.1.0'}", "");
        String latest100 = meta("{'latest':'1.0.0'}", "");
        assertThat(resolve(latest310, "is-positive", "^3.0.0", "latest", prefer("3.0.0", "version")))
          .isEqualTo("is-positive@3.0.0");
        assertThat(resolve(latest310, "is-positive", "^3.0.0", "latest", prefer("2.0.0", "version")))
          .isEqualTo("is-positive@3.1.0");
        assertThat(resolve(latest100, "is-positive", ">=1.0.0", "latest", prefer("^3.0.0", "range")))
          .isEqualTo("is-positive@3.1.0");
        assertThat(resolve(latest100, "is-positive", ">=1.0.0", "latest",
          prefer("3.0.0", "version", "3.1.0", "version"))).isEqualTo("is-positive@3.1.0");
        assertThat(resolve(latest310, "is-positive", "^3.0.0", "latest", prefer("^2.0.0", "range")))
          .isEqualTo("is-positive@3.1.0");
    }

    @Test
    void preferredDistTags() {
        assertThat(resolve(meta("{'latest':'3.1.0','stable':'3.0.0'}", ""), "is-positive", "^3.0.0", "latest",
          prefer("stable", "tag"))).isEqualTo("is-positive@3.0.0");
        assertThat(resolve(meta("{'latest':'3.1.0','stable':'2.0.0'}", ""), "is-positive", "^3.0.0", "latest",
          prefer("stable", "tag"))).isEqualTo("is-positive@3.1.0");
    }

    @Test
    void weights() {
        assertThat(resolve(meta("{'latest':'3.0.0'}", ""), "is-positive", "1.0.0 || 2.0.0", "latest",
          prefer("1.0.0 || 3.0.0", "range"))).isEqualTo("is-positive@1.0.0");
        assertThat(resolve(IS_POSITIVE, "is-positive", "^3.0.0", "latest",
          prefer("^3.0.0", "range", "3.0.0", "version"))).isEqualTo("is-positive@3.0.0");
        assertThat(resolve(IS_POSITIVE, "is-positive", "^3.0.0", "latest", prefer("^3.0.0", "range", "3.0.0",
          PreferredVersions.Selector.of("version", 100), "3.1.0", "version"))).isEqualTo("is-positive@3.0.0");
        assertThat(resolve(IS_POSITIVE, "is-positive", "^3.0.0", "latest", prefer("3.0.0",
          PreferredVersions.Selector.of("version", -1)))).isEqualTo("is-positive@3.1.0");
    }

    @Test
    void pickLowestVersionByStarWhenThereAreOnlyPrereleaseVersions() {
        Map<String, @Nullable Object> manifest = PickPackageFromMeta.pickPackageFromMeta(
          PickPackageFromMeta::pickLowestVersionByVersionRange, null, null, null,
          obj("{'versions':{'1.0.0-alpha.1':{'name':'is-positive','version':'1.0.0-alpha.1'}}," +
              "'dist-tags':{'latest':'1.0.0-alpha.1'}}"),
          PickPackageFromMeta.parseBareSpecifier("*", "is-positive", "latest", REGISTRY));
        assertThat(manifest.get("version")).isEqualTo("1.0.0-alpha.1");
    }

    /**
     * The release-age choice recorded with pnpm 11.2.2 for a range whose versions straddle the cutoff: the newest
     * version published before it, not {@code latest}.
     */
    @Test
    void theNewestMatureVersionUnderMinimumReleaseAge() {
        String meta = "{'name':'c','versions':{'2.0.2':{'name':'c','version':'2.0.2'},'2.3.0':{'name':'c'," +
                      "'version':'2.3.0'},'2.3.1':{'name':'c','version':'2.3.1'}},'dist-tags':{'latest':'2.3.1'}," +
                      "'time':{'2.0.2':'2026-01-01T00:00:00.000Z','2.3.0':'2026-10-07T00:30:00.000Z'," +
                      "'2.3.1':'2026-10-07T01:00:00.000Z'}}";
        PickPackage.PickerOptions options = PickPackage.PickerOptions.builder()
          .publishedBy(Instant.parse("2026-10-06T01:30:00Z")).build();
        PickPackageFromMeta.RegistryPackageSpec spec = PickPackageFromMeta.parseBareSpecifier("^2.0.0", "c", "latest",
          REGISTRY);
        assertThat(PickPackage.pickMatchingVersionFinal(options, spec, obj(meta)).get("version")).isEqualTo("2.0.2");
        spec = PickPackageFromMeta.parseBareSpecifier("^2.3.0", "c", "latest", REGISTRY);
        assertThat(PickPackage.pickMatchingVersionFinal(options, spec, obj(meta)).get("version")).isEqualTo("2.3.0");
    }

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
