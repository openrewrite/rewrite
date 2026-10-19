/*
 * Copyright 2026 the original author or authors.
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
package org.openrewrite.internal;

import org.openrewrite.SourceFile;

import java.util.List;

/**
 * Lets the test framework check that a recipe imports the types and static members the code it writes names.
 */
public interface ImportValidationService {

    /**
     * @return The types and static members {@code sourceFile} names that nothing in it imports or declares, such as
     * "`List` (java.util.List)".
     */
    List<String> unimported(SourceFile sourceFile);

    /**
     * Records, until closed, the types and static members that templates applied on this thread name without
     * declaring them, which resolve only through the imports of the file a template is applied to.
     */
    Recording recordUndeclared();

    interface Recording extends AutoCloseable {

        /**
         * @return Each name a template relied on the file to import, and where the template was applied from, such
         * as "`assertThat()` (org.assertj.core.api.Assertions) in a template applied at
         * com.example.MyRecipe$1.visitMethodInvocation(MyRecipe.java:42)".
         */
        List<String> getUndeclared();

        @Override
        void close();
    }
}
