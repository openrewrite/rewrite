/*
 * Copyright 2025 the original author or authors.
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
package org.openrewrite.gradle;

import lombok.EqualsAndHashCode;
import lombok.Value;
import org.jspecify.annotations.Nullable;
import org.openrewrite.*;

@Value
@EqualsAndHashCode(callSuper = false)
public class EnableDevelocityBuildCache extends Recipe {

    @Override
    public String getDisplayName() {
        return "Enable Develocity build cache";
    }

    @Override
    public String getDescription() {
        return "Add Develocity build cache configuration Develocity configuration given it lacks an existing configuration.";
    }

    @Option(displayName = "Enable local build cache",
            description = "Value for `//develocity/buildCache/local/enabled`.",
            example = "true",
            required = false)
    @Nullable
    String localEnabled;

    @Option(displayName = "Enable remote build cache",
            description = "Value for `//develocity/buildCache/remote/enabled`.",
            example = "true",
            required = false)
    @Nullable
    String remoteEnabled;

    @Option(displayName = "Enable remote build cache store",
            description = "Value for `//develocity/buildCache/remote/storeEnabled`.",
            example = "true",
            required = false)
    @Nullable
    String remoteStoreEnabled;

    @Override
    public Validated<Object> validate(ExecutionContext ctx) {
        return super.validate(ctx)
                .and(Validated.notBlank("localEnabled", localEnabled)
                        .or(Validated.notBlank("remoteEnabled", remoteEnabled))
                        .or(Validated.notBlank("remoteStoreEnabled", remoteStoreEnabled)));
    }


    @Override
    public TreeVisitor<?, ExecutionContext> getVisitor() {
        return super.getVisitor();
    }
}
