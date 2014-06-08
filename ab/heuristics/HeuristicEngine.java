package ab.heuristics;

import ab.learn.VectorQuantizer;
import ab.planner.TrajectoryPlanner;

import java.awt.*;
import java.awt.geom.Line2D;
import java.awt.geom.Point2D;
import java.util.*;
import java.util.List;

public class HeuristicEngine {

	private Rectangle _slingShot;
	private List<Rectangle> _pigs;
	private List<Rectangle> _wood;
	private List<Rectangle> _ice;
	private List<Rectangle> _stones;
	private List<Point> _terrain;
	private List<Rectangle> _redBirds;
	private List<Rectangle> _blueBirds;
	private List<Rectangle> _yellowBirds;
	private List<Rectangle> _whiteBirds;
	private List<Rectangle> _blackBirds;

	private static final int HIGHER = 30;
	private static final int MEDIUM = 20;
	private static final int LOW = 10;
	public class LineIterator implements Iterator<Point2D> {
	final static double DEFAULT_PRECISION = 1.0;
	final Line2D line;
	final double precision;
	
	final double sx, sy;
	final double dx, dy;
	
	double x,y,error;
	
	public LineIterator(Line2D line, double precision) {
		this.line = line;
		this.precision = precision;

		sx = line.getX1() < line.getX2() ? precision : -1 * precision;
		sy = line.getY1() < line.getY2() ? precision : -1 * precision;
			 
		dx =  Math.abs(line.getX2() - line.getX1());
		dy = Math.abs(line.getY2() - line.getY1());
		
		error = dx - dy;
		
		y = line.getY1();
		x = line.getX1();
	}
	
	public LineIterator(Line2D line) {
		this(line, DEFAULT_PRECISION);
	}
	
	@Override
	public boolean hasNext() {
	    return Math.abs( x - line.getX2()) > 0.9 || ( Math.abs(y - line.getY2()) > 0.9);
	}

	@Override
	public Point2D next() {
		Point2D ret = new Point2D.Double(x, y);
		
		double e2 = 2*error;
		if (e2 > -dy) {
			error -= dy;
			x += sx;
		}
		if (e2 < dx) {
			error += dx;
			y += sy;
		}
		
		return ret;
	}

	@Override
	public void remove() {
		throw new AssertionError();
	}
}
	public HeuristicEngine(Rectangle slingShot, List<Rectangle> pigs, List<Rectangle> wood, List<Rectangle> ice, List<Rectangle> stones, List<Point> terrain, List<Rectangle> redBirds, List<Rectangle> blueBirds, List<Rectangle> yellowBirds, List<Rectangle> whiteBirds, List<Rectangle> blackBirds) {

		_slingShot = slingShot;
		_wood = wood;
		_pigs = pigs;
		_ice = ice;
		_stones = stones;
		_terrain = terrain;
		_redBirds = redBirds;
		_blueBirds = blueBirds;
		_yellowBirds = yellowBirds;
		_whiteBirds = whiteBirds;
		_blackBirds = blackBirds;
	}

        public Point findFirstIntersection(List<Point> trajectory) {
            Point intersectionPoint = new Point();
            // decompose all the blocks to points.
            ArrayList<Point> _allPoints = new ArrayList<Point>(0);
            _allPoints.addAll(VectorQuantizer.decomposeToPoints(_pigs));
            _allPoints.addAll(VectorQuantizer.decomposeToPoints(_wood));
            _allPoints.addAll(VectorQuantizer.decomposeToPoints(_ice));
            _allPoints.addAll(VectorQuantizer.decomposeToPoints(_stones));
            HashSet<Point> allPoints = new HashSet<Point>();  // Use HashSet for quick membership check
            for (Point p: _allPoints) allPoints.add(p);
            // Find intersection of trajectory and allPoints
            double minX = 10000000;
            for (Point p: trajectory) {
                if (allPoints.contains(p) && p.getX() < minX) {
		    intersectionPoint = p;
                    minX = p.getX();   
                }
            }
            return intersectionPoint;
        }

