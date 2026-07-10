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
package org.openrewrite.maven.parity;

import org.junit.jupiter.api.Test;
import org.openrewrite.ExecutionContext;
import org.openrewrite.InMemoryExecutionContext;
import org.openrewrite.SourceFile;
import org.openrewrite.maven.MavenExecutionContextView;
import org.openrewrite.maven.MavenParser;
import org.openrewrite.maven.cache.InMemoryMavenPomCache;
import org.openrewrite.maven.tree.MavenResolutionResult;
import org.openrewrite.maven.tree.ResolvedDependency;
import org.openrewrite.maven.tree.Scope;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Pins the compile-scope widening fix: a coordinate declared {@code runtime} on its nearest edge but
 * {@code compile} via another path must land in the compile classpath, because Maven selects a coordinate's effective
 * scope as the widest across every path to its winning version. {@code io.grpc:grpc-core:1.60.1} declares
 * {@code error_prone_annotations:2.20.0} at {@code runtime} directly (nearest, depth 1) while {@code grpc-api} declares
 * it {@code compile} (depth 2); {@code mvn dependency:tree} keeps it {@code compile} ("scope not updated to compile"),
 * as does the legacy engine. The mapper previously read the raw declared {@code runtime} scope and dropped it from the
 * compile classpath.
 */
class CompileScopeWideningTest {

    @Test
    void wideningToCompileMatchesMaven() {
        ExecutionContext ctx = new InMemoryExecutionContext(Throwable::printStackTrace);
        ctx.putMessage("org.openrewrite.maven.resolution.engine", "maven");
        MavenExecutionContextView.view(ctx).setPomCache(new InMemoryMavenPomCache());
        SourceFile sf = MavenParser.builder().build().parse(ctx,
                "<project><groupId>org.probe</groupId><artifactId>app</artifactId><version>1</version>" +
                        "<dependencies><dependency><groupId>io.grpc</groupId><artifactId>grpc-core</artifactId>" +
                        "<version>1.60.1</version></dependency></dependencies></project>").findFirst().orElseThrow();
        MavenResolutionResult resolution = sf.getMarkers().findFirst(MavenResolutionResult.class).orElseThrow();
        assertThat(resolution.getDependencies().get(Scope.Compile))
                .extracting(ResolvedDependency::getArtifactId)
                .contains("error_prone_annotations");
    }
}
