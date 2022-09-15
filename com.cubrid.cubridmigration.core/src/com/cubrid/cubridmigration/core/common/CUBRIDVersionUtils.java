/*
 * Copyright (c) 2016 CUBRID Corporation.
 *
 * Redistribution and use in source and binary forms, with or without modification, 
 * are permitted provided that the following conditions are met: 
 *
 * - Redistributions of source code must retain the above copyright notice, 
 *   this list of conditions and the following disclaimer. 
 *
 * - Redistributions in binary form must reproduce the above copyright notice, 
 *   this list of conditions and the following disclaimer in the documentation 
 *   and/or other materials provided with the distribution. 
 *
 * - Neither the name of the <ORGANIZATION> nor the names of its contributors 
 *   may be used to endorse or promote products derived from this software without 
 *   specific prior written permission. 
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS" AND 
 * ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED 
 * WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE DISCLAIMED. 
 * IN NO EVENT SHALL THE COPYRIGHT OWNER OR CONTRIBUTORS BE LIABLE FOR ANY DIRECT, 
 * INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, 
 * BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, 
 * OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY, 
 * WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE) 
 * ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY 
 * OF SUCH DAMAGE. 
 *
 */
package com.cubrid.cubridmigration.core.common;

import java.util.ArrayList;

import com.cubrid.cubridmigration.core.dbobject.Catalog;

public class CUBRIDVersionUtils {
	private boolean isCUBRIDSource = false;
	
	private ArrayList<Boolean> hasMultiSchema = new ArrayList<Boolean>();
	
	private boolean isSourceVersionOver112 = false;
	private boolean isTargetVersionOver112 = false;
	
	private boolean addUserSchema = false;

	
	private CUBRIDVersionUtils () {}
	
	public CUBRIDVersionUtils (String version) {
		isVersionOver112(version);
	}

	private static class UtilHelper {
		private static final CUBRIDVersionUtils VERSION_UTIL = new CUBRIDVersionUtils();
	}
	
	public static CUBRIDVersionUtils getInstance() {
		return UtilHelper.VERSION_UTIL;
	}
	
	public boolean isCUBRIDSource() {
		return isCUBRIDSource;
	}

	public void setCUBRIDSource(boolean isCUBRIDSource) {
		this.isCUBRIDSource = isCUBRIDSource;
	}
	
	public boolean isSourceVersionOver112() {
		return isSourceVersionOver112;
	}
	
	public void setSourceVersionOver112(boolean isSourceVersionOver112) {
		this.isSourceVersionOver112 = isSourceVersionOver112;
	}
	
	public boolean isTargetVersionOver112() {
		return isTargetVersionOver112;
	}
	
	public void setTargetVersionOver112(boolean isTargetVersionOver112) {
		this.isTargetVersionOver112 = isTargetVersionOver112;
	}
	
	public void hasMultiSchema(boolean isMultiSchema) {
		if (hasMultiSchema.size() >= 2) {
			hasMultiSchema.set(1, isMultiSchema);
		}
		hasMultiSchema.add(isMultiSchema);
	}
	
	public boolean isTargetMultiSchema() {
		try {
			return hasMultiSchema.get(1);
		} catch (IndexOutOfBoundsException e) {
			return isSourceMultiSchema();
		}
	}
	
	public void setTargetMultiSchema(boolean isMultiSchema) {
		try {
			addUserSchema = isMultiSchema;
			if (hasMultiSchema.size() == 0) {
				hasMultiSchema.add(isMultiSchema);
			}
			
			hasMultiSchema.set(1, isMultiSchema);
		} catch (IndexOutOfBoundsException e) {
			hasMultiSchema.set(0, isMultiSchema);
		}
	}
	
	public boolean isSourceMultiSchema() {
		try {
			return hasMultiSchema.get(0);
		} catch (IndexOutOfBoundsException e) {
			return addUserSchema;
		}
	}
	
	public boolean isSourceVersionOver112(Object object){
		boolean over112 = false;
		
		if (object instanceof Catalog) {
			over112 = isVersionOver112((Catalog) object);
		} else if (object instanceof String) {
			over112 = isVersionOver112((String) object);			
		}
		
		setSourceVersionOver112(over112);
		
		return over112;
	}
	
	public boolean isTargetVersionOver112(Object object){
		boolean over112 = false;
		
		if (object instanceof Catalog) {
			over112 = isVersionOver112((Catalog) object);
		} else if (object instanceof String) {
			over112 = isVersionOver112((String) object);			
		}
		
		setTargetVersionOver112(over112);
		
		return over112;
	}
	
	public boolean isVersionOver112(Catalog catalog) {
		String version = catalog.getVersion().getDbProductVersion();
		
		String[] versionSplit = version.split("\\.");
		
		String stringVersion = (versionSplit[0] + versionSplit[1]);
		
		Integer versionNumber = Integer.parseInt(stringVersion);
		
		return versionNumber >= 112;
	}
	
	public boolean isVersionOver112(String version) {
		String[] versionSplit = version.split("\\.");
		
		String stringVersion = (versionSplit[0] + versionSplit[1]);
		
		Integer versionNumber = Integer.parseInt(stringVersion);
		
		return versionNumber >= 112;
	}
	
	public boolean addUserSchema() {
		return addUserSchema;
	}

	public void setAddUserSchema(boolean addOfflineUserSchema) {
		this.addUserSchema = addOfflineUserSchema;
	}
}
