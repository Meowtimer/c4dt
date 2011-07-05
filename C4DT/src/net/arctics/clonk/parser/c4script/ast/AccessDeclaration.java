package net.arctics.clonk.parser.c4script.ast;

import java.util.LinkedList;
import java.util.List;

import net.arctics.clonk.ClonkCore;
import net.arctics.clonk.index.Index;
import net.arctics.clonk.parser.Declaration;
import net.arctics.clonk.parser.DeclarationRegion;
import net.arctics.clonk.parser.IHasIncludes;
import net.arctics.clonk.parser.ParsingException;
import net.arctics.clonk.parser.c4script.C4ScriptParser;
import net.arctics.clonk.parser.c4script.ConstrainedProplist;
import net.arctics.clonk.parser.c4script.DeclarationObtainmentContext;
import net.arctics.clonk.parser.c4script.Function;
import net.arctics.clonk.parser.c4script.IType;
import net.arctics.clonk.parser.c4script.ITypeable;
import net.arctics.clonk.parser.c4script.PrimitiveType;
import net.arctics.clonk.parser.c4script.ProplistDeclaration;
import net.arctics.clonk.parser.c4script.TypeSet;
import net.arctics.clonk.parser.c4script.IHasConstraint.ConstraintKind;
import net.arctics.clonk.parser.c4script.Variable;
import net.arctics.clonk.parser.c4script.ast.IASTComparisonDelegate.DifferenceHandling;
import net.arctics.clonk.parser.c4script.ast.IASTComparisonDelegate.Option;
import net.arctics.clonk.util.ArrayUtil;

import org.eclipse.jface.text.IRegion;
import org.eclipse.jface.text.Region;

/**
 * An expression referring to some {@link Declaration}. Derived classes represent access to {@link Variable}s ({@link AccessVar}) and {@link Function} calls ({@link CallFunc}).
 * @author madeen
 *
 */
public abstract class AccessDeclaration extends Value {

	private static final long serialVersionUID = ClonkCore.SERIAL_VERSION_UID;
	protected transient Declaration declaration;
	protected String declarationName;
	
	/**
	 * Return the {@link Declaration} this expression refers to, obtaining it if that has not happened yet (see {@link #obtainDeclaration(DeclarationObtainmentContext)}).
	 * @param context Context passed to {@link #obtainDeclaration(DeclarationObtainmentContext)} if declaration is still null.
	 * @return The declaration or null if a declaration could not be found.
	 */
	public Declaration getDeclaration(DeclarationObtainmentContext context) {
		if (declaration == null) {
			declaration = obtainDeclaration(context);
		}
		return declaration;
	}

	/**
	 * Return the {@link Declaration} if it has already been obtained or null.
	 * @return The obtain declaration or null.
	 */
	public Declaration getDeclaration() {
		return declaration; // return without trying to obtain it (no parser context)
	}

	/**
	 * Obtain the declaration for this node. The base implementation will always return null, but call {@link #applyTypingByMemberUsage(DeclarationObtainmentContext)}
	 * @param context The {@link DeclarationObtainmentContext} (sounds proper to pass it)
	 * @return The declaration this node most likely refers to
	 */
	public Declaration obtainDeclaration(DeclarationObtainmentContext context) {
		applyTypingByMemberUsage(context);
		return null;
	}

	@Override
	public void reportErrors(C4ScriptParser parser) throws ParsingException {
		super.reportErrors(parser);
		getDeclaration(parser); // find the declaration so subclasses can complain about missing variables/functions
	}

	/**
	 * Create AccessDeclaration object using a declaration name. 
	 * @param declarationName
	 */
	public AccessDeclaration(String declarationName) {
		this.declarationName = declarationName;
	}
	
	@Override
	public void doPrint(ExprWriter output, int depth) {
		output.append(declarationName);
	}

	public IRegion declarationRegion(int offset) {
		return new Region(offset+getExprStart(), declarationName.length());
	}

	@Override
	public DeclarationRegion declarationAt(int offset, C4ScriptParser parser) {
		return new DeclarationRegion(getDeclaration(parser), region(0));
	}

	/**
	 * Return the declaration name this expression uses to refer to a {@link Declaration}.
	 * @return The declaration name
	 */
	public String getDeclarationName() {
		return declarationName;
	}
	
