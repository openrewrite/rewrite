/*
 * Copyright 2024 the original author or authors.
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
import org.jspecify.annotations.Nullable;
import org.openrewrite.*;
import org.openrewrite.groovy.GroovyIsoVisitor;
import org.openrewrite.java.tree.J;

@Value
@EqualsAndHashCode(callSuper = false)
public class RemoveEnableFeaturePreview extends Recipe {

    @Option(displayName = "The feature preview name",
            description = "The name of the feature preview to remove.",
            example = "ONE_LOCKFILE_PER_PROJECT")
    String previewFeatureName;

    @Override
    public String getDisplayName() {
        return "Remove an enabled Gradle preview feature";
    }

    @Override
    public String getDescription() {
        return "Remove an enabled Gradle preview feature from `settings.gradle`.";
    }

    @Override
    public TreeVisitor<?, ExecutionContext> getVisitor() {
        return Preconditions.check(new IsSettingsGradle<>(), new GroovyIsoVisitor<ExecutionContext>() {

            @Override
            public  J.@Nullable MethodInvocation visitMethodInvocation(J.MethodInvocation method, ExecutionContext ctx) {
                if ("enableFeaturePreview".equals(method.getSimpleName()) &&
                    method.getArguments().size() == 1 &&
                    J.Literal.isLiteralValue(method.getArguments().get(0), previewFeatureName)) {
                    return null;
                }
                return method;
            }
        });
    }
}
