/*
  This file is part of JOP, the Java Optimized Processor
    see <http://www.jopdesign.com/>

  Copyright (C) 2010, Martin Schoeberl (martin@jopdesign.com)

  This program is free software: you can redistribute it and/or modify
  it under the terms of the GNU General Public License as published by
  the Free Software Foundation, either version 3 of the License, or
  (at your option) any later version.

  This program is distributed in the hope that it will be useful,
  but WITHOUT ANY WARRANTY; without even the implied warranty of
  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
  GNU General Public License for more details.

  You should have received a copy of the GNU General Public License
  along with this program.  If not, see <http://www.gnu.org/licenses/>.
*/
package fixed;

import jembench.*;

/**
 * Run JemBench benchmarks with a fixed iteration count.
 * 
 * @author martin
 *
 */
public class LoopAes {
	
	public static final int FREQ = 60000000;

	/**
	 * @param args
	 */
	public static void main(String[] args) {
		
		StreamBenchmark bench = new jembench.stream.AES();
		System.out.println(bench);
		int t1 = (int) System.currentTimeMillis();
		Runnable r[] = bench.getWorkers();
		bench.reset(100);
		while (!bench.isFinished()) {
			for (int j=0; j<r.length; ++j) {
				r[j].run();
			}
		}
		int t2 = (int) System.currentTimeMillis();
		System.out.print(t2-t1);			
		System.out.println(" ms");
		System.out.print((t2-t1)*(FREQ/1000));
		System.out.println(" cycles");

		// TODO Auto-generated method stub

	}

}