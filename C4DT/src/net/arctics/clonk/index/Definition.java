package net.arctics.clonk.index;

import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.eclipse.swt.graphics.Image;

import net.arctics.clonk.ClonkCore;
import net.arctics.clonk.parser.Declaration;
import net.arctics.clonk.parser.ID;
import net.arctics.clonk.parser.c4script.ConstrainedType;
import net.arctics.clonk.parser.c4script.ScriptBase;
import net.arctics.clonk.parser.c4script.Variable;
import net.arctics.clonk.parser.c4script.FindDeclarationInfo;
import net.arctics.clonk.preferences.ClonkPreferences;

/**
 * A Clonk object definition.
 * @author madeen
 *
 */
public abstract class Definition extends ScriptBase {

	private static final long serialVersionUID = ClonkCore.SERIAL_VERSION_UID;

	/**
	 * Template to construct the info text of an object definition from
	 */
	public static final String INFO_TEXT_TEMPLATE = Messages.C4Object_InfoTextTemplate;

	/**
	 * localized name of the object; key is language code like "DE" and "US"
	 */
	private Map<String, String> localizedNames;

	/**
	 * id of the object
	 */
	protected ID id;

	/**
	 * Cached picture from Graphics.png
	 */
	private transient Image cachedPicture;

	private transient ConstrainedType objectType;

	/**
	 * Creates a new C4Object
	 * @param id C4ID (e.g. CLNK)
	 * @param name human-readable name
	 */
	protected Definition(ID id, String name) {
		this.id = id;
		this.name = name;
	}

	@Override
	public String toString() {
		return getName() + (id != null ? " (" + id.toString() + ")" : ""); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
	}

	public String idWithName() {
		return id() != null ? String.format(Messages.C4Object_IDWithName, getName(), id().toString()) : getName();
	}

	/**
	 * The id of this object. (e.g. CLNK)
	 * @return the id
	 */
	public ID id() {
		return id;
	}

	/**
	 * Sets the id property of this object.
	 * This method does not perform necessary changes to DefCore.txt.
	 * @param newId
	 */
	public void setId(ID newId) {
		if (id.equals(newId))
			return;
		ClonkIndex index = this.getIndex();
		index.removeDefinition(this);
		id = newId;
		index.addDefinition(this);
	}

	public Variable proxyVar() {
		return null;
	}

	@Override
	protected Declaration getThisDeclaration(String name, FindDeclarationInfo info) {
		Class<?> cls = info.getDeclarationClass();
		boolean variableRequired = false;
		if (
				cls == null ||
				cls == Definition.class ||
				(getEngine() != null && getEngine().getCurrentSettings().definitionsHaveStaticVariables && (variableRequired = Variable.class.isAssignableFrom(cls)))
		) {
			if (id != null && id.getName().equals(name))
				return variableRequired ? this.proxyVar() : this;
		}
		return null;
	}

	private static Pattern langNamePairPattern = Pattern.compile("(..):(.*)"); //$NON-NLS-1$

	public void readNames(String namesText) throws IOException {
		Matcher matcher = langNamePairPattern.matcher(namesText);
		if (localizedNames == null)
			localizedNames = new HashMap<String, String>();
		else
			localizedNames.clear();
		while (matcher.find()) {
			localizedNames.put(matcher.group(1), matcher.group(2));
		}
		chooseLocalizedName();
	}

	public void chooseLocalizedName() {
		if (localizedNames != null) {
			String preferredName = localizedNames.get(ClonkPreferences.getLanguagePref());
			if (preferredName != null)
				setName(preferredName);
		}
	}

	public Map<String, String> getLocalizedNames() {
		return localizedNames;
	}

	@Override
	public boolean nameContains(String text) {
		if (id() != null && id().getName().toUpperCase().indexOf(text) != -1)
			return true;
		if (getName() != null && getName().toUpperCase().contains(text))
			return true;
		if (localizedNames != null) {
			for (String key : localizedNames.keySet()) {
				String value = localizedNames.get(key);
				if (value.toUpperCase().indexOf(text) != -1)
					return true;
			}
		}
		return false;
	}

	@Override
	protected void gatherIncludes(final List<ScriptBase> list, ClonkIndex index) {
		super.gatherIncludes(list, index);
		if (index != null) index.forAllRelevantIndexes(new ClonkIndex.r() {
			@Override
			public void run(ClonkIndex index) {
				List<ScriptBase> appendages = index.appendagesOf(Definition.this);
				if (appendages != null)
					list.addAll(appendages);
			}
		});
	}

	@Override
	protected void finalize() throws Throwable {
		if (cachedPicture != null)
			cachedPicture.dispose();
		super.finalize();
	}

	public Image getCachedPicture() {
		return cachedPicture;
	}

	public void setCachedPicture(Image cachedPicture) {
		this.cachedPicture = cachedPicture;
	}
	
	public ConstrainedType getObjectType() {
		if (objectType == null) {
			objectType = new ConstrainedType(this, ConstraintKind.Exact);
		}
		return objectType;
	}

}
