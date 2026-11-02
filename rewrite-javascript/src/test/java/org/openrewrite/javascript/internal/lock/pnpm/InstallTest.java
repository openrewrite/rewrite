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
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.openrewrite.javascript.internal.LockFileRegeneration.Kind;
import org.openrewrite.javascript.internal.lock.LockOnlyInstall;
import org.openrewrite.javascript.internal.lock.Rejected;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * What {@code pnpm add} saves for a new dependency; the scenarios are pnpm 11.2.2's recordings of {@code pnpm add}.
 */
class InstallTest {

    @ParameterizedTest
    @CsvSource(nullValues = "unset", textBlock = """
      catalog-add-root-declares-in-catalog-strict-cleanup, strict, unset
      catalog-entry-one-importer,                          manual, unset
      catalog-add-mode-capitalized-mismatch,               Strict, unset
      catalog-add-mode-null-mismatch,                      null,   unset
      catalog-add-save-name-prefer,                        prefer, tools
      catalog-add-save-name-manual,                        manual, tools
      catalog-add-save-name-default,                       manual, default
      catalog-add-save-name-empty,                         manual, ''
      catalog-add-save-name-null,                          manual, unset
      """)
    void settingsAsPnpmReadsThem(String scenario, String mode, @Nullable String saveCatalogName) {
        Map<String, @Nullable Object> workspace = workspace(scenario);
        assertThat(Install.catalogMode(workspace)).isEqualTo(mode);
        assertThat(Install.saveCatalogName(workspace)).isEqualTo(saveCatalogName);
    }

    @Test
    void theDefaultCatalogsEntryOfTheSameSpecifierIsSavedAsCatalog() {
        assertThat(Install.installSome("wrappy", "1.0.1", workspace("catalog-entry-one-importer")))
          .isEqualTo(new Install.Saved("catalog:", null));
        assertThat(Install.installSome("wrappy", "1.0.2", workspace("catalog-entry-one-importer")))
          .isEqualTo(new Install.Saved("1.0.2", null));
    }

    @Test
    void aNewDependencyIsSavedInTheCatalogSaveCatalogNameNames() {
        assertThat(Install.installSome("wrappy", "1.0.2", workspace("catalog-add-save-name-manual")))
          .isEqualTo(new Install.Saved("catalog:tools", "tools"));
    }

    @Test
    void strictRejectsAVersionOutsideTheCatalogAndARangeFailsTheComparison() {
        assertThatThrownBy(() -> Install.installSome("wrappy", "1.0.1",
          workspace("catalog-add-root-declares-mismatch-strict")))
          .isInstanceOfSatisfying(Rejected.class, e -> assertThat(LockOnlyInstall.PNPM.failure(e))
            .satisfies(failure -> {
                assertThat(failure.getKind()).isEqualTo(Kind.PM_REJECTS);
                assertThat(failure.getDetail()).isEqualTo("pnpm rejects this edit: " +
                  "[ERR_PNPM_CATALOG_VERSION_MISMATCH] Wanted dependency outside the version range defined in catalog");
            }));
        assertThatThrownBy(() -> Install.installSome("wrappy", "^1.0.0", workspace("catalog-add-prefer-range")))
          .isInstanceOfSatisfying(Rejected.class, e -> assertThat(LockOnlyInstall.PNPM.failure(e).getKind())
            .isEqualTo(Kind.PM_FAILS));
    }

    private static Map<String, @Nullable Object> workspace(String scenario) {
        return WorkspaceManifest.readWorkspaceManifest(file("/lock/catalogs/pnpm/" + scenario + "/workspace-before"));
    }

    private static String file(String path) {
        try (InputStream in = InstallTest.class.getResourceAsStream(path)) {
            if (in == null) {
                throw new IllegalArgumentException(path);
            }
            ByteArrayOutputStream out = new ByteArrayOutputStream();
            byte[] buf = new byte[8192];
            int n;
            while ((n = in.read(buf)) >= 0) {
                out.write(buf, 0, n);
            }
            return new String(out.toByteArray(), StandardCharsets.UTF_8);
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }
}
