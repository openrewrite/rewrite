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
package org.openrewrite;

import lombok.EqualsAndHashCode;
import lombok.Value;
import org.openrewrite.internal.lang.Nullable;
import org.openrewrite.quark.Quark;

@Value
@EqualsAndHashCode(callSuper = true)
public class FindQuarks extends Recipe {
    @Option(displayName = "Include all Quarks",
            description = "Flag to include all instances of type `Quark`, instead of only those with parser failures. Defaults to false.",
            required = false)
    @Nullable
    Boolean includeAll;

    @Override
    public String getDisplayName() {
        return "Find instances of type `Quark`";
    }

    @Override
    public String getDescription() {
        return "Find instances of type `Quark`.";
    }

    @Override
    protected TreeVisitor<?, ExecutionContext> getVisitor() {
        return new TreeVisitor<Tree, ExecutionContext>() {
            @Override
            public Tree visitSourceFile(SourceFile sourceFile, ExecutionContext executionContext) {
                if (sourceFile instanceof Quark &&
                        ((includeAll != null && includeAll) || sourceFile.getMarkers().findFirst(ParseExceptionResult.class).isPresent())) {
                    return sourceFile.withMarkers(sourceFile.getMarkers().searchResult());
                }
                return sourceFile;
            }
        };
    }
}
