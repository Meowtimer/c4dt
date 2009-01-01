package net.arctics.clonk.parser.defcore;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

import net.arctics.clonk.parser.C4ID;
import net.arctics.clonk.parser.CompilerException;

public abstract class DefCoreOption {

	public static final Class<?>[] DEFCORE_TYPES = new Class<?>[] {
			SignedInteger.class, UnsignedInteger.class, DefCoreString.class,
			CategoriesArray.class, // categories
			IntegerArray.class, // simple integer array
			IDArray.class }; // components array - List<Pair<C4ID,Integer>>

	private String name;
	private String description;

	public DefCoreOption(String name) {
		this.name = name;
	}

	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}

	public void setDescription(String description) {
		this.description = description;
	}

	public String getDescription() {
		return description;
	}

	public abstract void setInput(String input) throws DefCoreParserException;
	public abstract String getStringRepresentation();

	/**
	 * Returns all DefCore.txt [DefCore] options that exist in Clonk.
	 * 
	 * @return
	 */
	public static List<DefCoreOption> createNewDefCoreList() {
		List<DefCoreOption> defCoreOptions = new ArrayList<DefCoreOption>();
		DefCoreOption option;
		option = new DefCoreC4ID("id");
		option.setDescription("Id des Objekts.");
		defCoreOptions.add(option);
		option = new IntegerArray("Version");
		option
				.setDescription("Minimale vom Objekt benötigte Engine-Version. Dieser Eintrag sollte bei jeder Änderung auf die jeweils\n  aktuelle Clonk-Version gesetzt werden, damit ersichtlich ist, zu welchem Zeitpunkt das Objekt erschienen ist.");
		defCoreOptions.add(option);
		option = new DefCoreString("Name");
		option
				.setDescription("Name des Objekts. Bei Mehrsprachigkeit Names.txt nutzen.");
		defCoreOptions.add(option);
		option = new CategoriesArray("Category");
		option
				.setDescription("Kategorie des Objekts. Siehe <a href=\"../../sdk/definition/category.html\">Objektkategorien</a>.");
		defCoreOptions.add(option);
		option = new SignedInteger("MaxUserSelect");
		option
				.setDescription("Maximale Anzahl beim Platzieren des Objekts im Menüsystem.");
		defCoreOptions.add(option);
		option = new DefCoreString("TimerCall");
		option
				.setDescription("Regelmäßig aufgerufene Funktion des Objektscripts.\n  Siehe <a href=\"../../sdk/definition/script.html\">Objektscripte</a>.");
		defCoreOptions.add(option);
		option = new SignedInteger("Timer");
		option
				.setDescription("Zeitabstand der TimerCalls in Frames. Ohne Angabe gilt der Vorgabewert 35.");
		defCoreOptions.add(option);
		option = new SignedInteger("ContactCalls");
		option
				.setDescription("0 oder 1. Legt fest, ob Kontaktaufrufe im Objektscript getätigt werden.");
		defCoreOptions.add(option);
		option = new SignedInteger("Width");
		option.setDescription("Breite des Objekts.");
		defCoreOptions.add(option);
		option = new SignedInteger("Height");
		option.setDescription("Höhe des Objekts.");
		defCoreOptions.add(option);
		option = new IntegerArray("Offset");
		option
				.setDescription("Koordinatenabstand der linken oberen Ecke zur Objektmitte.");
		defCoreOptions.add(option);
		option = new SignedInteger("Value");
		option.setDescription("Wert des Objekts in Geldpunkten.");
		defCoreOptions.add(option);
		option = new SignedInteger("Mass");
		option
				.setDescription("Gewicht des Objekts. Stein 10, Clonk 50, Hütte 1000, Burg 10000.");
		defCoreOptions.add(option);
		option = new IDArray("Components");
		option
				.setDescription("Elemente, aus denen das Objekt zusammen gesetzt ist.\n  Im Bau oder Wachstum befindliche Objekte enthalten nur anteilige Komponenten.");
		defCoreOptions.add(option);
		option = new IntegerArray("SolidMask");
		option
				.setDescription("Massivbereiche des Objekts. Zielrechteck aus Graphics.bmp aufs Objekt.");
		defCoreOptions.add(option);
		option = new IntegerArray("TopFace");
		option
				.setDescription("Verdeckende Oberflächen. Zielrechteck aus Graphics.bmp aufs Objekt.");
		defCoreOptions.add(option);
		option = new IntegerArray("Picture");
		option
				.setDescription("Repräsentativgrafik des Objekts. Rechteck aus Graphics.bmp.");
		defCoreOptions.add(option);
		option = new SignedInteger("Vertices");
		option.setDescription("Anzahl der Eckpunkte des Objekts. 1 bis 30.");
		defCoreOptions.add(option);
		option = new IntegerArray("VertexX");
		option
				.setDescription("Liste der horizontalen Koordinaten der Eckpunkte des Objekts.\n  Siehe <a href=\"../../sdk/definition/vertices.html\">Vertices</a>.");
		defCoreOptions.add(option);
		option = new IntegerArray("VertexY");
		option
				.setDescription("Liste der vertikalen Koordinaten der Eckpunkte des Objekts.");
		defCoreOptions.add(option);
		option = new IntegerArray("VertexCNAT");
		option
				.setDescription("Liste der Ausrichtungsbestimmungen der Eckpunkte.\n  Siehe <a href=\"../../sdk/definition/cnat.html\">CNAT</a>.");
		defCoreOptions.add(option);
		option = new IntegerArray("VertexFriction");
		option
				.setDescription("Liste der Reibungswerte der Eckpunkte, jeweils 1 bis 100.");
		defCoreOptions.add(option);
		option = new IntegerArray("Entrance");
		option
				.setDescription("Koordinaten des Eingangsbereichs des Objekts relativ zur Objektmitte.");
		defCoreOptions.add(option);
		option = new IntegerArray("Collection");
		option
				.setDescription("Koordinaten des Aufnahmebereichs des Objekts relativ zur Objektmitte.");
		defCoreOptions.add(option);
		option = new SignedInteger("CollectionLimit");
		option
				.setDescription("Anzahl der maximal aufnehmbaren Objekte, 0 für uneingeschränkt.");
		defCoreOptions.add(option);
		option = new SignedInteger("FireTop");
		option.setDescription("Flammenabstand zur Objektunterkante.");
		defCoreOptions.add(option);
		option = new SignedInteger("Placement");
		option
				.setDescription("Platzierung: 0 Oberfläche, 1 in Flüssigkeit, 2 in der Luft.");
		defCoreOptions.add(option);
		option = new SignedInteger("Exclusive");
		option
				.setDescription("0 oder 1. Legt fest, ob das Objekt dahinterliegende Objekte blockiert.");
		defCoreOptions.add(option);
		option = new SignedInteger("ContactIncinerate");
		option
				.setDescription("Kontaktentzündlichkeit: 0 keine, 1 (hoch) bis 5 (niedrig)");
		defCoreOptions.add(option);
		option = new SignedInteger("BlastIncinerate");
		option
				.setDescription("Explosionsentzündlichkeit: 0 keine, sonst bei angegebenem Schadenswert");
		defCoreOptions.add(option);
		option = new DefCoreC4ID("BurnTo");
		option.setDescription("Definitionswechsel bei Entzündung.");
		defCoreOptions.add(option);
		option = new SignedInteger("Base");
		option
				.setDescription("0 oder 1. Legt fest, ob das Objekt Heimatbasis sein kann.");
		defCoreOptions.add(option);
		option = new CategoriesArray("Line");
		option
				.setDescription("1 Energieleitung, 2 Zuflussrohr, 3 Abflussrohr.");
		defCoreOptions.add(option);
		option = new CategoriesArray("LineConnect");
		option
				.setDescription("Anschlussfähigkeit für Leitungen.\n  Siehe <a href=\"../../sdk/definition/lineconnect.html\">LineConnect</a>.");
		defCoreOptions.add(option);
		option = new SignedInteger("Prey");
		option
				.setDescription("0 oder 1. Legt fest, ob das Objekt ein Beutelebewesen ist.");
		defCoreOptions.add(option);
		option = new SignedInteger("Edible");
		option
				.setDescription("0 oder 1. Legt fest, ob das Objekt essbar ist (noch nicht unterstützt).");
		defCoreOptions.add(option);
		option = new SignedInteger("CrewMember");
		option
				.setDescription("0 oder 1. Wenn 1, wird das Objekt beim Kauf der Mannschaft hinzugefügt.\n  Mit CreateObject erzeugte CrewMember-Objekte müssen mit <a href=\"../../sdk/script/fn/MakeCrewMember\">MakeCrewMember</a>\n  der Mannschaft eines Spielers hinzugefügt werden.");
		defCoreOptions.add(option);
		option = new SignedInteger("Growth");
		option.setDescription("Wachstum des Objekts. Baum 1-4, Lebewesen 15.");
		defCoreOptions.add(option);
		option = new SignedInteger("Rebuy");
		option
				.setDescription("0 oder 1. Legt fest, ob das Objekt nach Verkauf zurückgekauft werden kann.");
		defCoreOptions.add(option);
		option = new SignedInteger("Construction");
		option
				.setDescription("0 oder 1. Legt fest, ob das Objekt als Baustelle gebaut werden kann.");
		defCoreOptions.add(option);
		option = new DefCoreC4ID("ConstructTo");
		option.setDescription("Definitionswechsel beim Bau.");
		defCoreOptions.add(option);
		option = new SignedInteger("Grab");
		option
				.setDescription("0 kein Anfassen, 1 Anfassen und Verschieben, 2 nur Anfassen.");
		defCoreOptions.add(option);
		option = new CategoriesArray("GrabPutGet");
		option
				.setDescription("Bitmaske: Bit 0 (Wert 1) Grab und Put, Bit 1 (Wert 2) Grab und Get");
		defCoreOptions.add(option);
		option = new SignedInteger("Collectible");
		option
				.setDescription("0 oder 1. Legt fest, ob das Objekt eingesammelt werden kann.");
		defCoreOptions.add(option);
		option = new SignedInteger("Rotate");
		option
				.setDescription("0 keine Rotation, 1 volle Rotation, 2-360 eingeschränkte Rotation");
		defCoreOptions.add(option);
		option = new SignedInteger("Chop");
		option
				.setDescription("0 oder 1. Legt fest, ob das Objekt gefällt werden kann.");
		defCoreOptions.add(option);
		option = new SignedInteger("Float");
		option
				.setDescription("Auftrieb in Flüssigkeiten: 0 kein Auftrieb, sonst Auftriebshöhe über Mitte");
		defCoreOptions.add(option);
		option = new SignedInteger("ContainBlast");
		option
				.setDescription("0 oder 1. Legt fest, ob Explosionen im Inneren nach außen wirken.");
		defCoreOptions.add(option);
		option = new SignedInteger("ColorByOwner");
		option
				.setDescription("0 oder 1. Bei Wert 1 wird das Ausgangsblau, bzw. die Overlay.png, nach dem Besitzer gefärbt.");
		defCoreOptions.add(option);
		option = new DefCoreString("ColorByMaterial");
		option
				.setDescription("Name des Materials, nach dem das Objekt gefärbt wird.");
		defCoreOptions.add(option);
		option = new SignedInteger("HorizontalFix");
		option
				.setDescription("0 oder 1. Bei Wert 1 kann das Objekt sich nur vertikal bewegen.");
		defCoreOptions.add(option);
		option = new SignedInteger("BorderBound");
		option
				.setDescription("Bitmaske: Bit 0 (1) Stop an Seiten, Bit 1 (2) Stop oben, Bit 2 (4) Stop unten");
		defCoreOptions.add(option);
		option = new SignedInteger("UprightAttach");
		option
				.setDescription("Wenn ungleich 0 (8-10) sitzt das Objekt aufgerichtet auf Massivbereichen auf.");
		defCoreOptions.add(option);
		option = new SignedInteger("StretchGrowth");
		option
				.setDescription("0 oder 1. Bei Wert 1 wird das Objekt wie ein Lebewesen vergrößert (abhängig von der Fertigstellung.\n  Siehe <a href=\"../../sdk/script/fn/GetCon.html\">GetCon</a>)");
		defCoreOptions.add(option);
		option = new SignedInteger("Basement");
		option
				.setDescription("0 kein Fundament, 1 Fundament, andere Werte speziell");
		defCoreOptions.add(option);
		option = new SignedInteger("NoBurnDecay");
		option
				.setDescription("0 oder 1. Bei Wert 1 verbrennt das Objekt nicht.");
		defCoreOptions.add(option);
		option = new SignedInteger("IncompleteActivity");
		option
				.setDescription("0 oder 1. Wert 1 für Lebewesen, die auch im Wachstum aktiv sein können.");
		defCoreOptions.add(option);
		option = new SignedInteger("Oversize");
		option
				.setDescription("0 oder 1. Das Objekt kann mit DoCon auf Übergröße vergrößert werden.");
		defCoreOptions.add(option);
		option = new SignedInteger("AttractLightning");
		option.setDescription("0 oder 1. Das Objekt zieht Blitze an.");
		defCoreOptions.add(option);
		option = new SignedInteger("Fragile");
		option.setDescription("0 oder 1. Objekt sollte nicht geworfen werden.");
		defCoreOptions.add(option);
		option = new SignedInteger("NoPushEnter");
		option
				.setDescription("0 oder 1. Das Objekt kann nicht durch Anfassen und Steuerung Rauf in andere Objekte hineingeschoben werden\n  (z.B. Fahrstuhlkorb).");
		defCoreOptions.add(option);
		option = new SignedInteger("VehicleControl");
		option
				.setDescription("Bitmaske: Bit 0 (Wert 1) das Objekt kann durch Anfassen von außen und/oder Bit 1 (Wert 2) von innen gesteuert\n  werden. Bei aktiviertem VehicleControl werden die selbständig auszuführenden Befehle (Commands) eines Clonks\n  als ControlCommand-Aufrufe an das Script des entsprechenden Fahrzeugs weitergeleitet und können von diesem\n  ausgewertet und abgefangen werden.\n  Siehe <a href=\"../../sdk/definition/script.html#ControlFunktionen\">Control-Funktionen</a>.");
		defCoreOptions.add(option);
		option = new SignedInteger("Pathfinder");
		option
				.setDescription("1 - 10. Beeinflusst die Suchtiefe des Wegfindungsalgorithmus (Standard: 1). Vorsicht: hohe Werte sind sehr rechenintensiv.\n  Mit diesem Wert setzen auch Objekte ohne CrewMember-Wert bei der Ausführung von Kommandos (z.B. MoveTo)\n  den internen Wegfindungsalgorithmus ein. Erweitert ab 4.95.4.");
		defCoreOptions.add(option);
		option = new SignedInteger("NoComponentMass");
		option
				.setDescription("0 oder 1. Bei 1 zählt der Inhalt des Objekts nicht mit zur Gesamtmasse. Dadurch lässt sich zum Beispiel\n  verhindern, dass Rucksäcke oder Köcher durch ihren Inhalt mörderische Wurfwaffen werden.");
		defCoreOptions.add(option);
		option = new SignedInteger("NoStabilize");
		option
				.setDescription("0 oder 1. Bei 1 richtet sich das Objekt nicht automatisch auf 0° Drehung auf, wenn es nur sehr leicht gedreht ist.");
		defCoreOptions.add(option);
		option = new SignedInteger("ClosedContainer");
		option
				.setDescription("0 oder 1. Bei 1 können enthaltene Clonks bei aktiviertem Fog Of War nicht herausschauen.");
		defCoreOptions.add(option);
		option = new SignedInteger("SilentCommands");
		option
				.setDescription("0 oder 1. Bei 1 werden bei fehlgeschlagenen Commands keine Meldungen ausgegeben.");
		defCoreOptions.add(option);
		option = new SignedInteger("NoBurnDamage");
		option
				.setDescription("0 oder 1. Bei 1 wird dem Objekt kein Schaden zugefügt, wenn es brennt.");
		defCoreOptions.add(option);
		option = new SignedInteger("TemporaryCrew");
		option
				.setDescription("0 oder 1. Bei 1 wird das Objekt nicht in die Dauerhafte Crew des Spielers eingetragen.");
		defCoreOptions.add(option);
		option = new SignedInteger("SmokeRate");
		option
				.setDescription("0 bis 200: Gibt die Rauchmenge an, die das Objekt im Brandfall verursacht. 0 ist kein Rauch, 100 ist die Standardmenge.\n  Bei Werten ungleich 0 wird allerdings immer die Maximalmenge Rauch produziert, wenn das Objekt in schneller Bewegung ist\n  (zum Beispiel Brandpfeile).");
		defCoreOptions.add(option);
		option = new SignedInteger("BlitMode");
		option
				.setDescription("0 oder 1. Bei 1 wird das Objekt additiv gezeichnet.");
		defCoreOptions.add(option);
		option = new SignedInteger("NoBreath");
		option
				.setDescription("0 oder 1. Bei 1 atmet das Objekt nicht, selbst wenn es ein Lebewesen ist.");
		defCoreOptions.add(option);
		option = new SignedInteger("ConSizeOff");
		option
				.setDescription("Wert &gt;=0. Abzug des benötigten Bauplatzes von oben.");
		defCoreOptions.add(option);
		option = new SignedInteger("NoSell");
		option
				.setDescription("0 oder 1. Bei 1 kann das Objekt nicht verkauft werden.");
		defCoreOptions.add(option);
		option = new SignedInteger("NoGet");
		option
				.setDescription("0 oder 1. Bei 1 kann das Objekt nicht manuell aus anderen Objekten heraus genommen werden.01.");
		defCoreOptions.add(option);
		option = new SignedInteger("NoFight");
		option
				.setDescription("0 oder 1. Bei 1 kämpft das Objekt nicht mit anderen Lebewesen, selbst wenn es selber ein Lebewesen ist.");
		defCoreOptions.add(option);
		option = new SignedInteger("LiftTop");
		option
				.setDescription("Wird ein Objekt bei einer Action mit der Procedure \"<a href=\"../../sdk/definition/procedures.html\">LIFT</a>\" mehr als die angegebene Höhe\n  über der Objektmitte angehoben, wird die Funktion LiftTop im Script aufgerufen.");
		defCoreOptions.add(option);
		option = new SignedInteger("RotatedEntrance");
		option
				.setDescription("0 Eingang ist nur bei aufgerichtetem Objekt geöffnet, 1 Eingang ist immer geöffnet, 2-360 Eingang ist innerhalb des entsprechenden Bereichs geöffnet. Ab 4.95.4.");
		defCoreOptions.add(option);
		option = new SignedInteger("MoveToRange");
		option
				.setDescription("Maximale Distanz, mit der ein Bewegungskommando einen Zielpunkt treffen muss, damit das Bewegungskommando als erfüllt gilt. Vorgabewert 5. Ab 4.95.4.");
		defCoreOptions.add(option);
		option = new SignedInteger("NoTransferZones");
		option
				.setDescription("0 oder 1. Bei 1 berücksichtigt die Wegfindung des Objekts keine Transferzonen an Gebäuden. Ab 4.95.4.");
		defCoreOptions.add(option);
		option = new SignedInteger("AutoContextMenu");
		option
				.setDescription("0 oder 1. Bei 1 wird für dieses Gebäude beim Betreten automatisch das Kontextmenü geöffnet. Ab 4.9.7.7.");
		defCoreOptions.add(option);
		return defCoreOptions;
	}

	/**
	 * Returns all DefCore.txt [Physical] options that exist in Clonk.
	 * 
	 * @return
	 */
	public static List<DefCoreOption> createNewPhysicalList() {
		List<DefCoreOption> defCoreOptions = new ArrayList<DefCoreOption>();
		DefCoreOption option;
		option = new SignedInteger("Energy");
		option.setDescription("0-100000. Maximale Energie bzw. Lebensenergie.");
		defCoreOptions.add(option);
		option = new SignedInteger("Breath");
		option.setDescription("0-100000. Maximaler Atem.");
		defCoreOptions.add(option);
		option = new SignedInteger("Walk");
		option.setDescription("0-100000. Laufgeschwindigkeit.");
		defCoreOptions.add(option);
		option = new SignedInteger("Jump");
		option.setDescription("0-100000. Sprungkraft.");
		defCoreOptions.add(option);
		option = new SignedInteger("Scale");
		option.setDescription("0-100000. Klettergeschwindigkeit.");
		defCoreOptions.add(option);
		option = new SignedInteger("Hangle");
		option.setDescription("0-100000. Hangelgeschwindigkeit.");
		defCoreOptions.add(option);
		option = new SignedInteger("Dig");
		option.setDescription("0-100000. Grabgeschwindigkeit.");
		defCoreOptions.add(option);
		option = new SignedInteger("Swim");
		option.setDescription("0-100000. Schwimmgeschwindigkeit.");
		defCoreOptions.add(option);
		option = new SignedInteger("Throw");
		option.setDescription("0-100000. Wurfkraft.");
		defCoreOptions.add(option);
		option = new SignedInteger("Push");
		option.setDescription("0-100000. Schiebekraft.");
		defCoreOptions.add(option);
		option = new SignedInteger("Fight");
		option.setDescription("0-100000. Kampfkraft.");
		defCoreOptions.add(option);
		option = new SignedInteger("Magic");
		option.setDescription("0-100000. Maximale Zauberenergie.");
		defCoreOptions.add(option);
		option = new SignedInteger("Float");
		option.setDescription("0-100. Fluggeschwindigkeit.");
		defCoreOptions.add(option);
		option = new SignedInteger("CanScale");
		option.setDescription("0 oder 1. Klettern.");
		defCoreOptions.add(option);
		option = new SignedInteger("CanHangle");
		option.setDescription("0 oder 1. Hangeln.");
		defCoreOptions.add(option);
		option = new SignedInteger("CanDig");
		option.setDescription("0 oder 1. Graben.");
		defCoreOptions.add(option);
		option = new SignedInteger("CanConstruct");
		option
				.setDescription("0 oder 1. Bauen. Bei Werten größer 1: Prozentuale Baugeschwindigkeit (100 entspricht dem Standard;\n  50 ist halbe Standardgeschwindigkeit). Erweitert");
		defCoreOptions.add(option);
		option = new SignedInteger("CorrosionResist");
		option
				.setDescription("0 oder 1. Legt fest, ob das Objekt Säure widersteht.");
		defCoreOptions.add(option);
		option = new SignedInteger("BreatheWater");
		option.setDescription("0 Objekt atmet Luft, 1 Objekt atmet Wasser.");
		defCoreOptions.add(option);
		return defCoreOptions;
	}
}
