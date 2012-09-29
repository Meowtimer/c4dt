package net.arctics.clonk.parser.c4script.statictyping;

import net.arctics.clonk.parser.SourceLocation;
import net.arctics.clonk.parser.c4script.IType;
import net.arctics.clonk.parser.c4script.ITypeable;

public final class TypeAnnotation extends SourceLocation {
	private static final long serialVersionUID = 1L;
	private ITypeable typeable;
	private IType type;
	public ITypeable typeable() {return typeable;}
	public void setTypeable(ITypeable typeable) {this.typeable = typeable;}
	public IType type() {return type;}
	public void setType(IType type) {this.type = type;}
	public TypeAnnotation(int start, int end) {super(start, end);}
}