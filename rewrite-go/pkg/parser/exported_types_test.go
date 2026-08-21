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

package parser

import "testing"

func TestGoBuildContextIsHostIndependent(t *testing.T) {
	ctx := goBuildContext()
	for _, f := range []struct{ field, got, want string }{
		{"GOOS", ctx.GOOS, "linux"},
		{"GOARCH", ctx.GOARCH, "amd64"},
		{"Compiler", ctx.Compiler, "gc"},
	} {
		if f.got != f.want {
			t.Errorf("%s = %q, want %q", f.field, f.got, f.want)
		}
	}
	if ctx.CgoEnabled {
		t.Error("CgoEnabled = true, want false")
	}
}
