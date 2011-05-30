package net.arctics.clonk.parser.c4script.ast;

import net.arctics.clonk.ClonkCore;
import net.arctics.clonk.parser.ParsingException;
import net.arctics.clonk.parser.c4script.C4ScriptParser;
import net.arctics.clonk.parser.c4script.DeclarationObtainmentContext;
import net.arctics.clonk.parser.c4script.IType;
import net.arctics.clonk.parser.c4script.PrimitiveType;
import net.arctics.clonk.parser.c4script.ProplistDeclaration;
import net.arctics.clonk.util.ArrayUtil;
import net.arctics.clonk.util.IConverter;

/**
 * Special expression that acts as a placeholder 
 * @author madeen
 *
 */
public class Wildcard extends PropListExpression {
	
	public Wildcard(ProplistDeclaration declaration) {
		super(declaration);
		cacheAttributes();
	}
	
	public static final String PROP_TAG = "Tag";
	public static final String PROP_TEMPLATE = "Template";
	public static final String PROP_CLASSES = "Classes";
	
	private static final long serialVersionUID = ClonkCore.SERIAL_VERSION_UID;

	/**
	 * List of ExprElm classes this wildcard accepts as substitute.
	 */
	private Class<? extends ExprElm>[] acceptedClasses;
	
	/**
	 * Tag identifying this wildcard
	 */
	private String tag;
	
	/**
	 * Template that defines how a possible match for this wildcard should look like
	 */
	private ExprElm template;
	
	@SuppressWarnings({ "unchecked", "rawtypes" })
	private void cacheAttributes() {
		tag = valueEvaluated(PROP_TAG, String.class);
		template = value(PROP_TEMPLATE);
		Object[] classes = valueEvaluated(PROP_CLASSES, Object[].class);
		acceptedClasses = classes != null ? ArrayUtil.map(classes, Class.class, new IConverter<Object, Class>() {
			@Override
			public Class<? extends ExprElm> convert(Object from) {
				try {
					return (Class<? extends ExprElm>) Class.forName(Wildcard.this.getClass().getPackage().getName() + "." + from.toString());
				} catch (ClassNotFoundException e) {
					return null;
				}
			}
		}) : null;
	}

	public Class<? extends ExprElm>[] getAcceptedClasses() {
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
	
	@Override
	protected IType obtainType(DeclarationObtainmentContext parser) {
		return PrimitiveType.ANY;
	}
	
	@Override
	public void reportErrors(C4ScriptParser parser) throws ParsingException {
		// no errors of course
	}
	
}