	public int numberOfBlocksBeforeTerrain(List<Point> trajectory) {
		if(trajectory == null)
			return -1;

		int intersectionX = 100000;
		int intersectionY = -1;
		Point intersection = null;

		for(Point p : trajectory) {
			if(this._terrain.contains(p)) {
				if (p.getX() < intersectionX) {
					intersectionX = (int)p.getX();
					intersection = p;
				}
			}
		}

		if(intersection == null) {
			return 1000;
		}

		intersectionY = (int)intersection.getY();

		List<Rectangle> blocks = new ArrayList<Rectangle>(0);
		if(_pigs != null)
			blocks.addAll(_pigs);
		if(_wood != null)
			blocks.addAll(_wood);
		if(_ice != null)
			blocks.addAll(_ice);
		if(_stones != null)
			blocks.addAll(_stones);

		List<Point> blockPerimeterPoints = new ArrayList<Point>(0);
		for(Rectangle r : blocks) {
			int topLeftX = (int)r.getX();
			int topLeftY = (int)r.getY();
			int height = (int)r.getHeight();
			int width = (int)r.getWidth();

			for(int i=0;i<height;i++) {
				blockPerimeterPoints.add(new Point(topLeftX, topLeftY + i));
				blockPerimeterPoints.add(new Point(topLeftX + (width - 1), topLeftY + i));
			}
			for(int i=0;i<width;i++) {
				blockPerimeterPoints.add(new Point(topLeftX + i, topLeftY));
				blockPerimeterPoints.add(new Point(topLeftX + i, topLeftY + (height - 1)));
			}
		}

		List<Point> validPoints = new ArrayList<Point>(0);
		for(Point p : blockPerimeterPoints) {
			if(p.getX() < intersectionX && p.getY() < intersectionY) {
				validPoints.add(p);
			}
		}

		int blockIntersections=0;
		for(Point t : trajectory) {
			if(validPoints.contains(t))
				blockIntersections++;
		}

		return (int)(blockIntersections/2);
	}

	// finds the bird on the slingshot
	public int findCurrentBird() {
		/*
		*returned result should be interpreted as
		* 1 - red
		* 2 - blue
		* 3 - yellow
		* 4 - white
		* 5 - black
		*/
		int result = -1;
		int leastY = 10000;

		for(Rectangle r : _redBirds) {
			if(r.getY() < leastY) {
				leastY = (int)r.getY();
				result = 1;
			}
		}
		for(Rectangle r : _blueBirds) {
			if(r.getY() < leastY) {
				leastY = (int)r.getY();
				result = 2;
			}
		}
		for(Rectangle r : _yellowBirds) {
			if(r.getY() < leastY) {
				leastY = (int)r.getY();
				result = 3;
			}
		}
		for(Rectangle r : _whiteBirds) {
			if(r.getY() < leastY) {
				leastY = (int)r.getY();
				result = 4;
			}
		}
		for(Rectangle r : _blackBirds) {
			if(r.getY() < leastY) {
				leastY = (int)r.getY();
				result = 5;
			}
		}

		return result;
	}

	// finds outermost blocks of the structure
	public List<Rectangle> findOuterBlocks() {
		ArrayList<Rectangle> allRect = new ArrayList<Rectangle>(0);
		if(_pigs != null)
			allRect.addAll(_pigs);
		if(_wood != null)
			allRect.addAll(_wood);
		if(_ice != null)
			allRect.addAll(_ice);
		if(_stones != null)
			allRect.addAll(_stones);
		
		ArrayList<Rectangle> result = new ArrayList<Rectangle>(0);
		for(Rectangle r: allRect)
		{
			ArrayList<Point2D.Double> points = new ArrayList<Point2D.Double>(0);
			for(int i=1;i<=30;i++)
			{
				Point2D.Double point = new Point2D.Double(r.getX()-i,r.getY()+(r.height/2));
				points.add(point);
			}
			for(int i=1;i<=30;i++)
			{
				Point2D.Double point = new Point2D.Double(r.getX()-30,r.getY()+i);
				points.add(point);
			}
			
			boolean inside = false;
			for(Rectangle s: allRect)
				{
					boolean temp = false;
					for(Point2D p:points)
					{
						if(s.contains(p))
						{
							temp = true;
							break;
						}
						else 
							temp = false;
					}

					if(temp)
					{
						inside = false;
						break;
					}
					else
						inside = true;
				} 

			if(inside)
				result.add(r);
		}
		//return findConnectedOuterBlocks(allRect);
		result.addAll(_pigs);

		return result;

		
	}

