package net.arctics.clonk.index;

import static net.arctics.clonk.util.Utilities.as;

import java.io.Serializable;

import net.arctics.clonk.Core;
import net.arctics.clonk.parser.Structure;
import net.arctics.clonk.parser.c4script.IType;
import net.arctics.clonk.parser.c4script.Variable;
import net.arctics.clonk.util.IHasRelatedResource;

import org.eclipse.core.resources.IResource;

public class StructureVariable extends Variable implements IReplacedWhenSaved {
	protected static class Ticket implements Serializable, IDeserializationResolvable {
		private static final long serialVersionUID = Core.SERIAL_VERSION_UID;
		private final String name, resourcePath;
		public Ticket(String name, String resourcePath) {
			super();
			this.name = name;
			this.resourcePath = resourcePath;
		}
		@Override
		public Object resolve(Index index, IndexEntity deserializee) {
			ProjectIndex prj = as(index, ProjectIndex.class);
			if (prj != null) {
				IResource res = prj.nature().getProject().findMember(resourcePath);
				if (res != null) {
					Structure structure = Structure.pinned(res, true, false);
					if (structure != null)
						return structure.findLocalDeclaration(name, Variable.class);
				}
			}
			return null;
		}
	}
	private static final long serialVersionUID = Core.SERIAL_VERSION_UID;
	public StructureVariable(String name, IType type) { super(name, type); }
	@Override
	public Object saveReplacement(Index context) {
		return new Ticket(name(), ((IHasRelatedResource)parent()).resource().getProjectRelativePath().toOSString());
	}
}
