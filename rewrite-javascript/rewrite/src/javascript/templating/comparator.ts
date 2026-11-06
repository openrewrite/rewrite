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
import {J, Type} from '../../java';
import {JS} from '../index';
import {JavaScriptSemanticComparatorVisitor, JTree} from '../comparator';
import {CaptureMarker, CaptureStorageValue, PlaceholderUtils, TEMPLATE_MODULE} from './utils';
import {Capture, CaptureConstraintContext, CaptureMap, DebugLogEntry, MatchExplanation} from './types';
import {CAPTURE_NAME_SYMBOL} from './capture';

// ============================================================================
// Debug Callbacks
// ============================================================================

/**
 * Debug callbacks for pattern matching.
 * These are always used together - either all present or all absent.
 */
export interface DebugCallbacks {
    log: (level: DebugLogEntry['level'], scope: DebugLogEntry['scope'], message: string, data?: any) => void;
    setExplanation: (reason: MatchExplanation['reason'], expected: string | Promise<string>,
                     actual: string | Promise<string>, details?: string, patternElement?: any,
                     targetElement?: any, focus?: Focus) => void;
    getExplanation: () => PendingExplanation | undefined;
    restoreExplanation: (explanation: PendingExplanation) => void;
    clearExplanation: () => void;
}

/** A match explanation whose expected and actual code may still be printing. */
export type PendingExplanation = Omit<MatchExplanation, 'expected' | 'actual'> & {
    expected: string | Promise<string>;
    actual: string | Promise<string>;
    focus?: Focus;
};

/**
 * Where within the marked node a mismatch is. That is its operator, or an empty mark at its start or
 * after one of its children, where the other side has something this side lacks.
 */
export type MarkFocus = 'operator' | 'start' | { after: J };

/** The {@link MarkFocus} on each side, where the mark is narrower than its node. */
export type Focus = { pattern?: MarkFocus, target?: MarkFocus };

/**
 * Snapshot of matcher state for backtracking.
 * Includes both capture storage and debug state.
 */
export interface MatcherState {
    storage: Map<string, CaptureStorageValue>;
    debugState?: {
        explanation?: PendingExplanation;
        logLength: number;
    };
}

/**
 * Callbacks for the matcher (debug and capture handling).
 */
