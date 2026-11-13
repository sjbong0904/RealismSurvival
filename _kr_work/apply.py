import json,sys,importlib.util
from pathlib import Path
sys.stdout.reconfigure(encoding='utf-8')
ns=sys.argv[1]
spec=importlib.util.spec_from_file_location('e',f'_kr_work/edits_{ns}.py'); m=importlib.util.module_from_spec(spec); spec.loader.exec_module(m)
p=Path(f'pack/lang-ko/{ns}.json')
raw=p.read_bytes(); crlf=b'\r\n' in raw
d=json.loads(raw.decode('utf-8'))
chg=0;add=0
for k,v in m.E.items():
    if k in d:
        if d[k]!=v: chg+=1
        d[k]=v
    else:
        d[k]=v; add+=1
s=json.dumps(d,ensure_ascii=False,indent=2)+'\n'
if crlf: s=s.replace('\n','\r\n')
p.write_bytes(s.encode('utf-8'))
print(ns,'changed',chg,'added',add,'total',len(d))
