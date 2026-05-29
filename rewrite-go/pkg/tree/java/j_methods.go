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

import "github.com/google/uuid"

func (n *Annotation) GetID() uuid.UUID { return n.ID }
func (n *Annotation) WithID(id uuid.UUID) J {
	if n.ID == id {
		return n
	}
	c := *n
	c.ID = id
	return &c
}
func (n *Annotation) GetPrefix() Space    { return n.Prefix }
func (n *Annotation) GetMarkers() Markers { return n.Markers }

func (n *ArrayAccess) GetID() uuid.UUID { return n.ID }
func (n *ArrayAccess) WithID(id uuid.UUID) J {
	if n.ID == id {
		return n
	}
	c := *n
	c.ID = id
	return &c
}
func (n *ArrayAccess) GetPrefix() Space    { return n.Prefix }
func (n *ArrayAccess) GetMarkers() Markers { return n.Markers }

func (n *ArrayDimension) GetID() uuid.UUID { return n.ID }
func (n *ArrayDimension) WithID(id uuid.UUID) J {
	if n.ID == id {
		return n
	}
	c := *n
	c.ID = id
	return &c
}
func (n *ArrayDimension) GetPrefix() Space    { return n.Prefix }
func (n *ArrayDimension) GetMarkers() Markers { return n.Markers }

func (n *ArrayType) GetID() uuid.UUID { return n.ID }
func (n *ArrayType) WithID(id uuid.UUID) J {
	if n.ID == id {
		return n
	}
	c := *n
	c.ID = id
	return &c
}
func (n *ArrayType) GetPrefix() Space    { return n.Prefix }
func (n *ArrayType) GetMarkers() Markers { return n.Markers }

func (n *Assignment) GetID() uuid.UUID { return n.ID }
func (n *Assignment) WithID(id uuid.UUID) J {
	if n.ID == id {
		return n
	}
	c := *n
	c.ID = id
	return &c
}
func (n *Assignment) GetPrefix() Space    { return n.Prefix }
func (n *Assignment) GetMarkers() Markers { return n.Markers }

func (n *AssignmentOperation) GetID() uuid.UUID { return n.ID }
func (n *AssignmentOperation) WithID(id uuid.UUID) J {
	if n.ID == id {
		return n
	}
	c := *n
	c.ID = id
	return &c
}
func (n *AssignmentOperation) GetPrefix() Space    { return n.Prefix }
func (n *AssignmentOperation) GetMarkers() Markers { return n.Markers }

func (n *Binary) GetID() uuid.UUID { return n.ID }
func (n *Binary) WithID(id uuid.UUID) J {
	if n.ID == id {
		return n
	}
	c := *n
	c.ID = id
	return &c
}
func (n *Binary) GetPrefix() Space    { return n.Prefix }
func (n *Binary) GetMarkers() Markers { return n.Markers }

func (n *Block) GetID() uuid.UUID { return n.ID }
func (n *Block) WithID(id uuid.UUID) J {
	if n.ID == id {
		return n
	}
	c := *n
	c.ID = id
	return &c
}
func (n *Block) GetPrefix() Space    { return n.Prefix }
func (n *Block) GetMarkers() Markers { return n.Markers }

func (n *Break) GetID() uuid.UUID { return n.ID }
func (n *Break) WithID(id uuid.UUID) J {
	if n.ID == id {
		return n
	}
	c := *n
	c.ID = id
	return &c
}
func (n *Break) GetPrefix() Space    { return n.Prefix }
func (n *Break) GetMarkers() Markers { return n.Markers }

func (n *Case) GetID() uuid.UUID { return n.ID }
func (n *Case) WithID(id uuid.UUID) J {
	if n.ID == id {
		return n
	}
	c := *n
	c.ID = id
	return &c
}
func (n *Case) GetPrefix() Space    { return n.Prefix }
func (n *Case) GetMarkers() Markers { return n.Markers }

func (n *Continue) GetID() uuid.UUID { return n.ID }
func (n *Continue) WithID(id uuid.UUID) J {
	if n.ID == id {
		return n
	}
	c := *n
	c.ID = id
	return &c
}
func (n *Continue) GetPrefix() Space    { return n.Prefix }
func (n *Continue) GetMarkers() Markers { return n.Markers }

