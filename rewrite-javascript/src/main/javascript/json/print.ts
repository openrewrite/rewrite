import {PrintOutputCapture, TreePrinters} from "../print";
import {JsonVisitor} from "./visitor";
import {
    Empty,
    Identifier,
    Json,
    JsonArray,
    JsonDocument,
    JsonKind,
    JsonObject,
    JsonRightPadded,
    Literal,
    Member,
    Space
} from "./tree";
import {Cursor} from "../tree";
import {Markers} from "../markers";

class JsonPrinter extends JsonVisitor<PrintOutputCapture> {
    protected async visitArray(array: JsonArray, p: PrintOutputCapture): Promise<Json | undefined> {
        this.beforeSyntax(array, p);
        p.append('[');
        await this.visitRightPaddedWithSuffix(array.values, ",", p);
        p.append(']');
        this.afterSyntax(array, p);
        return array;
    }

    protected async visitDocument(document: JsonDocument, p: PrintOutputCapture): Promise<Json | undefined> {
        this.beforeSyntax(document, p);
        await this.visit(document.value, p);
        await this.visitSpace(document.eof, p);
        this.afterSyntax(document, p);
        return document;
    }

    protected async visitEmpty(empty: Empty, p: PrintOutputCapture): Promise<Json | undefined> {
        this.beforeSyntax(empty, p);
        this.afterSyntax(empty, p);
        return empty;
    }

    protected async visitIdentifier(identifier: Identifier, p: PrintOutputCapture): Promise<Json | undefined> {
        this.beforeSyntax(identifier, p);
        p.append(identifier.name);
        this.afterSyntax(identifier, p);
        return identifier;
    }

    protected async visitLiteral(literal: Literal, p: PrintOutputCapture): Promise<Json | undefined> {
        this.beforeSyntax(literal, p);
        p.append(literal.source);
        this.afterSyntax(literal, p);
        return literal;
    }

    protected async visitMember(member: Member, p: PrintOutputCapture): Promise<Json | undefined> {
        this.beforeSyntax(member, p);
        await this.visitRightPadded(member.key, p);
        p.append(':');
        await this.visit(member.value, p);
        this.afterSyntax(member, p);
        return member;
    }

    protected async visitObject(jsonObject: JsonObject, p: PrintOutputCapture): Promise<Json | undefined> {
        this.beforeSyntax(jsonObject, p);
        p.append('{');
        await this.visitRightPaddedWithSuffix(jsonObject.members, ",", p);
        p.append('}');
        this.afterSyntax(jsonObject, p);
        return jsonObject;
    }

    protected async visitSpace(space: Space, p: PrintOutputCapture): Promise<Space> {
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

    private async visitRightPaddedWithSuffix(nodes: JsonRightPadded<Json>[], suffixBetween: string, p: PrintOutputCapture): Promise<void> {
        for (let i = 0; i < nodes.length; i++) {
            const node = nodes[i];
            await this.visit(node.element, p);
            await this.visitSpace(node.after, p);
            if (i < nodes.length - 1) {
                p.append(suffixBetween);
            }
        }
    }

    private beforeSyntax(json: Json, p: PrintOutputCapture): void {
        this.beforeSyntaxWithMarkers(json.prefix, json.markers, p);
    }

    private beforeSyntaxWithMarkers(prefix: Space, markers: Markers, p: PrintOutputCapture): void {
        for (const marker of markers.markers) {
            p.append(p.markerPrinter.beforePrefix(marker, new Cursor(marker, this.cursor), this.jsonMarkerWrapper));
        }
        this.visitSpace(prefix, p);
        this.visitMarkers(markers, p);
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

TreePrinters.register(JsonKind.Document, new JsonPrinter());
