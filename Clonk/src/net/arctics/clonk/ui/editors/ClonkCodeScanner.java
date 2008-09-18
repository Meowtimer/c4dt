package net.arctics.clonk.ui.editors;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import net.arctics.clonk.ClonkCore;
import net.arctics.clonk.parser.BuiltInDefinitions;
import net.arctics.clonk.parser.C4Type;

import org.eclipse.jface.text.rules.*;
import org.eclipse.jface.text.*;

public class ClonkCodeScanner extends RuleBasedScanner {

	/**
	 * Rule to detect clonk operators.
	 *
	 */
	private static final class OperatorRule implements IRule {

		/** Clonk operators */
		private final char[] CLONK_OPERATORS= { ':', ';', '.', '=', '/', '\\', '+', '-', '*', '<', '>', '?', '!', ',', '|', '&', '^', '%', '~'};
		/** Token to return for this rule */
		private final IToken fToken;

		/**
		 * Creates a new operator rule.
		 *
		 * @param token Token to use for this rule
		 */
		public OperatorRule(IToken token) {
			fToken= token;
		}

		/**
		 * Is this character an operator character?
		 *
		 * @param character Character to determine whether it is an operator character
		 * @return <code>true</code> if the character is an operator, <code>false</code> otherwise.
		 */
		public boolean isOperator(char character) {
			for (int index= 0; index < CLONK_OPERATORS.length; index++) {
				if (CLONK_OPERATORS[index] == character)
					return true;
			}
			return false;
		}

		/*
		 * @see org.eclipse.jface.text.rules.IRule#evaluate(org.eclipse.jface.text.rules.ICharacterScanner)
		 */
		public IToken evaluate(ICharacterScanner scanner) {

			int character= scanner.read();
			if (isOperator((char) character)) {
				do {
					character= scanner.read();
				} while (isOperator((char) character));
				scanner.unread();
				return fToken;
			} else {
				scanner.unread();
				return Token.UNDEFINED;
			}
		}
	}

	/**
	 * Rule to detect java brackets.
	 *
	 * @since 3.3
	 */
	private static final class BracketRule implements IRule {

		/** Java brackets */
		private final char[] JAVA_BRACKETS= { '(', ')', '{', '}', '[', ']' };
		/** Token to return for this rule */
		private final IToken fToken;

		/**
		 * Creates a new bracket rule.
		 *
		 * @param token Token to use for this rule
		 */
		public BracketRule(IToken token) {
			fToken= token;
		}

		/**
		 * Is this character a bracket character?
		 *
		 * @param character Character to determine whether it is a bracket character
		 * @return <code>true</code> if the character is a bracket, <code>false</code> otherwise.
		 */
		public boolean isBracket(char character) {
			for (int index= 0; index < JAVA_BRACKETS.length; index++) {
				if (JAVA_BRACKETS[index] == character)
					return true;
			}
			return false;
		}

		/*
		 * @see org.eclipse.jface.text.rules.IRule#evaluate(org.eclipse.jface.text.rules.ICharacterScanner)
		 */
		public IToken evaluate(ICharacterScanner scanner) {

			int character= scanner.read();
			if (isBracket((char) character)) {
				do {
					character= scanner.read();
				} while (isBracket((char) character));
				scanner.unread();
				return fToken;
			} else {
				scanner.unread();
				return Token.UNDEFINED;
			}
		}
	}
	
	public static Map<String,IToken> fTokenMap= new HashMap<String, IToken>();
	static String[] fgKeywords= {
		"break", 
		"continue",
		"const",
		"do", 
		"else",
		"for", "func", 
		"global",
		"if",
		"local",
		"private", "protected", "public", 
		"static",
		"this",
		"var",
		"while"
	};

	private static final String RETURN= "return"; //$NON-NLS-1$

