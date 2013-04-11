package net.arctics.clonk.index;

import net.arctics.clonk.Core;
import net.arctics.clonk.parser.Declaration;

public class DeferredDeclaration extends Declaration {
	private static final long serialVersionUID = Core.SERIAL_VERSION_UID;
	private transient IndexEntity entity;
	public DeferredDeclaration(IndexEntity entity, String path) {
		super();
		this.entity = entity;
		this.setName(path);
		//System.out.println(String.format("Creating deferred declaration for %s::%s", entity.qualifiedName(), path));
	}
	public Declaration resolve() {
		final Declaration d = entity.findDeclarationByPath(name(), Declaration.class);
		if (d == null) {
			System.out.println(String.format("Failed to resolve %s::%s", entity.qualifiedName(), name()));
			entity.findDeclarationByPath(name(), Declaration.class);
		}
		return d;
	}
}
