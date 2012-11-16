package net.arctics.clonk.parser.landscapescript;

import java.util.Collection;
import java.util.LinkedList;
import java.util.List;

import net.arctics.clonk.Core;
import net.arctics.clonk.parser.Declaration;

public class Overlay extends OverlayBase {
	
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
		gradient,
		poly
	}
	
	public String mat;
	public String tex;
	public Algorithm algo;
	public Range x;
	public Range y;
	public Range wdt;
	public Range hgt;
	public Range zoomX, zoomY;
	public Range ox, oy;
	public Range a, b;
	public Range turbulence;
	public Range rotate;
	public Boolean invert;
	public Range seed;
	public Boolean loosebounds;
	public Boolean mask;
	public Boolean grp;
	public Boolean sub;
	public Range lambda;
	
	private Overlay template;
	private Operator operator;
	private boolean isMap;
	
	private static final long serialVersionUID = Core.SERIAL_VERSION_UID;
	
	protected List<OverlayBase> subOverlays = new LinkedList<OverlayBase>();
	
	public boolean isMap() { return isMap; }
	public void isMap(boolean isit) { isMap = isit; }

	@Override
	public Operator operator() {
		return operator;
	}

	public void setOperator(Operator operator) {
		this.operator = operator;
	}

	@Override
	public OverlayBase findDeclaration(String declarationName, Class<? extends Declaration> declarationClass) {
		return findLocalDeclaration(declarationName, declarationClass);
	}
	
	@Override
	public OverlayBase findLocalDeclaration(String declarationName,
			Class<? extends Declaration> declarationClass) {
		if (Overlay.class.isAssignableFrom(declarationClass))
			for (OverlayBase o : subOverlays)
				if (o.name() != null && o.name().equals(declarationName) && declarationClass.isAssignableFrom(o.getClass()))
					return o;
		return null;
	}
	
	@Override
	public OverlayBase findDeclaration(String declarationName) {
		return findDeclaration(declarationName, Overlay.class);
	}
	
	public static Class<? extends OverlayBase> defaultClass(String type) {
		return DEFAULT_CLASS.get(type);
	}
	
	public Overlay templateWithName(String name) {
		for (Overlay level = this; level != null; level = (Overlay) level.parentDeclaration()) {
			OverlayBase o = level.findDeclaration(name, Overlay.class);
			if (o instanceof Overlay)
				return (Overlay) o;
		}
		return null;
	}
	
	@Override
	public OverlayBase template() {
		return template;
	}
	
	public OverlayBase createOverlay(String type, String name) throws InstantiationException, IllegalAccessException, CloneNotSupportedException {
		Class<? extends OverlayBase> cls = defaultClass(type);
		OverlayBase result;
		if (cls != null)
			result = cls.newInstance();
		else {
			Overlay template = templateWithName(type);
			if (template != null) {
				result = (Overlay) template.clone();
				((Overlay)result).template = template;
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
	
	public Overlay createOverlay(Class<? extends Overlay> cls, String name) throws InstantiationException, IllegalAccessException {
		Overlay result = cls.newInstance();
		result.name = name;
		result.setParentDeclaration(this);
		this.subOverlays.add(result);
		return result;
	}
	
	@Override
	public Object[] subDeclarationsForOutline() {
		return this.subOverlays.toArray();
	}
	
	@Override
	public String typeName() {
		OverlayBase t;
		for (t = template; t != null && t.template() != null; t = t.template());
		if (t != null)
			return t.nodeName();
		else
			return super.typeName();
	}
	
	public void clear() {
		beAutonomousClone();
	}
	
	@Override
	protected Object clone() throws CloneNotSupportedException {
		Overlay clone = (Overlay) super.clone();
		clone.beAutonomousClone(); // don't copy nested overlays
		return clone;
	}

	private void beAutonomousClone() {
		this.subOverlays = new LinkedList<OverlayBase>();
		this.body = null;
	}
	
	public OverlayBase overlayAt(int offset) {
		OverlayBase ov;
		Outer: for (ov = this; ov != null && ov.childCollection() != null && ov.childCollection().size() != 0;) {
			for (OverlayBase o : ov.childCollection())
				if (offset >= o.location().start() && offset < (o.body!=null?o.body:o.location()).end()) {
					ov = o;
					continue Outer;
				}
			break;
		}
		return ov;
	}

	@Override
	public Collection<? extends OverlayBase> childCollection() {
		return this.subOverlays;
	}

}
