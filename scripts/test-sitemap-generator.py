#!/usr/bin/env python3

import importlib.util
import sys
import tempfile
import xml.etree.ElementTree as ET
from pathlib import Path


sys.dont_write_bytecode = True
script_path = Path(__file__).with_name("generate-public-sitemap.py")
spec = importlib.util.spec_from_file_location("generate_public_sitemap", script_path)
module = importlib.util.module_from_spec(spec)
assert spec.loader is not None
spec.loader.exec_module(module)

assert module.extract_last_modified('<script>{"dateModified":"2026-08-31"}</script>') == "2026-08-31"
assert module.extract_last_modified('<script>{"dateModified":"2026-99-99"}</script>') is None
assert module.extract_last_modified('<html></html>') is None

with tempfile.TemporaryDirectory() as temporary_directory:
    output = Path(temporary_directory) / "sitemap.xml"
    module.write_atomically(
        [
            ("https://cleonhardt.de/", None),
            ("https://cleonhardt.de/insights/test", "2026-09-02"),
        ],
        output,
    )
    root = ET.parse(output).getroot()
    entries = list(root)
    assert entries[0].find("{*}lastmod") is None
    assert entries[1].find("{*}lastmod").text == "2026-09-02"

print("Sitemap-Generatorcheck bestanden: valide CMS-Daten werden als lastmod ausgegeben.")
