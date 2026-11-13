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
package org.openrewrite.javascript.internal.lock.berry;

import org.junit.jupiter.api.Test;
import org.openrewrite.javascript.internal.lock.Host;
import org.openrewrite.javascript.internal.lock.HostEnvironment;

import java.time.Instant;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Yarn's packages/plugin-catalog/tests/utils.test.ts, its cases with the catalogs set through {@code .yarnrc.yml}
 * (the two that spy on {@code bindDescriptor} through a mock resolver are covered by the binding resolver here).
 */
class CatalogsTest {

    @Test
    void isCatalogReference() {
        assertThat(Catalogs.isCatalogReference("catalog:")).isTrue();
        assertThat(Catalogs.isCatalogReference("catalog:some-reference")).isTrue();
        assertThat(Catalogs.isCatalogReference("catalog:@scope/package")).isTrue();
        for (String range : new String[]{"^1.0.0", "npm:^1.0.0", "file:./package", "workspace:^", "", "not-catalog:",
                "catalogs:", "catalog", "CATALOG:", " catalog:"}) {
            assertThat(Catalogs.isCatalogReference(range)).as(range).isFalse();
        }
    }

    @Test
    void getCatalogName() {
        assertThat(Catalogs.getCatalogName(descriptor(null, "test-package", "catalog:"))).isNull();
        assertThat(Catalogs.getCatalogName(descriptor(null, "test-package", "catalog:react18"))).isEqualTo("react18");
        assertThat(Catalogs.getCatalogName(descriptor(null, "test-package", "catalog:my-ref_with.special-chars")))
                .isEqualTo("my-ref_with.special-chars");
    }

    @Test
    void getCatalogEntryName() {
        assertThat(Catalogs.getCatalogEntryName(descriptor(null, "react", "catalog:"))).isEqualTo("react");
        assertThat(Catalogs.getCatalogEntryName(descriptor("types", "node", "catalog:"))).isEqualTo("@types/node");
    }

    @Test
    void resolvesFromTheDefaultCatalog() {
        Project project = project("catalog:\n  react: \"npm:^18.0.0\"\n  lodash: \"npm:~4.17.21\"\n");
        Descriptor resolved = resolve(project, descriptor(null, "react", "catalog:"));
        assertThat(resolved.range).isEqualTo("npm:^18.0.0");
        assertThat(StructUtils.stringifyIdent(resolved)).isEqualTo("react");
        assertThat(resolve(project, descriptor(null, "lodash", "catalog:")).range).isEqualTo("npm:~4.17.21");
    }

    @Test
    void normalizesTheResolvedDescriptor() {
        Project project = project("catalog:\n  typescript: ^5.0.0\n");
        assertThat(resolve(project, descriptor(null, "typescript", "catalog:")).range).isEqualTo("npm:^5.0.0");
    }

    @Test
    void throwsWhenTheCatalogIsEmptyOrLacksTheEntry() {
        assertThatThrownBy(() -> resolve(project(""), descriptor(null, "react", "catalog:")))
                .isInstanceOf(ReportError.class).hasMessageContaining("catalog not found or empty");
        assertThatThrownBy(() -> resolve(project("catalog:\n  lodash: ~4.17.21\n"),
                descriptor(null, "react", "catalog:")))
                .isInstanceOf(ReportError.class).hasMessageContaining("entry not found in default catalog");
    }

    @Test
    void handlesScopedPackages() {
        Descriptor resolved = resolve(project("catalog:\n  \"@types/node\": ^20.0.0\n"),
                descriptor("types", "node", "catalog:"));
        assertThat(resolved.range).isEqualTo("npm:^20.0.0");
        assertThat(StructUtils.stringifyIdent(resolved)).isEqualTo("@types/node");
    }

