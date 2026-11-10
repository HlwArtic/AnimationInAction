import pickle,re,sys,json
items=pickle.load(open('/tmp/neu/items.pkl','rb'))
for _f in ('/tmp/neu/old_items.json','/tmp/neu/old_heads.json','/tmp/neu/old_items3.json'):
    for _k,_v in json.load(open(_f)).items(): items[_k]=_v
def strip(s): return re.sub(r'§.','',s or '')
def tex(v):
    m=re.search(r'Value:\\?"([A-Za-z0-9+/=]+)',v.get('nbttag','')); return m.group(1) if m else None
def rar(v):
    for l in reversed(v.get('lore',[])):
        m=re.search(r'(COMMON|UNCOMMON|RARE|EPIC|LEGENDARY|MYTHIC|DIVINE|SPECIAL)',strip(l))
        if m: return m.group(1)
    return 'LEGENDARY'
def slug(n): return re.sub(r'[^a-z0-9]+','_',n.lower()).strip('_')
def dyecolor(v):
    m=re.search(r'[,{]color:(\d+)',v.get('nbttag','')); return int(m.group(1)) if m else None
rows=[]
def voxel(item,dmg=0,tint=''): return ('voxel',f"{item}|{dmg}|0|{tint}")
def sprite(item,dmg=0): return ('custom',f"sprite:{item}|{dmg}")
def hexc(c): return '%06X'%c
def leather(neu):
    v=items[neu]; item=str(v['itemid']); c=dyecolor(v)
    if c is None: c=0xA06540
    if c==0: c=0x222222
    return voxel(item,0,hexc(c))
def vanilla(neu):
    v=items[neu]; return voxel(str(v['itemid']),v.get('damage',0))
def add(cat,id_,name,regex,neu=None,rarity=None,headers='',flags=(),lk=None):
    rr=rarity or (rar(items[neu]) if neu else 'EPIC')
    if lk is None:
        v=items[neu]; t=tex(v)
        lk=('skull',t) if t else vanilla(neu)
    rows.append((id_,cat,name,rr,regex,headers,','.join(flags),lk[0],lk[1]))

add('REVENANT','warden_heart','Warden Heart',r'Warden\s+Heart','WARDEN_HEART')
add('SVEN','overflux_capacitor','Overflux Capacitor',r'Overflux\s+Capacitor','OVERFLUX_CAPACITOR')
add('SVEN','grizzly_bait','Grizzly Bait',r'Grizzly\s+(?:Bait|Salmon)','GRIZZLY_BAIT')
add('TARANTULA','primordial_eye','Primordial Eye',r'Primordial\s+Eye','PRIMORDIAL_EYE')
add('TARANTULA','shriveled_wasp','Shriveled Wasp',r'Shriveled\s+Wasp','SHRIVELED_WASP')
add('TARANTULA','digested_mosquito','Digested Mosquito',r'Digested\s+Mosquito','DIGESTED_MOSQUITO')
add('ENDERMAN','judgement_core','Judgement Core',r'Judg(?:e)?ment\s+Core','JUDGEMENT_CORE')
add('ENDERMAN','ender_slayer_7','Ender Slayer VII',r'Ender\s+Slayer\s*(?:VII|7)\b',rarity='EPIC',flags=['book'],lk=voxel('minecraft:enchanted_book'))
add('ENDERMAN','exceedingly_rare_ender_artifact_upgrader','Exceedingly Rare Ender Artifact Upgrader',r'Exceedingly\s+Rare\s+Ender\s+Artifact\s+Upgrade[rd]?','EXCEEDINGLY_RARE_ENDER_ARTIFACT_UPGRADER')
add('DEMONLORD','high_class_archfiend_dice','High Class Archfiend Dice',r'High\s+Class\s+Archfiend\s+Dice','HIGH_CLASS_ARCHFIEND_DICE')
add('DEMONLORD','archfiend_dice','Archfiend Dice',r'Archfiend\s+Dice','ARCHFIEND_DICE')
add('DEMONLORD','duplex_1','Duplex I',r'Duplex\s*(?:I|1)\b',rarity='EPIC',flags=['book'],lk=voxel('minecraft:enchanted_book'))
add('DIANA','chimera_1','Chimera I',r'Chimera\s*(?:I|1)\b',rarity='LEGENDARY',flags=['book'],lk=voxel('minecraft:enchanted_book'))
add('DIANA','daedalus_stick','Daedalus Stick',r'Daedalus\s+Stick','DAEDALUS_STICK')
add('DIANA','minos_relic','Minos Relic',r'Minos\s+Relic','MINOS_RELIC')

