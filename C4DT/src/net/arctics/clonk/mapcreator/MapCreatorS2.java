package net.arctics.clonk.mapcreator;

import org.eclipse.swt.graphics.ImageData;

public class MapCreatorS2 extends MapCreator {
	public MaterialMap MatMap;
	public TextureMap TexMap;
/*
 * OpenClonk, http://www.openclonk.org
 *
 * Copyright (c) 2001-2002, 2008  Peter Wortmann
 * Copyright (c) 2001-2002, 2005  Sven Eberhardt
 * Copyright (c) 2004, 2006-2009  Günther Brammer
 * Copyright (c) 2005-2006  Matthes Bender
 * Copyright (c) 2009  Nicolas Hake
 * Copyright (c) 2010  Benjamin Herr
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
// complex dynamic landscape creator

/*
#include <C4Include.h>
#include <C4MapCreatorS2.h>
#include <C4Random.h>
#include <C4Game.h>
#include <C4Aul.h>
#include <C4Material.h>
#include <C4ScriptHost.h>
#include <C4Texture.h>
#include <C4Record.h>

namespace {
	// node attribute entry for SetField search
	enum C4MCValueType
	{
		C4MCV_None,
		C4MCV_Integer,
		C4MCV_Percent,
		C4MCV_Pixels,
		C4MCV_Material,
		C4MCV_Texture,
		C4MCV_Algorithm,
		C4MCV_Boolean,
		C4MCV_Zoom,
		C4MCV_ScriptFunc
	};

	template<typename T>
	class MemberAdapter {
	public:
		typedef char (T::*OffsetType);

		MemberAdapter(T& object, OffsetType offset)
				: Object(object), Offset(offset)
		{
		}

		template<typename U>
		U& As()
		{
			typedef U (T::*TargetPtrType);
			return Object.*reinterpret_cast<TargetPtrType>(Offset);
		}

	private:
		T& Object;
		OffsetType Offset;
	};

	typedef MemberAdapter<C4MCOverlay>::OffsetType C4MCOverlayOffsetType;

	struct C4MCNodeAttr
	{
		const char* Name; // name of field
		C4MCValueType Type; // type of field
		C4MCOverlayOffsetType Offset; // offset of field in overlay MCOverlay-class
	};

	extern C4MCNodeAttr C4MCOvrlMap[];
}*/

/* --- C4MCCallbackArray --- */

/*
C4MCCallbackArray::C4MCCallbackArray(C4AulFunc *pSFunc, C4MapCreatorS2 *pMapCreator)
{
	// store fn
	pSF = pSFunc;
	// zero fields
	pMap=NULL; pNext=NULL;
	// store and add in map creator
	if ((this->pMapCreator=pMapCreator))
		pMapCreator->CallbackArrays.Add(this);
	// done
}

C4MCCallbackArray::~C4MCCallbackArray()
{
	// clear map, if present
	if (pMap) delete [] pMap;
}

void C4MCCallbackArray::EnablePixel(int32_t iX, int32_t iY)
{
	// array not yet created? then do that now!
	if (!pMap)
	{
		// safety
		if (!pMapCreator) return;
		// get current map size
		C4MCMap *pCurrMap = pMapCreator->pCurrentMap;
		if (!pCurrMap) return;
		iWdt = pCurrMap->Wdt; iHgt = pCurrMap->Hgt;
		// create bitmap
		int32_t iSize=(iWdt*iHgt+7)/8;
		pMap = new BYTE[iSize];
		memset(pMap, 0, iSize);
		// done
	}
	// safety: do not set outside map!
	if (iX<0 || iY<0 || iX>=iWdt || iY>=iHgt) return;
	// set in map
	int32_t iIndex = iX + iY*iWdt;
	pMap[iIndex/8] |= 1<<(iIndex%8);
	// done
}

void C4MCCallbackArray::Execute(int32_t iMapZoom)
{
	// safety
	if (!pSF || !pMap) return;
	// pre-create parset
	C4AulParSet Pars(C4VInt(0), C4VInt(0), C4VInt(iMapZoom));
	// call all funcs
	int32_t iIndex=iWdt*iHgt;
	while (iIndex--)
		if (pMap[iIndex/8]&(1<<(iIndex%8)))
		{
			// set pars
			Pars[0] = C4VInt((iIndex%iWdt) * iMapZoom - (iMapZoom/2));
			Pars[1] = C4VInt((iIndex/iWdt) * iMapZoom - (iMapZoom/2));
			// call
			pSF->Exec(NULL, &Pars);
		}
	// done
}*/



/* --- C4MCCallbackArrayList --- */
/*
void C4MCCallbackArrayList::Add(C4MCCallbackArray *pNewArray)
{
	// add to end
	if (pFirst)
	{
		C4MCCallbackArray *pLast = pFirst;
		while (pLast->pNext) pLast=pLast->pNext;
		pLast->pNext=pNewArray;
	}
	else pFirst=pNewArray;
}

void C4MCCallbackArrayList::Clear()
{
	// remove all arrays
	C4MCCallbackArray *pArray, *pNext=pFirst;
	while ((pArray=pNext))
	{
		pNext=pArray->pNext;
		delete pArray;
	}
	// zero first-field
	pFirst=NULL;
}

void C4MCCallbackArrayList::Execute(int32_t iMapZoom)
{
	// execute all arrays
	for (C4MCCallbackArray *pArray = pFirst; pArray; pArray=pArray->pNext)
		pArray->Execute(iMapZoom);
}
*/



/* --- C4MCNode --- */
/*
C4MCNode::C4MCNode(C4MCNode *pOwner)
{
	// reg to owner
	Reg2Owner(pOwner);
	// no name
	*Name=0;
}

C4MCNode::C4MCNode(C4MCNode *pOwner, C4MCNode &rTemplate, bool fClone)
{
	// set owner and stuff
	Reg2Owner(pOwner);
	// copy children from template
	for (C4MCNode *pChild=rTemplate.Child0; pChild; pChild=pChild->Next)
		pChild->clone(this);
	// no name
	*Name=0;
}

C4MCNode::~C4MCNode()
{
	// clear
	Clear();
	// remove from list
	if (Prev) Prev->Next = Next; else if (Owner) Owner->Child0 = Next;
	if (Next) Next->Prev = Prev; else if (Owner) Owner->ChildL = Prev;
}

void C4MCNode::Reg2Owner(C4MCNode *pOwner)
{
	// init list
	Child0=ChildL=NULL;
	// owner?
	if ((Owner=pOwner))
	{
		// link into it
		if ((Prev = Owner->ChildL))
			Prev->Next = this;
		else
			Owner->Child0 = this;
		Owner->ChildL = this;
		MapCreator=pOwner->MapCreator;
	}
	else
	{
		Prev=NULL;
		MapCreator=NULL;
	}
	// we're always last entry
	Next=NULL;
}

void C4MCNode::Clear()
{
	// delete all children; they'll unreg themselves
	while (Child0) delete Child0;
}

C4MCOverlay *C4MCNode::OwnerOverlay()
{
	for (C4MCNode *pOwnr=Owner; pOwnr; pOwnr=pOwnr->Owner)
		if (C4MCOverlay *pOwnrOvrl=pOwnr->Overlay())
			return pOwnrOvrl;
	// no overlay-owner
	return NULL;
}

C4MCNode *C4MCNode::GetNodeByName(const char *szName)
{
	// search local list (backwards: last node has highest priority)
	for (C4MCNode *pChild=ChildL; pChild; pChild=pChild->Prev)
		// name match?
		if (SEqual(pChild->Name, szName))
			// yeah, success!
			return pChild;
	// search owner, if present
	if (Owner) return Owner->GetNodeByName(szName);
	// nothing found
	return NULL;
}

bool C4MCNode::SetField(C4MCParser *pParser, const char *szField, const char *szSVal, int32_t iVal, C4MCTokenType ValType)
{
	// no fields in base class
	return false;
}

int32_t C4MCNode::IntPar(C4MCParser *pParser, const char *szSVal, int32_t iVal, C4MCTokenType ValType)
{
	// check if int32_t
	if (ValType == MCT_INT || ValType == MCT_PERCENT || ValType == MCT_PX)
		return iVal;
	throw C4MCParserErr(pParser, C4MCErr_FieldValInvalid, szSVal);
}

const char *C4MCNode::StrPar(C4MCParser *pParser, const char *szSVal, int32_t iVal, C4MCTokenType ValType)
{
	// check if identifier
	if (ValType != MCT_IDTF)
		throw C4MCParserErr(pParser, C4MCErr_FieldValInvalid, szSVal);
	return szSVal;
}

#define IntPar IntPar(pParser, szSVal, iVal, ValType) // shortcut for checked int32_t param
#define StrPar StrPar(pParser, szSVal, iVal, ValType) // shortcut for checked str param

void C4MCNode::ReEvaluate()
{
	// evaluate ourselves
	Evaluate();
	// evaluate children
	for (C4MCNode *pChild=Child0; pChild; pChild=pChild->Next)
		pChild->ReEvaluate();
}


// overlay

C4MCOverlay::C4MCOverlay(C4MCNode *pOwner) : C4MCNode(pOwner)
{
	// zero members
	X=Y=Wdt=Hgt=OffX=OffY=0;
	Material=MNone;
	*Texture=0;
	Op=MCT_NONE;
	MatClr=0;
	Algorithm=NULL;
	Sub=false;
	ZoomX=ZoomY=0;
	FixedSeed=Seed=0;
//  Alpha=Beta=0;
	Turbulence=Lambda=Rotate=0;
	Invert=LooseBounds=Group=Mask=false;
	pEvaluateFunc=pDrawFunc=NULL;
}

C4MCOverlay::C4MCOverlay(C4MCNode *pOwner, C4MCOverlay &rTemplate, bool fClone) : C4MCNode(pOwner, rTemplate, fClone)
{
	// copy fields
	X=rTemplate.X; Y=rTemplate.Y; Wdt=rTemplate.Wdt; Hgt=rTemplate.Hgt;
	RX=rTemplate.RX; RY=rTemplate.RY; RWdt=rTemplate.RWdt; RHgt=rTemplate.RHgt;
	OffX=rTemplate.OffX; OffY=rTemplate.OffY; ROffX=rTemplate.ROffX; ROffY=rTemplate.ROffY;
	Material=rTemplate.Material;
	SCopy(rTemplate.Texture, Texture, C4MaxName);
	Algorithm=rTemplate.Algorithm;
	Sub=rTemplate.Sub;
	ZoomX=rTemplate.ZoomX; ZoomY=rTemplate.ZoomY;
	MatClr=rTemplate.MatClr;
	Seed=rTemplate.Seed;
	Alpha=rTemplate.Alpha; Beta=rTemplate.Beta; Turbulence=rTemplate.Turbulence; Lambda=rTemplate.Lambda;
	Rotate=rTemplate.Rotate;
	Invert=rTemplate.Invert; LooseBounds=rTemplate.LooseBounds; Group=rTemplate.Group; Mask=rTemplate.Mask;
	FixedSeed=rTemplate.FixedSeed;
	pEvaluateFunc=rTemplate.pEvaluateFunc;
	pDrawFunc=rTemplate.pDrawFunc;
	// zero non-template-fields
	if (fClone) Op=rTemplate.Op; else Op=MCT_NONE;
}

bool C4MCOverlay::PeekPix(int32_t iX, int32_t iY)
{
	// start with this one
	C4MCOverlay *pOvrl=this; bool fLastSetC=false; C4MCTokenType eLastOp=MCT_NONE; BYTE Crap;
	// loop through op chain
	while (1)
	{
		fLastSetC=pOvrl->RenderPix(iX, iY, Crap, eLastOp, fLastSetC, false);
		eLastOp=pOvrl->Op;
		if (!pOvrl->Op) break;
		// must be another overlay, since there's an operator
		// hopefully, the preparser will catch all the other crap
		pOvrl=pOvrl->Next->Overlay();
	}
	// return result
	return fLastSetC;
}

// point

C4MCPoint::C4MCPoint(C4MCNode *pOwner) : C4MCNode(pOwner)
{
	// zero members
	X=Y=0;
}

C4MCPoint::C4MCPoint(C4MCNode *pOwner, C4MCPoint &rTemplate, bool fClone) : C4MCNode(pOwner, rTemplate, fClone)
{
	// copy fields
	X=rTemplate.X; Y=rTemplate.Y;
	RX=rTemplate.RX; RY=rTemplate.RY;
}

void C4MCPoint::Default()
{
	X=Y=0;
}

bool C4MCPoint::SetField(C4MCParser *pParser, const char *szField, const char *szSVal, int32_t iVal, C4MCTokenType ValType)
{
	// only explicit %/px
	if (ValType == MCT_INT) return false;
	if (SEqual (szField, "x"))
	{
		RX.Set(IntPar, ValType == MCT_PERCENT);
		return true;
	}
	else if (SEqual (szField, "y"))
	{
		RY.Set(IntPar, ValType == MCT_PERCENT);
		return true;
	}
	return false;
}

void C4MCPoint::Evaluate()
{
	// inherited
	C4MCNode::Evaluate();
	// get mat color
	// calc size
	if (Owner)
	{
		C4MCOverlay *pOwnrOvrl;
		if ((pOwnrOvrl=OwnerOverlay()))
		{
			X = RX.Evaluate(pOwnrOvrl->Wdt) + pOwnrOvrl->X;
			Y = RY.Evaluate(pOwnrOvrl->Hgt) + pOwnrOvrl->Y;
		}
	}
}

// map

C4MCMap::C4MCMap(C4MCNode *pOwner) : C4MCOverlay(pOwner)
{

}

C4MCMap::C4MCMap(C4MCNode *pOwner, C4MCMap &rTemplate, bool fClone) : C4MCOverlay(pOwner, rTemplate, fClone)
{

}

void C4MCMap::Default()
{
	// inherited
	C4MCOverlay::Default();
	// size by landscape def
	Wdt=MapCreator->Landscape->MapWdt.Evaluate();
	Hgt=MapCreator->Landscape->MapHgt.Evaluate();
	// map player extend
	MapCreator->PlayerCount = Max(MapCreator->PlayerCount, 1);
	if (MapCreator->Landscape->MapPlayerExtend)
		Wdt = Min(Wdt * Min(MapCreator->PlayerCount, (int) C4S_MaxMapPlayerExtend), (int) MapCreator->Landscape->MapWdt.Max);
}

bool C4MCMap::RenderTo(BYTE *pToBuf, int32_t iPitch)
{
	// set current render target
	if (MapCreator) MapCreator->pCurrentMap=this;
	// draw pixel by pixel
	for (int32_t iY=0; iY<Hgt; iY++)
	{
		for (int32_t iX=0; iX<Wdt; iX++)
		{
			// default to sky
			*pToBuf=0;
			// render pixel value
			C4MCOverlay *pRenderedOverlay = NULL;
			RenderPix(iX, iY, *pToBuf, MCT_NONE, false, true, &pRenderedOverlay);
			// add draw-callback for rendered overlay
			if (pRenderedOverlay)
				if (pRenderedOverlay->pDrawFunc)
					pRenderedOverlay->pDrawFunc->EnablePixel(iX, iY);
			// next pixel
			pToBuf++;
		}
		// next line
		pToBuf+=iPitch-Wdt;
	}
	// reset render target
	if (MapCreator) MapCreator->pCurrentMap=NULL;
	// success
	return true;
}

void C4MCMap::SetSize(int32_t iWdt, int32_t iHgt)
{
	// store new size
	Wdt=iWdt; Hgt=iHgt;
	// update relative values
	MapCreator->ReEvaluate();
}


// map creator

C4MapCreatorS2::C4MapCreatorS2(C4SLandscape *pLandscape, C4TextureMap *pTexMap, C4MaterialMap *pMatMap, int iPlayerCount) : C4MCNode(NULL)
{
	// me r b creator
	MapCreator=this;
	// store members
	Landscape=pLandscape; TexMap=pTexMap; MatMap=pMatMap;
	PlayerCount=iPlayerCount;
	// set engine field for default stuff
	DefaultMap.MapCreator=this;
	DefaultOverlay.MapCreator=this;
	DefaultPoint.MapCreator=this;
	// default to landscape settings
	Default();
}

C4MapCreatorS2::~C4MapCreatorS2()
{
	// clear fields
	Clear();
}

void C4MapCreatorS2::Default()
{
	// default templates
	DefaultMap.Default();
	DefaultOverlay.Default();
	DefaultPoint.Default();
	pCurrentMap=NULL;
}

void C4MapCreatorS2::Clear()
{
	// clear nodes
	C4MCNode::Clear();
	// clear callbacks
	CallbackArrays.Clear();
	// defaults templates
	Default();
}

bool C4MapCreatorS2::ReadFile(const char *szFilename, C4Group *pGrp)
{
	// create parser and read file
	try
	{
		C4MCParser(this).ParseFile(szFilename, pGrp);
	}
	catch (C4MCParserErr err)
	{
		err.show();
		return false;
	}
	// success
	return true;
}

bool C4MapCreatorS2::ReadScript(const char *szScript)
{
	// create parser and read
	try
	{
		C4MCParser(this).Parse(szScript);
	}
	catch (C4MCParserErr err)
	{
		err.show();
		return false;
	}
	// success
	return true;
}

C4MCMap *C4MapCreatorS2::GetMap(const char *szMapName)
{
	C4MCMap *pMap=NULL; C4MCNode *pNode;
	// get map
	if (szMapName && *szMapName)
	{
		// by name...
		if ((pNode = GetNodeByName(szMapName)))
			if (pNode->Type() == MCN_Map)
				pMap = (C4MCMap *) pNode;
	}
	else
	{
		// or simply last map entry
		for (pNode = ChildL; pNode; pNode=pNode->Prev)
			if (pNode->Type() == MCN_Map)
			{
				pMap = (C4MCMap *) pNode;
				break;
			}
	}
	return pMap;
}

CSurface8 * C4MapCreatorS2::Render(const char *szMapName)
{
	// get map
	C4MCMap *pMap=GetMap(szMapName);
	if (!pMap) return NULL;

	// get size
	int32_t sfcWdt, sfcHgt;
	sfcWdt=pMap->Wdt; sfcHgt=pMap->Hgt;
	if (!sfcWdt || !sfcHgt) return NULL;

	// create surface
	CSurface8 * sfc = new CSurface8(sfcWdt, sfcHgt);

	// render map to surface
	pMap->RenderTo(sfc->Bits, sfc->Pitch);

	// success
	return sfc;
}

static inline void DWordAlign(int &val)
{
	if (val%4) { val>>=2; val<<=2; val+=4; }
}

BYTE *C4MapCreatorS2::RenderBuf(const char *szMapName, int32_t &sfcWdt, int32_t &sfcHgt)
{
	// get map
	C4MCMap *pMap=GetMap(szMapName);
	if (!pMap) return NULL;

	// get size
	sfcWdt=pMap->Wdt; sfcHgt=pMap->Hgt;
	if (!sfcWdt || !sfcHgt) return NULL;
	int dwSfcWdt = sfcWdt;
	DWordAlign(dwSfcWdt);
	sfcWdt = dwSfcWdt;

	// create buffer
	BYTE *buf=new BYTE[sfcWdt*sfcHgt];

	// render and return it
	pMap->RenderTo(buf, sfcWdt);
	return buf;
}

C4MCParserErr::C4MCParserErr(C4MCParser *pParser, const char *szMsg)
{
	// create error message
	sprintf(Msg, "%s: %s (%d)", pParser->Filename, szMsg, pParser->Code ? SGetLine(pParser->Code, pParser->CPos) : 0);
}

C4MCParserErr::C4MCParserErr(C4MCParser *pParser, const char *szMsg, const char *szPar)
{
	char Buf[C4MaxMessage];
	// create error message
	sprintf(Buf, szMsg, szPar);
	sprintf(Msg, "%s: %s (%d)", pParser->Filename, Buf, pParser->Code ? SGetLine(pParser->Code, pParser->CPos) : 0);
}

void C4MCParserErr::show()
{
	// log error
	Log(Msg);
}


// parser

C4MCParser::C4MCParser(C4MapCreatorS2 *pMapCreator)
{
	// store map creator
	MapCreator=pMapCreator;
	// reset some fields
	Code=NULL; CPos=NULL; *Filename=0;
}

C4MCParser::~C4MCParser()
{
	// clean up
	Clear();
}

void C4MCParser::Clear()
{
	// clear code if present
	if (Code) delete [] Code; Code=NULL; CPos=NULL;
	// reset filename
	*Filename=0;
}

bool C4MCParser::AdvanceSpaces()
{
	char C, C2 = (char) 0;
	// defaultly, not in comment
	int32_t InComment = 0; // 0/1/2 = no comment/line comment/multi line comment
	// don't go past end
	while ((C = *CPos))
	{
		// loop until out of comment and non-whitespace is found
		switch (InComment)
		{
		case 0:
			if (C == '/')
			{
				CPos++;
				switch (*CPos)
				{
				case '/': InComment = 1; break;
				case '*': InComment = 2; break;
				default: CPos--; return true;
				}
			}
			else if ((BYTE) C > 32) return true;
			break;
		case 1:
			if (((BYTE) C == 13) || ((BYTE) C == 10)) InComment = 0;
			break;
		case 2:
			if ((C == '/') && (C2 == '*')) InComment = 0;
			break;
		}
		// next char; store prev
		CPos++; C2 = C;
	}
	// end of code reached; return false
	return false;
}

bool C4MCParser::GetNextToken()
{
	// move to start of token
	if (!AdvanceSpaces()) { CurrToken=MCT_EOF; return false; }
	// store offset
	const char *CPos0 = CPos;
	int32_t Len = 0;
	// token get state
	enum TokenGetState
	{
		TGS_None,       // just started
		TGS_Ident,      // getting identifier
		TGS_Int,        // getting integer
		TGS_Dir         // getting directive
	};
	TokenGetState State = TGS_None;

	// loop until finished
	while (true)
	{
		// get char
		char C = *CPos;

		switch (State)
		{
		case TGS_None:
			// get token type by first char
			// +/- are operators
			if ((((C >= '0') && (C <= '9')) || (C == '+') || (C == '-')))
				State = TGS_Int;                              // integer by +, -, 0-9
			else if (C == '#')  State = TGS_Dir;                              // directive by "#"
			else if (C == ';') {CPos++; CurrToken=MCT_SCOLON;   return true; }  // ";"
			else if (C == '=') {CPos++; CurrToken=MCT_EQ;       return true; }  // "="
			else if (C == '{') {CPos++; CurrToken=MCT_BLOPEN;   return true; }  // "{"
			else if (C == '}') {CPos++; CurrToken=MCT_BLCLOSE;  return true; }  // "}"
			else if (C == '&') {CPos++; CurrToken=MCT_AND;      return true; }  // "&"
			else if (C == '|') {CPos++; CurrToken=MCT_OR;       return true; }  // "|"
			else if (C == '^') {CPos++; CurrToken=MCT_XOR;      return true; }  // "^"
			else if (C >= '@')  State = TGS_Ident;                            // identifier by all non-special chars
			else
			{
				// unrecognized char
				CPos++;
				throw C4MCParserErr(this, "unexpected character found");
			}
			break;

		case TGS_Ident: // ident and directive: parse until non ident-char is found
		case TGS_Dir:
			if (((C < '0') || (C > '9')) && ((C < 'a') || (C > 'z')) && ((C < 'A') || (C > 'Z')) && (C != '_'))
			{
				// return ident/directive
				Len = Min<int32_t>(Len, C4MaxName);
				SCopy(CPos0, CurrTokenIdtf, Len);
				if (State==TGS_Ident) CurrToken=MCT_IDTF; else CurrToken=MCT_DIR;
				return true;
			}
			break;

		case TGS_Int: // integer: parse until non-number is found
			if ((C < '0') || (C > '9'))
			{
				// return integer
				Len = Min<int32_t>(Len, C4MaxName);
				CurrToken=MCT_INT;
				// check for "-"
				if (Len == 1 && *CPos0 == '-')
				{
					CurrToken = MCT_RANGE;
					return true;
				}
				else if ('%' == C) { CPos++; CurrToken=MCT_PERCENT; } // "%"
				else if ('p' == C)
				{
					// p or px
					++CPos;
					if ('x' == *CPos) ++CPos;
					CurrToken=MCT_PX;
				}
				SCopy(CPos0, CurrTokenIdtf, Len);
				// it's not, so return the int32_t
				sscanf(CurrTokenIdtf, "%d", &CurrTokenVal);
				return true;
			}
			break;

		}
		// next char
		CPos++; Len++;
	}

}

static void PrintNodeTree(C4MCNode *pNode, int depth)
{
	for (int i = 0; i < depth; ++i)
		printf("  ");
	switch (pNode->Type())
	{
	case MCN_Node: printf("Node %s\n", pNode->Name); break;
	case MCN_Overlay: printf("Overlay %s\n", pNode->Name); break;
	case MCN_Point: printf("Point %s\n", pNode->Name); break;
	case MCN_Map: printf("Map %s\n", pNode->Name); break;
	}
	for (C4MCNode * pChild = pNode->Child0; pChild; pChild = pChild->Next)
		PrintNodeTree(pChild, depth + 1);
}

void C4MCParser::ParseTo(C4MCNode *pToNode)
{
	C4MCNode *pNewNode=NULL;  // new node
	bool Done=false;          // finished?
	C4MCNodeType LastOperand = C4MCNodeType(-1); // last first operand of operator
	char FieldName[C4MaxName];// buffer for current field to access
	C4MCNode *pCpyNode;       // node to copy from
	// current state
	enum ParseState
	{
		PS_NONE,      // just started
		PS_KEYWD1,    // got block-opening keyword (map, overlay etc.)
		PS_KEYWD1N,   // got name for block
		PS_AFTERNODE, // node has been parsed; expect ; or operator
		PS_GOTOP,     // got operator
		PS_GOTIDTF,   // got identifier, expect '=', ';' or '{'; identifier remains in CurrTokenIdtf
		PS_GOTOPIDTF, // got identifier after operator; accept ';' or '{' only
		PS_SETFIELD   // accept field value; field is stored in FieldName
	};
	ParseState State = PS_NONE;
	// parse until end of file (or block)
	while (GetNextToken())
	{
		switch (State)
		{
		case PS_NONE:
		case PS_GOTOP:
			switch (CurrToken)
			{
			case MCT_DIR:
				// top level needed
				if (!pToNode->GlobalScope())
					throw C4MCParserErr(this, C4MCErr_NoDirGlobal);
				// no directives so far
				throw C4MCParserErr(this, C4MCErr_UnknownDir, CurrTokenIdtf);
				break;
			case MCT_IDTF:
				// identifier: check keywords
				if (SEqual(CurrTokenIdtf, C4MC_Overlay))
				{
					// overlay: create overlay node, using default template
					pNewNode = new C4MCOverlay(pToNode, MapCreator->DefaultOverlay, false);
					State=PS_KEYWD1;
				}
				else if (SEqual(CurrTokenIdtf, C4MC_Point) && !pToNode->GetNodeByName(CurrTokenIdtf))
				{
					// only in overlays
					if (!pToNode->Type() == MCN_Overlay)
						throw C4MCParserErr(this, C4MCErr_PointOnlyOvl);
					// create point node, using default template
					pNewNode = new C4MCPoint(pToNode, MapCreator->DefaultPoint, false);
					State=PS_KEYWD1;
				}
				else if (SEqual(CurrTokenIdtf, C4MC_Map))
				{
					// map: check top level
					if (!pToNode->GlobalScope())
						throw C4MCParserErr(this, C4MCErr_MapNoGlobal);
					// create map node, using default template
					pNewNode = new C4MCMap(pToNode, MapCreator->DefaultMap, false);
					State=PS_KEYWD1;
				}
				else
				{
					// so this is either a field-set or a defined node
					// '=', ';' or '{' may follow, none of these will clear the CurrTokenIdtf
					// so safely assume it preserved and just update the state
					if (State==PS_GOTOP) State=PS_GOTOPIDTF; else State=PS_GOTIDTF;
				}
				// operator: check type
				if (State == PS_GOTOP && pNewNode)
					if (LastOperand != pNewNode->Type())
						throw C4MCParserErr(this, C4MCErr_OpTypeErr);
				break;
			case MCT_BLCLOSE:
			case MCT_EOF:
				// block done
				Done=true;
				break;
			default:
				// we don't like that
				throw C4MCParserErr(this, C4MCErr_IdtfExp);
				break;
			}
			break;
		case PS_KEYWD1:
			if (CurrToken==MCT_IDTF)
			{
				// name the current node
				SCopy(CurrTokenIdtf, pNewNode->Name, C4MaxName);
				State=PS_KEYWD1N;
				break;
			}
			else if (pToNode->GlobalScope())
			{
				// disallow unnamed nodes in global scope
				throw C4MCParserErr(this, C4MCErr_UnnamedNoGlbl);
			}
			// in local scope, allow unnamed; so continue
		case PS_KEYWD1N:
			// do expect a block opening
			if (CurrToken!=MCT_BLOPEN)
				throw C4MCParserErr(this, C4MCErr_BlOpenExp);
			// parse new node
			ParseTo(pNewNode);
			// check file end
			if (CurrToken==MCT_EOF)
				throw C4MCParserErr(this, C4MCErr_EOF);
			// reset state
			State=PS_AFTERNODE;
			break;
		case PS_GOTIDTF:
		case PS_GOTOPIDTF:
			switch (CurrToken)
			{
			case MCT_EQ:
				// so it's a field set
				// not after operators
				if (State==PS_GOTOPIDTF)
					throw C4MCParserErr(this, C4MCErr_Obj2Exp);
				// store field name
				SCopy(CurrTokenIdtf, FieldName, C4MaxName);
				// update state to accept value
				State=PS_SETFIELD;
				break;
			case MCT_BLOPEN:
			case MCT_SCOLON:
			case MCT_AND: case MCT_OR: case MCT_XOR:
				// so it's a node copy
				// local scope only
				if (pToNode->GlobalScope())
					throw C4MCParserErr(this, C4MCErr_ReinstNoGlobal, CurrTokenIdtf);
				// get the node
				pCpyNode=pToNode->GetNodeByName(CurrTokenIdtf);
				if (!pCpyNode)
					throw C4MCParserErr(this, C4MCErr_UnknownObj, CurrTokenIdtf);
				// create the copy
				switch (pCpyNode->Type())
				{
				case MCN_Overlay:
					// create overlay
					pNewNode=new C4MCOverlay(pToNode, *((C4MCOverlay *) pCpyNode), false);
					break;
				case MCN_Map:
					// maps not allowed
					if (pCpyNode->Type() == MCN_Map)
						throw C4MCParserErr(this, C4MCErr_MapNoGlobal, CurrTokenIdtf);
					break;
				default:
					// huh?
					throw C4MCParserErr(this, C4MCErr_ReinstUnknown, CurrTokenIdtf);
					break;
				}
				// check type for operators
				if (State==PS_GOTOPIDTF)
					if (LastOperand != pNewNode->Type())
						throw C4MCParserErr(this, C4MCErr_OpTypeErr);
				// further overloads?
				if (CurrToken==MCT_BLOPEN)
				{
					// parse new node
					ParseTo(pNewNode);
					// get next token, as we'll simply fall through to PS_AFTERNODE
					GetNextToken();
					// check file end
					if (CurrToken==MCT_EOF)
						throw C4MCParserErr(this, C4MCErr_EOF);
				}
				// reset state
				State=PS_AFTERNODE;
				break;

			default:
				throw C4MCParserErr(this, C4MCErr_EqSColonBlOpenExp);
				break;
			}
			// fall through to next case, if it was a named node reinstanciation
			if (State != PS_AFTERNODE) break;
		case PS_AFTERNODE:
			// expect operator or semicolon
			switch (CurrToken)
			{
			case MCT_SCOLON:
				// reset state
				State=PS_NONE;
				break;
			case MCT_AND:
			case MCT_OR:
			case MCT_XOR:
				// operator: not in global scope
				if (pToNode->GlobalScope())
					throw C4MCParserErr(this, C4MCErr_OpsNoGlobal);
				// set operator
				if (!pNewNode->SetOp(CurrToken))
					throw C4MCParserErr(this, "';' expected");
				LastOperand=pNewNode->Type();
				// update state
				State=PS_GOTOP;
				break;
			default:
				throw C4MCParserErr(this, C4MCErr_SColonOrOpExp);
				break;
			}
			// node done
			// evaluate node and children, if this is top-level
			// we mustn't evaluate everything immediately, because parents must be evaluated first!
			if (pToNode->GlobalScope()) pNewNode->ReEvaluate();
			pNewNode=NULL;
			break;
		case PS_SETFIELD:
			ParseValue (pToNode, FieldName);
			/*// set field: accept integer constants and identifiers
			switch (CurrToken)
			  {
			  case MCT_IDTF:
			    // reset value field
			    CurrTokenVal=0;
			  case MCT_INT:
			    break;
			  default:
			    throw C4MCParserErr(this, C4MCErr_FieldConstExp, CurrTokenIdtf);
			    break;
			  }
			// set field
			if (!pToNode->SetField(this, FieldName, CurrTokenIdtf, CurrTokenVal, CurrToken))
			  // field not found
			  throw C4MCParserErr(this, C4MCErr_Field404, FieldName);
			// now, the one and only thing to get is a semicolon
			if (!GetNextToken())
			  throw C4MCParserErr(this, C4MCErr_EOF);
			if (CurrToken != MCT_SCOLON)
			  throw C4MCParserErr(this, C4MCErr_SColonExp);*/
			// reset state
			State=PS_NONE;
			break;
		}
		// don't get another token!
		if (Done) break;
	}
	// end of file expected?
	if (State != PS_NONE)
	{
		if (State == PS_GOTOP)
			throw C4MCParserErr(this, C4MCErr_Obj2Exp);
		else
			throw C4MCParserErr(this, C4MCErr_EOF);
	}
}

