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
package rotp.model.empires;

import static rotp.model.tech.Tech.miniFastRate;

import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.Polygon;
import java.awt.Shape;
import java.awt.geom.Ellipse2D;
import java.awt.geom.Rectangle2D;
import java.awt.image.BufferedImage;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.BiPredicate;
import java.util.function.Predicate;
import java.util.stream.Collectors;

import rotp.model.ai.AI;
import rotp.model.ai.interfaces.Diplomat;
import rotp.model.ai.interfaces.FleetCommander;
import rotp.model.ai.interfaces.General;
import rotp.model.ai.interfaces.Governor;
import rotp.model.ai.interfaces.Scientist;
import rotp.model.ai.interfaces.ShipCaptain;
import rotp.model.ai.interfaces.ShipDesigner;
import rotp.model.ai.interfaces.SpyMaster;
import rotp.model.colony.Colony;
import rotp.model.colony.ColonyShipyard;
import rotp.model.colony.MissileBase;
import rotp.model.empires.Leader.Objective;
import rotp.model.empires.Leader.Personality;
import rotp.model.empires.SpyNetwork.FleetView;
import rotp.model.events.SystemColonizedEvent;
import rotp.model.events.SystemHomeworldEvent;
import rotp.model.galaxy.Galaxy;
import rotp.model.galaxy.GalaxyFactory;
import rotp.model.galaxy.IMappedObject;
import rotp.model.galaxy.Location;
import rotp.model.galaxy.NamedObject;
import rotp.model.galaxy.Ship;
import rotp.model.galaxy.ShipFleet;
import rotp.model.galaxy.Ships;
import rotp.model.galaxy.StarSystem;
import rotp.model.galaxy.StarSystem.SystemBaseData;
import rotp.model.galaxy.Transport;
import rotp.model.game.DynOptions;
import rotp.model.game.GameSession;
import rotp.model.game.GovernorOptions;
import rotp.model.incidents.DiplomaticIncident;
import rotp.model.incidents.GenocideIncident;
import rotp.model.planet.PlanetType;
import rotp.model.ships.ShipDesign;
import rotp.model.ships.ShipDesignLab;
import rotp.model.ships.ShipLibrary;
import rotp.model.tech.Tech;
import rotp.model.tech.TechRoboticControls;
import rotp.model.tech.TechTree;
import rotp.ui.NoticeMessage;
import rotp.ui.UserPreferences;
import rotp.ui.diplomacy.DialogueManager;
import rotp.ui.diplomacy.DiplomaticReply;
import rotp.ui.main.GalaxyMapPanel;
import rotp.ui.notifications.DiscoverTechNotification;
import rotp.ui.notifications.GNNGenocideNotice;
import rotp.ui.notifications.GNNNotification;
import rotp.ui.notifications.PlunderShipTechNotification;
import rotp.ui.notifications.PlunderTechNotification;
import rotp.util.Base;
import rotp.util.LabelManager;

public final class Empire implements Base, NamedObject, Serializable {
    private static final long serialVersionUID = 1L;
    private static final float SHIP_MAINTENANCE_PCT = .02f;
    private static final int MAX_SECURITY_TICKS = 10;
    private static final float MAX_SECURITY_PCT = 0.20f;
    public static final int PLAYER_ID = 0;
    public static final int NULL_ID = -1;
    public static final int ABSTAIN_ID = -2;
    
    public static final int SHAPE_CIRCLE = 0;
    public static final int SHAPE_SQUARE = 1;
    public static final int SHAPE_DIAMOND = 2;
    public static final int SHAPE_TRIANGLE1 = 3;
    public static final int SHAPE_TRIANGLE2 = 4;

    public static Empire thePlayer() { return Galaxy.current().player(); }

    public static long[] times = new long[6];

    public int selectedAI = -1;
    public final int id;
    private Leader leader;
    private final String raceKey;
    private final int raceNameIndex;
    private TechTree tech = new TechTree();
    private final ShipDesignLab shipLab;
    private final int homeSysId;
    private final int[] compSysId; // modnar: add option to start game with additional colonies
    private int capitalSysId;
    public final SystemInfo sv;
    private final EmpireView[] empireViews;
    private final List<Ship> visibleShips = new ArrayList<>();
    private final List<StarSystem> shipBuildingSystems = new ArrayList<>();
    private final List<StarSystem> colonizedSystems = new ArrayList<>();
    private boolean extinct = false;
    private boolean galacticAlliance = false;
    private int lastCouncilVoteEmpId = Empire.NULL_ID;
    private Colony.Orders priorityOrders = Colony.Orders.NONE;
    private int bannerColor;
    private final List<StarSystem> newSystems = new ArrayList<>();
    private final EmpireStatus status;

    //bounds
    private float minX, maxX, minY, maxY;
    private float planetScanningRange = 0;
    private float shipScanningRange = 0;
    private boolean knowShipETA = false;
    private boolean scanPlanets = false;
    private boolean recalcDistances = true;
    private float combatTransportPct = 0;
    private int securityAllocation = 0;
    private int empireTaxLevel = 0;
    private boolean empireTaxOnlyDeveloped = true;
    private boolean divertColonyExcessToResearch = false;
    private float totalReserve = 0;
    private float tradePiracyRate = 0;
    private NamedObject lastAttacker;
    private int defaultMaxBases = UserPreferences.defaultMaxBases();
    private final String dataRaceKey;

    // BR: Dynamic options
    private final DynOptions dynamicOptions = new DynOptions();
    private DynOptions raceOptions;

    private transient float avgX, avgY, nameX1, nameX2;

    private transient AI ai;
    private transient boolean[] canSeeShips;
    private transient Race race;
    private transient Race dataRace;
    private transient BufferedImage shipImage;
    private transient BufferedImage shipImageLarge;
    private transient BufferedImage shipImageHuge;
    private transient BufferedImage scoutImage;
    private transient BufferedImage transportImage;
    private transient Color nameColor;
    private transient Color ownershipColor;
    private transient Color selectionColor;
    private transient Color reachColor;
    private transient Color shipBorderColor;
    private transient Color scoutBorderColor;
    private transient Color empireRangeColor;
    private transient float totalEmpireProduction;
    private transient float totalEmpireNonDynaProduction; // modnar: create unscaled production, to avoid infinite recursion
    private transient float totalEmpireShipMaintenanceCost;
    private transient float totalEmpireStargateCost;
    private transient float totalEmpireMissileBaseCost;
    private transient int inRange;
    public transient int numColoniesHistory;
    private transient String empireName;

    public AI ai() {
        if (ai == null) {
            if(selectedAI < 0)
                ai = new AI(this, options().selectedAI(this));
            else
                ai = new AI(this, selectedAI);
        }
        return ai;
    }
    public String getAiKey()  { return AI.sortedAiKeys().get(selectedAI); } // BR:
    public String getAiName() { return LabelManager.current().label(getAiKey()); } // BR:
    public Diplomat diplomatAI()                  { return ai().diplomat(); }
    public FleetCommander fleetCommanderAI()      { return ai().fleetCommander(); }
    public ShipCaptain shipCaptainAI()            { return ai().shipCaptain(); }
    public General generalAI()                    { return ai().general(); }
    public Governor governorAI()                  { return ai().governor(); }
    public SpyMaster spyMasterAI()                { return ai().spyMaster(); }
    public Scientist scientistAI()                { return ai().scientist(); }
    public ShipDesigner shipDesignerAI()          { return ai().shipDesigner(); }
    
    public Leader leader()                        { return leader; }
    public ShipDesignLab shipLab()                { return shipLab; }
    public EmpireStatus status()                  { return status; }
    public int homeSysId()                        { return homeSysId; }
    public int capitalSysId()                     { return capitalSysId; }
    public int compSysId(int i)                   { return compSysId[i]; } // modnar: add option to start game with additional colonies
    public String unparsedRaceName() {
    	if (isPlayer() && isCustomRace())
    		return dataRace().setupName;
    	return race().nameVariant(raceNameIndex);
    }
    public String raceName() { // TODO BR: manage custom race
    	if (isPlayer() && isCustomRace())
    		return dataRace().setupName;
    	return raceName(0);
    }
    public String raceName(int i) {
        List<String> names = substrings(unparsedRaceName(), '|');
        if (i >= names.size() || names.get(i).isEmpty())
            return names.get(0);
        else
            return names.get(i);
    }
    public boolean masksDiplomacy()               { return race().masksDiplomacy || ai().diplomat().masksDiplomacy(); }
    public List<StarSystem> shipBuildingSystems() { return shipBuildingSystems; }
    public boolean inGalacticAlliance()           { return galacticAlliance; }
    public void joinGalacticAlliance()            { galacticAlliance = true; }
    public float planetScanningRange()            { return max(3, planetScanningRange); }  // max() to correct old saves
    public void planetScanningRange(float d)      { planetScanningRange = d; }
    public float shipScanningRange()              { return shipScanningRange; }
    public void shipScanningRange(float d)        { shipScanningRange = d; }
    public float combatTransportPct()             { return combatTransportPct; }
    public void combatTransportPct(float d)       { combatTransportPct = d; }
    public float tradePiracyRate()                { return tradePiracyRate; }
    public void tradePiracyRate(float f)          { tradePiracyRate = f; }
    public boolean extinct()                      { return extinct; }
    public TechTree tech()                        { return tech; }
    public float totalReserve()                   { return totalReserve; }
    public NamedObject lastAttacker()             { return lastAttacker; }
    public void lastAttacker(NamedObject e)       { lastAttacker = e; }
    public List<ShipFleet> assignableFleets()     {
        if (tech().hyperspaceCommunications())
            return galaxy().ships.allFleets(id);
        else
            return galaxy().ships.notInTransitFleets(id);
    }
    public List<Ship> visibleShips()              { return visibleShips; }
    public EmpireView[] empireViews()             { return empireViews; }
    public List<StarSystem> newSystems()          { return newSystems; }
    public int lastCouncilVoteEmpId()             { return lastCouncilVoteEmpId; }
    public void lastCouncilVoteEmpId(int e)       { lastCouncilVoteEmpId = e; }
    public boolean knowShipETA()                  { return knowShipETA; }
    public void knowShipETA(boolean b)            { knowShipETA = (knowShipETA || b); }
    public boolean scanPlanets()                  { return scanPlanets; }
    public void scanPlanets(boolean b)            { scanPlanets = (scanPlanets || b); }
    public void setRecalcDistances()              { recalcDistances = true; }
    public int defaultMaxBases()                  { return defaultMaxBases; }
    public boolean incrDefaultMaxBases()  { 
        int maxBase=999;
        if (defaultMaxBases == maxBase)
            return false;
        defaultMaxBases = min(maxBase, defaultMaxBases+1);
        return true;
    }
    public boolean decrDefaultMaxBases() { 
        if (defaultMaxBases == 0) 
            return false;
        defaultMaxBases = max(0, defaultMaxBases-1); 
        return true;
    }

    public Colony.Orders priorityOrders()         { return priorityOrders; }
    public void priorityOrders(Colony.Orders o)   { priorityOrders = o; }
    public int colorId()                          { return bannerColor; }
    public void colorId(int i)                    { bannerColor = i; resetColors(); }
    public int shape()                            { return id / options().numColors(); }
    public float minX()                           { return minX; }
    public float maxX()                           { return maxX; }
    public float minY()                           { return minY; }
    public float maxY()                           { return maxY; }
    public boolean divertColonyExcessToResearch() { return divertColonyExcessToResearch; }
    public void toggleColonyExcessToResearch()    { divertColonyExcessToResearch = !divertColonyExcessToResearch; }
    
    public void changeColorId(int newColor) {
        int oldColor = colorId();
        
        Empire emp = galaxy().empireMatching(newColor, shape());
        if (emp != null)
            emp.colorId(oldColor);
        
        colorId(newColor);
    }
    public int defaultShipTint() {
        int maxRaces = 10;
        return id < maxRaces ? 0 : id % (ShipDesign.shipColors.length-1)+1;
    }
    private void resetColors() {
        nameColor = null;
        ownershipColor = null;
        selectionColor = null;
        reachColor = null;
        shipBorderColor = null;
        scoutBorderColor = null;
        empireRangeColor = null;
        shipImage = null;
        shipImageLarge = null;
        shipImageHuge = null;
        scoutImage = null;
        transportImage = null;
    }
    public boolean canSeeShips(int empId) {
        if (canSeeShips == null) {
            canSeeShips = new boolean[galaxy().numEmpires()];
            for (int i=0;i<canSeeShips.length;i++) 
                canSeeShips[i] = (i == id) || viewForEmpire(i).embassy().unity(); 
        }
        return canSeeShips[empId];
    }
    public Race race() {
        if (race == null)
            race = Race.keyed(raceKey);
        return race;
    }
    public Race dataRace() { // TODO BR: Follow dataRace reload
    	if (!isPlayer()) {
    		int x=1;
    	}
        if (dataRace == null)
        	if (dataRaceKey == null)
        		dataRace = Race.keyed(raceKey);
        	else
        		dataRace = Race.keyed(dataRaceKey, raceOptions());
        return dataRace;
    }
    public BufferedImage scoutImage() {
        if (scoutImage == null)
            scoutImage = ShipLibrary.current().scoutImage(shipColorId());
        return scoutImage;
    }
    public BufferedImage shipImage() {
        if (shipImage == null)
            shipImage = ShipLibrary.current().shipImage(shipColorId());
        return shipImage;
    }
    public BufferedImage shipImageLarge() {
        if (shipImageLarge == null)
            shipImageLarge = ShipLibrary.current().shipImageLarge(shipColorId());
        return shipImageLarge;
    }
    public BufferedImage shipImageHuge() {
        if (shipImageHuge == null)
            shipImageHuge = ShipLibrary.current().shipImageHuge(shipColorId());
        return shipImageHuge;
    }
    public BufferedImage transportImage() {
        if (transportImage == null)
            transportImage = ShipLibrary.current().transportImage(shipColorId());
        return transportImage;
    }
    public Color nameColor() {
        if (nameColor == null) {
            Color c = color();
            nameColor = newColor(c.getRed(), c.getGreen(), c.getBlue(), 160);
        }
        return nameColor;
    }
    public Color ownershipColor() {
        if (ownershipColor == null) {
            Color c = color();
            ownershipColor = newColor(c.getRed(), c.getGreen(), c.getBlue(), 80);
        }
        return ownershipColor;
    }
    public Color selectionColor() {
        if (selectionColor == null) {
            Color c = color();
            selectionColor = newColor(c.getRed(), c.getGreen(), c.getBlue(), 160);
        }
        return selectionColor;
    }
    public Color shipBorderColor() {
        if (shipBorderColor == null) {
            Color c = color();
            int cR = c.getRed();
            int cG = c.getGreen();
            int cB = c.getBlue();
            shipBorderColor = newColor(cR*6/8,cG*6/8,cB*6/8);
        }
        return shipBorderColor;
    }
    public Color scoutBorderColor() {
        if (scoutBorderColor == null) {
            Color c = color();
            int cR = c.getRed();
            int cG = c.getGreen();
            int cB = c.getBlue();
            scoutBorderColor = newColor(cR*4/8,cG*4/8,cB*4/8);
        }
        return scoutBorderColor;
    }
    public Color empireRangeColor() {
        if (empireRangeColor == null) {
            Color c = color();
            int cR = c.getRed();
            int cG = c.getGreen();
            int cB = c.getBlue();
            empireRangeColor = newColor(cR*3/12,cG*3/12,cB*3/12);
        }
        return empireRangeColor;
    }
    public Color reachColor() {
        if (reachColor == null) {
            Color c = color();
            reachColor = newColor(c.getRed(), c.getGreen(), c.getBlue(), 48);
        }
        return reachColor;
    }
    // modnar: add option to start game with additional colonies
    // modnar: compId is the System ID array for these additional colonies
    // BR: For Restart with new options and random races
    public Empire(Galaxy g, int empId, Race r, Race dr, StarSystem s,
    		int[] compId, Integer cId, String name, EmpireBaseData empSrc) {
        log("creating empire for ",  r.id);
        id = empId;
        raceKey = r.id;
        dataRaceKey = dr.id;
        raceOptions(dr.raceOptions()); // BR: for custom races
        homeSysId = capitalSysId = s.id;
        compSysId = compId; // modnar: add option to start game with additional colonies
        if (empSrc != null && empId != Empire.PLAYER_ID) // BR: For Restart with new options 
        	selectedAI = empSrc.raceAI();

        empireViews = new EmpireView[options().selectedNumberOpponents()+1];
        status = new EmpireStatus(this);
        sv = new SystemInfo(this);
        // many things need to know if this is the player civ, so set it early
        if (empId == Empire.PLAYER_ID) {
            divertColonyExcessToResearch = UserPreferences.divertColonyExcessToResearch();
            g.player(this);
        }
        
        colorId(cId);
        race = r;
        dataRace = dr;
        String raceName = r.nextAvailableName();
        raceNameIndex = r.nameIndex(raceName);
        String leaderName = name == null ? r.nextAvailableLeader() : name;
        leader = new Leader(this, leaderName);
        if (empSrc != null && empId != Empire.PLAYER_ID) { // BR: For Restart with new options 
        	leader.personality = empSrc.personality;
        	leader.objective   = empSrc.objective;
        }
        shipLab = new ShipDesignLab();
    }
    public void setBounds(float x1, float x2, float y1, float y2) {
        minX = x1;
        maxX = x2;
        minY = y1;
        maxY = y2;
    }
    public void loadStartingTechs() {
        tech.recalc(this);
    }
    public void loadStartingShipDesigns() {
        shipLab.init(this);
    }
    public boolean isPlayer()            { return id == PLAYER_ID; };
    public boolean isAI()                { return id != PLAYER_ID; };
    public boolean isPlayerControlled()  { return !isAIControlled(); }
    public boolean isAIControlled()      { return isAI() || options().isAutoPlay(); }
    //public boolean isAIControlled()      { return true; } //for quick switch to test how the AI would have fared in a game
    public Color color()                 { return options().color(bannerColor); }
    public int shipColorId()             { return colorId(); }
    @Override
    public String name() {  // TODO BR: manage custom race
        if (empireName == null)
        	if (isPlayer() && isCustomRace())
        		empireName = dataRace().empireTitle;
        	else
        		empireName = replaceTokens("[this_empire]", "this");
        return empireName;
    }
    public DiplomaticReply respond(String reason, Empire listener) {
        return respond(reason,listener,null);
    }
    public DiplomaticReply respond(String reason, Empire listener, Empire other) {
        return respond(reason,listener,other,"other");
    }
    public DiplomaticReply respond(String reason, Empire listener, Empire other, String otherName) {
        String message = DialogueManager.current().randomMessage(reason, this);
        message = replaceTokens(message, "my");
        message = listener.replaceTokens(message, "your");
        if (other != null)
            message = other.replaceTokens(message, otherName);
        return DiplomaticReply.answer(true, message);
    }
    public DiplomaticReply respond(String reason, DiplomaticIncident inc, Empire listener) {
        String message = DialogueManager.current().randomMessage(reason, this);
        message = replaceTokens(message, "my");
        message = listener.replaceTokens(message, "your");
        message = inc.decode(message);
        return DiplomaticReply.answer(true, message);
    }
    public void chooseNewCapital() {
        // make list of every colony that is not the current capital
        StarSystem currentCapital = galaxy().system(capitalSysId);
        List<StarSystem> allExceptCapital = new ArrayList<>(allColonizedSystems());
        allExceptCapital.remove(currentCapital);
        
        // from that, make a list of non-rebelling colonies
        List<StarSystem> possible = new ArrayList<>();
        for (StarSystem sys: allExceptCapital) {
            if (!sys.colony().inRebellion())
                possible.add(sys);
        }
        // if EVERY colony is rebelling, then choose from any of them
        if (possible.isEmpty())
            possible.addAll(allExceptCapital);
        
        // if still no other colonies, give it up (we are losing our capital and last colony)
        if (possible.isEmpty())
            return;
        
        // sort based on production, choose highest(last), then end any rebellions on it
        Collections.sort(possible, StarSystem.BASE_PRODUCTION);
        StarSystem newHome = possible.get(possible.size()-1);
        capitalSysId = newHome.id;
        newHome.colony().clearAllRebellion();        
    }
    public int shipCount(int hullSize) {
        return galaxy().ships.hullSizeCount(id, hullSize);
    }
    @Override
    public String toString()   { return concat("Empire: ", raceName()); }

