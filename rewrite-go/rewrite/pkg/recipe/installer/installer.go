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

package installer

import (
	"encoding/json"
	"fmt"
	"os"
	"os/exec"
	"path/filepath"
	"strings"
	"text/template"

	"github.com/openrewrite/rewrite/rewrite-go/pkg/recipe"
)

// RecipeModuleInfo holds information about an installed recipe module.
type RecipeModuleInfo struct {
	ImportPath string
	Version    string
	Recipes    []recipe.RecipeDescriptor
}

// Installer manages the Go recipe workspace and installs recipe modules.
type Installer struct {
	WorkspaceDir string            // e.g., ~/.rewrite/go-recipes/
	Logger       func(string, ...any) // optional logger
}

// NewInstaller creates an Installer with the default workspace directory.
func NewInstaller() *Installer {
	home, _ := os.UserHomeDir()
	return &Installer{
		WorkspaceDir: filepath.Join(home, ".rewrite", "go-recipes"),
	}
}

// ensureWorkspace creates the workspace directory and initializes go.mod if needed.
func (inst *Installer) ensureWorkspace() error {
	if err := os.MkdirAll(inst.WorkspaceDir, 0755); err != nil {
		return fmt.Errorf("create workspace: %w", err)
	}
	goMod := filepath.Join(inst.WorkspaceDir, "go.mod")
	if _, err := os.Stat(goMod); os.IsNotExist(err) {
		content := "module rewrite-recipe-workspace\n\ngo 1.23\n"
		if err := os.WriteFile(goMod, []byte(content), 0644); err != nil {
			return fmt.Errorf("write go.mod: %w", err)
		}
	}
	return nil
}

// InstallFromPath installs recipes from a local Go module path.
func (inst *Installer) InstallFromPath(localPath string, registry *recipe.Registry) (*RecipeModuleInfo, error) {
	if err := inst.ensureWorkspace(); err != nil {
		return nil, err
	}

	// Resolve absolute path
	absPath, err := filepath.Abs(localPath)
	if err != nil {
		return nil, fmt.Errorf("resolve path: %w", err)
	}

	// Read the module path from the local module's go.mod
	modulePath, err := readModulePath(absPath)
	if err != nil {
		return nil, fmt.Errorf("read module path from %s: %w", absPath, err)
	}

	// Add replace directive to workspace go.mod
	if err := inst.addReplace(modulePath, absPath); err != nil {
		return nil, err
	}

	// Add require directive
	if err := inst.addRequire(modulePath, "v0.0.0"); err != nil {
		return nil, err
	}

	// Run go mod tidy
	if err := inst.goCmd("mod", "tidy"); err != nil {
		return nil, fmt.Errorf("go mod tidy: %w", err)
	}

	// Load recipes
	return inst.loadRecipes(modulePath, registry)
}

// InstallFromPackage installs recipes from a remote Go module.
func (inst *Installer) InstallFromPackage(packageName string, version *string, registry *recipe.Registry) (*RecipeModuleInfo, error) {
	if err := inst.ensureWorkspace(); err != nil {
		return nil, err
	}

	// Determine version spec
	versionSpec := "latest"
	if version != nil && *version != "" {
		versionSpec = *version
	}

	// Run go get to fetch the module
	getArg := packageName + "@" + versionSpec
	if err := inst.goCmd("get", "-d", getArg); err != nil {
		return nil, fmt.Errorf("go get %s: %w", getArg, err)
	}

	// Read resolved version from go.mod
	resolvedVersion := inst.readResolvedVersion(packageName)

	// Load recipes
	info, err := inst.loadRecipes(packageName, registry)
	if err != nil {
		return nil, err
	}
	if resolvedVersion != "" {
		info.Version = resolvedVersion
	}
	return info, nil
}

