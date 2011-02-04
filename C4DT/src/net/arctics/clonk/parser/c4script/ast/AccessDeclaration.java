package net.arctics.clonk.parser.c4script.ast;

import net.arctics.clonk.ClonkCore;
import net.arctics.clonk.parser.Declaration;
import net.arctics.clonk.parser.DeclarationRegion;
import net.arctics.clonk.parser.ParsingException;
import net.arctics.clonk.parser.c4script.C4ScriptParser;
import net.arctics.clonk.parser.c4script.DeclarationObtainmentContext;
import net.arctics.clonk.parser.c4script.ITypedDeclaration;
import net.arctics.clonk.parser.c4script.ast.IDifferenceListener.Option;

import org.eclipse.jface.text.IRegion;
import org.eclipse.jface.text.Region;

public abstract class AccessDeclaration extends Value {

	private static final long serialVersionUID = ClonkCore.SERIAL_VERSION_UID;
	protected transient Declaration declaration;
	protected String declarationName;
	
	public Declaration getDeclaration(DeclarationObtainmentContext context) {
		if (declaration == null) {
			declaration = obtainDeclaration(context);
		}
		return declaration;
	}

	public Declaration getDeclaration() {
		return declaration; // return without trying to obtain it (no parser context)
	}

	public abstract Declaration obtainDeclaration(DeclarationObtainmentContext context);

	@Override
	public void reportErrors(C4ScriptParser parser) throws ParsingException {
		super.reportErrors(parser);
		getDeclaration(parser); // find the declaration so subclasses can complain about missing variables/functions
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
	
	public void setDeclarationName(String declarationName) {
		this.declarationName = declarationName;
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
		AccessDeclaration otherDec = (AccessDeclaration) other;
		if (listener.optionEnabled(Option.CheckForIdentity)) {
			if (!declarationName.equals(otherDec.declarationName)) {
				listener.differs(this, other, "declarationName");
				return false;
			}
		} else {
			if (declaration != otherDec.declaration) {
				listener.differs(this, other, "declaration");
				return false;
			}
		}
		return true;
	}

	public Class<? extends Declaration> declarationClass() {
		return Declaration.class;
	}
	
	@Override
	public IStoredTypeInformation createStoredTypeInformation(C4ScriptParser parser) {
		if (declaration instanceof ITypedDeclaration && ((ITypedDeclaration)declaration).typeIsInvariant()) {
			return null;
		} else {
			return super.createStoredTypeInformation(parser);
		}
	}
	
	@Override
	public void postSerialize(ExprElm parent, DeclarationObtainmentContext root) {
		super.postSerialize(parent, root);
		getDeclaration(root);
	}
	
}