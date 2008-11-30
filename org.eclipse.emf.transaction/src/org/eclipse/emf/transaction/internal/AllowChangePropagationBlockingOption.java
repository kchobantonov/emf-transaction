/**
 * <copyright>
 * 
 * Copyright (c) 2008 Zeligsoft Inc. and others.
 * All rights reserved.   This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 * 
 * Contributors:
 *   Zeligsoft - Initial API and implementation
 * 
 * </copyright>
 *
 * $Id: AllowChangePropagationBlockingOption.java,v 1.1 2008/11/30 16:38:08 cdamus Exp $
 */

package org.eclipse.emf.transaction.internal;

import java.util.Map;

import org.eclipse.emf.transaction.impl.TransactionImpl;
import org.eclipse.emf.transaction.util.BasicTransactionOptionMetadata;


/**
 * Metadata implementation for the non-trivial complexity of the
 * {@link TransactionImpl#ALLOW_CHANGE_PROPAGATION_BLOCKING} transaction option.
 * 
 * @author Christian W. Damus (cdamus)
 * 
 * @since 1.3
 */
public class AllowChangePropagationBlockingOption
		extends BasicTransactionOptionMetadata {

	/**
	 * Initializes me.
	 */
	public AllowChangePropagationBlockingOption() {
		super(TransactionImpl.ALLOW_CHANGE_PROPAGATION_BLOCKING, false, true,
			Boolean.class, Boolean.FALSE);
	}

	@Override
	public void inherit(Map<?, ?> parentOptions,
			Map<Object, Object> childOptions, boolean force) {

        // do not inherit the allow block option if the block option is
        // already applied
        if (!childOptions.containsKey(TransactionImpl.BLOCK_CHANGE_PROPAGATION)) {
            childOptions.put(getOption(), getValue(parentOptions));
        }
	}

}
