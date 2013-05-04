package net.arctics.clonk.ui.editors.c4script;

import net.arctics.clonk.Core;
import net.arctics.clonk.ast.ASTNode;
import net.arctics.clonk.c4script.Script;
import net.arctics.clonk.ui.editors.ClonkSourceViewerConfiguration;
import net.arctics.clonk.ui.editors.ClonkTextHover;
import net.arctics.clonk.util.StringUtil;
import net.arctics.clonk.util.Utilities;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IMarker;
import org.eclipse.core.resources.IResource;
import org.eclipse.jface.text.IRegion;
import org.eclipse.jface.text.ITextViewer;
import org.eclipse.jface.text.Region;

public class ScriptTextHover extends ClonkTextHover<C4ScriptEditor> {

	private EntityLocator entityLocator;

	public ScriptTextHover(ClonkSourceViewerConfiguration<C4ScriptEditor> clonkSourceViewerConfiguration) {
	    super(clonkSourceViewerConfiguration);
    }

	// some overriding necessary so hovers also work for declarations that can't be hyperlinked (engine declarations and such)

	@Override
	public String getHoverInfo(ITextViewer viewer, IRegion region) {
		final IFile scriptFile = Utilities.fileEditedBy(configuration.editor());
		final StringBuilder messageBuilder = new StringBuilder();
		if (entityLocator != null && entityLocator.entity() != null) {
			final ASTNode pred = entityLocator.expressionAtRegion() != null ? entityLocator.expressionAtRegion().predecessorInSequence() : null;
			final Script context = pred == null
				? configuration.editor().script()
				: configuration.editor().editingState().typingStrategy().localReporter
					(configuration.editor().script(), 0, null).typeOf(pred, Script.class);
			messageBuilder.append(entityLocator.entity().infoText(context));
		}
		else {
			final String superInfo = super.getHoverInfo(viewer, region);
			if (superInfo != null)
				messageBuilder.append(superInfo);
		}
		try {
			final IMarker[] markers = scriptFile.findMarkers(Core.MARKER_C4SCRIPT_ERROR, true, IResource.DEPTH_ONE);
			boolean foundSomeMarkers = false;
			for (final IMarker m : markers) {
				int charStart;
				final IRegion markerRegion = new Region(
					charStart = m.getAttribute(IMarker.CHAR_START, -1),
					m.getAttribute(IMarker.CHAR_END, -1)-charStart
				);
				if (Utilities.regionContainsOtherRegion(markerRegion, region)) {
					if (!foundSomeMarkers) {
						if (messageBuilder.length() > 0)
							messageBuilder.append("<br/><br/><b>"+Messages.C4ScriptTextHover_Markers1+"</b><br/>"); //$NON-NLS-1$
						foundSomeMarkers = true;
					}
					String msg = m.getAttribute(IMarker.MESSAGE).toString();
					msg = StringUtil.htmlerize(msg);
					messageBuilder.append(msg);
					messageBuilder.append("<br/>"); //$NON-NLS-1$
				}
			}
		} catch (final Exception e) {
			// whatever
		}
		return messageBuilder.toString();
	}

	@Override
	public IRegion getHoverRegion(ITextViewer viewer, int offset) {
		super.getHoverRegion(viewer, offset);
		final IRegion region = new Region(offset, 0);
		try {
			entityLocator = new EntityLocator(configuration.editor().script(), viewer.getDocument(), region);
		} catch (final Exception e) {
			e.printStackTrace();
			return null;
		}
		return region;
	}

}
