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

import org.apache.maven.building.Source;
import org.apache.maven.settings.Server;
import org.apache.maven.settings.Settings;
import org.apache.maven.settings.building.*;
import org.eclipse.aether.repository.Authentication;
import org.eclipse.aether.repository.AuthenticationContext;
import org.eclipse.aether.repository.AuthenticationDigest;
import org.eclipse.aether.repository.RemoteRepository;
import org.eclipse.aether.util.repository.AuthenticationBuilder;
import org.openrewrite.Parser;
import org.openrewrite.internal.StringUtils;
import org.openrewrite.internal.lang.Nullable;
import org.openrewrite.maven.tree.Maven;
import org.openrewrite.maven.tree.MavenModel;
import org.openrewrite.xml.XmlParser;
import org.openrewrite.xml.tree.Xml;

import java.io.File;
import java.io.InputStream;
import java.nio.file.Path;
import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

public class MavenParser implements Parser<Maven.Pom> {
    public static final File DEFAULT_LOCAL_REPOSITORY =
            new File(System.getProperty("user.home") + "/.m2/rewrite");

    private final XmlParser xmlParser = new XmlParser();
    private final boolean resolveDependencies;
    private final File localRepository;

    @Nullable
    private final File workspaceDir;
    private final List<RemoteRepository> remoteRepositories;

    private MavenParser(
            boolean resolveDependencies,
            File localRepository,
            @Nullable File workspaceDir,
            List<RemoteRepository> remoteRepositories
    ) {
        this.resolveDependencies = resolveDependencies;
        this.localRepository = localRepository;
        this.workspaceDir = workspaceDir;
        this.remoteRepositories = remoteRepositories;
    }

    public static Builder builder() {
        return new Builder();
    }

    @Override
    public List<Maven.Pom> parseInputs(Iterable<Input> sourceFiles, @Nullable Path relativeTo) {
        Iterable<Input> pomSourceFiles = acceptedInputs(sourceFiles);

        List<MavenModel> modules = new MavenModuleLoader(resolveDependencies,
                localRepository, workspaceDir, remoteRepositories).load(pomSourceFiles);

        List<Maven.Pom> poms = new ArrayList<>();
        Iterator<Xml.Document> xmlDocuments = xmlParser.parseInputs(pomSourceFiles, relativeTo).iterator();
        for (MavenModel module : modules) {
            poms.add(new Maven.Pom(module, xmlDocuments.next()));
        }

        return poms;
    }

    @Override
    public boolean accept(Path path) {
        return path.getFileName().toString().equals("pom.xml");
    }

    public static class Builder {
        private boolean resolveDependencies = true;
        private File localRepository = DEFAULT_LOCAL_REPOSITORY;
        private List<RemoteRepository> remoteRepositories = new ArrayList<>();
        private Input userSettingsXmlOverride;
        private Input globaLSettingsXmlOverride;

        @Nullable
        private File workspaceDir;

        public Builder() {
            remoteRepositories.add(new RemoteRepository.Builder("central", "default",
                    "https://repo1.maven.org/maven2/").build()
            );
        }

        /**
         * Override the location or contents of the settings.xml file.
         * If this isn't overridden then ${user.home}/.m2/settings.xml will be used, if such a file exists
         */
        public Builder userSettingsXml(Input settingsXmlOverride) {
            this.userSettingsXmlOverride = settingsXmlOverride;
            return this;
        }

        /**
         * Provide the global settings.xml from the maven installation.
         * If this isn't provided, then no global settings.xml is used as we cannot know for sure where the appropriate
         * maven installation may be located
         */
        public Builder globalSettingsXml(Input settingsXmlOverride) {
            this.globaLSettingsXmlOverride = settingsXmlOverride;
            return this;
        }

        public Builder localRepository(File localRepository) {
            this.localRepository = localRepository;
            return this;
        }

        public Builder workspaceDir(File workspaceDir) {
            this.workspaceDir = workspaceDir;
            return this;
        }

        public Builder resolveDependencies(boolean resolveDependencies) {
            this.resolveDependencies = resolveDependencies;
            return this;
        }

        public Builder remoteRepositories(List<RemoteRepository> remoteRepositories) {
            this.remoteRepositories = remoteRepositories;
            return this;
        }

        public Builder addRemoteRepository(RemoteRepository remoteRepository) {
            this.remoteRepositories.add(remoteRepository);
            return this;
        }

        public MavenParser build() {
            SettingsBuilder settingsBuilder =  new DefaultSettingsBuilderFactory().newInstance();
            SettingsBuildingRequest settingsRequest = new DefaultSettingsBuildingRequest();
            if(userSettingsXmlOverride == null) {
                File settingsXml = new File(System.getProperty("user.home"), ".m2/settings.xml");
                if(settingsXml.exists()) {
                    settingsRequest.setUserSettingsFile(settingsXml);
                }
            } else {
                StringSettingsSource source = new StringSettingsSource(StringUtils.readFully(userSettingsXmlOverride.getSource()));
                settingsRequest.setUserSettingsSource(source);
            }
            if(globaLSettingsXmlOverride == null) {
                // maven.conf is unlikely to be set in any non-maven process, but might as well mimic Maven's lookup behavior
                File globalSettingsXml = new File(System.getProperty("maven.conf"),"settings.xml" );
                if(globalSettingsXml.exists()) {
                    settingsRequest.setGlobalSettingsFile(globalSettingsXml);
                }
            } else {
                StringSettingsSource source = new StringSettingsSource(StringUtils.readFully(globaLSettingsXmlOverride.getSource()));
                settingsRequest.setGlobalSettingsSource(source);
            }

            Settings effectiveSettings;
            try {
                effectiveSettings = settingsBuilder.build(settingsRequest).getEffectiveSettings();
            } catch (SettingsBuildingException e) {
                throw new RuntimeException(e);
            }

            Map<String, Server> idToServer = effectiveSettings.getServers().stream()
                    .collect(Collectors.toMap(Server::getId, Function.identity()));
            Set<String> activeProfiles = new HashSet<>(effectiveSettings.getActiveProfiles());
            List<RemoteRepository> settingsDefinedRepositories = effectiveSettings.getProfiles().stream()
                    .filter(profile -> activeProfiles.contains(profile.getId()))
                    .flatMap(profile -> profile.getRepositories().stream())
                    .map(mvnRepository -> {
                        RemoteRepository.Builder builder = new RemoteRepository.Builder(
                                mvnRepository.getId(),
                                "default",
                                mvnRepository.getUrl());

                        Server credentials = idToServer.get(mvnRepository.getId());
                        if(credentials != null) {
                            Authentication authentication = new AuthenticationBuilder()
                                    .addPassword(credentials.getPassword())
                                    .addUsername(credentials.getUsername())
                                    .addPrivateKey(credentials.getPrivateKey(), credentials.getPassphrase())
                                    .build();
                            builder.setAuthentication(authentication);
                        }

                        return builder.build();
                    })
                    .collect(Collectors.toList());
            remoteRepositories.addAll(settingsDefinedRepositories);

            return new MavenParser(resolveDependencies, localRepository, workspaceDir, remoteRepositories);
        }
    }
}
