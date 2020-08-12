/*
 * Copyright (c) 2014 - 2018 Eric Lange
 *
 * Distributed under the MIT License.  See LICENSE.md at
 * https://github.com/LiquidPlayer/LiquidCore for terms and conditions.
 */
package org.liquidplayer.jstest;

import android.os.Handler;
import android.os.Looper;
import androidx.test.InstrumentationRegistry;
import android.util.Log;

import org.junit.Test;
import org.liquidplayer.javascript.JSContext;
import org.liquidplayer.javascript.JSContextGroup;
import org.liquidplayer.javascript.JSException;
import org.liquidplayer.javascript.JSFunction;
import org.liquidplayer.javascript.JSValue;

import java.io.File;
import java.io.IOException;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Semaphore;
import java.util.concurrent.TimeUnit;

import static org.junit.Assert.*;

public class JSContextTest {

    public interface JSContextInterface {
        @SuppressWarnings("unused")
        int func1();
    }
    public class JSContextClass extends JSContext implements JSContextInterface {
        JSContextClass() {
            super(JSContextInterface.class);
        }
        @Override
        public int func1() {
            return 55;
        }
    }
    public class JSContextInGroup extends JSContext implements JSContextInterface {
        JSContextInGroup(JSContextGroup inGroup) {
            super(inGroup, JSContextInterface.class);
        }
        @Override
        public int func1() {
            return property("testObject").toFunction().call().toNumber().intValue();
        }
    }

    @org.junit.Test
    public void testJSContextConstructor() {
        JSContext context = new JSContext();

        context.property("test",10);
        assertEquals(new Double(10.0),context.property("test").toNumber());

        JSContext context1 = new JSContextClass();
        JSValue ret = context1.evaluateScript("func1()");

        assertEquals(new Double(55), ret.toNumber());

        JSContextGroup contextGroup = new JSContextGroup();
        JSContext context2 = new JSContext(contextGroup);
        JSContext context3 = new JSContext(contextGroup);
        context2.evaluateScript("var forty_two = 42; var cx2_func = function() { return forty_two; };");
        JSValue cx2_func = context2.property("cx2_func");

        context3.property("cx3_func", cx2_func);
        JSValue forty_two = context3.evaluateScript("cx3_func()");
        assertEquals(new Double(42), forty_two.toNumber());

        JSContextInGroup context4 = new JSContextInGroup(contextGroup);
        context4.property("testObject", cx2_func);
        ret = context4.evaluateScript("func1()");
        assertEquals(new Double(42), ret.toNumber());

        assertEquals(context2.getGroup(),context3.getGroup());
        assertEquals(context3.getGroup(),context4.getGroup());
        assertNotEquals(context4.getGroup(),null);
        assertNotEquals(context1.getGroup(),context2.getGroup());
    }

    private boolean excp;

    @org.junit.Test
    public void testJSContextExceptionHandler() {
        JSContext context = new JSContext();
        try {
            context.property("does_not_exist").toFunction();
            fail();
        } catch (JSException e) {
            assertTrue(true);
        }

        excp = false;
        context.setExceptionHandler(new JSContext.IJSExceptionHandler() {
            @Override
            public void handle(JSException e) {
                excp = !excp;
            }
        });
        try {
            context.property("does_not_exist").toFunction();
            assertTrue(excp);
        } catch (JSException e) {
            fail();
        }

        context.clearExceptionHandler();
        try {
            context.property("does_not_exist").toFunction();
            fail();
        } catch (JSException e) {
            // excp should still be true
            assertTrue(excp);
        }

        excp = false;
        final JSContext context2 = new JSContext();
        context2.setExceptionHandler(new JSContext.IJSExceptionHandler() {
            @Override
            public void handle(JSException e) {
                excp = !excp;
                // Raise another exception.  Should throw JSException
                context2.property("does_not_exist").toFunction();
            }
        });
        try {
            context2.property("does_not_exist").toFunction();
            fail();
        } catch (JSException e) {
            assertTrue(excp);
        }
    }

