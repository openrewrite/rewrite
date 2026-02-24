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
import {getPaddedElement, isRightPadded, J} from '../../java';
import {JS} from '../index';
import {JavaScriptSemanticComparatorVisitor} from '../comparator';
import {CaptureMarker, CaptureStorageValue, PlaceholderUtils} from './utils';
import {Capture, CaptureConstraintContext, CaptureMap, DebugLogEntry, MatchExplanation} from './types';
import {CAPTURE_NAME_SYMBOL} from './capture';

/**
 * Debug callbacks for pattern matching.
 * These are always used together - either all present or all absent.
 * Part of Layer 1 (Core Instrumentation).
 */
export interface DebugCallbacks {
    log: (level: DebugLogEntry['level'], scope: DebugLogEntry['scope'], message: string, data?: any) => void;
    setExplanation: (reason: MatchExplanation['reason'], expected: string, actual: string, details?: string) => void;
    getExplanation: () => MatchExplanation | undefined;
    restoreExplanation: (explanation: MatchExplanation) => void;
    clearExplanation: () => void;
    pushPath: (name: string) => void;
    popPath: () => void;
}

/**
 * Snapshot of matcher state for backtracking.
 * Includes both capture storage and debug state.
 */
export interface MatcherState {
    storage: Map<string, CaptureStorageValue>;
    debugState?: {
        explanation?: MatchExplanation;
        logLength: number;
        path: string[];
    };
}

/**
 * Callbacks for the matcher (debug and capture handling).
 * Part of Layer 1 (Core Instrumentation).
 */
export interface MatcherCallbacks {
    handleCapture: (capture: CaptureMarker, target: J, wrapper?: J.RightPadded<J>) => boolean;
    handleVariadicCapture: (capture: CaptureMarker, targets: J[], wrappers?: J.RightPadded<J>[]) => boolean;
    saveState: () => MatcherState;
    restoreState: (state: MatcherState) => void;

    // Debug callbacks - either all present (when debugging enabled) or absent
    debug?: DebugCallbacks;
}

/**
 * Implementation of CaptureMap that wraps the capture storage.
 * Provides read-only access to previously matched captures.
 */
class CaptureMapImpl implements CaptureMap {
    constructor(private readonly storage: Map<string, CaptureStorageValue>) {}

    get<T>(capture: Capture<T>): T | undefined;
    get(capture: string): any;
    get(capture: Capture | string): any {
        // Use symbol to get internal name without triggering Proxy
        const name = typeof capture === 'string' ? capture : ((capture as any)[CAPTURE_NAME_SYMBOL] || capture.getName());
        return this.storage.get(name);
    }

    has(capture: Capture | string): boolean {
        // Use symbol to get internal name without triggering Proxy
        const name = typeof capture === 'string' ? capture : ((capture as any)[CAPTURE_NAME_SYMBOL] || capture.getName());
        return this.storage.has(name);
    }
}

/**
 * A comparator for pattern matching that is lenient about optional properties.
 * Allows patterns without type annotations to match actual code with type annotations.
 * Uses semantic comparison to match semantically equivalent code (e.g., isDate() and util.isDate()).
 */
export class PatternMatchingComparator extends JavaScriptSemanticComparatorVisitor {
    constructor(
        protected readonly matcher: MatcherCallbacks,
        lenientTypeMatching: boolean = true
    ) {
        // Enable lenient type matching based on pattern configuration (default: true for backward compatibility)
        super(lenientTypeMatching);
    }

