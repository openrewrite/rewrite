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
package org.openrewrite.javascript.search;

import org.junit.jupiter.api.Test;
import org.openrewrite.DocumentExample;
import org.openrewrite.javascript.marker.NodeResolutionResult;
import org.openrewrite.javascript.marker.NodeResolutionResult.Dependency;
import org.openrewrite.javascript.marker.NodeResolutionResult.ResolvedDependency;
import org.openrewrite.test.RecipeSpec;
import org.openrewrite.test.RewriteTest;

import java.util.List;

import static java.util.Collections.emptyList;
import static java.util.Collections.singletonList;
import static org.assertj.core.api.Assertions.assertThat;
import static org.openrewrite.Tree.randomId;
import static org.openrewrite.json.Assertions.json;
import static org.openrewrite.test.SourceSpecs.text;

class ModuleHasDependencyTest implements RewriteTest {

    private static final String PACKAGE_JSON = "{}";

    @Override
    public void defaults(RecipeSpec spec) {
        spec.recipe(new ModuleHasDependency("react", null, "^17.0.0", null));
    }

    @DocumentExample
    @Test
    void marksAllSourcesOfPackageWithDependency() {
        rewriteRun(
          json(
            PACKAGE_JSON,
            "/*~~(Module has dependency: react@^17.0.0)~~>*/{}",
            spec -> spec.path("packages/web/package.json").markers(node(direct("react", "^17.0.1", resolved("react", "17.0.2"))))
          ),
          text(
            "export const a = 1;",
            "~~(Module has dependency: react@^17.0.0)~~>export const a = 1;",
            spec -> spec.path("packages/web/src/a.ts")
          ),
          json(
            PACKAGE_JSON,
            spec -> spec.path("packages/api/package.json").markers(node(direct("react", "^18.2.0", resolved("react", "18.3.1"))))
          ),
          text(
            "export const b = 1;",
            spec -> spec.path("packages/api/src/b.ts")
          ),
          text(
            "export const c = 1;",
            spec -> spec.path("scripts/c.ts")
          )
        );
    }

    @Test
    void workspaceRootOwnsSourcesOutsideMembers() {
        rewriteRun(
          json(
            PACKAGE_JSON,
            "/*~~(Module has dependency: react@^17.0.0)~~>*/{}",
            spec -> spec.path("package.json").markers(node(direct("react", "^17.0.1", resolved("react", "17.0.2"))))
          ),
          text(
            "export const c = 1;",
            "~~(Module has dependency: react@^17.0.0)~~>export const c = 1;",
            spec -> spec.path("scripts/c.ts")
          ),
          json(
            PACKAGE_JSON,
            spec -> spec.path("packages/api/package.json").markers(node())
          ),
          text(
            "export const b = 1;",
            spec -> spec.path("packages/api/src/b.ts")
          )
        );
    }

    @Test
    void unresolvedDependencyUsesLowestAdmittedVersion() {
        rewriteRun(
          json(
            PACKAGE_JSON,
            "/*~~(Module has dependency: react@^17.0.0)~~>*/{}",
            spec -> spec.markers(node(direct("react", "~17.0.1", null)))
          )
        );
    }

    @Test
    void unparseableConstraintDoesNotMatchVersion() {
        rewriteRun(
          json(
            PACKAGE_JSON,
            spec -> spec.markers(node(direct("react", "workspace:*", null)))
          )
        );
    }

    @Test
    void transitiveDependencyMatchesUnlessOnlyDirect() {
        ResolvedDependency react = resolved("react", "17.0.2");
        ResolvedDependency ui = new ResolvedDependency("ui-kit", "1.0.0",
          singletonList(new Dependency("react", "^17.0.0", react)), null, null, null, null, null);
        NodeResolutionResult marker = node(direct("ui-kit", "^1.0.0", ui));
        rewriteRun(
          json(
            PACKAGE_JSON,
            "/*~~(Module has dependency: react@^17.0.0)~~>*/{}",
            spec -> spec.markers(marker)
          )
        );
        rewriteRun(
          spec -> spec.recipe(new ModuleHasDependency("react", null, "^17.0.0", true)),
          json(
            PACKAGE_JSON,
            spec -> spec.markers(marker)
          )
        );
    }

    @Test
    void scopeRestrictsDeclaredSection() {
        NodeResolutionResult marker = new NodeResolutionResult(randomId(), null, null, null, "package.json", null,
          emptyList(), singletonList(direct("jest", "^29.0.0", resolved("jest", "29.7.0"))),
          emptyList(), emptyList(), emptyList(), emptyList(), null, null, null);
        rewriteRun(
          spec -> spec.recipe(new ModuleHasDependency("jest", "dependencies", null, null)),
          json(
            PACKAGE_JSON,
            spec -> spec.markers(marker)
          )
        );
        rewriteRun(
          spec -> spec.recipe(new ModuleHasDependency("jest", "devDependencies", null, null)),
          json(
            PACKAGE_JSON,
            "/*~~(Module has dependency: jest)~~>*/{}",
            spec -> spec.markers(marker)
          )
        );
    }

    @Test
    void globMatchesScopedPackages() {
        rewriteRun(
          spec -> spec.recipe(new ModuleHasDependency("@angular/*", null, null, null)),
          json(
            PACKAGE_JSON,
            "/*~~(Module has dependency: @angular/*)~~>*/{}",
            spec -> spec.markers(node(direct("@angular/core", "^17.0.0", null)))
          )
        );
    }

    @Test
    void invalidOptionsFailValidation() {
        assertThat(new ModuleHasDependency("react", "imports", null, null).validate().isValid()).isFalse();
        assertThat(new ModuleHasDependency("react", null, "[1.0,2.0)", null).validate().isValid()).isFalse();
    }

    private static NodeResolutionResult node(Dependency... dependencies) {
        return new NodeResolutionResult(randomId(), null, null, null, "package.json", null,
          List.of(dependencies), emptyList(), emptyList(), emptyList(), emptyList(),
          emptyList(), NodeResolutionResult.PackageManager.Npm, null, null);
    }

    private static Dependency direct(String name, String constraint, ResolvedDependency resolved) {
        return new Dependency(name, constraint, resolved);
    }

    private static ResolvedDependency resolved(String name, String version) {
        return new ResolvedDependency(name, version, null, null, null, null, null, null);
    }
}
