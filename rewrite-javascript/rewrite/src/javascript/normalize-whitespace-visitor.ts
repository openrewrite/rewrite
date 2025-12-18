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
import {JavaScriptVisitor} from "./visitor";
import {J} from "../java";
import {Cursor, isScope, Tree} from "../tree";

/**
 * Ensures that whitespace is on the outermost AST element possible.
 * Called NormalizeFormat in Java.
 */
export class NormalizeWhitespaceVisitor<P> extends JavaScriptVisitor<P> {
    constructor(private stopAfter?: Tree) {
        super();
    }

    override async visit<R extends J>(tree: Tree, p: P, parent?: Cursor): Promise<R | undefined> {
        if (this.cursor?.getNearestMessage("stop") != null) {
            return tree as R;
        }
        return super.visit(tree, p, parent);
    }

    override async postVisit(tree: J, p: P): Promise<J | undefined> {
        if (this.stopAfter != null && isScope(this.stopAfter, tree)) {
            this.cursor?.root.messages.set("stop", true);
        }
        return super.postVisit(tree, p);
    }
}
