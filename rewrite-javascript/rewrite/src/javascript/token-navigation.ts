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

// Punctuation and keyword tokens are absent from node fields, and the LST needs their offsets to
// place whitespace. The compiler leaves them out of the tree it sends, so they are scanned back out
// of the source text; ts7/token-navigation.ts does that and this is the surface the parser reads.

export {childAt, childCountOf, childrenOf, firstTokenOf, lastTokenOf} from "./ts7/token-navigation";
