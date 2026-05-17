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
package org.openrewrite.android.marker;

import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.MapperFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.cfg.ConstructorDetector;
import com.fasterxml.jackson.databind.json.JsonMapper;
import com.fasterxml.jackson.module.paramnames.ParameterNamesModule;
import org.junit.jupiter.api.Test;

import java.util.UUID;

import static java.util.Arrays.asList;
import static java.util.Collections.singletonList;
import static org.assertj.core.api.Assertions.assertThat;

class AndroidProjectSerializationTest {

    private final ObjectMapper mapper = newMapper();

    private static ObjectMapper newMapper() {
        ObjectMapper m = JsonMapper.builder()
                .constructorDetector(ConstructorDetector.USE_PROPERTIES_BASED)
                .configure(MapperFeature.PROPAGATE_TRANSIENT_MARKER, true)
                .build()
                .registerModule(new ParameterNamesModule())
                .disable(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES)
                .setSerializationInclusion(JsonInclude.Include.NON_NULL);
        return m.setVisibility(m.getSerializationConfig().getDefaultVisibilityChecker()
                .withCreatorVisibility(JsonAutoDetect.Visibility.PUBLIC_ONLY)
                .withGetterVisibility(JsonAutoDetect.Visibility.NONE)
                .withIsGetterVisibility(JsonAutoDetect.Visibility.NONE)
                .withFieldVisibility(JsonAutoDetect.Visibility.ANY));
    }

    private ObjectMapper mapper() {
        return mapper;
    }

    @Test
    void roundTripAllFields() throws Exception {
        AndroidProject original = fullyPopulated();

        String json = mapper().writeValueAsString(original);
        AndroidProject restored = mapper().readValue(json, AndroidProject.class);

        assertThat(restored).isEqualTo(original);

        assertThat(restored.getId()).isEqualTo(original.getId());
        assertThat(restored.getPluginId()).isEqualTo("com.android.application");
        assertThat(restored.getAgpVersion()).isEqualTo("8.7.0");
        assertThat(restored.getNamespace()).isEqualTo("com.example.app");
        assertThat(restored.getApplicationId()).isEqualTo("com.example.app");

        assertThat(restored.getSdkVersions().getCompileSdk()).isEqualTo(34);
        assertThat(restored.getSdkVersions().getMinSdk()).isEqualTo(24);
        assertThat(restored.getSdkVersions().getTargetSdk()).isEqualTo(34);

        assertThat(restored.getBuildFeatures().isViewBinding()).isTrue();
        assertThat(restored.getBuildFeatures().isCompose()).isTrue();
        assertThat(restored.getBuildFeatures().isBuildConfig()).isTrue();
        assertThat(restored.getBuildFeatures().isDataBinding()).isFalse();

        assertThat(restored.getBuildTypes()).hasSize(2);
        assertThat(restored.getBuildTypes().get(0).getName()).isEqualTo("debug");
        assertThat(restored.getBuildTypes().get(1).getName()).isEqualTo("release");
        assertThat(restored.getBuildTypes().get(1).isMinifyEnabled()).isTrue();

        assertThat(restored.getProductFlavors()).hasSize(2);
        assertThat(restored.getProductFlavors().get(0).getName()).isEqualTo("free");
        assertThat(restored.getProductFlavors().get(0).getDimension()).isEqualTo("tier");

        assertThat(restored.getVariants()).hasSize(2);
        AndroidVariant paidDebug = restored.getVariants().get(1);
        assertThat(paidDebug.getName()).isEqualTo("paidDebug");
        assertThat(paidDebug.getBuildTypeName()).isEqualTo("debug");
        assertThat(paidDebug.getFlavorNames()).containsExactly("paid");
        assertThat(paidDebug.getSourceSetNames()).containsExactly("main", "paid", "debug", "paidDebug");

        assertThat(restored.getSourceSets()).hasSize(2);
        assertThat(restored.getSourceSets().get(0).getName()).isEqualTo("main");
        assertThat(restored.getSourceSets().get(0).getManifestFile()).isEqualTo("src/main/AndroidManifest.xml");
    }

