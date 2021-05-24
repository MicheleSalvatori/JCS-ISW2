package it.uniroma2.dicii.isw2.jcs.paramTests;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;

import java.util.Arrays;
import java.util.Collection;

import org.apache.jcs.JCS;
import org.apache.jcs.engine.CacheElement;
import org.apache.jcs.engine.behavior.ICacheElement;
import org.apache.jcs.engine.control.CompositeCache;
import org.apache.jcs.engine.control.CompositeCacheManager;
import org.apache.jcs.engine.memory.lru.LRUMemoryCache;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.junit.runners.Parameterized.Parameters;

@RunWith(Parameterized.class)
public class LRUMemoryCacheConcurrentUnitTest {
	// TODO vedere se ha senso metterli globali e allocarli nel configure()
//	private static CompositeCache cache;
//	private static LRUMemoryCache lru;

	private static int items = 200;
	private static String region;

	public LRUMemoryCacheConcurrentUnitTest(String region) {
		LRUMemoryCacheConcurrentUnitTest.region = region;
	}

	@Parameters
	public static Collection<Object[]> getTestParameters() {
		return Arrays.asList(new Object[][] { { "indexedRegion1" } });
	}

	@BeforeClass
	public static void configure() {
		JCS.setConfigFilename("/TestDiskCache.ccf");
	}

	@Test
	public void runTestForRegion() throws Exception {
		CompositeCacheManager cacheMgr = CompositeCacheManager.getUnconfiguredInstance();
		cacheMgr.configure("/TestDiskCache.ccf");
		CompositeCache cache = cacheMgr.getCache(region);

		LRUMemoryCache lru = new LRUMemoryCache();
		lru.initialize(cache);
		// Add items to cache

		for (int i = 0; i < items; i++) {
			ICacheElement ice = new CacheElement(cache.getCacheName(), i + ":key", region + " data " + i);
			ice.setElementAttributes(cache.getElementAttributes());
			lru.update(ice);
		}

		// Test that initial items have been purged

		for (int i = 0; i < 102; i++) {
			assertNull(lru.get(i + ":key"));
		}

		// Test that last items are in cache

		for (int i = 102; i < items; i++) {
			String value = (String) lru.get(i + ":key").getVal();
			assertEquals(region + " data " + i, value);
		}

		// Remove all the items

		for (int i = 0; i < items; i++) {
			lru.remove(i + ":key");
		}

		// Verify removal

		for (int i = 0; i < items; i++) {
			assertNull("Removed key should be null: " + i + ":key", lru.get(i + ":key"));
		}
	}

}
