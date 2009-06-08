package net.arctics.clonk.parser.mapcreator;

import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.util.Collection;
import java.util.LinkedList;
import java.util.List;

import org.eclipse.core.runtime.IPath;

import net.arctics.clonk.parser.C4Declaration;
import net.arctics.clonk.parser.C4Structure;
import net.arctics.clonk.parser.SourceLocation;
import net.arctics.clonk.util.ITreeNode;

public class C4MapOverlay extends C4Structure implements Cloneable, ITreeNode {
	
	public enum Algorithm {
		solid,
		random,
		checker,
		bozo,
		sin,
		boxes,
		rndchecker,
		lines,
		border,
		mandel,
		rndall,
		script
	}
	
	public enum Operator {
		Or('|'),
		And('&'),
		XOr('^');
		
		private char c;
		
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
	
	public String mat;
	public String tex;
	public Algorithm algo;
	public int x;
	public int y;
	public int wdt;
	public int hgt;
	public int zoomX, zoomY;
	public int ox, oy;
	public int a, b;
	public int turbulence;
	public int rotate;
	public boolean invert;
	public int seed;
	public boolean loosebounds;
	public boolean mask;
	public boolean grp;
	public boolean sub;
	public int lambda;
	
	private C4MapOverlay template;
	private Operator operator;
	private SourceLocation body;
	
	private static final long serialVersionUID = 1L;
	
	protected List<C4MapOverlay> subOverlays = new LinkedList<C4MapOverlay>();

	public Operator getOperator() {
		return operator;
	}

	public void setOperator(Operator operator) {
		this.operator = operator;
	}

	@Override
	public C4MapOverlay findDeclaration(String declarationName,
			Class<? extends C4Declaration> declarationClass) {
		if (C4MapOverlay.class.isAssignableFrom(declarationClass)) {
			for (C4MapOverlay o : subOverlays)
				if (o.getName() != null && o.getName().equals(declarationName) && declarationClass.isAssignableFrom(o.getClass()))
					return o;
		}
		return null;
	}
	
	@Override
	public C4MapOverlay findDeclaration(String declarationName) {
		return findDeclaration(declarationName, C4MapOverlay.class);
	}
	
	public boolean setAttribute(String attr, String value) throws SecurityException, NoSuchFieldException, NoSuchMethodException, IllegalArgumentException, IllegalAccessException, InvocationTargetException {
		Field f = getClass().getField(attr);
		if (f != null) {
			if (f.getType().getSuperclass() == Enum.class) {
				f.set(this, f.getType().getMethod("valueOf", String.class).invoke(f.getClass(), value));
			}
			else if (f.getType() == Integer.TYPE) {
				f.set(this, Integer.parseInt(value));
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
	
	public void copyFromTemplate(C4MapOverlay template) throws IllegalArgumentException, IllegalAccessException {
		for (Field field : getClass().getFields()) {
			field.set(this, field.get(template));
		}
	}
	
	public static Class<? extends C4MapOverlay> getDefaultClass(String type) {
		if (type.equals("map")) {
			return C4Map.class;
		}
		else if (type.equals("overlay")) {
			return C4MapOverlay.class;
		}
		return null;
	}
	
	public C4MapOverlay getTemplate(String type) {
		for (C4MapOverlay level = this; level != null; level = (C4MapOverlay) level.getParentDeclaration()) {
			C4MapOverlay o = level.findDeclaration(type, C4MapOverlay.class);
			if (o != null)
				return o;
		}
		return null;
	}
	
	public C4MapOverlay getTemplate() {
		return template;
	}
	
	public C4MapOverlay createOverlay(String type, String name) throws InstantiationException, IllegalAccessException, CloneNotSupportedException {
		Class<? extends C4MapOverlay> cls = getDefaultClass(type);
		C4MapOverlay result;
		if (cls != null) {
			result = cls.newInstance();
		}
		else {
			C4MapOverlay template = getTemplate(type);
			if (template != null) {
				result = (C4MapOverlay) template.clone();
				result.template = template;
			}
			else
				result = null;
		}
		if (result != null) {
			result.name = name;
			result.setParentDeclaration(this);
			this.subOverlays.add(result);
		}
		return result;
	}
	
	public C4MapOverlay createOverlay(Class<? extends C4MapOverlay> cls, String name) throws InstantiationException, IllegalAccessException {
		C4MapOverlay result = cls.newInstance();
		result.name = name;
		result.setParentDeclaration(this);
		this.subOverlays.add(result);
		return result;
	}
	
	private boolean hasAnyNamedSubOverlays() {
		for (C4MapOverlay o : this.subOverlays)
			if (o.getName() != null)
				return true;
		return false;
	}
	
	@Override
	public Object[] getSubDeclarationsForOutline() {
		LinkedList<C4MapOverlay> result = new LinkedList<C4MapOverlay>();
		for (C4MapOverlay o : this.subOverlays)
			if (o.getName() != null)
				result.add(o);
		return result.toArray(new C4MapOverlay[result.size()]);
	}
	
	@Override
	public boolean hasSubDeclarationsInOutline() {
		return hasAnyNamedSubOverlays();
	}
	
	@Override
	public String toString() {
		C4MapOverlay t;
		for (t = template; t != null && t.template != null; t = t.template);
		if (t != null)
			return t.name + " " + (name!=null?name:"");
		return (this.getClass()==C4Map.class?"map":"overlay") + " " + (name!=null?name:"");
	}
	
	public void clear() {
		haveOwnSubOverlaysList();
	}
	
	@Override
	protected Object clone() throws CloneNotSupportedException {
		C4MapOverlay clone = (C4MapOverlay) super.clone();
		clone.haveOwnSubOverlaysList(); // don't copy nested overlays
		return clone;
	}

	private void haveOwnSubOverlaysList() {
		this.subOverlays = new LinkedList<C4MapOverlay>();
	}
	
	public C4MapOverlay overlayAt(int offset) {
		C4MapOverlay newov, ov;
		Outer: for (ov = this; ov != null && ov.subOverlays.size() != 0; ov = newov) {
			for (C4MapOverlay o : ov.subOverlays) {
				if (offset >= o.getLocation().getStart() && offset < (o.body!=null?o.body:o.getLocation()).getEnd()) {
					newov = o;
					continue Outer;
				}
			}
			break;
		}
		return ov;
	}

	public void setBody(SourceLocation body) {
		this.body = body;		
	}

	public void addChild(ITreeNode node) {		
	}

	public Collection<? extends ITreeNode> getChildCollection() {
		return this.subOverlays;
	}

	public String getNodeName() {
		return "Landscape.txt"; 
	}

	public ITreeNode getParentNode() {
		if (getParentDeclaration() instanceof ITreeNode)
			return (ITreeNode) getParentDeclaration();
		return null;
	}

	public IPath getPath() {
		return ITreeNode.Default.getPath(this);
	}

	public boolean subNodeOf(ITreeNode node) {
		return ITreeNode.Default.subNodeOf(this, node);
	}

}
