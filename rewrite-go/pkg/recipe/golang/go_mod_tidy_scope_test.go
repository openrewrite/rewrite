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

import "testing"

func hasImport(imps []string, want string) bool {
	for _, i := range imps {
		if i == want {
			return true
		}
	}
	return false
}

func TestOwnedImportsScopesByModule(t *testing.T) {
	acc := &tidyAcc{
		goModDirs:        map[string]bool{"": true, "internal/tools": true},
		modulePathByDir:  map[string]string{"": "example.com/root", "internal/tools": "example.com/root/internal/tools"},
		requireModsByDir: map[string]map[string]bool{},
		fileImports: map[string][]string{
			"main.go":                   {"github.com/spf13/cobra"},
			"internal/tools/tools.go":   {"github.com/grpc-ecosystem/grpc-gateway/v2/protoc-gen-grpc-gateway"},
			"internal/helper/helper.go": {"github.com/root/own"}, // belongs to root (no nested go.mod here)
		},
	}
	ed := &goModTidyEditor{acc: acc}

	root := ed.ownedImports("")
	if !hasImport(root, "github.com/spf13/cobra") || !hasImport(root, "github.com/root/own") {
		t.Errorf("root module should own main.go and internal/helper imports; got %v", root)
	}
	if hasImport(root, "github.com/grpc-ecosystem/grpc-gateway/v2/protoc-gen-grpc-gateway") {
		t.Errorf("root module must NOT own the nested internal/tools import; got %v", root)
	}

	tools := ed.ownedImports("internal/tools")
	if !hasImport(tools, "github.com/grpc-ecosystem/grpc-gateway/v2/protoc-gen-grpc-gateway") {
		t.Errorf("internal/tools module should own tools.go import; got %v", tools)
	}
	if hasImport(tools, "github.com/spf13/cobra") {
		t.Errorf("internal/tools module must NOT own root imports; got %v", tools)
	}
}
