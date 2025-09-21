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
import {PrintOutputCapture, TreePrinters} from "../print";
import {JsonVisitor} from "./visitor";
import {Json} from "./tree";
import {Cursor} from "../tree";
import {Markers} from "../markers";

class JsonPrinter extends JsonVisitor<PrintOutputCapture> {
    protected async visitArray(array: Json.Array, p: PrintOutputCapture): Promise<Json | undefined> {
        await this.beforeSyntax(array, p);
        p.append('[');
        await this.visitRightPaddedWithSuffix(array.values, ",", p);
        p.append(']');
        this.afterSyntax(array, p);
        return array;
    }

    protected async visitDocument(document: Json.Document, p: PrintOutputCapture): Promise<Json | undefined> {
        await this.beforeSyntax(document, p);
        await this.visit(document.value, p);
        await this.visitSpace(document.eof, p);
        this.afterSyntax(document, p);
        return document;
    }

    protected async visitEmpty(empty: Json.Empty, p: PrintOutputCapture): Promise<Json | undefined> {
        await this.beforeSyntax(empty, p);
        this.afterSyntax(empty, p);
        return empty;
    }

    protected async visitIdentifier(identifier: Json.Identifier, p: PrintOutputCapture): Promise<Json | undefined> {
        await this.beforeSyntax(identifier, p);
        p.append(identifier.name);
        this.afterSyntax(identifier, p);
        return identifier;
    }

    protected async visitLiteral(literal: Json.Literal, p: PrintOutputCapture): Promise<Json | undefined> {
        await this.beforeSyntax(literal, p);
        p.append(literal.source);
        this.afterSyntax(literal, p);
        return literal;
    }

    protected async visitMember(member: Json.Member, p: PrintOutputCapture): Promise<Json | undefined> {
        await this.beforeSyntax(member, p);
        await this.visitRightPadded(member.key, p);
        p.append(':');
        await this.visit(member.value, p);
        this.afterSyntax(member, p);
        return member;
    }

    protected async visitObject(jsonObject: Json.Object, p: PrintOutputCapture): Promise<Json | undefined> {
        await this.beforeSyntax(jsonObject, p);
        p.append('{');
        await this.visitRightPaddedWithSuffix(jsonObject.members, ",", p);
        p.append('}');
        this.afterSyntax(jsonObject, p);
        return jsonObject;
    }

    public async visitSpace(space: Json.Space, p: PrintOutputCapture): Promise<Json.Space> {
        p.append(space.whitespace);
        for (const comment of space.comments) {
            await this.visitMarkers(comment.markers, p);
            if (comment.multiline) {
                p.append(`/*${comment.text}*/`);
            } else {
                p.append(`//${comment.text}`);
            }
            p.append(comment.suffix);
        }
        return space;
    }

    private async visitRightPaddedWithSuffix(nodes: Json.RightPadded<Json>[], suffixBetween: string, p: PrintOutputCapture): Promise<void> {
        for (let i = 0; i < nodes.length; i++) {
            const node = nodes[i];
            await this.visit(node.element, p);
            await this.visitSpace(node.after, p);
            if (i < nodes.length - 1) {
                p.append(suffixBetween);
            }
        }
    }

    private async beforeSyntax(json: Json, p: PrintOutputCapture): Promise<void> {
        await this.beforeSyntaxWithMarkers(json.prefix, json.markers, p);
    }

    private async beforeSyntaxWithMarkers(prefix: Json.Space, markers: Markers, p: PrintOutputCapture): Promise<void> {
        for (const marker of markers.markers) {
            p.append(p.markerPrinter.beforePrefix(marker, new Cursor(marker, this.cursor), this.jsonMarkerWrapper));
        }
        await this.visitSpace(prefix, p);
        await this.visitMarkers(markers, p);
        for (const marker of markers.markers) {
            p.append(p.markerPrinter.beforeSyntax(marker, new Cursor(marker, this.cursor), this.jsonMarkerWrapper));
        }
    }

    private afterSyntax(json: Json, p: PrintOutputCapture): void {
        this.afterSyntaxWithMarkers(json.markers, p);
    }

    private afterSyntaxWithMarkers(markers: Markers, p: PrintOutputCapture): void {
        for (const marker of markers.markers) {
            p.append(p.markerPrinter.afterSyntax(marker, new Cursor(marker, this.cursor), this.jsonMarkerWrapper));
        }
    }

    private jsonMarkerWrapper = (out: string): string => `/*~~${out}${out ? "~~" : ""}*/`;
}

TreePrinters.register(Json.Kind.Document, () => new JsonPrinter());
