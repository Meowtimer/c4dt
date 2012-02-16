package net.arctics.clonk.parser.mapcreator;

import java.util.Collection;
import java.util.LinkedList;
import java.util.List;

import net.arctics.clonk.Core;
import net.arctics.clonk.parser.Declaration;

public class MapOverlay extends MapOverlayBase {
	
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
	
	private MapOverlay template;
	private Operator operator;
	
	private static final long serialVersionUID = Core.SERIAL_VERSION_UID;
	
	protected List<MapOverlayBase> subOverlays = new LinkedList<MapOverlayBase>();

	@Override
	public Operator operator() {
		return operator;
	}

	public void setOperator(Operator operator) {
		this.operator = operator;
	}

	@Override
	public MapOverlayBase findDeclaration(String declarationName, Class<? extends Declaration> declarationClass) {
		return findLocalDeclaration(declarationName, declarationClass);
	}
	
	@Override
	public MapOverlayBase findLocalDeclaration(String declarationName,
			Class<? extends Declaration> declarationClass) {
		if (MapOverlay.class.isAssignableFrom(declarationClass)) {
			for (MapOverlayBase o : subOverlays)
				if (o.name() != null && o.name().equals(declarationName) && declarationClass.isAssignableFrom(o.getClass()))
					return o;
		}
		return null;
	}
	
	@Override
	public MapOverlayBase findDeclaration(String declarationName) {
		return findDeclaration(declarationName, MapOverlay.class);
	}
	
	public static Class<? extends MapOverlayBase> defaultClass(String type) {
		return DEFAULT_CLASS.get(type);
	}
	
	public MapOverlay templateWithName(String name) {
		for (MapOverlay level = this; level != null; level = (MapOverlay) level.parentDeclaration()) {
			MapOverlayBase o = level.findDeclaration(name, MapOverlay.class);
			if (o instanceof MapOverlay)
				return (MapOverlay) o;
		}
		return null;
	}
	
	@Override
	public MapOverlayBase template() {
		return template;
	}
	
	public MapOverlayBase createOverlay(String type, String name) throws InstantiationException, IllegalAccessException, CloneNotSupportedException {
		Class<? extends MapOverlayBase> cls = defaultClass(type);
		MapOverlayBase result;
		if (cls != null) {
			result = cls.newInstance();
		}
		else {
			MapOverlay template = templateWithName(type);
			if (template != null) {
				result = (MapOverlay) template.clone();
				((MapOverlay)result).template = template;
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
	
	public MapOverlay createOverlay(Class<? extends MapOverlay> cls, String name) throws InstantiationException, IllegalAccessException {
		MapOverlay result = cls.newInstance();
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
	public boolean hasSubDeclarationsInOutline() {
		return this.subOverlays.size() > 0;
	}
	
	@Override
	public String typeName() {
		MapOverlayBase t;
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
		MapOverlay clone = (MapOverlay) super.clone();
		clone.beAutonomousClone(); // don't copy nested overlays
		return clone;
	}

	private void beAutonomousClone() {
		this.subOverlays = new LinkedList<MapOverlayBase>();
		this.body = null;
	}
	
	public MapOverlayBase overlayAt(int offset) {
		MapOverlayBase ov;
		Outer: for (ov = this; ov != null && ov.childCollection() != null && ov.childCollection().size() != 0;) {
			for (MapOverlayBase o : ov.childCollection()) {
				if (offset >= o.location().start() && offset < (o.body!=null?o.body:o.location()).end()) {
					ov = o;
					continue Outer;
				}
			}
			break;
		}
		return ov;
	}

	@Override
	public Collection<? extends MapOverlayBase> childCollection() {
		return this.subOverlays;
	}

}
