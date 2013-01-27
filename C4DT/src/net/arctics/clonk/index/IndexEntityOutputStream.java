package net.arctics.clonk.index;

import java.io.IOException;
import java.io.ObjectOutputStream;
import java.io.OutputStream;

import net.arctics.clonk.parser.ASTNode;
import net.arctics.clonk.parser.Declaration;

public class IndexEntityOutputStream extends ObjectOutputStream {
	private final Index index;
	private final IndexEntity entity;

	public IndexEntityOutputStream(Index index, IndexEntity entity, OutputStream output) throws IOException {
		super(output);
		this.index = index;
		this.entity = entity;
		enableReplaceObject(true);
	}

	@Override
	protected Object replaceObject(Object obj) throws IOException {
		try {
			if (obj instanceof IReplacedWhenSaved)
				return ((IReplacedWhenSaved)obj).saveReplacement();
			if (obj instanceof Declaration && !(obj instanceof Index))
				return index.saveReplacementForEntityDeclaration((Declaration)obj, entity);
			if (entity != null && obj instanceof ASTNode) {
				ASTNode elm = (ASTNode)obj;
				Declaration owner = elm.owningDeclaration();
				if (owner != null && !owner.containedIn(entity))
					return new ASTNode.Ticket(owner, elm);
			}
			if (obj instanceof String)
				return ((String)obj).intern();

			return super.replaceObject(obj);
		} catch (Exception e) {
			e.printStackTrace();
			return null;
		}
	}

}