D='DUNGEONS'
add(D,'bonzo_mask',"Bonzo's Mask",r'Bonzo\W{0,2}s?\s+Mask','BONZO_MASK',flags=['section:Armor'])
add(D,'spirit_mask','Spirit Mask',r'Spirit\s+Mask','SPIRIT_MASK')
add(D,'spirit_boots','Spirit Boots',r'Spirit\s+Boots','THORNS_BOOTS',lk=leather('THORNS_BOOTS'))
add(D,'shadow_assassin_chestplate','Shadow Assassin Chestplate',r'Shadow\s+Assassin\s+Chestplate','SHADOW_ASSASSIN_CHESTPLATE',lk=leather('SHADOW_ASSASSIN_CHESTPLATE'))
add(D,'shadow_assassin_cloak','Shadow Assassin Cloak',r'Shadow\s+Assassin\s+Cloak','SHADOW_ASSASSIN_CLOAK')
add(D,'soulweaver_gloves','Soulweaver Gloves',r'Soulweaver\s+Gloves','SOULWEAVER_GLOVES')
add(D,'necromancer_lord_chestplate','Necromancer Lord Chestplate',r'Necromancer\s+Lord\s+Chestplate','NECROMANCER_LORD_CHESTPLATE',lk=leather('NECROMANCER_LORD_CHESTPLATE'))
add(D,'precursor_eye','Precursor Eye',r'Precursor\s+Eye','PRECURSOR_EYE')
add(D,'wither_chestplate','Wither Chestplate',r'Wither\s+Chestplate','WITHER_CHESTPLATE',lk=leather('WITHER_CHESTPLATE'))
add(D,'wither_leggings','Wither Leggings',r'Wither\s+Leggings','WITHER_LEGGINGS',lk=leather('WITHER_LEGGINGS'))
add(D,'bonzo_staff',"Bonzo's Staff",r'Bonzo\W{0,2}s?\s+Staff','BONZO_STAFF',flags=['section:Swords & weapons'])
add(D,'adaptive_blade','Adaptive Blade',r'Adaptive\s+Blade','STONE_BLADE')
add(D,'hyper_cleaver','Hyper Cleaver',r'Hyper\s+Cleaver','HYPER_CLEAVER')
add(D,'spirit_sword','Spirit Sword',r'Spirit\s+Sword','SPIRIT_SWORD')
add(D,'spirit_shortbow','Spirit Shortbow',r'Spirit\s+Shortbow','ITEM_SPIRIT_BOW')
add(D,'livid_dagger','Livid Dagger',r'Livid\s+Dagger','LIVID_DAGGER')
add(D,'shadow_fury','Shadow Fury',r'Shadow\s+Fury','SHADOW_FURY')
add(D,'last_breath','Last Breath',r'Last\s+Breath','LAST_BREATH')
add(D,'giants_sword',"Giant's Sword",r'Giant\W{0,2}s?\s+Sword','GIANTS_SWORD')
add(D,'necromancer_sword','Necromancer Sword',r'Necromancer\s+Sword','NECROMANCER_SWORD')
add(D,'giant_cleaver','Giant Cleaver',r'Giant\s+Cleaver','GIANT_CLEAVER')
add(D,'dark_claymore','Dark Claymore',r'Dark\s*Claymore','DARK_CLAYMORE')
add(D,'spirit_wing','Spirit Wing',r'Spirit\s+Wing','SPIRIT_WING',flags=['section:Equipment & items'])
add(D,'diamantes_handle',"Diamante's Handle",r'Diamante\W{0,2}s?\s+Handle','GIANT_FRAGMENT_DIAMOND')
add(D,'necrons_handle',"Necron's Handle",r'Necron\W{0,2}s?\s+Handle','NECRON_HANDLE')
add(D,'implosion_scroll','Implosion',r'\bImplosion\b','IMPLOSION_SCROLL')
add(D,'shadow_warp_scroll','Shadow Warp',r'Shadow\s+Warp','SHADOW_WARP_SCROLL')
add(D,'wither_shield_scroll','Wither Shield',r'Wither\s+Shield','WITHER_SHIELD_SCROLL')
add(D,'auto_recombobulator','Auto Recombobulator',r'Auto\s+Recombobulator','AUTO_RECOMBOBULATOR')
for n,w in enumerate(['First','Second','Third','Fourth','Fifth'],1):
    add(D,f'master_star_{n}',f'{w} Master Star',rf'{w}\s+Master\s+Star',f'{w.upper()}_MASTER_STAR')

