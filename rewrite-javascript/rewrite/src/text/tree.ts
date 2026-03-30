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
import {SourceFile, Tree, TreeKind} from "../tree";

export interface PlainText extends SourceFile {
    readonly kind: typeof PlainText.Kind.PlainText
    readonly text: string
    readonly snippets: PlainText.Snippet[]
}

export namespace PlainText {
    export const Kind = {
        PlainText: "org.openrewrite.text.PlainText",
        Snippet: "org.openrewrite.text.PlainText$Snippet"
    } as const

    export interface Snippet extends Tree {
        readonly kind: typeof PlainText.Kind.Snippet
        readonly text: string
    }
}

const textKindValues = new Set(Object.values(PlainText.Kind));

export function isPlainText(tree: any): tree is PlainText {
    return textKindValues.has(tree["kind"]);
}
