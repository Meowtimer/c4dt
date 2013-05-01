package net.arctics.clonk.c4script.ast;

import java.io.Serializable;

import net.arctics.clonk.Core;
import net.arctics.clonk.ast.ASTNode;
import net.arctics.clonk.ast.ASTNodePrinter;
import net.arctics.clonk.ast.EntityRegion;
import net.arctics.clonk.ast.IEntityLocator;
import net.arctics.clonk.ast.NameValueAssignment;
import net.arctics.clonk.c4script.Keywords;
import net.arctics.clonk.c4script.Script;
import net.arctics.clonk.stringtbl.StringTbl;

import org.eclipse.jface.text.Region;

public class FunctionDescription extends Statement implements Serializable {
	private static final long serialVersionUID = Core.SERIAL_VERSION_UID;

	private String contents;
	public FunctionDescription(String contents) {
		super();
		this.contents = contents;
	}
	@Override
	public void doPrint(ASTNodePrinter builder, int depth) {
		builder.append('[');
		builder.append(contents);
		builder.append(']');
	}
	public String contents() {
		return contents;
	}
	public void setContents(String contents) {
		this.contents = contents;
	}
	@Override
	public EntityRegion entityAt(int offset, IEntityLocator locator) {
		if (contents == null)
			return null;
		final String[] parts = contents.split("\\|"); //$NON-NLS-1$
		final Script script = parentOfType(Script.class);
		int off = 1;
		for (final String part : parts) {
			if (offset >= off && offset < off+part.length()) {
				if (part.startsWith("$") && part.endsWith("$")) { //$NON-NLS-1$ //$NON-NLS-2$
					final StringTbl stringTbl = script.localStringTblMatchingLanguagePref();
					if (stringTbl != null) {
						final NameValueAssignment entry = stringTbl.map().get(part.substring(1, part.length()-1));
						if (entry != null)
							return new EntityRegion(entry, new Region(start()+off, part.length()));
					}
				}
				else {
					final String[] nameValue = part.split("="); //$NON-NLS-1$
					if (nameValue.length == 2) {
						final String name = nameValue[0].trim();
						String value = nameValue[1].trim();
						final int sep = value.indexOf(':');
						if (sep != -1)
							value = value.substring(0, sep);
						if (name.equals(Keywords.Condition) || name.equals(Keywords.Image))
							return new EntityRegion(script.findDeclaration(value), new Region(start()+off+nameValue[0].length()+1, value.length()));
					}
				}
				break;
			}
			off += part.length()+1;
		}
		return null;
	}
	@Override
	public boolean equalAttributes(ASTNode other) {
		if (!super.equalAttributes(other))
			return false;
		if (!((FunctionDescription)other).contents.equals(this.contents))
			return false;
		return true;
	}
}