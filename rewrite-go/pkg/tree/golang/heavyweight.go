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

package golang

// Every tree is a recipe.Heavyweight.
func (*CompilationUnit) IsHeavyweight()     {}
func (*GoStmt) IsHeavyweight()              {}
func (*Defer) IsHeavyweight()               {}
func (*Send) IsHeavyweight()                {}
func (*Goto) IsHeavyweight()                {}
func (*Fallthrough) IsHeavyweight()         {}
func (*Composite) IsHeavyweight()           {}
func (*KeyValue) IsHeavyweight()            {}
func (*Slice) IsHeavyweight()               {}
func (*ArrayType) IsHeavyweight()           {}
func (*MapType) IsHeavyweight()             {}
func (*PointerType) IsHeavyweight()         {}
func (*Channel) IsHeavyweight()             {}
func (*FuncType) IsHeavyweight()            {}
func (*StructType) IsHeavyweight()          {}
func (*InterfaceType) IsHeavyweight()       {}
func (*TypeList) IsHeavyweight()            {}
func (*Union) IsHeavyweight()               {}
func (*UnderlyingType) IsHeavyweight()      {}
func (*TypeDecl) IsHeavyweight()            {}
func (*DeclarationBlock) IsHeavyweight()    {}
func (*MultiAssignment) IsHeavyweight()     {}
func (*Return) IsHeavyweight()              {}
func (*MethodDeclaration) IsHeavyweight()   {}
func (*StatementWithInit) IsHeavyweight()   {}
func (*CommClause) IsHeavyweight()          {}
func (*Select) IsHeavyweight()              {}
func (*ExpressionStatement) IsHeavyweight() {}
func (*TypeAssertion) IsHeavyweight()       {}
func (*StatementExpression) IsHeavyweight() {}
func (*IndexList) IsHeavyweight()           {}
func (*Unary) IsHeavyweight()               {}
func (*Binary) IsHeavyweight()              {}
func (*AssignmentOperation) IsHeavyweight() {}
func (*Variadic) IsHeavyweight()            {}
func (*GoMod) IsHeavyweight()               {}
func (*GoModDirective) IsHeavyweight()      {}
func (*GoModBlock) IsHeavyweight()          {}
func (*GoModValue) IsHeavyweight()          {}
func (*GoSum) IsHeavyweight()               {}
func (*GoSumLine) IsHeavyweight()           {}
