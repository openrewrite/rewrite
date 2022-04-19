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

import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlElementWrapper;
import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlProperty;
import lombok.*;
import lombok.experimental.FieldDefaults;
import lombok.experimental.NonFinal;
import org.openrewrite.ExecutionContext;
import org.openrewrite.Parser;
import org.openrewrite.internal.lang.Nullable;
import org.openrewrite.maven.internal.MavenXmlMapper;
import org.openrewrite.maven.internal.RawRepository;
import org.openrewrite.maven.tree.ProfileActivation;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import static java.util.Collections.emptyList;

@FieldDefaults(level = AccessLevel.PRIVATE)
@ToString(onlyExplicitlyIncluded = true)
@EqualsAndHashCode(onlyExplicitlyIncluded = true)
@Data
public class MavenSettings {

    @NonFinal
    @JacksonXmlElementWrapper(localName = "profiles")
    @JacksonXmlProperty(localName = "profile")
    List<Profile> profiles = emptyList();

    @NonFinal
    @JacksonXmlElementWrapper(localName = "activeProfiles")
    @JacksonXmlProperty(localName = "activeProfile")
    List<String> activeProfiles = emptyList();

    @NonFinal
    @JacksonXmlElementWrapper(localName = "mirrors")
    @JacksonXmlProperty(localName = "mirror")
    List<Mirror> mirrors = emptyList();

    @NonFinal
    @Nullable
    @JacksonXmlElementWrapper(localName = "servers")
    @JacksonXmlProperty(localName = "server")
    List<Server> servers = emptyList();

    @Nullable
    public static MavenSettings parse(Parser.Input source, ExecutionContext ctx) {
        try {
            return MavenXmlMapper.readMapper().readValue(source.getSource(), MavenSettings.class);
        } catch (IOException e) {
            ctx.getOnError().accept(new IOException("Failed to parse " + source.getPath(), e));
            return null;
        }
    }

    public List<RawRepository> getActiveRepositories(Iterable<String> activeProfiles) {
        List<RawRepository> activeRepositories = new ArrayList<>();

        for (Profile profile : profiles) {
            if (profile.isActive(activeProfiles) || profile.isActive(this.activeProfiles)) {
                if (profile.getRepositories() != null) {
                    activeRepositories.addAll(profile.getRepositories());
                }
            }
        }

        return activeRepositories;
    }

    @FieldDefaults(makeFinal = true, level = AccessLevel.PRIVATE)
    @Data
    public static class Profile {
        @Nullable
        String id;

        @Nullable
        ProfileActivation activation;

        @NonFinal
        @Nullable
        @JacksonXmlElementWrapper(localName = "repositories")
        @JacksonXmlProperty(localName = "repository")
        List<RawRepository> repositories;

        public boolean isActive(Iterable<String> activeProfiles) {
            return ProfileActivation.isActive(id, activeProfiles, activation);
        }

        public boolean isActive(String... activeProfiles) {
            return isActive(Arrays.asList(activeProfiles));
        }
    }

    @FieldDefaults(makeFinal = true, level = AccessLevel.PRIVATE)
    @Data
    public static class Mirror {
        @Nullable
        String id;

        @Nullable
        String url;

        @Nullable
        String mirrorOf;

        @Nullable
        Boolean releases;

        @Nullable
        Boolean snapshots;
    }

    @FieldDefaults(makeFinal = true, level = AccessLevel.PRIVATE)
    @Data
    public static class Server {
        String id;

        String username;
        String password;
    }
}