func (n *ControlParentheses) GetID() uuid.UUID { return n.ID }
func (n *ControlParentheses) WithID(id uuid.UUID) J {
	if n.ID == id {
		return n
	}
	c := *n
	c.ID = id
	return &c
}
func (n *ControlParentheses) GetPrefix() Space    { return n.Prefix }
func (n *ControlParentheses) GetMarkers() Markers { return n.Markers }

func (n *Else) GetID() uuid.UUID { return n.ID }
func (n *Else) WithID(id uuid.UUID) J {
	if n.ID == id {
		return n
	}
	c := *n
	c.ID = id
	return &c
}
func (n *Else) GetPrefix() Space    { return n.Prefix }
func (n *Else) GetMarkers() Markers { return n.Markers }

func (n *Empty) GetID() uuid.UUID { return n.ID }
func (n *Empty) WithID(id uuid.UUID) J {
	if n.ID == id {
		return n
	}
	c := *n
	c.ID = id
	return &c
}
func (n *Empty) GetPrefix() Space    { return n.Prefix }
func (n *Empty) GetMarkers() Markers { return n.Markers }

func (n *FieldAccess) GetID() uuid.UUID { return n.ID }
func (n *FieldAccess) WithID(id uuid.UUID) J {
	if n.ID == id {
		return n
	}
	c := *n
	c.ID = id
	return &c
}
func (n *FieldAccess) GetPrefix() Space    { return n.Prefix }
func (n *FieldAccess) GetMarkers() Markers { return n.Markers }

func (n *ForControl) GetID() uuid.UUID { return n.ID }
func (n *ForControl) WithID(id uuid.UUID) J {
	if n.ID == id {
		return n
	}
	c := *n
	c.ID = id
	return &c
}
func (n *ForControl) GetPrefix() Space    { return n.Prefix }
func (n *ForControl) GetMarkers() Markers { return n.Markers }

func (n *ForEachControl) GetID() uuid.UUID { return n.ID }
func (n *ForEachControl) WithID(id uuid.UUID) J {
	if n.ID == id {
		return n
	}
	c := *n
	c.ID = id
	return &c
}
func (n *ForEachControl) GetPrefix() Space    { return n.Prefix }
func (n *ForEachControl) GetMarkers() Markers { return n.Markers }

func (n *ForEachLoop) GetID() uuid.UUID { return n.ID }
func (n *ForEachLoop) WithID(id uuid.UUID) J {
	if n.ID == id {
		return n
	}
	c := *n
	c.ID = id
	return &c
}
func (n *ForEachLoop) GetPrefix() Space    { return n.Prefix }
func (n *ForEachLoop) GetMarkers() Markers { return n.Markers }

func (n *ForLoop) GetID() uuid.UUID { return n.ID }
func (n *ForLoop) WithID(id uuid.UUID) J {
	if n.ID == id {
		return n
	}
	c := *n
	c.ID = id
	return &c
}
func (n *ForLoop) GetPrefix() Space    { return n.Prefix }
func (n *ForLoop) GetMarkers() Markers { return n.Markers }

func (n *Identifier) GetID() uuid.UUID { return n.ID }
func (n *Identifier) WithID(id uuid.UUID) J {
	if n.ID == id {
		return n
	}
	c := *n
	c.ID = id
	return &c
}
func (n *Identifier) GetPrefix() Space    { return n.Prefix }
func (n *Identifier) GetMarkers() Markers { return n.Markers }

func (n *If) GetID() uuid.UUID { return n.ID }
func (n *If) WithID(id uuid.UUID) J {
	if n.ID == id {
		return n
	}
	c := *n
	c.ID = id
	return &c
}
func (n *If) GetPrefix() Space    { return n.Prefix }
func (n *If) GetMarkers() Markers { return n.Markers }

func (n *Import) GetID() uuid.UUID { return n.ID }
func (n *Import) WithID(id uuid.UUID) J {
	if n.ID == id {
		return n
	}
	c := *n
	c.ID = id
	return &c
}
func (n *Import) GetPrefix() Space    { return n.Prefix }
func (n *Import) GetMarkers() Markers { return n.Markers }

func (n *Label) GetID() uuid.UUID { return n.ID }
func (n *Label) WithID(id uuid.UUID) J {
	if n.ID == id {
		return n
	}
	c := *n
	c.ID = id
	return &c
}
func (n *Label) GetPrefix() Space    { return n.Prefix }
func (n *Label) GetMarkers() Markers { return n.Markers }

