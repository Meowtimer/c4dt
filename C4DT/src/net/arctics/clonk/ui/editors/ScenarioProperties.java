package net.arctics.clonk.ui.editors;

import static net.arctics.clonk.util.Utilities.as;

import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import net.arctics.clonk.Core;
import net.arctics.clonk.index.Definition;
import net.arctics.clonk.index.Index;
import net.arctics.clonk.index.Scenario;
import net.arctics.clonk.parser.ID;
import net.arctics.clonk.parser.c4script.SpecialEngineRules.ScenarioConfigurationProcessing;
import net.arctics.clonk.parser.inireader.ComplexIniEntry;
import net.arctics.clonk.parser.inireader.IDArray;
import net.arctics.clonk.parser.inireader.IniEntry;
import net.arctics.clonk.parser.inireader.IniItem;
import net.arctics.clonk.parser.inireader.IniSection;
import net.arctics.clonk.parser.inireader.IntegerArray;
import net.arctics.clonk.parser.inireader.ScenarioUnit;
import net.arctics.clonk.ui.OpenDefinitionDialog;
import net.arctics.clonk.ui.navigator.ClonkLabelProvider;
import net.arctics.clonk.util.IConverter;
import net.arctics.clonk.util.IPredicate;
import net.arctics.clonk.util.KeyValuePair;
import net.arctics.clonk.util.Sink;
import net.arctics.clonk.util.UI;

import org.eclipse.core.resources.IContainer;
import org.eclipse.core.runtime.IAdaptable;
import org.eclipse.jface.layout.TableColumnLayout;
import org.eclipse.jface.viewers.ColumnWeightData;
import org.eclipse.jface.viewers.IStructuredContentProvider;
import org.eclipse.jface.viewers.ITableLabelProvider;
import org.eclipse.jface.viewers.TableViewer;
import org.eclipse.jface.viewers.Viewer;
import org.eclipse.jface.viewers.ViewerDropAdapter;
import org.eclipse.jface.window.Window;
import org.eclipse.swt.SWT;
import org.eclipse.swt.dnd.DND;
import org.eclipse.swt.dnd.DragSourceAdapter;
import org.eclipse.swt.dnd.DragSourceEvent;
import org.eclipse.swt.dnd.Transfer;
import org.eclipse.swt.dnd.TransferData;
import org.eclipse.swt.events.ModifyEvent;
import org.eclipse.swt.events.ModifyListener;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.events.SelectionListener;
import org.eclipse.swt.events.VerifyEvent;
import org.eclipse.swt.events.VerifyListener;
import org.eclipse.swt.graphics.GC;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Group;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Scale;
import org.eclipse.swt.widgets.Slider;
import org.eclipse.swt.widgets.TabFolder;
import org.eclipse.swt.widgets.TabItem;
import org.eclipse.swt.widgets.Table;
import org.eclipse.swt.widgets.TableColumn;
import org.eclipse.swt.widgets.Text;
import org.eclipse.ui.IWorkbenchPropertyPage;
import org.eclipse.ui.dialogs.FilteredItemsSelectionDialog;
import org.eclipse.ui.dialogs.PropertyPage;
import org.eclipse.ui.part.PluginTransfer;
import org.eclipse.ui.part.PluginTransferData;

public class ScenarioProperties extends PropertyPage implements IWorkbenchPropertyPage {
	
	private static final String SCENARIO_PROPERTIES_MAIN_TAB_PREF = "scenarioPropertiesMainTab"; //$NON-NLS-1$
	private Scenario scenario;
	private ScenarioUnit scenarioConfiguration;
	private final Map<ID, Image> images = new HashMap<ID, Image>();
	
