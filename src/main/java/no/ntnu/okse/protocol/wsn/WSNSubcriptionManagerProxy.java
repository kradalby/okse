/*
 * The MIT License (MIT)
 *
 * Copyright (c) 2015 Norwegian Defence Research Establishment / NTNU
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in
 * all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN
 * THE SOFTWARE.
 */

package no.ntnu.okse.protocol.wsn;

import org.ntnunotif.wsnu.services.implementations.subscriptionmanager.AbstractPausableSubscriptionManager;
import org.oasis_open.docs.wsn.b_2.*;
import org.oasis_open.docs.wsn.bw_2.PauseFailedFault;
import org.oasis_open.docs.wsn.bw_2.ResumeFailedFault;
import org.oasis_open.docs.wsn.bw_2.UnableToDestroySubscriptionFault;
import org.oasis_open.docs.wsn.bw_2.UnacceptableTerminationTimeFault;
import org.oasis_open.docs.wsrf.rw_2.ResourceUnknownFault;

/**
 * Created by Aleksander Skraastad (myth) on 4/4/15.
 * <p>
 * okse is licenced under the MIT licence.
 */
public class WSNSubcriptionManagerProxy extends AbstractPausableSubscriptionManager {

    @Override
    public boolean subscriptionIsPaused(String s) {
        return false;
    }

    @Override
    public boolean keyExists(String s) {
        return false;
    }

    @Override
    public boolean hasSubscription(String s) {
        return false;
    }

    @Override
    public void addSubscriber(String s, long l) {

    }

    @Override
    public void removeSubscriber(String s) {

    }

    @Override
    public void update() {

    }

    @Override
    public ResumeSubscriptionResponse resumeSubscription(ResumeSubscription resumeSubscription) throws ResourceUnknownFault, ResumeFailedFault {
        return null;
    }

    @Override
    public PauseSubscriptionResponse pauseSubscription(PauseSubscription pauseSubscription) throws ResourceUnknownFault, PauseFailedFault {
        return null;
    }

    @Override
    public UnsubscribeResponse unsubscribe(Unsubscribe unsubscribe) throws ResourceUnknownFault, UnableToDestroySubscriptionFault {
        return null;
    }

    @Override
    public RenewResponse renew(Renew renew) throws ResourceUnknownFault, UnacceptableTerminationTimeFault {
        return null;
    }
}