    public String replaceTokens(String s, String key) { // TODO BR: complete
        List<String> tokens = this.varTokens(s, key);
        String s1 = s;
        for (String token: tokens) {
            String replString = concat("[",key, token,"]");
            // leader name is special case, not in dictionary
            if (token.equals("_name")) 
                s1 = s1.replace(replString, leader().name());
            else if (token.equals("_home"))
                s1 = s1.replace(replString, sv.name(capitalSysId()));   
            else if (isCustomPlayer() && !isRandomized() && token.equals("_race"))
                    s1 = s1.replace(replString, dataRace().setupName);              
            else if (isCustomPlayer() && !isRandomized() && token.equals("_empire"))
                    s1 = s1.replace(replString, dataRace().empireTitle);
            else {
                List<String> values = substrings(race().text(token), ',');
                String value = raceNameIndex < values.size() ? values.get(raceNameIndex) : values.get(0);
                s1 = s1.replace(replString, value);
            }
        }
        return s1;
    }
    public String label(String token) {
        List<String> values = substrings(race().text(token), ',');
        return raceNameIndex < values.size() ? values.get(raceNameIndex) : values.get(0);      
    }
    public boolean canSendTransportsFrom(StarSystem sys) {
        if (sys == null)
            return false;
        if (sys.empire() != this)
            return false;
        if (sv.maxTransportsToSend(sys.id) == 0)
            return false;
        if (sys.colony().inRebellion())
            return false;
        if (sys.colony().quarantined())
            return false;
        
        for (StarSystem abSys: galaxy().abandonedSystems()) {
            if (sv.inShipRange(abSys.id) && canColonize(abSys))
                return true;
        }
            
        return true;
    }
    public boolean canAbandonTo(StarSystem sys) {
        if (sys == null)
            return false;
        if (sys.empId() == id)
            return true;
        return false;
    }
    public boolean canSendTransportsTo(StarSystem sys) {
        if (sys == null)
            return false;
        if (!sv.isScouted(sys.id))
            return false;
        if (!sv.isColonized(sys.id) && !sv.isAbandoned(sys.id))
            return false;
        if (!sv.inShipRange(sys.id))
            return false;
        if ((sys.empire() == this) && sys.colony().inRebellion())
            return true;
        return canColonize(sys.planet().type());
    }
    public boolean canRallyFleetsFrom(int sysId) {
        return (sysId != StarSystem.NULL_ID) && (sv.empire(sysId) == this) && (allColonizedSystems().size() > 1);
    }
    public boolean canRallyFleetsTo(int sysId) {
        return (sysId != StarSystem.NULL_ID) && (sv.empire(sysId) == this) && (allColonizedSystems().size() > 1);
    }
    public boolean canRallyFleets() {
        return allColonizedSystems().size() > 1;
    }
    public boolean canSendTransports() {
        return allColonizedSystems().size() > 1;
    }
    public int maxTransportsAllowed(StarSystem sys) {
        if (!canSendTransportsTo(sys))
            return 0;
        else if (sys.empire() == this)
            return (int)(sv.currentSize(sys.id) - (int) sv.population(sys.id))+sys.transportSprite().amt();
        else
            return (int) sv.currentSize(sys.id);
    }
    public void changeAllExistingRallies(StarSystem dest) {
        if (canRallyFleetsTo(id(dest))) {
            for (StarSystem sys: allColonizedSystems()) {
                if (sv.hasRallyPoint(sys.id))
                    sv.rallySystem(sys.id, dest);
            }
        }
    }
    public void changeRalliesFromAToB(StarSystem source, StarSystem dest) {
        if (canRallyFleetsTo(id(dest))) {
            for (StarSystem sys: allColonizedSystems()) {
                if (sv.rallySystem(sys.id) == source)
                    sv.rallySystem(sys.id, dest);
            }
            sv.rallySystem(source.id, dest);
        }
    }
    public void startRallies(List<StarSystem> fromSystems, StarSystem dest) {
        if (canRallyFleetsTo(id(dest))) {
            for (StarSystem sys: fromSystems)
                sv.rallySystem(sys.id, dest);
        }
    }
    public void stopRallies(List<StarSystem> fromSystems) {
        for (StarSystem sys: fromSystems)
            sv.stopRally(sys.id);
    }
    public void cancelTransport(StarSystem from) {
        from.transportSprite().clear();
    }
    public void deployTransport(StarSystem from) {
        from.transportSprite().accept();
    }
    public void deployTransports(List<StarSystem> fromSystems, StarSystem dest, boolean synch) {
        if (synch) {
            float maxTime = 0;
            for (StarSystem from: fromSystems)
                maxTime = max(maxTime, from.colony().transport().travelTime(dest));
            for (StarSystem from: fromSystems)
                from.transportSprite().accept(maxTime);
        }
        else {
            for (StarSystem from: fromSystems)
                from.transportSprite().accept();
        }
    }
    public int travelTurns(StarSystem from, StarSystem dest, float speed) {
        if (from.hasStargate(this) && dest.hasStargate(this))
            return 1;
        return (int) Math.ceil(from.travelTime(from,dest,speed));
    }
    public void stopRalliesWithSystem(StarSystem dest) {
        List<StarSystem> systems = allColonizedSystems();
        sv.stopRally(dest.id);
        for (StarSystem sys: systems) {
            if (sv.rallySystem(sys.id) == dest) 
                sv.stopRally(sys.id);
        }
    }
    public void validate() {
        recalcPlanetaryProduction();
        validateColonizedSystems();
        validateFleets();
        for (StarSystem sys: colonizedSystems)
            sys.colony().validate();
    }
    private void validateColonizedSystems() {
        List<StarSystem> good = new ArrayList<>();
        for (StarSystem sys: colonizedSystems) {
            if (sys.isColonized() && (sys.empire() == this)) {
                if (!good.contains(sys))
                    good.add(sys);
            }
        }
        colonizedSystems.clear();
        colonizedSystems.addAll(good);
    }
    private void validateFleets() {
        for(ShipFleet fl : galaxy().ships.allFleets(id)) {
            if(fl.bcValue() == 0) {
                fl.clear();
                galaxy().ships.deleteFleet(fl);
            }
        }
    }
    public void cancelTransports(List<StarSystem> fromSystems) {
        for (StarSystem from: fromSystems)
            from.transportSprite().clear();
    }
    public void addColonyOrder(Colony.Orders order, float amt) {
        for (StarSystem sys: allColonizedSystems()) {
            Colony col = sys.colony();
            col.addColonyOrder(order, amt);
            if (col.reallocationRequired)
                governorAI().setColonyAllocations(col);     
        }
    }
    public void addColonizedSystem(StarSystem s) {
        if (!colonizedSystems.contains(s)) {
            colonizedSystems.add(s);
            setRecalcDistances();
            refreshViews();
            for (Empire ally: allies())
            {
                ally.setRecalcDistances();       
                ally.refreshViews();
            }
        }
    }
    public void removeColonizedSystem(StarSystem s) {
        colonizedSystems.remove(s);
        setRecalcDistances();
        refreshViews();
        for (Empire ally: allies())
        {
            ally.setRecalcDistances();
            ally.refreshViews();
        }
        
        if (colonizedSystems.isEmpty())
            goExtinct();
    }
    public void takeAbandonedSystem(StarSystem sys, Transport tr) {
        sys.addEvent(new SystemColonizedEvent(id));
        newSystems.add(sys);
        addColonizedSystem(sys);
        sys.becomeColonized(sys.name(), this);
        sys.colony().setPopulation(min(sys.planet().currentSize(),tr.size()));
        tr.size(0);
    }
    public Colony colonize(String sysName, StarSystem sys) {
        StarSystem home = galaxy().system(capitalSysId);
        sys.addEvent(new SystemColonizedEvent(id));
        newSystems.add(sys);
        Colony c = sys.becomeColonized(sysName, this);
        addColonizedSystem(sys);
        governorAI().setInitialAllocations(c);
        if (isPlayerControlled()) {
            int maxTransportPop =(int)(sys.planet().maxSize()-sys.colony().population());
            galaxy().giveAdvice("MAIN_ADVISOR_TRANSPORT", sysName, str(maxTransportPop), home.name());
            session().addSystemToAllocate(sys, text("MAIN_ALLOCATE_COLONIZED", sysName));
        }
        return c;
    }
    public Colony colonizeHomeworld() {
        StarSystem home = galaxy().system(homeSysId);
        home.addEvent(new SystemHomeworldEvent(id));
        newSystems.add(home);
        colonizedSystems.add(home);
        Colony c = home.becomeColonized(home.name(), this);
        c.setHomeworldValues();
        governorAI().setInitialAllocations(c);
        sv.refreshFullScan(homeSysId);
        return c;
    }
    // modnar: add option to start game with additional colonies
    public Colony colonizeCompanionWorld(int sysId) {
        StarSystem sys1 = galaxy().system(sysId);
        sys1.addEvent(new SystemColonizedEvent(id));
        newSystems.add(sys1);
        colonizedSystems.add(sys1);
        Colony c1 = sys1.becomeColonized(sys1.name(), this);
        c1.setCompanionWorldValues();
        governorAI().setInitialAllocations(c1);
        sv.refreshFullScan(sys1.id);
        return c1;
    }
    public boolean isHomeworld(StarSystem sys) {
        return sys.id == homeSysId;
    }
    public boolean isCapital(StarSystem sys) {
        return sys.id == capitalSysId;
    }
    public boolean isColony(StarSystem sys) {
        return (sv.empire(sys.id) == this) && isEnvironmentHostile(sys);
    }
    public boolean isEnvironmentHostile(StarSystem  sys) {
        return !dataRace().ignoresPlanetEnvironment() && sys.planet().isEnvironmentHostile();
    }
    public boolean isEnvironmentFertile(StarSystem  sys) {
        return sys.planet().isEnvironmentFertile();
    }
    public boolean isEnvironmentGaia(StarSystem  sys) {
        return sys.planet().isEnvironmentGaia();
    }
    public void setBeginningColonyAllocations() {
        // try to maximum industry at start of game (for players)
        Colony c = galaxy().system(homeSysId).colony();
        c.clearSpending();
        governorAI().setInitialAllocations(c);
    }
    public boolean colonyCanScan(StarSystem sys) {
        return scanPlanets && sv.withinRange(sys.id, planetScanningRange());
    }
    public boolean fleetCanScan(StarSystem sys) {
        if (!scanPlanets)
            return false;
        List<ShipFleet> fleets = galaxy().ships.allFleets(id);
        for (ShipFleet fl: fleets) {
            if (shipScanningRange() >= fl.distanceTo(sys))
                return true;
        }
        return false;
    }
    public float shipRange()              { return tech().shipRange(); }
    public float scoutRange()             { return tech().scoutRange(); }
    
    public float colonyShipRange() {
        // return max range of design with colony special
        float range = shipRange();
        for (int slot=0;slot<ShipDesignLab.MAX_DESIGNS;slot++) {
            ShipDesign d = shipLab().design(slot);
            if(d.active() && d.hasColonySpecial() && (d.range() > range))
                range = d.range();
        }
        return range;
    }
    public float researchingShipRange()   { return tech().researchingShipRange(); }
    public float researchingScoutRange()  { return tech().researchingScoutRange(); }
    public float learnableShipRange()     { return tech().learnableShipRange(); }
    public float learnableScoutRange()    { return tech().learnableScoutRange(); }
    public float shipReach(int turns)   { return min(shipRange(), turns*tech().topSpeed()); }
    public float scoutReach(int turns)  { return min(scoutRange(), turns*tech().topSpeed()); }
    