void C4MCParser::ParseValue(C4MCNode *pToNode, const char *szFieldName)
{
	int32_t Value;
	C4MCTokenType Type;
	switch (CurrToken)
	{
	case MCT_IDTF:
	{
		// set field
		if (!pToNode->SetField(this, szFieldName, CurrTokenIdtf, 0, CurrToken))
			// field not found
			throw C4MCParserErr(this, C4MCErr_Field404, szFieldName);
		if (!GetNextToken())
			throw C4MCParserErr(this, C4MCErr_EOF);
		break;
	}
	case MCT_INT:
	case MCT_PX:
	case MCT_PERCENT:
	{
		Value = CurrTokenVal;
		Type = CurrToken;
		if (!GetNextToken())
			throw C4MCParserErr(this, C4MCErr_EOF);
		// range
		if (MCT_RANGE == CurrToken)
		{
			// Get the second value
			if (!GetNextToken())
				throw C4MCParserErr(this, C4MCErr_EOF);
			if (MCT_INT == CurrToken || MCT_PX == CurrToken || MCT_PERCENT == CurrToken)
			{
				Value += Random (CurrTokenVal - Value);
			}
			else
				throw C4MCParserErr(this, C4MCErr_FieldConstExp, CurrTokenIdtf);
			Type = CurrToken;
			if (!GetNextToken())
				throw C4MCParserErr(this, C4MCErr_EOF);
		}
		if (!pToNode->SetField(this, szFieldName, CurrTokenIdtf, Value, Type))
			// field not found
			throw C4MCParserErr(this, C4MCErr_Field404, szFieldName);
		break;
	}
	default:
	{
		throw C4MCParserErr(this, C4MCErr_FieldConstExp, CurrTokenIdtf);
	}
	}

	// now, the one and only thing to get is a semicolon
	if (CurrToken != MCT_SCOLON)
		throw C4MCParserErr(this, C4MCErr_SColonExp);


	/*
	// set field: accept integer constants and identifiers
	switch (CurrToken)
	  {
	  case MCT_IDTF:
	    // reset value field
	    CurrTokenVal=0;
	    // set field
	    if (!pToNode->SetField(this, szFieldName, CurrTokenIdtf, CurrTokenVal, CurrToken))
	      // field not found
	      throw C4MCParserErr(this, C4MCErr_Field404, szFieldName);
	    break;
	  case MCT_INT:
	    Value1 = CurrTokenVal;
	    while (GetNextToken ())
	      {
	      switch (CurrToken)
	        {
	        case MCT_SCOLON:
	          // set field
	          if (!pToNode->SetField(this, szFieldName, CurrTokenIdtf, Value1, MCT_INT))
	            // field not found
	            throw C4MCParserErr(this, C4MCErr_Field404, szFieldName);
	          return;
	          break;
	        case MCT_RANGE:
	          break;
	        case MCT_INT:
	          Value2 = CurrTokenVal;
	          Value1 += Random (Value2 - Value1);
	          break;
	        default:
	          throw C4MCParserErr(this, C4MCErr_SColonExp);
	          break;
	        }
	      }
	    break;
	  default:
	    throw C4MCParserErr(this, C4MCErr_FieldConstExp, CurrTokenIdtf);
	    break;
	  }
	// now, the one and only thing to get is a semicolon
	if (!GetNextToken())
	  throw C4MCParserErr(this, C4MCErr_EOF);
	if (CurrToken != MCT_SCOLON)
	  throw C4MCParserErr(this, C4MCErr_SColonExp);*/
}

