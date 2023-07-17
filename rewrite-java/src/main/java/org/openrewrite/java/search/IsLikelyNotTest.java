/*
 * Copyright 2023 the original author or authors.
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
package org.openrewrite.java.search;

import org.openrewrite.*;

@Incubating(since = "7.36.0")
public class IsLikelyNotTest extends Recipe {
    @Override
    public String getDisplayName() {
        return "Find files that are likely not tests";
    }

    @Override
    public String getDescription() {
        return "Sources that do not contain indicators of being, or being exclusively for the use in tests. " +
                "This recipe is simply a negation of the `" + IsLikelyTest.class.getName() + "` recipe.";
    }

    @Override
    public TreeVisitor<?, ExecutionContext> getVisitor() {
        return Preconditions.not(new IsLikelyTest().getVisitor());
    }
}
