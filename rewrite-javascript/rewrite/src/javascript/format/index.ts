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

// Main formatting entry point
export * from "./format";

// Individual formatting visitors
export {TabsAndIndentsVisitor} from "./tabs-and-indents-visitor";
export {NormalizeWhitespaceVisitor} from "./normalize-whitespace-visitor";
export {MinimumViableSpacingVisitor} from "./minimum-viable-spacing-visitor";

// Prettier integration
export {prettierFormat, applyPrettierFormatting, getPrettierStyle} from "./prettier-format";
export {PrettierConfigLoader, loadPrettierVersion, clearPrettierModuleCache} from "./prettier-config-loader";
export type {PrettierDetectionResult} from "./prettier-config-loader";