export interface MatcherCallbacks {
    handleCapture: (capture: CaptureMarker, target: J, wrapper?: J.RightPadded<J>) => Promise<boolean>;
    handleVariadicCapture: (capture: CaptureMarker, targets: J[], wrappers?: J.RightPadded<J>[]) => Promise<boolean>;
    getCapture: (name: string) => CaptureStorageValue | undefined;
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

/** A capture found in a pattern, and the target node it binds to. */
interface CaptureAt {
    marker: CaptureMarker;
    target: J;
}

/** Parentheses that `JavaScriptSemanticComparatorVisitor.unwrap()` sees through. */
function isParenthesized(j: any): boolean {
    return j?.kind === J.Kind.Parentheses || j?.kind === J.Kind.ControlParentheses;
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
     * Lenient matching lets the code have a type annotation that the pattern leaves out, but not the
     * other way round: `const ${x}: number = ${v}` must not match `const a = 1`. The check is here,
     * not in the parent class, because the parent also compares the two places a repeated capture
     * binds. Neither of those is the pattern, so there both sides are treated alike.
     */
    override async visitVariableDeclarations(pattern: J.VariableDeclarations, other: J): Promise<J | undefined> {
        if (pattern.typeExpression && other.kind === J.Kind.VariableDeclarations &&
            !(other as J.VariableDeclarations).typeExpression) {
            return this.structuralMismatch<J>('typeExpression');
        }
        return super.visitVariableDeclarations(pattern, other);
    }

    /** The same for a return type: `function ${f}(): string {…}` must not match `function a() {…}`. */
    override async visitMethodDeclaration(pattern: J.MethodDeclaration, other: J): Promise<J | undefined> {
        if (pattern.returnTypeExpression && other.kind === J.Kind.MethodDeclaration &&
            !(other as J.MethodDeclaration).returnTypeExpression) {
            return this.typeMismatch('returnTypeExpression');
        }
        return super.visitMethodDeclaration(pattern, other);
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

    /** The capture this pattern node stands for, paired with the target node it should bind to. */
    protected captureAt(j: Tree, p: J): CaptureAt | undefined {
        const marker = PlaceholderUtils.getCaptureMarker(j as J);
        return marker ? {marker, target: p} : this.captureBehindParentheses(j, p);
    }

    /** The marker `attachCaptureMarkers` moved onto a parenthesized capture's `J.RightPadded`, which `unwrap()` skips past. */
    private captureBehindParentheses(j: Tree, p: J): CaptureAt | undefined {
        let pattern: any = j;
        let target: any = p;
        while (isParenthesized(pattern)) {
            const padded = pattern.tree as J.RightPadded<J> | undefined;
            if (!padded) {
                return undefined;
            }
            target = isParenthesized(target) ? target.tree.element : target;
            const marker = PlaceholderUtils.getCaptureMarker(padded);
            if (marker) {
                return {marker, target: target as J};
            }
            pattern = padded.element;
        }
        return undefined;
    }

    override async visit<R extends J>(j: Tree, p: J, parent?: Cursor): Promise<R | undefined> {
        // Check if the pattern node is a capture - this handles unwrapped captures
        // (Wrapped captures in J.RightPadded are handled by visitRightPadded override)
        // Note: targetCursor will be pushed by parent's visit() method after this check
        const capture = this.captureAt(j, p);
        if (capture) {
            const {marker: captureMarker, target} = capture;

            // Push targetCursor to position it at the captured node for constraint evaluation
            // Only create cursor if targetCursor was initialized (meaning user provided one)
            const savedTargetCursor = this.targetCursor;
            const cursorAtCapturedNode = this.targetCursor !== undefined
                ? new Cursor(target, this.targetCursor)
                : new Cursor(target);
            this.targetCursor = cursorAtCapturedNode;
            try {
                // Skip constraint for variadic captures - they're evaluated in matchSequence with the full array
                if (captureMarker.constraint && !captureMarker.variadicOptions &&
                    !this.satisfiesConstraint(captureMarker, j as J, target, cursorAtCapturedNode)) {
                    return this.abort(j) as R;
                }

                const success = await this.matcher.handleCapture(captureMarker, target, undefined);
                if (!success) {
                    this.explainCaptureConflict(captureMarker, j as J, [target]);
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

    /**
     * A function the pattern's `context` declares lives in no module of the source,
     * so it stands in for any function of its name.
     */
    protected override declaresAnyCallee(declaringType: Type.FullyQualified): boolean {
        return Type.FullyQualified.getFullyQualifiedName(declaringType) === TEMPLATE_MODULE;
    }

    protected hasSameKind(j: J, other: J): boolean {
        return super.hasSameKind(j, other) ||
            (j.kind == J.Kind.Identifier && PlaceholderUtils.isCapture(j as J.Identifier));
    }

    /** Whether `target`, the code at `patternElement`'s capture, satisfies the capture's constraint. */
    protected satisfiesConstraint(marker: CaptureMarker, _patternElement: JTree, target: J | J[],
                                  cursor: Cursor): boolean {
        return marker.constraint!(target as any, this.buildConstraintContext(cursor));
    }

    /** Called where a capture the pattern names twice holds other code than `targets`. Matching ignores it. */
    protected explainCaptureConflict(_marker: CaptureMarker, _patternElement: JTree, _targets: J[]): void {
    }

    /** Restores `state` after a failed alternative, keeping why it failed in case no other alternative matches. */
    private backtrack(state: MatcherState): void {
        const failure = this.matcher.debug?.getExplanation();
        this.matcher.restoreState(state);
        if (failure) {
            this.matcher.debug!.restoreExplanation(failure);
        }
    }

    /**
     * Override visitRightPadded to check if this wrapper has a CaptureMarker.
     * If so, capture the entire wrapper (to preserve markers like semicolons).
     */
    override async visitRightPadded<T extends J | boolean>(right: J.RightPadded<T>, p: J.RightPadded<T>): Promise<J.RightPadded<T>> {
        if (!this.match) {
            return right;
        }

        // Check if this RightPadded has a CaptureMarker (attached during pattern construction)
        // Note: Markers are now only at the wrapper level, not at the element level
        const captureMarker = PlaceholderUtils.getCaptureMarker(right);
        if (captureMarker) {
            // Extract the target wrapper if it's also a RightPadded
            const isRightPadded = p.kind === J.Kind.RightPadded;
            const targetWrapper = isRightPadded ? p : undefined;
            const targetElement = isRightPadded ? targetWrapper!.element : p;

            // Push targetCursor to position it at the captured element for constraint evaluation
            const savedTargetCursor = this.targetCursor;
            const cursorAtCapturedNode = this.targetCursor !== undefined
                ? (targetWrapper ? new Cursor(targetWrapper, this.targetCursor) : new Cursor(targetElement, this.targetCursor))
                : (targetWrapper ? new Cursor(targetWrapper) : new Cursor(targetElement));
            this.targetCursor = cursorAtCapturedNode;
            try {
                // Skip constraint for variadic captures - they're evaluated in matchSequence with the full array
                if (captureMarker.constraint && !captureMarker.variadicOptions &&
                    !this.satisfiesConstraint(captureMarker, right, targetElement as J, cursorAtCapturedNode)) {
                    return this.abort(right);
                }

                // Handle the capture with the wrapper - use the element for pattern matching
                const success = await this.matcher.handleCapture(captureMarker, targetElement as J, targetWrapper as J.RightPadded<J> | undefined);
                if (!success) {
                    this.explainCaptureConflict(captureMarker, right, [targetElement as J]);
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

    override async visitContainer<T extends J>(container: J.Container<T>, p: J.Container<T>): Promise<J.Container<T>> {
        // Check if any elements are variadic captures
        const hasVariadicCapture = container.elements.some(elem =>
            PlaceholderUtils.isVariadicCapture(elem)
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
        const isContainer = p.kind === J.Kind.Container;
        if (!isContainer) {
            // Set up cursors temporarily for kindMismatch to use
            const savedCursor = this.cursor;
            const savedTargetCursor = this.targetCursor;
            this.cursor = new Cursor(container, this.cursor);
            this.targetCursor = new Cursor(p, this.targetCursor);
            try {
                return this.kindMismatch<J.Container<T>>();
            } finally {
                this.cursor = savedCursor;
                this.targetCursor = savedTargetCursor;
            }
        }
        const otherContainer = p;

        // Push wrappers onto both cursors
        const savedCursor = this.cursor;
        const savedTargetCursor = this.targetCursor;
        this.cursor = new Cursor(container, this.cursor);
        this.targetCursor = new Cursor(otherContainer, this.targetCursor);
        try {
            // Use matchSequence for variadic matching
            // filterEmpty=true to skip J.Empty elements (they represent missing elements in destructuring)
            if (!await this.matchSequence(container.elements as J.RightPadded<J>[], otherContainer.elements as J.RightPadded<J>[], true)) {
                return this.structuralMismatch<J.Container<T>>('elements');
            }
        } finally {
            this.cursor = savedCursor;
            this.targetCursor = savedTargetCursor;
        }

        return container;
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
                return this.kindMismatch<J>();
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
                return this.structuralMismatch<J>('statements');
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
            return PlaceholderUtils.isVariadicCapture(stmt);
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
                return this.kindMismatch<J>();
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
                return this.structuralMismatch<J>('statements');
            }

            return compilationUnit;
        } finally {
            this.cursor = savedCursor;
            this.targetCursor = savedTargetCursor;
        }
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
        const captureMarker = PlaceholderUtils.getCaptureMarker(patternWrapper);
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
                const nextCaptureMarker = PlaceholderUtils.getCaptureMarker(patternElements[i]);
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
                        if (filterEmpty && candidateElement.element.kind === J.Kind.Empty) {
                            continue;
                        }

                        // Test if next pattern matches this element
                        const savedMatch = this.match;
                        const savedState = this.matcher.saveState();

                        await this.visitRightPadded(nextPattern, candidateElement);
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
                    ? rawWrappers.filter(w => w.element.kind !== J.Kind.Empty)
                    : rawWrappers;
                const capturedElements: J[] = capturedWrappers.map(w => w.element);

                // Check min/max constraints against filtered elements
                if (capturedElements.length < min || capturedElements.length > max) {
                    continue;
                }

                // For variadic captures, constraint receives the entire array of captured elements
                // The targetCursor points to the parent container (always defined in container matching)
                const constraintCursor = this.targetCursor || new Cursor(targetElements[0]);
                if (captureMarker.constraint &&
                    !this.satisfiesConstraint(captureMarker, patternWrapper, capturedElements, constraintCursor)) {
                    continue; // Try next consumption amount
                }

                // Save current state for backtracking
                const savedState = this.matcher.saveState();

                // Handle the variadic capture
                const success = await this.matcher.handleVariadicCapture(captureMarker, capturedElements, capturedWrappers);
                if (!success) {
                    this.matcher.restoreState(savedState);
                    this.explainCaptureConflict(captureMarker, patternWrapper, capturedElements);
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
                    // Why earlier alternatives failed explains nothing about a match that went on
                    this.matcher.debug?.clearExplanation();
                    return true;
                }

                this.backtrack(savedState);
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

        await this.visitRightPadded(patternWrapper, targetWrapper);

        if (!this.match) {
            this.match = savedMatch;
            this.backtrack(savedState);
            return false;
        }

        return true;
    }
}
