/*
 * Copyright 2026 the original author or authors.
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
package org.openrewrite.gradle.internal;

import lombok.Value;
import org.jspecify.annotations.Nullable;
import org.openrewrite.ExecutionContext;
import org.openrewrite.Tree;
import org.openrewrite.gradle.marker.GradleDependencyConfiguration;
import org.openrewrite.gradle.marker.GradleProject;
import org.openrewrite.gradle.marker.SpringDependencyManagementPlugin;
import org.openrewrite.gradle.trait.ExtraProperty;
import org.openrewrite.gradle.trait.SpringDependencyManagementPluginEntry;
import org.openrewrite.groovy.tree.G;
import org.openrewrite.internal.ListUtils;
import org.openrewrite.java.JavaIsoVisitor;
import org.openrewrite.java.tree.J;
import org.openrewrite.java.tree.JavaSourceFile;
import org.openrewrite.java.tree.Space;
import org.openrewrite.java.tree.Statement;
import org.openrewrite.kotlin.tree.K;
import org.openrewrite.maven.MavenDownloadingException;
import org.openrewrite.maven.internal.MavenPomDownloader;
import org.openrewrite.maven.tree.*;

import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static org.openrewrite.gradle.internal.GradleParseUtils.parseSnippet;
import static org.openrewrite.gradle.internal.GradleParseUtils.requireParsed;

/**
 * The Maven property through which a BOM imported by the {@code io.spring.dependency-management} plugin governs a
 * dependency's version. The plugin resolves BOM properties against the project's properties, so Spring documents
 * {@code ext['snakeyaml.version'] = '1.33'} as the way to deviate from a BOM without a constraint or resolution rule.
 */
@Value
public class SpringBomProperty {
    public static final String PLUGIN_ID = "io.spring.dependency-management";
    private static final Pattern PLACEHOLDER = Pattern.compile("\\$\\{([^}]+)}");

    String name;

    /**
     * Every artifact whose version the BOM derives from this property, directly or through a nested BOM import.
     */
    Set<GroupArtifact> governed;

    public static boolean isPluginApplied(GradleProject gradleProject) {
        return gradleProject.getPlugins().stream().anyMatch(plugin -> PLUGIN_ID.equals(plugin.getId()));
    }

    /**
     * The BOMs a script imports through {@code dependencyManagement { imports { mavenBom ... } }}, in declaration
     * order. A version given as a script property is resolved; one that cannot be determined is skipped.
     */
    public static List<GroupArtifactVersion> importedBoms(JavaSourceFile cu) {
        Map<String, String> properties = new HashMap<>();
        new ExtraProperty.Matcher().<Integer>asVisitor((property, p) -> {
            properties.put(property.getName(), property.getValue());
            return property.getTree();
        }).visit(cu, 0);
        List<GroupArtifactVersion> boms = new ArrayList<>();
        new SpringDependencyManagementPluginEntry.Matcher().<Integer>asVisitor((entry, p) -> {
            if ("mavenBom".equals(entry.getTree().getSimpleName())) {
                String version = entry.getVersion();
                String property = placeholder(version);
                if (property != null) {
                    version = properties.get(property);
                }
                if (version != null) {
                    for (String artifact : entry.getArtifacts()) {
                        boms.add(new GroupArtifactVersion(entry.getGroup(), artifact, version));
                    }
                }
            }
            return entry.getTree();
        }).visit(cu, 0);
        return boms;
    }

    /**
     * @return the property governing {@code ga} in the first BOM that manages it, or null when no BOM manages it or
     * its managed version is not a single overridable {@code ${property}} placeholder. The BOMs the plugin actually
     * imported are preferred over {@code scriptBoms}, which are only what the scripts happen to say.
     */
    public static @Nullable SpringBomProperty find(GradleProject gradleProject, List<GroupArtifactVersion> scriptBoms,
                                                   GroupArtifact ga, ExecutionContext ctx) {
        SpringDependencyManagementPlugin plugin = gradleProject.getSpringDependencyManagementPlugin();
        List<GroupArtifactVersion> boms = plugin == null || plugin.getImportedBoms().isEmpty() ?
                scriptBoms : plugin.getImportedBoms();
        List<MavenRepository> repositories = gradleProject.getMavenRepositories();
        MavenPomDownloader downloader = new MavenPomDownloader(ctx);
        for (GroupArtifactVersion bomGav : boms) {
            try {
                Pom bom = downloader.download(bomGav, null, null, repositories);
                String name = governingProperty(bom, ga, downloader, repositories);
                if (name != null && honorsOverrideOf(plugin, name)) {
                    return new SpringBomProperty(name, governedBy(bom, name, downloader, repositories));
                }
            } catch (MavenDownloadingException ignored) {
                // A BOM we cannot read cannot be overridden through its properties either
            }
        }
        return null;
    }

