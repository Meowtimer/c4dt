package net.arctics.clonk.landscapescript;

import static net.arctics.clonk.mapcreator.GlobalFunctions.Inside;
import static net.arctics.clonk.mapcreator.GlobalFunctions.Random;

import java.util.Collection;
import java.util.LinkedList;
import java.util.List;

import net.arctics.clonk.Core;
import net.arctics.clonk.ast.Declaration;
import net.arctics.clonk.ast.IEvaluationContext;
import net.arctics.clonk.c4script.ast.ControlFlowException;
import net.arctics.clonk.mapcreator.MapCreator;

public class Overlay extends OverlayBase {
	
	private static final long serialVersionUID = Core.SERIAL_VERSION_UID;
	
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
	public boolean invert;
	public Range seed;
	public boolean loosebounds;
	public boolean  mask;
	public boolean grp;
	public boolean sub;
	public Range lambda;
	
	private Overlay template;
	private Operator operator;
	private boolean isMap;
	
	public int Material;
	public int MatClr;
	public int X, Y, Wdt, Hgt, OffX, OffY;
	public int Seed, FixedSeed;
	
	protected List<OverlayBase> subOverlays = new LinkedList<OverlayBase>();
	
	public MapCreator mapCreator() { return null; }
	
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
		if (cls != null) {
			result = cls.newInstance();
			if (result instanceof Overlay)
				((Overlay)result).isMap(type.equals(Keywords.Map));
		}
		else {
			Overlay template = templateWithName(type);
			if (template != null) {
				result = template.clone();
				((Overlay)result).template = template;
			}
			else
				result = null;
		}
		if (result != null) {
			result.setName(name);
			result.setParent(this);
			result.prev = this.subOverlays.size() > 0 ? this.subOverlays.get(this.subOverlays.size()-1) : null;
			this.subOverlays.add(result);
		}
		return result;
	}
	
	public Overlay createOverlay(Class<? extends Overlay> cls, String name) throws InstantiationException, IllegalAccessException {
		Overlay result = cls.newInstance();
		result.name = name;
		result.setParent(this);
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
	public Overlay clone() {
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
				if (offset >= o.start() && offset < (o.body!=null?o.body:o).end()) {
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
	
	public Overlay OwnerOverlay() {
		return parentOfType(Overlay.class);
	}
	
	public void defaults() {
		// default algo
		algo = Algorithm.solid;
		// no mat (sky) default
		Material = 0;
		mat = "Sky";
		tex = null;
		// but if mat is set, assume it sub
		sub = true;
		// full size
		OffX=OffY=X=Y=0;
		ox = new Range(Unit.Percent, 0);
		oy = new Range(Unit.Percent, 0);
		x  = new Range(Unit.Percent, 0);
		y  = new Range(Unit.Percent, 0);
		Wdt=Hgt=100;
		wdt = new Range(Unit.Percent, 100);
		hgt = new Range(Unit.Percent, 100);
		// def zoom
		zoomX=zoomY=new Range(Unit.Percent, 100);
		// def values
		a = new Range(Unit.Pixels, 0);
		b = new Range(Unit.Pixels, 0);
		turbulence = new Range(Unit.Pixels, 0);
		rotate = new Range(Unit.Pixels, 0);
		invert = loosebounds = grp = mask = false;
		FixedSeed=0;
	}
	
	@Override
	public Object evaluate(IEvaluationContext context) throws ControlFlowException
	{
		// inherited
		super.evaluate(context);
		// get mat color
		if (Inside(Material,0,mapCreator().MatMap.size()-1))
		{
			MatClr=mapCreator().TexMap.GetIndexMatTex(mapCreator().MatMap.get(Material).name(), tex);
			if (sub) MatClr+=128;
		}
		else
			MatClr=0;
		// calc size
		if (parentDeclaration() != null)
		{
			Overlay pOwnrOvrl;
			if ((pOwnrOvrl=OwnerOverlay()) != null)
			{
				//int iOwnerX=pOwnrOvrl->X; int iOwnerY=pOwnrOvrl->Y;
				int iOwnerWdt=pOwnrOvrl.wdt.evaluated(); int iOwnerHgt=pOwnrOvrl.hgt.evaluated();
				X = x.evaluate(iOwnerWdt) + pOwnrOvrl.X;
				Y = y.evaluate(iOwnerHgt) + pOwnrOvrl.Y;
				Wdt = wdt.evaluate(iOwnerWdt);
				Hgt = hgt.evaluate(iOwnerHgt);
				OffX = ox.evaluate(iOwnerWdt);
				OffY = oy.evaluate(iOwnerHgt);
			}
		}
		// calc seed
		if ((Seed=FixedSeed)==0)
		{
			int r1=Random(32768);
			int r2=Random(65536);
			Seed=(r1<<16) | r2;
		}
		return this;
	}
	
	public Overlay firstOfChain() {
		Overlay result = this;
		for (OverlayBase o = this.prev; o != null && o.prev != null; o = o.prev) {
			if (!(o instanceof Overlay) || ((Overlay)o).operator() == null)
				break;
			result = (Overlay)o;
		}
		return result;
	}
	
	public boolean checkMask(int iX, int iY) {
		// bounds match?
		if (!loosebounds) if (iX<X || iY<Y || iX>=X+Wdt || iY>=Y+Hgt) return false;
		double dX=iX; double dY=iY;
		// apply turbulence
		if (turbulence.evaluated() > 0)
		{
			double Rad2Grad = Math.PI / 180;
			int j=3;
			for (int i=10; i<=turbulence.evaluated(); i*=10)
			{
				int Seed2; Seed2=Seed;
				for (int l=0; l<lambda.evaluated()+1; ++l)
				{
					for (double d=2; d<6; d+=1.5)
					{
						dX += Math.sin(((dX / 7 + Seed2 / zoomX.evaluated() + dY) / j + d) * Rad2Grad) * j / 2;
						dY += Math.cos(((dY / 7 + Seed2 / zoomY.evaluated() + dX) / j - d) * Rad2Grad) * j / 2;
					}
					Seed2 = (Seed * (Seed2<<3) + 0x4465) & 0xffff;
				}
				j+=3;
			}
		}
		// apply rotation
		if (rotate.evaluated() > 0)
		{
			/*double dRot=Rotate*pi/180;
			double l=sqrt((dX*dX)+(dY*dY));
			double o=atan(dY/dX);
			dX=cos(o+dRot)*l;
			dY=sin(o+dRot)*l;*/
			double dXo=dX, dYo=dY;
			dX = dXo*Math.cos(rotate.evaluated()) - dYo*Math.sin(rotate.evaluated());
			dY = dYo*Math.cos(rotate.evaluated()) + dXo*Math.sin(rotate.evaluated());
		}
		if (rotate.evaluated() > 0 || turbulence.evaluated() > 0)
			{ iX=(int) (dX*zoomX.evaluated()); iY=(int) (dY*zoomY.evaluated()); }
		else
			{ iX*=zoomX.evaluated(); iY*=zoomY.evaluated(); }
		// apply offset
		iX-=OffX*zoomX.evaluated(); iY-=OffY*zoomY.evaluated();
		// check bounds, if loose
		if (loosebounds) if (iX<X*zoomX.evaluated() || iY<Y*zoomY.evaluated() ||
			iX>=(X+Wdt)*zoomX.evaluated() || iY>=(Y+Hgt)*zoomY.evaluated()) return invert;
		// query algorithm
		return algo.compute(this, iX, iY)^invert;
	}
	
	boolean renderPix(int iX, int iY, int[] rPix, Operator eLastOp, boolean fLastSet, boolean fDraw, Overlay[] ppPixelSetOverlay)
	{
		// algo match?
		boolean SetThis=checkMask(iX, iY);
		boolean DoSet;
		// exec last op
		switch (eLastOp)
		{
		case And: // and
			DoSet=SetThis&&fLastSet;
			break;
		case Or: // or
			DoSet=SetThis||fLastSet;
			break;
		case XOr: // xor
			DoSet=SetThis^fLastSet;
			break;
		default: // no op
			DoSet=SetThis;
			break;
		}

		// set pix to local value and exec children, if no operator is following
		if ((DoSet && fDraw && operator == null) || grp)
		{
			// groups don't set a pixel value, if they're associated with an operator
			fDraw &= !grp || (operator == null);
			if (fDraw && DoSet && !mask)
			{
				rPix[0]=MatClr;
				if (ppPixelSetOverlay != null) ppPixelSetOverlay[0] = this;
			}
			boolean fLastSetC=false; eLastOp=null;
			// evaluate children overlays, if this was painted, too
			for (OverlayBase pChild : subOverlays)
				if (pChild instanceof Overlay)
				{
					Overlay pOvrl = (Overlay)pChild;
					fLastSetC=pOvrl.renderPix(iX, iY, rPix, eLastOp, fLastSetC, fDraw, ppPixelSetOverlay);
					if (grp && (pOvrl.operator == null))
						DoSet |= fLastSetC;
					eLastOp=pOvrl.operator;
				}
		}
		// done
		return DoSet;
	}

	public boolean inBounds(int x2, int iY) {
		// TODO Auto-generated method stub
		return false;
	}

	public boolean peekPix(int x2, int iY) {
		// TODO Auto-generated method stub
		return false;
	}

}
