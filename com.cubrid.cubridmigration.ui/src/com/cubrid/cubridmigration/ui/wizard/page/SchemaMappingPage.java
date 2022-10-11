
package com.cubrid.cubridmigration.ui.wizard.page;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.apache.log4j.Logger;
import org.eclipse.jface.dialogs.MessageDialog;
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
import org.eclipse.jface.viewers.TextCellEditor;
import org.eclipse.jface.viewers.Viewer;
import org.eclipse.swt.SWT;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.layout.FillLayout;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Group;
import org.eclipse.swt.widgets.TableColumn;
import org.eclipse.swt.widgets.TableItem;

import com.cubrid.common.ui.swt.table.celleditor.EditableComboBoxCellEditor;
import com.cubrid.cubridmigration.core.common.CUBRIDVersionUtils;
import com.cubrid.cubridmigration.core.common.log.LogUtil;
import com.cubrid.cubridmigration.core.dbobject.Catalog;
import com.cubrid.cubridmigration.core.dbobject.Schema;
import com.cubrid.cubridmigration.core.engine.config.MigrationConfiguration;
import com.cubrid.cubridmigration.ui.message.Messages;
import com.cubrid.cubridmigration.ui.wizard.MigrationWizard;

public class SchemaMappingPage extends MigrationWizardPage {
	
	private Logger logger = LogUtil.getLogger(SchemaMappingPage.class);
	
	private MigrationWizard wizard = null;
	private MigrationConfiguration config = null;
	
	private String[] propertyList = {Messages.sourceSchema, Messages.msgNote, Messages.msgSrcType, Messages.targetSchema, Messages.msgTarType};
	private String[] tarSchemaNameArray =  null;
	
	Catalog srcCatalog;
	Catalog tarCatalog;
		
	TableViewer srcTableViewer = null;
	TableViewer tarTableViewer = null;
	
	ArrayList<String> tarSchemaNameList = new ArrayList<String>();
	
	ArrayList<SrcTable> srcTableList = new ArrayList<SrcTable>();
	
	List<Schema> srcSchemaList = null;
	List<Schema> tarSchemaList = null;
	
	EditableComboBoxCellEditor comboEditor = null;
	
	TextCellEditor textEditor = null;
	
	private boolean firstVisible = true;
	
	private CUBRIDVersionUtils verUtil = CUBRIDVersionUtils.getInstance();
	
	protected class SrcTable {
		private boolean isSelected;
		
		private String note;		
				
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
		
		public String getNote() {
			return note;
		}
		
		public void setNote(String note){
			this.note = note;
		}
		
		public void setNote(boolean note) {
			if (note == true) {
				setNote(Messages.msgGrantSchema);
			} else {
				setNote(Messages.msgMainSchema);
			}
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
					return obj.getNote();
				case 2:
					return obj.getSrcDBType();
				case 3:
					return obj.getTarSchema();
				case 4:
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
		
		tableLayout.addColumnData(new ColumnWeightData(20, true));
		tableLayout.addColumnData(new ColumnWeightData(13, true));
		tableLayout.addColumnData(new ColumnWeightData(20, true));
		tableLayout.addColumnData(new ColumnWeightData(20, true));
		tableLayout.addColumnData(new ColumnWeightData(20, true));
		
		srcTableViewer.getTable().setLayout(tableLayout);
		srcTableViewer.getTable().setLinesVisible(true);
		srcTableViewer.getTable().setHeaderVisible(true);
		
		TableColumn col2 = new TableColumn(srcTableViewer.getTable(), SWT.LEFT);
		TableColumn col3 = new TableColumn(srcTableViewer.getTable(), SWT.LEFT);
		TableColumn col4 = new TableColumn(srcTableViewer.getTable(), SWT.LEFT);
		TableColumn col5 = new TableColumn(srcTableViewer.getTable(), SWT.LEFT);
		TableColumn col6 = new TableColumn(srcTableViewer.getTable(), SWT.LEFT);
		
		col2.setText(propertyList[0]);
		col3.setText(propertyList[1]);
		col4.setText(propertyList[2]);
		col5.setText(propertyList[3]);
		col6.setText(propertyList[4]);
		
//		Canvas canvas = new Canvas(srcTableViewer.getTable(), SWT.NONE);
//		canvas.setSize(200, 200);
//		canvas.setLocation(100, 100);
//		
//		GC gc = new GC(canvas);
//		gc.drawString("sample text", 100, 100);
		
	}
	
