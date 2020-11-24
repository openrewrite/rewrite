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

import org.openrewrite.Tree;
import org.openrewrite.internal.StreamUtils;
import org.openrewrite.maven.AbstractMavenSourceVisitor;
import org.openrewrite.maven.tree.Maven;
import org.openrewrite.maven.tree.Pom;
import org.openrewrite.maven.tree.Scope;

import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static org.openrewrite.internal.StreamUtils.distinctBy;

/**
 * Print the dependency graph in the CycloneDX (https://cyclonedx.org/) bill of materials (BOM) format.
 */
public class PrintMavenAsCycloneDxBom extends AbstractMavenSourceVisitor<String> {
    @Override
    public String defaultTo(Tree t) {
        return "";
    }

    @Override
    public String visitMaven(Maven maven) {
        Pom pom = maven.getModel();
        StringBuilder bom = new StringBuilder("<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n");

        bom.append("<bom xmlns=\"http://cyclonedx.org/schema/bom/1.2\" serialNumber=\"urn:uuid:");
        bom.append(maven.getId().toString());
        bom.append("\" version=\"1\">\n");

        bom.append("  <components>\n");
        bom.append("    <component type=\"library\" bom-ref=\"").append(pom.getArtifactId()).append("\">\n");
        bom.append("      <group>").append(pom.getGroupId()).append("</group>\n");
        bom.append("      <name>").append(pom.getArtifactId()).append("</name>\n");
        bom.append("      <version>").append(pom.getVersion()).append("</version>\n");
        bom.append("      <purl>pkg:maven/").append(pom.getGroupId()).append("/").append(pom.getArtifactId())
                .append("@").append(pom.getVersion()).append("</purl>\n");

        writeLicenses(maven, bom);
        writeDependencies(maven, bom);

        bom.append("    </component>\n");
        bom.append("  </components>\n");
        bom.append("</bom>");

        return bom.toString();
    }

    private void writeLicenses(Maven maven, StringBuilder bom) {
        if (!maven.getModel().getLicenses().isEmpty()) {
            bom.append("      <licenses>\n");

            for (Pom.License license : maven.getModel().getLicenses()) {
                bom.append("        <license>").append(license.getName()).append("</license>\n");
            }

            bom.append("      </licenses>\n");
        }
    }

    private void writeDependencies(Maven maven, StringBuilder bom) {
        Pom pom = maven.getModel();
        if (!pom.getDependencies().isEmpty()) {
            bom.append("      <dependencies>\n");
            bom.append("        <dependency ref=\"").append(pom.getArtifactId()).append("\">\n");

            List<Pom.Dependency> bomDependencies = pom.getDependencies().stream()
                    .filter(this::isCompileOrRuntime)
                    .flatMap(this::enumerateTransitives)
                    .filter(distinctBy(dep -> dep.getGroupId() + ":" + dep.getArtifactId()))
                    .sorted(Comparator.comparing(Pom.Dependency::getGroupId).thenComparing(Pom.Dependency::getArtifactId))
                    .collect(Collectors.toList());

            for(Pom.Dependency mvid : bomDependencies) {
                bom.append("          <dependency ref=\"pkg:maven/")
                        .append(mvid.getGroupId())
                        .append("/")
                        .append(mvid.getArtifactId())
                        .append("@")
                        .append(mvid.getVersion())
                        .append("\"/>\n");
            }

            bom.append("        </dependency>\n");
            bom.append("      </dependencies>\n");
        }
    }

    /**
     * Return a stream of the dependency and all of its transitive dependencies
     */
    private Stream<Pom.Dependency> enumerateTransitives(Pom.Dependency dependency) {
        return Stream.concat(
                Stream.of(dependency),
                dependency.getModel().getDependencies().stream()
                        .filter(this::isCompileOrRuntime)
                        .flatMap(this::enumerateTransitives)
        );
    }

    private boolean isCompileOrRuntime(Pom.Dependency dependency) {
        return dependency.getScope().equals(Scope.Compile) || dependency.getScope().equals(Scope.Runtime);
    }
}
