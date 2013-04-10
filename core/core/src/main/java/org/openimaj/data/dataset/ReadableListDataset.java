package org.openimaj.data.dataset;

import java.util.AbstractList;

import org.openimaj.data.identity.Identifiable;
import org.openimaj.data.identity.IdentifiableObject;
import org.openimaj.io.ObjectReader;

/**
 * Base class for {@link ListDataset}s in which each instance is read with an
 * {@link ObjectReader}.
 * 
 * @author Jonathon Hare (jsh2@ecs.soton.ac.uk)
 * 
 * @param <INSTANCE>
 *            the type of instances in the dataset
 */
public abstract class ReadableListDataset<INSTANCE> extends AbstractList<INSTANCE> implements ListDataset<INSTANCE> {
	protected ObjectReader<INSTANCE> reader;

	/**
	 * Construct with the given {@link ObjectReader}.
	 * 
	 * @param reader
	 *            the {@link ObjectReader}.
	 */
	public ReadableListDataset(ObjectReader<INSTANCE> reader) {
		this.reader = reader;
	}

	@Override
	public INSTANCE getRandomInstance() {
		return getInstance((int) (Math.random() * size()));
	}

	@Override
	public INSTANCE get(int index) {
		return this.getInstance(index);
	}

	/**
	 * Get an identifier for the instance at the given index. By default this
	 * just returns the index converted to a {@link String}, but sub-classes
	 * should override to to something more sensible if possible.
	 * 
	 * @param index
	 *            the index
	 * @return the identifier of the instance at the given index
	 */
	public String getID(int index) {
		return index + "";
	}

	/**
	 * Get the index of the instance with the given ID, or -1 if it can't be
	 * found.
	 * 
	 * @param id
	 *            the ID string
	 * @return the index; or -1 if not found.
	 */
	public int indexOfID(String id) {
		for (int i = 0; i < size(); i++) {
			if (getID(i).equals(id))
				return i;
		}
		return -1;
	}

	@Override
	public final int size() {
		return numInstances();
	}

	private class WrappedListDataset extends AbstractList<IdentifiableObject<INSTANCE>>
			implements
			ListDataset<IdentifiableObject<INSTANCE>>
	{
		private final ReadableListDataset<INSTANCE> internal;

		WrappedListDataset(ReadableListDataset<INSTANCE> internal) {
			this.internal = internal;
		}

		@Override
		public IdentifiableObject<INSTANCE> getRandomInstance() {
			final int index = (int) (Math.random() * size());

			return getInstance(index);
		}

		@Override
		public IdentifiableObject<INSTANCE> getInstance(int index) {
			return new IdentifiableObject<INSTANCE>(internal.getID(index), internal.getInstance(index));
		}

		@Override
		public IdentifiableObject<INSTANCE> get(int index) {
			return getInstance(index);
		}

		@Override
		public int size() {
			return internal.size();
		}

		@Override
		public int numInstances() {
			return internal.size();
		}
	}

	/**
	 * Create a view of this dataset in which the instances are wrapped up in
	 * {@link IdentifiableObject}s. The {@link #getID(int)} method is used to
	 * determine the identifier.
	 * 
	 * @return a view of this dataset with {@link Identifiable}-wrapped
	 *         instances
	 */
	public ListDataset<IdentifiableObject<INSTANCE>> toIdentifiable() {
		return new WrappedListDataset(this);
	}
}