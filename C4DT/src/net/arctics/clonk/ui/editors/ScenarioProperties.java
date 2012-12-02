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
import net.arctics.clonk.mapcreator.ClassicMapCreator;
import net.arctics.clonk.mapcreator.MapCreator;
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
import org.eclipse.jface.viewers.CellEditor;
import org.eclipse.jface.viewers.ColumnWeightData;
import org.eclipse.jface.viewers.EditingSupport;
import org.eclipse.jface.viewers.IStructuredContentProvider;
import org.eclipse.jface.viewers.ITableLabelProvider;
import org.eclipse.jface.viewers.StyledCellLabelProvider;
import org.eclipse.jface.viewers.TableViewer;
import org.eclipse.jface.viewers.TableViewerColumn;
import org.eclipse.jface.viewers.TextCellEditor;
import org.eclipse.jface.viewers.Viewer;
import org.eclipse.jface.viewers.ViewerCell;
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
import org.eclipse.swt.graphics.ImageData;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Slider;
import org.eclipse.swt.widgets.Spinner;
import org.eclipse.swt.widgets.TabFolder;
import org.eclipse.swt.widgets.TabItem;
import org.eclipse.swt.widgets.Table;
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
	private Image mapPreviewImage;
	private int mapPreviewNumPlayers = 1;
	
	private Image imageForSlider(String entryName) {
		return scenarioConfiguration.engine().image(entryName);
	}
	
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

		private final IDArray array;
		private Table table;
		private TableViewer viewer;
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
			this.viewer = createViewer();
			table.setHeaderVisible(true);
			table.setLinesVisible(true);
			TableViewerColumn imageColumn = new TableViewerColumn(viewer, SWT.LEFT);
			imageColumn.setLabelProvider(new StyledCellLabelProvider() {
				@Override
				public void update(ViewerCell cell) {
					@SuppressWarnings("unchecked")
					KeyValuePair<ID, Integer> kv = (KeyValuePair<ID, Integer>) cell.getElement();
					Definition def = scenario.nearestDefinitionWithId(kv.key());
					cell.setImage(def != null ? imageFor(def) : null);
				}
			});
			TableViewerColumn defColumn = new TableViewerColumn(viewer, SWT.LEFT);
			defColumn.setLabelProvider(new StyledCellLabelProvider() {
				@Override
				public void update(ViewerCell cell) {
					@SuppressWarnings("unchecked")
					KeyValuePair<ID, Integer> kv = (KeyValuePair<ID, Integer>) cell.getElement();
					Definition def = scenario.nearestDefinitionWithId(kv.key());
					cell.setText(def != null ? def.name() : kv.key().toString());
				}
			});
			defColumn.setEditingSupport(new EditingSupport(viewer) {
				@Override
				protected void setValue(Object element, final Object value) {
					class DefFinder extends Sink<Definition> {
						public Definition found;
						@Override
						public void receivedObject(Definition item) {
							if (
								(item.name() != null && item.name().equals(value)) ||
								(item.id() != null && item.id().stringValue().equals(value))
							) {
								found = item;
								decision(Decision.AbortIteration);
							}
						}
					}
					DefFinder finder = new DefFinder();
					scenario.index().allDefinitions(finder);
					if (finder.found != null && array.find(finder.found.id()) == null) {
						@SuppressWarnings("unchecked")
						KeyValuePair<ID, Integer> kv = (KeyValuePair<ID, Integer>)element;
						int index = array.components().indexOf(kv);
						array.components().remove(index);
						array.components().add(index, new KeyValuePair<ID, Integer>(finder.found.id(), kv.value()));
						viewer.refresh();
					}
				}
				@SuppressWarnings("unchecked")
				@Override
				protected Object getValue(Object element) {
					KeyValuePair<ID, Integer> kv = (KeyValuePair<ID, Integer>) element;
					Definition def = scenario.nearestDefinitionWithId(kv.key());
					return def != null ? def.name() : kv.key().toString();
				}
				@Override
				protected CellEditor getCellEditor(Object element) {
					return new TextCellEditor(viewer.getTable());
				}
				@Override
				protected boolean canEdit(Object element) {
					return true;
				}
			});
			defColumn.getColumn().setText(Messages.ScenarioProperties_Definition);
			TableViewerColumn countColumn = new TableViewerColumn(viewer, SWT.RIGHT);
			countColumn.getColumn().setText(Messages.ScenarioProperties_CountColumn);
			countColumn.setEditingSupport(new EditingSupport(viewer) {
				@SuppressWarnings("unchecked")
				@Override
				protected void setValue(Object element, Object value) {
					((KeyValuePair<ID, Integer>)element).setValue(Integer.parseInt(value.toString()));
					viewer.update(element, null);
				}
				@SuppressWarnings("unchecked")
				@Override
				protected Object getValue(Object element) {
					return ((KeyValuePair<ID, Integer>)element).value().toString();
				}
				@Override
				protected CellEditor getCellEditor(Object element) {
					return new TextCellEditor(viewer.getTable());
				}
				@Override
				protected boolean canEdit(Object element) {
					return true;
				}
			});
			countColumn.setLabelProvider(new StyledCellLabelProvider() {
				@Override
				public void update(ViewerCell cell) {
					@SuppressWarnings("unchecked")
					KeyValuePair<ID, Integer> kv = (KeyValuePair<ID, Integer>) cell.getElement();
					cell.setText(kv.value().toString());
				}
			});
			tableLayout.setColumnData(imageColumn.getColumn(), new ColumnWeightData(10));
			tableLayout.setColumnData(defColumn.getColumn(), new ColumnWeightData(80));
			tableLayout.setColumnData(countColumn.getColumn(), new ColumnWeightData(10));
			
			Composite buttons = new Composite(parent, SWT.NO_SCROLL);
			buttons.setLayoutData(new GridData(SWT.RIGHT, SWT.TOP, false, false, 1, 1));
			GridLayout buttonsLayout = noMargin(new GridLayout(1, true));
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
			this.array = (IDArray)entry.value();
			this.definitionFilter = entry.engine().specialRules().configurationEntryDefinitionFilter(entry);
			this.viewer.setInput(array);
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
	
	public static GridLayout noMargin(GridLayout layout) {
		layout.marginWidth = 0;
		layout.marginHeight = 0;
		layout.marginLeft = 0;
		layout.marginRight = 0;
		layout.marginBottom = 0;
		layout.marginTop = 0;
		layout.verticalSpacing = 0;
		return layout;
	}
	
	private class EntrySlider implements SelectionListener, ModifyListener, VerifyListener {
		private final String section, entry;
		private Runnable updateRunnable;
		public void setUpdateRunnable(Runnable updateRunnable) {
			this.updateRunnable = updateRunnable;
		}
		private Spinner spinner(final Composite parent, final String label, int index, boolean labelPostfix, String toolTipText) {
			Runnable r = new Runnable()
				{ @Override public void run()
					{new Label(parent, SWT.NULL).setText(label);}};
			if (!labelPostfix)
				r.run();
			Spinner spinner = new Spinner(parent, SWT.NULL);
			spinner.addSelectionListener(this);
			spinner.setMinimum(-100);
			spinner.setMaximum(+100);
			spinner.setSelection(value(index));
			spinner.setData(index);
			GridData scaleLayoutData = new GridData(GridData.FILL_HORIZONTAL);
			scaleLayoutData.grabExcessHorizontalSpace = true;
			spinner.setLayoutData(scaleLayoutData);
			spinner.setToolTipText(toolTipText);
			if (labelPostfix)
				r.run();
			return spinner;
		}
		public EntrySlider(Composite parent, int style, String section, String entry, String label) {
			this.section = section;
			this.entry = entry;
			Image img = imageForSlider(entry);
			Composite group = new Composite(parent, SWT.NO_SCROLL);
			group.setLayout(noMargin(new GridLayout(img != null ? 2 : 1, false)));
			group.setLayoutData(new GridData(GridData.FILL_BOTH));
			if (img != null) {
				Label icon = new Label(group, SWT.BORDER);
				icon.setImage(img);
			}
			Composite spinners = new Composite(group, SWT.NO_SCROLL);
			Label lbl = new Label(spinners, SWT.NULL);
			lbl.setText(label);
			lbl.setLayoutData(new GridData(SWT.LEFT, SWT.LEFT, true, false, 8, 1));
			spinners.setLayout(noMargin(new GridLayout(8, false)));
			spinner(spinners, "⠛", 0, false, Messages.ScenarioProperties_Standard); //$NON-NLS-1$
			spinner(spinners, "δ", 1, false, Messages.ScenarioProperties_Random); //$NON-NLS-1$
			spinner(spinners, "[", 2, false, Messages.ScenarioProperties_Minimum); //$NON-NLS-1$
			spinner(spinners, "]", 3, true, Messages.ScenarioProperties_Maximum); //$NON-NLS-1$
		}
		@Override
		public void widgetSelected(SelectionEvent e) {
			Integer ndx = (Integer) e.widget.getData();
			setValue(ndx, ((Spinner)e.widget).getSelection());
			if (updateRunnable != null)
				updateRunnable.run();
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
			} else {
				IntegerArray ints = (IntegerArray)((ComplexIniEntry)i).value();
				if (ints.values().length <= index)
					ints.grow(index+1);
				ints.values()[index].setSummedValue(value);
			}
		}
		private int value(int index) {
			IniEntry e = scenarioConfiguration.entryInSection(section, entry);
			try {
				return e instanceof ComplexIniEntry && ((ComplexIniEntry)e).value() instanceof IntegerArray
					? ((IntegerArray)((ComplexIniEntry)e).value()).values()[index].summedValue() : 0;
			} catch (IndexOutOfBoundsException bounds) {
				return 0;
			}
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
	private Label mapPreviewLabel;
	private Button screenSizeCheckbox;
	private Button layersCheckbox;
	
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
					item = section.addItem(new ComplexIniEntry(-1, -1, entryName, new IDArray()));
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
		makeGameTab();
		makeEquipmentTab();
		makeLandscapeTab();
		makeEnvironmentTab();
		makeWeatherTab();
		
		tabs.addSelectionListener(new SelectionAdapter() {
			{tabs.setSelection(Core.instance().getPreferenceStore().getInt(SCENARIO_PROPERTIES_MAIN_TAB_PREF));}
			@Override
			public void widgetSelected(SelectionEvent e) {
				Core.instance().getPreferenceStore().setValue(SCENARIO_PROPERTIES_MAIN_TAB_PREF, tabs.getSelectionIndex());
			}
		});
		return tabs;
	}

	private void makeWeatherTab() {
		Composite weather = tab(Messages.ScenarioProperties_WeatherTab);
		GridLayout weatherLayout = new GridLayout(2, false);
		weather.setLayout(weatherLayout);
		{
			slider(weather, "Weather", "Climate", Messages.ScenarioProperties_Climate); //$NON-NLS-1$ //$NON-NLS-2$
			slider(weather, "Disasters", "Earthquake", Messages.ScenarioProperties_Earthquake); //$NON-NLS-1$ //$NON-NLS-2$
			slider(weather, "Weather", "StartSeason", Messages.ScenarioProperties_Season); //$NON-NLS-1$ //$NON-NLS-2$
			slider(weather, "Disasters", "Volcano", Messages.ScenarioProperties_Volcano); //$NON-NLS-1$ //$NON-NLS-2$
			slider(weather, "Weather", "YearSpeed", Messages.ScenarioProperties_YearSpeed); //$NON-NLS-1$ //$NON-NLS-2$
			slider(weather, "Disasters", "Meteorite", Messages.ScenarioProperties_Meteorite); //$NON-NLS-1$ //$NON-NLS-2$
			slider(weather, "Weather", "Rain", Messages.ScenarioProperties_Rain); //$NON-NLS-1$ //$NON-NLS-2$
			slider(weather, "Landscape", "Gravity", Messages.ScenarioProperties_Gravity); //$NON-NLS-1$ //$NON-NLS-2$
			slider(weather, "Weather", "Lightning", Messages.ScenarioProperties_Lightning); //$NON-NLS-1$ //$NON-NLS-2$
			slider(weather, "Weather", "Wind", Messages.ScenarioProperties_Wind); //$NON-NLS-1$ //$NON-NLS-2$
		}
	}

	private void makeEnvironmentTab() {
		Composite environment = tab(Messages.ScenarioProperties_EnvironmentTab);
		environment.setLayout(new GridLayout(2, false));
		{
			Composite[] lanes = makeLanes(environment);
			listEditorFor(lanes[0], "Animals", "Animal", Messages.ScenarioProperties_Animals); //$NON-NLS-1$ //$NON-NLS-2$
			listEditorFor(lanes[1], "Animals", "Nest", Messages.ScenarioProperties_Nests); //$NON-NLS-1$ //$NON-NLS-2$
			listEditorFor(lanes[0], "Landscape", "Vegetation", Messages.ScenarioProperties_Vegetation); //$NON-NLS-1$ //$NON-NLS-2$
			listEditorFor(lanes[1], "Landscape", "InEarth", Messages.ScenarioProperties_InEarth); //$NON-NLS-1$ //$NON-NLS-2$
		}
	}

	private void makeLandscapeTab() {
		Composite landscape = tab(Messages.ScenarioProperties_LandscapeTab);
		{
			landscape.setLayout(new GridLayout(3, false));
			Composite options = new Composite(landscape, SWT.NULL);
			{
				options.setLayout(new GridLayout(1, false));
				EntrySlider[] sliders = new EntrySlider[] {
					slider(options, "Landscape", "MapWidth", Messages.ScenarioProperties_MapWidth), //$NON-NLS-1$ //$NON-NLS-2$
					slider(options, "Landscape", "MapHeight", Messages.ScenarioProperties_MapHeight), //$NON-NLS-1$ //$NON-NLS-2$
					slider(options, "Landscape", "MapZoom", Messages.ScenarioProperties_MapZoom), //$NON-NLS-1$ //$NON-NLS-2$
					slider(options, "Landscape", "Amplitude", Messages.ScenarioProperties_Amplitude), //$NON-NLS-1$ //$NON-NLS-2$
					slider(options, "Landscape", "Phase", Messages.ScenarioProperties_Phase), //$NON-NLS-1$ //$NON-NLS-2$
					slider(options, "Landscape", "Period", Messages.ScenarioProperties_Period), //$NON-NLS-1$ //$NON-NLS-2$
					slider(options, "Landscape", "Random", Messages.ScenarioProperties_Random), //$NON-NLS-1$ //$NON-NLS-2$
					slider(options, "Landscape", "LiquidLevel", Messages.ScenarioProperties_LiquidLevel) //$NON-NLS-1$ //$NON-NLS-2$
				};
				Runnable r = new Runnable() {
					@Override
					public void run() { drawMapPreviewFailsafe(); }
				};
				for (EntrySlider s : sliders)
					s.setUpdateRunnable(r);
			}
			Composite preview = new Composite(landscape, SWT.NO_SCROLL);
			{
				preview.setLayout(new GridLayout(1, false));
				new Label(preview, SWT.NULL).setText(Messages.ScenarioProperties_DynamicMap);
				mapPreviewLabel = new Label(preview, SWT.BORDER);
			}
			Composite displayOptions = new Composite(landscape, SWT.NO_SCROLL);
			{
				displayOptions.setLayout(new GridLayout(1, false));
				displayOptions.setLayoutData(new GridData(GridData.BEGINNING));
				SelectionAdapter redrawPreview = new SelectionAdapter() {
					@Override
					public void widgetSelected(SelectionEvent e) {
						drawMapPreviewFailsafe();
					}
				};
				screenSizeCheckbox = new Button(displayOptions, SWT.CHECK);
				screenSizeCheckbox.setText(Messages.ScenarioProperties_ScreenSize);
				screenSizeCheckbox.addSelectionListener(redrawPreview);
				layersCheckbox = new Button(displayOptions, SWT.CHECK);
				layersCheckbox.setSelection(true);
				layersCheckbox.setText(Messages.ScenarioProperties_Layers);
				layersCheckbox.addSelectionListener(redrawPreview);
				new Label(displayOptions, SWT.NULL).setText(Messages.ScenarioProperties_PreviewFor);
				SelectionAdapter playerPreviewNumListener = new SelectionAdapter() {
					@Override
					public void widgetSelected(SelectionEvent e) {
						mapPreviewNumPlayers = (Integer)e.widget.getData();
						drawMapPreviewFailsafe();
					}
				};
				for (int i = 1; i <= 4; i++) {
					Button b = new Button(displayOptions, SWT.RADIO);
					b.setText(String.format(Messages.ScenarioProperties_PlayerMapPreviewFormat, i));
					b.setData(i);
					b.addSelectionListener(playerPreviewNumListener);
				}
			}
		}
		drawMapPreviewFailsafe();
	}

	private void makeEquipmentTab() {
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
			buttons.setLayout(noMargin(new GridLayout(2, false)));
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
	}

	private void makeGameTab() {
		Composite game = tab(Messages.ScenarioProperties_GameTab);
		game.setLayout(new GridLayout(2, false));
		{
			Composite[] lanes = makeLanes(game);
			listEditorFor(lanes[0], "Game", "Goals", Messages.ScenarioProperties_Goals); //$NON-NLS-1$ //$NON-NLS-2$
			listEditorFor(lanes[1], "Game", "Rules", Messages.ScenarioProperties_Rules); //$NON-NLS-1$ //$NON-NLS-2$
		}
	}

	private void drawMapPreviewFailsafe() {
		try {
			drawMapPreview();
		} catch (Exception e) {
			e.printStackTrace(); // oh well
		}
	}
	
	private void drawMapPreview() {
		if (mapPreviewImage != null) {
			mapPreviewImage.dispose();
			mapPreviewImage = null;
		}
		MapCreator mapCreator = new ClassicMapCreator();
		ImageData data;
		try {
			data = mapCreator.create(scenarioConfiguration, layersCheckbox.getSelection(), mapPreviewNumPlayers);
		} catch (Exception e) {
			e.printStackTrace();
			return;
		}
		Image small = new Image(Display.getCurrent(), data);
		try {
			Image scaled = new Image(Display.getCurrent(), 300, 300);
			GC gc = new GC(scaled);
			try {
				gc.setAntialias(SWT.ON);
				gc.setInterpolation(SWT.HIGH);
				gc.drawImage(small, 0, 0, small.getBounds().width, small.getBounds().height, 0, 0,
					scaled.getBounds().width, scaled.getBounds().height);
			} finally {
				gc.dispose();
			}
			mapPreviewImage = scaled;
			if (mapPreviewLabel != null)
				mapPreviewLabel.setImage(mapPreviewImage);
		} finally {
			small.dispose();
		}
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
		if (mapPreviewImage != null) {
			mapPreviewImage.dispose();
			mapPreviewImage = null;
		}
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
