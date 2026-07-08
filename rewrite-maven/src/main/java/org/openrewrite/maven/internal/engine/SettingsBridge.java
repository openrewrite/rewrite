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
import org.openrewrite.maven.MavenExecutionContextView;
import org.openrewrite.maven.MavenSettings;
import org.openrewrite.maven.engine.shaded.org.apache.maven.model.Activation;
import org.openrewrite.maven.engine.shaded.org.apache.maven.model.ActivationProperty;
import org.openrewrite.maven.engine.shaded.org.apache.maven.model.Profile;
import org.openrewrite.maven.engine.shaded.org.apache.maven.model.Repository;
import org.openrewrite.maven.engine.shaded.org.apache.maven.model.RepositoryPolicy;
import org.openrewrite.maven.engine.shaded.org.eclipse.aether.ConfigurationProperties;
import org.openrewrite.maven.engine.shaded.org.eclipse.aether.repository.AuthenticationSelector;
import org.openrewrite.maven.engine.shaded.org.eclipse.aether.repository.MirrorSelector;
import org.openrewrite.maven.engine.shaded.org.eclipse.aether.repository.RemoteRepository;
import org.openrewrite.maven.engine.shaded.org.eclipse.aether.util.repository.AuthenticationBuilder;
import org.openrewrite.maven.engine.shaded.org.eclipse.aether.util.repository.DefaultAuthenticationSelector;
import org.openrewrite.maven.internal.RawRepositories;
import org.openrewrite.maven.tree.*;

import java.util.*;

/**
 * Pure translation (no I/O) of {@link MavenExecutionContextView} + its {@link MavenSettings} into the inputs the engine
 * needs: the ordered request-repository list, the session-level {@link MirrorSelector}/{@link AuthenticationSelector}
 * for descriptor-discovered repositories, per-server transport config properties, and {@link EffectiveSettings}.
 * <p>
 * Ordering and the settings-presence seam trap mirror {@code MavenPomDownloader.distinctNormalizedRepositories} /
 * {@code MavenExecutionContextView.getRepositories} exactly. Mirrors are applied on both the request list (a) and the
 * session {@link #mirrorSelector()} (b) through the same {@link MavenRepositoryMirror} semantics, so both paths agree.
 * There is deliberately no {@code ProxySelector}: settings {@code <proxies>} stay the sender's concern (DESIGN §0).
 */
public class SettingsBridge {

    private final MavenExecutionContextView ctx;
    private final @Nullable MavenSettings settings;
    private final @Nullable List<String> activeProfiles;
    private final Collection<MavenRepositoryMirror> mirrors;
    private final Collection<MavenRepositoryCredentials> credentials;

    public SettingsBridge(MavenExecutionContextView ctx, @Nullable MavenSettings settings,
                          @Nullable List<String> activeProfiles) {
        this.ctx = ctx;
        this.settings = settings;
        this.activeProfiles = activeProfiles;
        this.mirrors = ctx.getMirrors(settings);
        this.credentials = ctx.getCredentials(settings);
    }

    /** Default for the Maven-parsing path (project-poms constructor): absent knob means include. */
    public static boolean addLocalRepository(MavenExecutionContextView ctx) {
        return !Boolean.FALSE.equals(ctx.getAddLocalRepository());
    }

    /** Default for the Maven-parsing path (project-poms constructor): absent knob means include. */
    public static boolean addCentralRepository(MavenExecutionContextView ctx) {
        return !Boolean.FALSE.equals(ctx.getAddCentralRepository());
    }

