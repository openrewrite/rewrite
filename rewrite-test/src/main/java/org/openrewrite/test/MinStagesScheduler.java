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
package org.openrewrite.test;

import org.openrewrite.RecipeScheduler;

/**
 * A {@link RecipeScheduler} that keeps running stages until at least {@code minStages} have run,
 * even after the recipe has otherwise converged. RewriteTest uses this to force the extra
 * idempotency stage(s) a test expects — a converged stage that must make no further change. The
 * minimum-stage concept is a testing concern and deliberately lives here rather than in the core
 * scheduler. It re-runs the run's root recipe (not a wrapper), so results attribution is unaffected.
 */
class MinStagesScheduler extends RecipeScheduler {
    private final int minStages;

    MinStagesScheduler(int minStages) {
        this.minStages = minStages;
    }

    @Override
    protected boolean runAdditionalStage(int stage, int maxStages) {
        return stage < minStages && stage < maxStages;
    }
}
