package net.arctics.clonk.ui.editors.ini;

import net.arctics.clonk.util.IHasKeyAndValue;

import org.eclipse.jface.viewers.ColumnLabelProvider;
import org.eclipse.swt.graphics.Image;

public class IniEditorColumnLabelProvider extends ColumnLabelProvider {

	public enum WhatLabel {
		Key,
		Value
	}
	
	private WhatLabel whatLabel;
	
	public IniEditorColumnLabelProvider(WhatLabel whatLabel) {
		super();
		this.whatLabel = whatLabel;
	}

	public Image getImage(Object element) {
		return null;
	}

	@SuppressWarnings("unchecked")
	public String getText(Object element) {
		IHasKeyAndValue<String, String> keyVal = (IHasKeyAndValue<String, String>) element;
		switch (whatLabel) {
		case Key:
			return keyVal.getKey();
		case Value:
			return keyVal.getValue();
		}
		return "Unknown";
	}

}
