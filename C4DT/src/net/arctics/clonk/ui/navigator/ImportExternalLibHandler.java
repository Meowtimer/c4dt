package net.arctics.clonk.ui.navigator;

import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import net.arctics.clonk.resource.ClonkProjectNature;
import net.arctics.clonk.resource.ExternalLib;
import net.arctics.clonk.resource.c4group.C4GroupImporter;
import net.arctics.clonk.util.Utilities;

import org.eclipse.core.commands.AbstractHandler;
import org.eclipse.core.commands.ExecutionEvent;
import org.eclipse.core.commands.ExecutionException;
import org.eclipse.core.resources.IProject;
import org.eclipse.jface.dialogs.ProgressMonitorDialog;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.ITreeSelection;
import org.eclipse.ui.handlers.HandlerUtil;

public class ImportExternalLibHandler extends AbstractHandler {

	@Override
	public Object execute(ExecutionEvent event) throws ExecutionException {
		ISelection sel = HandlerUtil.getCurrentSelection(event);
		if (sel instanceof ITreeSelection) {
			ITreeSelection treeSel = (ITreeSelection) sel;
			Object[] objects = treeSel.toArray();
			Map<IProject, List<File>> libFiles = new HashMap<IProject, List<File>>();
			for (Object obj : objects) {
				if (obj instanceof ExternalLib) {
					IProject proj = (IProject) treeSel.getPathsFor(obj)[0].getFirstSegment();
					List<File> files = libFiles.get(proj);
					if (files == null)
						libFiles.put(proj, files = new ArrayList<File>(3));
					files.add(((ExternalLib) obj).getFile());
					ClonkProjectNature.get(proj).getIndex().removeExternalLib((ExternalLib) obj);
				}
			}
			for (Entry<IProject, List<File>> entry : libFiles.entrySet()) {
				List<File> files = entry.getValue();
				C4GroupImporter importer = new C4GroupImporter(files.toArray(new File[files.size()]), entry.getKey());
				final ProgressMonitorDialog progressDialog = new ProgressMonitorDialog(HandlerUtil.getActiveWorkbenchWindow(event).getShell());
				try {
					progressDialog.run(false, true, importer);
				} catch (Exception e) {
					e.printStackTrace();
					continue;
				}
				ClonkProjectNature.get(entry.getKey()).getIndex().refreshCache();
				Utilities.getProjectExplorer().getCommonViewer().refresh(entry.getKey());
			}
		}
		return null;
	}

}
