package net.arctics.clonk.index.serialization.replacements;

import net.arctics.clonk.Core;
import net.arctics.clonk.builder.ClonkProjectNature;
import net.arctics.clonk.index.Index;
import net.arctics.clonk.index.IndexEntity;

public class EntityReference extends EntityId {
	private static final long serialVersionUID = Core.SERIAL_VERSION_UID;
	protected String referencedProjectName;
	public EntityReference(IndexEntity referencedEntity) {
		super(referencedEntity);
		if (referencedEntity != null && referencedEntity.index() != null)
			referencedProjectName = referencedEntity.index().name();
	}
	@Override
	protected Index index(Index context) {
		if (referencedProjectName != null) {
			final ClonkProjectNature nat = ClonkProjectNature.get(referencedProjectName);
			return nat != null ? nat.index() : null;
		} else
			return null;
	}
	@Override
	public String toString() {
		return String.format("(%s, %d, %s)", referencedProjectName, referencedEntityId, referencedEntityToken != null ? referencedEntityToken.toString() : "<No Token>");
	}
}