	private Image imageFor(Definition def) {
		if (def != null) {
			Image img = images.get(def.id());
			if (img == null && !images.containsKey(def.id())) {
				if (def.definitionFolder() != null)
					try {
						Image original = UI.imageForContainer(def.definitionFolder());
						final int s = 16;
						if (original != null) try {
							Image scaled = new Image(Display.getCurrent(), s, s);
							GC gc = new GC(scaled);
							try {
								gc.setAntialias(SWT.ON);
								gc.setInterpolation(SWT.HIGH);
								gc.drawImage(original, 0, 0, original.getBounds().width, original.getBounds().height, 0, 0, s, s);
							} finally {
								gc.dispose();
							}
							img = scaled;
						} finally {
							original.dispose();
						}
					} catch (Exception e) {
						// ...
					}
				images.put(def.id(), img);
			}
			if (img == null)
				img = UI.definitionIcon(def);
			return img;
		}
		return null;
	}

	private class DefinitionListEditor {

		private final IniEntry entry;
		private final IDArray array;
		private Table table;
		private final TableViewer viewer;
		private final IPredicate<Definition> definitionFilter;

		private void createControl(Composite parent, String label) {
			Label l = new Label(parent, SWT.NULL);
			l.setText(label);
			l.setLayoutData(new GridData(SWT.LEFT, SWT.TOP, false, false, 2, 1));
			
			Composite tableComposite = new Composite(parent, SWT.NO_SCROLL);
			GridData tableLayoutData = new GridData(GridData.FILL_BOTH);
			tableLayoutData.heightHint = 0;
			tableComposite.setLayoutData(tableLayoutData);
			TableColumnLayout tableLayout = new TableColumnLayout();
			tableComposite.setLayout(tableLayout);
			table = new Table(tableComposite, SWT.FULL_SELECTION|SWT.MULTI);
			table.setHeaderVisible(true);
			table.setLinesVisible(true);
			TableColumn imageColumn = new TableColumn(table, SWT.LEFT);
			TableColumn defColumn = new TableColumn(table, SWT.LEFT);
			defColumn.setText(Messages.ScenarioProperties_Definition);
			TableColumn countColumn = new TableColumn(table, SWT.RIGHT);
			countColumn.setText(Messages.ScenarioProperties_CountColumn);
			tableLayout.setColumnData(imageColumn, new ColumnWeightData(10));
			tableLayout.setColumnData(defColumn, new ColumnWeightData(80));
			tableLayout.setColumnData(countColumn, new ColumnWeightData(10));
			
			Composite buttons = new Composite(parent, SWT.NO_SCROLL);
			buttons.setLayoutData(new GridData(SWT.RIGHT, SWT.TOP, false, false, 1, 1));
			GridLayout buttonsLayout = new GridLayout(1, true);
			buttonsLayout.verticalSpacing = 2;
			buttonsLayout.makeColumnsEqualWidth = true;
			buttons.setLayout(buttonsLayout);
			Button add = new Button(buttons, SWT.PUSH);
			add.addSelectionListener(new SelectionAdapter() {
				@Override
				public void widgetSelected(SelectionEvent e) {
					addDefinitions();
				}
			});
			add.setText(Messages.ScenarioProperties_AddDefinitions);
			Button remove = new Button(buttons, SWT.PUSH);
			remove.setText(Messages.ScenarioProperties_RemoveDefinitions);
			remove.addSelectionListener(new SelectionAdapter() {
				@Override
				public void widgetSelected(SelectionEvent e) {
					int[] indices = table.getSelectionIndices();
					for (int i = indices.length-1; i >= 0; i--)
						array.childCollection().remove(indices[i]);
					viewer.refresh();
				}
			});
			final Button inc = new Button(buttons, SWT.PUSH);
			inc.setText(Messages.ScenarioProperties_Inc);
			final Button dec = new Button(buttons, SWT.PUSH);
			dec.setText(Messages.ScenarioProperties_Dec);
			SelectionAdapter changeAmountListener = new SelectionAdapter() {
				@Override
				public void widgetSelected(SelectionEvent e) {
					int change = 0;
					if (e.getSource() == inc)
						change = 1;
					else if (e.getSource() == dec)
						change = -1;
					int[] indices = table.getSelectionIndices();
					for (int i = indices.length-1; i >= 0; i--) {
						KeyValuePair<ID, Integer> kv = array.childCollection().get(indices[i]);
						kv.setValue(kv.value()+change);
					}
					viewer.refresh();
				}
			};
			inc.addSelectionListener(changeAmountListener);
			dec.addSelectionListener(changeAmountListener);
		}
		
