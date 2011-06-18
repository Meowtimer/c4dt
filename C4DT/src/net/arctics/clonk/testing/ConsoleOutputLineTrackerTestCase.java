package net.arctics.clonk.testing;

import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;

import net.arctics.clonk.debug.ConsoleOutputLineTracker;

import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.debug.core.model.IProcess;
import org.eclipse.debug.core.model.IStreamMonitor;
import org.eclipse.debug.core.model.IStreamsProxy;
import org.eclipse.debug.ui.console.IConsole;
import org.eclipse.debug.ui.console.IConsoleHyperlink;
import org.eclipse.jface.text.IDocument;
import org.eclipse.jface.text.IRegion;
import org.eclipse.jface.text.Region;
import org.eclipse.ui.console.IHyperlink;
import org.eclipse.ui.console.IOConsoleOutputStream;
import org.eclipse.ui.console.IPatternMatchListener;
import org.junit.Test;
import static org.junit.Assert.*;

@SuppressWarnings("deprecation")
public class ConsoleOutputLineTrackerTestCase {
	private final class TestConsole implements IConsole {
		
		public class Link {
			public int offset;
			public int length;
			public IHyperlink hyperlink;
		}
		
		private List<Link> links = new ArrayList<Link>();
		
		@Override
		public void removePatternMatchListener(IPatternMatchListener matchListener) {
			// TODO Auto-generated method stub
			
		}

		@Override
		public IOConsoleOutputStream getStream(String streamIdentifier) {
			// TODO Auto-generated method stub
			return null;
		}

		@Override
		public IRegion getRegion(IHyperlink link) {
			// TODO Auto-generated method stub
			return null;
		}

		@Override
		public IRegion getRegion(IConsoleHyperlink link) {
			// TODO Auto-generated method stub
			return null;
		}

		@Override
		public IProcess getProcess() {
			// TODO Auto-generated method stub
			return null;
		}

		@Override
		public IDocument getDocument() {
			// TODO Auto-generated method stub
			return null;
		}

		@Override
		public void connect(IStreamMonitor streamMonitor, String streamIdentifer) {
			// TODO Auto-generated method stub
			
		}

		@Override
		public void connect(IStreamsProxy streamsProxy) {
			// TODO Auto-generated method stub
			
		}

		@Override
		public void addPatternMatchListener(IPatternMatchListener matchListener) {
			// TODO Auto-generated method stub
			
		}

		@Override
		public void addLink(IHyperlink hyperlink, int offset, int length) {
			Link link = new Link();
			link.offset = offset;
			link.length = length;
			link.hyperlink = hyperlink;
			links.add(link);
		}

		@Override
		public void addLink(IConsoleHyperlink link, int offset, int length) {
			// TODO Auto-generated method stub
			
		}
	}

	@Test
	public void testLinkCreation() {
		String line = "Error in Objects.ocd/Clonk.ocd/Script.c:123 Objects.ocd/Clonk.ocd :123123 :::";
		List<IResource> r = new LinkedList<IResource>();
		r.add(ResourcesPlugin.getWorkspace().getRoot().getProject("OpenClonk").findMember("Objects.ocd"));
		TestConsole console = new TestConsole();
		ConsoleOutputLineTracker.createResourceLinksInLine(line, r, console, new Region(0, 0));
		assertTrue(console.links.size() == 1 && console.links.get(0).offset == line.indexOf("Objects") && console.links.get(0).length == line.indexOf("123")+3 - line.indexOf("Objects"));
	}
}
