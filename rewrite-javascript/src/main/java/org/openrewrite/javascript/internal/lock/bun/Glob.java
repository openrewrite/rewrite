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
package org.openrewrite.javascript.internal.lock.bun;

import java.util.ArrayList;
import java.util.List;

/**
 * Port of bun's src/glob.zig {@code detectGlobSyntax} and src/glob/match.zig {@code match} (bun 1.3.10), over
 * UTF-16 code units: the patterns and paths it matches here are workspace paths. 1.4.0's src/glob/lib.rs and
 * matcher.rs are beside it where they differ for these: each token's backslashes and a pattern started past its
 * {@code !}s; their brace handling (a comma inside brackets, an unterminated group, the branch budget) is not. Before
 * 1.2.3 the matcher is src/glob/ascii.zig's ({@link BunRelease#triesEveryBraceBranch()}), ported beside it.
 */
final class Glob {

    enum MatchResult {
        NO_MATCH, MATCH, NEGATE_NO_MATCH, NEGATE_MATCH;

        boolean matches() {
            return this == MATCH || this == NEGATE_MATCH;
        }
    }

    private static final int MAX_BRACE_DEPTH = 10;

    private Glob() {
    }

    static boolean detectGlobSyntax(String potentialPattern) {
        return detectGlobSyntax(potentialPattern, BunRelease.PORTED);
    }

    /**
     * {@code detectGlobSyntax} as {@code release} tells glob syntax that no backslash escapes.
     */
    static boolean detectGlobSyntax(String potentialPattern, BunRelease release) {
        if (!potentialPattern.isEmpty() && potentialPattern.charAt(0) == '!') {
            return true;
        }
        for (char token : new char[]{'*', '{', '[', '?'}) {
            int offset = 0;
            while (offset < potentialPattern.length()) {
                int idx = potentialPattern.indexOf(token, offset);
                if (idx < 0) {
                    break;
                }
                int i = idx - offset;
                int backslashCount = 0;
                if (release.countsTheBackslashesBeforeEachGlobToken()) {
                    // lib.rs detect_glob_syntax: the run before the token itself, back to where the slice starts
                    while (i > 0 && potentialPattern.charAt(offset + i - 1) == '\\') {
                        backslashCount++;
                        i--;
                    }
                } else {
                    // bun counts the backslashes before the index relative to the remaining slice, in the whole
                    // pattern
                    while (i > 0 && potentialPattern.charAt(i - 1) == '\\') {
                        backslashCount++;
                        i--;
                    }
                }
                if (backslashCount % 2 == 0) {
                    return true;
                }
                offset = idx + 1;
            }
        }
        return false;
    }

    private static final class Wildcard {
        int globIndex;
        int pathIndex;
        int braceDepth;

        Wildcard copy() {
            Wildcard w = new Wildcard();
            w.globIndex = globIndex;
            w.pathIndex = pathIndex;
            w.braceDepth = braceDepth;
            return w;
        }
    }

    private static final class State {
        int pathIndex;
        int globIndex;
        Wildcard wildcard = new Wildcard();
        Wildcard globstar = new Wildcard();
        int braceDepth;

        State copy() {
            State s = new State();
            s.pathIndex = pathIndex;
            s.globIndex = globIndex;
            s.wildcard = wildcard.copy();
            s.globstar = globstar.copy();
            s.braceDepth = braceDepth;
            return s;
        }

        void backtrack() {
            pathIndex = wildcard.pathIndex;
            globIndex = wildcard.globIndex;
            braceDepth = wildcard.braceDepth;
        }

        void skipToSeparator(String path, boolean isEndInvalid) {
            if (pathIndex == path.length()) {
                wildcard.pathIndex += 1;
                return;
            }
            int p = pathIndex;
            while (p < path.length() && path.charAt(p) != '/') {
                p++;
            }
            if (isEndInvalid || p != path.length()) {
                p++;
            }
            wildcard.pathIndex = p;
            globstar = wildcard.copy();
        }
    }

    static MatchResult match(String glob, String path) {
        return match(glob, path, BunRelease.PORTED);
    }

