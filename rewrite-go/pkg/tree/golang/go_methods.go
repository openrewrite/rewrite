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

import (
	"github.com/google/uuid"

	"github.com/openrewrite/rewrite/rewrite-go/pkg/tree/java"
)

func (n *Channel) GetID() uuid.UUID { return n.ID }
func (n *Channel) WithID(id uuid.UUID) java.J {
	if n.ID == id {
		return n
	}
	c := *n
	c.ID = id
	return &c
}
func (n *Channel) GetPrefix() java.Space    { return n.Prefix }
func (n *Channel) GetMarkers() java.Markers { return n.Markers }

func (n *CommClause) GetID() uuid.UUID { return n.ID }
func (n *CommClause) WithID(id uuid.UUID) java.J {
	if n.ID == id {
		return n
	}
	c := *n
	c.ID = id
	return &c
}
func (n *CommClause) GetPrefix() java.Space    { return n.Prefix }
func (n *CommClause) GetMarkers() java.Markers { return n.Markers }

func (n *CompilationUnit) GetID() uuid.UUID { return n.ID }
func (n *CompilationUnit) WithID(id uuid.UUID) java.J {
	if n.ID == id {
		return n
	}
	c := *n
	c.ID = id
	return &c
}
func (n *CompilationUnit) GetPrefix() java.Space    { return n.Prefix }
func (n *CompilationUnit) GetMarkers() java.Markers { return n.Markers }

func (n *Composite) GetID() uuid.UUID { return n.ID }
func (n *Composite) WithID(id uuid.UUID) java.J {
	if n.ID == id {
		return n
	}
	c := *n
	c.ID = id
	return &c
}
func (n *Composite) GetPrefix() java.Space    { return n.Prefix }
func (n *Composite) GetMarkers() java.Markers { return n.Markers }

func (n *Defer) GetID() uuid.UUID { return n.ID }
func (n *Defer) WithID(id uuid.UUID) java.J {
	if n.ID == id {
		return n
	}
	c := *n
	c.ID = id
	return &c
}
func (n *Defer) GetPrefix() java.Space    { return n.Prefix }
func (n *Defer) GetMarkers() java.Markers { return n.Markers }

func (n *Fallthrough) GetID() uuid.UUID { return n.ID }
func (n *Fallthrough) WithID(id uuid.UUID) java.J {
	if n.ID == id {
		return n
	}
	c := *n
	c.ID = id
	return &c
}
func (n *Fallthrough) GetPrefix() java.Space    { return n.Prefix }
func (n *Fallthrough) GetMarkers() java.Markers { return n.Markers }

func (n *FuncType) GetID() uuid.UUID { return n.ID }
func (n *FuncType) WithID(id uuid.UUID) java.J {
	if n.ID == id {
		return n
	}
	c := *n
	c.ID = id
	return &c
}
func (n *FuncType) GetPrefix() java.Space    { return n.Prefix }
func (n *FuncType) GetMarkers() java.Markers { return n.Markers }

func (n *GoStmt) GetID() uuid.UUID { return n.ID }
func (n *GoStmt) WithID(id uuid.UUID) java.J {
	if n.ID == id {
		return n
	}
	c := *n
	c.ID = id
	return &c
}
func (n *GoStmt) GetPrefix() java.Space    { return n.Prefix }
func (n *GoStmt) GetMarkers() java.Markers { return n.Markers }

func (n *Goto) GetID() uuid.UUID { return n.ID }
func (n *Goto) WithID(id uuid.UUID) java.J {
	if n.ID == id {
		return n
	}
	c := *n
	c.ID = id
	return &c
}
func (n *Goto) GetPrefix() java.Space    { return n.Prefix }
func (n *Goto) GetMarkers() java.Markers { return n.Markers }

func (n *IndexList) GetID() uuid.UUID { return n.ID }
func (n *IndexList) WithID(id uuid.UUID) java.J {
	if n.ID == id {
		return n
	}
	c := *n
	c.ID = id
	return &c
}
func (n *IndexList) GetPrefix() java.Space    { return n.Prefix }
func (n *IndexList) GetMarkers() java.Markers { return n.Markers }

