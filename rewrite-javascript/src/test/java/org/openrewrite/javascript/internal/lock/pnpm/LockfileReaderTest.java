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

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.Assertions.entry;
import static org.openrewrite.javascript.internal.lock.pnpm.PnpmTestSupport.json;

/**
 * lockfile/fs/test/read.test.ts (the cases of the install's own read: wanted version 9.0, merge conflicts
 * autofixed), write.test.ts's read cases and yamlDocuments.test.ts of pnpm 11.2.2, with their fixtures.
 */
class LockfileReaderTest {

    @Test
    void readWantedLockfile() {
        LockfileObject lockfile = LockfileReader.read(
          "lockfileVersion: '9.0'\n\nimporters:\n  .:\n    dependencies:\n" +
          "      foo:\n        version: '1.0.0'\n        specifier: '1'\n" +
          "    dependenciesMeta:\n      foo:\n        injected: true\n").getLockfile();
        assertThat(lockfile.getLockfileVersion()).isEqualTo("9.0");
        ProjectSnapshot importer = lockfile.getImporters().get(".");
        assertThat(importer.getDependencies()).containsExactly(entry("foo", "1.0.0"));
        assertThat(importer.getDevDependencies()).isNull();
        assertThat(importer.getOptionalDependencies()).isNull();
        assertThat(importer.getSpecifiers()).containsExactly(entry("foo", "1"));
        assertThat(json(importer.getDependenciesMeta())).isEqualTo("{'foo':{'injected':true}}");
    }

    @Test
    void aLockOfAnotherMajorIsNotCompatible() {
        assertThatThrownBy(() -> LockfileReader.read("lockfileVersion: 2\n"))
          .isInstanceOf(LockfileReader.IgnoredLockfile.class)
          .hasMessage("Lockfile pnpm-lock.yaml not compatible with current pnpm");
    }

    @Test
    void readWantedLockfileWithInlineSpecifiersFormat() {
        LockfileObject lockfile = LockfileReader.read(
          "lockfileVersion: '9.0'\n\nimporters:\n  .:\n    dependencies:\n" +
          "      is-positive:\n        specifier: '^1.0.0'\n" +
          "        version: '1.0.0'\n\npackages:\n  is-positive@1.0.0:\n" +
          "    resolution: {integrity: 'sha1-ChbBDewTLAqLCzb793Fo5VDvg/g='}\n\n" +
          "snapshots:\n  is-positive@1.0.0: {}\n").getLockfile();
        assertThat(lockfile.getImporters().get(".").getDependencies()).containsEntry("is-positive", "1.0.0");
        assertThat(lockfile.getImporters().get(".").getSpecifiers()).containsEntry("is-positive", "^1.0.0");
        assertThat(json(lockfile.getPackages().get("is-positive@1.0.0").getResolution()))
          .isEqualTo("{'integrity':'sha1-ChbBDewTLAqLCzb793Fo5VDvg/g='}");
    }

    @Test
    void readWantedLockfileSkipsEnvDocumentInCombinedLockfile() {
        String envDoc = "---\nlockfileVersion: env-1.0\nimporters:\n  .:\n    configDependencies:\n" +
                        "      typescript: 5.0.0\n\n---\n";
        String mainDoc = "lockfileVersion: '9.0'\nimporters:\n  .:\n    dependencies:\n      is-positive:\n" +
                         "        version: 1.0.0\n        specifier: ^1.0.0\npackages:\n  is-positive@1.0.0:\n" +
                         "    resolution:\n      integrity: sha1-ChbBDewTLAqLCzb793Fo5VDvg/g=\n";
        LockfileObject lockfile = LockfileReader.read(envDoc + mainDoc).getLockfile();
        assertThat(lockfile.getLockfileVersion()).isEqualTo("9.0");
        assertThat(lockfile.getImporters().get(".").getDependencies()).containsEntry("is-positive", "1.0.0");
    }

    @Test
    void readWantedLockfileReturnsNullForEnvOnlyLockfileWithNoMainDocument() {
        assertThat(LockfileReader.read("---\nlockfileVersion: env-1.0\n").getLockfile()).isNull();
    }

    @Test
    void streamReadFirstYamlDocument() {
        assertThat(LockfileReader.readFirstYamlDocument("---\nfoo: bar\n---\nlockfileVersion: 9.0\n"))
          .isEqualTo("foo: bar");
        assertThat(LockfileReader.readFirstYamlDocument("lockfileVersion: 9.0\n")).isNull();
        assertThat(LockfileReader.readFirstYamlDocument(null)).isNull();
        assertThat(LockfileReader.readFirstYamlDocument("---\nfoo: bar\n")).isNull();
        assertThat(LockfileReader.readFirstYamlDocument("\uFEFF---\nfoo: bar\n---\nlockfileVersion: 9.0\n"))
          .isEqualTo("foo: bar");
        assertThat(LockfileReader.readFirstYamlDocument("")).isNull();
        String envContent = "lockfileVersion: env-1.0\nimporters:\n  .:\n    foo: bar";
        assertThat(LockfileReader.readFirstYamlDocument("---\n" + envContent + "\n---\nlockfileVersion: 9.0\n"))
          .isEqualTo(envContent);
        assertThat(LockfileReader.readFirstYamlDocument(("---\n" + envContent + "\n---\nlockfileVersion: 9.0\n")
          .replace("\n", "\r\n"))).isEqualTo(envContent);
        assertThat(LockfileReader.readFirstYamlDocument("\uFEFF---\r\nfoo: bar\r\n---\r\nlockfileVersion: 9.0\r\n"))
          .isEqualTo("foo: bar");
    }

    @Test
    void extractMainDocument() {
        String content = "lockfileVersion: 9.0\npackages: {}\n";
        assertThat(LockfileReader.extractMainDocument(content)).isEqualTo(content);
        assertThat(LockfileReader.extractMainDocument("---\nfoo: bar\n")).isEmpty();
        assertThat(LockfileReader.extractMainDocument("---\nfoo: bar\n---\n" + content)).isEqualTo(content);
        assertThat(LockfileReader.extractMainDocument(("---\nfoo: bar\n---\n" + content).replace("\n", "\r\n")))
          .isEqualTo(content);
        assertThat(LockfileReader.extractMainDocument("lockfileVersion: 9.0\r\npackages: {}\r\n"))
          .isEqualTo("lockfileVersion: 9.0\npackages: {}\n");
    }

    /**
     * A lock with merge conflict markers, recorded with pnpm 11.2.2: pnpm merges both sides and resolves.
     */
    @Test
    void autofixesMergeConflicts() {
        LockfileReader.ReadResult read = LockfileReader.read(
          "lockfileVersion: '9.0'\n\nimporters:\n\n  .:\n    dependencies:\n      ms:\n<<<<<<< ours\n" +
          "        specifier: 2.1.2\n        version: 2.1.2\n=======\n" +
          "        specifier: 2.1.3\n        version: 2.1.3\n>>>>>>> theirs\n");
        assertThat(read.isHadConflicts()).isTrue();
        assertThat(read.getLockfile().getImporters().get(".").getDependencies()).containsEntry("ms", "2.1.3");
        assertThat(read.getLockfile().getImporters().get(".").getSpecifiers()).containsEntry("ms", "2.1.3");
    }
}
