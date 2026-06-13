# Copyright 2025 the original author or authors.
# <p>
# Licensed under the Moderne Source Available License (the "License");
# you may not use this file except in compliance with the License.
# You may obtain a copy of the License at
# <p>
# https://docs.moderne.io/licensing/moderne-source-available-license
# <p>
# Unless required by applicable law or agreed to in writing, software
# distributed under the License is distributed on an "AS IS" BASIS,
# WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
# See the License for the specific language governing permissions and
# limitations under the License.

"""Pluggable type-attribution SPI for rewrite-python.

This is the Python analogue of rewrite-java's ``DefaultJavaTypeFactory`` vs a
table-backed factory split: it makes the type-attribution implementation the
parser uses pluggable behind a process-global seam.

Two implementations are envisioned:

* The **default** (:class:`DefaultJavaTypeFactory`, ships here) reproduces
  rewrite-python's long-standing *deep* behavior — full type attribution that
  crosses every Python package boundary (third-party and stdlib alike are fully
  expanded). A standalone rewrite-python user always gets this and is unaffected
  by any boundary work.

* A **table-backed alternative** (provided externally by the moderne-cli side
  and registered at RPC-server startup — NOT built here) drives ty-types with a
  first-party boundary so attribution cuts at package boundaries (emitting
  ``classRef`` shells) and resolves the cut types on demand from V3 type-table
  ``.bin`` files.

Why the default must stay deep: a ``classRef`` shell is a dead end unless
something can resolve it back to a full type, and only the type tables (which
the CLI owns) can. So **the boundary must not be applied by the default
factory** — only the table-backed alternative, which owns the tables, may cut.
Concretely, the default :meth:`JavaTypeFactory.initialize_session` ignores
``first_party_root`` and always expands fully; the ``firstPartyRoot`` config
channel is plumbed all the way to here, but its *use* is gated behind an
alternative factory.

The two override seams an alternative cares about:

1. :meth:`JavaTypeFactory.initialize_session` — whether/how the first-party
   boundary is applied when initializing ty-types (default: never — deep).
2. :meth:`JavaTypeFactory.resolve_class_ref` — how an external / ``classRef``
   class reference is resolved (default: leave it as the body-less shell the
   shared mapper already produces).
"""

from __future__ import annotations

from abc import ABC
from typing import TYPE_CHECKING, Any

if TYPE_CHECKING:
    from rewrite.java import JavaType


class JavaTypeFactory(ABC):
    """SPI for pluggable Python type attribution.

    All methods carry a working *deep* default, so an alternative implementation
    overrides only the seam(s) it needs. The base class is therefore the
    contract and the deep behavior at once; :class:`DefaultJavaTypeFactory` is
    the concrete shipping default.
    """

    def create_ty_client(self, dependency_path: str | None = None):
        """Create the ty-types client for a parse batch.

        Returns ``None`` when ty-types is unavailable (package not installed or
        binary missing), in which case the parser proceeds without type
        attribution. The default points ty-types at the caller-provisioned
        dependency environment so supertypes reaching into third-party packages
        resolve.

        Owning client creation here is what lets ``handle_parse_project`` obtain
        the factory instead of hard-instantiating ``TyTypesClient``; an
        alternative may return a client wired to its type tables.
        """
        try:
            from rewrite.python.ty_client import TyTypesClient
            return TyTypesClient(virtual_env=dependency_path)
        except (ImportError, RuntimeError):
            return None

    def initialize_session(self, ty_client, project_root: str,
                           first_party_root: str | None = None) -> bool:
        """Seam 1 — initialize the ty-types session for this parse batch.

        Default behavior is **deep**: ``first_party_root`` is ignored entirely,
        so types expand across every package boundary exactly as standalone
        rewrite-python always has. A boundary-cutting alternative overrides this
        to forward ``first_party_root`` (and owns the type tables that make the
        resulting ``classRef`` shells resolvable).
        """
        return ty_client.initialize(project_root)

    def resolve_class_ref(self, shell: JavaType.Class,
                          descriptor: dict[str, Any]) -> JavaType:
        """Seam 2 — resolve an out-of-boundary class reference.

        Called by the shared mapper for a ty-types ``classRef`` descriptor,
        passing the body-less :class:`JavaType.Class` shell it built (FQN + kind
        only) and the raw descriptor. The default leaves it as that shell — and
        is inert in deep mode, which never emits a ``classRef`` since the
        boundary is never applied. A table-backed alternative resolves the shell
        to a full type from V3 type-table ``.bin`` files, keyed by the shell's
        fully qualified name.
        """
        return shell


class DefaultJavaTypeFactory(JavaTypeFactory):
    """The deep, boundary-crossing type attribution that ships with
    openrewrite. Inherits every default unchanged; named explicitly so callers
    and tests can assert identity and so the registry has a concrete default.
    """


# Process-global registered factory. One RPC-server process attributes types
# through a single factory; the default deep impl is used until something (e.g.
# the moderne-cli table-backed side, after importing this package into the RPC
# server) registers an alternative.
_factory: JavaTypeFactory = DefaultJavaTypeFactory()


def register_java_type_factory(factory: JavaTypeFactory) -> None:
    """Register the process-global type-attribution factory, replacing the
    default. Intended to be called once at RPC-server startup by a host (e.g.
    moderne-cli) that provides a table-backed implementation.
    """
    global _factory
    _factory = factory


def get_java_type_factory() -> JavaTypeFactory:
    """Return the registered process-global factory.

    Defaults to :class:`DefaultJavaTypeFactory` (deep) until
    :func:`register_java_type_factory` installs an alternative.
    """
    return _factory


def reset_java_type_factory() -> None:
    """Restore the default deep factory. Primarily for tests that register an
    alternative and need to undo it.
    """
    global _factory
    _factory = DefaultJavaTypeFactory()
