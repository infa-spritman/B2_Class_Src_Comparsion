/*****************************************************************************
 ** ANGRYBIRDS AI AGENT FRAMEWORK
 ** Copyright (c) 2013, XiaoYu (Gary) Ge, Stephen Gould, Jochen Renz
 **  Sahan Abeyasinghe,Jim Keys, Kar-Wai Lim, Zain Mubashir, Andrew Wang, Peng Zhang
 ** All rights reserved.
 **This work is licensed under the Creative Commons Attribution-NonCommercial-ShareAlike 3.0 Unported License. 
 **To view a copy of this license, visit http://creativecommons.org/licenses/by-nc-sa/3.0/ 
 *or send a letter to Creative Commons, 444 Castro Street, Suite 900, Mountain View, California, 94041, USA.
 *****************************************************************************/
package ab.demo;

import java.awt.Point;
import java.awt.Rectangle;
import java.awt.image.BufferedImage;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Random;
import java.awt.geom.Point2D;

import ab.demo.other.ClientActionRobot;
import ab.demo.other.ClientActionRobotJava;
import ab.demo.other.Env;
import ab.planner.TrajectoryPlanner;
import ab.vision.GameStateExtractor.GameState;
import ab.vision.Vision;
import ab.vision.GameStateExtractor;
//Naive agent (server/client version)

import ab.learn.*;
import ab.heuristics.*;

public class ClientNaiveAgent implements Runnable {

    //focus point
    private int focus_x;
    private int focus_y;
    //Wrapper of the communicating messages
    private ClientActionRobotJava ar;
    public byte currentLevel = 1;
    public int currentState = 0;
    TrajectoryPlanner tp;
    private int id = 22394;
    private boolean firstShot;
    private Point prevTarget;
    private StructureParser parser;
    private LevelImprover improver = new LevelImprover();
    private boolean []visitedLevels;
    private int consecutiveReload = 0;
    private List<Integer[]> deltas;

    /**
     * Constructor using the default IP
     * */
    public ClientNaiveAgent() {
        // the default ip is the localhost
        ar = new ClientActionRobotJava("127.0.0.1");
        tp = new TrajectoryPlanner();
        prevTarget = null;
        firstShot = true;

    }
    /**
     * Constructor with a specified IP
     * */
    public ClientNaiveAgent(String ip) {
        ar = new ClientActionRobotJava(ip);
        tp = new TrajectoryPlanner();
        prevTarget = null;
        firstShot = true;

    }
    public ClientNaiveAgent(String ip, int id)
    {
        ar = new ClientActionRobotJava(ip);
        tp = new TrajectoryPlanner();
        prevTarget = null;
        firstShot = true;
        this.id = id;
    }
    

    public void calculateScoreDelta() {
        deltas = new ArrayList<Integer[]>(500);
        for (int i = 0; i < improver.levelSequence.size(); i++) {
            ArrayList<StateRecord> subsequence = improver.levelSequence.get(i);
            for (int j = 1; j < subsequence.size(); j++) {
                StateRecord srF = subsequence.get(j);
                StateRecord srI = subsequence.get(j-1);
                int scoreI = (Integer)srI.record.get(srI.record.size()-2);
                int scoreF = (Integer)srF.record.get(srF.record.size()-2);
                Integer[] scores = {scoreF-scoreI, i, j-1};
                deltas.add(scores);
            }
        }
    }

