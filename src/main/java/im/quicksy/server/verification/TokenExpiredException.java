package im.quicksy.server.verification;

public class TokenExpiredException extends RequestFailedException {
    public TokenExpiredException(String message, int code) {
        super(message, code);
    }

    public TokenExpiredException(String message) {
        super(message, 0);
    }

    public TokenExpiredException(Exception e) {
        super(e);
    }
}