func (n *InterfaceType) GetID() uuid.UUID { return n.ID }
func (n *InterfaceType) WithID(id uuid.UUID) java.J {
	if n.ID == id {
		return n
	}
	c := *n
	c.ID = id
	return &c
}
func (n *InterfaceType) GetPrefix() java.Space    { return n.Prefix }
func (n *InterfaceType) GetMarkers() java.Markers { return n.Markers }

func (n *KeyValue) GetID() uuid.UUID { return n.ID }
func (n *KeyValue) WithID(id uuid.UUID) java.J {
	if n.ID == id {
		return n
	}
	c := *n
	c.ID = id
	return &c
}
func (n *KeyValue) GetPrefix() java.Space    { return n.Prefix }
func (n *KeyValue) GetMarkers() java.Markers { return n.Markers }

func (n *MapType) GetID() uuid.UUID { return n.ID }
func (n *MapType) WithID(id uuid.UUID) java.J {
	if n.ID == id {
		return n
	}
	c := *n
	c.ID = id
	return &c
}
func (n *MapType) GetPrefix() java.Space    { return n.Prefix }
func (n *MapType) GetMarkers() java.Markers { return n.Markers }

func (n *MultiAssignment) GetID() uuid.UUID { return n.ID }
func (n *MultiAssignment) WithID(id uuid.UUID) java.J {
	if n.ID == id {
		return n
	}
	c := *n
	c.ID = id
	return &c
}
func (n *MultiAssignment) GetPrefix() java.Space    { return n.Prefix }
func (n *MultiAssignment) GetMarkers() java.Markers { return n.Markers }

func (n *Return) GetID() uuid.UUID { return n.ID }
func (n *Return) WithID(id uuid.UUID) java.J {
	if n.ID == id {
		return n
	}
	c := *n
	c.ID = id
	return &c
}
func (n *Return) GetPrefix() java.Space    { return n.Prefix }
func (n *Return) GetMarkers() java.Markers { return n.Markers }

func (n *MethodDeclaration) GetID() uuid.UUID { return n.ID }
func (n *MethodDeclaration) WithID(id uuid.UUID) java.J {
	if n.ID == id {
		return n
	}
	c := *n
	c.ID = id
	return &c
}
func (n *MethodDeclaration) GetPrefix() java.Space    { return n.Prefix }
func (n *MethodDeclaration) GetMarkers() java.Markers { return n.Markers }

func (n *PointerType) GetID() uuid.UUID { return n.ID }
func (n *PointerType) WithID(id uuid.UUID) java.J {
	if n.ID == id {
		return n
	}
	c := *n
	c.ID = id
	return &c
}
func (n *PointerType) GetPrefix() java.Space    { return n.Prefix }
func (n *PointerType) GetMarkers() java.Markers { return n.Markers }

func (n *Unary) GetID() uuid.UUID { return n.ID }
func (n *Unary) WithID(id uuid.UUID) java.J {
	if n.ID == id {
		return n
	}
	c := *n
	c.ID = id
	return &c
}
func (n *Unary) GetPrefix() java.Space    { return n.Prefix }
func (n *Unary) GetMarkers() java.Markers { return n.Markers }

func (n *Binary) GetID() uuid.UUID { return n.ID }
func (n *Binary) WithID(id uuid.UUID) java.J {
	if n.ID == id {
		return n
	}
	c := *n
	c.ID = id
	return &c
}
func (n *Binary) GetPrefix() java.Space    { return n.Prefix }
func (n *Binary) GetMarkers() java.Markers { return n.Markers }

func (n *AssignmentOperation) GetID() uuid.UUID { return n.ID }
func (n *AssignmentOperation) WithID(id uuid.UUID) java.J {
	if n.ID == id {
		return n
	}
	c := *n
	c.ID = id
	return &c
}
func (n *AssignmentOperation) GetPrefix() java.Space    { return n.Prefix }
func (n *AssignmentOperation) GetMarkers() java.Markers { return n.Markers }

