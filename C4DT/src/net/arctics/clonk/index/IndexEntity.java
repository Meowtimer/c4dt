package net.arctics.clonk.index;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import net.arctics.clonk.parser.Structure;

/**
 * Entity in a {@link Index} which is stored in its own file inside the \<name\>.index folder
 * @author madeen
 *
 */
public abstract class IndexEntity extends Structure {

	private static final long serialVersionUID = 1L;

	/**
	 * Flag indicating whether the entity was loaded already loaded from disk or not.
	 */
	protected transient boolean notFullyLoaded = false;
	protected transient boolean dirty = false; 
	protected transient Index index;
	protected long entityId;
	
	/**
	 * Mark this entity as being out of sync with the file it was read from.
	 * @param flag Dirty or not dirty.
	 */
	@Override
	public void markAsDirty() {
		dirty = true;
	}
	
	/**
	 * Mark this entity as not dirty (@see {@link #markAsDirty()})
	 */
	public void notDirty() {
		dirty = false;
	}

	@Override
	public boolean isDirty() {
		return dirty;
	}
	
	public IndexEntity(Index index) {
		this.index = index;
		if (index != null)
			entityId = index.addEntityReturningId(this);
	}
	
	@Override
	public final Index getIndex() {
		return index;
	}

	/**
	 * Require this entity to be loaded. If {@link #notFullyLoaded} is true it is set to false and {@link Index#loadEntity(IndexEntity)} is called.
	 */
	public final synchronized void requireLoaded() {
		if (notFullyLoaded) {
			notFullyLoaded = false;
			try {
				index.loadEntity(this);
			} catch (Exception e) {
				if (e instanceof FileNotFoundException)
					System.out.println("Entity file for " + this.toString() + " not found");
				e.printStackTrace();
			}
		}
	}
	
	/**
	 * Return the index-specific id of this entity.
	 * @return The identifier
	 */
	public final long entityId() {
		return entityId;
	}
	
	/**
	 * Return an additional object helping with identifying this entity uniquely.
	 * This is to avoid cases where the {@link #entityId()} changed and identifying the entity by this id would result in erroneous results.
	 * This token, if saved, should be saved in the index file and not the entity specific file so it's readily available.
	 * @return The additional identification token
	 */
	public Object additionalEntityIdentificationToken() {
		return null;
	}
	
	/**
	 * Save the state of this entity to the stream. This method won't automatically serialize the whole object.
	 * Individual fields need to be written explicitly in derived classes.
	 * Those fields handled by {@link #save(ObjectOutputStream)}/{@link #load(ObjectInputStream)} need to be declared 'transient'.
	 * Other non-transient fields will be serialized automatically by the index in a single file. Those fields should not be too expensive to load since all
	 * the index entities and their non-transient fields will be loaded at once, potentially making loading large indexes a hassle.
	 * @param stream The stream to save the entity's state to
	 * @throws IOException 
	 */
	public synchronized void save(ObjectOutputStream stream) throws IOException {
		// override
	}
	
	/**
	 * Save this entity by requesting a stream to write to from the index.
	 * @throws IOException 
	 */
	public final synchronized void save() throws IOException {
		if (index != null) try {
			requireLoaded();
			index.saveEntity(this);
		} catch (Exception e) {
			e.printStackTrace();
		}
	}
	
	/**
	 * Counterpart to {@link #load(ObjectInputStream)}. This method is required to load fields in the same order as they were saved by {@link #save(ObjectOutputStream)}.
	 * @param stream The stream to load the entity's state from
	 * @throws IOException
	 * @throws ClassNotFoundException
	 */
	public void load(ObjectInputStream stream) throws IOException, ClassNotFoundException {
	}
	
	@Override
	public int hashCode() {
		return (int) entityId;
	}
	
	@Override
	public boolean equals(Object obj) {
		if (obj == null)
			return false;
		return obj.getClass() == this.getClass() && ((IndexEntity)obj).entityId == this.entityId && ((IndexEntity)obj).index == this.index;
	}
	
	/**
	 * Return whether {@link #save()} will be called by the {@link Index}.
	 * @return True or false.
	 */
	public boolean saveCalledByIndex() {
		return false;
	}
	
}
