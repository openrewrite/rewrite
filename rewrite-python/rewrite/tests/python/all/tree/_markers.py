"""Shared pytest markers for type attribution tests."""
import json
import os
import shutil
import subprocess
import tempfile

import pytest


def _ty_types_has_module_name() -> bool:
    """Check if the installed ty-types CLI provides moduleName on descriptors."""
    if shutil.which('ty-types') is None:
        return False
    fname = None
    try:
        with tempfile.NamedTemporaryFile(mode='w', suffix='.py', delete=False) as f:
            f.write('class _C:\n    pass\n')
            fname = f.name
        result = subprocess.run(['ty-types', fname], capture_output=True, text=True, timeout=30)
        data = json.loads(result.stdout)
        return any(
            'moduleName' in d
            for d in data.get('types', {}).values()
        )
    except Exception:
        return False
    finally:
        if fname is not None:
            try:
                os.unlink(fname)
            except OSError:
                pass


requires_module_name = pytest.mark.skipif(
    not _ty_types_has_module_name(),
    reason="ty-types CLI does not provide moduleName on descriptors"
)
