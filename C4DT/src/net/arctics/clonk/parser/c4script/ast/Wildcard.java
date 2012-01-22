package net.arctics.clonk.parser.c4script.ast;

import java.util.HashMap;
import java.util.Map;

import net.arctics.clonk.ClonkCore;
import net.arctics.clonk.parser.ParsingException;
import net.arctics.clonk.parser.c4script.C4ScriptParser;
import net.arctics.clonk.parser.c4script.DeclarationObtainmentContext;
import net.arctics.clonk.parser.c4script.IType;
import net.arctics.clonk.parser.c4script.PrimitiveType;
import net.arctics.clonk.parser.c4script.ProplistDeclaration;
import net.arctics.clonk.parser.c4script.ast.IASTComparisonDelegate.DifferenceHandling;
import net.arctics.clonk.util.ArrayUtil;
import net.arctics.clonk.util.IConverter;
import net.arctics.clonk.util.Sink;

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
	private ExprElm original;
	
	@SuppressWarnings({ "unchecked", "rawtypes" })
	private void cacheAttributes() {
		tag = valueEvaluated(PROP_TAG, String.class);
		original = value(PROP_TEMPLATE);
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
	
	public ExprElm template() {
		return original;
	}
	
	public void setTemplate(ExprElm template) {
		this.original = template;
	}
	
	@Override
	public boolean hasSideEffects() {
		return true; // of course
	}
	
	public String tag() {
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
	public boolean isModifiable(C4ScriptParser context) {
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
	
	@Override
	public DifferenceHandling compare(ExprElm other, IASTComparisonDelegate listener) {
		if (tag != null && (acceptedClasses == null || ArrayUtil.indexOf(other.getClass(), acceptedClasses) != -1)) {
			listener.wildcardMatched(this, other);
			return DifferenceHandling.EqualShortCircuited;
		}
		return DifferenceHandling.Differs;
	}
	
	public static class WildcardMatchingSuccess {
		public ExprElm matched;
		public Map<String, ExprElm> wildcardMatches;
		public WildcardMatchingSuccess(ExprElm matched) {
			super();
			this.matched = matched;
			this.wildcardMatches = new HashMap<String, ExprElm>();
		}
	}
	
	public static void matchWildcards(ExprElm template, ExprElm body, Sink<WildcardMatchingSuccess> sink) {
		final WildcardMatchingSuccess s = new WildcardMatchingSuccess(body);
		IASTComparisonDelegate d = new IASTComparisonDelegate() {
			@Override
			public DifferenceHandling differs(ExprElm a, ExprElm b, Object what) {
				return DifferenceHandling.Differs;
			}

			@Override
			public boolean optionEnabled(Option option) {
				return false;
			}

			@Override
			public void wildcardMatched(Wildcard wildcard, ExprElm expression) {
				s.wildcardMatches.put(wildcard.tag(), expression);
			}
		};
		if (body.compare(template, d).isEqual())
			sink.receivedObject(s);
		else {
			for (ExprElm e : body.subElements()) {
				matchWildcards(template, e, sink);
			}
		}
	}
	
	public static ExprElm generateReplacement(ExprElm original, ExprElm topLevel, WildcardMatchingSuccess wildcardReplacements) throws CloneNotSupportedException {
		if (topLevel == null) {
			topLevel = original.clone();
		}
		if (original instanceof Wildcard) {
			Wildcard w = (Wildcard) original;
			if (w.tag() != null) {
				ExprElm e = wildcardReplacements.wildcardMatches.get(w.tag());
				if (e != null)
					if (topLevel == original)
						return e.clone();
					else
						original.getParent().replaceSubElement(original, e.clone(), 0);
			}
		}
		for (ExprElm e : original.subElements()) {
			generateReplacement(e, topLevel, wildcardReplacements);
		}
		return topLevel;
	}
	
}
