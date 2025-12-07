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
import {Option, Recipe} from '../recipe';
import {TreeVisitor} from '../visitor';
import {ExecutionContext} from '../execution';
import {SourceFile, Tree} from '../tree';
import {isParseError} from '../parse-error';
import {MarkersKind, ParseExceptionResult} from '../markers';
import {TreePrinters} from '../print';
import {createTwoFilesPatch} from 'diff';
import * as fs from 'fs';
import * as path from 'path';

/**
 * Callback for reporting validation results.
 * @param message The message to report
 */
export type ReportCallback = (message: string) => void;

/**
 * A recipe that validates parsing by:
 * 1. Reporting parse errors to stderr
 * 2. Checking parse-to-print idempotence and showing diffs for failures
 *
 * Use this recipe to validate that the parser correctly handles your codebase.
 */
export class ValidateParsingRecipe extends Recipe {
    name = 'org.openrewrite.validate-parsing';
    displayName = 'Validate parsing';
    description = 'Validates that all source files parse correctly and that parse-to-print is idempotent. Reports parse errors and shows diffs for idempotence failures.';

    @Option({
        displayName: 'Project root',
        description: 'The root directory of the project, used to resolve source file paths for idempotence checking.',
        required: false
    })
    projectRoot!: string;

    /**
     * Optional callback for reporting messages. If not set, uses console.log.
     */
    onReport?: ReportCallback;

    private parseErrorCount = 0;
    private idempotenceFailureCount = 0;

    private report(message: string): void {
        if (this.onReport) {
            this.onReport(message);
        } else {
            console.log(message);
        }
    }

    async editor(): Promise<TreeVisitor<any, ExecutionContext>> {
        const recipe = this;
        return new class extends TreeVisitor<Tree, ExecutionContext> {
            async visit<R extends Tree>(tree: Tree | undefined, ctx: ExecutionContext): Promise<R | undefined> {
                if (!tree) return undefined;

                const sourceFile = tree as SourceFile;

                // Check for parse errors
                if (isParseError(sourceFile)) {
                    const parseException = sourceFile.markers.markers.find(
                        m => m.kind === MarkersKind.ParseExceptionResult
                    ) as ParseExceptionResult | undefined;
                    const message = parseException?.message ?? 'Unknown parse error';
                    recipe.report(`Parse error in ${sourceFile.sourcePath}: ${message}`);
                    recipe.parseErrorCount++;
                    return tree as R;
                }

                // Check parse-to-print idempotence
                try {
                    const printed = await TreePrinters.print(sourceFile);
                    const originalPath = path.join(recipe.projectRoot, sourceFile.sourcePath);
                    const original = fs.readFileSync(originalPath, 'utf-8');

                    if (printed !== original) {
                        recipe.idempotenceFailureCount++;
                        recipe.report(`Parse-to-print idempotence failure in ${sourceFile.sourcePath}:`);

                        // Generate and print diff
                        const diff = createTwoFilesPatch(
                            sourceFile.sourcePath,
                            sourceFile.sourcePath,
                            original,
                            printed,
                            'original',
                            'printed',
                            {context: 3}
                        );
                        recipe.report(diff);
                    }
                } catch (e: any) {
                    recipe.idempotenceFailureCount++;
                    recipe.report(`Failed to check idempotence for ${sourceFile.sourcePath}: ${e.message}`);
                }

                return tree as R;
            }
        };
    }

    async onComplete(_ctx: ExecutionContext): Promise<void> {
        if (this.parseErrorCount > 0 || this.idempotenceFailureCount > 0) {
            this.report('');
            if (this.parseErrorCount > 0) {
                this.report(`${this.parseErrorCount} file(s) had parse errors.`);
            }
            if (this.idempotenceFailureCount > 0) {
                this.report(`${this.idempotenceFailureCount} file(s) had parse-to-print idempotence failures.`);
            }
        }
    }

    get hasErrors(): boolean {
        return this.parseErrorCount > 0 || this.idempotenceFailureCount > 0;
    }
}
