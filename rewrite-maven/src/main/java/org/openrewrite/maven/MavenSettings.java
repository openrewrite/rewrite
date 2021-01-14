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
import org.openrewrite.Parser;
import org.openrewrite.internal.lang.Nullable;
import org.openrewrite.maven.internal.RawRepositories;

import javax.xml.stream.XMLInputFactory;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

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

    @JsonCreator
    MavenSettings(
            @JsonProperty("profiles") @Nullable Profiles profiles,
            @JsonProperty("activeProfiles") @Nullable ActiveProfiles activeProfiles) {
        this.profiles = profiles;
        this.activeProfiles = activeProfiles;
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

        return activeRepositories;
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
            if (id != null) {
                for (String activeProfile : activeProfiles) {
                    if (activeProfile.trim().equals(id)) {
                        return true;
                    }
                }
            }
            return false;
        }
    }
}
