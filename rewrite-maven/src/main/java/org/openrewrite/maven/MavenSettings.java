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

import com.ctc.wstx.stax.WstxInputFactory;
import com.ctc.wstx.stax.WstxOutputFactory;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.xml.XmlFactory;
import com.fasterxml.jackson.dataformat.xml.XmlMapper;
import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlElementWrapper;
import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlProperty;
import lombok.*;
import lombok.experimental.FieldDefaults;
import lombok.experimental.NonFinal;
import org.openrewrite.Parser;
import org.openrewrite.internal.lang.Nullable;
import org.openrewrite.maven.internal.RawRepositories;

import javax.xml.stream.XMLInputFactory;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.net.URI;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import static java.util.Collections.emptyList;

@FieldDefaults(makeFinal = true, level = AccessLevel.PRIVATE)
@ToString(onlyExplicitlyIncluded = true)
@EqualsAndHashCode(onlyExplicitlyIncluded = true)
public class MavenSettings {
    private static final ObjectMapper xmlMapper;

    static {
        // disable namespace handling, as some POMs contain undefined namespaces like Xlint in
        // https://repo.maven.apache.org/maven2/com/sun/istack/istack-commons/3.0.11/istack-commons-3.0.11.pom
        XMLInputFactory input = new WstxInputFactory();
        input.setProperty(XMLInputFactory.IS_REPLACING_ENTITY_REFERENCES, false);
        input.setProperty(XMLInputFactory.IS_NAMESPACE_AWARE, false);
        xmlMapper = new XmlMapper(new XmlFactory(input, new WstxOutputFactory()))
                .disable(DeserializationFeature.ACCEPT_EMPTY_STRING_AS_NULL_OBJECT)
                .disable(DeserializationFeature.ACCEPT_EMPTY_ARRAY_AS_NULL_OBJECT)
                .disable(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES);
    }

    @Nullable
    Profiles profiles;

    @Nullable
    ActiveProfiles activeProfiles;

    @Nullable
    @Getter
    Mirrors mirrors;

    @JsonCreator
    MavenSettings(
            @JsonProperty("profiles") @Nullable Profiles profiles,
            @JsonProperty("activeProfiles") @Nullable ActiveProfiles activeProfiles,
            @JsonProperty("mirrors") @Nullable Mirrors mirrors) {
        this.profiles = profiles;
        this.activeProfiles = activeProfiles;
        this.mirrors = mirrors;
    }

    public static MavenSettings parse(Parser.Input source) {
        try {
            return xmlMapper.readValue(source.getSource(), MavenSettings.class);
        } catch (IOException e) {
            throw new UncheckedIOException("Failed to parse " + source.getPath(), e);
        }
    }

    public List<RawRepositories.Repository> getActiveRepositories(Collection<String> activeProfiles) {
        List<RawRepositories.Repository> activeRepositories = new ArrayList<>();

        if (profiles != null) {
            for (Profile profile : profiles.getProfiles()) {
                if (profile.isActive(activeProfiles) || (this.activeProfiles != null &&
                        profile.isActive(this.activeProfiles.getActiveProfiles()))) {
                    if (profile.repositories != null) {
                        activeRepositories.addAll(profile.repositories.getRepositories());
                    }
                }
            }
        }
        return applyMirrors(activeRepositories);
    }

    public List<RawRepositories.Repository> applyMirrors(Collection<RawRepositories.Repository> repositories) {
        if(mirrors == null) {
            if(repositories instanceof List) {
                return (List<RawRepositories.Repository>) repositories;
            } else {
                return new ArrayList<>(repositories);
            }
        } else {
            return mirrors.applyMirrors(repositories);
        }
    }

    public RawRepositories.Repository applyMirrors(RawRepositories.Repository repository) {
        if(mirrors == null) {
            return repository;
        } else {
            return mirrors.applyMirrors(repository);
        }
    }

    @FieldDefaults(level = AccessLevel.PRIVATE)
    @Getter
    @Setter
    public static class Profiles {
        @JacksonXmlProperty(localName = "profile")
        @JacksonXmlElementWrapper(useWrapping = false)
        List<Profile> profiles = emptyList();
    }

    @FieldDefaults(level = AccessLevel.PRIVATE)
    @Getter
    @Setter
    public static class ActiveProfiles {
        @JacksonXmlProperty(localName = "activeProfile")
        @JacksonXmlElementWrapper(useWrapping = false)
        List<String> activeProfiles = emptyList();
    }

    @FieldDefaults(makeFinal = true, level = AccessLevel.PRIVATE)
    @Data
    public static class Profile {
        @Nullable
        String id;

        @Nullable
        RawRepositories repositories;

        @JsonIgnore
        public boolean isActive(Collection<String> activeProfiles) {
            if(id != null) {
                for (String activeProfile : activeProfiles) {
                    if(activeProfile.trim().equals(id)) {
                        return true;
                    }
                }
            }
            return false;
        }
    }

    @FieldDefaults(level = AccessLevel.PRIVATE)
    @Getter
    @Setter
    public static class Mirrors {
        @JacksonXmlProperty(localName = "mirror")
        @JacksonXmlElementWrapper(useWrapping = false)
        List<Mirror> mirrors = emptyList();

