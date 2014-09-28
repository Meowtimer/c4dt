package net.arctics.clonk.util;

import java.io.InputStream;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.Writer;
import java.lang.reflect.Field;

import net.arctics.clonk.ast.AppendableBackedNodePrinter;
import net.arctics.clonk.ini.CustomIniUnit;

public abstract class SettingsBase implements Cloneable {
	public SettingsBase() {
		try {
			for (final Field f : getClass().getFields())
				if (f.getType() == String.class)
					f.set(this, "");
		} catch (final Exception e) {
			e.printStackTrace();
		}
	}
	@Override
	public boolean equals(final Object obj) {
		if (obj == this)
			return true;
		if (obj.getClass() != this.getClass())
			return false;
		for (final Field f : getClass().getFields())
			try {
				final Object fVal = f.get(this);
				final Object objVal = f.get(obj);
				if (!Utilities.eq(fVal, objVal))
					return false;
			} catch (final Exception e) {
				e.printStackTrace();
				return false;
			}
		return true;
	}
	public void loadFrom(final InputStream stream) {
		try {
			CustomIniUnit.load(stream, this);
		} catch (final Exception e) {
			e.printStackTrace();
		}
	}
	public void saveTo(final OutputStream stream, final SettingsBase defaults) {
		try {
			try (Writer writer = new OutputStreamWriter(stream)) {
				CustomIniUnit.save(new AppendableBackedNodePrinter(writer), this, defaults);
			}
		} catch (final Exception e) {
			e.printStackTrace();
		}
	}
	public static <T extends SettingsBase> T createFrom(final Class<T> cls, final InputStream stream) {
		try {
			final T settings = cls.newInstance();
			settings.loadFrom(stream);
			return settings;
		} catch (final Exception e) {
			return null;
		}
	}
	@Override
	public SettingsBase clone() {
		try {
			return (SettingsBase)super.clone();
		} catch (final CloneNotSupportedException e) {
			return null;
		}
	}
}