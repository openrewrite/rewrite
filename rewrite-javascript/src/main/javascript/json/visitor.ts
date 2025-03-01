import {mapAsync, produceAsync, SourceFile, TreeVisitor, ValidImmerRecipeReturnType} from "../";
import {
    Document,
    Empty,
    Identifier,
    isJson,
    Json,
    JsonArray,
    JsonKind,
    JsonObject,
    JsonRightPadded,
    Literal,
    Member,
    Space
} from "./tree";
import {createDraft, Draft, finishDraft} from "immer";

export class JsonVisitor<P> extends TreeVisitor<Json, P> {
    isAcceptable(sourceFile: SourceFile): boolean {
        return isJson(sourceFile);
    }

    protected async visitArray(array: JsonArray, p: P): Promise<Json | undefined> {
        return this.produceJson<JsonArray>(array, p, async draft => {
            draft.values = await mapAsync(array.values, value => this.visitRightPadded(value, p));
        })
    }

    protected async visitDocument(document: Document, p: P): Promise<Json | undefined> {
        return this.produceJson<Document>(document, p, async draft => {
            draft.value = await this.visitDefined(document.value, p);
            draft.eof = await this.visitSpace(document.eof, p);
        })
    }

    protected async visitEmpty(empty: Empty, p: P): Promise<Json | undefined> {
        return this.produceJson(empty, p)
    }

    protected async visitIdentifier(identifier: Identifier, p: P): Promise<Json | undefined> {
        return this.produceJson(identifier, p)
    }

    protected async visitLiteral(literal: Literal, p: P): Promise<Json | undefined> {
        return this.produceJson(literal, p)
    }

    protected async visitMember(member: Member, p: P): Promise<Json | undefined> {
        return this.produceJson<Member>(member, p, async draft => {
            draft.key = (await this.visitRightPadded(member.key, p))!;
            draft.value = await this.visitDefined(member.value, p);
        });
    }

    protected async visitObject(jsonObject: JsonObject, p: P): Promise<Json | undefined> {
        return this.produceJson<JsonObject>(jsonObject, p, async draft => {
            draft.members = await mapAsync(jsonObject.members, member => this.visitRightPadded(member, p));
        });
    }

    protected async visitRightPadded<T extends Json>(right: JsonRightPadded<T>, p: P):
        Promise<JsonRightPadded<T> | undefined> {
        return produceAsync<JsonRightPadded<T>>(right, async draft => {
            draft.element = await this.visitDefined(right.element, p);
            draft.after = await this.visitSpace(right.after, p)
        });
    }

    protected async visitSpace(space: Space, p: P): Promise<Space> {
        return space;
    }

    protected async produceJson<J extends Json>(
        before: J,
        p: P,
        recipe?: (draft: Draft<J>) =>
            ValidImmerRecipeReturnType<Draft<J>> |
            PromiseLike<ValidImmerRecipeReturnType<Draft<J>>>
    ): Promise<J> {
        const draft: Draft<J> = createDraft(before);
        (draft as Draft<Json>).prefix = await this.visitSpace(before.prefix, p);
        (draft as Draft<Json>).markers = await this.visitMarkers(before.markers, p);
        if (recipe) {
            await recipe(draft);
        }
        return finishDraft(draft) as J;
    }

    protected accept(t: Json, p: P): Promise<Json | undefined> {
        switch (t.kind) {
            case JsonKind.Array:
                return this.visitArray(t as JsonArray, p);
            case JsonKind.Document:
                return this.visitDocument(t as Document, p);
            case JsonKind.Empty:
                return this.visitEmpty(t as Empty, p);
            case JsonKind.Identifier:
                return this.visitIdentifier(t as Identifier, p);
            case JsonKind.Literal:
                return this.visitLiteral(t as Literal, p);
            case JsonKind.Member:
                return this.visitMember(t as Member, p);
            case JsonKind.Object:
                return this.visitObject(t as JsonObject, p);
            default:
                throw new Error(`Unexpected JSON kind ${t.kind}`);
        }
    }
}
