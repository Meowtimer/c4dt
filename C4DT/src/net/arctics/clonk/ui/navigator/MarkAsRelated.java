package net.arctics.clonk.ui.navigator;

import java.util.ArrayList;
import java.util.List;

import net.arctics.clonk.ClonkCore;
import net.arctics.clonk.resource.ClonkProjectNature;

import org.eclipse.core.commands.ExecutionEvent;
import org.eclipse.core.commands.ExecutionException;
import org.eclipse.core.resources.IContainer;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.QualifiedName;
import org.eclipse.jface.resource.StringConverter;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.swt.graphics.RGB;
import org.eclipse.swt.widgets.ColorDialog;
import org.eclipse.ui.handlers.HandlerUtil;

public class MarkAsRelated extends ClonkResourceHandler {
	
	public static final QualifiedName RELATED_FOLDERS_TAG = new QualifiedName(ClonkCore.PLUGIN_ID, "relatedFoldersTag");
	public static final QualifiedName RELATED_FOLDERS_COLOR = new QualifiedName(ClonkCore.PLUGIN_ID, "relatedFoldersColor");

	@Override
	public Object execute(ExecutionEvent event) throws ExecutionException {
		ISelection selection = HandlerUtil.getCurrentSelection(event);
		if (selection instanceof IStructuredSelection) {
			IStructuredSelection structuredSelection = (IStructuredSelection) selection;
			List<IContainer> clonkProjectFolders = new ArrayList<IContainer>(structuredSelection.size());
			StringBuilder commonTag = new StringBuilder();
			for (Object obj : structuredSelection.toArray()) {
				if (obj instanceof IContainer && ClonkProjectNature.get((IContainer)obj) != null) {
					clonkProjectFolders.add((IContainer)obj);
					commonTag.append(((IContainer)obj).getProjectRelativePath().toOSString());
					commonTag.append(" ");
				}
			}
			String commonTagString = commonTag.toString();
			for (IContainer c : clonkProjectFolders)
				try {
					c.setPersistentProperty(RELATED_FOLDERS_TAG, commonTagString);
				} catch (CoreException e) {
					e.printStackTrace();
				}
			ColorDialog colorDialog = new ColorDialog(HandlerUtil.getActiveShell(event));
			colorDialog.setText("Select a color for this set of folders (You don't have to, press Cancel in that case");
			RGB rgb = colorDialog.open();
			if (rgb != null) {
				String rgbString = StringConverter.asString(rgb);
				for (IContainer c : clonkProjectFolders)
					try {
						c.setPersistentProperty(RELATED_FOLDERS_COLOR, rgbString);
					} catch (CoreException e) {
						e.printStackTrace();
					}
			}
		}
		return null;
	}

}