    /**
     * {@code match} as {@code release} matches: from 1.4.0 the pattern starts past its leading {@code !}s
     * ({@link BunRelease#readsAGlobstarAfterANegation}); before 1.2.3 with src/glob/ascii.zig's matcher
     * ({@link BunRelease#triesEveryBraceBranch()}).
     */
    static MatchResult match(String glob, String path, BunRelease release) {
        if (!release.triesEveryBraceBranch()) {
            return matchKeepingTheLongestBranch(glob, path);
        }
        State state = new State();
        boolean negated = false;
        while (state.globIndex < glob.length() && glob.charAt(state.globIndex) == '!') {
            negated = !negated;
            state.globIndex++;
        }
        int globStart = release.readsAGlobstarAfterANegation() ? state.globIndex : 0;
        boolean matched = globMatchImpl(state, glob, globStart, path, new ArrayList<>(), release);
        if (negated) {
            return matched ? MatchResult.NEGATE_NO_MATCH : MatchResult.NEGATE_MATCH;
        }
        return matched ? MatchResult.MATCH : MatchResult.NO_MATCH;
    }

    private static boolean globMatchImpl(State state, String glob, int globStart, String path, List<int[]> braceStack,
                                         BunRelease release) {
        mainLoop:
        while (state.globIndex < glob.length() || state.pathIndex < path.length()) {
            fallthrough:
            if (state.globIndex < glob.length()) {
                char c = glob.charAt(state.globIndex);
                boolean toElse = false;
                switch (c) {
                    case '*': {
                        boolean isGlobstar = state.globIndex + 1 < glob.length() &&
                                             glob.charAt(state.globIndex + 1) == '*';
                        if (isGlobstar) {
                            state.globIndex = skipGlobstars(glob, state.globIndex);
                        }
                        state.wildcard.globIndex = state.globIndex;
                        state.wildcard.pathIndex = state.pathIndex + 1;
                        state.wildcard.braceDepth = state.braceDepth;
                        boolean inGlobstar = false;
                        if (isGlobstar) {
                            state.globIndex += 2;
                            boolean isEndInvalid = state.globIndex < glob.length();
                            if (isEndInvalid && state.pathIndex == path.length() &&
                                glob.length() - state.globIndex == 2 && glob.charAt(state.globIndex) == '/' &&
                                glob.charAt(state.globIndex + 1) == '*') {
                                continue;
                            }
                            if ((Math.max(0, state.globIndex - globStart) < 3 ||
                                 glob.charAt(state.globIndex - 3) == '/') &&
                                (!isEndInvalid || glob.charAt(state.globIndex) == '/')) {
                                if (isEndInvalid) {
                                    state.globIndex++;
                                }
                                state.skipToSeparator(path, isEndInvalid);
                                inGlobstar = true;
                            }
                        } else {
                            state.globIndex++;
                        }
                        if (!inGlobstar && state.pathIndex < path.length() && path.charAt(state.pathIndex) == '/') {
                            state.wildcard = state.globstar.copy();
                        }
                        continue;
                    }
                    case '?':
                        if (state.pathIndex < path.length()) {
                            if (path.charAt(state.pathIndex) != '/') {
                                state.globIndex++;
                                state.pathIndex++;
                                continue;
                            }
                            break fallthrough;
                        }
                        toElse = true;
                        break;
                    case '[':
                        if (state.pathIndex < path.length()) {
                            state.globIndex++;
                            boolean negated = false;
                            if (state.globIndex < glob.length() &&
                                (glob.charAt(state.globIndex) == '^' || glob.charAt(state.globIndex) == '!')) {
                                negated = true;
                                state.globIndex++;
                            }
                            boolean first = true;
                            boolean isMatch = false;
                            char target = path.charAt(state.pathIndex);
                            while (state.globIndex < glob.length() && (first || glob.charAt(state.globIndex) != ']')) {
                                int low = glob.charAt(state.globIndex);
                                if (low == '\\') {
                                    state.globIndex++;
                                    if (state.globIndex >= glob.length()) {
                                        return false;
                                    }
                                    low = unescaped(glob.charAt(state.globIndex));
                                }
                                state.globIndex++;
                                int high = low;
                                if (state.globIndex + 1 < glob.length() && glob.charAt(state.globIndex) == '-' &&
                                    glob.charAt(state.globIndex + 1) != ']') {
                                    state.globIndex++;
                                    high = glob.charAt(state.globIndex);
                                    if (high == '\\') {
                                        state.globIndex++;
                                        if (state.globIndex >= glob.length()) {
                                            return false;
                                        }
                                        high = unescaped(glob.charAt(state.globIndex));
                                    }
                                    state.globIndex++;
                                }
                                if (low <= target && target <= high) {
                                    isMatch = true;
                                }
                                first = false;
                            }
                            if (state.globIndex >= glob.length()) {
                                return false;
                            }
                            state.globIndex++;
                            if (isMatch != negated) {
                                state.pathIndex++;
                                continue;
                            }
                            break fallthrough;
                        }
                        toElse = true;
                        break;
                    case '{':
                        for (int[] brace : braceStack) {
                            if (brace[0] == state.globIndex) {
                                state.globIndex = brace[1];
                                state.braceDepth++;
                                continue mainLoop;
                            }
                        }
                        return matchBrace(state, glob, path, braceStack, release);
                    case ',':
                    case '}':
                        if (state.braceDepth > 0) {
                            skipBranch(state, glob, release);
                            continue;
                        }
                        toElse = true;
                        break;
                    default:
                        toElse = true;
                        break;
                }
                if (toElse && state.pathIndex < path.length()) {
                    char cc = c;
                    if (cc == '\\') {
                        state.globIndex++;
                        if (state.globIndex >= glob.length()) {
                            return false;
                        }
                        cc = unescaped(glob.charAt(state.globIndex));
                    }
                    char pc = path.charAt(state.pathIndex);
                    boolean isMatch = cc == '/' ? pc == '/' : pc == cc;
                    if (isMatch) {
                        state.globIndex++;
                        state.pathIndex++;
                        if (cc == '/') {
                            state.wildcard = state.globstar.copy();
                        }
                        continue;
                    }
                }
            }
            if (state.wildcard.pathIndex > 0 && state.wildcard.pathIndex <= path.length()) {
                state.backtrack();
                continue;
            }
            return false;
        }
        return true;
    }

