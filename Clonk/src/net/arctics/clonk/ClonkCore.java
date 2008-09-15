package net.arctics.clonk;

import java.beans.XMLDecoder;
import java.io.BufferedInputStream;
import java.io.ByteArrayInputStream;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;

import net.arctics.clonk.parser.C4Function;
import net.arctics.clonk.parser.C4Object;
import net.arctics.clonk.parser.C4Variable;

import org.eclipse.jface.resource.ImageDescriptor;
import org.eclipse.ui.plugin.AbstractUIPlugin;
import org.osgi.framework.BundleContext;

/**
 * The activator class controls the plug-in life cycle
 */
public class ClonkCore extends AbstractUIPlugin {

	// The plug-in ID
	public static final String PLUGIN_ID = "net.arctics.clonk";
	
	public static final List<C4Function> ENGINE_FUNCTIONS = new ArrayList<C4Function>(505);
	
	// The shared instance
	private static ClonkCore plugin;
	
	private List<C4Object> objects;
	
	/**
	 * The constructor
	 */
	public ClonkCore() {
		objects = new ArrayList<C4Object>();
	}

	public void addToIndex(C4Object object) {
		objects.add(object);
	}
	
	public void removeFromIndex(C4Object object) {
		objects.remove(object);
	}
	
	/*
	 * (non-Javadoc)
	 * @see org.eclipse.ui.plugin.AbstractUIPlugin#start(org.osgi.framework.BundleContext)
	 */
	public void start(BundleContext context) throws Exception {
		super.start(context);
		plugin = this;
		
		URL engineIndex = getBundle().getEntry("res/engine.xml");
		if (engineIndex != null) {
			java.beans.XMLDecoder decoder = new XMLDecoder(new BufferedInputStream(engineIndex.openStream()));
			try {
				while (true) {
					Object obj = decoder.readObject();
					if (obj instanceof C4Function) {
						ENGINE_FUNCTIONS.add((C4Function)obj);
					}
					else {
						System.out.println("Read unknown object from engine.xml: " + obj.getClass().getName());
					}
				}
			}
			catch(ArrayIndexOutOfBoundsException e) {
				// finished
			}
		}
		
		
//		FileInputStream file = new FileInputStream("javadata.txt");
//		java.beans.XMLEncoder encoder = new XMLEncoder(new BufferedOutputStream(new FileOutputStream("engine.xml")));
//		C4Function func = null;
//		while((func = parseFunction(file)) != null) {
//			encoder.writeObject(func);
//		}
//		encoder.flush();
//		encoder.close();
	}
	
	private C4Function parseFunction(InputStream stream) throws IOException {
		if (stream.read() == 'F') {
			char[] buffer = new char[256];
			String funcName = null, returnType = null, desc = null;
			List<C4Variable> vars = new ArrayList<C4Variable>(5);
			int i = 0;
			int read = 0;
			while((read = stream.read()) >= 0) {
				if (read == '(') {
					funcName = new String(buffer,0,i);
					break;
				}
				else buffer[i++] = (char)read;
			}
			do {
				C4Variable var;
				var = parseParameter(stream);
				if(var != null) vars.add(var);
				else break;
			} while(true);
			
//			if (stream.read() != ')') System.out.println("There should be a ) right now");
			i = 0;
			read = 0;
			while((read = stream.read()) >= 0) {
				if (read == ':') {
					returnType = new String(buffer,0,i);
					break;
				}
				else buffer[i++] = (char)read;
			}
			buffer = new char[4096];
			i = 0;
			read = 0;
			while((read = stream.read()) >= 0) {
				if (read == ';') {
					int c2 = stream.read();
					int c3 = stream.read();
					if (c2 == ';' && c3 == ';') {
						desc = new String(buffer,0,i);
						break;
					}
					else {
						buffer[i++] = (char)read;
						buffer[i++] = (char)c2;
						buffer[i++] = (char)c3;
					}
				}
				else buffer[i++] = (char)read;
			}
			
			return new C4Function(funcName,returnType,desc,vars.toArray(new C4Variable[] { }));
		}
		else {
			return null;
		}
	}
	
	private C4Variable parseParameter(InputStream stream) throws IOException {
		if (stream.read() == 'P') {
			char[] buffer = new char[256];
			String varType = null, parName = null, desc = null;;
			int i = 0;
			int read = 0;
			while((read = stream.read()) >= 0) {
				if (read == ' ') {
					varType = new String(buffer,0,i);
					break;
				}
				else buffer[i++] = (char)read;
			}
			i = 0;
			read = 0;
			while((read = stream.read()) >= 0) {
				if (read == ':') {
					parName = new String(buffer,0,i);
					break;
				}
				else buffer[i++] = (char)read;
			}
			
			buffer = new char[4096];
			i = 0;
			read = 0;
			while((read = stream.read()) >= 0) {
				if (read == ';') {
					int c2 = stream.read();
					int c3 = stream.read();
					if (c2 == ';' && c3 == ';') {
						desc = new String(buffer,0,i);
						break;
					}
					else {
						buffer[i++] = (char)read;
						buffer[i++] = (char)c2;
						buffer[i++] = (char)c3;
					}
				}
				else buffer[i++] = (char)read;
			}
			
			return new C4Variable(parName,varType,desc);
		}
		else {
			return null;
		}
	}

	/*
	 * (non-Javadoc)
	 * @see org.eclipse.ui.plugin.AbstractUIPlugin#stop(org.osgi.framework.BundleContext)
	 */
	public void stop(BundleContext context) throws Exception {
		plugin = null;
		super.stop(context);
	}

	/**
	 * Returns the shared instance
	 *
	 * @return the shared instance
	 */
	public static ClonkCore getDefault() {
		return plugin;
	}

	/**
	 * Returns an image descriptor for the image file at the given
	 * plug-in relative path
	 *
	 * @param path the path
	 * @return the image descriptor
	 */
	public static ImageDescriptor getImageDescriptor(String path) {
		return imageDescriptorFromPlugin(PLUGIN_ID, path);
	}
}
