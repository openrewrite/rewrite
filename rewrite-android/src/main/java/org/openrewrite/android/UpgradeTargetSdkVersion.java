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
package org.openrewrite.android;

import lombok.EqualsAndHashCode;
import lombok.Value;
import org.jspecify.annotations.Nullable;
import org.openrewrite.Option;
import org.openrewrite.android.internal.AbstractUpgradeSdkVersion;
import org.openrewrite.android.internal.SdkVersionValueSourceResolver;

import java.util.Set;
import java.util.function.IntPredicate;

/**
 * Upgrade the {@code targetSdk} (or legacy {@code targetSdkVersion}) value in
 * an Android module's {@code android { defaultConfig { } }} block. Handles every
 * DSL surface that the project decision matrix calls for: literal int,
 * {@code 'android-N'} string form, extra-property reference, version-catalog
 * reference, and {@code gradle.properties} reference.
 * <p>
 * Optional {@code minSdkFloor}: if set, refuses to upgrade past the floor. Used
 * to coordinate with a separate min-SDK bump so the upgrade doesn't accidentally
 * land a target that exceeds a policy floor.
 */
@Value
@EqualsAndHashCode(callSuper = false)
public class UpgradeTargetSdkVersion extends AbstractUpgradeSdkVersion {

    @Option(displayName = "To",
            description = "The new `targetSdk` value to set.",
            example = "34")
    Integer to;

    @Option(displayName = "Min SDK floor",
            description = "If set, refuses to upgrade `targetSdk` past this value. Useful when " +
                    "coordinating with a separate min-SDK policy that caps the target SDK.",
            required = false,
            example = "33")
    @Nullable
    Integer minSdkFloor;

    @Override
    public String getDisplayName() {
        return "Upgrade Android `targetSdk` version";
    }

    @Override
    public String getDescription() {
        return "Sets the `targetSdk` (or legacy `targetSdkVersion`) value in an Android module's " +
                "`android { defaultConfig { } }` block. Handles literal int, string form (`'android-N'`), " +
                "extra-property reference, version-catalog reference (`libs.versions.*.toml`), and " +
                "`gradle.properties` reference. Will not downgrade an already-newer value, and will not " +
                "upgrade past `minSdkFloor` if specified.";
    }

    @Override
    public String getInstanceNameSuffix() {
        return String.valueOf(to);
    }

    @Override
    protected Set<String> sdkAssignmentNames() {
        return SdkVersionValueSourceResolver.TARGET_SDK_NAMES;
    }

    @Override
    protected int newValue() {
        return to;
    }

    @Override
    protected IntPredicate currentValueAcceptable() {
        Integer floor = minSdkFloor;
        int target = to;
        return current -> {
            if (current >= target) {
                return false;
            }
            // If a floor is set and the proposed target exceeds it, don't bump.
            return floor == null || target <= floor;
        };
    }
}
