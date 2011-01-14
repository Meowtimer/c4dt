package net.arctics.clonk.parser.c4script;

import java.io.Serializable;
import java.lang.ref.WeakReference;
import java.util.Iterator;
import net.arctics.clonk.ClonkCore;
import net.arctics.clonk.index.Definition;
import net.arctics.clonk.index.ClonkIndex;
import net.arctics.clonk.parser.ID;
import net.arctics.clonk.resource.ClonkProjectNature;

public class ImportedObject implements Serializable, IType {

	private static final long serialVersionUID = ClonkCore.SERIAL_VERSION_UID;
	
	private ID id;
	private transient WeakReference<Definition> object;
	private String referencedProject;
	
	private ImportedObject(Definition obj) {
		setObject(obj);
	}
	
	public IType getStaticType() {
		return PrimitiveType.OBJECT;
	}

	public void setObject(Definition object) {
		if (object != null) {
			this.object = new WeakReference<Definition>(object);
			this.id = object.getId();
			this.referencedProject = object.getIndex().getProject().getName();
		} else {
			this.object = null;
			this.id = null;
			this.referencedProject = null;
		}
	}
	
	public ID getId() {
		return id;
	}
	
	public Definition getObject() {
		return object != null ? object.get() : null;
	}

	@Override
	public String toString() {
		return getObject() != null ? String.format("<Import: %s>", getObject().getPath().toString()) : "<Empty>";
	}

	@Override
	public Iterator<IType> iterator() {
		if (getObject() != null) {
			return getObject().iterator();
		} else {
			return PrimitiveType.OBJECT.iterator();
		}
	}

	@Override
	public boolean canBeAssignedFrom(IType other) {
		return getObject() != null && getObject().canBeAssignedFrom(other);
	}

	@Override
	public String typeName(boolean special) {
		return (getObject() != null ? getObject() : PrimitiveType.OBJECT).typeName(special);
	}

	@Override
	public boolean intersects(IType typeSet) {
		return getObject() != null && getObject().intersects(typeSet);
	}

	@Override
	public boolean containsType(IType type) {
		return type.equals(this) || (getObject() != null && getObject().containsType(type));
	}

	@Override
	public boolean containsAnyTypeOf(IType... types) {
		return getObject() != null ? getObject().containsAnyTypeOf(types) : false;
	}

	@Override
	public int specificness() {
		return getObject() != null ? getObject().specificness() : -1;
	}

	@Override
	public IType staticType() {
		return getObject() != null ? getObject().staticType() : null;
	}
	
	public IType resolve() {
		IType result = null;
		ClonkProjectNature externalNature = ClonkProjectNature.get(referencedProject);
		if (externalNature != null) {
			ClonkIndex index = externalNature.getIndex();
			if (index != null)
				result = index.getObjectFromEverywhere(id);
			else
				System.out.println(String.format("Warning: Failed to obtain index for %s when resolving %s", referencedProject, id.toString()));
		}
		return result != null ? result : PrimitiveType.OBJECT;
	}
	
	public static IType getSerializableType(ClonkIndex indexBeingSerialized, Definition obj) {
		if (obj.getIndex() != indexBeingSerialized) {
			return new ImportedObject(obj);
		} else {
			return obj;
		}
	}
	
}
