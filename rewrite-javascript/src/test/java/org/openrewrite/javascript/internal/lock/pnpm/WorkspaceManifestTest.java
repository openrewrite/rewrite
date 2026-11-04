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
import static org.openrewrite.javascript.internal.lock.pnpm.PnpmTestSupport.json;

/**
 * workspace/workspace-manifest-reader/test/index.ts of pnpm 11.2.2, its fixtures inline.
 */
class WorkspaceManifestTest {

    private static final String PACKAGES = "packages:\n  - \"packages/**\"\n  - \"types\"\n\n";

    private static void invalid(String text, String message) {
        assertThatThrownBy(() -> WorkspaceManifest.readWorkspaceManifest(text))
          .isInstanceOf(PnpmError.class)
          .hasMessageContaining(message)
          .extracting(e -> ((PnpmError) e).getCode()).isEqualTo("ERR_PNPM_INVALID_WORKSPACE_CONFIGURATION");
    }

    @Test
    void worksWithAValidWorkspaceFile() {
        assertThat(json(WorkspaceManifest.readWorkspaceManifest("packages:\n  - \"packages/**\"\n  - \"types\"\n")))
          .isEqualTo("{'packages':['packages/**','types']}");
    }

    @Test
    void throwsOnStringOrArrayContent() {
        invalid("\"pkg\"\n", "Expected object but found - string");
        invalid("- \"pkg\"\n", "Expected object but found - array");
    }

    @Test
    void thePackagesField() {
        assertThat(WorkspaceManifest.readWorkspaceManifest("packages:\n")).isNotNull();
        invalid("packages: \"pkg\"\n", "packages field is not an array");
        invalid("packages:\n  - \"\"\n", "Missing or empty package");
        invalid("packages:\n  - 1\n", "Invalid package type - number");
    }

    @Test
    void worksWhenNoWorkspaceFileIsPresentOrItIsEmptyOrNull() {
        assertThat(WorkspaceManifest.readWorkspaceManifest(null)).isNull();
        assertThat(WorkspaceManifest.readWorkspaceManifest("")).isNull();
        assertThat(WorkspaceManifest.readWorkspaceManifest("null\n")).isNull();
    }

    @Test
    void catalogField() {
        assertThat(json(WorkspaceManifest.readWorkspaceManifest(PACKAGES + "catalog:\n  foo: ^1.0.0\n")))
          .isEqualTo("{'packages':['packages/**','types'],'catalog':{'foo':'^1.0.0'}}");
        invalid(PACKAGES + "catalog:\n  - test\n", "Expected catalog field to be an object, but found - array");
        invalid(PACKAGES + "catalog: 5\n", "Expected catalog field to be an object, but found - number");
        invalid(PACKAGES + "catalog:\n  foo: {}\n",
          "Invalid catalog entry for foo. Expected string, but found: object");
    }

    @Test
    void catalogsField() {
        assertThat(json(WorkspaceManifest.readWorkspaceManifest(PACKAGES + "catalog:\n  bar: ^1.0.0\n\ncatalogs:\n" +
                                                                "  foo:\n    bar: ^2.0.0\n")))
          .isEqualTo("{'packages':['packages/**','types'],'catalog':{'bar':'^1.0.0'}," +
                     "'catalogs':{'foo':{'bar':'^2.0.0'}}}");
        invalid(PACKAGES + "catalogs:\n  - foo\n", "Expected catalogs field to be an object, but found - array");
        invalid(PACKAGES + "catalogs: 5\n", "Expected catalogs field to be an object, but found - number");
        invalid(PACKAGES + "catalogs:\n  foo:\n    - bar\n",
          "Expected named catalog foo to be an object, but found - array");
        invalid(PACKAGES + "catalogs:\n  foo: 92\n", "Expected named catalog foo to be an object, but found - number");
        invalid(PACKAGES + "catalogs:\n  foo: null\n", "Expected named catalog foo to be an object, but found - null");
        invalid(PACKAGES + "catalogs:\n  foo:\n    bar: {}\n",
          "Catalog 'foo' has invalid entry 'bar'. Expected string specifier, but found: object");
    }
}
