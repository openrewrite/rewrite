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

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.openrewrite.javascript.internal.lock.pnpm.PnpmTestSupport.json;
import static org.openrewrite.javascript.internal.lock.pnpm.PnpmTestSupport.obj;

/**
 * catalogs/config, catalogs/protocol-parser and catalogs/resolver tests of pnpm 11.2.2. The resolver's
 * {@code matchCatalogResolveResult} dispatcher is a switch at each call site here, so its three cases are not ported.
 */
class CatalogsTest {

    @SuppressWarnings("unchecked")
    private static Map<String, @Nullable Map<String, @Nullable Object>> catalogs(String json) {
        return (Map<String, @Nullable Map<String, @Nullable Object>>) (Map<?, ?>) obj(json);
    }

    @Test
    void combinesImplicitDefaultAndNamedCatalogs() {
        assertThat(json(Catalogs.getCatalogsFromWorkspaceManifest(obj(
          "{'catalog':{'foo':'^1.0.0'},'catalogs':{'bar':{'baz':'^2.0.0'}}}"))))
          .isEqualTo("{'default':{'foo':'^1.0.0'},'bar':{'baz':'^2.0.0'}}");
    }

    @Test
    void combinesExplicitDefaultAndNamedCatalogs() {
        assertThat(json(Catalogs.getCatalogsFromWorkspaceManifest(obj(
          "{'catalogs':{'default':{'foo':'^1.0.0'},'bar':{'baz':'^2.0.0'}}}"))))
          .isEqualTo("{'default':{'foo':'^1.0.0'},'bar':{'baz':'^2.0.0'}}");
    }

    @Test
    void throwsIfDefaultCatalogIsDefinedMultipleTimes() {
        assertThatThrownBy(() -> Catalogs.getCatalogsFromWorkspaceManifest(obj(
          "{'catalog':{'bar':'^2.0.0'},'catalogs':{'default':{'foo':'^1.0.0'}}}")))
          .hasMessageContaining("The 'default' catalog was defined multiple times");
    }

    @Test
    void parseCatalogProtocol() {
        assertThat(Catalogs.parseCatalogProtocol("catalog:foo")).isEqualTo("foo");
        assertThat(Catalogs.parseCatalogProtocol("catalog:bar")).isEqualTo("bar");
        assertThat(Catalogs.parseCatalogProtocol("^1.0.0")).isNull();
        assertThat(Catalogs.parseCatalogProtocol("catalog:default")).isEqualTo("default");
        assertThat(Catalogs.parseCatalogProtocol("catalog:")).isEqualTo("default");
    }

    @Test
    void resolvesTheDefaultCatalog() {
        Map<String, @Nullable Map<String, @Nullable Object>> catalogs = catalogs("{'default':{'foo':'1.0.0'}}");
        assertThat(Catalogs.resolveFromCatalog(catalogs, "foo", "catalog:"))
          .isEqualTo(new Catalogs.Resolution("found", "default", "1.0.0", null));
        assertThat(Catalogs.resolveFromCatalog(catalogs, "foo", "catalog:default"))
          .isEqualTo(new Catalogs.Resolution("found", "default", "1.0.0", null));
    }

    @Test
    void resolvesNamedCatalog() {
        assertThat(Catalogs.resolveFromCatalog(catalogs("{'foo':{'bar':'1.0.0'}}"), "bar", "catalog:foo"))
          .isEqualTo(new Catalogs.Resolution("found", "foo", "1.0.0", null));
    }

    @Test
    void returnsUnusedForSpecifierNotUsingCatalogProtocol() {
        assertThat(Catalogs.resolveFromCatalog(catalogs("{'foo':{'bar':'1.0.0'}}"), "bar", "^2.0.0").getType())
          .isEqualTo("unused");
    }

    private static String error(String catalogsJson, String alias, String bareSpecifier) {
        Catalogs.Resolution result = Catalogs.resolveFromCatalog(catalogs(catalogsJson), alias, bareSpecifier);
        assertThat(result.getType()).isEqualTo("misconfiguration");
        return result.getError().getMessage();
    }

    @Test
    void misconfiguration() {
        assertThat(error("{'foo':{'bar':'1.0.0'}}", "bar", "catalog:"))
          .isEqualTo("No catalog entry 'bar' was found for catalog 'default'.");
        assertThat(error("{'foo':{'bar':'1.0.0'}}", "bar", "catalog:baz"))
          .isEqualTo("No catalog entry 'bar' was found for catalog 'baz'.");
        assertThat(error("{'foo':{'bar':'1.0.0'}}", "foo", "catalog:foo"))
          .isEqualTo("No catalog entry 'foo' was found for catalog 'foo'.");
        assertThat(error("{'foo':{'bar':'catalog:foo'}}", "bar", "catalog:foo"))
          .isEqualTo("Found invalid catalog entry using the catalog protocol recursively. The entry for 'bar' in " +
                     "catalog 'foo' is invalid.");
        assertThat(error("{'foo':{'bar':'workspace:*'}}", "bar", "catalog:foo"))
          .isEqualTo("The workspace protocol cannot be used as a catalog value. The entry for 'bar' in catalog 'foo' " +
                     "is invalid.");
        assertThat(error("{'foo':{'bar':'file:./bar.tgz'}}", "bar", "catalog:foo"))
          .isEqualTo("The entry for 'bar' in catalog 'foo' declares a dependency using the 'file' protocol. " +
                     "This is not yet supported, but may be in a future version of pnpm.");
        assertThat(error("{'foo':{'bar':'link:./bar'}}", "bar", "catalog:foo"))
          .isEqualTo("The entry for 'bar' in catalog 'foo' declares a dependency using the 'link' protocol. " +
                     "This is not yet supported, but may be in a future version of pnpm.");
    }
}
