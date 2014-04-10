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