G='DRAGONS'
first=True
for t in ['Old','Protector','Strong','Superior','Unstable','Wise','Young']:
    grp=f'group:{t.lower()}_dragon_armor'
    for piece,fn in (('Helmet',None),('Chestplate',leather),('Leggings',leather),('Boots',leather)):
        k=f'{t.upper()}_DRAGON_{piece.upper()}'; fl=[grp]
        if first: fl.append('section:Dragon armor sets (helmet + chestplate + leggings + boots)'); first=False
        add(G,slug(f'{t} dragon {piece}'),f'{t} Dragon {piece}',rf'{t}\s+Dragon\s+{piece}',k,flags=fl,lk=(fn(k) if fn else None))
add(G,'aspect_of_the_dragons','Aspect of the Dragons',r'Aspect\s+of\s+the\s+Dragons?','ASPECT_OF_THE_DRAGON',flags=['section:Weapons'])
add(G,'dragon_claw','Dragon Claw',r'Dragon\s+Claw','DRAGON_CLAW',flags=['section:Items'])
add(G,'dragon_horn','Dragon Horn',r'Dragon\s+Horn','DRAGON_HORN')
add(G,'dragon_scale','Dragon Scale',r'Dragon\s+Scale','DRAGON_SCALE')

add('FISHING','deep_sea_orb','Deep Sea Orb',r'Deep\s+Sea\s+Orb','DEEP_SEA_ORB')
add('FISHING','great_white_shark_tooth','Great White Shark Tooth',r'Great\s+White\s+Shark\s+Tooth','GREAT_WHITE_SHARK_TOOTH')
add('FISHING','magma_lord_fragment','Magma Lord Fragment',r'Magma\s*Lord\s+Fragments?','MAGMA_LORD_FRAGMENT')
add('FISHING','music_rune_1','Music Rune I',r'Music\s+Rune\s*(?:I|1)\b','MUSIC_RUNE;1')
add('FISHING','phantom_rod','Phantom Rod',r'Phantom\s+Rod','PHANTOM_ROD')
add('FISHING','radioactive_vial','Radioactive Vial',r'Radioactive\s+Vial','RADIOACTIVE_VIAL')
add('FISHING','soul_fragment','Soul Fragment',r'Soul\s+Fragment','SOUL_FRAGMENT')
add('FISHING','thunder_fragment','Thunder Fragment',r'Thunder\s+(?:Fragment|Shard)s?','THUNDER_SHARDS')
add('FISHING','vampire_fang','Vampire Fang',r'Vampire\s+Fang','PET_ITEM_VAMPIRE_FANG')
for k,n in [('CROPIE','Cropie'),('FERMENTO','Fermento'),('HELIANTHUS','Helianthus'),('SQUASH','Squash')]:
    add('FARMING',slug(n),n,rf'(?<!Condensed\s)\b{n}\b(?!\s+(?:Helmet|Chestplate|Leggings|Boots|Talisman|Ring|Artifact|Relic|Armor|Pet))',k)
add('MINING','divan_alloy',"Divan's Alloy",r'Divan\W{0,2}s?\s+Alloy','DIVAN_ALLOY',flags=['section:Items'])
add('MINING','divans_fragment',"Divan's Fragment",r'Divan\W{0,2}s?\s+Fragment','DIVAN_FRAGMENT')
add('MINING','glacite_jewel','Glacite Jewel',r'Glacite\s+Jewel','GLACITE_JEWEL')
add('MINING','glossy_gemstone','Glossy Gemstone',r'Glossy\s+(?:Gemstone|Mineral)s?','GLOSSY_GEMSTONE')
add('MINING','quick_claw','Quick Claw',r'Quick\s+Claw','PET_ITEM_QUICK_CLAW')
add('MINING','sorrow','Sorrow',r'\bSorrow\b(?!\s+(?:Helmet|Chestplate|Leggings|Boots|Armor))','SORROW')
for i,g in enumerate(['Amber','Amethyst','Aquamarine','Citrine','Jade','Jasper','Onyx','Opal','Peridot','Ruby','Sapphire','Topaz']):
    add('MINING',f'perfect_{g.lower()}_gem',f'Perfect {g} Gemstone',rf'Perfect\s+{g}\s+Gem(?:stone)?',f'PERFECT_{g.upper()}_GEM',flags=(['section:Perfect gemstones'] if i==0 else []))
