/*
 * Copyright (c) 2008, Institute for Software Integrated Systems, Vanderbilt University
 * All rights reserved.
 *
 * Permission to use, copy, modify, and distribute this software and its
 * documentation for any purpose, without fee, and without written agreement is
 * hereby granted, provided that the above copyright notice, the following
 * two paragraphs and the author appear in all copies of this software.
 *
 * IN NO EVENT SHALL THE VANDERBILT UNIVERSITY BE LIABLE TO ANY PARTY FOR
 * DIRECT, INDIRECT, SPECIAL, INCIDENTAL, OR CONSEQUENTIAL DAMAGES ARISING OUT
 * OF THE USE OF THIS SOFTWARE AND ITS DOCUMENTATION, EVEN IF THE VANDERBILT
 * UNIVERSITY HAS BEEN ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 *
 * THE VANDERBILT UNIVERSITY SPECIFICALLY DISCLAIMS ANY WARRANTIES,
 * INCLUDING, BUT NOT LIMITED TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY
 * AND FITNESS FOR A PARTICULAR PURPOSE.  THE SOFTWARE PROVIDED HEREUNDER IS
 * ON AN "AS IS" BASIS, AND THE VANDERBILT UNIVERSITY HAS NO OBLIGATION TO
 * PROVIDE MAINTENANCE, SUPPORT, UPDATES, ENHANCEMENTS, OR MODIFICATIONS.
 * 
 * @author Himanshu Neema
 */

package org.cpswt.coa;

import org.cpswt.hla.InteractionRoot;

/**
 * Stores last arrived interaction in the Federation Manager and is used by
 * OutcomeFilters of sequence model orchestrator to check the last arrived
 * interaction of the Outcome it filters.
 *
 * @author Himanshu Neema
 */
public class ArrivedInteraction {
    private InteractionRoot _ir;
    private Double _arrTime;

    public ArrivedInteraction(InteractionRoot ir, Double arrTime) {
        if (ir == null) {
            throw new NullPointerException("Passed InteractionRoot instance is null!");
        }
        if (arrTime == null) {
            throw new NullPointerException("Passed 'arrivedTime' parameter is null!");
        }
        if (arrTime < 0) {
            throw new IllegalArgumentException("Passed 'arrivedTime' can't be a negative number!");
        }

        this._ir = ir;
        this._arrTime = arrTime;
    }

    public InteractionRoot getInteractionRoot() {
        return _ir;
    }

    public Double getArrivalTime() {
        return _arrTime;
    }
}