    @Test
    void forwardCompatOldLstWithMissingFields() throws Exception {
        // Simulate an LST serialized before optional collection / nested-marker fields existed.
        // Only the required identity fields are present; everything else should defer to defaults.
        // Marker has @JsonTypeInfo(property = "@c"), so the discriminator is required even on legacy payloads.
        String legacyJson = "{" +
                "\"@c\":\"org.openrewrite.android.marker.AndroidProject\"," +
                "\"@ref\":1," +
                "\"id\":\"11111111-1111-1111-1111-111111111111\"," +
                "\"pluginId\":\"com.android.library\"," +
                "\"agpVersion\":\"7.4.2\"" +
                "}";

        AndroidProject restored = mapper().readValue(legacyJson, AndroidProject.class);

        assertThat(restored.getId()).isEqualTo(UUID.fromString("11111111-1111-1111-1111-111111111111"));
        assertThat(restored.getPluginId()).isEqualTo("com.android.library");
        assertThat(restored.getAgpVersion()).isEqualTo("7.4.2");
        assertThat(restored.getNamespace()).isNull();
        assertThat(restored.getApplicationId()).isNull();

        // Null-shim getters must return empty defaults rather than NPE.
        assertThat(restored.getSdkVersions()).isNotNull();
        assertThat(restored.getSdkVersions().getCompileSdk()).isNull();
        assertThat(restored.getBuildFeatures()).isNotNull();
        assertThat(restored.getBuildFeatures().isCompose()).isFalse();
        assertThat(restored.getBuildTypes()).isEmpty();
        assertThat(restored.getProductFlavors()).isEmpty();
        assertThat(restored.getVariants()).isEmpty();
        assertThat(restored.getSourceSets()).isEmpty();
    }

    @Test
    void pathsAreForwardSlashRelativeStrings() {
        AndroidProject project = fullyPopulated();

        for (AndroidSourceSet sourceSet : project.getSourceSets()) {
            for (String dir : sourceSet.getJavaSrcDirs()) {
                assertRelativeForwardSlash(dir);
            }
            for (String dir : sourceSet.getKotlinSrcDirs()) {
                assertRelativeForwardSlash(dir);
            }
            for (String dir : sourceSet.getResDirs()) {
                assertRelativeForwardSlash(dir);
            }
            if (sourceSet.getManifestFile() != null) {
                assertRelativeForwardSlash(sourceSet.getManifestFile());
            }
        }
    }

    private static void assertRelativeForwardSlash(String path) {
        assertThat(path)
                .as("path %s should use forward slashes", path)
                .doesNotContain("\\");
        assertThat(path)
                .as("path %s should be project-root-relative (no leading slash)", path)
                .doesNotStartWith("/");
        // Absolute Windows paths begin with a drive letter and colon (e.g., "C:/...").
        assertThat(path.length() < 2 || path.charAt(1) != ':')
                .as("path %s should not be a Windows-absolute path", path)
                .isTrue();
    }

    private static AndroidProject fullyPopulated() {
        AndroidSourceSet mainSrc = new AndroidSourceSet(
                "main",
                singletonList("src/main/java"),
                singletonList("src/main/kotlin"),
                singletonList("src/main/res"),
                "src/main/AndroidManifest.xml");

        AndroidSourceSet paidDebugSrc = new AndroidSourceSet(
                "paidDebug",
                singletonList("src/paidDebug/java"),
                singletonList("src/paidDebug/kotlin"),
                singletonList("src/paidDebug/res"),
                "src/paidDebug/AndroidManifest.xml");

        AndroidBuildType debug = new AndroidBuildType("debug", ".debug", "-debug", false);
        AndroidBuildType release = new AndroidBuildType("release", null, null, true);

        AndroidProductFlavor free = new AndroidProductFlavor("free", "tier", ".free", "-free");
        AndroidProductFlavor paid = new AndroidProductFlavor("paid", "tier", null, null);

        AndroidVariant freeRelease = new AndroidVariant(
                "freeRelease",
                "release",
                singletonList("free"),
                asList("main", "free", "release", "freeRelease"));

        AndroidVariant paidDebug = new AndroidVariant(
                "paidDebug",
                "debug",
                singletonList("paid"),
                asList("main", "paid", "debug", "paidDebug"));

        AndroidSdkVersions sdkVersions = new AndroidSdkVersions(34, 24, 34);
        AndroidBuildFeatures buildFeatures = new AndroidBuildFeatures(
                true,   // viewBinding
                false,  // dataBinding
                true,   // compose
                true,   // buildConfig
                false,  // aidl
                false,  // renderScript
                false   // mlModelBinding
        );

        return AndroidProject.builder()
                .id(UUID.fromString("22222222-2222-2222-2222-222222222222"))
                .pluginId("com.android.application")
                .agpVersion("8.7.0")
                .namespace("com.example.app")
                .applicationId("com.example.app")
                .sdkVersions(sdkVersions)
                .buildFeatures(buildFeatures)
                .buildTypes(asList(debug, release))
                .productFlavors(asList(free, paid))
                .variants(asList(freeRelease, paidDebug))
                .sourceSets(asList(mainSrc, paidDebugSrc))
                .build();
    }

}
