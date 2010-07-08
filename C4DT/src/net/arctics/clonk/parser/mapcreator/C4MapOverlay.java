package net.arctics.clonk.parser.mapcreator;

import java.util.Collection;
import java.util.LinkedList;
import java.util.List;

import net.arctics.clonk.ClonkCore;
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
	
	private C4MapOverlay template;
	private Operator operator;
	
	private static final long serialVersionUID = ClonkCore.SERIAL_VERSION_UID;
	
	protected List<C4MapOverlayBase> subOverlays = new LinkedList<C4MapOverlayBase>();

	@Override
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
		return DEFAULT_CLASS.get(type);
	}
	
	public C4MapOverlay getTemplate(String type) {
		for (C4MapOverlay level = this; level != null; level = (C4MapOverlay) level.getParentDeclaration()) {
			C4MapOverlayBase o = level.findDeclaration(type, C4MapOverlay.class);
			if (o instanceof C4MapOverlay)
				return (C4MapOverlay) o;
		}
		return null;
	}
	
	@Override
	public C4MapOverlayBase getTemplate() {
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
		return result.toArray(new C4MapOverlayBase[result.size()]);
	}
	
	@Override
	public boolean hasSubDeclarationsInOutline() {
		return hasAnyNamedSubOverlays();
	}
	
	@Override
	public String getTypeName() {
		C4MapOverlayBase t;
		for (t = template; t != null && t.getTemplate() != null; t = t.getTemplate());
		if (t != null)
			return t.getNodeName();
		else
			return super.getTypeName();
	}
	
	@Override
	public String toString() {
		return getTypeName() + (name!=null?(" "+name):""); //$NON-NLS-1$ //$NON-NLS-2$
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

}