    public String rangeTechNeededToScout(int sysId) {
        float dist = sv.distance(sysId);
        return tech().rangeTechNeededToScoutDistance(dist);
    }
    public String rangeTechNeededToReach(int sysId) {
        float dist = sv.distance(sysId);
        return tech().rangeTechNeededToReachDistance(dist);
    }    
    public String environmentTechNeededToColonize(int sysId) {
        if (canColonize(sysId))
            return null;
        int hostility = sv.planetType(sysId).hostility();
        return tech().environmentTechNeededToColonize(hostility);
    }    
    public boolean canColonize(int sysId) {
        StarSystem sys = galaxy().system(sysId);
        return canColonize(sys.planet().type());
    }
    public boolean canColonize(StarSystem sys) {
        return canColonize(sys.planet().type());
    }
    public boolean canColonize(PlanetType pt) {
        if (pt == null)  // hasn't been scouted yet
            return false;
        if (pt.isAsteroids())
            return false;
        if (ignoresPlanetEnvironment())
            return true;
        return tech().canColonize(pt);
    }
    public boolean canColonize(PlanetType pt, int newHostilityLevel) {
        if (pt == null)  // hasn't been scouted yet
            return false;
        if (pt.isAsteroids())
            return false;
        if (ignoresPlanetEnvironment())
            return true;
        return tech().canColonize(pt, newHostilityLevel);
    }
    public boolean isLearningToColonize(PlanetType pt) {
        if (pt == null)  // hasn't been scouted yet
            return false;
        if (pt.isAsteroids())
            return false;
        if (ignoresPlanetEnvironment())
            return true;
        return tech().isLearningToColonize(pt);
    }
    public boolean canLearnToColonize(PlanetType pt) {
        if (pt == null)  // hasn't been scouted yet
            return false;
        if (pt.isAsteroids())
            return false;
        if (ignoresPlanetEnvironment())
            return true;
        return tech().canLearnToColonize(pt);
    }
    public boolean knowETA(Ship sh) {
        return knowShipETA || canSeeShips(sh.empId());
    }
    public StarSystem defaultSystem() {
        StarSystem home = galaxy().system(capitalSysId);
        if (home.empire() == this)
            return home;
        else
            return allColonizedSystems().get(0);
    }
    public void addViewFor(Empire emp) {
        if ((emp != null) && (emp != this))
            empireViews[emp.id] = new EmpireView(this, emp);
    }
    public float tradeIncomePerBC() {
        float empireBC = totalPlanetaryProduction();
        float income = netTradeIncome();
        return income / empireBC;
    }
    public void nextTurn() {
        log(this + ": NextTurn");
        shipBuildingSystems.clear();
        newSystems.clear();
        recalcPlanetaryProduction();
        tech().preNextTurn();
        for (ShipDesign d : shipLab.designs()) {
            if (d != null)
                d.preNextTurn();
        }
        
        // assign funds/costs for diplomatic activities
        for (EmpireView v : empireViews()) {
          if ((v!= null) && v.embassy().contact())
                v.spies().report().clear();
        }

        List<StarSystem> allColonies = allColonizedSystems();
        List<Transport> transports = transports();

        if (!extinct) {
            if (allColonies.isEmpty() && transports.isEmpty()) {
                goExtinct();
                return;
            }
        }

        // assign planetary funds/costs & enact development
        for (StarSystem s: allColonies) {
            Colony col = s.planet().colony();
            addReserve(col.production() * col.colonyTaxPct());
            col.nextTurn();
        }
        // modnar: newRace GearHead gets 0.25 BC directly from 1.0 POP
        // BR: direct call to Race
        addToTreasury(dataRace().bCBonus() * totalPlanetaryPopulation());
        recalcPlanetaryProduction();
    }
    public void postNextTurn() {
        log(this + ": postNextTurn");
        float civProd = totalPlanetaryProduction();
        float spyMod = spySpendingModifier();

        // assign funds/costs for diplomatic activities
        for (EmpireView v : empireViews()) {
          if ((v!= null) && v.embassy().contact())
                v.nextTurn(civProd, spyMod);
        }
    }
    public void assessTurn() {
         log(this + ": AssessTurn"); 
        // have to assess trade & security views
        // before colonies since taxes may change  
        empireViewsAssessTurn();
        recalcPlanetaryProduction();

        if (status() != null)
            status().assessTurn();

        for (int i=0;i<sv.count();i++) {
            if ((sv.empire(i) == this) && sv.isColonized(i))
                sv.colony(i).assessTurn();
        }
    }
    public void lowerECOToCleanIfEcoComplete() {
        List<StarSystem> systems = new ArrayList<>(colonizedSystems);
        for (StarSystem sys: systems) {
            if (sys.isColonized())
                sys.colony().lowerECOToCleanIfEcoComplete();
        }
    }
    public void empireViewsAssessTurn() {
        for (EmpireView v : empireViews()) {
            if ((v!= null) && v.embassy().contact()) {
                v.embassy().assessTurn();
                v.trade().assessTurn();
            }
        }
    }
    public void makeDiplomaticOffers() {
        for (EmpireView v : empireViews()) {
            if ((v!= null) && v.embassy().contact())
                v.makeDiplomaticOffers();
        }
    }
    public void hideSpiesAgainst(int empId) {
        EmpireView v = viewForEmpire(empId);
        if (v != null)
            v.spies().beginHide();
    }
    public void shutdownSpyNetworksAgainst(int empId) {
        EmpireView v = viewForEmpire(empId);
        if (v != null) 
            v.spies().shutdownSpyNetworks();
    }
    public StarSystem retreatSystem(StarSystem from) {
        return shipCaptainAI().retreatSystem(from);
    }
    public void retreatShipsFrom(int empId) {
        List<Transport> transports = transports();
        for (Transport tr: transports) {
            if (tr.destination().empId() == empId)
                tr.orderToSurrenderOnArrival();
        }
        ShipCaptain shipCaptain = shipCaptainAI();
        Ships shipMgr = galaxy().ships;
        List<ShipFleet> fleets = shipMgr.allFleets(id);
        for (ShipFleet fl: fleets) {
            // if orbiting a system colonized by empId, then retreat it
            if (fl.isOrbiting()) {
                StarSystem orbitSys = fl.system();
                if (orbitSys.empId() == empId) {
                    StarSystem dest = shipCaptain.retreatSystem(orbitSys); 
                    if (dest != null)
                        shipMgr.retreatFleet(fl, dest.id);
                }
            }
            // if in transit to a system colonized by empId, then
            // set it to retreat on arrival
            else if (fl.isInTransit()) {
                StarSystem dest = fl.destination();
                if (dest.empId() == empId)
                    fl.makeRetreatOnArrival();
            }
        }
    }
    public void completeResearch() {
        tech.allocateResearch();
    }
    public void acquireTradedTechs() {
        tech.acquireTradedTechs();
    }
    public void makeNextTurnDecisions() {
        recalcPlanetaryProduction();

        log(this + ": make NextTurnDecisions");
        NoticeMessage.setSubstatus(text("TURN_SCRAP_SHIPS"));
        shipLab.nextTurn();

        // empire settings
        if (isAIControlled()) {
            scientistAI().setTechTreeAllocations();
            securityAllocation = spyMasterAI().suggestedInternalSecurityLevel();
            empireTaxLevel = governorAI().suggestedEmpireTaxLevel();
            //ail: calling this before fleetCommanderAI avoids a possible case where a fleet is slower than it could be due to scrapping ships after the fleet was launched
            NoticeMessage.setSubstatus(text("TURN_DESIGN_SHIPS"));
            shipDesignerAI().nextTurn();
            fleetCommanderAI().nextTurn();
            ai().sendTransports();
        }

        if (isAIControlled()) {
            ai().treasurer().allocateReserve();
            // diplomatic activities
            for (EmpireView ev : empireViews()) {
                if ((ev != null) && ev.embassy().contact())
                    ev.setSuggestedAllocations();
            }
        } else {
            autospend();
            autotransport();
            // colonize first, then attack, then scout
            autocolonize();
            autoattack();
            autoscout();
            if(session().getGovernorOptions().isAutoSpy() || session().getGovernorOptions().isAutoInfiltrate())
            {
                for (EmpireView ev : empireViews()) {
                    if ((ev != null) && ev.embassy().contact())
                        ev.setSuggestedAllocations();
                }
            }
            // If planets are governed, redo allocations now
            for (int i = 0; i < this.sv.count(); ++i) {
                if (this.sv.empire(i) == this && this.sv.isColonized(i)) {
                    this.sv.colony(i).governIfNeeded();
                }
            }
        }
        
        // colony development (sometimes done for player if auto-pilot)
        NoticeMessage.setSubstatus(text("TURN_COLONY_SPENDING"));
        for (int n=0; n<sv.count(); n++) {
            if (sv.empId(n) == id)
                if(!sv.colony(n).isGovernor() || isAIControlled() || sv.colony(n).shipyard().shipLimitReached() || sv.colony(n).shipyard().stargateCompleted()) //do not overrule the governor if it is enabled, except it's for ship-limit-reached
                    governorAI().setColonyAllocations(sv.colony(n));
        }
    }
    /**
     * Spend reserve automatically (if enabled).
     *
     * Spend only on planets with production &lt; 30% average
     * Spend only the amount planet can consume this turn
     * Start with planet with lowest production, and end when money runs out
     * or no suitable planets are available.
     * Spend only on planets with governor on.
     * Spend only if industry and ecology are not complete.
     *
     */
    public void autospend() {
        GovernorOptions options = session().getGovernorOptions();
        if (isAIControlled() || !options.isAutospend()) {
            return;
        }
        // if reserve is low, don't even attempt to spend money
        if ((int)this.totalReserve() <= options.getReserve()) {
            return;
        }

        List<Colony> colonies = new LinkedList<>();

        for (StarSystem ss: this.colonizedSystems) {
            Colony c = ss.colony();
            if (c.isGovernor() && c.maxReserveNeeded() >= 1) {
                if (!c.industry().isCompleted() || !c.ecology().isCompleted()) {
                    colonies.add(c);
                }
            }
        }
        Collections.sort(colonies,
                (Colony o1, Colony o2) -> (int)Math.signum(o1.production() - o2.production()));
//        for (Colony c: colonies) {
//            System.out.println("Autospend "+c.production()+" "+c.name());
//        }
        // spend money
        for (Colony c: colonies) {
            if (c.maxReserveNeeded() <= 0) {
                continue;
            }
            float available = totalReserve() - options.getReserve();
            if (available <= 1) {
                break;
            }
            // overallocate by 1 BC to speed up fractional spending
            int bcToSpend = (int)Math.ceil(Math.min(available, c.maxReserveNeeded()));
            allocateReserve(c, bcToSpend);
//            System.out.format("Autospend allocated %d bs to %s%n", bcToSpend, c.name());
        }
    }
    // New autotransport. Start with targets first.
    public void autotransport() {
        GovernorOptions options = session().getGovernorOptions();
        if (isAIControlled()) {
            return;
        }
        if(options.isAutotransportXilmi())
            ai.sendTransports();
        if(!options.isAutotransport())
            return;

        List<Colony> colonies = new LinkedList<>();
        for (int i = 0; i < this.sv.count(); ++i) {
            if (this.sv.empire(i) == this && this.sv.isColonized(i)) {
                Colony c = this.sv.colony(i);
                // don't transport to populations that are missing less than 4 population
                if (c.planet().currentSize() - c.expectedPopulation() > 4 ) {
                    colonies.add(c);
                }
            }
        }
        // all colonies at full population- nothing to do
        if (colonies.isEmpty()) {
            return;
        }

        Collections.sort(colonies,
                (Colony o1, Colony o2) -> (int)Math.signum(o1.expectedPopPct() - o2.expectedPopPct()));
        /*for (Colony c: colonies) {
            System.out.println("Transport Recipient "+c.expectedPopPct()+" "+c.expectedPopulation()+"/"+c.planet().currentSize()+" "+c.name());
        }*/

        List<Colony> donors = new LinkedList<>();
        for (int i = 0; i < this.sv.count(); ++i) {
            if (this.sv.empire(i) == this && this.sv.isColonized(i)) {
                Colony c = this.sv.colony(i);
                // don't send population from planets with governor off
                if (!c.isGovernor()) {
                    continue;
                }
                if (c.transporting() || !c.canTransport() || c.maxTransportsAllowed() < 1) {
                    continue;
                }
                // don't ship population from planets with 20 or less population
                if (c.planet().currentSize() <= 20) {
                    continue;
                }
                // we don't have excess population. Allow transporting 1 pop to stabilize planet ant max-1
                if (c.expectedPopulation() < (c.planet().currentSize() - 2)) {
                    continue;
                }
                // if this option is checked, don't send out population out of planets which are Rich or Artefacts
                if (options.isTransportRichDisabled() &&
                        (c.planet().isResourceRich() || c.planet().isResourceUltraRich() ||
                                c.planet().isArtifact() || c.planet().isOrionArtifact())) {
                    continue;
                }
                // ship population out earlier, don't check if ecology is all done.
                // ship population out earlier, don't check if industry is all done.
                donors.add(c);
            }
        }
        /*for (Colony c: donors) {
            System.out.println("Transport Donor "+c.expectedPopulation()+"/"+c.planet().currentSize()+" "+c.name());
        }*/
        // no potential donors- no reason to do any of this.
        if (donors.isEmpty()) {
            return;
        }
        // for each underpopulated colony, find a suitable donor and ship some population
        // More expensive approach- after sending out some population, resort the target list, so that
        // population is more evenly distributed when there are many colonies that need population transported
        while (!colonies.isEmpty() && !donors.isEmpty()) {
            Colony c = colonies.get(0);
            float neededPopulation = (int) (c.planet().currentSize() - c.expectedPopulation());
            // Sort donors by distance
            Colony donor = Collections.min(donors, (Colony o1, Colony o2) -> (int) Math.signum(
                    o1.travelTime(o1, c, this.tech().transportTravelSpeed()) -
                            o2.travelTime(o2, c, this.tech().transportTravelSpeed())));
            if (neededPopulation > 1) {
                float transportTime = donor.travelTime(donor, c, this.tech().transportTravelSpeed());
                // limit max transport time
                double maxTime = c.starSystem().inNebula() ? options.getTransportMaxTurns() * 1.5 : options.getTransportMaxTurns();
                if (transportTime > maxTime) {
                    // if first donor doesn't match max travel time, others surely won't.
                    // so no way we're going to transport population to this colony, remove it from the list
                    colonies.remove(c);
                    continue;
                }
                // TODO: Take into account proper governing, simulate X turns
                // That's close to impossible, Colony has multiple side effects on Planet and on Empire and
                // produces ships
                // if population will grow large enough naturally before transports arrive, don't transport.
                double expectedPopAtTransportTime = c.population() +
                        Math.pow(1+c.unrestrictedPopGrowth() / c.population(), transportTime);
                if (expectedPopAtTransportTime >= c.planet().currentSize()) {
                    // colony will be full by the time population arrives from the closest donor, skip this colony
                    colonies.remove(c);
                    continue;
                }

//                System.out.println("Will transport from "+donor.name()+" to "+c.name());
//                System.out.println("Before transport expectedPopulation= "+c.expectedPopulation());
                // if we expect transports in excess of maximum population, ship that away too.
                int expectedOverPopulation = Math.round(donor.expectedPopulation() - donor.planet().currentSize());
                int growth = Math.max(1, Math.round(donor.unrestrictedPopGrowth()));
                //System.out.println("Donor "+donor.name()+" overPopulation="+expectedOverPopulation+" growth="+growth);
                int populationToTransport = Math.max(expectedOverPopulation, growth);
                // if this option is set, transport 2x population from poor planets
                if (options.isTransportPoorDouble() && (donor.planet().isResourcePoor() || donor.planet().isResourceUltraPoor()) ) {
                    populationToTransport *= 2;
                }
                donor.scheduleTransportsToSystem(c.starSystem(), populationToTransport);
//                System.out.println("After transport expectedPopulation="+c.expectedPopulation());
                donors.remove(donor);
            } else {
                colonies.remove(c);
            }
            Collections.sort(colonies,
                    (Colony o1, Colony o2) -> (int)Math.signum(o1.expectedPopPct() - o2.expectedPopPct()));
        }
    }
    private List<ShipDesign> scoutDesigns() {
        // Pick scout designs
        List<ShipDesign> scoutDesigns = new ArrayList<>();
        for (ShipDesign sd: shipLab().designs()) {
            if (sd.isAutoScout()) {
                scoutDesigns.add(sd);
            }
        }
        // sort extended range vs normal range. Send out normal range first.
        // sort scouts fastest to slowest, send out fastest scouts first.
        scoutDesigns.sort((d1, d2) -> {
            int rangeDiff = Boolean.compare(d1.isExtendedRange(), d2.isExtendedRange() );
            if (rangeDiff != 0) {
                return rangeDiff;
            } else {
                return d2.warpSpeed() - d1.warpSpeed();
            }
        } );
        return scoutDesigns;
    }
    private boolean hasExtendedRange(List<ShipDesign> designs) {
        return designs.stream().anyMatch(sd -> sd.isExtendedRange());
    }

    private List<Integer> filterTargets(Predicate<Integer> filterFunction) {
        List<Integer> targets = new LinkedList<>();
        for (int i = 0; i < this.sv.count(); ++i) {
            if (filterFunction.test(i)) {
                targets.add(i);
            }
        }
        return targets;
    }

    private List<ShipFleet> filterFleets(List<ShipDesign> designs, int shipCount, BiPredicate<ShipFleet, ShipDesign> extraFilter) {
        List<ShipFleet> fleets = new LinkedList<>();
        List<ShipFleet> allFleets = galaxy().ships.notInTransitFleets(id);
        for (ShipFleet sf : allFleets) {
            if (!sf.isOrbiting() || !sf.canSend()) {
                // we only use idle (orbiting) fleets
                continue;
            }
            for (ShipDesign sd: designs) {
                // must have at least minimum number of ships
                if (sf.num(sd.id()) >= shipCount) {
                    // don't send ships orbiting enemy planets, they are obviously needed there!
                    // It's ok to send ships orbiting empty systems
                    // Ok, let's make it OK to send away scouts & colony ships, but not "autoattack" ships
                    if (sd.isArmed() && sd.isAutoAttack()) {
                        if (sf.system().empire() != this &&
                                !PlanetType.NONE.equals(sf.system().planet().type().key())) {
                            continue;
                        }
                    }

                    if (extraFilter.test(sf, sd)) {
                        fleets.add(sf);
                    }
                    break;
                }
            }
        }
        return fleets;
    }

    // Total count of fleets we can send, with given count of ships in each, of given designs in all given fleets
    private int fleetCount(List<ShipFleet> fleets, List<ShipDesign> designs, int sendCount) {
        int count = 0;
        for (ShipFleet sf : fleets ) {
            for (ShipDesign sd: designs) {
                count += sf.num(sd.id()) / sendCount;
            }
        }
        return count;
    }

    private int warpSpeed(ShipFleet sf, List<ShipDesign> designs, int sendCount) {
        int minWarpSpeed = -1;
        for (ShipDesign sd: designs) {
            if (sf.num(sd.id()) >= sendCount) {
                if (minWarpSpeed < 0 || sd.warpSpeed() < minWarpSpeed) {
                    minWarpSpeed = sd.warpSpeed();
                }
            }
        }
        return minWarpSpeed;
    }
    private int warpSpeed(List<ShipDesign> designs) {
        int minWarpSpeed = -1;
        for (ShipDesign sd: designs) {
            if (minWarpSpeed < 0 || sd.warpSpeed() < minWarpSpeed) {
                minWarpSpeed = sd.warpSpeed();
            }
        }
        return minWarpSpeed;
    }
    private boolean alreadyHasDesignsOrbiting(int si, List<ShipDesign> designs,
                                              BiPredicate<ShipDesign, Integer> designFitForSystem) {
        // if target system already has a colony ship in orbit, don't send colony ships there
        ShipFleet orbitingFleet = sv.system(si).orbitingFleetForEmpire(this);
        if (orbitingFleet != null) {
            for (ShipDesign d1: designs) {
                if (orbitingFleet.hasShip(d1) && designFitForSystem.test(d1, si)) {
                    return true;
                }
            }
        }
        return false;
    }

    private boolean deploy(ShipFleet sf,  ShipDesign sd, int target, int sendCount) {
        int[] counts = new int[ShipDesignLab.MAX_DESIGNS];
        counts[sd.id()] = sendCount;

        boolean entireFleet = true;
        for (int i = 0; i < ShipDesignLab.MAX_DESIGNS; i++) {
            if (sf.num(i) != counts[i]) {
                entireFleet = false;
            }
        }

        boolean success;
        if (entireFleet) {
            galaxy().ships.deployFleet(sf, sv.system(target).id);
            System.out.println("Deployed entire ship fleet "+sv.descriptiveName(sf.sysId())+" "+sv.name(sf.sysId())
                    +" to "+sv.descriptiveName(target)+" "+sv.name(target)+
                    " design "+sd.name());
            success = true;
        } else {
            success = galaxy().ships.deploySubfleet(sf, counts, sv.system(target).id);
            System.out.println("Deploy subfleet from "+sv.descriptiveName(sf.sysId())+" "+sv.name(sf.sysId())
                    +" to "+sv.descriptiveName(target)+" "+sv.name(target)+
                    " design "+sd.name()+" success="+success);
        }
        return success;
    }

    /**
     * Sort targets to by value and to be as close to source system as possible
     */
    private interface SystemsSorter {
        void sort(Integer sourceSystem, List<Integer> targets, int warpSpeed);
    }

    /**
     * Sort flets by value and to be as close to target system as possible
     */
    private interface FleetsSorter {
        void sort(Integer targetSysten, List<ShipFleet> fleets, int warpSpeed);
    }

