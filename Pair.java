public class Pair<T, S>
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

	T first() { return _first; }
	S second() { return _second; }

	public String toString() {
		return first().toString() + " " + second().toString();
	}
}