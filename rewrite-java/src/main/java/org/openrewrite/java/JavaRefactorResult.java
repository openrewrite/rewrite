/*
 * Copyright 2020 the original authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.openrewrite.java;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.openrewrite.RefactorResult;
import org.openrewrite.java.tree.J;

import java.io.Serializable;
import java.util.Set;

@RequiredArgsConstructor
public class JavaRefactorResult implements RefactorResult, Serializable {
    private final J.CompilationUnit original;

    @Getter
    private final J.CompilationUnit fixed;

    @Getter
    private final Set<String> rulesThatMadeChanges;

    @Override
    public String getSourcePath() {
        return original.getSourcePath();
    }

    @Override
    public String getOriginalSource() {
        return original.print();
    }

    @Override
    public String getFixedSource() {
        return fixed.print();
    }
}
