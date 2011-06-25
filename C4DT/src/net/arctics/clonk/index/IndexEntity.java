package net.arctics.clonk.index;

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

	protected transient boolean notFullyLoaded = false;
	protected transient boolean dirty = false; 
	protected transient Index index;
	protected long entityId;
	
	public final void markAsDirty() {
		dirty = true;
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

	public final void requireLoaded() {
		if (notFullyLoaded) {
			notFullyLoaded = false;
			try {
				index.loadEntity(this);
			} catch (Exception e) {
				e.printStackTrace();
			}
		}
	}
	
	public synchronized final void saveIfDirty() {
		if (dirty) {
			dirty = false;
			try {
				this.save();
			} catch (IOException e) {
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
	public synchronized void save() throws IOException {
		if (index != null) {
			ObjectOutputStream s = index.getEntityOutputStream(this);
			try {
				save(s);
			} finally {
				s.close();
			}
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
		return obj.getClass() == this.getClass() && ((IndexEntity)obj).entityId == this.entityId;
	}
	
}