    /**
     * The plugin substitutes a project property for an imported BOM property only for properties it imported, so
     * where the plugin's own view is available it decides.
     */
    private static boolean honorsOverrideOf(@Nullable SpringDependencyManagementPlugin plugin, String name) {
        return plugin == null || plugin.getImportedProperties().isEmpty() || plugin.getImportedProperties().containsKey(name);
    }

    private static @Nullable String governingProperty(Pom bom, GroupArtifact ga, MavenPomDownloader downloader,
                                                      List<MavenRepository> repositories) throws MavenDownloadingException {
        for (ManagedDependency managed : bom.getDependencyManagement()) {
            // The BOM's own entry wins over anything it imports, and a literal version cannot be overridden
            if (managed instanceof ManagedDependency.Defined && manages(managed, ga)) {
                return overridable(placeholder(managed.getVersion()));
            }
        }
        for (ManagedDependency managed : bom.getDependencyManagement()) {
            if (managed instanceof ManagedDependency.Imported) {
                String property = overridable(placeholder(managed.getVersion()));
                Pom nested = property == null ? null : downloadImported(bom, managed, downloader, repositories);
                if (nested != null && nested.getDependencyManagement().stream().anyMatch(md -> md instanceof ManagedDependency.Defined && manages(md, ga))) {
                    return property;
                }
            }
        }
        return null;
    }

    private static Set<GroupArtifact> governedBy(Pom bom, String name, MavenPomDownloader downloader,
                                                 List<MavenRepository> repositories) throws MavenDownloadingException {
        Set<GroupArtifact> governed = new LinkedHashSet<>();
        for (ManagedDependency managed : bom.getDependencyManagement()) {
            if (!name.equals(placeholder(managed.getVersion()))) {
                continue;
            }
            if (managed instanceof ManagedDependency.Defined) {
                governed.add(new GroupArtifact(managed.getGroupId(), managed.getArtifactId()));
            } else {
                Pom nested = downloadImported(bom, managed, downloader, repositories);
                if (nested != null) {
                    for (ManagedDependency md : nested.getDependencyManagement()) {
                        if (md instanceof ManagedDependency.Defined) {
                            governed.add(new GroupArtifact(md.getGroupId(), md.getArtifactId()));
                        }
                    }
                }
            }
        }
        return governed;
    }

    private static @Nullable Pom downloadImported(Pom bom, ManagedDependency imported, MavenPomDownloader downloader,
                                                  List<MavenRepository> repositories) throws MavenDownloadingException {
        String version = bom.getValue(imported.getVersion());
        if (version == null || version.contains("${")) {
            return null;
        }
        return downloader.download(new GroupArtifactVersion(imported.getGroupId(), imported.getArtifactId(), version), null, null, repositories);
    }

    /**
     * The other governed artifacts some configuration resolves, keyed to the configurations resolving them.
     */
    public Map<GroupArtifact, Set<String>> governedOnClasspath(GradleProject gradleProject, GroupArtifact except) {
        Map<GroupArtifact, Set<String>> onClasspath = new LinkedHashMap<>();
        for (GradleDependencyConfiguration configuration : gradleProject.getConfigurations()) {
            for (ResolvedDependency resolved : configuration.getResolved()) {
                GroupArtifact ga = resolved.getGav().asGroupArtifact();
                if (!ga.equals(except) && governed.contains(ga)) {
                    onClasspath.computeIfAbsent(ga, k -> new HashSet<>()).add(configuration.getName());
                }
            }
        }
        return onClasspath;
    }

