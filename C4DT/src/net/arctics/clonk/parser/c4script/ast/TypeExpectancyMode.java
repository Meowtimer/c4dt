package net.arctics.clonk.parser.c4script.ast;

import net.arctics.clonk.parser.c4script.ITypeable;

/**
 * Enum describing how strongly some {@link ITypeable} entity is expected to be of some type.
 * @author madeen
 *
 */
public enum TypeExpectancyMode {
	/**
	 * Expected but not required.
	 */
	Expect,
	/**
	 * Might be of that type, but not too sure.
	 */
	Hint,
	/**
	 * Force the type of the entity.
	 */
	Force
}