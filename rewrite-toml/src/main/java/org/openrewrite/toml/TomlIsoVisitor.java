/*
 * Copyright 2025 the original author or authors.
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
package org.openrewrite.toml;

import org.openrewrite.toml.tree.Toml;

public class TomlIsoVisitor<P> extends TomlVisitor<P> {

    @Override
    public Toml.Array visitArray(Toml.Array array, P p) {
        return (Toml.Array) super.visitArray(array, p);
    }

    @Override
    public Toml.Document visitDocument(Toml.Document document, P p) {
        return (Toml.Document) super.visitDocument(document, p);
    }

    @Override
    public Toml.Identifier visitIdentifier(Toml.Identifier identifier, P p) {
        return (Toml.Identifier) super.visitIdentifier(identifier, p);
    }

    @Override
    public Toml.KeyValue visitKeyValue(Toml.KeyValue keyValue, P p) {
        return (Toml.KeyValue) super.visitKeyValue(keyValue, p);
    }

    @Override
    public Toml.Literal visitLiteral(Toml.Literal literal, P p) {
        return (Toml.Literal) super.visitLiteral(literal, p);
    }
}
