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
package org.openrewrite.javascript.internal.lock.npm;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.LinkedHashSet;
import java.util.Set;

/**
 * Writes, to the file {@code REWRITE_LOCK_INVALID_EDGES} names, every edge that is missing or invalid
 * after loading each {@code after} lock {@link NpmScenarios} finds: the edges npm itself leaves behind,
 * which the ideal tree builder must know about. Not a check; a study.
 */
class NpmInvalidEdgesReport {

    @Test
    @EnabledIfEnvironmentVariable(named = "REWRITE_LOCK_INVALID_EDGES", matches = ".+")
    void write() throws IOException {
        // a scenario committed from the corpus is found under both roots and reported once
        Set<String> lines = new LinkedHashSet<>();
        lines.add("scenario\tlock\tfrom\ttype\tname\tspec\tto\terror");
        for (NpmScenarios.Scenario scenario : NpmScenarios.discover()) {
            for (NpmScenarios.Lock lock : scenario.locks()) {
                if (lock.isBefore() || lock.isInput() || NpmLockRoundTripTest.lockfileVersion(lock.getText()) < 2) {
                    continue;
                }
                String row = scenario.getName() + "\t" + lock.getFileName() + "\t";
                Node tree;
                try {
                    tree = NpmLockRoundTripTest.load(lock, NpmOptions.fromNpmrc(lock.getNpmrc()),
                      NpmLockRoundTripTest.projectPath(lock.getText()));
                } catch (RuntimeException e) {
                    lines.add(row + "\t\t\t\t\t" + "load failed: " + e.getMessage().replace('\n', ' '));
                    continue;
                }
                for (Edge edge : NpmLockRoundTripTest.problemEdges(tree)) {
                    Node to = edge.getTo();
                    lines.add(row + edge.getFrom().getLocation() + "\t" + edge.getType() + "\t" + edge.getName() + "\t" +
                              edge.getSpec() + "\t" + (to == null ? "" : to.getLocation()) + "\t" +
                              (edge.getError() == null ? "" : edge.getError()));
                }
            }
        }
        Files.write(Paths.get(System.getenv("REWRITE_LOCK_INVALID_EDGES")), String.join("\n", lines).concat("\n")
          .getBytes(StandardCharsets.UTF_8));
    }
}
