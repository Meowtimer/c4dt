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
import net.arctics.clonk.parser.c4script.inference.dabble.DabbleInference.Visitor;

/**
 * Stored Type Information that applies the stored type information by determining the {@link ITypeable} being referenced by some arbitrary {@link ASTNode} and setting its type.
 * @author madeen
 *
 */
public final class ExpressionTypeVariable extends TypeVariable {
	private final ASTNode expression;

	public ExpressionTypeVariable(ASTNode referenceElm, Visitor visitor) {
		super();
		this.expression = referenceElm;
		ITypeable typeable = typeableFromExpression(referenceElm, visitor);
		if (typeable != null)
			this.type = typeable.type();
	}

	private static final ASTComparisonDelegate IDENTITY_DIFFERENCE_LISTENER = new ASTComparisonDelegate(null) {
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
	public boolean binds(ASTNode expr, Visitor visitor) {
		if (expr instanceof AccessDeclaration && expression instanceof AccessDeclaration && ((AccessDeclaration)expr).declaration() == ((AccessDeclaration)expression).declaration())
			return !isAnyOf(((AccessDeclaration)expr).declaration(), visitor.cachedEngineDeclarations().VarAccessFunctions);
		ASTNode chainA, chainB;
		for (chainA = expr, chainB = expression; chainA != null && chainB != null; chainA = chainA.predecessorInSequence(), chainB = chainB.predecessorInSequence())
			if (!chainA.compare(chainB, IDENTITY_DIFFERENCE_LISTENER))
				return false;
		return chainA == null || chainB == null;
	}

	@Override
	public boolean same(ITypeVariable other) {
		if (other instanceof ExpressionTypeVariable)
			return ((ExpressionTypeVariable)other).expression.equals(expression);
		else
			return false;
	}

	public static ITypeable typeableFromExpression(ASTNode referenceElm, Visitor visitor) {
		EntityRegion decRegion = referenceElm.entityAt(referenceElm.getLength()-1, visitor);
		if (decRegion != null && decRegion.entityAs(ITypeable.class) != null)
			return decRegion.entityAs(ITypeable.class);
		else
			return null;
	}

	@Override
	public void apply(boolean soft, Visitor visitor) {
		ITypeable typeable = typeableFromExpression(expression, visitor);
		if (typeable != null) {
			// don't apply typing to non-local things if only applying type information softly
			// this prevents assigning types to instance variables when only hovering over some function or something like that
			if (soft && !typeable.isLocal())
				return;
			// only set types of declarations inside the current index so definition references of one project
			// don't leak into a referenced base project (ClonkMars def referenced in ClonkRage or something)
			Index index = typeable.index();
			if (index == null || index != visitor.script().index())
				return;

			typeable.expectedToBeOfType(type, TypingJudgementMode.Force);
		}
	}

	@Override
	public String toString() {
		return String.format("[%s: %s]", expression.toString(), type.typeName(true));
	}

	public static ITypeVariable makeTypeInfo(Declaration declaration, Visitor visitor) {
		if (declaration != null)
			return new ExpressionTypeVariable(new AccessVar(declaration), visitor);
		else
			return null;
	}

	@Override
	public Declaration declaration(Visitor visitor) {
		return as(typeableFromExpression(expression, visitor), Declaration.class);
	}

}