    /* 
     * Run the Client (Naive Agent)
     */
    public void run() {    
        byte[] arr = ar.configure(ClientActionRobot.intToByteArray(id));
        System.out.println("number of levels - " + Arrays.toString(arr));
        //parser = new StructureParser("../../../play.dat");
        //load the initial level (default 1)
        ar.loadLevel((byte)1);
        visitedLevels = new boolean[arr[2]];
        currentState = 0;
        improver.levelSequence = new ArrayList<ArrayList<StateRecord>>(arr[2]);
        GameState state;
        while (true) {
           
            state = solve();
            
            if (consecutiveReload > 3) { // load next unvisited level or level 1
                boolean succeed = false;
                for (int i = 0; i < visitedLevels.length; i++) {
                    if (!visitedLevels[i]) {
                        ar.loadLevel((byte)(i+1));
                        succeed = true;
                        break;
                    }
                }
                if (!succeed)
                    ar.loadLevel((byte)1);
            }
            //If the level is solved , go to the next level
            if (state == GameState.WON) {
                            
                System.out.println(" loading the level " + (currentLevel + 1) );
                ar.loadLevel(++currentLevel);
                visitedLevels[currentLevel-1] = true;
                currentState = 0;
                consecutiveReload = 0;
                //display the global best scores
                int[] scores = ar.checkScore();
                System.out.println("The global best score: ");
                for (int i = 0; i < scores.length ; i ++)
                {
            
                          System.out.print( " level " + (i+1) + ": " + scores[i]);
                }
                System.out.println();
                System.out.println(" My score: ");
                scores = ar.checkMyScore();
                for (int i = 0; i < scores.length ; i ++)
                {
                   
                          System.out.print( " level " + (i+1) + ": " + scores[i]);
                }
                System.out.println();
                // make a new trajectory planner whenever a new level is entered
                tp = new TrajectoryPlanner();

                // first shot on this level, try high shot first
                firstShot = true;    
            } else 
                //If lost, then restart the level
                if (state == GameState.LOST) {
                System.out.println("restart");
                currentState = 0;
                ar.restartLevel();
                consecutiveReload++;
                System.out.println("cons. reload number - "+ consecutiveReload);
                visitedLevels[currentLevel-1] = true;
                System.out.println("current state is - "+currentState);
            } else 
                if (state == GameState.LEVEL_SELECTION) {
                System.out.println("unexpected level selection page, go to the last current level : "
                                + currentLevel);
                ar.loadLevel(currentLevel);
                visitedLevels[currentLevel-1] = true;
                consecutiveReload = 0;
                currentState = 0;
            } else if (state == GameState.MAIN_MENU) {
                System.out
                        .println("unexpected main menu page, reload the level : "
                                + currentLevel);
                ar.loadLevel(currentLevel);
                consecutiveReload = 0;
                visitedLevels[currentLevel-1] = true;
                currentState = 0;
            } else if (state == GameState.EPISODE_MENU) {
                System.out.println("unexpected episode menu page, reload the level: "
                                + currentLevel);
                ar.loadLevel(currentLevel);
                visitedLevels[currentLevel-1] = true;
                currentState = 0;
            }
        }
    }


      /** 
       * Solve a particular level by shooting birds directly to pigs
       * @return GameState: the game state after shots.
     */
    public GameState solve()

