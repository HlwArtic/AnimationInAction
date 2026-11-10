from PIL import Image
import numpy as np
FL={'top':1.0,'left':0.634,'right':0.436}
def load(p): return np.array(Image.open(p).convert('RGB')).astype(float)
def bilerp(img,x,y):
    x=min(max(x-0.5,0),img.shape[1]-1.001); y=min(max(y-0.5,0),img.shape[0]-1.001)
    x0,y0=int(x),int(y); fx,fy=x-x0,y-y0
    return (img[y0,x0]*(1-fx)*(1-fy)+img[y0,x0+1]*fx*(1-fy)+img[y0+1,x0]*(1-fx)*fy+img[y0+1,x0+1]*fx*fy)
def tiles(img,bb,n=8):
    x0,y0,x1,y1=bb; x1+=1; y1+=1; W=x1-x0; H=y1-y0; cx=(x0+x1)/2
    T=np.array((cx,y0));UR=np.array((x1,y0+H/4));LR=np.array((x1,y0+3*H/4));B=np.array((cx,y1));LL=np.array((x0,y0+3*H/4));UL=np.array((x0,y0+H/4));C=np.array((cx,y0+H/2))
    def quad(P00,P10,P01,P11):
        t=np.zeros((n,n,3))
        for j in range(n):
            for i in range(n):
                u=(i+0.5)/n; v=(j+0.5)/n
                p=P00*(1-u)*(1-v)+P10*u*(1-v)+P01*(1-u)*v+P11*u*v
                acc=0;k=0
                for dx in(-0.5,0,0.5):
                    for dy in(-0.5,0,0.5): acc=acc+bilerp(img,p[0]+dx,p[1]+dy);k+=1
                t[j,i]=acc/k
        return t
    return quad(UL,T,C,UR),quad(UL,C,LL,B),quad(C,UR,B,LR)
def avg_tiles(img,bbs):
    ts=[tiles(img,bb) for bb in bbs]
    return tuple(np.mean([t[i] for t in ts],axis=0) for i in range(3))
def unshade(t,f): return np.clip(t/f,0,255)
def build_skin(top,left,right,chamfer=2):
    top=unshade(top,FL['top']); left=unshade(left,FL['left']); right=unshade(right,FL['right'])
    sk=np.zeros((64,64,4),dtype=np.uint8)
    def put(x0,y0,tile,mult=1.0):
        for j in range(8):
            for i in range(8):
                a=255
                if chamfer and (i+j<chamfer or (7-i)+j<chamfer or i+(7-j)<chamfer or (7-i)+(7-j)<chamfer): a=0
                sk[y0+j,x0+i,:3]=np.clip(tile[j,i]*mult,0,255); sk[y0+j,x0+i,3]=a
    put(8,0,top); put(16,0,top,0.85); put(0,8,right); put(8,8,left); put(16,8,right); put(24,8,left)
    return sk
def segment_runs(img,box,pred):
    x0,y0,x1,y1=box; sub=img[y0:y1,x0:x1].astype(int); m=pred(sub)
    cols=np.where(m.any(axis=0))[0]; runs=[];s=cols[0];p=cols[0]
    for c in cols[1:]:
        if c-p>3: runs.append((s,p)); s=c
        p=c
    runs.append((s,p)); out=[]
    for a,b_ in runs:
        if b_-a<20: continue
        ys=np.where(m[:,a:b_+1].any(axis=1))[0]; out.append((a+x0,ys.min()+y0,b_+x0,ys.max()+y0))
    return out
