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
const versions=['0.0.0','0.5.0','0.9.0','1.0.0-alpha','1.0.0','1.2.0','1.2.3','1.3.0-alpha','1.3.0','2.0.0-alpha','2.0.0','2.5.0','3.0.0-alpha','3.0.0','1.0.0+build-1'];
const ranges=['>1','>1.2','<x','>*','<=1','<=1.2','<1','<1.2','^1.2.3','~1.2.3','1.x','1.2.x','>=0.5 0','1.0.0 || ^2.0.0','=2.5.0 <2','>=1.0.0-alpha <2','1 - 2.x','18446744073709551616.0.0','1.0.0+build-1'];
console.log('# Recorded with '+Bun.version+' '+Bun.revision);
for(const range of ranges)for(const version of versions)console.log(`${version}\t${range}\t${Bun.semver.satisfies(version,range)}`);