// loadRecipes generates a helper program, builds and runs it to discover recipes.
func (inst *Installer) loadRecipes(modulePath string, registry *recipe.Registry) (*RecipeModuleInfo, error) {
	// Generate the helper program
	helperDir := filepath.Join(inst.WorkspaceDir, "_helper")
	if err := os.MkdirAll(helperDir, 0755); err != nil {
		return nil, fmt.Errorf("create helper dir: %w", err)
	}

	helperSrc := filepath.Join(helperDir, "main.go")
	if err := generateHelper(helperSrc, modulePath); err != nil {
		return nil, fmt.Errorf("generate helper: %w", err)
	}

	// Build the helper
	helperBin := filepath.Join(helperDir, "helper")
	cmd := exec.Command("go", "build", "-o", helperBin, ".")
	cmd.Dir = helperDir
	cmd.Env = append(os.Environ(), "GO111MODULE=on")
	if output, err := cmd.CombinedOutput(); err != nil {
		return nil, fmt.Errorf("build helper: %s: %w", string(output), err)
	}

	// Run the helper to get recipe descriptors
	cmd = exec.Command(helperBin)
	cmd.Dir = helperDir
	output, err := cmd.Output()
	if err != nil {
		return nil, fmt.Errorf("run helper: %w", err)
	}

	// Parse the JSON output
	var descriptors []recipe.RecipeDescriptor
	if err := json.Unmarshal(output, &descriptors); err != nil {
		return nil, fmt.Errorf("parse helper output: %w", err)
	}

	// Register recipes in the registry
	for _, desc := range descriptors {
		registry.RegisterDescriptor(desc)
	}

	// Clean up helper
	os.RemoveAll(helperDir)

	return &RecipeModuleInfo{
		ImportPath: modulePath,
		Recipes:    descriptors,
	}, nil
}

// helperTemplate is the Go source template for the recipe discovery helper program.
var helperTemplate = template.Must(template.New("helper").Parse(`package main

import (
	"encoding/json"
	"fmt"
	"os"

	"github.com/openrewrite/rewrite/rewrite-go/pkg/recipe"
	recipes "{{.ModulePath}}"
)

func main() {
	reg := recipe.NewRegistry()
	recipes.Activate(reg)

	descriptors := reg.AllRecipes()
	data, err := json.Marshal(descriptors)
	if err != nil {
		fmt.Fprintf(os.Stderr, "marshal error: %v\n", err)
		os.Exit(1)
	}
	fmt.Print(string(data))
}
`))

// generateHelper creates a Go program that imports the recipe module,
// calls Activate, and outputs recipe descriptors as JSON.
func generateHelper(path string, modulePath string) error {
	f, err := os.Create(path)
	if err != nil {
		return err
	}
	defer f.Close()

	return helperTemplate.Execute(f, map[string]string{
		"ModulePath": modulePath,
	})
}

// goCmd runs a go command in the workspace directory.
func (inst *Installer) goCmd(args ...string) error {
	cmd := exec.Command("go", args...)
	cmd.Dir = inst.WorkspaceDir
	cmd.Env = append(os.Environ(), "GO111MODULE=on")
	output, err := cmd.CombinedOutput()
	if err != nil {
		return fmt.Errorf("%s: %s", strings.Join(args, " "), string(output))
	}
	if inst.Logger != nil {
		inst.Logger("go %s: %s", strings.Join(args, " "), string(output))
	}
	return nil
}

// addReplace adds a replace directive to the workspace go.mod.
func (inst *Installer) addReplace(modulePath, localPath string) error {
	return inst.goCmd("mod", "edit", "-replace="+modulePath+"="+localPath)
}

// addRequire adds a require directive to the workspace go.mod.
func (inst *Installer) addRequire(modulePath, version string) error {
	return inst.goCmd("mod", "edit", "-require="+modulePath+"@"+version)
}

// readModulePath reads the module path from a go.mod file in the given directory.
func readModulePath(dir string) (string, error) {
	data, err := os.ReadFile(filepath.Join(dir, "go.mod"))
	if err != nil {
		return "", err
	}
	for _, line := range strings.Split(string(data), "\n") {
		line = strings.TrimSpace(line)
		if strings.HasPrefix(line, "module ") {
			return strings.TrimSpace(strings.TrimPrefix(line, "module ")), nil
		}
	}
	return "", fmt.Errorf("no module directive found in go.mod")
}

// readResolvedVersion reads the resolved version of a module from the workspace go.mod.
func (inst *Installer) readResolvedVersion(modulePath string) string {
	data, _ := os.ReadFile(filepath.Join(inst.WorkspaceDir, "go.mod"))
	for _, line := range strings.Split(string(data), "\n") {
		line = strings.TrimSpace(line)
		if strings.Contains(line, modulePath) && !strings.HasPrefix(line, "module") {
			parts := strings.Fields(line)
			if len(parts) >= 2 {
				return parts[len(parts)-1]
			}
		}
	}
	return ""
}
