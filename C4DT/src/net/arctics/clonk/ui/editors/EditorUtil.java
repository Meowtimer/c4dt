package net.arctics.clonk.ui.editors;

import java.util.Iterator;

import net.arctics.clonk.ClonkCore;
import net.arctics.clonk.util.Utilities;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.ui.IEditorPart;
import org.eclipse.ui.IEditorReference;
import org.eclipse.ui.PlatformUI;

public class EditorUtil {
	public static Iterable<IEditorPart> editorPartsToBeSaved() {
		return new Iterable<IEditorPart>() {
			@Override
			public Iterator<IEditorPart> iterator() {
				return new Iterator<IEditorPart>() {

					private int index = -1;
					private IEditorReference[] refs = PlatformUI.getWorkbench().getActiveWorkbenchWindow().getActivePage().getEditorReferences();
					private IEditorPart next;
					
					@Override
					public boolean hasNext() {
						if (next == null) {
							for (int i = index+1; i < refs.length; i++) {
								IEditorPart part = refs[i].getEditor(true);
								IFile file = Utilities.fileBeingEditedBy(part);
								try {
									if (file != null && file.getProject().hasNature(ClonkCore.CLONK_NATURE_ID)) {
										if (part.isDirty()) {
											next = part;
											index = i;
											break;
										}
									}
								} catch (CoreException e) {
									continue;
								}
							}
						}
						return next != null;
					}

					@Override
					public IEditorPart next() {
						IEditorPart result = next;
						next = null;
						return result;
					}

					@Override
					public void remove() {
						// nope
					}
				};
			}
		};
	}
}
