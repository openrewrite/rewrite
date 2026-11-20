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
package org.openrewrite.coldfusion.format;

import lombok.EqualsAndHashCode;
import lombok.Value;
import org.openrewrite.ExecutionContext;
import org.openrewrite.Recipe;
import org.openrewrite.TreeVisitor;

@Value
@EqualsAndHashCode(callSuper = false)
public class AutoFormat extends Recipe {

    @Override
    public String getDisplayName() {
        return "Format ColdFusion code";
    }

    @Override
    public String getDescription() {
        return "Format CFML, in script and in tags, with the style detected for the repository or CFFormat's defaults. " +
               "Whitespace that a template writes to its output stays as it is.";
    }

    @Override
    public TreeVisitor<?, ExecutionContext> getVisitor() {
        return new AutoFormatVisitor<>(null);
    }
}
