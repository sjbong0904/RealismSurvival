# -*- coding: utf-8 -*-
import zipfile
from pathlib import Path

jar = Path.home() / (
    ".gradle/caches/modules-2/files-2.1/dev.ftb.mods/ftb-quests/"
    "26.1.2.9/27bad0ad9d181386f7ffa61d85dabfd95136b08c/ftb-quests-26.1.2.9.jar"
)
z = zipfile.ZipFile(jar)
for name in z.namelist():
    if not name.endswith(".class"):
        continue
    data = z.read(name)
    if b"processInitialTranslation" in data:
        print("FOUND", name)
