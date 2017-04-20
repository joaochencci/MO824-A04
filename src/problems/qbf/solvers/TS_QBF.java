package problems.qbf.solvers;

import java.io.IOException;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.Collections;

import metaheuristics.tabusearch.AbstractTS;
import problems.qbf.QBF_Inverse;
import solutions.Solution;

/**
 * Metaheuristic TS (Tabu Search) for obtaining an optimal solution to a QBF
 * (Quadractive Binary Function -- {@link #QuadracticBinaryFunction}).
 * Since by default this TS considers minimization problems, an inverse QBF
 *  function is adopted.
 * 
 * @author ccavellucci, fusberti
 */
public class TS_QBF extends AbstractTS<Integer> {
	
	private final Integer fake = new Integer(-1);
	
	/**
	 * Constructor for the TS_QBF class. An inverse QBF objective function is
	 * passed as argument for the superclass constructor.
	 * 
	 * @param tenure
	 *            The Tabu tenure parameter.
	 * @param iterations
	 *            The number of iterations which the TS will be executed.
	 * @param filename
	 *            Name of the file for which the objective function parameters
	 *            should be read.
	 * @throws IOException
	 *             necessary for I/O operations.
	 */
	public TS_QBF(Integer tenure, String filename) throws IOException {
		super(new QBF_Inverse(filename), tenure);
	}

	/* (non-Javadoc)
	 * @see metaheuristics.tabusearch.AbstractTS#makeCL()
	 */
	@Override
	public ArrayList<Integer> makeCL() {
		
		ArrayList<Integer> _CL = new ArrayList<Integer>();
		
		for (int i = 0; i < ObjFunction.getDomainSize(); i++) {
			Integer cand = new Integer(i);
			_CL.add(cand);
		}
		
		return _CL;
	}

	/* (non-Javadoc)
	 * @see metaheuristics.tabusearch.AbstractTS#makeRCL()
	 */
	@Override
	public ArrayList<Integer> makeRCL() {
		
		ArrayList<Integer> _RCL = new ArrayList<Integer>();
		
		return _RCL;
	}
	
	/* (non-Javadoc)
	 * @see metaheuristics.tabusearch.AbstractTS#makeTL()
	 */
	@Override
	public ArrayDeque<Integer> makeTL() {
		
		ArrayDeque<Integer> _TS = new ArrayDeque<Integer>(2*tenure);
		
		for (int i=0; i<2*tenure; i++) {
			_TS.add(fake);
		}
		
		return _TS;
	}

	/* (non-Javadoc)
	 * @see metaheuristics.tabusearch.AbstractTS#updateCL()
	 */
	@Override
	public void updateCL(Solution<Integer> solution) {

		CL.clear();
		
		for (int i = 0; i < ObjFunction.getDomainSize(); i++) {
			Integer cand = new Integer(i);
			Integer left = new Integer(i-1);
			Integer right = new Integer(i+1); 
			if(!(solution.contains(cand) || solution.contains(left) || solution.contains(right))) {
				CL.add(cand);
			}
		}
	}

	/**
	 * {@inheritDoc}
	 * 
	 * This createEmptySol instantiates an empty solution and it attributes a
	 * zero cost, since it is known that a QBF solution with all variables set
	 * to zero has also zero cost.
	 */
	@Override
	public Solution<Integer> createEmptySol() {
		
		Solution<Integer> sol = new Solution<Integer>();
		sol.cost = 0.0;
		
		return sol;
	}
	
	@Override
	public boolean solveStopCriteria(Double bestCost) {
		   
	   endTime = System.currentTimeMillis(); 
	   
	   if (bestCost.compareTo(flagCost) < 0) {
	      flagCost = bestCost;
	      total = 0;
	   }
	   else {
		  total++;
	   }
	   
	   return (total == iterations || (endTime-startTime) >= limitTime); 
	}

