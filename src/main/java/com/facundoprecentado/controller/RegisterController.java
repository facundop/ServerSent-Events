package com.facundoprecentado.controller;

import java.io.IOException;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import org.springframework.http.MediaType;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import com.facundoprecentado.model.State;

@Controller
public class RegisterController {
	
    /** List of all emitters connected to the service. */
    private final List<SseEmitter> sseEmitter = new LinkedList<>();
    /** Temporary state for demonstration. */
    private State state = new State(0);
    /** Counter for state changes. */
    private int counter = 1;

    private final Runnable changeState = () -> {
        System.out.println("Sending auto message " + counter);
        state = new State(counter++);

        synchronized (sseEmitter) {
            sseEmitter.forEach((SseEmitter emitter) -> {
                try {
                    emitter.send(state, MediaType.APPLICATION_JSON);
                } catch (IOException e) {
                    emitter.complete();
                    sseEmitter.remove(emitter);
                }
            });
        }
    };

    /**
     * Initialize the controller and start a repeated task to model state changes.
     */
    public RegisterController() {
        ScheduledExecutorService scheduledPool = Executors.newScheduledThreadPool(4);
        scheduledPool.scheduleWithFixedDelay(changeState, 0, 1, TimeUnit.SECONDS);
    }

    /**
     * Some website.
     * @param model model in spring mvc
     * @return a .jsp ressource
     */
    @RequestMapping( "/" )
    public String sseExamplePage(Map<String, Object> model) {
        model.put("state", state);
        return "index";
    }

    /**
     * Viewer can register here to get sse messages.
     * @return an server state event emitter
     * @throws IOException if registering the new emitter fails
     */
    @RequestMapping (path = "/register", method = RequestMethod.GET)
    public SseEmitter register() throws IOException {
    	System.out.println("Registering a stream.");

        SseEmitter emitter = new SseEmitter();

        synchronized (sseEmitter) {
            sseEmitter.add(emitter);
        }
        emitter.onCompletion(() -> sseEmitter.remove(emitter));

        return emitter;
    }

}