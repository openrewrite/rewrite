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
package org.openrewrite.properties;

import org.openrewrite.RefactorVisitorSupport;
import org.openrewrite.Tree;
import org.openrewrite.properties.tree.Properties;

public class PropertiesRefactorVisitor extends PropertiesSourceVisitor<Properties> implements RefactorVisitorSupport {
    @Override
    public Properties defaultTo(Tree t) {
        return (Properties) t;
    }

    @Override
    public Properties visitFile(Properties.File file) {
        Properties.File f = refactor(file, super::visitFile);
        f = f.withContent(refactor(f.getContent()));
        return f;
    }
}