    /**
     * (a) The ordered request-repository list: local &rarr; settings/ctx &rarr; passed pom repos &rarr; central,
     * id-keyed with later same-id entries overwriting earlier, then each interpolated, mirror-applied, and
     * credential-attached, then de-duplicated by resulting id (first wins). When {@code settings != null} the
     * settings-presence seam trap applies: {@code ctx.getRepositories} returns only the settings' active repositories
     * (enriched by same-id ctx repos), so ctx-injected repos without a matching settings id are dropped.
     */
    public List<MavenRepository> requestRepositories(List<MavenRepository> pomRepositories,
                                                     boolean addLocalRepository,
                                                     boolean addCentralRepository,
                                                     Map<String, String> properties) {
        Map<@Nullable String, MavenRepository> byId = new LinkedHashMap<>();
        if (addLocalRepository) {
            byId.put("local", ctx.getLocalRepository());
        }
        for (MavenRepository repo : ctx.getRepositories(settings, activeProfiles)) {
            byId.put(repo.getId(), repo);
        }
        for (MavenRepository repo : pomRepositories) {
            byId.put(repo.getId(), repo);
        }
        if (addCentralRepository && !byId.containsKey("central")) {
            byId.put("central", MavenRepository.MAVEN_CENTRAL);
        }

        List<MavenRepository> result = new ArrayList<>();
        Set<@Nullable String> seen = new HashSet<>();
        for (MavenRepository repo : byId.values()) {
            MavenRepository normalized = normalize(repo, properties);
            if (seen.add(normalized.getId())) {
                result.add(normalized);
            }
        }
        return result;
    }

    private MavenRepository normalize(MavenRepository repo, Map<String, String> properties) {
        MavenRepository normalized = repo;
        if (normalized.getUri().contains("${")) {
            normalized = normalized.withUri(
                    ResolvedPom.placeholderHelper.replacePlaceholders(normalized.getUri(), properties::get));
        }
        normalized = MavenRepositoryMirror.apply(mirrors, normalized);
        normalized = MavenRepositoryCredentials.apply(credentials, normalized);
        return normalized;
    }

    /**
     * (b) The session mirror selector for descriptor-discovered repositories. It delegates to the same
     * {@link MavenRepositoryMirror} semantics as {@link #requestRepositories}, so a repository the request list would
     * mirror is mirrored identically here — the two paths agree by construction. Returns {@code null} (aether's
     * "no mirror" contract) when no mirror matches.
     */
    public MirrorSelector mirrorSelector() {
        return this::applyMirror;
    }

    private @Nullable RemoteRepository applyMirror(RemoteRepository repository) {
        MavenRepository as = new MavenRepository(repository.getId(), repository.getUrl(),
                null, null, false, null, null, null, null);
        MavenRepository mirrored = MavenRepositoryMirror.apply(mirrors, as);
        if (mirrored == as) {
            return null;
        }
        return new RemoteRepository.Builder(repository)
                .setId(mirrored.getId())
                .setUrl(mirrored.getUri())
                .build();
    }

    /**
     * (b) The session authentication selector, populated from settings servers (whose passwords are already decrypted
     * by {@code MavenSettings.parse} via {@code MavenSecuritySettings}) plus any ctx-injected credentials, keyed by
     * server/repository id.
     */
    public AuthenticationSelector authenticationSelector() {
        DefaultAuthenticationSelector selector = new DefaultAuthenticationSelector();
        for (MavenRepositoryCredentials credential : credentials) {
            selector.add(credential.getId(), new AuthenticationBuilder()
                    .addUsername(credential.getUsername())
                    .addPassword(credential.getPassword())
                    .build());
        }
        return selector;
    }

