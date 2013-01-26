package net.arctics.clonk.index;

import java.io.IOException;
import java.io.ObjectOutputStream;
import java.io.OutputStream;

import net.arctics.clonk.parser.Declaration;
import net.arctics.clonk.parser.ASTNode;
import net.arctics.clonk.parser.c4script.inference.dabble.SpecialEngineRules.SpecialRule;

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
			if (obj instanceof IndexEntity)
				return index.saveReplacementForEntity((IndexEntity)obj);
			else if (obj instanceof Declaration && !(obj instanceof Index))
				return index.saveReplacementForEntityDeclaration((Declaration)obj, entity);
			else if (obj instanceof SpecialRule)
				return new SpecialRule.Ticket((SpecialRule)obj);
			else if (entity != null && obj instanceof ASTNode) {
				ASTNode elm = (ASTNode)obj;
				Declaration owner = elm.owningDeclaration();
				if (owner != null && !owner.containedIn(entity))
					return new ASTNode.Ticket(owner, elm);
			}
			else if (obj instanceof String)
				return ((String)obj).intern();
			return super.replaceObject(obj);
		} catch (Exception e) {
			e.printStackTrace();
			return null;
		}
	}

}