		public DefinitionListEditor(String label, Composite parent, IniEntry entry) {
			createControl(parent, label);
			this.entry = entry;
			this.array = (IDArray)entry.value();
			this.viewer = createViewer();
			this.definitionFilter = entry.engine().specialRules().configurationEntryDefinitionFilter(entry);
		}

		private TableViewer createViewer() {
			TableViewer viewer = new TableViewer(table) {
				@Override
				public void refresh() {
					super.refresh();
				}
			};
			Transfer[] transferTypes = new Transfer[] { PluginTransfer.getInstance() };
			viewer.addDragSupport(DND.DROP_MOVE, transferTypes, new DragSourceAdapter() {
				@Override
				public void dragSetData(DragSourceEvent event) {
					ByteBuffer buffer = ByteBuffer.allocate(4*table.getSelectionCount());
					for (int i = 0; i < table.getSelectionCount(); i++)
						buffer.putInt(i*4, table.getSelectionIndices()[i]);
					event.data = new PluginTransferData(Core.PLUGIN_ID, buffer.array());
				}
			});
			viewer.addDropSupport(DND.DROP_MOVE, transferTypes, new ViewerDropAdapter(viewer) {
				private KeyValuePair<ID, Integer> target;
				@SuppressWarnings("unchecked")
				@Override
				public boolean validateDrop(Object target, int operation, TransferData transferType) {
					try {
						this.target = as(target, KeyValuePair.class);
						return true;
					} catch (Exception e) {}
					return false;
				}
				@Override
				public boolean performDrop(Object data) {
					try {
						List<KeyValuePair<ID, Integer>> items = DefinitionListEditor.this.array.childCollection();
						int c = items.indexOf(this.target);
						if (c == -1)
							c = items.size();
						
						PluginTransferData d = (PluginTransferData) data;
						ByteBuffer b = ByteBuffer.wrap(d.getData());
						int[] selection = new int[d.getData().length/4];
						for (int i = 0; i < selection.length; i++)
							selection[i] = b.getInt(i*4);
						@SuppressWarnings("unchecked")
						KeyValuePair<ID, Integer>[] draggedItems = new KeyValuePair[selection.length];
						for (int i = selection.length-1; i >= 0; i--) {
							draggedItems[i] = items.get(selection[i]);
							if (selection[i] < c)
								c--;
						}
						List<KeyValuePair<ID, Integer>> draggedItemsList = Arrays.asList(draggedItems);
						items.removeAll(draggedItemsList);
						items.addAll(c, draggedItemsList);
						DefinitionListEditor.this.viewer.refresh();
						return true;
					} catch (Exception e) {
						e.printStackTrace();
					}
					return false;
				}
			});
			viewer.setContentProvider(new IStructuredContentProvider() {
				@Override
				public void inputChanged(Viewer viewer, Object oldInput, Object newInput) {}
				@Override
				public void dispose() {}
				@Override
				public Object[] getElements(Object inputElement) {
					return DefinitionListEditor.this.array.components().toArray();
				}
			});
			class LabelProvider extends ClonkLabelProvider implements ITableLabelProvider {
				@Override
				public String getText(Object element) {
					return getColumnText(element, 1);
				}
				@Override
				public Image getImage(Object element) {
					return super.getImage(element);
				}
				@Override
				public Image getColumnImage(Object element, int columnIndex) {
					@SuppressWarnings("unchecked")
					KeyValuePair<ID, Integer> kv = (KeyValuePair<ID, Integer>) element;
					switch (columnIndex) {
					case 0:
						Definition def = scenario.nearestDefinitionWithId(kv.key());
						return imageFor(def);
					default:
						return null;
					}
				}
				@Override
				public String getColumnText(Object element, int columnIndex) {
					@SuppressWarnings("unchecked")
					KeyValuePair<ID, Integer> kv = (KeyValuePair<ID, Integer>) element;
					switch (columnIndex) {
					case 1:
						Definition def = scenario.nearestDefinitionWithId(kv.key());
						return def != null ? def.name() : kv.key().toString();
					case 2:
						return kv.value().toString();
					default:
						return null;
					}
				}
			};
			viewer.setLabelProvider(new LabelProvider());
			viewer.setInput(array);
			return viewer;
		}
		
