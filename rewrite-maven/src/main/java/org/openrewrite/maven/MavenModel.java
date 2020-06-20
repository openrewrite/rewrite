package org.openrewrite.maven;

import lombok.AccessLevel;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.experimental.FieldDefaults;
import org.openrewrite.internal.lang.Nullable;

import java.util.List;

/**
 * A minified, serializable form of {@link org.apache.maven.model.Model}
 * for inclusion as a data element of {@link Maven.Pom}.
 */
@FieldDefaults(makeFinal = true, level = AccessLevel.PRIVATE)
@EqualsAndHashCode(callSuper = false, onlyExplicitlyIncluded = true)
@Data
public class MavenModel {
    @Nullable
    MavenModel parent;

    @EqualsAndHashCode.Include
    ModuleVersionId moduleVersion;

    List<Dependency> dependencies;

    @FieldDefaults(makeFinal = true, level = AccessLevel.PRIVATE)
    @Data
    public static class Dependency {
        ModuleVersionId moduleVersion;
        String scope;
    }

    @FieldDefaults(makeFinal = true, level = AccessLevel.PRIVATE)
    @Data
    public static class ModuleVersionId {
        String groupId;
        String artifactId;
        String version;
    }
}
