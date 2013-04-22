package net.arctics.clonk.mapcreator;

import net.arctics.clonk.ini.IDArray;
import net.arctics.clonk.ini.IniDefaultSection;
import net.arctics.clonk.ini.IniField;
import net.arctics.clonk.ini.MaterialArray;

@IniDefaultSection(name="Landscape")
public class LandscapeSection {
	public static final int C4S_MaxMapPlayerExtend = 4;
	
	@IniField public boolean ExactLandscape;
	@IniField public ScenarioValue VegetationLevel;
	@IniField public IDArray Vegetation;
	@IniField public ScenarioValue InEarthLevel;
	@IniField public IDArray InEarth;
	@IniField public boolean BottomOpen,TopOpen;
	@IniField public int LeftOpen,RightOpen;
	@IniField public boolean AutoScanSideOpen;
	@IniField public String SkyDef;
	@IniField public int[] SkyDefFade;
	@IniField public boolean NoScan;
	@IniField public ScenarioValue Gravity;
	@IniField public ScenarioValue MapWidth,MapHeight,MapZoom;
	@IniField public ScenarioValue Amplitude,Phase,Period,Random;
	@IniField public ScenarioValue LiquidLevel;
	@IniField public int MapPlayerExtend;
	@IniField public MaterialArray Layers;
	@IniField public String Material;
	@IniField public String Liquid;
	@IniField public boolean KeepMapCreator; // set if the mapcreator will be needed in the scenario (for DrawDefMap)
	@IniField public int SkyScrollMode;  // sky scrolling mode for newgfx
	@IniField public int FoWRes; // chunk size of FoGOfWar
	@IniField public int MaterialZoom;
	
	public int[] GetMapSize(int iPlayerNum) {
		int[] size = new int[] {MapWidth.Evaluate(), MapHeight.Evaluate()};
		iPlayerNum = Math.max(iPlayerNum, 1 );
		if (MapPlayerExtend > 0)
			size[0] = Math.min(size[0] * Math.min(iPlayerNum, C4S_MaxMapPlayerExtend), MapWidth.Max);
		return size;
	}
	
	void Default() {
		BottomOpen=false; TopOpen=true;
		LeftOpen=0; RightOpen=0;
		AutoScanSideOpen=true;
		SkyDef=null;
		SkyDefFade = new int[6];
		for (int cnt=0; cnt<6; cnt++) SkyDefFade[cnt]=0;
		VegetationLevel = new ScenarioValue(50,30,0,100);
		Vegetation = new IDArray();
		InEarthLevel = new ScenarioValue(50,0,0,100);
		InEarth = new IDArray();
		MapWidth = new ScenarioValue(100,0,64,250);
		MapHeight = new ScenarioValue(50,0,40,250);
		MapZoom = new ScenarioValue(8,0,5,15);
		Amplitude = new ScenarioValue(0,0);
		Phase = new ScenarioValue(50);
		Period = new ScenarioValue(15);
		Random = new ScenarioValue(0);
		LiquidLevel = new ScenarioValue();
		MapPlayerExtend=0;
		Layers = new MaterialArray();
		Material = "Earth";
		Liquid = "Water";
		ExactLandscape=false;
		Gravity = new ScenarioValue(100,0,10,200);
		NoScan=false;
		KeepMapCreator=false;
		SkyScrollMode=0;
		FoWRes=64;
		MaterialZoom=4;
	}
}
