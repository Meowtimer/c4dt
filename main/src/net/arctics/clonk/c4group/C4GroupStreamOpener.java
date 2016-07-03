package net.arctics.clonk.c4group;

import java.net.URI;

import org.eclipse.core.filesystem.EFS;
import org.eclipse.core.filesystem.IFileStore;
import org.eclipse.core.resources.IContainer;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.IResourceDelta;
import org.eclipse.core.resources.IResourceDeltaVisitor;
import org.eclipse.core.resources.IResourceVisitor;
import org.eclipse.core.runtime.CoreException;

public class C4GroupStreamOpener implements IResourceDeltaVisitor, IResourceVisitor {
	public static final int OPEN = 0;
	public static final int CLOSE = 1;

	private final int operation;

	public C4GroupStreamOpener(final int operation) {
		this.operation = operation;
	}

	@Override
	public boolean visit(final IResourceDelta delta) throws CoreException {
		final IResource res = delta.getResource();
		return visit(res);
	}

	@Override
	public boolean visit(final IResource res) throws CoreException {
		if (res.getParent() != null && res.getParent().equals(res.getProject()) && res instanceof IContainer) {
			URI uri = null;
			try {
				uri = res.getLocationURI();
			} catch (final Exception e) {
				System.out.println(res.getFullPath().toString());
			}
			final IFileStore store = EFS.getStore(uri);
			if (store instanceof C4Group) {
				final C4Group group = (C4Group) store;
				try {
					switch (operation) {
					case OPEN:
						group.requireStream();
						break;
					case CLOSE:
						group.releaseStream();
						break;
					}
				} catch (final Exception e) {
					e.printStackTrace();
				}
			}
		}
		return res instanceof IProject;
	}
}