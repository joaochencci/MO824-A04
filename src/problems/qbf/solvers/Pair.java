package problems.qbf.solvers;

public class Pair<L,R> {

	private final L left;
	private final R right;
	
    public static <L,R> Pair<L,R> createPair(L left, R right) {
        return new Pair<L,R>(left, right);
    }
    
	public Pair(L left, R right) {
		this.left = left;
		this.right = right;
	}
	
	public L getLeft() { return left; }
	public R getRight() { return right; }
	
	@Override
	public int hashCode() { 
		return (left == null ? 0 : left.hashCode()) ^ (right == null ? 0 : right.hashCode());
	}
	
	@Override
	public boolean equals(Object o) {
		if (!(o instanceof Pair)) return false;
		Pair<?,?> pairo = (Pair<?,?>)o;
		return this.left.equals(pairo.getLeft()) && this.right.equals(pairo.getRight());
	}
}