    @org.junit.Test
    public void testJSContextEvaluateScript() {
        final String script1 = "" +
                "var val1 = 1;\n" +
                "var val2 = 'foo';\n" +
                "does_not_exist(do_something);";
        String url = "http://liquidplayer.com/script1.js";

        JSContext context = new JSContext();

        try {
            context.evaluateScript(script1, url, 1);
            fail();
        } catch (JSException e) {
            String stack = e.getError().toObject().property("stack").toString();
            String expected = "at " + url + ":4:";
            assertTrue(stack.contains(expected));
        }

        context.property("localv",69);
        JSValue val = context.evaluateScript("this.localv");
        assertEquals(new Double(69), val.toNumber());
    }

    private Exception thrownInMainThread = null;

    @org.junit.Test
    public void testMainThreadSupport() throws Exception {
        Handler handler = new Handler(Looper.getMainLooper());
        final Semaphore mutex = new Semaphore(0);
        handler.post(new Runnable() {
            @Override
            public void run() {
                try {
                    JSContextTest test = new JSContextTest();
                    test.testJSContextConstructor();
                    test.testJSContextEvaluateScript();
                    test.testJSContextExceptionHandler();
                    test.testDeadReferences();
                } catch (Exception e) {
                    thrownInMainThread = e;
                } finally {
                    mutex.release();
                }
            }
        });
        mutex.acquireUninterruptibly();
        if (thrownInMainThread != null) throw thrownInMainThread;
    }

    @Test
    public void testDeadReferences() {
        JSContext context = new JSContext();
        for (int i=0; i<200; i++) {
            new JSValue(context);
        }
        assertTrue(true);
    }

    private class ContextTest {

        Semaphore semaphore = new Semaphore(0);

        private JSContext thisContext;
        private final Map<Integer,JSContext> contextMap =
                Collections.synchronizedMap(new HashMap<Integer,JSContext>());
        private final Object mutex = new Object();

        ContextTest() {
        }

        void createContext() {

            JSContext context = new JSContext();
            context.setExceptionHandler(new JSExceptionHandler() {
                @Override
                public void handle(JSException exception) {
                    Log.e("Issue18", exception.getMessage());
                }
            });

            thisContext = context;
        }

        void callFunction() {

            new Thread() {
                @Override
                public void run() {
                    android.util.Log.d("Issue18", "THREAD TEST 1 ");
                    android.util.Log.d("Issue18", "THREAD TEST " + thisContext);
                    semaphore.release();
                }
            }.start();

        }

        void createContext(int id) {

            JSContext context = new JSContext();
            context.setExceptionHandler(new JSExceptionHandler());

            try {
                synchronized (mutex) {
                    contextMap.put(id, context);
                }

            }
            catch (Exception ioe) {
                ioe.printStackTrace();
            }
        }


        void callFunction(final int id) {

            ExecutorService service = Executors.newSingleThreadExecutor();
            Runnable runnable = new Runnable() {
                @Override
                public void run() {
                    synchronized (mutex) {
                        JSContext context = contextMap.get(id);
                        assertNotNull(context);
                        Log.d("Thread", "context : " + context.toString());
                    }
                    semaphore.release();
                }
            };
            service.execute(runnable);
        }

        class JSExceptionHandler implements JSContext.IJSExceptionHandler {
            @Override
            public void handle(JSException exception) {
                android.util.Log.e("Issue18", "Exception: " + exception.toString());
            }
        }

    }

    @Test
    public void Issue18Test() throws Exception {
        ContextTest contextTest = new ContextTest();
        contextTest.createContext();
        contextTest.callFunction();
        contextTest.semaphore.acquire();

        ContextTest contextTest2 = new ContextTest();
        contextTest2.createContext(12);
        for(int i =0; i < 10; i++) {
            contextTest2.callFunction(12);
        }
        contextTest2.semaphore.acquire();

        ContextTest contextTest3 = new ContextTest();
        contextTest3.createContext(42);
        for(int i =0; i < 10; i++) {
            contextTest3.callFunction(42);
        }
        contextTest3.semaphore.acquire();
    }