    {
        System.out.println("current state is - "+currentState);
        // capture Image
        BufferedImage screenshot = ar.doScreenShot();

        // process image
        Vision vision = new Vision(screenshot);

        Rectangle sling = vision.findSlingshot();

        //If the level is loaded (in PLAYING　state)but no slingshot detected, then the agent will request to fully zoom out.
        while (sling == null && ar.checkState() == GameState.PLAYING) {
            System.out.println("no slingshot detected. Please remove pop up or zoom out");
            
            try {
                Thread.sleep(1000);
            } catch (InterruptedException e) {
                
                e.printStackTrace();
            }
            ar.fullyZoomOut();
            screenshot = ar.doScreenShot();
            vision = new Vision(screenshot);
            sling = vision.findSlingshot();
        }

        //find birds and pigs
        List<Rectangle> red_birds = vision.findRedBirds();
        List<Rectangle> blue_birds = vision.findBlueBirds();
        List<Rectangle> yellow_birds = vision.findYellowBirds();
        List<Rectangle> white_birds = vision.findWhiteBirds();
        List<Rectangle> black_birds = vision.findBlackBirds();

        List<Rectangle> pigs = vision.findPigs();

        List<Rectangle> wood = vision.findWood();
        List<Rectangle> ice = vision.findIce();
        List<Rectangle> stones = vision.findStones();
        List<Point> terrain = vision.findTerrain(pigs,wood,ice,stones);

        GameStateExtractor _se = null;

        int bird_count = 0;
        bird_count = red_birds.size() + blue_birds.size() + yellow_birds.size();

        System.out.println("...found " + pigs.size() + " pigs and "
                + bird_count + " birds");
        GameState state = ar.checkState();
        int tap_time = 100;
        int beginScore = 0;
        // if there is a sling, then play, otherwise skip.
        if (sling != null) {
            ar.fullyZoomOut();
            Point releasePoint = null;
            System.out.println("Sling is not null.");
            //If there are pigs, we pick up a pig randomly and shoot it. 
            if (!pigs.isEmpty()) {
                Point _tpto;
                // Record initial score.
                _se = new GameStateExtractor();
                beginScore = _se.getScoreInGame(screenshot);
                // create a heuristic engine
                HeuristicEngine he = new HeuristicEngine(sling, pigs, wood, ice, stones, terrain, red_birds, blue_birds, yellow_birds, white_birds, black_birds);
                //create a vector qunatizer
                VectorQuantizer vq = new VectorQuantizer(pigs, wood, ice, stones);
                Rectangle boundingRect = vq.getBoundingStructure();
                double [][][]quantizedStructure = vq.quantize(boundingRect);
                // -- LevelImprover shit!
                StateRecord srecord = new StateRecord(quantizedStructure);
                if (currentState == 0) { // new level encountered
                    ArrayList<StateRecord> lsqnc = new ArrayList<StateRecord>();
                    lsqnc.add(srecord);
                    if (currentLevel-1 >= improver.levelSequence.size()) {
                        improver.levelSequence.add(lsqnc);
                    }
                    //improver.levelSequence.add(lsqnc);
                    System.out.println("size: "+improver.levelSequence.size());
                    System.out.println("*level: "+currentLevel+", *state: "+currentState);
                } else {
                    ArrayList<StateRecord> lsync = improver.levelSequence.get(currentLevel-1);
                    if (currentState >= lsync.size()) {
                        lsync.add(srecord);
                        System.out.println("~~~~~~rec: " +lsync.get(currentState).record);
                        System.out.println("level: "+currentLevel+", *state: "+currentState);
                    } else {
                       // lsync.set(currentState, srecord);
                        System.out.println("level: "+currentLevel+", state: "+currentState);
                    }
                }
               // double minDistance = parser.getStructureDistance(quantizedStructure);
               // System.out.println("~~~~ distance --- "+minDistance);
                if (false) {
		    System.out.println("Using learning");
                    /* use learnt data
                    Level nearestLevel = parser.getNearestLevel(quantizedStructure);
                    double angleToShoot = nearestLevel.getMaxScoreAngle();
                    Point tempReleasePoint = tp.findReleasePoint(sling, angleToShoot);
                    TrajectoryPlanner tempTp = new TrajectoryPlanner();
                    tempTp.setTrajectory(sling, tempReleasePoint);
                    List<Point> trajectory = tempTp._trajectory;
                    _tpto = he.findFirstIntersection(trajectory);*/
                } else {
                    // use heuristics
		    System.out.println("Using heuristics");
                    List<Rectangle> outerBlocks = he.findOuterBlocks();
                    ArrayList<Integer> weightedDistance = he.weightedDistance(outerBlocks);
                    List<Integer> filteredDistance = he.filter_outer_blocks(outerBlocks,weightedDistance);
                    for(int i=1; i<filteredDistance.size(); i++) {
                        boolean is_sorted = true;

                        for(int j=0; j < filteredDistance.size() - i; j++) { // skip the already sorted largest elements
                          if(filteredDistance.get(j) > filteredDistance.get(j+1)) {
                             int temp = filteredDistance.get(j);
                             filteredDistance.set(j, filteredDistance.get(j+1));
                             filteredDistance.set(j+1, temp);
                             Rectangle temp1 = outerBlocks.get(j);
                             outerBlocks.set(j, outerBlocks.get(j+1));
                             outerBlocks.set(j+1, temp1);
                             is_sorted = false;
                          }
                        }
                        if (is_sorted) break;
                    }
                    System.out.println("Point to hit - "+outerBlocks.get(0));
                    // list of targets to hit.
                    List<Point> target = new ArrayList<Point>();
                    for (Rectangle p : outerBlocks) {
                        target.add(new Point((int)p.getX(), (int)p.getY()));
                    }
                    List<Point> _target = new ArrayList<Point>();
                    _target.add(target.get(0));
                    List<Point> blackListed = new ArrayList<Point>();
                    ArrayList allPoints = improver.levelSequence.get(currentLevel-1).get(currentState).record;
                    for (int i = 0; i < improver.levelSequence.size(); i++) {
                        for (int j = 0; j < improver.levelSequence.get(i).size(); j++) {
                            StateRecord sr = improver.levelSequence.get(i).get(j);
                            System.out.println("~ i -"+i+",j -"+j+" "+sr.record);
                        }
                        System.out.println("$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$");
                    }
                    System.out.println("ap: "+allPoints);
                    if (allPoints == null) allPoints = new ArrayList();
                    List<Point> blackPoints = new ArrayList<Point>();
                    for (int i = 1; i < allPoints.size(); i+=2) {
                        blackPoints.add((Point)allPoints.get(i));
                    }
                    List<Point> possibleHitPoints = he.rust(target, blackPoints);
                    int blockIndex = he.find_outerblock_to_hit(outerBlocks, filteredDistance);
                    releasePoint = possibleHitPoints.get(0);
                    TrajectoryPlanner temptp = new TrajectoryPlanner();
                    temptp.setTrajectory(sling,releasePoint);
                    List<Point> traj = temptp._trajectory;
                    _tpto = he.findFirstIntersection(traj);
                }
                
                //_tpto = new Point(0,0);
                // fallback on naive agent
                if (_tpto == null) {
                    // randomly pick up a pig
                    Random r = new Random();
                    int _index = r.nextInt(pigs.size());
                    Rectangle pig = pigs.get(_index);
                    _tpto = new Point((int) pig.getCenterX(), (int) pig.getCenterY());
                }

                    System.out.println("the target point is " + _tpto);

                    /* if the target is very close to before, randomly choose a point near it
                    if (prevTarget != null && distance(prevTarget, _tpt) < 10) {
                        double _angle = r.nextDouble() * Math.PI * 2;
                        _tpt.x = _tpt.x + (int) (Math.cos(_angle) * 10);
                        _tpt.y = _tpt.y + (int) (Math.sin(_angle) * 10);
                        System.out.println("Randomly changing to " + _tpt);
                    }*/

                    prevTarget = new Point(_tpto.x, _tpto.y);

                    // code to managetap after launch for differnt birds
                    Point _tpt = null;
                    Point tap_point = null;
                    int birdType = he.findCurrentBird();
                    if(birdType == -1) {
                        System.out.println("unable to detect launch bird type..");
                        _tpt = _tpto;
                    }
                    else if(birdType == 1) {
                        _tpt = _tpto;
                    }
                    else if(birdType == 2) {
                        _tpt = _tpto;
                    }
                    else if(birdType == 3) {
                        _tpt = _tpto;
                        /*vq.getBoundingStructure();
                        int tempX = vq.getMinX();
                        List<Point> temprel = tp.estimateLaunchPoint(sling,_tpt);
                        TrajectoryPlanner temptp = new TrajectoryPlanner();
                        temptp.setTrajectory(sling,temprel.get(0));
                        int tempY = tp.getYCoordinate(sling, temprel.get(0), tempX);
                        tap_point = new Point(tempX, tempY);*/
                        tap_point = _tpt;
                    }
                    else if(birdType == 4) {
                        _tpt = new Point((int)_tpto.getX(), (int)(_tpto.getY() - (_tpto.getY())/2));
                        tap_point = _tpt;
                    }
                    else if(birdType == 5) {
                        _tpt = _tpto;
                        tap_point = _tpt;
                    }
                    else {
                        _tpt = _tpto;
                    }

                    // estimate the trajectory
                    ArrayList<Point> pts = tp.estimateLaunchPoint(sling, _tpt);

                    // do a high shot when entering a level to find an accurate velocity
                    /*if (firstShot && pts.size() > 1) {
                        releasePoint = pts.get(1);
                    } else if (pts.size() == 1)
                        releasePoint = pts.get(0);
                    else {
                        // System.out.println("first shot " + firstShot);
                        // randomly choose between the trajectories, with a 1 in
                        // 6 chance of choosing the high one
                        if (r.nextInt(6) == 0)
                            releasePoint = pts.get(1);
                        else
                            releasePoint = pts.get(0);
                    }*/
                    List<Point> traj1 = null;
                    List<Point> traj2 = null;

                    if(pts.size() > 1) {
                        TrajectoryPlanner tp1 = new TrajectoryPlanner();
                        tp1.setTrajectory(sling,pts.get(1));
                        traj1 = tp1._trajectory;
                    }
                    if(pts.size() > 0) {
                        TrajectoryPlanner tp2 = new TrajectoryPlanner();
                        tp2.setTrajectory(sling,pts.get(0));
                        traj2 = tp2._trajectory;
                    }

                    if (/*firstShot && */pts.size() > 0 && he.numberOfBlocksBeforeTerrain(traj2)>0/*birdType!=3*/) {
                        releasePoint = pts.get(0);
                    } else if (pts.size() > 1 && he.numberOfBlocksBeforeTerrain(traj1)>0/*birdType!=2*/)
                        releasePoint = pts.get(1);
                    else {
                        releasePoint = pts.get(0);
                    }

                    Point refPoint = tp.getReferencePoint(sling);
                    //Get the center of the active bird as focus point 
                    focus_x = (int) ((Env.getFocuslist()
                            .containsKey(currentLevel)) ? Env.getFocuslist()
                            .get(currentLevel).getX() : refPoint.x);
                    focus_y = (int) ((Env.getFocuslist()
                            .containsKey(currentLevel)) ? Env.getFocuslist()
                            .get(currentLevel).getY() : refPoint.y);
                    System.out.println("the release point is: " + releasePoint);

                    // Get the release point from the trajectory prediction module
                    System.out.println("Shoot!!");

                    currentState++;  // Increment the current state in level.
                    if (releasePoint != null) {
                        double releaseAngle = tp.getReleaseAngle(sling,
                                releasePoint);
                        System.out.println(" The release angle is : "
                                + Math.toDegrees(releaseAngle));
                        int base = 0;
                        //tap later when the angle is more than PI/4
                        if (releaseAngle > Math.PI / 4)
                            base = 2100;
                        else
                            base = 550;
                        /*tap_time = (int) (base + Math.random() * 1500);
                        System.out.println("tap_time " + tap_time);*/

                        if(tap_point == null) {
                            tap_time = (int) (base + Math.random() * 800);
                        }
                        else {
                            tap_time = tp.getTapTime(sling, releasePoint, tap_point);
                        }
                        if(birdType==3)
                            tap_time -= 250;
                        
                    } else
                        System.err.println("Out of Knowledge");
                }
                
                // check whether the slingshot is changed. the change of the slingshot indicates a change in the scale.
                    ar.fullyZoomOut();
                    screenshot = ar.doScreenShot();
                    vision = new Vision(screenshot);
                    Rectangle _sling = vision.findSlingshot();
                    if (sling.equals(_sling)) {

                        // make the shot
                        ar.shoot(focus_x, focus_y, (int) releasePoint.getX()
                                - focus_x, (int) releasePoint.getY() - focus_y,
                                0, tap_time, false);

                        try {
                            Thread.sleep(6000);
                        } catch(Exception e) {
                            e.printStackTrace();
                        }

                        // check the state after the shot
                        BufferedImage sshot = ar.doScreenShot();
                        state = ar.checkState();
                        int finScore;
                        if(state == GameState.WON) {
                            finScore = _se.getScoreEndGame(sshot);
                        }
                        else if(state == GameState.PLAYING) {
                            finScore = _se.getScoreInGame(sshot);
                        }
                        else {
                            finScore = beginScore;
                        }
                        ArrayList<StateRecord> _lsync = improver.levelSequence.get(currentLevel-1);
                        System.out.println("lsync size - "+_lsync);
                        assert (currentState != 0);
                        StateRecord sr = _lsync.get(currentState-1);
                        if (sr.record == null) {
                            System.out.println("record is null - "+currentLevel+", "+currentState);
                            sr.record = new ArrayList();
                        }
                        sr.record.add(finScore - beginScore);
                        sr.record.add(releasePoint);
                        System.out.println("sr record - "+sr.record);
                        System.out.println("Score earned - "+(finScore - beginScore));
                        // update parameters after a shot is made
                        if (state == GameState.PLAYING) {
                            screenshot = ar.doScreenShot();
                            vision = new Vision(screenshot);
                            List<Point> traj = vision.findTrajPoints();
                            tp.adjustTrajectory(traj, sling, releasePoint);
                            firstShot = false;
                        }
                    } else
                        System.out.println("scale is changed, can not execute the shot, will re-segement the image");
        }
        return state;
    }

    private double distance(Point p1, Point p2) {
        return Math.sqrt((double) ((p1.x - p2.x) * (p1.x - p2.x) + (p1.y - p2.y)* (p1.y - p2.y)));
    }

    public static void main(String args[]) {

        ClientNaiveAgent na;
        // In the root directory of Bot (outside of src).
        if(args.length > 0)
            na = new ClientNaiveAgent(args[0]);
        else
            na = new ClientNaiveAgent();
        na.run();
        
    }
}
