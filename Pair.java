public class Pair<T extends Comparable<T>, S extends Comparable<S>> implements Comparable<Pair<T, S>>
{
	private T _first;
	private S _second;

	public Pair() {
		_first = null;
		_second = null;
	}

	public Pair(T first, S second) {
		_first = first;
		_second = second;
	}

	public int compareTo(Pair<T, S> other) {
		if (!this.first().equals(other.first()))
			return this.first().compareTo(other.first());
		else
			return this.second().compareTo(other.second());
	}

	T first() { return _first; }
	S second() { return _second; }

	public String toString() {
		return first().toString() + " " + second().toString();
	}
}