    //@Test
    public void snapshotTest() throws IOException {
        String source1 = "function f() { return 42; }";
        String source2 =
                "function f() { return g() * 2; }" +
                "function g() { return 43; }" +
                "/./.test('a')";

        File snapshot1 = JSContextGroup.createSnapshot(source1,
                new File(InstrumentationRegistry.getInstrumentation().getTargetContext().getFilesDir() + "/snapshot1.snap"));
        File snapshot2 = JSContextGroup.createSnapshot(source2,
                new File(InstrumentationRegistry.getInstrumentation().getTargetContext().getFilesDir() + "/snapshot2.snap"));

        JSContextGroup group1 = new JSContextGroup(snapshot1);
        JSContext context1 = new JSContext(group1);
        assertEquals(42, context1.evaluateScript("f()").toNumber().intValue());

        JSContextGroup group2 = new JSContextGroup(snapshot2);
        JSContext context2 = new JSContext(group2);
        assertEquals(86, context2.evaluateScript("f()").toNumber().intValue());
        assertEquals(43, context2.evaluateScript("g()").toNumber().intValue());

        if(snapshot1.delete()) android.util.Log.d("snapshotTest()", "Deleted " + snapshot1.getAbsolutePath());
        if(snapshot2.delete()) android.util.Log.d("snapshotTest()", "Deleted " + snapshot2.getAbsolutePath());
    }

    public void terminateTest(final JSContext context) throws InterruptedException {
        final String endlessLoop = "while (true) {}";
        final CountDownLatch latch = new CountDownLatch(1);
        final Thread jsthread = new Thread() {
            @Override public void run() {
                try {
                    context.evaluateScript(endlessLoop);
                } catch (JSException e) {
                    latch.countDown();
                }
            }
        };
        jsthread.start();
        Thread.sleep(500);
        context.getGroup().terminateExecution();
        assertTrue(latch.await(10, TimeUnit.SECONDS));
    }

    @Test
    public void terminateTest() throws InterruptedException {
        JSContext context = new JSContext();
        terminateTest(context);
    }

    public void scheduleTest(final JSContext context) throws InterruptedException {
        final CountDownLatch latch = new CountDownLatch(1);
        context.getGroup().schedule(new Runnable() {
            @Override
            public void run() {
                assertEquals(5, context.evaluateScript("5").toNumber().intValue());
                latch.countDown();
            }
        });
        latch.await(10, TimeUnit.SECONDS);
    }

    @Test
    public void scheduleTest() throws InterruptedException {
        JSContext context = new JSContext();
        scheduleTest(context);
    }

    @Test
    public void timerTest() throws InterruptedException {
        final CountDownLatch latch = new CountDownLatch(1);
        JSContext context = new JSContext();
        JSFunction callback = new JSFunction(context, "callback") {
            @SuppressWarnings("unused")
            public void callback() {
                latch.countDown();
            }
        };
        context.property("Callback", callback);
        context.evaluateScript("setTimeout(Callback, 200)");
        assertTrue(latch.await(10, TimeUnit.SECONDS));
    }

    @Test
    public void timerTest1() throws InterruptedException {
        final CountDownLatch latch = new CountDownLatch(1);
        JSContext context = new JSContext();
        JSFunction callback = new JSFunction(context, "callback") {
            @SuppressWarnings("unused")
            public void callback() {
                latch.countDown();
            }
        };
        context.property("Callback", callback);
        context.evaluateScript("setTimeout(Callback)");
        assertTrue(latch.await(10, TimeUnit.SECONDS));
    }

    @Test
    public void timerTest2() throws InterruptedException {
        final CountDownLatch latch = new CountDownLatch(3);
        JSContext context = new JSContext();
        final JSFunction callback = new JSFunction(context, "callback") {
            @SuppressWarnings("unused")
            public void callback(Integer fortytwo) {
                assertEquals(Integer.valueOf(42), fortytwo);
                int count = context.property("count").toNumber().intValue();
                count ++;
                context.property("count", count);
                if (count == 3) {
                    context.evaluateScript("clearTimeout(timer)");
                }
                latch.countDown();
            }
        };
        context.property("count", 0);
        context.property("Callback", callback);
        context.evaluateScript("var timer = setInterval(Callback, 200, 42)");
        assertTrue(latch.await(10, TimeUnit.SECONDS));
        Thread.sleep(600);
        int count = context.property("count").toNumber().intValue();
        assertEquals(3, count);
    }

    @org.junit.After
    public void shutDown() {
    }
}