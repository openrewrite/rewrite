/*
 * Copyright 2020 the original author or authors.
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
package org.openrewrite.maven.utilities;

import org.openrewrite.internal.ListUtils;
import org.openrewrite.maven.tree.*;
import org.openrewrite.xml.tree.Xml;

import java.time.Instant;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Print the dependency graph in the CycloneDX (https://cyclonedx.org/) bill of materials (BOM) format.
 */
public final class PrintMavenAsCycloneDxBom {

    private PrintMavenAsCycloneDxBom() {
    }

    public static String print(Xml.Document maven) {

        MavenResolutionResult resolutionResult = maven.getMarkers().findFirst(MavenResolutionResult.class)
                .orElseThrow(() -> new IllegalStateException("Expected to find a maven resolution marker"));

        ResolvedPom pom = resolutionResult.getPom();

        StringBuilder bom = new StringBuilder("<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n");
        bom.append("<bom xmlns=\"http://cyclonedx.org/schema/bom/1.2\" serialNumber=\"urn:uuid:")
                .append(maven.getId().toString()).append("\" version=\"1\">\n");
        writeMetadata(pom, bom);

        List<ResolvedDependency> compileScopeDependencies = resolutionResult.getDependencies().get(Scope.Compile);
        List<ResolvedDependency> providedScopeDependencies = resolutionResult.getDependencies().get(Scope.Provided);

        if (providedScopeDependencies != null && !providedScopeDependencies.isEmpty()) {
            //Filter out duplicate group/artifacts that already exist in compile scope
            Set<GroupArtifact> artifacts = compileScopeDependencies.stream().map(PrintMavenAsCycloneDxBom::dependencyToGroupArtifact).collect(Collectors.toSet());
            providedScopeDependencies = providedScopeDependencies.stream().filter(d -> !artifacts.contains(PrintMavenAsCycloneDxBom.dependencyToGroupArtifact(d))).collect(Collectors.toList());
        }

        //May need to do more dependencies (in the various scopes)
        writeComponents(compileScopeDependencies, providedScopeDependencies, bom);
        writeDependencies(ListUtils.concatAll(compileScopeDependencies, providedScopeDependencies), bom);

        bom.append("</bom>\n");

        return bom.toString();
    }

    private static GroupArtifact dependencyToGroupArtifact(ResolvedDependency dependency) {
        return new GroupArtifact(dependency.getGroupId(), dependency.getArtifactId());
    }

    private static void writeMetadata(ResolvedPom pom, StringBuilder bom) {
        bom.append("    <metadata>\n");
        bom.append("        <timestamp>").append(Instant.now().toString()).append("</timestamp>\n");
        bom.append("        <tools>\n");
        bom.append("            <tool>\n");
        bom.append("                <vendor>OpenRewrite</vendor>\n");
        bom.append("                <name>OpenRewrite CycloneDX</name>\n");
        //Probably should pull the version from build properties.
        bom.append("                <version>7.18.0</version>\n");
        bom.append("            </tool>\n");
        bom.append("        </tools>\n");

        //(Scope scope, String groupId, String artifactId, String version, String packaging, List<String> licenses, String bomReference, StringBuilder bom) {
        String packaging = ("war".equals(pom.getPackaging()) || "ear".equals(pom.getPackaging())) ? "application" : "library";

        writeComponent(
                Scope.Compile,
                pom.getValue(pom.getGroupId()),
                pom.getArtifactId(),
                pom.getValue(pom.getVersion()),
                packaging,
                pom.getRequested().getLicenses(),
                bom);

        bom.append("    </metadata>\n");
    }

