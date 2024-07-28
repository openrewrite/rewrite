/*
 * Copyright 2021 the original author or authors.
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
package org.openrewrite.java;

import lombok.EqualsAndHashCode;
import lombok.Value;
import org.openrewrite.*;
import org.openrewrite.internal.lang.Nullable;
import org.openrewrite.java.search.DeclaresMethod;
import org.openrewrite.java.tree.J;

@Value
@EqualsAndHashCode(callSuper = false)
public class ChangeMethodAccessLevel extends Recipe {

    @Option(displayName = "Method pattern",
            description = "A method pattern that is used to find matching method declarations/invocations.",
            example = "org.mockito.Matchers anyVararg()")
    String methodPattern;

    @Option(displayName = "New access level",
            description = "New method access level to apply to the method.",
            example = "public",
            valid = {"private", "protected", "package", "public"})
    String newAccessLevel;

    @Option(displayName = "Match on overrides",
            description = "When enabled, find methods that are overrides of the method pattern.",
            required = false)
    @Nullable
    Boolean matchOverrides;

    @Override
    public String getInstanceNameSuffix() {
        return String.format("`%s` to `%s`", methodPattern, newAccessLevel);
    }

    @Override
    public String getDisplayName() {
        return "Change method access level";
    }

    @Override
    public String getDescription() {
        return "Change the access level (public, protected, private, package private) of a method.";
    }

    @Override
    public Validated<Object> validate() {
        return super.validate().and(Validated.test("newAccessLevel", "Must be one of 'private', 'protected', 'package', 'public'",
                newAccessLevel, level -> "private".equals(level) || "protected".equals(level) || "package".equals(level) || "public".equals(level)));
    }

    @Override
    public TreeVisitor<?, ExecutionContext> getVisitor() {
        J.Modifier.Type type;
        switch (newAccessLevel) {
            case "public":
                type = J.Modifier.Type.Public;
                break;
            case "protected":
                type = J.Modifier.Type.Protected;
                break;
            case "private":
                type = J.Modifier.Type.Private;
                break;
            default:
                type = null;
        }

        return Preconditions.check(new DeclaresMethod<>(methodPattern, matchOverrides),
                new ChangeMethodAccessLevelVisitor<>(new MethodMatcher(methodPattern, matchOverrides), type));
    }
}
