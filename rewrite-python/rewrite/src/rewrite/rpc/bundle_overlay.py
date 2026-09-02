"""A private module table per recipe bundle, so every bundle runs in this one process.

``sys.modules`` is process-global, so two bundles' incompatible versions of one package cannot both
be ``sys.modules['dep']`` at once. They take turns instead: entering a bundle swaps its entries in
and swaps them back out on the way through, so a package the bundle brought resolves to its own
copy for as long as its code is running.

A module belongs to a bundle when it was loaded from that bundle's venv. Deciding it that way
rather than by name keeps the engine shared without a list to maintain: the engine is imported from
the shared engine root, so it matches no bundle and every bundle sees the one instance. A
dependency the engine declares is already imported and so resolves to the engine's copy — the same
answer a child gets, whose ``PYTHONPATH`` puts the engine roots first.
"""
import sys
import threading
from contextlib import contextmanager
from pathlib import Path

# Entering a bundle mutates process-global state, so only one may be active at a time. The RPC
# server reads and serves one request at a time, so this never contends.
_lock = threading.RLock()


class Bundle:
    def __init__(self, dist: str, site_packages):
        self.dist = dist
        self._prefix = str(Path(site_packages).resolve()) + "/"
        self.modules = {}
        self._depth = 0

    def _owns(self, module) -> bool:
        origin = getattr(getattr(module, "__spec__", None), "origin", None)
        return bool(origin) and origin.startswith(self._prefix)

    @contextmanager
    def active(self):
        """Run the body with this bundle's modules installed, capturing whatever it imports."""
        with _lock:
            self._depth += 1
            if self._depth > 1:
                try:
                    yield self          # a recipe calling into its own bundle re-enters; the
                finally:                # outermost frame owns the swap
                    self._depth -= 1
                return
            saved_path = sys.path[:]
            displaced = {}
            for name, module in self.modules.items():
                if name in sys.modules:
                    displaced[name] = sys.modules[name]
                sys.modules[name] = module
            sys.path.insert(0, self._prefix)
            before = set(sys.modules)
            try:
                yield self
            finally:
                for name in set(sys.modules) - before:
                    if self._owns(sys.modules[name]):
                        self.modules[name] = sys.modules.pop(name)
                for name in self.modules:
                    sys.modules.pop(name, None)
                sys.modules.update(displaced)
                sys.path[:] = saved_path
                self._depth -= 1
