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
package org.openrewrite.test;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.Accessors;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Accessors(fluent = true)
@Builder
public class TypeValidation {

    @Builder.Default
    private boolean classDeclarations = true;

    @Builder.Default
    private boolean identifiers = true;

    @Builder.Default
    private boolean methodDeclarations = true;

    @Builder.Default
    private boolean methodInvocations = true;

    @Builder.Default
    private boolean constructorInvocations = true;

    public static TypeValidation none() {
        return new TypeValidation(false,false,false,false,false);
    }
}
