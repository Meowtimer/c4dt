package net.arctics.clonk.parser;


import java.util.Set;

import net.arctics.clonk.parser.c4script.IIndexEntity;
import net.arctics.clonk.parser.c4script.ITypeable;

import org.eclipse.jface.text.IRegion;
import org.eclipse.jface.text.Region;

/**
 * A region in some document referring to one or potentially many declarations.
 * @author madeen
 *
 */
public final class EntityRegion {
	private IIndexEntity entity;
	private IRegion region;
	private String text;
	private Set<IIndexEntity> potentialEntities;
	public Declaration concreteDeclaration() {
		if (entity instanceof Declaration)
			return (Declaration)entity;
		else
			return null;
	}
	public IIndexEntity entity() {
		return entity;
	}
	public ITypeable typedDeclaration() {
		if (entity instanceof ITypeable)
			return (ITypeable)entity;
		else
			return null;
	}
	public EntityRegion(IIndexEntity declaration, IRegion region, String text) {
		super();
		this.entity = declaration;
		this.region = region;
		this.text = text;
	}
	public EntityRegion(IIndexEntity declaration, IRegion region) {
		this(declaration, region, null);
	}
	/**
	 * Create new declaration region with a list of potential declarations and an IRegion object. If the list only contains one item the declaration field
	 * will be set to this item and potentialDeclarations will be left as null. For more than one element, potentialDeclarations will be set to the parameter and declaration will be left as null.
	 * @param potentialDeclarations The list of potential declarations 
	 * @param region The text region
	 */
	public EntityRegion(Set<IIndexEntity> potentialDeclarations, IRegion region) {
		if (potentialDeclarations.size() == 1)
			for (IIndexEntity d : potentialDeclarations) {
				this.entity = d;
				break;
			}
		else
			this.potentialEntities = potentialDeclarations;
		this.region = region;
	}
	public EntityRegion(ITypeable typedDeclaration) {
		this.entity = typedDeclaration;
	}
	/**
	 * The text region.
	 * @return The region
	 */
	public IRegion region() {
		return region;
	}
	/**
	 * Text of the document at the specified region.
	 * @return The text
	 */
	public String text() {
		return text;
	}
	public void setEntity(IIndexEntity declaration) {
		this.entity = declaration;
	}
	public EntityRegion incrementRegionBy(int offset) {
		region = new Region(region.getOffset()+offset, region.getLength());
		return this;
	}
	/**
	 * Return a list of declarations this region could refer to.
	 * @return The list.
	 */
	public Set<IIndexEntity> potentialEntities() {
		return potentialEntities;
	}
	@Override
	public String toString() {
		if (entity != null && region != null)
			return String.format("%s@(%s) %s", entity.toString(), region.toString(), text); //$NON-NLS-1$
		else if (potentialEntities != null)
			return String.format("<%d potential regions>@(%s) %s", potentialEntities.size(), region.toString(), text); //$NON-NLS-1$
		else
			return "Empty DeclarationRegion"; //$NON-NLS-1$
	}
}