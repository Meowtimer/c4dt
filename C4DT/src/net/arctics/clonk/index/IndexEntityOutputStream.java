package net.arctics.clonk.index;

import java.io.IOException;
import java.io.ObjectOutputStream;
import java.io.OutputStream;

import net.arctics.clonk.parser.Declaration;
import net.arctics.clonk.parser.c4script.SpecialScriptRules.SpecialRule;

public class IndexEntityOutputStream extends ObjectOutputStream {
	private final Index index;

	public IndexEntityOutputStream(Index index, OutputStream output) throws IOException {
		super(output);
		this.index = index;
		enableReplaceObject(true);
	}

	@Override
	protected Object replaceObject(Object obj) throws IOException {
		try {
			if (obj instanceof IndexEntity)
				return index.saveReplacementForEntity((IndexEntity)obj);
			else if (obj instanceof Declaration && !(obj instanceof Index))
				return index.getSaveReplacementForEntityDeclaration((Declaration)obj);
			else if (obj instanceof SpecialRule)
				return new SpecialRule.Ticket((SpecialRule)obj);
			else if (obj instanceof String)
				return ((String)obj).intern();
			else
				return super.replaceObject(obj);
		} catch (Exception e) {
			e.printStackTrace();
			return null;
		}
	}

}
