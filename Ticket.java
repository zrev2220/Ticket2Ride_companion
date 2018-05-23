public class Ticket implements Comparable
{
	private int aIdx;
	private int bIdx;
	private String aCity;
	private String bCity;

	public Ticket(String a, String b)
	{
		if (Ticket2Ride.getInstance().cityToInt.get(a) > Ticket2Ride.getInstance().cityToInt.get(b))
		{
			// swap a and b so that a < b
			String tmp = a;
			a = b;
			b = tmp;
		}
		this.aCity = a;
		this.bCity = b;
		this.aIdx = Ticket2Ride.getInstance().cityToInt.get(a);
		this.bIdx = Ticket2Ride.getInstance().cityToInt.get(b);
	}

	public Ticket(int a, int b)
	{
		if (a > b)
		{
			// swap a and b so that a < b
			int tmp = a;
			a = b;
			b = tmp;
		}
		this.aCity = Ticket2Ride.getInstance().intToCity.get(a);
		this.bCity = Ticket2Ride.getInstance().intToCity.get(b);
		this.aIdx = a;
		this.bIdx = b;
	}

	public int aIdx() { return this.aIdx; }
	public int bIdx() { return this.bIdx; }
	public String aCity() { return this.aCity; }
	public String bCity() { return this.bCity; }

	public int compareTo(Object o)
	{
		Ticket other = (Ticket) o;
		return (this.aIdx == other.aIdx) ? this.bIdx - other.bIdx : this.aIdx - other.aIdx;
	}
}