func (n *Literal) GetID() uuid.UUID { return n.ID }
func (n *Literal) WithID(id uuid.UUID) J {
	if n.ID == id {
		return n
	}
	c := *n
	c.ID = id
	return &c
}
func (n *Literal) GetPrefix() Space    { return n.Prefix }
func (n *Literal) GetMarkers() Markers { return n.Markers }

func (n *MethodDeclaration) GetID() uuid.UUID { return n.ID }
func (n *MethodDeclaration) WithID(id uuid.UUID) J {
	if n.ID == id {
		return n
	}
	c := *n
	c.ID = id
	return &c
}
func (n *MethodDeclaration) GetPrefix() Space    { return n.Prefix }
func (n *MethodDeclaration) GetMarkers() Markers { return n.Markers }

func (n *MethodInvocation) GetID() uuid.UUID { return n.ID }
func (n *MethodInvocation) WithID(id uuid.UUID) J {
	if n.ID == id {
		return n
	}
	c := *n
	c.ID = id
	return &c
}
func (n *MethodInvocation) GetPrefix() Space    { return n.Prefix }
func (n *MethodInvocation) GetMarkers() Markers { return n.Markers }

func (n *ParameterizedType) GetID() uuid.UUID { return n.ID }
func (n *ParameterizedType) WithID(id uuid.UUID) J {
	if n.ID == id {
		return n
	}
	c := *n
	c.ID = id
	return &c
}
func (n *ParameterizedType) GetPrefix() Space    { return n.Prefix }
func (n *ParameterizedType) GetMarkers() Markers { return n.Markers }

func (n *Parentheses) GetID() uuid.UUID { return n.ID }
func (n *Parentheses) WithID(id uuid.UUID) J {
	if n.ID == id {
		return n
	}
	c := *n
	c.ID = id
	return &c
}
func (n *Parentheses) GetPrefix() Space    { return n.Prefix }
func (n *Parentheses) GetMarkers() Markers { return n.Markers }

func (n *Return) GetID() uuid.UUID { return n.ID }
func (n *Return) WithID(id uuid.UUID) J {
	if n.ID == id {
		return n
	}
	c := *n
	c.ID = id
	return &c
}
func (n *Return) GetPrefix() Space    { return n.Prefix }
func (n *Return) GetMarkers() Markers { return n.Markers }

func (n *Switch) GetID() uuid.UUID { return n.ID }
func (n *Switch) WithID(id uuid.UUID) J {
	if n.ID == id {
		return n
	}
	c := *n
	c.ID = id
	return &c
}
func (n *Switch) GetPrefix() Space    { return n.Prefix }
func (n *Switch) GetMarkers() Markers { return n.Markers }

func (n *TypeCast) GetID() uuid.UUID { return n.ID }
func (n *TypeCast) WithID(id uuid.UUID) J {
	if n.ID == id {
		return n
	}
	c := *n
	c.ID = id
	return &c
}
func (n *TypeCast) GetPrefix() Space    { return n.Prefix }
func (n *TypeCast) GetMarkers() Markers { return n.Markers }

func (n *Unary) GetID() uuid.UUID { return n.ID }
func (n *Unary) WithID(id uuid.UUID) J {
	if n.ID == id {
		return n
	}
	c := *n
	c.ID = id
	return &c
}
func (n *Unary) GetPrefix() Space    { return n.Prefix }
func (n *Unary) GetMarkers() Markers { return n.Markers }

func (n *VariableDeclarations) GetID() uuid.UUID { return n.ID }
func (n *VariableDeclarations) WithID(id uuid.UUID) J {
	if n.ID == id {
		return n
	}
	c := *n
	c.ID = id
	return &c
}
func (n *VariableDeclarations) GetPrefix() Space    { return n.Prefix }
func (n *VariableDeclarations) GetMarkers() Markers { return n.Markers }

func (n *VariableDeclarator) GetID() uuid.UUID { return n.ID }
func (n *VariableDeclarator) WithID(id uuid.UUID) J {
	if n.ID == id {
		return n
	}
	c := *n
	c.ID = id
	return &c
}
func (n *VariableDeclarator) GetPrefix() Space    { return n.Prefix }
func (n *VariableDeclarator) GetMarkers() Markers { return n.Markers }
