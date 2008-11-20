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
import java.util.HashSet;
import java.util.Set;

import net.arctics.clonk.parser.C4Field;
import net.arctics.clonk.parser.C4Function;
import net.arctics.clonk.parser.C4ID;
import net.arctics.clonk.parser.C4Object;
import net.arctics.clonk.parser.C4ObjectExtern;
import net.arctics.clonk.parser.C4Variable;
import net.arctics.clonk.parser.ClonkIndex;
import net.arctics.clonk.resource.ClonkProjectNature;
import net.arctics.clonk.resource.InputStreamRespectingUniqueIDs;
import org.eclipse.core.resources.IFolder;
import org.eclipse.core.resources.IResourceChangeEvent;
import org.eclipse.core.resources.IResourceChangeListener;
import org.eclipse.core.resources.IResourceDelta;
import org.eclipse.core.resources.IResourceDeltaVisitor;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.QualifiedName;
import org.eclipse.jface.resource.ImageDescriptor;
import org.eclipse.swt.widgets.Display;
import org.eclipse.ui.plugin.AbstractUIPlugin;
import org.osgi.framework.BundleContext;

/**
 * The activator class controls the plug-in life cycle
 */
public class ClonkCore extends AbstractUIPlugin implements IResourceChangeListener, IResourceDeltaVisitor {

	// The plug-in ID
	public static final String PLUGIN_ID = "net.arctics.clonk";
	public static final String CLONK_NATURE_ID = "net.arctics.clonk.clonknature";
	public static final String MARKER_EXTERN_LIB_ERROR = PLUGIN_ID + ".externliberror";
	public static final QualifiedName FOLDER_C4ID_PROPERTY_ID = new QualifiedName("net.arctics.clonk","c4id");
	public static final QualifiedName C4OBJECT_PROPERTY_ID = new QualifiedName("net.arctics.clonk","c4object");
	
//	public static final List<C4Function> ENGINE_FUNCTIONS = new ArrayList<C4Function>(505);
	
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
		
//		URL engineConstants = getBundle().getEntry("res/constants.txt");
//		if (engineConstants != null) {
//			BufferedReader reader = new BufferedReader(new InputStreamReader(engineConstants.openStream()));
//			String line;
//			while((line = reader.readLine()) != null) {
//				C4Variable var = new C4Variable(line,C4VariableScope.VAR_CONST);
//				var.setObject(ENGINE_OBJECT);
//				ENGINE_OBJECT.addField(var);
//			}
//		}
		
		ResourcesPlugin.getWorkspace().addResourceChangeListener(this);
//		try {
//			FileInputStream file = new FileInputStream("javadata.txt");
//			ObjectOutputStream encoder = new ObjectOutputStream(new BufferedOutputStream(new FileOutputStream("engine.dat")));
//	//		java.beans.XMLEncoder encoder = new XMLEncoder(new BufferedOutputStream(new FileOutputStream("engine.xml")));
//			C4Function func = null;
//			while((func = parseFunction(file)) != null) {
//				encoder.writeObject(func);
//			}
//			encoder.flush();
//			encoder.close();
//		}
//		catch (FileNotFoundException e) {
//			e.printStackTrace();
//		}
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
					field.setObject(ENGINE_OBJECT);
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
	
