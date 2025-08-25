/*
 * Copyright 2020 the original author or authors.
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
package org.openrewrite.maven;

import org.jspecify.annotations.Nullable;
import org.openrewrite.DelegatingExecutionContext;
import org.openrewrite.ExecutionContext;
import org.openrewrite.maven.cache.InMemoryMavenPomCache;
import org.openrewrite.maven.cache.MavenPomCache;
import org.openrewrite.maven.internal.MavenParsingException;
import org.openrewrite.maven.tree.*;

import java.time.Duration;
import java.util.*;
import java.util.stream.Stream;

import static java.util.Collections.emptyList;
import static java.util.stream.Collectors.toList;
import static java.util.stream.Collectors.toMap;
import static org.openrewrite.maven.tree.MavenRepository.MAVEN_LOCAL_DEFAULT;

@SuppressWarnings({"unused", "UnusedReturnValue"})
public class MavenExecutionContextView extends DelegatingExecutionContext {
    private static final String MAVEN_SETTINGS = "org.openrewrite.maven.settings";
    private static final String MAVEN_ACTIVE_PROFILES = "org.openrewrite.maven.activeProfiles";
    private static final String MAVEN_MIRRORS = "org.openrewrite.maven.mirrors";
    private static final String MAVEN_CREDENTIALS = "org.openrewrite.maven.auth";
    private static final String MAVEN_LOCAL_REPOSITORY = "org.openrewrite.maven.localRepo";
    private static final String MAVEN_ADD_LOCAL_REPOSITORY = "org.openrewrite.maven.useLocalRepo";
    private static final String MAVEN_ADD_CENTRAL_REPOSITORY = "org.openrewrite.maven.useCentralRepo";
    private static final String MAVEN_REPOSITORIES = "org.openrewrite.maven.repos";
    private static final String MAVEN_PINNED_SNAPSHOT_VERSIONS = "org.openrewrite.maven.pinnedSnapshotVersions";
    private static final String MAVEN_POM_CACHE = "org.openrewrite.maven.pomCache";
    private static final String MAVEN_RESOLUTION_LISTENER = "org.openrewrite.maven.resolutionListener";
    private static final String MAVEN_RESOLUTION_TIME = "org.openrewrite.maven.resolutionTime";

    public MavenExecutionContextView(ExecutionContext delegate) {
        super(delegate);
    }

    public static MavenExecutionContextView view(ExecutionContext ctx) {
        if (ctx instanceof MavenExecutionContextView) {
            return (MavenExecutionContextView) ctx;
        }
        return new MavenExecutionContextView(ctx);
    }

    public MavenExecutionContextView recordResolutionTime(Duration time) {
        //noinspection DataFlowIssue
        this.computeMessage(MAVEN_RESOLUTION_TIME, time.toMillis(), () -> 0L, Long::sum);
        return this;
    }

    public Duration getResolutionTime() {
        return Duration.ofMillis(getMessage(MAVEN_RESOLUTION_TIME, 0L));
    }

    public MavenExecutionContextView setResolutionListener(ResolutionEventListener listener) {
        putMessage(MAVEN_RESOLUTION_LISTENER, listener);
        return this;
    }

    public ResolutionEventListener getResolutionListener() {
        return getMessage(MAVEN_RESOLUTION_LISTENER, ResolutionEventListener.NOOP);
    }

    public MavenExecutionContextView setMirrors(@Nullable Collection<MavenRepositoryMirror> mirrors) {
        putMessage(MAVEN_MIRRORS, mirrors);
        return this;
    }

    public Collection<MavenRepositoryMirror> getMirrors() {
        return getMessage(MAVEN_MIRRORS, emptyList());
    }

    /**
     * Get mirrors set on this execution context, unless overridden by a supplied maven settings file.
     *
     * @param mavenSettings The maven settings defining mirrors to use, if any.
     * @return The mirrors to use for dependency resolution.
     */
    public Collection<MavenRepositoryMirror> getMirrors(@Nullable MavenSettings mavenSettings) {
        if (mavenSettings != null && !Objects.equals(mavenSettings, getSettings())) {
            return mapMirrors(mavenSettings);
        }
        return getMirrors();
    }

    public MavenExecutionContextView setCredentials(Collection<MavenRepositoryCredentials> credentials) {
        putMessage(MAVEN_CREDENTIALS, credentials);
        return this;
    }

    public Collection<MavenRepositoryCredentials> getCredentials() {
        return getMessage(MAVEN_CREDENTIALS, emptyList());
    }

    /**
     * Get credentials set on this execution context, unless overridden by a supplied maven settings file.
     *
     * @param mavenSettings The maven settings defining credentials (in its server configuration) to use, if any.
     * @return The credentials to use for dependency resolution.
     */
    public Collection<MavenRepositoryCredentials> getCredentials(@Nullable MavenSettings mavenSettings) {

        //Prefer any credentials defined in the mavenSettings passed to this method, but also consider any credentials
        //defined in the context as well.
        List<MavenRepositoryCredentials> credentials = new ArrayList<>();
        if (mavenSettings != null) {
            credentials.addAll(mapCredentials(mavenSettings));
        }
        credentials.addAll(getMessage(MAVEN_CREDENTIALS, emptyList()));
        return credentials;
    }

    public MavenExecutionContextView setPomCache(MavenPomCache pomCache) {
        putMessage(MAVEN_POM_CACHE, pomCache);
        return this;
    }

    public MavenPomCache getPomCache() {
        return (MavenPomCache) getMessages().computeIfAbsent(MAVEN_POM_CACHE, k -> new InMemoryMavenPomCache());
    }

    public MavenExecutionContextView setLocalRepository(MavenRepository localRepository) {
        putMessage(MAVEN_LOCAL_REPOSITORY, localRepository);
        return this;
    }

    public MavenRepository getLocalRepository() {
        MavenRepository configuredProperty = getMessage(MAVEN_LOCAL_REPOSITORY);
        if (configuredProperty != null) {
            return configuredProperty;
        }
        MavenSettings settings = getSettings();
        if (settings != null) {
            return settings.getMavenLocal();
        }
        return MAVEN_LOCAL_DEFAULT;
    }

    public MavenExecutionContextView setAddLocalRepository(boolean useLocalRepository) {
        putMessage(MAVEN_ADD_LOCAL_REPOSITORY, useLocalRepository);
        return this;
    }

    public @Nullable Boolean getAddLocalRepository() {
        return getMessage(MAVEN_ADD_LOCAL_REPOSITORY, null);
    }

    public MavenExecutionContextView setAddCentralRepository(boolean useCentralRepository) {
        putMessage(MAVEN_ADD_CENTRAL_REPOSITORY, useCentralRepository);
        return this;
    }

    public @Nullable Boolean getAddCentralRepository() {
        return getMessage(MAVEN_ADD_CENTRAL_REPOSITORY);
    }

    public MavenExecutionContextView setRepositories(List<MavenRepository> repositories) {
        putMessage(MAVEN_REPOSITORIES, repositories);
        return this;
    }

    public List<MavenRepository> getRepositories() {
        return getMessage(MAVEN_REPOSITORIES, emptyList());
    }

    /**
     * Get repositories set on this execution context, unless overridden by a supplied maven settings file.
     *
     * @param mavenSettings  The maven settings defining repositories to use, if any.
     * @param activeProfiles The active profiles to use, if any, with the accompanying maven settings.
     * @return The repositories to use for dependency resolution.
     */
    public List<MavenRepository> getRepositories(@Nullable MavenSettings mavenSettings,
                                                 @Nullable List<String> activeProfiles) {
        if (mavenSettings != null) {
            return mapRepositories(mavenSettings, activeProfiles == null ? emptyList() : activeProfiles);
        }
        return getMessage(MAVEN_REPOSITORIES, emptyList());
    }

    /**
     * Require dependency resolution that encounters a matching group:artifact:version coordinate to resolve to a
     * particular dated snapshot version, effectively making snapshot resolution deterministic.
     *
     * @param pinnedSnapshotVersions A set of group:artifact:version and the dated snapshot version to pin them to.
     */
    public MavenExecutionContextView setPinnedSnapshotVersions(Collection<ResolvedGroupArtifactVersion> pinnedSnapshotVersions) {
        putMessage(MAVEN_PINNED_SNAPSHOT_VERSIONS, pinnedSnapshotVersions);
        return this;
    }

    public Collection<ResolvedGroupArtifactVersion> getPinnedSnapshotVersions() {
        return getMessage(MAVEN_PINNED_SNAPSHOT_VERSIONS, emptyList());
    }

    public MavenExecutionContextView setActiveProfiles(List<String> activeProfiles) {
        putMessage(MAVEN_ACTIVE_PROFILES, activeProfiles);
        return this;
    }

    public List<String> getActiveProfiles() {
        return getMessage(MAVEN_ACTIVE_PROFILES, emptyList());
    }

    /**
     * Replaces the current Maven execution settings with the provided {@link MavenSettings}.
     * <p>
     * This method does <strong>not</strong> merge with or preserve any existing configuration.
     * All previously configured repositories, mirrors, credentials, active profiles,
     * and local repository settings will be discarded and replaced entirely with those
     * derived from the given {@code settings}.
     * </p>
     *
     * <p>If {@code settings} is {@code null}, this call is a no-op and the current
     * configuration remains unchanged.</p>
     *
     * @param settings        the Maven settings to apply; if {@code null}, no changes are made
     * @param activeProfiles  additional active profiles to consider; these are resolved
     *                        against the provided settings to determine the effective profiles
     * @return this execution context view with the updated Maven settings applied
     */
    public MavenExecutionContextView setMavenSettings(@Nullable MavenSettings settings, String... activeProfiles) {
        if (settings == null) {
            return this;
        }

        putMessage(MAVEN_SETTINGS, settings);
        List<String> effectiveActiveProfiles = mapActiveProfiles(settings, activeProfiles);
        setActiveProfiles(effectiveActiveProfiles);
        setCredentials(mapCredentials(settings));
        setMirrors(mapMirrors(settings));
        setLocalRepository(settings.getMavenLocal());
        setRepositories(mapRepositories(settings, effectiveActiveProfiles));

        return this;
    }

    public @Nullable MavenSettings getSettings() {
        return getMessage(MAVEN_SETTINGS, null);
    }

    /**
     * The maven settings in effect are a combination of the settings defined in the context and the settings that
     * were in effect when a given pom was parsed.
     *
     */
    public @Nullable MavenSettings effectiveSettings(MavenResolutionResult mrr) {
        MavenSettings effectiveSettings = getMessage(MAVEN_SETTINGS);
        if (effectiveSettings == null) {
            effectiveSettings = mrr.getMavenSettings();
        } else {
            effectiveSettings = effectiveSettings.merge(mrr.getMavenSettings());
        }
        return effectiveSettings;
    }

    private static List<String> mapActiveProfiles(MavenSettings settings, String... activeProfiles) {
        if (settings.getActiveProfiles() == null) {
            return Arrays.asList(activeProfiles);
        }
        return Stream.concat(
                        settings.getActiveProfiles().getActiveProfiles().stream(),
                        Arrays.stream(activeProfiles))
                .distinct()
                .collect(toList());
    }

    private static List<MavenRepositoryCredentials> mapCredentials(MavenSettings settings) {
        if (settings.getServers() != null) {
            return settings.getServers().getServers().stream()
                    .map(server -> new MavenRepositoryCredentials(server.getId(), server.getUsername(), server.getPassword()))
                    .collect(toList());
        }
        return emptyList();
    }

    private static List<MavenRepositoryMirror> mapMirrors(MavenSettings settings) {
        if (settings.getMirrors() != null) {
            return settings.getMirrors().getMirrors().stream()
                    .map(mirror -> new MavenRepositoryMirror(mirror.getId(), mirror.getUrl(), mirror.getMirrorOf(), mirror.getReleases(), mirror.getSnapshots(), settings.getServers()))
                    .collect(toList());
        }
        return emptyList();
    }

    private List<MavenRepository> mapRepositories(MavenSettings settings, List<String> activeProfiles) {
        Map<String, MavenRepository> repositories = this.getRepositories().stream()
                .collect(toMap(MavenRepository::getId, r -> r, (a, b) -> a));
        return settings.getActiveRepositories(activeProfiles).stream()
                .map(repo -> {
                    try {
                        MavenRepository knownRepo = repositories.get(repo.getId());
                        if (knownRepo != null) {
                            return new MavenRepository(
                                    repo.getId(),
                                    repo.getUrl(),
                                    repo.getReleases() != null ? repo.getReleases().getEnabled() : knownRepo.getReleases(),
                                    repo.getSnapshots() != null ? repo.getSnapshots().getEnabled() : knownRepo.getSnapshots(),
                                    knownRepo.isKnownToExist() && knownRepo.getUri().equals(repo.getUrl()),
                                    knownRepo.getUsername(),
                                    knownRepo.getPassword(),
                                    knownRepo.getTimeout(),
                                    knownRepo.getDeriveMetadataIfMissing()
                            );
                        } else {
                            return new MavenRepository(
                                    repo.getId(),
                                    repo.getUrl(),
                                    repo.getReleases() == null ? null : repo.getReleases().getEnabled(),
                                    repo.getSnapshots() == null ? null : repo.getSnapshots().getEnabled(),
                                    null,
                                    null,
                                    null
                            );
                        }
                    } catch (Exception exception) {
                        //noinspection DataFlowIssue
                        this.getOnError().accept(new MavenParsingException(
                                "Unable to parse URL %s for Maven settings repository id %s",
                                exception,
                                repo.getUrl(), repo.getId()));
                        return null;
                    }
                })
                .filter(Objects::nonNull)
                .collect(toList());
    }
}
