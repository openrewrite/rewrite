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

import com.fasterxml.jackson.annotation.JsonCreator;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Value;
import lombok.With;
import org.jspecify.annotations.Nullable;
import org.openrewrite.marker.Marker;

import java.io.Serializable;
import java.util.List;
import java.util.UUID;

import static java.util.Collections.emptyList;
import static org.openrewrite.Tree.randomId;

/**
 * Build-time metadata captured from the Android Gradle Plugin (AGP). Attached
 * <em>alongside</em> {@link org.openrewrite.gradle.marker.GradleProject} on every
 * source file in an Android module, never wrapping or extending it. This split
 * lets all existing {@code rewrite-gradle} recipes keep working unchanged on
 * Android projects, while Android-aware recipes pick up the additional context.
 * <p>
 * Like other build-context markers, {@code AndroidProject} is constructed during
 * project ingest by a builder running inside a Gradle build (in moderne-cli's
 * {@code MetadataPlugin}) and is not automatically available on LSTs parsed
 * outside that pipeline. Tests assemble fixtures explicitly.
 */
@SuppressWarnings("unused")
@Value
@AllArgsConstructor(onConstructor_ = {@JsonCreator})
@Builder
public class AndroidProject implements Marker, Serializable {

    @With
    @Builder.Default
    UUID id = randomId();

    /**
     * The full Gradle plugin ID that triggered Android handling
     * ({@code com.android.application}, {@code com.android.library},
     * {@code com.android.test}, {@code com.android.dynamic-feature},
     * {@code com.android.asset-pack}, {@code com.android.asset-pack-bundle},
     * {@code com.android.fused-library}, {@code com.android.kmp.library}, ...).
     * Stored as a string rather than an enum so new AGP plugin variants are never
     * silently dropped on ingest.
     */
    @With
    String pluginId;

    /**
     * The AGP version resolved at ingest. Read from
     * {@code com.android.builder.model.Version.ANDROID_GRADLE_PLUGIN_VERSION}; if
     * that class is unavailable, falls back to the plugin jar's
     * {@code META-INF/MANIFEST.MF} {@code Implementation-Version}.
     */
    @With
    String agpVersion;

    /**
     * AGP 7+ {@code android.namespace}. Null for older projects, which fall back to
     * the {@code package} attribute on the merged AndroidManifest.
     */
    @With
    @Nullable
    String namespace;

    /**
     * {@code defaultConfig.applicationId}. Null for library and other non-application
     * plugin types where no application ID is configured.
     */
    @With
    @Nullable
    String applicationId;

    @With
    @Builder.Default
    AndroidSdkVersions sdkVersions = new AndroidSdkVersions(null, null, null);

    @With
    @Builder.Default
    AndroidBuildFeatures buildFeatures = new AndroidBuildFeatures(
            false, false, false, false, false, false, false);

    @With
    @Builder.Default
    List<AndroidBuildType> buildTypes = emptyList();

    @With
    @Builder.Default
    List<AndroidProductFlavor> productFlavors = emptyList();

    @With
    @Builder.Default
    List<AndroidVariant> variants = emptyList();

    @With
    @Builder.Default
    List<AndroidSourceSet> sourceSets = emptyList();

    public AndroidSdkVersions getSdkVersions() {
        // Forward-compat shim for LSTs serialized before this field existed.
        //noinspection ConstantValue
        if (sdkVersions == null) {
            return new AndroidSdkVersions(null, null, null);
        }
        return sdkVersions;
    }

    public AndroidBuildFeatures getBuildFeatures() {
        //noinspection ConstantValue
        if (buildFeatures == null) {
            return new AndroidBuildFeatures(false, false, false, false, false, false, false);
        }
        return buildFeatures;
    }

    public List<AndroidBuildType> getBuildTypes() {
        //noinspection ConstantValue
        if (buildTypes == null) {
            return emptyList();
        }
        return buildTypes;
    }

    public List<AndroidProductFlavor> getProductFlavors() {
        //noinspection ConstantValue
        if (productFlavors == null) {
            return emptyList();
        }
        return productFlavors;
    }

    public List<AndroidVariant> getVariants() {
        //noinspection ConstantValue
        if (variants == null) {
            return emptyList();
        }
        return variants;
    }

    public List<AndroidSourceSet> getSourceSets() {
        //noinspection ConstantValue
        if (sourceSets == null) {
            return emptyList();
        }
        return sourceSets;
    }
}
