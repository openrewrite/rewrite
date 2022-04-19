package org.openrewrite.maven.tree;

import lombok.AccessLevel;
import lombok.Data;
import lombok.Value;
import lombok.experimental.FieldDefaults;
import org.openrewrite.internal.lang.Nullable;

import java.util.List;
import java.util.Map;

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

    @Nullable
    Map<String, Object> configuration;

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
        Map<String, Object> configuration;
    }
}

