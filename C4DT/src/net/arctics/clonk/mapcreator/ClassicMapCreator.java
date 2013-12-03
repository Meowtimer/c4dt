package net.arctics.clonk.mapcreator; 

import static net.arctics.clonk.mapcreator.GlobalFunctions.BoundBy;
import static net.arctics.clonk.mapcreator.GlobalFunctions.C4S_MaxPlayer;
import static net.arctics.clonk.mapcreator.GlobalFunctions.Fill;
import static net.arctics.clonk.mapcreator.GlobalFunctions.Inside;
import static net.arctics.clonk.mapcreator.GlobalFunctions.Random;

import org.eclipse.swt.graphics.ImageData;

/*
 * OpenClonk, http://www.openclonk.org
 *
 * Copyright (c) 1998-2000  Matthes Bender
 * Copyright (c) 2005, 2007  Sven Eberhardt
 * Copyright (c) 2006, 2009  Günther Brammer
 * Copyright (c) 2001-2009, RedWolf Design GmbH, http://www.clonk.de
 *
 * Portions might be copyrighted by other authors who have contributed
 * to OpenClonk.
 *
 * Permission to use, copy, modify, and/or distribute this software for any
 * purpose with or without fee is hereby granted, provided that the above
 * copyright notice and this permission notice appear in all copies.
 * See isc_license.txt for full license and disclaimer.
 *
 * "Clonk" is a registered trademark of Matthes Bender.
 * See clonk_trademark_license.txt for full license.
 */

/* Create map from dynamic landscape data in scenario */

/*
#include <C4Include.h>
#include <C4Map.h>

#include <C4Random.h>
#include <C4Texture.h>
#include <C4Group.h>

#include <CSurface8.h>
#include <Bitmap256.h>
*/

public class ClassicMapCreator extends MapCreator {
	
