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
export * from "./tree";
export * from "./visitor";
export * from "./assertions";
export * from "./parser";
export * from "./style";
export * from "./markers";
export * from "./node-resolution-result";
export * from "./package-json-parser";
export * from "./package-manager";
export * from "./dependency-manager";
export * from "./preconditions";
export * from "./templating/index";
export * from "./method-matcher";
export * from "./format";
export * from "./autodetect";
export * from "./tree-debug";
export * from "./project-parser";

export * from "./scope";
export type {QuoteChar, AddImportOptions} from "./add-import";
export {ImportStyle, moduleNameOf, AddImport} from "./add-import";
// AMD mechanics `recipes-ui5` builds on directly, beyond the `maybeBind` surface below.
export type {AmdBlock} from "./amd";
export {
    DEFAULT_AMD_CALLEES, amdBlockOf, dependencyNames, parameterNames, withDependency, withoutDependencyAt,
    RemoveAmdDependency, removeNewlyUnusedAmdBindings
} from "./amd";
export type {MaybeBindOptions, MaybeUnbindOptions, ModuleBindings} from "./binding";
export {maybeBind, maybeUnbind, moduleBindings, isAmdBlock, maybeRemoveImport, maybeAddImport} from "./binding";
export * from "./remove-import";
export * from "./cleanup/index";
export * from "./recipes/index";
export * from "./search/index";

import "./print";
import "./rpc";
