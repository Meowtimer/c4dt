package net.arctics.clonk;

import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import net.arctics.clonk.parser.C4ID;
import net.arctics.clonk.parser.C4ObjectExtern;
import net.arctics.clonk.parser.ClonkIndex;
import net.arctics.clonk.parser.inireader.IniData;
import net.arctics.clonk.resource.ClonkLibBuilder;
import net.arctics.clonk.resource.InputStreamRespectingUniqueIDs;

import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.QualifiedName;
import org.eclipse.jface.resource.ImageDescriptor;
import org.eclipse.jface.resource.ImageRegistry;
import org.eclipse.swt.graphics.Image;
import org.eclipse.ui.plugin.AbstractUIPlugin;
import org.osgi.framework.BundleContext;

/**
 * The activator class controls the plug-in life cycle
 */
public class ClonkCore extends AbstractUIPlugin {

	// The plug-in ID
	public static final String PLUGIN_ID = "net.arctics.clonk";
	public static final String CLONK_NATURE_ID = PLUGIN_ID + ".clonknature";
	public static final String MARKER_EXTERN_LIB_ERROR = PLUGIN_ID + ".externliberror";
	public static final QualifiedName FOLDER_C4ID_PROPERTY_ID = new QualifiedName(PLUGIN_ID,"c4id");
	public static final QualifiedName C4OBJECT_PROPERTY_ID = new QualifiedName(PLUGIN_ID,"c4object");
	public static final QualifiedName SCRIPT_PROPERTY_ID = new QualifiedName(PLUGIN_ID, "script");
	
	public C4ObjectExtern ENGINE_OBJECT;
	public ClonkIndex EXTERN_INDEX;
	public IniData INI_CONFIGURATIONS;
	
	// The shared instance
	private static ClonkCore plugin;
	
	private ClonkLibBuilder libBuilder = null;
	
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
		
		loadIniConfigurations();
		
		loadEngineObject();
		loadExternIndex();
	}
	
	private void loadIniConfigurations() {
		try {
			IniData data = new IniData(getBundle().getEntry("res/iniconfig.xml").openStream());
			data.parse();
			INI_CONFIGURATIONS = data; // set this variable when parsing completed successfully
		} catch (FileNotFoundException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

//	private void loadOld(String path) throws IOException, ClassNotFoundException {
//		ObjectInputStream decoder = new ObjectInputStream(new BufferedInputStream(new FileInputStream(new File(path))));
//		//		java.beans.XMLDecoder decoder = new XMLDecoder(new BufferedInputStream(engineIndex.openStream()));
//		try {
//			while (true) {
//				Object obj = decoder.readObject();
//				if (obj instanceof C4Field) {
//					C4Field field = (C4Field)obj;
//					field.setScript(ENGINE_OBJECT);
//					ENGINE_OBJECT.addField(field);
//					//					ENGINE_FUNCTIONS.add((C4Function)obj);
//				}
//				else {
//					System.out.println("Read unknown object from engine: " + obj.getClass().getName());
//				}
//			}
//		}
//		catch(EOFException e) {
//			// finished
//		}
//	}

	private void loadEngineObject() throws FileNotFoundException, IOException, ClassNotFoundException {
		InputStream engineStream;
		if (getEngineCacheFile().toFile().exists()) {
			engineStream = new FileInputStream(getEngineCacheFile().toFile());
		}
		else {
			engineStream = getBundle().getEntry("res/engine").openStream();
		}
		try {
			try {
				ObjectInputStream objStream = new InputStreamRespectingUniqueIDs(engineStream);
				ENGINE_OBJECT = (C4ObjectExtern)objStream.readObject();
			} catch (Exception e) {
				e.printStackTrace();
				ENGINE_OBJECT = new C4ObjectExtern(C4ID.getSpecialID("Engine"),"Engine",null, null);
			}
		} finally {
			engineStream.close();
		}
	}
	
//	private int nooper;
//	
//	private void addFunction(String name, C4Type retType, C4Type... parmTypes) {
//		C4Variable[] parms = new C4Variable[parmTypes.length];
//		for (int i = 0; i < parms.length; i++)
//			parms[i] = new C4Variable("par"+i, parmTypes[i], "", C4VariableScope.VAR_VAR);
//		C4Function f = new C4Function(name, retType, parms);
//		ENGINE_OBJECT.addField(f);
//	}
//	
//	private void removeSystemDuplicates() {
//		List<C4Function> toBeRemoved = new LinkedList<C4Function>();
//		for (C4Function f : ENGINE_OBJECT.functions()) {
//			C4Function dup = EXTERN_INDEX.findGlobalFunction(f.getName());
//			if (dup != null && dup.getScript() instanceof C4ScriptExtern)
//				toBeRemoved.add(f);
//		}
//		if (toBeRemoved.size() != 0) {
//			for (C4Function r : toBeRemoved)
//				ENGINE_OBJECT.removeField(r);
//			saveEngineObject();
//		}
//	}
	
//	private void chanceToAddMissingThingsToEngine() {
//	//	removeSystemDuplicates();
//	}

	public static IPath getEngineCacheFile() {
		IPath path = ClonkCore.getDefault().getStateLocation();
		return path.append("engine");
	}
	
	public ClonkLibBuilder getLibBuilder() {
		if (libBuilder == null) libBuilder = new ClonkLibBuilder();
		return libBuilder;
	}

	public static IPath getExternLibCacheFile() {
		IPath path = ClonkCore.getDefault().getStateLocation();
		return path.append("externlib");
	}
	
	public void saveEngineObject() {
		try {
			IPath engine = getEngineCacheFile();
			
			File engineFile = engine.toFile();
			if (engineFile.exists())
				engineFile.delete();
			
			FileOutputStream outputStream = new FileOutputStream(engineFile);
			ObjectOutputStream encoder = new ObjectOutputStream(new BufferedOutputStream(outputStream));
			encoder.writeObject(ENGINE_OBJECT);
			encoder.close();
		} catch (FileNotFoundException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		}
		
	}
	
	public void saveExternIndex(IProgressMonitor monitor) throws FileNotFoundException {
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
	
	public void loadExternIndex() {
		final File index = getExternLibCacheFile().toFile();
		if (!index.exists()) {
			EXTERN_INDEX = new ClonkIndex();
		} else {
			try {
				FileInputStream in = new FileInputStream(index);
				ObjectInputStream objStream = new InputStreamRespectingUniqueIDs(in);
				EXTERN_INDEX = (ClonkIndex)objStream.readObject();
				EXTERN_INDEX.fixReferencesAfterSerialization();
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
	
	/**
	 * Returns an icon image (uses image registry where possible). If the icon doesn't exist,
	 * a "missing" image is returned.
	 * 
	 * @param iconName Name of the icon
	 */
	public Image getIconImage(String iconName) {
		
		// Already exists?
		ImageRegistry reg = getImageRegistry();
		Image img = reg.get(iconName);
		if (img != null)
			return img;
		
		// Create
		ImageDescriptor descriptor = getIconImageDescriptor(iconName);
		reg.put(iconName, img = descriptor.createImage(true));
		return img;
	}

	public ImageDescriptor getIconImageDescriptor(String iconName) {
		ImageDescriptor descriptor = getImageDescriptor("icons/" + iconName + ".png");
		if(descriptor == null)
			descriptor = ImageDescriptor.getMissingImageDescriptor();
		return descriptor;
	}
	
}
