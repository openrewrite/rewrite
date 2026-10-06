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

/**
 * The package a bare module specifier, or a path below `node_modules`, names. Attribution names a
 * type declared in an installed package after it, so `@scope/pkg/sub` gives `@scope/pkg`.
 */
export function packageNameOf(specifier: string): string {
    const segments = specifier.split('/');
    return normalizePackageName(segments[0].startsWith('@') && segments.length > 1
        ? `${segments[0]}/${segments[1]}`
        : segments[0]);
}

/**
 * The specifier consumers import a node_modules package under. A DefinitelyTyped package
 * `@types/<pkg>` names `<pkg>`, and `@types/testing-library__react` names `@testing-library/react`.
 * Types reached directly and through a declaration file's path then share one name.
 */
function normalizePackageName(packageName: string): string {
    if (packageName.startsWith('@types/')) {
        packageName = packageName.substring('@types/'.length);
        // Decode __ encoding for scoped packages: testing-library__react -> @testing-library/react
        if (packageName.includes('__')) {
            const parts = packageName.split('__');
            if (parts.length === 2) {
                packageName = `@${parts[0]}/${parts[1]}`;
            }
        }
    }
    return packageName;
}
