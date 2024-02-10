package duke;

import duke.task.Deadline;
import duke.task.Event;
import duke.task.Task;
import duke.task.Todo;

import java.time.format.DateTimeFormatter;

import java.nio.file.Path;
import java.nio.file.Paths;

import java.time.LocalDateTime;

import javafx.application.Application;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.TextField;
import javafx.scene.control.Label;
import javafx.scene.layout.AnchorPane;
import javafx.scene.layout.VBox;
import javafx.scene.layout.Region;
import javafx.stage.Stage;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;

/**
 * Represents the main Duke class
 */
public class Duke extends Application {

    /**
     * Represents the type of command
     */
    public enum CommandType {
        BYE, LIST, MARK, UNMARK, TODO, DEADLINE, EVENT, DELETE, FIND
    }

    /**
     * Represents the storage of tasks
     */
    private Storage storage;

    /**
     * Represents the list of tasks
     */
    private TaskList tasks;

    /**
     * Represents the user interface
     */
    private Ui ui;

    /**
     * Represents the parser
     */
    private Parser parser;

    private ScrollPane scrollPane;
    private VBox dialogContainer;
    private TextField userInput;
    private Button sendButton;
    private Scene scene;

    private Image user = new Image(this.getClass().getResourceAsStream("/images/Dauser.jpg"));
    private Image duke = new Image(this.getClass().getResourceAsStream("/images/Daduke.jpg"));


    /**
     * Constructor for Duke
     */
    public Duke() {
        ui = new Ui("KokBot");
        storage = new Storage(Paths.get("data", "duke.txt"));
        parser = new Parser();
        try {
            tasks = new TaskList(storage.load());
        } catch (DukeException e) {
            ui.showLoadingError();
            tasks = new TaskList();
        }
    }

    /**
     * Constructor for Duke
     *
     * @param filePath Path of the file
     * @param botName  Name of the bot
     */
    public Duke(Path filePath, String botName) {
        ui = new Ui(botName);
        storage = new Storage(filePath);
        parser = new Parser();
        try {
            tasks = new TaskList(storage.load());
        } catch (DukeException e) {
            ui.showLoadingError();
            tasks = new TaskList();
        }
    }

    @Override
    public void start(Stage stage) {

        // Step 1. Setting up required components

        // The container for the content of the chat to scroll.
        scrollPane = new ScrollPane();
        dialogContainer = new VBox();
        scrollPane.setContent(dialogContainer);

        userInput = new TextField();
        sendButton = new Button("Send");

        AnchorPane mainLayout = new AnchorPane();
        mainLayout.getChildren().addAll(scrollPane, userInput, sendButton);

        scene = new Scene(mainLayout);

        stage.setScene(scene);
        stage.show();

        // Step 2. Formatting the window to look as expected
        stage.setTitle("Duke");
        stage.setResizable(false);
        stage.setMinHeight(600.0);
        stage.setMinWidth(400.0);

        mainLayout.setPrefSize(400.0, 600.0);

        scrollPane.setPrefSize(385, 535);
        scrollPane.setHbarPolicy(ScrollPane.ScrollBarPolicy.NEVER);
        scrollPane.setVbarPolicy(ScrollPane.ScrollBarPolicy.ALWAYS);

        scrollPane.setVvalue(1.0);
        scrollPane.setFitToWidth(true);

        // You will need to import `javafx.scene.layout.Region` for this.
        dialogContainer.setPrefHeight(Region.USE_COMPUTED_SIZE);

        userInput.setPrefWidth(325.0);

        sendButton.setPrefWidth(55.0);

        AnchorPane.setTopAnchor(scrollPane, 1.0);

        AnchorPane.setBottomAnchor(sendButton, 1.0);
        AnchorPane.setRightAnchor(sendButton, 1.0);

        AnchorPane.setLeftAnchor(userInput, 1.0);
        AnchorPane.setBottomAnchor(userInput, 1.0);

        // Step 3. Add functionality to handle user input.
        // Part 3. Add functionality to handle user input.
//        sendButton.setOnMouseClicked((event) -> {
//            handleUserInput();
//        });
//
//        userInput.setOnAction((event) -> {
//            handleUserInput();
//        });

        // Scroll down to the end every time dialogContainer's height changes.
        dialogContainer.heightProperty().addListener((observable) -> scrollPane.setVvalue(1.0));
    }

    /**
     * Iteration 1:
     * Creates a label with the specified text and adds it to the dialog container.
     *
     * @param text String containing text to add
     * @return a label with the specified text that has word wrap enabled.
     */
    private Label getDialogLabel(String text) {
        // You will need to import `javafx.scene.control.Label`.
        Label textToAdd = new Label(text);
        textToAdd.setWrapText(true);

        return textToAdd;
    }

