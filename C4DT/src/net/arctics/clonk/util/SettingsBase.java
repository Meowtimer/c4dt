package net.arctics.clonk.util;

import java.io.InputStream;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.Writer;
import java.lang.reflect.Field;

import net.arctics.clonk.c4script.ast.AppendableBackedExprWriter;
import net.arctics.clonk.parser.inireader.CustomIniUnit;

public abstract class SettingsBase implements Cloneable {		

	public SettingsBase() {
		try {
			for (Field f : getClass().getFields())
				if (f.getType() == String.class)
					f.set(this, "");
		} catch (Exception e) {
			e.printStackTrace();
		}
	}
	@Override
	public boolean equals(Object obj) {
		if (obj == this)
			return true;
		if (obj.getClass() != this.getClass())
			return false;
		for (Field f : getClass().getFields())
			try {
				Object fVal = f.get(this);
				Object objVal = f.get(obj);
				if (!Utilities.eq(fVal, objVal))
					return false;
			} catch (Exception e) {
				e.printStackTrace();
				return false;
			}
		return true;
	}
	public void loadFrom(InputStream stream) {
		try {
			CustomIniUnit.load(stream, this);
		} catch (Exception e) {
			e.printStackTrace();
		}
	}
	public void saveTo(OutputStream stream, SettingsBase defaults) {
		try {
			try (Writer writer = new OutputStreamWriter(stream)) {
				CustomIniUnit.save(new AppendableBackedExprWriter(writer), this, defaults);
			}
		} catch (Exception e) {
			e.printStackTrace(); 
		}
	}
	public static <T extends SettingsBase> T createFrom(Class<T> cls, InputStream stream) {
		try {
			T settings = cls.newInstance();
			settings.loadFrom(stream);
			return settings;
		} catch (Exception e) {
			return null;
		}
	}
	@Override
	public SettingsBase clone() {
		try {
			return (SettingsBase)super.clone();
		} catch (CloneNotSupportedException e) {
			return null;
		}
	}
}