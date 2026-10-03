package dto.Error;

public record ErrorResponse(
        String message,
        int status,
        long timestamp
) {
    public ErrorResponse(String message, int status) {
        this(message, status, System.currentTimeMillis());
    }
}