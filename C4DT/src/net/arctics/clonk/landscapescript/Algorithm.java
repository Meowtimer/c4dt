package net.arctics.clonk.landscapescript;

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
	poly;
	
	public class Peek {
		public Overlay overlay;
		public int x;
		public int y;
		public Overlay topOverlay;
		public Peek(Overlay overlay, int x, int y, Overlay topOverlay) {
			super();
			this.overlay = overlay;
			this.x = x;
			this.y = y;
			this.topOverlay = topOverlay;
		}
		public boolean prepare() {
			// zoom out
			x/=overlay.zoomX.evaluated(); y/=overlay.zoomY.evaluated();
			// get owning overlay
			Overlay ovrl2=overlay.OwnerOverlay();
			if (ovrl2 == null) return false;
			// get uppermost overlay
			Overlay nextOvrl;
			for (topOverlay=ovrl2; (nextOvrl=topOverlay.OwnerOverlay()) != null; topOverlay = nextOvrl) {}
			// get first of operator-chain
			ovrl2=ovrl2.firstOfChain();
			// set new overlay
			overlay=ovrl2;
			// success
			return true;
		}
	}
	
	public boolean compute(Overlay overlay, int iX, int iY) {
		final Range a = overlay.a;
		final Range b = overlay.b;
		final int s = overlay.seed.evaluated();
		final int z = 100;
		final int z2 = z*z;
		
		int pxa, pxb;
		switch (this) {
		case solid:
			return true;
		case checker:
			return (((iX/(100*10))%2)^((iY/(100*10))%2)) == 0 ? true : false;
		case bozo:
			// do some bozo stuff - keep it regular here, since it may be modified by turbulence
			final int iXC=(iX/10+s+(iY/80))%(z*2)-z;
			final int iYC=(iY/10+s+(iX/80))%(z*2)-z;
			final int id=Math.abs(iXC*iYC); // ((iSeed^iX^iY)%z)
			return id > z2*(a.evaluate(100)+10)/50;
		case sin:
			return iY > (Math.sin(iX/z*10)+1)*z*10;
		case boxes:
			// For percents instead of Pixels
			pxb = b.evaluate(overlay.Wdt);
			pxa = a.evaluate(overlay.Hgt);
			// return whether inside box
			return Math.abs(iX+(s%4738))%(pxb*z+1)<pxa*z+1 && Math.abs(iY+(s/4738))%(pxb*z+1)<pxa*z+1;
		case rndchecker:
			// randomly set squares with size of 10
			iX /= (z*10); iY /= (z*10);
			//$FALL-THROUGH$
		case random:
			return ((((s ^ (iX<<2) ^ (iY<<5))^((s>>16)+1+iX+(iY<<2)))/17)%(a.evaluate(100)+2)) == 0 ? true : false;
		case lines:
			// For percents instead of Pixels
			pxb = b.evaluate(overlay.Wdt);
			pxa = a.evaluate(overlay.Wdt);
			// return whether inside line
			return Math.abs(iX+(s%4738))%(pxb*z+1)<pxa*z+1;
		case border:
			//Overlay pTopOvrl;
			// get params before, since pOvrl will be changed by PreparePeek
			final int la=a.evaluate(overlay.Wdt); final int lb=b.evaluate(overlay.Hgt);
			// prepare a pixel peek from owner
			final Peek peek = new Peek(overlay, iX, iY, null);
			if (!peek.prepare()) return false;
			// query a/b pixels in x/y-directions
			for (int x=iX-la; x<=iX+la; x++) if (peek.topOverlay.inBounds(x, iY)) if (!peek.topOverlay.peekPix(x, iY)) return true;
			for (int y=iY-lb; y<=iY+lb; y++) if (peek.topOverlay.inBounds(iX, y)) if (!peek.topOverlay.peekPix(iX, y)) return true;
			// nothing found
			return false;
		case mandel:
			// how many iterations?
			int iMandelIter = a.evaluate(100) != 0 ? a.evaluate(100) : 1000;
			if (iMandelIter < 10) iMandelIter = 10;
			// calc c & ci values
			final double c =  ((double) iX / z / overlay.Wdt - .5 * ((double) overlay.zoomX.evaluated() / z)) * 4;
			final double ci = ((double) iY / z / overlay.Hgt - .5 * ((double) overlay.zoomY.evaluated() / z)) * 4;
			// create _z & _zi
			double _z = c, _zi = ci;
			double xz;
			int i;
			for (i=0; i<iMandelIter; i++)
			{
				xz = _z * _z - _zi * _zi;
				_zi = 2 * _z * _zi + ci;
				_z = xz + c;
				if (_z * _z + _zi * _zi > 4) break;
			}
			return !(i<iMandelIter);
		case rndall:
			return s%100<a.evaluate(100);
		case script:
			return false;
		case gradient:
			return (Math.abs((iX^(iY*3)) * 2531011L) % 214013L) % z > iX / overlay.Wdt;
		case poly:
			/*// Wo do not support empty polygons.
			if (overlay.subOverlays.size() == 0) return false;
			int uX = 0; // last point before current point
			int uY = 0; // with uY != iY
			int cX, cY; // current point
			int lX = 0; // x of really last point before current point
			int count = 0;
			boolean ignore = false;
			int zX; //Where edge intersects with line
			OverlayBase *pChild, *pStartChild;
			//get a point with uY!=iY, or anyone
			for (pChild = pOvrl -> ChildL; pChild->Prev; pChild = pChild->Prev)
				if (pChild->Type() == MCN_Point)
				{
					uX = ((C4MCPoint*) pChild) -> X * 100;
					lX = uX;
					uY = ((C4MCPoint*) pChild) -> Y * 100;
					if (iY != uY) break;
				}
			pStartChild = pChild -> Next;
			if (!pStartChild) pStartChild = pOvrl->Child0;
			if (!pStartChild) return false;
			for (pChild = pStartChild; ; pChild=pChild -> Next)
			{
				if (!pChild) pChild = pOvrl->Child0;
				if (pChild->Type() == MCN_Point)
				{
					cX = ((C4MCPoint*) pChild) -> X * 100;
					cY = ((C4MCPoint*) pChild) -> Y * 100;
					//If looking at line
					if (ignore)
					{
						//if C is on line
						if (cY == iY)
						{
							//if I is on edge
							if (((lX < iX) == (iX < cX)) || (cX == iX)) return true;
						}
						else
						{
							//if edge intersects line
							if ((uY < iY) == (iY < cY) && (lX >= iX)) count++;
							ignore = false;
							uX = cX;
							uY = cY;
						}
					} else if (cY == iY)
					{
						//are I and C the same points?
						if (cX == iX) return true;
						//skip this point for now
						ignore = true;
					}
					else
					{
						//if edge intersects line
						if ((uY < iY) == (iY <= cY))
							if (iX < Min (uX, cX))
								count++;
							else if (iX <= Max (uX, cX))
							{
								//and edge intersects with ray
								if (iX < (zX = ((cX - uX) * (iY - uY) / (cY - uY)) + uX)) count++;
								//if I lays on CU
								if (zX == iX) return true;
							}
						uX = cX;
						uY = cY;
					}
					lX = cX;
				}
				if (pChild -> Next == pStartChild) break;
				if (!pChild -> Next) if (pStartChild == pOvrl->Child0) break;
			}
			//if edge has changed side of ray uneven times
			if ((count & 1) > 0) return true; else return false;
			*/
		default:
			return false;
		}
	}
}