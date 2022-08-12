package com.cubrid.common.ui.swt.table.celleditor;

import org.eclipse.jface.viewers.ComboBoxCellEditor;
import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.CCombo;
import org.eclipse.swt.widgets.Composite;

public class EditableComboBoxCellEditor extends ComboBoxCellEditor {
	
	private String inputString = "";
	
	public EditableComboBoxCellEditor(Composite parent, String[] items, int style) {
		super(parent, items, style);
	}
	
	public EditableComboBoxCellEditor(Composite parent, String[] items) {
		super(parent, items, SWT.NONE);
	}
	
    @Override
    protected Object doGetValue() {
        final Object value = super.doGetValue();
        if (value instanceof Integer && (Integer) value == -1) {
            inputString = ((CCombo) getControl()).getText();
        }
        return value;
    }

    public String getInputString() {
        return inputString;
    }
}
