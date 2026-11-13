import json,sys
sys.path.insert(0,'scripts')
from pathlib import Path
import build_korean_resourcepack as b
nss="sophisticatedbackpacks sophisticatedcore artifacts ironchest treeliable fluidtank creativecore oritech modernfix balm entityculling cristellib shogi terrablender biomesoplenty pipez seasonhud solcarrot".split()
def rd(p):
    p=Path(p)
    return json.loads(p.read_text(encoding='utf-8')) if p.exists() else {}
for ns in nss:
    jar=b.find_jar({'sophisticatedbackpacks':'sophisticated-backpacks','sophisticatedcore':'sophisticated-core','ironchest':'iron-chests','fluidtank':'large-fluid-tank','cristellib':'cristel-lib','biomesoplenty':'biomes-o-plenty','solcarrot':'spice-of-life-carrot'}.get(ns,ns))
    en=ko=None
    en=b.load_json_from_jar(jar,f"assets/{ns}/lang/en_us.json") if jar else {}
    ko=b.load_json_from_jar(jar,f"assets/{ns}/lang/ko_kr.json") if jar else {}
    ov=rd(f'pack/lang-ko/{ns}.json')
    src=rd(f'src/main/resources/assets/{ns}/lang/ko_kr.json')
    print(ns, jar.name if jar else None, 'en',len(en),'jarko',len(ko),'ov',len(ov),'src',len(src), 'ov-notin-en',len(set(ov)-set(en)), 'src-notin-ov',len(set(src)-set(ov)))
    rows=[]
    for k in ov:
        rows.append({'k':k,'en':en.get(k),'jko':ko.get(k),'ov':ov[k],'src':src.get(k)})
    for k in src:
        if k not in ov: rows.append({'k':k,'en':en.get(k),'jko':ko.get(k),'ov':None,'src':src[k]})
    Path(f'_kr_work/{ns}.dump.json').write_text(json.dumps(rows,ensure_ascii=False,indent=0),encoding='utf-8')

