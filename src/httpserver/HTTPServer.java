package httpserver;

import audioStreaming.MicService;
import audioStreaming.SpeakerService;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.exc.UnrecognizedPropertyException;
import httpserver.status.ActionResult;
import httpserver.status.AudioStatus;
import io.javalin.Javalin;
import io.javalin.http.Context;
import main.Properties;
import main.PropertyKey;
import main.SotaSystemController;

import java.util.function.BiFunction;
import java.util.function.Supplier;

public class HTTPServer {
    Javalin app = null;
//    SotaSystemController controller = null;
    private final ObjectMapper mapper = new ObjectMapper();

    public HTTPServer(SotaSystemController controller) {
        int httpPort = Properties.getPropAsInt(PropertyKey.KEY_NET_HTTP_PORT);
        app = Javalin.create(config -> {
            config.defaultContentType = "application/json";
        }).start(httpPort);

//        this.controller = controller;

        ObjectMapper mapper = new ObjectMapper();  // manual JSON parsing for better error handling
        mapper.findAndRegisterModules();
    }

    private void setCtxError(Context ctx, String error) {
        ctx.status(400); // malformed request
        ctx.json(java.util.Collections.singletonMap( "Error", error ));
    }

    private void createStatusEndpoint(
            String path,
            Supplier<AudioStatus> getter,
            BiFunction<AudioStatus, String, ActionResult> setter) {

        app.get(path, ctx -> {
            ctx.json(getter.get());
        });

        app.post(path, ctx -> {
            try {
                AudioStatus req = mapper.readValue(ctx.body(), (Class<AudioStatus>) AudioStatus.class);
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

    public void enableMicEndpoints(MicService micService) {
        createStatusEndpoint(
                "/mic",
                micService::getStatus,
                micService::setStatus
        );
    }

    public void enableSpeakerEndpoints(SpeakerService speakerService) {
        createStatusEndpoint(
                "/speaker",
                speakerService::getStatus,
                (req, ip) -> speakerService.setStatus(req)
        );
    }
}
