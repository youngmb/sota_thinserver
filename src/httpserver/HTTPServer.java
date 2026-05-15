package httpserver;

import audioStreaming.MicService;
import audioStreaming.SpeakerService;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.exc.UnrecognizedPropertyException;
import audioStreaming.AudioStatus;
import io.javalin.Javalin;
import io.javalin.http.Context;
import main.Properties;
import main.PropertyKey;
import main.SotaSystemController;
import sota.servos.ServoService;
import sota.servos.ServoSystemStatus;
import sota.servos.ServosCommand;
import sota.servos.ServosStatus;

import java.util.function.BiFunction;
import java.util.function.Supplier;

public class HTTPServer {
    Javalin app = null;

    private final ObjectMapper mapper = new ObjectMapper();

    public HTTPServer(SotaSystemController controller) {
        int httpPort = Properties.getPropAsInt(PropertyKey.KEY_NET_HTTP_PORT);
        app = Javalin.create(config -> {
            config.defaultContentType = "application/json";
        }).start(httpPort);

        ObjectMapper mapper = new ObjectMapper();  // manual JSON parsing for better error handling
        mapper.findAndRegisterModules();
    }

    private void setCtxError(Context ctx, String error) {
        ctx.status(400); // malformed request
        ctx.json(java.util.Collections.singletonMap( "Error", error ));
    }

    private <getT, postT> void createStatusEndpoint(
            String path,
            Supplier<getT> getter,
            BiFunction<postT, String, ActionResult> setter,
            Class<postT> postType  // needed because of type erasure
    ) {

        app.get(path, ctx -> {
            ctx.json(getter.get());
        });

        app.post(path, ctx -> {
            try {
                postT req = mapper.readValue(ctx.body(), postType);
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

    // THESE are templated but Java solves the template types by the parameter list
    public void enableMicEndpoints(MicService micService) {
        createStatusEndpoint(
                "/mic",
                micService::getStatus,
                micService::postStatus,
                AudioStatus.class
        );
    }

    public void enableSpeakerEndpoints(SpeakerService speakerService) {
        createStatusEndpoint(
                "/speaker",
                speakerService::getStatus,
                speakerService::postStatus,
                AudioStatus.class
        );
    }

    public void enableMotorEndpoints(ServoService motorService) {

        // general motor information
        createStatusEndpoint(
                "/servos/info",
                motorService::getSystemStatus,
                motorService::postSystemStatus,
                ServoSystemStatus.class
        );

        // joint-space radians
        createStatusEndpoint(
                "/servos/jointspace",
                motorService::getJointSpaceStatus,
                motorService::postJointSpaceStatus,
                ServosCommand.class
        );

        // world-space cartesian coordinates
        createStatusEndpoint(
                "/servos/worldspace_skeleton",
                motorService::getWorldSpaceStatus,
                motorService::postWorldSpaceStatus,
                ServosStatus.class
        );
    }
}