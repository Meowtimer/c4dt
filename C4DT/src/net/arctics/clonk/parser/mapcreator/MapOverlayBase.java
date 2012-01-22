package net.arctics.clonk.parser.mapcreator;

import java.io.Serializable;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Modifier;
import java.util.Collection;
import java.util.Map;

import net.arctics.clonk.ClonkCore;
import net.arctics.clonk.parser.Declaration;
import net.arctics.clonk.parser.SourceLocation;
import net.arctics.clonk.parser.Structure;
import net.arctics.clonk.parser.c4script.ast.Conf;
import net.arctics.clonk.util.ArrayUtil;
import net.arctics.clonk.util.IPrintable;
import net.arctics.clonk.util.ITreeNode;
import net.arctics.clonk.util.StringUtil;

import org.eclipse.core.runtime.IPath;

public class MapOverlayBase extends Structure implements Cloneable, ITreeNode, IPrintable {

	private static final long serialVersionUID = ClonkCore.SERIAL_VERSION_UID;
	
	public static class Keywords {
		public static final String Point = "point"; //$NON-NLS-1$
		public static final String Overlay = "overlay"; //$NON-NLS-1$
		public static final String Map = "map"; //$NON-NLS-1$
	}
	
	public static final Map<String, Class<? extends MapOverlayBase>> DEFAULT_CLASS = ArrayUtil.map(
		false,
		Keywords.Point   , MapPoint.class, 
		Keywords.Overlay , MapOverlay.class, 
		Keywords.Map     , MapCreatorMap.class 
	);

	public enum Operator {
		Or('|'),
		And('&'),
		XOr('^');
		
		private final char c;
		
		Operator(char c) {
			this.c = c;
		}
		
		@Override
		public String toString() {
			return String.valueOf(c);
		}
		
		public static Operator valueOf(char c) {
			for (Operator o : values()) {
				if (o.c == c)
					return o;
			}
			return null;
		}
	}
	
	public enum Unit {
		Percent,
		Pixels;
		
		public static Unit parse(String px) {
			if (px.equals("px")) //$NON-NLS-1$
				return Pixels;
			if (px.equals("%")) //$NON-NLS-1$
				return Percent;
			return Pixels;
		}
		@Override
		public String toString() {
		    switch (this) {
		    case Percent:
		    	return "%"; //$NON-NLS-1$
		    case Pixels:
		    	return "px"; //$NON-NLS-1$
		    default:
		    	return super.toString();
		    }
		}
	}
	
	public static class NumVal implements Serializable {

		private static final long serialVersionUID = ClonkCore.SERIAL_VERSION_UID;
		
		private Unit unit;
		private int value;
		public Unit getUnit() {
        	return unit;
        }
		public void setUnit(Unit unit) {
        	this.unit = unit;
        }
		public int getValue() {
        	return value;
        }
		public void setValue(int value) {
        	this.value = value;
        }
		public NumVal(Unit unit, int value) {
	        super();
	        this.unit = unit;
	        this.value = value;
        }
		public static NumVal parse(String value) {
			if (value == null)
				return null;
			int i;
			for (i = value.length()-1; i >= 0 && !Character.isDigit(value.charAt(i)); i--);
			String unit = value.substring(i+1);
			String number = value.substring(0, i+1);
			if (number.length() > 0 && number.charAt(0) == '+')
				number = number.substring(1); // Integer.parseInt coughs on '+': a lesson in ridiculousness
			return new NumVal(Unit.parse(unit), Integer.parseInt(number));
        }
		@Override
		public String toString() {
		    return value+unit.toString();
		}
	}
	
	public static class Range implements Serializable {

		private static final long serialVersionUID = ClonkCore.SERIAL_VERSION_UID;
		
		private final NumVal lo, hi;

		public Range(NumVal lo, NumVal hi) {
			super();
			this.lo = lo;
			this.hi = hi;
		}

		public NumVal getLo() {
			return lo;
		}

		public NumVal getHi() {
			return hi;
		}
		
		@Override
		public String toString() {
			if (lo != null && hi != null) {
				return lo.toString() + " - " + hi.toString(); //$NON-NLS-1$
			}
			else if (lo != null) {
				return lo.toString();
			}
			else
				return "<Empty Range>"; //$NON-NLS-1$
		}
	}
	
	protected SourceLocation body;

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
		return ITreeNode.Default.getPath(this);
	}

	@Override
	public Collection<? extends MapOverlayBase> childCollection() {
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
			if (f.getType().getSuperclass() == Enum.class) {
				f.set(this, f.getType().getMethod("valueOf", String.class).invoke(f.getClass(), valueLo)); //$NON-NLS-1$
			}
			else if (f.getType() == NumVal.class) {
				f.set(this, NumVal.parse(valueLo));
			}
			else if (f.getType() == Range.class) {
				f.set(this, new Range(NumVal.parse(valueLo), NumVal.parse(valueHi)));
			}
			else if (f.getType() == String.class) {
				f.set(this, valueLo);
			}
			else if (f.getType() == Boolean.TYPE) {
				f.set(this, Integer.parseInt(valueLo) == 1);
			}
			else
				return false;
			return true;
		}
		return false;
	}
	
	public void copyFromTemplate(MapOverlayBase template) throws IllegalArgumentException, IllegalAccessException {
		for (Field field : getClass().getFields()) {
			field.set(this, field.get(template));
		}
	}
	
	public void setBody(SourceLocation body) {
		this.body = body;		
	}

	public MapOverlayBase template() {
		return null;
	}
	
	public Operator operator() {
		return null;
	}
	
	public String typeName() {
		for (String key : DEFAULT_CLASS.keySet()) {
			if (DEFAULT_CLASS.get(key).equals(this.getClass())) {
				return key;
			}
		}
		return null;
	}
	
	@Override
	public void print(StringBuilder builder, int depth) {
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
			for (Field f : this.getClass().getFields()) {
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
			}
			Collection<? extends MapOverlayBase> children = this.childCollection();
			if (children != null) {
				Operator lastOp = null;
				for (MapOverlayBase child : children) {
					if (lastOp == null) {
						builder.append(StringUtil.multiply(Conf.indentString, depth));
					}
					child.print(builder, depth+1);
					Operator op = child.operator();
					if (op != null) {
						builder.append(" "); //$NON-NLS-1$
						builder.append(op.toString());
						builder.append(" "); //$NON-NLS-1$
					}
					else {
						builder.append(";\n"); //$NON-NLS-1$
					}
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
