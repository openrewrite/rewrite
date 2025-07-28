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
import {PlainTextVisitor} from "./visitor";
import {PlainText} from "./tree";
import {Cursor} from "../tree";
import {Markers} from "../markers";

class PlainTextPrinter extends PlainTextVisitor<PrintOutputCapture> {
    private static readonly TEXT_MARKER_WRAPPER = (out: string): string => `~~${out}${out ? "~~" : ""}>`;

    async visitText(text: PlainText, p: PrintOutputCapture): Promise<PlainText | undefined> {
        await this.visitMarkableText(text.markers, text.text, p);
        for (const snippet of text.snippets) {
            this.cursor = new Cursor(snippet, this.cursor);
            await this.visitSnippet(snippet, p);
            this.cursor = this.cursor.parent!;
        }
        return text;
    }

    async visitSnippet(snippet: PlainText.Snippet, p: PrintOutputCapture): Promise<PlainText.Snippet | undefined> {
        await this.visitMarkableText(snippet.markers, snippet.text, p);
        return snippet;
    }

    private async visitMarkableText(markers: Markers, text: string, p: PrintOutputCapture): Promise<void> {
        for (const marker of markers.markers) {
            p.append(p.markerPrinter.beforePrefix(marker, new Cursor(marker, this.cursor), PlainTextPrinter.TEXT_MARKER_WRAPPER));
        }
        await this.visitMarkers(markers, p);
        for (const marker of markers.markers) {
            p.append(p.markerPrinter.beforeSyntax(marker, new Cursor(marker, this.cursor), PlainTextPrinter.TEXT_MARKER_WRAPPER));
        }
        p.append(text);
        for (const marker of markers.markers) {
            p.append(p.markerPrinter.afterSyntax(marker, new Cursor(marker, this.cursor), PlainTextPrinter.TEXT_MARKER_WRAPPER));
        }
    }
}

TreePrinters.register(PlainText.Kind.PlainText, () => new PlainTextPrinter());
