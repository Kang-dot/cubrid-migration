/*
 * Copyright (C) 2008 Search Solution Corporation.
 * Copyright (C) 2016 CUBRID Corporation.
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

import org.junit.Test;

public class SSHUtilsTest {

    static {
        PathUtils.initPaths();
    }

    @Test
    public void testSSH() throws Exception {
        // TODO: this depends on the detail test env, should configured by developer
        /*SSHHost host = new SSHHost();
        host.setAuthType(0);
        host.setHost("test-dbtool2");
        host.setPort(22);
        host.setUser("cubrid92");
        host.setPassword("cubrid92");
        Session session = SSHUtils.newSSHSession(host);
        String rfile = "/tmp/test.t";
        String lfile = PathUtils.getBaseTempDir() + "test.t";
        File file = new File(lfile);
        if (!file.exists()) {
        	PathUtils.createFile(file);
        }
        try {
        	SSHUtils.scpTo(session, lfile, rfile);
        	PathUtils.deleteFile(file);
        	SSHUtils.scpFrom(session, rfile, lfile);
        	Assert.assertTrue(file.exists());
        } finally {
        	PathUtils.deleteFile(file);
        }*/
    }
}