	// methods to determine type of a rectangle
    //weighted distance method from outer block to nearest pig
    //input - result of outerblocks()
    //output - list of weighted distances from each outer block

	public ArrayList<Integer> weightedDistance(List<Rectangle> outerblocks)
	{
		ArrayList<Rectangle> allRect = new ArrayList<Rectangle>(0);
		//if(_pigs != null)
		//	allRect.addAll(_pigs);
		if(_wood != null)
			allRect.addAll(_wood);
		if(_ice != null)
			allRect.addAll(_ice);
		if(_stones != null)
			allRect.addAll(_stones);

		ArrayList<Integer> weighted_dist = new ArrayList<Integer>(0);
		int ob = 0;
		//filtering outerblocks but still not in picture....
		double pig_max_x=0;
		for(Rectangle r: _pigs)
		{
			if(pig_max_x<r.getX())
				{
				pig_max_x = r.getX();
				}
		}
		////////////////////////////
		for(Rectangle r: outerblocks )
			{
				int nearest_pig = 0;
				double min_dis = 10000;
				int index = 0;
				for(Rectangle p: _pigs)
					{
						double temp = Point2D.distance(p.getX(),p.getY(),r.getX(),r.getY());
						if(p.getX() > r.getX() && temp < min_dis && !isPig(r)) // pig should be after the outer block.
							{
								min_dis = temp;
								nearest_pig = index;
							}
					index+=1;
					//System.out.println("nearrest pig is found if not -1"+ nearest_pig);
					}

					Line2D.Double line = new Line2D.Double(new Point2D.Double(_pigs.get(nearest_pig).getX(),_pigs.get(nearest_pig).getY()),new Point2D.Double(r.getX(),r.getY()));

					//System.out.println(line.intersects(allRect.get(0)));
					//List<Point2D> vertices = line.vertices();
					int weight = 0;
					int currentBird = findCurrentBird();
					if(currentBird==1)//Red Bird 
					{

						//for(Point2D v: vertices)
						//{
							for(Rectangle blocks: allRect)
							{

								//int area = blocks.width * blocks.height;
								int area = Math.min(blocks.width,blocks.height);
								if(line.intersects(blocks))
								{
									//System.out.println("redcalled");
									if(isWood(blocks))
									{
										weight += LOW * area;
										
										//System.out.println(weight);
									}
									else if(isIce(blocks))
										weight += MEDIUM * area;
									else if(isStone(blocks))
										weight += HIGHER * area;
									else 
									{
										continue;
									}
									//System.out.println(weight/area);

								}
							}
						//}
					}
					else if (currentBird==2) //Blue Bird
					{
						//for(Point2D v: vertices)
						//{
							for(Rectangle blocks: allRect)
							{
								//int area = blocks.width * blocks.height;
								int area = Math.min(blocks.width,blocks.height);
								if(line.intersects(blocks))
								{
									if(isWood(blocks))
										weight += MEDIUM * area;
									else if(isIce(blocks))
										weight += LOW * area;
									else if(isStone(blocks))
										weight += HIGHER * area;
									else 
										continue;
								}
							}
						//}

					}
					else if (currentBird==3) // Yellow Bird
					{
						//for(Point2D v: vertices)
						//{
							for(Rectangle blocks: allRect)
							{
								int area = Math.min(blocks.width,blocks.height);
								if(line.intersects(blocks))
								{
									if(isWood(blocks))
										weight += LOW * area;
									else if(isIce(blocks))
										weight += MEDIUM * area;
									else if(isStone(blocks))
										weight += HIGHER * area;
									else 
										continue;
								}
							}
						//}
					}
					else if (currentBird==4) // White Bird
					{
						//for(Point2D v: vertices)
						//{
							for(Rectangle blocks: allRect)
							{
								int area = Math.min(blocks.width,blocks.height);
								if(line.intersects(blocks))
								{
									if(isWood(blocks))
										weight += HIGHER * area;
									else if(isIce(blocks))
										weight += MEDIUM * area;
									else if(isStone(blocks))
										weight += LOW * area;
									else 
										continue;
								}
							}
						//}

					}
					else //Black Bird
					{
						//for(Point2D v: vertices)
						//{
							for(Rectangle blocks: allRect)
							{
								int area = Math.min(blocks.width,blocks.height);
								if(line.intersects(blocks))
								{
									if(isWood(blocks))
										weight += LOW * area;
									else if(isIce(blocks))
										weight += MEDIUM * area;
									else if(isStone(blocks))
										weight += HIGHER * area;
									else 
										continue;
								}
							}
						//}
					}
			int terrain_intersection = 0;
			//line = new Line2D.Double(new Point2D.Double(_pigs.get(nearest_pig_x).getX(),_pigs.get(nearest_pig_x).getY()),new Point2D.Double(r.getX(),r.getY()));
			
			List<Point2D> ary = new ArrayList<Point2D>();
			Point2D current;
			for(Iterator<Point2D> iter = new LineIterator(line); iter.hasNext();) {
            current =iter.next();
            ary.add(current);
       		}
			for(Point2D p:ary)
			{
				Point pnt = new Point((int)p.getX(),(int)p.getY());
				if(_terrain.contains(pnt))
					terrain_intersection+=1;
			}
//			System.out.println("terrain_intersection" + terrain_intersection + ", weight =  "+weight);

			if(terrain_intersection > 0)
				weight = 2000000;

			if(r.getX() < pig_max_x)
				weighted_dist.add(weight);
			else
				weighted_dist.add(3000000);	
//			System.out.println("For outer block number " + ob + "weight is" + weight);
			ob+=1;
			}
		return weighted_dist;
	} 
	
