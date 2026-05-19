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

package recipe

import "testing"

func TestOptionDefaultsToNonSecret(t *testing.T) {
	opt := Option("apiToken", "API token", "API token.")
	if opt.Secret {
		t.Fatalf("expected Secret to default to false")
	}
}

func TestAsSecretSetsFlag(t *testing.T) {
	opt := Option("apiToken", "API token", "API token.").AsSecret()
	if !opt.Secret {
		t.Fatalf("expected AsSecret() to set Secret=true")
	}
	if !opt.Required {
		t.Fatalf("AsSecret() must not clear Required (it remains required by default)")
	}
}

func TestAsSecretPreservesValueAndOtherFields(t *testing.T) {
	// Critical invariant: AsSecret() does NOT redact the runtime Value. The Value
	// stays raw so the recipe can still execute with it; redaction is a persistence-
	// boundary concern handled by consumers (marketplace writer, CLI trace, SaaS).
	opt := Option("apiToken", "API token", "API token.").
		WithValue("hunter2").
		WithExample("***").
		AsSecret()
	if opt.Value != "hunter2" {
		t.Fatalf("expected raw Value to be preserved, got %v", opt.Value)
	}
	if opt.Example != "***" {
		t.Fatalf("expected Example to be preserved, got %v", opt.Example)
	}
	if !opt.Secret {
		t.Fatalf("expected Secret=true")
	}
}
