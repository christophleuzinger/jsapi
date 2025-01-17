/*
 * File:    $HeadURL: https://jsapi.svn.sourceforge.net/svnroot/jsapi/trunk/org.jvoicexml.jsapi2.jse/src/org/jvoicexml/jsapi2/jse/recognition/sphinx4/Sphinx4ResultListener.java $
 * Version: $LastChangedRevision: 608 $
 * Date:    $Date: 2010-12-08 13:15:40 +0100 (Mi, 08 Dez 2010) $
 * Author:  $LastChangedBy: sterad $
 *
 * JSAPI - An independent reference implementation of JSR 113.
 *
 * Copyright (C) 2005-2012 JVoiceXML group - http://jvoicexml.sourceforge.net
 *
 *  This library is free software; you can redistribute it and/or
 *  modify it under the terms of the GNU Library General Public
 *  License as published by the Free Software Foundation; either
 *  version 2 of the License, or (at your option) any later version.
 *
 *  This library is distributed in the hope that it will be useful,
 *  but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 *  Library General Public License for more details.
 *
 *  You should have received a copy of the GNU Library General Public
 *  License along with this library; if not, write to the Free Software
 *  Foundation, Inc., 59 Temple Place, Suite 330, Boston, MA  02111-1307  USA
 *
 */

package org.jvoicexml.jsapi2.recognition.sphinx4;

import java.util.logging.Level;
import java.util.logging.Logger;

import javax.speech.recognition.GrammarException;
import javax.speech.recognition.ResultEvent;
import javax.speech.recognition.RuleGrammar;

import org.jvoicexml.jsapi2.recognition.BaseResult;

import edu.cmu.sphinx.decoder.ResultListener;
import edu.cmu.sphinx.result.Result;
import edu.cmu.sphinx.util.props.PropertyException;
import edu.cmu.sphinx.util.props.PropertySheet;

/**
 * Result listener for results from the sphinx recognizer.
 * 
 * @author Dirk Schnelle
 * @author Stefan Radomski
 * @version $Revision: 608 $
 */
class Sphinx4ResultListener implements ResultListener {
    /** Logger for this class. */
    private static final Logger LOGGER = Logger
            .getLogger(Sphinx4ResultListener.class.getName());

    /** The recognizer which is notified when a result is obtained. */
    private final Sphinx4Recognizer recognizer;

    /** The current Result. */
    private BaseResult currentResult;

    /**
     * Construct a new result listener.
     * 
     * @param rec
     *            The recognizer.
     */
    public Sphinx4ResultListener(final Sphinx4Recognizer rec) {
        recognizer = rec;
    }

    /**
     * Method called when a result is generated.
     * 
     * @param result
     *            The new result.
     */
    @Override
    public void newResult(final Result result) {
        LOGGER.log(Level.INFO, "received result: {0}", result);
        LOGGER.log(Level.INFO, "isFinal: {0}", result.isFinal());
        if (!result.isFinal() || "<sil>".equalsIgnoreCase(result.toString())) {
            LOGGER.warning("result is not final or <sil>. forget about it.");
            return;
        }
        
        if(result.toString().contains("<unk>")) {
            LOGGER.warning("result contains <unk> which represents negative result");
            return;
        }            

        /**
         * For the current implementation with the SRGSGrammarContainer in
         * Sphinx4, all active grammars are actually a single one. Call the
         * recognizer with the best token and let him figure out, which grammar
         * actually produced the result.
         */
        final RuleGrammar grammar = recognizer.getRuleGrammar(result
                .getBestFinalToken());
        currentResult = new BaseResult(grammar);

        final ResultEvent created = new ResultEvent(currentResult,
                ResultEvent.RESULT_CREATED, false, false);
        recognizer.postResultEvent(created);

        final String utterance = result.getBestFinalResultNoFiller();
        try {
            currentResult.setResult(utterance);
        } catch (GrammarException e) {
            final ResultEvent rejected = new ResultEvent(currentResult,
                    ResultEvent.RESULT_REJECTED, false, false);
            recognizer.postResultEvent(rejected);
        }

        final ResultEvent grammarFinalized = new ResultEvent(currentResult,
                ResultEvent.GRAMMAR_FINALIZED);
        recognizer.postResultEvent(grammarFinalized);

        if (currentResult.getResultState() == javax.speech.recognition.Result.REJECTED) {
            currentResult.setResultState(javax.speech.recognition.Result.REJECTED);
            final ResultEvent rejected = new ResultEvent(currentResult,
                    ResultEvent.RESULT_REJECTED, false, false);
            recognizer.postResultEvent(rejected);
        } else {
            final ResultEvent accepted = new ResultEvent(currentResult,
                    ResultEvent.RESULT_ACCEPTED, false, false);
            recognizer.postResultEvent(accepted);
        }
    }

    /**
     * {@inheritDoc}
     * unused.
     */
    @Override
    public void newProperties(final PropertySheet sheet)
            throws PropertyException {
    }
}
