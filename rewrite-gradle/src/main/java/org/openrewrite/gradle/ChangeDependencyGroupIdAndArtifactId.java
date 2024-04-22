package org.openrewrite.gradle;

import lombok.EqualsAndHashCode;
import lombok.Value;
import org.openrewrite.*;
import org.openrewrite.gradle.marker.GradleDependencyConfiguration;
import org.openrewrite.gradle.marker.GradleProject;
import org.openrewrite.gradle.util.ChangeStringLiteral;
import org.openrewrite.gradle.util.Dependency;
import org.openrewrite.gradle.util.DependencyStringNotationConverter;
import org.openrewrite.groovy.GroovyIsoVisitor;
import org.openrewrite.groovy.tree.G;
import org.openrewrite.internal.ListUtils;
import org.openrewrite.internal.StringUtils;
import org.openrewrite.internal.lang.Nullable;
import org.openrewrite.java.MethodMatcher;
import org.openrewrite.java.tree.Expression;
import org.openrewrite.java.tree.J;
import org.openrewrite.maven.tree.GroupArtifact;
import org.openrewrite.maven.tree.ResolvedDependency;
import org.openrewrite.semver.DependencyMatcher;
import org.openrewrite.semver.Semver;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

import static java.util.Objects.requireNonNull;
import static org.openrewrite.Validated.required;
import static org.openrewrite.Validated.test;
import static org.openrewrite.internal.StringUtils.isBlank;

@Value
@EqualsAndHashCode(callSuper = false)
public class ChangeDependencyGroupIdAndArtifactId extends ScanningRecipe<ChangeDependencyGroupIdAndArtifactId.Accumulator> {
    @Override
    public String getDisplayName() {
        return "Change Gradle dependency";
    }

    @Override
    public String getInstanceNameSuffix() {
        return String.format("`%s:%s -> %s:%s`", oldGroupId, oldArtifactId, newGroupId, newArtifactId);
    }

    @Override
    public String getDescription() {
        return "Change Gradle dependency coordinates. Either or both of `newGroupId` or `newArtifactId` " +
               "must differ from `oldGroupId` or `newGroupId`, respectively. Useful when a library adopts a new groupId " +
               "or artifactId, or when replacing a library with an alternative.";
    }

    @Option(displayName = "Old groupId",
            description = "The old groupId to replace. The groupId is the first part of a dependency coordinate `com.google.guava:guava:VERSION`. Supports glob expressions.",
            example = "org.openrewrite.recipe")
    String oldGroupId;

    @Option(displayName = "Old artifactId",
            description = "The old artifactId to replace. The artifactId is the second part of a dependency coordinate `com.google.guava:guava:VERSION`. Supports glob expressions.",
            example = "rewrite-testing-frameworks")
    String oldArtifactId;

    @Option(displayName = "New groupId",
            description = "The new groupId to use. Defaults to the existing group id.",
            example = "corp.internal.openrewrite.recipe",
            required = false)
    @Nullable
    String newGroupId;

    @Option(displayName = "New artifactId",
            description = "The new artifactId to use. Defaults to the existing artifact id.",
            example = "rewrite-testing-frameworks",
            required = false)
    @Nullable
    String newArtifactId;

    @Option(displayName = "New version",
            description = "An exact version number or node-style semver selector used to select the version number.",
            example = "29.X",
            required = false)
    @Nullable
    String newVersion;

    @Option(displayName = "Version pattern",
            description = "Allows version selection to be extended beyond the original Node Semver semantics. So for example," +
                          "Setting 'version' to \"25-29\" can be paired with a metadata pattern of \"-jre\" to select Guava 29.0-jre",
            example = "-jre",
            required = false)
    @Nullable
    String versionPattern;

    @Option(displayName = "Configuration",
            description = "The Gradle dependency configuration within which the specified dependencies should be changed. " +
                          "If not specified, the dependency will be changed in all configurations.",
            required = false)
    String configuration;

    public static class Accumulator {
        UpgradeDependencyVersion.DependencyVersionState state = new UpgradeDependencyVersion.DependencyVersionState();
        Map<GroupArtifact, GroupArtifact> groupArtifactChanges = new HashMap<>();
    }

