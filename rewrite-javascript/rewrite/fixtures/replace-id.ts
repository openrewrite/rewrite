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
import {createDraft, finishDraft} from "immer";
import {ExecutionContext, Markers, randomId, Recipe, TreeVisitor} from "@openrewrite/rewrite";
import {JavaScriptVisitor} from "@openrewrite/rewrite/javascript";
import {J} from "@openrewrite/rewrite/java";

export class ReplaceId extends Recipe {
    name = "org.openrewrite.example.javascript.replace-id"
    displayName = "Replace IDs";
    description = "Replaces the ID of every `Tree` and `Marker` object in a JavaScript source.";

    get editor(): TreeVisitor<any, ExecutionContext> {
        return new class extends JavaScriptVisitor<ExecutionContext> {
            protected async preVisit(tree: J, _p: ExecutionContext): Promise<J | undefined> {
                let draft = createDraft(tree);
                draft.id = randomId();
                return finishDraft(draft);
            }

            protected async visitMarkers(markers: Markers, p: ExecutionContext): Promise<Markers> {
                let draft = createDraft(markers);
                draft.id = randomId();
                return super.visitMarkers(finishDraft(draft), p);
            }
        }
    }
}
