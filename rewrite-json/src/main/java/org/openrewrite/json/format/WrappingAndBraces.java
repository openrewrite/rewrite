/*
 * Copyright 2025 the original author or authors.
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
package org.openrewrite.json.format;

import org.openrewrite.ExecutionContext;
import org.openrewrite.Recipe;
import org.openrewrite.json.style.WrappingAndBracesStyle;

public class WrappingAndBraces extends Recipe {
    @Override
    public String getDisplayName() {
        return "JSON new lines";
    }

    @Override
    public String getDescription() {
        return "Split members into separate lines in JSON.";
    }

    @Override
    public WrappingAndBracesVisitor<ExecutionContext> getVisitor() {
        return new WrappingAndBracesVisitor<>(WrappingAndBracesStyle.DEFAULT, null);
    }
}