	private static String[] fgTypes= { "any", "array", "bool", "dword", "id", "int", "object", "string" }; //$NON-NLS-1$ //$NON-NLS-5$ //$NON-NLS-7$ //$NON-NLS-6$ //$NON-NLS-8$ //$NON-NLS-9$  //$NON-NLS-10$ //$NON-NLS-3$ //$NON-NLS-4$ //$NON-NLS-2$

	private static String[] fgConstants= { "false", "null", "true" }; //$NON-NLS-3$ //$NON-NLS-2$ //$NON-NLS-1$

//	private static String[] fgBuiltInFunctions = {"CreateObject","FindObjects"};
	
	private static String[] fgDirectives = {"include", "strict", "appendto"};
	
	public final static String KEYWORDS = "__keywords";
	
	private IRule[] currentRules;
	
	public ClonkCodeScanner(ColorManager manager) {
		
		

		
		IToken defaultToken = new Token(new TextAttribute(manager.getColor(IClonkColorConstants.DEFAULT)));
		
		IToken operator = new Token(new TextAttribute(manager.getColor(IClonkColorConstants.OPERATOR)));
		IToken keyword = new Token(new TextAttribute(manager.getColor(IClonkColorConstants.KEYWORD)));
		IToken type = new Token(new TextAttribute(manager.getColor(IClonkColorConstants.TYPE)));
		IToken engineFunction = new Token(new TextAttribute(manager.getColor(IClonkColorConstants.ENGINE_FUNCTION)));
		IToken objCallbackFunction = new Token(new TextAttribute(manager.getColor(IClonkColorConstants.OBJ_CALLBACK)));
		IToken string = new Token(new TextAttribute(manager.getColor(IClonkColorConstants.STRING)));
//		IToken number = new Token(new TextAttribute(manager.getColor(IClonkColorConstants.NUMBER)));
		IToken bracket = new Token(new TextAttribute(manager.getColor(IClonkColorConstants.BRACKET)));
		IToken returnToken = new Token(new TextAttribute(manager.getColor(IClonkColorConstants.RETURN)));
		IToken pragma = new Token(new TextAttribute(manager.getColor(IClonkColorConstants.PRAGMA)));
		
//		fTokenMap.put(ClonkScriptPartitionScanner.C4S_STRING, string);
		
		List<IRule> rules = new ArrayList<IRule>();
		
		rules.add(new SingleLineRule("\"", "\"", string, '\\'));
		
		// Add generic whitespace rule.
		rules.add(new WhitespaceRule(new ClonkWhitespaceDetector()));
		

		// Add rule for operators
		rules.add(new OperatorRule(operator));

		// Add rule for brackets
		rules.add(new BracketRule(bracket));

		WordScanner wordDetector= new WordScanner();
		CombinedWordRule combinedWordRule= new CombinedWordRule(wordDetector, defaultToken);
		
		// Add word rule for keyword 'return'.
		CombinedWordRule.WordMatcher returnWordRule= new CombinedWordRule.WordMatcher();
		returnWordRule.addWord(RETURN, returnToken);  
		combinedWordRule.addWordMatcher(returnWordRule);

		// Add word rule for keywords, types, and constants.
		CombinedWordRule.WordMatcher wordRule= new CombinedWordRule.WordMatcher();
//		for (int i=0; i<fgKeywords.length; i++)
//			wordRule.addWord(fgKeywords[i], keyword);
		for (String c4keyword : BuiltInDefinitions.KEYWORDS)
			wordRule.addWord(c4keyword.trim(), keyword);
		for (String c4keyword : BuiltInDefinitions.DECLARATORS)
			wordRule.addWord(c4keyword.trim(), keyword);
//		for (int i=0; i<fgTypes.length; i++)
//			wordRule.addWord(fgTypes[i], type);
		for (C4Type c4type : C4Type.values()) 
			wordRule.addWord(c4type.name().trim(), type);
		for (int i=0; i<fgConstants.length; i++)
			wordRule.addWord(fgConstants[i], type);
		for (int i=0; i<ClonkCore.ENGINE_FUNCTIONS.size(); i++)
			wordRule.addWord(ClonkCore.ENGINE_FUNCTIONS.get(i).getName(), engineFunction);
		for (int i=0; i<BuiltInDefinitions.OBJECT_CALLBACKS.length; i++)
			wordRule.addWord(BuiltInDefinitions.OBJECT_CALLBACKS[i], objCallbackFunction);
		
		
		combinedWordRule.addWordMatcher(wordRule);
		
		rules.add(combinedWordRule);
		
		rules.add(new PragmaRule(fgDirectives,pragma));
		
//		for (int i=0; i<fgDirectives.length; i++)
//			rules.add(new PatternRule("#" + fgDirectives[i]," ",pragma,(char)0,true));
		
//		WordRule engineFunctionRule = new WordRule(new WordScanner());
		
		//rules.add(new NumberRule(number));
		
//		String engineFunctions = "Abs AbsX AbsY ActIdle Activate AddCommand AddEffect AddMenuItem AddMsgBoardCmd AddVertex And Angle AnyContainer AppendCommand ArcCos ArcSin AssignVar BitAnd BlastObjects BoundBy Bubble Buy C4D_All C4D_Background C4D_Goal C4D_Knowledge C4D_Living C4D_Object C4D_Parallax C4D_Rule C4D_StaticBack C4D_Structure C4D_Vehicle C4Id C4V_Any C4V_Bool C4V_C4ID C4V_C4Object C4V_Int C4V_String COMD_Down COMD_DownLeft COMD_DownRight COMD_Left COMD_None COMD_Right COMD_Stop COMD_Up COMD_UpLeft COMD_UpRight Call CastAny CastBool CastC4ID CastC4Object CastInt CastObjects CastPXS CastParticles ChangeDef ChangeEffect CheckEffect CheckEnergyNeedChain ClearMenuItems ClearParticles ClearScheduleCall CloseMenu Collect ComponentAll ComposeContents Contained Contents ContentsCount Cos CreateArray CreateConstruction CreateContents CreateMenu CreateObject CreateParticle CrewMember DIR_Left DIR_Right DeathAnnounce Dec DecVar DefinitionCall DigFree DigFreeRect Distance Div DoBreath DoCon DoCrewExp DoDamage DoEnergy DoHomebaseMaterial DoMagicEnergy DoRGBaValue DoScore DoScoreboardShow DrawDefMap DrawMap DrawMaterialQuad DrawParticleLine EditCursor EffectCall EffectVar EliminatePlayer EnergyCheck Enter Equal Exit Explode Extinguish ExtractLiquid ExtractMaterialAmount FightWith FindBase FindConstructionSite FindContents FindObject FindObject2 FindObjectOwner FindObjects FindOtherContents Find_Action Find_ActionTarget Find_Allied Find_And Find_AnyContainer Find_AtPoint Find_Category Find_Container Find_Distance Find_Exclude Find_Func Find_Hostile Find_ID Find_InRect Find_NoContainer Find_Not Find_OCF Find_Or Find_Owner FinishCommand Fling Format FrameCounter FreeRect GBackLiquid GBackSemiSolid GBackSky GBackSolid GainMissionAccess GameCall GameOver GetActMapVal GetActTime GetAction GetActionTarget GetAlive GetBase GetBreath GetCaptain GetCategory GetChar GetClimate GetClrModulation GetColor GetColorDw GetComDir GetCommand GetComponent GetCon GetContact GetController GetCrew GetCrewCount GetCrewEnabled GetCrewExtraData GetCursor GetDamage GetDefBottom GetDefCoreVal GetDefinition GetDesc GetDir GetEffect GetEffectCount GetEnergy GetEntrance GetGravity GetHiRank GetHomebaseMaterial GetID GetKiller GetLength GetMagicEnergy GetMass GetMaterial GetMaterialColor GetMaterialCount GetMaterialVal GetMenu GetMenuSelection GetMissionAccess GetName GetNeededMatStr GetOCF GetObjectBlitMode GetObjectInfoCoreVal GetObjectVal GetOwner GetPathLength GetPhase GetPhysical GetPlayerByIndex GetPlayerCount GetPlayerID GetPlayerInfoCoreVal GetPlayerName GetPlayerTeam GetPlayerVal GetPlrColorDw GetPlrDownDouble GetPlrExtraData GetPlrKnowledge GetPlrMagic GetPlrValue GetPlrValueGain GetPlrView GetPlrViewMode GetPortrait GetProcedure GetR GetRDir GetRGBaValue GetRank GetScenarioVal GetScore GetSeason GetSelectCount GetSkyAdjust GetSkyColor GetSystemTime GetTaggedPlayerName GetTemperature GetTime GetType GetValue GetVertex GetVertexNum GetVisibility GetWealth GetWind GetX GetXDir GetY GetYDir Global GlobalN GrabContents GrabObjectInfo GreaterThan HSL HSL2RGB HSLa Hostile InLiquid Inc IncVar Incinerate InsertMaterial Inside IsNetwork IsNewgfx IsRef Jump Kill LandscapeHeight LandscapeWidth LaunchEarthquake LaunchLightning LaunchVolcano LessThan Local LocalN Log MakeCrewMember Material MaterialName Max Message Min Mod Mul Music MusicLevel NO_OWNER NoContainer Not Object ObjectCall ObjectCount ObjectCount2 ObjectDistance ObjectNumber ObjectSetAction OnFire Or Par PathFree PlaceAnimal PlaceInMaterial PlaceObjects PlaceVegetation PlayerMessage PlrMessage Pow PrivateCall ProtectedCall Punch PushParticles RGB RGB2HSL RGBa Random RandomX ReloadDef ReloadParticle RemoveEffect RemoveObject RemoveVertex ResetGamma ResetPhysical Resort ResortObject ResortObjects SEqual Schedule ScheduleCall ScoreboardCol ScriptCounter ScriptGo ScrollContents SelectCrew SelectMenuItem Sell SetAction SetActionData SetActionTargets SetAlive SetCategory SetClimate SetClrModulation SetColor SetColorDw SetComDir SetCommand SetComponent SetCon SetController SetCrewEnabled SetCrewExtraData SetCursor SetDir SetEntrance SetFilmView SetFoW SetGameSpeed SetGamma SetGlobal SetGraphics SetGravity SetHostility SetLandscapePixel SetLength SetLocal SetMass SetMatAdjust SetMaterialColor SetMaxPlayer SetMenuSize SetName SetObjectBlitMode SetObjectOrder SetOwner SetPhase SetPhysical SetPicture SetPlayList SetPlrExtraData SetPlrKnowledge SetPlrMagic SetPlrShowCommand SetPlrShowControl SetPlrShowControlPos SetPlrView SetPlrViewRange SetPortrait SetPosition SetR SetRDir SetRGBaValue SetScoreboardData SetSeason SetShape SetSkyAdjust SetSkyColor SetSkyFade SetSkyParallax SetSolidMask SetTemperature SetTransferZone SetVar SetVertex SetVertexXY SetViewOffset SetVisibility SetWealth SetWind SetXDir SetYDir ShakeFree ShakeObjects ShiftContents ShowInfo SimFlight Sin SkyPar_Keep Smoke SortScoreboard Sound SoundLevel Split2Components SplitRGBaValue Sqrt StartScriptProfiler StopScriptProfiler Stuck Sub Sum TrainPhysical UnselectCrew VIS_All VIS_Allies VIS_Enemies VIS_God VIS_Local VIS_None VIS_Owner Value Var VarN VerticesStuck _inherited eval inherited";
//		for(String func : engineFunctions.split(" ")) {
//			engineFunctionRule.addWord(func, engineFunction);
//		}

//		rules.add(new OperatorScanner());
////		rules.add(operatorRule);
//		rules.add(keywordRule);
//		rules.add(typeRule);
//		rules.add(engineFunctionRule);
		currentRules = (IRule[])rules.toArray(new IRule[0]);
		setRules(currentRules);
	}
}
