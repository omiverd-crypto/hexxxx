import os, numpy as np
from PIL import Image
D = 'src/main/resources/assets/hexpowers/textures/fx/'
def load(n, size=None):
    img = Image.open(D+n+'.png').convert('RGBA')
    if size: img = img.resize((size,size), Image.LANCZOS)
    return np.asarray(img, float)/255.0
def tint(a, rgb, k=1.0):
    return a[...,3:4]*np.array(rgb)[None,None,:]/255.0*k
def sheet(items, cols, cell, bg=(8,8,14)):
    rows=(len(items)+cols-1)//cols
    canvas=np.zeros((rows*cell, cols*cell,3))+np.array(bg)/255.0
    for i,(img) in enumerate(items):
        im=Image.fromarray((np.clip(img,0,1)*255).astype(np.uint8)).resize((cell,cell), Image.LANCZOS)
        r,c=divmod(i,cols); canvas[r*cell:(r+1)*cell, c*cell:(c+1)*cell]=np.asarray(im)/255.0
    return canvas
RED=(228,18,45); BLUE=(62,155,255); PUR=(168,69,255)
def layered(sig, rgb, ring_rgb):
    s=tint(load(sig),rgb)*1.0; r=tint(load('rune_ring'),ring_rgb)*0.9
    # mimic stack: sigil + counter-rotated rune ring slightly larger (just sum)
    g=tint(load('glow',512),rgb)*0.55
    return np.clip(g+s+r*0.7,0,1)
items=[layered('sigil_chaos',RED,(255,120,140)), layered('sigil_arcane',BLUE,(160,210,255)), layered('sigil_hex',PUR,(210,150,255))]
Image.fromarray((sheet(items,3,420)*255).astype(np.uint8)).save('/tmp/prev_sigils.png')
# plain sigils, no ring, to judge line art
items=[tint(load(n),c) for n,c in (('sigil_chaos',RED),('sigil_arcane',BLUE),('sigil_hex',PUR))]
Image.fromarray((sheet(items,3,420)*255).astype(np.uint8)).save('/tmp/prev_sigils_plain.png')
# misc sheet
hg=load('hex_grid'); tile=np.tile(tint(hg,RED), (2,2,1))[:444, :768]
misc=[tint(load('rune_ring'),PUR), tint(load('swirl'),RED), tint(load('shock_ring'),BLUE),
      tint(load('flare',512),RED)*1.0+tint(load('glow',512),RED)*0.4, tint(load('glow',512),PUR)]
Image.fromarray((sheet(misc,5,300)*255).astype(np.uint8)).save('/tmp/prev_misc.png')
Image.fromarray((np.clip(tile,0,1)*255).astype(np.uint8)).save('/tmp/prev_hex.png')
# beam + pillar + ravens
b=tint(load('beam'),BLUE); b2=tint(load('beam'),RED)
beams=np.vstack([b,b2,tint(load('beam'),PUR)])
Image.fromarray((np.clip(beams,0,1)*255).astype(np.uint8)).save('/tmp/prev_beams.png')
p=tint(load('pillar'),PUR); p=np.hstack([p,tint(load('pillar'),RED),tint(load('pillar'),BLUE)])
Image.fromarray((np.clip(p,0,1)*255).astype(np.uint8)).save('/tmp/prev_pillars.png')
rv=Image.open(D+'raven.png').convert('RGBA'); bgi=Image.new('RGBA',rv.size,(70,60,90,255)); bgi.alpha_composite(rv)
bgi.resize((rv.width*3, rv.height*3), Image.LANCZOS).convert('RGB').save('/tmp/prev_ravens.png')
print('ok')
