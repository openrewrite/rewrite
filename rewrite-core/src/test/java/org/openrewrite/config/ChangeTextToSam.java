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
package org.openrewrite.config;

import org.openrewrite.ExecutionContext;
import org.openrewrite.Option;
import org.openrewrite.Recipe;
import org.openrewrite.TreeVisitor;
import org.openrewrite.internal.lang.Nullable;
import org.openrewrite.text.PlainText;
import org.openrewrite.text.PlainTextVisitor;

@SuppressWarnings("unused") // Used in a yaml recipe
public class ChangeTextToSam extends Recipe {

    @Override
    public String getDisplayName() {
        return "Change text to Sam";
    }

    @Override
    public String getDescription() {
        return "Does the text file say Sam? It will after you run this recipe.";
    }

    @Option(displayName = "bool",
            description = "Exists to test that optional configuration may be omitted",
            required = false)
    @Nullable
    Boolean bool;

    @Override
    public TreeVisitor<?, ExecutionContext> getVisitor() {
        return new PlainTextVisitor<>() {
            @Override
            public PlainText visitText(PlainText text, ExecutionContext executionContext) {
                return text.withText("Sam");
            }
        };
    }
}
