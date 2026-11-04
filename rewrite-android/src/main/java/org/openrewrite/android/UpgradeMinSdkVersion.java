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
package org.openrewrite.android;

import lombok.EqualsAndHashCode;
import lombok.Value;
import org.openrewrite.Option;
import org.openrewrite.android.internal.AbstractUpgradeSdkVersion;
import org.openrewrite.android.internal.SdkVersionValueSourceResolver;

import java.util.Set;

/**
 * Upgrade the {@code minSdk} (or legacy {@code minSdkVersion}) value in an
 * Android module's {@code android { defaultConfig { } }} block. Handles every
 * DSL surface that the project decision matrix calls for: literal int,
 * {@code 'android-N'} string form, extra-property reference, version-catalog
 * reference, and {@code gradle.properties} reference.
 */
@Value
@EqualsAndHashCode(callSuper = false)
public class UpgradeMinSdkVersion extends AbstractUpgradeSdkVersion {

    @Option(displayName = "To",
            description = "The new `minSdk` value to set.",
            example = "24")
    Integer to;

    @Override
    public String getDisplayName() {
        return "Upgrade Android `minSdk` version";
    }

    @Override
    public String getDescription() {
        return "Sets the `minSdk` (or legacy `minSdkVersion`) value in an Android module's " +
                "`android { defaultConfig { } }` block. Handles literal int, string form (`'android-N'`), " +
                "extra-property reference, version-catalog reference (`libs.versions.*.toml`), and " +
                "`gradle.properties` reference. Will not downgrade an already-newer value.";
    }

    @Override
    public String getInstanceNameSuffix() {
        return String.valueOf(to);
    }

    @Override
    protected Set<String> sdkAssignmentNames() {
        return SdkVersionValueSourceResolver.MIN_SDK_NAMES;
    }

    @Override
    protected int newValue() {
        return to;
    }
}