    public void autoSendShips(List<ShipDesign> designs, List<Integer> targets,
                              SystemsSorter systemsSorter, FleetsSorter fleetsSorter,
                              BiPredicate<ShipDesign, Integer> designFitForSystem,
                              BiPredicate<ShipFleet, ShipDesign> extraFleetFilter,
                              int sendCount) {

        // find fleets that have the required designs
        List<ShipFleet> fleets = filterFleets(designs, sendCount, extraFleetFilter);
        if (fleets.isEmpty()) {
            System.out.println("No idle ships to send");
            return;
        }
        for (ShipFleet sf: fleets) {
            System.out.println("Suitable fleet "+sf+" "+sf.system().name());
        }

        // number of fleets we can send, we are sending more than one ship in a fleet now
        int fleetCount = fleetCount(fleets, designs, sendCount);

        if (targets.size() > fleetCount) {
            System.out.println("MORE TARGET SYSTEMS THAN SHIPS");
            // we have more stars to explore than we have ships, so
            // we take ships and send them to closest systems.
            for (ShipFleet sf : fleets) {
                if (targets.isEmpty()) {
                    break;
                }
                System.out.println("Deploying ships from Fleet " + sf + " " + sf.system().name());
                int warpSpeed = warpSpeed(sf, designs, sendCount);
                systemsSorter.sort(sf.sysId(), targets, warpSpeed);

                for (ShipDesign sd: designs) {
                    // don't send same fleet to multiple destinations by mistake
                    if (!sf.isOrbiting()) {
                        continue;
                    }
                    // we have to save this number. That's because if we invoke deploySubfleet on a fleet of 1 ship
                    // entire fleet gets redirected and it will keep returning 1 ship available as it's the same fleet
                    // with same 1 ship, resulting in continued loop.
                    int numShipsAvailable = sf.num(sd.id());
                    for (Iterator<Integer> it = targets.iterator(); it.hasNext() && numShipsAvailable >= sendCount ; ) {
                        int si = it.next();
                        if (alreadyHasDesignsOrbiting(si, designs, designFitForSystem)) {
                            continue;
                        }
                        boolean deployed = false;
                        if (!sd.isExtendedRange() && sv.inShipRange(si) &&
                                designFitForSystem.test(sd, si) && extraFleetFilter.test(sf, sd)) {
                            // deploy
                            deployed = deploy(sf, sd, si, sendCount);
                        } else if (sd.isExtendedRange() && sv.inScoutRange(si) &&
                                designFitForSystem.test(sd, si) && extraFleetFilter.test(sf, sd)) {
                            // deploy
                            deployed = deploy(sf, sd, si, sendCount);
                        }
                        if (deployed) {
                            numShipsAvailable -= sendCount;
                            // remove this system as it has a ship assigned already
                            it.remove();
                        }
                    }
                }
            }
        } else {
            System.out.println("MORE SHIPS THAN TARGET SYSTEMS");
            // We sort target systems by distance from home as the starting point
            int warpSpeed = warpSpeed(designs);
            System.out.println("Warp Speed "+warpSpeed);
            systemsSorter.sort(homeSysId, targets, warpSpeed);

            nextTarget:
            for (Iterator<Integer> it = targets.iterator(); it.hasNext(); ) {
                int si = it.next();

                if (alreadyHasDesignsOrbiting(si, designs, designFitForSystem)) {
                    continue;
                }
                System.out.println("Finding fleets for system " + sv.name(si)+" "+sv.descriptiveName(si));
                fleetsSorter.sort(si, fleets, warpSpeed);
                for (ShipFleet f : fleets) {
                    System.out.println("Fleet " + sv.descriptiveName(f.sysId()) +" "+sv.name(f.sysId()) +
                            " to "+sv.descriptiveName(si)+" "+sv.name(si) +
                            " travel time " + f.travelTime(sv.system(si), warpSpeed));
                }
                for (ShipFleet sf: fleets) {
                    // if fleet was sent elsewhere during previous iteration, don't redirect it again
                    if (!sf.inOrbit()) {
                        continue;
                    }
                    for (ShipDesign sd : designs) {
                        int count = sf.num(sd.id());
                        System.out.println("We have " + count + " ships of design " + sd.name());
                        if (count < sendCount) {
                            continue;
                        }
                        boolean deployed = false;
                        if (!sd.isExtendedRange() && sv.inShipRange(si) &&
                                designFitForSystem.test(sd, si) && extraFleetFilter.test(sf, sd)) {
                            // deploy
                            deployed = deploy(sf, sd, si, sendCount);
                        } else if (sd.isExtendedRange() && sv.inScoutRange(si) &&
                                designFitForSystem.test(sd, si) && extraFleetFilter.test(sf, sd)) {
                            // deploy
                            deployed = deploy(sf, sd, si, sendCount);
                        }
                        if (deployed) {
                            // remove this system as it has a ship assigned already
                            it.remove();
                            // let's go find a new target
                            continue nextTarget;
                        }
                    }
                }
            }
        }
    }

    public void autoscout() {
        GovernorOptions options = session().getGovernorOptions();
        if (isAIControlled() || !options.isAutoScout()) {
            return;
        }

        List<ShipDesign> designs = scoutDesigns();
        // no scout ship designs
        if (designs.isEmpty()) {
            // System.out.println("No Scout designs"); // BR: //
            return;
        } else {
            for (ShipDesign sd: designs) {
                System.out.println("Scout Design "+sd.name()+" "+sd.isExtendedRange()+" sz="+sd.sizeDesc()+" warp="+sd.warpSpeed());
            }
        }

        boolean extendedRangeAllowed = hasExtendedRange(designs);

        List<Integer> targets = filterTargets(i -> {
            // scouttime only gets set for scouted systems, not ones we were forced to retreat from, don't use scouttime
            if (sv.view(i).scouted() || sv.view(i).isGuarded()) {
                return false;
            }
            if (extendedRangeAllowed && !sv.inScoutRange(i)) {
                return false;
            }
            if (!extendedRangeAllowed && !sv.inShipRange(i)) {
                return false;
            }
            // don't send scouts to occupied planets
            if (sv.view(i).empire() != null) {
                return false;
            }
            // ships already on route- no need to scout
            if (!this.ownFleetsTargetingSystem(sv.system(i)).isEmpty()) {
                System.out.println("System "+i+" "+sv.descriptiveName(i)+" already has ships going there");
                return false;
            }
            if (sv.view(i).orbitingFleets() != null && !sv.view(i).orbitingFleets().isEmpty()) {
//                    System.out.println("System "+i+" "+sv.descriptiveName(i)+" has ships in orbit");
                for (ShipFleet f: sv.view(i).orbitingFleets()) {
                    // WTF, planet should be scouting if own ships are orbiting
                    if (f.empId() == this.id) {
                        return false;
                    }
                    // if fleet isn't armed- ignore it
                    if (!f.isArmed()) {
                        continue;
                    }
                    // if fleet belongs to allied/non-aggression pact empire- ignore it
                    if (this.pactWith(f.empId) || this.alliedWith(f.empId)) {
                        continue;
                    }
                    // don't scout systems guarded by by armed enemy.
                    System.out.println("System "+i+" "+sv.descriptiveName(i)+" has armed enemy ships in orbit");
                    return false;
                }
            } else {
                System.out.println("System "+i+" "+sv.descriptiveName(i)+" has NO orbiting fleets");
            }
            return true;
        });

        // No systems to scout
        if (targets.isEmpty()) {
            return;
        }
        // shuffle toScout list. This is to prevent colony ship with autoscouting on from going directly to the habitable planet.
        // That's because AFAIK map generation sets one of the 2 nearby planets to be habitable, and it's always first one in the list
        // So if we keep default order, that's cheating
        Collections.shuffle(targets);

        for (Integer i: targets) {
            System.out.println("ToScout "+i+" "+sv.name(i) + " "+sv.descriptiveName(i)+" "+sv.view(i).scouted()+" "+sv.inScoutRange(i)+" "+sv.inShipRange(i)+" colonized="+sv.view(i).isColonized()+" bases="+sv.view(i).bases());
        }

        SystemsSorter systemsSorter = (sourceSystem, targets1, warpSpeed) -> {
            StarSystem source = sv.system(sourceSystem);
            targets1.sort((s1, s2) ->
                    (int)Math.signum(source.travelTimeTo(sv.system(s1), warpSpeed) -
                            source.travelTimeTo(sv.system(s2), warpSpeed)) );
        };

        FleetsSorter fleetsSorter = (targetSysten, fleets, warpSpeed) -> fleets.sort((f1, f2) ->
                (int)Math.signum(f1.travelTime(sv.system(targetSysten), warpSpeed) -
                        f2.travelTime(sv.system(targetSysten), warpSpeed)) );

        int sendCount = Math.max(1, GameSession.instance().getGovernorOptions().getAutoScoutShipCount());
        // don't send out armed scout ships when enemy fleet is incoming, hence the need for defend predicate
        autoSendShips(designs, targets, systemsSorter, fleetsSorter, (d, si) -> true, defendFirstPredicate(),
                sendCount);
    }

    public void autocolonize() {
        GovernorOptions options = session().getGovernorOptions();
        if (isAIControlled() || !options.isAutoColonize()) {
            return;
        }

        List<ShipDesign> designs = new ArrayList<>();

        for (ShipDesign sd: shipLab().designs()) {
            // ignore design if it's not a colony ship, even if it's market autocolonize
            if (sd.isAutoColonize() && sd.hasColonySpecial()) {
                designs.add(sd);
            }
        }
        // non-extended range to extended range
        // least hostile to most hostile colony bases, send out least hostile colony bases first
        // sort ships fastest to slowest, send out fastest colony ships first
        designs.sort((d1, d2) -> {
            int rangeDiff = Boolean.compare(d1.isExtendedRange(), d2.isExtendedRange() );
            // desc order
            int envDiff = d1.colonySpecial().tech().environment() - d2.colonySpecial().tech().environment();
            if (rangeDiff != 0) {
                return rangeDiff;
            } else if (envDiff != 0) {
                return envDiff;
            } else {
                return d2.warpSpeed() - d1.warpSpeed();
            }
        } );

        // no colony ship designs
        if (designs.isEmpty()) {
            // System.out.println("No Colony ship designs"); // BR: //
            return;
        } else {
            for (ShipDesign sd: designs) {
                System.out.println("Colony Design "+sd.name()+" "+sd.isExtendedRange()+" sz="+sd.sizeDesc()+" warp="+sd.warpSpeed());
            }
        }

        BiPredicate<ShipDesign, Integer> designFitForSystem =
            (sd, si) -> dataRace().ignoresPlanetEnvironment() ||
                    (canColonize(si) && sd.colonySpecial().canColonize(sv.system(si).planet().type()));

        boolean extendedRange = hasExtendedRange(designs);
        List<Integer> targets = filterTargets(i -> {
            // only colonize scouted systems, systems with planets, unguarded systems.
            // don't attempt to colonize systems already owned by someone
            // TODO: Exclude systems that have enemy fleets orbiting?
            if (!sv.view(i).isColonized() && sv.view(i).scouted() && canColonize(i)
                    && !sv.isGuarded(i) && sv.view(i).empire() == null ) {

                // if we don't have tech or ships to colonize this planet, ignore it.
                // Since 2.15, for a game with restricted colonization option, we have to check each design if it can colonize
                if (!dataRace().ignoresPlanetEnvironment()) {
                    boolean canColonize = false;
                    for (ShipDesign sd: designs) {
                        if (this.canColonize(i) && sd.colonySpecial().canColonize(sv.system(i).planet().type())) {
                            canColonize = true;
                            break;
                        }
                    }
                    if (!canColonize) {
                        return false;
                    }
                }

                boolean inRange;
                if (extendedRange) {
                    inRange = sv.inScoutRange(i);
                } else {
                    inRange = sv.inShipRange(i);
                }
                if (!inRange) {
                    return false;
                }
                // colony ship already on route- no need to send more
                for (ShipFleet sf: this.ownFleetsTargetingSystem(sv.system(i))) {
                    //
                    for (ShipDesign sd: shipLab().designs()) {
                        if (!sd.hasColonySpecial()) {
                            continue;
                        }
                        if (!sf.hasShip(sd)) {
                            continue;
                        }
                        // if planet already has a ship enroute which can colonize it, don't send another
                        if (designFitForSystem.test(sd, i)) {
                            System.out.println("System "+sv.name(i)+" already has colony ships going there");
                            return false;
                        }
                    }
                }
                return true;
            } else {
                return false;
            }
        });

        // No systems to colonize
        if (targets.isEmpty()) {
            return;
        }
        for (Integer i: targets) {
            System.out.println("ToColonize "+sv.name(i) + " scouted="+sv.view(i).scouted()+" extrange="+sv.inScoutRange(i)+" range="+sv.inShipRange(i)+" type"+sv.planetType(i));
        }

        FleetsSorter fleetsSorter = (targetSysten, fleets, warpSpeed) -> fleets.sort((f1, f2) ->
                (int)Math.signum(f1.travelTime(sv.system(targetSysten), warpSpeed) -
                        f2.travelTime(sv.system(targetSysten), warpSpeed)) );

        int sendCount = Math.max(1, GameSession.instance().getGovernorOptions().getAutoColonyShipCount());
        // don't send out armed colony ships when enemy fleet is incoming, hence the need for defend predicate
        autoSendShips(designs, targets, new ColonizePriority("toColonize"), fleetsSorter,
                designFitForSystem, defendFirstPredicate(), sendCount);
    }

    // similar to autocolonize. Send ships to enemy planets and systems with enemy ships in orbit
    public void autoattack() {
        GovernorOptions options = session().getGovernorOptions();
        if (isAIControlled() || !options.isAutoAttack()) {
            return;
        }

        List<ShipDesign> designs = new ArrayList<>();

        for (ShipDesign sd: shipLab().designs()) {
            // ignore design if it doesn't have weapons
            if (sd.isAutoAttack() && sd.isArmed()) {
                designs.add(sd);
            }
        }
        // non-extended range to extended range
        // Cheapest to most expensive
        // sort ships fastest to slowest, send out fastest colony ships first
        designs.sort((d1, d2) -> {
            int rangeDiff = Boolean.compare(d1.isExtendedRange(), d2.isExtendedRange() );
            // desc order
            int warpDiff = d2.warpSpeed() - d1.warpSpeed();
            // asc order, cheapest first
            int costDiff = d1.cost() - d2.cost();
            if (rangeDiff != 0) {
                return rangeDiff;
            } else if (warpDiff != 0) {
                return warpDiff;
            } else {
                return costDiff;
            }
        } );

        // no colony ship designs
        if (designs.isEmpty()) {
            System.out.println("No Attack ship designs");
            return;
        } else {
            for (ShipDesign sd: designs) {
                System.out.println("Attack Design "+sd.name()+" "+sd.isExtendedRange()+" sz="+sd.sizeDesc()+" warp="+sd.warpSpeed());
            }
        }

        BiPredicate<ShipDesign, Integer> designFitForSystem = (sd, si) -> true;

        boolean extendedRange = hasExtendedRange(designs);
        List<Integer> hostileEmpires = new ArrayList<>();
        for (EmpireView enemy: this.enemyViews()) {
            System.out.println("autoattack Enemy "+enemy.toString());
            hostileEmpires.add(enemy.empId());
        }
        int sendCount = Math.max(1, GameSession.instance().getGovernorOptions().getAutoAttackShipCount());
        List<Integer> targets = filterTargets(i -> {
            // consider both scouted and unscouted systems if they belong to the enemy
            boolean inRange;
            if (extendedRange) {
                inRange = sv.inScoutRange(i);
            } else {
                inRange = sv.inShipRange(i);
            }
            if (!inRange) {
                return false;
            }
            List<ShipFleet> fleets = sv.orbitingFleets(i);
            if (fleets != null) {
                for (ShipFleet sf: fleets) {
                    if (sf.empire() == this && sf.isArmed()) {
                        // don't target planets which already have own armed fleets in orbit
                        return false;
                    }
                }
            }
            // armed ships already on route- no need to send more
            for (ShipFleet sf: this.ownFleetsTargetingSystem(sv.system(i))) {
                for (ShipDesign sd: designs) {
                    // attack fleet already on its way, don't send more
                    if (sf.num(sd.id()) >= sendCount) {
                        return false;
                    }
                }
            }
            if (sv.empire(i) != null && hostileEmpires.contains(sv.empire(i).id)) {
                System.out.println("System "+sv.name(i)+" belongs to enemy empire, targeting");
                return true;
            }
            // This will send ships to own colonies that have enemy ships in orbit. I guess that's OK
            for (ShipFleet f: fleets) {
                if (hostileEmpires.contains(f.empId) && !f.retreating()) {
                    System.out.println("System "+sv.name(i)+" has enemy ships, targeting");
                    return true;
                }
            }
            return false;
        });

        // No systems to colonize
        if (targets.isEmpty()) {
            return;
        }
        for (Integer i: targets) {
            System.out.println("ToAttack "+sv.name(i) + " scouted="+sv.view(i).scouted()+" extrange="+sv.inScoutRange(i)+" range="+sv.inShipRange(i));
        }

        FleetsSorter fleetsSorter = (targetSysten, fleets, warpSpeed) -> fleets.sort((f1, f2) ->
                (int)Math.signum(f1.travelTime(sv.system(targetSysten), warpSpeed) -
                        f2.travelTime(sv.system(targetSysten), warpSpeed)) );


        autoSendShips(designs, targets, new ColonizePriority("toAttack"), fleetsSorter,
                designFitForSystem, defendFirstPredicate(), sendCount);
    }

    private BiPredicate<ShipFleet, ShipDesign> defendFirstPredicate() {
        // check if we have hostile incoming fleets. If so, don't send out ships
        // there's no method to do that, so let's build one.
        Set<Integer> systemHasHostileIncoming = new HashSet<>();
        Set<Integer> hostiles = hostiles().stream()
                .map(h -> h.empId())
                .collect(Collectors.toSet());
        for (ShipFleet sf : galaxy().ships.inTransitFleets()) {
            if (sf.destination().empire() != null && sf.destination().empire() == this && hostiles.contains(sf.empire().id)) {
                if (sf.destination().name().equalsIgnoreCase("Baham")) {
                    System.out.println("Important Baham");
                }
                System.out.println("Must defend system "+sf.destination().name());
                systemHasHostileIncoming.add(sf.destSysId());
            }
        }

        // also, don't send out fleet if orbiting enemy planet
        BiPredicate<ShipFleet, ShipDesign> defendFirst = (sf,sd) -> {
            // unarmed ships don't contribute to defence
            if (!sd.isArmed()) {
                return true;
            }
            if (systemHasHostileIncoming.contains(sf.system().id)) {
                return false;
            } else if (sf.system().empire() != null && sf.system().empire() != this) {
                // Don't send out armed ships orbiting enemy systems.
                // It's OK to send away armed ships orbiting uncolonized planets if there are no incoming
                // enemy fleets and that's checked above.
                return false;
            } else {
                return true;
            }
        };
        return defendFirst;
    }

    private class ColonizePriority implements SystemsSorter {
        private final String message;

        public ColonizePriority(String message) {
            this.message = message;
        }

        @Override
        public void sort(Integer sourceSystem, List<Integer> targets, int warpSpeed) {
            // ok, let's use both distance and value of planet to prioritize colonization, 50% and 50%
            StarSystem source = sv.system(sourceSystem);

            float maxDistance = -1;
            float maxValue = -1;
            for (int sid : targets) {
                float value = planetValue(sid);
                if (maxValue < 0 || maxValue < value) {
                    maxValue = value;
                }
                float distance = source.travelTimeTo(sv.system(sid), warpSpeed);
                if (maxDistance < 0 || maxDistance < distance) {
                    maxDistance = distance;
                }
            }
            // could happen if we only have 1 colony ship already orbiting the only remaining colonizable planet
            if (Math.abs(maxDistance) <= 0.1) {
                maxDistance = 1;
            }
            System.out.println(message+" maxDistance =" + maxDistance + " maxValue=" + maxValue);

            float maxDistance1 = maxDistance;
            float maxValue1 = maxValue;
            // planets with lowest weight are most desirable (closest/best)
            targets.sort((s1, s2) -> (int) Math.signum(autocolonizeWeight(source, s1, maxDistance1, maxValue1, warpSpeed)
                    - autocolonizeWeight(source, s2, maxDistance1, maxValue1, warpSpeed)));
            for (int si : targets) {
                double weight = autocolonizeWeight(source, si, maxDistance, maxValue, warpSpeed);
                System.out.format(message+" System %d %s travel=%.1f value=%.1f weight=%.2f%n",
                        si, sv.name(si), source.travelTimeTo(sv.system(si), warpSpeed),
                        planetValue(si), weight);
            }
        }
    }

