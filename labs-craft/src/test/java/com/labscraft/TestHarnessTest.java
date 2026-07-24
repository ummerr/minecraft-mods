package com.labscraft;

import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import java.util.function.Supplier;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * Proves the JUnit 5 + Mockito test harness runs. Does not (and must not)
 * touch Minecraft classes — game classes are not remapped on the test
 * classpath by default.
 */
class TestHarnessTest {
	@Test
	void modIdIsStable() {
		assertEquals("labscraft", LabsCraft.MOD_ID);
	}

	@Test
	void mockitoWorks() {
		@SuppressWarnings("unchecked")
		Supplier<String> supplier = Mockito.mock(Supplier.class);
		Mockito.when(supplier.get()).thenReturn("labscraft");
		assertEquals("labscraft", supplier.get());
		Mockito.verify(supplier).get();
	}
}
