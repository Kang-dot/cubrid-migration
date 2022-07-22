
package com.cubrid.cubridmigration.ui.wizard.page;

import java.util.ArrayList;
import java.util.List;

import org.eclipse.jface.dialogs.PageChangedEvent;
import org.eclipse.jface.dialogs.PageChangingEvent;
import org.eclipse.jface.viewers.CellEditor;
import org.eclipse.jface.viewers.ComboBoxCellEditor;
import org.eclipse.jface.viewers.ICellModifier;
import org.eclipse.jface.viewers.ILabelProviderListener;
import org.eclipse.jface.viewers.IStructuredContentProvider;
import org.eclipse.jface.viewers.ITableLabelProvider;
import org.eclipse.jface.viewers.TableViewer;
import org.eclipse.jface.viewers.Viewer;
import org.eclipse.swt.SWT;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Group;
import org.eclipse.swt.widgets.TableItem;

import com.cubrid.cubridmigration.core.dbobject.Catalog;
import com.cubrid.cubridmigration.core.dbobject.Schema;
import com.cubrid.cubridmigration.core.engine.config.MigrationConfiguration;
import com.cubrid.cubridmigration.core.engine.config.SourceEntryTableConfig;
import com.cubrid.cubridmigration.ui.wizard.MigrationWizard;

public class SchemaMappingPage extends MigrationWizardPage {
	
	private String[] propertyList = {"Source Schema", "Source Database Type", "Target Schema", "Target Database Type"};
	
	TableViewer mappingTable = null;
	
	ArrayList<String> tarSchemaNameList = null;
	
	protected class SchemaObject {
		private String srcSchema;
		private String srcDBtype;
		private String tarSchema;
		private String tarDBtype;
		
		public SchemaObject (String srcDBtype, String tarDBtype) {
			this.srcDBtype = srcDBtype;
			this.tarDBtype = tarDBtype;
		}
		
		public String getSrcSchema() {
			return srcSchema;
		}
		public void setSrcSchema(String srcSchema) {
			this.srcSchema = srcSchema;
		}
		public String getSrcDBtype() {
			return srcDBtype;
		}
		public void setSrcDBtype(String srcDBtype) {
			this.srcDBtype = srcDBtype;
		}
		public String getTarSchema() {
			return tarSchema;
		}
		public void setTarSchema(String tarSchema) {
			this.tarSchema = tarSchema;
		}
		public String getTarDBtype() {
			return tarDBtype;
		}
		public void setTarDBtype(String tarDBtype) {
			this.tarDBtype = tarDBtype;
		}
	}
	
	public SchemaMappingPage(String pageName) {
		super(pageName);
	}
	
	@Override
	public void createControl(Composite parent) {
		// TODO Auto-generated method stub
		
		Composite container = new Composite(parent, SWT.NONE);
		container.setLayout(new GridLayout());
		setControl(container);
		container.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));
		
		Group tableGroup = new Group(container, SWT.NONE);
		mappingTable = new TableViewer(tableGroup, SWT.NONE);
		
		mappingTable.setContentProvider(new IStructuredContentProvider() {
			
			@Override
			public Object[] getElements(Object inputElement) {
				if (inputElement instanceof SchemaObject) {
					
				} else {
					return new Object[0];
				}
				
				// TODO Auto-generated method stub
				return null;
			}
			
			@Override
			public void inputChanged(Viewer viewer, Object oldInput, Object newInput) {}
			
			@Override
			public void dispose() {}
			
		});
		
		mappingTable.setLabelProvider(new ITableLabelProvider() {
			@Override
			public String getColumnText(Object element, int columnIndex) {
				// TODO Auto-generated method stub
				SchemaObject obj = (SchemaObject) element;
				
				switch (columnIndex) {
				case 0:
					return obj.getSrcSchema();
				case 1:
					return obj.getSrcDBtype();
				case 2:
					return obj.getTarSchema();
				case 3:
					return obj.getTarDBtype();
					
				default:
					return null;
						
				}
			}
			
			@Override
			public void removeListener(ILabelProviderListener listener) {}
			
			@Override
			public boolean isLabelProperty(Object element, String property) {return false;}
			
			@Override
			public void dispose() {}
			
			@Override
			public void addListener(ILabelProviderListener listener) {}
			
			@Override
			public Image getColumnImage(Object element, int columnIndex) {return null;}
		});
		
		setCellEditor();
		
		mappingTable.setCellModifier(new ICellModifier() {
			
			@Override
			public void modify(Object element, String property, Object value) {
				// TODO Auto-generated method stub
				
				TableItem item = (TableItem) element;
				
				SchemaObject obj = (SchemaObject) item.getData();
				
				String newValue = (String) value;
				
				if (property.equals(propertyList[3])) {
					obj.setTarDBtype(newValue);
				}
				
				mappingTable.refresh();
			}
			
			@Override
			public Object getValue(Object element, String property) {
				// TODO Auto-generated method stub
				
				SchemaObject obj = (SchemaObject) element;
				
				if (property.equals(propertyList[3])) {
					return obj.getTarSchema();
				} else {
					return null;
				}
			}
			
			@Override
			public boolean canModify(Object element, String property) {
				// TODO Auto-generated method stub
				
				if (property.equals(propertyList[3])){
					return true;
				} else {
					return false;
				}
			}
		});
		
		mappingTable.setColumnProperties(propertyList);
		
		
	}
	
	private void setSchemaData(Catalog srcCatalog, Catalog tarCatalog) {
		//TODO: extract schema names and DB type
		srcCatalog.getSchemas();
		tarCatalog.getSchemas();
		
		String srcDBtype = srcCatalog.getVersion().getDbProductName();
		String tarDBtype = tarCatalog.getVersion().getDbProductName();
		
		List<Schema> srcSchemaNameList = srcCatalog.getSchemas();
		List<Schema> tarSchemaList = tarCatalog.getSchemas();
		
		
		
	}
	
	private void setCellEditor() {
		CellEditor[] editors = new CellEditor[4];
		editors[0] = null;
		editors[1] = null;
		editors[2] = new ComboBoxCellEditor(mappingTable.getTable(), getTargetSchemaList());
		editors[3] = null;
		
		mappingTable.setCellEditors(editors);
	}
	
	private String[] getTargetSchemaList() {
		
		return propertyList;
	}
	
	@Override
	protected void afterShowCurrentPage(PageChangedEvent event) {
		// TODO Auto-generated method stub
		final MigrationWizard mw = getMigrationWizard();
		setTitle("Schema mapping page");
		setDescription("mapping the schema");
		
		MigrationConfiguration cfg = mw.getMigrationConfig();
		Catalog srcCatalog = mw.getSourceCatalog();
		Catalog tarCatalog = mw.getTargetCatalog();
		
	}
	
	@Override
	protected void handlePageLeaving(PageChangingEvent event) {
		// TODO Auto-generated method stub
	}
}
