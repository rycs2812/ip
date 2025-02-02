package duke;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;

/**
 * An abstraction for a command to Duke.
 */
public enum Command {
    DELETE("delete"),
    FIND("find"),
    LIST("list"),
    DEADLINE("deadline"),
    EVENT("event"),
    TODO("todo"),
    MARK("mark"),
    UNMARK("unmark");

    private static final String INPUT_DATE_FORMAT = "dd-MM-yyyy";

    private final String name;
    Command(String name) {
        this.name = name;
    }

    /**
     * Returns the response string after running the command with the given Duke bot.
     *
     * @param args Command arguments.
     * @param duke Duke bot executing the command.
     * @return The response string.
     */
    public String run(String args, Duke duke) {
        String errorMessage = getUsageError(args, duke);
        if (!errorMessage.isEmpty()) {
            // If the command is used incorrectly
            return errorMessage;
        }

        switch(name) {
        case "delete":
            int index = Integer.parseInt(args) - 1;
            return duke.deleteTask(index);

        case "find":
            if (args.charAt(0) == '"' && args.charAt(args.length() - 1) == '"') {
                args = args.substring(1, args.length() - 1);
            }
            return duke.findTask(args);

        case "list":
            return duke.listTasks();

        case "deadline":
            String[] temp = args.split(" /by ", 2);
            String[] recurring = temp[1].split(" /every ", 2);
            int period = getPeriod(temp[1]);
            Task t = new Deadline(temp[0], LocalDate.parse(recurring[0].trim(),
                    DateTimeFormatter.ofPattern(INPUT_DATE_FORMAT)), period);;
            return duke.addTask(t);

        case "event":
            temp = args.split(" /at ", 2);
            recurring = temp[1].split(" /every ", 2);
            period = getPeriod(temp[1]);
            t = new Event(temp[0], LocalDate.parse(recurring[0].trim(),
                    DateTimeFormatter.ofPattern(INPUT_DATE_FORMAT)), period);
            return duke.addTask(t);

        case "todo":
            t = new ToDo(args);
            return duke.addTask(t);

        case "mark":
            // Fallthrough

        case "unmark":
            index = Integer.parseInt(args) - 1;
            boolean b = name.equals("mark");
            return duke.markTask(index, b);

        default:
            return "";
        }
    }

    private boolean isValidDate(String date) {
        String dateTrimmed = date.trim();
        try {
            LocalDate d = LocalDate.parse(dateTrimmed, DateTimeFormatter.ofPattern(INPUT_DATE_FORMAT));
            return true;
        } catch (DateTimeParseException e) {
            return false;
        }
    }

    /**
     * Returns an error message if the arguments supplied to the command are invalid, and
     * an empty String otherwise.
     *
     * @param args The command arguments.
     * @param duke The duke bot executing the commands.
     * @return An error message if the arguments are invalid.
     */
    private String getUsageError(String args, Duke duke) {
        TaskList taskList = duke.getTaskList();
        String usage = getCorrectUsage();
        String error = "ERROR: \n\n";
        assert taskList != null : "Task list is null";

        switch(name) {
        case "delete":
            if (args.isEmpty()) {
                return error + "Please specify a task number.\n\n" + usage;
            }
            try {
                int index = Integer.parseInt(args);
                int count = taskList.getCount();
                if (index <= 0 || index > taskList.getCount()) {
                    return error + "Please specify a valid task number. There are "
                            + count + " tasks in the list\n\n" + usage;
                }
            } catch (NumberFormatException e) {
                return error + "Please specify a task number.\n\n" + usage;
            }

            return "";

        case "find":
            if (args.isBlank()) {
                return error + "Please specify at least one keyword.\n\n" + usage;
            } else if (args.charAt(0) != '"' || args.charAt(args.length() - 1) != '"') {
                if (args.split(" ").length > 1) {
                    return error + "'find' only expects 1 argument.\n\n If there are multiple "
                            + "keywords, please enclose them in quotation marks (\"\").";
                }
            } else {
                if (args.length() == 2 || args.substring(1, args.length() - 1).isBlank()) {
                    return error + "Please specify at least one keyword.\n\n" + usage;
                }
            }
            return "";

        case "list":
            if (!args.isEmpty()) {
                return error + "'list' expects no arguments.";
            }
            return "";

        case "deadline":
            String[] temp = args.split(" /by ", 2);
            if (temp.length < 2) {
                return error + "Please specify a task and deadline.\n\n" + usage;
            } else {
                String[] recurring = temp[1].split(" /every ", 2);
                int period = getPeriod(temp[1]);
                if (period < 0) {
                    return error + "Please specify a valid period for recurring tasks.\n\n" + usage;
                } else if (period == 0) {
                    if (!isValidDate(temp[1])) {
                        return error + "Please specify the due date in the right format.\n\n" + usage;
                    }
                }
                if (!isValidDate(recurring[0])) {
                    return error + "Please specify the due date in the right format.\n\n" + usage;
                }
            }
            return "";

        case "event":
            temp = args.split(" /at ", 2);
            if (temp.length < 2) {
                return error + "Please specify an event and date.\n\n" + usage;
            } else {
                String[] recurring = temp[1].split(" /every ", 2);
                int period = getPeriod(temp[1]);
                if (period < 0) {
                    return error + "Please specify a valid period for recurring tasks.\n\n" + usage;
                } else if (period == 0) {
                    if (!isValidDate(temp[1])) {
                        return error + "Please specify the event date in the right format.\n\n" + usage;
                    }
                }
                if (!isValidDate(recurring[0])) {
                    return error + "Please specify the due date in the right format.\n\n" + usage;
                }
            }

            return "";

        case "todo":
            if (args.isEmpty()) {
                return error + "Please specify a task.\n\n" + usage;
            }
            return "";

        case "mark":
            // Fallthrough

        case "unmark":
            if (args.isEmpty()) {
                return error + "Please specify a task number\n\n" + usage;
            } else {
                try {
                    int index = Integer.parseInt(args) - 1;
                    if (index < 0 || index >= taskList.getCount()) {
                        return error + "Please specify a valid task number.\n"
                                + "There are " + taskList.getCount()
                                + " task(s) in the list.\n\n" + usage;
                    }
                } catch (NumberFormatException e) {
                    return error + "Please specify a task number.\n"
                            + "\"" + args + "\"" + " is not a task number\n\n" + usage;
                }
            }
            return "";

        default:
            return "";
        }
    }

    private int getPeriod(String args) {
        int period = 0;
        String[] temp = args.split(" /every ", 2);
        if (temp.length < 2) {
            return period; // No period supplied
        } else {
            try {
                period = Integer.parseInt(temp[1].trim());
                return period > 0 ? period : -1;
            } catch (NumberFormatException e) {
                return -1; // Invalid period supplied
            }
        }
    }

    private String getCorrectUsage() {
        String ret = "Correct usage: ";
        switch(name) {
        case "delete":
            return ret + "delete [task-number]";

        case "find":
            return ret + "find [keyword(s)]";

        case "deadline":
            return "Usage #1: deadline [task-description] /by [DD-MM-YYYY]\n\n"
                    + "Usage #2: deadline [task-description] /by [DD-MM-YYYY] /every [how-many-days]";

        case "event":
            return "Usage #1: event [task-description] /at [DD-MM-YYYY]\n\n"
                    + "Usage #2: event [task-description] /at [DD-MM-YYYY] /every [how-many-days]";

        case "todo":
            return ret + "todo [task-description]";

        case "mark":
            return ret + "mark [task-number]";

        case "unmark":
            return ret + "unmark [task-number]";

        default:
            return "";
        }
    }
}
