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
import {Cursor, Tree} from '../..';
import {J} from '../../java';
import {JS} from '../index';
import {JavaScriptSemanticComparatorVisitor} from '../comparator';
import {CaptureMarker, CaptureStorageValue, PlaceholderUtils} from './utils';

/**
 * A comparator for pattern matching that is lenient about optional properties.
 * Allows patterns without type annotations to match actual code with type annotations.
 * Uses semantic comparison to match semantically equivalent code (e.g., isDate() and util.isDate()).
 */
export class PatternMatchingComparator extends JavaScriptSemanticComparatorVisitor {
    constructor(
        private readonly matcher: {
            handleCapture: (capture: CaptureMarker, target: J, wrapper?: J.RightPadded<J>) => boolean;
            handleVariadicCapture: (capture: CaptureMarker, targets: J[], wrappers?: J.RightPadded<J>[]) => boolean;
            saveState: () => Map<string, CaptureStorageValue>;
            restoreState: (state: Map<string, CaptureStorageValue>) => void;
        },
        lenientTypeMatching: boolean = true
    ) {
        // Enable lenient type matching based on pattern configuration (default: true for backward compatibility)
        super(lenientTypeMatching);
    }

    override async visit<R extends J>(j: Tree, p: J, parent?: Cursor): Promise<R | undefined> {
        // Check if the pattern node is a capture - this handles unwrapped captures
        // (Wrapped captures in J.RightPadded are handled by visitRightPadded override)
        // Note: targetCursor will be pushed by parent's visit() method after this check
        const captureMarker = PlaceholderUtils.getCaptureMarker(j)!;
        if (captureMarker) {

            // Push targetCursor to position it at the captured node for constraint evaluation
            // Only create cursor if targetCursor was initialized (meaning user provided one)
            const savedTargetCursor = this.targetCursor;
            const cursorAtCapturedNode = this.targetCursor !== undefined
                ? new Cursor(p, this.targetCursor)
                : undefined;
            this.targetCursor = cursorAtCapturedNode;
            try {
                // Evaluate constraint with cursor at the captured node
                if (captureMarker.constraint && !captureMarker.constraint(p, cursorAtCapturedNode)) {
                    return this.abort(j) as R;
                }

                const success = this.matcher.handleCapture(captureMarker, p, undefined);
                if (!success) {
                    return this.abort(j) as R;
                }
                return j as R;
            } finally {
                this.targetCursor = savedTargetCursor;
            }
        }

        if (!this.match) {
            return j as R;
        }

        // Continue with parent's visit which will push targetCursor and traverse
        return await super.visit(j, p, parent);
    }

    protected hasSameKind(j: J, other: J): boolean {
        return super.hasSameKind(j, other) ||
            (j.kind == J.Kind.Identifier && PlaceholderUtils.isCapture(j as J.Identifier));
    }

    /**
     * Override visitRightPadded to check if this wrapper has a CaptureMarker.
     * If so, capture the entire wrapper (to preserve markers like semicolons).
     */
    override async visitRightPadded<T extends J | boolean>(right: J.RightPadded<T>, p: J): Promise<J.RightPadded<T>> {
        if (!this.match) {
            return right;
        }

        // Check if this RightPadded has a CaptureMarker (attached during pattern construction)
        // Note: Markers are now only at the wrapper level, not at the element level
        const captureMarker = PlaceholderUtils.getCaptureMarker(right);
        if (captureMarker) {
            // Extract the target wrapper if it's also a RightPadded
            const isRightPadded = (p as any).kind === J.Kind.RightPadded;
            const targetWrapper = isRightPadded ? (p as unknown) as J.RightPadded<T> : undefined;
            const targetElement = isRightPadded ? targetWrapper!.element : p;

            // Push targetCursor to position it at the captured element for constraint evaluation
            // Only create cursor if targetCursor was initialized (meaning user provided one)
            const savedTargetCursor = this.targetCursor;
            const cursorAtCapturedNode = this.targetCursor !== undefined
                ? (targetWrapper ? new Cursor(targetWrapper, this.targetCursor) : new Cursor(targetElement, this.targetCursor))
                : undefined;
            this.targetCursor = cursorAtCapturedNode;
            try {
                // Evaluate constraint with cursor at the captured node
                if (captureMarker.constraint && !captureMarker.constraint(targetElement as J, cursorAtCapturedNode)) {
                    return this.abort(right);
                }

                // Handle the capture with the wrapper - use the element for pattern matching
                const success = this.matcher.handleCapture(captureMarker, targetElement as J, targetWrapper as J.RightPadded<J> | undefined);
                if (!success) {
                    return this.abort(right);
                }
                return right;
            } finally {
                this.targetCursor = savedTargetCursor;
            }
        }

        // Not a capture wrapper - use parent implementation
        return super.visitRightPadded(right, p);
    }

