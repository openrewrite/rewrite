/*
 * Copyright 2026 the original author or authors.
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
import {
    ExecutionContext,
    foundSearchResult,
    JavaScript,
    Recipe,
    RecipeMarketplace,
    RecipeRef,
    ScanningRecipe,
    TreeVisitor
} from "@openrewrite/rewrite";
import {PlainText, PlainTextVisitor} from "@openrewrite/rewrite/text";
import {hasSourcePath} from "@openrewrite/rewrite/javascript";
import {prepareJavaRecipe} from "@openrewrite/rewrite/rpc";

export async function activate(marketplace: RecipeMarketplace) {
    await marketplace.install(ExclaimWhereGo, JavaScript);
}

/**
 * Edits, with its recipe list, only the text files that mention "go", which a scanning condition finds.
 */
class ExclaimWhereGo extends ScanningRecipe<{}> {
    name = "org.openrewrite.example.text.exclaim-where-go";
    displayName = "Exclaim where go";
    description = "Appends an exclamation mark to each text file that mentions go, and replaces go with went.";

    initialValue(): {} {
        return {};
    }

    async editPreconditions(): Promise<(Recipe | RecipeRef)[]> {
        return [new MentionsGo(), hasSourcePath("**/*.txt")];
    }

    async recipeList(): Promise<Recipe[]> {
        return [await prepareJavaRecipe("org.openrewrite.text.FindAndReplace", {find: "go", replace: "went"})];
    }

    async editorWithData(): Promise<TreeVisitor<any, ExecutionContext>> {
        return new class extends PlainTextVisitor<ExecutionContext> {
            override async visitText(text: PlainText): Promise<PlainText | undefined> {
                return {...text, text: text.text + "!"};
            }
        };
    }
}

class MentionsGo extends ScanningRecipe<Set<string>> {
    name = "org.openrewrite.example.text.mentions-go";
    displayName = "Mentions go";
    description = "Finds the text files that mention go.";

    initialValue(): Set<string> {
        return new Set();
    }

    async scanner(acc: Set<string>): Promise<TreeVisitor<any, ExecutionContext>> {
        return new class extends PlainTextVisitor<ExecutionContext> {
            override async visitText(text: PlainText): Promise<PlainText | undefined> {
                if (text.text.includes("go")) {
                    acc.add(text.sourcePath);
                }
                return text;
            }
        };
    }

    async editorWithData(acc: Set<string>): Promise<TreeVisitor<any, ExecutionContext>> {
        return new class extends PlainTextVisitor<ExecutionContext> {
            override async visitText(text: PlainText): Promise<PlainText | undefined> {
                return acc.has(text.sourcePath) ? foundSearchResult(text) : text;
            }
        };
    }
}
