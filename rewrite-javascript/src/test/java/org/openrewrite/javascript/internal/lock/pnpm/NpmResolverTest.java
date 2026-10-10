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
import org.openrewrite.javascript.internal.lock.pnpm.PickPackageFromMeta.NamedRegistryPackageSpec;
import org.openrewrite.javascript.internal.lock.pnpm.PickPackageFromMeta.RegistryPackageSpec;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Port of pnpm 11.2.2's resolving/npm-resolver tests whichVersionIsPinned, workspacePrefToNpm, normalizeRegistryUrl,
 * resolveJsr, resolveNamedRegistry, parseBareSpecifier and jsr-specifier-parser; the auth header case is the host's.
 */
class NpmResolverTest {

    @ParameterizedTest
    @CsvSource({
      "^1.0.0, major",
      "~1.0.0, minor",
      "1.0.0, patch",
      "*, none",
      "workspace:^1.0.0, major",
      "npm:foo@1.0.0, patch",
      "npm:@foo/foo@1.0.0, patch",
      "npm:foo@^1.0.0, major",
      "npm:@foo/foo@^1.0.0, major",
      "npm:@pnpm.e2e/qar@100.0.0, patch",
      "jsr:@foo/foo@1.0.0, patch",
      "jsr:foo@^1.0.0, major"
    })
    void whichVersionIsPinned(String spec, String expected) {
        assertThat(NpmResolver.whichVersionIsPinned(spec)).isEqualTo(expected);
    }

    @ParameterizedTest
    @CsvSource({
      "https://registry.example.com:443/package.tgz, https://registry.example.com/package.tgz",
      "http://registry.example.com:80/package.tgz, http://registry.example.com/package.tgz",
      "https://registry.example.com:8443/package.tgz, https://registry.example.com:8443/package.tgz",
      "http://registry.example.com:8080/package.tgz, http://registry.example.com:8080/package.tgz",
      "https://registry.example.com/package.tgz, https://registry.example.com/package.tgz",
      "http://registry.example.com/package.tgz, http://registry.example.com/package.tgz",
      "https://artifactory:443/api/npm/npm-virtual/uuid/-/uuid-9.0.1.tgz, " +
      "https://artifactory/api/npm/npm-virtual/uuid/-/uuid-9.0.1.tgz",
      "invalid-url, invalid-url"
    })
    void normalizeRegistryUrl(String input, String expected) {
        assertThat(NpmResolver.normalizeRegistryUrl(input)).isEqualTo(expected);
    }

    @Test
    void workspacePrefToNpmResolvesWorkspaceOnlyVersionAliases() {
        assertThat(NpmResolver.workspacePrefToNpm("workspace:")).isEqualTo("*");
        assertThat(NpmResolver.workspacePrefToNpm("workspace:*")).isEqualTo("*");
        assertThat(NpmResolver.workspacePrefToNpm("workspace:^")).isEqualTo("*");
        assertThat(NpmResolver.workspacePrefToNpm("workspace:~")).isEqualTo("*");
    }

    @Test
    void workspacePrefToNpmResolvesPackageNameAliases() {
        assertThat(NpmResolver.workspacePrefToNpm("workspace:is-positive@3.0.0")).isEqualTo("npm:is-positive@3.0.0");
        assertThat(NpmResolver.workspacePrefToNpm("workspace:is-positive@*")).isEqualTo("npm:is-positive@*");
        assertThat(NpmResolver.workspacePrefToNpm("workspace:is-positive@^")).isEqualTo("npm:is-positive@*");
    }

    @Test
    void workspacePrefToNpmResolvesScopedPackageNameAliases() {
        assertThat(NpmResolver.workspacePrefToNpm("workspace:@scope/is-positive@1.2.3"))
          .isEqualTo("npm:@scope/is-positive@1.2.3");
        assertThat(NpmResolver.workspacePrefToNpm("workspace:@scope/is-positive@^1.2.3"))
          .isEqualTo("npm:@scope/is-positive@^1.2.3");
        assertThat(NpmResolver.workspacePrefToNpm("workspace:@scope/is-positive@*"))
          .isEqualTo("npm:@scope/is-positive@*");
        assertThat(NpmResolver.workspacePrefToNpm("workspace:@scope/is-positive@~"))
          .isEqualTo("npm:@scope/is-positive@*");
    }

