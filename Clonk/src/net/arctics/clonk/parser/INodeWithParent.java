package net.arctics.clonk.parser;

public interface INodeWithParent {
	String nodeName();
	INodeWithParent parentNode();
}
