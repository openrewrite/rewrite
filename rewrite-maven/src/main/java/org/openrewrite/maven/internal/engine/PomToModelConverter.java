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
package org.openrewrite.maven.internal.engine;

import org.jspecify.annotations.Nullable;
import org.openrewrite.maven.engine.shaded.org.apache.maven.model.Activation;
import org.openrewrite.maven.engine.shaded.org.apache.maven.model.ActivationProperty;
import org.openrewrite.maven.engine.shaded.org.apache.maven.model.Build;
import org.openrewrite.maven.engine.shaded.org.apache.maven.model.DependencyManagement;
import org.openrewrite.maven.engine.shaded.org.apache.maven.model.Exclusion;
import org.openrewrite.maven.engine.shaded.org.apache.maven.model.Model;
import org.openrewrite.maven.engine.shaded.org.apache.maven.model.Parent;
import org.openrewrite.maven.engine.shaded.org.apache.maven.model.PluginExecution;
import org.openrewrite.maven.engine.shaded.org.apache.maven.model.PluginManagement;
import org.openrewrite.maven.engine.shaded.org.apache.maven.model.Prerequisites;
import org.openrewrite.maven.engine.shaded.org.apache.maven.model.Repository;
import org.openrewrite.maven.engine.shaded.org.apache.maven.model.RepositoryPolicy;
import org.openrewrite.maven.engine.shaded.org.apache.maven.model.io.DefaultModelWriter;
import org.openrewrite.maven.tree.GroupArtifact;
import org.openrewrite.maven.tree.MavenRepository;
import org.openrewrite.maven.tree.Pom;
import org.openrewrite.maven.tree.ProfileActivation;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Properties;

/**
 * Converts a synthetic {@link Pom} (rewrite-gradle's {@code Pom.builder()} marker-approximation graphs, which have no
 * backing XML — DESIGN §0) into the shaded raw {@link Model} the model builder consumes, carrying exactly what the
 * {@link Pom} carries. It is the fill for {@link ReactorWorkspace}'s null-bytes seam; real project/remote poms always
 * come from XML (printed document bytes or the pom-bytes cache region) and never touch this converter.
 */
public class PomToModelConverter {

