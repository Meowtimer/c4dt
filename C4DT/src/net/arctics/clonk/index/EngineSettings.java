package net.arctics.clonk.index;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import net.arctics.clonk.c4group.GroupType;
import net.arctics.clonk.ini.IniField;
import net.arctics.clonk.preferences.ClonkPreferences;
import net.arctics.clonk.util.ArrayUtil;
import net.arctics.clonk.util.SettingsBase;

/**
 * Settings object encapsulating configuration options for a specific engine.
 * Read from res/engines/&lt;engine&gt;/configuration.ini and <workspace>/.metadata/.plugins/net.arctics.clonk/engines/&lt;engine&gt;/configuration.ini
 * @author madeen
 *
 */
public class EngineSettings extends SettingsBase {

	private static final String SOURCE = "Source";
	private static final String INTRINSIC = "Intrinsic";

	/** Default strictness level applied to scripts with no explicit #strict line. */
	@IniField(category=INTRINSIC)
	public long strictDefaultLevel;
	/** Maximum string length of string constants. */
	@IniField(category=INTRINSIC)
	public long maxStringLen;
	/** Whether engine supports colon ID syntax (:Clonk, :Firestone). Enforcing this syntax was discussed and then dropped. */
	@IniField(category=INTRINSIC)
	public boolean colonIDSyntax;
	/**
	 * Whether declarations of static non-const variables are allowed to include an assignment. OC added support for this.
	 */
	@IniField(category=INTRINSIC)
	public boolean supportsNonConstGlobalVarAssignment;
	/**
	 * HACK: In OC, object definition constants (Clonk, Firestone) actually are parsed as referring to a {@link ConcreteVariable} object each {@link Definition} maintains as its 'proxy variable'.<br/>
	 * This toggle activates/deactivates this behavior.
	 * */
	@IniField(category=INTRINSIC)
	public boolean definitionsHaveProxyVariables;
	/** Whether engine supports ref parameters (int & x). OpenClonk stopped supporting it. */
	@IniField(category=INTRINSIC)
	public boolean supportsRefs;
	/** Whether engine supports creating a debug connection and single-stepping through C4Script code */
	@IniField(category=INTRINSIC)
	public boolean supportsDebugging;
	/** Name of editor mode option (CR: console, OC: editor) */
	@IniField(category=INTRINSIC)
	public String editorCmdLineOption;
	/** Format for commandline options (ClonkRage: /%s vs OpenClonk: --%s) */
	@IniField(category=INTRINSIC)
	public String cmdLineOptionFormat;
	/** Format for commandline options that take an argument --%s=%s */
	@IniField(category=INTRINSIC)
	public String cmdLineOptionWithArgumentFormat;
	/** Whether engine supports -x <command> */
	@IniField(category=INTRINSIC)
	public boolean supportsEmbeddedUtilities;
	/** Whether engine parser allows obj-> ~DoSomething() */
	@IniField(category=INTRINSIC)
	public boolean spaceAllowedBetweenArrowAndTilde;
	/** String of the form c4d->DefinitionGroup,... specifying what file extension denote what group type. */
	@IniField(category=INTRINSIC)
	public String fileExtensionToGroupTypeMapping;
	/** Extension for material definition files */
	@IniField(category=INTRINSIC)
	public String materialExtension;
	/** Engine supports proplists (OC) */
	@IniField(category=INTRINSIC)
	public boolean supportsProplists;
	@IniField(category=INTRINSIC)
	public boolean supportsVarArgsDeclaration;
	/** ';'-separated list of file extensions supported as sound files */
	@IniField(category=INTRINSIC)
	public String supportedSoundFileExtensions;
	/** Engine supports passing references to functions */
	@IniField(category=INTRINSIC)
	public boolean supportsFunctionRefs;
	/** Support floating point variables in script */
	@IniField(category=INTRINSIC)
	public boolean supportsFloats;
	/** Support an implicitly defined proplist named 'global' */
	@IniField(category=INTRINSIC)
	public boolean supportsGlobalProplists;
	@IniField(category=INTRINSIC)
	/** Engine supports nil keyword **/
	public boolean supportsNil;
	/** Treat 0 as being of type ANY so assigning 0 to object variables and such does not result in warnings */
	@IniField(category=INTRINSIC)
	public boolean zeroIsAny;
	@IniField(category=INTRINSIC)
	public boolean integersConvertibleToIDs;
	@IniField(category=INTRINSIC)
	public boolean supportsFunctionVisibility;
	@IniField(category=INTRINSIC)
	public boolean supportsStrictDirective;

