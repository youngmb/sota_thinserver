package httpserver;

public class ActionResult {
    public final boolean success;
    public final String error;

    public ActionResult(boolean success, String error) {
        this.success = success;
        this.error = error;
    }

    public static ActionResult ok() {
        return new ActionResult(true, null);
    }

    public static ActionResult fail(String error) {
        return new ActionResult(false, error);
    }
}