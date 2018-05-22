public class OrderedTriple<T extends Comparable<T>, S extends Comparable<S>, U extends Comparable<U>> implements Comparable<OrderedTriple<T, S, U>>
{
	private T _first;
	private S _second;
	private U _third;

	public OrderedTriple() {
		_first = null;
		_second = null;
		_third = null;
	}

	public OrderedTriple(T first, S second, U third) {
		_first = first;
		_second = second;
		_third = third;
	}

	public int compareTo(OrderedTriple<T, S, U> other) {
		if (!this.first().equals(other.first()))
			return this.first().compareTo(other.first());
		else if (!this.second().equals(other.second()))
			return this.second().compareTo(other.second());
		else
			return this.third().compareTo(other.third());
	}

	T first() { return _first; }
	S second() { return _second; }
	U third() { return _third; }

	public String toString() {
		return first().toString() + " " + second().toString() + " " + third().toString();
	}
}