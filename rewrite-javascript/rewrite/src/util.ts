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
export async function mapAsync<T, U>(arr: T[], fn: (t: T, i: number) => Promise<U | undefined>): Promise<U[]> {
    let results: U[] | undefined = undefined;
    
    for (let i = 0; i < arr.length; i++) {
        const result = await fn(arr[i], i);
        
        if (result !== arr[i]) {
            if (results === undefined) {
                results = arr.slice(0, i) as unknown[] as U[];
            }
            
            if (result !== undefined) {
                results.push(result);
            }
        } else if (results !== undefined && result !== undefined) {
            results.push(result);
        }
    }
    
    return results === undefined ? arr as unknown[] as U[] : results;
}
