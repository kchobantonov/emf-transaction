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
 * $Id: TransactionImpl.java,v 1.1 2006/01/03 20:41:54 cdamus Exp $
 */
package org.eclipse.emf.transaction.impl;

import java.util.Collections;
import java.util.List;
import java.util.Map;

import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.emf.common.notify.Notification;
import org.eclipse.emf.ecore.change.ChangeDescription;
import org.eclipse.emf.transaction.RollbackException;
import org.eclipse.emf.transaction.TXChangeDescription;
import org.eclipse.emf.transaction.TXCommandStack;
import org.eclipse.emf.transaction.TXEditingDomain;
import org.eclipse.emf.transaction.Transaction;
import org.eclipse.emf.transaction.internal.EMFTransactionDebugOptions;
import org.eclipse.emf.transaction.internal.Tracing;
import org.eclipse.emf.transaction.util.CompositeChangeDescription;

/**
 * The default transaction implementation.
 *
 * @author Christian W. Damus (cdamus)
 */
public class TransactionImpl
	implements InternalTransaction {

	private static long nextId = 0L;
	
	final long id;
	
	private final TXEditingDomain domain;
	private final Thread owner;
	private final boolean readOnly;
	private final Map options;
	
	private InternalTransaction parent;
	private InternalTransaction root;
	
	private boolean active;
	private boolean closing; // prevents re-entrant commit/rollback
	private boolean rollingBack;
	protected final CompositeChangeDescription change = new CompositeChangeDescription();
	protected final List notifications = new java.util.ArrayList();
	
	private boolean aborted;
	private IStatus status = Status.OK_STATUS;
	
	/**
	 * Initializes me with my editing domain and read-only state.
	 * 
	 * @param domain the editing domain in which I operate
	 * @param readOnly <code>true</code> if I am read-only; <code>false</code>
	 *     if I am read/write
	 */
	public TransactionImpl(TXEditingDomain domain, boolean readOnly) {
		this(domain, readOnly, null);
	}
	
	/**
	 * Initializes me with my editing domain, read-only state, and additional
	 * options.
	 * 
	 * @param domain the editing domain in which I operate
	 * @param readOnly <code>true</code> if I am read-only; <code>false</code>
	 *     if I am read/write
	 * @param options my options, or <code>null</code> for defaults
	 */
	public TransactionImpl(TXEditingDomain domain, boolean readOnly, Map options) {
		this.domain = domain;
		this.readOnly = readOnly;
		this.owner = Thread.currentThread();
		
		this.options = (options == null)
				? Collections.EMPTY_MAP
				: Collections.unmodifiableMap(new java.util.HashMap(options));
		
		synchronized (TransactionImpl.class) {
			this.id = nextId++;
		}
	}

	
	// Documentation copied from the inherited specification
	public synchronized void start() throws InterruptedException {
		if (Thread.currentThread() != getOwner()) {
			IllegalStateException exc = new IllegalStateException("Not transaction owner"); //$NON-NLS-1$
			Tracing.throwing(TransactionImpl.class, "start", exc); //$NON-NLS-1$
			throw exc;
		}
		
		if (isActive()) {
			IllegalStateException exc = new IllegalStateException("Transaction is already active"); //$NON-NLS-1$
			Tracing.throwing(TransactionImpl.class, "start", exc); //$NON-NLS-1$
			throw exc;
		}
		
		getInternalDomain().activate(this);
		active = true;

		if (this != getInternalDomain().getActiveTransaction()) {
			IllegalStateException exc = new IllegalStateException("Activated transaction while another is active"); //$NON-NLS-1$
			Tracing.throwing(TransactionImpl.class, "start", exc); //$NON-NLS-1$
			throw exc;
		}
		
		if (Tracing.shouldTrace(EMFTransactionDebugOptions.TRANSACTIONS)) {
			int depth = 1;
			for (Transaction tx = getParent(); tx != null; tx = tx.getParent()) depth++;
				
			Tracing.trace("*** Started " + TXEditingDomainImpl.getDebugID(this) //$NON-NLS-1$
				+ " read-only=" + isReadOnly() //$NON-NLS-1$
				+ " owner=" + getOwner().getName() //$NON-NLS-1$
				+ " depth=" + depth //$NON-NLS-1$
				+ " options=" + getOptions() //$NON-NLS-1$
				+ " at " + Tracing.now()); //$NON-NLS-1$
		}
		
		// do this after activation, because I only have a parent once I have
		//    been activated
		if (parent != null) {
			// the parent stops recording here; I record for myself to implement
			//    support rollback of my changes only
			parent.pause();
		}
		
		startRecording();
	}
	
	// Documentation copied from the inherited specification
	public final TXEditingDomain getEditingDomain() {
		return domain;
	}

	// Documentation copied from the inherited specification
	public final Transaction getParent() {
		return parent;
	}
	
	// Documentation copied from the inherited specification
	public final void setParent(InternalTransaction parent) {
		this.parent = parent;
		this.root = (parent != null) ? parent.getRoot() : this;
	}
	
	// Documentation copied from the inherited specification
	public final InternalTransaction getRoot() {
		return root;
	}

	// Documentation copied from the inherited specification
	public final Thread getOwner() {
		return owner;
	}

	// Documentation copied from the inherited specification
	public final boolean isReadOnly() {
		return readOnly;
	}

	// Documentation copied from the inherited specification
	public final Map getOptions() {
		return options;
	}

	// Documentation copied from the inherited specification
	public synchronized boolean isActive() {
		return active;
	}
	
	// Documentation copied from the inherited specification
	public IStatus getStatus() {
		return status;
	}
	
	// Documentation copied from the inherited specification
	public void setStatus(IStatus status) {
		if (status == null) {
			status = Status.OK_STATUS;
		}
		
		this.status = status;
	}
	
	// Documentation copied from the inherited specification
	public synchronized void abort(IStatus status) {
		assert status != null;
		
		this.aborted = true;
		this.status = status;
		
		if (parent != null) {
			// propagate
			parent.abort(status);
		}
	}
	
	/**
	 * Queries whether I have been aborted.
	 * 
	 * @return <code>true</code> if I have been aborted; <code>false</code>, otherwise
	 * 
	 * @see InternalTransaction#abort(IStatus)
	 */
	protected boolean isAborted() {
		return aborted;
	}

	// Documentation copied from the inherited specification
	public void commit() throws RollbackException {
		if (Thread.currentThread() != getOwner()) {
			IllegalStateException exc = new IllegalStateException("Not transaction owner"); //$NON-NLS-1$
			Tracing.throwing(TransactionImpl.class, "commit", exc); //$NON-NLS-1$
			throw exc;
		}
		
		if (closing) {
			IllegalStateException exc = new IllegalStateException("Transaction is already closing"); //$NON-NLS-1$
			Tracing.throwing(TransactionImpl.class, "commit", exc); //$NON-NLS-1$
			throw exc;
		}
		
		if (!isActive()) {
			IllegalStateException exc = new IllegalStateException("Transaction is already closed"); //$NON-NLS-1$
			Tracing.throwing(TransactionImpl.class, "commit", exc); //$NON-NLS-1$
			throw exc;
		}
		
		if (Tracing.shouldTrace(EMFTransactionDebugOptions.TRANSACTIONS)) {
			Tracing.trace("*** Committing " + TXEditingDomainImpl.getDebugID(this) + " at " + Tracing.now()); //$NON-NLS-1$ //$NON-NLS-2$
		}
		
		try {
			// first, check whether I have been aborted.  If so, then I must roll back
			if (isAborted()) {
				closing = false;
				rollback();
				RollbackException exc = new RollbackException(getStatus());
				Tracing.throwing(TransactionImpl.class, "commit", exc); //$NON-NLS-1$
				throw exc;
			}
			
			closing = true;
			
			if (isTriggerEnabled(this)) {
				try {
					getInternalDomain().precommit(this);
				} catch (RollbackException e) {
					Tracing.catching(TransactionImpl.class, "commit", e); //$NON-NLS-1$
					closing = false;  // rollback checks this flag
					rollback();
					Tracing.throwing(TransactionImpl.class, "commit", e); //$NON-NLS-1$
					throw e;
				}
			}
			
			if ((getRoot() == this) && isValidationEnabled(this)) {
				// only the root validates
				IStatus validationStatus = validate();
				setStatus(validationStatus);
				
				if (validationStatus.getSeverity() >= IStatus.ERROR) {
					closing = false;  // rollback checks this flag
					rollback();
					RollbackException exc = new RollbackException(validationStatus);
					Tracing.throwing(TransactionImpl.class, "commit", exc); //$NON-NLS-1$
					throw exc;
				}
			}
		} finally {
			// in case of exception, rollback() already stopped recording
			stopRecording();
			
			close();
		}
	}

	// Documentation copied from the inherited specification
	public void rollback() {
		if (Thread.currentThread() != getOwner()) {
			IllegalStateException exc = new IllegalStateException("Not transaction owner"); //$NON-NLS-1$
			Tracing.throwing(TransactionImpl.class, "rollback", exc); //$NON-NLS-1$
			throw exc;
		}
		
		if (closing) {
			IllegalStateException exc = new IllegalStateException("Transaction is already closing"); //$NON-NLS-1$
			Tracing.throwing(TransactionImpl.class, "rollback", exc); //$NON-NLS-1$
			throw exc;
		}
		
		if (!isActive()) {
			IllegalStateException exc = new IllegalStateException("Transaction is already closed"); //$NON-NLS-1$
			Tracing.throwing(TransactionImpl.class, "rollback", exc); //$NON-NLS-1$
			throw exc;
		}
		
		closing = true;
		rollingBack = true;
		
		if (Tracing.shouldTrace(EMFTransactionDebugOptions.TRANSACTIONS)) {
			Tracing.trace("*** Rolling back " + TXEditingDomainImpl.getDebugID(this) + " at " + Tracing.now()); //$NON-NLS-1$ //$NON-NLS-2$
		}
		
		try {
			if (isValidationEnabled(this)) {
				// ensure that validation of a nesting transaction does not
				//     include any of my changes that I have rolled back
				getInternalDomain().getValidator().remove(this);
				
				stopRecording();
				change.apply();
			}
			
			// forget the description.  The changes are reverted
			change.clear();
		} finally {
			rollingBack = false;
			close();
		}
	}

	// Documentation copied from the inherited specification
	public void yield() {
		getEditingDomain().yield();
	}

	// Documentation copied from the inherited specification
	public TXChangeDescription getChangeDescription() {
		return (change.isEmpty() || isActive()) ? null : change;
	}

	/**
	 * Obtains my owning editing domain as the internal interface.
	 * 
	 * @return the internal view of my editing domain
	 */
	protected InternalTXEditingDomain getInternalDomain() {
		return (InternalTXEditingDomain) getEditingDomain();
	}

	/**
	 * Starts recording changes upong activation or resumption from a child
	 * transaction, unless undo recording is disabled by my options.
	 */
	private void startRecording() {
		TXChangeRecorder recorder = getInternalDomain().getChangeRecorder();
		
		if (isUndoEnabled(this) && !recorder.isRecording()) {
			recorder.beginRecording();
		}
	}
	
	/**
	 * Stops recording changes and adds them to my composite change description,
	 * unless undo recording is disabled by my options.
	 */
	private void stopRecording() {
		TXChangeRecorder recorder = getInternalDomain().getChangeRecorder();
		
		if (isUndoEnabled(this) && recorder.isRecording()) {
			change.add(recorder.endRecording());
		}
	}
	
	// Documentation copied from the inherited specification
	public void pause() {
		// if we are rolling back, then we don't need to worry about recording
		//    changes because we are permanently undoing changes.
		//    See additional comments in the resume(TXChangeDescription) method
		if (!isRollingBack()) {
			stopRecording();
		}
	}
	
	// Documentation copied from the inherited specification
	public void resume(TXChangeDescription nestedChanges) {
		// if we are rolling back, then we don't need to worry about recording
		//    changes because we are permanently undoing changes.  It can happen
		//    that a nested transaction is created in order to roll back changes
		//    that are nested within non-EMF changes, which would cause a
		//    concurrent modification of our composite change if we were to add
		//    the nested transaction's changes to us
		if (!isRollingBack()) {
			if (isUndoEnabled(this) && (nestedChanges != null)) {
				change.add(nestedChanges);
			}
			
			startRecording();
		}
	}
	
	// Documentation copied from the inherited specification
	public boolean isRollingBack() {
		return rollingBack || ((parent != null) && parent.isRollingBack());
	}

	/**
	 * Closes me.  This is the last step in committing or rolling back,
	 * deactivating me in my editing domain.  Also, if I have a parent
	 * transaction, I {@link InternalTransaction#resume(ChangeDescription) resume}
	 * it.
	 * <p>
	 * If a subclass overrides this method, it <em>must</em> ensure that this
	 * implementation is also invoked.
	 * </p>
	 */
	protected synchronized void close() {
		if (isActive()) {
			active = false;
			closing = false;
			getInternalDomain().deactivate(this);
			
			if (parent != null) {
				// my parent resumes recording its changes now that mine are either
				//    committed to it or rolled back
				parent.resume(change);
			}
			
			if (Tracing.shouldTrace(EMFTransactionDebugOptions.TRANSACTIONS)) {
				Tracing.trace("*** Closed " + TXEditingDomainImpl.getDebugID(this) + " at " + Tracing.now()); //$NON-NLS-1$ //$NON-NLS-2$
			}
		}
	}
	
	// Documentation copied from the inherited specification
	public void add(Notification notification) {
		notifications.add(notification);
	}
	
	// Documentation copied from the inherited specification
	public List getNotifications() {
		return notifications;
	}
	
	/**
	 * Validates me.  Should only be called during commit.
	 * 
	 * @return the result of validation.  If this is an error or worse,
	 *     then I must roll back
	 */
	protected IStatus validate() {
		if (Tracing.shouldTrace(EMFTransactionDebugOptions.TRANSACTIONS)) {
			Tracing.trace("*** Validating " + TXEditingDomainImpl.getDebugID(this) + " at " + Tracing.now()); //$NON-NLS-1$ //$NON-NLS-2$
		}
		
		return getInternalDomain().getValidator().validate();
	}
	
	public String toString() {
		return "Transaction[active=" + isActive() //$NON-NLS-1$
			+ ", read-only=" + isReadOnly() //$NON-NLS-1$
			+ ", owner=" + getOwner().getName() + ']'; //$NON-NLS-1$
	}
	
	/**
	 * Queries whether the specified transaction should record undo information,
	 * according to its {@link Transaction#getOptions() options} and
	 * {@link Transaction#isReadOnly() read-only state}.
	 * 
	 * @param tx a transaction
	 * @return <code>true</code> if the transaction should record undo
	 *     information; <code>false</code>, otherwise
	 */
	protected static boolean isUndoEnabled(Transaction tx) {
		return !(tx.isReadOnly()
				|| hasOption(tx, TXCommandStack.OPTION_NO_UNDO)
				|| hasOption(tx, TXCommandStack.OPTION_UNPROTECTED));
	}
	
	/**
	 * Queries whether the specified transaction should validate changes,
	 * according to its {@link Transaction#getOptions() options} and
	 * {@link Transaction#isReadOnly() read-only state}.
	 * 
	 * @param tx a transaction
	 * @return <code>true</code> if the transaction should validate
	 *     changes; <code>false</code>, otherwise
	 */
	protected static boolean isValidationEnabled(Transaction tx) {
		return !(tx.isReadOnly()
				|| hasOption(tx, TXCommandStack.OPTION_NO_VALIDATION)
				|| hasOption(tx, TXCommandStack.OPTION_UNPROTECTED));
	}
	
	/**
	 * Queries whether the specified transaction should invoke pre-commit,
	 * listeners, according to its {@link Transaction#getOptions() options} and
	 * {@link Transaction#isReadOnly() read-only state}.
	 * 
	 * @param tx a transaction
	 * @return <code>true</code> if the transaction should perform the pre-commit
	 *     procedures; <code>false</code>, otherwise
	 */
	protected static boolean isTriggerEnabled(Transaction tx) {
		return !(tx.isReadOnly()
				|| hasOption(tx, TXCommandStack.OPTION_NO_TRIGGERS)
				|| hasOption(tx, TXCommandStack.OPTION_UNPROTECTED));
	}
	
	/**
	 * Queries whether the specified transaction should send post-commit events,
	 * according to its {@link Transaction#getOptions() options}.
	 * 
	 * @param tx a transaction
	 * @return <code>true</code> if the transaction should send post-commit
	 *     events; <code>false</code>, otherwise
	 */
	protected static boolean isNotificationEnabled(Transaction tx) {
		return !hasOption(tx, TXCommandStack.OPTION_NO_NOTIFICATIONS);
	}
	
	/**
	 * Queries whether the specified transaction is an unprotected write,
	 * according to its {@link Transaction#getOptions() options} and
	 * {@link Transaction#isReadOnly() read-only state}.
	 * 
	 * @param tx a transaction
	 * @return <code>true</code> if the transaction is an unprotected write
	 *     transaction; <code>false</code>, otherwise
	 */
	protected static boolean isUnprotected(Transaction tx) {
		return !tx.isReadOnly()
				&& hasOption(tx, TXCommandStack.OPTION_UNPROTECTED);
	}
	
	/**
	 * Queries whether the specified transaction has a boolean option.
	 * 
	 * @param tx a transaction
	 * @param option the boolean-valued option to query
	 * @return <code>true</code> if the transaction has the option;
	 *    <code>false</code> if it does not
	 */
	protected static boolean hasOption(Transaction tx, String option) {
		return Boolean.TRUE.equals(tx.getOptions().get(option));
	}
}
