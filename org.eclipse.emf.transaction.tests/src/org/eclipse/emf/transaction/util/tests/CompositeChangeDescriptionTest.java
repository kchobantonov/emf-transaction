/**
 * <copyright>
 *
 * Copyright (c) 2005 IBM Corporation and others.
 * All rights reserved.   This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *   IBM - Initial API and implementation
 *
 * </copyright>
 *
 * $Id: CompositeChangeDescriptionTest.java,v 1.3 2006/01/30 19:47:50 cdamus Exp $
 */
package org.eclipse.emf.transaction.util.tests;

import java.util.Iterator;

import junit.framework.Test;
import junit.framework.TestSuite;

import org.eclipse.emf.common.notify.Notifier;
import org.eclipse.emf.ecore.change.ChangeDescription;
import org.eclipse.emf.ecore.change.impl.ChangeDescriptionImpl;
import org.eclipse.emf.ecore.change.util.ChangeRecorder;
import org.eclipse.emf.ecore.resource.ResourceSet;
import org.eclipse.emf.ecore.resource.impl.ResourceSetImpl;
import org.eclipse.emf.examples.extlibrary.Book;
import org.eclipse.emf.examples.extlibrary.EXTLibraryFactory;
import org.eclipse.emf.examples.extlibrary.Library;
import org.eclipse.emf.transaction.TransactionChangeDescription;
import org.eclipse.emf.transaction.tests.AbstractTest;
import org.eclipse.emf.transaction.util.CompositeChangeDescription;

/**
 * Tests the {@link CompositeChangeDescription} class.
 *
 * @author Christian W. Damus (cdamus)
 */
public class CompositeChangeDescriptionTest extends AbstractTest {
	private ResourceSet rset;
	private CompositeChangeDescription change;
	private ChangeRecorder recorder;
	
	public CompositeChangeDescriptionTest(String name) {
		super(name);
	}

	public static Test suite() {
		return new TestSuite(CompositeChangeDescriptionTest.class, "Composite Change Description Tests"); //$NON-NLS-1$
	}
	
	/**
	 * Tests the accumulation of object changes.
	 */
	public void test_objectChanges() {
		Book book = (Book) find("root/Root Book"); //$NON-NLS-1$
		assertNotNull(book);
		root.setName("New root name"); //$NON-NLS-1$
		
		ChangeDescription change1 = recorder.endRecording();
		change.add(change1);
		
		assertTrue(change.getObjectChanges().containsAll(change1.getObjectChanges()));
		
		recorder.beginRecording(null, rset.getResources());
		
		root.getBooks().remove(book);

		ChangeDescription change2 = recorder.endRecording();
		change.add(change2);
		
		assertTrue(change.getObjectChanges().containsAll(change2.getObjectChanges()));
		
		// haven't lost change1's changes
		assertTrue(change.getObjectChanges().containsAll(change1.getObjectChanges()));
	}

	/**
	 * Tests the accumulation of objects to attach.
	 */
	public void test_objectsToAttach() {
		Library newRoot = EXTLibraryFactory.eINSTANCE.createLibrary();
		testResource.getContents().add(newRoot);
		
		ChangeDescription change1 = recorder.endRecording();
		change.add(change1);
		
		assertTrue(change.getObjectsToAttach().containsAll(change1.getObjectsToAttach()));
		
		recorder.beginRecording(null, rset.getResources());
		
		Book newBook = EXTLibraryFactory.eINSTANCE.createBook();
		newRoot.getBooks().add(newBook);

		ChangeDescription change2 = recorder.endRecording();
		change.add(change2);
		
		assertTrue(change.getObjectsToAttach().containsAll(change2.getObjectsToAttach()));
		
		// haven't lost change1's changes
		assertTrue(change.getObjectsToAttach().containsAll(change1.getObjectsToAttach()));
	}

	/**
	 * Tests the accumulation of objects to detach.
	 */
	public void test_objectsToDetach() {
		Book book = (Book) find("root/Root Book"); //$NON-NLS-1$
		assertNotNull(book);
		root.getBooks().remove(book);
		
		ChangeDescription change1 = recorder.endRecording();
		change.add(change1);
		
		assertTrue(change.getObjectsToDetach().containsAll(change1.getObjectsToDetach()));
		
		recorder.beginRecording(null, rset.getResources());
		
		testResource.getContents().remove(root);

		ChangeDescription change2 = recorder.endRecording();
		change.add(change2);
		
		assertTrue(change.getObjectsToDetach().containsAll(change2.getObjectsToDetach()));
		
		// haven't lost change1's changes
		assertTrue(change.getObjectsToDetach().containsAll(change1.getObjectsToDetach()));
	}

