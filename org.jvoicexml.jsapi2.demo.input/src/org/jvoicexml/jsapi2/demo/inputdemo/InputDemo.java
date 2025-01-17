/*
 * File:    $HeadURL: https://jsapi.svn.sourceforge.net/svnroot/jsapi/trunk/org.jvoicexml.jsapi2.jse/demo/InputDemo/src/org/jvoicexml/jsapi2/demo/inputdemo/InputDemo.java $
 * Version: $LastChangedRevision: 329 $
 * Date:    $LastChangedDate $
 * Author:  $LastChangedBy: schnelle $
 *
 * JSAPI - A base implementation for JSR 113.
 *
 * Copyright (C) 2009-2010 JVoiceXML group - http://jvoicexml.sourceforge.net
 *
 */

package org.jvoicexml.jsapi2.demo.inputdemo;

import java.io.InputStream;
import java.util.logging.ConsoleHandler;
import java.util.logging.Handler;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.speech.AudioManager;
import javax.speech.Engine;
import javax.speech.EngineManager;
import javax.speech.SpeechLocale;
import javax.speech.recognition.Grammar;
import javax.speech.recognition.GrammarManager;
import javax.speech.recognition.Recognizer;
import javax.speech.recognition.RecognizerMode;
import javax.speech.recognition.Result;
import javax.speech.recognition.ResultEvent;
import javax.speech.recognition.ResultListener;
import javax.speech.recognition.ResultToken;
import javax.speech.synthesis.Synthesizer;
import javax.speech.synthesis.SynthesizerMode;

import org.jvoicexml.jsapi2.recognition.sphinx4.SphinxEngineListFactory;
import org.jvoicexml.jsapi2.synthesis.freetts.FreeTTSEngineListFactory;

/**
 * @author Dirk Schnelle-Walka
 * 
 */
public final class InputDemo implements ResultListener {
    /** The synthesizer to use. */
    private Synthesizer synthesizer;

    /** The recognizer to use. */
    private Recognizer recognizer;

    /** Locking mechanism. */
    private final Object lock = new Object();

    /** The recognition result. */
    private Result result;

    /**
     * Working method for the demo.
     * 
     * @throws Exception
     *             error running the demo
     */
    public void run() throws Exception {
        // Create a synthesizer for the default Locale.
        synthesizer = (Synthesizer) EngineManager
                .createEngine(SynthesizerMode.DEFAULT);
        // Create a recognizer for the default Locale.
        recognizer = (Recognizer) EngineManager
                .createEngine(new RecognizerMode(SpeechLocale.ENGLISH));

        final AudioManager manager = recognizer.getAudioManager();
        manager.setMediaLocator("capture://audio?rate=8000&bits=16&channels=2&endian=big&encoding=pcm&signed=true");
        // Get it ready to speak
         synthesizer.allocate();
         synthesizer.resume();
         synthesizer.waitEngineState(Engine.RESUMED);

        recognizer.allocate();
        recognizer.addResultListener(this);

        final GrammarManager grammarManager = recognizer.getGrammarManager();
        final InputStream in = InputDemo.class
                .getResourceAsStream("yesno.srgs");
        final Grammar grammar = grammarManager.loadGrammar("grammar:greeting",
                null, in, "UTF-8");
        grammar.setActivatable(true);

        recognizer.requestFocus();
        recognizer.processGrammars();
        recognizer.resume();
        // Tell the user what to do as soon as the recognizer is ready.
        recognizer.waitEngineState(Engine.RESUMED);
        // Speak the intro string
        synthesizer.speak("Please say yes or no", null);
        System.out.println("Please say yes or no...");
        synchronized (lock) {
            lock.wait();
        }
        System.out.print("Recognized: ");
        ResultToken[] tokens = result.getBestTokens();

        for (int i = 0; i < tokens.length; i++) {
            System.out.print(tokens[i].getText() + " ");
        }
        System.out.println();
        recognizer.deallocate();
        synthesizer.deallocate();
        System.exit(0);
    }

    /**
     * Starts this demo.
     * 
     * @param args
     *            command line arguments.
     */
    public static void main(final String[] args) {
        InputDemo demo = new InputDemo();

        // Enable logging at all levels.
        Handler handler = new ConsoleHandler();
        handler.setLevel(Level.ALL);
        Logger.getLogger("").addHandler(handler);
        Logger.getLogger("").setLevel(Level.ALL);

        try {
            EngineManager
                    .registerEngineListFactory(FreeTTSEngineListFactory.class
                            .getName());

            EngineManager
                    .registerEngineListFactory(SphinxEngineListFactory.class
                            .getName());

            demo.run();
        } catch (Exception e) {
            e.printStackTrace();
            System.exit(-1);
        }

    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void resultUpdate(final ResultEvent event) {
        System.out.println(event);
        if (event.getId() == ResultEvent.RESULT_ACCEPTED) {
            result = (Result) (event.getSource());
            synchronized (lock) {
                lock.notifyAll();
            }
        } else {
            // synthesizer.speak("I did not understand what you said", null);
        }
    }

}
