# Copyright 2026 the original author or authors.
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

"""Tests for the DatetimeUtcNow recipe."""

from rewrite.test import RecipeSpec, python
from rewrite.python.recipes import DatetimeUtcNow


class TestDatetimeUtcNow:
    """Tests for the DatetimeUtcNow recipe."""

    # --- utcnow tests ---

    def test_from_import_utcnow(self):
        """from datetime import datetime + datetime.utcnow() -> adds timezone, rewrites call."""
        spec = RecipeSpec(recipe=DatetimeUtcNow())
        spec.rewrite_run(
            python(
                """\
                from datetime import datetime
                x = datetime.utcnow()
                """,
                """\
                from datetime import datetime, timezone
                x = datetime.now(timezone.utc)
                """,
            )
        )

    def test_direct_import_utcnow(self):
        """import datetime + datetime.datetime.utcnow() -> rewrites with datetime.timezone.utc."""
        spec = RecipeSpec(recipe=DatetimeUtcNow())
        spec.rewrite_run(
            python(
                """\
                import datetime
                x = datetime.datetime.utcnow()
                """,
                """\
                import datetime
                x = datetime.datetime.now(datetime.timezone.utc)
                """,
            )
        )

    def test_no_change_when_no_datetime_import(self):
        """No change when there's no datetime import."""
        spec = RecipeSpec(recipe=DatetimeUtcNow())
        spec.rewrite_run(
            python(
                """\
                x = datetime.utcnow()
                """,
            )
        )

    def test_no_change_when_already_now(self):
        """No change when method is already now()."""
        spec = RecipeSpec(recipe=DatetimeUtcNow())
        spec.rewrite_run(
            python(
                """\
                from datetime import datetime, timezone
                x = datetime.now(timezone.utc)
                """,
            )
        )

    # --- utcfromtimestamp tests ---

    def test_from_import_utcfromtimestamp(self):
        """from datetime import datetime + datetime.utcfromtimestamp(ts) -> fromtimestamp(ts, tz=timezone.utc)."""
        spec = RecipeSpec(recipe=DatetimeUtcNow())
        spec.rewrite_run(
            python(
                """\
                from datetime import datetime
                x = datetime.utcfromtimestamp(ts)
                """,
                """\
                from datetime import datetime, timezone
                x = datetime.fromtimestamp(ts, tz=timezone.utc)
                """,
            )
        )

    def test_direct_import_utcfromtimestamp(self):
        """import datetime + datetime.datetime.utcfromtimestamp(ts) -> fromtimestamp(ts, tz=datetime.timezone.utc)."""
        spec = RecipeSpec(recipe=DatetimeUtcNow())
        spec.rewrite_run(
            python(
                """\
                import datetime
                x = datetime.datetime.utcfromtimestamp(ts)
                """,
                """\
                import datetime
                x = datetime.datetime.fromtimestamp(ts, tz=datetime.timezone.utc)
                """,
            )
        )
