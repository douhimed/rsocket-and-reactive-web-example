package org.adex.serverrsocketapp;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.extern.log4j.Log4j2;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.messaging.handler.annotation.DestinationVariable;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.stereotype.Controller;

import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.time.Duration;
import java.time.Instant;
import java.util.stream.Stream;

@SpringBootApplication
public class ServerRsocketAppApplication {

    public static void main(String[] args) {
        SpringApplication.run(ServerRsocketAppApplication.class, args);
    }

}

@Controller
@Log4j2
class RequestController {

    @MessageMapping(Constants.MONO_SERVER)
    public Mono<Response> monoToClient(@Payload Request request) {
        return Mono.just(buildMessage(request));
    }

    @MessageMapping(Constants.STREAM_SERER)
    public Flux<Response> streamToClient(@Payload Request request){
        return Flux.fromStream(Stream.generate(()->buildMessage(request))).delayElements(Duration.ofSeconds(1));
    }

    @MessageMapping(Constants.EMPTY_SERVER + "/{name}")
    public Mono<Void> fireAndForget(@DestinationVariable String name){
        log.info(">>> log for fire and forget service : " + name);
        return Mono.empty();
    }

    private Response buildMessage(@Payload Request request) {
        return new Response("Hello " + request.getName() + ", " + Instant.now());
    }

}

@Data
@NoArgsConstructor
@AllArgsConstructor
class Response {
    private String message;
}

@Data
@NoArgsConstructor
@AllArgsConstructor
class Request {
    private String name;
}


final class Constants {
     public final static String MONO_SERVER = "mono-server";
     public final static String STREAM_SERER = "stream-server";
     public final static String EMPTY_SERVER = "fire-and-forget-server";

     private Constants(){
         throw new RuntimeException("Utility class");
     }
}