    public double autocolonizeWeight(StarSystem source, int targetId, float maxDistance, float maxValue, int warpSpeed) {
        // let's flip value percent and sort by descending order. That's because in rare cases distancePercent could be
        // greater than 1
        float valuePercent = 1 - planetValue(targetId) / maxValue;
        float distancePercent = source.travelTimeTo(sv.system(targetId), warpSpeed) / maxDistance;
        // so distance is worth 50% of weight, value 50%
        return valuePercent * 0.5 + distancePercent * 0.5;
    }

    // taken from AIFleetCommander.setColonyFleetPlan()
    public float planetValue(int sid) {
        float value = this.sv.currentSize(sid);

        //increase value by 5 for each of our systems it is near, and
        //decrease by 2 for alien systems. This is an attempt to encourage
        //colonization of inner colonies (easier to defend) even if they
        //are not as good as outer colonies
        int[] nearbySysIds = this.sv.galaxy().system(sid).nearbySystems();
        for (int nearSysId : nearbySysIds) {
            int nearEmpId = this.sv.empId(nearSysId);
            if (nearEmpId != NULL_ID) {
                if (nearEmpId == this.id)
                    value += 5;
                else
                    value -= 2;
            }
        }
        // assume that we will terraform  the planet
        value += this.tech().terraformAdj();
        //multiply *2 for artifacts, *3 for super-artifacts
        value *= (1 + this.sv.artifactLevel(sid));
        if (this.sv.isUltraRich(sid))
            value *= 3;
        else if (this.sv.isRich(sid))
            value *= 2;
        else if (this.sv.isPoor(sid))
            value /= 2;
        else if (this.sv.isUltraPoor(sid))
            value /= 3;
        return value;
    }

    public String decode(String s, Empire listener) {
        String s1 = this.replaceTokens(s, "my");
        s1 = listener.replaceTokens(s1, "your");
        return s1;
    }
    public String decode(String s, Empire listener, Empire other) {
        String s1 = this.replaceTokens(s, "my");
        s1 = listener.replaceTokens(s1, "your");
        s1 = other.replaceTokens(s1, "other");
        return s1;
    }
    public List<Transport> transports() {
        Galaxy gal = galaxy();
        List<Transport> transports = new ArrayList<>();
        for (Transport tr: gal.transports()) {
            if (tr.empId() == id)
                transports.add(tr);
        }
        return transports;
    }
    public int transportsInTransit(StarSystem s) {
        Galaxy gal = galaxy();
        int transports = s.orbitingTransports(id);
        
        for (Transport tr: gal.transports()) {
            if ((tr.empId() == id) && (tr.destSysId() == s.id))
                transports += tr.size();
        }
        return transports;
    }
    public int enemyTransportsInTransit(StarSystem s) {
        int transports = s.orbitingTransports(id);
        
        boolean[] enemyMap = enemyMap();
        for (Ship sh: visibleShips) {
            if (sh.isTransport()) {
                if (enemyMap[sh.empId()] && (sh.destSysId() == s.id))
                if (aggressiveWith(sh.empId()) && sh.destSysId() == s.id)
                    transports += ((Transport)sh).size();
            }
        }
        return transports;
    }
    public int unfriendlyTransportsInTransit(StarSystem s) {
        int transports = s.orbitingTransports(id);
        
        for (Ship sh: visibleShips) {
            if (sh.isTransport()) {
                if (aggressiveWith(sh.empId()) && sh.destSysId() == s.id)
                    transports += ((Transport)sh).size();
            }
        }
        return transports;
    }
    public float transportTravelSpeed(IMappedObject fr, IMappedObject to) {
        if (!fr.passesThroughNebula(fr, to))
            return tech().transportTravelSpeed();
        
        float dist = fr.distanceTo(to);
        float time = fr.travelTime(fr, to, tech().transportTravelSpeed());
        return dist/time;
    }
    public void checkForRebellionSpread() {
        if (extinct)
            return;

        // for each rebelling colony, check the nearest 10 systems to see
        // if rebellion spreads to a nearby colony. Chance of spread is 5%
        // divided by the distance in l-y... i.e a colony 5 light-years away
        // has a 1% chance each turn to rebel
        //
        // this is a different and less extreme mechanic than the one described 
        // in the OSG (which was never implemented in actual MOO1 code anyway) 
        // and will scale better for really large maps. 
        
        // build a list of rebelling systems
        List<StarSystem> allSystems = allColonizedSystems();
        List<StarSystem> rebellingSystems = new ArrayList<>();
        for (StarSystem sys: allSystems) {
            if (sys.colony().inRebellion()) {
                rebellingSystems.add(sys);
            }
        }

        // for each rebelling system, build a list of nearby systems that
        // are colonized by the same empire, are not the capital and not currently
        // rebelling. Check each of them for potential rebellion
        for (StarSystem sys: rebellingSystems) {
            for (int nearbyId: sys.nearbySystems()) {
                StarSystem near = galaxy().system(nearbyId);
                if ((near.empire() == this) && !near.colony().isCapital() && !near.colony().inRebellion()) {
                    float pct = 0.05f / (sys.distanceTo(near));
                    if (random() < pct)
                        near.colony().spreadRebellion();
                }
            }
        }
    }
    public boolean inRevolt() {
        float rebellingPop = 0;
        float loyalPop = 0;
        for (StarSystem sys: allColonizedSystems()) {
            if (sys.colony().inRebellion())
                rebellingPop += sys.colony().population();
            else
                loyalPop += sys.colony().population();
        }
        return rebellingPop > loyalPop;
    }
    public void overthrowLeader() {
        if (isPlayer()) {
            //session().status().loseOverthrown();
            return;
        }

        leader = new Leader(this);
        for (EmpireView view: empireViews()) {
            if (view != null)
                view.breakAllTreaties();
        }
        
        // end all rebellions
        for (StarSystem sys: allColonizedSystems()) 
            sys.colony().clearAllRebellion();

        if (viewForEmpire(player()).embassy().contact()) {
            String leaderDesc = text("LEADER_PERSONALITY_FORMAT", leader.personality(),leader.objective());
            String message = text("GNN_OVERTHROW", leaderDesc);
            message = replaceTokens(message, "alien");
            GNNNotification.notifyRebellion(message);
        }
    }
    public boolean inEconomicRange(int empId) {
        Empire e = galaxy().empire(empId);
        float range = max(e.scoutRange(), scoutRange());
        for (StarSystem sys: e.allColonizedSystems()) {
            if (sv.distance(sys.id) <= range)
                return true;
        }
        return false;
    }
    public boolean inShipRange(int empId) {
        Empire e = galaxy().empire(empId);
        for (StarSystem sys: e.allColonizedSystems()) {
            if (sv.distance(sys.id) <= shipRange())
                return true;
        }
        return false;
    }
    // only used as a test method to accelerate testing
    // of the galactic council
    public void makeFullContact() {
        for (EmpireView v : empireViews()) {
            if ((v!= null) && !v.embassy().contact())
                v.embassy().makeFirstContact();
        }
    }
    public float orionCouncilBonus() {
        Galaxy gal = galaxy();
        for (int i=0; i<sv.count(); i++) {
            if ((sv.empire(i) == this) && gal.system(i).planet().isOrionArtifact())
                return .2f;
        }
        return 0;
    }
    public void addVisibleShip(Ship sh) {
        if (sh.visibleTo(id) && !visibleShips.contains(sh))
            visibleShips.add(sh);
    }
    public void addVisibleShips(List<? extends Ship> ships) {
        if (ships != null) {
            for (Ship sh: ships)
                addVisibleShip(sh);
        }
    }
    public List<ShipFleet> enemyFleets() {
        List<ShipFleet> list = new ArrayList<>();
        for (Ship sh: visibleShips()) {
            if (sh instanceof ShipFleet) {
                ShipFleet fl = (ShipFleet) sh;
                if ((fl.empId() != id) && !fl.isEmpty())
                    list.add((ShipFleet) sh);
            }
        }
        return list;
    }
    public void startGame() {
        refreshViews();
        setVisibleShips();
        StarSystem home = galaxy().system(homeSysId);
        governorAI().setInitialAllocations(home.colony());
    }
    public void refreshViews() {
        log(this + ": refresh views");
        if (recalcDistances) {
            NoticeMessage.setSubstatus(text("TURN_RECALC_DISTANCES"));
            sv.calculateSystemDistances();
            recalcDistances = false;
        }

        Galaxy gal = galaxy();
        for (int i=0;i<sv.count();i++) {
            StarSystem sys = gal.system(i);
            if (sys.empire() == this)
                sv.refreshFullScan(i);
            else if ((sys.orbitingFleetForEmpire(this) != null)
            && !sys.orbitingShipsInConflict())
                sv.refreshFullScan(i);
            else if (colonyCanScan(sys))
                sv.refreshLongRangePlanetScan(i);
            else if (fleetCanScan(sys))
                sv.refreshLongRangeShipScan(i);
            else if (sv.isScouted(i)) // don't keep stale fleet info
                sv.view(i).clearFleetInfo();
        }

        for (EmpireView v : empireViews()) {
            if (v!= null)
                v.refresh();
        }
        // redetermine border/support/inner status for colonies
        for (int n=0;n<sv.count();n++)
            sv.resetSystemData(n);

    }
    public void setVisibleShips(int sysId) {
        addVisibleShips(sv.orbitingFleets(sysId));
        addVisibleShips(sv.exitingFleets(sysId));
    }
    public void setVisibleShips() {
        Galaxy gal = galaxy();
        visibleShips.clear();

        List<ShipFleet> myShips = galaxy().ships.allFleets(id);
        List<StarSystem> mySystems = this.allColonizedSystems();

        // get transports in transit
        for (Transport tr : gal.transports()) {
            if (canSeeShips(tr.empId())
            || (tr.visibleTo(id) && canScanTo(tr, mySystems, myShips) ))
                addVisibleShip(tr);
        }

        // get fleets in transit
        for (ShipFleet sh : gal.ships.allFleets()) {
            if (canSeeShips(sh.empId())
            || (sh.visibleTo(id) && canScanTo(sh, mySystems, myShips) ))
                addVisibleShip(sh);
        }

        // inform our spies!
        for (Ship fl : visibleShips) {
            if (fl instanceof ShipFleet)
                detectFleet((ShipFleet)fl);
        }
    }
    public boolean canScanTo(IMappedObject loc) {
        return planetsCanScanTo(loc) || shipsCanScanTo(loc);
    }
    public boolean planetsCanScanTo(IMappedObject loc) {
        if (planetScanningRange() == 0)
            return false;

        Galaxy gal = galaxy();
        for (int i=0; i<sv.count(); i++) {
            if ((sv.empire(i) == this) && (gal.system(i).distanceTo(loc) <= planetScanningRange()))
                return true;
        }
        return false;
    }
    public boolean canScanTo(IMappedObject loc, List<StarSystem> systems, List<ShipFleet> ships) {
        float planetRange = planetScanningRange();
        for (StarSystem sys: systems) {
            if (sys.distanceTo(loc) <= planetRange)
                return true;
        }

        float shipRange = shipScanningRange();
        for (Ship sh: ships) {
            if (sh.distanceTo(loc) <= shipRange)
                return true;
        }
        return false;
    }
    public boolean shipsCanScanTo(IMappedObject loc) {
        List<ShipFleet> fleets = galaxy().ships.allFleets(id);
        for (ShipFleet fl : fleets) {
            if (fl.distanceTo(loc) <= shipScanningRange)
                return true;
        }
        return false;
    }
    public float estimatedShipFirepower(Empire emp, int shipSize, int shieldLevel) {
        return 0;
    }
    public boolean canScoutTo(Location xyz) {
        Galaxy gal = galaxy();
        for (int i=0; i<gal.numStarSystems(); i++) {
           StarSystem s = gal.system(i);
            if ((s.empire() == this) && (s.distanceTo(xyz) <= tech.scoutRange())  )
                return true;
        }
        return false;
    }
    public float distanceToSystem(StarSystem sys, List<StarSystem> froms) {
        float distance = Float.MAX_VALUE;
        for (StarSystem from: froms)
            distance = min(from.distanceTo(sys), distance);

        return distance;
    }

    public float distanceTo(IMappedObject xyz) {
        float distance = Float.MAX_VALUE;

        Galaxy gal = galaxy();
        List<Empire> allies = this.allies();
        if (allies.isEmpty()) {
            for (int i=0; i<gal.numStarSystems(); i++) {
                StarSystem s = gal.system(i);
                if (s.empire() == this)
                    distance = min(s.distanceTo(xyz), distance);
                if (distance == 0)
                    return distance;
            }
        }
        else {
            for (int i=0; i<gal.numStarSystems(); i++) {
                StarSystem s = gal.system(i);
                if (alliedWith(s.empire().id))
                    distance = min(s.distanceTo(xyz), distance);
                if (distance == 0)
                    return distance;
            }
        }
        return distance;
    }
    public int rangeTo(StarSystem sys) {
        return (int) Math.ceil(sv.distance(sys.id));
    }
    public StarSystem mostPopulousSystemForCiv(Empire c) {
        StarSystem bestSystem, biggestSystem;

        bestSystem = null;
        biggestSystem = null;
        float maxPop1 = 0;
        float maxPop2 = 0;

        for (StarSystem sys: systemsForCiv(c)) {
            float sysPop = sv.population(sys.id);
            Colony col = sv.colony(sys.id);
            if (sysPop > maxPop1) {
                maxPop1 = sysPop;
                biggestSystem = sys;
            }
            if (col != null) {
                if ((sysPop > maxPop2) && (! col.inRebellion()) ){
                    maxPop2 = sysPop;
                    bestSystem = sys;
                }
            }
        }

        if (bestSystem != null)
            return bestSystem;
        else
            return biggestSystem;
    }
    public boolean isAnyColonyConstructing(ShipDesign d) {
        for (int n=0;n<sv.count();n++) {
            if ((sv.empire(n) == this) && (sv.colony(n).shipyard().queuedBCForDesign(d) > 0))
                return true;
        }
        return false;
    }
    public List<StarSystem> coloniesConstructing(ShipDesign d) {
        Galaxy gal = galaxy();
        List<StarSystem> colonies = new ArrayList<>();

        for (int i=0;i<sv.count();i++) {
            if ((sv.empire(i) == this) && (sv.colony(i).shipyard().queuedBCForDesign(d) > 0))
                colonies.add(gal.system(i));
        }
        return colonies;
    }
    public int shipDesignCount(int designId) {
        return galaxy().ships.shipDesignCount(id, designId);
    }
    public void swapShipConstruction(ShipDesign oldDesign) {
        swapShipConstruction(oldDesign, null);
    }
    public void swapShipConstruction(ShipDesign oldDesign, ShipDesign newDesign) {
        for (StarSystem sys: allColonizedSystems()) {
            ColonyShipyard shipyard = sys.colony().shipyard();
            if (shipyard.design() == oldDesign) {
                if ((newDesign != null) && newDesign.active())
                    shipyard.switchToDesign(newDesign);
                else
                    shipyard.goToNextDesign();
            }
        }
    }
    public DiplomaticTreaty treatyWithEmpire(int empId) {
        if ((empId < 0) || (empId >= empireViews.length) || (empId == id))
            return null;
        
        return empireViews[empId].embassy().treaty();
    }
    public EmpireView viewForEmpire(Empire emp) {
        if ((emp != null) && (emp != this))
            return empireViews[emp.id];
        return null;
    }
    public EmpireView viewForEmpire(int empId) {
        if ((empId < 0) || (empId >= empireViews.length) || (empId == id))
            return null;
        
        return empireViews[empId];
    }
    public boolean hasContact(Empire c) {
        EmpireView v = viewForEmpire(c);
        return (v != null) && (v.embassy().contact() && !v.empire().extinct);
    }
    public void makeContact(Empire c) {
        EmpireView v = viewForEmpire(c);
        if (v != null)
            v.setContact();
    }
    public boolean atWar() {
        for (EmpireView v: empireViews()) {
            if ((v != null) && !v.empire().extinct() && v.embassy().anyWar())
                return true;
        }
        return false;
    }
    public float powerLevel(Empire e) {
        return militaryPowerLevel(e) + industrialPowerLevel(e);
    }
    public float militaryPowerLevel(Empire e) {
        // TechTree t0 = e == this ? tech() : viewForEmpire(e).spies().tech();
        float fleet = totalFleetSize(e);
        float techLvl = (float)Math.pow(1 / miniFastRate, tech().avgTechLevel());
        return fleet*techLvl;
    }
    public float militaryPowerLevel() {
        float fleet = totalArmedFleetSize();
        float techLvl = (float)Math.pow(1 / miniFastRate, tech().avgTechLevel());
        return fleet*techLvl;
    }
    public float industrialPowerLevel(Empire e) {
        // TechTree t0 = e == this ? tech() : viewForEmpire(e).spies().tech();
        float prod = totalPlanetaryProduction(e);
        float techLvl = (float)Math.pow(1 / miniFastRate, tech().avgTechLevel());
        return prod*techLvl;
    }
    // modnar: add dynamic difficulty option, change AI colony production
    // create unscaled production power level, to avoid infinite recursion
    public float nonDynaIndPowerLevel() {
        float prod = nonDynaTotalProd();
        float techLvl = (float)Math.pow(1 / miniFastRate, tech().avgTechLevel());
        return prod*techLvl;
    }
    public void clearDataForExtinctEmpire(int empId) {
        EmpireView view = viewForEmpire(empId);
        view.spies().shutdownSpyNetworks();
        
        // clear and re-add should be faster than removing ships
        // since each remove would recopy the list
        List<Ship> oldShips = new ArrayList<>(visibleShips());
        visibleShips.clear();
        for (Ship sh: oldShips) {
            if (sh.empId() != empId)
               visibleShips.add(sh);
        }
        
        // clear out system view data. Inefficient on large maps
        int n = sv.count();
        for (int i=0;i<n;i++) {
            if (sv.empId(i) == empId)
                sv.view(i).goExtinct();
        }
    }
    public boolean hasAnyContact() {  return !contactedEmpires().isEmpty(); }

