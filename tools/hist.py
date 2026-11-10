import re,json,urllib.request,sys,time
BASE='NotEnoughUpdates/NotEnoughUpdates-REPO'
def get(url,t=30):
    req=urllib.request.Request(url,headers={'User-Agent':'Mozilla/5.0'})
    return urllib.request.urlopen(req,timeout=t).read().decode('utf-8','ignore')
def shas_for(item):
    out=[]
    h=get(f'https://github.com/{BASE}/commits/master/items/{item}.json')
    for s in re.findall(r'/commit/([0-9a-f]{40})',h):
        if s not in out: out.append(s)
    return out
def version(item,sha):
    try: return json.loads(get(f'https://raw.githubusercontent.com/{BASE}/{sha}/items/{item}.json'))
    except Exception as e: return None
def tex(v):
    m=re.search(r'Value:\\?"([A-Za-z0-9+/=]+)',v.get('nbttag','')); return m.group(1) if m else None
if __name__=='__main__':
    for item in sys.argv[1:]:
        try: shas=shas_for(item)
        except Exception as e: print(item,'history fail',e); continue
        print(f"\n== {item}: {len(shas)} commits")
        for s in shas:
            v=version(item,s)
            if not v: print("  ",s[:8],"(file missing at this commit)"); continue
            print("  ",s[:8],v.get('itemid'),v.get('damage'),'skull-texture' if tex(v) else '-', (v.get('modver') or '')[:20])
