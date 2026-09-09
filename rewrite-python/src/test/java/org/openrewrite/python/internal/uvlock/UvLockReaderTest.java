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

import java.util.Objects;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.openrewrite.python.internal.uvlock.UvLockFixtures.resource;

class UvLockReaderTest {

    @Test
    void headerAndPackages() {
        UvLock lock = UvLockReader.parse(resource("a-multi-package/uv.lock"));
        assertThat(lock.getVersion()).isEqualTo(1);
        assertThat(lock.getRevision()).isEqualTo(3);
        assertThat(lock.getRequiresPython()).isEqualTo(">=3.12");
        assertThat(lock.getResolutionMarkers()).isNull();
        assertThat(lock.getPackages()).extracting(UvLockPackage::getName).containsExactly(
          "certifi", "charset-normalizer", "click", "colorama", "fixture-a", "idna", "requests", "urllib3");
        UvLockPackage root = Objects.requireNonNull(lock.getPackage("fixture-a"));
        assertThat(root.getSource().getType()).isEqualTo(UvLockSource.Type.VIRTUAL);
        assertThat(root.getSource().getValue()).isEqualTo(".");
        assertThat(Objects.requireNonNull(root.getMetadata()).getRequiresDist()).hasSize(2);
    }

    @Test
    void uploadTimeStoredVerbatimWithTrimmedTrailingZeros() {
        UvLock lock = UvLockReader.parse(resource("a-multi-package/uv.lock"));
        UvLockPackage pkg = Objects.requireNonNull(lock.getPackage("charset-normalizer"));
        assertThat(Objects.requireNonNull(pkg.getWheels()))
          .extracting(UvLockArtifact::getUploadTime)
          .contains("2026-07-07T14:33:57.9Z", "2026-07-07T14:34:12.47Z", "2026-07-07T14:34:03.04Z");
    }

    @Test
    void forkDuplicatesAndResolutionMarkers() {
        UvLock lock = UvLockReader.parse(resource("e2-true-fork/uv.lock"));
        assertThat(lock.getResolutionMarkers()).containsExactly(
          "python_full_version >= '3.11'", "python_full_version < '3.11'");
        assertThat(lock.getPackages()).extracting(UvLockPackage::getVersion)
          .containsExactly("0.1.0", "4.5.0", "4.16.0");
        UvLockPackage older = lock.getPackages().get(1);
        assertThat(older.getResolutionMarkers()).containsExactly("python_full_version < '3.11'");
        UvLockDependency edge = Objects.requireNonNull(
          Objects.requireNonNull(lock.getPackage("fixture-e2")).getDependencies()).get(0);
        assertThat(edge.getVersion()).isEqualTo("4.5.0");
        assertThat(Objects.requireNonNull(edge.getSource()).getValue()).isEqualTo("https://pypi.org/simple");
        assertThat(edge.getMarker()).isEqualTo("python_full_version < '3.11'");
    }

    @Test
    void providesExtrasKeepsDeclarationOrder() {
        UvLock lock = UvLockReader.parse(resource("g2-extras-order/uv.lock"));
        UvLockMetadata metadata = Objects.requireNonNull(
          Objects.requireNonNull(lock.getPackage("fixture-g2")).getMetadata());
        assertThat(metadata.getProvidesExtras()).containsExactly("zeta", "alpha");
        // While the optional-dependencies subtable keys are sorted
        assertThat(Objects.requireNonNull(lock.getPackage("fixture-g2")).getOptionalDependencies())
          .containsKeys("alpha", "zeta");
    }

    @Test
    void optionsAndManifest() {
        UvLock m2 = UvLockReader.parse(resource("m2-options/uv.lock"));
        assertThat(Objects.requireNonNull(m2.getOptions()).getExcludeNewer()).isEqualTo("2024-01-01T00:00:00Z");

        UvLock q = UvLockReader.parse(resource("q-workspace/uv.lock"));
        assertThat(Objects.requireNonNull(q.getManifest()).getMembers()).containsExactly("fixture-q", "libone");
        UvLockPackage libone = Objects.requireNonNull(q.getPackage("libone"));
        assertThat(libone.getSource().getType()).isEqualTo(UvLockSource.Type.EDITABLE);
        assertThat(libone.getSource().getValue()).isEqualTo("packages/libone");
    }

