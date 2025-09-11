/*
 * Copyright 2025 the original author or authors.
 * <p>
 * Licensed under the Moderne Source Available License (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * <p>
 * https://docs.moderne.io/licensing/moderne-source-available-license
 * <p>
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
import {mapAsync} from "../util";
import {produceAsync, TreeVisitor, ValidImmerRecipeReturnType} from "../visitor";
import {SourceFile} from "../tree";
import {isJson, Json} from "./tree";
import {createDraft, Draft, finishDraft} from "immer";

export class JsonVisitor<P> extends TreeVisitor<Json, P> {
    async isAcceptable(sourceFile: SourceFile): Promise<boolean> {
        return isJson(sourceFile);
    }

    protected async visitArray(array: Json.Array, p: P): Promise<Json | undefined> {
        return this.produceJson<Json.Array>(array, p, async draft => {
            draft.values = await mapAsync(array.values, value => this.visitRightPadded(value, p));
        })
    }

    protected async visitDocument(document: Json.Document, p: P): Promise<Json | undefined> {
        return this.produceJson<Json.Document>(document, p, async draft => {
            draft.value = await this.visitDefined(document.value, p);
            draft.eof = await this.visitSpace(document.eof, p);
        })
    }

    protected async visitEmpty(empty: Json.Empty, p: P): Promise<Json | undefined> {
        return this.produceJson(empty, p)
    }

    protected async visitIdentifier(identifier: Json.Identifier, p: P): Promise<Json | undefined> {
        return this.produceJson(identifier, p)
    }

    protected async visitLiteral(literal: Json.Literal, p: P): Promise<Json | undefined> {
        return this.produceJson(literal, p)
    }

    protected async visitMember(member: Json.Member, p: P): Promise<Json | undefined> {
        return this.produceJson<Json.Member>(member, p, async draft => {
            draft.key = (await this.visitRightPadded(member.key, p))!;
            draft.value = await this.visitDefined(member.value, p);
        });
    }

    protected async visitObject(jsonObject: Json.Object, p: P): Promise<Json | undefined> {
        return this.produceJson<Json.Object>(jsonObject, p, async draft => {
            draft.members = await mapAsync(jsonObject.members, member => this.visitRightPadded(member, p));
        });
    }

    protected async visitRightPadded<T extends Json>(right: Json.RightPadded<T>, p: P):
        Promise<Json.RightPadded<T> | undefined> {
        return produceAsync<Json.RightPadded<T>>(right, async draft => {
            draft.element = await this.visitDefined(right.element, p);
            draft.after = await this.visitSpace(right.after, p)
        });
    }

    protected async visitSpace(space: Json.Space, p: P): Promise<Json.Space> {
        return space;
    }

    protected async produceJson<J extends Json>(
        before: Json | undefined,
        p: P,
        recipe?: (draft: Draft<J>) =>
            ValidImmerRecipeReturnType<Draft<J>> |
            PromiseLike<ValidImmerRecipeReturnType<Draft<J>>>
    ): Promise<J | undefined> {
        if (before === undefined) {
            return undefined;
        }
        const draft: Draft<J> = createDraft(before as J);
        (draft as Draft<Json>).prefix = await this.visitSpace(before!.prefix, p);
        (draft as Draft<Json>).markers = await this.visitMarkers(before!.markers, p);
        if (recipe) {
            await recipe(draft);
        }
        return finishDraft(draft) as J;
    }

    protected accept(t: Json, p: P): Promise<Json | undefined> {
        switch (t.kind) {
            case Json.Kind.Array:
                return this.visitArray(t as Json.Array, p);
            case Json.Kind.Document:
                return this.visitDocument(t as Json.Document, p);
            case Json.Kind.Empty:
                return this.visitEmpty(t as Json.Empty, p);
            case Json.Kind.Identifier:
                return this.visitIdentifier(t as Json.Identifier, p);
            case Json.Kind.Literal:
                return this.visitLiteral(t as Json.Literal, p);
            case Json.Kind.Member:
                return this.visitMember(t as Json.Member, p);
            case Json.Kind.Object:
                return this.visitObject(t as Json.Object, p);
            default:
                throw new Error(`Unexpected JSON kind ${t.kind}`);
        }
    }
}
