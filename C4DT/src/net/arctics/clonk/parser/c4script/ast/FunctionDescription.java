package net.arctics.clonk.parser.c4script.ast;

import java.io.Serializable;

import net.arctics.clonk.Core;
import net.arctics.clonk.parser.EntityRegion;
import net.arctics.clonk.parser.NameValueAssignment;
import net.arctics.clonk.parser.ParserErrorCode;
import net.arctics.clonk.parser.ParsingException;
import net.arctics.clonk.parser.c4script.C4ScriptParser;
import net.arctics.clonk.parser.c4script.Keywords;
import net.arctics.clonk.parser.c4script.ast.IASTComparisonDelegate.DifferenceHandling;
import net.arctics.clonk.parser.stringtbl.StringTbl;

import org.eclipse.jface.text.Region;

public class FunctionDescription extends Statement implements Serializable {
	private static final long serialVersionUID = Core.SERIAL_VERSION_UID;

	private String contents;
	public FunctionDescription(String contents) {
		super();
		this.contents = contents;
	}
	@Override
	public void doPrint(ExprWriter builder, int depth) {
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
	public EntityRegion declarationAt(int offset, C4ScriptParser parser) {
		if (contents == null)
			return null;
		String[] parts = contents.split("\\|"); //$NON-NLS-1$
		int off = 1;
		for (String part : parts) {
			if (offset >= off && offset < off+part.length()) {
				if (part.startsWith("$") && part.endsWith("$")) { //$NON-NLS-1$ //$NON-NLS-2$
					StringTbl stringTbl = parser.script().localStringTblMatchingLanguagePref();
					if (stringTbl != null) {
						NameValueAssignment entry = stringTbl.map().get(part.substring(1, part.length()-1));
						if (entry != null)
							return new EntityRegion(entry, new Region(start()+off, part.length()));
					}
				}
				else {
					String[] nameValue = part.split("="); //$NON-NLS-1$
					if (nameValue.length == 2) {
						String name = nameValue[0].trim();
						String value = nameValue[1].trim();
						int sep = value.indexOf(':');
						if (sep != -1)
							value = value.substring(0, sep);
						if (name.equals(Keywords.Condition) || name.equals(Keywords.Image))
							return new EntityRegion(parser.script().findDeclaration(value), new Region(start()+off+nameValue[0].length()+1, value.length()));
					}
				}
				break;
			}
			off += part.length()+1;
		}
		return null;
	}
	@Override
	public void reportProblems(C4ScriptParser parser) throws ParsingException {
		// see StringLiteral.reportErrors
		if (parser.hasAppendTo())
			return;
		int off = 1;
		for (String part : contents.split("\\|")) { //$NON-NLS-1$
			if (part.startsWith("$") && part.endsWith("$")) { //$NON-NLS-1$ //$NON-NLS-2$
				StringTbl stringTbl = parser.script().localStringTblMatchingLanguagePref();
				String entryName = part.substring(1, part.length()-1);
				if (stringTbl == null || stringTbl.map().get(entryName) == null)
					parser.warning(ParserErrorCode.UndeclaredIdentifier, new Region(start()+off, part.length()), 0, entryName);
			}
			off += part.length()+1;
		}
	}
	@Override
	public DifferenceHandling compare(ExprElm other, IASTComparisonDelegate listener) {
		DifferenceHandling sup = super.compare(other, listener);
		if (sup != DifferenceHandling.Equal)
			return sup;
		if (!((FunctionDescription)other).contents.equals(this.contents))
			return listener.differs(this, other, "contents");
		else
			return DifferenceHandling.Equal;
	}
}