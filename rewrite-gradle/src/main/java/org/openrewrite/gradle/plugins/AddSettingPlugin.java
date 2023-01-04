package org.openrewrite.gradle.plugins;

import lombok.EqualsAndHashCode;
import lombok.Value;
import org.openrewrite.ExecutionContext;
import org.openrewrite.Option;
import org.openrewrite.Recipe;
import org.openrewrite.TreeVisitor;
import org.openrewrite.gradle.IsSettingsGradle;
import org.openrewrite.internal.lang.Nullable;

@Value
@EqualsAndHashCode(callSuper = true)
public class AddSettingPlugin extends Recipe {
    @Option(displayName = "Plugin id",
            description = "The plugin id to apply.",
            example = "com.jfrog.bintray")
    String pluginIdPattern;

    @Option(displayName = "Plugin version",
            description = "An exact version number or node-style semver selector used to select the version number.",
            example = "3.x")
    String version;

    @Option(displayName = "Version pattern",
            description = "Allows version selection to be extended beyond the original Node Semver semantics. So for example," +
                    "Setting 'version' to \"25-29\" can be paired with a metadata pattern of \"-jre\" to select Guava 29.0-jre",
            example = "-jre",
            required = false)
    @Nullable
    String versionPattern;

    @Override
    public String getDisplayName() {
        return "Add a Gradle settings plugin";
    }

    @Override
    public String getDescription() {
        return "Add a Gradle settings plugin to `build.gradle(.kts)`.";
    }

    @Override
    protected TreeVisitor<?, ExecutionContext> getSingleSourceApplicableTest() {
        return new IsSettingsGradle<>();
    }

    @Override
    protected TreeVisitor<?, ExecutionContext> getVisitor() {
        return new AddPluginVisitor(pluginIdPattern, version, versionPattern);
    }
}
