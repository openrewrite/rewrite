package org.openrewrite.gradle;

import lombok.EqualsAndHashCode;
import lombok.Value;
import org.jspecify.annotations.Nullable;
import org.openrewrite.ExecutionContext;
import org.openrewrite.SourceFile;
import org.openrewrite.Tree;
import org.openrewrite.gradle.marker.GradleDependencyConfiguration;
import org.openrewrite.gradle.marker.GradleProject;
import org.openrewrite.internal.StringUtils;
import org.openrewrite.maven.tree.GroupArtifact;
import org.openrewrite.maven.tree.GroupArtifactVersion;
import org.openrewrite.maven.tree.ResolvedDependency;
import org.openrewrite.text.PlainText;
import org.openrewrite.text.PlainTextVisitor;

import java.util.*;

import static java.util.Collections.emptyList;
import static org.openrewrite.gradle.UpgradeDependencyVersion.replaceVersion;

@Value
@EqualsAndHashCode(callSuper = false)
public class UpdateDependencyLock extends PlainTextVisitor<ExecutionContext> {

    Map<GroupArtifact, String> versionOverrides;

    public UpdateDependencyLock(Map<GroupArtifact, String> versionOverrides) {
        this.versionOverrides = versionOverrides;
    }

    public UpdateDependencyLock() {
        this(new HashMap<>());
    }

    @Override
    public boolean isAcceptable(SourceFile sourceFile, ExecutionContext executionContext) {
        return sourceFile instanceof PlainText && sourceFile.getSourcePath().endsWith("gradle.lockfile");
    }

    @Override
    public @Nullable Tree visit(@Nullable Tree tree, ExecutionContext ctx) {
        if (tree == null) {
            return null;
        }
        Optional<GradleProject> gradleProject = tree.getMarkers().findFirst(GradleProject.class);
        if (!gradleProject.isPresent()) {
            return tree;
        }

        Map<GroupArtifactVersion, SortedSet<String>> lockedVersions = new HashMap<>();
        SortedSet<String> empty = new TreeSet<>();
        List<String> comments = new ArrayList<>();

        PlainText plainText = (PlainText) tree;
        String text = plainText.getText();
        if (StringUtils.isBlank(text)) {
            return plainText;
        }

        GradleProject project = gradleProject.get();
        project.getConfigurations().stream()
                .filter(GradleDependencyConfiguration::isCanBeResolved)
                .forEach(conf -> {
                    if (conf.getResolved().isEmpty()) {
                        empty.add(conf.getName());
                    } else {
                        for (ResolvedDependency resolved : conf.getDirectResolved()) {
                            lockedVersions.computeIfAbsent(getVersion(resolved), k -> new TreeSet<>()).add(conf.getName());
                        }
                    }
                });

        for (Map.Entry<GroupArtifactVersion, SortedSet<String>> entry : lockedVersions.entrySet()) {
            if (versionOverrides.containsKey(entry.getKey().asGroupArtifact())) {
                SortedSet<String> configurations = entry.getValue();
                GroupArtifactVersion lockedVersion = entry.getKey();
                project = replaceVersion(project, ctx, lockedVersion, configurations);
            }
        }

        Arrays.stream(text.split("\n")).forEach(lock -> {
            if (isComment(lock)) {
                comments.add(lock);
            }
        });

        SortedSet<String> locks = new TreeSet<>();
        lockedVersions.forEach((gav, confs) -> {
            String lock = asLock(gav, confs);
            if (lock != null) {
                locks.add(lock);
            }
        });

        StringBuilder sb = new StringBuilder();
        if (comments != null && !comments.isEmpty()) {
            sb.append(String.join("\n", comments));
        }
        if (comments != null && !comments.isEmpty() && locks != null && !locks.isEmpty()) {
            sb.append("\n");
        }
        if (locks != null && !locks.isEmpty()) {
            sb.append(String.join("\n", locks));
        }
        sb.append("\n").append(asLock(null, empty));

        if (plainText.getText().contentEquals(sb)) {
            return tree;
        }
        return plainText.withText(sb.toString()).withMarkers(tree.getMarkers().setByType(project));
    }

    private GroupArtifactVersion getVersion(ResolvedDependency dependency) {
        GroupArtifactVersion gav = dependency.getGav().asGroupArtifactVersion();
        if (versionOverrides.containsKey(gav.asGroupArtifact())) {
            return gav.withVersion(versionOverrides.get(gav.asGroupArtifact()));
        }
        return gav;
    }

    private @Nullable String asLock(@Nullable GroupArtifactVersion gav, @Nullable Set<String> configurations) {
        TreeSet<String> sortedConfigurations = new TreeSet<>(configurations == null ? emptyList() : configurations);
        if (gav == null) {
            return "empty=" + String.join(",", sortedConfigurations);
        }
        if (sortedConfigurations.isEmpty()) {
            return null;
        }
        return String.format("%s=%s", gav, String.join(",", sortedConfigurations));
    }

    private static boolean isComment(String entry) {
        return entry.startsWith("# ");
    }
}