    @Test
    void flatIndexArtifactsHavePathOnly() {
        UvLock lock = UvLockReader.parse(resource("l-flat-index/uv.lock"));
        UvLockPackage six = Objects.requireNonNull(lock.getPackage("six"));
        assertThat(six.getSource().getValue()).isEqualTo("assets/shared");
        UvLockArtifact wheel = Objects.requireNonNull(six.getWheels()).get(0);
        assertThat(wheel.getPath()).isEqualTo("six-1.17.0-py2.py3-none-any.whl");
        assertThat(wheel.getUrl()).isNull();
        assertThat(wheel.getHash()).isNull();
        assertThat(wheel.getSize()).isNull();
        assertThat(wheel.getUploadTime()).isNull();
        assertThat(six.getSdist()).isNull();
    }

    @Test
    void oldRevisionsDetected() {
        UvLock rev0 = UvLockReader.parse(resource("o-old-uv/proj-0.5.0/uv.lock.as-0.5.0"));
        assertThat(rev0.getRevision()).isNull();
        assertThat(rev0.expectsUploadTime()).isFalse();
        assertThat(Objects.requireNonNull(Objects.requireNonNull(rev0.getPackage("six")).getSdist()).getUploadTime()).isNull();

        UvLock rev2 = UvLockReader.parse(resource("o-old-uv/proj-0.7.0/uv.lock.as-0.7.0"));
        assertThat(rev2.getRevision()).isEqualTo(2);
        assertThat(rev2.expectsUploadTime()).isTrue();
    }

    @Test
    void emptyMetadataTablePreserved() {
        UvLock lock = UvLockReader.parse(resource("m3-lexical/uv.lock"));
        assertThat(lock.getRequiresPython()).isEqualTo(">=3.10, <3.15");
        UvLockMetadata metadata = Objects.requireNonNull(
          Objects.requireNonNull(lock.getPackage("fixture-m3")).getMetadata());
        assertThat(metadata.getRequiresDist()).isNull();
        assertThat(Objects.requireNonNull(metadata.getRequiresDev())).containsKey("dev");
    }

    @Test
    void unrecognizedVersionThrows() {
        assertThatThrownBy(() -> UvLockReader.parse("version = 2\n"))
          .isInstanceOf(UvLockFormatException.class)
          .hasMessageContaining("version");
    }

    @Test
    void unrecognizedRevisionThrows() {
        assertThatThrownBy(() -> UvLockReader.parse("version = 1\nrevision = 4\n"))
          .isInstanceOf(UvLockFormatException.class)
          .hasMessageContaining("revision");
    }

    @Test
    void unknownTopLevelKeyThrows() {
        assertThatThrownBy(() -> UvLockReader.parse("version = 1\nfrozen = \"true\"\n"))
          .isInstanceOf(UvLockFormatException.class)
          .hasMessageContaining("frozen");
    }

    @Test
    void unknownPackageKeyThrows() {
        String lock = "version = 1\n\n[[package]]\nname = \"six\"\nversion = \"1.17.0\"\n" +
          "source = { registry = \"https://pypi.org/simple\" }\nyanked = \"maybe\"\n";
        assertThatThrownBy(() -> UvLockReader.parse(lock))
          .isInstanceOf(UvLockFormatException.class)
          .hasMessageContaining("yanked");
    }

    @Test
    void unknownTableThrows() {
        assertThatThrownBy(() -> UvLockReader.parse("version = 1\n\n[distribution]\n"))
          .isInstanceOf(UvLockFormatException.class)
          .hasMessageContaining("[distribution]");
    }

    @Test
    void unknownSourceTypeThrows() {
        String lock = "version = 1\n\n[[package]]\nname = \"six\"\nversion = \"1.17.0\"\n" +
          "source = { workspace = \".\" }\n";
        assertThatThrownBy(() -> UvLockReader.parse(lock))
          .isInstanceOf(UvLockFormatException.class)
          .hasMessageContaining("workspace");
    }

    @Test
    void escapeSequencesThrow() {
        assertThatThrownBy(() -> UvLockReader.parse("version = 1\nrequires-python = \">=3.12\\t\"\n"))
          .isInstanceOf(UvLockFormatException.class)
          .hasMessageContaining("Escape");
    }
}
