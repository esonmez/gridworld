import java.util.ArrayList;

/**
 * Created by emresonmez on 11/9/15.
 */
public class Main {
    private FileUtils fileUtils;
    private ArrayList<double[][]> gridWorlds;
    private double[][] averagedMaze;

    public Main(){
        fileUtils = new FileUtils();
        this.populateGridWorlds();
    }

    /**
     * Create four grid worlds (each with one goal state) and one grid world
     * that is the average of the four original grid worlds.
     */
    private void populateGridWorlds() {
        double[] startRow = {0,-1,-1,-1,-1};
        double[] emptyRow = {-1,-1,-1,-1,-1};
        double[] reward1 = {-1,100,-1,-1,-1};
        double[] reward2  = {-1,-1,-1,100,-1};
        double[] reward3 = {100,-1,-1,-1,-1};
        double[] reward4 = {-1,-1,100,-1,-1};

        double[][] grid1 = {startRow, reward1, emptyRow, emptyRow, emptyRow};
        double[][] grid2 = {startRow, emptyRow, reward2, emptyRow, emptyRow};
        double[][] grid3 = {startRow, emptyRow, emptyRow, reward3, emptyRow};
        double[][] grid4 = {startRow, emptyRow, emptyRow, emptyRow, reward4};

        // get the averaged grid world, setting each reward state to the average of all rewards.


        this.gridWorlds = new ArrayList<>();
        this.gridWorlds.add(grid1);
        this.gridWorlds.add(grid2);
        this.gridWorlds.add(grid3);
        this.gridWorlds.add(grid4);

        double[][] superGrid = {startRow, reward1, reward2, reward3, reward4};
        this.averagedMaze = getAveragedGridWorld(superGrid, 100, 4);
        this.gridWorlds.add(averagedMaze);
    }

    /**
     * Get an averaged grid world from the four original grid worlds.
     * @param gridWorld
     * @param reward
     * @param numRewards
     * @return
     */
    private double[][] getAveragedGridWorld(double[][] gridWorld, int reward, int numRewards) {
        double[][] averagedGridWorld = new double[gridWorld[0].length][gridWorld[0].length];

        double averagedReward = (reward + 1.0 - numRewards) / numRewards;

        for (int i = 0; i < gridWorld[0].length; i++) {
            for (int j = 0; j < gridWorld[0].length; j++) {
                if (gridWorld[i][j] == reward) {
                    averagedGridWorld[i][j] = averagedReward;
                }
            }
        }

        return averagedGridWorld;
    }


    /**
     * Execute the following algorithm:
     *   1) Run q-learning on each individual grid world. Get the q matrix.
     *      Using this q matrix to initialize the qLearner, get the reward
     *      of one episode for each of the four grid worlds. Average the
     *      four rewards.
     *
     *   2) Run q-learning on the averaged grid world. Get the q matrix.
     *      Using this q matrix to initialize the qLearner, get the reward
     *      of one episode for each of the four grid worlds. Average the
     *      four rewards.
     *
     * The goal is to figure out whether the policy generated in (2) does
     * better, on average, than any policy generated in (1).
     *
     * @throws MazeException
     */
    private void runQ() throws MazeException {
        boolean terminateAtGoalState = true; // exit episode if goal state is reached
        int numSteps = 10000;
        int goalValue = 24; // this is the minimum threshold that defines a goal
        int stepValue = -1;
        double goalGamma = 0;
        double stepGamma = .99;
        int startX = 0;
        int startY = 0;
        double alpha = 0.1;
        double epsilon = 0.95;

        QLearner qLearner = new QLearner(terminateAtGoalState, numSteps, goalValue, goalGamma, stepValue, stepGamma,
                this.gridWorlds, startX, startY, alpha, epsilon);

        // Part 1. Run learning on individual grid worlds with 1000 runs.
        this.runLearningOnIndividualGridWorlds(qLearner, 1000);

        // Part 2. Run learning on averaged grid world.
        this.runLearningOnAveragedGridWorld(qLearner);
    }

    /**
     * 1. Gets Q matrix by running numTrials of qLearning on individual grid worlds.
     * 2. Uses this matrix as a policy for each of the four grid worlds and gets the
     *    reward of the policy for each one. The four rewards are averaged.
     * 3. Part 2 is executed 100 times and the average is returned.
     *
     * @param qLearner
     * @param numTrials
     * @throws MazeException
     */
    private void runLearningOnIndividualGridWorlds(QLearner qLearner, int numTrials) throws MazeException {
        double[][][] emptyQ = new double[5][5][4];

        System.out.println("Running q-learning on all four mazes.");
        for (int i = 0; i < 4; i++) {
            qLearner.setQ(emptyQ);
            qLearner.setCurrentMaxReward(0);
            qLearner.runQLearner(numTrials, i, false);
            double[][][] q = qLearner.getQ();
            double reward = qLearner.getCurrentMaxReward();
            double averagedReward = this.runMazesWithQMatrixAndGetAverageReward(qLearner, q, 10);
            System.out.println("Averaged reward across 4 mazes, policy generated from maze " + i + ": "  + averagedReward);
        }
    }

    /**
     * 1. Gets Q matrix by running numTrials of qLearning on
     *    averaged grid world.
     * 2. Uses this matrix as a policy for each of the four
     *    grid worlds and gets the reward of the policy for
     *    each one. The four rewards are averaged.
     * 3. Part 2 is executed 100 times and the average is
     *    returned.
     *
     * @param qLearner
     * @throws MazeException
     */
    private void runLearningOnAveragedGridWorld(QLearner qLearner) throws MazeException {
        double[][][] emptyQ = new double[5][5][4];
        qLearner.setQ(emptyQ);

        double averagedGoalGamma = .99 * 3 / 4;

        qLearner.setGoalGamma(averagedGoalGamma);
        qLearner.setTerminateAtGoalState(false);
        qLearner.runQLearner(1000, 4, false);

        double[][][] qSaved = qLearner.getQ();
        qLearner.setTerminateAtGoalState(true);
        qLearner.setGoalGamma(0);
        double averagedReward = runMazesWithQMatrixAndGetAverageReward(qLearner, qSaved, 10);
        System.out.println("Averaged reward across 4 mazes, policy generated from average of 4 mazes :" + averagedReward);
    }

    /**
     * 1. Uses a Q matrix to initialize the learner and then calculates
     *    the reward of using this policy on each of the four grid worlds.
     * 2. The four rewards are averaged.
     * 3. Steps 1 and 2 are repeated numRuns times and the average of the
     *    runs is returned.
     *
     * @param qLearner
     * @param qInitial
     * @param numRuns
     * @return
     * @throws MazeException
     */
    private double runMazesWithQMatrixAndGetAverageReward(QLearner qLearner, double[][][] qInitial, int numRuns) throws MazeException {
        double cumulativeRewardSum = 0;

        for (int j = 0; j < numRuns; j++) {
            double totalReward = 0;
            for (int i = 0; i < 4; i++) {
                qLearner.setQ(qInitial);
                qLearner.setCurrentMaxReward(0);
                qLearner.runQLearner(1, i, true);
                double reward = qLearner.getCurrentMaxReward();
                totalReward += reward;
            }
            double averagedTotalReward = totalReward / 4.0;
            cumulativeRewardSum += averagedTotalReward;
        }

        return cumulativeRewardSum / numRuns;
    }

    public static void main(String[] args) throws MazeException {
        Main main = new Main();
        main.runQ();
    }

}