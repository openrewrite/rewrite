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
import org.openrewrite.maven.AbstractMavenSourceVisitor;
import org.openrewrite.maven.tree.Maven;
import org.openrewrite.maven.tree.MavenModel;

import java.util.Map;
import java.util.Set;

public class PrintMavenAsCycloneDxBom extends AbstractMavenSourceVisitor<String> {
    @Override
    public String defaultTo(Tree t) {
        return "";
    }

    @Override
    public String visitPom(Maven.Pom pom) {
        StringBuilder bom = new StringBuilder("<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n");

        bom.append("<bom xmlns=\"http://cyclonedx.org/schema/bom/1.2\" serialNumber=\"urn:uuid:");
        bom.append(pom.getId().toString());
        bom.append("\" version=\"1\">\n");

        bom.append("  <components>\n");
        bom.append("    <component type=\"library\" bom-ref=\"").append(pom.getArtifactId()).append("\">\n");
        bom.append("      <group>").append(pom.getGroupId()).append("</group>\n");
        bom.append("      <name>").append(pom.getArtifactId()).append("</name>\n");
        bom.append("      <version>").append(pom.getVersion()).append("</version>\n");
        bom.append("      <purl>pkg:maven/").append(pom.getGroupId()).append("/").append(pom.getArtifactId())
                .append("@").append(pom.getVersion()).append("</purl>\n");

        writeLicenses(pom, bom);
        writeDependencies(pom, bom);

        bom.append("    </component>\n");
        bom.append("  </components>\n");
        bom.append("</bom>");

        return bom.toString();
    }

    private void writeLicenses(Maven.Pom pom, StringBuilder bom) {
        if (!pom.getModel().getLicenses().isEmpty()) {
            bom.append("      <licenses>\n");

            for (MavenModel.License license : pom.getModel().getLicenses()) {
                bom.append("        <license>").append(license.getName()).append("</license>\n");
            }

            bom.append("      </licenses>\n");
        }
    }

    private void writeDependencies(Maven.Pom pom, StringBuilder bom) {
        if (!pom.getModel().getDependencies().isEmpty()) {
            bom.append("      <dependencies>\n");
            bom.append("        <dependency ref=\"").append(pom.getArtifactId()).append("\">\n");

            for (Map.Entry<String, Set<MavenModel.ModuleVersionId>> dependenciesForScope : pom.getModel().getTransitiveDependenciesByScope().entrySet()) {
                String scope = dependenciesForScope.getKey();
                if (scope.equals("compile") || scope.equals("runtime")) {
                    for (MavenModel.ModuleVersionId mvid : dependenciesForScope.getValue()) {
                        bom.append("          <dependency ref=\"pkg:maven/")
                                .append(mvid.getGroupId())
                                .append("/")
                                .append(mvid.getArtifactId())
                                .append("@")
                                .append(mvid.getVersion())
                                .append("\"/>\n");
                    }
                }
            }

            bom.append("        </dependency>\n");
            bom.append("      </dependencies>\n");
        }
    }
}
