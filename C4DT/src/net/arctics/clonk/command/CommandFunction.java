package net.arctics.clonk.command;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;

/**
 * Attribute marking functions that are to be exported to the macro engine
 * @author madeen
 *
 */
@Retention(RetentionPolicy.RUNTIME)
public @interface CommandFunction {}