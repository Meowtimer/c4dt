package net.arctics.clonk.parser;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public abstract class C4Object extends C4ScriptBase {

	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;
	
	private Map<String, String> localizedNames;
	
	protected C4ID id;
	
//	private List<IC4ObjectListener> changeListeners = new LinkedList<IC4ObjectListener>();
	
	/**
	 * Creates a new C4Object and assigns it to <code>container</code>
	 * @param id C4ID (e.g. CLNK)
	 * @param name intern name
	 * @param container ObjectName.c4d resource
	 */
	protected C4Object(C4ID id, String name) {
		this.id = id;
		this.name = name;
	}

	@Override
	public String toString() {
		return getName() + (id != null ? " (" + id.toString() + ")" : "");
	}
	
	/**
	 * The id of this object. (e.g. CLNK)
	 * @return the id
	 */
	public C4ID getId() {
		return id;
	}
	
	/**
	 * Sets the id property of this object.
	 * This method does not change resources.
	 * @param newId
	 */
	public void setId(C4ID newId) {
		if (id.equals(newId))
			return;
		ClonkIndex index = this.getIndex();
		index.removeObject(this);
		id = newId;
		index.addObject(this);
	}
	

	@Override
	protected boolean refersToThis(String name, FindFieldInfo info) {
		if (info.getFieldClass() == null || info.getFieldClass() == C4Object.class) {
			if (id != null && id.getName().equals(name))
				return true;
		}
		return false;
	}
	
	private static Pattern langNamePairPattern = Pattern.compile("(..):(.*)");
	
	public void readNames(String namesText) throws IOException {
		Matcher matcher = langNamePairPattern.matcher(namesText);
		if (localizedNames == null)
			localizedNames = new HashMap<String, String>();
		else
			localizedNames.clear();
		while (matcher.find()) {
			localizedNames.put(matcher.group(1), matcher.group(2));
		}
		String engName = localizedNames.get("DE");
		if (engName != null)
			setName(engName);
	}

	public Map<String, String> getLocalizedNames() {
		return localizedNames;
	}
	
	public boolean nameContains(String text) {
		if (getId().getName().indexOf(text) != -1)
			return true;
		if (getName().toUpperCase().contains(text))
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
	
}
