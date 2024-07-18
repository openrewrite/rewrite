/*
 * Copyright 2024 the original author or authors.
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
package org.openrewrite.semver;

import org.openrewrite.Validated;
import org.openrewrite.internal.StringUtils;
import org.openrewrite.internal.lang.Nullable;

public class ExactVersionWithPattern extends LatestRelease {
    String version;

    public ExactVersionWithPattern(String version, String metadataPattern) {
        super(metadataPattern);
        this.version = version;
    }

    @Override
    public boolean isValid(@Nullable String currentVersion, String version) {
        return super.isValid(currentVersion, version) &&
                super.compare(currentVersion, version, this.version) == 0;
    }

    public static Validated<ExactVersionWithPattern> build(String toVersion, @Nullable String metadataPattern) {
        if (StringUtils.isNullOrEmpty(metadataPattern)) {
            return Validated.invalid("exactVersionWithPattern", metadataPattern, "metadataPattern is null or empty");
        }
        Validated<ExactVersion> exactVersionValidated = ExactVersion.build(toVersion);
        if (!exactVersionValidated.isValid()) {
            return Validated.invalid("exactVersionWithPattern", toVersion, "not an exact version");
        }
        return Validated.valid("exactVersionWithPattern", new ExactVersionWithPattern(toVersion, metadataPattern));
    }
}
