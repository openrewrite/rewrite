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
package org.openrewrite.maven.engine;

import org.eclipse.aether.RepositorySystem;
import org.eclipse.aether.RepositorySystemSession;
import org.eclipse.aether.artifact.DefaultArtifact;
import org.eclipse.aether.collection.CollectRequest;
import org.eclipse.aether.collection.CollectResult;
import org.eclipse.aether.graph.Dependency;
import org.eclipse.aether.graph.DependencyNode;
import org.eclipse.aether.repository.RemoteRepository;

import java.util.Collections;

import static org.junit.jupiter.api.Assertions.assertEquals;

/** Shared collect + graph-assertion helpers for the engine tests. */
final class EngineTests {

    private EngineTests() {
    }

    static CollectResult collect(RepositorySystem system, RepositorySystemSession session, RemoteRepository repo,
                                 String gav) throws Exception {
        CollectRequest request = new CollectRequest();
        request.setRoot(new Dependency(new DefaultArtifact(gav), ""));
        request.setRepositories(Collections.singletonList(repo));
        return system.collectDependencies(session, request);
    }

    /** Asserts the linear app -> lib-a -> lib-b chain served by {@link TinyMavenRepo}. */
    static void assertLinearGraph(CollectResult result) {
        DependencyNode app = result.getRoot();
        assertEquals("app", app.getArtifact().getArtifactId());
        assertEquals(1, app.getChildren().size(), "app should have exactly one dependency");
        DependencyNode libA = app.getChildren().get(0);
        assertEquals("lib-a", libA.getArtifact().getArtifactId());
        assertEquals(1, libA.getChildren().size(), "lib-a should have exactly one dependency");
        DependencyNode libB = libA.getChildren().get(0);
        assertEquals("lib-b", libB.getArtifact().getArtifactId());
        assertEquals(0, libB.getChildren().size(), "lib-b is a leaf");
    }
}
