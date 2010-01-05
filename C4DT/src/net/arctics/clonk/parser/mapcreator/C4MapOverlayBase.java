package net.arctics.clonk.parser.mapcreator;

import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.util.Collection;

import org.eclipse.core.runtime.IPath;

import net.arctics.clonk.parser.C4Declaration;
import net.arctics.clonk.parser.C4Structure;
import net.arctics.clonk.parser.SourceLocation;
import net.arctics.clonk.util.ITreeNode;

public class C4MapOverlayBase extends C4Structure implements Cloneable, ITreeNode {

	private static final long serialVersionUID = 1L;

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
	
	public static class UnitInteger {
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
		public UnitInteger(Unit unit, int value) {
	        super();
	        this.unit = unit;
	        this.value = value;
        }
		public static UnitInteger parse(String value) {
			int i;
			for (i = value.length()-1; i >= 0 && !Character.isDigit(value.charAt(i)); i--);
			String unit = value.substring(i+1);
			String number = value.substring(0, i+1);
			if (number.length() > 0 && number.charAt(0) == '+')
				number = number.substring(1); // Integer.parseInt coughs on '+': a lesson in ridiculousness
			return new UnitInteger(Unit.parse(unit), Integer.parseInt(number));
        }
		@Override
		public String toString() {
		    return value+unit.toString();
		}
	}
	
	protected SourceLocation body;

	@Override
	public C4Declaration findLocalDeclaration(String declarationName, Class<? extends C4Declaration> declarationClass) {
		return null;
	}

	@Override
	public ITreeNode getParentNode() {
		if (getParentDeclaration() instanceof ITreeNode)
			return (ITreeNode) getParentDeclaration();
		else
			return null;
	}

	@Override
	public IPath getPath() {
		return ITreeNode.Default.getPath(this);
	}

	@Override
	public Collection<? extends C4MapOverlayBase> getChildCollection() {
		return null;
	}

	@Override
	public boolean subNodeOf(ITreeNode node) {
		return ITreeNode.Default.subNodeOf(this, node);
	}

	@Override
	public void addChild(ITreeNode node) {
	}
	
	public boolean setAttribute(String attr, String value) throws SecurityException, NoSuchFieldException, NoSuchMethodException, IllegalArgumentException, IllegalAccessException, InvocationTargetException {
		Field f = getClass().getField(attr);
		if (f != null) {
			if (f.getType().getSuperclass() == Enum.class) {
				f.set(this, f.getType().getMethod("valueOf", String.class).invoke(f.getClass(), value)); //$NON-NLS-1$
			}
			else if (f.getType() == UnitInteger.class) {
				f.set(this, UnitInteger.parse(value));
			}
			else if (f.getType() == String.class) {
				f.set(this, value);
			}
			else if (f.getType() == Boolean.TYPE) {
				f.set(this, Integer.parseInt(value) == 1);
			}
			else
				return false;
			return true;
		}
		return false;
	}
	
	public void copyFromTemplate(C4MapOverlayBase template) throws IllegalArgumentException, IllegalAccessException {
		for (Field field : getClass().getFields()) {
			field.set(this, field.get(template));
		}
	}
	
	public void setBody(SourceLocation body) {
		this.body = body;		
	}

}
