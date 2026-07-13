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
package org.openrewrite.maven.internal.engine;

import lombok.Value;
import org.jspecify.annotations.Nullable;
import org.openrewrite.maven.engine.shaded.org.apache.maven.model.building.ModelBuildingResult;
import org.openrewrite.maven.tree.MavenRepository;
import org.openrewrite.maven.tree.ResolvedGroupArtifactVersion;

import java.util.Map;

/**
 * What {@link EngineEffectivePom#build} hands B2: either the shaded {@link ModelBuildingResult} (effective model + raw
 * lineage via {@code getModelIds}/{@code getRawModel} + {@code getActivePomProfiles}) or the mapped failure — a
 * {@link org.openrewrite.maven.MavenDownloadingException} for an unresolvable parent/BOM, a
 * {@link org.openrewrite.maven.internal.MavenParsingException} for a Maven-fatal validation problem (cycles,
 * self-parents), per {@link ModelParityErrorMapper}. Exactly one of {@code result}/{@code failure} is non-null.
 * {@code servedBy} carries the {@code gav → repository} attribution for whatever was resolved.
 */
@Value
public class EngineModelBuildingOutcome {

    @Nullable ModelBuildingResult result;

    @Nullable Throwable failure;

    Map<ResolvedGroupArtifactVersion, MavenRepository> servedBy;

    public boolean isSuccess() {
        return result != null;
    }

    public static EngineModelBuildingOutcome success(ModelBuildingResult result,
                                                     Map<ResolvedGroupArtifactVersion, MavenRepository> servedBy) {
        return new EngineModelBuildingOutcome(result, null, servedBy);
    }

    public static EngineModelBuildingOutcome failure(Throwable failure,
                                                     Map<ResolvedGroupArtifactVersion, MavenRepository> servedBy) {
        return new EngineModelBuildingOutcome(null, failure, servedBy);
    }
}