WANT_DYES=['Aquamarine','Archfiend','Bone','Brick Red','Byzantium','Carmine','Celadon','Celeste','Cyclamen','Dung','Flame','Holly','Livid','Mango','Matcha','Midnight','Mocha','Nadeshiko','Necron','Nyanza','Pearlescent','Periwinkle','Secret','Wild Strawberry']
dyemap={strip(v['displayname']).strip():k for k,v in items.items() if k.startswith('DYE_')}
for base in WANT_DYES:
    n=base+' Dye'; k=dyemap[n]
    rx=r'\s+'.join(re.escape(w) for w in base.split())+r'\s+Dye'
    if base=='Necron': rx=r'Necron\W{0,2}s?\s+Dye'
    if base=='Holly': rx=r'Holl?y\s+Dye'
    if base=='Pearlescent': rx=r'Pearl(?:e)?scent\s+Dye'
    add('DYES',slug(n),n,r'\b'+rx+r'\b',k,flags=['group:dyes','fixed'])
pets={}
for k,v in items.items():
    m=re.fullmatch(r'([A-Z0-9_]+);(\d+)',k); dn=strip(v.get('displayname',''))
    if m and dn.startswith('[Lvl'):
        pets.setdefault(m.group(1),{'name':re.sub(r'^\[Lvl[^\]]*\]\s*','',dn),'v':[]})['v'].append((int(m.group(2)),k))
WANT_PETS=['Phoenix','Enderman','Ender Dragon','Bal','Scatha','Rat','Slug','Snowman','Flying Fish']
for pid,d in sorted(pets.items(),key=lambda x:x[1]['name']):
    if d['name'] not in WANT_PETS: continue
    best=None
    for _,k in sorted(d['v'],reverse=True):
        if tex(items[k]): best=k;break
    n=d['name']; nm=r'\s+'.join(re.escape(w) for w in n.split())
    rx=r'^(?:(?:Common|Uncommon|Rare|Epic|Legendary|Mythic)\s+)?(?:\[Lvl\s*\d+\]\s*)?'+nm+r'(?:\s+Pet)?$'
    add('PETS',slug(n)+'_pet',n,rx,best,rarity='LEGENDARY')
add('MISC','dark_candy','Dark Candy',r'Dark\s+Candy','DARK_CANDY')
add('MISC','golden_dragon_egg','Golden Dragon Egg',r'Golden\s+Dragon\s+Egg','GOLDEN_DRAGON_EGG_NPC',rarity='LEGENDARY')
add('MISC','piece_of_skyblock_code','Piece of Skyblock Code',r'Piece\s+of\s+Skyblock\s+Code',rarity='LEGENDARY',flags=['inv'],lk=('custom','file:piece_of_skyblock_code'))
add('MISC','runebook','Runebook',r'Rune\s*book','RUNEBOOK')
add('MISC','shard_of_pure_wisdom','Shard of Pure Wisdom',r'Shard\s+of\s+Pure\s+Wisdom',rarity='EPIC',flags=['inv'],lk=('custom','file:shard_of_pure_wisdom'))

ids=[r[0] for r in rows]; assert len(ids)==len(set(ids)),[i for i in ids if ids.count(i)>1]
import os
out=os.path.join(os.path.dirname(os.path.abspath(__file__)),'..','src','main','resources','assets','animationinaction','drops.tsv')
with open(out,'w',encoding='utf-8') as f:
    for r in rows:
        assert all('\t' not in c and '\n' not in c for c in r),r
        f.write('\t'.join(r)+'\n')
from collections import Counter
print(len(rows),"entries |",dict(Counter(r[7] for r in rows)))
print({c:n for c,n in Counter(r[1] for r in rows).items()})
print("voxel item types:",sorted({r[8].split('|')[0] for r in rows if r[7]=='voxel'}))
