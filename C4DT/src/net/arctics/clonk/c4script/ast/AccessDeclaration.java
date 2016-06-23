package net.arctics.clonk.c4script.ast;

import static net.arctics.clonk.util.Utilities.as;

import org.eclipse.jface.text.IRegion;
import org.eclipse.jface.text.Region;

import net.arctics.clonk.Core;
import net.arctics.clonk.ast.ASTNode;
import net.arctics.clonk.ast.ASTNodePrinter;
import net.arctics.clonk.ast.Declaration;
import net.arctics.clonk.ast.EntityRegion;
import net.arctics.clonk.ast.ExpressionLocator;
import net.arctics.clonk.ast.IPlaceholderPatternMatchTarget;
import net.arctics.clonk.c4script.Function;
import net.arctics.clonk.c4script.Variable;
import net.arctics.clonk.index.serialization.replacements.IDeferredDeclaration;

/**
 * An expression referring to some {@link Declaration}. Derived classes represent access to {@link Variable}s ({@link AccessVar}) and {@link Function} calls ({@link CallDeclaration}).
 * @author madeen
 *
 */
public abstract class AccessDeclaration extends ASTNode implements IPlaceholderPatternMatchTarget {

	private static final long serialVersionUID = Core.SERIAL_VERSION_UID;
	protected Declaration declaration;
	protected String declarationName;

	/**
	 * Return the {@link Declaration} if it has already been obtained or null.
	 * @return The obtain declaration or null.
	 */
	public Declaration declaration() { return declaration; }
	/**
	 * Assign a declaration. Called from obtainment logic
	 * @param d The declaration to assign this node
	 */
	public void setDeclaration(final Declaration d) { this.declaration = d; }

	/**
	 * Create AccessDeclaration object using a declaration name.
	 * @param declarationName
	 */
	public AccessDeclaration(final String declarationName) {
		this.declarationName = declarationName;
	}

	@Override
	public void doPrint(final ASTNodePrinter output, final int depth) {
		output.append(name());
	}

	public IRegion declarationRegion(final int offset) {
		return new Region(offset+start(), name().length());
	}

	@Override
	public EntityRegion entityAt(final int offset, final ExpressionLocator<?> locator) {
		return new EntityRegion(declaration(), region(0));
	}

	/**
	 * Return the declaration name this expression uses to refer to a {@link Declaration}.
	 * @return The declaration name
	 */
	public String name() { return declarationName; }

	/**
	 * Set the declaration name.
	 * @param declarationName The name
	 */
	public void setName(final String declarationName) { this.declarationName = declarationName; }
	@Override
	public int identifierLength() { return name().length(); }

	/**
	 * Return whether this expression only indirectly refers to a declaration (e.g. inherited/_inherited)
	 * @return Whether or not.
	 */
	public boolean indirectAccess() {
		return declaration == null || !declaration.name().equals(name());
	}

	@Override
	public boolean equalAttributes(final ASTNode other) {
		if (!super.equalAttributes(other)) {
			return false;
		}
		final AccessDeclaration otherDec = (AccessDeclaration) other;
		if (!name().equals(otherDec.declarationName)) {
			return false;
		}
		return true;
	}

	/**
	 * Returns the class declarations referenced by this {@link AccessDeclaration} need to be instances of.
	 * @return The {@link Declaration} class
	 */
	public Class<? extends Declaration> declarationClass() { return Declaration.class; }
	@Override
	public String patternMatchingText() { return name(); }

	@Override
	public void postLoad(final ASTNode parent) {
		super.postLoad(parent);
		if (declaration instanceof IDeferredDeclaration) {
			declaration = as(((IDeferredDeclaration)declaration).resolve(), Declaration.class);
		}
	}

}