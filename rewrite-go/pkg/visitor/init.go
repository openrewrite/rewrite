/*
 * Copyright 2025 the original author or authors.
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

package visitor

import "reflect"

// Init sets the Self field on a visitor to enable virtual dispatch,
// then returns the visitor. Works with any visitor struct that has
// an exported Self field (GoVisitor, JavaVisitor, XmlVisitor, etc.).
//
//	func (r *myRecipe) Editor() recipe.TreeVisitor {
//	    return visitor.Init(&myVisitor{oldName: r.OldName})
//	}
func Init[T any](v *T) *T {
	rv := reflect.ValueOf(v).Elem()
	for i := 0; i < rv.NumField(); i++ {
		f := rv.Field(i)
		if rv.Type().Field(i).Name == "Self" && f.CanSet() {
			f.Set(reflect.ValueOf(v))
			return v
		}
		// Check embedded structs for a Self field.
		if rv.Type().Field(i).Anonymous && f.Kind() == reflect.Struct {
			sf := f.FieldByName("Self")
			if sf.IsValid() && sf.CanSet() {
				sf.Set(reflect.ValueOf(v))
				return v
			}
		}
	}
	return v
}
