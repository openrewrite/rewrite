import unittest

from rewrite import GeneralFormatStyle
from rewrite.python.format.normalize_line_breaks_visitor import NormalizeLineBreaksVisitor
from rewrite.test import from_visitor, RecipeSpec, rewrite_run, python


class TestNormalizeLineBreaksVisitor(unittest.TestCase):

    def setUp(self):
        # language=python
        self.windows = (
            "class Test:\r\n"
            "    # some comment\r\n"
            "    def test(self):\r\n"
            "        print()\r\n"
            "        a = [\r\n"
            "              1,\r\n"
            "              2,\r\n"
            "            ]\r\n"
            "\r\n"
        )
        # language=python
        self.linux = (
            "class Test:\n"
            "    # some comment\n"
            "    def test(self):\n"
            "        print()\n"
            "        a = [\n"
            "              1,\n"
            "              2,\n"
            "            ]\n"
            "\n"
        )

    @staticmethod
    def normalize_line_breaks(use_crlf: bool) -> RecipeSpec:
        style = GeneralFormatStyle(_use_crlf_new_lines=use_crlf)
        return RecipeSpec().with_recipe(from_visitor(NormalizeLineBreaksVisitor(style)))

    def test_windows_to_linux(self):
        rewrite_run(
            python(
                self.windows,
                self.linux
            ),
            spec=self.normalize_line_breaks(use_crlf=False)
        )

    def test_linux_to_windows(self):
        rewrite_run(
            python(
                self.linux,
                self.windows
            ),
            spec=self.normalize_line_breaks(use_crlf=True)
        )
