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
 * UTF-16 code units: the patterns and paths it matches here are workspace paths.
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
                // bun counts the backslashes before the index relative to the remaining slice, in the whole pattern
                int i = idx - offset;
                int backslashCount = 0;
                while (i > 0 && potentialPattern.charAt(i - 1) == '\\') {
                    backslashCount++;
                    i--;
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
        State state = new State();
        boolean negated = false;
        while (state.globIndex < glob.length() && glob.charAt(state.globIndex) == '!') {
            negated = !negated;
            state.globIndex++;
        }
        boolean matched = globMatchImpl(state, glob, 0, path, new ArrayList<>());
        if (negated) {
            return matched ? MatchResult.NEGATE_NO_MATCH : MatchResult.NEGATE_MATCH;
        }
        return matched ? MatchResult.MATCH : MatchResult.NO_MATCH;
    }

    private static boolean globMatchImpl(State state, String glob, int globStart, String path, List<int[]> braceStack) {
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
                        return matchBrace(state, glob, path, braceStack);
                    case ',':
                    case '}':
                        if (state.braceDepth > 0) {
                            skipBranch(state, glob);
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

    private static boolean matchBrace(State state, String glob, String path, List<int[]> braceStack) {
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
                        return matchBraceBranch(state, glob, path, openBraceIndex, branchIndex, braceStack);
                    }
                }
            } else if (c == ',') {
                if (braceDepth == 1) {
                    if (matchBraceBranch(state, glob, path, openBraceIndex, branchIndex, braceStack)) {
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
                                            List<int[]> braceStack) {
        if (braceStack.size() >= MAX_BRACE_DEPTH) {
            return false;
        }
        braceStack.add(new int[]{openBraceIndex, branchIndex});
        State branchState = state.copy();
        branchState.globIndex = branchIndex;
        branchState.braceDepth = braceStack.size();
        boolean matched = globMatchImpl(branchState, glob, branchIndex, path, braceStack);
        braceStack.remove(braceStack.size() - 1);
        return matched;
    }

    private static void skipBranch(State state, String glob) {
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
                    if (state.braceDepth == endBraceDepth) {
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
