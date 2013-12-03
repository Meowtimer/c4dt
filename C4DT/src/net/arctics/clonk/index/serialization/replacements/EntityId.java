package net.arctics.clonk.index.serialization.replacements;

import java.io.Serializable;

import net.arctics.clonk.Core;
import net.arctics.clonk.index.IDeserializationResolvable;
import net.arctics.clonk.index.Index;
import net.arctics.clonk.index.IndexEntity;
import net.arctics.clonk.util.Utilities;

public class EntityId implements Serializable, IDeserializationResolvable {
	private static final long serialVersionUID = Core.SERIAL_VERSION_UID;
	protected long referencedEntityId;
	protected Object referencedEntityToken;
	public EntityId(final IndexEntity referencedEntity) {
		this.referencedEntityId = referencedEntity.entityId();
		this.referencedEntityToken = referencedEntity.additionalEntityIdentificationToken();
	}
	@Override
	public String toString() {
		return String.format("(%d, %s)", referencedEntityId, referencedEntityToken != null ? referencedEntityToken.toString() : "<No Token>");
	}
	protected Index index(final Index context) {
		return context; // ;>
	}
	@Override
	public IndexEntity resolve(final Index index, final IndexEntity deserializee) {
		IndexEntity result = null;
		final Index externalIndex = index(index);
		if (externalIndex != null) {
			result = externalIndex.entityWithId(referencedEntityId);
			if (result == null || !Utilities.eq(result.additionalEntityIdentificationToken(), referencedEntityToken))
				if (referencedEntityToken != null)
					for (final IndexEntity e : externalIndex.entities()) {
						final Object token = e.additionalEntityIdentificationToken();
						if (e != null && referencedEntityToken.equals(token)) {
							result = e;
							break;
						}
					}
		}
		else
			System.out.println(String.format("Warning: Failed to obtain index when resolving '%s' from '%s'", this.toString(), index.name()));
		return result;
	}
}