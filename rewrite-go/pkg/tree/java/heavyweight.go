/*
 * Copyright 2026 the original author or authors.
 *
 * Licensed under the Moderne Source Available License (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * https://docs.moderne.io/licensing/moderne-source-available-license
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package java

// Every tree is a recipe.Heavyweight.
func (*Identifier) IsHeavyweight()            {}
func (*Literal) IsHeavyweight()               {}
func (*Binary) IsHeavyweight()                {}
func (*Block) IsHeavyweight()                 {}
func (*Return) IsHeavyweight()                {}
func (*If) IsHeavyweight()                    {}
func (*Else) IsHeavyweight()                  {}
func (*Assignment) IsHeavyweight()            {}
func (*AssignmentOperation) IsHeavyweight()   {}
func (*MethodDeclaration) IsHeavyweight()     {}
func (*TypeParameters) IsHeavyweight()        {}
func (*TypeParameter) IsHeavyweight()         {}
func (*ForLoop) IsHeavyweight()               {}
func (*ForControl) IsHeavyweight()            {}
func (*ForEachLoop) IsHeavyweight()           {}
func (*ForEachControl) IsHeavyweight()        {}
func (*Switch) IsHeavyweight()                {}
func (*Case) IsHeavyweight()                  {}
func (*Break) IsHeavyweight()                 {}
func (*Continue) IsHeavyweight()              {}
func (*Label) IsHeavyweight()                 {}
func (*Annotation) IsHeavyweight()            {}
func (*Empty) IsHeavyweight()                 {}
func (*Unary) IsHeavyweight()                 {}
func (*FieldAccess) IsHeavyweight()           {}
func (*MethodInvocation) IsHeavyweight()      {}
func (*VariableDeclarations) IsHeavyweight()  {}
func (*VariableDeclarator) IsHeavyweight()    {}
func (*ArrayType) IsHeavyweight()             {}
func (*Parentheses) IsHeavyweight()           {}
func (*ParenthesizedTypeTree) IsHeavyweight() {}
func (*TypeCast) IsHeavyweight()              {}
func (*ControlParentheses) IsHeavyweight()    {}
func (*ArrayAccess) IsHeavyweight()           {}
func (*ArrayDimension) IsHeavyweight()        {}
func (*ParameterizedType) IsHeavyweight()     {}
func (*Import) IsHeavyweight()                {}
func (*ParseError) IsHeavyweight()            {}