	public List<Integer> filter_outer_blocks(List<Rectangle> outerblocks,List<Integer> distances)
	{
		ArrayList<Integer> new_weights = new ArrayList<Integer>(0);
		for (Integer i: distances) {
			new_weights.add(i);
		}
		
		int counter = 0;
		for(Rectangle r: outerblocks){

				int nearest_pig = 0;
				double min_dis = 100000;
				int index = 0;
		
				for(Rectangle p: _pigs)
				{
						double temp = Point2D.distance(p.getX(),p.getY(),r.getX(),r.getY());
						if(p.getX() > r.getX() && temp < min_dis && !isPig(r)) // pig should be after the outer block.
							{
								min_dis = temp;
								nearest_pig = index;
							}
					index+=1;
					//System.out.println("nearrest pig is found if not -1"+ nearest_pig);
				}
					double nearest_pig_x = _pigs.get(nearest_pig).getX();
					double nearest_pig_y = _pigs.get(nearest_pig).getY();
		ArrayList<Point2D.Double> targets =  new ArrayList<Point2D.Double>(0);
		targets.add(new Point2D.Double(r.getX(),r.getY()+r.height/2));

		TrajectoryPlanner tp = new TrajectoryPlanner();
		ArrayList<Point> releasepoints = tp.estimateLaunchPoint(_slingShot,new Point((int)targets.get(0).getX(),(int)targets.get(0).getY()));
		int terrain_intersection=0;
		boolean ignore = false;
			for(Point p: releasepoints)
			{
				TrajectoryPlanner temptp = new TrajectoryPlanner();
				temptp.setTrajectory(_slingShot,p);
				List<Point> traj_points = temptp._trajectory;
				terrain_intersection=0;
				for(Point t: traj_points)
				{
					if(_terrain.contains(t)){
						if(t.getX() < nearest_pig_x && t.getY() < nearest_pig_y)
							terrain_intersection+=1;
					}
				}

				if(terrain_intersection>0)
					ignore = true;
				else 
				{
					ignore = false;
					break;
				}
				//System.out.println("Traj Intersection for outer block" + counter + "with terrain is =" + terrain_intersection);
			}
			if(ignore)
				new_weights.set(counter,1000000);
//		System.out.println("New weights is " + new_weights.get(counter));
		counter+=1;
		}
		return new_weights;
	}

