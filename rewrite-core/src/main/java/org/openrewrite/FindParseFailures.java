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
import org.openrewrite.marker.Markup;

import java.util.Optional;

@Value
@EqualsAndHashCode(callSuper = true)
public class FindParseFailures extends Recipe {
    @Override
    public String getDisplayName() {
        return "Find source files with `ParseExceptionResult` markers";
    }

    @Override
    public String getDescription() {
        return "Find source files with `ParseExceptionResult` markers.";
    }

    @Override
    protected TreeVisitor<?, ExecutionContext> getVisitor() {
        return new TreeVisitor<Tree, ExecutionContext>() {
            @Override
            public @Nullable Tree visitSourceFile(SourceFile sourceFile, ExecutionContext executionContext) {
                Optional<ParseExceptionResult> parseExceptionResult = sourceFile.getMarkers().findFirst(ParseExceptionResult.class);
                if (parseExceptionResult.isPresent()) {
                    return Markup.error(sourceFile, parseExceptionResult.get().getMessage(), null);
                }
                return sourceFile;
            }
        };
    }
}
