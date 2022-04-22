package org.openrewrite.maven.tree;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.TreeNode;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonNode;
import lombok.AccessLevel;
import lombok.Data;
import lombok.Getter;
import lombok.Value;
import lombok.experimental.FieldDefaults;
import org.openrewrite.Incubating;
import org.openrewrite.internal.lang.Nullable;
import org.openrewrite.maven.internal.MavenXmlMapper;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

@Incubating(since = "7.22.0")
@FieldDefaults(makeFinal = true, level = AccessLevel.PRIVATE)
@Value
public class Plugin {

    String groupId;
    String artifactId;

    @Nullable
    String version;

    @Nullable
    Boolean extensions;

    @Nullable
    Boolean inherited;

    JsonNode configuration;

    List<Dependency> dependencies;
    List<Execution> executions;

    @FieldDefaults(makeFinal = true, level = AccessLevel.PRIVATE)
    @Value
    public static class Execution {

        String id;
        List<String> goals;

        String phase;

        @Nullable
        Boolean inherited;

        @Nullable
        JsonNode configuration;
    }

    @Nullable
    @Incubating(since = "7.22.0")
    public String getConfigurationStringValue(String path) {
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

    @Nullable
    @Incubating(since = "7.22.0")
    public <T> T getConfiguration(String path, Class<T> configClass) {
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

    @Incubating(since = "7.22.0")
    public <T> List<T> getConfigurationList(String path, Class<T> elementClass) {
        JsonNode current = configuration;
        if (!path.isEmpty()) {
            String[] elements = path.split("\\.");
            for (String element : elements) {
                current = current.findPath(element);
            }
        }
        if (current.isMissingNode()) {
            return Collections.emptyList();
        }

        if (current.isObject() && current.size() == 1) {
            //It is very common in XML to have a nested wrapper around a list of values. This is just a convenience to
            //drill down into the nested element when only a single element exists.
            current = current.iterator().next();
        }
        if (current.isValueNode()) {
            return Collections.singletonList(MavenXmlMapper.readMapper().convertValue(current, new TypeReference<T>() {}));
        }
        if (!current.isArray()) {
            return Collections.emptyList();
        }
        return MavenXmlMapper.readMapper().convertValue(current, new TypeReference<List<T>>() {});
    }
}

