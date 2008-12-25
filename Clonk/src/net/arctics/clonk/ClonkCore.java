package net.arctics.clonk;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.EOFException;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import net.arctics.clonk.parser.C4Field;
import net.arctics.clonk.parser.C4Function;
import net.arctics.clonk.parser.C4ID;
import net.arctics.clonk.parser.C4ObjectExtern;
import net.arctics.clonk.parser.C4Variable;
import net.arctics.clonk.parser.ClonkIndex;
import net.arctics.clonk.resource.InputStreamRespectingUniqueIDs;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.QualifiedName;
import org.eclipse.jface.resource.ImageDescriptor;
import org.eclipse.ui.plugin.AbstractUIPlugin;
import org.osgi.framework.BundleContext;

/**
 * The activator class controls the plug-in life cycle
 */
public class ClonkCore extends AbstractUIPlugin {

	// The plug-in ID
	public static final String PLUGIN_ID = "net.arctics.clonk";
	public static final String CLONK_NATURE_ID = "net.arctics.clonk.clonknature";
	public static final String MARKER_EXTERN_LIB_ERROR = PLUGIN_ID + ".externliberror";
	public static final QualifiedName FOLDER_C4ID_PROPERTY_ID = new QualifiedName("net.arctics.clonk","c4id");
	public static final QualifiedName C4OBJECT_PROPERTY_ID = new QualifiedName("net.arctics.clonk","c4object");
	public static final QualifiedName SCRIPT_PROPERTY_ID = new QualifiedName("net.arctics.clonk", "script");
	
	public static final C4ObjectExtern ENGINE_OBJECT = new C4ObjectExtern(C4ID.getSpecialID("Engine"),"Engine",null);
	public static ClonkIndex EXTERN_INDEX;
	
	// The shared instance
	private static ClonkCore plugin;
	
	/**
	 * The constructor
	 * @throws IOException 
	 */
	public ClonkCore() {
	}
	
	/*
	 * (non-Javadoc)
	 * @see org.eclipse.ui.plugin.AbstractUIPlugin#start(org.osgi.framework.BundleContext)
	 */
	public void start(BundleContext context) throws Exception {
		super.start(context);
		plugin = this;
		
		loadEngineObject();
		loadExternIndex();
		
	}

	private void loadEngineObject() throws FileNotFoundException, IOException,
			ClassNotFoundException {
		InputStream engineStream;
		if (getEngineCacheFile().toFile().exists()) {
			engineStream = new FileInputStream(getEngineCacheFile().toFile());
		}
		else {
			engineStream = getBundle().getEntry("res/engine").openStream();
		}
		ObjectInputStream decoder = new ObjectInputStream(new BufferedInputStream(engineStream));
//			java.beans.XMLDecoder decoder = new XMLDecoder(new BufferedInputStream(engineIndex.openStream()));
		try {
			while (true) {
				Object obj = decoder.readObject();
				if (obj instanceof C4Field) {
					C4Field field = (C4Field)obj;
					field.setScript(ENGINE_OBJECT);
					ENGINE_OBJECT.addField(field);
//						ENGINE_FUNCTIONS.add((C4Function)obj);
				}
				else {
					System.out.println("Read unknown object from engine: " + obj.getClass().getName());
				}
			}
		}
		catch(EOFException e) {
			// finished
		}
	}
	
	public static IPath getEngineCacheFile() {
		IPath path = ClonkCore.getDefault().getStateLocation();
		return path.append("engine");
	}
	
	public static IPath getExternLibCacheFile() {
		IPath path = ClonkCore.getDefault().getStateLocation();
		return path.append("externlib");
	}
	
	@SuppressWarnings("deprecation")
	public static void saveEngineObject() {
		try {
			IPath engine = getEngineCacheFile();
			
			if (engine.toFile().exists()) engine.toFile().delete();
			
			FileOutputStream outputStream = new FileOutputStream(engine.toFile());
			
			ObjectOutputStream encoder = new ObjectOutputStream(new BufferedOutputStream(outputStream));
			for(C4Variable var : ENGINE_OBJECT.getDefinedVariables()) {
				encoder.writeObject(var);
			}
			for(C4Function func : ENGINE_OBJECT.getDefinedFunctions()) {
				encoder.writeObject(func);
			}
			encoder.flush();
			encoder.close();
		} catch (FileNotFoundException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		}
		
	}
	
	public static void saveExternIndex(IProgressMonitor monitor) throws FileNotFoundException {
		if (monitor != null) monitor.beginTask("Saving libs", 1);
		final File index = getExternLibCacheFile().toFile();
		FileOutputStream out = new FileOutputStream(index);
		
		try {
			ObjectOutputStream objStream = new ObjectOutputStream(out);
			objStream.writeObject(EXTERN_INDEX);
			objStream.close();
			if (monitor != null) monitor.worked(1);
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		if (monitor != null) monitor.done();
	}
	
	public static void loadExternIndex() {
		final File index = getExternLibCacheFile().toFile();
		if (!index.exists()) {
			EXTERN_INDEX = new ClonkIndex();
		} else {
			try {
				FileInputStream in = new FileInputStream(index);
				ObjectInputStream objStream = new InputStreamRespectingUniqueIDs(in);
				EXTERN_INDEX = (ClonkIndex)objStream.readObject();
			} catch (Exception e) {
				EXTERN_INDEX = new ClonkIndex();
			}
		}
	}

	/*
	 * (non-Javadoc)
	 * @see org.eclipse.ui.plugin.AbstractUIPlugin#stop(org.osgi.framework.BundleContext)
	 */
	public void stop(BundleContext context) throws Exception {
		//saveExternIndex(); clean build causes save
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
