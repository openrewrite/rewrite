/*
 * Copyright 2026 the original author or authors.
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
package org.openrewrite.maven.internal.engine;

import org.jspecify.annotations.Nullable;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.openrewrite.InMemoryExecutionContext;
import org.openrewrite.maven.MavenExecutionContextView;
import org.openrewrite.maven.MavenSettings;
import org.openrewrite.maven.engine.shaded.org.apache.maven.model.Profile;
import org.openrewrite.maven.engine.shaded.org.eclipse.aether.ConfigurationProperties;
import org.openrewrite.maven.engine.shaded.org.eclipse.aether.DefaultRepositorySystemSession;
import org.openrewrite.maven.engine.shaded.org.eclipse.aether.RepositorySystemSession;
import org.openrewrite.maven.engine.shaded.org.eclipse.aether.repository.Authentication;
import org.openrewrite.maven.engine.shaded.org.eclipse.aether.repository.AuthenticationContext;
import org.openrewrite.maven.engine.shaded.org.eclipse.aether.repository.AuthenticationSelector;
import org.openrewrite.maven.engine.shaded.org.eclipse.aether.repository.RemoteRepository;
import org.openrewrite.maven.internal.RawRepositories;
import org.openrewrite.maven.tree.MavenRepository;
import org.openrewrite.maven.tree.MavenRepositoryMirror;
import org.openrewrite.maven.tree.ProfileActivation;

import java.util.*;

import static java.util.Collections.*;
import static org.junit.jupiter.api.Assertions.*;

class SettingsBridgeTest {

    private static MavenExecutionContextView ctx() {
        return MavenExecutionContextView.view(new InMemoryExecutionContext());
    }

    private static MavenRepository repo(String id, String uri) {
        return new MavenRepository(id, uri, null, null, false, null, null, null, null);
    }

    private static List<String> ids(List<MavenRepository> repos) {
        List<String> ids = new ArrayList<>();
        for (MavenRepository repo : repos) {
            ids.add(repo.getId());
        }
        return ids;
    }

    @Test
    void requestRepositoryOrdering_localSettingsPomCentral() {
        MavenExecutionContextView ctx = ctx();
        ctx.setLocalRepository(MavenRepository.MAVEN_LOCAL_DEFAULT);
        ctx.setRepositories(singletonList(repo("ctxRepo", "https://ctx.example/repo")));

        List<MavenRepository> result = new SettingsBridge(ctx, null, null)
                .requestRepositories(singletonList(repo("pomRepo", "https://pom.example/repo")), true, true, emptyMap());

        // local -> settings/ctx -> passed pom repos -> central
        assertEquals(Arrays.asList("local", "ctxRepo", "pomRepo", "central"), ids(result));
    }

    @ParameterizedTest
    @CsvSource({"true,true", "true,false", "false,true", "false,false"})
    void triStateAddLocalAndCentral(boolean addLocal, boolean addCentral) {
        MavenExecutionContextView ctx = ctx();
        ctx.setLocalRepository(MavenRepository.MAVEN_LOCAL_DEFAULT);

        List<MavenRepository> result = new SettingsBridge(ctx, null, null)
                .requestRepositories(singletonList(repo("pomRepo", "https://pom.example/repo")), addLocal, addCentral, emptyMap());

        assertEquals(addLocal, ids(result).contains("local"));
        assertEquals(addCentral, ids(result).contains("central"));
        assertTrue(ids(result).contains("pomRepo"));
    }

    @Test
    void addLocalCentralTriStateDefaults() {
        // The parse-context defaults: absent knob means include (mirrors the project-poms downloader constructor).
        assertTrue(SettingsBridge.addLocalRepository(ctx()));
        assertTrue(SettingsBridge.addCentralRepository(ctx()));

        MavenExecutionContextView off = ctx();
        off.setAddLocalRepository(false);
        off.setAddCentralRepository(false);
        assertFalse(SettingsBridge.addLocalRepository(off));
        assertFalse(SettingsBridge.addCentralRepository(off));
    }

    @Test
    void ctxInjectedReposSurviveWhenNoSettings() {
        MavenExecutionContextView ctx = ctx();
        ctx.setRepositories(Arrays.asList(repo("x1", "https://x1"), repo("s1", "https://s1")));

        List<MavenRepository> result = new SettingsBridge(ctx, null, null)
                .requestRepositories(emptyList(), false, false, emptyMap());

        assertEquals(Arrays.asList("x1", "s1"), ids(result));
    }

    @Test
    void settingsPresenceDropsCtxInjectedReposWithoutMatchingSettingsId() {
        // The documented seam trap: when a settings object is present, getRepositories returns only the settings'
        // active repositories (enriched by same-id ctx repos), so an unrelated ctx-injected repo is dropped.
        MavenSettings settings = settingsWithActiveRepository("s1", "https://s1");

        MavenExecutionContextView ctx = ctx();
        ctx.setRepositories(Arrays.asList(
                repo("x1", "https://x1"),
                new MavenRepository("s1", "https://s1", null, null, true, "u", "p", null, null)));

        List<MavenRepository> result = new SettingsBridge(ctx, settings, emptyList())
                .requestRepositories(emptyList(), false, false, emptyMap());

        assertEquals(singletonList("s1"), ids(result), "ctx-injected x1 dropped; only settings' s1 survives");
        assertTrue(result.get(0).isKnownToExist(), "same-id ctx repo enriches the settings repo");
    }

    @Test
    void uriPropertyInterpolationInRequestList() {
        MavenExecutionContextView ctx = ctx();
        ctx.setRepositories(singletonList(repo("nexus", "${NEXUS}/repo")));

        List<MavenRepository> result = new SettingsBridge(ctx, null, null)
                .requestRepositories(emptyList(), false, false, singletonMap("NEXUS", "https://nexus.example"));

        assertEquals("https://nexus.example/repo", result.get(0).getUri());
    }

    @Test
    void mirrorAppliedInRequestListButNeverToLocal() {
        MavenExecutionContextView ctx = ctx();
        ctx.setLocalRepository(MavenRepository.MAVEN_LOCAL_DEFAULT);
        ctx.setMirrors(singletonList(new MavenRepositoryMirror("mirror", "https://mirror.example", "*", null, null, null)));

        List<MavenRepository> result = new SettingsBridge(ctx, null, null)
                .requestRepositories(emptyList(), true, true, emptyMap());

        MavenRepository local = result.stream().filter(r -> "local".equals(r.getId())).findFirst().orElseThrow(AssertionError::new);
        assertEquals(MavenRepository.MAVEN_LOCAL_DEFAULT.getUri(), local.getUri(), "mirrors never apply to local");
        assertTrue(result.stream().anyMatch(r -> "mirror".equals(r.getId()) && "https://mirror.example".equals(r.getUri())),
                "central mirrored to the mirror repo");
        assertTrue(result.stream().noneMatch(r -> "central".equals(r.getId())), "central replaced by its mirror");
    }

    @Test
    void mirrorSelectorAgreesWithRequestListPathAcrossMatrix() {
        List<MavenRepositoryMirror> mirrors = Arrays.asList(
                new MavenRepositoryMirror("centralMirror", "https://cm.example", "central", null, null, null),
                new MavenRepositoryMirror("externalMirror", "https://ext.example", "external:*", null, null, null));

        MavenExecutionContextView ctx = ctx();
        ctx.setMirrors(mirrors);
        SettingsBridge bridge = new SettingsBridge(ctx, null, null);

        List<MavenRepository> matrix = Arrays.asList(
                MavenRepository.MAVEN_CENTRAL,
                repo("internal", "https://nexus.internal/repo"),
                repo("localhostRepo", "http://localhost:8080/repo"),
                repo("fileRepo", "file:///tmp/repo"));

        for (MavenRepository repo : matrix) {
            MavenRepository viaRequestList = MavenRepositoryMirror.apply(mirrors, repo);
            RemoteRepository viaSelector = bridge.mirrorSelector().getMirror(
                    new RemoteRepository.Builder(repo.getId(), "default", repo.getUri()).build());

            if (viaRequestList == repo) {
                assertNull(viaSelector, "no mirror on the request list but the selector produced one for " + repo.getId());
            } else {
                assertNotNull(viaSelector, "request list mirrored " + repo.getId() + " but the selector did not");
                assertEquals(viaRequestList.getId(), viaSelector.getId(), "mirror id disagreement for " + repo.getId());
                assertEquals(viaRequestList.getUri(), viaSelector.getUrl(), "mirror url disagreement for " + repo.getId());
            }
        }
    }

    @Test
    void credentialsFromSettingsServersFlowToRequestListAndAuthenticationSelector() {
        MavenSettings settings = settingsWithServer("foo", "user", "secret", null);

        // (a) request list: decrypted credentials attach to the same-id repository.
        List<MavenRepository> repos = new SettingsBridge(ctx(), settings, emptyList())
                .requestRepositories(singletonList(repo("foo", "https://foo.example/repo")), false, false, emptyMap());
        assertEquals("user", repos.get(0).getUsername());
        assertEquals("secret", repos.get(0).getPassword());

        // (b) session AuthenticationSelector: same credentials, keyed by server id.
        AuthenticationSelector selector = new SettingsBridge(ctx(), settings, emptyList()).authenticationSelector();
        assertNull(selector.getAuthentication(new RemoteRepository.Builder("bar", "default", "https://bar").build()),
                "no authentication for an unknown id");

        RemoteRepository fooRepo = new RemoteRepository.Builder("foo", "default", "https://foo.example/repo").build();
        Authentication auth = selector.getAuthentication(fooRepo);
        assertNotNull(auth);
        RemoteRepository authed = new RemoteRepository.Builder(fooRepo).setAuthentication(auth).build();
        RepositorySystemSession session = new DefaultRepositorySystemSession(r -> false);
        AuthenticationContext authCtx = AuthenticationContext.forRepository(session, authed);
        try {
            assertEquals("user", authCtx.get(AuthenticationContext.USERNAME));
            assertEquals("secret", authCtx.get(AuthenticationContext.PASSWORD));
        } finally {
            AuthenticationContext.close(authCtx);
        }
    }

    @Test
    void perServerHeadersAndTimeoutsAsSessionConfigProperties() {
        MavenSettings.ServerConfiguration configuration = new MavenSettings.ServerConfiguration(
                singletonList(new MavenSettings.HttpHeader("X-Auth", "token")), 5000L);
        MavenSettings settings = settingsWithServer("nexus", "u", "p", configuration);

        Map<String, Object> props = new SettingsBridge(ctx(), settings, emptyList()).serverConfigProperties();

        @SuppressWarnings("unchecked")
        Map<String, String> headers = (Map<String, String>) props.get(ConfigurationProperties.HTTP_HEADERS + ".nexus");
        assertEquals("token", headers.get("X-Auth"));
        assertEquals(5000, props.get(ConfigurationProperties.CONNECT_TIMEOUT + ".nexus"));
        assertEquals(5000, props.get(ConfigurationProperties.REQUEST_TIMEOUT + ".nexus"));
    }

    @Test
    void effectiveSettingsCarriesExternalProfilesActiveIdsAndUserProperties() {
        RawRepositories.Repository rawRepo = new RawRepositories.Repository("r1", "https://r1",
                new RawRepositories.ArtifactPolicy("true"), new RawRepositories.ArtifactPolicy("false"));
        RawRepositories rawRepos = new RawRepositories();
        rawRepos.setRepositories(singletonList(rawRepo));
        MavenSettings.Profile profile = new MavenSettings.Profile("p1",
                new ProfileActivation(true, null, null), rawRepos);
        MavenSettings settings = new MavenSettings(null,
                new MavenSettings.Profiles(singletonList(profile)),
                new MavenSettings.ActiveProfiles(singletonList("p1")), null, null);

        EffectiveSettings effective = new SettingsBridge(ctx(), settings, singletonList("cliProfile"))
                .effectiveSettings(singletonMap("k", "v"));

        assertEquals(1, effective.getExternalProfiles().size());
        Profile modelProfile = effective.getExternalProfiles().get(0);
        assertEquals("p1", modelProfile.getId());
        assertTrue(modelProfile.getActivation().isActiveByDefault());
        assertEquals(1, modelProfile.getRepositories().size());
        assertEquals("r1", modelProfile.getRepositories().get(0).getId());
        assertEquals("https://r1", modelProfile.getRepositories().get(0).getUrl());

        assertEquals(Arrays.asList("p1", "cliProfile"), effective.getActiveProfiles());
        assertEquals(singletonMap("k", "v"), effective.getUserProperties());
    }

    private static MavenSettings settingsWithActiveRepository(String id, String url) {
        RawRepositories rawRepos = new RawRepositories();
        rawRepos.setRepositories(singletonList(new RawRepositories.Repository(id, url, null, null)));
        MavenSettings.Profile profile = new MavenSettings.Profile("p1",
                new ProfileActivation(true, null, null), rawRepos);
        return new MavenSettings(null, new MavenSettings.Profiles(singletonList(profile)), null, null, null);
    }

    private static MavenSettings settingsWithServer(String id, String username, String password,
                                                    MavenSettings.@Nullable ServerConfiguration configuration) {
        MavenSettings.Server server = new MavenSettings.Server(id, username, password, configuration);
        return new MavenSettings(null, null, null, null, new MavenSettings.Servers(singletonList(server)));
    }
}