    /**
     * Whether every artifact is published at {@code version}, so that moving them all through one property
     * override does not leave the build unresolvable.
     */
    public static boolean isPublished(String version, Collection<GroupArtifact> gas, List<MavenRepository> repositories, ExecutionContext ctx) {
        MavenPomDownloader downloader = new MavenPomDownloader(ctx);
        for (GroupArtifact ga : gas) {
            try {
                if (!downloader.downloadMetadata(ga, null, repositories).getVersioning().getVersions().contains(version)) {
                    return false;
                }
            } catch (MavenDownloadingException e) {
                return false;
            }
        }
        return true;
    }

    private static boolean manages(ManagedDependency managed, GroupArtifact ga) {
        return managed.getGroupId().equals(ga.getGroupId()) && managed.getArtifactId().equals(ga.getArtifactId());
    }

    private static @Nullable String placeholder(@Nullable String version) {
        Matcher matcher = version == null ? null : PLACEHOLDER.matcher(version);
        return matcher != null && matcher.matches() ? matcher.group(1) : null;
    }

    private static @Nullable String overridable(@Nullable String property) {
        // project.* placeholders resolve against the BOM's own coordinates, not the importing project's properties
        return property == null || property.startsWith("project.") ? null : property;
    }

    /**
     * The properties the script assigns through any {@code ext}/{@code extra} form, whatever the value's shape.
     */
    public static Set<String> declaredProperties(JavaSourceFile cu) {
        Set<String> declared = new LinkedHashSet<>();
        new JavaIsoVisitor<Integer>() {
            @Override
            public J preVisit(J tree, Integer p) {
                String name = ExtraProperty.Matcher.assignedProperty(getCursor());
                if (name != null) {
                    declared.add(name);
                }
                return tree;
            }
        }.visit(cu, 0);
        return declared;
    }

    /**
     * Inserts {@code ext['name'] = 'value'} (or {@code extra["name"] = "value"} for the Kotlin DSL) after the
     * {@code plugins} block, where the dependency management plugin reads it lazily when the BOM is resolved.
     */
    public static JavaSourceFile addDeclaration(JavaSourceFile cu, String name, String value, ExecutionContext ctx) {
        if (cu instanceof K.CompilationUnit) {
            K.CompilationUnit k = (K.CompilationUnit) cu;
            Statement declaration = parseSnippet("extra[\"" + name + "\"] = \"" + value + "\"", true, ctx)
                    .map(requireParsed(K.CompilationUnit.class))
                    .map(parsed -> ((J.Block) parsed.getStatements().get(0)).getStatements().get(0))
                    .orElseThrow(() -> new IllegalStateException("Unable to parse extra property declaration"));
            return k.withStatements(ListUtils.mapFirst(k.getStatements(), statement -> {
                if (!(statement instanceof J.Block)) {
                    return statement;
                }
                J.Block block = (J.Block) statement;
                return block.withStatements(insertAfterPlugins(block.getStatements(), declaration));
            }));
        }
        G.CompilationUnit g = (G.CompilationUnit) cu;
        Statement declaration = parseSnippet("ext['" + name + "'] = '" + value + "'", false, ctx)
                .map(requireParsed(G.CompilationUnit.class))
                .map(parsed -> parsed.getStatements().get(0))
                .orElseThrow(() -> new IllegalStateException("Unable to parse ext property declaration"));
        return g.withStatements(insertAfterPlugins(g.getStatements(), declaration));
    }

    private static List<Statement> insertAfterPlugins(List<Statement> statements, Statement declaration) {
        declaration = declaration.withId(Tree.randomId());
        int index = 0;
        for (int i = 0; i < statements.size(); i++) {
            if (statements.get(i) instanceof J.MethodInvocation) {
                String name = ((J.MethodInvocation) statements.get(i)).getSimpleName();
                if ("plugins".equals(name) || "buildscript".equals(name)) {
                    index = i + 1;
                }
            }
        }
        if (index > 0) {
            return ListUtils.insert(statements, declaration.withPrefix(Space.format("\n\n")), index);
        }
        // Take over the leading whitespace and comments so a license header stays first
        Space first = Space.firstPrefix(statements);
        return ListUtils.insert(Space.formatFirstPrefix(statements, Space.format("\n\n")), declaration.withPrefix(first), 0);
    }
}