	/**
	 * Tests the accumulation of resource changes.
	 */
	public void test_resourceChanges() {
		Library newRoot = EXTLibraryFactory.eINSTANCE.createLibrary();
		testResource.getContents().add(newRoot);
		
		ChangeDescription change1 = recorder.endRecording();
		change.add(change1);
		
		assertTrue(change.getResourceChanges().containsAll(change1.getResourceChanges()));
		
		recorder.beginRecording(null, rset.getResources());
		
		testResource.getContents().remove(root);

		ChangeDescription change2 = recorder.endRecording();
		change.add(change2);
		
		assertTrue(change.getResourceChanges().containsAll(change2.getResourceChanges()));
		
		// haven't lost change1's changes
		assertTrue(change.getResourceChanges().containsAll(change1.getResourceChanges()));
	}

	/**
	 * Tests the apply() method.
	 */
	public void test_apply() {
		Book book = (Book) find("root/Root Book"); //$NON-NLS-1$
		assertNotNull(book);
		String oldName = root.getName();
		root.setName("New root name"); //$NON-NLS-1$
		
		ChangeDescription change1 = recorder.endRecording();
		change.add(change1);
		
		recorder.beginRecording(null, rset.getResources());
		
		root.getBooks().remove(book);

		ChangeDescription change2 = recorder.endRecording();
		change.add(change2);
		
		change.apply();
		
		// check that the previous state was restored
		assertEquals(oldName, root.getName());
		assertTrue(root.getBooks().contains(book));
	}

	/**
	 * Tests the applyAndReverse() method.
	 */
	public void test_applyAndReverse() {
		Book book = (Book) find("root/Root Book"); //$NON-NLS-1$
		assertNotNull(book);
		String oldName = root.getName();
		String newName = "New root name"; //$NON-NLS-1$
		root.setName(newName);
		
		ChangeDescription change1 = recorder.endRecording();
		change.add(change1);
		
		recorder.beginRecording(null, rset.getResources());
		
		root.getBooks().remove(book);

		ChangeDescription change2 = recorder.endRecording();
		change.add(change2);
		
		change.applyAndReverse();
		
		// check that the previous state was restored
		assertEquals(oldName, root.getName());
		assertTrue(root.getBooks().contains(book));
		
		change.applyAndReverse();
		
		// check that the next state was restored
		assertEquals(newName, root.getName());
		assertFalse(root.getBooks().contains(book));
		
		change.applyAndReverse();
		
		// check that the first state was restored
		assertEquals(oldName, root.getName());
		assertTrue(root.getBooks().contains(book));
	}
	
	/**
	 * Tests the canApply() method.
	 */
	public void test_canApply() {
		Book book = (Book) find("root/Root Book"); //$NON-NLS-1$
		assertNotNull(book);
		root.setName("New root name"); //$NON-NLS-1$
		
		ChangeDescription change1 = recorder.endRecording();
		change.add(change1);
		
		recorder.beginRecording(null, rset.getResources());
		
		root.getBooks().remove(book);

		ChangeDescription change2 = recorder.endRecording();
		change.add(change2);
		
		assertTrue(change.canApply());
		
		change.add(new NonApplicableChange());
		
		assertFalse(change.canApply());
	}
	
	//
	// Fixture methods
	//
	
	protected void doSetUp()
		throws Exception {
		
		super.doSetUp();
		
		// remove the resource from the editing domain, so that the protocol
		//    no longer applies.  Add it to our own resource set
		startWriting();
		testResource.getResourceSet().getResources().remove(testResource);
		rset = new ResourceSetImpl();
		rset.getResources().add(testResource);
		commit();
		
		// brute-force remove the TransactionChangeRecorder
		for (Iterator iter = testResource.getAllContents(); iter.hasNext();) {
			((Notifier) iter.next()).eAdapters().clear();  
		}
		testResource.eAdapters().clear();
		
		recorder = new ChangeRecorder(rset.getResources());
		change = new CompositeChangeDescription();
	}
	
	protected void doTearDown()
		throws Exception {
		
		change = null;
		recorder = null;
		rset = null;
		
		super.doTearDown();
	}
	
	/**
	 * A non-applicable change description for testing purposes.
	 *
	 * @author Christian W. Damus (cdamus)
	 */
	private static class NonApplicableChange
			extends ChangeDescriptionImpl
			implements TransactionChangeDescription {
		
		public boolean canApply() {
			return false;
		}
		
		public boolean isEmpty() {
			return false;
		}
	}
	
}
