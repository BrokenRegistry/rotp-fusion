/*
 * Copyright 2015-2020 Ray Fowler
 * 
 * Licensed under the GNU General Public License, Version 3 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * 
 *     https://www.gnu.org/licenses/gpl-3.0.html
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package rotp.model.combat;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import rotp.model.empires.DiplomaticEmbassy;
import rotp.model.empires.DiplomaticTreaty;
import rotp.model.empires.Empire;
import rotp.model.galaxy.ShipFleet;
import rotp.model.galaxy.SpaceMonster;
import rotp.model.galaxy.StarSystem;
import rotp.model.game.IInGameOptions;
import rotp.model.ships.ShipDesign;
import rotp.model.ships.ShipDesignLab;
import rotp.ui.RotPUI;
import rotp.ui.combat.ShipBattleUI;
import rotp.util.Base;

public class ShipCombatManager implements Base {
    // private static final int MAX_TURNS = 100;
    private static int MAX_TURNS() { return IInGameOptions.maxCombatTurns.get(); };
    private static Thread autoRunThread;
    @SuppressWarnings("unused")
	private static Thread runningThread;
    private static int EMPTY    = 0;
    private static int ASTEROID = 1;
    private static int SHIPS    = 2;
    private static int PLANET   = 3;
    // combat vars
    public ShipBattleUI ui;
    private StarSystem system;
    private List<Empire> empiresInConflict = new ArrayList<>();

    private final List<CombatStack> allStacks = new ArrayList<>();
    private boolean interdiction = false;
    public boolean autoComplete = false;
    public boolean autoResolve = false;
    public boolean allowRetreat = false;
    public boolean performingStackTurn = false;
    public boolean showAnimations = true;
    private boolean playerInBattle = false;
    private CombatStack currentStack;
    private ShipCombatResults results;
    private boolean finished = false;
    public static final int maxX = 9;
    public static final int maxY = 7;
    private int turnCounter = 0;
    private final int[] startingPosn = { 30,40,20,50,10,60,0 };
    private final double[][] riskMap = new double[maxX+1][maxY+1];
    private int[][] initialMap       = new int[maxX+1][maxY+1];
    public boolean[][] asteroidMap = new boolean[maxX+1][maxY+1];
    private boolean initialPause;
    private List<CombatStack> currentTurnList;
    boolean moO1Asteroids; // if true => impact beam and missiles
    boolean isAsteroid(float x, float y) { return asteroidMap[(int)(x+0.5)][(int)(y+0.5)]; }
    public int asteroidsInPath(float srcX, float srcY, float tarX, float tarY) {
    	// Maybe take spreading into account an fractional impact 
    	int count = 0;
    	float dX = tarX - srcX;
    	float dY = tarY - srcY;
    	float dist = (float) Math.sqrt(dX*dX + dY*dY);
    	float step = 0.2f;
    	float stepX = step * dX/dist;
    	float stepY = step * dY/dist;
    	int   lastX = -1;
    	int   lastY = -1;
       	float x = srcX;
       	float y = srcY;
       	while (dist>0) {
       		x += stepX;
       		y += stepY;
       		dist -= step;
       		if (isAsteroid(x, y)) {
      			int idX = (int)(x+0.5);
      			int idY = (int)(y+0.5);
      			if (lastX != idX || lastY != idY) {
      				count++;
      				lastX = idX;
      				lastY = idY;
      			}
       		}
       	}
    	return count;
    }
    boolean interdiction()                     { return interdiction; }
    public ShipCombatResults results()         { return results; }
    public StarSystem system()                 { return system; }
    void system(StarSystem sys)                { system = sys; }
    public CombatStack currentStack()          { return currentStack; }
    public List<CombatStack> activeStacks()    { return results.activeStacks(); }
    public void ui(ShipBattleUI panel)         { ui = panel; }
    public boolean showAnimations()            { return showAnimations && (ui != null) && playerInBattle; }
    public List<CombatStack> allStacks()       { return allStacks; }
    public void setInitialPause()              { initialPause = true; }
    
    /* public boolean involves(Empire emp) {
        return (results.attacker() == emp) || (results.defender() == emp);
    } */
    public boolean redrawMap = false;
    public int turnCounter() { return turnCounter; }
    public void battle(StarSystem sys) {
        playerInBattle = false;
        if (sys.hasMonster()) {
            battle(sys, sys.monster());
            galaxy().ships.launchFleets(system.id); // Launch retreating fleets
            return;                   
        }
        empiresInConflict = sys.empiresInConflict();
        log("Ship Combat starting in ", player().sv.name(sys.id), " between empires: ", empiresInConflict.toString());

        // build list of possible conflicts, where two orbiting fleets are in conflict at this system
        List<EmpireMatchup> matchups = new ArrayList<>();
        for (Empire e1: empiresInConflict) {
            for (Empire e2: empiresInConflict) {
                if (e1.aggressiveWith(e2, sys)) {
                    boolean alreadyAdded = false;
                    for (EmpireMatchup m: matchups) {
                        if (m.matches(e1,e2))
                            alreadyAdded = true;
                    }
                    if (!alreadyAdded)
                        matchups.add(new EmpireMatchup(e1,e2));
                }
            }
        }
        // randomize the order that we do the potential combats
        Collections.shuffle(matchups);
        
        // decide for each matchup if we should start combat
        // if a fleet retreats or is destroyed in one combat, it will 
        //  be "null" in subsequent potential combats
        while (!matchups.isEmpty()) {
            EmpireMatchup match = matchups.get(0);
            matchups.remove(match);
            Empire emp1 = match.emp1;
            Empire emp2 = match.emp2;
            ShipFleet fleet1 = sys.orbitingFleetForEmpire(emp1);
            ShipFleet fleet2 = sys.orbitingFleetForEmpire(emp2);
            boolean fleet1Armed = (fleet1 != null) && fleet1.isArmed(sys);
            boolean fleet2Armed = (fleet2 != null) && fleet2.isArmed(sys);
            boolean fleet1ArmedForShips = (fleet1 != null) && fleet1.isArmedForShipCombat();
            boolean fleet2ArmedForShips = (fleet2 != null) && fleet2.isArmedForShipCombat();
            Empire homeEmpire = sys.empire();
            boolean startCombat = false;
            // if both fleets are armed, we always have combat
            if (fleet1Armed && fleet2Armed) 
                startCombat = true; 
            // if we have a colony that belongs to either of the fleets, combat 
            // rules are different
            else if (sys.isColonized()  && ((sys.empire() == emp1) || (sys.empire() == emp2))) {
                // if colony is armed, we have combat
                if (sys.colony().defense().isArmed())
                    startCombat = true;
                // else if colony is unarmed we might have combat if one of the fleets is armed
                // and there is a home fleet. If no home fleet, then this goes to the bombardment phase
                else if (fleet1Armed || fleet2Armed) {
                    if ((fleet1 != null) && (fleet1.empire() == homeEmpire)) 
                        startCombat = true;
                    else if ((fleet2 != null) && (fleet2.empire() == homeEmpire)) 
                        startCombat = true;
               }
            }
            // else there is no colony in combat.. start battle if at least one fleet is armed
            // for ship combat (i.e. no bombers vs bombers)
            else if (fleet1ArmedForShips || fleet2ArmedForShips)
                startCombat = true;
            // if any of those choices matched, begin combat
            if (startCombat) {
                battle(sys, emp1, emp2);
                Empire victor = results.victor();
                if (emp1 != victor)
                    retreatEmpire(emp1);
                if (emp2 != victor)
                    retreatEmpire(emp2);
                galaxy().ships.launchFleets(system.id); // Launch retreating fleets
            }
        }
    }
    public void battle(StarSystem sys, SpaceMonster monster) {
    	system = sys;
        monster.initCombat();
        empiresInConflict = sys.empiresInConflict();
        List<Empire> empires = new ArrayList<>(empiresInConflict);
        Collections.shuffle(empires);
        for (Empire emp: empires) {
//        	if (sys.orbitingFleetForEmpire(emp).isArmed()) // TO DO BR: COMMENT
//        		System.out.println( getTurn() +  " " + monster.name() + " attacked by " + emp.name() + " (" + emp.id + ")");
            battle(sys, emp, monster);
            if (!monster.alive()) {
            	// TO DO BR: COMMENT
//            	System.out.println("  --> " + this.getTurn() +  " " + monster.name() + " killed by " + emp.name() + " (" + emp.id + ")");
            	break;
            }
        }    
    }
    public boolean validSquare(int x, int y) {
        if ((x < 0) || (y< 0) || (x>maxX) || (y>maxY))
            return false;
        return !asteroidMap[x][y];
    }
    private void battle(StarSystem sys, Empire emp1, Empire emp2) {
        finished = false;
        playerInBattle = emp1.isPlayerControlled() || emp2.isPlayerControlled();
        
        ShipFleet fl1 = sys.orbitingFleetForEmpire(emp1);
        ShipFleet fl2 = sys.orbitingFleetForEmpire(emp2);
        int sysEmpId = sys.empId();
        
        // each empire() gets an encounter scan of the opposing fleet
        // in addition, if a fleet has a NAP with the system empire(),
        // update the ownership for the system
        if (fl1 != null) {
            if ((fl2 != null) && fl2.allowsScanning())
                emp2.scanFleet(fl1);
            else
                emp2.encounterFleet(fl1);
            if (emp1.pactWith(sysEmpId))
                emp1.sv.view(sys.id).setEmpire();
        }
        
        if (fl2 != null) {
            if ((fl1 != null) && fl1.allowsScanning())
                emp1.scanFleet(fl2);
            else
                emp1.encounterFleet(fl2);
            if (emp2.pactWith(sysEmpId))
                emp2.sv.view(sys.id).setEmpire();
        }

        beginInSystem(sys, emp1, emp2);
        log("Resolving ship battle between empire1:", emp1.name(), "  empire2:", emp2.name());
        ui = null;

        setupBattle(emp1, emp2);

        if (combatIsFinished())
            return;

        checkDeclareWar(emp1, emp2);
        checkDeclareWar(emp2, emp1);

        if (playerInBattle())
            RotPUI.instance().promptForShipCombat(this);
        else {
            resolveAllCombat();
        }
        endOfCombat(true);
    }
    private void battle(StarSystem sys, Empire emp, SpaceMonster monster) {
        finished = false;
        playerInBattle = emp.isPlayerControlled();
        system = sys;
        results = new ShipCombatResults(this, system, emp, monster);
        if (system.empire() == emp)
            emp.lastAttacker(monster);
        monster.lastAttacker(emp);
        log("Resolving ship battle between empire1:", emp.name(), "  monster:", monster.name());
        setupBattle(emp, monster);
        if (combatIsFinished())
            return;

        if (emp.isPlayerControlled())
            RotPUI.instance().promptForShipCombat(this);
        else {
            resolveAllCombat();
        }
        endOfCombat(true);
    }
    private void beginInSystem(StarSystem s, Empire emp1, Empire emp2) {
        system = s;
        results = new ShipCombatResults(this, system, emp1, emp2);
        
        // set last attacker for colony empire() in case genocide occurs
        if (system.empire() == emp1)
            emp1.lastAttacker(emp2);
        else if (system.empire() == emp2)
            emp2.lastAttacker(emp1);
    }
    private boolean playerInBattle() {
        for (CombatStack st : results.activeStacks()) {
            if (st.isPlayerControlled())
                return true;
        }
        return false;
    }

    public void toggleAutoComplete() {
        autoComplete = !autoComplete;
        log("Toggling Auto Complete: "+autoComplete);

        if (autoComplete) {
            autoRunThread = new Thread(autoRunProcess());
            autoRunThread.start();
            runningThread = autoRunThread;
        }
        else {
            if(autoRunThread != null) {
                try {
                    autoRunThread.join();
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
            continueToNextPlayerStack();
        }
    }
    private void setupCurrentTurnList() {
        // use temp to avoid rare comod error when auto-resolving in
        // the middle of combat, which can potentially update both activeStacks()
        // and currentTurnList
        List<CombatStack> temp = new ArrayList<>(activeStacks());
        Collections.sort(temp, CombatStack.INITIATIVE);
        currentTurnList = new ArrayList<>(temp);
    }
    public void resolveAllCombat() {
        clearAsteroids();

        autoComplete = true;
        autoResolve = true;
        performingStackTurn = true;       
        while (shouldContinue())
            performNextStackTurn();
    } 
    private boolean shouldContinue() {
        return autoComplete && !combatIsFinished();
    }
    public void continueToNextPlayerStack() {
        log("Continuing To Next Player Stack");
        if (combatIsFinished()) 
            return;
        
        performingStackTurn = true;
        
        
        if (initialPause) {
            initialPause = false;
            ui.paintAllImmediately();     
            sleep(1000);
        }
        currentStack.performTurn();
        boolean playerTurn = false;
        while (!playerTurn) {
            if (!currentStack.usingAI() && !currentStack.isTurnComplete())
                playerTurn = true;
            else
                performNextStackTurn();
            if (combatIsFinished()) 
                return;
        }

        performingStackTurn = false;
    }
    private Runnable autoRunProcess() {
        return () -> {
            performingStackTurn = true;
            while (shouldContinue())
                performNextStackTurn();
            performingStackTurn = false;
            autoRunThread = null;
        };
    }
    /* public boolean canScan(Empire civ, CombatStack st) {
        if (st.empire() == civ)
            return true;
        if (st.cloaked)
            return false;
        if (system.empire() == civ)
            return true;

        for (CombatStack stack : results.activeStacks()) {
            if ((stack.empire() == civ) && stack.canScan())
                return true;
        }
        return false;
    } */
    public void setupBombardment(StarSystem sys, ShipFleet fleet) {
        ui = null;
        system = sys;
        checkDeclareWar(fleet.empire(), system.empire());

        beginInSystem(system, fleet.empire(), null);

        if (results.colonyStack != null)
            addStackToCombat(results.colonyStack);

        addInitialStacks(fleet.empire());
        scanShips();
        addEmpiresToCombat();
        results().defender = system.empire();
    }
    private void checkDeclareWar(Empire emp1, Empire emp2) {
        // decide if we should declare war
        if (emp1.isPlayerControlled())
            return;  // player can declare his own wars
        
        // don't automatically trigger war over uncolonized systems
        if (!system.isColonized())
            return;
        
        // don't automatically trigger when emp2 stumbled into our system
        if (system.empire() == emp1)
            return;
        DiplomaticEmbassy emb1 = emp1.viewForEmpire(emp2).embassy();
        if (!emb1.anyWar()) {
            if (emb1.onWarFooting())
                emb1.declareWar();
        }
    }
    private void setupBattle(Empire emp1, Empire emp2) {
        raiseHostilityLevels();

        turnCounter = 0;
        interdiction = false;
        performingStackTurn = false;
        autoComplete = false;
        autoResolve = false;
        allowRetreat = false;
        showAnimations = true;
        redrawMap = true;
        initCombatStacks(emp1, emp2);

        if (combatIsFinished())
            return;

        placeCombatStacks();
        placeAsteroids();
        allStacks.clear();
        allStacks.addAll(results.activeStacks());

        setupCurrentTurnList();
        currentStack = currentTurnList.get(0);
        currentStack.beginTurn();
    }
    private void setupBattle(Empire emp, SpaceMonster monster) {
        turnCounter = 0;
        interdiction = false;
        performingStackTurn = false;
        autoComplete = false;
        autoResolve = false;
        showAnimations = true;
        redrawMap = true;
        initCombatStacks(emp, monster);

        if (combatIsFinished())
            return;

        placeCombatStacks();
        placeAsteroids();
        allStacks.clear();
        allStacks.addAll(results.activeStacks());

        setupCurrentTurnList();
        currentStack =  currentTurnList.get(0);
        currentStack.beginTurn();
    }
    public void endOfCombat(boolean logIncidents) {
        // auto-retreat any attacking ships that are stuck in stasis.
        // if they have nowhere to retreat, destroy them (don't show retreat animation in this case)
        List<CombatStack> activeStacks = new ArrayList<>(results.activeStacks());
        for (CombatStack st: activeStacks) {
            if (st.inStasis && st.isShip()) {
                CombatStackShip sh = (CombatStackShip) st;
                StarSystem dest = sh.empire().retreatSystem(system());
                boolean prevShow = showAnimations;
                showAnimations = false;
                if ((dest == null) || (dest == system))
                    destroyStack(sh);
                else
                {
                    turnCounter = MAX_TURNS(); //Set turn-counter to max-turns so retreating works in this case
                    retreatStack(sh, dest);
                }
                showAnimations = prevShow;
            }          
        }
        
        // cancel the retreat of any ships that belong to the combat victor
        Empire victor = results.victor();
        if (victor != null) {
            List<ShipDesign> retreatedShips = new ArrayList<>(results.shipsRetreated().keySet());
            boolean retreatsCancelled = false;
            for (ShipDesign des: retreatedShips) {
                if (victor.shipLab().design(des.id()) == des) {
                    results.shipsRetreated().remove(des);
                    retreatsCancelled = true;
                }        
            }
            if (retreatsCancelled)
                galaxy().ships.cancelRetreatingFleets(victor.id, system().id);
        }
        
        // ensure rebels are killed in proportion to overall population
        results.killRebels();

        // update treaty with results
        Empire defender = results.defender();
        Empire attacker = results.attacker();
        if ((attacker != null) && (defender != null)) {
            DiplomaticTreaty treaty = defender.treaty(attacker);
            treaty.loseFleet(attacker, results.shipHullPointsDestroyed(attacker));
            treaty.loseFleet(defender, results.shipHullPointsDestroyed(defender));
            if (results.colonyStack != null) {
                treaty.losePopulation(defender, results.popDestroyed());
                treaty.loseFactories(defender, results.factoriesDestroyed());
            }
        }

        if (logIncidents)
            results.logIncidents();
    }
    public boolean retreatStack(CombatStackShip stack, StarSystem s) {
        log("Retreating: ", stack.fullName());
        performingStackTurn = true;
        stack.drawRetreat();
        results.addShipsRetreated(stack.design(), stack.num);
        removeFromCombat(stack);
        stack.retreatToSystem(s);
        //turnDone(stack);
        performingStackTurn = false;
        return true;
    }
    public void destroyStack(CombatStack stack) {
        log("Destroyed: ", stack.fullName());
        if (stack instanceof CombatStackShip)
            results.addShipStackDestroyed(((CombatStackShip)stack).design(), stack.origNum);
        else if (stack instanceof CombatStackColony)
            results.addBasesDestroyed(stack.num);

        stack.becomeDestroyed();
        
        if (!stack.isColony())
           removeFromCombat(stack);
        if (stack == currentStack)
            turnDone(stack);
    }
    private void raiseHostilityLevels() {
        List<ShipFleet> fleets = system.orbitingFleets();
        for (ShipFleet fl : fleets)
            fl.empire().sv.raiseHostility(system.id);
    }
    private void addInitialStacks(Empire emp) {
        CombatStack colonyWard = null;
        if ((results.colonyStack != null) && (results.colonyStack.colony.empire() == emp))
            colonyWard = results.colonyStack;
        
        ShipFleet fl = system.orbitingFleetForEmpire(emp);
        if (fl == null)
            return;

        fl.system(system);
        fl.retreating(false);
        for (int i=0;i<ShipDesignLab.MAX_DESIGNS;i++) {
            if (fl.num(i) > 0) {
                ShipDesign d = fl.empire().shipLab().design(i);
                if (d != null) {
                    CombatStackShip stack = new CombatStackShip(fl, i, this);
                    if (stack.isArmed())
                        stack.ward(colonyWard);
                    addStackToCombat(stack);
                }
            }
        }
    }
    private void initCombatStacks(Empire emp1, Empire emp2) {
        if (results.colonyStack != null)
            addStackToCombat(results.colonyStack);

        addInitialStacks(emp1);
        addInitialStacks(emp2);
        scanShips();

        // unless system has monster, remove any stacks that want to retreat
        if (system.hasMonster())
            return;

        boolean retreating = true;
        List<CombatStack> retreatingFleets = new ArrayList<>();

        while (retreating) {
            retreatingFleets.clear();
            for (CombatStack st : results.activeStacks()) {
                if (st.usingAI() && st.wantToRetreat()) {
                    if (st.retreat())
                        retreatingFleets.add(st);
                }
            }
            results.activeStacks().removeAll(retreatingFleets);
            retreating = !retreatingFleets.isEmpty();
        }
        List<Empire> passives = new ArrayList<>();
        passives.add(emp1);
        passives.add(emp2);
        // ships & armed colonies mean there is still a
        // "combatable" stack for the empire()
        for (CombatStack st: results.activeStacks()) {
            if (st.isArmed() || !st.isColony())
                passives.remove(st.empire());
        }
        for (Empire passiveEmp: passives) {
            log("retreating empires from init: ",passives.toString());
            empiresInConflict.remove(passiveEmp);
        }
    }
    private void initCombatStacks(Empire emp, SpaceMonster monster) {
        if (results.colonyStack != null)
            addStackToCombat(results.colonyStack);

        addInitialStacks(emp);
        for (CombatStack st: monster.combatStacks()) {
            if (!st.destroyed())
                addStackToCombat(st);
        }

        // unless system has monster, remove any stacks that want to retreat
        // BR: let scout get away from monsters
        // if (system.hasMonster())
        //     return;

        boolean retreating = true;
        List<CombatStack> retreatingFleets = new ArrayList<>();

        while (retreating) {
            retreatingFleets.clear();
            for (CombatStack st : results.activeStacks()) {
                if (st.usingAI() && st.wantToRetreat()) {
                    if (st.retreat())
                        retreatingFleets.add(st);
                }
            }
            results.activeStacks().removeAll(retreatingFleets);
            retreating = !retreatingFleets.isEmpty();
        }
        List<Empire> passives = new ArrayList<>();
        passives.add(emp);
        // ships & armed colonies mean there is still a
        // "combatable" stack for the empire()
        for (CombatStack st: results.activeStacks()) {
            if (st.isArmed() || !st.isColony())
                passives.remove(st.empire());
        }
        for (Empire passiveEmp: passives) {
            log("retreating empires from init: ",passives.toString());
            empiresInConflict.remove(passiveEmp);
        }
    }
    private void retreatEmpire(Empire e) {
        List<CombatStack> retreatingStacks = new ArrayList<>();

        List<CombatStack> activeStacks = new ArrayList<>(results.activeStacks());
        for (CombatStack st : activeStacks) {
            if ((st.empire() == e) && st.isShip()) {
                CombatStackShip ship = (CombatStackShip) st;
                if (ship.retreat()) {
                    retreatingStacks.add(ship);
                }
            }
        }
        for (CombatStack st: retreatingStacks)
            results.activeStacks().remove(st);
    }
    private void addEmpiresToCombat() {
        boolean playerInCombat = false;
        List<Empire> empiresInCombat = new ArrayList<>();
        for (CombatStack st : results.activeStacks()) {
            if (st.isPlayerControlled())
                playerInCombat = true;
            if (!empiresInCombat.contains(st.empire()))
                empiresInCombat.add(st.empire());
        }

        // build civs array, placing player ships first
        results.clearEmpires();
        if (playerInCombat) {
            results.addEmpire(player());
            empiresInCombat.remove(player());
        }

        for (Empire c : empiresInCombat)
            results.addEmpire(c);
    }
    private void clearInitialMap() {
    	moO1Asteroids = options().moo1AsteroidsProperties();
        for (int x=0;x<=maxX;x++) {
            for (int y=0;y<=maxY;y++)
                initialMap[x][y] = 0;
        }
    }
    private void clearAsteroids() {
        for (int x=0;x<=maxX;x++) {
            for (int y=0;y<=maxY;y++)
                asteroidMap[x][y] = false;
        }
    }
    private void placeAsteroids() {
        // clear the asteroid map before starting
        clearAsteroids();

        if (options().moo1AsteroidsLocation()) {
        	int emptyCols = 2;
        	int numRows = maxY+1;
        	int numCols = maxX+1 - 2*emptyCols; // remove the borders
        	int numAsteroids = system.planet().asteroidsCount();
        	int maxLoop = 100;
        	while (numAsteroids>0 && maxLoop>0) {
        		int x = rng().nextInt(numCols)+emptyCols;
        		int y = rng().nextInt(numRows);
        		int up   = min(y+1, maxY);
        		int down = max(y-1, 0);
        		if (!(initialMap[x][y]>EMPTY
        				|| initialMap[x+1][y] > EMPTY		// Sides
        				|| initialMap[x-1][y] > EMPTY
        				|| initialMap[x][up]  > EMPTY
        				|| initialMap[x][down] > EMPTY
        				|| initialMap[x+1][up] > ASTEROID	// Diagonals
        				|| initialMap[x-1][up] > ASTEROID
        				|| initialMap[x+1][down] > ASTEROID
        				|| initialMap[x-1][down] > ASTEROID
        				)) {
        			asteroidMap[x][y] = true;
        			initialMap [x][y] = ASTEROID;
        			numAsteroids--;
        		}
       			maxLoop--;
       			if (maxLoop<50) {
       				emptyCols = 1;
       				numCols = maxX+1 - 2*emptyCols;
       			}
       				
        	}
        	return;
        }
        
        boolean isAsteroidSystem = system().planet().type().isAsteroids();
        CombatStackColony colony = results().colonyStack;
        int beltW = isAsteroidSystem ? 8: 4;
        int startX = 0;
        if (isAsteroidSystem)
            startX = 1;
        else if (colony == null)
            startX = 3;
        else if (colony.isPlayerControlled())
            startX = 4;
        else
            startX = 2;
        int endX = startX+beltW;
        for (int x=startX; x<endX;x++) {
            int num = 0;
            for (int y=0;y<=maxY;y++) {
                asteroidMap[x][y] = false;
                if ((num < 3) && (random() < 0.375)) {
                    num++;
                    asteroidMap[x][y] = true;
                }
            }
        }
    }
    private void trimAsteroids() {
    	if (options().asteroidsVanish()) {
            for (int x=0; x<=maxX; x++) {
                for (int y=0;y<=maxY; y++) {
                    if (asteroidMap[x][y] && (random() < .05)) {
                        asteroidMap[x][y] = false;
                        redrawMap = true;
                        break;
                    }
                }
            }
    	}
    }
    private void placeCombatStacks() {
        clearInitialMap();
        addEmpiresToCombat();
        int[] posnAdj = { 0,9 };
        int empIndex = 0;
        // for each empire(), place ship stacks
        for (Empire c : results.empires()) {
            int stackIndex = 0;
            int idOffset = 0;
            for (CombatStack st : results.activeStacks()) {
                if (st.empire() == c) {
                	int stackPosn = startingPosn[stackIndex+idOffset] + posnAdj[empIndex];
                	if (st.isColony() && options().moo1PlanetLocation()) {
                	        int[] posPlanetAdj = { 1,8 };
                	        stackPosn = startingPosn[stackIndex] + posPlanetAdj[empIndex];
                	        idOffset = -1;
                	}
                    st.x = stackPosn % 10;
                    st.y = stackPosn / 10;
                    if (st.x > 5)
                        st.reverse();
                    stackIndex++;
                    initialMap[st.x][st.y] = st.isColony()? PLANET : SHIPS;
                    //log("Ship Stack: "+st);
                }
            }
            empIndex++;
        }

        // set interdiction flag
        interdiction = false;

        if (results.colonyStack != null) {
            if (results.colonyStack.colony.hasInterdiction()) {
                for (CombatStack st : results.activeStacks()) {
                    if (st.hasTeleporting() && st.aggressiveWith(results.colonyStack))
                        interdiction = true;
                }
            }
        }
    }
    public CombatStack moveStackNearest(CombatStack newStack, int x, int y) {
        float minDist = Float.MAX_VALUE;
        List<Integer> nearX = new ArrayList<>();
        List<Integer> nearY = new ArrayList<>();
        for (int x1=0;x1<=maxX;x1++) {
            for (int y1=0;y1<=maxY;y1++) {
                if (asteroidMap[x1][y1])
                    continue;
                float dist = distance(x, y,x1, y1);
                if ((dist == 0) || (dist > minDist))
                    continue;
                CombatStack prevStack = stackAt(x1,y1);
                if ((prevStack != null) && !newStack.canEat(prevStack))
                    continue;
                if (dist < minDist) {
                    nearX.clear();
                    nearY.clear();
                }
                minDist = dist;
                nearX.add(x1);
                nearY.add(y1);
            }
        } 
        int index = roll(0, nearX.size()-1);
        int tgtX = nearX.get(index);
        int tgtY = nearY.get(index);
        CombatStack tgtStack = stackAt(tgtX, tgtY);
        this.moveStack(newStack, tgtX, tgtY);
        //newStack.x = tgtX;
        //newStack.y = tgtY;
        return tgtStack;
    }
    private void scanShips() {
        // scan only if have scanners and NOT same civ as planet (already scanned)
        for (CombatStack st : results.activeStacks()) {
            if (st.canScan()) {
                for (CombatStack st2 : results.activeStacks()) {
                    if (st2.isShip()) {
                        CombatStackShip sh2 = (CombatStackShip) st2;
                        st.empire().scanDesign(sh2.design(), st2.empire());
                    }
                }
            }
        }
    }
    private boolean remainingStacksInConflict() {
        // check remaining stacks for conflict, excluding unarmed colony stacks
        // which may have been added as potential bombing targets
        List<CombatStack> combatableStacks = new ArrayList<>();
        List<CombatStack> activeStacks = new ArrayList<>(results.activeStacks());
        for (CombatStack st: activeStacks) {
            // only armed colonies count as combatable stacks
            if (st.isColony()) {
                if (st.isArmed())
                    combatableStacks.add(st);
            }
            // monsters always count
            else if (st.isMonster())
                combatableStacks.add(st);
            // attacking ships not in stasis count
            else if (!st.inStasis)
                combatableStacks.add(st);
            //ail: when there's still missiles flying around don't end the combat (the stack that fired it doesn't count as armed anymore)
            if(!st.missiles().isEmpty())
            {
                for(CombatStackMissile missile : st.missiles())
                {
                    //ail: the missile also needs to have a target that still is there as otherwise this can lead to combat going on after target with incoming missile was destroyed
                    if(missile.target.num > 0)
                        return true;
                }
            }
        }
        for (CombatStack stack1 : combatableStacks) {
            for (CombatStack stack2 : combatableStacks) {
                if (stack1.isArmed() || stack2.isArmed()) {
                    if ((stack1 != stack2) && stack1.hostileTo(stack2, system))
                        return true;
                }
            }
        }
        // don't retreat if we still have missiles in flight
        return false;
    }
    public void addStackToCombat(CombatStack st) {
        st.mgr = this;

        if (st.isMissile()) {
            CombatStackMissile miss =(CombatStackMissile) st;
            miss.target.addMissile(miss);
            return;
        }
        results.activeStacks().add(st);
    }

    public CombatStack stackAt(int x, int y) {
        for (CombatStack st : results.activeStacks()) {
            if (st.atGrid(x,y))
                return st;
        }
        return null;
    }
    public boolean canMoveTo(CombatStack st, int x, int y) {
        if ((x < 0) || (x > maxX) || (y < 0) || (y > maxY))
            return false;

        if (!st.canCollide() && (stackAt(x, y) != null))
            return false;

        if (st.canTeleport() && !interdiction)
            return true;
        
        for (CombatStack s: results.activeStacks()) {            
            if(st.ignoreRepulsors() || (s.empire() == st.empire()) || s.inStasis)
                continue;
            if(s.movePointsTo(x, y) <= s.repulsorRange())
                return false;
        }

        return st.canMoveTo(x, y);
    }
    public boolean canTacticallyMoveTo(CombatStack st, int x, int y) {
        if ((x < 0) || (x > maxX) || (y < 0) || (y > maxY))
            return false;

        if (asteroidMap[x][y])
            return false;

        if (!st.canCollide() && (stackAt(x, y) != null))
            return false;

        return st.canMoveTo(x, y);
    }
    public boolean canTeleportTo(CombatStack st, int x, int y) {
        if ((x < 0) || (x > maxX) || (y < 0) || (y > maxY))
            return false;

        if (asteroidMap[x][y])
            return false;

        if (!st.canCollide() && (stackAt(x, y) != null))
            return false;

        if (st.canTeleport() && !interdiction)
            return true;

        return false;
    }
    // public boolean finished()    { return finished; }
    public boolean combatIsFinished() {
        if (finished)
            return true;
        // stop after max turns to avoid infinite looping
        if (turnCounter > MAX_TURNS()) {
            retreatEmpire(results.attacker());
            log("combat finished-- max turns exceeded. Retreating: "+results.attacker());
            finished = true;
            if (showAnimations())
                ui.showResult();
            return true;
        }

        // no one is in conflict, the battle is over
        if (!remainingStacksInConflict()) {
            log("combat finished-- remaining stacks unarmed or not in conflict");
            finished = true;
            if (showAnimations())
                ui.showResult();
            endOfCombat(true);
            return true;
        }
        return false;
    }
    public void turnDone(CombatStack st) {
        //log(st.fullName(), " - Done");
        st.endTurn();

        if (activeStacks().isEmpty()) {
            endOfCombat(true);
            return;
        }
        
        int currIndex = currentTurnList.indexOf(st);
        int nextIndex = -1;
        int lastIndex =currentTurnList.size()-1;
        
        // we need to find the next available stack to take a turn 
        // from the currentTurnList. Skip any stacks that are:
        // -- destroyed (by any earlier stack in the list)
        // -- unarmed colonies (they can't shoot or move, so skip)
        // when we find one, set nextIndex and break out of the loop
        for (int i=currIndex+1;i<=lastIndex;i++) {
            CombatStack stack = currentTurnList.get(i);
            if (stack.destroyed())
                continue;
            if (stack.isColony() && !stack.isArmed()) {
                stack.beginTurn();
                stack.endTurn();
                continue;
            }
            nextIndex = i;
            break;
        }
        
        // if no valid stack found in the current turn list
        // then reset the turn list and start a new combat round
        if (nextIndex < 0) {
            setupCurrentTurnList();
            currentStack = currentTurnList.get(0);
            turnCounter++;
            trimAsteroids();
        }
        else
            currentStack = currentTurnList.get(nextIndex);

        currentStack.beginTurn();
    }
    private void performNextStackTurn() {
        generateRiskMap(currentStack);
        currentStack.performTurn();
    }
    void removeFromCombat(CombatStack st) {
        activeStacks().remove(st);
        if (currentStack == st)
            turnDone(st);
        
        if (st.isMissile()) {
            removeMissileFromCombat((CombatStackMissile)st);
            return;
        }
        
        removeMissilesLaunchedFromStack(st);
        removeMissilesTargetingStack(st);

        if (!st.isColony())
            allStacks.remove(st);
        
    }
    private void removeMissilesLaunchedFromStack(CombatStack st) {
        List<CombatStack> stacks = new ArrayList<>(allStacks());
        for (CombatStack stack: stacks) {
            List<CombatStackMissile> missiles = new ArrayList<>(stack.missiles());
            for (CombatStackMissile miss: missiles) {
                if (miss.owner == st) 
                    removeMissileFromCombat(miss);
            }
        }
    }
    private void removeMissilesTargetingStack(CombatStack st) {
        List<CombatStackMissile> missiles = new ArrayList<>(st.missiles());
        for (CombatStackMissile miss: missiles) {
            removeMissileFromCombat(miss);
        }
    }
    private void removeMissileFromCombat(CombatStackMissile miss) {
        List<CombatStackMissile> missList = miss.target.missiles();
        synchronized(missList) {
            missList.remove(miss);
        }
    }
    public void performMoveStackToPoint(CombatStack st, int x, int y) {
        if (!st.canMove())
            return;
        else if ((x == st.x) && (y == st.y))
            return;
        else if (st.canMoveTo(x, y))
            moveStack(st, x, y);
        else
            teleportStack(st, x, y);
    }
    public void performMoveStackAlongPath(CombatStack st, FlightPath path) {
        // if proposed path is too long for this stack's remaining
        // move but the stack can teleport, then do that,
        if ((path.size() > st.move) && st.canTeleport) {
            teleportStack(st, path.destX(), path.destY());
            return;
        }

        // proposed path may be too long for this stack. If stack
        // can't teleport, cut down length of path
        if (path.size() > st.move) 
            path.limitMoves((int)st.move);

        // move the stack along it's path until done or destroyed (by missiles)
        for (int i=0;i<path.size();i++) {
            if (!st.destroyed()) 
                moveStack(st, path.mapX(i), path.mapY(i));
        }
    }
    public boolean moveStack(CombatStack st, int x1, int y1) {
        //log(currentStack.fullName(), " moving to: ", str(x1), ",", str(y1));
        boolean moved = st.moveTo(x1,y1);
        if(moved)
            CheckForReactionFire(st);
        return moved;
    }
    private void teleportStack(CombatStack st, int x1, int y1) {
        //log(currentStack.fullName() + " teleporting to: " + x1 + "," + y1);
        st.teleportTo(x1,y1, 0.1f);
    }
    public void performAttackTarget(CombatStack st) {
        while (st.selectBestWeapon(st.target)) 
            st.fireWeapon(st.target);
    }
    /* public void attackTarget(CombatStack st) {
        //log("mgr attackTarget done:" + st.civ.race.name + st.name());
    } */
    private void generateRiskMap(CombatStack st) {
        for (int x=0;x<=maxX;x++) {
            for (int y=0;y<maxY;y++)
                    riskMap[x][y] = riskAt(st, x, y);
        }
    }
    private double riskAt(CombatStack stack, int x, int y) {
        return 0.0d;
    }
    public boolean[] validMoveMap(CombatStack stack) {
        int gridW = maxX+3;
        int gridH = maxY+3;
        boolean[] valid = new boolean[gridW*gridH];

        // outside borders are non-traversable
        for (int x=0;x<gridW;x++) {
            for (int y=0;y<gridH;y++)
                valid[y*gridW+x] = (x>0) && (x<gridW-1) && (y>0) && (y<gridH-1);
        }

        // asteroids are not traversable
        for (int x=0;x<=maxX;x++) {
            for (int y=0;y<=maxY;y++) {
                if (asteroidMap[x][y])
                    valid[(y+1)*gridW+(x+1)] = false;
            }
        }

        // combat stacks are not traversable
        // enemy stacks may have a repulsor range that is also not traversable
        List<CombatStack> stacks = new ArrayList<>(results.activeStacks());
        for (CombatStack s: stacks) {            
            int r = stack.ignoreRepulsors() || (s.empire() == stack.empire()) || s.inStasis ? 0 : s.repulsorRange();
            if ((r == 0) && stack.canEat(s)) 
                continue;
            else if (r == 0) 
                valid[(s.y+1)*gridW+(s.x+1)] = false;
            else {
                for (int x=0-r;x<=r;x++) {
                    for (int y=0-r;y<=r;y++) {
                        int x0 = s.x+x+1;
                        int y0 = s.y+y+1;
                        valid[y0*gridW+x0] = false;
                    }
                }
            }
        }
        return valid;
    }
    private void CheckForReactionFire(CombatStack stack)
    {
        if(stack.cloaked)
            return;
        List<CombatStack> stacks = new ArrayList<>(results.activeStacks());
        for (CombatStack s: stacks) {
            if(s.empire() == stack.empire())
                continue;
            if(s.initiative() <= stack.initiative())
                continue;
            if(s.canAttack(stack) && !s.cloaked)
            {
                performReactionFire(s, stack);
            }
        }
    }
    private void performReactionFire(CombatStack stack, CombatStack target)
    {
        if(target == null)
            return;
        for (int i=0;i<stack.numWeapons(); i++) {
            if(stack.weapon(i).isBeamWeapon() && ((CombatStackShip)stack).shipComponentCanAttack(target, i))
                stack.fireWeapon(target, i, true);
        }
    }
    private class EmpireMatchup {
        private Empire emp1;
        private Empire emp2;
        private EmpireMatchup(Empire e1, Empire e2) {
            emp1 = e1;
            emp2 = e2;
        }
        private boolean matches (Empire e1, Empire e2) {
            if ((emp1 == e1) && (emp2 == e2))
                return true;
            if ((emp1 == e2) && (emp2 == e1))
                return true;
            return false;
        }
        // public boolean includes(Empire e1) { return (emp1 == e1) || (emp2 == e1); }
    }
}
