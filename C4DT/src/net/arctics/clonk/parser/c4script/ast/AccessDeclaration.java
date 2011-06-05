package net.arctics.clonk.parser.c4script.ast;

import java.util.LinkedList;
import java.util.List;

import net.arctics.clonk.ClonkCore;
import net.arctics.clonk.index.ClonkIndex;
import net.arctics.clonk.parser.Declaration;
import net.arctics.clonk.parser.DeclarationRegion;
import net.arctics.clonk.parser.ParsingException;
import net.arctics.clonk.parser.c4script.C4ScriptParser;
import net.arctics.clonk.parser.c4script.ConstrainedProplist;
import net.arctics.clonk.parser.c4script.DeclarationObtainmentContext;
import net.arctics.clonk.parser.c4script.IType;
import net.arctics.clonk.parser.c4script.ITypeable;
import net.arctics.clonk.parser.c4script.PrimitiveType;
import net.arctics.clonk.parser.c4script.ScriptBase;
import net.arctics.clonk.parser.c4script.TypeSet;
import net.arctics.clonk.parser.c4script.IHasConstraint.ConstraintKind;
import net.arctics.clonk.parser.c4script.ast.IASTComparisonDelegate.DifferenceHandling;
import net.arctics.clonk.parser.c4script.ast.IASTComparisonDelegate.Option;

import org.eclipse.jface.text.IRegion;
import org.eclipse.jface.text.Region;

public abstract class AccessDeclaration extends Value {

	private static final long serialVersionUID = ClonkCore.SERIAL_VERSION_UID;
	protected transient Declaration declaration;
	protected String declarationName;
	
	public Declaration getDeclaration(DeclarationObtainmentContext context) {
		if (declaration == null) {
			declaration = obtainDeclaration(context);
		}
		return declaration;
	}

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

	public AccessDeclaration(String fieldName) {
		this.declarationName = fieldName;
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

	public String getDeclarationName() {
		return declarationName;
	}
	
	public void setDeclarationName(String declarationName) {
		this.declarationName = declarationName;
	}

	@Override
	public int getIdentifierLength() {
		return declarationName.length();
	}

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
	
	protected void applyTypingByMemberUsage(DeclarationObtainmentContext context) {
		ExprElm pred = getPredecessorInSequence();
		if (pred == null)
			return;
		IType t = pred.getType(context);
		if (t != null && t.specificness() > PrimitiveType.OBJECT.specificness())
			return;

		if (context instanceof C4ScriptParser && pred != null) {
			final List<IType> typesWithThatMember = new LinkedList<IType>();
			context.getContainer().getIndex().forAllRelevantIndexes(new ClonkIndex.r() {
				@Override
				public void run(ClonkIndex index) {
					for (Declaration d : index.declarationsWithName(declarationName, Declaration.class))
						if (!d.isGlobal() && AccessDeclaration.this.declarationClass().isAssignableFrom(d.getClass()) && d.getParentDeclaration() instanceof ScriptBase)
							typesWithThatMember.add(new ConstrainedProplist((ScriptBase)d.getParentDeclaration(), ConstraintKind.Includes));
				}
			});
			if (typesWithThatMember.size() > 0) {
				typesWithThatMember.add(t);
				IType ty = TypeSet.create(typesWithThatMember);
				ty.setTypeDescription(String.format(Messages.AccessDeclaration_TypesSporting, declarationName));
				pred.expectedToBeOfType(ty, (C4ScriptParser) context, TypeExpectancyMode.Force);
			}
		}
	}
	
}