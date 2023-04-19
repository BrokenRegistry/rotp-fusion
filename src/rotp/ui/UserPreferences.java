/*
 * Copyright 2015-2020 Ray Fowler
 *
 * Licensed under the GNU General Public License, Version 3 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *	 https://www.gnu.org/licenses/gpl-3.0.html
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package rotp.ui;


import static rotp.model.empires.SystemView.AUTO_FLAG_NOT;
import static rotp.model.empires.SystemView.FLAG_COLOR_AQUA;
import static rotp.model.empires.SystemView.FLAG_COLOR_BLUE;
import static rotp.model.empires.SystemView.FLAG_COLOR_GREEN;
import static rotp.model.empires.SystemView.FLAG_COLOR_LTBLUE;
import static rotp.model.empires.SystemView.FLAG_COLOR_NONE;
import static rotp.model.empires.SystemView.FLAG_COLOR_ORANGE;
import static rotp.model.empires.SystemView.FLAG_COLOR_PINK;
import static rotp.model.empires.SystemView.FLAG_COLOR_PURPLE;
import static rotp.model.empires.SystemView.FLAG_COLOR_RED;
import static rotp.model.empires.SystemView.FLAG_COLOR_WHITE;
import static rotp.model.empires.SystemView.FLAG_COLOR_YELLOW;
import static rotp.model.empires.SystemView.flagAssignationMap;
import static rotp.model.empires.SystemView.flagColorMap;
import static rotp.model.game.IGameOptions.AI_HOSTILITY_NORMAL;
import static rotp.model.game.IGameOptions.AUTOPLAY_OFF;
import static rotp.model.game.IGameOptions.COLONIZING_NORMAL;
import static rotp.model.game.IGameOptions.COUNCIL_REBELS;
import static rotp.model.game.IGameOptions.DIFFICULTY_NORMAL;
import static rotp.model.game.IGameOptions.FUEL_RANGE_NORMAL;
import static rotp.model.game.IGameOptions.GALAXY_AGE_NORMAL;
import static rotp.model.game.IGameOptions.NEBULAE_NORMAL;
import static rotp.model.game.IGameOptions.OPPONENT_AI_CRUEL;
import static rotp.model.game.IGameOptions.PLANET_QUALITY_NORMAL;
import static rotp.model.game.IGameOptions.RANDOMIZE_AI_NONE;
import static rotp.model.game.IGameOptions.RANDOM_EVENTS_NO_MONSTERS;
import static rotp.model.game.IGameOptions.RESEARCH_NORMAL;
import static rotp.model.game.IGameOptions.SHAPE_RECTANGLE;
import static rotp.model.game.IGameOptions.SIZE_SMALL;
import static rotp.model.game.IGameOptions.STAR_DENSITY_NORMAL;
import static rotp.model.game.IGameOptions.TECH_TRADING_YES;
import static rotp.model.game.IGameOptions.TERRAFORMING_NORMAL;
import static rotp.model.game.IGameOptions.WARP_SPEED_NORMAL;
import static rotp.model.game.IGameOptions.baseRaceOptions;
import static rotp.model.game.MOO1GameOptions.getAiHostilityOptions;
import static rotp.model.game.MOO1GameOptions.getAutoplayOptions;
import static rotp.model.game.MOO1GameOptions.getColonizingOptions;
import static rotp.model.game.MOO1GameOptions.getCouncilWinOptions;
import static rotp.model.game.MOO1GameOptions.getFuelRangeOptions;
import static rotp.model.game.MOO1GameOptions.getGalaxyAgeOptions;
import static rotp.model.game.MOO1GameOptions.getGalaxyShapeOptions;
import static rotp.model.game.MOO1GameOptions.getGalaxySizeOptions;
import static rotp.model.game.MOO1GameOptions.getGameDifficultyOptions;
import static rotp.model.game.MOO1GameOptions.getNebulaeOptions;
import static rotp.model.game.MOO1GameOptions.getPlanetQualityOptions;
import static rotp.model.game.MOO1GameOptions.getRandomEventOptions;
import static rotp.model.game.MOO1GameOptions.getRandomizeAIOptions;
import static rotp.model.game.MOO1GameOptions.getResearchRateOptions;
import static rotp.model.game.MOO1GameOptions.getStarDensityOptions;
import static rotp.model.game.MOO1GameOptions.getTechTradingOptions;
import static rotp.model.game.MOO1GameOptions.getTerraformingOptions;
import static rotp.model.game.MOO1GameOptions.getWarpSpeedOptions;
import static rotp.ui.game.SetupGalaxyUI.mouseBoxIndex;
import static rotp.ui.util.InterfaceParam.langLabel;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.util.Arrays;
import java.util.LinkedList;

import rotp.Rotp;
import rotp.model.empires.GalacticCouncil;
import rotp.model.events.RandomEvents;
import rotp.model.galaxy.StarSystem;
import rotp.model.game.GameSession;
import rotp.model.game.GovernorOptions;
import rotp.model.game.IGameOptions;
import rotp.model.game.MOO1GameOptions;
import rotp.ui.main.GalaxyMapPanel;
import rotp.ui.util.GlobalCROptions;
import rotp.ui.util.InterfaceParam;
import rotp.ui.util.ParamAAN2;
import rotp.ui.util.ParamBoolean;
import rotp.ui.util.ParamButtonHelp;
import rotp.ui.util.ParamCR;
import rotp.ui.util.ParamFloat;
import rotp.ui.util.ParamInteger;
import rotp.ui.util.ParamList;
import rotp.ui.util.ParamOptions;
import rotp.ui.util.ParamString;
import rotp.ui.util.ParamSubUI;
import rotp.ui.util.ParamTech;
import rotp.ui.util.ParamTitle;
import rotp.ui.util.PlayerShipSet;
import rotp.ui.util.RandomAlienRaces;
import rotp.ui.util.SpecificCROption;
import rotp.util.FontManager;
import rotp.util.LanguageManager;
import rotp.util.sound.SoundManager;

public class UserPreferences {
	private static final String WINDOW_MODE = "GAME_SETTINGS_WINDOWED";
	private static final String BORDERLESS_MODE = "GAME_SETTINGS_BORDERLESS";
	private static final String FULLSCREEN_MODE = "GAME_SETTINGS_FULLSCREEN";
	private static final String GRAPHICS_LOW = "GAME_SETTINGS_GRAPHICS_LOW";
	private static final String GRAPHICS_MEDIUM = "GAME_SETTINGS_GRAPHICS_MED";
	private static final String GRAPHICS_HIGH = "GAME_SETTINGS_GRAPHICS_HIGH";
	// private static final String AUTOCOLONIZE_YES = "GAME_SETTINGS_AUTOCOLONIZE_YES";
	// private static final String AUTOCOLONIZE_NO = "GAME_SETTINGS_AUTOCOLONIZE_NO";
	private static final String AUTOBOMBARD_NO = "GAME_SETTINGS_AUTOBOMBARD_NO";
	private static final String AUTOBOMBARD_NEVER = "GAME_SETTINGS_AUTOBOMBARD_NEVER";
	private static final String AUTOBOMBARD_YES = "GAME_SETTINGS_AUTOBOMBARD_YES";
	private static final String AUTOBOMBARD_WAR = "GAME_SETTINGS_AUTOBOMBARD_WAR";
	private static final String AUTOBOMBARD_INVADE = "GAME_SETTINGS_AUTOBOMBARD_INVADE";
	private static final String TEXTURES_NO = "GAME_SETTINGS_TEXTURES_NO";
	private static final String TEXTURES_INTERFACE = "GAME_SETTINGS_TEXTURES_INTERFACE";
	private static final String TEXTURES_MAP = "GAME_SETTINGS_TEXTURES_MAP";
	private static final String TEXTURES_BOTH = "GAME_SETTINGS_TEXTURES_BOTH";
	private static final String SAVEDIR_DEFAULT = "GAME_SETTINGS_SAVEDIR_DEFAULT";
	private static final String SAVEDIR_CUSTOM = "GAME_SETTINGS_SAVEDIR_CUSTOM";
	private static final String SENSITIVITY_HIGH = "GAME_SETTINGS_SENSITIVITY_HIGH";
	private static final String SENSITIVITY_MEDIUM = "GAME_SETTINGS_SENSITIVITY_MEDIUM";
	private static final String SENSITIVITY_LOW = "GAME_SETTINGS_SENSITIVITY_LOW";

	private static final String PREFERENCES_FILE  = "Remnants.cfg";
	public  static final String GAME_OPTIONS_FILE = "Game.options";
	public  static final String LAST_OPTIONS_FILE = "Last.options";
	public  static final String LIVE_OPTIONS_FILE = "Live.options";
	public  static final String USER_OPTIONS_FILE = "User.options";
	public  static final String GALAXY_TEXT_FILE  = "Galaxy.txt";
	private static final int    MAX_BACKUP_TURNS  = 20; // modnar: change max turns between backups to 20
	private static final String keyFormat = "%-25s: "; // BR: from 20 to 25 for a better alignment

	// BR common for All MOD entries
	public  static final String BASE_UI		= "SETUP_";
	private static final String GAME_UI		= "GAME_SETTINGS_";
	private static final String ADV_UI		= "SETTINGS_";
	public static final String MOD_UI		= "SETTINGS_MOD_";
	public static final String HEADERS		= "HEADERS_";
	public static final String ALL_GUI_ID	= "ALL_GUI";

	// Sub UI Options parameters
	// BR: AUTO-FLAG PARAMETERS SUB UI
	public static final ParamList autoFlagAssignation1	= new ParamList(
			MOD_UI, "AUTO_FLAG_ASSIGN_1",
			AUTO_FLAG_NOT, flagAssignationMap);
	public static final ParamList autoFlagAssignation2	= new ParamList(
			MOD_UI, "AUTO_FLAG_ASSIGN_2",
			AUTO_FLAG_NOT, flagAssignationMap);
	public static final ParamList autoFlagAssignation3	= new ParamList(
			MOD_UI, "AUTO_FLAG_ASSIGN_3",
			AUTO_FLAG_NOT, flagAssignationMap);
	public static final ParamList autoFlagAssignation4	= new ParamList(
			MOD_UI, "AUTO_FLAG_ASSIGN_4",
			AUTO_FLAG_NOT, flagAssignationMap);
	public static final ParamList flagTerranColor		= new ParamList(
			MOD_UI, "AUTO_FLAG_TERRAN",
			FLAG_COLOR_GREEN, flagColorMap);
	public static final ParamList flagJungleColor		= new ParamList(
			MOD_UI, "AUTO_FLAG_JUNGLE",
			FLAG_COLOR_GREEN, flagColorMap);
	public static final ParamList flagOceanColor		= new ParamList(
			MOD_UI, "AUTO_FLAG_OCEAN",
			FLAG_COLOR_AQUA, flagColorMap);
	public static final ParamList flagAridColor			= new ParamList(
			MOD_UI, "AUTO_FLAG_ARID",
			FLAG_COLOR_YELLOW, flagColorMap);
	public static final ParamList flagSteppeColor		= new ParamList(
			MOD_UI, "AUTO_FLAG_STEPPE",
			FLAG_COLOR_YELLOW, flagColorMap);
	public static final ParamList flagDesertColor		= new ParamList(
			MOD_UI, "AUTO_FLAG_DESERT",
			FLAG_COLOR_YELLOW, flagColorMap);
	public static final ParamList flagMinimalColor		= new ParamList(
			MOD_UI, "AUTO_FLAG_MINIMAL",
			FLAG_COLOR_YELLOW, flagColorMap);
	public static final ParamList flagBarrenColor		= new ParamList(
			MOD_UI, "AUTO_FLAG_BARREN",
			FLAG_COLOR_ORANGE, flagColorMap);
	public static final ParamList flagTundraColor		= new ParamList(
			MOD_UI, "AUTO_FLAG_TUNDRA",
			FLAG_COLOR_WHITE, flagColorMap);
	public static final ParamList flagDeadColor			= new ParamList(
			MOD_UI, "AUTO_FLAG_DEAD",
			FLAG_COLOR_WHITE, flagColorMap);
	public static final ParamList flagInfernoColor		= new ParamList(
			MOD_UI, "AUTO_FLAG_INFERNO",
			FLAG_COLOR_RED, flagColorMap);
	public static final ParamList flagToxicColor		= new ParamList(
			MOD_UI, "AUTO_FLAG_TOXIC",
			FLAG_COLOR_RED, flagColorMap);
	public static final ParamList flagRadiatedColor		= new ParamList(
			MOD_UI, "AUTO_FLAG_RADIATED",
			FLAG_COLOR_PURPLE, flagColorMap);
	public static final ParamList flagAsteroidColor		= new ParamList(
			MOD_UI, "AUTO_FLAG_ASTEROID",
			FLAG_COLOR_PINK, flagColorMap);

	public static final ParamList flagEnvGaiaColor		= new ParamList(
			MOD_UI, "AUTO_FLAG_ENV_GAIA",
			FLAG_COLOR_GREEN, flagColorMap);
	public static final ParamList flagEnvFertileColor	= new ParamList(
			MOD_UI, "AUTO_FLAG_ENV_FERTILE",
			FLAG_COLOR_AQUA, flagColorMap);
	public static final ParamList flagEnvNormalColor	= new ParamList(
			MOD_UI, "AUTO_FLAG_ENV_NORMAL",
			FLAG_COLOR_NONE, flagColorMap);
	public static final ParamList flagEnvHostileColor	= new ParamList(
			MOD_UI, "AUTO_FLAG_ENV_HOSTILE",
			FLAG_COLOR_ORANGE, flagColorMap);
	public static final ParamList flagEnvNoneColor		= new ParamList(
			MOD_UI, "AUTO_FLAG_ENV_NONE",
			FLAG_COLOR_NONE, flagColorMap);

	public static final ParamList flagUltraPoorColor	= new ParamList(
			MOD_UI, "AUTO_FLAG_ULTRA_POOR",
			FLAG_COLOR_YELLOW, flagColorMap);
	public static final ParamList flagPoorColor			= new ParamList(
			MOD_UI, "AUTO_FLAG_POOR",
			FLAG_COLOR_ORANGE, flagColorMap);
	public static final ParamList flagAssetNormalColor	= new ParamList(
			MOD_UI, "AUTO_FLAG_NORMAL",
			FLAG_COLOR_NONE, flagColorMap);
	public static final ParamList flagRichColor			= new ParamList(
			MOD_UI, "AUTO_FLAG_RICH",
			FLAG_COLOR_PINK, flagColorMap);
	public static final ParamList flagUltraRichColor	= new ParamList(
			MOD_UI, "AUTO_FLAG_ULTRA_RICH",
			FLAG_COLOR_RED, flagColorMap);
	public static final ParamList flagAntaranColor		= new ParamList(
			MOD_UI, "AUTO_FLAG_RUINS_ANTARAN",
			FLAG_COLOR_LTBLUE, flagColorMap);
	public static final ParamList flagOrionColor		= new ParamList(
			MOD_UI, "AUTO_FLAG_RUINS_ORION",
			FLAG_COLOR_BLUE, flagColorMap);
	public static final ParamList flagNoneColor			= new ParamList(
			MOD_UI, "AUTO_FLAG_NONE",
			FLAG_COLOR_NONE, flagColorMap);

	public static final ParamList flagTechGaiaColor		= new ParamList(
			MOD_UI, "AUTO_FLAG_TECH_GAIA",
			FLAG_COLOR_GREEN, flagColorMap);
	public static final ParamList flagTechFertileColor	= new ParamList(
			MOD_UI, "AUTO_FLAG_TECH_FERTILE",
			FLAG_COLOR_GREEN, flagColorMap);
	public static final ParamList flagTechGoodColor		= new ParamList(
			MOD_UI, "AUTO_FLAG_TECH_GOOD",
			FLAG_COLOR_BLUE, flagColorMap);
	public static final ParamList flagTechStandardColor	= new ParamList(
			MOD_UI, "AUTO_FLAG_TECH_STANDARD",
			FLAG_COLOR_YELLOW, flagColorMap);
	public static final ParamList flagTechBarrenColor	= new ParamList(
			MOD_UI, "AUTO_FLAG_TECH_BARREN",
			FLAG_COLOR_ORANGE, flagColorMap);
	public static final ParamList flagTechDeadColor		= new ParamList(
			MOD_UI, "AUTO_FLAG_TECH_DEAD",
			FLAG_COLOR_WHITE, flagColorMap);
	public static final ParamList flagTechToxicColor	= new ParamList(
			MOD_UI, "AUTO_FLAG_TECH_TOXIC",
			FLAG_COLOR_RED, flagColorMap);
	public static final ParamList flagTechRadiatedColor	= new ParamList(
			MOD_UI, "AUTO_FLAG_TECH_RADIATED",
			FLAG_COLOR_PURPLE, flagColorMap);
	public static final ParamList flagTechNoneColor		= new ParamList(
			MOD_UI, "AUTO_FLAG_TECH_NONE",
			FLAG_COLOR_PINK, flagColorMap);
	
	// MOD GUI OPTIONS:
	// BR: ===== First Mod GUI: 
	public static final ParamAAN2	 artifactsHomeworld		= new ParamAAN2("HOME_ARTIFACT");
	public static final ParamAAN2	 fertileHomeworld		= new ParamAAN2("HOME_FERTILE");
	public static final ParamAAN2	 richHomeworld			= new ParamAAN2("HOME_RICH");
	public static final ParamAAN2	 ultraRichHomeworld		= new ParamAAN2("HOME_ULTRA_RICH");
	public static final ParamFloat	 minDistArtifactPlanet	= new ParamFloat(
			MOD_UI, "DIST_ARTIFACT_PLANET", 0.0f, 0.0f, null, 0.2f, 1f, 5f, "0.0##", "0.0");
	public static final ParamBoolean battleScout			= new ParamBoolean(
			MOD_UI, "BATTLE_SCOUT", false);
	public static final ParamBoolean randomTechStart		= new ParamBoolean(
			MOD_UI, "RANDOM_TECH_START", false);
	private static final ParamInteger companionWorlds		= new ParamInteger(
			MOD_UI, "COMPANION_WORLDS"
			, 0, -4, 6, true);
	private static final ParamInteger retreatRestrictionTurns= new ParamInteger(
			MOD_UI, "RETREAT_RESTRICTION_TURNS"
			, 100, 0, 100, 1, 5, 20);
	private static final ParamList	 retreatRestrictions	= new ParamList(
			MOD_UI, "RETREAT_RESTRICTIONS", "None")
			.put("None",	MOD_UI + "RETREAT_NONE")
			.put("AI",		MOD_UI + "RETREAT_AI")
			.put("Player",	MOD_UI + "RETREAT_PLAYER")
			.put("Both",	MOD_UI + "RETREAT_BOTH");
	private static final ParamList	 targetBombard			= new ParamList(
			MOD_UI, "TARGET_BOMBARD", "None")
			.put("None",	MOD_UI + "RETREAT_NONE")
			.put("AI",		MOD_UI + "RETREAT_AI")
			.put("Player",	MOD_UI + "RETREAT_PLAYER")
			.put("Both",	MOD_UI + "RETREAT_BOTH");
	public static final ParamInteger customDifficulty		= new ParamInteger(
			MOD_UI, "CUSTOM_DIFFICULTY"
			, 100, 20, 500, 1, 5, 20);
	public static final ParamBoolean dynamicDifficulty		= new ParamBoolean(
			MOD_UI, "DYNAMIC_DIFFICULTY", false);
	private static final ParamFloat	 missileSizeModifier	= new ParamFloat(
			MOD_UI, "MISSILE_SIZE_MODIFIER"
			, 2f/3f, 0.1f, 1f, 0.01f, 0.05f, 0.2f, "0.##", "%");
	public static final ParamBoolean challengeMode			= new ParamBoolean(
			MOD_UI, "CHALLENGE_MODE", false);
	public static final ParamBoolean maximizeSpacing		= new ParamBoolean(
			MOD_UI, "MAX_SPACINGS", false);
	public static final ParamInteger spacingLimit			= new ParamInteger(
			MOD_UI, "MAX_SPACINGS_LIM"
			, 16, 3, Rotp.maximumSystems-1, 1, 10, 100);
	public static final ParamInteger minStarsPerEmpire		= new ParamInteger(
			MOD_UI, "MIN_STARS_PER_EMPIRE"
			, 3, 3, Rotp.maximumSystems-1, 1, 5, 20);
	public static final ParamInteger prefStarsPerEmpire		= new ParamInteger(
			MOD_UI, "PREF_STARS_PER_EMPIRE"
			, 10, 3, Rotp.maximumSystems-1, 1, 10, 100);
	public static final ParamInteger dynStarsPerEmpire		= new ParamInteger(
			MOD_UI, "DYN_STARS_PER_EMPIRE"
			, 10, 3, Rotp.maximumSystems-1, 1, 10, 100) {
		@Override public Integer defaultValue() {
			return prefStarsPerEmpire.get();
		}
	};
	public static final ParamBoolean restartChangesAliensAI	= new ParamBoolean(
			MOD_UI, "RESTART_CHANGES_ALIENS_AI", false);
	public static final ParamBoolean restartChangesPlayerAI	= new ParamBoolean(
			MOD_UI, "RESTART_CHANGES_PLAYER_AI", false);
	public static final ParamBoolean restartAppliesSettings	= new ParamBoolean(
			MOD_UI, "RESTART_APPLY_SETTINGS",false);
	public static final ParamList  restartChangesPlayerRace	= new ParamList(
			MOD_UI, "RESTART_PLAYER_RACE", "Swap")
			.put("Last", MOD_UI + "RESTART_PLAYER_RACE_LAST")
			.put("Swap", MOD_UI + "RESTART_PLAYER_RACE_SWAP")
			.put("GuiSwap",	 MOD_UI + "RESTART_PLAYER_RACE_GUI_SWAP")
			.put("GuiLast",	 MOD_UI + "RESTART_PLAYER_RACE_GUI_LAST");
	private static final ParamFloat   counciRequiredPct		= new ParamFloat(
			MOD_UI, "COUNCIL_REQUIRED_PCT"
			, GalacticCouncil.PCT_REQUIRED
			, 0f, 0.99f, 0.01f/3f, 0.02f, 0.1f, "0.0##", "‰") {
		@Override public Float set(Float newValue) {
			GalacticCouncil.PCT_REQUIRED = newValue;
			return super.set(newValue);
		}
	};
	private static final ParamInteger eventsStartTurn		= new ParamInteger(
			MOD_UI, "EVENTS_START_TURN"
			, RandomEvents.START_TURN, 1, null, 1, 5, 20) {
		@Override public Integer set(Integer newValue) {
			RandomEvents.START_TURN = newValue;
			return super.set(newValue);
		}
	};
//	public static final ParamBoolean alternateMonsterMessages = new ParamBoolean(
//			MOD_UI, "ALT_MONSTER_MESSAGES", true);
	public static final ParamInteger piratesDelayTurn		= new ParamInteger(
			MOD_UI, "PIRATES_DELAY_TURN",	25, 0, null, 1, 5, 20);
	public static final ParamInteger amoebaDelayTurn		= new ParamInteger(
			MOD_UI, "AMOEBA_DELAY_TURN",	100, 0, null, 1, 5, 20);
	public static final ParamInteger crystalDelayTurn		= new ParamInteger(
			MOD_UI, "CRYSTAL_DELAY_TURN",	100, 0, null, 1, 5, 20);
	public static final ParamInteger piratesReturnTurn		= new ParamInteger(
			MOD_UI, "PIRATES_RETURN_TURN",	0, 0, null, 1, 5, 20);
	public static final ParamInteger amoebaReturnTurn		= new ParamInteger(
			MOD_UI, "AMOEBA_RETURN_TURN",	0, 0, null, 1, 5, 20);
	public static final ParamInteger crystalReturnTurn		= new ParamInteger(
			MOD_UI, "CRYSTAL_RETURN_TURN",	0, 0, null, 1, 5, 20);
	public static final ParamInteger piratesMaxSystems		= new ParamInteger(
			MOD_UI, "PIRATES_MAX_SYSTEMS",	0, 0, null, 1, 5, 20);
	public static final ParamInteger amoebaMaxSystems		= new ParamInteger(
			MOD_UI, "AMOEBA_MAX_SYSTEMS",	0, 0, null, 1, 5, 20);
	public static final ParamInteger crystalMaxSystems		= new ParamInteger(
			MOD_UI, "CRYSTAL_MAX_SYSTEMS",	0, 0, null, 1, 5, 20);
	private static final ParamTech    techIrradiated		= new 
			ParamTech("TECH_IRRADIATED",	3, "ControlEnvironment",6); // level 18
	private static final ParamTech    techCloaking			= new 
			ParamTech("TECH_CLOAKING",		2, "Cloaking",			0); // level 27
	private static final ParamTech    techStargate			= new 
			ParamTech("TECH_STARGATES",		4, "Stargate", 			0); // level 27
	private static final ParamTech    techHyperspace		= new 
			ParamTech("TECH_HYPERSPACE",	0, "HyperspaceComm",	0); // level 34
	private static final ParamTech    techIndustry2			= new 
			ParamTech("TECH_INDUSTRY_2",	1, "ImprovedIndustrial",7); // level 38
	private static final ParamTech    techThorium			= new 
			ParamTech("TECH_THORIUM",		4, "FuelRange",			8); // level 41
	private static final ParamTech    techTransport			= new 
			ParamTech("TECH_TRANSPORTERS",	4, "CombatTransporter",	0); // level 45
	public static final ParamInteger randomAlienRacesMin		= new ParamInteger(
			MOD_UI, "RACES_RAND_MIN"
			, -50, -100, 100, 1, 5, 20);
	public static final ParamInteger randomAlienRacesMax		= new ParamInteger(
			MOD_UI, "RACES_RAND_MAX"
			, 50, -100, 100, 1, 5, 20);
	public static final ParamInteger randomAlienRacesTargetMax	= new ParamInteger(
			MOD_UI, "RACES_RAND_TARGET_MAX"
			, 75, null, null, 1, 10, 100);
	public static final ParamInteger randomAlienRacesTargetMin	= new ParamInteger(
			MOD_UI, "RACES_RAND_TARGET_MIN"
			, 0, null, null, 1, 10, 100);
	public static final ParamBoolean randomAlienRacesSmoothEdges= new ParamBoolean(
			MOD_UI, "RACES_RAND_EDGES", true);
	public static final RandomAlienRaces randomAlienRaces		= new RandomAlienRaces (
			MOD_UI, "RACES_ARE_RANDOM", RandomAlienRaces.TARGET);
	public static final LinkedList<ParamTech> techModList		= new LinkedList<>(Arrays.asList(
			techIrradiated, techCloaking, techStargate, techHyperspace,
			techIndustry2, techThorium, techTransport
			));
	public static final ParamInteger bombingTarget				= new ParamInteger(
			MOD_UI, "BOMBING_TARGET"
			, 10, null, null, 1, 5, 20);
	public static final ParamInteger flagColorCount				= new ParamInteger(
			MOD_UI, "FLAG_COLOR_COUNT", 1, 1, 4);


	// BR: ===== Global settings Mod GUI:
	private static boolean gamePlayed = false; // to differentiate startup from loaded game
	private static boolean loadRequest = false; // to Load options requested in menu
	public static final ParamOptions menuSpecial 		= new ParamOptions("");
	public static final ParamFloat   showFleetFactor	= new ParamFloat(
			MOD_UI, "SHOW_FLEET_FACTOR"
			, 1.0f, 0.3f, 3f, 0.01f, 0.05f, 0.2f, "%", "%") {
		@Override public Float set(Float newValue) {
			GalaxyMapPanel.MAX_STARGATE_SCALE		 = (int) (40 * newValue);
			GalaxyMapPanel.MAX_FLEET_UNARMED_SCALE	 = (int) (40 * newValue);
			GalaxyMapPanel.MAX_FLEET_TRANSPORT_SCALE = (int) (60 *newValue);
			GalaxyMapPanel.MAX_FLEET_SMALL_SCALE	 = (int) (60 * newValue);
			GalaxyMapPanel.MAX_FLEET_LARGE_SCALE	 = (int) (80 * newValue);
			GalaxyMapPanel.MAX_FLEET_HUGE_SCALE		 = (int) (100 * newValue);
			return super.set(newValue);
		}
	};
	public static final ParamFloat   showFlagFactor		= new ParamFloat(
			MOD_UI, "SHOW_FLAG_FACTOR"
			, 1.0f, 0.3f, 3f, 0.01f, 0.05f, 0.2f, "%", "%") {
		@Override public Float set(Float newValue) {
			GalaxyMapPanel.MAX_FLAG_SCALE = (int) (80 * newValue);
			return super.set(newValue);
		}
	};
	public static final ParamFloat   showPathFactor		= new ParamFloat(
			MOD_UI, "SHOW_PATH_FACTOR"
			, 1.0f, 0.3f, 3f, 0.01f, 0.05f, 0.2f, "%", "%") {
		@Override public Float set(Float newValue) {
			GalaxyMapPanel.MAX_RALLY_SCALE = (int) (100 * newValue);
			return super.set(newValue);
		}
	};
	public static final ParamInteger showNameMinFont	= new ParamInteger(
			MOD_UI, "SHOW_NAME_MIN_FONT"
			, 8, 2, 24, 1, 2, 5) {
		@Override public Integer set(Integer newValue) {
			StarSystem.minFont	= newValue;
			StarSystem.minFont2	= Math.round(newValue/showInfoFontRatio.get());
			return super.set(newValue);
		}
	};
	public static final ParamFloat   showInfoFontRatio	= new ParamFloat(
			MOD_UI, "SHOW_INFO_FONT_RATIO"
			, 0.7f, 0.2f, 3f, 0.01f, 0.05f, 0.2f, "%", "%") {
		@Override public Float set(Float newValue) {
			StarSystem.minFont2	= Math.round(StarSystem.minFont/newValue);
			return super.set(newValue);
		}
	};
	public static final ParamFloat   mapFontFactor		= new ParamFloat(
			MOD_UI, "MAP_FONT_FACTOR"
			, 1.0f, 0.3f, 3f, 0.01f, 0.05f, 0.2f, "%", "%") {
		@Override public Float set(Float newValue) {
			StarSystem.fontPct = Math.round(newValue * 100);
			return super.set(newValue);
		}
	};
	static final ParamOptions menuStartup		= new ParamOptions(
			MOD_UI, "MENU_STARTUP", ParamOptions.VANILLA);
	static final ParamOptions menuAfterGame		= new ParamOptions(
			MOD_UI, "MENU_AFTER_GAME", ParamOptions.VANILLA);
	public static final ParamOptions menuLoadGame		= new ParamOptions(
			MOD_UI, "MENU_LOADING_GAME", ParamOptions.GAME);
	public static final ParamBoolean compactOptionOnly	= new ParamBoolean(
			MOD_UI, "COMPACT_OPTION_ONLY", false);
	public static final ParamBoolean showGridCircular	= new ParamBoolean(
			MOD_UI, "SHOW_GRID_CIRCULAR", false);
	public static final ParamBoolean showTooltips		= new ParamBoolean(
			MOD_UI, "SHOW_TOOLTIPS", true);
	public static final ParamBoolean useFusionFont		= new ParamBoolean(
			MOD_UI, "USE_FUSION_FONT", false) {
		@Override public Boolean set(Boolean newValue) {
			FontManager.INSTANCE.resetGalaxyFont();
			return super.set(newValue);
		}
	};
	public static final ParamBoolean showNextCouncil	= new ParamBoolean(
			MOD_UI, "SHOW_NEXT_COUNCIL", false); // Show years left until next council
	public static final ParamInteger galaxyPreviewColorStarsSize = new ParamInteger(
			MOD_UI, "GALAXY_PREVIEW_COLOR_SIZE" , 5, 0, 20, 1, 2, 5);
	public static final ParamInteger minListSizePopUp		= new ParamInteger(
			MOD_UI, "MIN_LIST_SIZE_POP_UP" , 4, 0, 10, true)
			.specialZero(MOD_UI + "MIN_LIST_SIZE_POP_UP_NEVER");
	public static final ParamInteger showLimitedWarnings	= new ParamInteger(
			MOD_UI, "SHOW_LIMITED_WARNINGS" , -1, -1, 49, 1, 2, 5)
			.loop(true)
			.specialNegative(MOD_UI + "SHOW_LIMITED_WARNINGS_ALL");
	public static final ParamBoolean showAlliancesGNN		= new ParamBoolean(
			MOD_UI, "SHOW_ALLIANCES_GNN", true);
	public static final ParamBoolean techExchangeAutoRefuse = new ParamBoolean(
			MOD_UI, "TECH_EXCHANGE_AUTO_NO", false);

	// BR: Galaxy Menu addition
	public static final ParamInteger galaxyRandSource		= new ParamInteger(
			MOD_UI, "GALAXY_RAND_SOURCE" , 0, 0, Integer.MAX_VALUE, 1, 100, 10000).loop(true);
	public static final ParamBoolean showNewRaces 			= new ParamBoolean(
			MOD_UI, "SHOW_NEW_RACES", false);
	public static final GlobalCROptions globalCROptions 	= new GlobalCROptions (
			BASE_UI, "OPP_CR_OPTIONS", SpecificCROption.BASE_RACE.value);
	public static final ParamBoolean useSelectableAbilities	= new ParamBoolean(
			BASE_UI, "SELECT_CR_OPTIONS", false);
	public static final ParamString  shapeOption3   		= new ParamString(
			BASE_UI, "SHAPE_OPTION_3", "");
	public static final ParamList    shapeOption2   		= new ParamList( // Duplicate Do not add the list
			BASE_UI, "SHAPE_OPTION_2") {
		@Override public String getFromOption() {
			return RotPUI.mergedGuiOptions().selectedGalaxyShapeOption2();
		}
		@Override public void setOption(String newValue) {
			RotPUI.mergedGuiOptions().selectedGalaxyShapeOption2(newValue);
		}
	};
	public static final ParamList    shapeOption1   		= new ParamList( // Duplicate Do not add the list
			BASE_UI, "SHAPE_OPTION_1") {
		@Override public String getFromOption() {
			return RotPUI.mergedGuiOptions().selectedGalaxyShapeOption1();
		}
		@Override public void setOption(String newValue) {
			RotPUI.mergedGuiOptions().selectedGalaxyShapeOption1(newValue);
		}
	};
	public static final ParamList    shapeSelection			= new ParamList( // Duplicate Do not add the list
			BASE_UI, "GALAXY_SHAPE", getGalaxyShapeOptions(),  SHAPE_RECTANGLE) {
		@Override public String     getFromOption() {
			return RotPUI.mergedGuiOptions().selectedGalaxyShape();
		}
		@Override public void setOption(String newValue) {
			RotPUI.mergedGuiOptions().selectedGalaxyShape(newValue);
		}
	};
	public static final ParamList    sizeSelection  		= new ParamList( // Duplicate Do not add the list
			BASE_UI, "GALAXY_SIZE", getGalaxySizeOptions(), SIZE_SMALL) {
		@Override public String getFromOption() {
			return RotPUI.mergedGuiOptions().selectedGalaxySize();
		}
		@Override public void setOption(String newValue) {
			RotPUI.mergedGuiOptions().selectedGalaxySize(newValue);
		}
		@Override public String name(int id) {
			String diffLbl = super.name(id);
			String label   = getLangLabel(id);
			int size = RotPUI.mergedGuiOptions().numberStarSystems(label);
			if (label.equals("SETUP_GALAXY_SIZE_DYNAMIC"))
				diffLbl += " (Variable; now = " + size + ")";
			else
				diffLbl += " (" + size + ")";
			return diffLbl;
		}
		@Override public String realHelp(int id) {
			String label   = getLangLabel(id);
			if (label.equals("SETUP_GALAXY_SIZE_DYNAMIC"))
				return super.realHelp(id);
			if (label.equals("SETUP_GALAXY_SIZE_MAXIMUM"))
				return super.realHelp(id);
			int size = RotPUI.mergedGuiOptions().numberStarSystems(label);
			if (size < 101)
				return langLabel("SETUP_GALAXY_SIZE_MOO1_DESC");
			if (size < 1001)
				return langLabel("SETUP_GALAXY_SIZE_UP1000_DESC");
			return langLabel("SETUP_GALAXY_SIZE_OVER1000_DESC");
		}
	};
	public static final ParamList    difficultySelection	= new ParamList( // Duplicate Do not add the list
			BASE_UI, "GAME_DIFFICULTY", getGameDifficultyOptions(), DIFFICULTY_NORMAL) {
		@Override public String getFromOption() {
			return RotPUI.mergedGuiOptions().selectedGameDifficulty();
		}
		@Override public void setOption(String newValue) {
			RotPUI.mergedGuiOptions().selectedGameDifficulty(newValue);
		}
		@Override public String name(int id) {
			String diffLbl = super.name(id);
			String label   = getLangLabel(id);
			if (label.equals("SETUP_DIFFICULTY_CUSTOM"))
				diffLbl += " (" + Integer.toString(UserPreferences.customDifficulty.get()) + "%)";
			else {
				float modifier = IGameOptions.aiProductionModifier(label);
				diffLbl += " (" + Integer.toString(Math.round(100 * modifier)) + "%)";
			}
			return diffLbl;
		}
	};
	public static final ParamInteger aliensNumber 			= new ParamInteger( // Duplicate Do not add the list
			BASE_UI, "ALIENS_NUMBER", 1, 0, 49, 1, 5, 20, true) {
		@Override public Integer getFromOption() {
			maxValue(RotPUI.mergedGuiOptions().maximumOpponentsOptions());
			return RotPUI.mergedGuiOptions().selectedNumberOpponents();
		}
		@Override public void setOption(Integer newValue) {
			RotPUI.mergedGuiOptions().selectedOpponentRace(newValue, null);
			RotPUI.mergedGuiOptions().selectedNumberOpponents(newValue);
		}
		@Override public Integer defaultValue() {
			return RotPUI.mergedGuiOptions().defaultOpponentsOptions();
		}
	};
	
	public static final LinkedList<InterfaceParam> optionsGalaxy = new LinkedList<>(
			Arrays.asList(
					showNewRaces, globalCROptions, useSelectableAbilities, shapeOption3,
					galaxyRandSource,
					dynStarsPerEmpire // This one is a duplicate, but it helps readability
					));

	// BR: Race Menu addition
	public static final PlayerShipSet playerShipSet		= new PlayerShipSet(
			MOD_UI, "PLAYER_SHIP_SET");
	public static final ParamBoolean  playerIsCustom	= new ParamBoolean(
			BASE_UI, "BUTTON_CUSTOM_PLAYER_RACE", false);
	public static final ParamCR       playerCustomRace	= new ParamCR(
			MOD_UI, baseRaceOptions().getFirst());

	public static final LinkedList<InterfaceParam> optionsRace = new LinkedList<>(
			Arrays.asList(
					playerShipSet, playerIsCustom, playerCustomRace
					));

	// BR: Custom Race Menu
	private static final LinkedList<InterfaceParam> optionsCustomRaceBase = new LinkedList<>(
			Arrays.asList(
					playerIsCustom, playerCustomRace
					));
	public static LinkedList<InterfaceParam> optionsCustomRace() {
		LinkedList<InterfaceParam> list = new LinkedList<>();
		list.addAll(optionsCustomRaceBase);
		return list;
	}
	
	// BR: All the dynamic parameters
	public static LinkedList<InterfaceParam> allModOptions() {
		LinkedList<InterfaceParam> allModOptions = new LinkedList<>();
		allModOptions.addAll(modOptionsStaticA);
		allModOptions.addAll(modOptionsStaticB);
		allModOptions.addAll(modOptionsDynamicA);
		allModOptions.addAll(modOptionsDynamicB);
		allModOptions.addAll(optionsGalaxy);
		allModOptions.addAll(optionsRace);
		allModOptions.addAll(optionsCustomRaceBase);
		allModOptions.addAll(autoFlagOptions);
		allModOptions.addAll(GovernorOptions.governorOptions);
		return allModOptions;
	}
    // Other Global parameters
	public static final ParamString bitmapGalaxyLastFolder = new ParamString(
			BASE_UI, "BITMAP_LAST_FOLDER", Rotp.jarPath());
	
    // All the Global parameters
	private static LinkedList<InterfaceParam> globalOptions() {
		LinkedList<InterfaceParam> globalOptions = new LinkedList<>();
		globalOptions.addAll(modGlobalOptionsUI);
		globalOptions.add(bitmapGalaxyLastFolder);
		return globalOptions;
	}
	
	// Duplicates for Base Advanced Options
	private static final ParamList galaxyAge		= new ParamList( // Duplicate Do not add the list
			ADV_UI, "GALAXY_AGE", getGalaxyAgeOptions(), GALAXY_AGE_NORMAL) {
		@Override public String getFromOption() {
			return RotPUI.mergedGuiOptions().selectedGalaxyAge();
		}
		@Override public void setOption(String newValue) {
			RotPUI.mergedGuiOptions().selectedGalaxyAge(newValue);
		}
	};
	private static final ParamList starDensity		= new ParamList( // Duplicate Do not add the list
			ADV_UI, "STAR_DENSITY", getStarDensityOptions(), STAR_DENSITY_NORMAL) {
		@Override public String getFromOption() {
			return RotPUI.mergedGuiOptions().selectedStarDensityOption();
		}
		@Override public void setOption(String newValue) {
			RotPUI.mergedGuiOptions().selectedStarDensityOption(newValue);
		}
	};
	private static final ParamList nebulae			= new ParamList( // Duplicate Do not add the list
			ADV_UI, "NEBULAE", getNebulaeOptions(), NEBULAE_NORMAL) {
		@Override public String getFromOption() {
			return RotPUI.mergedGuiOptions().selectedNebulaeOption();
		}
		@Override public void setOption(String newValue) {
			RotPUI.mergedGuiOptions().selectedNebulaeOption(newValue);
		}
	};
	private static final ParamList randomEvents		= new ParamList( // Duplicate Do not add the list
			ADV_UI, "RANDOM_EVENTS", getRandomEventOptions(), RANDOM_EVENTS_NO_MONSTERS) {
		@Override public String getFromOption() {
			return RotPUI.mergedGuiOptions().selectedRandomEventOption();
		}
		@Override public void setOption(String newValue) {
			RotPUI.mergedGuiOptions().selectedRandomEventOption(newValue);
		}
	};
	private static final ParamList planetQuality	= new ParamList( // Duplicate Do not add the list
			ADV_UI, "PLANET_QUALITY", getPlanetQualityOptions(), PLANET_QUALITY_NORMAL) {
		@Override public String getFromOption() {
			return RotPUI.mergedGuiOptions().selectedPlanetQualityOption();
		}
		@Override public void setOption(String newValue) {
			RotPUI.mergedGuiOptions().selectedPlanetQualityOption(newValue);
		}
	};
	private static final ParamList terraforming		= new ParamList( // Duplicate Do not add the list
			ADV_UI, "TERRAFORMING", getTerraformingOptions(), TERRAFORMING_NORMAL) {
		@Override public String getFromOption() {
			return RotPUI.mergedGuiOptions().selectedTerraformingOption();
		}
		@Override public void setOption(String newValue) {
			RotPUI.mergedGuiOptions().selectedTerraformingOption(newValue);
		}
	};
	private static final ParamList colonizing		= new ParamList( // Duplicate Do not add the list
			ADV_UI, "COLONIZING", getColonizingOptions(), COLONIZING_NORMAL) {
		@Override public String getFromOption() {
			return RotPUI.mergedGuiOptions().selectedColonizingOption();
		}
		@Override public void setOption(String newValue) {
			RotPUI.mergedGuiOptions().selectedColonizingOption(newValue);
		}
	};
	private static final ParamList councilWin		= new ParamList( // Duplicate Do not add the list
			ADV_UI, "COUNCIL_WIN", getCouncilWinOptions(), COUNCIL_REBELS) {
		@Override public String getFromOption() {
			return RotPUI.mergedGuiOptions().selectedCouncilWinOption();
		}
		@Override public void setOption(String newValue) {
			RotPUI.mergedGuiOptions().selectedCouncilWinOption(newValue);
		}
		@Override protected String descriptionId() {
			return "SETTINGS_COUNCIL_DESC";
		}
	};
	private static final ParamList randomizeAI		= new ParamList( // Duplicate Do not add the list
			ADV_UI, "RANDOMIZE_AI", getRandomizeAIOptions(), RANDOMIZE_AI_NONE) {
		@Override public String getFromOption() {
			return RotPUI.mergedGuiOptions().selectedRandomizeAIOption();
		}
		@Override public void setOption(String newValue) {
			RotPUI.mergedGuiOptions().selectedRandomizeAIOption(newValue);
		}
	};
	private static final ParamList autoplay			= new ParamList( // Duplicate Do not add the list
			ADV_UI, "AUTOPLAY", getAutoplayOptions(), AUTOPLAY_OFF) {
		@Override public String getFromOption() {
			return RotPUI.mergedGuiOptions().selectedAutoplayOption();
		}
		@Override public void setOption(String newValue) {
			RotPUI.mergedGuiOptions().selectedAutoplayOption(newValue);
		}
	};
	private static final ParamList researchRate		= new ParamList( // Duplicate Do not add the list
			ADV_UI, "RESEARCH_RATE", getResearchRateOptions(), RESEARCH_NORMAL) {
		@Override public String getFromOption() {
			return RotPUI.mergedGuiOptions().selectedResearchRate();
		}
		@Override public void setOption(String newValue) {
			RotPUI.mergedGuiOptions().selectedResearchRate(newValue);
		}
	};
	private static final ParamList warpSpeed		= new ParamList( // Duplicate Do not add the list
			ADV_UI, "WARP_SPEED", getWarpSpeedOptions(), WARP_SPEED_NORMAL) {
		@Override public String getFromOption() {
			return RotPUI.mergedGuiOptions().selectedWarpSpeedOption();
		}
		@Override public void setOption(String newValue) {
			RotPUI.mergedGuiOptions().selectedWarpSpeedOption(newValue);
		}
	};
	private static final ParamList fuelRange		= new ParamList( // Duplicate Do not add the list
			ADV_UI, "FUEL_RANGE", getFuelRangeOptions(), FUEL_RANGE_NORMAL) {
		@Override public String getFromOption() {
			return RotPUI.mergedGuiOptions().selectedFuelRangeOption();
		}
		@Override public void setOption(String newValue) {
			RotPUI.mergedGuiOptions().selectedFuelRangeOption(newValue);
			if (GameSession.instance().status().inProgress())
				GameSession.instance().galaxy().resetAllAI();
		}
	};
	private static final ParamList techTrading		= new ParamList( // Duplicate Do not add the list
			ADV_UI, "TECH_TRADING", getTechTradingOptions(), TECH_TRADING_YES) {
		@Override public String getFromOption() {
			return RotPUI.mergedGuiOptions().selectedTechTradeOption();
		}
		@Override public void setOption(String newValue) {
			RotPUI.mergedGuiOptions().selectedTechTradeOption(newValue);
		}
	};
	private static final ParamList aiHostility		= new ParamList( // Duplicate Do not add the list
			ADV_UI, "AI_HOSTILITY", getAiHostilityOptions(), AI_HOSTILITY_NORMAL) {
		@Override public String getFromOption() {
			return RotPUI.mergedGuiOptions().selectedAIHostilityOption();
		}
		@Override public void setOption(String newValue) {
			RotPUI.mergedGuiOptions().selectedAIHostilityOption(newValue);
		}
	};
	private static final ParamBoolean autoColonize_	= new ParamBoolean( // Duplicate Do not add the list
			GAME_UI, "AUTOCOLONIZE", false) {
		@Override public Boolean get() { return autoColonize(); }
		@Override public Boolean set(Boolean newValue) {
			autoColonize(newValue);
			return autoColonize();
		}
	};
	private static final ParamList autoBombard_		= new ParamList( // Duplicate Do not add the list
			GAME_UI, "AUTOBOMBARD",
			Arrays.asList(
					AUTOBOMBARD_NO,
					AUTOBOMBARD_NEVER,
					AUTOBOMBARD_YES,
					AUTOBOMBARD_WAR,
					AUTOBOMBARD_INVADE
					),
			AUTOBOMBARD_NO) {
		@Override public String get() { return autoBombardMode(); }
		@Override public String set(String newValue) {
			autoBombardMode(newValue);
			return autoBombardMode();
		}
	};
	public static final LinkedList<InterfaceParam> advancedOptions = new LinkedList<>(
			Arrays.asList(
					galaxyAge, starDensity, nebulae, planetQuality, terraforming,
					null,
					randomEvents, aiHostility, councilWin, randomizeAI, autoplay,
					null,
					researchRate, warpSpeed, fuelRange, techTrading, colonizing
					));

	private static final ParamTitle headerSpacer = new ParamTitle("SPACER");
	// Sub GUI Lists and GUI List	
	// Parameters on these list are auto saved in dynamic list options
	private static final LinkedList<LinkedList<InterfaceParam>> autoFlagOptionsMap = 
			new LinkedList<LinkedList<InterfaceParam>>();
	static {
		autoFlagOptionsMap.add(new LinkedList<>(Arrays.asList(
				new ParamTitle("AUTO_FLAG_ID_SELECTION"),
				autoFlagAssignation1, autoFlagAssignation2,
				autoFlagAssignation3, autoFlagAssignation4,

				headerSpacer,
				new ParamTitle("AUTO_FLAG_COLONY_TECH"),
				flagTechGaiaColor, flagTechFertileColor, flagTechGoodColor,
				flagTechStandardColor, flagTechBarrenColor, flagTechDeadColor,
				flagTechToxicColor, flagTechRadiatedColor, flagTechNoneColor
				)));
		autoFlagOptionsMap.add(new LinkedList<>(Arrays.asList(
				new ParamTitle("AUTO_FLAG_RESOURCES"),
				flagOrionColor, flagAntaranColor,
				flagUltraRichColor, flagRichColor, flagAssetNormalColor,
				flagPoorColor, flagUltraPoorColor, flagNoneColor,
				headerSpacer, new ParamTitle("AUTO_FLAG_ENVIRONMENT"),
				flagEnvGaiaColor, flagEnvFertileColor,
				flagEnvNormalColor,	flagEnvHostileColor, flagEnvNoneColor
				)));
		autoFlagOptionsMap.add(new LinkedList<>(Arrays.asList(
				new ParamTitle("AUTO_FLAG_TYPE"),
				flagTerranColor, flagJungleColor, flagOceanColor,
				flagAridColor, flagSteppeColor, flagDesertColor, flagMinimalColor,
				flagBarrenColor, flagTundraColor, flagDeadColor,
				flagInfernoColor, flagToxicColor, flagRadiatedColor,
				flagAsteroidColor
				)));
	};
	public static final String		AUTO_FLAG_GUI_ID	= "AUTO_FLAG";
	private static final ParamSubUI	autoFlagOptionsUI	= new ParamSubUI(
			MOD_UI, "AUTO_FLAG_UI", autoFlagOptionsMap,
			"AUTO_FLAG_TITLE", AUTO_FLAG_GUI_ID);
	public static final LinkedList<InterfaceParam> autoFlagOptions = autoFlagOptionsUI.optionsList();

	// GUI Options Lists
	public static final LinkedList<InterfaceParam> mergedStaticOptions	= new LinkedList<>();
	public static final LinkedList<LinkedList<InterfaceParam>> mergedStaticOptionsMap	= 
			new LinkedList<LinkedList<InterfaceParam>>();
	static {
		mergedStaticOptionsMap.add(new LinkedList<>(Arrays.asList(
				new ParamTitle("START_GALAXY_OPTIONS"),
				galaxyAge, starDensity, nebulae, maximizeSpacing,
				spacingLimit, minStarsPerEmpire, prefStarsPerEmpire, dynStarsPerEmpire,

				headerSpacer,
				new ParamTitle("START_PLANET_OPTIONS"),
				planetQuality, minDistArtifactPlanet
				)));
		mergedStaticOptionsMap.add(new LinkedList<>(Arrays.asList(
				new ParamTitle("START_EMPIRE_OPTIONS"),
				artifactsHomeworld, fertileHomeworld, richHomeworld, ultraRichHomeworld,
				companionWorlds, battleScout, randomTechStart, randomizeAI
				)));
		mergedStaticOptionsMap.add(new LinkedList<>(Arrays.asList(
				new ParamTitle("START_TECH_CONTROL"),
				techIrradiated, techCloaking, techStargate, techHyperspace,
				techIndustry2, techThorium, techTransport,

				headerSpacer,
				new ParamTitle("START_RANDOM_ALIENS"),
				randomAlienRacesTargetMax, randomAlienRacesTargetMin, randomAlienRaces,
				randomAlienRacesMax, randomAlienRacesMin, randomAlienRacesSmoothEdges
				)));
		mergedStaticOptionsMap.add(new LinkedList<>(Arrays.asList(
				new ParamTitle("RESTART_OPTIONS"),
				restartChangesPlayerRace, restartChangesPlayerAI,
				restartChangesAliensAI, restartAppliesSettings,

				headerSpacer,
				new ParamTitle("MENU_OPTIONS"),
				useFusionFont, compactOptionOnly
				)));
		for (LinkedList<InterfaceParam> list : mergedStaticOptionsMap) {
			for (InterfaceParam param : list) {
				if (param != null && !param.isTitle())
					mergedStaticOptions.add(param);
			}
		}
	};
	public static final LinkedList<InterfaceParam> mergedDynamicOptions	= new LinkedList<>();
	public static final LinkedList<LinkedList<InterfaceParam>> mergedDynamicOptionsMap	= 
			new LinkedList<LinkedList<InterfaceParam>>();
	static {
		mergedDynamicOptionsMap.add(new LinkedList<>(Arrays.asList(
				new ParamTitle("GAME_DIFFICULTY"),
				difficultySelection, customDifficulty,
				dynamicDifficulty, challengeMode,

				headerSpacer,
				new ParamTitle("GAME_VARIOUS"),
				terraforming, colonizing, researchRate,
				warpSpeed, fuelRange, 

				headerSpacer,
				new ParamTitle("GAME_OTHER"),
				showAlliancesGNN, showLimitedWarnings,
				techExchangeAutoRefuse, autoplay
				)));
		mergedDynamicOptionsMap.add(new LinkedList<>(Arrays.asList(
				new ParamTitle("GAME_RELATIONS"),
				councilWin, counciRequiredPct,
				techTrading, aiHostility,

				headerSpacer,
				new ParamTitle("GAME_COMBAT"),
				retreatRestrictions, retreatRestrictionTurns, missileSizeModifier,
				targetBombard, bombingTarget, autoBombard_, autoColonize_
				)));
		mergedDynamicOptionsMap.add(new LinkedList<>(Arrays.asList(
				new ParamTitle("RANDOM_EVENTS_OPT"),
				randomEvents, eventsStartTurn,
				piratesDelayTurn, piratesReturnTurn, piratesMaxSystems,
				amoebaDelayTurn, amoebaReturnTurn, amoebaMaxSystems,
				crystalDelayTurn, crystalReturnTurn, crystalMaxSystems,

				headerSpacer,
				new ParamTitle("PLANETS_FLAG_OPTIONS"),
				flagColorCount, autoFlagOptionsUI
				)));
		mergedDynamicOptionsMap.add(new LinkedList<>(Arrays.asList(
				new ParamTitle("ZOOM_FACTORS"),
				showFleetFactor, showFlagFactor, showPathFactor,
				showNameMinFont, showInfoFontRatio, mapFontFactor,

				headerSpacer,
				new ParamTitle("MENU_OPTIONS"),
				menuStartup, menuAfterGame, menuLoadGame,
				minListSizePopUp, showGridCircular,
				showTooltips, galaxyPreviewColorStarsSize,
				compactOptionOnly
				)));
		for (LinkedList<InterfaceParam> list : mergedDynamicOptionsMap) {
			for (InterfaceParam param : list) {
				if (param != null && !param.isTitle())
					mergedDynamicOptions.add(param);
			}
		}
	};	
	public static final LinkedList<InterfaceParam> modOptionsStaticA  = new LinkedList<>(
			Arrays.asList(
			artifactsHomeworld, fertileHomeworld, richHomeworld, ultraRichHomeworld,
			null,
			techIrradiated, techCloaking, techStargate, techHyperspace,
			null,
			techIndustry2, techThorium, techTransport, randomTechStart, 
			null,
			companionWorlds, battleScout
			));
	public static final LinkedList<InterfaceParam> modOptionsStaticB  = new LinkedList<>(
			Arrays.asList(
			minStarsPerEmpire, prefStarsPerEmpire, maximizeSpacing, spacingLimit, minDistArtifactPlanet,
			null,
			randomAlienRacesTargetMax, randomAlienRacesTargetMin, randomAlienRaces,
			null,
			randomAlienRacesMax, randomAlienRacesMin, randomAlienRacesSmoothEdges,
			null,
			restartChangesPlayerAI, restartChangesAliensAI, restartAppliesSettings, restartChangesPlayerRace
			));
	public static final LinkedList<InterfaceParam> modOptionsDynamicA = new LinkedList<>(
			Arrays.asList(
				customDifficulty, dynamicDifficulty, challengeMode,
				null,
				missileSizeModifier, retreatRestrictions, retreatRestrictionTurns,
				null,
				bombingTarget, targetBombard, flagColorCount, autoFlagOptionsUI
			));
	public static final LinkedList<InterfaceParam> modOptionsDynamicB = new LinkedList<>(
			Arrays.asList(
				eventsStartTurn, counciRequiredPct,
				null,
				amoebaDelayTurn, amoebaMaxSystems, amoebaReturnTurn,
				null,
				crystalDelayTurn, crystalMaxSystems, crystalReturnTurn,
				null,
				piratesDelayTurn, piratesMaxSystems, piratesReturnTurn
			));
	// This list is used by the ModGlobalOptionsUI menu
	public static final LinkedList<InterfaceParam> modGlobalOptionsUI = new LinkedList<>(
			Arrays.asList(
			menuStartup, menuAfterGame, menuLoadGame, minListSizePopUp, showAlliancesGNN,
			null,
			showGridCircular, showTooltips, galaxyPreviewColorStarsSize, showLimitedWarnings, techExchangeAutoRefuse,
			null,
			showFleetFactor, showFlagFactor, showPathFactor, useFusionFont,
			null,
			showNameMinFont, showInfoFontRatio, mapFontFactor, showNextCouncil, compactOptionOnly
			));

	// Only used for displaying help... Do not include to UI list.
//	public static final ParamList opponentAI = new ParamList( // For Help Do not add the list
//			BASE_UI, "OPPONENT_AI",
//			MOO1GameOptions.getOpponentAIOptions(),
//			OPPONENT_AI_CRUEL) {
//		@Override public String	get()	{
//			return RotPUI.mergedGuiOptions().selectedOpponentAIOption();
//		}
//		@Override public String	guideValue()	{ return langLabel(get()); }
//	};
//	public static final ParamList specificAI = new ParamList( // For Help Do not add the list
//			BASE_UI, "SPECIFIC_AI",
//			MOO1GameOptions.getSpecificOpponentAIOptions(),
//			OPPONENT_AI_CRUEL) {
//		@Override public String	get()	{
//			System.out.println("mouseBoxIndex() = " + mouseBoxIndex());
//			return RotPUI.mergedGuiOptions().specificOpponentAIOption(mouseBoxIndex());
//		}
//		@Override public String	guideValue()	{ return langLabel(get()); }
//	};
	public static final ParamButtonHelp userButtonHelp = new ParamButtonHelp( // For Help Do not add the list
			"SETTINGS_BUTTON_USER",
			"SETTINGS_GLOBAL_USER_SET",
			"SETTINGS_LOCAL_USER_SET",
			"SETTINGS_GLOBAL_USER_SAVE",
			"SETTINGS_LOCAL_USER_SAVE");
	public static final ParamButtonHelp lastButtonHelp = new ParamButtonHelp( // For Help Do not add the list
			"SETTINGS_BUTTON_LAST",
			"SETTINGS_GLOBAL_LAST_SET",
			"SETTINGS_LOCAL_LAST_SET",
			"SETTINGS_GLOBAL_LAST_GAME",
			"SETTINGS_LOCAL_LAST_GAME");
	public static final ParamButtonHelp defaultButtonHelp = new ParamButtonHelp( // For Help Do not add the list
			"SETTINGS_BUTTON_DEFAULT",
			"SETTINGS_GLOBAL_DEFAULT",
			"SETTINGS_LOCAL_DEFAULT",
			"SETTINGS_GLOBAL_RESTORE",
			"SETTINGS_LOCAL_RESTORE");
	public static final ParamButtonHelp startButtonHelp = new ParamButtonHelp( // For Help Do not add the list
			"SETUP_START",
			"SETUP_BUTTON_START",
			"",
			"SETUP_BUTTON_RESTART",
			"");
	
	private static boolean showMemory  = false;
	private static boolean playMusic   = true;
	private static boolean playSounds  = true;
	private static int musicVolume     = 10;
	private static int soundVolume     = 10;
	private static int defaultMaxBases = 0;
	private static boolean displayYear = false;
	private static boolean governorOnByDefault        = true; // BR:
	private static boolean governorAutoSpendByDefault = false;
	private static boolean legacyGrowth      = true; // BR:
	private static boolean governorAutoApply = true; // BR:
	private static boolean autoColonize      = false;
	private static boolean divertColonyExcessToResearch = true;
	private static boolean disableAdvisor = true;
	private static String autoBombardMode = AUTOBOMBARD_NO;
	private static String displayMode     = BORDERLESS_MODE;
	private static String graphicsMode    = GRAPHICS_HIGH;
	private static String texturesMode    = TEXTURES_BOTH;
	private static String sensitivityMode = SENSITIVITY_MEDIUM;
	private static String saveDir = "";
	private static float uiTexturePct = 0.20f;
	private static int screenSizePct = 93;
	private static int backupTurns = 5; // modnar: change default turns between backups to 5

	/**
	 * Advanced setting GUI Default Button Action
	 */
	public static void setToDefault() {
		autoColonize = false;
		autoBombardMode = AUTOBOMBARD_NO;
		displayMode = WINDOW_MODE;
		graphicsMode = GRAPHICS_HIGH;
		texturesMode = TEXTURES_BOTH;
		sensitivityMode = SENSITIVITY_MEDIUM;
		screenSizePct = 93;
		backupTurns = 5; // modnar: change default turns between backups to 5
		saveDir = "";
		uiTexturePct = 0.20f;
		showMemory = false;
		if (!playMusic)
			SoundManager.current().toggleMusic();
		if (!playSounds)
			SoundManager.current().toggleSounds();
		musicVolume = 10;
		soundVolume = 10;
		SoundManager.current().resetSoundVolumes();
		save();
	}
	public static void setForNewGame() {
		autoColonize = false;
		autoBombardMode = AUTOBOMBARD_NO;
	}
	public static int musicVolume()		   { return musicVolume; }
	public static int soundVolume()		   { return soundVolume; }
	public static boolean showMemory()	   { return showMemory; }
	public static void toggleMemory()	   { showMemory = !showMemory; save(); }
	public static boolean fullScreen()	   { return displayMode.equals(FULLSCREEN_MODE); }
	public static boolean windowed()	   { return displayMode.equals(WINDOW_MODE); }
	public static boolean borderless()	   { return displayMode.equals(BORDERLESS_MODE); }
	public static String displayMode()	   { return displayMode; }
	public static void toggleDisplayMode() {
		switch(displayMode) {
			case WINDOW_MODE:	  displayMode = BORDERLESS_MODE; break;
			case BORDERLESS_MODE: displayMode = FULLSCREEN_MODE; break;
			case FULLSCREEN_MODE: displayMode = WINDOW_MODE; break;
			default:			  displayMode = WINDOW_MODE; break;
		}
		save();
	}
	public static String graphicsMode()	 { return graphicsMode; }
	public static void toggleGraphicsMode()   {
		switch(graphicsMode) {
			case GRAPHICS_HIGH:   graphicsMode = GRAPHICS_MEDIUM; break;
			case GRAPHICS_MEDIUM: graphicsMode = GRAPHICS_LOW; break;
			case GRAPHICS_LOW:	  graphicsMode = GRAPHICS_HIGH; break;
			default :			  graphicsMode = GRAPHICS_HIGH; break;
		}
		save();
	}
	public static void toggleTexturesMode()   {
		switch(texturesMode) {
			case TEXTURES_NO:		 texturesMode = TEXTURES_INTERFACE; break;
			case TEXTURES_INTERFACE: texturesMode = TEXTURES_MAP; break;
			case TEXTURES_MAP:	     texturesMode = TEXTURES_BOTH; break;
			case TEXTURES_BOTH:	     texturesMode = TEXTURES_NO; break;
			default :				 texturesMode = TEXTURES_BOTH; break;
		}
		save();
	}
	public static String texturesMode()	 { return texturesMode; }
	public static boolean texturesInterface() { return texturesMode.equals(TEXTURES_INTERFACE) || texturesMode.equals(TEXTURES_BOTH); }
	public static boolean texturesMap()	   { return texturesMode.equals(TEXTURES_MAP) || texturesMode.equals(TEXTURES_BOTH); }

	public static void toggleSensitivityMode()   {
		switch(sensitivityMode) {
			case SENSITIVITY_LOW:	   sensitivityMode = SENSITIVITY_MEDIUM; break;
			case SENSITIVITY_MEDIUM:	sensitivityMode = SENSITIVITY_HIGH; break;
			case SENSITIVITY_HIGH:	  sensitivityMode = SENSITIVITY_LOW; break;
			default :				   sensitivityMode = SENSITIVITY_MEDIUM; break;
		}
		save();
	}
	public static String sensitivityMode()	  { return sensitivityMode; }
	// public static boolean sensitivityHigh()	  { return sensitivityMode.equals(SENSITIVITY_HIGH); }
	public static boolean sensitivityMedium() { return sensitivityMode.equals(SENSITIVITY_MEDIUM); }
	public static boolean sensitivityLow()	  { return sensitivityMode.equals(SENSITIVITY_LOW); }

	// public static String autoColonizeMode()	  { return autoColonize ? AUTOCOLONIZE_YES : AUTOCOLONIZE_NO; }
	public static void toggleAutoColonize()	  { autoColonize = !autoColonize; save();  }
	public static boolean autoColonize()	  { return autoColonize; }
	private static void autoColonize(boolean val)  { autoColonize = val; }

	public static void toggleAutoBombard()	  {
		switch(autoBombardMode) {
			case AUTOBOMBARD_NO:	 autoBombardMode = AUTOBOMBARD_NEVER; break;
			case AUTOBOMBARD_NEVER:  autoBombardMode = AUTOBOMBARD_YES; break;
			case AUTOBOMBARD_YES:	 autoBombardMode = AUTOBOMBARD_WAR; break;
			case AUTOBOMBARD_WAR:	 autoBombardMode = AUTOBOMBARD_INVADE; break;
			case AUTOBOMBARD_INVADE: autoBombardMode = AUTOBOMBARD_NO; break;
			default:				 autoBombardMode = AUTOBOMBARD_NO; break;
		}
		save();
	}
	private static void autoBombardMode(String val)   { autoBombardMode = val; }
	public static String autoBombardMode()   { return autoBombardMode; }
	// public static boolean autoBombardNo()    { return autoBombardMode.equals(AUTOBOMBARD_NO); }
	public static boolean autoBombardNever() { return autoBombardMode.equals(AUTOBOMBARD_NEVER); }
	public static boolean autoBombardYes()   { return autoBombardMode.equals(AUTOBOMBARD_YES); }
	public static boolean autoBombardWar()   { return autoBombardMode.equals(AUTOBOMBARD_WAR); }
	public static boolean autoBombardInvading() { return autoBombardMode.equals(AUTOBOMBARD_INVADE); }

	public static boolean playAnimations() { return !graphicsMode.equals(GRAPHICS_LOW); }
	public static boolean antialiasing() { return graphicsMode.equals(GRAPHICS_HIGH); }
	public static boolean playSounds()   { return playSounds; }
	public static void toggleSounds()    { playSounds = !playSounds;	save(); }
	public static boolean playMusic()    { return playMusic; }
	public static void toggleMusic()     { playMusic = !playMusic; save();  }
	// BR: redirection for compatibility
	public static int companionWorlds()			{ return Math.abs(companionWorlds.get()); } // modnar: add option to start game with additional colonies
	public static int companionWorldsSigned()	{ return companionWorlds.get(); } // BR: to manage old and new distribution
	public static float missileSizeModifier()	{ return missileSizeModifier.get(); } 
	public static int retreatRestrictions()		{ return retreatRestrictions.getIndex(); }
	public static int retreatRestrictionTurns()	{ return retreatRestrictionTurns.get(); }
	public static boolean targetBombardAllowedForAI() {
		switch (targetBombard.get()) {
			case  "Both":
			case  "AI":
				return true;
			default:
				return false;
		}
	}
	public static boolean targetBombardAllowedForPlayer() {
		switch (targetBombard.get()) {
			case  "Both":
			case  "Player":
				return true;
			default:
				return false;
		}
	}
	// \BR:
	public static int screenSizePct()       { return screenSizePct; }
	private static void screenSizePct(int i) { setScreenSizePct(i); }
	public static String saveDirectoryPath() {
		if (saveDir.isEmpty())
			return Rotp.jarPath();
		else
			return saveDir;
	}
	public static String backupDirectoryPath() {
		return saveDirectoryPath()+"/"+GameSession.BACKUP_DIRECTORY;
	}
	public static String saveDir()       { return saveDir; }
	public static void saveDir(String s) { saveDir = s; save(); }
	public static String saveDirStr()    {
		if (saveDir.isEmpty())
			return SAVEDIR_DEFAULT;
		else
			return SAVEDIR_CUSTOM;
	}
	public static int backupTurns() { return backupTurns; }
	public static boolean backupTurns(int i) {
		int prev = backupTurns;
		backupTurns = Math.min(Math.max(0,i),MAX_BACKUP_TURNS);
		save();
		return prev != backupTurns;
	}
	public static void toggleBackupTurns() {
		if ((backupTurns >= MAX_BACKUP_TURNS) || (backupTurns < 0)) // modnar: add negative check
			backupTurns = 0;
		else // modnar: change backupTurns to be: 0, 1, 5 ,10, 20
			backupTurns = (int) Math.round(1.0f + 4.87f*backupTurns - 0.93f*Math.pow(backupTurns, 2) + 0.063f*Math.pow(backupTurns, 3) );
		save();
	}
	public static void toggleYearDisplay()	{ displayYear = !displayYear; save(); }
	public static boolean displayYear()	   { return displayYear; }
	// public static void setDefaultMaxBases(int bases)	{ defaultMaxBases = bases; }
	public static int defaultMaxBases()	{ return defaultMaxBases; }
	public static void setGovernorOn(boolean governorOn)	{ governorOnByDefault = governorOn; save(); } // BR:
	public static boolean governorOnByDefault() { return governorOnByDefault; }
	public static void setAutoSpendOn(boolean autospendOn)  { governorAutoSpendByDefault = autospendOn; save(); }
	public static boolean governorAutoSpendByDefault() { return governorAutoSpendByDefault; }
	public static void setLegacyGrowth(boolean legacy_Growth)  { legacyGrowth = legacy_Growth; save(); } // BR:
	public static boolean legacyGrowth() { return legacyGrowth; } // BR:
	public static void setGovernorAutoApply(boolean auto_Apply)  { governorAutoApply = auto_Apply; save(); } // BR:
	public static boolean governorAutoApply() { return governorAutoApply; } // BR:
	// public static void setDivertColonyExcessToResearch(boolean divertOn)  {divertColonyExcessToResearch = divertOn; save(); }
	public static boolean divertColonyExcessToResearch()  { return divertColonyExcessToResearch; }
	public static boolean disableAdvisor()			{ return disableAdvisor; }
	private static void uiTexturePct(int i)			{ uiTexturePct = i / 100.0f; }
	static float uiTexturePct()						{ return uiTexturePct; }
	static boolean gamePlayed()						{return gamePlayed; }
	public static void gamePlayed(boolean played)	{ gamePlayed = played; }
	static boolean loadRequest()			  		{return loadRequest; }
	public static void loadRequest(boolean load)	{
		if (load) {
			// For games launched without going to the GUI
			// The "backGround newGameOptions() will then be set
			loadRequest = load;
			RotPUI.createNewOptions(); // This will reset loadRequest
		} // not else Because SetupRaceUI will do a createNewGameOptions()
		loadRequest = load;
	}
	static void loadAndSave() {
		load();
		save();
	}
	@SuppressWarnings("null")
	public static void load() {
		String path = Rotp.jarPath();
		File configFile = new File(path, PREFERENCES_FILE);
		// modnar: change to InputStreamReader, force UTF-8
		try ( BufferedReader in = new BufferedReader( new InputStreamReader( new FileInputStream(configFile), "UTF-8"));) {
			String input;
			if (in != null) {
				while ((input = in.readLine()) != null)
					loadPreferenceLine(input.trim());
			}
		}
		catch (FileNotFoundException e) {
			System.err.println(path+PREFERENCES_FILE+" not found.");
		}
		catch (IOException e) {
			System.err.println("UserPreferences.load -- IOException: "+ e.toString());
		}
	}
	public static int save() {
		String path = Rotp.jarPath();
		try (FileOutputStream fout = new FileOutputStream(new File(path, PREFERENCES_FILE));
			// modnar: change to OutputStreamWriter, force UTF-8
			PrintWriter out = new PrintWriter(new OutputStreamWriter(fout, "UTF-8")); ) {
			out.println("===== Base ROTP Settings =====");
			out.println();
			out.println(keyFormat("DISPLAY_MODE")+displayModeToSettingName(displayMode));
			out.println(keyFormat("GRAPHICS")+graphicsModeToSettingName(graphicsMode));
			out.println(keyFormat("MUSIC")+ yesOrNo(playMusic));
			out.println(keyFormat("SOUNDS")+ yesOrNo(playSounds));
			out.println(keyFormat("MUSIC_VOLUME")+ musicVolume);
			out.println(keyFormat("SOUND_VOLUME")+ soundVolume);
			out.println(keyFormat("SAVE_DIR")+ saveDir);
			out.println(keyFormat("BACKUP_TURNS")+ backupTurns);
			out.println(keyFormat("AUTOCOLONIZE")+ yesOrNo(autoColonize));
			out.println(keyFormat("AUTOBOMBARD")+autoBombardToSettingName(autoBombardMode));
			out.println(keyFormat("TEXTURES")+texturesToSettingName(texturesMode));
			out.println(keyFormat("SENSITIVITY")+sensitivityToSettingName(sensitivityMode));
			out.println(keyFormat("SHOW_MEMORY")+ yesOrNo(showMemory));
			out.println(keyFormat("DISPLAY_YEAR")+ yesOrNo(displayYear));
			out.println(keyFormat("SCREEN_SIZE_PCT")+ screenSizePct());
			out.println(keyFormat("UI_TEXTURE_LEVEL")+(int) (uiTexturePct()*100));
			out.println(keyFormat("DISABLE_ADVISOR") + yesOrNo(disableAdvisor));
			out.println(keyFormat("LANGUAGE")+ languageDir());
			// BR: Governors GUI
			out.println();
			out.println("===== Governor Settings =====");
			out.println();
			out.println(keyFormat("DEFAULT_MAX_BASES") + defaultMaxBases);
			out.println(keyFormat("GOVERNOR_ON_BY_DEFAULT") + yesOrNo(governorOnByDefault));
			out.println(keyFormat("AUTOSPEND_ON_BY_DEFAULT") + yesOrNo(governorAutoSpendByDefault));
			out.println(keyFormat("DIVERT_COLONY_EXCESS_TO_RESEARCH")+ yesOrNo(divertColonyExcessToResearch));
			out.println(keyFormat("LEGACY_GROWTH") + yesOrNo(legacyGrowth)); // BR:
			out.println(keyFormat("GOVERNOR_AUTO_APPLY") + yesOrNo(governorAutoApply)); // BR:

			out.println();
			out.println("===== MOD Global GUI Settings =====");
			out.println();
			for (InterfaceParam param : globalOptions()) {
				if (param != null)
					out.println(keyFormat(param.getCfgLabel()) + param.getCfgValue());
			}
			return 0;
		}
		catch (IOException e) {
			System.err.println("UserPreferences.save -- IOException: "+ e.toString());
			return -1;
		}
	}

	private static String keyFormat(String s)  { return String.format(keyFormat, s); }

	private static void loadPreferenceLine(String line) {
		if (line.isEmpty())
			return;

		String[] args = line.split(":");
		if (args.length < 2)
			return;

		String key = args[0].toUpperCase().trim();
		String val = args[1].trim();
		// for values that may have embedded :, like the save dir path
		String fullVal = val;
		for (int i=2;i<args.length;i++)
			fullVal = fullVal+":"+args[i];
		if (key.isEmpty() || val.isEmpty())
				return;

		if (Rotp.logging)
			System.out.println("Key:"+key+"  value:"+val);
		switch(key) {
			case "DISPLAY_MODE": displayMode = displayModeFromSettingName(val); return;
			case "GRAPHICS":     graphicsMode = graphicsModeFromSettingName(val); return;
			case "MUSIC":        playMusic = yesOrNo(val); return;
			case "SOUNDS":       playSounds = yesOrNo(val); return;
			case "MUSIC_VOLUME": setMusicVolume(val); return;
			case "SOUND_VOLUME": setSoundVolume(val); return;
			case "SAVE_DIR":     saveDir  = fullVal.trim(); return;
			case "BACKUP_TURNS": backupTurns  = Integer.valueOf(val); return;
			case "AUTOCOLONIZE": autoColonize = yesOrNo(val); return;
			case "AUTOBOMBARD":  autoBombardMode = autoBombardFromSettingName(val); return;
			case "TEXTURES":     texturesMode = texturesFromSettingName(val); return;
			case "SENSITIVITY":  sensitivityMode = sensitivityFromSettingName(val); return;
			case "SHOW_MEMORY":  showMemory = yesOrNo(val); return;
			case "DISPLAY_YEAR": displayYear = yesOrNo(val); return;
			case "DEFAULT_MAX_BASES": defaultMaxBases = Integer.valueOf(val); return;
			case "GOVERNOR_ON_BY_DEFAULT":  governorOnByDefault = yesOrNo(val); return;
			case "AUTOSPEND_ON_BY_DEFAULT": governorAutoSpendByDefault = yesOrNo(val); return;
			case "DIVERT_COLONY_EXCESS_TO_RESEARCH": divertColonyExcessToResearch = yesOrNo(val); return;
			case "LEGACY_GROWTH": legacyGrowth = yesOrNo(val); return; // BR:
			case "GOVERNOR_AUTO_APPLY": governorAutoApply = yesOrNo(val); return; // BR:
			case "DISABLE_ADVISOR": disableAdvisor = yesOrNo(val); return;
			case "SCREEN_SIZE_PCT": screenSizePct(Integer.valueOf(val)); return;
			case "UI_TEXTURE_LEVEL": uiTexturePct(Integer.valueOf(val)); return;
			case "LANGUAGE": selectLanguage(val); return;
			default:
			// BR: Global Mod GUI
				for (InterfaceParam param : globalOptions()) {
					if (param != null 
							&& key.equalsIgnoreCase(param.getCfgLabel())) {
						if (param instanceof ParamString)
							param.setFromCfgValue(fullVal.trim());
						else
							param.setFromCfgValue(val);
						break;
					}
				}
				return;
		}
	}
	private static String yesOrNo(boolean b) {
		return b ? "YES" : "NO";
	}
	private static boolean yesOrNo(String s) {
		return s.equalsIgnoreCase("YES");
	}
	private static void selectLanguage(String s) {
		LanguageManager.selectLanguage(s);
	}
	private static void setMusicVolume(String s) {
		int val = Integer.valueOf(s);
		musicVolume = Math.max(0, Math.min(10,val));
	}
	private static void setSoundVolume(String s) {
		int val = Integer.valueOf(s);
		soundVolume = Math.max(0, Math.min(10,val));
	}
	private static String languageDir() {
		return LanguageManager.selectedLanguageDir();
	}
	private static void setScreenSizePct(int i) {
		screenSizePct = Math.max(50,Math.min(i,100));
	}
	public static boolean shrinkFrame() {
		int oldSize = screenSizePct;
		setScreenSizePct(screenSizePct-5);
		return oldSize != screenSizePct;
	}
	public static boolean expandFrame() {
		int oldSize = screenSizePct;
		setScreenSizePct(screenSizePct+5);
		return oldSize != screenSizePct;
	}
	private static String displayModeToSettingName(String s) {
		switch(s) {
			case WINDOW_MODE:	  return "Windowed";
			case BORDERLESS_MODE: return "Borderless";
			case FULLSCREEN_MODE: return "Fullscreen";
		}
		return "Windowed";
	}
	private static String displayModeFromSettingName(String s) {
		switch(s) {
			case "Windowed":   return WINDOW_MODE;
			case "Borderless": return BORDERLESS_MODE;
			case "Fullscreen": return FULLSCREEN_MODE;
		}
		return WINDOW_MODE;
	}
	private static String graphicsModeToSettingName(String s) {
		switch(s) {
			case GRAPHICS_LOW:	  return "Low";
			case GRAPHICS_MEDIUM: return "Medium";
			case GRAPHICS_HIGH:   return "High";
		}
		return "High";
	}
	private static String graphicsModeFromSettingName(String s) {
		switch(s) {
			case "Low":	   return GRAPHICS_LOW;
			case "Medium": return GRAPHICS_MEDIUM;
			case "High":   return GRAPHICS_HIGH;
		}
		return GRAPHICS_HIGH;
	}
	private static String autoBombardToSettingName(String s) {
		switch(s) {
			case AUTOBOMBARD_NO:	 return "No";
			case AUTOBOMBARD_NEVER:  return "Never";
			case AUTOBOMBARD_YES:	 return "Yes";
			case AUTOBOMBARD_WAR:	 return "War";
			case AUTOBOMBARD_INVADE: return "Invade";
		}
		return "No";
	}
	private static String autoBombardFromSettingName(String s) {
		switch(s) {
			case "No":	   return AUTOBOMBARD_NO;
			case "Never":  return AUTOBOMBARD_NEVER;
			case "Yes":    return AUTOBOMBARD_YES;
			case "War":	   return AUTOBOMBARD_WAR;
			case "Invade": return AUTOBOMBARD_INVADE;
		}
		return AUTOBOMBARD_NO;
	}
	private static String texturesToSettingName(String s) {
		switch(s) {
			case TEXTURES_NO:		 return "No";
			case TEXTURES_INTERFACE: return "Interface";
			case TEXTURES_MAP:	     return "Map";
			case TEXTURES_BOTH:	     return "Both";
		}
		return "Both";
	}
	private static String texturesFromSettingName(String s) {
		switch(s) {
			case "No":        return TEXTURES_NO;
			case "Interface": return TEXTURES_INTERFACE;
			case "Map":       return TEXTURES_MAP;
			case "Both":      return TEXTURES_BOTH;
		}
		return TEXTURES_BOTH;
	}
	private static String sensitivityToSettingName(String s) {
		switch(s) {
			case SENSITIVITY_HIGH:   return "High";
			case SENSITIVITY_MEDIUM: return "Medium";
			case SENSITIVITY_LOW:    return "Low";
		}
		return "Medium";
	}
	private static String sensitivityFromSettingName(String s) {
		switch(s) {
			case "High":   return SENSITIVITY_HIGH;
			case "Medium": return SENSITIVITY_MEDIUM;
			case "Low":    return SENSITIVITY_LOW;
		}
		return SENSITIVITY_MEDIUM;
	}
	public static void increaseMusicLevel()	{
		musicVolume = Math.min(10, musicVolume+1);
		SoundManager.current().resetMusicVolumes();
		save();
	}
	public static void decreaseMusicLevel()	{
		musicVolume = Math.max(0, musicVolume-1);
		SoundManager.current().resetMusicVolumes();
		save();
	};
	public static void increaseSoundLevel()	{
		soundVolume = Math.min(10, soundVolume+1);
		SoundManager.current().resetSoundVolumes();
		save();
	}
	public static void decreaseSoundLevel()	{
		soundVolume = Math.max(0, soundVolume-1);
		SoundManager.current().resetSoundVolumes();
		save();
	}
}
