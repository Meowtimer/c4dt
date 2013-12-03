package net.arctics.clonk.ui.search;

import static net.arctics.clonk.util.Utilities.defaulting;

import java.lang.ref.WeakReference;

import net.arctics.clonk.ast.ASTNode;
import net.arctics.clonk.ast.Structure;

import org.eclipse.search.ui.text.Match;

public class SearchMatch extends Match {
	private final String line;
	private final int lineOffset;
	private final WeakReference<ASTNode> node;
	private final boolean potential;
	private final boolean indirect;
	@Override
	public String toString() { return line; }
	public String line() { return line; }
	public int lineOffset() { return lineOffset; }
	public boolean isPotential() { return potential; }
	public boolean isIndirect() { return indirect; }
	public SearchMatch(final String line, final int lineOffset, final Object element, final ASTNode node, final boolean potential, final boolean indirect) {
		super(element, node.sectionOffset()+node.identifierStart(), node.identifierLength());
		this.node = new WeakReference<>(node);
		this.line = defaulting(line, "...");
		this.lineOffset = lineOffset;
		this.potential = potential;
		this.indirect = indirect;
	}
	public Structure structure() {
		final Structure s = (Structure) getElement();
		if (s != null)
			return (Structure) s.latestVersion();
		else
			return null;
	}
	public ASTNode node() { return node.get(); }
}
