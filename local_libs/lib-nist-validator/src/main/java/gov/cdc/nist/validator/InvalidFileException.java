package gov.cdc.nist.validator;

public class InvalidFileException extends Exception {
    public InvalidFileException(String message) {
        super(message);
    }
}
