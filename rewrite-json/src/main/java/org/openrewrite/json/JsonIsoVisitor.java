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
package org.openrewrite.json;

import org.openrewrite.json.tree.Json;

public class JsonIsoVisitor<P> extends JsonVisitor<P> {

    @Override
    public Json.Array visitArray(Json.Array array, P p) {
        return (Json.Array) super.visitArray(array, p);
    }

    @Override
    public Json.Document visitDocument(Json.Document document, P p) {
        return (Json.Document) super.visitDocument(document, p);
    }

    @Override
    public Json.Empty visitEmpty(Json.Empty empty, P p) {
        return (Json.Empty) super.visitEmpty(empty, p);
    }

    @Override
    public Json.Identifier visitIdentifier(Json.Identifier identifier, P p) {
        return (Json.Identifier) super.visitIdentifier(identifier, p);
    }

    @Override
    public Json.Literal visitLiteral(Json.Literal literal, P p) {
        return (Json.Literal) super.visitLiteral(literal, p);
    }

    @Override
    public Json.Member visitMember(Json.Member member, P p) {
        return (Json.Member) super.visitMember(member, p);
    }

    @Override
    public Json.JsonObject visitObject(Json.JsonObject obj, P p) {
        return (Json.JsonObject) super.visitObject(obj, p);
    }
}
