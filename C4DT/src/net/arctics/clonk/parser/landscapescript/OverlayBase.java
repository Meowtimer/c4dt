package net.arctics.clonk.parser.landscapescript;

import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Modifier;
import java.util.Collection;
import java.util.Map;

import net.arctics.clonk.Core;
import net.arctics.clonk.parser.Declaration;
import net.arctics.clonk.parser.SourceLocation;
import net.arctics.clonk.parser.Structure;
import net.arctics.clonk.parser.c4script.Conf;
import net.arctics.clonk.parser.c4script.ast.ASTNodePrinter;
import net.arctics.clonk.util.ArrayUtil;
import net.arctics.clonk.util.IPrintable;
import net.arctics.clonk.util.ITreeNode;
import net.arctics.clonk.util.StringUtil;

import org.eclipse.core.runtime.IPath;

public class OverlayBase extends Structure implements Cloneable, ITreeNode, IPrintable {

	private static final long serialVersionUID = Core.SERIAL_VERSION_UID;
	
	public static final Map<String, Class<? extends OverlayBase>> DEFAULT_CLASS = ArrayUtil.map(
		false,
		Keywords.Point   , Point.class, 
		Keywords.Overlay , Overlay.class, 
		Keywords.Map     , Overlay.class 
	);

	protected SourceLocation body;
	protected OverlayBase prev;

	@Override
	public Declaration findLocalDeclaration(String declarationName, Class<? extends Declaration> declarationClass) {
		return null;
	}

	@Override
	public ITreeNode parentNode() {
		if (parentDeclaration() instanceof ITreeNode)
			return (ITreeNode) parentDeclaration();
		else
			return null;
	}

	@Override
	public IPath path() {
		return ITreeNode.Default.path(this);
	}

	@Override
	public Collection<? extends OverlayBase> childCollection() {
		return null;
	}

	@Override
	public boolean subNodeOf(ITreeNode node) {
		return ITreeNode.Default.subNodeOf(this, node);
	}

	@Override
	public void addChild(ITreeNode node) {
	}
	
	public boolean setAttribute(String attr, String valueLo, String valueHi) throws SecurityException, NoSuchFieldException, NoSuchMethodException, IllegalArgumentException, IllegalAccessException, InvocationTargetException {
		Field f = getClass().getField(attr);
		if (f != null) {
			if (f.getType().getSuperclass() == Enum.class)
				f.set(this, f.getType().getMethod("valueOf", String.class).invoke(f.getClass(), valueLo)); //$NON-NLS-1$
			else if (f.getType() == NumVal.class)
				f.set(this, NumVal.parse(valueLo));
			else if (f.getType() == Range.class)
				f.set(this, new Range(NumVal.parse(valueLo), NumVal.parse(valueHi)));
			else if (f.getType() == String.class)
				f.set(this, valueLo);
			else if (f.getType() == Boolean.TYPE)
				f.set(this, Integer.parseInt(valueLo) == 1);
			else
				return false;
			return true;
		}
		return false;
	}
	
	public void copyFromTemplate(OverlayBase template) throws IllegalArgumentException, IllegalAccessException {
		for (Field field : getClass().getFields())
			field.set(this, field.get(template));
	}
	
	public void setBody(SourceLocation body) {
		this.body = body;		
	}

	public OverlayBase template() {
		return null;
	}
	
	public Operator operator() {
		return null;
	}
	
	public String typeName() {
		for (String key : DEFAULT_CLASS.keySet())
			if (DEFAULT_CLASS.get(key).equals(this.getClass()))
				return key;
		return null;
	}
	
	@Override
	public void doPrint(ASTNodePrinter builder, int depth) {
		try {
			String type = typeName();
			if (type != null) {
				builder.append(type);
				if (nodeName() != null) {
					builder.append(" "); //$NON-NLS-1$
					builder.append(nodeName());
				}
				builder.append(" {\n"); //$NON-NLS-1$
			}
			for (Field f : this.getClass().getFields())
				if (Modifier.isPublic(f.getModifiers()) && !Modifier.isStatic(f.getModifiers())) {
					Object val = f.get(this);
					// flatly cloned attributes of template -> don't print
					// FIXME: doesn't work for enums of course -.-
					if (val != null && (template() == null || (val != f.get(template())))) {
						builder.append(StringUtil.multiply(Conf.indentString, depth));
						builder.append(f.getName());
						builder.append(" = "); //$NON-NLS-1$
						builder.append(val.toString());
						builder.append(";"); //$NON-NLS-1$
						builder.append("\n"); //$NON-NLS-1$
					}
				}
			Collection<? extends OverlayBase> children = this.childCollection();
			if (children != null) {
				Operator lastOp = null;
				for (OverlayBase child : children) {
					if (lastOp == null)
						builder.append(StringUtil.multiply(Conf.indentString, depth));
					child.print(builder, depth+1);
					Operator op = child.operator();
					if (op != null) {
						builder.append(" "); //$NON-NLS-1$
						builder.append(op.toString());
						builder.append(" "); //$NON-NLS-1$
					} else
						builder.append(";\n"); //$NON-NLS-1$
					lastOp = op;
				}
			}
			if (type != null) {
				builder.append(StringUtil.multiply(Conf.indentString, depth));
				builder.append("}"); //$NON-NLS-1$
			}
		} catch (Exception e) {
			e.printStackTrace();
		}
	}
	
	@Override
	public String toString(int depth) {
		StringBuilder builder = new StringBuilder();
		this.print(builder, depth);
		return builder.toString();
	}
	
	@Override
	public String toString() {
		return typeName() + (name!=null?(" "+name):""); //$NON-NLS-1$ //$NON-NLS-2$
	}

}
