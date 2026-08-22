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
package org.openrewrite.docker.trait;

import org.openrewrite.Cursor;
import org.openrewrite.docker.tree.Docker;

/**
 * The build stages a Dockerfile declares, as seen from somewhere inside it.
 */
final class Stages {

    private Stages() {
    }

    /// Whether `name` is the `AS` alias of a stage of the file the cursor stands in. Docker lowercases
    /// both the alias it records and the name it looks up, so `--from=Builder` finds `AS builder`.
    static boolean isDeclaredStage(Cursor cursor, String name) {
        Docker.File file = cursor.firstEnclosing(Docker.File.class);
        if (file == null) {
            return false;
        }
        for (Docker.Stage stage : file.getStages()) {
            Docker.From.As as = stage.getFrom().getAs();
            if (as != null && name.equalsIgnoreCase(as.getName().getText())) {
                return true;
            }
        }
        return false;
    }
}
