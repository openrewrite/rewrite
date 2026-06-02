/*
 * Copyright 2022 the original author or authors.
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
package org.openrewrite.maven.tree;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonNode;
import lombok.AccessLevel;
import lombok.Value;
import lombok.experimental.FieldDefaults;
import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;
import org.openrewrite.maven.internal.MavenXmlMapper;

import java.util.List;

import static java.util.Collections.emptyList;
import static java.util.Collections.singletonList;

@FieldDefaults(makeFinal = true, level = AccessLevel.PRIVATE)
@Value
public class Plugin {
    // default value as per https://maven.apache.org/xsd/maven-4.0.0.xsd
    public static final String PLUGIN_DEFAULT_GROUPID = "org.apache.maven.plugins";

    @Nullable
    String groupId;
    String artifactId;

    @Nullable
    String version;

    @Nullable
    String extensions;

    @Nullable
    String inherited;

    @Nullable
    JsonNode configuration;

    List<Dependency> dependencies;
    List<Execution> executions;

    public String getGroupId() {
        return groupId == null ? PLUGIN_DEFAULT_GROUPID : groupId;
    }

    @FieldDefaults(makeFinal = true, level = AccessLevel.PRIVATE)
    @Value
    public static class Execution {

        @Nullable
        String id;

        @Nullable
        List<String> goals;

        @Nullable
        String phase;

        @Nullable
        String inherited;

        @Nullable
        JsonNode configuration;
    }

    public @Nullable String getConfigurationStringValue(String path) {
        if (configuration == null) {
            return null;
        }

        JsonNode current = configuration;
        if (!path.isEmpty()) {
            String[] elements = path.split("\\.");
            for (String element : elements) {
                current = current.findPath(element);
            }
        }
        if (current.isMissingNode() || !current.isValueNode()) {
            return null;
        }
        return MavenXmlMapper.readMapper().convertValue(current, String.class);
    }

    public <T> @Nullable T getConfiguration(String path, Class<T> configClass) {
        if (configuration == null) {
            return null;
        }

        JsonNode current = configuration;
        if (!path.isEmpty()) {
            String[] elements = path.split("\\.");
            for (String element : elements) {
                current = current.findPath(element);
            }
        }
        if (current.isMissingNode()) {
            return null;
        }

        try {
            //Note, attempting to use convertValue to an POJO resulted in issues with wrapped lists. Working around this
            //by converting the Jnode to a string and then into the POJO. This does work as expected.
            return MavenXmlMapper.readMapper().readValue(MavenXmlMapper.writeMapper().writeValueAsString(current), configClass);
        } catch (JsonProcessingException e) {
            throw new IllegalStateException("Error while converting the configuration class [" + configClass.getCanonicalName() + "]. " + e.getMessage());
        }
    }

    public <T> List<T> getConfigurationList(String path, Class<T> elementClass) {
        if (configuration == null) {
            return emptyList();
        }

        JsonNode current = configuration;
        if (!path.isEmpty()) {
            String[] elements = path.split("\\.");
            for (String element : elements) {
                current = current.findPath(element);
            }
        }
        if (current.isMissingNode()) {
            return emptyList();
        }

        if (current.isObject() && current.size() == 1) {
            //It is very common in XML to have a nested wrapper around a list of values. This is just a convenience to
            //drill down into the nested element when only a single element exists.
            current = current.iterator().next();
        }
        if (current.isValueNode()) {
            return singletonList(MavenXmlMapper.readMapper().convertValue(current, new TypeReference<T>() {}));
        }
        if (!current.isArray()) {
            return emptyList();
        }
        return MavenXmlMapper.readMapper().convertValue(current, new TypeReference<List<T>>() {});
    }
}