    public boolean inRangeOfAnyEmpire() {
        if (inRange < 0) {
            inRange = 0;
            for (EmpireView v: empireViews()) {
                if ((v!= null) && v.embassy().contact() && v.inEconomicRange())
                    inRange = 1;
            }
        }
        return inRange == 1;        
    }
    public List<Empire> contactedEmpires() {
        List<Empire> r = new ArrayList<>();

        for (EmpireView v: empireViews()) {
            if ((v!= null) && v.embassy().contact() && !v.empire().extinct)
                r.add(v.empire());
        }
        return r;
    }
    public List<EmpireView> contactedCivsThatKnow(Tech t) {
        List<EmpireView> r = new ArrayList<>();

        for (EmpireView cv : empireViews()) {
            if ((cv!= null) && cv.embassy().contact() && cv.spies().tech().knows(t))
                r.add(cv);
        }
        return r;
    }
    public DiplomaticTreaty treaty(Empire e) {
        EmpireView v = viewForEmpire(e.id);
        if (v == null)
            return null;
        else
            return v.embassy().treaty();
    }
    public int numColonies() {
        int count = 0;
        for (int n=0; n<sv.count(); n++) {
            if (sv.empire(n) == this)
                count++;
        }
        return count;
    }
    public float requestedTotalSpyCostPct() {
        float sum = 0;
        for (EmpireView ev : empireViews()) {
            if (ev != null)
                sum += ev.spies().allocationCostPct();
        }
        return sum;
    }
    public float totalSpyCostPct() {
        float requested = requestedTotalSpyCostPct();
        return min(0.5f, requested);
    }
    public float spySpendingModifier() {
        float requested = requestedTotalSpyCostPct();
        if (requested > 0.5f)
            return 0.5f / requested;
        return 1.0f;
    }
    public int totalActiveSpies() {
        int sum = 0;
        for (EmpireView ev : empireViews()) {
            if (ev != null)
                sum += ev.spies().numActiveSpies();
        }
        return sum;
    }
    public int internalSecurity()            { return securityAllocation; }
    public void internalSecurity(int i)      {
        securityAllocation = bounds(0,i,MAX_SECURITY_TICKS);
        flagColoniesToRecalcSpending();
    }
    public float internalSecurityPct()       { return (float) securityAllocation/MAX_SECURITY_TICKS; }
    public void increaseInternalSecurity()   { internalSecurity(securityAllocation+1); }
    public void decreaseInternalSecurity()   { internalSecurity(securityAllocation-1); }
    public void securityAllocation(float d) {
        // d assumed to be between 0 & 1, representing pct of slider clicked
        float incr = 1.0f/(MAX_SECURITY_TICKS+1);
        float sum = 0;
        for (int i=0;i<MAX_SECURITY_TICKS+1;i++) {
            sum += incr;
            if (d <= sum) {
                internalSecurity(i);
                return;
            }
        }
        internalSecurity(MAX_SECURITY_TICKS);
    }
    public float requestedSecurityCostPct() {
        return MAX_SECURITY_PCT*securityAllocation/MAX_SECURITY_TICKS/2;
    }
    public float totalInternalSecurityPct() {
        return inRangeOfAnyEmpire() ? MAX_SECURITY_PCT*securityAllocation/MAX_SECURITY_TICKS : 0;
    }
    public float internalSecurityCostPct() {
        return (totalInternalSecurityPct()/2);
    }
    public float totalSecurityCostPct() {
        return totalSpyCostPct() + internalSecurityCostPct();
    }
    public float baseSpyCost() {
        return (25 + (tech.computer().techLevel()*2)) * dataRace().spyCostMod();
    }
    public float troopKillRatio(StarSystem s) {
		// modnar: this old estimate gives completely wrong results for ground combat
        //float killRatio = (50 + tech.troopCombatAdj(false)) / (50 + sv.defenderCombatAdj(s.id));
		
		// modnar: correct ground combat ratio estimates
		float killRatio = 1.0f;
		if (sv.defenderCombatAdj(s.id) >= tech.troopCombatAdj(false)) {
			float defAdv = sv.defenderCombatAdj(s.id) - tech.troopCombatAdj(false);
			// killRatio = attackerCasualties / defenderCasualties
			killRatio = (float) ((Math.pow(100,2) - Math.pow(100-defAdv,2)/2) / (Math.pow(100-defAdv,2)/2));
		}
		else {
			float atkAdv = tech.troopCombatAdj(false) - sv.defenderCombatAdj(s.id);
			// killRatio = attackerCasualties / defenderCasualties
			killRatio = (float) ((Math.pow(100-atkAdv,2)/2) / (Math.pow(100,2) - Math.pow(100-atkAdv,2)/2));
		}
        return killRatio;
    }
    public List<EmpireView> contacts() {
        List<EmpireView> r = new ArrayList<>();
        for (EmpireView v : empireViews()) {
            if ((v!= null) && !v.empire().extinct && v.embassy().contact())
                r.add(v);
        }
        return r;
    }
    public List<EmpireView> commonContacts(Empire emp2) {
        List<EmpireView> r = new ArrayList<>();
        if (emp2.extinct)
            return r;
        for (EmpireView v : empireViews()) {
            if ((v!= null) && !v.empire().extinct && v.embassy().contact()) {
                if (v.empire() == emp2)
                    r.add(v);
                else {
                    EmpireView v2 = v.empire().viewForEmpire(emp2);
                    if (v2.embassy().contact())
                        r.add(v);
                }
            }
        }
        return r;
    }
    public int numEnemies() {
        int n = 0;
        for (EmpireView v : empireViews()) {
            if ((v!= null) && !v.empire().extinct
            && (v.embassy().anyWar() || v.embassy().onWarFooting()))
                n++;
        }
        return n;
    }
    public List<Empire> warEnemies() {
        List<Empire> r = new ArrayList<>();
        for (EmpireView v : empireViews()) {
            if ((v!= null) && !v.empire().extinct
            && v.embassy().anyWar())
                r.add(v.empire());
        }
        return r;
    }
    public List<Empire> enemies() {
        List<Empire> r = new ArrayList<>();
        for (EmpireView v : empireViews()) {
            if ((v!= null) && !v.empire().extinct
            && (v.embassy().anyWar() || v.embassy().onWarFooting()))
                r.add(v.empire());
        }
        return r;
    }
    public boolean[] enemyMap() {
        // returns a boolean array where the index is an empire id and 
        // the array value is true if that empire is an "enemy"
        EmpireView[] empViews = empireViews();
        boolean[] map = new boolean[empViews.length];
        for (int i=0;i<map.length;i++) {
            EmpireView v = empViews[i];
            map[i] = (v != null) && !v.empire().extinct
                        && (v.embassy().anyWar() || v.embassy().onWarFooting());
        }
        return map;
    }
    public List<EmpireView> enemyViews() {
        List<EmpireView> r = new ArrayList<>();
        for (EmpireView v : empireViews()) {
            if ((v!= null) && !v.empire().extinct
            && (v.embassy().anyWar() || v.embassy().onWarFooting()))
                r.add(v);
        }
        return r;
    }
    public List<EmpireView> hostiles() {
        List<EmpireView> r = new ArrayList<>();
        for (EmpireView v : empireViews()) {
            if ((v!= null) && !v.empire().extinct && !v.embassy().isFriend())
                r.add(v);
        }
        return r;
    }
    public boolean hasNonEnemiesKnownBy(Empire e) {
        return !nonEnemiesKnownBy(e).isEmpty();
    }
    public List<Empire> nonEnemiesKnownBy(Empire empOther) {
        List<Empire> enemies = new ArrayList<>();
        // return any empires we are both in economic range of 
        // and this empire not already at war with
        List<Empire> contacts = contactedEmpires();
        contacts.remove(empOther);
        for (Empire emp: contacts) {
            if (!atWarWith(emp.id) && inEconomicRange(emp.id) && empOther.inEconomicRange(emp.id) && !unityWith(emp.id))
                enemies.add(emp);
        }
        return enemies;
    }
    public List<Empire> allies() {
        List<Empire> r = new ArrayList<>();
        for (EmpireView v : empireViews()) {
            if ((v!= null) && !v.empire().extinct && v.embassy().isAlly())
                r.add(v.empire());
        }
        return r;
    }
    public boolean hasAlliesKnownBy(Empire emp1) {
        for (EmpireView v : empireViews()) {
            if ((v!= null) && !v.empire().extinct && (v.empire() != emp1) && v.embassy().isAlly() && emp1.hasContact(v.empire()))
                return true;
        }
        return false;
    }
    public List<Empire> alliesKnownBy(Empire emp1) {
        List<Empire> allies = new ArrayList<>();
        for (EmpireView v : empireViews()) {
            if ((v!= null) && !v.empire().extinct && (v.empire() != emp1) && v.embassy().isAlly() && emp1.hasContact(v.empire()))
                allies.add(v.empire());
        }
        return allies;
    }
    public boolean friendlyWith(int empId) {
        if (empId == id) return true;
        if (empId == Empire.NULL_ID) return false;

        EmpireView v = viewForEmpire(empId);
        return v == null ? false : v.embassy().isFriend();
    }
    public boolean pactWith(int empId) {
        if (empId == id) return true;
        if (empId == Empire.NULL_ID) return false;

        EmpireView v = viewForEmpire(empId);
        return v == null ? false : v.embassy().pact();
    }
    public boolean alliedWith(int empId) {
        if (empId == id) return true;
        if (empId == Empire.NULL_ID) return false;

        EmpireView v = viewForEmpire(empId);
        return v == null ? false : v.embassy().alliance() || v.embassy().unity();
    }
    public boolean unityWith(int empId) {
        if (empId == id) return true;
        if (empId == Empire.NULL_ID) return false;

        EmpireView v = viewForEmpire(empId);
        return v == null ? false : v.embassy().unity();
    }
    public boolean tradingWith(Empire c) {
        if (c == this) return true;
        if (c == null) return false;
        if (c.extinct) return false;

        EmpireView v = viewForEmpire(c);
        return v == null ? false : v.trade().active();
    }
    public boolean aggressiveWith(int empId) {
        if (empId == id) return false;
        if (empId == Empire.NULL_ID) return false;

        EmpireView v = viewForEmpire(empId);
        if (v == null)
            return false;
        
        if (v.embassy().peaceTreatyInEffect())
            return false;
        return v.embassy().canAttackWithoutPenalty();
    }
    public boolean aggressiveWith(Empire c, StarSystem s) {
        if (c == this) return false;
        if (c == null) return false;
        if (c.extinct) return true;

        EmpireView v = viewForEmpire(c);
        if (v == null)
            return true;
        return v.embassy().canAttackWithoutPenalty(s);
    }
    public boolean atWarWith(int empId) {
        if (empId == id) return false;
        if (empId == Empire.NULL_ID) return false;

        EmpireView v = viewForEmpire(empId);
        if (v == null)
            return false;
        return v.embassy().anyWar();
    }
    public boolean hasTradeWith(Empire c) {
        if (c == this) return false;
        if (c == null) return false;
        if (c.extinct) return false;

        EmpireView v = viewForEmpire(c);
        if (v == null)
            return false;
        return v.trade().active();
    }
    public int contactAge(Empire c) {
        if (c == this) return 0;
        if (c == null) return 0;
        if (c.extinct) return 0;

        EmpireView v = viewForEmpire(c);
        if (v == null)
            return 0;
        return v.embassy().contactAge();
    }
    public void shareSystemInfoWithAlly(Empire c) {
        sv.shareAllyData(c.sv);
    }
    public void shareSystemInfoWithAllies(SystemView v) {
        for (Empire ally: allies()) {
            ally.sv.refreshAllySharingScan(v.sysId);
        }
    }
    /*
    public List<StarSystem> systemsNeedingTransports(int minTransport) {
        List<StarSystem> systems = new ArrayList<>();
        for (StarSystem sys: colonizedSystems) {
            if (sys.colony().inRebellion() || (sv.popNeeded(sys.id) >= minTransport) )
                systems.add(sys);
        }
        return systems;
    }
    */
    public List<StarSystem> systemsInShipRange(Empire c) {
        // returns list of systems in ship range
        // if c provided, restricts list to that owner
        Galaxy gal = galaxy();
        List<StarSystem> systems = new ArrayList<>();
        for (int n=0;n<sv.count();n++) {
            StarSystem sys = gal.system(n);
            if (sv.inShipRange(sys.id)) {
                if ((c == null) || (sv.empire(sys.id) == c))
                    systems.add(sys);
            }
        }
        return systems;
    }
    /*
    public List<StarSystem> systemsSparingTransports(int minTransport) {
        List<StarSystem> systems = new ArrayList<>();
        for (StarSystem sys: colonizedSystems) {
            if (!sys.colony().inRebellion() && sv.maxPopToGive(sys.id) >= minTransport )
                systems.add(sys);
        }
        return systems;
    }
    */
    public List<StarSystem> allColonizedSystems() {
        return colonizedSystems;
    }
    public List<StarSystem> allySystems() {
        Galaxy gal = galaxy();
        List<StarSystem> systems = new ArrayList<>();
        for (int i=0;i<sv.count();i++) {
            if (alliedWith(sv.empId(i)))
                systems.add(gal.system(i));
        }
            return systems;
    }
    public StarSystem colonyNearestToSystem(StarSystem sys) {
        List<StarSystem> colonies = new ArrayList<>(allColonizedSystems());
        colonies.remove(sys);
        if (colonies.isEmpty())
            return null;

        StarSystem.TARGET_SYSTEM = sys;
        Collections.sort(colonies, StarSystem.DISTANCE_TO_TARGET_SYSTEM);
        return colonies.get(0);
    }
    public int alliedColonyNearestToSystem(StarSystem sys, float speed) {
        List<StarSystem> colonies = allySystems();
        colonies.remove(sys);

        // build list of allied systems closest to sys, in travel turns (not distance)
        List<StarSystem> closestSystems = new ArrayList<>();
        int minTurns = Integer.MAX_VALUE;
        for (StarSystem colony: colonies) {
            int turns = (int) Math.ceil(colony.travelTimeTo(sys, speed));
            if (turns < minTurns) {
                closestSystems.clear();
                closestSystems.add(colony);
                minTurns = turns;
            }
            else if (turns == minTurns)
                closestSystems.add(colony);
        }
        if (closestSystems.isEmpty())
            return StarSystem.NULL_ID;
        if (closestSystems.size() == 1)
            return closestSystems.get(0).id;
       
        // if there is more than one system within the minimum travel turns, 
        // choose the one closest, by distance
        StarSystem.TARGET_SYSTEM = sys;
        Collections.sort(closestSystems, StarSystem.DISTANCE_TO_TARGET_SYSTEM);
        return closestSystems.get(0).id;
    }
    public int optimalStagingPoint(StarSystem target, float speed) {
        List<StarSystem> colonies = allySystems();
        colonies.remove(target);

        // build a list of allied systems from sys that take the fewer travel turns
        // from that list, return the one with the greatest range
        // the idea is to try and keep the staging point outside of enemy sensor range
        // without hurting the travel time to the target
        List<StarSystem> closestSystems = new ArrayList<>();
        int minTurns = Integer.MAX_VALUE;
        for (StarSystem stagingPoint: colonies) {
            // modnar: don't allow colonies with enemy fleet in orbit be considered for stagingPoint
            if (!stagingPoint.enemyShipsInOrbit(stagingPoint.empire())) {
                int turns = (int) Math.ceil(stagingPoint.travelTimeTo(target, speed));
                if (turns < minTurns) {
                    closestSystems.clear();
                    closestSystems.add(stagingPoint);
                    minTurns = turns;
                }
                else if (turns == minTurns)
                    closestSystems.add(stagingPoint);
            }
        }
        if (closestSystems.isEmpty())
            return StarSystem.NULL_ID;
        
        if (closestSystems.size() == 1)
            return closestSystems.get(0).id;
        
        Empire targetEmpire = target.empire();
        if (targetEmpire == null) 
            return closestSystems.get(0).id;
        
        float maxDistance = Float.MIN_VALUE;
        StarSystem bestStagingPoint = null;
        for (StarSystem stage: closestSystems) {
            float dist = targetEmpire.sv.distance(stage.id);
            if (dist > maxDistance) {
                maxDistance = dist;
                bestStagingPoint = stage;
            }
        }
        return bestStagingPoint == null ? StarSystem.NULL_ID : bestStagingPoint.id;
    }
    public List<StarSystem> systemsForCiv(Empire emp) {
        Galaxy gal = galaxy();
        List<StarSystem> systems = new ArrayList<>();
        for (int i=0;i<sv.count();i++) {
            if (sv.empire(i) == emp) 
                systems.add(gal.system(i));
        }
        return systems;
    }
    public int numSystemsForCiv(Empire emp) {
        int num = 0;
        for (int n=0;n<sv.count();n++) {
            if (sv.empire(n) == emp)
                num++;
        }
        return num;
    }
    public int numColonizedSystems() {
        return colonizedSystems.size();
    }
    public List<ShipFleet> fleetsForEmpire(Empire c) {
        List<ShipFleet> fleets2 = new ArrayList<>();
        for (Ship sh: visibleShips()) {
            if ((sh.empId() == c.id) && sh instanceof ShipFleet)
                fleets2.add((ShipFleet) sh);
        }
        return fleets2;
    }
    public boolean anyUnexploredSystems() {
        for (int n=0;n<sv.count(); n++) {
            if (!sv.isScouted(n))
                return true;
        }
        return false;
    }
    public List<StarSystem> unexploredSystems() {
        Galaxy gal = galaxy();
        List<StarSystem> systems = new ArrayList<>();
        for (int n=0;n<sv.count(); n++) {
            if (!sv.isScouted(n))
                systems.add(gal.system(n));
        }
        return systems;
    }
    public List<StarSystem> uncolonizedPlanetsInShipRange(int newType) {
        Galaxy gal = galaxy();
        List<StarSystem> systems = new ArrayList<>();
        for (int i=0;i<sv.count();i++) {
            StarSystem sys = gal.system(i);
            if (sv.isScouted(i) && sv.inShipRange(i) && canColonize(sys.planet().type(), newType) && !sv.isColonized(i))
                systems.add(sys);
        }
        return systems;
    }
    public List<StarSystem> uncolonizedPlanetsInExtendedShipRange(int newType) {
        Galaxy gal = galaxy();
        List<StarSystem> systems = new ArrayList<>();
        for (int i=0;i<sv.count();i++) {
            StarSystem sys = gal.system(i);
            if (sv.isScouted(i) && sv.inScoutRange(i) && canColonize(sys.planet().type(), newType) && !sv.isColonized(i))
                systems.add(sys);
        }
        return systems;
    }
    public List<StarSystem> uncolonizedPlanetsInRange(float range) {
        Galaxy gal = galaxy();
        List<StarSystem> systems = new ArrayList<>();
        for (int i=0;i<sv.count();i++) {
            StarSystem sys = gal.system(i);
            if (sv.isScouted(i) && (sv.distance(i) <= range) && canColonize(sys.planet().type()) && !sv.isColonized(i))
                systems.add(sys);
        }
        return systems;
    }
    public PlanetType minUncolonizedPlanetTypeInShipRange(boolean checkHabitable) {
        // of all uncolonized planets in range that we can colonize
        // find the most hostile type... this guides the colony ship design
        PlanetType minType = PlanetType.keyed(PlanetType.TERRAN);
        for (int n=0;n<sv.count();n++) {
            if (sv.isScouted(n) && sv.inShipRange(n) && !sv.isColonized(n)) {
                PlanetType pType = sv.planetType(n);
                if (!checkHabitable || canColonize(pType)) {
                    if (pType.hostility() > minType.hostility())
                        minType = pType;
                }
            }
        }
        return minType;
    }
    public boolean knowsAllActiveEmpires() {
        for (Empire e: galaxy().activeEmpires()) {
            if (this != e) {
                if (!knowsOf(e))
                    return false;
            }
        }
        return true;
    }
    public boolean knowsOf(Empire e) {
        if (e == null)
            return false;
        if (e == this)
            return true;
        if (hasContacted(e.id))
            return true;
        for (Empire emp : contactedEmpires()) {
            EmpireView v = this.viewForEmpire(emp.id);
            if (v.inEconomicRange() && emp.hasContacted(e.id)) 
                return true;
        }
        return false;
    }
    public boolean hasContacted(int empId) {
        EmpireView ev = this.viewForEmpire(empId);
        return (ev != null) && ev.embassy().contact();
    }
    public boolean knowsOf(int empId) {
        if (empId == Empire.NULL_ID)
            return false;
        if (empId == id)
            return true;
        if (hasContacted(empId))
            return true;
        for (Empire emp : contactedEmpires()) {
            if (emp.hasContacted(empId))
                return true;
        }
        return false;
    }
    public List<ShipFleet> ownFleetsTargetingSystem(StarSystem target) {
        List<ShipFleet> fleets1 = new ArrayList<>();
        for (ShipFleet fl : galaxy().ships.inTransitFleets()) {
            if (fl.empId() == this.id) {
                if (fl.inTransit() && (fl.destSysId() == target.id))
                    fleets1.add(fl);
                else if (!fl.inTransit() && (fl.sysId() == target.id))
                    fleets1.add(fl);
            }
        }
        return fleets1;
    }
    public void scrapExcessBases(StarSystem sys, int max) {
        if (sv.empire(sys.id) == this) {
            Colony col = sys.colony();
            if (col.defense().bases() > max) {
                log("civScrapBases  bases:", str(col.defense().bases()), " max: ", str(max), " cost: ", str(tech.newMissileBaseCost()));
                totalReserve += ((col.defense().bases() - max) * tech.newMissileBaseCost() / 4);
                col.defense().bases(max);
                //ai().setColonyAllocations(col);
                sv.refreshFullScan(sys.id);
            }
        }
    }
    public float bestEnemyShieldLevel() {
        float best = 0;
        for (EmpireView v : empireViews()) {
            if (v != null)
                best = max(best, v.spies().tech().maxDeflectorShieldLevel());
        }
        return best;
    }
    public float bestEnemyPlanetaryShieldLevel() {
        float best = 0;
        for (EmpireView v : empireViews()) {
            if (v != null) {
                float shieldLevel = v.spies().tech().maxDeflectorShieldLevel() + v.spies().tech().maxPlanetaryShieldLevel();
                best = max(best, shieldLevel);
            }
        }
        return best;
    }
    public void addToTreasury(float amt) {
        totalReserve += amt;
    }
    public void addReserve(float amt) {
        addToTreasury(amt/2);
    }
    public void stealTech(String id) {
        tech().learnTech(id);
        log("Tech: "+tech(id).name(), " stolen");
    }
    public void learnTech(String techId) {
        boolean newTech = tech().learnTech(techId);
        if (newTech && isPlayerControlled()) {
            log("Tech: ", techId, " researched");
            DiscoverTechNotification.create(techId);
        }
        // share techs with New Republic allies
        for (EmpireView v: empireViews) {
            if ((v != null) && v.embassy().unity())
                v.empire().tech().acquireTechThroughTrade(techId, id);
        }
    }
    /*
    public void acquireTechThroughTrade(String techId, int empId) {
        tech().acquireTechThroughTrade(techId, empId);
    }
*/
    public void plunderTech(Tech t, StarSystem s, Empire emp) {
        boolean newTech = tech().learnTech(t.id);
        if (newTech && isPlayerControlled()) {
            log("Tech: ", t.name(), " plundered from: ", s.name());
            PlunderTechNotification.create(t.id, s.id, emp.id);
        }
    }
    public void plunderShipTech(Tech t, int empId) {
        boolean newTech = tech().learnTech(t.id);
        if (newTech && isPlayerControlled()) {
            log("Ship tech: ", t.name(), " plundered ");
            PlunderShipTechNotification.create(t.id, empId);
        }
    }
    public void plunderAncientTech(StarSystem s) {
        boolean isOrion = s.planet().isOrionArtifact();
        int numTechs = s.planet().bonusTechs();
        int levelDiff = isOrion ? 25: 10;
        int minLevel = isOrion ? 20: 1;
        s.planet().plunderBonusTech();
        
        for (int i=0;i<numTechs;i++) {
            Tech t = tech().randomUnknownTech(minLevel, levelDiff, isPlayerControlled());
            if (t == null) // if none found, then break out of loop
                break;
            boolean newTech = tech().learnTech(t.id);
            if (newTech && isPlayerControlled()) {
                log("Tech: ", t.name(), " discovered on: ", s.name());
                PlunderTechNotification.create(t.id, s.id, -1);
            }
        }
    }
    // BR: Custom Races
    public String  raceKey()				   { return raceKey; }
    public String  dataRaceKey()			   { return dataRaceKey; }
    public boolean isCustomRace()			   { return dataRace().isCustomRace(); }
    public boolean isCustomPlayer()			   { return isPlayer() && isCustomRace(); }
    public boolean isRandomized()			   { return dataRace().isRandomized(); }
    public DynOptions raceOptions()			   { return raceOptions; }
    public void raceOptions(DynOptions opts)   { raceOptions = opts; }
    public String  description4()			   { return dataRace().description4(); }
    public DynOptions dynamicOptions()		   { return dynamicOptions; }
    // Modnar added features
    public float bCBonus()                     { return dataRace().bCBonus(); }
    public float hPFactor()                    { return dataRace().hPFactor();  }
    public float maintenanceFactor()           { return dataRace().maintenanceFactor(); }
    public float shipSpaceFactor()             { return dataRace().shipSpaceFactor(); }
    public String planetRessource()            { return dataRace().planetRessource(); }
    public String planetEnvironment()          { return dataRace().planetEnvironment(); }
    // \BR:
    public int preferredShipSize()             { return dataRace().preferredShipSize(); }
    public int diplomacyBonus()                { return dataRace().diplomacyBonus(); }
    public int robotControlsAdj()              { return dataRace().robotControlsAdj(); }
    public float councilBonus()                { return dataRace().councilBonus(); }
    public float baseRelations(Empire e)       { return dataRace().baseRelations(e.dataRace()); }
    public float tradePctBonus()               { return dataRace().tradePctBonus(); }
    public float researchBonusPct()            { return dataRace().researchBonusPct(); }
    public float techDiscoveryPct()            { return dataRace().techDiscoveryPct(); }
    public float growthRateMod()               { return dataRace().growthRateMod(); }
    public float workerProductivityMod()       { return dataRace().workerProductivityMod(); }
    public float internalSecurityAdj()         { return dataRace().internalSecurityAdj(); }
    public float spyInfiltrationAdj()          { return dataRace().spyInfiltrationAdj(); }
    public float techMod(int cat)              { return dataRace().techMod[cat]; }
    public int groundAttackBonus()             { return dataRace().groundAttackBonus(); }
    public int shipAttackBonus()               { return dataRace().shipAttackBonus(); }
    public int shipDefenseBonus()              { return dataRace().shipDefenseBonus(); }
    public int shipInitiativeBonus()           { return dataRace().shipInitiativeBonus(); }
    public boolean ignoresPlanetEnvironment()  { return dataRace().ignoresPlanetEnvironment(); }
    public boolean ignoresFactoryRefit()       { return dataRace().ignoresFactoryRefit(); }
    public boolean canResearch(Tech t)         { return t.canBeResearched(dataRace()); }
    public int maxRobotControls() {
        return tech.baseRobotControls() + robotControlsAdj();
    }
    public int baseRobotControls() {
        return TechRoboticControls.BASE_ROBOT_CONTROLS + robotControlsAdj();
    }
    public float workerProductivity() {
        float bookFormula = ((tech.planetology().techLevel() * 3) + 50) / 100;
        return bookFormula * workerProductivityMod();
    }
    public float totalIncome()                { return netTradeIncome() + totalPlanetaryIncome(); }
    public float netIncome()                  { return totalIncome() - totalShipMaintenanceCost() - totalStargateCost() - totalMissileBaseCost(); }
    public float empireTaxRevenue()           { 
        if (empireTaxOnlyDeveloped())
            return totalTaxableDevelopedPlanetaryProduction() * empireTaxPct() / 2; 
        else
            return totalTaxablePlanetaryProduction() * empireTaxPct() / 2; 
    }
    public float empireInternalSecurityCost() {
        return inRangeOfAnyEmpire() ? totalTaxablePlanetaryProduction() * internalSecurityCostPct() : 0f;
    }
    public float empireExternalSpyingCost()   { return totalTaxablePlanetaryProduction() * totalSpyCostPct(); }
    
