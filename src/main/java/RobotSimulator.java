import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.*;
import java.util.stream.Collectors;

/**
 * The application is a simulation of a Rover Robot moving on a square zone to be explored, of
 * dimensions 10 units x 10 units. Coordinates start at 0,0 (bottom left corner).
 * - The input is an input stream of unknown length that will contain a series of lines terminated in \n.
 * - The following commands may be sent:
 * DEPLOY X,Y,F : Deploys the robot to position X,Y if it is a valid position
 * PIT X,Y      : Adds a pit
 * MOVE         : Moves the robot by 1 position in the current orientation direction
 * LEFT         : Robot will stay in the same coordinate, but face the direction to the left of the current one (i.e.: SOUTH -> EAST)
 * RIGHT        : Robot will stay in the same coordinate, but face the direction to the right of the current one (i.e.: SOUTH -> WEST)
 * REPORT       : Issue a line to the report in the format "X,Y,DIRECTION" (without quotes and all caps)
 * - The robot can not be deployed to a position with a pit or off the limits of the zone.
 * - PIT commands are optional and can only follow a DEPLOY command and precede MOVE, LEFT, RIGHT.
 * - REPORT commands can happen at any time.
 * - The first command to be considered is DEPLOY. Commands before this can be safely ignored.
 * - Failure to parse one line should be logged to System.err but continue
 * - Multiple DEPLOYs can be called, so:
 * -- PITs can follow any DEPLOY command, not necessarily the first one.
 * -- The robot can't be deployed on a PIT.
 * -- A new DEPLOY command redeploys the "same" robot (there is only one on the board).
 * -- Multiple PIT commands can be issued, as long as they only follow a DEPLOY or another PIT command.
 * - The robot can't move into PIT or off the zone.
 * - The following error messages can be reported back (in this exact format):
 * -- "Outside Zone: Ignored"   -> When trying to deploy/move out of the zone
 * -- "PIT Detected: Ignored"   -> When trying to deploy/move onto a pit.
 * -- "ROBOT Detected: Ignored" -> When trying to add a pit to the current position of the robot.
 * <p>
 * Create an application that can read in commands of the following form:
 * <p>
 * DEPLOY X,Y,F
 * PIT X,Y
 * PIT X,Y
 * PIT X,Y
 * MOVE
 * LEFT
 * RIGHT
 * REPORT
 */
public class RobotSimulator {
    private final int BOARD_MAX_DIMENSION = 10;
    private int[][] board;
    private int xCoordinate, yCoordinate;
    private Direction direction;
    private boolean isActivated;
    private Set<Coordinate> pits;

    RobotSimulator() {
        board = new int[BOARD_MAX_DIMENSION][BOARD_MAX_DIMENSION];
        xCoordinate = -1;
        yCoordinate = -1;
        direction = null;
        isActivated = false;
        pits = new HashSet<>();
    }
    /**
     * Should process the input and return the report lines as result.
     *
     * @param input the input.
     * @return the reported lines.
     */
    public List<String> process(InputStream input) {
        List<String> inputCommands;
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(input))) {
            List<String> commands =  reader.lines().collect(Collectors.toList()); // Process line
            for(String command: commands) {
                String[] commandTokens = command.split(" ");
                Command mainCommand = Command.valueOf(commandTokens[0]);
                String commandArgs = commandTokens[1];
                String[] commandArgsArray = commandArgs.split(",");
                int inputXCoordinate = -1, inputYCoordinate = -1;

                switch(mainCommand) {
                    case DEPLOY:
                        inputXCoordinate = Integer.parseInt(commandArgsArray[0]);
                        inputYCoordinate = Integer.parseInt(commandArgsArray[1]);
                        Direction inputDirection = Direction.valueOf(commandArgsArray[2]);

                        if (pits.contains(new Coordinate(inputXCoordinate, inputYCoordinate)) ||
                        inputXCoordinate < 0 || inputXCoordinate >= BOARD_MAX_DIMENSION ||
                        inputYCoordinate < 0 || inputYCoordinate >= BOARD_MAX_DIMENSION) {
                            continue;
                        } else {
                            this.xCoordinate = inputXCoordinate;
                            this.yCoordinate = inputYCoordinate;
                            this.direction = inputDirection;
                            this.isActivated = true;
                        }

                        break;
                    case PIT:
                        inputXCoordinate = Integer.parseInt(commandArgsArray[0]);
                        inputYCoordinate = Integer.parseInt(commandArgsArray[1]);
                        pits.add(new Coordinate(inputXCoordinate, inputYCoordinate));
                        break;
                    case MOVE:
                        break;
                    case LEFT:
                        break;
                    case RIGHT:
                        break;
                    case REPORT:
                        break;
                    default:
                        System.err.println("Unrecognised command");
                }
            }
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        // throw new UnsupportedOperationException();
    }
}

enum Command {
    DEPLOY,
    PIT,
    MOVE,
    RIGHT,
    LEFT,
    REPORT
}

enum Direction {
    NORTH,
    SOUTH,
    EAST,
    WEST;
}

class Coordinate {
    private final int x;
    private final int y;

    Coordinate(int x, int y) {
        this.x = x;
        this.y = y;
    }

    public int getX() {
        return x;
    }

    public int getY() {
        return y;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Coordinate that = (Coordinate) o;
        return x == that.x && y == that.y;
    }

    @Override
    public int hashCode() {
        return Objects.hash(x, y);
    }
}