    private static void writeComponents(List<ResolvedDependency> dependencies, List<ResolvedDependency> provided, StringBuilder bom) {
        if (dependencies.isEmpty()) {
            return;
        }

        bom.append("    <components>\n");
        for (ResolvedDependency dependency : dependencies) {
            writeComponent(
                    Scope.Compile,
                    dependency.getGroupId(),
                    dependency.getArtifactId(),
                    dependency.getVersion(),
                    "library",
                    dependency.getLicenses(),
                    bom);
        }
        for (ResolvedDependency dependency : provided) {
            writeComponent(
                    Scope.Provided,
                    dependency.getGroupId(),
                    dependency.getArtifactId(),
                    dependency.getVersion(),
                    "library",
                    dependency.getLicenses(),
                    bom);
        }
        bom.append("    </components>\n");
    }
    private static void writeDependencies(List<ResolvedDependency> dependencies, StringBuilder bom) {
        if (dependencies.isEmpty()) {
            return;
        }
        bom.append("    <dependencies>\n");
        for (ResolvedDependency dependency : dependencies) {
            writeDependency(dependency, bom);
        }
        bom.append("    </dependencies>\n");
    }

    private static void writeDependency(ResolvedDependency dependency, StringBuilder bom) {
        String bomReference = getBomReference(dependency.getGroupId(), dependency.getArtifactId(), dependency.getVersion());
        bom.append("        <dependency ref=\"").append(bomReference).append("\">\n");
        if (dependency.getDependencies() != null) {
            for (ResolvedDependency nested : dependency.getDependencies()) {
                bom.append("            <dependency ref=\"")
                        .append(getBomReference(
                                nested.getGroupId(),
                                nested.getArtifactId(),
                                nested.getVersion())
                        ).append("\"/>\n");
            }
        }
        bom.append("        </dependency>\n");
    }

    private static void writeComponent(Scope scope, String groupId, String artifactId, String version,
                                       String packaging, List<License> licenses, StringBuilder bom) {

        String indent = "        ";
        String bomReference = getBomReference(groupId, artifactId, version);
        bom.append(indent).append("<component bom-ref=\"").append(bomReference).append("\" type=\"").append(packaging).append("\">\n");
        bom.append(indent).append("    <group>").append(groupId).append("</group>\n");
        bom.append(indent).append("    <name>").append(artifactId).append("</name>\n");
        bom.append(indent).append("    <version>").append(version).append("</version>\n");

        if (scope != null) {
            //Cyclone schema allows three scopes:
            String cycloneScope;
            switch (scope) {
                case Compile:
                case System:
                    cycloneScope = "required";
                    break;
                case None:
                case Invalid:
                case Test:
                    cycloneScope = "excluded";
                    break;
                default:
                    cycloneScope = "optional";
            }
            bom.append(indent).append("    <scope>").append(cycloneScope).append("</scope>\n");
        }
        writeLicenses(licenses, bom, indent);
        bom.append(indent).append("    <purl>").append(bomReference).append("</purl>\n");
        bom.append(indent).append("</component>\n");
    }

    private static void writeLicenses(List<License> licenses, StringBuilder bom, String indent) {

        if (!licenses.isEmpty()) {
            bom.append(indent).append("    <licenses>\n");

            for (License license : licenses) {
                bom.append(indent).append("        <license>\n");
                String spdxId = null;

                //This logic maps the rewrite license type to the spdx equivalent.

                //The only license type that we can establish unambiguously is the Apache 2.0 license.

                //BSD has several SPDX Mappings (no way to resolve this)
                //CDDL has a v1.0 and v1.1 (we do not distinguish them)
                //CreativeCommons has several SPDX Mappings (no way to resolve this)
                //Eclipse has a v1.0 and v2.0 (we do not distinguish them)
                //GPL has several SPDX Mappings (no way to resolve this)
                //LGPL has several SPDX Mappings (no way to resolve this)
                //MIT has several SPDX Mappings (no way to resolve this)
                //Mozilla has several SPDX Mappings (no way to resolve this)
                //PublicDomain unclear which ID to use.

                if (license.getType() == License.Type.Apache2) {
                    spdxId = "Apache-2.0";
                }
                if (spdxId != null) {
                    bom.append(indent).append("            <id>").append(spdxId).append("</id>\n");
                }
                bom.append(indent).append("            <name>").append(license.getName()).append("</name>\n");
                bom.append(indent).append("        </license>\n");
            }
            bom.append(indent).append("    </licenses>\n");
        }
    }

    private static String getBomReference(String group, String artifactId, String version) {
        return "pkg:maven/" + group + "/" + artifactId + "@" + version + "?type=jar";
    }
}
