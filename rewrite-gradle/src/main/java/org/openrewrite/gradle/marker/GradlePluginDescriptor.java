package org.openrewrite.gradle.marker;

import lombok.Value;
import org.openrewrite.internal.lang.Nullable;

@Value
public class GradlePluginDescriptor {
    /**
     * The fully qualified name of the class which implements the plugin.
     */
    String fullyQualifiedClassName;

    /**
     * The ID by which a plugin can be applied in the plugins{} block. Not all Gradle plugins have an ID, including
     * script plugins, or plugins which are implementation details of other plugins.
     */
    @Nullable
    String id;
}
