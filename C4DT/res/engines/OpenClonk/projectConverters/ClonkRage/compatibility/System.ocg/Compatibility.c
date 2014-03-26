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