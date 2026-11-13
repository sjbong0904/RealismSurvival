import json,glob,sys,os
sys.stdout.reconfigure(encoding='utf-8')
base=os.path.expandvars(r'%APPDATA%\.minecraft\assets')
best=None
for f in glob.glob(base+'/indexes/*.json'):
    d=json.load(open(f,encoding='utf-8'))['objects']
    if 'minecraft/lang/ko_kr.json' in d:
        print(f, os.path.getmtime(f)); 
        h=d['minecraft/lang/ko_kr.json']['hash']; h2=h
        if best is None or os.path.getmtime(f)>best[0]: best=(os.path.getmtime(f),h,h2)
ko=json.load(open(f'{base}/objects/{best[1][:2]}/{best[1]}',encoding='utf-8'))
en=json.load(open(f'{base}/objects/{best[2][:2]}/{best[2]}',encoding='utf-8'))
json.dump(ko,open('_kr_work/vanilla_ko.json','w',encoding='utf-8'),ensure_ascii=False)

print(len(ko),len(en))

