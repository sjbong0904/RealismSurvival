import sys,json,re
sys.path.insert(0,'scripts'); sys.stdout.reconfigure(encoding='utf-8')
import build_korean_resourcepack as b
j=b.find_jar('oritech'); ko=b.load_json_from_jar(j,'assets/oritech/lang/ko_kr.json'); en=b.load_json_from_jar(j,'assets/oritech/lang/en_us.json')
for k,v in ko.items():
    e=en.get(k,'')
    if re.search(r'Resonator|Resource Node|Quarry|Drone|Destroyer|Fluid|source|Oracle|Sneak|loaded chunk|Interaction|Trip|Addon|addon|resonance',e) :
        print(k,'|',e[:90].replace('\n','\\n'),'|',v[:100].replace('\n','\\n'))