    private static char unescaped(char c) {
        switch (c) {
            case 'a':
                return 'a';
            case 'b':
                return '\b';
            case 'n':
                return '\n';
            case 'r':
                return '\r';
            case 't':
                return '\t';
            default:
                return c;
        }
    }

    private static boolean matchBrace(State state, String glob, String path, List<int[]> braceStack,
                                      BunRelease release) {
        int braceDepth = 0;
        boolean inBrackets = false;
        int openBraceIndex = state.globIndex;
        int branchIndex = 0;
        while (state.globIndex < glob.length()) {
            char c = glob.charAt(state.globIndex);
            if (c == '{') {
                if (!inBrackets) {
                    braceDepth++;
                    if (braceDepth == 1) {
                        branchIndex = state.globIndex + 1;
                    }
                }
            } else if (c == '}') {
                if (!inBrackets) {
                    braceDepth--;
                    if (braceDepth == 0) {
                        return matchBraceBranch(state, glob, path, openBraceIndex, branchIndex, braceStack, release);
                    }
                }
            } else if (c == ',') {
                if (braceDepth == 1) {
                    if (matchBraceBranch(state, glob, path, openBraceIndex, branchIndex, braceStack, release)) {
                        return true;
                    }
                    branchIndex = state.globIndex + 1;
                }
            } else if (c == '[') {
                if (!inBrackets) {
                    inBrackets = true;
                }
            } else if (c == ']') {
                inBrackets = false;
            } else if (c == '\\') {
                state.globIndex++;
            }
            state.globIndex++;
        }
        return false;
    }

    private static boolean matchBraceBranch(State state, String glob, String path, int openBraceIndex, int branchIndex,
                                            List<int[]> braceStack, BunRelease release) {
        if (braceStack.size() >= MAX_BRACE_DEPTH) {
            return false;
        }
        braceStack.add(new int[]{openBraceIndex, branchIndex});
        State branchState = state.copy();
        branchState.globIndex = branchIndex;
        branchState.braceDepth = braceStack.size();
        boolean matched = globMatchImpl(branchState, glob, branchIndex, path, braceStack, release);
        braceStack.remove(braceStack.size() - 1);
        return matched;
    }