	/**
	 * {@inheritDoc}
	 * 
	 * The local search operator developed for the QBF objective function is
	 * composed by the neighborhood moves Insertion, Removal and 2-Exchange.
	 */
	@Override
	public void neighborhoodMove() {

		ArrayList<Pair<Integer,Integer>> ML = new ArrayList<Pair<Integer,Integer>>();
		
		Double minDeltaCost = Double.POSITIVE_INFINITY;
		
		Integer bestCandIn = null;
		Integer bestCandOut = null;

		updateCL(incumbentSol); // a lista cont�m apenas candidatos vi�veis
		
		for (Integer candIn : CL) { // adiciona as opera��es de inser��o
			ML.add(Pair.createPair(candIn, null));
		}
		
		for (Integer candOut : CL) { // adiciona as opera��es de remo��o
			ML.add(Pair.createPair(null, candOut));
		}
		
		for (Integer candOut : incumbentSol) { // adiciona as opera��es de troca
			
			Solution<Integer> iSol = new Solution<Integer>(incumbentSol);
			iSol.remove(candOut);
			updateCL(iSol);
			
			for (Integer candIn : CL) {
				if (candIn.compareTo(candOut) == 0) {
					continue;
				}
				ML.add(Pair.createPair(candIn, candOut));
			}
		}
		
		Collections.shuffle(ML);
		
		int length = (int)(ML.size()*percent);
		
		// caso seja a estrat�gia alternativa o tamanho da amostragem aumenta
		// ap�s uma certa quantidade de itera��es sem melhoria
		
		if (percent < 1.0 && (float)total/iterations > percent) {
			length = (int)(ML.size()*((float)total/iterations));
		}
		
		for (int i = 0; i < length; ++i) {
			
			Pair<Integer,Integer> pair = ML.get(i);
			
			Integer candIn = pair.getLeft();
			Integer candOut = pair.getRight();
			
			Double deltaCost = null;
						
			if (candIn != null && candOut != null) {
				deltaCost = ObjFunction.evaluateExchangeCost(candIn, candOut, incumbentSol);
			}
			else if (candIn != null){
				deltaCost = ObjFunction.evaluateInsertionCost(candIn, incumbentSol);
			}
			else {
				deltaCost = ObjFunction.evaluateRemovalCost(candOut, incumbentSol);
			}
			
			if ((!TL.contains(candIn) && !TL.contains(candOut)) || incumbentSol.cost+deltaCost < bestSol.cost) {
				
				if (deltaCost < minDeltaCost) {
					minDeltaCost = deltaCost;
					bestCandIn = candIn;
					bestCandOut = candOut;
				}
				
				if (first && minDeltaCost < 0.0) { // first-improving
					break;
				}
			}
		}
		
		// Implement the best non-tabu move
		TL.poll();
		if (bestCandOut != null) {
			incumbentSol.remove(bestCandOut);
			TL.add(bestCandOut);
		} else {
			TL.add(fake);
		}
		TL.poll();
		if (bestCandIn != null) {
			incumbentSol.add(bestCandIn);
			TL.add(bestCandIn);
		} else {
			TL.add(fake);
		}
		
		ObjFunction.evaluate(incumbentSol);		
	}
	
	public void sort(Solution<Integer> solution) {
		solution.sort(new Comparator<Integer>() {
	        @Override
	        public int compare(Integer first, Integer second) {
	            return  first.compareTo(second);
	        }
	    });
	}
		
	/**
	 * A main method used for testing the TS metaheuristic.
	 */
	public static void main(String[] args) throws IOException {		
				
		TS_QBF tabusearch = new TS_QBF(tenure, "instances/"+filename);
		
		startTime = System.currentTimeMillis();
		Solution<Integer> bestSol = tabusearch.solve();
		endTime = System.currentTimeMillis();
		
		long totalTime = endTime - startTime;
		tabusearch.sort(bestSol);
		
		System.out.println("maxVal = "+bestSol+" Time = "+(double)totalTime/(double)1000+" seg");
	}

	// par�metros e configura��es para testes
	
	static String filename = "qbf060";
	static Integer tenure = 10;
	static Double percent = 1.0; 
	
	static Boolean first = true;
	
	static Integer iterations = 100000;

	static Long limitTime = 1800000l;
	static Long startTime = 0l;
	static Long endTime = 0l;
	
	protected Integer total = 0;
}
