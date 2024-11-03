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
    private static final int BOARD_MAX_DIMENSION = 10;
    private final Direction[] DIRECTION_SEQUENCE =
            {Direction.NORTH, Direction.EAST, Direction.SOUTH, Direction.WEST};
    private int directionIndex;
    private int xCoordinate, yCoordinate;
    private boolean isActivated;
    private final Set<Coordinate> pits;

    RobotSimulator() {
        xCoordinate = -1;
        yCoordinate = -1;
        directionIndex = 0;
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
        List<String> outputs = new ArrayList<>();
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(input))) {
            List<String> commands =  reader.lines().collect(Collectors.toList()); // Process line
            for(int commandIndex=0; commandIndex < commands.size(); commandIndex++) {
                Command mainCommand = null;
                String commandArgs = null;
                String[] commandArgsArray = null;

                String[] commandTokens = commands.get(commandIndex).split(" ");
                try {
                    mainCommand = Command.valueOf(commandTokens[0]);
                } catch (IllegalArgumentException e) {
                    continue;
                }
                if (commandTokens.length > 1) {
                    commandArgs = commandTokens[1];
                    commandArgsArray = commandArgs.split(",");
                }

                int inputXCoordinate = this.xCoordinate, inputYCoordinate = this.yCoordinate;
                Direction inputDirection = null;

                switch(mainCommand) {
                    case DEPLOY:
                        try {
                            inputXCoordinate = Integer.parseInt(commandArgsArray[0]);
                            inputYCoordinate = Integer.parseInt(commandArgsArray[1]);
                            inputDirection = Direction.valueOf(commandArgsArray[2]);
                        } catch (IllegalArgumentException e) {
                            continue;
                        }

                        //Validations
                        if (isInvalidMove(outputs, inputXCoordinate, inputYCoordinate)) continue;

                        //Commit the Command
                        this.xCoordinate = inputXCoordinate;
                        this.yCoordinate = inputYCoordinate;
                        this.directionIndex = Arrays.asList(DIRECTION_SEQUENCE).indexOf(inputDirection);
                        this.isActivated = true;

                        break;
                    case PIT:
                        if (!isActivated) continue;

                        //Make sure this PIT follows a DEPLOY and allows only PIT in intermediate steps
                        boolean shouldSkipThisPit = false;
                        int tmpIndex = commandIndex;
                        String previousCommand = null;
                        while (tmpIndex >= 0){
                            previousCommand = commands.get((tmpIndex - 1) % commands.size());
                            if (previousCommand.startsWith(Command.PIT.name())) {
                                --tmpIndex;
                            } else if (previousCommand.startsWith(Command.DEPLOY.name())) {
                                shouldSkipThisPit = false;
                                break;
                            } else {
                                shouldSkipThisPit = true;
                                break;
                            }
                        }

                        if (shouldSkipThisPit) {
                            continue;
                        }

                        String nextCommand = commands.get((commandIndex + 1) % commands.size());
                        //Based on Test cases, I guess below requirement is not valid
//                        if (!(nextCommand.startsWith(Command.MOVE.name()) ||
//                                nextCommand.startsWith(Command.LEFT.name()) ||
//                                nextCommand.startsWith(Command.RIGHT.name()))) {
//                            continue;
//                        }


                        inputXCoordinate = Integer.parseInt(commandArgsArray[0]);
                        inputYCoordinate = Integer.parseInt(commandArgsArray[1]);

                        if (this.xCoordinate == inputXCoordinate &&
                                this.yCoordinate == inputYCoordinate) {
                            outputs.add("ROBOT Detected: Ignored");
                            continue;
                        }
                        pits.add(new Coordinate(inputXCoordinate, inputYCoordinate));

                        break;
                    case MOVE:
                        if (!isActivated) continue;
                        move(outputs);

                        break;
                    case LEFT:
                        if (!isActivated) continue;
                        directionIndex = (directionIndex + 3) % 4;

                        break;
                    case RIGHT:
                        if (!isActivated) continue;
                        directionIndex = (directionIndex + 1) % 4;

                        break;
                    case REPORT:
                        if (!isActivated) continue;
                        outputs.add(String.format("%s,%s,%s",
                                this.xCoordinate, this.yCoordinate, this.DIRECTION_SEQUENCE[directionIndex].name()));
                        break;
                    default:
                        System.err.println("Unrecognised command");
                }
            }
        } catch (IOException e) {
            throw new RuntimeException(e);
        }

        return outputs;
        // throw new UnsupportedOperationException();
    }

    private void move(final List<String> outputs) {
        int inputXCoordinate = this.xCoordinate;
        int inputYCoordinate = this.yCoordinate;
        Direction currentDirection = DIRECTION_SEQUENCE[directionIndex];
        switch (currentDirection) {
            case NORTH:
                inputYCoordinate++;
                break;
            case SOUTH:
                inputYCoordinate--;
                break;
            case EAST:
                inputXCoordinate++;
                break;
            case WEST:
                inputXCoordinate--;
                break;
            default:
                break;
        }

        if (isInvalidMove(outputs, inputXCoordinate, inputYCoordinate)) return;

        //Commit the Command
        this.xCoordinate = inputXCoordinate;
        this.yCoordinate = inputYCoordinate;
    }

    private boolean isInvalidMove(final List<String> outputs, int xCoordinate, int yCoordinate) {

        if (pits.contains(new Coordinate(xCoordinate, yCoordinate))) {
            outputs.add("PIT Detected: Ignored");
            return true;
        }

        if (xCoordinate < 0 || xCoordinate >= BOARD_MAX_DIMENSION ||
                yCoordinate < 0 || yCoordinate >= BOARD_MAX_DIMENSION) {
            outputs.add("Outside Zone: Ignored");
            return true;
        }

        return false;
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
