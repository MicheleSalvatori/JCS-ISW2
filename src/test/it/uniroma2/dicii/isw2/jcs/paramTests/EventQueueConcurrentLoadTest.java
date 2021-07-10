package it.uniroma2.dicii.isw2.jcs.paramTests;

import static org.junit.Assert.assertTrue;

import java.util.Arrays;
import java.util.Collection;

import org.apache.jcs.engine.CacheElement;
import org.apache.jcs.engine.CacheEventQueue;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.junit.runners.Parameterized.Parameters;

@RunWith(value = Parameterized.class)
public class EventQueueConcurrentLoadTest {

	private static CacheEventQueue queue = null;
	private static CacheListenerImpl listen = null;

	private static int idleTime = 2;
	private static int maxFailure = 3;
	private static int waitBeforeRetry = 100;

	// Test parameters
//	private String testName;
	private int end;
	private int expectedPutCount;

	public EventQueueConcurrentLoadTest(int end, int expectedPutCount) {
		this.end = end;
		this.expectedPutCount = expectedPutCount;
//		this.testName = testName;
	}

	@Parameters
	public static Collection<Object[]> getTestParameters() {
		return Arrays.asList(new Object[][] { {200, 200 }, { 1200, 1400 }, { 5200, 6600 } });
	}

	@BeforeClass
	public static void configure() {
		listen = new CacheListenerImpl();
		queue = new CacheEventQueue(listen, 1L, "testCache1", maxFailure, waitBeforeRetry);
		queue.setWaitToDieMillis(idleTime);
	}

	@AfterClass
	public static void tearDown() {
		queue.destroy();
		queue = null;
		listen = null;
	}

	@Test
	public void runPutTest() throws Exception {
		for (int i = 0; i <= end; i++) {
			CacheElement elem = new CacheElement("testCache1", i + ":key", i + "data");
			queue.addPutEvent(elem);
		}

		while (!queue.isEmpty()) {
			synchronized (this) {
				System.out.println("queue is still busy, waiting 250 millis");
				this.wait(250);
			}
		}
		System.out.println("queue is empty, comparing putCount");

		// this becomes less accurate with each test. It should never fail. If
		// it does things are very off.
		assertTrue("The put count [" + listen.putCount + "] is below the expected minimum threshold ["
				+ expectedPutCount + "]", listen.putCount >= (expectedPutCount - 1));
	}

	@Test(expected = Test.None.class)
	public void runRemoveTest() throws Exception {
		for (int i = 0; i <= end; i++) {
			queue.addRemoveEvent(i + ":key");
		}
	}

	@Test(expected = Test.None.class)
	public void runStopProcessingTest() throws Exception {
		queue.stopProcessing();
	}

	@Test
	public void runPutDelayTest() throws Exception {
		while (!queue.isEmpty()) {
			synchronized (this) {
				System.out.println("queue is busy, waiting 250 millis to begin");
				this.wait(250);
			}
		}
		System.out.println("queue is empty, begin");

		// get it going
		CacheElement elem = new CacheElement("testCache1", "a:key", "adata");
		queue.addPutEvent(elem);

		for (int i = 0; i <= end; i++) {
			synchronized (this) {
				if (i % 2 == 0) {
					this.wait(idleTime);
				} else {
					this.wait(idleTime / 2);
				}
			}
			CacheElement elem2 = new CacheElement("testCache1", i + ":key", i + "data");
			queue.addPutEvent(elem2);
		}

		while (!queue.isEmpty()) {
			synchronized (this) {
				System.out.println("queue is still busy, waiting 250 millis");
				this.wait(250);
			}
		}
		System.out.println("queue is empty, comparing putCount");

		Thread.sleep(1000);

		// this becomes less accurate with each test. It should never fail. If // it
		// does things are very off.
		assertTrue("The put count [" + listen.putCount + "] is below the expected minimum threshold ["
				+ expectedPutCount + "]", listen.putCount >= (expectedPutCount - 1));

	}

}
