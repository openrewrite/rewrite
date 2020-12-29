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
package org.openrewrite.refactor;

import org.openrewrite.SourceVisitor;

/**
 * Discover the most common indentation level of a tree, and whether this indentation is built with spaces or tabs.
 */
public interface FindIndent extends SourceVisitor<Void> {
    boolean isIndentedWithSpaces();
    int getMostCommonIndent();

    /**
     * @return The total number of source lines that this indent decision was made on.
     */
    int getTotalLines();
}
