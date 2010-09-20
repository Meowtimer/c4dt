package net.arctics.clonk.parser.c4script.ast;

import net.arctics.clonk.ClonkCore;
import net.arctics.clonk.parser.C4Declaration;
import net.arctics.clonk.parser.ParsingException;
import net.arctics.clonk.parser.c4script.C4ScriptParser;

import org.eclipse.jface.text.IRegion;
import org.eclipse.jface.text.Region;

public abstract class AccessDeclaration extends Value {
	/**
	 * 
	 */
	private static final long serialVersionUID = ClonkCore.SERIAL_VERSION_UID;
	protected transient C4Declaration declaration;
	protected final String declarationName;

	public C4Declaration getDeclaration(C4ScriptParser parser) {
		if (declaration == null) {
			declaration = obtainDeclaration(parser);
		}
		return declaration;
	}

	public C4Declaration getDeclaration() {
		return declaration; // return without trying to obtain it (no parser context)
	}

	public abstract C4Declaration obtainDeclaration(C4ScriptParser parser);

	@Override
	public void reportErrors(C4ScriptParser parser) throws ParsingException {
		super.reportErrors(parser);
		getDeclaration(parser); // find the field so subclasses can complain about missing variables/functions
	}

	public AccessDeclaration(String fieldName) {
		this.declarationName = fieldName;
	}
	@Override
	public void doPrint(ExprWriter output, int depth) {
		output.append(declarationName);
	}

	public IRegion declarationRegion(int offset) {
		return new Region(offset+getExprStart(), declarationName.length());
	}

	@Override
	public DeclarationRegion declarationAt(int offset, C4ScriptParser parser) {
		return new DeclarationRegion(getDeclaration(parser), region(0));
	}

	public String getDeclarationName() {
		return declarationName;
	}

	@Override
	public int getIdentifierLength() {
		return declarationName.length();
	}

	public boolean indirectAccess() {
		return declaration == null || !declaration.getName().equals(declarationName);
	}
	
	@Override
	public boolean compare(ExprElm other, IDifferenceListener listener) {
		if (!super.compare(other, listener))
			return false;
		if (!declarationName.equals(((AccessDeclaration)other).declarationName)) {
			listener.differs(this, other, field("declarationName"));
			return false;
		} else {
			return true;
		}
	}
	
}