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
import java.lang.reflect.InvocationTargetException;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.xml.parsers.ParserConfigurationException;
import javax.xml.xpath.XPathExpressionException;

import net.arctics.clonk.index.C4Engine;
import net.arctics.clonk.index.C4ObjectExtern;
import net.arctics.clonk.index.ExternIndex;
import net.arctics.clonk.index.ProjectIndex;
import net.arctics.clonk.parser.inireader.IniData;
import net.arctics.clonk.parser.inireader.IniUnit;
import net.arctics.clonk.parser.mapcreator.C4MapCreator;
import net.arctics.clonk.parser.stringtbl.StringTbl;
import net.arctics.clonk.preferences.ClonkPreferences;
import net.arctics.clonk.resource.ClonkLibBuilder;
import net.arctics.clonk.resource.ClonkProjectNature;
import net.arctics.clonk.resource.InputStreamRespectingUniqueIDs;
import net.arctics.clonk.util.IRunnableWithProgressAndResult;

import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResourceChangeEvent;
import org.eclipse.core.resources.IResourceChangeListener;
import org.eclipse.core.resources.ISaveContext;
import org.eclipse.core.resources.ISaveParticipant;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.Platform;
import org.eclipse.core.runtime.QualifiedName;
import org.eclipse.jface.dialogs.ProgressMonitorDialog;
import org.eclipse.jface.resource.ImageDescriptor;
import org.eclipse.jface.resource.ImageRegistry;
import org.eclipse.swt.graphics.Image;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.editors.text.TextFileDocumentProvider;
import org.eclipse.ui.plugin.AbstractUIPlugin;
import org.osgi.framework.BundleContext;
import org.xml.sax.SAXException;

/**
 * The core of the plugin. The singleton instance of this class stores various global things, including
 * the extern index that contains all objects imported from external object packs and the engine object that
 * contains global predefined functions of Clonk.
 */
public class ClonkCore extends AbstractUIPlugin implements ISaveParticipant, IResourceChangeListener {

	/**
	 * The Plugin-ID
	 */
	public static final String PLUGIN_ID = ClonkCore.class.getPackage().getName();

	/**
	 * id for Clonk project natures
	 */
	public static final String CLONK_NATURE_ID = id("clonknature"); //$NON-NLS-1$
	public static final String CLONK_DEPS_NATURE_ID = id("clonkdepsnature"); //$NON-NLS-1$

	/**
	 * id for error markers that denote errors occuring while importing extern libs
	 */
	public static final String MARKER_EXTERN_LIB_ERROR = id("externliberror"); //$NON-NLS-1$

	/**
	 * id for error markers that denote errors in a script
	 */
	public static final String MARKER_C4SCRIPT_ERROR = id("c4scripterror"); //$NON-NLS-1$
	
	/**
	 * id for error markers that denote errors in a ini file
	 */
	public static final String MARKER_INI_ERROR = id("inierror");

	public static final QualifiedName FOLDER_C4ID_PROPERTY_ID = new QualifiedName(PLUGIN_ID, "c4id"); //$NON-NLS-1$
	public static final QualifiedName C4OBJECT_PROPERTY_ID = new QualifiedName(PLUGIN_ID, "c4object"); //$NON-NLS-1$
	public static final QualifiedName C4STRUCTURE_PROPERTY_ID = new QualifiedName(PLUGIN_ID, "structure"); //$NON-NLS-1$

	/**
	 * The engine object contains global functions and variables defined by Clonk itself
	 */
	private C4Engine activeEngine;
	
	/**
	 * List of engines currently loaded
	 */
	private Map<String, C4Engine> engines = new HashMap<String, C4Engine>();

	/**
	 * Index that contains objects and scripts imported from external object packs and .c4g-groups 
	 */
	private ExternIndex externIndex;

	/**
	 * ini configuration definitions for the various Clonk configuration files
	 */
	public IniData iniConfigurations;

	/**
	 * Shared instance
	 */
	private static ClonkCore plugin;

	/**
	 * builder to import external libs
	 */
	private ClonkLibBuilder libBuilder = null;

	private TextFileDocumentProvider textFileDocumentProvider;

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

