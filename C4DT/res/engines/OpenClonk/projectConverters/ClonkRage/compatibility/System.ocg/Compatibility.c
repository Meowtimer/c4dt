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