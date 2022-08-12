
package com.cubrid.cubridmigration.ui.wizard.page;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.eclipse.jface.dialogs.PageChangedEvent;
import org.eclipse.jface.dialogs.PageChangingEvent;
import org.eclipse.jface.viewers.CellEditor;
import org.eclipse.jface.viewers.ColumnWeightData;
import org.eclipse.jface.viewers.ICellModifier;
import org.eclipse.jface.viewers.ILabelProviderListener;
import org.eclipse.jface.viewers.IStructuredContentProvider;
import org.eclipse.jface.viewers.ITableLabelProvider;
import org.eclipse.jface.viewers.TableLayout;
import org.eclipse.jface.viewers.TableViewer;
import org.eclipse.jface.viewers.Viewer;
import org.eclipse.jface.viewers.ViewerCell;
import org.eclipse.swt.SWT;
import org.eclipse.swt.graphics.Color;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.layout.FillLayout;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Group;
import org.eclipse.swt.widgets.TableColumn;
import org.eclipse.swt.widgets.TableItem;

import com.cubrid.common.ui.swt.table.celleditor.EditableComboBoxCellEditor;
import com.cubrid.cubridmigration.core.common.CUBRIDVersionUtils;
import com.cubrid.cubridmigration.core.dbobject.Catalog;
import com.cubrid.cubridmigration.core.dbobject.Schema;
import com.cubrid.cubridmigration.core.engine.config.MigrationConfiguration;
import com.cubrid.cubridmigration.ui.MigrationUIPlugin;
import com.cubrid.cubridmigration.ui.message.Messages;
import com.cubrid.cubridmigration.ui.wizard.MigrationWizard;

public class SchemaMappingPage extends MigrationWizardPage {
	
	public static final Image CHECK_IMAGE = MigrationUIPlugin.getImage("icon/checked.gif");
	public static final Image UNCHECK_IMAGE = MigrationUIPlugin.getImage("icon/unchecked.gif");
	
	private String[] propertyList = {Messages.sourceSchema, Messages.msgSrcType, Messages.targetSchema, Messages.msgTarType};
	private String[] tarSchemaNameArray =  null;
	
	Catalog srcCatalog;
	Catalog tarCatalog;
		
	TableViewer srcTableViewer = null;
	TableViewer tarTableViewer = null;
	
	ArrayList<String> tarSchemaNameList = new ArrayList<String>();
	
	ArrayList<SrcTable> srcTableList = new ArrayList<SrcTable>();
	ArrayList<TarTable> tarTableList = new ArrayList<TarTable>();
	
	List<Schema> srcSchemaList = null;
	List<Schema> tarSchemaList = null;
	
	EditableComboBoxCellEditor comboEditor = null;
	
	private boolean firstVisible = true;
	
	private long createTime = 0;
	
	protected class SrcTable {
		private boolean isSelected;
		
		private String srcSchema;
		private String srcDBType;

		private String tarSchema;
		private String tarDBType;
		
		public boolean isSelected() {
			return isSelected;
		}
		
		public void setSelected(boolean isSelected) {
			this.isSelected = isSelected;
		}
		
		public String getSrcSchema() {
			return srcSchema;
		}
		
		public void setSrcSchema(String srcSchema) {
			this.srcSchema = srcSchema;
		}
		
		public String getSrcDBType() {
			return srcDBType;
		}

		public void setSrcDBType(String srcDBtype) {
			this.srcDBType = srcDBtype;
		}
		
		public String getTarSchema() {
			return tarSchema;
		}

		public void setTarSchema(String tarSchema) {
			this.tarSchema = tarSchema;
		}

		public String getTarDBType() {
			return tarDBType;
		}

		public void setTarDBType(String tarDBType) {
			this.tarDBType = tarDBType;
		}
	}
	
	protected class TarTable {
		private String tarSchema;
		private String tarDBType;

		public String getTarSchema() {
			return tarSchema;
		}
		public void setTarSchema(String tarSchema) {
			this.tarSchema = tarSchema;
		}
		public String getTarDBType() {
			return tarDBType;
		}
		public void setTarDBType(String tarDBType) {
			this.tarDBType = tarDBType;
		}
	}
	
	public SchemaMappingPage(String pageName) {
		super(pageName);
	}
	