    @Test
    void resolvesFromNamedCatalogs() {
        Project project = project("catalog:\n  lodash: \"npm:^4.17.21\"\ncatalogs:\n  react18:\n" +
                                  "    react: \"npm:^18.3.1\"\n    react-dom: \"npm:^18.3.1\"\n  react17:\n" +
                                  "    react: \"npm:^17.0.2\"\n  empty: {}\n");
        assertThat(resolve(project, descriptor(null, "react", "catalog:react18")).range).isEqualTo("npm:^18.3.1");
        assertThat(resolve(project, descriptor(null, "react", "catalog:react17")).range).isEqualTo("npm:^17.0.2");
        assertThat(resolve(project, descriptor(null, "lodash", "catalog:")).range).isEqualTo("npm:^4.17.21");
        assertThatThrownBy(() -> resolve(project, descriptor(null, "react", "catalog:nonexistent")))
                .isInstanceOf(ReportError.class).hasMessageContaining("catalog \"nonexistent\" not found or empty");
        assertThatThrownBy(() -> resolve(project, descriptor(null, "vue", "catalog:react18")))
                .isInstanceOf(ReportError.class).hasMessageContaining("entry not found in catalog \"react18\"");
        assertThatThrownBy(() -> resolve(project, descriptor(null, "react", "catalog:empty")))
                .isInstanceOf(ReportError.class).hasMessageContaining("catalog \"empty\" not found or empty");
        assertThatThrownBy(() -> resolve(project(""), descriptor(null, "react", "catalog:react18")))
                .isInstanceOf(ReportError.class).hasMessageContaining("catalog \"react18\" not found or empty");
    }

    @Test
    void bindsToTheTopLevelWorkspace() {
        Project project = project("catalog:\n  local: \"file:./local\"\n");
        assertThat(resolve(project, descriptor(null, "local", "catalog:")).range)
                .isEqualTo("file:./local::locator=test-project%40workspace%3A.");
    }

    private static Descriptor resolve(Project project, Descriptor dependency) {
        Resolver resolver = new BindingResolver();
        return Catalogs.resolveDescriptorFromCatalog(project, dependency, resolver,
                new Resolver.ResolveOptions(project, resolver, BerryScenarios.NO_REGISTRY));
    }

    private static Descriptor descriptor(String scope, String name, String range) {
        return StructUtils.makeDescriptor(StructUtils.makeIdent(scope, name), range);
    }

    private static Project project(String yarnrc) {
        Configuration options = Configuration.load(Collections.singletonMap(".yarnrc.yml", yarnrc),
                HostEnvironment.of(Collections.emptyMap()));
        Project project = new Project(BerryScenarios.restricted(options, YarnRelease.of("4.10.3")), Instant.EPOCH, null,
                Collections.emptyMap(), Host.UNKNOWN);
        project.setupWorkspaces(new FastGlob(Collections.singletonList("."), null, Host.UNKNOWN),
                cwd -> Manifest.fromText("{\"name\": \"test-project\", \"version\": \"1.0.0\"}"));
        return project;
    }

    /**
     * The file resolver's {@code bindDescriptor}, which binds a {@code file:} range to its parent; any other range
     * is returned as it is, as the test's mock resolver does.
     */
    private static final class BindingResolver implements Resolver {
        @Override
        public Descriptor bindDescriptor(Descriptor descriptor, Locator fromLocator, ResolveOptions opts) {
            if (!descriptor.range.startsWith("file:")) {
                return descriptor;
            }
            return StructUtils.bindDescriptor(descriptor,
                    Collections.singletonMap("locator", StructUtils.stringifyLocator(fromLocator)));
        }

        @Override
        public boolean supportsDescriptor(Descriptor descriptor, ResolveOptions opts) {
            throw new UnsupportedOperationException();
        }

        @Override
        public boolean supportsLocator(Locator locator, ResolveOptions opts) {
            throw new UnsupportedOperationException();
        }

        @Override
        public boolean shouldPersistResolution(Locator locator, ResolveOptions opts) {
            throw new UnsupportedOperationException();
        }

        @Override
        public Map<String, Descriptor> getResolutionDependencies(Descriptor descriptor, ResolveOptions opts) {
            throw new UnsupportedOperationException();
        }

        @Override
        public List<Locator> getCandidates(Descriptor descriptor, Map<String, Package> dependencies,
                                           ResolveOptions opts) {
            throw new UnsupportedOperationException();
        }

        @Override
        public Satisfying getSatisfying(Descriptor descriptor, Map<String, Package> dependencies,
                                        List<Locator> locators, ResolveOptions opts) {
            throw new UnsupportedOperationException();
        }

        @Override
        public Package resolve(Locator locator, ResolveOptions opts) {
            throw new UnsupportedOperationException();
        }
    }
}
