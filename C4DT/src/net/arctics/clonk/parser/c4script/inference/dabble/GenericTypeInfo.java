package net.arctics.clonk.parser.c4script.inference.dabble;

import static net.arctics.clonk.util.Utilities.as;
import static net.arctics.clonk.util.Utilities.isAnyOf;
import net.arctics.clonk.index.Index;
import net.arctics.clonk.parser.ASTNode;
import net.arctics.clonk.parser.Declaration;
import net.arctics.clonk.parser.EntityRegion;
import net.arctics.clonk.parser.c4script.ITypeable;
import net.arctics.clonk.parser.c4script.ast.ASTComparisonDelegate;
import net.arctics.clonk.parser.c4script.ast.AccessDeclaration;
import net.arctics.clonk.parser.c4script.ast.AccessVar;
import net.arctics.clonk.parser.c4script.ast.TypingJudgementMode;
import net.arctics.clonk.parser.c4script.inference.dabble.DabbleInference.ScriptProcessor;

/**
 * Stored Type Information that applies the stored type information by determining the {@link ITypeable} being referenced by some arbitrary {@link ASTNode} and setting its type.
 * @author madeen
 *
 */
public final class GenericTypeInfo extends TypeInfo {
	private final ASTNode expression;

	public GenericTypeInfo(ASTNode referenceElm, ScriptProcessor processor) {
		super();
		this.expression = referenceElm;
		ITypeable typeable = typeableFromExpression(referenceElm, processor);
		if (typeable != null)
			this.type = typeable.type();
	}

	private static final ASTComparisonDelegate IDENTITY_DIFFERENCE_LISTENER = new ASTComparisonDelegate() {
		@Override
		public boolean considerDifferent() {
			AccessDeclaration leftDec = as(left, AccessDeclaration.class);
			AccessDeclaration rightDec = as(right, AccessDeclaration.class);
			if (leftDec != null && rightDec != null)
				if (leftDec.declaration() != rightDec.declaration())
					return true;
			return false;
		}
	};

	@Override
	public boolean storesTypeInformationFor(ASTNode expr, ScriptProcessor processor) {
		if (expr instanceof AccessDeclaration && expression instanceof AccessDeclaration && ((AccessDeclaration)expr).declaration() == ((AccessDeclaration)expression).declaration())
			return !isAnyOf(((AccessDeclaration)expr).declaration(), processor.cachedEngineDeclarations().VarAccessFunctions);
		ASTNode chainA, chainB;
		for (chainA = expr, chainB = expression; chainA != null && chainB != null; chainA = chainA.predecessorInSequence(), chainB = chainB.predecessorInSequence())
			if (!chainA.compare(chainB, IDENTITY_DIFFERENCE_LISTENER))
				return false;
		return chainA == null || chainB == null;
	}

	@Override
	public boolean refersToSameExpression(ITypeInfo other) {
		if (other instanceof GenericTypeInfo)
			return ((GenericTypeInfo)other).expression.equals(expression);
		else
			return false;
	}

	public static ITypeable typeableFromExpression(ASTNode referenceElm, ScriptProcessor processor) {
		EntityRegion decRegion = referenceElm.entityAt(referenceElm.getLength()-1, processor);
		if (decRegion != null && decRegion.entityAs(ITypeable.class) != null)
			return decRegion.entityAs(ITypeable.class);
		else
			return null;
	}

	@Override
	public void apply(boolean soft, ScriptProcessor processor) {
		ITypeable typeable = typeableFromExpression(expression, processor);
		if (typeable != null) {
			// don't apply typing to non-local things if only applying type information softly
			// this prevents assigning types to instance variables when only hovering over some function or something like that
			if (soft && !typeable.isLocal())
				return;
			// only set types of declarations inside the current index so definition references of one project
			// don't leak into a referenced base project (ClonkMars def referenced in ClonkRage or something)
			Index index = typeable.index();
			if (index == null || index != processor.script().index())
				return;

			typeable.expectedToBeOfType(type, TypingJudgementMode.Force);
		}
	}

	@Override
	public String toString() {
		return String.format("[%s: %s]", expression.toString(), type.typeName(true));
	}

	public static ITypeInfo makeTypeInfo(Declaration declaration, ScriptProcessor processor) {
		if (declaration != null)
			return new GenericTypeInfo(new AccessVar(declaration), processor);
		else
			return null;
	}

	@Override
	public Declaration declaration(ScriptProcessor processor) {
		return as(typeableFromExpression(expression, processor), Declaration.class);
	}

}