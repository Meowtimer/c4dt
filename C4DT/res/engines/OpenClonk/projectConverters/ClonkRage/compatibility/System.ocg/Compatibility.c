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

static const METHOD_Classic = 1;
static const METHOD_JumpNRun = 2;