	/*public List<Integer> ranking_outer_blocks(List<Rectangle> outerblocks)
	{
		List<Integer> ranks = new List<Integer>(0);
		for(Rectangle r:outerblocks)
		{
			ranks.add(-1);
		}


		return null;

	}*/

	public int find_outerblock_to_hit(List<Rectangle> outerblocks, List<Integer> distances)
	{
		
		int min = -1;
		int value = 100000;
		int counter = 0;
		boolean found = false;
		for(Integer i:distances)
		{
			if(value>i && i>10)
			{
				found = true;
				value = i;
				min = counter;
			}
			counter+=1;
		}
//		System.out.println("Found outer block and its index is + " + min +"found="+found);
		if(found && value < 1000)
		 return min;
		else
		 {
			int i=0;
			for(Rectangle r: outerblocks)
			{
				if(isPig(r)) {
				TrajectoryPlanner tp = new TrajectoryPlanner();
				ArrayList<Point> releasepoints = tp.estimateLaunchPoint(_slingShot,new Point((int)r.getX(),(int)r.getY()));
				int terrain_intersection=0;
				boolean ignore = false;
				for(Point p: releasepoints)
				{
					TrajectoryPlanner temptp = new TrajectoryPlanner();
					temptp.setTrajectory(_slingShot,p);
					List<Point> traj_points = temptp._trajectory;
					terrain_intersection=0;
					for(Point t: traj_points)
					{
						if(_terrain.contains(t)){
							if(t.getX() < r.getX() && t.getY() < r.getY())
								terrain_intersection+=1;
						}
					}

				if(terrain_intersection>0)
					ignore = true;
				else 
				{
					ignore = false;
					break;
				}

				
				//System.out.println("Traj Intersection for outer block" + counter + "with terrain is =" + terrain_intersection);
				}
				if(!ignore)
					return i;
			}
				i++;
			}
			return i;
		}	
	}

	public Point2D find_point_on_outer_block(Rectangle r)
	{
		int targetpoint;
		ArrayList<Point2D.Double> targets =  new ArrayList<Point2D.Double>(0);
		targets.add(new Point2D.Double(r.getX(),r.getY()));
		targets.add(new Point2D.Double(r.getX(),r.getY()+r.height/2));
		targets.add(new Point2D.Double(r.getX(),r.getY()+r.height));
		
		int index=0;
		int nearest_pig=0;
		int min_dis = 100000;
		for(Rectangle p: _pigs)
					{
						double temp = Point2D.distance(p.getX(),p.getY(),r.getX(),r.getY());
						if(p.getX() > r.getX() && temp < min_dis) // pig should be after the outer block.
							{
								nearest_pig = index;
							}
					index+=1;
					//System.out.println("index"+nearest_pig);
					}
		double nearest_pig_x = _pigs.get(nearest_pig).getX();
		double nearest_pig_y = _pigs.get(nearest_pig).getY();

		for(int i=0;i<3;i++)
		{
			TrajectoryPlanner traj = new TrajectoryPlanner();
			ArrayList<Point> releasepoints = traj.estimateLaunchPoint(_slingShot,new Point((int)targets.get(i).getX(),(int)targets.get(i).getY()));
			for(Point p:releasepoints)
			{
				traj.setTrajectory(_slingShot,p);
				ArrayList<Point> trajectory_points = traj._trajectory;
				//System.out.println(trajectory_points.size());
				for(Point t:trajectory_points)
				{
					if(t.getX()==nearest_pig_x && t.getY()==nearest_pig_y)
					{
//						System.out.println("Target Point is" + i);
						return targets.get(i);
					}
				}
			}
		}
		return targets.get(1);//returning middle point of block when x co ordinate of pig not on trajectory path.
	}

