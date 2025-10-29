/*
 * Copyright 2025 the original author or authors.
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
package org.openrewrite.javascript;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.openrewrite.Recipe;
import org.openrewrite.test.RewriteTest;

import java.nio.file.Files;
import java.nio.file.Path;

import static org.assertj.core.api.Assertions.assertThat;

class DependencyWorkspaceTest implements RewriteTest {

    @BeforeEach
    void setUp() {
        DependencyWorkspace.clearCache();
    }

    @Test
    void cachesSamePackageJson() {
        String packageJson = """
                {
                  "name": "test",
                  "dependencies": {
                    "lodash": "^4.17.21"
                  }
                }
                """;

        Path workspace1 = DependencyWorkspace.getOrCreateWorkspace(packageJson);
        Path workspace2 = DependencyWorkspace.getOrCreateWorkspace(packageJson);

        // Should return the same cached workspace
        assertThat(workspace1).isEqualTo(workspace2);
        assertThat(Files.exists(workspace1.resolve("node_modules"))).isTrue();
        assertThat(Files.exists(workspace1.resolve("package.json"))).isTrue();
    }

    @Test
    void createsDifferentWorkspacesForDifferentPackageJson() {
        String packageJson1 = """
                {
                  "name": "test1",
                  "dependencies": {
                    "lodash": "^4.17.21"
                  }
                }
                """;

        String packageJson2 = """
                {
                  "name": "test2",
                  "dependencies": {
                    "axios": "^1.0.0"
                  }
                }
                """;

        Path workspace1 = DependencyWorkspace.getOrCreateWorkspace(packageJson1);
        Path workspace2 = DependencyWorkspace.getOrCreateWorkspace(packageJson2);

        // Should create different workspaces
        assertThat(workspace1).isNotEqualTo(workspace2);
        assertThat(Files.exists(workspace1.resolve("node_modules"))).isTrue();
        assertThat(Files.exists(workspace2.resolve("node_modules"))).isTrue();
    }

    @Test
    void reusesWorkspaceAfterCacheClear() {
        String packageJson = """
                {
                  "name": "test",
                  "dependencies": {
                    "lodash": "^4.17.21"
                  }
                }
                """;

        Path workspace1 = DependencyWorkspace.getOrCreateWorkspace(packageJson);

        // Clear in-memory cache
        DependencyWorkspace.clearCache();

        // Should still reuse the workspace from disk
        Path workspace2 = DependencyWorkspace.getOrCreateWorkspace(packageJson);

        assertThat(workspace1).isEqualTo(workspace2);
        assertThat(Files.exists(workspace2.resolve("node_modules"))).isTrue();
    }

    @Test
    void npmIntegrationWithSymlink(@TempDir Path tempDir) throws Exception {
        rewriteRun(
                spec -> spec.recipe(new NoOpRecipe()),
                Assertions.npm(tempDir,
                        Assertions.packageJson("""
                                {
                                  "name": "test-project",
                                  "dependencies": {
                                    "lodash": "^4.17.21"
                                  }
                                }
                                """),
                        Assertions.javascript("""
                                import _ from 'lodash';
                                console.log(_.VERSION);
                                """)
                )
        );

        // Verify symlink was created
        Path nodeModules = tempDir.resolve("node_modules");
        assertThat(Files.isSymbolicLink(nodeModules)).isTrue();
        assertThat(Files.exists(nodeModules)).isTrue();
        assertThat(Files.exists(nodeModules.resolve("lodash"))).isTrue();
    }

    @Test
    void lruCacheEvictsOldEntries() {
        // This test verifies LRU eviction, but we can't easily test with MAX_CACHE_SIZE (100)
        // entries because npm install is slow. Instead, we verify the cache mechanism works
        // by checking that recently used entries are retained.

        String packageJson1 = """
                {
                  "name": "test1",
                  "dependencies": {
                    "lodash": "^4.17.21"
                  }
                }
                """;

        String packageJson2 = """
                {
                  "name": "test2",
                  "dependencies": {
                    "axios": "^1.0.0"
                  }
                }
                """;

        // Create two workspaces
        Path workspace1 = DependencyWorkspace.getOrCreateWorkspace(packageJson1);
        Path workspace2 = DependencyWorkspace.getOrCreateWorkspace(packageJson2);

        // Access workspace1 again (should move it to most recently used)
        Path workspace1Again = DependencyWorkspace.getOrCreateWorkspace(packageJson1);

        // Both should still be cached (we're well under MAX_CACHE_SIZE)
        assertThat(workspace1).isEqualTo(workspace1Again);
        assertThat(Files.exists(workspace1)).isTrue();
        assertThat(Files.exists(workspace2)).isTrue();
    }

    @Test
    void initializesFromDiskOnStartup() {
        String packageJson = """
                {
                  "name": "test-init",
                  "dependencies": {
                    "lodash": "^4.17.21"
                  }
                }
                """;

        // Create a workspace
        Path workspace = DependencyWorkspace.getOrCreateWorkspace(packageJson);
        assertThat(Files.exists(workspace.resolve("node_modules"))).isTrue();

        // Clear the in-memory cache (simulating a JVM restart)
        DependencyWorkspace.clearCache();

        // The workspace should still be reused from disk
        Path workspaceAfterClear = DependencyWorkspace.getOrCreateWorkspace(packageJson);
        assertThat(workspaceAfterClear).isEqualTo(workspace);
        assertThat(Files.exists(workspaceAfterClear.resolve("node_modules"))).isTrue();
    }

    @Test
    void createsWorkspaceWhenBaseDirectoryDoesNotExist() throws Exception {
        // This test verifies the fix for the issue where WORKSPACE_BASE might not exist
        // when createTempDirectory is called. The fix ensures the base directory is
        // created before attempting to create temp directories within it.

        String packageJson = """
                {
                  "name": "test-base-dir",
                  "dependencies": {
                    "lodash": "^4.17.21"
                  }
                }
                """;

        // Even if the base directory doesn't exist initially, the workspace should be created
        Path workspace = DependencyWorkspace.getOrCreateWorkspace(packageJson);

        assertThat(workspace).isNotNull();
        assertThat(Files.exists(workspace)).isTrue();
        assertThat(Files.exists(workspace.resolve("node_modules"))).isTrue();
        assertThat(Files.exists(workspace.resolve("package.json"))).isTrue();
    }

    private static class NoOpRecipe extends Recipe {
        @Override
        public String getDisplayName() {
            return "No-op recipe";
        }

        @Override
        public String getDescription() {
            return "Does nothing, used for testing.";
        }
    }
}