        public List<RawRepositories.Repository> applyMirrors(Collection<RawRepositories.Repository> repositories) {
                return repositories.stream()
                        .map(this::applyMirrors)
                        .distinct()
                        .collect(Collectors.toList());
        }

        public RawRepositories.Repository applyMirrors(RawRepositories.Repository repository) {
            RawRepositories.Repository result = repository;
            for(Mirror mirror : mirrors) {
                result = mirror.apply(result);
            }
            return result;
        }
    }

    @FieldDefaults(makeFinal = true, level = AccessLevel.PRIVATE)
    @Data
    public static class Mirror {

        /**
         * The id of this mirror.
         */
        @Nullable
        String id;

        /**
         * The optional name that describes the mirror.
         */
        @Nullable
        String name;

        @Nullable
        URI url;

        /**
         * The server ID of the repository being mirrored, e.g., "central".
         * This can be a literal id, but it can also take a few other patterns:
         *     * = everything
         *     external:* = everything not on the localhost and not file based.
         *     repo,repo1 = repo or repo1
         *     !repo1 = everything except repo1
         *
         * See: https://maven.apache.org/guides/mini/guide-mirror-settings.html#advanced-mirror-specification
         *
         */
        @Nullable
        String mirrorOf;

        @NonFinal
        private ApplicabilitySpec applicabilitySpec = null;

        private ApplicabilitySpec buildApplicabilitySpec() {
            if(mirrorOf == null) {
                return APPLICABLE_TO_NOTHING;
            }
            if(mirrorOf.equals("*")) {
                return APPLICABLE_TO_EVERYTHING;
            }
            int colonIndex = mirrorOf.indexOf(':');
            String mirrorOfWithoutExternal = mirrorOf;
            boolean externalOnly = false;
            if(colonIndex != -1) {
                externalOnly = true;
                mirrorOfWithoutExternal = mirrorOf.substring(colonIndex + 1);
            }
            List<String> mirrorsOf = Arrays.stream(mirrorOfWithoutExternal.split(",")).collect(Collectors.toList());
            Set<String> excludedRepos = new HashSet<>();
            Set<String> includedRepos = new HashSet<>();
            for(String mirror : mirrorsOf) {
                if(mirror.startsWith("!")) {
                    excludedRepos.add(mirror.substring(1));
                } else {
                    includedRepos.add(mirror);
                }
            }

            return new DefaultApplicabilitySpec(externalOnly, excludedRepos, includedRepos);
        }

        private interface ApplicabilitySpec {
            boolean isApplicable(RawRepositories.Repository repo);
        }

        private static ApplicabilitySpec APPLICABLE_TO_EVERYTHING = repo -> true;
        private static ApplicabilitySpec APPLICABLE_TO_NOTHING = repo -> false;
        private static class DefaultApplicabilitySpec implements ApplicabilitySpec {
            final boolean isExternalOnly;
            final Set<String> excludedRepos;
            final Set<String> includedRepos;

            DefaultApplicabilitySpec(boolean isExternalOnly, Set<String> excludedRepos, Set<String> includedRepos) {
                this.isExternalOnly = isExternalOnly;
                this.excludedRepos = excludedRepos;
                this.includedRepos = includedRepos;
            }

            @Override
            public boolean isApplicable(RawRepositories.Repository repo) {
                if(isExternalOnly && isInternal(repo)) {
                    return false;
                }
                // Named inclusion/exclusion beats wildcard inclusion/exclusion
                if(excludedRepos.stream().anyMatch(it -> it.equals("*"))) {
                    return includedRepos.contains(repo.getId());
                }
                if(includedRepos.stream().anyMatch(it -> it.equals("*"))) {
                    return !excludedRepos.contains(repo.getId());
                }
                return !excludedRepos.contains(repo.getId()) && includedRepos.contains(repo.getId());
            }

            private boolean isInternal(RawRepositories.Repository repo) {
                URI repoUri = URI.create(repo.getUrl());
                if(repoUri.getScheme().startsWith("file")) {
                    return true;
                }
                // Best-effort basis, by no means a full guarantee of detecting all possible local URIs
                if(repoUri.getHost().equals("localhost") || repoUri.getHost().equals("127.0.0.1")) {
                    return true;
                }
                return false;
            }
        }

        /**
         * Apply this mirror to the supplied Repository.
         * If this mirror is applicable, a Repository with the URL specified in this mirror will be returned.
         * If this mirror is inapplicable, the supplied repository will be returned unmodified.
         */
        public RawRepositories.Repository apply(RawRepositories.Repository repo) {
            if(isApplicable(repo) && url != null) {
                return new RawRepositories.Repository(id, url.toString(), repo.getReleases(), repo.getSnapshots());
            } else {
                return repo;
            }
        }

        /**
         * Returns true if this mirror is applicable to the supplied repository, otherwise false.
         */
        public boolean isApplicable(RawRepositories.Repository repo) {
            if(applicabilitySpec == null) {
                applicabilitySpec = buildApplicabilitySpec();
            }
            return applicabilitySpec.isApplicable(repo);
        }
    }
}
