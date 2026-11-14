/*
 * Copyright 2021 the original author or authors.
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
package org.openrewrite.maven.cleanup;

import lombok.Getter;
import org.jspecify.annotations.Nullable;
import org.openrewrite.ExecutionContext;
import org.openrewrite.Recipe;
import org.openrewrite.TreeVisitor;
import org.openrewrite.maven.MavenIsoVisitor;
import org.openrewrite.maven.tree.ManagedDependency;
import org.openrewrite.maven.tree.MavenResolutionResult;
import org.openrewrite.maven.tree.Pom;
import org.openrewrite.maven.tree.Profile;
import org.openrewrite.maven.tree.ResolvedGroupArtifactVersion;
import org.openrewrite.xml.RemoveContentVisitor;
import org.openrewrite.xml.tree.Xml;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class DependencyManagementDependencyRequiresVersion extends Recipe {

    @Getter
    final String displayName = "Remove dependency management entries that manage nothing";

    @Getter
    final String description = "A dependency management entry declaring nothing but its coordinates manages nothing " +
      "of its own. A missing `version` alone is not enough, as an entry can still manage `scope`, `exclusions`, " +
      "`optional` or `systemPath` for a dependency versioned elsewhere. Maven also merges dependency management " +
      "one entry at a time on the management key rather than field by field, so such an entry hides, rather than " +
      "inherits from, an entry for the same coordinates coming from a parent, from an imported BOM, or from an " +
      "earlier entry of the same POM. Entries are removed only where no such entry can be hidden, which leaves " +
      "parent and BOM POMs alone.";

    @Override
    public TreeVisitor<?, ExecutionContext> getVisitor() {
        return new MavenIsoVisitor<ExecutionContext>() {
            @Override
            public Xml.Tag visitTag(Xml.Tag tag, ExecutionContext ctx) {
                if (isManagedDependencyTag() && isInert(tag)) {
                    doAfterVisit(new RemoveContentVisitor<>(tag, true, true));
                }
                return super.visitTag(tag, ctx);
            }

            /**
             * An entry is provably inert only when it declares its coordinates and nothing else, and nothing else
             * manages those coordinates. A missing {@code version} proves nothing on its own: the entry may still
             * manage {@code scope}, {@code exclusions}, {@code optional} or {@code systemPath} for a dependency
             * versioned elsewhere, and {@code type} and {@code classifier} decide which dependencies it applies to.
             */
            private boolean isInert(Xml.Tag tag) {
                for (Xml.Tag child : tag.getChildren()) {
                    if (!"groupId".equals(child.getName()) && !"artifactId".equals(child.getName())) {
                        return false;
                    }
                }
                String groupId = resolve(tag, "groupId");
                String artifactId = resolve(tag, "artifactId");
                return groupId != null && artifactId != null && !managedElsewhere(tag, groupId, artifactId);
            }

            /**
             * Because Maven merges dependency management per entry rather than per field, an entry declaring only
             * coordinates hides an entry for the same key coming from a parent, an imported BOM or a sibling entry,
             * and removing it lets that hidden entry take effect. Only management this POM can see is provably
             * absent, so a POM others inherit from or import is left alone. The one consumer this cannot recognize
             * is a project importing a POM packaged as anything but {@code pom} as a BOM, since only the parent
             * relation is recorded on either side.
             */
            private boolean managedElsewhere(Xml.Tag tag, String groupId, String artifactId) {
                if ("pom".equals(getResolutionResult().getPom().getPackaging()) || !getResolutionResult().getModules().isEmpty()) {
                    return true;
                }
                Pom pom = getResolutionResult().getPom().getRequested();
                if (pom.getParent() != null) {
                    MavenResolutionResult parent = getResolutionResult().getParent();
                    // A parent outside this repository is not resolved here, so what it manages is unknown.
                    if (parent == null || parent.getPom().getManagedDependency(groupId, artifactId, null, null) != null) {
                        return true;
                    }
                    Set<ResolvedGroupArtifactVersion> visited = new HashSet<>();
                    for (MavenResolutionResult ancestor = parent; ancestor != null; ancestor = ancestor.getParent()) {
                        if (!visited.add(ancestor.getPom().getGav())) {
                            // The parent chain contains a cycle, so leave the dependency unchanged.
                            return true;
                        }
                        // A resolved ancestor only reflects the profiles active when it was parsed
                        if (declaresProfileDependencyManagement(ancestor.getPom().getRequested())) {
                            return true;
                        }
                        if (ancestor.getPom().getRequested().getParent() != null && ancestor.getParent() == null) {
                            // The ancestry leaves this repository, so profiles further up cannot be inspected either.
                            return true;
                        }
                    }
                }
                // This POM's own profiles take precedence over the entry under review, so only what a BOM they
                // import might manage is unknown
                if (importsBom(pom.getDependencyManagement())) {
                    return true;
                }
                for (Profile profile : pom.getProfiles()) {
                    if (importsBom(profile.getDependencyManagement())) {
                        return true;
                    }
                }
                // The entry-wise merge collapses duplicate management keys to the last entry, so removing this one
                // can change which sibling takes effect; Maven flags duplicates itself ("must be unique")
                Xml.Tag dependencies = getCursor().getParentOrThrow().getValue();
                for (Xml.Tag sibling : dependencies.getChildren("dependency")) {
                    if (sibling == tag) {
                        continue;
                    }
                    String siblingGroupId = resolve(sibling, "groupId");
                    String siblingArtifactId = resolve(sibling, "artifactId");
                    if (siblingGroupId == null || siblingArtifactId == null ||
                      (groupId.equals(siblingGroupId) && artifactId.equals(siblingArtifactId))) {
                        return true;
                    }
                }
                return false;
            }

            /**
             * @return The child element's value with properties resolved, or {@code null} when it is absent or
             * unresolvable, which leaves it unknown what the entry hides.
             */
            private @Nullable String resolve(Xml.Tag tag, String childName) {
                String value = getResolutionResult().getPom().getValue(tag.getChildValue(childName).orElse(null));
                return value == null || containsUnresolvedPlaceholder(value) ? null : value;
            }
        };
    }

    // A surviving `${` means Maven could not resolve the property, so the value is unusable for comparison
    private static boolean containsUnresolvedPlaceholder(String value) {
        return value.contains("${");
    }

    private static boolean importsBom(@Nullable List<ManagedDependency> dependencyManagement) {
        if (dependencyManagement != null) {
            for (ManagedDependency managed : dependencyManagement) {
                if (managed instanceof ManagedDependency.Imported) {
                    return true;
                }
            }
        }
        return false;
    }

    private static boolean declaresProfileDependencyManagement(Pom pom) {
        for (Profile profile : pom.getProfiles()) {
            List<ManagedDependency> dependencyManagement = profile.getDependencyManagement();
            if (dependencyManagement != null && !dependencyManagement.isEmpty()) {
                return true;
            }
        }
        return false;
    }
}