    override async visitMethodInvocation(methodInvocation: J.MethodInvocation, other: J): Promise<J | undefined> {
        // Check if any arguments are variadic captures
        const hasVariadicCapture = methodInvocation.arguments.elements.some(arg =>
            PlaceholderUtils.isVariadicCapture(arg)
        );

        // If no variadic captures, use parent implementation (which includes semantic/type-aware matching)
        if (!hasVariadicCapture) {
            return super.visitMethodInvocation(methodInvocation, other);
        }

        // Otherwise, handle variadic captures ourselves
        if (!this.match || other.kind !== J.Kind.MethodInvocation) {
            return this.abort(methodInvocation);
        }

        const otherMethodInvocation = other as J.MethodInvocation;

        // Compare select
        if ((methodInvocation.select === undefined) !== (otherMethodInvocation.select === undefined)) {
            return this.abort(methodInvocation);
        }

        // Visit select if present
        if (methodInvocation.select && otherMethodInvocation.select) {
            await this.visit(methodInvocation.select.element, otherMethodInvocation.select.element);
            if (!this.match) return methodInvocation;
        }

        // Compare typeParameters
        if ((methodInvocation.typeParameters === undefined) !== (otherMethodInvocation.typeParameters === undefined)) {
            return this.abort(methodInvocation);
        }

        // Visit typeParameters if present
        if (methodInvocation.typeParameters && otherMethodInvocation.typeParameters) {
            if (methodInvocation.typeParameters.elements.length !== otherMethodInvocation.typeParameters.elements.length) {
                return this.abort(methodInvocation);
            }

            // Visit each type parameter in lock step (visit RightPadded to check for markers)
            for (let i = 0; i < methodInvocation.typeParameters.elements.length; i++) {
                await this.visitRightPadded(methodInvocation.typeParameters.elements[i], otherMethodInvocation.typeParameters.elements[i] as any);
                if (!this.match) return methodInvocation;
            }
        }

        // Visit name
        await this.visit(methodInvocation.name, otherMethodInvocation.name);
        if (!this.match) return methodInvocation;

        // Special handling for variadic captures in arguments
        if (!await this.matchArguments(methodInvocation.arguments.elements, otherMethodInvocation.arguments.elements)) {
            return this.abort(methodInvocation);
        }

        return methodInvocation;
    }

    override async visitBlock(block: J.Block, other: J): Promise<J | undefined> {
        // Check if any statements have CaptureMarker indicating they're variadic
        const hasVariadicCapture = block.statements.some(stmt => {
            const captureMarker = PlaceholderUtils.getCaptureMarker(stmt);
            return captureMarker?.variadicOptions !== undefined;
        });

        // If no variadic captures, use parent implementation
        if (!hasVariadicCapture) {
            return super.visitBlock(block, other);
        }

        // Otherwise, handle variadic captures ourselves
        if (!this.match || other.kind !== J.Kind.Block) {
            return this.abort(block);
        }

        const otherBlock = other as J.Block;

        // Special handling for variadic captures in statements
        if (!await this.matchSequence(block.statements, otherBlock.statements, false)) {
            return this.abort(block);
        }

        return block;
    }

    override async visitJsCompilationUnit(compilationUnit: JS.CompilationUnit, other: J): Promise<J | undefined> {
        // Check if any statements are variadic captures
        const hasVariadicCapture = compilationUnit.statements.some(stmt => {
            return PlaceholderUtils.isVariadicCapture(stmt);
        });

        // If no variadic captures, use parent implementation
        if (!hasVariadicCapture) {
            return super.visitJsCompilationUnit(compilationUnit, other);
        }

        // Otherwise, handle variadic captures ourselves
        if (!this.match || other.kind !== JS.Kind.CompilationUnit) {
            return this.abort(compilationUnit);
        }

        const otherCompilationUnit = other as JS.CompilationUnit;

        // Special handling for variadic captures in top-level statements
        if (!await this.matchSequence(compilationUnit.statements, otherCompilationUnit.statements, false)) {
            return this.abort(compilationUnit);
        }

        return compilationUnit;
    }

