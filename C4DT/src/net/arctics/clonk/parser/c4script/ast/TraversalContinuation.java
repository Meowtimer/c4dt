package net.arctics.clonk.parser.c4script.ast;

public enum TraversalContinuation {
	Continue,
	TraverseSubElements,
	SkipSubElements,
	Cancel
}