		protected void addDefinitions() {
			final List<Definition> defs = new LinkedList<Definition>();
			for (Index i : scenario.index().relevantIndexes())
				i.allDefinitions(new Sink<Definition>() {
					@Override
					public void receivedObject(Definition item) {
						defs.add(item);
					}
					@Override
					public boolean filter(Definition item) {
						for (KeyValuePair<ID, Integer> kv : array.components())
							if (kv.key().equals(item.id()))
								return false;
						return definitionFilter.test(item);
					}
				});
			OpenDefinitionDialog chooser = new OpenDefinitionDialog(defs);
			chooser.setImageStore(new IConverter<Definition, Image>() {
				@Override
				public Image convert(Definition from) {
					return imageFor(from);
				}
			});
			chooser.setTitle(Messages.ScenarioProperties_AddDefinitionTitle);
			chooser.setInitialPattern(".", FilteredItemsSelectionDialog.FULL_SELECTION); //$NON-NLS-1$
			switch (chooser.open()) {
			case Window.OK:
				for (Definition d : chooser.selectedDefinitions()) {
					KeyValuePair<ID, Integer> kv = array.find(d.id());
					if (kv != null)
						kv.setValue(kv.value()+1);
					else
						array.add(d.id(), 1);
				}
				viewer.refresh();
				break;
			}
		}

		public void copyFrom(DefinitionListEditor other) {
			array.components().clear();
			for (int i = 0; i < other.array.components().size(); i++)
				try {
					array.add((KeyValuePair<ID, Integer>) other.array.components().get(i).clone());
				} catch (CloneNotSupportedException e) {
					e.printStackTrace();
				}
			viewer.refresh();
		}
	}
	