void C4MCParser::ParseFile(const char *szFilename, C4Group *pGrp)
{
	size_t iSize; // file size

	// clear any old data
	Clear();
	// store filename
	SCopy(szFilename, Filename, C4MaxName);
	// check group
	if (!pGrp) throw C4MCParserErr(this, C4MCErr_NoGroup);
	// get file
	if (!pGrp->AccessEntry(szFilename, &iSize))
		// 404
		throw C4MCParserErr(this, C4MCErr_404);
	// file is empty?
	if (!iSize) return;
	// alloc mem
	Code = new char[iSize+1];
	// read file
	pGrp->Read((void *) Code, iSize);
	Code[iSize]=0;
	// parse it
	CPos=Code;
	ParseTo(MapCreator);
	if (0) PrintNodeTree(MapCreator, 0);
	// free code
	// on errors, this will be done be destructor
	Clear();
}

void C4MCParser::Parse(const char *szScript)
{
	// clear any old data
	Clear();
	// parse it
	CPos=szScript;
	ParseTo(MapCreator);
	if (0) PrintNodeTree(MapCreator, 0);
	// free code
	// on errors, this will be done be destructor
	Clear();

}


// algorithms ---------------------

// helper func
bool PreparePeek(C4MCOverlay **ppOvrl, int32_t &iX, int32_t &iY, C4MCOverlay **ppTopOvrl)
{
	// zoom out
	iX/=(*ppOvrl)->ZoomX; iY/=(*ppOvrl)->ZoomY;
	// get owning overlay
	C4MCOverlay *pOvrl2=(*ppOvrl)->OwnerOverlay();
	if (!pOvrl2) return false;
	// get uppermost overlay
	C4MCOverlay *pNextOvrl;
	for (*ppTopOvrl=pOvrl2; (pNextOvrl=(*ppTopOvrl)->OwnerOverlay()); *ppTopOvrl=pNextOvrl) {}
	// get first of operator-chain
	pOvrl2=pOvrl2->FirstOfChain();
	// set new overlay
	*ppOvrl=pOvrl2;
	// success
	return true;
}