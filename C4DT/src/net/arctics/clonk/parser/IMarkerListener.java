package net.arctics.clonk.parser;

import net.arctics.clonk.parser.c4script.C4ScriptParser;

/**
 * A listener that will be notified if a marker is about to be created.
 * @author madeen
 *
 */
public interface IMarkerListener {
	/**
	 * Result enum for {@link IMarkerListener#markerEncountered(C4ScriptParser, Problem, int, int, int, int, Object...)}
	 * @author madeen
	 *
	 */
	public enum Decision {
		/**
		 * Don't create the marker, the accused is innocent
		 */
		DropCharges,
		/**
		 * Continue creating the marker
		 */
		PassThrough
	}
	/**
	 * Called when a marker is about to be created. The listener gets a chance to do its own processing and possibly order the calling parser to forego creating the actual marker regularly.
	 * @param positionProvider Position provider providing positions
	 * @param code the parser error code
	 * @param node 
	 * @param markerStart start of the marker region
	 * @param markerEnd end of the marker region
	 * @param flags true if the marker wouldn't cause an exception in the parsing process
	 * @param severity IMarker severity value
	 * @param args Arguments used to construct the marker message
	 * @return Returning WhatToDo.DropCharges causes the parser to not create the marker.
	 */
	Decision markerEncountered(Markers markers, IASTPositionProvider positionProvider, Problem code, ASTNode node, int markerStart, int markerEnd, int flags, int severity, Object... args);
}