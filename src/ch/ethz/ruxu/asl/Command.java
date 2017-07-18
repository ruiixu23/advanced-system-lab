package ch.ethz.ruxu.asl;

enum Command {
    // The request is a delete command to remove a single attachment from the server
    DELETE ("d"),

    // The request is a get command to retrieve a single attachment from the server
    GET ("g"),

    // The request is a set command to store a single attachment to the server
    SET ("s");

     // Short-hand name of the command
    private final String name;

    /**
     * Construct a command enum with the specified short-hand name
     * @param name The short-hand name
     */
    Command(String name) {
        this.name = name;
    }

    @Override
    public String toString() {
        return this.name;
    }
}
