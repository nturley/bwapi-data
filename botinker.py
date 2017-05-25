import json

START_BUILD = 80

class Creatable:
    def __init__(self, name):
        self.name = name
        self.busy = False
        self.next_events = []

    def save_data(self, bo):
        self.data = bo.creatables[self.name]

class Event:
    def __init__(self, frame, resourceDelta=None, unbusy=None, remove=None, add=None):
        self.frame = frame
        self.resourceDelta = resourceDelta
        # creatable to make unbusy
        self.unbusy = unbusy
        # name of unit to add
        self.add = add
        # creatable to remove
        self.remove = remove

class GameState:
    def __init__(self, minerals, gas, creatables, bo, frame):
        self.bo = bo
        self.frame = frame
        self.resources = Price(minerals, gas, 0)
        for c in creatables:
            c.save_data(bo)
            self.resources.supply += c.data['supplyProvided']
            self.resources.supply -= c.data['supplyRequired']
        self.creatables = creatables

    def has(self, name):
        return any([c.name == name for c in self.creatables])

    def has_idle(self, name):
        idlers = [ c for c in self.creatables if c.name == name and not c.busy]
        if idlers:
            return idlers[0]
        return None

    def can_create(self, name):
        requirements = self.bo.required(name)
        if not all([self.has(r) for r in requirements]):
            return False
        creator = self.has_idle( self.bo.whatCreates(name))
        if creator is None:
            return False
        price = self.bo.getCost(name)
        if not self.resources.canAfford(price):
            return False
        return True

    def createEvents(self, name):
        creator = self.has_idle(self.bo.whatCreates(name))
        if creator is None:
            raise Exception('cant find creator')
        creator.busy = True
        buildtime = self.bo.creatables[name]['createTime']
        cost = self.bo.getCost(name)
        if creator in ['SCV', 'Probe', 'Drone']:
            startEvent = Event(self.frame + START_BUILD, cost)
            completeEvent = Event(startEvent.frame + buildtime, add=name)
            if creator.name == 'SCV':
                completeEvent.unbusy = creator
            if creator.name == 'Probe':
                startEvent.unbusy = creator
            if creator.name == 'Drone':
                startEvent.remove = creator
            return [startEvent, completeEvent]




class Price:
    def __init__(self, minerals, gas, supply):
        self.minerals = minerals
        self.gas = gas
        self.supply = supply

    def canAfford(self, other):
        if self.minerals < other.minerals or self.gas < other.gas or self.supply < other.supply:
            return False
        return True


class BuildOrder:
    def __init__(self):
        with open('cleanTypes.json') as f:
            self.rawjson = json.load(f)
            for k, v in self.rawjson['unitTypes'].iteritems():
                v['type'] = 'unit'
                v['whatCreates'] = v['whatBuilds']
                v['required'] = v['requiredUnits']
                v['createTime'] = v['buildTime']
                if v['requiredTech'] != 'None':
                    v['required'].append(v['requiredTech'])
            for k, v in self.rawjson['techTypes'].iteritems():
                v['type'] = 'tech'
                v['whatCreates'] = v['whatResearches']
                v['supplyRequired'] = 0
                v['supplyProvided'] = 0
                v['required'] = []
                v['createTime'] = v['researchTime']
                if v['requiredUnit'] != 'None':
                    v['required'] = [v['requiredUnit']]
            for k,v in self.rawjson['upgradeTypes'].iteritems():
                v['type'] = 'upgrade'
                v['whatCreates'] = v['whatUpgrades']
                v['supplyProvided'] = 0
                v['supplyRequired'] = 0
                v['required'] = []
                v['createTime'] = v['upgradeTime']
                if v['whatsRequired'] != 'None':
                    v['required'] = [v['whatsRequired']]
            self.creatables = self.rawjson['unitTypes']
            self.creatables.update(self.rawjson['techTypes'])
            self.creatables.update(self.rawjson['upgradeTypes'])

    
    def validateBO(self, bo, start, startMinerals, startGas):
        time = 0


    def required(self, name):
        target = self.creatables[name]
        return target['required']

    def whatCreates(self, name):
        target = self.creatables[name]
        return target['whatCreates']

    def getCost(self, name):
        target = self.creatables[name]
        return Price(target['mineralPrice'], target['gasPrice'], target['supplyRequired'])

bo = BuildOrder()
units = [Creatable('SCV')] * 4 + [Creatable('Command_Center')]
init_game = GameState(50, 0, units, bo, 0)

print init_game.can_create('SCV')