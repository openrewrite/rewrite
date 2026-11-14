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
package org.openrewrite.javascript.internal.lock.npm;

import org.jspecify.annotations.Nullable;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.openrewrite.javascript.internal.lock.CannotKnow;
import org.openrewrite.javascript.internal.lock.Spec;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.openrewrite.javascript.internal.lock.npm.NpmTreeSupport.json;

/**
 * pacote 11.17.0's registry fetcher run on these packuments and options.
 */
class PacoteTest {

    private static final String A = "sha512-AQEBAQEBAQEBAQEBAQEBAQEBAQEBAQEBAQEBAQEBAQEBAQEBAQEBAQEBAQEBAQEBAQEBAQEBA" +
                                    "QEBAQEBAQEBAQ==";
    private static final String B = "sha512-AgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICA" +
                                    "gICAgICAgICAg==";
    private static final String S = "sha1-AwMDAwMDAwMDAwMDAwMDAwMDAwM=";

    private static PackumentSource registry(String packument) {
        Map<String, Object> doc = json(packument);
        return new PackumentSource() {
            @Override
            public Map<String, Object> packument(String registry, String name, boolean fullMetadata) {
                return doc;
            }

            @Override
            @SuppressWarnings("unchecked")
            public Map<String, Object> manifest(String registry, String name, String version) {
                return (Map<String, Object>) Js.get(doc.get("versions"), version);
            }
        };
    }

    private static Map<String, Object> manifest(String packument, String spec, Pacote.Options options) {
        return pacote(packument, VirtualTreeLoader.ProjectFiles.builder().build())
          .manifest(Spec.npa(spec, "/"), options);
    }

    private static Pacote pacote(String packument, VirtualTreeLoader.ProjectFiles files) {
        return new Pacote(registry(packument), NpmOptions.builder().build(), files, "/");
    }

    private static String versioned(String dist) {
        return "{\"name\":\"x\",\"dist-tags\":{\"latest\":\"1.0.0\"},\"versions\":{\"1.0.0\":{\"name\":\"x\"," +
               "\"version\":\"1.0.0\",\"dist\":{\"tarball\":\"https://r/x-1.0.0.tgz\"" + dist + "}}," +
               "\"2.0.0\":{\"name\":\"x\",\"version\":\"2.0.0\",\"dist\":{\"tarball\":\"https://r/x-2.0.0.tgz\"" +
                 dist + "}}}}";
    }

    private static final String LATEST_BELOW_HIGHEST = versioned("");

    // the fetcher's constructor: this.defaultTag = opts.defaultTag || 'latest'
    @Test
    void defaultTagFallsBackToLatest() {
        assertThat(manifest(LATEST_BELOW_HIGHEST, "x@*", Pacote.Options.builder().build()).get("version"))
          .isEqualTo("1.0.0");
        assertThat(manifest(LATEST_BELOW_HIGHEST, "x@*", Pacote.Options.builder().defaultTag("").build())
          .get("version"))
          .isEqualTo("1.0.0");
        assertThat(manifest(LATEST_BELOW_HIGHEST, "x@*", Pacote.Options.builder().defaultTag("null").build())
          .get("version"))
          .isEqualTo("2.0.0");
        assertThat(manifest(LATEST_BELOW_HIGHEST, "x@*", Pacote.Options.builder().defaultTag("beta").build())
          .get("version"))
          .isEqualTo("2.0.0");
    }