    private static final String NPM = "https://registry.npmjs.org/";
    private static final String JSR = "https://npm.jsr.io/";
    private static final String GH = "https://npm.pkg.github.com/";
    private static final String ENTERPRISE = "https://npm.enterprise.example.com/";
    private static final Set<String> GH_ALIASES = Collections.singleton("gh");

    private static final class FixtureRegistry implements RegistryMetadata {
        final Map<String, String> documents = new LinkedHashMap<>();
        final List<String> asked = new ArrayList<>();

        FixtureRegistry serve(String registry, String name, String fixture) {
            documents.put(registry + name, fixture(fixture));
            return this;
        }

        @Override
        public @Nullable String packument(String registry, String name, boolean fullMetadata) {
            asked.add(registry + name);
            return documents.get(registry + name);
        }

        @Override
        public @Nullable String attestations(String registry, String name, String version) {
            return null;
        }

        @Override
        public @Nullable String document(String url) {
            return null;
        }
    }

    private static String fixture(String name) {
        try (InputStream in = NpmResolverTest.class.getResourceAsStream("/pnpm/npm-resolver/" + name)) {
            if (in == null) {
                throw new IllegalStateException("no fixture " + name);
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

    private static NpmResolver resolver(FixtureRegistry registry, @Nullable Map<String, @Nullable Object> named) {
        Map<String, String> registries = new LinkedHashMap<>();
        registries.put("default", NPM);
        registries.put("@jsr", JSR);
        return new NpmResolver(registry, registries, NpmResolver.mergeNamedRegistries(named), null, true, false,
          Instant.parse("2026-10-08T00:00:00Z"));
    }

    private static ResolveDependencies.WantedDependency wanted(@Nullable String alias, String bareSpecifier) {
        return new ResolveDependencies.WantedDependency(alias, bareSpecifier, false, false);
    }

    private static PackageRequester.RequestPackageOptions calcSpecifier(boolean calcSpecifier) {
        PackageRequester.RequestPackageOptions opts = new PackageRequester.RequestPackageOptions();
        opts.calcSpecifier = calcSpecifier;
        return opts;
    }

    @Test
    void resolveFromJsr() {
        FixtureRegistry registry = new FixtureRegistry()
          .serve(JSR, "@jsr/rus__greet", "jsr-rus-greet")
          .serve(JSR, "@jsr/luca__cases", "jsr-luca-cases");
        NpmResolver.ResolveResult result = resolver(registry, null).resolveJsr(wanted("@rus/greet", "jsr:0.0.3"),
          calcSpecifier(true), null);
        assertThat(result).isNotNull();
        assertThat(result.resolvedVia).isEqualTo("jsr-registry");
        assertThat(result.id).isEqualTo("@jsr/rus__greet@0.0.3");
        assertThat(result.latest).isEqualTo("0.0.3");
        assertThat(result.manifest).containsEntry("name", "@jsr/rus__greet").containsEntry("version", "0.0.3");
        assertThat(result.resolution.get("integrity")).isInstanceOf(String.class);
        assertThat(result.resolution).containsEntry("tarball", "https://npm.jsr.io/~/11/@jsr/rus__greet/0.0.3.tgz");
        assertThat(result.normalizedBareSpecifier).isEqualTo("jsr:0.0.3");
        assertThat(registry.asked).containsExactly(JSR + "@jsr/rus__greet");
    }

    @Test
    void resolveFromJsrWithAliasRenaming() {
        FixtureRegistry registry = new FixtureRegistry().serve(JSR, "@jsr/rus__greet", "jsr-rus-greet");
        NpmResolver.ResolveResult result = resolver(registry, null).resolveJsr(wanted("greet", "jsr:@rus/greet@0.0.3"),
          calcSpecifier(false), null);
        assertThat(result).isNotNull();
        assertThat(result.resolvedVia).isEqualTo("jsr-registry");
        assertThat(result.id).isEqualTo("@jsr/rus__greet@0.0.3");
        assertThat(result.latest).isEqualTo("0.0.3");
        assertThat(result.resolution).containsEntry("tarball", "https://npm.jsr.io/~/11/@jsr/rus__greet/0.0.3.tgz");
        assertThat(result.alias).isEqualTo("@rus/greet");
    }

    @Test
    void resolveFromJsrRejectsAPackageWithoutScope() {
        assertThatThrownBy(() -> resolver(new FixtureRegistry(), null).resolveJsr(wanted("greet", "jsr:0.0.3"),
          calcSpecifier(false), null))
          .isInstanceOfSatisfying(PnpmError.class,
            e -> assertThat(e.getCode()).isEqualTo("ERR_PNPM_MISSING_JSR_PACKAGE_SCOPE"));
    }

    @Test
    void resolveFromJsrFlagsAnImmaturePick() {
        FixtureRegistry registry = new FixtureRegistry().serve(JSR, "@jsr/rus__greet", "jsr-rus-greet");
        PackageRequester.RequestPackageOptions opts = calcSpecifier(false);
        opts.publishedBy = Instant.parse("2020-01-01T00:00:00Z");
        NpmResolver.ResolveResult result = resolver(registry, null).resolveJsr(wanted("@rus/greet", "jsr:0.0.3"), opts,
          null);
        assertThat(result).isNotNull();
        assertThat(result.id).isEqualTo("@jsr/rus__greet@0.0.3");
        assertThat(result.policyViolation).isNotNull();
        assertThat(result.policyViolation.getName()).isEqualTo("@jsr/rus__greet");
        assertThat(result.policyViolation.getVersion()).isEqualTo("0.0.3");
        assertThat(result.policyViolation.getCode()).isEqualTo("MINIMUM_RELEASE_AGE_VIOLATION");
    }

    @Test
    void resolveFromNamedRegistryWithTheBuiltInGhAlias() {
        FixtureRegistry registry = new FixtureRegistry().serve(GH, "@acme/private", "gh-acme-private");
        NpmResolver.ResolveResult result = resolver(registry, null).resolveFromNamedRegistry(
          wanted("@acme/private", "gh:^2.0.0"), calcSpecifier(true), null);
        assertThat(result).isNotNull();
        assertThat(result.resolvedVia).isEqualTo("named-registry");
        assertThat(result.id).isEqualTo("@acme/private@2.1.0");
        assertThat(result.latest).isEqualTo("2.1.0");
        assertThat(result.manifest).containsEntry("name", "@acme/private").containsEntry("version", "2.1.0");
        assertThat(result.resolution).containsEntry("tarball",
          "https://npm.pkg.github.com/download/@acme/private/2.1.0/acme-private-2.1.0.tgz");
        assertThat(result.normalizedBareSpecifier).isEqualTo("gh:^2.1.0");
        assertThat(result.alias).isEqualTo("@acme/private");
    }

    @Test
    void resolveFromNamedRegistryKeepsTheScopedNameUnderAnotherAlias() {
        FixtureRegistry registry = new FixtureRegistry().serve(GH, "@acme/private", "gh-acme-private");
        NpmResolver.ResolveResult result = resolver(registry, null).resolveFromNamedRegistry(
          wanted("my-private", "gh:@acme/private@^1.0.0"), calcSpecifier(true), null);
        assertThat(result).isNotNull();
        assertThat(result.id).isEqualTo("@acme/private@1.0.0");
        assertThat(result.normalizedBareSpecifier).isEqualTo("gh:@acme/private@^1.0.0");
        assertThat(result.alias).isEqualTo("@acme/private");
    }

    @Test
    void resolveFromNamedRegistryAsksTheNamedRegistry() {
        FixtureRegistry registry = new FixtureRegistry().serve(GH, "@acme/private", "gh-acme-private");
        NpmResolver.ResolveResult result = resolver(registry, null).resolveFromNamedRegistry(
          wanted("@acme/private", "gh:2.0.0"), calcSpecifier(false), null);
        assertThat(registry.asked).containsExactly(GH + "@acme/private");
        assertThat(result).isNotNull();
        assertThat(result.id).isEqualTo("@acme/private@2.0.0");
    }

    @Test
    void resolveFromAUserDefinedNamedRegistry() {
        FixtureRegistry registry = new FixtureRegistry().serve(ENTERPRISE, "@acme/private", "gh-acme-private");
        NpmResolver.ResolveResult result = resolver(registry, Collections.singletonMap("work", ENTERPRISE))
          .resolveFromNamedRegistry(wanted("@acme/private", "work:^2.0.0"), calcSpecifier(true), null);
        assertThat(result).isNotNull();
        assertThat(result.resolvedVia).isEqualTo("named-registry");
        assertThat(result.id).isEqualTo("@acme/private@2.1.0");
        assertThat(result.normalizedBareSpecifier).isEqualTo("work:^2.1.0");
    }

    @Test
    void userConfigOverridesTheBuiltInGhAlias() {
        FixtureRegistry registry = new FixtureRegistry().serve(ENTERPRISE, "@acme/private", "gh-acme-private");
        NpmResolver.ResolveResult result = resolver(registry, Collections.singletonMap("gh", ENTERPRISE))
          .resolveFromNamedRegistry(wanted("@acme/private", "gh:^2.0.0"), calcSpecifier(false), null);
        assertThat(result).isNotNull();
        assertThat(result.id).isEqualTo("@acme/private@2.1.0");
        assertThat(registry.asked).containsExactly(ENTERPRISE + "@acme/private");
    }

    @Test
    void aMalformedUserDefinedRegistryUrlIsRejected() {
        for (String url : new String[]{"npm.work.example.com", "ftp://npm.work.example.com/"}) {
            assertThatThrownBy(() -> NpmResolver.mergeNamedRegistries(Collections.singletonMap("work", url)))
              .isInstanceOfSatisfying(PnpmError.class,
                e -> assertThat(e.getCode()).isEqualTo("ERR_PNPM_INVALID_NAMED_REGISTRY_URL"));
        }
    }

    @Test
    void resolveFromNamedRegistryLeavesOtherSpecifiers() {
        NpmResolver resolver = resolver(new FixtureRegistry(), null);
        for (String spec : new String[]{"^1.0.0", "npm:@acme/private@1.0.0", "jsr:@acme/private", "work:^1.0.0"}) {
            assertThat(resolver.resolveFromNamedRegistry(wanted("@acme/private", spec), calcSpecifier(false), null))
              .as(spec).isNull();
        }
        for (String spec : new String[]{"github:owner/repo", "github:owner/repo#main", "github:@acme/foo"}) {
            assertThat(resolver.resolveFromNamedRegistry(wanted(null, spec), calcSpecifier(false), null)).as(spec)
              .isNull();
        }
        assertThat(resolver.resolveFromNamedRegistry(wanted(null, "gh:2.0.0"), calcSpecifier(false), null)).isNull();
    }

    @Test
    void resolveFromNamedRegistryRejectsAnInvalidScopedName() {
        NpmResolver resolver = resolver(new FixtureRegistry(), null);
        for (String spec : new String[]{"gh:@acme", "gh:@acme@2.0.0"}) {
            assertThatThrownBy(() -> resolver.resolveFromNamedRegistry(wanted(null, spec), calcSpecifier(false), null))
              .isInstanceOfSatisfying(PnpmError.class,
                e -> assertThat(e.getCode()).isEqualTo("ERR_PNPM_INVALID_NAMED_REGISTRY_PACKAGE_NAME"));
        }
    }

    @Test
    void npmVersionSelectorFallsBackToTheOuterAlias() {
        RegistryPackageSpec range = PickPackageFromMeta.parseBareSpecifier("npm:^1.0.0", "is-positive", "latest", NPM);
        assertThat(range).isNotNull();
        assertThat(range.getName()).isEqualTo("is-positive");
        assertThat(range.getType()).isEqualTo("range");
        RegistryPackageSpec version = PickPackageFromMeta.parseBareSpecifier("npm:1.0.0", "@acme/foo", "latest", NPM);
        assertThat(version).isEqualTo(new RegistryPackageSpec("version", "@acme/foo", "1.0.0", null));
    }

    @Test
    void namedRegistryParserLeavesOtherSpecifiers() {
        for (String spec : new String[]{"^1.0.0", "1.0.0", "latest", "npm:foo", "npm:@foo/bar", "jsr:@foo/bar",
          "catalog:", "workspace:*", "github:owner/repo", "github:owner/repo#main", "github:@acme/foo"}) {
            assertThat(named(spec, GH_ALIASES, null)).as(spec).isNull();
        }
        assertThat(named("gh:^1.0.0", GH_ALIASES, null)).isNull();
        assertThat(named("work:@acme/foo", GH_ALIASES, null)).isNull();
    }

    @Test
    void namedRegistryVersionSelectorWithAScopedAlias() {
        assertThat(named("gh:^1.0.0", GH_ALIASES, "@acme/foo")).isEqualTo(spec("range", "@acme/foo", ">=1.0.0 <2.0.0-0",
          "gh"));
        assertThat(named("gh:1.0.0", GH_ALIASES, "@acme/foo")).isEqualTo(spec("version", "@acme/foo", "1.0.0", "gh"));
        assertThat(named("gh:latest", GH_ALIASES, "@acme/foo")).isEqualTo(spec("tag", "@acme/foo", "latest", "gh"));
    }

    @Test
    void namedRegistryScopedNameWithAndWithoutSelector() {
        assertThat(named("gh:@acme/foo", GH_ALIASES, null)).isEqualTo(spec("tag", "@acme/foo", "latest", "gh"));
        assertThat(named("gh:@acme/foo@^1.0.0", GH_ALIASES, null))
          .isEqualTo(spec("range", "@acme/foo", ">=1.0.0 <2.0.0-0", "gh"));
        assertThat(named("gh:@acme/foo@1.0.0", GH_ALIASES, null))
          .isEqualTo(spec("version", "@acme/foo", "1.0.0", "gh"));
        assertThat(named("gh:@acme/foo@beta", GH_ALIASES, null)).isEqualTo(spec("tag", "@acme/foo", "beta", "gh"));
    }

    @Test
    void namedRegistryScopeWithoutName() {
        for (String spec : new String[]{"gh:@acme@^1.0.0", "gh:@acme", "gh:@acme/"}) {
            assertThatThrownBy(() -> named(spec, GH_ALIASES, null))
              .isInstanceOfSatisfying(PnpmError.class,
                e -> assertThat(e.getCode()).isEqualTo("ERR_PNPM_INVALID_NAMED_REGISTRY_PACKAGE_NAME"));
        }
        assertThatThrownBy(() -> named("work:@acme", Collections.singleton("work"), null))
          .hasMessageContaining("'work:'");
    }

    @Test
    void namedRegistryFallsBackToTheDependencyAlias() {
        NamedRegistryPackageSpec scoped = named("gh:^1.0.0", GH_ALIASES, "@acme/foo");
        assertThat(scoped).isNotNull();
        assertThat(scoped.getSpec().getName()).isEqualTo("@acme/foo");
        NamedRegistryPackageSpec unscoped = named("work:^4.0.0", Collections.singleton("work"), "lodash");
        assertThat(unscoped).isNotNull();
        assertThat(unscoped.getSpec().getName()).isEqualTo("lodash");
        assertThat(unscoped.getRegistryName()).isEqualTo("work");
    }

    @Test
    void namedRegistryUnscopedNameInTheBody() {
        Set<String> work = Collections.singleton("work");
        assertThat(named("work:lodash@^4.0.0", work, null)).isEqualTo(spec("range", "lodash", ">=4.0.0 <5.0.0-0",
          "work"));
        assertThat(named("work:lodash", work, null)).isEqualTo(spec("tag", "lodash", "latest", "work"));
    }

    @Test
    void namedRegistryReportsTheAliasItMatched() {
        assertThat(named("work:@acme/foo@^1.0.0", new LinkedHashSet<>(Arrays.asList("gh", "work")), null))
          .isEqualTo(spec("range", "@acme/foo", ">=1.0.0 <2.0.0-0", "work"));
    }

    @Test
    void jsrParserLeavesOtherSpecifiers() {
        for (String spec : new String[]{"^1.0.0", "1.0.0", "latest", "npm:foo", "npm:@foo/bar", "npm:@jsr/foo__bar",
          "catalog:", "workspace:*"}) {
            assertThat(JsrSpecifierParser.parseJsrSpecifier(spec, null)).as(spec).isNull();
        }
    }

    @Test
    void jsrVersionSelectorUnderAnAlias() {
        assertThat(JsrSpecifierParser.parseJsrSpecifier("jsr:^1.0.0", "@foo/bar"))
          .isEqualTo(new JsrSpecifierParser.JsrSpec("@foo/bar", "@jsr/foo__bar", "^1.0.0"));
        assertThat(JsrSpecifierParser.parseJsrSpecifier("jsr:1.0.0", "@foo/bar"))
          .isEqualTo(new JsrSpecifierParser.JsrSpec("@foo/bar", "@jsr/foo__bar", "1.0.0"));
        assertThat(JsrSpecifierParser.parseJsrSpecifier("jsr:latest", "@foo/bar"))
          .isEqualTo(new JsrSpecifierParser.JsrSpec("@foo/bar", "@jsr/foo__bar", "latest"));
    }

    @Test
    void jsrScopeAndName() {
        assertThat(JsrSpecifierParser.parseJsrSpecifier("jsr:@foo/bar", null))
          .isEqualTo(new JsrSpecifierParser.JsrSpec("@foo/bar", "@jsr/foo__bar", null));
        for (String selector : new String[]{"^1.0.0", "1.0.0", "latest"}) {
            assertThat(JsrSpecifierParser.parseJsrSpecifier("jsr:@foo/bar@" + selector, null))
              .isEqualTo(new JsrSpecifierParser.JsrSpec("@foo/bar", "@jsr/foo__bar", selector));
        }
    }

    @Test
    void jsrNameWithoutScope() {
        assertThatThrownBy(() -> JsrSpecifierParser.parseJsrSpecifier("jsr:foo@^1.0.0", null))
          .isInstanceOfSatisfying(PnpmError.class,
            e -> assertThat(e.getCode()).isEqualTo("ERR_PNPM_MISSING_JSR_PACKAGE_SCOPE"));
    }

    @Test
    void jsrScopeWithoutName() {
        for (String spec : new String[]{"jsr:@foo@^1.0.0", "jsr:@foo"}) {
            assertThatThrownBy(() -> JsrSpecifierParser.parseJsrSpecifier(spec, null))
              .isInstanceOfSatisfying(PnpmError.class,
                e -> assertThat(e.getCode()).isEqualTo("ERR_PNPM_INVALID_JSR_PACKAGE_NAME"));
        }
    }

    private static @Nullable NamedRegistryPackageSpec named(String spec, Set<String> aliases, @Nullable String alias) {
        return PickPackageFromMeta.parseNamedRegistrySpecifierToRegistryPackageSpec(spec, aliases, alias, "latest");
    }

    private static NamedRegistryPackageSpec spec(String type, String name, String fetchSpec, String registryName) {
        return new NamedRegistryPackageSpec(new RegistryPackageSpec(type, name, fetchSpec, null), registryName);
    }
}
