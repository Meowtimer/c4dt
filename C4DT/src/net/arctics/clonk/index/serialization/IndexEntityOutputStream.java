package net.arctics.clonk.index.serialization;

import java.io.IOException;
import java.io.ObjectOutputStream;
import java.io.OutputStream;

import net.arctics.clonk.ast.ASTNode;
import net.arctics.clonk.ast.Declaration;
import net.arctics.clonk.index.IReplacedWhenSaved;
import net.arctics.clonk.index.Index;
import net.arctics.clonk.index.IndexEntity;
import net.arctics.clonk.index.serialization.replacements.IDeferredDeclaration;

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
			if (obj instanceof IDeferredDeclaration) {
				final IDeferredDeclaration deferred = (IDeferredDeclaration)obj;
				obj = deferred.resolve();
				if (obj == null || obj instanceof IDeferredDeclaration)
					throw new IllegalStateException(String.format("Deferred declaration while serializing: %s", deferred.toString()));
			}
			if (obj instanceof IReplacedWhenSaved)
				return ((IReplacedWhenSaved)obj).saveReplacement(index);
			if (obj instanceof Declaration && !(obj instanceof Index))
				return index.saveReplacementForEntityDeclaration((Declaration)obj, entity);
			if (entity != null && obj instanceof ASTNode) {
				final ASTNode elm = (ASTNode)obj;
				final Declaration owner = elm.owner();
				if (owner != null && !owner.containedIn(entity))
					return new ASTNodeTicket(owner, elm);
			}

			return super.replaceObject(obj);
		} catch (final Exception e) {
			e.printStackTrace();
			return null;
		}
	}

}
