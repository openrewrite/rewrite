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

import lombok.EqualsAndHashCode;
import lombok.Value;
import org.openrewrite.*;
import org.openrewrite.gradle.search.FindGradleProject;
import org.openrewrite.internal.ListUtils;
import org.openrewrite.internal.lang.Nullable;
import org.openrewrite.properties.ChangePropertyValue;
import org.openrewrite.properties.PropertiesParser;

import java.nio.file.Paths;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;

import static java.util.Collections.singletonList;

@Value
@EqualsAndHashCode(callSuper = false)
public class AddProperty extends Recipe {

    @Option(displayName = "Property name",
            description = "The name of the property to add.",
            example = "org.gradle.caching")
    String key;

    @Option(displayName = "Property value",
            description = "The value of the property to add.",
            example = "true")
    String value;

    @Option(displayName = "Overwrite if exists",
            description = "If a property with the same key exists, overwrite.",
            example = "Enable the Gradle build cache")
    @Nullable
    Boolean overwrite;

    @Override
    public String getDisplayName() {
        return "Add Gradle property";
    }

    @Override
    public String getDescription() {
        return "Add a property to the `gradle.properties` file.";
    }

    @Override
    protected TreeVisitor<?, ExecutionContext> getApplicableTest() {
        return new FindGradleProject(FindGradleProject.SearchCriteria.Marker).getVisitor();
    }

    @Override
    protected List<SourceFile> visit(List<SourceFile> before, ExecutionContext ctx) {
        AtomicBoolean exists = new AtomicBoolean();
        List<SourceFile> after = ListUtils.map(before, sourceFile -> {
            if (sourceFile.getSourcePath().endsWith(Paths.get("gradle.properties"))) {
                exists.set(true);
                Tree t = !Boolean.TRUE.equals(overwrite) ?
                        sourceFile :
                        new ChangePropertyValue(key, value, null, null, null)
                                .getVisitor().visitNonNull(sourceFile, ctx);
                return (SourceFile) new org.openrewrite.properties.AddProperty(key, value, null, null)
                        .getVisitor()
                        .visitNonNull(t, ctx);
            }
            return sourceFile;
        });

        if (!exists.get()) {
            after = ListUtils.concatAll(before, PropertiesParser.builder().build()
                    .parseInputs(singletonList(Parser.Input.fromString(Paths.get("gradle.properties"),
                            key + "=" + value)), null, ctx));
        }

        return after;
    }
}
