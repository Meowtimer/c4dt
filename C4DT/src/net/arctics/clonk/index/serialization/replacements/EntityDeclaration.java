package net.arctics.clonk.index.serialization.replacements;

import java.io.Serializable;

import net.arctics.clonk.Core;
import net.arctics.clonk.index.DeferredDeclaration;
import net.arctics.clonk.index.IDeserializationResolvable;
import net.arctics.clonk.index.Index;
import net.arctics.clonk.index.IndexEntity;
import net.arctics.clonk.parser.Declaration;
import net.arctics.clonk.parser.c4script.IType;

public class EntityDeclaration implements Serializable, IDeserializationResolvable {
	private static final long serialVersionUID = Core.SERIAL_VERSION_UID;
	private final IndexEntity containingEntity;
	private final String declarationPath;
	private final Class<? extends Declaration> declarationClass;
	public EntityDeclaration(Declaration declaration, IndexEntity containingEntity) {
		this.containingEntity = containingEntity;
		this.declarationPath = declaration.pathRelativeToIndexEntity();
		this.declarationClass = declaration.getClass();
	}
	@Override
	public Declaration resolve(Index index) {
		Declaration result;
		if (containingEntity != null) {
			containingEntity.requireLoaded();
			result = containingEntity.findDeclarationByPath(declarationPath, declarationClass);
		} else
			result = null;
		if (result == null)
			if (containingEntity != null && !IType.class.isAssignableFrom(declarationClass))
				return new DeferredDeclaration(containingEntity, declarationPath);
			else
				System.out.println(String.format("Giving up on resolving '%s::%s'",
					containingEntity != null ? containingEntity.qualifiedName() : "<null>",
					declarationPath
				));
		return result;
	}
}