package edu.isi.bmkeg.skm.topicmodeling.util;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.annotation.DirtiesContext.ClassMode;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration(locations={"/Testcontext-bmkeg.xml"})
@DirtiesContext(classMode=ClassMode.AFTER_CLASS)

public class MinHeapTest {
	
	@Before
	public void setUp() throws Exception {
		
	}

	@After
	public void tearDown() throws Exception {

	}
	
	@Test
	public void test() throws Exception {

		Double array[] = new Double[]{-0.9999,0.05,0.6,0.002343};
		MinHeap<Double> heap = new MinHeap<Double>(array.length);
		for (int i = 0; i < array.length; i++) {
			heap.add(array[i]);
		}
		while (!heap.isEmpty()) {
			System.out.println(heap.removeMin());
		}
	}

}
