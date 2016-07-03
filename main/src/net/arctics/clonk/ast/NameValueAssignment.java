package net.arctics.clonk.ast;

import static net.arctics.clonk.util.Utilities.as;

import java.util.Collection;

import org.eclipse.jface.text.IRegion;
import org.eclipse.jface.text.Region;

import net.arctics.clonk.Core;
import net.arctics.clonk.index.IIndexEntity;
import net.arctics.clonk.util.IHasKeyAndValue;
import net.arctics.clonk.util.INode;
import net.arctics.clonk.util.ITreeNode;

/**
 * Declaration of some kind consisting basically of a {@link #name()} being assigned a {@link #stringValue()}.
 * @author madeen
 *
 */
public class NameValueAssignment extends Declaration implements IHasKeyAndValue<String, String>, ITreeNode {
	private static final long serialVersionUID = Core.SERIAL_VERSION_UID;
	protected Object value;
	public NameValueAssignment(final int start, final int end, final String k, final Object v) {
		super(start, end);
		this.name = k;
		value = v;
	}
	public Object value() { return value; }
	public void value(Object val) { value = val; }
	@Override
	public String key() { return name; }
	@Override
	public String stringValue() { return value.toString(); }
	@Override
	public String toString() { return key() + "=" + stringValue(); } //$NON-NLS-1$
	@Override
	public void addChild(final ITreeNode node) {}
	@Override
	public Collection<? extends INode> childCollection() { return null; }
	@Override
	public String nodeName() { return key(); }
	@Override
	public ITreeNode parentNode() { return as(parent, ITreeNode.class); }
	@Override
	public IRegion regionToSelect() { return new Region(start()+getLength()-stringValue().length(), stringValue().length()); }
	@Override
	public String infoText(final IIndexEntity context) { return key() + "=" + stringValue(); } //$NON-NLS-1$
	@Override
	public ASTNode[] subElements() { return new ASTNode[] { ASTNodeWrap.wrap(value) }; }
	@Override
	public void setSubElements(ASTNode[] elms) { value = ASTNodeWrap.unwrap(elms[0]); }
}
