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
import org.openrewrite.Option;
import org.openrewrite.Recipe;

@Value
@EqualsAndHashCode(callSuper = true)
public class RemoveImplements extends Recipe {
    @Override
    public String getDisplayName() {
        return "Remove interface implementations";
    }

    @Override
    public String getDescription() {
        return "Removes `implements` clauses from classes implementing the specified interface. " +
                "Removes `@Overrides` annotations from methods which no longer override anything.";
    }

    @Option(displayName = "Interface Type",
            description = "The fully qualified name of the interface to remove.",
            example = "java.io.Serializable")
    String interfaceType;

    @Option(displayName = "Filter",
            description = "Only apply the interface removal to classes with fully qualified names that begin with this filter. " +
                    "Supplying the fully qualified name of a class limits ",
            example = "com.yourorg")
    String filter;
}
