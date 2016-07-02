package net.arctics.clonk.index;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.security.InvalidParameterException;

import net.arctics.clonk.Core;
import net.arctics.clonk.ast.Declaration;
import net.arctics.clonk.ast.Structure;
import net.arctics.clonk.util.Sink;

/**
 * Entity in a {@link Index} which is stored in its own file inside the \<name\>.index folder
 * @author madeen
 */
public abstract class IndexEntity extends Structure implements IReplacedWhenSaved {

	private static final long serialVersionUID = Core.SERIAL_VERSION_UID;

	public interface TopLevelEntity {}

	public static abstract class LoadedEntitiesSink<T extends IndexEntity> implements Sink<T> {
		@Override
		public boolean filter(final T item) { return item.loaded == Loaded.Yes; }
	}

	protected enum Loaded {
		No,
		Pending,
		Yes
	}

	/**
	 * Flag indicating whether the entity was loaded already loaded from disk or not.
	 */
	protected transient Loaded loaded = Loaded.Yes;
	protected transient Index index;
	protected long entityId;

	public IndexEntity(final Index index) {
		this.index = index;
		if (index != null) {
			entityId = index.addEntity(this);
		} else if (!(this instanceof TopLevelEntity)) {
			throw new InvalidParameterException("index");
		}
	}

	@Override
	public final Index index() { return index; }

	public void forceIndex(Index index) {
		this.index = index;
	}

	/**
	 * Require this entity to be loaded. If {@link #loaded} is false it is set to true and {@link Index#loadEntity(IndexEntity)} is called.
	 */
	public final void requireLoaded() {
		if (index == null || loaded == Loaded.Yes) {
			return;
		}
		synchronized (index.loadSynchronizer()) {
			if (loaded == Loaded.No) {
				loaded = Loaded.Pending;
				try {
					index.loadEntity(this);
				} catch (final Exception e) {
					if (e instanceof FileNotFoundException) {
						System.out.println("Entity file for " + this.toString() + " not found");
					}
					e.printStackTrace();
				}
				loaded = Loaded.Yes;
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
	public void save(final ObjectOutputStream stream) throws IOException {
		// override
	}

	/**
	 * Save this entity by requesting a stream to write to from the {@link #index()}.
	 * @throws IOException
	 */
	public final void save() throws IOException {
		if (index == null) {
			return;
		}
		synchronized (index.saveSynchronizer()) {
			requireLoaded();
			index.saveEntity(this);
		}
	}

	/**
	 * Counterpart to {@link #load(ObjectInputStream)}. This method is required to load fields in the same order as they were saved by {@link #save(ObjectOutputStream)}.
	 * @param stream The stream to load the entity's state from
	 * @throws IOException
	 * @throws ClassNotFoundException
	 */
	public void load(final ObjectInputStream stream) throws IOException, ClassNotFoundException {
	}

	@Override
	public int hashCode() {
		return (int) entityId;
	}

	@Override
	public boolean equals(final Object obj) {
		if (obj == this) {
			return true;
		}
		if (obj == null) {
			return false;
		}
		return obj.getClass() == this.getClass() && ((IndexEntity)obj).entityId == this.entityId && ((IndexEntity)obj).index == this.index;
	}

	/**
	 * Return whether {@link #save()} will be called by the {@link Index}.
	 * @return True or false.
	 */
	public boolean saveCalledByIndex() {
		return false;
	}

	@Override
	public void postLoad(final Declaration parent, final Index root) {
		this.index = root;
		super.postLoad(parent, root);
	}

	@Override
	public Object saveReplacement(final Index context) { return context.saveReplacementForEntity(this); }

}