    /**
     * (c) Per-server transport config properties keyed by server id, in the exact key scheme
     * {@code HttpSenderTransporterFactory} reads: {@code aether.transport.http.headers.<id>} (a header map),
     * {@code aether.transport.http.connectTimeout.<id>} and {@code aether.transport.http.requestTimeout.<id>}
     * (millisecond ints). A server {@code <configuration><timeout>} sets both connect and read timeouts, matching
     * {@code MavenPomDownloader.applyAuthenticationAndTimeoutToRequest}.
     */
    public Map<String, Object> serverConfigProperties() {
        Map<String, Object> properties = new HashMap<>();
        if (settings == null || settings.getServers() == null) {
            return properties;
        }
        for (MavenSettings.Server server : settings.getServers().getServers()) {
            MavenSettings.ServerConfiguration configuration = server.getConfiguration();
            if (configuration == null) {
                continue;
            }
            String id = server.getId();
            List<MavenSettings.HttpHeader> httpHeaders = configuration.getHttpHeaders();
            if (httpHeaders != null && !httpHeaders.isEmpty()) {
                Map<String, String> headers = new LinkedHashMap<>();
                for (MavenSettings.HttpHeader header : httpHeaders) {
                    headers.put(header.getName(), header.getValue());
                }
                properties.put(ConfigurationProperties.HTTP_HEADERS + "." + id, headers);
            }
            if (configuration.getTimeout() != null) {
                int timeout = configuration.getTimeout().intValue();
                properties.put(ConfigurationProperties.CONNECT_TIMEOUT + "." + id, timeout);
                properties.put(ConfigurationProperties.REQUEST_TIMEOUT + "." + id, timeout);
            }
        }
        return properties;
    }

    /**
     * (d) The {@link EffectiveSettings} for slice B's {@code ModelBuildingRequest}: settings profiles as Maven model
     * profiles (Maven owns their activation), explicitly-active profile ids, and the given user properties.
     */
    public EffectiveSettings effectiveSettings(Map<String, String> userProperties) {
        List<Profile> externalProfiles = new ArrayList<>();
        List<String> activeProfileIds = new ArrayList<>();
        if (settings != null) {
            if (settings.getProfiles() != null) {
                for (MavenSettings.Profile profile : settings.getProfiles().getProfiles()) {
                    externalProfiles.add(toModelProfile(profile));
                }
            }
            if (settings.getActiveProfiles() != null) {
                activeProfileIds.addAll(settings.getActiveProfiles().getActiveProfiles());
            }
        }
        if (activeProfiles != null) {
            for (String id : activeProfiles) {
                if (!activeProfileIds.contains(id)) {
                    activeProfileIds.add(id);
                }
            }
        }
        return new EffectiveSettings(externalProfiles, activeProfileIds, new LinkedHashMap<>(userProperties));
    }

    private static Profile toModelProfile(MavenSettings.Profile profile) {
        Profile model = new Profile();
        model.setId(profile.getId());
        model.setSource("settings.xml");
        ProfileActivation activation = profile.getActivation();
        if (activation != null) {
            Activation modelActivation = new Activation();
            if (activation.getActiveByDefault() != null) {
                modelActivation.setActiveByDefault(activation.getActiveByDefault());
            }
            modelActivation.setJdk(activation.getJdk());
            ProfileActivation.Property property = activation.getProperty();
            if (property != null) {
                ActivationProperty modelProperty = new ActivationProperty();
                modelProperty.setName(property.getName());
                modelProperty.setValue(property.getValue());
                modelActivation.setProperty(modelProperty);
            }
            model.setActivation(modelActivation);
        }
        if (profile.getRepositories() != null) {
            for (RawRepositories.Repository repository : profile.getRepositories().getRepositories()) {
                model.addRepository(toModelRepository(repository));
            }
        }
        return model;
    }

    private static Repository toModelRepository(RawRepositories.Repository repository) {
        Repository model = new Repository();
        model.setId(repository.getId());
        model.setUrl(repository.getUrl());
        model.setReleases(toModelPolicy(repository.getReleases()));
        model.setSnapshots(toModelPolicy(repository.getSnapshots()));
        return model;
    }

    private static @Nullable RepositoryPolicy toModelPolicy(RawRepositories.@Nullable ArtifactPolicy policy) {
        if (policy == null) {
            return null;
        }
        RepositoryPolicy model = new RepositoryPolicy();
        if (policy.getEnabled() != null) {
            model.setEnabled(policy.getEnabled());
        }
        return model;
    }
}
