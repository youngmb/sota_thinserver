package httpserver;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.exc.UnrecognizedPropertyException;
import httpserver.status.ActionResult;
import httpserver.status.MicStatus;
import httpserver.status.SpeakerStatus;
import io.javalin.Javalin;
import io.javalin.http.Context;
import main.Properties;
import main.PropertyKey;
import main.SotaSystemController;

import java.util.function.BiConsumer;
import java.util.function.BiFunction;
import java.util.function.Supplier;

public class HTTPServer {
    private final int httpPort = Properties.getPropAsInt(PropertyKey.KEY_NET_HTTP_PORT);
    Javalin app = null;
    SotaSystemController controller = null;
    private final ObjectMapper mapper = new ObjectMapper();

    public HTTPServer(SotaSystemController controller) {
        app = Javalin.create(config -> {
            config.defaultContentType = "application/json";
        }).start(httpPort);

        this.controller = controller;

        ObjectMapper mapper = new ObjectMapper();  // manual JSON parsing for better error handling
        mapper.findAndRegisterModules();
    }

    private void setCtxError(Context ctx, String error) {
        ctx.status(400); // malformed request
        ctx.json(java.util.Collections.singletonMap( "Error", error ));
    }

    private <T> void createStatusEndpoint(
            String path,
            Class<T> statusClass,
            Supplier<T> getter,
            BiFunction<T, String, ActionResult> setter) {

        app.get(path, ctx -> {
            ctx.json(getter.get());
        });

        app.post(path, ctx -> {
            try {
                T req = mapper.readValue(ctx.body(), statusClass);
                ActionResult ar = setter.apply(req, ctx.ip());

                if (ar.success)
                   ctx.json(getter.get());
                else
                    setCtxError(ctx, ar.error);

            } catch (UnrecognizedPropertyException e) {
                setCtxError(ctx, "Unknown field: '" + e.getPropertyName()+"'.");

            } catch (Exception e) {
                setCtxError(ctx, e.getMessage());
            }
        });
    }


    public void enableMicEndpoints() {
        createStatusEndpoint(
                "/mic",
                MicStatus.class,
                () -> controller.http_getMicStatus(),
                (req, ip) -> controller.http_setMicStatus(req, ip)
        );
    }

    public void enableSpeakerEndpoints() {
        createStatusEndpoint(
                "/speaker",
                SpeakerStatus.class,
                () -> controller.http_getSpeakerStatus(),
                (req, ip) -> controller.http_setSpeakerStatus(req)
        );
    }
}
