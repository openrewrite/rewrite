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

export const PlainTextKind = {
    ...TreeKind,
    PlainText: "org.openrewrite.text.PlainText",
    Snippet: "org.openrewrite.text.PlainText$Snippet"
} as const

const textKindValues = new Set(Object.values(PlainTextKind));

export function isPlainText(tree: any): tree is PlainText {
    return textKindValues.has(tree["kind"]);
}

export interface PlainText extends SourceFile {
    readonly kind: typeof PlainTextKind.PlainText
    readonly text: string
    readonly snippets: Snippet[]
}

export interface Snippet extends Tree {
    readonly kind: typeof PlainTextKind.Snippet
    readonly text: string
}
