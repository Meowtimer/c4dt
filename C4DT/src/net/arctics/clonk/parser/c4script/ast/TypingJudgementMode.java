package net.arctics.clonk.parser.c4script.ast;

import net.arctics.clonk.parser.c4script.ITypeable;

/**
 * Enum describing how strongly some {@link ITypeable} entity is expected to be of some type.
 * @author madeen
 *
 */
public enum TypingJudgementMode {
	/**
	 * Force the type of the entity.
	 */
	OVERWRITE,
	/**
	 * Unify with existing typing judgement.
	 */
	UNIFY
}