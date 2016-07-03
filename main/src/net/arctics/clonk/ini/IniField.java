/**
 * 
 */
package net.arctics.clonk.ini;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;

@Retention(RetentionPolicy.RUNTIME)
public @interface IniField {
	public String category() default "";
}