    /**
     * You should have your own function to generate a response to user input.
     * Replace this stub with your completed method.
     */
    public String getResponse(String input) {
        try {
            Command cmd = parser.parse(input);
            assert cmd != null : "Command should not be null";
            switch (cmd.type) {
            case BYE:
                return ui.showGoodbye();
            case LIST:
                if (cmd.args.length > 0) {
                    return ui.showTaskList(tasks.getTaskStrings(cmd.args[0]));
                } else {
                    return ui.showTaskList(tasks.getTaskStrings(""));
                }
            case MARK:
                int toMark = Integer.parseInt(cmd.args[0]) - 1;
                tasks.markTaskAsDone(toMark);
                return ui.showTaskMarked(tasks.getTask(toMark));
            case UNMARK:
                int toUnmark = Integer.parseInt(cmd.args[0]) - 1;
                tasks.markTaskAsUndone(toUnmark);
                return ui.showTaskUnmarked(tasks.getTask(toUnmark));
            case TODO:
                Todo newTodo = createTodo(cmd.args[0]);
                tasks.addTask(newTodo);
                return ui.showTaskAdded(newTodo, tasks.getSize());
            case DEADLINE:
                Deadline newDeadline = createDeadline(cmd.args[0], cmd.args[1]);
                tasks.addTask(newDeadline);
                return ui.showTaskAdded(newDeadline, tasks.getSize());
            case EVENT:
                Event newEvent = createEvent(cmd.args[0], cmd.args[1], cmd.args[2]);
                tasks.addTask(newEvent);
                return ui.showTaskAdded(newEvent, tasks.getSize());
                //numList(duke.tasks.getSize());
            case DELETE:
                Task deletedTask = tasks.deleteTask(Integer.parseInt(cmd.args[0]) - 1);
                return ui.showTaskDeleted(deletedTask, tasks.getSize());
            case FIND:
                return ui.showMatchingTasks(tasks.getMatchingTasks(cmd.args[0]));
            default:
                throw new DukeException("Unknown command");
            }
        } catch (duke.DukeException e) {
            return e.getMessage();
        } finally {
            save();
        }
    }

    /**
     * Saves the current tasks back to the file
     */
    public void save() {
        try {
            storage.updateFile(tasks.getFileStrings());
        } catch (DukeException e) {
            // do nothing
        }
    }

    /**
     * Creates a LocalDateTime object from a string
     *
     * @param input String to be parsed
     * @return LocalDateTime object
     * @throws DukeException If the string is not in a valid date-time format
     */
    public static LocalDateTime createDateTime(String input) throws DukeException {

        String[] possibleDates = {
                "d/M/yyyy",
                "d-M-yyyy",
                "d/M/yy",
                "d-M-yy",
                "dMMyyyy",
                "dMMyy",

                "dd/MM/yyyy",
                "dd-MM-yyyy",
                "yyyy-MM-dd",

                "dd/MM/yy",
                "dd-MM-yy",
                "ddMMyyyy",
                "ddMMyy",};

        String[] possibleTimes = {"HHmm", "HH:mm", "HH", "h:mma",};

        for (String datePattern : possibleDates) {
            for (String timePattern : possibleTimes) {
                //check that time pattern comes before date pattern
                try {
                    DateTimeFormatter formatter = DateTimeFormatter.ofPattern(timePattern + " " + datePattern);
                    return LocalDateTime.parse(input, formatter);
                } catch (Exception e) {
                    //do nothing
                }
                //check that time pattern comes after date pattern
                try {
                    DateTimeFormatter formatter = DateTimeFormatter.ofPattern(datePattern + " " + timePattern);
                    return LocalDateTime.parse(input, formatter);
                } catch (Exception e) {
                    //do nothing
                }
            }
        }
        return null;
    }

    /**
     * Creates a To do task
     *
     * @param description Description of the To do
     * @return To do task
     * @throws DukeException If the description is empty
     */
    public static Todo createTodo(String description) throws DukeException {
        Todo newTodo = new Todo(description);
        return newTodo;
    }

    /**
     * Creates a Deadline task
     *
     * @param description Description of the Deadline
     * @param dueDate     Due date of the Deadline
     * @return Deadline task
     * @throws DukeException If the due date is not in a valid date-time format
     */
    public static Deadline createDeadline(String description, String dueDate) throws DukeException {

        LocalDateTime dueDateTime = createDateTime(dueDate);
        if (dueDateTime == null) {
            throw new DukeException("Unknown usage - due date of \"deadline\" is not in a valid date-time format.");
        }

        return new Deadline(description, dueDateTime);
    }

    /**
     * Creates an Event task
     *
     * @param description Description of the Event
     * @param startDate   Start date of the Event
     * @param endDate     End date of the Event
     * @return Event task
     * @throws DukeException If the start date is after the end date
     */
    public static Event createEvent(String description, String startDate, String endDate) throws DukeException {

        LocalDateTime startDateTime = createDateTime(startDate);
        if (startDateTime == null) {
            throw new DukeException("Unknown usage - start date of \"event\" is not in a valid date-time format.");
        }

        LocalDateTime endDateTime = createDateTime(endDate);
        if (endDateTime == null) {
            throw new DukeException("Unknown usage - end date of \"event\" is not in a valid date-time format.");
        }

        if (startDateTime.isAfter(endDateTime)) {
            throw new DukeException("Unknown usage - start date of \"event\" is after end date.");
        }

        return new Event(description, startDateTime, endDateTime);
    }
}
