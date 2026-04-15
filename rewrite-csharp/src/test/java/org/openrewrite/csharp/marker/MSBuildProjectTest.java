/*
 * Copyright 2025 the original author or authors.
 * <p>
 * Licensed under the Moderne Source Available License (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * <p>
 * https://docs.moderne.io/licensing/moderne-source-available-license
 * <p>
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.openrewrite.csharp.marker;

import org.junit.jupiter.api.Test;
import org.openrewrite.xml.XmlParser;
import org.openrewrite.xml.tree.Xml;

import java.nio.file.Paths;
import java.util.LinkedHashMap;
import java.util.Map;

import static java.util.Collections.singletonList;
import static org.assertj.core.api.Assertions.assertThat;

class MSBuildProjectTest {

    @Test
    void builderCreatesMarkerWithDefaults() {
        MSBuildProject marker = MSBuildProject.builder().build();
        assertThat(marker.getId()).isNotNull();
        assertThat(marker.getSdk()).isNull();
        assertThat(marker.getProperties()).isEmpty();
        assertThat(marker.getTargetFrameworks()).isEmpty();
    }

    @Test
    void builderCreatesFullMarker() {
        MSBuildProject.PackageReference ref = new MSBuildProject.PackageReference(
          "Newtonsoft.Json", "$(NJVersion)", "13.0.3");

        MSBuildProject.ResolvedPackage resolved = MSBuildProject.ResolvedPackage.builder()
          .name("Newtonsoft.Json")
          .resolvedVersion("13.0.3")
          .depth(0)
          .build();

        MSBuildProject.ProjectReference projRef = new MSBuildProject.ProjectReference("../Lib/Lib.csproj");

        MSBuildProject.TargetFramework tfm = MSBuildProject.TargetFramework.builder()
          .targetFramework("net8.0")
          .packageReferences(singletonList(ref))
          .resolvedPackages(singletonList(resolved))
          .projectReferences(singletonList(projRef))
          .build();

        Map<String, MSBuildProject.PropertyValue> props = new LinkedHashMap<>();
        props.put("NJVersion", new MSBuildProject.PropertyValue("13.0.3", Paths.get("project.csproj")));

        MSBuildProject marker = MSBuildProject.builder()
          .sdk("Microsoft.NET.Sdk.Web")
          .properties(props)
          .targetFrameworks(singletonList(tfm))
          .build();

        assertThat(marker.getSdk()).isEqualTo("Microsoft.NET.Sdk.Web");
        assertThat(marker.getTargetFrameworks()).hasSize(1);
        assertThat(marker.getTargetFrameworks().getFirst().getTargetFramework()).isEqualTo("net8.0");
        assertThat(marker.getTargetFrameworks().getFirst().getPackageReferences()).hasSize(1);
        assertThat(marker.getTargetFrameworks().getFirst().getPackageReferences().getFirst().getInclude())
          .isEqualTo("Newtonsoft.Json");
        assertThat(marker.getTargetFrameworks().getFirst().getPackageReferences().getFirst().getRequestedVersion())
          .isEqualTo("$(NJVersion)");
        assertThat(marker.getTargetFrameworks().getFirst().getPackageReferences().getFirst().getResolvedVersion())
          .isEqualTo("13.0.3");
        assertThat(marker.getTargetFrameworks().getFirst().getResolvedPackages()).hasSize(1);
        assertThat(marker.getTargetFrameworks().getFirst().getProjectReferences()).hasSize(1);
        assertThat(marker.getProperties()).containsKey("NJVersion");
        assertThat(marker.getProperties().get("NJVersion").getDefinedIn())
          .isEqualTo(Paths.get("project.csproj"));
    }

    @Test
    void markerAttachesToXmlDocument() {
        String csproj = "<Project Sdk=\"Microsoft.NET.Sdk\">\n" +
                        "  <PropertyGroup>\n" +
                        "    <TargetFramework>net8.0</TargetFramework>\n" +
                        "  </PropertyGroup>\n" +
                        "</Project>";

        Xml.Document doc = XmlParser.builder().build()
          .parse(csproj)
          .findFirst()
          .map(Xml.Document.class::cast)
          .orElseThrow();

        MSBuildProject marker = MSBuildProject.builder()
          .sdk("Microsoft.NET.Sdk")
          .build();

        doc = doc.withMarkers(doc.getMarkers().add(marker));

        assertThat(doc.getMarkers().findFirst(MSBuildProject.class))
          .isPresent()
          .hasValueSatisfying(m -> assertThat(m.getSdk()).isEqualTo("Microsoft.NET.Sdk"));
    }

    @Test
    void multiTfmProject() {
        MSBuildProject.TargetFramework net8 = MSBuildProject.TargetFramework.builder()
          .targetFramework("net8.0")
          .packageReferences(singletonList(
            new MSBuildProject.PackageReference("Foo", "1.0.0", "1.0.0")))
          .build();

        MSBuildProject.TargetFramework netStandard = MSBuildProject.TargetFramework.builder()
          .targetFramework("netstandard2.0")
          .packageReferences(java.util.Arrays.asList(
            new MSBuildProject.PackageReference("Foo", "1.0.0", "1.0.0"),
            new MSBuildProject.PackageReference("Bar", "2.0.0", "2.0.0")))
          .build();

        MSBuildProject marker = MSBuildProject.builder()
          .targetFrameworks(java.util.Arrays.asList(net8, netStandard))
          .build();

        assertThat(marker.getTargetFrameworks()).hasSize(2);
        assertThat(marker.getTargetFrameworks().get(0).getPackageReferences()).hasSize(1);
        assertThat(marker.getTargetFrameworks().get(1).getPackageReferences()).hasSize(2);
    }
}