    // pacote 13 (npm 8.19.4) fetches a bare name, which npm-package-arg 9 reads as a tag with no spec, as the
    // configured tag (registry.js: "handle case when npm-package-arg guesses wrong"), as npm 9 picks for its '*'
    @Test
    void emptySpecTakesTheConfiguredTagBefore9_0_0() {
        String packument = "{\"name\":\"x\",\"dist-tags\":{\"latest\":\"1.0.0\",\"beta\":\"2.0.0\"},\"versions\":{" +
          "\"1.0.0\":{\"name\":\"x\",\"version\":\"1.0.0\",\"dist\":{\"tarball\":\"https://r/x-1.0.0.tgz\"}}," +
          "\"2.0.0\":{\"name\":\"x\",\"version\":\"2.0.0\",\"dist\":{\"tarball\":\"https://r/x-2.0.0.tgz\"}}}}";
        for (String version : new String[]{"8.19.4", "9.0.0"}) {
            NpmRelease release = NpmRelease.of(version);
            Pacote pacote = new Pacote(registry(packument), NpmOptions.builder().release(release).build(),
              VirtualTreeLoader.ProjectFiles.builder().build(), "/");
            Spec bare = Spec.npa("x@", "/", release.npmPackageArg());
            assertThat(pacote.manifest(bare, Pacote.Options.builder().defaultTag("beta").build()).get("version"))
              .as(version).isEqualTo("2.0.0");
            assertThat(pacote.manifest(bare, Pacote.Options.builder().build()).get("version"))
              .as(version).isEqualTo("1.0.0");
        }
        assertThat(Spec.npa("x@", "/", NpmRelease.of("8.19.4").npmPackageArg()).getType()).isEqualTo(Spec.Type.TAG);
    }

    private static @Nullable Object integrity(String dist, @Nullable Object held) {
        return manifest(versioned(dist), "x@1.0.0", Pacote.Options.builder().integrity(held).build()).get("_integrity");
    }

    // the integrity setter merges dist's hashes into the caller's; EINTEGRITY only for an algorithm both carry
    @Test
    void integrityMergesTheCallersWithDists() {
        String withA = ",\"integrity\":\"" + A + "\"";
        assertThat(integrity(withA, null)).isEqualTo(A);
        assertThat(integrity(withA, S)).isEqualTo(S + " " + A);
        assertThat(integrity(withA, A)).isEqualTo(A);
        assertThat(integrity(withA, "abc")).isEqualTo(A);
        assertThatThrownBy(() -> integrity(withA, S + " " + B))
          .isInstanceOf(NpmRejection.class)
          .hasMessage("Integrity checksum failed when using sha512: wanted " + S + " " + B + " but got " + A + ".");

        String shasumOnly = ",\"shasum\":\"0303030303030303030303030303030303030303\"";
        assertThat(integrity(shasumOnly, null)).isEqualTo(S);
        assertThat(integrity(shasumOnly, A)).isEqualTo(A + " " + S);
        assertThat(integrity(",\"integrity\":\"abc\"" + shasumOnly, A)).isEqualTo(A);
        assertThat(integrity(",\"integrity\":\"abc\"" + shasumOnly, null)).isNull();

        assertThat(integrity("", S)).isEqualTo(S);
        assertThat(integrity("", "abc")).isNull();
        assertThat(integrity("", 5)).isEqualTo("5");
        assertThatThrownBy(() -> integrity(withA, 5)).hasMessage("this.integrity.match is not a function");
    }

    // dist.signatures is read under `if (this.integrity)` whether or not the manifest has a dist
    @Test
    void heldIntegrityWithoutDistReadsSignaturesOfUndefined() {
        String noDist = "{\"name\":\"x\",\"dist-tags\":{\"latest\":\"1.0.0\"}," +
                        "\"versions\":{\"1.0.0\":{\"name\":\"x\",\"version\":\"1.0.0\"}}}";
        assertThat(manifest(noDist, "x@1.0.0", Pacote.Options.builder().build())).doesNotContainKey("_integrity");
        assertThatThrownBy(() -> manifest(noDist, "x@1.0.0", Pacote.Options.builder().integrity(S).build()))
          .hasMessage("Cannot read properties of undefined (reading 'signatures')");
        assertThatThrownBy(() -> manifest(noDist, "x@1.0.0", Pacote.Options.builder().integrity(5).build()))
          .hasMessage("Cannot read properties of undefined (reading 'signatures')");
    }