	public List<Point> rust(List<Point> targets, List<Point> blackRelPoints) {
		ArrayList<Point> relPoints = new ArrayList<Point>(0);
		TrajectoryPlanner tp = new TrajectoryPlanner();

		for(Point p : targets) {
			List<Point> releasePoints = tp.estimateLaunchPoint(_slingShot, p);
			relPoints.addAll(releasePoints);
		}
        //System.out.println("relpoints - "+relPoints);
        System.out.println("size of blackrelpoints - "+blackRelPoints.size());
		List<Point> whiteListedPoints = new ArrayList<Point>();
        whiteListedPoints.addAll(relPoints);
		for (Point p : relPoints) {
			for (Point p2 : blackRelPoints) {
				if (Math.abs(p2.getX() - p.getX())+Math.abs(p2.getY() - p.getY()) < 0.0002) {
					whiteListedPoints.remove(p);
				}
			}
		}
		return whiteListedPoints;
	}

	public boolean isPig(Rectangle obj) {
		return _pigs.contains(obj);
	}
	public boolean isWood(Rectangle obj) {
		return _wood.contains(obj);
	}
	public boolean isIce(Rectangle obj) {
		return _ice.contains(obj);
	}
	public boolean isStone(Rectangle obj) {
		return _stones.contains(obj);
	}

    public Rectangle findRectangle(){
        ArrayList<Rectangle> allRect = new ArrayList<Rectangle>(0);
        if(_pigs != null)
            allRect.addAll(_pigs);

        double pig_min_x=100000,pig_min_y=100000;
        double pig_max_y=0,pig_max_x = 0;
        for(Rectangle r: _pigs)
        {
            if(pig_min_x>r.getX())
            {
                pig_min_x = r.getX();
            }
        }

        for(Rectangle r:_pigs)
        {
                if(r.getX()==pig_min_x){
                    if(r.getY()>pig_max_y)
                        pig_max_y = r.getY();
                }
        }
          Point2D left_most_top_pig = new Point2D.Double(pig_min_x,pig_max_y);

        for(Rectangle r: _pigs)
        {
            if(pig_max_x<r.getX())
            {
                pig_max_x = r.getX();
            }
        }
        for(Rectangle r:_pigs)
        {
            if(r.getX()==pig_max_x){
                if(r.getY()<pig_min_y)
                    pig_min_y = r.getY();
            }
        }

        Point2D right_most_bottom_pig = new Point2D.Double(pig_max_x,pig_min_y);
        return new Rectangle(Integer.parseInt(""+pig_min_x),Integer.parseInt(""+pig_max_y),Integer.parseInt(""+Math.abs(pig_max_x-pig_min_x)),Integer.parseInt(""+Math.abs(pig_max_y-pig_min_y)));
    }

