package net.arctics.clonk.preferences;

import org.eclipse.jface.preference.IPreferenceStore;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.ui.ide.IDEEncoding;
import org.eclipse.ui.ide.dialogs.AbstractEncodingFieldEditor;
import org.eclipse.ui.ide.dialogs.EncodingFieldEditor;

/**
 * Class copypasta'd from {@link EncodingFieldEditor}, adjusted to be more forgiving when used in the context of Clonk preference pages.
 * @author madeen
 *
 */
public class ExceptionlessEncodingFieldEditor extends AbstractEncodingFieldEditor {
	
	/**
	 * Creates a new encoding field editor with the given preference name, label
	 * and parent.
	 * 
	 * @param name
	 *            the name of the preference this field editor works on
	 * @param labelText
	 *            the label text of the field editor
	 * @param groupTitle
	 *            the title for the field editor's control. If groupTitle is
	 *            <code>null</code> the control will be unlabelled
	 *            (by default a {@link Composite} instead of a {@link Group}.
	 * @param parent
	 *            the parent of the field editor's control
	 * @see AbstractEncodingFieldEditor#setGroupTitle(String)
	 * @since 3.3
	 */
	public ExceptionlessEncodingFieldEditor(final String name, final String labelText,
			final String groupTitle, final Composite parent) {
		super();
		init(name, labelText);
		setGroupTitle(groupTitle);
		createControl(parent);
	}
	/**
	 * Create a new instance of the receiver on the preference called name
	 * with a label of labelText.
	 * @param name
	 * @param labelText
	 * @param parent
	 */
	public ExceptionlessEncodingFieldEditor(final String name, final String labelText, final Composite parent) {
		super();
		init(name, labelText);
		createControl(parent);
	}
	/* (non-Javadoc)
	 * @see org.eclipse.ui.internal.ide.dialogs.AbstractEncodingFieldEditor#getStoredValue()
	 */
	@Override
	protected String getStoredValue() {
		final String val = getPreferenceStore().getString(getPreferenceName());
		// return null if equal to default so AbstractEncodingFieldEditor will reflect that
		if (val.equals(getPreferenceStore().getDefaultString(getPreferenceName())))
			return null;
		else
			return val;
	}
	
	/* (non-Javadoc)
	 * @see org.eclipse.jface.preference.FieldEditor#doStore()
	 */
	@Override
	protected void doStore() {
		final String encoding = getSelectedEncoding();
		
		if(hasSameEncoding(encoding))
			return;
		
		IDEEncoding.addIDEEncoding(encoding);
		
		if (encoding.equals(getDefaultEnc()))
			getPreferenceStore().setToDefault(getPreferenceName());
		else
			getPreferenceStore().setValue(getPreferenceName(), encoding);
	}
	
	@Override
	public void setPreferenceStore(final IPreferenceStore store) {
		if (store != null)
			super.setPreferenceStore(store);
	}

}