    public boolean incrementEmpireTaxLevel()  { return empireTaxLevel(empireTaxLevel+1); }
    public boolean decrementEmpireTaxLevel()  { return empireTaxLevel(empireTaxLevel-1); }
    public float empireTaxPct()               { return (float) empireTaxLevel / 100; }
    public float maxEmpireTaxPct()            { return (float) maxEmpireTaxLevel()/100; }
    public int empireTaxLevel()               { return empireTaxLevel; }
    public boolean empireTaxOnlyDeveloped()   { return empireTaxOnlyDeveloped; }
    public void toggleEmpireTaxOnlyDeveloped(){ 
        empireTaxOnlyDeveloped = !empireTaxOnlyDeveloped;
        if (empireTaxLevel > 0)
            flagColoniesToRecalcSpending();
    }
    public int maxEmpireTaxLevel()            { return 20; }
    public boolean empireTaxLevel(int i)      {
        int prevLevel = empireTaxLevel;
        empireTaxLevel = bounds(0,i,maxEmpireTaxLevel());
        
        if (empireTaxLevel != prevLevel)
            flagColoniesToRecalcSpending();
        return empireTaxLevel != prevLevel;
    }
    //ail: needs to be public for when spending in spy-network is adjusted
    public void flagColoniesToRecalcSpending() {
        // tax rate has changed in some way... flag colonies so they
        // recalc properly
        List<StarSystem> allSystems = allColonizedSystems();
        for (StarSystem sys: allSystems)
        {
            if(sys.colony() == null)
                continue;
            sys.colony().toggleRecalcSpending();
        }
    }
    public boolean hasTrade() {
        for (EmpireView v : empireViews()) {
            if ((v != null) && (v.trade().level() > 0))
                return true;
        }
        return false;
    }
    public float netTradeIncome() {
        float trade = totalTradeIncome();
        return trade <= 0 ? trade : trade * (1-tradePiracyRate);
    }
    public float totalTradeIncome() {
        float sum = 0;
        for (EmpireView v : empireViews()) {
            if (v != null)
                sum += v.trade().profit();
        }
        return sum;
    }
    public int totalTradeTreaties() {
        int sum = 0;
        for (EmpireView v : empireViews()) {
            if (v != null)
                sum += v.trade().level();
        }
        return sum;
    }
    public float totalFleetSize(Empire emp) {
        if (this == emp)
            return totalFleetSize();

        float spyPts = 0;
        FleetView fv = viewForEmpire(emp).spies().fleetView();
        if (!fv.noReport()) {
            spyPts += (fv.small()*ShipDesign.hullPoints(ShipDesign.SMALL));
            spyPts += (fv.medium()*ShipDesign.hullPoints(ShipDesign.MEDIUM));
            spyPts += (fv.large()*ShipDesign.hullPoints(ShipDesign.LARGE));
            spyPts += (fv.huge()*ShipDesign.hullPoints(ShipDesign.HUGE));
        }

        float visiblePts = 0;
        for (Ship sh : visibleShips()) {
            if ((sh.empId() == emp.id) && sh instanceof ShipFleet) {
                ShipFleet sh1 = (ShipFleet) sh;
                visiblePts += sh1.hullPoints();
            }
        }
        return max(spyPts, visiblePts);
    }
    public Float totalFleetSize() {
        float pts = 0;
        List<ShipFleet> fleets = galaxy().ships.allFleets(id);
        for (ShipFleet fl: fleets) 
            pts += fl.hullPoints();
        return pts;
    }
    public Float totalArmedFleetSize() {
        float pts = 0;
        int[] counts = galaxy().ships.shipDesignCounts(id);
        for (int i=0;i<ShipDesignLab.MAX_DESIGNS; i++) {
            ShipDesign d = shipLab().design(i);
            if (d.active() && d.isArmed() && !d.hasColonySpecial())
                pts += (counts[i] *d.hullPoints());
        }
        return pts;
    }
    public float totalFleetCost() {
        float pts = 0;
        List<ShipFleet> fleets = galaxy().ships.allFleets(id);
        for (ShipFleet fl: fleets)
            pts += fl.bcValue();
        return pts;
    }
    public Float totalEmpirePopulation() {
        float totalPop = 0;
        List<StarSystem> systems = new ArrayList<>(allColonizedSystems());
        for (StarSystem sys: systems)
            totalPop += sys.colony().population();
        List<Transport> allTransports = transports();
        for(Transport tr: allTransports)
            totalPop += tr.size();

        return totalPop;
    }
    public Float totalPlanetaryPopulation() {
        float totalPop = 0;
        List<StarSystem> systems = new ArrayList<>(allColonizedSystems());
        for (StarSystem sys: systems)
            totalPop += sys.colony().population();
        return totalPop;
    }
    public float totalPlanetaryPopulation(Empire emp) {
        float totalPop = 0;
        if (emp == this) {
            List<StarSystem> systems = new ArrayList<>(allColonizedSystems());
            for (StarSystem sys: systems)
                totalPop += sys.colony().population();
        }
        else {
            for (int n=0; n<sv.count(); n++) {
                if ((sv.empire(n) == emp))
                    totalPop += sv.population(n);
            }
        }
        return totalPop;
    }
    public float totalPlanetaryIncome() {
        float totalProductionBC = 0;
        List<StarSystem> systems = new ArrayList<>(allColonizedSystems());
        for (StarSystem sys: systems)
            totalProductionBC += sys.colony().totalIncome();

        return totalProductionBC;
    }
    public float totalTaxablePlanetaryProduction() {
        float totalProductionBC = 0;
        List<StarSystem> systems = new ArrayList<>(allColonizedSystems());
        for (StarSystem sys: systems) {
            Colony col = sys.colony();
            if (!col.embargoed())
                totalProductionBC += col.production();
        }
        return totalProductionBC;
    }
    public float totalTaxableDevelopedPlanetaryProduction() {
        float totalProductionBC = 0;
        List<StarSystem> systems = new ArrayList<>(allColonizedSystems());
        for (StarSystem sys: systems) {
            Colony col = sys.colony();
            if (!col.embargoed() && col.isDeveloped())
                totalProductionBC += col.production();
        }
        return totalProductionBC;
    }
    public void recalcPlanetaryProduction() {
        totalEmpireProduction = -999;
        totalEmpireNonDynaProduction = -999;
        totalEmpireShipMaintenanceCost = -999;
        totalEmpireStargateCost = -999;
        totalEmpireMissileBaseCost = -999;
        inRange = -1;
    }
    public Float totalPlanetaryProduction() {
        if (totalEmpireProduction <= 0) {
            float totalProductionBC = 0;
            List<StarSystem> systems = new ArrayList<>(allColonizedSystems());
            for (StarSystem sys: systems)
                totalProductionBC += sys.colony().production();
            totalEmpireProduction = totalProductionBC;
        }
        return totalEmpireProduction;
    }
    public float totalPlanetaryProduction(Empire emp) {
        if (emp == this)
            return totalPlanetaryProduction();

        float totalProductionBC = 0;
        for (int i=0; i<sv.count(); i++) {
            if ((sv.empire(i) == emp) && (sv.colony(i) != null))
                totalProductionBC += sv.colony(i).production();
        }
        return totalProductionBC;
    }
    // modnar: add dynamic difficulty option, change AI colony production
    // create unscaled production, nonDynaTotalProd, to avoid infinite recursion
    public Float nonDynaTotalProd() {
        if (totalEmpireNonDynaProduction <= 0) {
            float totalProductionBC = 0;
            List<StarSystem> systems = new ArrayList<>(allColonizedSystems());
            for (StarSystem sys: systems) 
                totalProductionBC += sys.colony().nonDynaProd();
            totalEmpireNonDynaProduction = totalProductionBC;
        }
        return totalEmpireNonDynaProduction;
    }
    public float nonDynaTotalProd(Empire emp) {
        if (emp == this)
            return nonDynaTotalProd();

        float totalProductionBC = 0;
        for (int i=0; i<sv.count(); i++) {
            if ((sv.empire(i) == emp) && (sv.colony(i) != null))
                totalProductionBC += sv.colony(i).nonDynaProd();
        }
        return totalProductionBC;
    }
    public float totalShipMaintenanceCost() {
        if (totalEmpireShipMaintenanceCost < 0) {
            int[] counts = galaxy().ships.shipDesignCounts(id);
            float cost = 0;
            // modnar: newRace GearHead gets 50% ship maintenance cost
            // BR: Made dataRace call
            float raceBonus = dataRace().maintenanceFactor();
            for (int i=0;i<counts.length;i++) 
                cost += (counts[i] * shipLab.design(i).cost());      
            totalEmpireShipMaintenanceCost = cost * SHIP_MAINTENANCE_PCT * raceBonus;
        }
        
        return totalEmpireShipMaintenanceCost;
    }
    public float totalStargateCost() {
        if (totalEmpireStargateCost < 0) {
            float totalCostBC = 0;
            // modnar: newRace GearHead gets 50% stargate maintenance cost
             // BR: Made dataRace call
           float raceBonus = dataRace().maintenanceFactor();
            List<StarSystem> allSystems = new ArrayList<>(allColonizedSystems());
            for (StarSystem sys: allSystems)
                totalCostBC += sys.colony().shipyard().stargateMaintenanceCost();
            totalEmpireStargateCost = totalCostBC * raceBonus;
        }
        return totalEmpireStargateCost;
    }
    public float totalMissileBaseCost() {
        if (totalEmpireMissileBaseCost < 0) {
            float totalCostBC = 0;
            // modnar: newRace GearHead gets 50% base maintenance cost
            // BR: Made dataRace call
            float raceBonus = dataRace().maintenanceFactor();
            List<StarSystem> allSystems = new ArrayList<>(allColonizedSystems());
            Map<MissileBase, Float> baseCosts = new HashMap<>();
            for (StarSystem sys: allSystems)
                totalCostBC += sys.colony().defense().missileBaseMaintenanceCost(baseCosts);
            totalEmpireMissileBaseCost = totalCostBC * raceBonus;
        }
        return totalEmpireMissileBaseCost;
    }
    public float shipMaintCostPerBC() {
        float empireBC = totalPlanetaryProduction();
        return totalShipMaintenanceCost() / empireBC;
    }
    public float stargateCostPerBC() {
        float empireBC = totalPlanetaryProduction();
        return totalStargateCost() / empireBC;
    }
    public float missileBaseCostPerBC() {
        float empireBC = totalPlanetaryProduction();
        return totalMissileBaseCost() / empireBC;
    }
    public float totalPlanetaryIndustrialSpending() {
        float totalIndustrialSpendingBC = 0;
        List<StarSystem> systems = new ArrayList<>(allColonizedSystems());
        for (StarSystem sys: systems)
            totalIndustrialSpendingBC += (sys.colony().pct(Colony.INDUSTRY) * sys.colony().totalIncome());
        return totalIndustrialSpendingBC;
    }
    public float totalPlanetaryResearch() {
        if (tech().researchCompleted())
            return 0;
        float totalResearchBC = 0;
        List<StarSystem> systems = new ArrayList<>(allColonizedSystems());
        for (StarSystem sys: systems)
            totalResearchBC += sys.colony().totalPlanetaryResearch(); // some research BC may stay with colony
        return totalResearchBC;
    }
    public float totalEmpireResearch(float totalRp) {
        TechTree t = tech();
        if (t.researchCompleted())
            return 0;
        float total = 0.0f;
        total += t.computer().currentResearch(totalRp);
        total += t.construction().currentResearch(totalRp);
        total += t.forceField().currentResearch(totalRp);
        total += t.planetology().currentResearch(totalRp);
        total += t.propulsion().currentResearch(totalRp);
        total += t.weapon().currentResearch(totalRp);
        return total;
    }
    public float totalPlanetaryResearchSpending() {
        if (tech().researchCompleted())
            return 0;
        float totalResearchBC = 0;
        List<StarSystem> systems = new ArrayList<>(allColonizedSystems());
        for (StarSystem sys: systems)
            totalResearchBC += sys.colony().totalPlanetaryResearchSpending();
        return totalResearchBC;
    }
    public Float totalPlanetaryFactories() {
        float factories = 0;
        List<StarSystem> systems = new ArrayList<>(allColonizedSystems());
        for (StarSystem sys: systems)
            factories += sys.colony().industry().factories();
        return factories;
    }
    public void allocateReserve(Colony col, int amount) {
        float amt = min(totalReserve, amount);
        totalReserve -= amt;
        col.adjustReserveIncome(amt);
    }
    public void goExtinct() {
        // prevent double notifications
        if (extinct)
            return;
        
        if (lastAttacker instanceof Empire)
            GenocideIncident.create(this, (Empire) lastAttacker);
        GNNGenocideNotice.create(this, lastAttacker);

        extinct = true;
        // iterate over list copy to avoid comodification
        List<ShipFleet> fleets = galaxy().ships.allFleets(id);
        for (ShipFleet fl: fleets) {
            log("disband#1 fleet: ", fl.toString());
            fl.disband();
        }

        galaxy().removeAllTransports(id);

        for (EmpireView v : empireViews()) {
            if (v != null)
            {
                v.embassy().removeContact();
                v.empire().clearDataForExtinctEmpire(id);
            }
        }

        Galaxy g = galaxy();
        if (g.council().finalWar()) {
            g.council().removeEmpire(this);
        }
        else { 
            List<Empire> activeEmpires = galaxy().activeEmpires();
            // Player has gone extinct. Determine loss condition
            if (isPlayer()) {
                // no one killed us... abandonment suicide
                if (lastAttacker == null)
                    session().status().loseNoColonies();
                else
                    session().status().loseMilitary();
            }
            // an AI empire has gone extinct.. see if player win 
            // if only one empire is left then player must have won
            else if (activeEmpires.size() == 1)
                session().status().winMilitary();
            // multiple empires, all allied with player.. that's a win
            else if (galaxy().allAlliedWithPlayer()) 
                session().status().winMilitaryAlliance();
        }            
        status.assessTurn();
    }
    public ShipView shipViewFor(ShipDesign d ) {
        if (d == null)
            return null;

        if (d.empire() == this)
            return shipLab.shipViewFor(d);

        EmpireView cv = viewForEmpire(d.empire());
        if (cv != null)
            return cv.spies().shipViewFor(d);

        return null;
    }
    private void detectFleet(ShipFleet fl) {
        EmpireView cv = viewForEmpire(fl.empire());
        if (cv == null)
            return;

        int[] visible = fl.visibleShips(id);
        for (int i=0;i<visible.length;i++) {
            if (visible[i] > 0)
                cv.spies().detectShip(fl.empire().shipLab().design(i));
        }
    }
    public Shape drawShape(Graphics2D g, int x, int y, int w, int h) {
        return drawShape(g,x,y,w,h,color());
    }
    public Shape drawShape(Graphics2D g, int x, int y, int w, int h, Color c) {
        // Color c1 = new Color(c.getRed(),c.getGreen(),c.getBlue(),192);
        g.setColor(c);
        int m = w/10;
        switch(shape()) {
            case Empire.SHAPE_SQUARE:
                Rectangle2D rect = new Rectangle2D.Float(x+m,y+m,w-m-m,h-m-m);
                g.fill(rect); 
                return rect;
            case Empire.SHAPE_DIAMOND:
                Polygon p = new Polygon();
                p.addPoint(x, y+h/2);
                p.addPoint(x+w/2, y);
                p.addPoint(x+w, y+h/2);
                p.addPoint(x+w/2, y+h);
                g.fill(p); 
                return p;
            case Empire.SHAPE_TRIANGLE1:
                Polygon p1 = new Polygon();
                p1.addPoint(x+w/2, y);
                p1.addPoint(x, y+h);
                p1.addPoint(x+w,y+h);
                g.fill(p1);
                return p1;
            case Empire.SHAPE_TRIANGLE2:
                Polygon p2 = new Polygon();
                p2.addPoint(x+w/2, y+h);
                p2.addPoint(x, y);
                p2.addPoint(x+w,y);
                g.fill(p2);
                return p2;
            case Empire.SHAPE_CIRCLE:
            default:
                Ellipse2D ell = new Ellipse2D.Float(x,y,w,h);
                g.fill(ell); 
                return ell;
        }
    }
    public void encounterFleet(ShipFleet fl) {
        if (fl == null)
            return;
        EmpireView cv = viewForEmpire(fl.empire().id);
        if (cv == null)
            return;

        int[] visible = fl.visibleShips(id);
        for (int i=0;i<visible.length;i++) {
            if (visible[i] > 0)
                cv.spies().encounterShip(fl.empire().shipLab().design(i));
        }
    }
    public void scanFleet(ShipFleet fl) {
        EmpireView cv = viewForEmpire(fl.empire());
        if (cv == null)
            return;

        int[] visible = fl.visibleShips(id);
        for (int i=0;i<visible.length;i++) {
            if (visible[i] > 0)
                cv.spies().scanShip(fl.empire().shipLab().design(i));
        }
    }
    public void scanDesign(ShipDesign st, Empire emp) {
        EmpireView cv = viewForEmpire(emp);
        if (cv != null)
            cv.spies().scanShip(st);
    }
    public List<StarSystem> orderedColonies() {
        List<StarSystem> list = new ArrayList<>(allColonizedSystems());
        Collections.sort(list, IMappedObject.MAP_ORDER);
        return list;
    }
    public List<StarSystem> orderedTransportTargetSystems() {
        // we can only send transports to scouted, colonized
        // systems in range, with planets that we have the
        // technology to colonize
        Galaxy gal = galaxy();
        List<StarSystem> list = new ArrayList<>();
        for (int i=0; i<sv.count();i++) {
            StarSystem sys = gal.system(i);
            if (sv.inShipRange(i)
            && sv.isScouted(i)
            && sv.isColonized(i)
            && tech().canColonize(sys.planet().type()))
                list.add(sys);
        }
        Collections.sort(list, IMappedObject.MAP_ORDER);
        return list;
    }
    public List<StarSystem> orderedFleetTargetSystems(ShipFleet fl) {
        float range = fl.range();
        Galaxy gal = galaxy();
        List<StarSystem> list = new ArrayList<>();
        for (int n=0; n<sv.count();n++) {
            if (sv.withinRange(n, range))
                list.add(gal.system(n));
        }
        Collections.sort(list, IMappedObject.MAP_ORDER);
        return list;
    }
    public List<StarSystem> orderedShipConstructingColonies() {
        List<StarSystem> list = new ArrayList<>(shipBuildingSystems);
        Collections.sort(list, IMappedObject.MAP_ORDER);
        return list;
    }
    public List<ShipFleet> allFleets() {
        List<ShipFleet> list = new ArrayList<>();
        List<ShipFleet> fleets = galaxy().ships.allFleets(id);
        for (ShipFleet fl: fleets) {
            if (!fl.isEmpty())
                list.add(fl);
        }
        return list;
    }
    public List<ShipFleet> orderedFleets() {
        List<ShipFleet> list = new ArrayList<>();
        List<ShipFleet> fleets = galaxy().ships.allFleets(id);
        for (ShipFleet fl: fleets) {
            if (!fl.isEmpty())
                list.add(fl);
        }
        Collections.sort(list, IMappedObject.MAP_ORDER);
        return list;
    }
    public List<ShipFleet> orderedIdleFleets() {
        List<ShipFleet> list = new ArrayList<>();
        List<ShipFleet> fleets = galaxy().ships.allFleets(id);
        for (ShipFleet fl: fleets) {
            if (!fl.isEmpty())
            {
                if(fl.isDeployed() || fl.isInTransit())
                    continue;
                if(fl.system() != null && warEnemies().contains(fl.system().empire()))
                    continue;
                list.add(fl);                
            }
        }
        Collections.sort(list, ShipFleet.COST);
        return list;
    }
    public List<ShipFleet> orderedEnemyFleets() {
        List<ShipFleet> list = new ArrayList<>(enemyFleets());
        Collections.sort(list, IMappedObject.MAP_ORDER);
        return list;
    }
    public List<StarSystem> orderedUnderAttackSystems(boolean showUnarmed, boolean showTransports) {
        List<StarSystem> list = new ArrayList<>();
        Galaxy g = galaxy();
        Empire pl = player();
        for (StarSystem sys: pl.allColonizedSystems()) {
            if (sys.enemyShipsInOrbit(pl))
                list.add(sys);
        }
        if (knowShipETA) {
            List<Ship> vShips = player().visibleShips();
            for (Ship sh: vShips) {
                if (sh.empId() != pl.id) {
                    StarSystem sys = g.system(sh.destSysId());
                    if (sys != null) {
                        // don't care about ships going to already-added systems or AI systems
                        if (!list.contains(sys) && (sys.empire() == pl)) {
                            Empire emp = g.empire(sh.empId());
                            // add if incoming fleet is hostile to player
                            if (emp.aggressiveWith(pl.id)) {
                                boolean showShip = showUnarmed
                                        || (showTransports && (sh instanceof Transport)) || sh.isPotentiallyArmed(pl);
                                if (showShip)
                                    list.add(sys);
                            }
                        }
                    }
                }
            }
        }
        Collections.sort(list, IMappedObject.MAP_ORDER);
        return list;
    }
    public static Set<Empire> allContacts(Empire e1, Empire e2) {
        Set<Empire> contacts = new HashSet<>();
        contacts.addAll(e1.contactedEmpires());
        contacts.addAll(e2.contactedEmpires());
        contacts.remove(e1);
        contacts.remove(e2);
        return contacts;
    }
    public void setEmpireMapAvgCoordinates() {
        Empire[] emps = galaxy().empires();
        float[] xAvg = new float[emps.length];
        float[] yAvg = new float[emps.length];
        float[] xMin = new float[emps.length];
        float[] xMax = new float[emps.length];
        int[] num = new int[emps.length];
        
        for (int i=0;i<emps.length;i++) 
            xMin[i] = Float.MAX_VALUE;
        
        int n = galaxy().numStarSystems();
        for (int i=0;i<n;i++) {
            int empId = sv.empId(i);
            if (empId >= 0) {
                if (!sv.name(i).isEmpty()) {
                    StarSystem sys = sv.system(i);
                    xAvg[empId] += sys.x();
                    yAvg[empId] += sys.y();
                    xMin[empId] = min(xMin[empId], sys.x());
                    xMax[empId] = max(xMax[empId], sys.x());
                    num[empId]++;
                }
            }
        }
        
        for (Empire emp: emps) {
            int id = emp.id;
            emp.avgX = xAvg[id]/num[id];
            emp.avgY = yAvg[id]/num[id];
            emp.nameX1 =xMin[id];
            emp.nameX2 = xMax[id];
        }  
    }
    public void draw(GalaxyMapPanel map, Graphics2D g2) {
        draw(map, g2, nameX1, nameX2, avgX, avgY);
    }
    public void draw(GalaxyMapPanel map, Graphics2D g2, float xMin, float xMax, float xAvg, float yAvg) {
        if (map.hideSystemNames())
            return;
        
        // old save: new var hasn't been calculated yet
        if (avgX == 0)
            return;
        
        float empW = (xMax-xMin)*2/3;
        float adj = max(0,3-empW);
        int x0 = map.mapX(xMin-adj);
        int x1 = map.mapX(xMax+adj);
         
        
        int mapX = map.mapX(xAvg);
        int mapY = map.mapY(yAvg);
        String longName = "XXXXXXXXXX";
        String name = raceName();
        int fontSize = scaledFont(g2,longName,x1-x0,60,12);
        //int fontSize = max(12, min(40, (int) (50*(x1-x0)/scale)));
        if (fontSize >= 12) {
            if (!name.isEmpty()) {
                g2.setFont(narrowFont(fontSize));
                g2.setColor(nameColor());
                int sw = g2.getFontMetrics().stringWidth(name);
                int x = mapX - (sw/2);
                int y = mapY - (fontSize/2);
                drawString(g2,name, x, y);
            }
        }
    }
   