	private void getSchemaValues() {
		
		Catalog targetCatalog = wizard.getTargetCatalog();
		Catalog sourceCatalog = wizard.getSourceCatalog();
		
		List<Schema> targetSchemaList = targetCatalog.getSchemas();
		List<Schema> sourceSchemaList = sourceCatalog.getSchemas();
		

		tarSchemaNameList = new ArrayList<String>();
		ArrayList<String> dropDownSchemaList = new ArrayList<String>();
		
		
		for (Schema schema : targetSchemaList) {
			tarSchemaNameList.add(schema.getName());
			dropDownSchemaList.add(schema.getName());
		}
		
		for (Schema schema : sourceSchemaList) {
//			if (schema.getName().equalsIgnoreCase("DBA") || schema.getName().equalsIgnoreCase("PUBLIC")) {
//				continue;
//			}
			
			if (tarSchemaNameList.contains(schema.getName().toUpperCase())) {
				continue;
			}
			
			dropDownSchemaList.add(schema.getName());
		}
		
//		dropDownSchemaList.add(0, Messages.msgDefaultSchema);
		
		if (targetCatalog.isDBAGroup()) {
//			String[] schemaNameArray = dropDownSchemaList.toArray(new String[] {});
//			tarSchemaNameArray = new String[schemaNameArray.length + 1];
//			tarSchemaNameArray[0] = "";
//			tarSchemaNameArray = new String[schemaNameArray.length];
			
			tarSchemaNameArray = dropDownSchemaList.toArray(new String[] {});
			
//			System.arraycopy(schemaNameArray, 0, tarSchemaNameArray, 0, schemaNameArray.length);
		} else {
			tarSchemaNameArray = new String[] {targetCatalog.getConnectionParameters().getConUser()};
			
			CUBRIDVersionUtils verUtil = CUBRIDVersionUtils.getInstance();
			
 			verUtil.setTargetMultiSchema(false);
		}
	}
	