	/**
	 * Set the declaration name.
	 * @param declarationName The name
	 */
	public void setDeclarationName(String declarationName) {
		this.declarationName = declarationName;
	}

	@Override
	public int getIdentifierLength() {
		return declarationName.length();
	}

	/**
	 * Return whether this expression only indirectly refers to a declaration (e.g. inherited/_inherited)
	 * @return Whether or not.
	 */
	public boolean indirectAccess() {
		return declaration == null || !declaration.getName().equals(declarationName);
	}
	
	@Override
	public DifferenceHandling compare(ExprElm other, IASTComparisonDelegate listener) {
		DifferenceHandling handling = super.compare(other, listener);
		if (handling != DifferenceHandling.Equal)
			return handling;
		AccessDeclaration otherDec = (AccessDeclaration) other;
		if (!listener.optionEnabled(Option.CheckForIdentity)) {
			if (!declarationName.equals(otherDec.declarationName)) {
				return listener.differs(this, other, "declarationName"); //$NON-NLS-1$
			}
		} else {
			if (declaration != otherDec.declaration) {
				return listener.differs(this, other, "declaration"); //$NON-NLS-1$
			}
		}
		return DifferenceHandling.Equal;
	}

	/**
	 * Returns the class declarations referenced by this {@link AccessDeclaration} need to be instances of.
	 * @return The {@link Declaration} class
	 */
	public Class<? extends Declaration> declarationClass() {
		return Declaration.class;
	}
	
	@Override
	public IStoredTypeInformation createStoredTypeInformation(C4ScriptParser parser) {
		if (declaration instanceof ITypeable && ((ITypeable)declaration).typeIsInvariant()) {
			return null;
		} else {
			return super.createStoredTypeInformation(parser);
		}
	}
	
	@Override
	public void postSerialize(ExprElm parent, DeclarationObtainmentContext root) {
		super.postSerialize(parent, root);
		getDeclaration(root);
	}
	
	private transient boolean applyTypingByMemberUsagePending;
	/**
	 * Set the type of this expression's predecessor to a set of types that define a declaration called {@link #getDeclarationName()}.
	 * @param context
	 * @return
	 */
	protected boolean applyTypingByMemberUsage(DeclarationObtainmentContext context) {
		if (true)
			return false;
		if (applyTypingByMemberUsagePending)
			return false;
		else
			applyTypingByMemberUsagePending = true;
		try {
			ExprElm pred = getPredecessorInSequence();
			if (pred == null || (pred instanceof MemberOperator && ((MemberOperator)pred).hasTilde()))
				return false;
			IType t = pred.getType(context);
			if (t != null && t.specificness() > PrimitiveType.OBJECT.specificness())
				return false;

			if (context instanceof C4ScriptParser && pred != null) {
				final List<IType> typesWithThatMember = new LinkedList<IType>();
				if (context.getCurrentFunc() != null) {
					for (ProplistDeclaration pd : ArrayUtil.filteredIterable(context.getCurrentFunc().getOtherDeclarations(), ProplistDeclaration.class)) {
						for (Variable v : pd.getComponents()) {
							if (v.getName().equals(declarationName))
								typesWithThatMember.add(new ConstrainedProplist(pd, ConstraintKind.Includes));
						}
					}
				}
				context.getContainer().getIndex().forAllRelevantIndexes(new Index.r() {
					@Override
					public void run(Index index) {
						for (Declaration d : index.declarationsWithName(declarationName, Declaration.class))
							if (!d.isGlobal() && AccessDeclaration.this.declarationClass().isAssignableFrom(d.getClass()) && d.getParentDeclaration() instanceof IHasIncludes)
								typesWithThatMember.add(new ConstrainedProplist((IHasIncludes)d.getParentDeclaration(), ConstraintKind.Includes));
					}
				});
				if (typesWithThatMember.size() > 0) {
					if (t != PrimitiveType.UNKNOWN)
						typesWithThatMember.add(t);
					IType ty = TypeSet.create(typesWithThatMember);
					ty.setTypeDescription(String.format(Messages.AccessDeclaration_TypesSporting, declarationName));
					pred.expectedToBeOfType(ty, (C4ScriptParser) context, TypeExpectancyMode.Force);
					return true;
				}
			}
			return false;
		} finally {
			applyTypingByMemberUsagePending = false;
		}
	}
	
}