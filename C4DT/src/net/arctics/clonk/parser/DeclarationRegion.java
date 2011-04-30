package net.arctics.clonk.parser;


import java.util.List;

import net.arctics.clonk.parser.c4script.IEntityLocatedInIndex;
import net.arctics.clonk.parser.c4script.ITypeable;

import org.eclipse.jface.text.IRegion;
import org.eclipse.jface.text.Region;

/**
 * A region in some document referring to one or potentially many declarations.
 * @author madeen
 *
 */
public final class DeclarationRegion {
	private transient IEntityLocatedInIndex declaration;
	private IRegion region;
	private String text;
	private List<Declaration> potentialDeclarations;
	public Declaration getConcreteDeclaration() {
		if (declaration instanceof Declaration)
			return (Declaration)declaration;
		else
			return null;
	}
	public ITypeable getTypedDeclaration() {
		if (declaration instanceof ITypeable)
			return (ITypeable)declaration;
		else
			return null;
	}
	public DeclarationRegion(Declaration declaration, IRegion region, String text) {
		super();
		this.declaration = declaration;
		this.region = region;
		this.text = text;
	}
	public DeclarationRegion(Declaration declaration, IRegion region) {
		this(declaration, region, null);
	}
	/**
	 * Create new declaration region with a list of potential declarations and an IRegion object. If the list only contains one item the declaration field
	 * will be set to this item and potentialDeclarations will be left as null. For more than one element, potentialDeclarations will be set to the parameter and declaration will be left as null.
	 * @param potentialDeclarations The list of potential declarations 
	 * @param region The text region
	 */
	public DeclarationRegion(List<Declaration> potentialDeclarations, IRegion region) {
		super();
		if (potentialDeclarations.size() == 1)
			this.declaration = potentialDeclarations.get(0);
		else
			this.potentialDeclarations = potentialDeclarations;
		this.region = region;
	}
	public DeclarationRegion(ITypeable typedDeclaration) {
		this.declaration = typedDeclaration;
	}
	/**
	 * The text region.
	 * @return The region
	 */
	public IRegion getRegion() {
		return region;
	}
	/**
	 * Text of the document at the specified region.
	 * @return The text
	 */
	public String getText() {
		return text;
	}
	public void setDeclaration(Declaration declaration) {
		this.declaration = declaration;
	}
	public DeclarationRegion addOffsetInplace(int offset) {
		region = new Region(region.getOffset()+offset, region.getLength());
		return this;
	}
	/**
	 * Return a list of declarations this region could refer to.
	 * @return The list.
	 */
	public List<Declaration> getPotentialDeclarations() {
		return potentialDeclarations;
	}
	@Override
	public String toString() {
		if (declaration != null && region != null)
			return declaration.toString() + "@(" + region.toString() + ")" + (text != null ? "("+text+")" : ""); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$ //$NON-NLS-4$ //$NON-NLS-5$
		else
			return "Empty DeclarationRegion"; //$NON-NLS-1$
	}
}