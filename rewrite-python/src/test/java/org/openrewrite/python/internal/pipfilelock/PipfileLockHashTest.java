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
package org.openrewrite.python.internal.pipfilelock;

import org.junit.jupiter.api.Test;
import org.openrewrite.python.internal.pipfilelock.PipfileLockHash.HashVariant;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.openrewrite.python.internal.pipfilelock.Fixtures.pipfile;
import static org.openrewrite.python.internal.pipfilelock.PipfileLockHash.hash;

/**
 * All expected hashes were recorded from Python 3.14 via
 * {@code src/test/resources/pipfilelock/oracle.py hash [--canonical] [--sources-json ...]},
 * which reproduces pipenv's {@code Project.calculate_pipfile_hash}. The minimal fixture's
 * hash was additionally confirmed against a real {@code pipenv lock} run (pipenv 2026.6.2),
 * whose lock is checked in as {@code locks/real-pipenv-2026.6.2.Pipfile.lock}.
 */
class PipfileLockHashTest {

    @Test
    void minimal() {
        assertThat(hash(pipfile("minimal.Pipfile"), null, HashVariant.LEGACY))
          .isEqualTo("1b0334bbff72090cca9555fe2a3301493b925cd232de7eecf1ed9b35de6bd733");
    }

    @Test
    void matchesHashStoredByRealPipenvLock() {
        // locks/real-pipenv-2026.6.2.Pipfile.lock was produced by `pipenv lock` from minimal.Pipfile
        Map<String, Object> lock = PipfileLockWriter.read(Fixtures.resource("locks/real-pipenv-2026.6.2.Pipfile.lock"));
        Object stored = ((Map<?, ?>) ((Map<?, ?>) lock.get("_meta")).get("hash")).get("sha256");
        assertThat(hash(pipfile("minimal.Pipfile"), null, HashVariant.LEGACY)).isEqualTo(stored);
    }

    @Test
    void canonicalAgreesWithLegacyWhenNamesAlreadyCanonical() {
        assertThat(hash(pipfile("minimal.Pipfile"), null, HashVariant.CANONICAL))
          .isEqualTo(hash(pipfile("minimal.Pipfile"), null, HashVariant.LEGACY));
    }

    @Test
    void multipleSourcesWithEnvPlaceholdersNotExpanded() {
        assertThat(hash(pipfile("multi-source-env.Pipfile"), null, HashVariant.LEGACY))
          .isEqualTo("c8cc6b112412548d263c7e45ddd91b3134dc01fa16dc4f8dbe94be69f41a3ab0");
    }

    @Test
    void noSourceFallsBackToPypiDefault() {
        assertThat(hash(pipfile("no-source.Pipfile"), null, HashVariant.LEGACY))
          .isEqualTo("d2be1bfd4f8e21d1a5b7cd94828102726b5ca8834b4b391365cdc3e30c473927");
    }

    @Test
    void noSourceUsesFallbackSources() {
        Map<String, Object> corp = new LinkedHashMap<>();
        corp.put("name", "corp");
        corp.put("url", "https://pypi.corp.example.com/simple");
        corp.put("verify_ssl", true);
        Map<String, Object> mirror = new LinkedHashMap<>();
        mirror.put("name", "mirror");
        mirror.put("url", "https://mirror.example.com/simple");
        mirror.put("verify_ssl", false);
        assertThat(hash(pipfile("no-source.Pipfile"), List.of(corp, mirror), HashVariant.LEGACY))
          .isEqualTo("18d66258d981039319a6809638e00379788f054cb892fee021ba8a3758cf5241");
    }

    @Test
    void quotedKeysHashUnquoted() {
        assertThat(hash(pipfile("quoted-keys.Pipfile"), null, HashVariant.LEGACY))
          .isEqualTo("2cedec755da04e75358fceb72e44d4dd5ba38b78e960b2f5d1a81a68f927fc62");
        // "ruamel.yaml" canonicalizes to "ruamel-yaml", so the variants diverge here
        assertThat(hash(pipfile("quoted-keys.Pipfile"), null, HashVariant.CANONICAL))
          .isEqualTo("2a69808fc37e4445667ab07611c26f288dbdfd1a9eb518d45298be6eb2677e10");
    }

    @Test
    void inlineTableEntries() {
        assertThat(hash(pipfile("inline-table.Pipfile"), null, HashVariant.LEGACY))
          .isEqualTo("b73cbc2910a8d50be8df8e1ea4dc674cdd942ddb2f113364ab6f98e999231cba");
    }

    @Test
    void customCategoriesHashedButScriptsPipenvPipfileExcluded() {
        assertThat(hash(pipfile("custom-categories.Pipfile"), null, HashVariant.LEGACY))
          .isEqualTo("e90c0889bae811bc3393ebd618b3c224263734dd41eb8d3f8a67b39cd4c776bd");
    }

    @Test
    void unicodeValuesIncludingAstralPlane() {
        assertThat(hash(pipfile("unicode.Pipfile"), null, HashVariant.LEGACY))
          .isEqualTo("2240e7e945ab8dbf1604a69bee2f177067bedb1b38a87182ab5550894f25d8b2");
    }

    @Test
    void canonicalVariantDivergesOnNonCanonicalNames() {
        String legacy = hash(pipfile("canonical-divergence.Pipfile"), null, HashVariant.LEGACY);
        String canonical = hash(pipfile("canonical-divergence.Pipfile"), null, HashVariant.CANONICAL);
        assertThat(legacy).isEqualTo("348ee7ebdd82437880022b4ca66056b6793709ed612fabcf3ad5ea125d31b223");
        assertThat(canonical).isEqualTo("c7948c5eadab97b5b4110d72561a29defa6bab06d57b9284cd2f2cfa94e1b36c");
        assertThat(canonical).isNotEqualTo(legacy);
    }

    @Test
    void pep503Canonicalization() {
        assertThat(PipfileLockHash.canonicalize("Flask")).isEqualTo("flask");
        assertThat(PipfileLockHash.canonicalize("Flask_SQLAlchemy")).isEqualTo("flask-sqlalchemy");
        assertThat(PipfileLockHash.canonicalize("ruamel.YAML")).isEqualTo("ruamel-yaml");
        assertThat(PipfileLockHash.canonicalize("a-_.b")).isEqualTo("a-b");
    }
}
