package httpserver;

import audioStreaming.MicService;
import audioStreaming.SpeakerService;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.exc.UnrecognizedPropertyException;
import audioStreaming.AudioStatus;
import com.fasterxml.jackson.databind.module.SimpleModule;
import io.javalin.Javalin;
import io.javalin.http.Context;
import io.javalin.plugin.json.JavalinJackson;
import main.Properties;
import main.PropertyKey;
import main.SotaSystemController;
import sota.pose.PoseService;
import sota.pose.PoseSystemStatus;
import sota.pose.PoseCommand;

import java.awt.*;
import java.util.function.BiFunction;
import java.util.function.Supplier;

public class HTTPServer {
    Javalin app = null;

    private final ObjectMapper mapper = new ObjectMapper();

    public HTTPServer(SotaSystemController controller) {
        int httpPort = Properties.getPropAsInt(PropertyKey.KEY_NET_HTTP_PORT);

        ObjectMapper mapper = JavalinJackson.getObjectMapper();

        // register our JSON color serializer
        SimpleModule module = new SimpleModule();
        module.addSerializer(Color.class, new ColorSerializer.Serializer());
        module.addDeserializer(Color.class, new ColorSerializer.Deserializer());
        mapper.registerModule(module);

        JavalinJackson.configure(mapper);

        app = Javalin.create(config -> {
            config.defaultContentType = "application/json";
        }).start(httpPort);
    }

    public void stop() {        if (app != null)  app.stop();     }


    private void setCtxError(Context ctx, String error) {   // generate an error response instead of stack trace
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

    public void enablePoseEndpoints(PoseService PoseService) {

        // general motor information
        createStatusEndpoint(
                "/pose/system",
                PoseService::getSystemStatus,
                PoseService::postSystemStatus,
                PoseSystemStatus.class
        );

        createStatusEndpoint(
                "/pose",
                PoseService::getPoseStatus,
                PoseService::postPoseStatus,
                PoseCommand.class
        );
    }
}