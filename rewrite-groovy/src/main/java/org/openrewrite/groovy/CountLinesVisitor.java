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
package org.openrewrite.groovy;

import org.openrewrite.Tree;

// Originally created this thinking there would be a need for groovy-specific specialization, but so far the
// java line counter has been sufficient.
public class CountLinesVisitor extends org.openrewrite.java.CountLinesVisitor {

    public static int countLines(Tree tree) {
        return org.openrewrite.java.CountLinesVisitor.countLines(tree);
    }
}