    /**
     * Matches argument lists, with special handling for variadic captures.
     * A variadic capture can match zero or more consecutive arguments.
     */
    private async matchArguments(patternArgs: J.RightPadded<J>[], targetArgs: J.RightPadded<J>[]): Promise<boolean> {
        return this.matchSequence(patternArgs, targetArgs, true);
    }

    /**
     * Generic sequence matching with variadic capture support.
     * Works for any sequence of JRightPadded elements (arguments, statements, etc.).
     * A variadic capture can match zero or more consecutive elements.
     *
     * Uses pivot detection to optimize matching, with backtracking as fallback.
     *
     * @param patternElements The pattern elements (JRightPadded)
     * @param targetElements The target elements to match against (JRightPadded)
     * @param filterEmpty Whether to filter out J.Empty elements when capturing (true for arguments, false for statements)
     * @returns true if the sequence matches, false otherwise
     */
    private async matchSequence(patternElements: J.RightPadded<J>[], targetElements: J.RightPadded<J>[], filterEmpty: boolean): Promise<boolean> {
        return await this.matchSequenceOptimized(patternElements, targetElements, 0, 0, filterEmpty);
    }

    /**
     * Optimized sequence matcher with pivot detection and backtracking.
     * For variadic patterns, tries to detect pivots (where next pattern matches) to avoid
     * unnecessary backtracking. Falls back to full backtracking when pivots are ambiguous.
     *
     * @param patternElements The pattern elements (JRightPadded)
     * @param targetElements The target elements to match against (JRightPadded)
     * @param patternIdx Current position in pattern
     * @param targetIdx Current position in target
     * @param filterEmpty Whether to filter out J.Empty elements when capturing
     * @returns true if the remaining sequence matches, false otherwise
     */
    private async matchSequenceOptimized(
        patternElements: J.RightPadded<J>[],
        targetElements: J.RightPadded<J>[],
        patternIdx: number,
        targetIdx: number,
        filterEmpty: boolean
    ): Promise<boolean> {
        // Base case: all patterns matched
        if (patternIdx >= patternElements.length) {
            return targetIdx >= targetElements.length; // Success if all targets consumed
        }

        // Check for markers at wrapper level only (markers are now only at the outermost level)
        const patternWrapper = patternElements[patternIdx];
        const captureMarker = PlaceholderUtils.getCaptureMarker(patternWrapper);
        const isVariadic = captureMarker?.variadicOptions !== undefined;

        if (isVariadic) {
            // Variadic pattern: try different consumption amounts with backtracking
            const variadicOptions = captureMarker!.variadicOptions;
            const min = variadicOptions?.min ?? 0;
            const max = variadicOptions?.max ?? Infinity;

            // Calculate maximum possible consumption
            let nonVariadicRemainingPatterns = 0;
            for (let i = patternIdx + 1; i < patternElements.length; i++) {
                const nextCaptureMarker = PlaceholderUtils.getCaptureMarker(patternElements[i]);
                const nextIsVariadic = nextCaptureMarker?.variadicOptions !== undefined;
                if (!nextIsVariadic) {
                    nonVariadicRemainingPatterns++;
                }
            }
            const remainingTargetElements = targetElements.length - targetIdx;
            const maxPossible = Math.min(remainingTargetElements - nonVariadicRemainingPatterns, max);

            // Pivot detection optimization: try to find where next pattern matches
            // This avoids unnecessary backtracking when constraints make the split point obvious
            let pivotDetected = false;
            let pivotAt = -1;

            if (patternIdx + 1 < patternElements.length && min <= maxPossible) {
                const nextPattern = patternElements[patternIdx + 1];

                // Scan through possible consumption amounts starting from min
                for (let tryConsume = min; tryConsume <= maxPossible; tryConsume++) {
                    // Check if element after our consumption would match next pattern
                    if (targetIdx + tryConsume < targetElements.length) {
                        const candidateElement = targetElements[targetIdx + tryConsume];

                        // Skip J.Empty for arguments
                        if (filterEmpty && candidateElement.element.kind === J.Kind.Empty) {
                            continue;
                        }

                        // Test if next pattern matches this element
                        const savedMatch = this.match;
                        const savedState = this.matcher.saveState();

                        await this.visitRightPadded(nextPattern, candidateElement as any);
                        const matchesNext = this.match;

                        this.match = savedMatch;
                        this.matcher.restoreState(savedState);

                        if (matchesNext) {
                            // Found pivot! Try this consumption amount first
                            pivotDetected = true;
                            pivotAt = tryConsume;
                            break;
                        }
                    }
                }
            }

            // Try different consumption amounts
            // If pivot detected, try that first; otherwise use greedy approach (max to min)
            const consumptionOrder: number[] = [];
            if (pivotDetected && pivotAt >= 0) {
                // Try pivot first, then others as fallback
                consumptionOrder.push(pivotAt);
                for (let c = maxPossible; c >= min; c--) {
                    if (c !== pivotAt) {
                        consumptionOrder.push(c);
                    }
                }
            } else {
                // Greedy approach: max to min
                for (let c = maxPossible; c >= min; c--) {
                    consumptionOrder.push(c);
                }
            }

            for (const consume of consumptionOrder) {
                // Capture elements for this consumption amount
                const capturedWrappers: J.RightPadded<J>[] = [];
                for (let i = 0; i < consume; i++) {
                    const wrapped = targetElements[targetIdx + i];
                    const element = wrapped.element;
                    // For arguments, filter out J.Empty as it represents an empty argument list
                    // For statements, include all elements
                    if (!filterEmpty || element.kind !== J.Kind.Empty) {
                        capturedWrappers.push(wrapped);
                    }
                }

                // Extract just the elements for the constraint check
                const capturedElements: J[] = capturedWrappers.map(w => w.element);

                // Re-check min/max constraints against actual captured elements (after filtering if applicable)
                if (capturedElements.length < min || capturedElements.length > max) {
                    continue; // Try next consumption amount
                }

                // Evaluate constraint for variadic capture
                // For variadic captures, constraint receives the entire array of captured elements
                // The targetCursor is positioned in the target tree (parent context)
                if (captureMarker.constraint) {
                    if (!captureMarker.constraint(capturedElements as any, this.targetCursor)) {
                        continue; // Try next consumption amount
                    }
                }

                // Save current state for backtracking
                const savedState = this.matcher.saveState();

                // Handle the variadic capture
                const success = this.matcher.handleVariadicCapture(captureMarker, capturedElements, capturedWrappers);
                if (!success) {
                    // Restore state and try next amount
                    this.matcher.restoreState(savedState);
                    continue;
                }

                // Try to match the rest of the pattern
                const restMatches = await this.matchSequenceOptimized(
                    patternElements,
                    targetElements,
                    patternIdx + 1,
                    targetIdx + consume,
                    filterEmpty
                );

                if (restMatches) {
                    return true; // Found a valid matching
                }

                // Backtrack: restore state and try next amount
                this.matcher.restoreState(savedState);
            }

            return false; // No consumption amount worked
        } else {
            // Regular non-variadic element - must match exactly one target element
            if (targetIdx >= targetElements.length) {
                return false; // Pattern has more elements than target
            }

            const targetWrapper = targetElements[targetIdx];
            const targetElement = targetWrapper.element;

            // For arguments, J.Empty represents no argument, so regular captures should not match it
            if (filterEmpty && targetElement.kind === J.Kind.Empty) {
                return false;
            }

            // Save current state for backtracking (both match state and capture bindings)
            const savedMatch = this.match;
            const savedState = this.matcher.saveState();

            await this.visitRightPadded(patternWrapper, targetWrapper as any);

            if (!this.match) {
                // Restore state on match failure
                this.match = savedMatch;
                this.matcher.restoreState(savedState);
                return false;
            }

            // Continue matching the rest
            const restMatches = await this.matchSequenceOptimized(
                patternElements,
                targetElements,
                patternIdx + 1,
                targetIdx + 1,
                filterEmpty
            );

            if (!restMatches) {
                // Restore full state on backtracking failure
                this.match = savedMatch;
                this.matcher.restoreState(savedState);
            }

            return restMatches;
        }
    }
}
