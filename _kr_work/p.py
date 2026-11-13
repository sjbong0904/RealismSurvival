import json,sys
sys.path.insert(0,'scripts')
from pathlib import Path
import build_korean_resourcepack as b
ROOT=Path('.')
nss="sophisticatedbackpacks sophisticatedcore artifacts ironchest treeliable fluidtank creativecore oritech modernfix balm entityculling cristellib shogi terrablender biomesoplenty pipez seasonhud solcarrot".split()
import inspect
print(inspect.getsource(b.build_namespace)); print(inspect.getsource(b.find_jar))
