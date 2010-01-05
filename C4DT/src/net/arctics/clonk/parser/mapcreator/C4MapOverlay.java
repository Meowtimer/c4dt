package net.arctics.clonk.parser.mapcreator;

import java.util.Collection;
import java.util.LinkedList;
import java.util.List;

import net.arctics.clonk.parser.C4Declaration;

public class C4MapOverlay extends C4MapOverlayBase {
	
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
		script,
		poly
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
	public UnitInteger x;
	public UnitInteger y;
	public UnitInteger wdt;
	public UnitInteger hgt;
	public UnitInteger zoomX, zoomY;
	public UnitInteger ox, oy;
	public UnitInteger a, b;
	public UnitInteger turbulence;
	public UnitInteger rotate;
	public boolean invert;
	public UnitInteger seed;
	public boolean loosebounds;
	public boolean mask;
	public boolean grp;
	public boolean sub;
	public UnitInteger lambda;
	
	private C4MapOverlay template;
	private Operator operator;
	
	private static final long serialVersionUID = 1L;
	
	protected List<C4MapOverlayBase> subOverlays = new LinkedList<C4MapOverlayBase>();

	public Operator getOperator() {
		return operator;
	}

	public void setOperator(Operator operator) {
		this.operator = operator;
	}

	@Override
	public C4MapOverlayBase findDeclaration(String declarationName, Class<? extends C4Declaration> declarationClass) {
		return findLocalDeclaration(declarationName, declarationClass);
	}
	
	@Override
	public C4MapOverlayBase findLocalDeclaration(String declarationName,
			Class<? extends C4Declaration> declarationClass) {
		if (C4MapOverlay.class.isAssignableFrom(declarationClass)) {
			for (C4MapOverlayBase o : subOverlays)
				if (o.getName() != null && o.getName().equals(declarationName) && declarationClass.isAssignableFrom(o.getClass()))
					return o;
		}
		return null;
	}
	
	@Override
	public C4MapOverlayBase findDeclaration(String declarationName) {
		return findDeclaration(declarationName, C4MapOverlay.class);
	}
	
	public static Class<? extends C4MapOverlayBase> getDefaultClass(String type) {
		if (type.equals("map")) { //$NON-NLS-1$
			return C4Map.class;
		}
		else if (type.equals("overlay")) { //$NON-NLS-1$
			return C4MapOverlay.class;
		}
		else if (type.equals("point")) {
			return C4MapPoint.class;
		}
		return null;
	}
	
	public C4MapOverlay getTemplate(String type) {
		for (C4MapOverlay level = this; level != null; level = (C4MapOverlay) level.getParentDeclaration()) {
			C4MapOverlayBase o = level.findDeclaration(type, C4MapOverlay.class);
			if (o instanceof C4MapOverlay)
				return (C4MapOverlay) o;
		}
		return null;
	}
	
	public C4MapOverlay getTemplate() {
		return template;
	}
	
	public C4MapOverlayBase createOverlay(String type, String name) throws InstantiationException, IllegalAccessException, CloneNotSupportedException {
		Class<? extends C4MapOverlayBase> cls = getDefaultClass(type);
		C4MapOverlayBase result;
		if (cls != null) {
			result = cls.newInstance();
		}
		else {
			C4MapOverlay template = getTemplate(type);
			if (template != null) {
				result = (C4MapOverlay) template.clone();
				((C4MapOverlay)result).template = template;
			}
			else
				result = null;
		}
		if (result != null) {
			result.setName(name);
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
		for (C4MapOverlayBase o : this.subOverlays)
			if (o.getName() != null)
				return true;
		return false;
	}
	
	@Override
	public Object[] getSubDeclarationsForOutline() {
		LinkedList<C4MapOverlayBase> result = new LinkedList<C4MapOverlayBase>();
		for (C4MapOverlayBase o : this.subOverlays)
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
			return t.name + " " + (name!=null?name:""); //$NON-NLS-1$ //$NON-NLS-2$
		return (this.getClass()==C4Map.class?"map":"overlay") + " " + (name!=null?name:""); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$ //$NON-NLS-4$
	}
	
	public void clear() {
		beAutonomousClone();
	}
	
	@Override
	protected Object clone() throws CloneNotSupportedException {
		C4MapOverlay clone = (C4MapOverlay) super.clone();
		clone.beAutonomousClone(); // don't copy nested overlays
		return clone;
	}

	private void beAutonomousClone() {
		this.subOverlays = new LinkedList<C4MapOverlayBase>();
		this.body = null;
	}
	
	public C4MapOverlayBase overlayAt(int offset) {
		C4MapOverlayBase ov;
		Outer: for (ov = this; ov != null && ov.getChildCollection() != null && ov.getChildCollection().size() != 0;) {
			for (C4MapOverlayBase o : ov.getChildCollection()) {
				if (offset >= o.getLocation().getStart() && offset < (o.body!=null?o.body:o.getLocation()).getEnd()) {
					ov = o;
					continue Outer;
				}
			}
			break;
		}
		return ov;
	}

	public Collection<? extends C4MapOverlayBase> getChildCollection() {
		return this.subOverlays;
	}

	public String getNodeName() {
		return "Landscape.txt";  //$NON-NLS-1$
	}

}