    /**
     * Convert a synthetic {@link Pom} and print it as raw XML bytes — the XML-first input the model builder needs for a
     * root pom that has no backing document (rewrite-gradle's {@code Pom.builder()} graphs).
     */
    public byte[] toXml(Pom pom) {
        try {
            ByteArrayOutputStream out = new ByteArrayOutputStream();
            new DefaultModelWriter().write(out, java.util.Collections.emptyMap(), convert(pom));
            return out.toByteArray();
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    public Model convert(Pom pom) {
        Model model = new Model();
        model.setModelVersion("4.0.0");
        if (pom.getParent() != null) {
            org.openrewrite.maven.tree.Parent p = pom.getParent();
            Parent parent = new Parent();
            parent.setGroupId(p.getGav().getGroupId());
            parent.setArtifactId(p.getGav().getArtifactId());
            parent.setVersion(p.getGav().getVersion());
            if (p.getRelativePath() != null) {
                parent.setRelativePath(p.getRelativePath());
            }
            model.setParent(parent);
        }
        model.setGroupId(pom.getGav().getGroupId());
        model.setArtifactId(pom.getGav().getArtifactId());
        model.setVersion(pom.getGav().getVersion());
        model.setPackaging(pom.getPackaging());
        model.setName(pom.getName());
        if (pom.getPrerequisites() != null) {
            Prerequisites prerequisites = new Prerequisites();
            prerequisites.setMaven(pom.getPrerequisites().getMaven());
            model.setPrerequisites(prerequisites);
        }
        model.setProperties(toProperties(pom.getProperties()));
        model.setDependencies(dependencies(pom.getDependencies()));
        if (!pom.getDependencyManagement().isEmpty()) {
            DependencyManagement dm = new DependencyManagement();
            dm.setDependencies(managedDependencies(pom.getDependencyManagement()));
            model.setDependencyManagement(dm);
        }
        model.setRepositories(repositories(pom.getRepositories()));
        model.setPluginRepositories(repositories(pom.getPluginRepositories()));
        model.setLicenses(licenses(pom.getLicenses()));
        model.setProfiles(profiles(pom.getProfiles()));
        if (pom.getSubprojects() != null) {
            model.setModules(new ArrayList<>(pom.getSubprojects()));
        }
        Build build = build(pom.getPlugins(), pom.getPluginManagement());
        if (build != null) {
            model.setBuild(build);
        }
        return model;
    }

    private static Properties toProperties(Map<String, String> properties) {
        Properties result = new Properties();
        properties.forEach((k, v) -> result.setProperty(k, v == null ? "" : v));
        return result;
    }

    private static List<org.openrewrite.maven.engine.shaded.org.apache.maven.model.Dependency> dependencies(
            List<org.openrewrite.maven.tree.Dependency> dependencies) {
        List<org.openrewrite.maven.engine.shaded.org.apache.maven.model.Dependency> result = new ArrayList<>(dependencies.size());
        for (org.openrewrite.maven.tree.Dependency d : dependencies) {
            result.add(dependency(d.getGav().getGroupId(), d.getArtifactId(), d.getVersion(),
                    d.getType(), d.getClassifier(), d.getScope(), d.getOptional(), d.getExclusions()));
        }
        return result;
    }

    private static List<org.openrewrite.maven.engine.shaded.org.apache.maven.model.Dependency> managedDependencies(
            List<org.openrewrite.maven.tree.ManagedDependency> managed) {
        List<org.openrewrite.maven.engine.shaded.org.apache.maven.model.Dependency> result = new ArrayList<>(managed.size());
        for (org.openrewrite.maven.tree.ManagedDependency m : managed) {
            if (m instanceof org.openrewrite.maven.tree.ManagedDependency.Imported) {
                result.add(dependency(m.getGroupId(), m.getArtifactId(), m.getVersion(),
                        "pom", null, "import", null, null));
            } else {
                org.openrewrite.maven.tree.ManagedDependency.Defined d = (org.openrewrite.maven.tree.ManagedDependency.Defined) m;
                result.add(dependency(m.getGroupId(), m.getArtifactId(), m.getVersion(),
                        d.getType(), d.getClassifier(), d.getScope(), null, d.getExclusions()));
            }
        }
        return result;
    }

    private static org.openrewrite.maven.engine.shaded.org.apache.maven.model.Dependency dependency(
            @Nullable String groupId, String artifactId, @Nullable String version, @Nullable String type,
            @Nullable String classifier, @Nullable String scope, @Nullable String optional,
            @Nullable List<GroupArtifact> exclusions) {
        org.openrewrite.maven.engine.shaded.org.apache.maven.model.Dependency dependency =
                new org.openrewrite.maven.engine.shaded.org.apache.maven.model.Dependency();
        dependency.setGroupId(groupId);
        dependency.setArtifactId(artifactId);
        dependency.setVersion(version);
        dependency.setType(type);
        dependency.setClassifier(classifier);
        dependency.setScope(scope);
        dependency.setOptional(optional);
        if (exclusions != null) {
            for (GroupArtifact ga : exclusions) {
                Exclusion exclusion = new Exclusion();
                exclusion.setGroupId(ga.getGroupId());
                exclusion.setArtifactId(ga.getArtifactId());
                dependency.addExclusion(exclusion);
            }
        }
        return dependency;
    }

    private static List<Repository> repositories(List<MavenRepository> repositories) {
        List<Repository> result = new ArrayList<>(repositories.size());
        for (MavenRepository repo : repositories) {
            Repository repository = new Repository();
            repository.setId(repo.getId());
            repository.setUrl(repo.getUri());
            repository.setReleases(policy(repo.getReleases()));
            repository.setSnapshots(policy(repo.getSnapshots()));
            result.add(repository);
        }
        return result;
    }

    private static @Nullable RepositoryPolicy policy(@Nullable String enabled) {
        if (enabled == null) {
            return null;
        }
        RepositoryPolicy policy = new RepositoryPolicy();
        policy.setEnabled(enabled);
        return policy;
    }

    private static List<org.openrewrite.maven.engine.shaded.org.apache.maven.model.License> licenses(
            List<org.openrewrite.maven.tree.License> licenses) {
        List<org.openrewrite.maven.engine.shaded.org.apache.maven.model.License> result = new ArrayList<>(licenses.size());
        for (org.openrewrite.maven.tree.License license : licenses) {
            org.openrewrite.maven.engine.shaded.org.apache.maven.model.License l =
                    new org.openrewrite.maven.engine.shaded.org.apache.maven.model.License();
            l.setName(license.getName());
            result.add(l);
        }
        return result;
    }

    private static List<org.openrewrite.maven.engine.shaded.org.apache.maven.model.Profile> profiles(
            List<org.openrewrite.maven.tree.Profile> profiles) {
        List<org.openrewrite.maven.engine.shaded.org.apache.maven.model.Profile> result = new ArrayList<>(profiles.size());
        for (org.openrewrite.maven.tree.Profile p : profiles) {
            org.openrewrite.maven.engine.shaded.org.apache.maven.model.Profile profile =
                    new org.openrewrite.maven.engine.shaded.org.apache.maven.model.Profile();
            profile.setId(p.getId());
            if (p.getActivation() != null) {
                profile.setActivation(activation(p.getActivation()));
            }
            profile.setProperties(toProperties(p.getProperties()));
            profile.setDependencies(dependencies(p.getDependencies()));
            if (!p.getDependencyManagement().isEmpty()) {
                DependencyManagement dm = new DependencyManagement();
                dm.setDependencies(managedDependencies(p.getDependencyManagement()));
                profile.setDependencyManagement(dm);
            }
            profile.setRepositories(repositories(p.getRepositories()));
            profile.setPluginRepositories(repositories(p.getPluginRepositories()));
            Build build = build(p.getPlugins(), p.getPluginManagement());
            if (build != null) {
                profile.setBuild(build);
            }
            result.add(profile);
        }
        return result;
    }

    private static Activation activation(ProfileActivation a) {
        Activation activation = new Activation();
        activation.setActiveByDefault(Boolean.TRUE.equals(a.getActiveByDefault()));
        activation.setJdk(a.getJdk());
        if (a.getProperty() != null) {
            ActivationProperty property = new ActivationProperty();
            property.setName(a.getProperty().getName());
            property.setValue(a.getProperty().getValue());
            activation.setProperty(property);
        }
        return activation;
    }

    private static @Nullable Build build(List<org.openrewrite.maven.tree.Plugin> plugins,
                                         List<org.openrewrite.maven.tree.Plugin> pluginManagement) {
        if (plugins.isEmpty() && pluginManagement.isEmpty()) {
            return null;
        }
        Build build = new Build();
        build.setPlugins(plugins(plugins));
        if (!pluginManagement.isEmpty()) {
            PluginManagement pm = new PluginManagement();
            pm.setPlugins(plugins(pluginManagement));
            build.setPluginManagement(pm);
        }
        return build;
    }

    private static List<org.openrewrite.maven.engine.shaded.org.apache.maven.model.Plugin> plugins(
            List<org.openrewrite.maven.tree.Plugin> plugins) {
        List<org.openrewrite.maven.engine.shaded.org.apache.maven.model.Plugin> result = new ArrayList<>(plugins.size());
        for (org.openrewrite.maven.tree.Plugin p : plugins) {
            org.openrewrite.maven.engine.shaded.org.apache.maven.model.Plugin plugin =
                    new org.openrewrite.maven.engine.shaded.org.apache.maven.model.Plugin();
            plugin.setGroupId(p.getGroupId());
            plugin.setArtifactId(p.getArtifactId());
            plugin.setVersion(p.getVersion());
            plugin.setExtensions(p.getExtensions());
            plugin.setInherited(p.getInherited());
            plugin.setConfiguration(PluginConfigurations.toDom(p.getConfiguration()));
            plugin.setDependencies(dependencies(p.getDependencies()));
            for (org.openrewrite.maven.tree.Plugin.Execution e : p.getExecutions()) {
                PluginExecution execution = new PluginExecution();
                execution.setId(e.getId());
                execution.setPhase(e.getPhase());
                execution.setInherited(e.getInherited());
                if (e.getGoals() != null) {
                    execution.setGoals(new ArrayList<>(e.getGoals()));
                }
                execution.setConfiguration(PluginConfigurations.toDom(e.getConfiguration()));
                plugin.addExecution(execution);
            }
            result.add(plugin);
        }
        return result;
    }
}
