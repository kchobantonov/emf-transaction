/**
 * Copyright (c) 2007 IBM Corporation and others.
 * This program and the accompanying materials are made
 * available under the terms of the Eclipse Public License 2.0
 * which is available at https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *   IBM - Initial API and implementation
 */
package org.eclipse.emf.workspace.util.tests;

import java.util.Collections;

import junit.framework.Test;
import junit.framework.TestSuite;

import org.eclipse.core.resources.ResourceAttributes;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.emf.common.command.Command;
import org.eclipse.emf.examples.extlibrary.Book;
import org.eclipse.emf.transaction.RollbackException;
import org.eclipse.emf.transaction.Transaction;
import org.eclipse.emf.transaction.TransactionalEditingDomain;
import org.eclipse.emf.transaction.util.TransactionUtil;
import org.eclipse.emf.workspace.tests.AbstractTest;
import org.eclipse.emf.workspace.tests.fixtures.TestCommand;
import org.eclipse.emf.workspace.util.WorkspaceValidateEditSupport;


/**
 * Tests validate-edit support.
 *
 * @author Christian W. Damus (cdamus)
 */
public class ValidateEditTest extends AbstractTest {
    
    private static final String newTitle = "New Title"; //$NON-NLS-1$
    
    private Book book;
    
    private final Command cmd = new TestCommand() {
        public void execute() {
            try {
                book.setTitle(newTitle);
            } catch (Exception e) {
                fail(e);
            }
        }};
	
	public ValidateEditTest(String name) {
		super(name);
	}
	
	public static Test suite() {
		return new TestSuite(ValidateEditTest.class, "Validate-Edit Support Tests"); //$NON-NLS-1$
	}

	/**
	 * A control test for a scenario in which validateEdit will find all
	 * resources to be modifiable.
	 */
	public void test_noValidateEditRequired() {
        try {
            getCommandStack().execute(cmd, null);
            
            assertTitleChanged();
            assertResourceDirty();
        } catch (Exception e) {
            fail(e);
        }
	}

    /**
     * Simple unmodifiable resource scenario.
     */
    public void ignore_test_validateEditRollback() {
        setResourceReadOnly();
        
        try {
            getCommandStack().execute(cmd, null);
            
            fail("Should have rolled back"); //$NON-NLS-1$
        } catch (RollbackException e) {
            // success
            System.out.println("Got expected exception: " + e.getLocalizedMessage()); //$NON-NLS-1$
        } catch (Exception e) {
            fail(e);
        }
        
        assertTitleNotChanged();
        assertResourceNotDirty();
    }
	
	//
	// Fixture methods
	//
	
	@Override
	protected void doSetUp()
		throws Exception {
		
		super.doSetUp();
		
		setValidateEdit();
		
		// workspace validate-edit implementation depends on mod tracking
		testResource.setTrackingModification(true);
		
        startReading();
        book = (Book) find("root/Root Book"); //$NON-NLS-1$
        commit();
        assertNotNull(book);
	}
	
	@Override
	protected void doTearDown()
		throws Exception {
		
		book = null;
		
		super.doTearDown();
	}
	
	void setResourceReadOnly() {
        ResourceAttributes attr = new ResourceAttributes();
        attr.setReadOnly(true);
        
        try {
            file.setResourceAttributes(attr);
        } catch (CoreException e) {
            fail(e);
        }
	}
	
	void setValidateEdit() {
        TransactionalEditingDomain.DefaultOptions defaults = TransactionUtil
            .getAdapter(domain, TransactionalEditingDomain.DefaultOptions.class);
        
        defaults.setDefaultTransactionOptions(Collections.singletonMap(
            Transaction.OPTION_VALIDATE_EDIT, new WorkspaceValidateEditSupport()));
	}
	
	void assertTitleChanged() {
	    assertEquals(newTitle, book.getTitle());
	}
	
	void assertTitleNotChanged() {
	    assertFalse(newTitle.equals(book.getTitle()));
	}
	
	void assertResourceDirty() {
	    assertTrue("Resource not dirty", testResource.isModified()); //$NON-NLS-1$
	}
    
    void assertResourceNotDirty() {
        assertFalse("Resource is dirty", testResource.isModified()); //$NON-NLS-1$
    }
}
