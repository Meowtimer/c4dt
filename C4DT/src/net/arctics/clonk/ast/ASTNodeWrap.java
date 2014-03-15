package net.arctics.clonk.ast;

import net.arctics.clonk.Core;

public class ASTNodeWrap extends ASTNode {
	private static final long serialVersionUID = Core.SERIAL_VERSION_UID;
	private final Object wrapped;
	public ASTNodeWrap(Object wrapped) { this.wrapped = wrapped; }
	public Object wrapped() { return wrapped; }
	public static ASTNode wrap(Object value) {
		return value instanceof ASTNode ? (ASTNode)value : new ASTNodeWrap(value);
	}
	public static Object unwrap(ASTNode node) {
		return node instanceof ASTNodeWrap ? ((ASTNodeWrap)node).wrapped() : node;
	}
}
