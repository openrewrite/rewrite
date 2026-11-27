/*
 * Copyright 2026 the original author or authors.
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

import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.MapperFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.databind.cfg.ConstructorDetector;
import com.fasterxml.jackson.databind.json.JsonMapper;
import com.fasterxml.jackson.module.paramnames.ParameterNamesModule;
import org.junit.jupiter.api.Test;
import org.openrewrite.csharp.marker.MSBuildProject.ResolvedPackage;
import org.openrewrite.csharp.marker.MSBuildProject.TargetFramework;

import static java.util.Collections.singletonList;
import static org.assertj.core.api.Assertions.assertThat;

/**
 * The marker is written into the LST with Jackson, so what it records about a package has to survive that round
 * trip, and an LST written before a field existed still has to be readable.
 */
class MSBuildProjectSerializationTest {

    /**
     * Mirrors the mapper configuration that serializes LSTs and recipe edits.
     */
    private static ObjectMapper mapper() {
        ObjectMapper m = JsonMapper.builder()
                .constructorDetector(ConstructorDetector.USE_PROPERTIES_BASED)
                .configure(MapperFeature.PROPAGATE_TRANSIENT_MARKER, true)
                .disable(MapperFeature.REQUIRE_TYPE_ID_FOR_SUBTYPES)
                .build();
        m.registerModule(new ParameterNamesModule());
        m.disable(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES);
        m.disable(SerializationFeature.FAIL_ON_EMPTY_BEANS);
        m.setSerializationInclusion(JsonInclude.Include.NON_NULL);
        m.setVisibility(m.getSerializationConfig().getDefaultVisibilityChecker()
                .withCreatorVisibility(JsonAutoDetect.Visibility.PUBLIC_ONLY)
                .withGetterVisibility(JsonAutoDetect.Visibility.NONE)
                .withIsGetterVisibility(JsonAutoDetect.Visibility.NONE)
                .withFieldVisibility(JsonAutoDetect.Visibility.ANY));
        return m;
    }

    @Test
    void roundTripSourceRepository() throws Exception {
        ResolvedPackage newtonsoft = ResolvedPackage.builder()
                .name("Newtonsoft.Json")
                .resolvedVersion("13.0.3")
                .repositoryUrl("https://github.com/JamesNK/Newtonsoft.Json")
                .repositoryType("git")
                .repositoryCommit("0a2e291c0d9c0c7675d445703e51750363a549ef")
                .projectUrl("https://www.newtonsoft.com/json")
                .build();
        MSBuildProject marker = MSBuildProject.builder()
                .sdk("Microsoft.NET.Sdk")
                .targetFrameworks(singletonList(TargetFramework.builder()
                        .targetFramework("net8.0")
                        .resolvedPackages(singletonList(newtonsoft))
                        .build()))
                .build();

        ObjectMapper mapper = mapper();
        MSBuildProject read = mapper.readValue(mapper.writeValueAsString(marker), MSBuildProject.class);

        ResolvedPackage readPackage = read.getTargetFrameworks().get(0).getResolvedPackages().get(0);
        assertThat(readPackage.getRepositoryUrl()).isEqualTo("https://github.com/JamesNK/Newtonsoft.Json");
        assertThat(readPackage.getRepositoryType()).isEqualTo("git");
        assertThat(readPackage.getRepositoryCommit()).isEqualTo("0a2e291c0d9c0c7675d445703e51750363a549ef");
        assertThat(readPackage.getProjectUrl()).isEqualTo("https://www.newtonsoft.com/json");
        assertThat(read).isEqualTo(marker);
    }

    @Test
    void readsMarkerWrittenBeforeSourceRepositoryWasRecorded() throws Exception {
        //language=json
        String written = """
                {
                  "@c": "org.openrewrite.csharp.marker.MSBuildProject",
                  "@ref": 1,
                  "id": "5f1c3b0a-9d0e-4c57-8a55-0f2f0f6a4c11",
                  "sdk": "Microsoft.NET.Sdk",
                  "properties": {},
                  "packageSources": [],
                  "targetFrameworks": [
                    {
                      "targetFramework": "net8.0",
                      "packageReferences": [],
                      "resolvedPackages": [
                        {
                          "@ref": 2,
                          "name": "Newtonsoft.Json",
                          "resolvedVersion": "13.0.3",
                          "dependencies": [],
                          "depth": 0,
                          "type": "package",
                          "compileAssemblies": ["lib/net6.0/Newtonsoft.Json.dll"],
                          "runtimeAssemblies": ["lib/net6.0/Newtonsoft.Json.dll"],
                          "frameworkAssemblies": [],
                          "buildFiles": [],
                          "buildMultiTargetingFiles": [],
                          "contentFiles": [],
                          "runtimeTargets": [],
                          "resourceAssemblies": [],
                          "analyzerAssemblies": [],
                          "hasInstallScripts": false,
                          "hasXdtTransforms": false,
                          "hasLegacyContentFolder": false,
                          "dependencyRanges": {}
                        }
                      ],
                      "projectReferences": []
                    }
                  ]
                }
                """;

        MSBuildProject read = mapper().readValue(written, MSBuildProject.class);

        ResolvedPackage readPackage = read.getTargetFrameworks().get(0).getResolvedPackages().get(0);
        assertThat(readPackage.getName()).isEqualTo("Newtonsoft.Json");
        assertThat(readPackage.getCompileAssemblies()).containsExactly("lib/net6.0/Newtonsoft.Json.dll");
        assertThat(readPackage.getRepositoryUrl()).isNull();
        assertThat(readPackage.getRepositoryType()).isNull();
        assertThat(readPackage.getRepositoryCommit()).isNull();
        assertThat(readPackage.getProjectUrl()).isNull();
    }
}
