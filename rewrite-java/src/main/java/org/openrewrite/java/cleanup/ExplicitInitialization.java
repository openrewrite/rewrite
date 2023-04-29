/*
 * Copyright 2020 the original author or authors.
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
package org.openrewrite.java.cleanup;

import org.openrewrite.ExecutionContext;
import org.openrewrite.Recipe;
import org.openrewrite.SourceFile;
import org.openrewrite.TreeVisitor;
import org.openrewrite.java.JavaIsoVisitor;
import org.openrewrite.java.style.Checkstyle;
import org.openrewrite.java.style.ExplicitInitializationStyle;
import org.openrewrite.java.tree.JavaSourceFile;

import java.time.Duration;
import java.util.Collections;
import java.util.Set;

public class ExplicitInitialization extends Recipe {

    @Override
    public String getDisplayName() {
        return "Explicit initialization";
    }

    @Override
    public String getDescription() {
        return "Checks if any class or object member is explicitly initialized to default for its type value:\n" +
               "- `null` for object references\n" +
               "- zero for numeric types and `char`\n" +
               "- and `false` for `boolean`\n" +
               "Removes explicit initializations where they aren't necessary.";
    }

    @Override
    public Set<String> getTags() {
        return Collections.singleton("RSPEC-3052");
    }

    @Override
    public Duration getEstimatedEffortPerOccurrence() {
        return Duration.ofMinutes(2);
    }

    @Override
    public TreeVisitor<?, ExecutionContext> getVisitor() {
        return new JavaIsoVisitor<ExecutionContext>() {
            @Override
            public JavaSourceFile visitJavaSourceFile(JavaSourceFile cu, ExecutionContext ctx) {
                ExplicitInitializationStyle style = ((SourceFile) cu).getStyle(ExplicitInitializationStyle.class);
                if (style == null) {
                    style = Checkstyle.explicitInitialization();
                }
                doAfterVisit(new ExplicitInitializationVisitor<>(style));
                return cu;
            }
        };
    }
}
