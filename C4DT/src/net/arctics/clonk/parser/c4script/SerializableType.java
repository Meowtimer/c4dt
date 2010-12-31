package net.arctics.clonk.parser.c4script;

import java.io.Serializable;
import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import net.arctics.clonk.ClonkCore;
import net.arctics.clonk.index.C4Object;
import net.arctics.clonk.index.ClonkIndex;
import net.arctics.clonk.parser.C4ID;

public class SerializableType implements Serializable, IType {

	private static final long serialVersionUID = ClonkCore.SERIAL_VERSION_UID;
	
	private C4ID id;
	private transient WeakReference<C4Object> object;
	private transient IType completeType;
	private IType staticType;	
	
	public IType getStaticType() {
		return staticType;
	}
	
	public IType getCompleteType() {
		return completeType;
	}
	
	public void setCompleteType(IType completeType) {
		this.completeType = completeType;
		staticType = C4TypeSet.staticIngredients(completeType);
		setObject(C4TypeSet.objectIngredient(completeType));
	}
	
	public void setObject(C4Object object) {
		this.object = object != null ? new WeakReference<C4Object>(object) : null;
		this.id = object != null ? object.getId() : null;
	}
	
	public C4ID getId() {
		return id;
	}
	
	public C4Object getObject() {
		return object != null ? object.get() : null;
	}
	
	public void restoreType(C4ScriptBase context) {
		if (context.getResource() != null && context.getIndex() != null) {
			setObject(context.getIndex().getObjectNearestTo(context.getResource(), id));
		}
	}
	
	@Override
	public String toString() {
		return completeType != null ? completeType.toString() : "<Empty>";
	}

	@Override
	public Iterator<IType> iterator() {
		if (completeType != null) {
			return completeType.iterator();
		} else {
			List<IType> types = new ArrayList<IType>(2);
			if (staticType != null) {
				types.add(staticType);
			}
			C4Object obj = getObject();
			if (obj != null) {
				types.add(obj);
			}
			return types.iterator();
		}
	}

	@Override
	public boolean canBeAssignedFrom(IType other) {
		return completeType != null && completeType.canBeAssignedFrom(other);
	}

	@Override
	public String typeName(boolean special) {
		return completeType != null ? completeType.typeName(special) : "<Empty>";
	}

	@Override
	public boolean intersects(IType typeSet) {
		return completeType != null && completeType.intersects(typeSet);
	}

	@Override
	public boolean containsType(IType type) {
		return type.equals(this) || (completeType != null && completeType.containsType(type));
	}

	@Override
	public boolean containsAnyTypeOf(IType... types) {
		return completeType != null && completeType.containsAnyTypeOf(types);
	}

	@Override
	public int specificness() {
		return completeType != null ? completeType.specificness() : -1;
	}

	@Override
	public IType staticType() {
		return completeType != null ? completeType.staticType() : null;
	}

	@Override
	public IType serializableVersion(ClonkIndex indexToBeSerialized) {
		return this; // -.-;
	}
	
	static IType serializableTypeFrom(IType type, ClonkIndex index) {
		if (type.serializableVersion(index) == type) {
			return type;
		}
		SerializableType result = new SerializableType();
		result.setCompleteType(type);
		return result;
	}
	
}
