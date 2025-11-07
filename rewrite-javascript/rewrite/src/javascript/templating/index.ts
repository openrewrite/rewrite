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

// Export public types
export type {
    VariadicOptions,
    CaptureOptions,
    Capture,
    Any,
    TemplateParam,
    PatternOptions,
    TemplateParameter,
    TemplateOptions,
    RewriteRule,
    RewriteConfig
} from './types';

// Export capture functionality
export {
    and,
    or,
    not,
    capture,
    any,
    param,
    _
} from './capture';

// Export pattern functionality
export {
    Pattern,
    PatternBuilder,
    MatchResult,
    pattern
} from './pattern';

// Export rewrite functionality
export {
    rewrite,
    fromRecipe
} from './rewrite';

// Export template functionality
export {
    Template,
    TemplateBuilder,
    template
} from './template';
