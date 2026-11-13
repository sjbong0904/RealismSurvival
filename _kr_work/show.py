import json,sys
sys.stdout.reconfigure(encoding='utf-8')
for ns in sys.argv[1:]:
    print('=====',ns)
    for r in json.load(open(f'_kr_work/{ns}.dump.json',encoding='utf-8')):
        print(f"[{r['k']}]\n EN: {r['en']}\n OV: {r['ov']}" + ("" if r['src'] is None or r['src']==r['ov'] else f"\n SRC: {r['src']}") + (f"\n JKO: {r['jko']}" if r['jko'] and r['jko']!=r['ov'] else ""))
