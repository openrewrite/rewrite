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
package org.openrewrite.golang.internal.modgraph;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class GoModFileTest {

    @Test
    void parsesModuleGoAndBothRequireForms() {
        GoModFile mf = GoModFile.parse(
          "module github.com/example/Project\n" +
          "\n" +
          "go 1.21\n" +
          "\n" +
          "require github.com/spf13/cobra v1.8.0\n" +
          "\n" +
          "require (\n" +
          "\tgithub.com/davecgh/go-spew v1.1.1 // indirect\n" +
          "\tgopkg.in/yaml.v3 v3.0.1 // indirect\n" +
          "\tgithub.com/stretchr/testify v1.9.0\n" +
          ")\n"
        );

        assertThat(mf.modulePath()).isEqualTo("github.com/example/Project");
        assertThat(mf.goVersion()).isEqualTo("1.21");
        assertThat(mf.requires()).hasSize(4);

        // single require, direct
        assertThat(mf.requires()).anySatisfy(r -> {
            assertThat(r.path).isEqualTo("github.com/spf13/cobra");
            assertThat(r.version).isEqualTo("v1.8.0");
            assertThat(r.indirect).isFalse();
        });
        // block require, indirect flag honored
        assertThat(mf.requires()).anySatisfy(r -> {
            assertThat(r.path).isEqualTo("github.com/davecgh/go-spew");
            assertThat(r.indirect).isTrue();
        });
        // block require, direct (no comment)
        assertThat(mf.requires()).anySatisfy(r -> {
            assertThat(r.path).isEqualTo("github.com/stretchr/testify");
            assertThat(r.indirect).isFalse();
        });
    }

    @Test
    void parsesReplaceFormsSingleAndBlock() {
        GoModFile mf = GoModFile.parse(
          "module m\n\n" +
          "replace github.com/old/pkg => github.com/new/pkg v1.2.3\n" +
          "replace (\n" +
          "\tgithub.com/a v1.0.0 => github.com/a v1.0.1\n" +
          "\tgithub.com/local => ../local/path\n" +
          ")\n"
        );

        assertThat(mf.replaces()).hasSize(3);
        assertThat(mf.replaces()).anySatisfy(r -> {
            assertThat(r.oldPath).isEqualTo("github.com/old/pkg");
            assertThat(r.oldVersion).isNull();
            assertThat(r.newPath).isEqualTo("github.com/new/pkg");
            assertThat(r.newVersion).isEqualTo("v1.2.3");
        });
        assertThat(mf.replaces()).anySatisfy(r -> {
            assertThat(r.oldPath).isEqualTo("github.com/a");
            assertThat(r.oldVersion).isEqualTo("v1.0.0");
            assertThat(r.newVersion).isEqualTo("v1.0.1");
        });
        // local filesystem replace: no new version
        assertThat(mf.replaces()).anySatisfy(r -> {
            assertThat(r.oldPath).isEqualTo("github.com/local");
            assertThat(r.newPath).isEqualTo("../local/path");
            assertThat(r.newVersion).isNull();
        });
    }

    @Test
    void escapesUppercaseForProxyAndCacheLayout() {
        assertThat(ModulePath.escapePath("github.com/Azure/azure-sdk-for-go"))
          .isEqualTo("github.com/!azure/azure-sdk-for-go");
        assertThat(ModulePath.escapePath("github.com/spf13/cobra"))
          .isEqualTo("github.com/spf13/cobra"); // no uppercase, unchanged
        assertThat(ModulePath.escapeVersion("v1.2.3")).isEqualTo("v1.2.3");
        assertThat(ModulePath.escapeVersion("v0.0.0-20210101000000-ABCDEF"))
          .isEqualTo("v0.0.0-20210101000000-!a!b!c!d!e!f");
    }
}
