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
package org.openrewrite.properties;

import org.openrewrite.properties.tree.Properties;

public class PropertiesIsoVisitor<P> extends PropertiesVisitor<P> {

    @Override
    public Properties.Comment visitComment(Properties.Comment comment, P p) {
        return (Properties.Comment) super.visitComment(comment, p);
    }

    @Override
    public Properties.File visitFile(Properties.File file, P p) {
        return (Properties.File) super.visitFile(file, p);
    }

    @Override
    public Properties.Entry visitEntry(Properties.Entry entry, P p) {
        return (Properties.Entry) super.visitEntry(entry, p);
    }
}
