import {TreeVisitor} from "../visitor";
import {SourceFile, Tree} from "../tree";
import {mapAsync} from "../util";
import {isPlainText, PlainText, Snippet} from "./tree";

export class PlainTextVisitor<P> extends TreeVisitor<Tree, P> {
    isAcceptable(sourceFile: SourceFile): boolean {
        return isPlainText(sourceFile);
    }

    protected async visitText(text: PlainText, p: P): Promise<PlainText | undefined> {
        return this.produceTree<PlainText>(text, p, async draft => {
            draft.snippets = await mapAsync(text.snippets, snippet => this.visit(snippet, p));
        })
    }

    protected async visitSnippet(snippet: Snippet, p: P): Promise<Snippet | undefined> {
        return this.produceTree<Snippet>(snippet, p)
    }
}
