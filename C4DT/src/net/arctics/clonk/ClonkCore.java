package net.arctics.clonk;

import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.io.ObjectOutputStream;
import java.io.OutputStream;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.Collections;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import javax.xml.parsers.ParserConfigurationException;
import javax.xml.xpath.XPathExpressionException;

import net.arctics.clonk.index.C4Engine;
import net.arctics.clonk.index.ProjectIndex;
import net.arctics.clonk.parser.inireader.IniUnit;
import net.arctics.clonk.parser.mapcreator.C4MapCreator;
import net.arctics.clonk.parser.stringtbl.StringTbl;
import net.arctics.clonk.preferences.ClonkPreferences;
import net.arctics.clonk.resource.ClonkProjectNature;
import net.arctics.clonk.util.IStorageLocation;
import net.arctics.clonk.util.ReadOnlyIterator;
import net.arctics.clonk.util.UI;

import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.IResourceChangeEvent;
import org.eclipse.core.resources.IResourceChangeListener;
import org.eclipse.core.resources.ISaveContext;
import org.eclipse.core.resources.ISaveParticipant;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.AssertionFailedException;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.Path;
import org.eclipse.core.runtime.QualifiedName;
import org.eclipse.jface.resource.ImageDescriptor;
import org.eclipse.jface.resource.ImageRegistry;
import org.eclipse.jface.text.IDocument;
import org.eclipse.jface.util.IPropertyChangeListener;
import org.eclipse.jface.util.PropertyChangeEvent;
import org.eclipse.swt.graphics.Image;
import org.eclipse.ui.editors.text.TextFileDocumentProvider;
import org.eclipse.ui.plugin.AbstractUIPlugin;
import org.eclipse.ui.texteditor.IDocumentProvider;
import org.osgi.framework.BundleContext;
import org.xml.sax.SAXException;

