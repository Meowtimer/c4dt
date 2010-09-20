package net.arctics.clonk.parser.c4script.ast.evaluate;

public interface IVariableValueProvider {
	Object getValueForVariable(String varName);
}