package edu.isi.bmkeg.skm.topicmodeling.util;

import java.util.ArrayList;

public class MinHeap<T extends Comparable<T>> extends ArrayList<T> {

	private static final long serialVersionUID = 1L;

	public MinHeap(int initialCapacity) {
		super(initialCapacity);
	}

	public boolean add(T keyObj) {
		super.add(null);
		int k = size() - 1;
		while (k > 0) {
			int parent = (k - 1) / 2;
			T parentobj = get(parent);
			// MinHeap -
			// for minheap - if(keyObj.compareTo(parentobj) <= 0)
			if (keyObj.compareTo(parentobj) >= 0) {
				break;
			}
			set(k, parentobj);
			k = parent;
		}
		set(k, keyObj);
		
		return true;
	}

	public T getMin() {
		return get(0);
	}

	public T removeMin() {
		T removeNode = get(0);
		T lastNode = super.remove(size() - 1);
		percolateUp(0, lastNode);
		return removeNode;
	}

	public T replaceMin(T keyObj) {
		T removeNode = get(0);
		percolateUp(0, keyObj);
		return removeNode;
	}

	private void percolateUp(int k, T keyObj) {
		if (isEmpty())
			return;

		while (k < size() / 2) {
			int child = 2 * k + 1; // left child
			if (child < size() - 1
					// MinHeap -
					// for maxheap - && (get(child).compareTo(get(child + 1)) < 0))
					&& (get(child).compareTo(get(child + 1)) > 0)) {
				child++;
			}
			// MinHeap -
			// for maxheap - (keyObj.compareTo(get(child)) >= 0)
			if (keyObj.compareTo(get(child)) <= 0) {
				break;
			}
			set(k, get(child));
			k = child;
		}
		set(k, keyObj);
	}

}