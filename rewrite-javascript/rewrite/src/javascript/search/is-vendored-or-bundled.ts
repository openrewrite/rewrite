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
import * as picomatch from "picomatch";
import {ExecutionContext} from "../../execution";
import {TreeVisitor} from "../../visitor";
import {foundSearchResult} from "../../markers";
import {J, TextComment} from "../../java";
import {JS} from "../tree";

const PATH_PATTERNS = [
    "**/node_modules/**",
    "**/bower_components/**",
    "**/vendor/**",
    "**/dist/**",
    "**/*.min.*",
    "**/*.bundle.*"
];

const SOURCE_MAPPING_URL = /^[#@] sourceMappingURL=/;

/**
 * Marks JavaScript and TypeScript sources that are vendored, bundled or build output.
 * In-process counterpart of {@code org.openrewrite.javascript.search.FindVendoredOrBundled};
 * keep the two in sync.
 */
export class IsVendoredOrBundled extends TreeVisitor<any, ExecutionContext> {
    private readonly matcher: picomatch.Matcher =
        picomatch.default ? picomatch.default(PATH_PATTERNS) : (picomatch as any)(PATH_PATTERNS);

    protected async preVisit(tree: any, _: ExecutionContext): Promise<any> {
        this.stopAfterPreVisit();
        if (tree.kind === JS.Kind.CompilationUnit && this.isVendoredOrBundled(tree as JS.CompilationUnit)) {
            return foundSearchResult(tree);
        }
        return tree;
    }

    private isVendoredOrBundled(cu: JS.CompilationUnit): boolean {
        if (this.matcher(cu.sourcePath.replace(/\\/g, '/'))) {
            return true;
        }
        const comments = cu.eof.comments;
        const last = comments[comments.length - 1];
        return last?.kind === J.Kind.TextComment && SOURCE_MAPPING_URL.test((last as TextComment).text.trim());
    }
}