	public static void saveExternIndex() throws FileNotFoundException {
		final File index = getExternLibCacheFile().toFile();
		FileOutputStream out = new FileOutputStream(index);
		try {
			ObjectOutputStream objStream = new ObjectOutputStream(out);
			objStream.writeObject(EXTERN_INDEX);
			objStream.close();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
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
	
//	private C4Function parseFunction(InputStream stream) throws IOException {
//		if (stream.read() == 'F') {
//			char[] buffer = new char[256];
//			String funcName = null, returnType = null, desc = null;
//			List<C4Variable> vars = new ArrayList<C4Variable>(5);
//			int i = 0;
//			int read = 0;
//			while((read = stream.read()) >= 0) {
//				if (read == '(') {
//					funcName = new String(buffer,0,i);
//					break;
//				}
//				else buffer[i++] = (char)read;
//			}
//			do {
//				C4Variable var;
//				var = parseParameter(stream);
//				if(var != null) vars.add(var);
//				else break;
//			} while(true);
//			
////			if (stream.read() != ')') System.out.println("There should be a ) right now");
//			i = 0;
//			read = 0;
//			while((read = stream.read()) >= 0) {
//				if (read == ':') {
//					returnType = new String(buffer,0,i);
//					break;
//				}
//				else buffer[i++] = (char)read;
//			}
//			buffer = new char[4096];
//			i = 0;
//			read = 0;
//			while((read = stream.read()) >= 0) {
//				if (read == ';') {
//					int c2 = stream.read();
//					int c3 = stream.read();
//					if (c2 == ';' && c3 == ';') {
//						desc = new String(buffer,0,i);
//						break;
//					}
//					else {
//						buffer[i++] = (char)read;
//						buffer[i++] = (char)c2;
//						buffer[i++] = (char)c3;
//					}
//				}
//				else buffer[i++] = (char)read;
//			}
//			
//			return new C4Function(funcName,returnType,desc,vars.toArray(new C4Variable[] { }));
//		}
//		else {
//			return null;
//		}
//	}
	
//	private C4Variable parseParameter(InputStream stream) throws IOException {
//		if (stream.read() == 'P') {
//			char[] buffer = new char[256];
//			String varType = null, parName = null, desc = null;;
//			int i = 0;
//			int read = 0;
//			while((read = stream.read()) >= 0) {
//				if (read == ' ') {
//					varType = new String(buffer,0,i);
//					break;
//				}
//				else buffer[i++] = (char)read;
//			}
//			i = 0;
//			read = 0;
//			while((read = stream.read()) >= 0) {
//				if (read == ':') {
//					parName = new String(buffer,0,i);
//					break;
//				}
//				else buffer[i++] = (char)read;
//			}
//			
//			buffer = new char[4096];
//			i = 0;
//			read = 0;
//			while((read = stream.read()) >= 0) {
//				if (read == ';') {
//					int c2 = stream.read();
//					int c3 = stream.read();
//					if (c2 == ';' && c3 == ';') {
//						desc = new String(buffer,0,i);
//						break;
//					}
//					else {
//						buffer[i++] = (char)read;
//						buffer[i++] = (char)c2;
//						buffer[i++] = (char)c3;
//					}
//				}
//				else buffer[i++] = (char)read;
//			}
//			
//			return new C4Variable(parName,varType,desc);
//		}
//		else {
//			return null;
//		}
//	}

	/*
	 * (non-Javadoc)
	 * @see org.eclipse.ui.plugin.AbstractUIPlugin#stop(org.osgi.framework.BundleContext)
	 */
	public void stop(BundleContext context) throws Exception {
		//saveExternIndex(); clean build causes save
		ResourcesPlugin.getWorkspace().removeResourceChangeListener(this);
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

	private transient Set<ClonkProjectNature> gatheredNatures;
	public void resourceChanged(IResourceChangeEvent event) {
		try {
			gatheredNatures = new HashSet<ClonkProjectNature>();
			event.getDelta().accept(this);
			Display.getDefault().asyncExec(new Runnable() {
				public void run() {
					for (ClonkProjectNature nature : gatheredNatures)
						try {
							nature.saveIndexData();
						} catch (CoreException e) {
							// TODO Auto-generated catch block
							e.printStackTrace();
						} // resave indexdata
					gatheredNatures = null;	
				}
			});
		} catch (CoreException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}

	public boolean visit(IResourceDelta delta) throws CoreException {
		if (delta.getKind() == IResourceDelta.REMOVED) {
			ClonkProjectNature proj = Utilities.getProject(delta.getResource());
			gatheredNatures.add(proj);
			if (proj != null && delta.getResource() instanceof IFolder) {
				C4Object obj = C4Object.objectCorrespondingTo((IFolder)delta.getResource());
				if (obj != null)
					proj.getIndexedData().removeObject(obj);
			}
		}
		return true;
	}
}
