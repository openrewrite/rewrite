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
import {afterAll, beforeAll, beforeEach} from "vitest";
import {RecipeSpec} from "../src/test";

// Specs are registered as they are constructed, which happens while a test file is being imported.
RecipeSpec.trackSuiteConfiguration();

// Vitest runs a file's describe bodies to collect it before running any of its tests, so what a
// spec holds here is the suite's own configuration, and what it holds later is one test's.
beforeAll(() => RecipeSpec.captureSuiteConfiguration());
beforeEach(() => RecipeSpec.restoreSuiteConfiguration());
afterAll(() => RecipeSpec.forgetSuiteSpecs());