    public Vector supportVectorForEachBlock(Point2D givenPoint){           //will get x,y of rectangle found above
        ArrayList<Rectangle> allRect = new ArrayList<Rectangle>(0);

        if(_wood != null)
            allRect.addAll(_wood);
        if(_ice != null)
            allRect.addAll(_ice);
        if(_stones != null)
            allRect.addAll(_stones);

        Vector v1 = new Vector();

        for(Rectangle r:_pigs){
            Line2D.Double line = new Line2D.Double(givenPoint,new Point2D.Double(r.getX(),r.getY()));
            int weight = 0;
            int currentBird = findCurrentBird();
            if(currentBird==1)//Red Bird
            {

                //for(Point2D v: vertices)
                //{
                for(Rectangle blocks: allRect)
                {

                    //int area = blocks.width * blocks.height;
                    int area = Math.min(blocks.width,blocks.height);
                    if(line.intersects(blocks))
                    {
                        //System.out.println("redcalled");
                        if(isWood(blocks))
                        {
                            weight += LOW * area;

                            //System.out.println(weight);
                        }
                        else if(isIce(blocks))
                            weight += MEDIUM * area;
                        else if(isStone(blocks))
                            weight += HIGHER * area;
                        else
                        {
                            continue;
                        }
                        //System.out.println(weight/area);

                    }
                }
                //}
            }
            else if (currentBird==2) //Blue Bird
            {
                //for(Point2D v: vertices)
                //{
                for(Rectangle blocks: allRect)
                {
                    //int area = blocks.width * blocks.height;
                    int area = Math.min(blocks.width,blocks.height);
                    if(line.intersects(blocks))
                    {
                        if(isWood(blocks))
                            weight += MEDIUM * area;
                        else if(isIce(blocks))
                            weight += LOW * area;
                        else if(isStone(blocks))
                            weight += HIGHER * area;
                        else
                            continue;
                    }
                }
                //}

            }
            else if (currentBird==3) // Yellow Bird
            {
                //for(Point2D v: vertices)
                //{
                for(Rectangle blocks: allRect)
                {
                    int area = Math.min(blocks.width,blocks.height);
                    if(line.intersects(blocks))
                    {
                        if(isWood(blocks))
                            weight += LOW * area;
                        else if(isIce(blocks))
                            weight += MEDIUM * area;
                        else if(isStone(blocks))
                            weight += HIGHER * area;
                        else
                            continue;
                    }
                }
                //}
            }
            else if (currentBird==4) // White Bird
            {
                //for(Point2D v: vertices)
                //{
                for(Rectangle blocks: allRect)
                {
                    int area = Math.min(blocks.width,blocks.height);
                    if(line.intersects(blocks))
                    {
                        if(isWood(blocks))
                            weight += HIGHER * area;
                        else if(isIce(blocks))
                            weight += MEDIUM * area;
                        else if(isStone(blocks))
                            weight += LOW * area;
                        else
                            continue;
                    }
                }
                //}

            }
            else //Black Bird
            {
                //for(Point2D v: vertices)
                //{
                for(Rectangle blocks: allRect)
                {
                    int area = Math.min(blocks.width,blocks.height);
                    if(line.intersects(blocks))
                    {
                        if(isWood(blocks))
                            weight += LOW * area;
                        else if(isIce(blocks))
                            weight += MEDIUM * area;
                        else if(isStone(blocks))
                            weight += HIGHER * area;
                        else
                            continue;
                    }
                }
                //}
            }
            int terrain_intersection = 0;
            List<Point2D> ary = new ArrayList<Point2D>();
            Point2D current;
            for(Iterator<Point2D> iter = new LineIterator(line); iter.hasNext();) {
                current =iter.next();
                ary.add(current);
            }
            for(Point2D p:ary)
            {
                Point pnt = new Point((int)p.getX(),(int)p.getY());
                if(_terrain.contains(pnt))
                    terrain_intersection+=1;
            }
//			System.out.println("terrain_intersection" + terrain_intersection + ", weight =  "+weight);

            if(terrain_intersection > 0)
                weight = 2000000;

            if(r.getX() > givenPoint.getX())
                v1.add(weight);
            else
                v1.add(3000000);
        }
             return v1;
    }

    public List<Integer> trajectory_weight(Point2D givenPoint){
        ArrayList<Integer> relPoints_weight = new ArrayList<Integer>(0);
        TrajectoryPlanner tp = new TrajectoryPlanner();
        ArrayList<Point> releasepoints = tp.estimateLaunchPoint(_slingShot,new Point((int)givenPoint.getX(),(int)givenPoint.getY()));
        int terrain_intersection=0,counter=0;
        boolean ignore = false;
        for(Point p: releasepoints)
        {   int weight =0;
            TrajectoryPlanner temptp = new TrajectoryPlanner();
            temptp.setTrajectory(_slingShot,p);
            List<Point> traj_points = temptp._trajectory;
            terrain_intersection=0;
            for(Point t: traj_points)
            {
                if(_terrain.contains(t)){
                    if(t.getX() < givenPoint.getX() && t.getY() < givenPoint.getY())
                        terrain_intersection+=1;
                }
                for(Rectangle r:_wood)
                {
                    if(r.contains(givenPoint))
                        weight+=2;
                }

                for(Rectangle r:_ice)
                {
                    if(r.contains(givenPoint))
                        weight+=1;
                }
                for(Rectangle r:_stones)
                {
                    if(r.contains(givenPoint))
                        weight+=3;
                }

            }

            if(terrain_intersection>0)
                relPoints_weight.add(counter,1000000);
            else
                relPoints_weight.add(counter,weight);


            counter+=1;
        }


        return relPoints_weight;

}
}