    // BR:
    /**
	 * @return the Challenge Mod State
	 */
	public boolean isChallengeMode() {
		for (StarSystem system : colonizedSystems) {
			if (system.colony() != null) {
				return system.colony().isChallengeMode();
			}
		}
        return false; // This should never happen... But better safe than sorry!
	} // \BR

    // BR:
    /**
	 * @return the current number of companion worlds
	 */
	public int getCompanionWorldsNumber() {
        if (compSysId != null) {
        	return compSysId.length;
        }
        return 0;
	} // \BR

    // BR:
    /**
	 * @return the current Name of Home World
	 */
	public String getHomeWorldName() {
        return galaxy().system(homeSysId).name();
	} // \BR
    // BR:
    /**
     * Change Home World and Companions Name
	 * @param NewName the new HomeWorld Name
	 */
	public void setHomeWorldName(String newName) {
		sv.name(homeSysId, newName);
        int numCompWorlds = getCompanionWorldsNumber();
        if (numCompWorlds > 0) { 
            for (int id = 0; id < numCompWorlds; id++) {
               	String name = GalaxyFactory.compSysName[id] + " " + newName;
               	sv.name(compSysId[id], name);
             }
        }
	} // \BR
    public static Comparator<Empire> TOTAL_POPULATION = (Empire o1, Empire o2) -> o2.totalPlanetaryPopulation().compareTo(o1.totalPlanetaryPopulation());
    public static Comparator<Empire> TOTAL_PRODUCTION = (Empire o1, Empire o2) -> o2.totalPlanetaryProduction().compareTo(o1.totalPlanetaryProduction());
    public static Comparator<Empire> AVG_TECH_LEVEL   = (Empire o1, Empire o2) -> o2.tech.avgTechLevel().compareTo(o1.tech.avgTechLevel());
    public static Comparator<Empire> TOTAL_FLEET_SIZE = (Empire o1, Empire o2) -> o2.totalFleetSize().compareTo(o1.totalFleetSize());
    public static Comparator<Empire> RACE_NAME        = (Empire o1, Empire o2) -> o1.raceName().compareTo(o2.raceName());
    public static Comparator<Empire> HISTORICAL_SIZE  = (Empire o1, Empire o2) -> Base.compare(o2.numColoniesHistory, o1.numColoniesHistory);
	// ==================== EmpireBaseData ====================
	//
	public static class EmpireBaseData {
		public String raceKey;
		public String dataRaceKey;
		public String empireName;
		public String dataName; // TODO BR: Validate for custom Races
		public String raceName;  // TODO BR: Validate for custom Races
		public String leaderName;
		public boolean isCustomRace;
		public DynOptions raceOptions;
		public int raceAI;
		public Personality personality;
		public Objective objective;

		public SystemBaseData homeSys;
		public int[] compSysId;
		public SystemBaseData[] companions;
		
		public EmpireBaseData(Empire src, SystemBaseData[] systems) {
			raceKey		 = src.raceKey;
			dataRaceKey	 = src.dataRaceKey;
			empireName	 = src.name();
			raceName	 = src.raceName();
			leaderName	 = src.leader().name();
			isCustomRace = src.isCustomRace();
			if (isCustomRace)
				dataName = src.dataRace().setupName;
			else 
				dataName = src.dataRace().setupName();
			raceOptions	 = src.raceOptions();
			raceAI		 = src.selectedAI;
			personality	 = src.leader().personality;
			objective	 = src.leader().objective;

			homeSys 	= systems[src.homeSysId()];
			compSysId	= src.compSysId;
			int compNum = src.getCompanionWorldsNumber();
			if(compSysId != null) {
				companions = new SystemBaseData[compNum];
				for (int i=0; i<compNum; i++)
					companions[i] = systems[i];
			}
		}
		public void setRace(String r, String dr, boolean isCR,
				DynOptions options, int ai) {
			raceKey		 = r;
			dataRaceKey	 = dr;
			isCustomRace = isCR;
			raceOptions	 = options;
			raceAI		 = ai;
		}
		public void raceAI(int ai)	{ raceAI = ai; }
		public int  raceAI()		{ return raceAI; }
		
	}
}