	int MapIFT;
	ImageData MapBuf;
	int MapWdt,MapHgt;
	int Exclusive;	

public ClassicMapCreator()
{
	Reset();
}

void Reset()
{
	MapIFT=128;
	MapBuf=null;
	Exclusive=-1;
}

void SetPix(final int x, final int y, final int col)
{
	// Safety
	if (!Inside(x,0,MapWdt-1) || !Inside(y,0,MapHgt-1)) return;
	// Exclusive
	if (Exclusive>-1) if (GetPix(x,y)!=Exclusive) return;
	// Set pix
	MapBuf.setPixel(x, y, col);
}

void SetSpot(final int x, final int y, final int rad, final int col)
{
	int ycnt,xcnt,lwdt,dpy;
	for (ycnt=-rad; ycnt<=rad; ycnt++)
	{
		lwdt= (int) Math.sqrt(rad*rad-ycnt*ycnt); dpy=y+ycnt;
		for (xcnt=-lwdt; xcnt<lwdt+((lwdt==0)?1:0); xcnt++)
			SetPix(x+xcnt,dpy,col);
	}
}

void DrawLayer(int x, int y, final int size, final int col)
{
	int cnt,cnt2;
	for (cnt=0; cnt<size; cnt++)
	{
		x+=Random(9)-4; y+=Random(3)-1;
		for (cnt2=Random(3); cnt2<5; cnt2++)
			{ SetPix(x+cnt2,y,col); SetPix(x+cnt2+1,y+1,col); }
	}
}

int GetPix(final int x, final int y)
{
	// Safety
	if (!Inside(x,0,MapWdt-1) || !Inside(y,0,MapHgt-1)) return 0;
	// Get pix
	return MapBuf.getPixel(x,y);
}

@Override
public void create(final ImageData sfcMap,
                          final LandscapeSection rLScape, final TextureMap rTexMap,
                          final boolean fLayers, int iPlayerNum)
{
	final double fullperiod= 20.0 * Math.PI;
	int ccol;
	int cx,cy;

	// Safeties
	if (sfcMap == null) return;
	iPlayerNum=BoundBy(iPlayerNum,1,C4S_MaxPlayer);

	// Set creator variables
	MapBuf = sfcMap;
	MapWdt = MapBuf.width; MapHgt = MapBuf.height;

	// Reset map (0 is sky)
	Fill(MapBuf, 0);

	// Surface
	ccol=rTexMap.GetIndexMatTex(rLScape.Material)+MapIFT;
	final float amplitude= rLScape.Amplitude.Evaluate();
	final float phase=     rLScape.Phase.Evaluate();
	float period=    rLScape.Period.Evaluate();
	if (rLScape.MapPlayerExtend > 0) period *= Math.min(iPlayerNum, LandscapeSection.C4S_MaxMapPlayerExtend);
	final float natural=   rLScape.Random.Evaluate();
	final int level0=    Math.min(MapWdt,MapHgt)/2;
	final int maxrange=  level0*3/4;
	double cy_curve,cy_natural; // -1.0 - +1.0 !

	double rnd_cy,rnd_tend; // -1.0 - +1.0 !
	rnd_cy= (Random(2000+1)-1000)/1000.0;
	rnd_tend= (Random(200+1)-100)/20000.0;

	for (cx=0; cx<MapWdt; cx++)
	{

		rnd_cy+=rnd_tend;
		rnd_tend+= (double) (Random(100+1)-50)/10000;
		if (rnd_tend>+0.05) rnd_tend=+0.05;
		if (rnd_tend<-0.05) rnd_tend=-0.05;
		if (rnd_cy<-0.5) rnd_tend+=0.01;
		if (rnd_cy>+0.5) rnd_tend-=0.01;

		cy_natural=rnd_cy*natural/100.0;
		cy_curve=Math.sin(fullperiod*period/100.0*cx/MapWdt
		             +2.0*Math.PI*phase/100.0) * amplitude/100.0;

		cy=level0+BoundBy((int)(maxrange*(cy_curve+cy_natural)),
		                  -maxrange,+maxrange);


		SetPix(cx,cy,ccol);
	}

	// Raise bottom to surface
	for (cx=0; cx<MapWdt; cx++)
		for (cy=MapHgt-1; (cy>=0) && GetPix(cx,cy) == 0; cy--)
			SetPix(cx,cy,ccol);
	// Raise liquid level
	Exclusive=0;
	ccol=rTexMap.GetIndexMatTex(rLScape.Liquid);
	final int wtr_level=rLScape.LiquidLevel.Evaluate();
	for (cx=0; cx<MapWdt; cx++)
		for (cy=MapHgt*(100-wtr_level)/100; cy<MapHgt; cy++)
			SetPix(cx,cy,ccol);
	Exclusive=-1;

	// Layers
	if (fLayers)
	{

		// Base material
		Exclusive=rTexMap.GetIndexMatTex(rLScape.Material)+MapIFT;

		int cnt,clayer,layer_num,sptx,spty;

		// Process layer name list
		for (clayer=0; clayer<rLScape.Layers.components().size(); clayer++)
			if (rLScape.Layers.name(clayer) != null)
			{
				// Draw layers
				ccol=rTexMap.GetIndexMatTex(rLScape.Layers.name(clayer))+MapIFT;
				layer_num=rLScape.Layers.count(clayer);
				layer_num=layer_num*MapWdt*MapHgt/15000;
				for (cnt=0; cnt<layer_num; cnt++)
				{
					// Place layer
					sptx=Random(MapWdt);
					for (spty=0; (spty<MapHgt) && (GetPix(sptx,spty)!=Exclusive); spty++) {}
					spty+=5+Random((MapHgt-spty)-10);
					DrawLayer(sptx,spty,Random(15),ccol);

				}
			}

		Exclusive=-1;

	}

}

/*bool Load(
        BYTE **pbypBuffer,
        int &rBufWdt, int &rMapWdt, int &rMapHgt,
        C4Group &hGroup, const char *szEntryName,
        C4TextureMap &rTexMap)
  {
  bool fOwnBuf=false;

  C4BMP256Info Bmp;

  // Access entry in group, read bitmap info
  if (!hGroup.AccessEntry(szEntryName)) return false;
  if (!hGroup.Read(&Bmp,sizeof(Bmp))) return false;
  if (!Bmp.Valid()) return false;
  if (!hGroup.Advance(Bmp.FileBitsOffset())) return false;

  // If buffer is present, check for sufficient size
  if (*pbypBuffer)
    {
    if ((Bmp.Info.biWidth>rMapWdt)
     || (Bmp.Info.biHeight>rMapHgt) ) return false;
    }
  // Else, allocate buffer, set sizes
  else
    {
    rMapWdt = Bmp.Info.biWidth;
    rMapHgt = Bmp.Info.biHeight;
    rBufWdt = rMapWdt; int dwBufWdt = rBufWdt; DWordAlign(dwBufWdt); rBufWdt = dwBufWdt;
    if (!(*pbypBuffer = new BYTE [rBufWdt*rMapHgt]))
      return false;
    fOwnBuf=true;
    }

  // Read bits to buffer
  for (int cline=Bmp.Info.biHeight-1; cline>=0; cline--)
    if (!hGroup.Read(*pbypBuffer+rBufWdt*cline,rBufWdt))
      { if (fOwnBuf) delete [] *pbypBuffer; return false; }

  // Validate texture indices
  MapBuf=*pbypBuffer;
  MapBufWdt=rBufWdt;
  MapWdt=rMapWdt; MapHgt=rMapHgt;
  ValidateTextureIndices(rTexMap);

  return true;
  }*/

/*
void ValidateTextureIndices(TextureMap rTextureMap)
{
	int iX,iY;
	for (iY=0; iY<MapHgt; iY++)
		for (iX=0; iX<MapWdt; iX++)
			if (!rTextureMap.GetEntry(GetPix(iX,iY)))
				SetPix(iX,iY,0);
}
*/
}