    /**
     * {@code skipBranch}: on to the {@code }} that closes the branch's own group; 1.2.3 stops at the first {@code }},
     * a nested group's too ({@link BunRelease#skipsABraceBranchToItsOwnClose()}).
     */
    private static void skipBranch(State state, String glob, BunRelease release) {
        boolean inBrackets = false;
        int endBraceDepth = state.braceDepth - 1;
        while (state.globIndex < glob.length()) {
            char c = glob.charAt(state.globIndex);
            if (c == '{') {
                if (!inBrackets) {
                    state.braceDepth++;
                }
            } else if (c == '}') {
                if (!inBrackets) {
                    state.braceDepth--;
                    if (state.braceDepth == endBraceDepth || !release.skipsABraceBranchToItsOwnClose()) {
                        state.globIndex++;
                        return;
                    }
                }
            } else if (c == '[') {
                if (!inBrackets) {
                    inBrackets = true;
                }
            } else if (c == ']') {
                inBrackets = false;
            } else if (c == '\\') {
                state.globIndex++;
            }
            state.globIndex++;
        }
    }

    // src/glob/ascii.zig (1.2.0 - 1.2.2): Devon Govett's glob-match, which GlobWalker.zig's matchImpl repeats over code
    // points

    private static final class AsciiState {
        int pathIndex;
        int globIndex;
        int wildcardGlobIndex;
        int wildcardPathIndex;
        int globstarGlobIndex;
        int globstarPathIndex;

        AsciiState copy() {
            AsciiState s = new AsciiState();
            s.pathIndex = pathIndex;
            s.globIndex = globIndex;
            s.wildcardGlobIndex = wildcardGlobIndex;
            s.wildcardPathIndex = wildcardPathIndex;
            s.globstarGlobIndex = globstarGlobIndex;
            s.globstarPathIndex = globstarPathIndex;
            return s;
        }

        void backtrack() {
            globIndex = wildcardGlobIndex;
            pathIndex = wildcardPathIndex;
        }

        // skipBraces: 0 Invalid, 1 Comma, 2 EndBrace
        int skipBraces(String glob, boolean stopOnComma) {
            int braces = 1;
            boolean inBrackets = false;
            for (; globIndex < glob.length() && braces > 0; globIndex++) {
                char c = glob.charAt(globIndex);
                if (c == '{') {
                    if (!inBrackets) {
                        braces++;
                    }
                } else if (c == '}') {
                    if (!inBrackets) {
                        braces--;
                    }
                } else if (c == ',') {
                    if (stopOnComma && braces == 1 && !inBrackets) {
                        globIndex++;
                        return 1;
                    }
                } else if (c == '[') {
                    if (!inBrackets) {
                        inBrackets = true;
                    }
                } else if (c == ']') {
                    inBrackets = false;
                } else if (c == '\\') {
                    globIndex++;
                }
            }
            return braces != 0 ? 0 : 2;
        }
    }

    private static final class AsciiBraceStack {
        final AsciiState[] stack = new AsciiState[MAX_BRACE_DEPTH];
        int len;
        int longestBraceMatch;

        AsciiState push(AsciiState state) {
            stack[len++] = state.copy();
            AsciiState s = new AsciiState();
            s.pathIndex = state.pathIndex;
            s.globIndex = state.globIndex + 1;
            return s;
        }

        AsciiState pop(AsciiState state) {
            len--;
            AsciiState s = new AsciiState();
            s.globIndex = state.globIndex;
            s.pathIndex = longestBraceMatch;
            s.wildcardGlobIndex = stack[len].wildcardGlobIndex;
            s.wildcardPathIndex = stack[len].wildcardPathIndex;
            s.globstarGlobIndex = stack[len].globstarGlobIndex;
            s.globstarPathIndex = stack[len].globstarPathIndex;
            if (len == 0) {
                longestBraceMatch = 0;
            }
            return s;
        }

