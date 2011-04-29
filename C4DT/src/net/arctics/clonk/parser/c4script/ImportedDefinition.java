package net.arctics.clonk.parser.c4script;

import java.io.Serializable;
import java.lang.ref.WeakReference;
import java.util.Iterator;
import net.arctics.clonk.ClonkCore;
import net.arctics.clonk.index.Definition;
import net.arctics.clonk.index.ClonkIndex;
import net.arctics.clonk.parser.ID;
import net.arctics.clonk.resource.ClonkProjectNature;

public class ImportedDefinition implements Serializable, IType {

	private static final long serialVersionUID = ClonkCore.SERIAL_VERSION_UID;
	
	private ID id;
	private transient WeakReference<Definition> definition;
	private String referencedProject;
	
	private ImportedDefinition(Definition obj) {
		setDefinition(obj);
	}
	
	public IType getStaticType() {
		return PrimitiveType.OBJECT;
	}

	public void setDefinition(Definition definition) {
		if (definition != null) {
			this.definition = new WeakReference<Definition>(definition);
			this.id = definition.id();
			this.referencedProject = definition.getIndex().getProject().getName();
		} else {
			this.definition = null;
			this.id = null;
			this.referencedProject = null;
		}
	}
	
	public ID getId() {
		return id;
	}
	
	public Definition getDefinition() {
		return definition != null ? definition.get() : null;
	}

	@Override
	public String toString() {
		return getDefinition() != null ? String.format("<Import: %s>", getDefinition().getPath().toString()) : "<Empty>";
	}

	@Override
	public Iterator<IType> iterator() {
		if (getDefinition() != null) {
			return getDefinition().iterator();
		} else {
			return PrimitiveType.OBJECT.iterator();
		}
	}

	@Override
	public boolean canBeAssignedFrom(IType other) {
		return getDefinition() != null && getDefinition().canBeAssignedFrom(other);
	}

	@Override
	public String typeName(boolean special) {
		return (getDefinition() != null ? getDefinition() : PrimitiveType.OBJECT).typeName(special);
	}

	@Override
	public boolean intersects(IType typeSet) {
		return getDefinition() != null && getDefinition().intersects(typeSet);
	}

	@Override
	public boolean containsType(IType type) {
		return type.equals(this) || (getDefinition() != null && getDefinition().containsType(type));
	}

	@Override
	public boolean containsAnyTypeOf(IType... types) {
		return getDefinition() != null ? getDefinition().containsAnyTypeOf(types) : false;
	}

	@Override
	public int specificness() {
		return getDefinition() != null ? getDefinition().specificness() : -1;
	}

	@Override
	public IType staticType() {
		return getDefinition() != null ? getDefinition().staticType() : null;
	}
	
	public IType resolve() {
		IType result = null;
		ClonkProjectNature externalNature = ClonkProjectNature.get(referencedProject);
		if (externalNature != null) {
			ClonkIndex index = externalNature.getIndex();
			if (index != null) {
				result = index.getDefinitionFromEverywhere(id);
				if (result == null) {
					System.out.println(String.format("Couldn't find object %s in index for %s", id.toString(), index.getProject().getName()));
				}
			}
			else
				System.out.println(String.format("Warning: Failed to obtain index for %s when resolving %s", referencedProject, id.toString()));
		}
		// return null if nothing found instead of PrimitiveType.OBJECT so there won't be class cast exceptions

		// reasons for this method unexpectedly returning null might be that "Clean all projects" is initiated before
		// indexes for some projects have been loaded. Cleaning those projects then triggers loading the index but at that moment, the index of
		// the project the definition has been imported from is already cleared so that index.getObjectFromEverywhere fails to
		// find the imported definition.
		
		return result; 
	}
	
	public static IType getSerializableType(ClonkIndex indexBeingSerialized, Definition obj) {
		if (obj.getIndex() != indexBeingSerialized) {
			return new ImportedDefinition(obj);
		} else {
			return obj;
		}
	}
	
}
