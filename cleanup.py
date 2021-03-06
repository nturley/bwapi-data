import json

useful_unit_props = [
    u'mineralPrice',
    u'gasPrice',
    u'requiredUnits',
    u'whatBuilds',
    u'supplyRequired',
    u'supplyProvided',
    u'getRace',
    u'buildTime',
    u'requiredTech'
]

useful_tech_props = [
    u'mineralPrice',
    u'gasPrice',
    u'getRace',
    u'researchTime',
    u'whatResearches',
    u'requiredUnit'
]

useful_upgrade_props = [
    u'mineralPrice',
    u'gasPrice',
    u'upgradeTime',
    u'whatUpgrades',
    u'whatsRequired',
    u'getRace'
]



def clean(s):
    if type(s) is list:
        return [clean(i) for i in s]
    try:
        return s.replace(u'Zerg_', '').replace(u'Protoss_', '').replace(u'Terran_', '')
    except:
        return s

with open('types.json') as f:
    rawjson = json.load(f)
    ut = {
        clean(u[u'toString']): {
            key : clean(value) for (key, value) in u.iteritems() if key in useful_unit_props
        } for u in rawjson[u'unitTypes'] if not u[u'whatBuilds'] in [u'None', u'Unknown']
    }
    tt = {
        t[u'toString']: {
            key: clean(value) for (key, value) in t.iteritems() if key in useful_tech_props
        } for t in rawjson[u'techTypes'] if not t[u'whatResearches'] in [u'None', u'Unknown']
    }
    upt = {
        (u[u'toString'] if not u'level' in u else u[u'toString'] + u" L" + str(u[u'level'])): {
            key: clean(value) for (key, value) in u.iteritems() if key in useful_upgrade_props
        } for u in rawjson[u'upgradeTypes']
    }

types = {'unitTypes': ut, 'techTypes': tt, 'upgradeTypes': upt}
with open('cleanTypes.json', 'w') as f:
    json.dump(types, f, sort_keys=True, indent=2, separators=(',', ': '))