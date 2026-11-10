import os, glob, json, pickle, shutil, subprocess, sys
here = os.path.dirname(os.path.abspath(__file__))
os.makedirs('/tmp/neu', exist_ok=True)
zp = '/tmp/neu/neu.zip'
if not os.path.exists(zp):
    subprocess.check_call(['curl', '-sL', '-m', '300', '-o', zp, 'https://codeload.github.com/NotEnoughUpdates/NotEnoughUpdates-REPO/zip/refs/heads/master'])
subprocess.check_call(['unzip', '-q', '-o', zp, '-d', '/tmp/neu/repo'])
root = glob.glob('/tmp/neu/repo/*/')[0]
items = {}
for f in glob.glob(root + 'items/*.json'):
    try: items[os.path.basename(f)[:-5]] = json.load(open(f, encoding='utf-8'))
    except Exception: pass
pickle.dump(items, open('/tmp/neu/items.pkl', 'wb'))
for f in glob.glob(os.path.join(here, 'data', 'old_*.json')): shutil.copy(f, '/tmp/neu/')
print(len(items), 'items ready in /tmp/neu')