	private class EntrySlider implements SelectionListener, ModifyListener, VerifyListener {
		private final String section, entry;
		private Scale scale(Composite parent, String label, int index) {
			new Label(parent, SWT.NULL).setText(label);
			Scale scale = new Scale(parent, SWT.HORIZONTAL);
			scale.addSelectionListener(this);
			scale.setMinimum(0);
			scale.setMaximum(100);
			scale.setSelection(value(index));
			scale.setData(index);
			GridData scaleLayoutData = new GridData(GridData.FILL_HORIZONTAL);
			scaleLayoutData.grabExcessHorizontalSpace = true;
			scale.setLayoutData(scaleLayoutData);
			return scale;
		}
		private Text text(Composite parent, String label, int index) {
			new Label(parent, SWT.NULL).setText(label);
			Text text = new Text(parent, SWT.SINGLE);
			text.setText(String.valueOf(value(index)));
			text.setData(index);
			text.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));
			text.addModifyListener(this);
			text.addVerifyListener(this);
			return text;
		}
		public EntrySlider(Composite parent, int style, String section, String entry, String label) {
			this.section = section;
			this.entry = entry;
			Group group = new Group(parent, SWT.SHADOW_IN);
			group.setLayoutData(new GridData(GridData.FILL_BOTH));
			group.setText(label);
			group.setLayout(new GridLayout(2, false));
			
			scale(group, "Standard", 0);
			scale(group, "Random", 1);
			Composite c = new Composite(group, SWT.NULL);
			c.setLayout(new GridLayout(4, false));
			GridData layoutData = new GridData(GridData.FILL_BOTH);
			layoutData.horizontalSpan = 2;
			layoutData.grabExcessHorizontalSpace = true;
			c.setLayoutData(layoutData);
			text(c, "Min", 2);
			text(c, "Max", 3);
		}
		@Override
		public void widgetSelected(SelectionEvent e) {
			setValue((Integer) e.widget.getData(), ((Scale)e.widget).getSelection());
		}
		@Override
		public void modifyText(ModifyEvent e) {
			try {
				setValue((Integer)e.widget.getData(), Integer.parseInt(((Text)e.widget).getText()));
			} catch (NumberFormatException x) {}
		}
		@Override
		public void verifyText(VerifyEvent e) {
			try {
				Integer.parseInt(((Text)e.widget).getText());
			} catch (Exception x) {
				e.doit = false;
			}
			e.doit = true;
		}
		private void setValue(int index, int value) {
			IniSection s = scenarioConfiguration.sectionWithName(section, true);
			IniItem i = s.subItemByKey(entry);
			if (i == null) {
				int[] values = new int[4];
				values[index] = value;
				s.addItem(i = new ComplexIniEntry(-1, -1, entry, new IntegerArray(values)));
			} else
				((IntegerArray)((ComplexIniEntry)i).value()).values()[index].setSummedValue(value);
		}
		private int value(int index) {
			IniEntry e = scenarioConfiguration.entryInSection(section, entry);
			return e instanceof ComplexIniEntry && ((ComplexIniEntry)e).value() instanceof IntegerArray
				? ((IntegerArray)((ComplexIniEntry)e).value()).values()[index].summedValue() : 0;
		}
		@Override
		public void widgetDefaultSelected(SelectionEvent e) {
			widgetSelected(e);
		}
	}
	
	public Slider makeValueSlider(Composite parent, String label, String section, String entry) {
		Label l = new Label(parent, SWT.NULL);
		l.setText(label);
		Slider s = new Slider(parent, SWT.NULL);
		return s;
	}
	
	public ScenarioProperties() {
		// TODO Auto-generated constructor stub
	}

	private TabFolder tabs;
	
	private Composite tab(String title) {
		TabItem tab = new TabItem(tabs, SWT.NULL);
		tab.setText(title);
		Composite composite = new Composite(tabs, SWT.NO_SCROLL);
		tab.setControl(composite);
		return composite;
	}
	
	@Override
	public void setElement(IAdaptable element) {
		scenario = Scenario.get((IContainer)element.getAdapter(IContainer.class));
		scenarioConfiguration = scenario.scenarioConfiguration();
		scenario.engine().specialRules().processScenarioConfiguration(scenarioConfiguration, ScenarioConfigurationProcessing.Load);
	}
	
	public DefinitionListEditor listEditorFor(Composite parent, String sectionName, String entryName, String friendlyName) {
		ComplexIniEntry entry;
		try {
			entry = as(scenarioConfiguration.sectionWithName(sectionName, false).subItemByKey(entryName), ComplexIniEntry.class);
			if (entry == null)
				throw new NullPointerException();
		} catch (NullPointerException itemCreation) {
			try {
				IniSection section = scenarioConfiguration.sectionWithName(sectionName, true);
				IniItem item = section.subItemByKey(entryName);
				if (item == null)
					section.addItem(item = new ComplexIniEntry(-1, -1, entryName, new IDArray()));
				entry = (ComplexIniEntry)item;
			} catch (Exception fail) {
				return null;
			}
		}
		if (entry.value() instanceof IDArray)
			return new DefinitionListEditor(friendlyName, parent, entry);
		else
			return null;
	}
	
	public EntrySlider slider(Composite parent, String section, String entry, String label) {
		return new EntrySlider(parent, SWT.HORIZONTAL, section, entry, label);
	}
	
	private Composite[] makeLanes(Composite parent) {
		Composite leftLane = new Composite(parent, SWT.NO_SCROLL);
		leftLane.setLayoutData(new GridData(GridData.FILL_BOTH));
		leftLane.setLayout(new GridLayout(2, false));
		Composite rightLane = new Composite(parent, SWT.NO_SCROLL);
		rightLane.setLayoutData(new GridData(GridData.FILL_BOTH));
		rightLane.setLayout(new GridLayout(2, false));
		return new Composite[] {leftLane, rightLane};
	}
	
	@Override
	protected Control createContents(Composite parent) {
		tabs = new TabFolder(parent, SWT.BORDER);
		Composite game = tab(Messages.ScenarioProperties_GameTab);
		game.setLayout(new GridLayout(2, false));
		{
			Composite[] lanes = makeLanes(game);
			listEditorFor(lanes[0], "Game", "Goals", Messages.ScenarioProperties_Goals); //$NON-NLS-1$ //$NON-NLS-2$
			listEditorFor(lanes[1], "Game", "Rules", Messages.ScenarioProperties_Rules); //$NON-NLS-1$ //$NON-NLS-2$
		}
		Composite equipment = tab(Messages.ScenarioProperties_EquipmentTab);
		TabFolder players = new TabFolder(equipment, SWT.BORDER);
		players.setLayoutData(new GridData(GridData.FILL_BOTH));
		final Map<Integer, List<DefinitionListEditor>> playerEditors = new HashMap<Integer, List<DefinitionListEditor>>();
		for (int i = 1; i <= 4; i++) {
			final int playerIndex = i;
			TabItem tab = new TabItem(players, SWT.NULL);
			tab.setText(String.format(Messages.ScenarioProperties_PlayerTabFormat, i));
			final Composite composite = new Composite(players, SWT.NULL);
			tab.setControl(composite);
			composite.setLayout(new GridLayout(2, false));
			composite.setLayoutData(new GridData(GridData.FILL_BOTH));
			String section = String.format("Player%d", i); //$NON-NLS-1$
			final List<DefinitionListEditor> thisPlayerEditors = new ArrayList<ScenarioProperties.DefinitionListEditor>(8);
			playerEditors.put(playerIndex, thisPlayerEditors);
			final Composite[] simpleLanes = makeLanes(composite);
			thisPlayerEditors.addAll(Arrays.asList(
				listEditorFor(simpleLanes[0], section, "Crew", Messages.ScenarioProperties_PlayerCrew), //$NON-NLS-1$
				listEditorFor(simpleLanes[1], section, "Buildings", Messages.ScenarioProperties_PlayerBuildings), //$NON-NLS-1$
				listEditorFor(simpleLanes[0], section, "Material", Messages.ScenarioProperties_PlayerMaterial), //$NON-NLS-1$
				listEditorFor(simpleLanes[1], section, "Vehicles", Messages.ScenarioProperties_PlayerVehicles) //$NON-NLS-1$
			));
			final Composite[] extendedLanes = makeLanes(composite);
			thisPlayerEditors.addAll(Arrays.asList(
				listEditorFor(extendedLanes[0], section, "Knowledge", Messages.ScenarioProperties_Knowledge), //$NON-NLS-1$
				listEditorFor(extendedLanes[1], section, "Magic", Messages.ScenarioProperties_Magic), //$NON-NLS-1$
				listEditorFor(extendedLanes[0], section, "HomeBaseMaterial", Messages.ScenarioProperties_HomeBaseMaterial), //$NON-NLS-1$
				listEditorFor(extendedLanes[1], section, "HomeBaseProduction", Messages.ScenarioProperties_HomeBaseProduction) //$NON-NLS-1$
			));
			Composite buttons = new Composite(composite, SWT.NO_SCROLL);
			buttons.setLayout(new GridLayout(2, false));
			buttons.setLayoutData(new GridData(SWT.LEFT, SWT.TOP, false, false, 2, 1));
			Button copyToOthers = new Button(buttons, SWT.PUSH);
			copyToOthers.setText(Messages.ScenarioProperties_CopyToOtherPlayers);
			copyToOthers.addSelectionListener(new SelectionAdapter() {
				@Override
				public void widgetSelected(SelectionEvent e) {
					for (Map.Entry<Integer, List<DefinitionListEditor>> editors : playerEditors.entrySet())
						if (editors.getKey() != playerIndex)
							for (int i = 0; i < thisPlayerEditors.size(); i++)
								editors.getValue().get(i).copyFrom(thisPlayerEditors.get(i));
				}
			});
			final Button extended = new Button(buttons, SWT.PUSH);
			extended.addSelectionListener(new SelectionAdapter() {
				final String[] extendedText = new String[] {Messages.ScenarioProperties_PlayerExtended, Messages.ScenarioProperties_PlayerSimple};
				boolean extendedOn = false;
				void setLanesVisible(Composite[] lanes, boolean visible) {
					for (Composite l : lanes) {
						l.setVisible(visible);
						((GridData)l.getLayoutData()).exclude = !visible;
					}
					composite.layout();
				}
				void setText() {
					extended.setText(extendedText[extendedOn ? 1 : 0]);
					composite.layout();
				}
				@Override
				public void widgetSelected(SelectionEvent e) {
					extendedOn = !extendedOn;
					setLanesVisible(simpleLanes, !extendedOn);
					setLanesVisible(extendedLanes, extendedOn);
					setText();
				}
				
				{ setLanesVisible(extendedLanes, false); setText(); }
			});
		}
		equipment.setLayout(new GridLayout(2, false));

		Composite landscape = tab(Messages.ScenarioProperties_LandscapeTab);
		Composite environment = tab(Messages.ScenarioProperties_EnvironmentTab);
		environment.setLayout(new GridLayout(2, false));
		{
			Composite[] lanes = makeLanes(environment);
			listEditorFor(lanes[0], "Animals", "Animal", Messages.ScenarioProperties_Animals); //$NON-NLS-1$ //$NON-NLS-2$
			listEditorFor(lanes[1], "Animals", "Nest", Messages.ScenarioProperties_Nests); //$NON-NLS-1$ //$NON-NLS-2$
			listEditorFor(lanes[0], "Landscape", "Vegetation", Messages.ScenarioProperties_Vegetation); //$NON-NLS-1$ //$NON-NLS-2$
			listEditorFor(lanes[1], "Landscape", "InEarth", Messages.ScenarioProperties_InEarth); //$NON-NLS-1$ //$NON-NLS-2$
		}
		Composite weather = tab(Messages.ScenarioProperties_WeatherTab);
		weather.setLayout(new GridLayout(2, false));
		{
			slider(weather, "Weather", "Climate", Messages.ScenarioProperties_Climate); //$NON-NLS-1$ //$NON-NLS-2$
			slider(weather, "Weather", "StartSeason", Messages.ScenarioProperties_Season); //$NON-NLS-1$ //$NON-NLS-2$
			
			slider(weather, "Disasters", "Earthquake", Messages.ScenarioProperties_Earthquake); //$NON-NLS-1$ //$NON-NLS-2$
			slider(weather, "Disasters", "Volcano", Messages.ScenarioProperties_Volcano); //$NON-NLS-1$ //$NON-NLS-2$
			slider(weather, "Disasters", "Meteorite", Messages.ScenarioProperties_Meteorite); //$NON-NLS-1$ //$NON-NLS-2$
			slider(weather, "Landscape", "Gravity", Messages.ScenarioProperties_Gravity); //$NON-NLS-1$ //$NON-NLS-2$
		}
		
		tabs.addSelectionListener(new SelectionAdapter() {
			{tabs.setSelection(Core.instance().getPreferenceStore().getInt(SCENARIO_PROPERTIES_MAIN_TAB_PREF));}
			@Override
			public void widgetSelected(SelectionEvent e) {
				Core.instance().getPreferenceStore().setValue(SCENARIO_PROPERTIES_MAIN_TAB_PREF, tabs.getSelectionIndex());
			}
		});
		
		return tabs;
	}
	
	@Override
	public boolean performOk() {
		scenario.engine().specialRules().processScenarioConfiguration(scenarioConfiguration, ScenarioConfigurationProcessing.Save);
		scenarioConfiguration.save(true);
		return super.performOk();
	}
	
	@Override
	public boolean performCancel() {
		scenarioConfiguration.parser().parse(false, true);
		return super.performCancel();
	}
	
	@Override
	public void dispose() {
		try {
			for (Image i : images.values())
				if (i != null)
					i.dispose();
		} catch (Exception e) {
			e.printStackTrace();
		}
		super.dispose();
	}

}
