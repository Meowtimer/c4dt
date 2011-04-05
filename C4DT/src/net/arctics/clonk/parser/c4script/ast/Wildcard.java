package net.arctics.clonk.parser.c4script.ast;

import java.util.ArrayList;
import java.util.List;

import net.arctics.clonk.ClonkCore;
import net.arctics.clonk.parser.c4script.C4ScriptParser;
import net.arctics.clonk.util.Pair;

/**
 * Special expression that acts as a placeholder 
 * @author madeen
 *
 */
public class Wildcard extends ExprElm {
	
	// not so easy to find some character sequences that won't be misinterpreted
	public static final String START = "§(";
	public static final String END = ")";
	
	public static final String PROP_TAG = "Tag";
	public static final String PROP_TEMPLATE = "Template";
	public static final String PROP_CLASSES = "Classes";
	
	private static final long serialVersionUID = ClonkCore.SERIAL_VERSION_UID;

	/**
	 * List of ExprElm classes this wildcard accepts as substitute.
	 */
	private List<Class<? extends ExprElm>> acceptedClasses;
	private String tag;
	
	/**
	 * Template that defines how a possible match for this wildcard should look like
	 */
	private ExprElm template;
	
	public void setAcceptedClasses(List<Class<? extends ExprElm>> acceptedClasses) {
		this.acceptedClasses = acceptedClasses;
	}
	public List<Class<? extends ExprElm>> getAcceptedClasses() {
		return acceptedClasses;
	}
	
	public ExprElm getTemplate() {
		return template;
	}
	
	public void setTemplate(ExprElm template) {
		this.template = template;
	}
	
	@Override
	public boolean hasSideEffects() {
		return true; // of course
	}
	
	public String getTag() {
		return tag;
	}
	
	public void setTag(String tag) {
		this.tag = tag;
	}
	
	@Override
	public boolean isFinishedProperly() {
		return true; // it always is
	}
	
	private boolean printAttribute(boolean addSpace, ExprWriter output, String prop, Object value) {
		if (addSpace)
			output.append(" ");
		String v = value != null ? value.toString() : null;
		if (v != null) {
			output.append(prop);
			output.append(":");
			output.append(v);
			return true;
		} else
			return false;
	}
	
	@Override
	public void doPrint(ExprWriter output, int depth) {
		output.append(START);
		List<Pair<String, Object>> attributes = new ArrayList<Pair<String, Object>>(3);
		attributes.add(new Pair<String, Object>(PROP_TAG, tag));
		attributes.add(new Pair<String, Object>(PROP_TEMPLATE, template));
		attributes.add(new Pair<String, Object>(PROP_CLASSES, new Object() {
			@Override
			public String toString() {
				if (acceptedClasses != null) {
					StringBuilder builder = new StringBuilder();
					for (Class<? extends ExprElm> c : acceptedClasses) {
						if (builder.length() > 0)
							builder.append(", ");
						builder.append(c.getSimpleName());
					}
					return builder.toString();
				} else
					return null;
			}
		}));
		boolean space = false;
		for (Pair<String, Object> a : attributes)
			space = printAttribute(space, output, a.getFirst(), a.getSecond());
		output.append(END);
	}
	
	@Override
	public ExprElm[] getSubElements() {
		return new ExprElm[] {template};
	}
	
	@Override
	public boolean modifiable(C4ScriptParser context) {
		return true; // sure it is
	}
	
	@Override
	public boolean isValidAtEndOfSequence(C4ScriptParser context) {
		return true; // of course - what else
	}
	
	@Override
	public boolean isValidInSequence(ExprElm predecessor, C4ScriptParser context) {
		return true; // whatever you say
	}
	
}
