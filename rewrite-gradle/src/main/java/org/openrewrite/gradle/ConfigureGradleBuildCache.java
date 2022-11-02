/*
 * Copyright 2022 the original author or authors.
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

import org.openrewrite.ExecutionContext;
import org.openrewrite.Parser;
import org.openrewrite.Recipe;
import org.openrewrite.SourceFile;
import org.openrewrite.internal.ListUtils;
import org.openrewrite.properties.AddProperty;
import org.openrewrite.properties.PropertiesParser;

import java.nio.file.Paths;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;

import static java.util.Collections.singletonList;

public class ConfigureGradleBuildCache extends Recipe {

    @Override
    public String getDisplayName() {
        return "Enable Gradle build cache";
    }

    @Override
    public String getDescription() {
        return "The Gradle build cache is a cache mechanism that aims to save time by reusing outputs produced by other builds. The build cache works by storing (locally or remotely) build " +
               "outputs and allowing builds to fetch these outputs from the cache when it is determined that inputs have not changed, avoiding the expensive work of regenerating them. See " +
               "[build cache](https://docs.gradle.org/current/userguide/build_cache.html) docs.";
    }

//    @Override
//    protected TreeVisitor<?, ExecutionContext> getApplicableTest() {
//        return new FindGradleProject().getVisitor();
//    }

    @Override
    protected List<SourceFile> visit(List<SourceFile> before, ExecutionContext ctx) {
        AtomicBoolean exists = new AtomicBoolean();
        List<SourceFile> after = ListUtils.map(before, sourceFile -> {
            if (sourceFile.getSourcePath().endsWith(Paths.get("gradle.properties"))) {
                exists.set(true);
                return (SourceFile) new AddProperty("org.gradle.caching", "true", null)
                        .getVisitor()
                        .visitNonNull(sourceFile, ctx);
            }
            return sourceFile;
        });

        if (!exists.get()) {
            after = ListUtils.concatAll(before, PropertiesParser.builder().build()
                            .parseInputs(singletonList(Parser.Input.fromString(Paths.get("gradle.properties"),
                                    "org.gradle.caching=true")), null, ctx));
        }

        return after;
    }
}
