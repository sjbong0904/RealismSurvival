import sys,json,re
sys.path.insert(0,'scripts'); sys.stdout.reconfigure(encoding='utf-8')
import build_korean_resourcepack as b
j=b.find_jar('oritech'); ko=b.load_json_from_jar(j,'assets/oritech/lang/ko_kr.json'); en=b.load_json_from_jar(j,'assets/oritech/lang/en_us.json')
for k,v in ko.items():
    e=en.get(k,'')
    if re.search(r'transfer|\bOil\b|crude|Waterlogged|Peak|Minimum|stored',e,re.I) and len(e)<120:
        print(k,'|',e.replace('\n','\\n'),'|',v.replace('\n','\\n'))