    // pacote's own answer for a directory whose manifest has only directories.bin: npm 10.5.2 and 11.6.1 return the
    // bin of its files, npm 10.6.0 to 11.6.0 return none
    @ParameterizedTest
    @CsvSource({"10.5.2,true", "10.6.0,false", "10.9.9,false", "11.6.0,false", "11.6.1,true", "11.17.0,true"})
    void directoryManifestReadsBinDirectory(String release, boolean readsFiles) {
        VirtualTreeLoader.ProjectFiles files = VirtualTreeLoader.ProjectFiles.builder()
          .manifest("vendor/x", "{\"name\":\"x\",\"version\":\"1.0.0\",\"directories\":{\"bin\":\"bin\"}}")
          .build();
        Pacote pacote = new Pacote(registry(LATEST_BELOW_HIGHEST),
          NpmOptions.builder().release(NpmRelease.of(release)).build(), files, "/");
        if (readsFiles) {
            assertThatThrownBy(() -> pacote.manifest(Spec.npa("file:vendor/x", "/"), Pacote.Options.builder().build()))
              .isInstanceOf(PackageJson.NeedsPackageFiles.class);
        } else {
            assertThat(pacote.manifest(Spec.npa("file:vendor/x", "/"), Pacote.Options.builder().build()))
              .containsEntry("version", "1.0.0").containsKey("directories").doesNotContainKey("bin");
        }
    }

    private static @Nullable Object bin(String release, String bin) {
        String packument = "{\"name\":\"x\",\"dist-tags\":{\"latest\":\"1.0.0\"},\"versions\":{\"1.0.0\":" +
                           "{\"name\":\"x\",\"version\":\"1.0.0\",\"bin\":" + bin + "}}}";
        return new Pacote(registry(packument), NpmOptions.builder().release(NpmRelease.of(release)).build(),
          VirtualTreeLoader.ProjectFiles.builder().build(), "/")
          .manifest(Spec.npa("x@1.0.0", "/"), Pacote.Options.builder().build()).get("bin");
    }

