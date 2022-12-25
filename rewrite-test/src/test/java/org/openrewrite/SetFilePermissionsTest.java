/*
 * Copyright 2022 the original author or authors.
 * <p>
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * <p>
 * https://www.apache.org/licenses/LICENSE-2.0
 * <p>
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.openrewrite;

import org.junit.jupiter.api.Test;
import org.openrewrite.test.RewriteTest;
import org.openrewrite.test.SourceSpecs;

import static org.assertj.core.api.Assertions.assertThat;

class SetFilePermissionsTest implements RewriteTest {

    @Test
    void revokeReadPermissions() {
        rewriteRun(
          spec -> spec.recipe(new SetFilePermissions("test.txt", false, true, true))
            .afterRecipe(run -> run.getResults().forEach(r -> {
                assertThat(r.getAfter()).isNotNull();
                assertThat(r.getAfter().getFileAttributes()).isNotNull();
                assertThat(r.getAfter().getFileAttributes().isReadable()).isFalse();
                assertThat(r.getAfter().getFileAttributes().isWritable()).isTrue();
                assertThat(r.getAfter().getFileAttributes().isExecutable()).isTrue();
            })),
          SourceSpecs.text("", "", spec -> spec.path("test.txt"))
        );
    }

    @Test
    void revokeWritePermissions() {
        rewriteRun(
          spec -> spec.recipe(new SetFilePermissions("test.txt", true, false, true))
            .afterRecipe(run -> run.getResults().forEach(r -> {
                assertThat(r.getAfter()).isNotNull();
                assertThat(r.getAfter().getFileAttributes()).isNotNull();
                assertThat(r.getAfter().getFileAttributes().isReadable()).isTrue();
                assertThat(r.getAfter().getFileAttributes().isWritable()).isFalse();
                assertThat(r.getAfter().getFileAttributes().isExecutable()).isTrue();
            })),
          SourceSpecs.text("", "", spec -> spec.path("test.txt"))
        );
    }

    @Test
    void revokeExecutablePermissions() {
        rewriteRun(
          spec ->
            spec.recipe(new SetFilePermissions("test.txt", true, true, false))
              .afterRecipe(run -> run.getResults().forEach(r -> {
                  assertThat(r.getAfter()).isNotNull();
                  assertThat(r.getAfter().getFileAttributes()).isNotNull();
                  assertThat(r.getAfter().getFileAttributes().isReadable()).isTrue();
                  assertThat(r.getAfter().getFileAttributes().isWritable()).isTrue();
                  assertThat(r.getAfter().getFileAttributes().isExecutable()).isFalse();
              })),
          SourceSpecs.text("", "", spec -> spec.path("test.txt"))
        );
    }
}
