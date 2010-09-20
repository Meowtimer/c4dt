package net.arctics.clonk.parser.c4script.ast;

import net.arctics.clonk.parser.C4Declaration;

import org.eclipse.jface.text.IRegion;
import org.eclipse.jface.text.Region;

public final class DeclarationRegion {
	private transient C4Declaration declaration;
	private IRegion region;
	private String text;
	public C4Declaration getDeclaration() {
		return declaration;
	}
	public DeclarationRegion(C4Declaration declaration, IRegion region, String text) {
		super();
		this.declaration = declaration;
		this.region = region;
		this.text = text;
	}
	public DeclarationRegion(C4Declaration declaration, IRegion region) {
		this(declaration, region, null);
	}
	public IRegion getRegion() {
		return region;
	}
	public String getText() {
		return text;
	}
	public void setDeclaration(C4Declaration declaration) {
		this.declaration = declaration;
	}
	public DeclarationRegion addOffsetInplace(int offset) {
		region = new Region(region.getOffset()+offset, region.getLength());
		return this;
	}
	@Override
	public String toString() {
		if (declaration != null && region != null)
			return declaration.toString() + "@(" + region.toString() + ")" + (text != null ? "("+text+")" : ""); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$ //$NON-NLS-4$ //$NON-NLS-5$
		else
			return "Empty DeclarationRegion"; //$NON-NLS-1$
	}
}