    /**
     * The {@code bin} of the lock entry npm 10.8.0, 10.8.1 and 10.9.0 each write for a manifest, asked of their own
     * npm-pick-manifest, pacote and arborist: @npmcli/package-json 5.1.1 and 5.2.0 (npm 10.8.1 to 10.8.3) drop a name
     * or a path that starts with a dot, and before them a colon in a path is kept.
     */
    @ParameterizedTest
    @CsvSource(delimiter = '|', quoteCharacter = '`', value = {
      "{\"x\":\".bin/x.js\"}|{\"x\":\".bin/x.js\"}||{\"x\":\".bin/x.js\"}",
      "{\"x\":\"./.output/x.mjs\"}|{\"x\":\".output/x.mjs\"}||{\"x\":\".output/x.mjs\"}",
      "{\"x\":\".x.js\",\"y\":\"y.js\"}|{\"x\":\".x.js\",\"y\":\"y.js\"}|{\"y\":\"y.js\"}|" +
      "{\"x\":\".x.js\",\"y\":\"y.js\"}",
      "{\".x\":\"x.js\",\"y\":\"y.js\"}|{\".x\":\"x.js\",\"y\":\"y.js\"}|{\"y\":\"y.js\"}|" +
      "{\".x\":\"x.js\",\"y\":\"y.js\"}",
      "{\"x\":\"...\",\"y\":\"..a\",\"z\":\".../z\"}|{\"x\":\"...\",\"y\":\"..a\",\"z\":\".../z\"}||" +
      "{\"x\":\"...\",\"y\":\"..a\",\"z\":\".../z\"}",
      "{\"x\":\"a:b/c.js\"}|{\"x\":\"a:b/c.js\"}|{\"x\":\"a/b/c.js\"}|{\"x\":\"a/b/c.js\"}",
      "{\"x\":\"C:\\\\tools\\\\x.js\"}|{\"x\":\"C:/tools/x.js\"}|{\"x\":\"C/tools/x.js\"}|{\"x\":\"C/tools/x.js\"}",
      "{\"a:x\":\"x.js\"}|{\"x\":\"x.js\"}|{\"x\":\"x.js\"}|{\"x\":\"x.js\"}",
      "{\"../../etc/x\":\"x.js\",\"a/b\":\"b.js\",\"z\":\"z.js\"}|{\"x\":\"x.js\",\"b\":\"b.js\",\"z\":\"z.js\"}|" +
      "{\"x\":\"x.js\",\"b\":\"b.js\",\"z\":\"z.js\"}|{\"x\":\"x.js\",\"b\":\"b.js\",\"z\":\"z.js\"}",
      "{\"x\":\"../../x.js\",\"y\":\"/abs/y.js\",\"z\":\"./z.js\"}|" +
      "{\"x\":\"x.js\",\"y\":\"abs/y.js\",\"z\":\"z.js\"}|" +
      "{\"x\":\"x.js\",\"y\":\"abs/y.js\",\"z\":\"z.js\"}|{\"x\":\"x.js\",\"y\":\"abs/y.js\",\"z\":\"z.js\"}",
      "{\"x\":\"bin\\\\x.js\"}|{\"x\":\"bin/x.js\"}|{\"x\":\"bin/x.js\"}|{\"x\":\"bin/x.js\"}",
      "{\"x\":\"\",\"y\":\".\",\"z\":\"..\",\"w\":\"./\"}|||",
      "{\"x\":1,\"y\":null,\"z\":\"z.js\"}|{\"z\":\"z.js\"}|{\"z\":\"z.js\"}|{\"z\":\"z.js\"}",
      "{\"x\":\"dir/\",\"y/\":\"y.js\"}|{\"x\":\"dir/\",\"y\":\"y.js\"}|{\"x\":\"dir/\",\"y\":\"y.js\"}|" +
      "{\"x\":\"dir/\",\"y\":\"y.js\"}",
      "\"cli.js\"|{\"x\":\"cli.js\"}|{\"x\":\"cli.js\"}|{\"x\":\"cli.js\"}",
      "[\"bin/a.js\",\"./b\"]|{\"a.js\":\"bin/a.js\",\"b\":\"b\"}|{\"a.js\":\"bin/a.js\",\"b\":\"b\"}|" +
      "{\"a.js\":\"bin/a.js\",\"b\":\"b\"}",
      "5|||",
      "true|||"
    })
    void binAsEachReleasesPackageJsonLeavesIt(String bin, @Nullable String at10_8_0, @Nullable String at10_8_1,
                                              @Nullable String at10_9_0) {
        assertThat(bin("10.6.0", bin)).isEqualTo(at10_8_0 == null ? null : json(at10_8_0));
        assertThat(bin("10.8.0", bin)).isEqualTo(at10_8_0 == null ? null : json(at10_8_0));
        assertThat(bin("10.8.1", bin)).isEqualTo(at10_8_1 == null ? null : json(at10_8_1));
        assertThat(bin("10.8.3", bin)).isEqualTo(at10_8_1 == null ? null : json(at10_8_1));
        assertThat(bin("10.9.0", bin)).isEqualTo(at10_9_0 == null ? null : json(at10_9_0));
        assertThat(bin("11.17.0", bin)).isEqualTo(at10_9_0 == null ? null : json(at10_9_0));
    }

    // DirFetcher.manifest(): the directory's package.json among the project's files, else nothing to read
    @Test
    void directorySpecReadsTheProjectsFiles() {
        VirtualTreeLoader.ProjectFiles files = VirtualTreeLoader.ProjectFiles.builder()
          .manifest("vendor/x", "{\"name\":\"x\",\"version\":\"1.0.0\",\"dependencies\":{\"y\":\"1\"}}")
          .build();
        Map<String, Object> mani = pacote(LATEST_BELOW_HIGHEST, files)
          .manifest(Spec.npa("file:vendor/x", "/"), Pacote.Options.builder().integrity(S).build());
        assertThat(mani).containsEntry("_resolved", "/vendor/x").containsEntry("_from", "file:vendor/x")
          .containsEntry("_integrity", S).containsKey("dependencies");
        assertThatThrownBy(() -> pacote(LATEST_BELOW_HIGHEST, files)
          .manifest(Spec.npa("file:vendor/z", "/"), Pacote.Options.builder().build()))
          .isInstanceOf(CannotKnow.class)
          .hasMessageContaining("directory manifest file:vendor/z (vendor/z)");
    }
}
