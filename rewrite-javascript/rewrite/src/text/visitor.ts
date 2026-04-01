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
import {TreeVisitor} from "../visitor";
import {SourceFile, Tree} from "../tree";
import {mapAsync} from "../util";
import {isPlainText, PlainText} from "./tree";

export class PlainTextVisitor<P> extends TreeVisitor<Tree, P> {
    async isAcceptable(sourceFile: SourceFile): Promise<boolean> {
        return isPlainText(sourceFile);
    }

    protected async visitText(text: PlainText, p: P): Promise<PlainText | undefined> {
        return this.produceTree<PlainText>(text, p, async draft => {
            draft.snippets = await mapAsync(text.snippets, snippet => this.visit(snippet, p));
        })
    }

    protected async visitSnippet(snippet: PlainText.Snippet, p: P): Promise<PlainText.Snippet | undefined> {
        return this.produceTree<PlainText.Snippet>(snippet, p)
    }

    protected async accept(t: Tree, p: P): Promise<Tree | undefined> {
        switch (t.kind) {
            case PlainText.Kind.PlainText:
                return this.visitText(t as PlainText, p);
            case PlainText.Kind.Snippet:
                return this.visitSnippet(t as PlainText.Snippet, p);
            default:
                throw new Error(`Unexpected text kind ${t.kind}`);
        }
    }
}