	private void setOnlineEditor() {
		comboEditor = new EditableComboBoxCellEditor(srcTableViewer.getTable(), tarSchemaNameArray);
		
		CellEditor[] editors = new CellEditor[] {
				null,
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
				
				if (property.equals(propertyList[3])) {
					srcTable.setTarSchema(returnValue((Integer) value, tabItem));
				}
				
				srcTableViewer.refresh();
			}
			
			@Override
			public Object getValue(Object element, String property) {
				if (property.equals(propertyList[3])) {
					return returnIndex(element);
				} else {
					return null;
				}
			}
			
			@Override
			public boolean canModify(Object element, String property) {
				if (property.equals(propertyList[3])) {
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
					String testValue = item.getText();
					
					MessageDialog.openError(getShell(), Messages.msgError, "This schema does not exist");
					
					return testValue;
				}
			}
		});
	}
	
	private void setOfflineEditor() {
		textEditor = new TextCellEditor(srcTableViewer.getTable());
		
		CellEditor[] editors = new CellEditor[] {
				null,
				null,
				null,
				textEditor,
				null
		};
		
		srcTableViewer.setCellEditors(editors);
		srcTableViewer.setCellModifier(new ICellModifier() {

			@Override
			public boolean canModify(Object element, String property) {
				if (property.equals(propertyList[3])) {
					return true;
				} else {
					return false;
				}
			}

			@Override
			public Object getValue(Object element, String property) {
				if (property.equals(propertyList[3])) {
					return ((SrcTable) element).getTarSchema();
				} else {
					return null;
				}
			}

			@Override
			public void modify(Object element, String property, Object value) {
				TableItem tabItem = (TableItem) element;
				SrcTable srcTable = (SrcTable) tabItem.getData();
				
				if (property.equals(propertyList[3])) {
					srcTable.setTarSchema((String) value);
				}
				srcTableViewer.refresh();
			}
		});
	}
	
	private void setOfflineSchemaMappingPage() {
		setOfflineData();
		if (CUBRIDVersionUtils.getInstance().addUserSchema()) {
			setOfflineEditor();			
		}
	}
	
	private void setOfflineData() {
		srcCatalog = wizard.getSourceCatalog();
		srcSchemaList = srcCatalog.getSchemas();
		Map<String, String> scriptSchemaMap = config.getScriptSchemaMapping();
		
		for (Schema schema : srcSchemaList) {
			SrcTable srcTable = new SrcTable();
			srcTable.setSrcDBType(srcCatalog.getDatabaseType().getName());
			srcTable.setSrcSchema(schema.getName());
			srcTable.setNote(schema.isGrantorSchema());
			
			srcTable.setTarDBType(Messages.msgCubridDump);
			if (!schema.isGrantorSchema()) {
				srcTableList.add(0, srcTable);
			} else {
				srcTableList.add(srcTable);
			}
			
			if (scriptSchemaMap.size() != 0) {
				logger.info("offline script schema");
				srcTable.setTarSchema(scriptSchemaMap.get(srcTable.getSrcSchema()));
			} else {
				if (CUBRIDVersionUtils.getInstance().addUserSchema()) {
					srcTable.setTarSchema(Messages.msgTypeSchema);
				} else {
					srcTable.setTarSchema(Messages.msgUserSchemaDisable);
					verUtil.setTargetMultiSchema(false);
				}
			}
		}
	}
	
	private void setOnlineSchemaMappingPage() {
		setOnlineData();
		getSchemaValues();
		
		if (CUBRIDVersionUtils.getInstance().isTargetMultiSchema()) {
			setOnlineEditor();
		}
	}
	
	private void setOnlineData() {
		srcCatalog = wizard.getSourceCatalog();
		tarCatalog = wizard.getTargetCatalog();
		
		//TODO: extract schema names and DB type
		srcSchemaList = srcCatalog.getSchemas();
		tarSchemaList = tarCatalog.getSchemas();
		Map<String, String> scriptSchemaMap = config.getScriptSchemaMapping();
		
		for (Schema schema : srcSchemaList) {
			SrcTable srcTable = new SrcTable();
			srcTable.setSrcDBType(srcCatalog.getDatabaseType().getName());
			srcTable.setSrcSchema(schema.getName());
			srcTable.setNote(schema.isGrantorSchema());
			
			if (!schema.isGrantorSchema()) {
				srcTableList.add(0, srcTable);
			} else {
				srcTableList.add(srcTable);
			}
			
			srcTable.setTarDBType(tarCatalog.getDatabaseType().getName());
			
			if (scriptSchemaMap.size() != 0 && srcCatalog.getDatabaseType().getID() == 1) {
				logger.info("script schema");
				
				if (!verUtil.isSourceVersionOver112()) {
					srcTable.setTarSchema(scriptSchemaMap.get("").toUpperCase());
				} else {
					srcTable.setTarSchema(scriptSchemaMap.get(srcTable.getSrcSchema()).toUpperCase());
				}
				
				logger.info("srcTable target schema : " + srcTable.getTarSchema());
			} else {
				if (tarCatalog.isDBAGroup() && verUtil.isTargetVersionOver112()) {
					srcTable.setTarSchema(srcTable.getSrcSchema());
				} else {
					srcTable.setTarSchema(tarCatalog.getSchemas().get(0).getName());
				}
			}
		}
	}
	
	@Override
	protected void afterShowCurrentPage(PageChangedEvent event) {
		// TODO need reset when select different target connection
		wizard = getMigrationWizard();
		config = wizard.getMigrationConfig();
		
		if (firstVisible) {
			setTitle(Messages.schemaMappingPageTitle);
			setDescription(Messages.schemaMappingPageDescription);
			
			if (!config.targetIsOnline()) {
				setOfflineSchemaMappingPage();
			} else {
				setOnlineSchemaMappingPage();
			}
			
			srcTableViewer.setInput(srcTableList);
			
			firstVisible = false;
		}
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
		
		for (SrcTable srcTable : srcTableList) {
			for (Schema schema : srcSchemaList) {
				if (srcTable.getSrcSchema().equals(schema.getName())) {
					
					if (srcTable.getTarSchema().isEmpty() || isDefaultMessage(srcTable.getTarSchema())) {
						//CMT112 need alert dialog
						MessageDialog.openError(getShell(), Messages.msgError, Messages.msgErrEmptySchemaName);
						
						return false;
					}
					
					schema.setTargetSchemaName(srcTable.getTarSchema());
					
					logger.info("src schema : " + srcTable.getSrcSchema());
					logger.info("tar schema : " + srcTable.getTarSchema());
					
					if (!containsIgnoreCase(tarSchemaNameList, srcTable.getTarSchema())) {
						logger.info("need to create a new schema for target db");
						schema.setNewTargetSchema(true);
						config.setNewTargetSchema(srcTable.getTarSchema());
					} else {
						schema.setNewTargetSchema(false);
					}
					
					logger.info("------------------------------------------");
					
				}
			}
		}
		
		return true;
	}
	
	private boolean isDefaultMessage(String enterSchema) {
		if (enterSchema.equals(Messages.msgDefaultSchema) ||
				enterSchema.equals(Messages.msgTypeSchema)) {
			return true;
		}
		
		return false;
	}
	
	private boolean containsIgnoreCase(List<String> stringList, String schemaName) {
		for (String containSchemaName : stringList) {
			if (containSchemaName.equalsIgnoreCase(schemaName)) {
				return true;
			}
		}
		
		return false;
	}
}
