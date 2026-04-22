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
package org.openrewrite.maven.utilities;

import org.junit.jupiter.api.Test;
import org.openrewrite.InMemoryExecutionContext;

import static org.assertj.core.api.Assertions.assertThat;

class JavaSourceSetUpdaterTest {

    @Test
    void typeCacheIsSharedAcrossUpdaters() {
        InMemoryExecutionContext ctx = new InMemoryExecutionContext();

        new JavaSourceSetUpdater(ctx);
        Object firstCache = ctx.getMessage(JavaSourceSetUpdater.TYPE_CACHE_KEY);

        new JavaSourceSetUpdater(ctx);
        Object secondCache = ctx.getMessage(JavaSourceSetUpdater.TYPE_CACHE_KEY);

        // Memoizing typesFromPath only pays off if every updater built from the same
        // ExecutionContext sees the same map — otherwise each visitor re-scans the JAR.
        assertThat(firstCache).isNotNull().isSameAs(secondCache);
    }
}
