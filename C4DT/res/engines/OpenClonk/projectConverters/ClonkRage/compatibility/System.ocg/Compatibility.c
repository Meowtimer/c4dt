global func NoOp(what)
{
	Log("NoOp: %s", what);
	return true;
}

global func EvaluateID(string name)
{
	return nil; // nope
}

global func UnknownFunction(string name)
{
	Log("Unknown function called: %s", name);
}

global func GetPhysical()
{
	return 0;
}

global func SetPhysical()
{
	return false;
}

global func GetPlrMagic()
{
	return false;
}

global func ResetPhysical()
{
	return false;
}

global func CastParticles()
{
	
}

global func ShakeObjects()
{
	
}

global func LaunchLightning()
{
	
}

global func ScoreboardCol()
{
	
}

static const METHOD_Classic = 1;
static const METHOD_JumpAndRun = 2;
static const METHOD_None = 3;

static const OCF_Chop = 256;
static const OCF_Living = OCF_Alive;

global func Bubble(int amount, int x, int y)
{
	if (amount==nil || amount==0) 
		amount=3;

	for (var i = 0; i < amount; i++)
	{
		var bubble = CreateObject(Bubble_, x, y, NO_OWNER);
		if (bubble) bubble.creator = this;
	}
	return;
}

global func GetPlrDownDouble() { return false; }
global func GetPlrJumpAndRunControl() { return false; }
global func Incinerate() { return false; }
global func SetObjectOrder() { return false; }
global func SetPlrMagic() { return false; }
global func GetActMapVal() { return nil; }

global func BlastObject()
{
}

global func ChangeEffect()
{
}

global func CheckEnergyNeedChain()
{
}

global func ClearLastPlrCom()
{
}

global func Extinguish()
{
}

global func ExtractLiquid()
{
}

global func FindBase()
{
}

global func FlameConsumeMaterial()
{
}

global func GetActMapVal()
{
}

global func GetBase()
{
}

global func GetCaptain()
{
}

global func GetMaterialColor()
{
}

global func GetPlrDownDouble()
{
}

global func GetPlrJumpAndRunControl()
{
}

global func GetPortrait()
{
}

global func GetTaggedPlayerName()
{
}

global func Incinerate()
{
}

global func NoContainer()
{
}

global func OnFire()
{
}

global func ResortObjects()
{
}

global func ScriptGo()
{
}

global func SelectCrew()
{
}

global func SetLandscapePixel()
{
}

global func SetObjectOrder()
{
}

global func SetPortrait()
{
}

global func SetVisibility()
{
}

global func Smoke()
{
}

global func Split2Components()
{
}

global func Value()
{
}

global func goto()
{
}