        AsciiState last() {
            return stack[len - 1];
        }
    }

    /**
     * src/glob/ascii.zig {@code match}, before 1.2.3: a brace group keeps the longest branch that matched
     * ({@code longest_brace_match}) and never tries a shorter one again, {@code {} opens a group only while the path
     * has characters left, a character matched right after {@code }} closes the group, a {@code ,} or {@code }}
     * outside a group matches nothing, and a pattern it cannot read (a class without its {@code ]}, a group without
     * its {@code }}, a trailing backslash) is no match under {@code !} too.
     */
    private static MatchResult matchKeepingTheLongestBranch(String glob, String path) {
        AsciiState state = new AsciiState();
        AsciiBraceStack braceStack = new AsciiBraceStack();
        boolean negated = false;
        while (state.globIndex < glob.length() && glob.charAt(state.globIndex) == '!') {
            negated = !negated;
            state.globIndex++;
        }
        while (state.globIndex < glob.length() || state.pathIndex < path.length()) {
            if (state.globIndex < glob.length()) {
                char gc = glob.charAt(state.globIndex);
                if (gc == '*') {
                    boolean isGlobstar = state.globIndex + 1 < glob.length() && glob.charAt(state.globIndex + 1) == '*';
                    if (isGlobstar) {
                        int index = state.globIndex + 2;
                        while (index + 3 <= glob.length() && glob.startsWith("/**", index)) {
                            index += 3;
                        }
                        state.globIndex = index - 2;
                    }
                    state.wildcardGlobIndex = state.globIndex;
                    state.wildcardPathIndex = state.pathIndex + 1;
                    if (isGlobstar) {
                        state.globIndex += 2;
                        if (glob.length() == state.globIndex) {
                            state.globstarGlobIndex = state.wildcardGlobIndex;
                            state.globstarPathIndex = state.wildcardPathIndex;
                        } else if (glob.charAt(state.globIndex) == '/' &&
                                   (state.globIndex < 3 || glob.charAt(state.globIndex - 3) == '/')) {
                            if (state.pathIndex == 0 ||
                                state.pathIndex < path.length() && path.charAt(state.pathIndex - 1) == '/') {
                                state.globIndex++;
                            }
                            state.globstarGlobIndex = state.wildcardGlobIndex;
                            state.globstarPathIndex = state.wildcardPathIndex;
                        }
                    } else {
                        state.globIndex++;
                    }
                    if (state.globstarPathIndex != state.wildcardPathIndex && state.pathIndex < path.length() &&
                        path.charAt(state.pathIndex) == '/') {
                        if (state.globstarPathIndex > 0 && state.pathIndex + 1 < path.length()) {
                            state.globIndex = state.globstarGlobIndex;
                            state.wildcardGlobIndex = state.globstarGlobIndex;
                        } else {
                            state.wildcardPathIndex = 0;
                        }
                    }
                    if (braceStack.len > 0 && state.globIndex < glob.length() &&
                        (glob.charAt(state.globIndex) == ',' || glob.charAt(state.globIndex) == '}')) {
                        if (state.skipBraces(glob, false) == 0) {
                            return MatchResult.NO_MATCH;
                        }
                    }
                    continue;
                } else if (gc == '?') {
                    if (state.pathIndex < path.length() && path.charAt(state.pathIndex) != '/') {
                        state.globIndex++;
                        state.pathIndex++;
                        continue;
                    }
                } else if (gc == '[') {
                    if (state.pathIndex < path.length()) {
                        state.globIndex++;
                        char c = path.charAt(state.pathIndex);
                        boolean classNegated = false;
                        if (state.globIndex < glob.length() &&
                            (glob.charAt(state.globIndex) == '^' || glob.charAt(state.globIndex) == '!')) {
                            classNegated = true;
                            state.globIndex++;
                        }
                        boolean first = true;
                        boolean isMatch = false;
                        while (state.globIndex < glob.length() && (first || glob.charAt(state.globIndex) != ']')) {
                            char low = glob.charAt(state.globIndex);
                            if (low == '\\') {
                                state.globIndex++;
                                if (state.globIndex >= glob.length()) {
                                    return MatchResult.NO_MATCH;
                                }
                                low = unescaped(glob.charAt(state.globIndex));
                            }
                            state.globIndex++;
                            char high = low;
                            if (state.globIndex + 1 < glob.length() && glob.charAt(state.globIndex) == '-' &&
                                glob.charAt(state.globIndex + 1) != ']') {
                                state.globIndex++;
                                high = glob.charAt(state.globIndex);
                                if (high == '\\') {
                                    state.globIndex++;
                                    if (state.globIndex >= glob.length()) {
                                        return MatchResult.NO_MATCH;
                                    }
                                    high = unescaped(glob.charAt(state.globIndex));
                                }
                                state.globIndex++;
                            }
                            if (low <= c && c <= high) {
                                isMatch = true;
                            }
                            first = false;
                        }
                        if (state.globIndex >= glob.length()) {
                            return MatchResult.NO_MATCH;
                        }
                        state.globIndex++;
                        if (isMatch != classNegated) {
                            state.pathIndex++;
                            continue;
                        }
                    }
                } else if (gc == '{') {
                    if (state.pathIndex < path.length()) {
                        if (braceStack.len >= MAX_BRACE_DEPTH) {
                            return MatchResult.NO_MATCH;
                        }
                        state = braceStack.push(state);
                        continue;
                    }
                } else if (gc == '}') {
                    if (braceStack.len > 0) {
                        braceStack.longestBraceMatch = Math.max(braceStack.longestBraceMatch, state.pathIndex);
                        state.globIndex++;
                        state = braceStack.pop(state);
                        continue;
                    }
                } else if (gc == ',') {
                    if (braceStack.len > 0) {
                        braceStack.longestBraceMatch = Math.max(braceStack.longestBraceMatch, state.pathIndex);
                        state.pathIndex = braceStack.last().pathIndex;
                        state.globIndex++;
                        state.wildcardGlobIndex = 0;
                        state.wildcardPathIndex = 0;
                        state.globstarGlobIndex = 0;
                        state.globstarPathIndex = 0;
                        continue;
                    }
                } else if (state.pathIndex < path.length()) {
                    char cc = gc;
                    if (cc == '\\') {
                        state.globIndex++;
                        if (state.globIndex >= glob.length()) {
                            return MatchResult.NO_MATCH;
                        }
                        cc = unescaped(glob.charAt(state.globIndex));
                    }
                    if (path.charAt(state.pathIndex) == cc) {
                        if (braceStack.len > 0 && state.globIndex > 0 && glob.charAt(state.globIndex - 1) == '}') {
                            braceStack.longestBraceMatch = state.pathIndex;
                            state = braceStack.pop(state);
                        }
                        state.globIndex++;
                        state.pathIndex++;
                        if (cc != '/') {
                            state.globstarPathIndex = 0;
                        }
                        continue;
                    }
                }
            }
            if (state.wildcardPathIndex > 0 && state.wildcardPathIndex <= path.length()) {
                state.backtrack();
                continue;
            }
            if (braceStack.len > 0) {
                int skipped = state.skipBraces(glob, true);
                if (skipped == 0) {
                    return MatchResult.NO_MATCH;
                } else if (skipped == 1) {
                    state.pathIndex = braceStack.last().pathIndex;
                    continue;
                }
                if (braceStack.longestBraceMatch > 0) {
                    state = braceStack.pop(state);
                    continue;
                }
                state = braceStack.last().copy();
                braceStack.len--;
                if (state.wildcardPathIndex > 0 && state.wildcardPathIndex <= path.length()) {
                    state.backtrack();
                    continue;
                }
            }
            return negated ? MatchResult.NEGATE_MATCH : MatchResult.NO_MATCH;
        }
        return negated ? MatchResult.NEGATE_NO_MATCH : MatchResult.MATCH;
    }

    private static int skipGlobstars(String glob, int globIndex) {
        globIndex += 2;
        while (globIndex + 4 <= glob.length() && glob.startsWith("/**/", globIndex)) {
            globIndex += 3;
        }
        if (globIndex + 3 == glob.length() && glob.startsWith("/**", globIndex)) {
            globIndex += 3;
        }
        return globIndex - 2;
    }
}