    @Override
    public Validated<Object> validate() {
        Validated<Object> validated = super.validate();
        if (newVersion != null) {
            validated = validated.and(Semver.validate(newVersion, versionPattern));
        }
        validated = validated.and(required("newGroupId", newGroupId).or(required("newArtifactId", newArtifactId)));
        validated = validated.and(test(
                "coordinates",
                "newGroupId OR newArtifactId must be different from before",
                this,
                r -> {
                    boolean sameGroupId = isBlank(r.newGroupId) || Objects.equals(r.oldGroupId, r.newGroupId);
                    boolean sameArtifactId = isBlank(r.newArtifactId) || Objects.equals(r.oldArtifactId, r.newArtifactId);
                    return !(sameGroupId && sameArtifactId);
                }
        ));
        return validated;
    }

    @Override
    public ChangeDependencyGroupIdAndArtifactId.Accumulator getInitialValue(ExecutionContext ctx) {
        return new ChangeDependencyGroupIdAndArtifactId.Accumulator();
    }

    @Override
    public TreeVisitor<?, ExecutionContext> getScanner(ChangeDependencyGroupIdAndArtifactId.Accumulator acc) {
        TreeVisitor<?, ExecutionContext> udvScanner = getUpgradeDependencyVersion(null, null).getScanner(acc.state);
        ChangeDependencyVisitor cdv = new ChangeDependencyVisitor(oldGroupId, oldArtifactId, newGroupId, newArtifactId, configuration);

        return new TreeVisitor<Tree, ExecutionContext>() {
            @Override
            public @Nullable Tree visit(@Nullable Tree tree, ExecutionContext ctx, Cursor parent) {
                if(!(tree instanceof SourceFile)) {
                    return tree;
                }
                SourceFile s = (SourceFile) tree;
                if(udvScanner.isAcceptable(s, ctx)) {
                    udvScanner.visit(s, ctx);
                }
                if(cdv.isAcceptable(s, ctx)) {
                    cdv.visit(s, ctx);
                    acc.groupArtifactChanges.putAll(cdv.getUpdatedDependencies());
                }
                return s;
            }
        };
    }

    @Override
    public TreeVisitor<?, ExecutionContext> getVisitor(ChangeDependencyGroupIdAndArtifactId.Accumulator acc) {
        TreeVisitor<?, ExecutionContext> udvVisitor = getUpgradeDependencyVersion(null, null)
                .getVisitor(acc.state);
        ChangeDependencyVisitor cdv = new ChangeDependencyVisitor(oldGroupId, oldArtifactId, newGroupId, newArtifactId, configuration);

        return new TreeVisitor<Tree, ExecutionContext>() {
            @Override
            public @Nullable Tree visit(@Nullable Tree tree, ExecutionContext ctx) {
                if(!(tree instanceof SourceFile)) {
                    return tree;
                }
                SourceFile s = (SourceFile) tree;
                if(cdv.isAcceptable(s ,ctx)) {
                    s = (SourceFile) cdv.visitNonNull(s, ctx);
                }
                if(udvVisitor.isAcceptable(s, ctx)) {
                    for (GroupArtifact updatedGa : acc.groupArtifactChanges.values()) {
                        s = (SourceFile) getUpgradeDependencyVersion(updatedGa.getGroupId(), updatedGa.getArtifactId())
                                .getVisitor(acc.state)
                                .visitNonNull(s, ctx);
                    }
                }
                return s;
            }
        };
    }

    UpgradeDependencyVersion getUpgradeDependencyVersion(@Nullable String groupId, @Nullable String artifactId) {
        return new UpgradeDependencyVersion(groupId, artifactId, newVersion, versionPattern);
    }
}

@Value
@EqualsAndHashCode(callSuper = false)
class ChangeDependencyVisitor extends GroovyIsoVisitor<ExecutionContext> {

    String groupId;
    String artifactId;
    @Nullable
    String newGroupId;
    @Nullable
    String newArtifactId;
    @Nullable
    String configuration;

    MethodMatcher dependencyDsl = new MethodMatcher("DependencyHandlerSpec *(..)");
    Map<GroupArtifact, GroupArtifact> updatedDependencies = new HashMap<>();