func (n *Variadic) GetID() uuid.UUID { return n.ID }
func (n *Variadic) WithID(id uuid.UUID) java.J {
	if n.ID == id {
		return n
	}
	c := *n
	c.ID = id
	return &c
}
func (n *Variadic) GetPrefix() java.Space    { return n.Prefix }
func (n *Variadic) GetMarkers() java.Markers { return n.Markers }

func (n *Send) GetID() uuid.UUID { return n.ID }
func (n *Send) WithID(id uuid.UUID) java.J {
	if n.ID == id {
		return n
	}
	c := *n
	c.ID = id
	return &c
}
func (n *Send) GetPrefix() java.Space    { return n.Prefix }
func (n *Send) GetMarkers() java.Markers { return n.Markers }

func (n *Slice) GetID() uuid.UUID { return n.ID }
func (n *Slice) WithID(id uuid.UUID) java.J {
	if n.ID == id {
		return n
	}
	c := *n
	c.ID = id
	return &c
}
func (n *Slice) GetPrefix() java.Space    { return n.Prefix }
func (n *Slice) GetMarkers() java.Markers { return n.Markers }

func (n *StatementExpression) GetID() uuid.UUID { return n.ID }
func (n *StatementExpression) WithID(id uuid.UUID) java.J {
	if n.ID == id {
		return n
	}
	c := *n
	c.ID = id
	return &c
}
func (n *StatementExpression) GetPrefix() java.Space    { return n.Prefix }
func (n *StatementExpression) GetMarkers() java.Markers { return n.Markers }

func (n *StructType) GetID() uuid.UUID { return n.ID }
func (n *StructType) WithID(id uuid.UUID) java.J {
	if n.ID == id {
		return n
	}
	c := *n
	c.ID = id
	return &c
}
func (n *StructType) GetPrefix() java.Space    { return n.Prefix }
func (n *StructType) GetMarkers() java.Markers { return n.Markers }

func (n *TypeDecl) GetID() uuid.UUID { return n.ID }
func (n *TypeDecl) WithID(id uuid.UUID) java.J {
	if n.ID == id {
		return n
	}
	c := *n
	c.ID = id
	return &c
}
func (n *TypeDecl) GetPrefix() java.Space    { return n.Prefix }
func (n *TypeDecl) GetMarkers() java.Markers { return n.Markers }

func (n *DeclarationBlock) GetID() uuid.UUID { return n.ID }
func (n *DeclarationBlock) WithID(id uuid.UUID) java.J {
	if n.ID == id {
		return n
	}
	c := *n
	c.ID = id
	return &c
}
func (n *DeclarationBlock) GetPrefix() java.Space    { return n.Prefix }
func (n *DeclarationBlock) GetMarkers() java.Markers { return n.Markers }

func (n *TypeList) GetID() uuid.UUID { return n.ID }
func (n *TypeList) WithID(id uuid.UUID) java.J {
	if n.ID == id {
		return n
	}
	c := *n
	c.ID = id
	return &c
}
func (n *TypeList) GetPrefix() java.Space    { return n.Prefix }
func (n *TypeList) GetMarkers() java.Markers { return n.Markers }

func (n *Union) GetID() uuid.UUID { return n.ID }
func (n *Union) WithID(id uuid.UUID) java.J {
	if n.ID == id {
		return n
	}
	c := *n
	c.ID = id
	return &c
}
func (n *Union) GetPrefix() java.Space    { return n.Prefix }
func (n *Union) GetMarkers() java.Markers { return n.Markers }

func (n *UnderlyingType) GetID() uuid.UUID { return n.ID }
func (n *UnderlyingType) WithID(id uuid.UUID) java.J {
	if n.ID == id {
		return n
	}
	c := *n
	c.ID = id
	return &c
}
func (n *UnderlyingType) GetPrefix() java.Space    { return n.Prefix }
func (n *UnderlyingType) GetMarkers() java.Markers { return n.Markers }