	@Override
	public void createControl(Composite parent) {
		Composite container = new Composite(parent, SWT.NONE);
		container.setLayout(new FillLayout());
		container.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));
		
		createSrcTable(container);
		
		setControl(container);
		
	}
	
	private void createSrcTable(Composite container) {
		Group srcTableViewerGroup = new Group(container, SWT.NONE);
		srcTableViewerGroup.setLayout(new FillLayout());
		srcTableViewer = new TableViewer(srcTableViewerGroup, SWT.FULL_SELECTION);
		
		srcTableViewer.setContentProvider(new IStructuredContentProvider() {
			@Override
			public Object[] getElements(Object inputElement) {
				if (inputElement instanceof List) {
					
					@SuppressWarnings("unchecked")
					List<SrcTable> schemaObj = (ArrayList<SrcTable>) inputElement;
					
					return schemaObj.toArray();
				} else {
					return new Object[0];
				}
			}
			
			@Override
			public void inputChanged(Viewer viewer, Object oldInput, Object newInput) {}
			
			@Override
			public void dispose() {}
			
		});
		
		srcTableViewer.setLabelProvider(new ITableLabelProvider() {
			@Override
			public String getColumnText(Object element, int columnIndex) {
				SrcTable obj = (SrcTable) element;
				
				switch (columnIndex) {
				case 0:
					return obj.getSrcSchema();
				case 1:
					return obj.getSrcDBType();
				case 2:
					return obj.getTarSchema();
				case 3:
					return obj.getTarDBType();
					
				default:
					return null;
						
				}
			}
			
			@Override
			public Image getColumnImage(Object element, int columnIndex) {
				return null;
			}
			
			@Override
			public void removeListener(ILabelProviderListener listener) {}
			
			@Override
			public boolean isLabelProperty(Object element, String property) {return false;}
			
			@Override
			public void dispose() {}
			
			@Override
			public void addListener(ILabelProviderListener listener) {}
			
		});
		
		srcTableViewer.setColumnProperties(propertyList);
		
		TableLayout tableLayout = new TableLayout();
		
		tableLayout.addColumnData(new ColumnWeightData(25, true));
		tableLayout.addColumnData(new ColumnWeightData(25, true));
		tableLayout.addColumnData(new ColumnWeightData(25, true));
		tableLayout.addColumnData(new ColumnWeightData(25, true));
		
		srcTableViewer.getTable().setLayout(tableLayout);
		srcTableViewer.getTable().setLinesVisible(true);
		srcTableViewer.getTable().setHeaderVisible(true);
		
		TableColumn col2 = new TableColumn(srcTableViewer.getTable(), SWT.LEFT);
		TableColumn col3 = new TableColumn(srcTableViewer.getTable(), SWT.LEFT);
		TableColumn col4 = new TableColumn(srcTableViewer.getTable(), SWT.LEFT);
		TableColumn col5 = new TableColumn(srcTableViewer.getTable(), SWT.LEFT);
		
		col2.setText(propertyList[0]);
		col3.setText(propertyList[1]);
		col4.setText(propertyList[2]);
		col5.setText(propertyList[3]);
		
	}
	
	private void getSchemaValues() {
		
		MigrationWizard mw = getMigrationWizard();
		
		Catalog catalog = mw.getTargetCatalog();
		
		List<Schema> schemaList = catalog.getSchemas();

		tarSchemaNameList = new ArrayList<String>();
		
		for (Schema schema : schemaList) {
			tarSchemaNameList.add(schema.getName());
		}
		
		if (catalog.isDBAGroup()) {
			String[] schemaNameArray = tarSchemaNameList.toArray(new String[] {});
			
			tarSchemaNameArray = new String[schemaNameArray.length + 1];
			
			tarSchemaNameArray[0] = "";
			
			System.arraycopy(schemaNameArray, 0, tarSchemaNameArray, 1, schemaNameArray.length);
		} else {
			tarSchemaNameArray = new String[] {catalog.getConnectionParameters().getConUser()};
			CUBRIDVersionUtils.getInstance().setTargetMultiSchema(false);
		}
	}
	
	private void setEditor() {
		comboEditor = new EditableComboBoxCellEditor(srcTableViewer.getTable(), tarSchemaNameArray);
		
		CellEditor[] editors = new CellEditor[] {
				null,
				null,
				comboEditor,
				null
		};

		
		if (!tarCatalog.isDBAGroup()) {
			return;
		}
		
		srcTableViewer.setCellEditors(editors);
		srcTableViewer.setCellModifier(new ICellModifier() {
			
			@Override
			public void modify(Object element, String property, Object value) {
				
				TableItem tabItem = (TableItem) element;
				SrcTable srcTable = (SrcTable) tabItem.getData();
				
				if (property.equals(propertyList[2])) {
					srcTable.setTarSchema(returnValue((Integer) value, tabItem));
				}
				
				srcTableViewer.refresh();
			}
			
			@Override
			public Object getValue(Object element, String property) {
				if (property.equals(propertyList[2])) {
					return returnIndex(element);
				} else {
					return null;
				}
			}
			
			@Override
			public boolean canModify(Object element, String property) {
				if (property.equals(propertyList[2])) {
					return true;
				} else {
					return false;
				}
			}
			
			public int returnIndex(Object element) {
				if (element instanceof SrcTable) {
					SrcTable srcTable = (SrcTable) element;
					
					for (int i = 0; i < tarSchemaNameArray.length; i++) {
						if (tarSchemaNameArray[i].equals(srcTable.getTarSchema())) {						
							return i;
						}
					}
				}
				
				return 0;
			}
			public String returnValue(int index, TableItem item) {
				
				if (index != -1) {
					return tarSchemaNameArray[index];
				} else {
					return isSchemaNameValid(comboEditor.getInputString());
				}
			}
			
			public String isSchemaNameValid(String schemaName) {
				
				//CMT112 : need alert window 
				
				Pattern pattern = Pattern.compile("^[a-zA-Z_]*$");
				Matcher matcher = pattern.matcher(schemaName);
				
				boolean isValid = matcher.matches();
				
				System.out.println(isValid);
				
				if (isValid){
					return schemaName;
				} else {
					return "";
				}
			}
		});
	}
	
	private void setData() {
		final MigrationWizard mw = getMigrationWizard();
		srcCatalog = mw.getSourceCatalog();
		tarCatalog = mw.getTargetCatalog();
		
		setSchemaData();
	}
	
	private void setSchemaData() {
		//TODO: extract schema names and DB type
		srcSchemaList = srcCatalog.getSchemas();
		tarSchemaList = tarCatalog.getSchemas();
				
		for (Schema schema : srcSchemaList) {
			SrcTable srcTable = new SrcTable();
			srcTable.setSrcDBType(srcCatalog.getDatabaseType().getName());
			srcTable.setSrcSchema(schema.getName());
			
			
			if (tarCatalog.isDBAGroup()) {
				srcTable.setTarSchema(schema.getName());
			} else {
				srcTable.setTarSchema(tarCatalog.getConnectionParameters().getConUser().toUpperCase());
			}
			srcTable.setTarDBType(tarCatalog.getDatabaseType().getName());
			
			srcTableList.add(srcTable);
		}
	}
	
	@Override
	protected void afterShowCurrentPage(PageChangedEvent event) {
		// TODO need reset when select different target connection
		if (firstVisible) {
			setTitle(Messages.schemaMappingPageTitle);
			setDescription(Messages.schemaMappingPageDescription);
			
			setData();
			getSchemaValues();
			setEditor();
			
			srcTableViewer.setInput(srcTableList);
			
			firstVisible = false;
		}
	}
	
	protected boolean isTargetChanged() {
		MigrationWizard mw = getMigrationWizard();
		Catalog catalog = mw.getTargetCatalog();
		
		long newCreatedTime = catalog.getCreateTime();
		
		if (createTime == 0) {
			createTime = newCreatedTime;
			return false;
		}
		if (createTime != newCreatedTime) {
			return true;
		}
		
		return false;
	}
	
	@Override
	protected void handlePageLeaving(PageChangingEvent event) {
		if (!isPageComplete()) {
			return;
		}
		if (isGotoNextPage(event)) {
			event.doit = saveSelectedTable();
		}
	}
	//TODO: return false only
	private boolean saveSelectedTable() {
		
		MigrationConfiguration cfg = getMigrationWizard().getMigrationConfig();
		
		for (SrcTable srcTable : srcTableList) {
			for (Schema schema : srcSchemaList) {
				if (srcTable.getSrcSchema().equals(schema.getName())) {
					schema.setTargetSchemaName(srcTable.getTarSchema());
					
					
					//CMT112 println is test code. will remove later or pr version
					System.out.println("src schema : " + srcTable.getSrcSchema());
					System.out.println("tar schema : " + srcTable.getTarSchema());
					
					if (!tarSchemaNameList.contains(srcTable.getTarSchema())) {
						System.out.println("need to create a new schema for target db");
						
						schema.setNewTargetSchema(true);
						
						cfg.setNewTargetSchema(srcTable.getTarSchema());
					}
					
					System.out.println("------------------------------------------");
					
				}
			}
		}
		
		return true;
	}
}
