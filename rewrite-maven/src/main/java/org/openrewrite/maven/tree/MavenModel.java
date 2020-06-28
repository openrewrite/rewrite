package org.openrewrite.maven.tree;

import lombok.AccessLevel;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.With;
import lombok.experimental.FieldDefaults;
import org.openrewrite.internal.lang.Nullable;

import java.util.Collection;
import java.util.List;
import java.util.Map;

/**
 * A minified, serializable form of {@link org.apache.maven.model.Model}
 * for inclusion as a data element of {@link Maven.Pom}.
 */
@FieldDefaults(makeFinal = true, level = AccessLevel.PRIVATE)
@EqualsAndHashCode(callSuper = false, onlyExplicitlyIncluded = true)
@Data
public class MavenModel {
    @With
    @Nullable
    MavenModel parent;

    @EqualsAndHashCode.Include
    ModuleVersionId moduleVersion;

    @With
    List<Dependency> dependencies;

    @With
    Map<String, String> properties;

    /**
     * Modules inheriting from the POM this model represents. To cut the
     * object cycle, the parent of all of these modules will be null.
     */
    Collection<MavenModel> inheriting;

    @FieldDefaults(makeFinal = true, level = AccessLevel.PRIVATE)
    @Data
    public static class Dependency {
        @With
        ModuleVersionId moduleVersion;

        @With
        String scope;
    }

    @FieldDefaults(makeFinal = true, level = AccessLevel.PRIVATE)
    @Data
    public static class ModuleVersionId {
        @With
        String groupId;

        @With
        String artifactId;

        @With
        String version;
    }

    public String valueOf(String value) {
        value = value.trim();
        return value.startsWith("${") && value.endsWith("}") ?
                properties.get(value.substring(2, value.length() - 1)) :
                value;
    }
}
