/*
 * Copyright 2020 the original author or authors.
 * <p>
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * <p>
 * https://www.apache.org/licenses/LICENSE-2.0
 * <p>
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.openrewrite;

/**
 * A marker interface indicating that an implementing {@link SourceVisitor}
 * relies on state from visiting multiple source files to make a transformation.
 *
 * For example, if by visiting a particular Java class a visitor decides to change a field name,
 * that field name needs to be updated in referencing classes.
 */
public interface MultiSourceVisitor {
}