    @Override
    public G.CompilationUnit visitCompilationUnit(G.CompilationUnit compilationUnit, ExecutionContext ctx) {
        if(!IsBuildGradle.matches(compilationUnit.getSourcePath())) {
            return compilationUnit;
        }
        G.CompilationUnit cu = super.visitCompilationUnit(compilationUnit, ctx);
        if(cu != compilationUnit) {
            cu = cu.withMarkers(cu.getMarkers().withMarkers(ListUtils.map(cu.getMarkers().getMarkers(), m -> {
                if (m instanceof GradleProject) {
                    return updateModel((GradleProject) m, updatedDependencies);
                }
                return m;
            })));
        }
        return cu;
    }

    @Override
    public J.MethodInvocation visitMethodInvocation(J.MethodInvocation method, ExecutionContext ctx) {
        J.MethodInvocation m = super.visitMethodInvocation(method, ctx);
        if (!dependencyDsl.matches(m) || !(StringUtils.isBlank(configuration) || m.getSimpleName().equals(configuration))) {
            return m;
        }

        List<Expression> depArgs = m.getArguments();
        if (depArgs.get(0) instanceof J.Literal || depArgs.get(0) instanceof G.GString || depArgs.get(0) instanceof G.MapEntry) {
            m = updateDependency(m);
        } else if (depArgs.get(0) instanceof J.MethodInvocation &&
                   (((J.MethodInvocation) depArgs.get(0)).getSimpleName().equals("platform") ||
                    ((J.MethodInvocation) depArgs.get(0)).getSimpleName().equals("enforcedPlatform"))) {
            m = m.withArguments(ListUtils.map(depArgs, platform -> updateDependency((J.MethodInvocation) platform)));
        }

        return m;
    }

    private J.MethodInvocation updateDependency(J.MethodInvocation m) {
        List<Expression> depArgs = m.getArguments();
        if (depArgs.get(0) instanceof J.Literal) {
            String gav = (String) ((J.Literal) depArgs.get(0)).getValue();
            if(gav == null) {
                return m;
            }
            DependencyMatcher depMatcher = requireNonNull(DependencyMatcher.build(groupId + ":" + artifactId).getValue());
            Dependency dependency = DependencyStringNotationConverter.parse(gav);
            if(!depMatcher.matches(dependency.getGroupId(), dependency.getArtifactId(), dependency.getVersion())) {
                return m;
            }
            String targetGroupId = newGroupId != null ? newGroupId : dependency.getGroupId();
            String targetArtifactId = newArtifactId != null ? newArtifactId : dependency.getArtifactId();
            if(targetGroupId.equals(dependency.getGroupId()) && targetArtifactId.equals(dependency.getArtifactId())) {
                return m;
            }
            Dependency newDependency = dependency.withGroupId(targetGroupId)
                    .withArtifactId(targetArtifactId);
            updatedDependencies.put(dependency.getGav().asGroupArtifact(), newDependency.getGav().asGroupArtifact());
            m = m.withArguments(ListUtils.mapFirst(m.getArguments(), arg -> ChangeStringLiteral.withStringValue((J.Literal) arg, newDependency.toStringNotation())));
        } else if (depArgs.get(0) instanceof G.GString) {
            List<J> strings = ((G.GString) depArgs.get(0)).getStrings();
            if(strings.size() < 2 || !(strings.get(0) instanceof J.Literal)) {
                return m;
            }
            DependencyMatcher depMatcher = requireNonNull(DependencyMatcher.build(groupId + ":" + artifactId).getValue());
            Dependency dependency = DependencyStringNotationConverter.parse((String) requireNonNull(((J.Literal) strings.get(0)).getValue()));
            if(!depMatcher.matches(dependency.getGroupId(), dependency.getArtifactId(), dependency.getVersion())) {
                return m;
            }
            String targetGroupId = newGroupId != null ? newGroupId : dependency.getGroupId();
            String targetArtifactId = newArtifactId != null ? newArtifactId : dependency.getArtifactId();
            if(targetGroupId.equals(dependency.getGroupId()) && targetArtifactId.equals(dependency.getArtifactId())) {
                return m;
            }
            Dependency newDependency = dependency.withArtifactId(newArtifactId).withGroupId(targetGroupId);
            updatedDependencies.put(dependency.getGav().asGroupArtifact(), newDependency.getGav().asGroupArtifact());
            String replacement = newDependency.toStringNotation();
            m = m.withArguments(ListUtils.mapFirst(depArgs, arg -> {
                G.GString gString = (G.GString) arg;
                return gString.withStrings(ListUtils.mapFirst(gString.getStrings(), l -> ((J.Literal) l).withValue(replacement).withValueSource(replacement)));
            }));
        } else if (depArgs.get(0) instanceof G.MapEntry) {
            G.MapEntry artifactEntry = null;
            G.MapEntry groupEntry = null;
            String groupId = null;
            String artifactId = null;
            String version = null;

            String versionStringDelimiter = "'";
            for (Expression e : depArgs) {
                if (!(e instanceof G.MapEntry)) {
                    continue;
                }
                G.MapEntry arg = (G.MapEntry) e;
                if (!(arg.getKey() instanceof J.Literal) || !(arg.getValue() instanceof J.Literal)) {
                    continue;
                }
                J.Literal key = (J.Literal) arg.getKey();
                J.Literal value = (J.Literal) arg.getValue();
                if (!(key.getValue() instanceof String) || !(value.getValue() instanceof String)) {
                    continue;
                }
                String keyValue = (String) key.getValue();
                String valueValue = (String) value.getValue();
                if ("group".equals(keyValue)) {
                    groupId = valueValue;
                    groupEntry = arg;
                } else if ("name".equals(keyValue)) {
                    if (value.getValueSource() != null) {
                        versionStringDelimiter = value.getValueSource().substring(0, value.getValueSource().indexOf(valueValue));
                    }
                    artifactEntry = arg;
                    artifactId = valueValue;
                } else if ("version".equals(keyValue)) {
                    version = valueValue;
                }
            }
            DependencyMatcher depMatcher = requireNonNull(DependencyMatcher.build(groupId + ":" + artifactId).getValue());
            if (groupId == null || artifactId == null || !depMatcher.matches(groupId, artifactId, version)) {
                return m;
            }
            String delimiter = versionStringDelimiter;
            G.MapEntry finalArtifact = artifactEntry;
            G.MapEntry finalGroup = groupEntry;
            String targetArtifactId = newArtifactId != null ? newArtifactId : artifactId;
            String targetGroupId = newGroupId != null ? newGroupId : groupId;
            updatedDependencies.put(new GroupArtifact(groupId, artifactId), new GroupArtifact(targetGroupId, targetArtifactId));
            m = m.withArguments(ListUtils.map(m.getArguments(), arg -> {
                if (arg == finalArtifact) {
                    return finalArtifact.withValue(((J.Literal) finalArtifact.getValue())
                            .withValue(targetArtifactId)
                            .withValueSource(delimiter + targetArtifactId + delimiter));
                } else if (arg == finalGroup) {
                    return finalGroup.withValue(((J.Literal) finalGroup.getValue())
                            .withValue(targetGroupId)
                            .withValueSource(delimiter + targetGroupId + delimiter));
                }
                return arg;
            }));
        }
        return m;
    }

