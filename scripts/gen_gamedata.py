import json, sys
src, out = sys.argv[1], sys.argv[2]
d = json.load(open(src))
B = {}
for b in d['buildings']:
    if b.get('village') != 'home' or not b['levels']: continue
    B[b['_id']] = {'n': b['name'], 't': b.get('type'), 'r': b.get('upgrade_resource'),
        'L': [[l.get('required_townhall', 0), l.get('build_time', 0), l.get('build_cost', 0)] for l in b['levels']]}
    if b['_id'] == 1000071:
        B[b['_id']]['L'] = [[l['required_townhall'], l.get('build_time', 0), l.get('build_cost', 0)] for l in b['levels']]
T = {}
for x in d['traps']:
    if x.get('village') != 'home': continue
    T[x['_id']] = {'n': x['name'], 'r': x.get('upgrade_resource'), 'L': [[l.get('required_townhall', 0), l.get('build_time', 0), l.get('build_cost', 0)] for l in x['levels']]}
U = {}
for x in d['troops']:
    if x.get('village') != 'home' or x.get('is_seasonal') or x.get('super_troop'): continue
    U[x['_id']] = {'n': x['name'], 'pb': x.get('production_building'), 'pl': x.get('production_building_level'), 'r': x.get('upgrade_resource'),
        'L': [[l.get('required_townhall', 0), l.get('required_lab_level', 0), l.get('upgrade_time', 0), l.get('upgrade_cost', 0)] for l in x['levels']]}
SUP = {x['_id']: {'n': x['name'], 'o': x['super_troop']['original_id']} for x in d['troops'] if x.get('super_troop') and x.get('village') == 'home'}
SP = {}
for x in d['spells']:
    if x.get('is_seasonal'): continue
    SP[x['_id']] = {'n': x['name'].replace(' Spell', ''), 'pb': x.get('production_building'), 'pl': x.get('production_building_level'), 'r': x.get('upgrade_resource'),
        'L': [[l.get('required_townhall', 0), l.get('required_lab_level', 0), l.get('upgrade_time', 0), l.get('upgrade_cost', 0)] for l in x['levels']]}
H = {}
for x in d['heroes']:
    if x['_id'] in (28000003, 28000005): continue
    H[x['_id']] = {'n': x['name'], 'r': x.get('upgrade_resource'), 'L': [[l.get('required_townhall', 0), l.get('required_hero_tavern_level') or 0, l.get('upgrade_time', 0), l.get('upgrade_cost', 0)] for l in x['levels']]}
P = {}
for x in d['pets']:
    P[x['_id']] = {'n': x['name'], 'L': [[l.get('required_townhall', 0), l.get('required_pet_house_level', 0), l.get('upgrade_time', 0), l.get('upgrade_cost', 0)] for l in x['levels']]}
E = {}
for x in d['equipment']:
    E[x['_id']] = {'n': x['name'], 'h': x['hero'], 'q': x['rarity'], 'L': [[l.get('required_townhall', 0), l.get('required_blacksmith_level', 0)] for l in x['levels']]}
TH = {}
for l in B[1000001]['L']: pass
unl = {}
for b in d['buildings']:
    if b['_id'] == 1000001:
        for l in b['levels']:
            unl[l['level']] = [[u['_id'], u['quantity']] for u in (l.get('unlocks') or [])]
HELP = {x['_id']: x['name'] for x in d['helpers']}
# Builder Base: names only, for tracking running upgrades (no BB planning).
BBN = {x['_id']: x['name'] for k in ('buildings', 'traps', 'troops', 'heroes') for x in d[k] if x.get('village') == 'builderBase'}
data = {'B': B, 'T': T, 'U': U, 'SUP': SUP, 'SP': SP, 'H': H, 'P': P, 'E': E, 'UNL': unl, 'HELP': HELP, 'BBN': BBN}
s = json.dumps(data, separators=(',', ':'))
open(out, 'w').write('const GD=' + s + ';')
print(len(s))
