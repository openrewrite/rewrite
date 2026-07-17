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
package org.openrewrite.python.internal.uvlock;

import org.junit.jupiter.api.Test;

import java.util.Arrays;
import java.util.Collections;

import static java.util.Collections.singletonList;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class UvLockWriterTest {

    private UvLockPackage minimalPackage() {
        return UvLockPackage.builder()
          .name("six")
          .version("1.17.0")
          .source(UvLockSource.registry("https://pypi.org/simple"))
          .build();
    }

    private String write(UvLockPackage pkg) {
        return UvLockWriter.write(new UvLock(1, 3, ">=3.12", null, null, null, null, null, null, singletonList(pkg)));
    }

    @Test
    void uploadTimeEmittedVerbatim() {
        // uv trims trailing fractional zeros; the writer must never reformat the stored string
        UvLockPackage pkg = minimalPackage().withWheels(singletonList(
          UvLockArtifact.remote("https://example.com/six.whl", "sha256:abc", 11050L, "2026-07-07T14:33:57.9Z")));
        assertThat(write(pkg)).contains(
          "{ url = \"https://example.com/six.whl\", hash = \"sha256:abc\", size = 11050, upload-time = \"2026-07-07T14:33:57.9Z\" },");
    }

    @Test
    void revisionlessStyleOmitsUploadTime() {
        UvLockPackage pkg = minimalPackage().withSdist(
          UvLockArtifact.remote("https://example.com/six.tar.gz", "sha256:abc", 34031L, null));
        String written = UvLockWriter.write(new UvLock(1, null, ">=3.12", null, null, null, null, null, null, singletonList(pkg)));
        assertThat(written)
          .doesNotContain("revision")
          .doesNotContain("upload-time")
          .contains("sdist = { url = \"https://example.com/six.tar.gz\", hash = \"sha256:abc\", size = 34031 }");
    }

    @Test
    void singleElementRequiresDistStaysInlineAtAnyWidth() {
        char[] filler = new char[200];
        Arrays.fill(filler, 'x');
        String longMarker = "sys_platform == '" + new String(filler) + "'";
        UvLockPackage pkg = minimalPackage().withMetadata(UvLockMetadata.builder()
          .requiresDist(singletonList(
            UvLockRequirement.builder().name("charset-normalizer").marker(longMarker).specifier(">=3.0").build()))
          .build());
        assertThat(write(pkg)).contains(
          "requires-dist = [{ name = \"charset-normalizer\", marker = \"" + longMarker + "\", specifier = \">=3.0\" }]\n");
    }

    @Test
    void multiElementRequiresDistIsMultiline() {
        UvLockPackage pkg = minimalPackage().withMetadata(UvLockMetadata.builder()
          .requiresDist(Arrays.asList(
            UvLockRequirement.builder().name("click").specifier(">=8.1").build(),
            UvLockRequirement.builder().name("requests").extras(singletonList("socks")).specifier(">=2.31").build()))
          .build());
        assertThat(write(pkg)).contains(
          "requires-dist = [\n" +
            "    { name = \"click\", specifier = \">=8.1\" },\n" +
            "    { name = \"requests\", extras = [\"socks\"], specifier = \">=2.31\" },\n" +
            "]\n");
    }

    @Test
    void dependencyArrayIsMultilineEvenForOneElement() {
        UvLockPackage pkg = minimalPackage().withDependencies(singletonList(
          UvLockDependency.of("colorama").withMarker("sys_platform == 'win32'")));
        assertThat(write(pkg)).contains(
          "dependencies = [\n" +
            "    { name = \"colorama\", marker = \"sys_platform == 'win32'\" },\n" +
            "]\n");
    }

    @Test
    void markerStoredStringIsNotReNormalized() {
        // The format layer must not touch markers; normalization is the engine's job
        String unnormalized = "sys_platform=='win32' and  python_version >= '3.8'";
        UvLockPackage pkg = minimalPackage().withDependencies(singletonList(
          UvLockDependency.of("colorama").withMarker(unnormalized)));
        assertThat(write(pkg)).contains("marker = \"" + unnormalized + "\"");
    }

    @Test
    void emptyMetadataEmitsBareHeader() {
        UvLockPackage pkg = minimalPackage().withMetadata(UvLockMetadata.builder()
          .requiresDev(Collections.singletonMap("dev", singletonList(
            UvLockRequirement.builder().name("iniconfig").specifier(">=2.0").build())))
          .build());
        assertThat(write(pkg)).contains(
          "[package.metadata]\n" +
            "\n" +
            "[package.metadata.requires-dev]\n" +
            "dev = [{ name = \"iniconfig\", specifier = \">=2.0\" }]\n");
    }

    @Test
    void stringsRequiringEscapesAreRejected() {
        UvLockPackage pkg = minimalPackage().withVersion("1.0\"evil");
        assertThatThrownBy(() -> write(pkg))
          .isInstanceOf(UvLockFormatException.class)
          .hasMessageContaining("escaping");
    }

    @Test
    void endsWithExactlyOneNewline() {
        String written = write(minimalPackage());
        assertThat(written).endsWith(" }\n");
        assertThat(written).doesNotEndWith("\n\n");
    }
}