    /**
     * Builds the constraint context with the cursor and current captures.
     * @param cursor The cursor to include in the context
     * @returns The constraint context for evaluating capture constraints
     */
    protected buildConstraintContext(cursor: Cursor): CaptureConstraintContext {
        const state = this.matcher.saveState();
        return {
            cursor,
            captures: new CaptureMapImpl(state.storage)
        };
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
                : new Cursor(p);
            this.targetCursor = cursorAtCapturedNode;
            try {
                // Evaluate constraint with context (cursor + previous captures)
                // Skip constraint for variadic captures - they're evaluated in matchSequence with the full array
                if (captureMarker.constraint && !captureMarker.variadicOptions) {
                    const context = this.buildConstraintContext(cursorAtCapturedNode);
                    if (!captureMarker.constraint(p, context)) {
                        const captureName = captureMarker.captureName || 'unnamed';
                        const targetKind = (p as any).kind || 'unknown';
                        return this.constraintFailed(captureName, targetKind) as R;
                    }
                }

                const success = this.matcher.handleCapture(captureMarker, p, undefined);
                if (!success) {
                    const captureName = captureMarker.captureName || 'unnamed';
                    return this.captureConflict(captureName) as R;
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
     * Additional specialized abort methods for pattern matching scenarios.
     */

    protected constraintFailed(captureName: string, targetKind: string) {
        const pattern = this.cursor?.value as any;
        return this.abort(pattern, 'constraint-failed', `capture[${captureName}]`, 'constraint satisfied', `constraint failed for ${targetKind}`);
    }

    protected captureConflict(captureName: string) {
        const pattern = this.cursor?.value as any;
        return this.abort(pattern, 'capture-conflict', `capture[${captureName}]`, 'compatible binding', 'conflicting binding');
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
        const captureMarker = PlaceholderUtils.getCaptureMarkerFromPadded(right);
        if (captureMarker) {
            // Extract the target wrapper if it's also a RightPadded
            // With intersection types, RightPadded elements have `padding` property but no `element`
            const isRightPaddedOther = isRightPadded(p);
            const targetWrapper = isRightPaddedOther ? (p as unknown) as J.RightPadded<T> : undefined;
            // For tree types, the padded value IS the element (use getPaddedElement helper)
            const targetElement = isRightPaddedOther ? getPaddedElement(targetWrapper!) : p;

            // Push targetCursor to position it at the captured element for constraint evaluation
            const savedTargetCursor = this.targetCursor;
            const cursorAtCapturedNode = this.targetCursor !== undefined
                ? (targetWrapper ? new Cursor(targetWrapper, this.targetCursor) : new Cursor(targetElement, this.targetCursor))
                : (targetWrapper ? new Cursor(targetWrapper) : new Cursor(targetElement));
            this.targetCursor = cursorAtCapturedNode;
            try {
                // Evaluate constraint with cursor at the captured node (always defined)
                // Skip constraint for variadic captures - they're evaluated in matchSequence with the full array
                if (captureMarker.constraint && !captureMarker.variadicOptions && !captureMarker.constraint(targetElement as J, this.buildConstraintContext(cursorAtCapturedNode))) {
                    const captureName = captureMarker.captureName || 'unnamed';
                    const targetKind = (targetElement as any).kind || 'unknown';
                    return this.constraintFailed(captureName, targetKind);
                }

                // Handle the capture with the wrapper - use the element for pattern matching
                const success = this.matcher.handleCapture(captureMarker, targetElement as J, targetWrapper as J.RightPadded<J> | undefined);
                if (!success) {
                    const captureName = captureMarker.captureName || 'unnamed';
                    return this.captureConflict(captureName);
                }
                return right;
            } finally {
                this.targetCursor = savedTargetCursor;
            }
        }

        // Not a capture wrapper - use parent implementation
        return super.visitRightPadded(right, p);
    }

    override async visitContainer<T extends J>(container: J.Container<T>, p: J): Promise<J.Container<T>> {
        // Check if any elements are variadic captures
        const hasVariadicCapture = container.elements.some(elem =>
            PlaceholderUtils.isVariadicCaptureFromPadded(elem)
        );

        // If no variadic captures, use parent implementation
        if (!hasVariadicCapture) {
            return super.visitContainer(container, p);
        }

        // Otherwise, handle variadic captures ourselves
        if (!this.match) {
            return container;
        }

        // Extract the other container
        const isContainer = (p as any).kind === J.Kind.Container;
        if (!isContainer) {
            // Set up cursors temporarily for kindMismatch to use
            const savedCursor = this.cursor;
            const savedTargetCursor = this.targetCursor;
            this.cursor = new Cursor(container, this.cursor);
            this.targetCursor = new Cursor(p, this.targetCursor);
            try {
                return this.kindMismatch();
            } finally {
                this.cursor = savedCursor;
                this.targetCursor = savedTargetCursor;
            }
        }
        const otherContainer = p as unknown as J.Container<T>;

        // Push wrappers onto both cursors
        const savedCursor = this.cursor;
        const savedTargetCursor = this.targetCursor;
        this.cursor = new Cursor(container, this.cursor);
        this.targetCursor = new Cursor(otherContainer, this.targetCursor);
        try {
            // Use matchSequence for variadic matching
            // filterEmpty=true to skip J.Empty elements (they represent missing elements in destructuring)
            if (!await this.matchSequence(container.elements as J.RightPadded<J>[], otherContainer.elements as J.RightPadded<J>[], true)) {
                return this.structuralMismatch('elements');
            }
        } finally {
            this.cursor = savedCursor;
            this.targetCursor = savedTargetCursor;
        }

        return container;
    }

    /**
     * Visit a single element in a container (for non-variadic matching).
     * Extracted to allow debug subclass to add path tracking.
     *
     * @param element The pattern element
     * @param otherElement The target element
     * @param index The index in the container
     * @returns true if matching should continue, false if it failed
     */
    protected async visitContainerElement<T extends J>(
        element: J.RightPadded<T>,
        otherElement: J.RightPadded<T>,
        index: number
    ): Promise<boolean> {
        await this.visitRightPadded(element as any, otherElement as any);
        return this.match;
    }

    override async visitMethodInvocation(methodInvocation: J.MethodInvocation, other: J): Promise<J | undefined> {
        // Check if any arguments are variadic captures
        const hasVariadicCapture = methodInvocation.arguments.elements.some(arg =>
            PlaceholderUtils.isVariadicCaptureFromPadded(arg)
        );

        // If no variadic captures, use parent implementation (which includes semantic/type-aware matching)
        if (!hasVariadicCapture) {
            return super.visitMethodInvocation(methodInvocation, other);
        }

        // Otherwise, handle variadic captures ourselves
        if (!this.match) {
            return this.abort(methodInvocation);
        }

        if (other.kind !== J.Kind.MethodInvocation) {
            // Set up cursors for kindMismatch
            const savedCursor = this.cursor;
            const savedTargetCursor = this.targetCursor;
            this.cursor = new Cursor(methodInvocation, this.cursor);
            this.targetCursor = new Cursor(other, this.targetCursor);
            try {
                return this.kindMismatch();
            } finally {
                this.cursor = savedCursor;
                this.targetCursor = savedTargetCursor;
            }
        }

        const otherMethodInvocation = other as J.MethodInvocation;

        // Set up cursors for the entire method
        const savedCursor = this.cursor;
        const savedTargetCursor = this.targetCursor;
        this.cursor = new Cursor(methodInvocation, this.cursor);
        this.targetCursor = new Cursor(otherMethodInvocation, this.targetCursor);
        try {
            // Compare select
            if ((methodInvocation.select === undefined) !== (otherMethodInvocation.select === undefined)) {
                return this.structuralMismatch('select');
            }

            // Visit select if present
            // For tree types, the padded value IS the element (use getPaddedElement helper)
            if (methodInvocation.select && otherMethodInvocation.select) {
                await this.visit(getPaddedElement(methodInvocation.select), getPaddedElement(otherMethodInvocation.select));
                if (!this.match) return methodInvocation;
            }

            // Compare typeParameters
            if ((methodInvocation.typeParameters === undefined) !== (otherMethodInvocation.typeParameters === undefined)) {
                return this.structuralMismatch('typeParameters');
            }

            // Visit typeParameters if present
            if (methodInvocation.typeParameters && otherMethodInvocation.typeParameters) {
                if (methodInvocation.typeParameters.elements.length !== otherMethodInvocation.typeParameters.elements.length) {
                    return this.arrayLengthMismatch('typeParameters.elements');
                }

                // Visit each type parameter in lock step (visit RightPadded to check for markers)
                for (let i = 0; i < methodInvocation.typeParameters.elements.length; i++) {
                    await this.visitRightPadded(methodInvocation.typeParameters.elements[i], otherMethodInvocation.typeParameters.elements[i] as any);
                    if (!this.match) return methodInvocation;
                }
            }

            // Visit name
            await this.visit(methodInvocation.name, otherMethodInvocation.name);
            if (!this.match) {
                return methodInvocation;
            }

            // Special handling for variadic captures in arguments
            if (!await this.matchArguments(methodInvocation.arguments.elements, otherMethodInvocation.arguments.elements)) {
                return this.structuralMismatch('arguments');
            }

            return methodInvocation;
        } finally {
            this.cursor = savedCursor;
            this.targetCursor = savedTargetCursor;
        }
    }

    override async visitBlock(block: J.Block, other: J): Promise<J | undefined> {
        // Check if any statements have CaptureMarker indicating they're variadic
        const hasVariadicCapture = block.statements.some(stmt => {
            const captureMarker = PlaceholderUtils.getCaptureMarkerFromPadded(stmt);
            return captureMarker?.variadicOptions !== undefined;
        });

        // If no variadic captures, use parent implementation
        if (!hasVariadicCapture) {
            return super.visitBlock(block, other);
        }

        // Otherwise, handle variadic captures ourselves
        if (!this.match) {
            return this.abort(block);
        }

        if (other.kind !== J.Kind.Block) {
            // Set up cursors for kindMismatch
            const savedCursor = this.cursor;
            const savedTargetCursor = this.targetCursor;
            this.cursor = new Cursor(block, this.cursor);
            this.targetCursor = new Cursor(other, this.targetCursor);
            try {
                return this.kindMismatch();
            } finally {
                this.cursor = savedCursor;
                this.targetCursor = savedTargetCursor;
            }
        }

        const otherBlock = other as J.Block;

        // Set up cursors for structural comparison
        const savedCursor = this.cursor;
        const savedTargetCursor = this.targetCursor;
        this.cursor = new Cursor(block, this.cursor);
        this.targetCursor = new Cursor(otherBlock, this.targetCursor);
        try {
            // Special handling for variadic captures in statements
            if (!await this.matchSequence(block.statements, otherBlock.statements, false)) {
                return this.structuralMismatch('statements');
            }

            return block;
        } finally {
            this.cursor = savedCursor;
            this.targetCursor = savedTargetCursor;
        }
    }

    override async visitJsCompilationUnit(compilationUnit: JS.CompilationUnit, other: J): Promise<J | undefined> {
        // Check if any statements are variadic captures
        const hasVariadicCapture = compilationUnit.statements.some(stmt => {
            return PlaceholderUtils.isVariadicCaptureFromPadded(stmt);
        });

        // If no variadic captures, use parent implementation
        if (!hasVariadicCapture) {
            return super.visitJsCompilationUnit(compilationUnit, other);
        }

        // Otherwise, handle variadic captures ourselves
        if (!this.match) {
            return this.abort(compilationUnit);
        }

        if (other.kind !== JS.Kind.CompilationUnit) {
            // Set up cursors for kindMismatch
            const savedCursor = this.cursor;
            const savedTargetCursor = this.targetCursor;
            this.cursor = new Cursor(compilationUnit, this.cursor);
            this.targetCursor = new Cursor(other, this.targetCursor);
            try {
                return this.kindMismatch();
            } finally {
                this.cursor = savedCursor;
                this.targetCursor = savedTargetCursor;
            }
        }

        const otherCompilationUnit = other as JS.CompilationUnit;

        // Set up cursors for structural comparison
        const savedCursor = this.cursor;
        const savedTargetCursor = this.targetCursor;
        this.cursor = new Cursor(compilationUnit, this.cursor);
        this.targetCursor = new Cursor(otherCompilationUnit, this.targetCursor);
        try {
            // Special handling for variadic captures in top-level statements
            if (!await this.matchSequence(compilationUnit.statements, otherCompilationUnit.statements, false)) {
                return this.structuralMismatch('statements');
            }

            return compilationUnit;
        } finally {
            this.cursor = savedCursor;
            this.targetCursor = savedTargetCursor;
        }
    }

    /**
     * Matches argument lists, with special handling for variadic captures.
     * A variadic capture can match zero or more consecutive arguments.
     */
    private async matchArguments(patternArgs: J.RightPadded<J>[], targetArgs: J.RightPadded<J>[]): Promise<boolean> {
        return await this.matchSequence(patternArgs, targetArgs, true);
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
    protected async matchSequence(patternElements: J.RightPadded<J>[], targetElements: J.RightPadded<J>[], filterEmpty: boolean): Promise<boolean> {
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
    protected async matchSequenceOptimized(
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
        const captureMarker = PlaceholderUtils.getCaptureMarkerFromPadded(patternWrapper);
        const isVariadic = captureMarker?.variadicOptions !== undefined;

        if (isVariadic) {
            // Variadic pattern: try different consumption amounts with backtracking
            const variadicOptions = captureMarker!.variadicOptions;
            const min = variadicOptions?.min ?? 0;
            const max = variadicOptions?.max ?? Infinity;

            // Calculate maximum possible consumption and check if remaining patterns are deterministic
            let nonVariadicRemainingPatterns = 0;
            let allRemainingPatternsAreDeterministic = true;
            for (let i = patternIdx + 1; i < patternElements.length; i++) {
                const nextCaptureMarker = PlaceholderUtils.getCaptureMarkerFromPadded(patternElements[i]);
                const nextIsVariadic = nextCaptureMarker?.variadicOptions !== undefined;
                if (!nextIsVariadic) {
                    nonVariadicRemainingPatterns++;
                }
                // A pattern is deterministic if it's not a capture at all (i.e., a literal/fixed structure)
                // Variadic captures and non-variadic captures are both non-deterministic
                if (nextCaptureMarker) {
                    allRemainingPatternsAreDeterministic = false;
                }
            }
            const remainingTargetElements = targetElements.length - targetIdx;
            const maxPossible = Math.min(remainingTargetElements - nonVariadicRemainingPatterns, max);

            // Pivot detection optimization: try to find where next pattern matches
            // This avoids unnecessary backtracking when constraints make the split point obvious
            let pivotDetected = false;
            let pivotAt = -1;

            // Skip pivot detection if we're using deterministic optimization
            // (when all remaining patterns are literals, there's only ONE valid consumption amount)
            const useDeterministicOptimization = allRemainingPatternsAreDeterministic && maxPossible >= min && maxPossible <= max;

            if (!useDeterministicOptimization && patternIdx + 1 < patternElements.length && min <= maxPossible) {
                const nextPattern = patternElements[patternIdx + 1];

                // Scan through possible consumption amounts starting from min
                for (let tryConsume = min; tryConsume <= maxPossible; tryConsume++) {
                    // Check if element after our consumption would match next pattern
                    if (targetIdx + tryConsume < targetElements.length) {
                        const candidateElement = targetElements[targetIdx + tryConsume];

                        // Skip J.Empty for arguments
                        if (filterEmpty && candidateElement.kind === J.Kind.Empty) {
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

            // Determine consumption order
            const consumptionOrder: number[] = [];

            // OPTIMIZATION: If all remaining patterns are deterministic (literals, not captures),
            // there's only ONE mathematically valid consumption amount. Skip backtracking entirely.
            // Example: foo(${args}, 999) matching foo(1,2,42) -> args MUST be [1,2], only try consume=2
            if (useDeterministicOptimization) {
                consumptionOrder.push(maxPossible);
            } else if (pivotDetected && pivotAt >= 0) {
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
                // For empty argument lists, there will be a single J.Empty element that we need to filter out
                const rawWrappers = targetElements.slice(targetIdx, targetIdx + consume);
                const capturedWrappers = filterEmpty
                    ? rawWrappers.filter(w => w.kind !== J.Kind.Empty)
                    : rawWrappers;
                const capturedElements: J[] = capturedWrappers.map(w => getPaddedElement(w as J.RightPadded<J>));

                // Check min/max constraints against filtered elements
                if (capturedElements.length < min || capturedElements.length > max) {
                    continue;
                }

                // Evaluate constraint for variadic capture
                // For variadic captures, constraint receives the entire array of captured elements
                // The targetCursor points to the parent container (always defined in container matching)
                if (captureMarker.constraint) {
                    const cursor = this.targetCursor || new Cursor(targetElements[0]);
                    if (!captureMarker.constraint(capturedElements as any, this.buildConstraintContext(cursor))) {
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
            // For tree types, the padded value IS the element
            const targetElement = getPaddedElement(targetWrapper as J.RightPadded<J>);

            // For arguments, J.Empty represents no argument, so regular captures should not match it
            if (filterEmpty && targetElement.kind === J.Kind.Empty) {
                return false;
            }

            if (!await this.visitSequenceElement(patternWrapper, targetWrapper, targetIdx)) {
                return false;
            }

            // Continue matching the rest
            return await this.matchSequenceOptimized(
                patternElements,
                targetElements,
                patternIdx + 1,
                targetIdx + 1,
                filterEmpty
            );
        }
    }

    /**
     * Visit a single element in a sequence during non-variadic matching.
     * Extracted to allow debug subclass to add path tracking.
     *
     * @param patternWrapper The pattern element
     * @param targetWrapper The target element
     * @param targetIdx The index in the target sequence
     * @returns true if matching succeeded, false otherwise
     */
    protected async visitSequenceElement(
        patternWrapper: J.RightPadded<J>,
        targetWrapper: J.RightPadded<J>,
        targetIdx: number
    ): Promise<boolean> {
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

        return true;
    }
}

/**
 * Debug-instrumented version of PatternMatchingComparator.
 * Overrides methods to add path tracking, logging, and explanation capture.
 * Zero cost when not instantiated - production code uses the base class.
 */
export class DebugPatternMatchingComparator extends PatternMatchingComparator {
    private get debug(): DebugCallbacks {
        return this.matcher.debug!;
    }

    /**
     * Extracts the last segment of a kind string (after the last dot).
     * For example: "org.openrewrite.java.tree.J.MethodInvocation" -> "MethodInvocation"
     */
    private formatKind(kind: string): string {
        return kind.substring(kind.lastIndexOf('.') + 1);
    }

    /**
     * Formats a value for display in error messages.
     */
    private formatValue(value: any): string {
        if (value === null) return 'null';
        if (value === undefined) return 'undefined';
        if (typeof value === 'string') return `"${value}"`;
        if (typeof value === 'number' || typeof value === 'boolean') return String(value);

        // For objects with a kind property (LST nodes)
        if (value && typeof value === 'object' && value.kind) {
            const kind = this.formatKind(value.kind);

            // Show key identifying properties for common node types
            if (value.simpleName) return `${kind}("${value.simpleName}")`;
            if (value.value !== undefined) return `${kind}(${this.formatValue(value.value)})`;

            return kind;
        }

        return String(value);
    }

    /**
     * Override abort to capture explanation when debug is enabled.
     * Only sets explanation on the first abort call (when this.match is still true).
     * This preserves the most specific explanation closest to the actual mismatch.
     */
    protected override abort<T>(t: T, reason?: string, propertyName?: string, expected?: any, actual?: any): T {
        // If already aborted, don't overwrite the explanation
        // The first abort is typically the most specific
        if (!this.match) {
            return t;
        }

        // If we have context about the mismatch, capture it
        if (reason && this.debug && (expected !== undefined || actual !== undefined)) {
            const expectedStr = this.formatValue(expected);
            const actualStr = this.formatValue(actual);

            this.debug.setExplanation(
                reason as any,
                expectedStr,
                actualStr,
                'Property values do not match'
            );
        }

        // Set `this.match = false`
        return super.abort(t, reason, propertyName, expected, actual);
    }

    /**
     * Override helper methods to extract detailed context from cursors.
     */

    protected override kindMismatch() {
        const pattern = this.cursor?.value as any;
        const target = this.targetCursor?.value as any;
        // Pass the full kind strings - formatValue() will detect and format them
        return this.abort(pattern, 'kind-mismatch', 'kind', this.formatKind(pattern?.kind), this.formatKind(target?.kind));
    }

    protected override structuralMismatch(propertyName: string) {
        const pattern = this.cursor?.value as any;
        const target = this.targetCursor?.value as any;
        const expectedValue = pattern?.[propertyName];
        const actualValue = target?.[propertyName];
        return this.abort(pattern, 'structural-mismatch', propertyName, expectedValue, actualValue);
    }

    protected override arrayLengthMismatch(propertyName: string) {
        const pattern = this.cursor?.value as any;
        const target = this.targetCursor?.value as any;
        const expectedArray = pattern?.[propertyName];
        const actualArray = target?.[propertyName];
        const expectedLen = Array.isArray(expectedArray) ? expectedArray.length : 'not an array';
        const actualLen = Array.isArray(actualArray) ? actualArray.length : 'not an array';
        return this.abort(pattern, 'array-length-mismatch', propertyName, expectedLen, actualLen);
    }

    protected override valueMismatch(propertyName?: string, expected?: any, actual?: any) {
        const pattern = this.cursor?.value as any;
        const target = this.targetCursor?.value as any;

        // Track number of paths pushed for cleanup
        let pathsPushed = 0;

        // Handle path tracking only if propertyName is provided
        if (propertyName) {
            // Split dotted property paths (e.g., "name.simpleName" → ["name", "simpleName"])
            const pathParts = propertyName.split('.');
            pathsPushed = pathParts.length;

            // Add each property to path with kind information for nested objects
            const kindStr = this.formatKind(pattern?.kind);
            this.debug.pushPath(`${kindStr}#${pathParts[0]}`);

            // For nested properties, try to get the kind of intermediate objects
            let currentObj = pattern?.[pathParts[0]];
            for (let i = 1; i < pathParts.length; i++) {
                if (currentObj && typeof currentObj === 'object' && currentObj.kind) {
                    // Include the kind of the nested object
                    const nestedKind = this.formatKind(currentObj.kind);
                    this.debug.pushPath(`${nestedKind}#${pathParts[i]}`);
                } else {
                    // Fallback to just the property name if no kind available
                    this.debug.pushPath(pathParts[i]);
                }
                currentObj = currentObj?.[pathParts[i]];
            }
        }

        try {
            // If expected/actual provided, use them directly
            if (expected !== undefined || actual !== undefined) {
                return this.abort(pattern, 'value-mismatch', propertyName, expected, actual);
            }

            // Otherwise, try to extract from cursors (fallback for older code)
            if (propertyName) {
                // Navigate dotted property paths
                const getNestedValue = (obj: any, path: string) => {
                    return path.split('.').reduce((current, prop) => current?.[prop], obj);
                };

                const expectedValue = getNestedValue(pattern, propertyName);
                const actualValue = getNestedValue(target, propertyName);
                return this.abort(pattern, 'value-mismatch', propertyName, expectedValue, actualValue);
            } else {
                // No property name - compare whole objects
                return this.abort(pattern, 'value-mismatch', propertyName, pattern, target);
            }
        } finally {
            // Pop all the path components we pushed
            for (let i = 0; i < pathsPushed; i++) {
                this.debug.popPath();
            }
        }
    }

    override async visit<R extends J>(j: Tree, p: J, parent?: Cursor): Promise<R | undefined> {
        const captureMarker = PlaceholderUtils.getCaptureMarker(j)!;
        if (captureMarker) {
            const savedTargetCursor = this.targetCursor;
            const cursorAtCapturedNode = this.targetCursor !== undefined
                ? new Cursor(p, this.targetCursor)
                : new Cursor(p);
            this.targetCursor = cursorAtCapturedNode;
            try {
                if (captureMarker.constraint && !captureMarker.variadicOptions) {
                    this.debug.log('debug', 'constraint', `Evaluating constraint for capture: ${captureMarker.captureName}`);
                    const constraintResult = captureMarker.constraint(p, this.buildConstraintContext(cursorAtCapturedNode));
                    if (!constraintResult) {
                        this.debug.log('info', 'constraint', `Constraint failed for capture: ${captureMarker.captureName}`);
                        this.debug.setExplanation('constraint-failed', `Capture ${captureMarker.captureName} with valid constraint`, `Constraint failed for ${(p as any).kind}`, `Constraint evaluation returned false`);
                        return this.abort(j) as R;
                    }
                    this.debug.log('debug', 'constraint', `Constraint passed for capture: ${captureMarker.captureName}`);
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

        return await super.visit(j, p, parent);
    }

    protected override async visitElement<T extends J>(j: T, other: T): Promise<T> {
        if (!this.match) {
            return j;
        }

        const kindStr = this.formatKind(j.kind);
        if (j.kind !== other.kind) {
            return this.abort(j, 'kind-mismatch', 'kind', kindStr, this.formatKind(other.kind));
        }

        for (const key of Object.keys(j)) {
            // Skip internal/private properties, id, markers, prefix, and padding (handled by visitor framework)
            if (key.startsWith('_') || key === 'kind' || key === 'id' || key === 'markers' || key === 'prefix' || key === 'padding') {
                continue;
            }

            const jValue = (j as any)[key];
            const otherValue = (other as any)[key];

            if (Array.isArray(jValue)) {
                if (!Array.isArray(otherValue) || jValue.length !== otherValue.length) {
                    this.debug.pushPath(`${kindStr}#${key}`);
                    const result = this.abort(j, 'array-length-mismatch', key, jValue.length,
                        Array.isArray(otherValue) ? otherValue.length : otherValue);
                    this.debug.popPath();
                    return result;
                }

                for (let i = 0; i < jValue.length; i++) {
                    this.debug.pushPath(`${kindStr}#${key}`);
                    this.debug.pushPath(i.toString());
                    try {
                        await this.visitProperty(jValue[i], otherValue[i]);
                        if (!this.match) {
                            return j;
                        }
                    } finally {
                        this.debug.popPath();
                        this.debug.popPath();
                    }
                }
            } else {
                this.debug.pushPath(`${kindStr}#${key}`);
                try {
                    await this.visitProperty(jValue, otherValue);
                    if (!this.match) {
                        return j;
                    }
                } finally {
                    this.debug.popPath();
                }
            }
        }

        return j;
    }

    override async visitRightPadded<T extends J | boolean>(right: J.RightPadded<T>, p: J): Promise<J.RightPadded<T>> {
        if (!this.match) {
            return right;
        }

        const captureMarker = PlaceholderUtils.getCaptureMarkerFromPadded(right);
        if (captureMarker) {
            // With intersection types, RightPadded elements have `padding` property but no `element`
            const isRightPaddedOther = isRightPadded(p);
            const targetWrapper = isRightPaddedOther ? (p as unknown) as J.RightPadded<T> : undefined;
            // For tree types, the padded value IS the element (use getPaddedElement helper)
            const targetElement = isRightPaddedOther ? getPaddedElement(targetWrapper!) : p;

            const savedTargetCursor = this.targetCursor;
            const cursorAtCapturedNode = this.targetCursor !== undefined
                ? (targetWrapper ? new Cursor(targetWrapper, this.targetCursor) : new Cursor(targetElement, this.targetCursor))
                : (targetWrapper ? new Cursor(targetWrapper) : new Cursor(targetElement));
            this.targetCursor = cursorAtCapturedNode;
            try {
                if (captureMarker.constraint && !captureMarker.variadicOptions) {
                    this.debug.log('debug', 'constraint', `Evaluating constraint for wrapped capture: ${captureMarker.captureName}`);
                    const constraintResult = captureMarker.constraint(targetElement as J, this.buildConstraintContext(cursorAtCapturedNode));
                    if (!constraintResult) {
                        this.debug.log('info', 'constraint', `Constraint failed for wrapped capture: ${captureMarker.captureName}`);
                        this.debug.setExplanation('constraint-failed', `Capture ${captureMarker.captureName} with valid constraint`, `Constraint failed for ${(targetElement as any).kind}`, `Constraint evaluation returned false`);
                        return this.abort(right);
                    }
                    this.debug.log('debug', 'constraint', `Constraint passed for wrapped capture: ${captureMarker.captureName}`);
                }

                const success = this.matcher.handleCapture(captureMarker, targetElement as J, targetWrapper as J.RightPadded<J> | undefined);
                if (!success) {
                    return this.abort(right);
                }
                return right;
            } finally {
                this.targetCursor = savedTargetCursor;
            }
        }

        return await super.visitRightPadded(right, p);
    }

    override async visitContainer<T extends J>(container: J.Container<T>, p: J): Promise<J.Container<T>> {
        if (!this.match) {
            return container;
        }

        const isContainer = (p as any).kind === J.Kind.Container;
        if (!isContainer) {
            return this.abort(container);
        }
        const otherContainer = p as unknown as J.Container<T>;

        const hasVariadicCapture = container.elements.some(elem =>
            PlaceholderUtils.isVariadicCaptureFromPadded(elem)
        );

        const savedCursor = this.cursor;
        const savedTargetCursor = this.targetCursor;
        this.cursor = new Cursor(container, this.cursor);
        this.targetCursor = new Cursor(otherContainer, this.targetCursor);
        try {
            if (hasVariadicCapture) {
                if (!await this.matchSequence(container.elements as J.RightPadded<J>[], otherContainer.elements as J.RightPadded<J>[], true)) {
                    return this.arrayLengthMismatch('elements');
                }
            } else {
                // Non-variadic path - track indices
                if (container.elements.length !== otherContainer.elements.length) {
                    return this.arrayLengthMismatch('elements');
                }

                for (let i = 0; i < container.elements.length; i++) {
                    this.debug.pushPath(i.toString());
                    try {
                        if (!await this.visitContainerElement(container.elements[i], otherContainer.elements[i], i)) {
                            return container;
                        }
                    } finally {
                        this.debug.popPath();
                    }
                }
            }
        } finally {
            this.cursor = savedCursor;
            this.targetCursor = savedTargetCursor;
        }

        return container;
    }

    /**
     * Override visitContainerProperty to add path tracking with property context.
     */
    protected override async visitContainerProperty<T extends J>(
        propertyName: string,
        container: J.Container<T>,
        otherContainer: J.Container<T>
    ): Promise<J.Container<T>> {
        // Get parent from cursor
        const parent = this.cursor.value as J;

        // Push path for the property
        const kindStr = this.formatKind((parent as any).kind);
        this.debug.pushPath(`${kindStr}#${propertyName}`);

        try {
            await this.visitContainer(container, otherContainer as any);
            return container;
        } finally {
            this.debug.popPath();
        }
    }

    /**
     * Override visitRightPaddedProperty to add path tracking with property context.
     */
    protected override async visitRightPaddedProperty<T extends J | boolean>(
        propertyName: string,
        rightPadded: J.RightPadded<T>,
        otherRightPadded: J.RightPadded<T>
    ): Promise<J.RightPadded<T>> {
        // Get parent from cursor
        const parent = this.cursor.value as J;

        // Push path for the property
        const kindStr = this.formatKind((parent as any).kind);
        this.debug.pushPath(`${kindStr}#${propertyName}`);

        try {
            return await this.visitRightPadded(rightPadded, otherRightPadded as any);
        } finally {
            this.debug.popPath();
        }
    }

    /**
     * Override visitLeftPaddedProperty to add path tracking with property context.
     */
    protected override async visitLeftPaddedProperty<T extends J | J.Space | number | string | boolean>(
        propertyName: string,
        leftPadded: J.LeftPadded<T>,
        otherLeftPadded: J.LeftPadded<T>
    ): Promise<J.LeftPadded<T>> {
        // Get parent from cursor
        const parent = this.cursor.value as J;

        // Push path for the property
        const kindStr = this.formatKind((parent as any).kind);
        this.debug.pushPath(`${kindStr}#${propertyName}`);

        try {
            return await this.visitLeftPadded(leftPadded, otherLeftPadded as any);
        } finally {
            this.debug.popPath();
        }
    }


    protected override async visitContainerElement<T extends J>(
        element: J.RightPadded<T>,
        otherElement: J.RightPadded<T>,
        index: number
    ): Promise<boolean> {
        // Don't push index here - it should be handled by the caller with proper context
        return await super.visitContainerElement(element, otherElement, index);
    }

    protected override async visitArrayProperty<T>(
        parent: J,
        propertyName: string,
        array1: T[],
        array2: T[],
        visitor: (item1: T, item2: T, index: number) => Promise<void>
    ): Promise<void> {
        // Push path for the property
        const kindStr = this.formatKind((parent as any).kind);
        this.debug.pushPath(`${kindStr}#${propertyName}`);

        try {
            // Check length mismatch (will have path context)
            if (array1.length !== array2.length) {
                this.arrayLengthMismatch(propertyName);
                return;
            }

            // Visit each element with index tracking
            for (let i = 0; i < array1.length; i++) {
                this.debug.pushPath(i.toString());
                try {
                    await visitor(array1[i], array2[i], i);
                    if (!this.match) {
                        return;
                    }
                } finally {
                    this.debug.popPath();
                }
            }
        } finally {
            this.debug.popPath();
        }
    }

    protected override async matchSequence(
        patternElements: J.RightPadded<J>[],
        targetElements: J.RightPadded<J>[],
        filterEmpty: boolean
    ): Promise<boolean> {
        // Push path component for the container
        // Extract kind from cursors if available
        const pattern = this.cursor?.value as any;
        if (pattern && pattern.kind) {
            const kindStr = this.formatKind(pattern.kind);
            // Determine property name based on the kind
            let propertyName = 'elements';
            if (pattern.kind.includes('MethodInvocation')) {
                propertyName = 'arguments';
            } else if (pattern.kind.includes('Block')) {
                propertyName = 'statements';
            }
            this.debug.pushPath(`${kindStr}#${propertyName}`);
        }

        try {
            return await super.matchSequence(patternElements, targetElements, filterEmpty);
        } finally {
            if (this.cursor?.value) {
                this.debug.popPath();
            }
        }
    }

    protected override async visitSequenceElement(
        patternWrapper: J.RightPadded<J>,
        targetWrapper: J.RightPadded<J>,
        targetIdx: number
    ): Promise<boolean> {
        this.debug.pushPath(targetIdx.toString());
        try {
            // Save current state for backtracking (both match state and capture bindings)
            const savedMatch = this.match;
            const savedState = this.matcher.saveState();

            await this.visitRightPadded(patternWrapper, targetWrapper as any);

            if (!this.match) {
                // Preserve explanation before restoring state
                const explanation = this.debug.getExplanation();
                // Restore state on match failure
                this.match = savedMatch;
                this.matcher.restoreState(savedState);
                // Restore the explanation if one was set during matching
                if (explanation) {
                    this.debug.restoreExplanation(explanation);
                }
                return false;
            }

            return true;
        } finally {
            this.debug.popPath();
        }
    }

    protected override async matchSequenceOptimized(
        patternElements: J.RightPadded<J>[],
        targetElements: J.RightPadded<J>[],
        patternIdx: number,
        targetIdx: number,
        filterEmpty: boolean
    ): Promise<boolean> {
        if (patternIdx >= patternElements.length) {
            return targetIdx >= targetElements.length;
        }

        const patternWrapper = patternElements[patternIdx];
        const captureMarker = PlaceholderUtils.getCaptureMarkerFromPadded(patternWrapper);
        const isVariadic = captureMarker?.variadicOptions !== undefined;

        if (isVariadic) {
            const variadicOptions = captureMarker!.variadicOptions;
            const min = variadicOptions?.min ?? 0;
            const max = variadicOptions?.max ?? Infinity;

            let nonVariadicRemainingPatterns = 0;
            let allRemainingPatternsAreDeterministic = true;
            for (let i = patternIdx + 1; i < patternElements.length; i++) {
                const nextCaptureMarker = PlaceholderUtils.getCaptureMarkerFromPadded(patternElements[i]);
                const nextIsVariadic = nextCaptureMarker?.variadicOptions !== undefined;
                if (!nextIsVariadic) {
                    nonVariadicRemainingPatterns++;
                }
                if (nextCaptureMarker) {
                    allRemainingPatternsAreDeterministic = false;
                }
            }
            const remainingTargetElements = targetElements.length - targetIdx;
            const maxPossible = Math.min(remainingTargetElements - nonVariadicRemainingPatterns, max);

            let pivotDetected = false;
            let pivotAt = -1;

            // Skip pivot detection if we're using deterministic optimization
            // (when all remaining patterns are literals, there's only ONE valid consumption amount)
            const useDeterministicOptimization = allRemainingPatternsAreDeterministic && maxPossible >= min && maxPossible <= max;

            if (!useDeterministicOptimization && patternIdx + 1 < patternElements.length && min <= maxPossible) {
                const nextPattern = patternElements[patternIdx + 1];

                for (let tryConsume = min; tryConsume <= maxPossible; tryConsume++) {
                    if (targetIdx + tryConsume < targetElements.length) {
                        const candidateElement = targetElements[targetIdx + tryConsume];

                        if (filterEmpty && candidateElement.kind === J.Kind.Empty) {
                            continue;
                        }

                        const savedMatch = this.match;
                        const savedState = this.matcher.saveState();

                        await this.visitRightPadded(nextPattern, candidateElement as any);
                        const matchesNext = this.match;

                        this.match = savedMatch;
                        this.matcher.restoreState(savedState);

                        if (matchesNext) {
                            pivotDetected = true;
                            pivotAt = tryConsume;
                            break;
                        }
                    }
                }
            }

            const consumptionOrder: number[] = [];
            // OPTIMIZATION: If all remaining patterns are deterministic (literals, not captures),
            // there's only ONE mathematically valid consumption amount. Skip backtracking entirely.
            // Example: foo(${args}, 999) matching foo(1,2,42) -> args MUST be [1,2], only try consume=2
            if (useDeterministicOptimization) {
                consumptionOrder.push(maxPossible);
            } else if (pivotDetected && pivotAt >= 0) {
                consumptionOrder.push(pivotAt);
                for (let c = maxPossible; c >= min; c--) {
                    if (c !== pivotAt) {
                        consumptionOrder.push(c);
                    }
                }
            } else {
                for (let c = maxPossible; c >= min; c--) {
                    consumptionOrder.push(c);
                }
            }

            for (const consume of consumptionOrder) {
                // Capture elements for this consumption amount
                // For empty argument lists, there will be a single J.Empty element that we need to filter out
                const rawWrappers = targetElements.slice(targetIdx, targetIdx + consume);
                const capturedWrappers = filterEmpty
                    ? rawWrappers.filter(w => w.kind !== J.Kind.Empty)
                    : rawWrappers;
                const capturedElements: J[] = capturedWrappers.map(w => getPaddedElement(w as J.RightPadded<J>));

                // Check min/max constraints against filtered elements
                if (capturedElements.length < min || capturedElements.length > max) {
                    continue;
                }

                if (captureMarker.constraint) {
                    this.debug.log('debug', 'constraint', `Evaluating variadic constraint for capture: ${captureMarker.captureName} (${capturedElements.length} elements)`);
                    const cursor = this.targetCursor || new Cursor(targetElements[0]);
                    const constraintResult = captureMarker.constraint(capturedElements as any, this.buildConstraintContext(cursor));
                    if (!constraintResult) {
                        this.debug.log('info', 'constraint', `Variadic constraint failed for capture: ${captureMarker.captureName}`);
                        continue;
                    }
                    this.debug.log('debug', 'constraint', `Variadic constraint passed for capture: ${captureMarker.captureName}`);
                }

                const savedState = this.matcher.saveState();

                const success = this.matcher.handleVariadicCapture(captureMarker, capturedElements, capturedWrappers);
                if (!success) {
                    this.matcher.restoreState(savedState);
                    continue;
                }

                const restMatches = await this.matchSequenceOptimized(
                    patternElements,
                    targetElements,
                    patternIdx + 1,
                    targetIdx + consume,
                    filterEmpty
                );

                if (restMatches) {
                    return true;
                }

                // Preserve explanation from this failed attempt before restoring state
                // This is especially important when using deterministic optimization (only one attempt)
                const currentExplanation = this.debug.getExplanation();
                this.matcher.restoreState(savedState);
                // Restore the explanation if one was set during this attempt
                if (currentExplanation) {
                    this.debug.restoreExplanation(currentExplanation);
                }
            }

            return false;
        } else {
            if (targetIdx >= targetElements.length) {
                return false;
            }

            const targetWrapper = targetElements[targetIdx];
            // For tree types, the padded value IS the element
            const targetElement = getPaddedElement(targetWrapper as J.RightPadded<J>);

            if (filterEmpty && targetElement.kind === J.Kind.Empty) {
                return false;
            }

            if (!await this.visitSequenceElement(patternWrapper, targetWrapper, targetIdx)) {
                return false;
            }

            return await this.matchSequenceOptimized(
                patternElements,
                targetElements,
                patternIdx + 1,
                targetIdx + 1,
                filterEmpty
            );
        }
    }
}
