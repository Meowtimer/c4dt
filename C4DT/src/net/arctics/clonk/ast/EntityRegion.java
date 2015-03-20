package net.arctics.clonk.ast;


import static net.arctics.clonk.util.Utilities.as;

import java.util.Set;

import net.arctics.clonk.index.IIndexEntity;

import org.eclipse.jface.text.IRegion;
import org.eclipse.jface.text.Region;

/**
 * A region in some document referring to one or potentially many {@link IIndexEntity}s.
 * @author madeen
 *
 */
public final class EntityRegion {
	private IIndexEntity entity;
	private IRegion region;
	private String text;
	private Set<? extends IIndexEntity> potentialEntities;
	/**
	 * Return the {@link #entity()} cast to <T>.
	 * @param cls Class specifying what to cast {@link #entity()} to.
	 * @param <T> Type to cast {@link #entity()} to.
	 * @return {@link #entity()} cast to {@link Declaration} or null
	 */
	public <T> T entityAs(final Class<T> cls) {
		return as(entity, cls);
	}
	/**
	 * Return the {@link IIndexEntity} this region refers to.
	 * @return The entity
	 */
	public IIndexEntity entity() {
		return entity;
	}
	/**
	 * Create a region with an entity, a text region and text representing the it.
	 * @param entity The entity
	 * @param region The text region
	 * @param text The text
	 */
	public EntityRegion(final IIndexEntity entity, final IRegion region, final String text) {
		super();
		this.entity = entity;
		this.region = region;
		this.text = text;
	}
	/**
	 * Same as {@link #EntityRegion(IIndexEntity, IRegion, String)}, but with {@link #text()} set to null.
	 * @param entity The entity
	 * @param region The text region
	 */
	public EntityRegion(final IIndexEntity entity, final IRegion region) {
		this(entity, region, null);
	}
	/**
	 * Create new declaration region with a list of potential declarations and an {@link IRegion} object. If the list only contains one item the {@link #entity()} field
	 * will be set to this item and {@link #potentialEntities()} will be left as null. For more than one element, {@link #potentialEntities()} will be set to the parameter and {@link #entity()} will be left as null.
	 * @param potentialEntities The list of potential {@link IIndexEntity}s
	 * @param region The text region
	 */
	public EntityRegion(final Set<? extends IIndexEntity> potentialEntities, final IRegion region) {
		if (potentialEntities.size() == 1)
			for (final IIndexEntity d : potentialEntities) {
				this.entity = d;
				break;
			}
		else
			this.potentialEntities = potentialEntities;
		this.region = region;
	}

	/**
	 * Initialize a region with only {@link #entity()} set.
	 * @param entity The entity
	 */
	public EntityRegion(final IIndexEntity entity) {
		this(entity, null, null);
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
	/**
	 * Set {@link #entity()}.
	 * @param entity The entity to set {@link #entity()} to.
	 */
	public void setEntity(final IIndexEntity entity) {
		this.entity = entity;
	}
	/**
	 * Increment the text region of this entity region by the specified amount.
	 * @param offset The amount to increase by
	 * @return Returns this.
	 */
	public EntityRegion incrementRegionBy(final int offset) {
		region = new Region(region.getOffset()+offset, region.getLength());
		return this;
	}
	/**
	 * Return a list of {@link IIndexEntity}s this region could refer to.
	 * @return The list.
	 */
	public Set<? extends IIndexEntity> potentialEntities() {
		return potentialEntities;
	}

	@Override
	public String toString() {
		return
			entity != null && region != null ? String.format("%s@(%s) %s", entity.toString(), region.toString(), text) : //$NON-NLS-1$
			potentialEntities != null ? String.format("<%d potential regions>@(%s) %s", potentialEntities.size(), region.toString(), text) : //$NON-NLS-1$
			"Empty DeclarationRegion"; //$NON-NLS-1$
	}
}