package reactive.test;

import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.Test;
import org.reactivestreams.Subscription;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

@Slf4j
public class MonoTest {

    @Test
    public void monoSubscriber() {
        String name = "Soares";
        Mono<String> mono = Mono.just(name).log();

        mono.subscribe();

        log.info("-----------------------");
        StepVerifier.create(mono)
                .expectNext(name)
                .verifyComplete();
    }

    @Test
    public void subscriberConsumer() {
        String name = "Soares";
        Mono<String> mono = Mono.just(name).log();

        mono.subscribe(s -> log.info("Value {}", s));

        log.info("-----------------------");
        StepVerifier.create(mono)
                .expectNext(name)
                .verifyComplete();
    }

    @Test
    public void subscriberConsumerError() {
        String name = "Soares";
        Mono<String> mono = Mono.just(name)
                .map(s -> {
                    throw new RuntimeException("Testing mono with error");
                });

        mono.subscribe(s -> log.info("Value {}", s), s -> log.error("Something bad happened"));
        mono.subscribe(s -> log.info("Value {}", s), Throwable::printStackTrace);

        log.info("-----------------------");
        StepVerifier.create(mono)
                .expectError(RuntimeException.class)
                .verify();
    }

    @Test
    public void monoSubscriberConsumerComplete() {
        String name = "Soares";
        Mono<String> mono = Mono.just(name)
                .log()
                .map(String::toUpperCase);

        mono.subscribe(s -> log.info("Value {}", s),
                Throwable::printStackTrace, () -> log.info("FINISHED"));

        log.info("-----------------------");
        StepVerifier.create(mono)
                .expectNext(name.toUpperCase())
                .verifyComplete();
    }

    @Test
    public void monoSubscriberConsumerSubscription() {
        String name = "Soares";
        Mono<String> mono = Mono.just(name)
                .log()
                .map(String::toUpperCase);

        mono.subscribe(s -> log.info("Value {}", s),
                Throwable::printStackTrace,
                () -> log.info("FINISHED"),
                Subscription::cancel);

        log.info("-----------------------");
        StepVerifier.create(mono)
                .expectNext(name.toUpperCase())
                .verifyComplete();
    }

    @Test
    public void monoDoOnMethods() {
        String name = "Soares";
        Mono<Object> mono = Mono.just(name)
                .log()
                .map(String::toUpperCase)
                .doOnSubscribe(subcription -> log.info("Subscribed"))
                .doOnRequest(longNumber -> log.info("Request Received, starting doing something..."))
                .doOnNext(s -> log.info("value is here. Executing doNext {}", s))
                .flatMap(s -> Mono.empty())
                .doOnNext(s -> log.info("value is here. Executing doNext {}", s))
                .doOnSuccess(s -> log.info("doOnSuccess executed", s));

        mono.subscribe(s -> log.info("Value {}", s),
                Throwable::printStackTrace, () -> log.info("FINISHED"));
    }

    @Test
    public void monoDoOnError() {
        Mono<Object> error = Mono.error(new IllegalAccessError("Illegal argument exception"))
                .doOnError(e -> MonoTest.log.error("Error message: {}", e.getMessage()))
                // esta linha na continua pois finaliza no 'doOnError'
                .doOnNext(s -> log.info("Executing this doOnNext"))
                .log();

        error.subscribe();
        StepVerifier.create(error)
                .expectError(IllegalAccessError.class)
                .verify();
    }

    @Test
    public void monoDoOnErrorResume() {
        String name = "Soares";
        Mono<Object> error = Mono.error(new IllegalAccessError("Illegal argument exception"))
                .doOnError(e -> MonoTest.log.error("Error message: {}", e.getMessage()))
                // aplicacao continua por conta do 'onErrorResume'
                .onErrorResume(s ->{
                    log.info("Inside Error");
                    return Mono.just(name);
                })
                .log();

        error.subscribe();
        StepVerifier.create(error)
                .expectNext(name)
                .verifyComplete();
    }

    @Test
    public void monoDoOnErrorReturn() {
        String name = "Soares";
        Mono<Object> error = Mono.error(new IllegalAccessError("Illegal argument exception"))
                .onErrorReturn("Empty")
                // aplicacao continua por conta do 'onErrorResume'
                .onErrorResume(s ->{
                    log.info("Inside Error");
                    return Mono.just(name);
                })
                .log();

        error.subscribe();
        StepVerifier.create(error)
                .expectNext("Empty")
                .verifyComplete();
    }

}