/**
 * The core of the plugin. The singleton instance of this class stores various global things, like engine objects and preferences.
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

	/**
	 * id for error markers that denote errors in a script
	 */
	public static final String MARKER_C4SCRIPT_ERROR = id("c4scripterror"); //$NON-NLS-1$
	public static final String MARKER_C4SCRIPT_ERROR_WHILE_TYPING = id("c4scripterrorwhiletyping"); //$NON-NLS-1$
	
	/**
	 * id for error markers that denote errors in a ini file
	 */
	public static final String MARKER_INI_ERROR = id("inierror"); //$NON-NLS-1$

	public static final QualifiedName FOLDER_C4ID_PROPERTY_ID = new QualifiedName(PLUGIN_ID, "c4id"); //$NON-NLS-1$
	public static final QualifiedName C4OBJECT_PROPERTY_ID = new QualifiedName(PLUGIN_ID, "c4object"); //$NON-NLS-1$
	public static final QualifiedName C4STRUCTURE_PROPERTY_ID = new QualifiedName(PLUGIN_ID, "structure"); //$NON-NLS-1$
	
	public static final long SERIAL_VERSION_UID = 1L;

	/**
	 * The engine object contains global functions and variables defined by Clonk itself
	 */
	private C4Engine activeEngine;
	
	/**
	 * List of engines currently loaded
	 */
	private Map<String, C4Engine> loadedEngines = new HashMap<String, C4Engine>();

	/**
	 * Shared instance
	 */
	private static ClonkCore plugin;

	/**
	 * Provider used by the plugin to provide text of documents
	 */
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
	@SuppressWarnings("deprecation")
	public void start(BundleContext context) throws Exception {
		super.start(context);
		plugin = this;

		loadActiveEngine();

		// FIXME: this is deprecated and stuff
		ResourcesPlugin.getWorkspace().addSaveParticipant(this, this);
		//ResourcesPlugin.getWorkspace().addResourceChangeListener(this, IResourceChangeEvent.PRE_DELETE);

		registerStructureClasses();
		
		// react to active engine being changed
		getPreferenceStore().addPropertyChangeListener(new IPropertyChangeListener() {
			@Override
			public void propertyChange(PropertyChangeEvent event) {
				if (event.getProperty().equals(ClonkPreferences.ACTIVE_ENGINE)) {
					setActiveEngineByName(ClonkPreferences.getPreferenceOrDefault(ClonkPreferences.ACTIVE_ENGINE));
				}
			}
		});
	}

	private void registerStructureClasses() {
		IniUnit.register();
		StringTbl.register();
		C4MapCreator.register();
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
	
	public Map<String, C4Engine> getLoadedEngines() {
		return Collections.unmodifiableMap(loadedEngines);
	}
	
	private String engineNameFromPath(String path) {
		String folderName = path.endsWith("/") //$NON-NLS-1$
			? path.substring(path.lastIndexOf('/', path.length()-2)+1, path.length()-1)
			: path.substring(path.lastIndexOf('/')+1);
		return folderName.startsWith(".") ? null : folderName; //$NON-NLS-1$
	}
	
	public List<String> getAvailableEngines() {
		List<String> result = new LinkedList<String>();
		// get built-in engine definitions
		for (@SuppressWarnings("unchecked")
		Enumeration<String> paths = getBundle().getEntryPaths("res/engines"); paths.hasMoreElements();) { //$NON-NLS-1$
			String engineName = engineNameFromPath(paths.nextElement());
			if (engineName != null) {
				result.add(engineName);
			}
		}
		// get engine definitions from workspace
		File[] workspaceEngines = getWorkspaceStorageLocationForEngines().toFile().listFiles();
		if (workspaceEngines != null) {
			for (File wEngine : workspaceEngines) {
				// only accepting folders should be sufficient
				if (!wEngine.isDirectory())
					continue;
				String engineName = engineNameFromPath(new Path(wEngine.getAbsolutePath()).toString());
				if (engineName != null && !result.contains(engineName))
					result.add(engineName);
			}
		}
		return result;
	}
	
	public Iterable<C4Engine> loadedEngines() {
		return new Iterable<C4Engine>() {
			@Override
			public Iterator<C4Engine> iterator() {
				return new ReadOnlyIterator<C4Engine>(loadedEngines.values().iterator());
			}
		};
	}
	
	public C4Engine loadEngine(final String engineName) {
		if (engineName == null || engineName.equals("")) //$NON-NLS-1$
			return null;
		C4Engine result = loadedEngines.get(engineName);
		if (result != null)
			return result;
		IStorageLocation[] locations = new IStorageLocation[2];
		if (getBundle() != null) {
			// bundle given; assume the usual storage locations (workspace and plugin bundle contents) are present
			getIDEStorageLocations(engineName, locations);
		} else {
			// no bundle? seems to run headlessly
			getHeadlessStorageLocations(engineName, locations);
		}
		result = C4Engine.loadFromStorageLocations(locations);
		if (result != null)
			loadedEngines.put(engineName, result);
		return result;
	}
	
	private void getIDEStorageLocations(final String engineName, IStorageLocation[] locations) {
		locations[0] = new FolderStorageLocation(engineName) {
			@Override
			protected IPath getStorageLocationForEngine(String engineName) {
				return getWorkspaceStorageLocationForEngine(engineName);
			}
		};
		
		locations[1] = new IStorageLocation() {
			@Override
			public URL getURL(String entryName, boolean create) {
				return create ? null : getBundle().getEntry(String.format("res/engines/%s/%s", engineName, entryName)); //$NON-NLS-1$
			}
			@Override
			public String getName() {
				return engineName;
			}
			@Override
			public OutputStream getOutputStream(URL storageURL) {
				return null;
			}
			@SuppressWarnings("unchecked")
			@Override
			public Enumeration<URL> getURLs(String containerName) {
				return ClonkCore.getDefault().getBundle().findEntries(String.format("res/engines/%s/%s", engineName, containerName), "*.*", false); //$NON-NLS-1$ //$NON-NLS-2$
			}
		};
	}
	
	private void getHeadlessStorageLocations(String engineName, IStorageLocation[] locations) {
		locations[0] = new FolderStorageLocation(engineName) {
			private IPath storageLocationPath = new Path(engineConfigurationFolder).append(this.engineName);
			@Override
			protected IPath getStorageLocationForEngine(String engineName) {
				return storageLocationPath;
			}
		};
	}

	private String engineConfigurationFolder;

	public void loadActiveEngine() throws FileNotFoundException, IOException, ClassNotFoundException, XPathExpressionException, ParserConfigurationException, SAXException {
		setActiveEngineByName(ClonkPreferences.getPreferenceOrDefault(ClonkPreferences.ACTIVE_ENGINE));
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

	public IPath getWorkspaceStorageLocationForActiveEngine() {
		return getWorkspaceStorageLocationForEngine(ClonkPreferences.getPreferenceOrDefault(ClonkPreferences.ACTIVE_ENGINE));
	}
	
	public IPath getWorkspaceStorageLocationForEngine(String engineName) {
		IPath path = getWorkspaceStorageLocationForEngines(); //$NON-NLS-1$
		File dir = path.toFile();
		if (!dir.exists())
			dir.mkdir();
		return path.append(String.format("%s", engineName)); //$NON-NLS-1$
	}

	private IPath getWorkspaceStorageLocationForEngines() {
	    return getStateLocation().append("engines"); //$NON-NLS-1$
    }

	public static IPath getExternLibCacheFile() {
		IPath path = ClonkCore.getDefault().getStateLocation();
		return path.append("externlib"); //$NON-NLS-1$
	}

	public void exportEngineToXMLInWorkspace(String engineName) {
		try {
			IPath engineXML = getWorkspaceStorageLocationForEngine(engineName).addFileExtension("xml"); //$NON-NLS-1$

			File engineXMLFile = engineXML.toFile();
			if (engineXMLFile.exists())
				engineXMLFile.delete();

			FileWriter writer = new FileWriter(engineXMLFile);
			try {
				this.getActiveEngine().exportAsXML(writer);
			} finally {
				writer.close();
			}
		} catch (FileNotFoundException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		}		
	}
	
	public void saveEngineInWorkspace(String engineName) {
		try {
			IPath engine = getWorkspaceStorageLocationForEngine(engineName);

			File engineFile = engine.toFile();
			if (engineFile.exists())
				engineFile.delete();

			FileOutputStream outputStream = new FileOutputStream(engineFile);
			//XMLEncoder encoder = new XMLEncoder(new BufferedOutputStream(outputStream));
			ObjectOutputStream encoder = new ObjectOutputStream(new BufferedOutputStream(outputStream));
			encoder.writeObject(getActiveEngine());
			encoder.close();
			loadedEngines.remove(engineName);
		} catch (FileNotFoundException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
	
	public void saveActiveEngineInWorkspace() {
		saveEngineInWorkspace(getActiveEngine().getName());
	}

	/*
	 * (non-Javadoc)
	 * @see org.eclipse.ui.plugin.AbstractUIPlugin#stop(org.osgi.framework.BundleContext)
	 */
	@SuppressWarnings("deprecation")
	public void stop(BundleContext context) throws Exception {
		//saveExternIndex(); clean build causes save
		ResourcesPlugin.getWorkspace().removeSaveParticipant(this);
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
	
	public static void headlessInitialize(String engineConfigurationFolder, String engine) {
		if (plugin == null) {
			plugin = new ClonkCore();
			plugin.engineConfigurationFolder = engineConfigurationFolder;
			plugin.setActiveEngineByName(engine);
		}
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
	}

	public void rollback(ISaveContext context) {
	}

	public void saving(ISaveContext context) throws CoreException {
		ClonkProjectNature clonkProj;
		switch (context.getKind()) {
		case ISaveContext.PROJECT_SAVE:
			clonkProj = ClonkProjectNature.get(context.getProject());
			if (clonkProj != null) {
				clonkProj.saveIndex();
			}
			break;
		case ISaveContext.SNAPSHOT:
		case ISaveContext.FULL_SAVE:
			for (C4Engine engine : loadedEngines.values()) {
				try {
					engine.saveSettings();
				} catch (IOException e) {
					UI.informAboutException(Messages.ClonkCore_ErrorWhileSavingSettings, e, engine.getName());
				}
			}
			for (IProject project : ResourcesPlugin.getWorkspace().getRoot().getProjects()) {
				try {
					if (!project.isOpen())
						continue;
					clonkProj = ClonkProjectNature.get(project);
					if (clonkProj != null) {
						clonkProj.saveIndex();
					}
				} catch (Exception e) {
					UI.informAboutException(Messages.ClonkCore_ErrorWhileSavingIndex, e, project.getName());
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

	/**
	 * @param activeEngine the engineObject to set
	 */
	private void setActiveEngine(C4Engine activeEngine) {
		this.activeEngine = activeEngine;
	}
	
	public void setActiveEngineByName(String engineName) {
		C4Engine e = loadEngine(engineName);
		// make sure names are correct
		if (e != null) {
			e.setName(engineName);
			setActiveEngine(e);
		}
	}

	/**
	 * @return the engineObject
	 */
	public C4Engine getActiveEngine() {
		return activeEngine;
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
	
	private abstract class FolderStorageLocation implements IStorageLocation {
		protected final String engineName;

		private FolderStorageLocation(String engineName) {
			this.engineName = engineName;
		}

		@Override
		public String getName() {
			return engineName;
		}

		@Override
		public URL getURL(String entryName, boolean create) {
			try {
				File file = getFile(entryName);
				try {
					if (create) {
						try {
							file.getParentFile().mkdirs();
							file.createNewFile();
						} catch (IOException e) {
							e.printStackTrace();
							return null;
						}
					}
					return file.exists() ? file.toURI().toURL() : null;
				} catch (MalformedURLException e) {
					e.printStackTrace();
					return null;
				}
			} catch (AssertionFailedException assertionFail) {
				// happens when invoking getURL without having initialized the workspace (headless utilities)
				return null;
			}
		}

		protected abstract IPath getStorageLocationForEngine(String engineName);

		private File getFile(String entryName) {
			IPath path = getStorageLocationForEngine(engineName);
			File file = path.append(entryName).toFile();
			return file;
		}

		@Override
		public OutputStream getOutputStream(URL storageURL) {
			try {
				return new FileOutputStream(new File(storageURL.getFile()));
			} catch (IOException e) {
				e.printStackTrace();
				return null;
			}
		}

		@Override
		public Enumeration<URL> getURLs(String containerName) {
			final File folder = getFile(containerName);
			return new Enumeration<URL>() {
				
				private int index = -1;
				private File[] files = folder.listFiles();

				@Override
				public boolean hasMoreElements() {
					return files != null && index+1 < files.length;
				}

				@Override
				public URL nextElement() {
					try {
						return files[++index].toURI().toURL();
					} catch (MalformedURLException e) {
						e.printStackTrace();
						return null;
					}
				}
				
			};
		}
	}

	public interface IDocumentAction {
		void run(IDocument document);
	}
	
	public void performActionsOnFileDocument(IResource resource, IDocumentAction action) throws CoreException {
		IDocumentProvider provider = getTextFileDocumentProvider();
		provider.connect(resource);
		try {
			IDocument document = provider.getDocument(resource);
			action.run(document);
			provider.saveDocument(null, resource, document, true);
		} finally {
			provider.disconnect(resource);
		}
	}

	@Override
	public void resourceChanged(IResourceChangeEvent event) {
		try {
			switch (event.getType()) {
			case IResourceChangeEvent.PRE_DELETE:
				// delete old index - could be renamed i guess but renaming a project is not exactly a common activity
				if (event.getResource() instanceof IProject && ((IProject)event.getResource()).hasNature(CLONK_NATURE_ID)) {
					ClonkProjectNature proj = ClonkProjectNature.get(event.getResource());
					proj.getIndexFileLocation().toFile().delete();
				}
			}
		} catch (CoreException e) {
			e.printStackTrace();
		}
	}

}