		ResourcesPlugin.getWorkspace().addSaveParticipant(this, this);
		ResourcesPlugin.getWorkspace().addResourceChangeListener(this, IResourceChangeEvent.PRE_DELETE);

		registerStructureClasses();
	}

	private void registerStructureClasses() {
		IniUnit.register();
		StringTbl.register();
		C4MapCreator.register();
	}

	private void loadIniConfigurations() {
		try {
			IniData data = new IniData(getBundle().getEntry("res/iniconfig.xml").openStream()); //$NON-NLS-1$
			data.parse();
			iniConfigurations = data; // set this variable when parsing completed successfully
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
	
	public Map<String, C4Engine> getEngines() {
		return Collections.unmodifiableMap(engines);
	}
	
	private C4Engine loadEngine(final String engineName) {
		InputStream engineStream;
		C4Engine result = engines.get(engineName);
		if (result != null)
			return result;
		try {
			if (getEngineCacheFile().toFile().exists()) {
				engineStream = new FileInputStream(getEngineCacheFile().toFile());
			}
			else {
				engineStream = getBundle().getEntry(String.format("res/engines/%s.engine", engineName)).openStream(); //$NON-NLS-1$
			}
			ObjectInputStream objStream = new InputStreamRespectingUniqueIDs(engineStream);
			result = (C4Engine)objStream.readObject();
			result.postSerialize(null);
		} catch (Exception e) {
			// fallback to xml
			ProgressMonitorDialog progressDialog = new ProgressMonitorDialog(PlatformUI.getWorkbench().getActiveWorkbenchWindow().getShell());
			try {
				IRunnableWithProgressAndResult<C4Engine> xmlImportor = new IRunnableWithProgressAndResult<C4Engine>() {
					private C4Engine engine;
					
					@Override
                    public C4Engine getResult() {
	                    return engine;
                    }

					@Override
                    public void run(IProgressMonitor monitor) throws InvocationTargetException, InterruptedException {
						engine = new C4Engine(engineName);
	            		try {
	            			engine.importFromXML(getBundle().getEntry(String.format("res/engines/%s.engine.xml", engineName)).openStream(), monitor); //$NON-NLS-1$
	            		} catch (Exception e) {
	            			e.printStackTrace();
	            		}
                    }
				};
	            progressDialog.run(false, false, xmlImportor);
	            result = xmlImportor.getResult();
            } catch (Exception e1) {
	            e1.printStackTrace();
            }
		}
		if (result != null)
			engines.put(engineName, result);
		return result;
	}

	public void loadEngineObject() throws FileNotFoundException, IOException, ClassNotFoundException, XPathExpressionException, ParserConfigurationException, SAXException {
		C4Engine engine = loadEngine(ClonkPreferences.getPreferenceOrDefault(ClonkPreferences.ACTIVE_ENGINE));
		setActiveEngine(engine);
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
		return path.append("engine"); //$NON-NLS-1$
	}

	public ClonkLibBuilder getLibBuilder() {
		if (libBuilder == null) libBuilder = new ClonkLibBuilder();
		return libBuilder;
	}

	public static IPath getExternLibCacheFile() {
		IPath path = ClonkCore.getDefault().getStateLocation();
		return path.append("externlib"); //$NON-NLS-1$
	}

	public void saveEngineObject() {
		try {
			IPath engine = getEngineCacheFile();

			File engineFile = engine.toFile();
			if (engineFile.exists())
				engineFile.delete();

			FileOutputStream outputStream = new FileOutputStream(engineFile);
			//XMLEncoder encoder = new XMLEncoder(new BufferedOutputStream(outputStream));
			ObjectOutputStream encoder = new ObjectOutputStream(new BufferedOutputStream(outputStream));
			encoder.writeObject(getActiveEngine());
			encoder.close();
		} catch (FileNotFoundException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		}

	}

	public void saveExternIndex(IProgressMonitor monitor) throws FileNotFoundException {
		if (monitor != null) monitor.beginTask(Messages.ClonkCore_14, 1);
		final File index = getExternLibCacheFile().toFile();
		FileOutputStream out = new FileOutputStream(index);

		try {
			ObjectOutputStream objStream = new ObjectOutputStream(out);
			objStream.writeObject(externIndex);
			objStream.close();
			if (monitor != null) monitor.worked(1);
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		if (monitor != null)
			monitor.done();
	}

	public void loadExternIndex() {
		final File index = getExternLibCacheFile().toFile();
		if (!index.exists()) {
			externIndex = new ExternIndex();
		} else {
			try {
				FileInputStream in = new FileInputStream(index);
				ObjectInputStream objStream = new InputStreamRespectingUniqueIDs(in);
				externIndex = (ExternIndex)objStream.readObject();
				externIndex.postSerialize();
			} catch (Exception e) {
				externIndex = new ExternIndex();
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
		ImageDescriptor descriptor = getImageDescriptor("icons/" + iconName + ".png"); //$NON-NLS-1$ //$NON-NLS-2$
		if(descriptor == null)
			descriptor = ImageDescriptor.getMissingImageDescriptor();
		return descriptor;
	}

	public void doneSaving(ISaveContext context) {
	}

	public void prepareToSave(ISaveContext context) throws CoreException {
		// i don't need any preparation!1
	}

	public void rollback(ISaveContext context) {
	}

	public void saving(ISaveContext context) throws CoreException {
		ClonkProjectNature clonkProj;
		switch (context.getKind()) {
		case ISaveContext.PROJECT_SAVE:
			clonkProj = ClonkProjectNature.getClonkNature(context.getProject());
			if (clonkProj != null) {
				clonkProj.saveIndex();
			}
			break;
		case ISaveContext.SNAPSHOT:
		case ISaveContext.FULL_SAVE:
			for (IProject project : ResourcesPlugin.getWorkspace().getRoot().getProjects()) {
				if (!project.isOpen())
					continue;
				clonkProj = ClonkProjectNature.getClonkNature(project);
				if (clonkProj != null) {
					clonkProj.saveIndex();
				}
			}
			removeOldIndexes();
			break;
		}
	}

	private void removeOldIndexes() {
		File stateDir = getStateLocation().toFile();
		for (String file : stateDir.list()) {
			if (file.endsWith(ProjectIndex.INDEXFILE_SUFFIX) && ResourcesPlugin.getWorkspace().getRoot().findMember(file.substring(0, file.length()-ProjectIndex.INDEXFILE_SUFFIX.length())) == null) {
				new File(stateDir, file).delete();
			}
		}
	}

	public static String id(String id) {
		return PLUGIN_ID + "." + id; //$NON-NLS-1$
	}

	public String getLanguagePref() {
		return Platform.getPreferencesService().getString(PLUGIN_ID, ClonkPreferences.PREFERRED_LANGID, "DE", null); //$NON-NLS-1$
	}

	/**
	 * @param activeEngine the engineObject to set
	 */
	private void setActiveEngine(C4Engine activeEngine) {
		this.activeEngine = activeEngine;
	}

	/**
	 * @return the engineObject
	 */
	public C4ObjectExtern getActiveEngine() {
		return activeEngine;
	}

	/**
	 * @param externIndex the externIndex to set
	 */
	public void setExternIndex(ExternIndex externIndex) {
		this.externIndex = externIndex;
	}

	/**
	 * @return the externIndex
	 */
	public ExternIndex getExternIndex() {
		return externIndex;
	}

	/**
	 * Return the shared text file document provider
	 * @return the provider
	 */
	public TextFileDocumentProvider getTextFileDocumentProvider() {
		if (textFileDocumentProvider == null)
			textFileDocumentProvider = new TextFileDocumentProvider();
		return textFileDocumentProvider;
	}

	@Override
	public void resourceChanged(IResourceChangeEvent event) {
		try {
			switch (event.getType()) {
			case IResourceChangeEvent.PRE_DELETE:
				// delete old index - could be renamed i guess but renaming a project is not exactly a common activity
				if (event.getResource() instanceof IProject && ((IProject)event.getResource()).hasNature(CLONK_NATURE_ID)) {
					ClonkProjectNature proj = ClonkProjectNature.getClonkNature(event.getResource());
					proj.getIndexFileLocation().toFile().delete();
				}
			}
		} catch (CoreException e) {
			e.printStackTrace();
		}
	}

}
