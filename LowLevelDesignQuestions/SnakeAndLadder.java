import java.util.*;

// Entities (Player, Snake, Ladder)
class Player {
    private String name;
    private int position;
    
    public Player(String name) {
        this.name = name;
        this.position = 0;
    }

    public String getName() {
        return name;
    }

    public int getCurrentPosition() {
        return position;
    }

    public void setPosition(int newPosition) {
        this.position = newPosition;
    }
}

class Snake extends AbstractGameEntity {
    public Snake(int startPosition, int endPosition) {
        super(startPosition, endPosition);
    }

    @Override
    public int applyRule(int currentPosition) {
        System.out.println("Hit a snake! Going down from " + startPosition + " to " + endPosition);
        return endPosition;
    }
}

class Ladder extends AbstractGameEntity {
    public Ladder(int startPosition, int endPosition) {
        super(startPosition, endPosition);
    }

    @Override
    public int applyRule(int currentPosition) {
        System.out.println("Hit a ladder! Going up from " + startPosition + " to " + endPosition);
        return endPosition;
    }
}

// Abstract Classes
abstract class AbstractGameEntity {
    protected int startPosition;
    protected int endPosition;

    public AbstractGameEntity(int startPosition, int endPosition) {
        this.startPosition = startPosition;
        this.endPosition = endPosition;
    }

    public abstract int applyRule(int currentPosition);
}

// Interfaces
interface PlayerService {
    void addPlayer(Player player);
    Player getCurrentPlayer();
    void updatePlayerPosition(Player player, int newPosition);
    boolean hasPlayerWon(Player player);
}

interface BoardService {
    void setupBoard(int size, List<Snake> snakes, List<Ladder> ladders);
    int movePlayer(Player player, int steps);
    boolean isValidPosition(int position);
}

interface DiceService {
    int rollDice();
}

interface MovementService {
    int calculateNewPosition(Player player, int diceValue);
}

interface GameOrchestrator {
    void startGame();
    void playTurn();
    boolean isGameOver();
    Player getWinner();
}

// Service Implementations
class PlayerServiceImpl implements PlayerService {
    private List<Player> players;
    private int currentPlayerIndex = 0;
    private int boardSize;

    public PlayerServiceImpl(List<Player> players, int boardSize) {
        this.players = players;
        this.boardSize = boardSize;
    }
    
    @Override
    public void addPlayer(Player player) {
        players.add(player);
    }
    
    @Override
    public Player getCurrentPlayer() {
        Player player = players.get(currentPlayerIndex);
        currentPlayerIndex = (currentPlayerIndex + 1) % players.size();
        return player;
    }

    @Override
    public void updatePlayerPosition(Player player, int newPosition) {
        player.setPosition(newPosition);
    }

    @Override
    public boolean hasPlayerWon(Player player) {
        return player.getCurrentPosition() >= boardSize;
    }
}

class BoardServiceImpl implements BoardService {
    private int size;
    private List<Snake> snakes;
    private List<Ladder> ladders;

    @Override
    public void setupBoard(int size, List<Snake> snakes, List<Ladder> ladders) {
        this.size = size;
        this.snakes = snakes;
        this.ladders = ladders;
    }

    @Override
    public int movePlayer(Player player, int steps) {
        int newPosition = player.getCurrentPosition() + steps;
        newPosition = applySnakesAndLadders(newPosition);
        return newPosition;
    }

    @Override
    public boolean isValidPosition(int position) {
        return position <= size;
    }

    private int applySnakesAndLadders(int position) {
        for (Snake snake : snakes) {
            if (snake.startPosition == position) {
                return snake.applyRule(position);
            }
        }
        for (Ladder ladder : ladders) {
            if (ladder.startPosition == position) {
                return ladder.applyRule(position);
            }
        }
        return position;
    }
}

class DiceServiceImpl implements DiceService {
    private Random random = new Random();
    
    @Override
    public int rollDice() {
        return random.nextInt(6) + 1; // Dice rolls between 1 and 6
    }
}

class MovementServiceImpl implements MovementService {
    private BoardService boardService;
    
    public MovementServiceImpl(BoardService boardService) {
        this.boardService = boardService;
    }

    @Override
    public int calculateNewPosition(Player player, int diceValue) {
        int newPosition = boardService.movePlayer(player, diceValue);
        return boardService.isValidPosition(newPosition) ? newPosition : player.getCurrentPosition();
    }
}

class GameOrchestratorImpl implements GameOrchestrator {
    private PlayerService playerService;
    private DiceService diceService;
    private BoardService boardService;
    private MovementService movementService;
    
    public GameOrchestratorImpl(PlayerService playerService, DiceService diceService, 
                                BoardService boardService, MovementService movementService) {
        this.playerService = playerService;
        this.diceService = diceService;
        this.boardService = boardService;
        this.movementService = movementService;
    }

    @Override
    public void startGame() {
        System.out.println("Game started!");
        while (!isGameOver()) {
            playTurn();
        }
        System.out.println("Game over! Winner: " + getWinner().getName());
    }

    @Override
    public void playTurn() {
        Player currentPlayer = playerService.getCurrentPlayer();
        int diceRoll = diceService.rollDice();
        System.out.println(currentPlayer.getName() + " rolled a " + diceRoll);
        
        int newPosition = movementService.calculateNewPosition(currentPlayer, diceRoll);
        playerService.updatePlayerPosition(currentPlayer, newPosition);
        
        System.out.println(currentPlayer.getName() + " moved to position " + newPosition);
        
        if (playerService.hasPlayerWon(currentPlayer)) {
            System.out.println(currentPlayer.getName() + " has won!");
        }
    }

    @Override
    public boolean isGameOver() {
        for (Player player : playerService.getCurrentPlayer()) {
            if (playerService.hasPlayerWon(player)) {
                return true;
            }
        }
        return false;
    }

    @Override
    public Player getWinner() {
        for (Player player : playerService.getCurrentPlayer()) {
            if (playerService.hasPlayerWon(player)) {
                return player;
            }
        }
        return null;
    }
}

// Main Class (Entry Point)
public class Main {
    public static void main(String[] args) {
        // Create players
        Player player1 = new Player("Alice");
        Player player2 = new Player("Bob");

        List<Player> players = Arrays.asList(player1, player2);

        // Create board with snakes and ladders
        List<Snake> snakes = Arrays.asList(new Snake(14, 7), new Snake(22, 3));
        List<Ladder> ladders = Arrays.asList(new Ladder(4, 14), new Ladder(9, 31));

        BoardService boardService = new BoardServiceImpl();
        boardService.setupBoard(100, snakes, ladders);

        PlayerService playerService = new PlayerServiceImpl(players, 100);
        DiceService diceService = new DiceServiceImpl();
        MovementService movementService = new MovementServiceImpl(boardService);

        // Create the orchestrator
        GameOrchestrator gameOrchestrator = new GameOrchestratorImpl(playerService, diceService, boardService, movementService);

        // Start the game
        gameOrchestrator.startGame();
    }
}