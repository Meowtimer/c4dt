package net.arctics.clonk.parser.inireader;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.TYPE)
public @interface IniDefaultSection {
	public static final String DEFAULT = "Properties";
	String name();
}
