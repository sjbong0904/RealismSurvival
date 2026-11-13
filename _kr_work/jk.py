import sys,json
sys.path.insert(0,'scripts'); sys.stdout.reconfigure(encoding='utf-8')
import build_korean_resourcepack as b
for n,ns,pat in [('spice-of-life-carrot','solcarrot',''),('biomes-o-plenty','biomesoplenty','tropic'),('pipez','pipez','plugin|pipez'),('creativecore','creativecore',''),('iron-chests','ironchest','')]:
    j=b.find_jar(n); ko=b.load_json_from_jar(j,f'assets/{ns}/lang/ko_kr.json'); en=b.load_json_from_jar(j,f'assets/{ns}/lang/en_us.json')
    import re
    print('==',ns)
    for k,v in ko.items():
        if not pat or re.search(pat,k+v,re.I):
            print(k,'|',en.get(k),'|',v)
