package gr.uom.java.ast.decomposition.matching.conditional;

public class Pair<T>
{
	private T first;
	private T second;
	
	public Pair(T first, T second)
	{
		this.first  = first;
		this.second = second;
	}

    @Override
	@SuppressWarnings("unchecked")
	public boolean equals(Object other)
	{
		if (this == other)
		{
			return true;
		}
		if (other == null)
		{
			return false;
		}
		if (this.getClass() != other.getClass())
		{
			return false;
		}
		Pair<T> otherPair = (Pair<T>) other;
		return ((this.first == otherPair.first ||
				(this.first != null && otherPair.first != null && this.first.equals(otherPair.first)))
				&&
				(this.second == otherPair.second ||
				(this.second != null && otherPair.second != null && this.second.equals(otherPair.second))));
    }

    @Override
	public int hashCode()
    {
		final int prime = 31;
		int result = 1;
		result = prime * result + ((this.first == null) ? 0 : this.first.hashCode());
		result = prime * result + ((this.second == null) ? 0 : this.second.hashCode());
		return result;
	}

	public T getFirst()
    {
    	return this.first;
    }

    public void setFirst(T first)
    {
    	this.first = first;
    }

    public T getSecond() {
    	return this.second;
    }

    public void setSecond(T second)
    {
    	this.second = second;
    }

    @Override
    public String toString()
    { 
           return "(" + this.first + ", " + this.second + ")"; 
    }
}