    private GradleProject updateModel(GradleProject gp, Map<GroupArtifact, GroupArtifact> updatedDependencies) {
        Map<String, GradleDependencyConfiguration> nameToConfigurations = gp.getNameToConfiguration();
        Map<String, GradleDependencyConfiguration> updatedNameToConfigurations = new HashMap<>();
        for (Map.Entry<String, GradleDependencyConfiguration> nameToConfiguration : nameToConfigurations.entrySet()) {
            String configurationName = nameToConfiguration.getKey();
            GradleDependencyConfiguration configuration = nameToConfiguration.getValue();

            List<org.openrewrite.maven.tree.Dependency> newRequested = configuration.getRequested()
                    .stream()
                    .map(requested -> requested.withGav(requested.getGav()
                            .withGroupArtifact(updatedDependencies.getOrDefault(requested.getGav().asGroupArtifact(), requested.getGav().asGroupArtifact()))))
                    .collect(Collectors.toList());

            List<ResolvedDependency> newResolved = configuration.getResolved().stream()
                    .map(resolved ->
                            resolved.withGav(resolved.getGav()
                                    .withGroupArtifact(updatedDependencies.getOrDefault(resolved.getGav().asGroupArtifact(), resolved.getGav().asGroupArtifact()))))
                    .collect(Collectors.toList());

            updatedNameToConfigurations.put(configurationName, configuration.withRequested(newRequested).withDirectResolved(newResolved));
        }

        return gp.withNameToConfiguration(updatedNameToConfigurations);
    }
}
