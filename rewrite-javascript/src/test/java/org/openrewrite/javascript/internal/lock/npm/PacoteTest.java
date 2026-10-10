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