	// Settings that are actually intended to be user-configurable

	/** Template for Documentation URL. */
	@IniField
	public String docURLTemplate;
	@IniField
	public boolean useDocsFromRepository;
	/** Path to engine executable. */
	@IniField
	public String engineExecutablePath;
	/** Path to game folder. */
	@IniField
	public String gamePath;
	/** Path to OC repository. To be used for automatically importing engine definitions (FIXME: needs proper implementation). */
	@IniField
	public String repositoryPath;
	/** Path to c4group executable */
	@IniField
	public String c4GroupPath;
	/** Whether to dynamically load documentation from the given repository **/
	@IniField
	public boolean readDocumentationFromRepository;

	// Fields related to scanning cpp source files for built-in declarations.

	@IniField(category=SOURCE)
	public String initFunctionMapPattern;
	@IniField(category=SOURCE)
	public String constMapPattern;
	@IniField(category=SOURCE)
	public String fnMapPattern;
	@IniField(category=SOURCE)
	public String fnMapEntryPattern;
	@IniField(category=SOURCE)
	public String constMapEntryPattern;
	@IniField(category=SOURCE)
	public String addFuncPattern;
	@IniField(category=SOURCE)
	public String fnDeclarationPattern;
	/**
	 * List of cpp source file paths - relative to a repository - to import declarations from. Used by {@link CPPSourceDeclarationsImporter}
	 */
	@IniField(category=SOURCE)
	public String cppSources;
	/**
	 * Functions defined by scripts, called by engine
	 */
	@IniField(category=SOURCE)
	public String callbackFunctions;

	private transient Map<String, GroupType> fetgtm;
	private transient Map<GroupType, String> rfetgtm;
	private transient List<String> supportedSoundFileExtensions_;

	/**
	 * Return a map mapping a file name extension to a group type for this engine.
	 * @return The map.
	 */
	public Map<String, GroupType> fileExtensionToGroupTypeMapping() {
		if (fetgtm == null) {
			fetgtm = new HashMap<String, GroupType>(GroupType.values().length);
			for (final String mapping : fileExtensionToGroupTypeMapping.split(",")) {
				final String[] elms = mapping.split("->");
				if (elms.length >= 2) {
					final GroupType gt = GroupType.valueOf(elms[1]);
					fetgtm.put(elms[0], gt);
				}
			}
			rfetgtm = ArrayUtil.reverseMap(fetgtm, new HashMap<GroupType, String>());
		}
		return fetgtm;
	}

	/**
	 * Reverse map of {@link #fileExtensionToGroupTypeMapping()}
	 * @return The reverse map
	 */
	public Map<GroupType, String> groupTypeToFileExtensionMapping() {
		fileExtensionToGroupTypeMapping();
		return rfetgtm;
	}

	private transient String fileDialogFilterString;
	/**
	 * Return a filter string for c4group files to be used with file dialogs
	 * @return The filter string
	 */
	public String fileDialogFilterForGroupFiles() {
		if (fileDialogFilterString == null) {
			final StringBuilder builder = new StringBuilder(6*fileExtensionToGroupTypeMapping().size());
			for (final String ext : fileExtensionToGroupTypeMapping().keySet()) {
				builder.append("*.");
				builder.append(ext);
				builder.append(";");
			}
			fileDialogFilterString = builder.toString();
		}
		return fileDialogFilterString;
	}

	/**
	 * Return documentation URL for a function name. The result will be a local file URL if {@link #useDocsFromRepository} is set,
	 * a link based on the {@link #docURLTemplate} if not.
	 * @param functionName The function name
	 * @return The URL string
	 */
	public String documentationURLForFunction(final String functionName) {
		final String urlFormatString = useDocsFromRepository
			? "file://" + repositoryPath + "/docs/sdk/script/fn/%1$s.xml"
			: docURLTemplate;
		final String pref = ClonkPreferences.languagePref();
		return String.format(urlFormatString, functionName, pref.equals("DE") ? "de" : "en");
	}

	/**
	 * Return a list of sound file extensions this engine supports.
	 * @return The extension list
	 */
	public List<String> supportedSoundFileExtensions() {
		if (supportedSoundFileExtensions_ == null)
			supportedSoundFileExtensions_ = Arrays.asList(supportedSoundFileExtensions.split("\\;"));
		return supportedSoundFileExtensions_;
	}

	/**
	 * Return array containing names of function this kind of engine will natively call.
	 * @return The array of callback function names
	 */
	public String[] callbackFunctions() {
		return callbackFunctions != null ? callbackFunctions.split(",") : new String[0];
	}

}