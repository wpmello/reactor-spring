package reactive.test;

import lombok.AllArgsConstructor;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.ToString;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;
import reactor.test.StepVerifier;

import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.util.List;
import java.util.concurrent.atomic.AtomicLong;

@Slf4j
public class OperatorsTest {

    @Test
    public void subscribeOnSingle() {
        Flux<Integer> flux = Flux.range(1, 4)
                .map(i -> {
                    log.info("Map 1 - Number {} Thread {}", i, Thread.currentThread().getName());
                    return i;
                })
                .subscribeOn(Schedulers.boundedElastic())
                .map(i -> {
                    log.info("Map 2 - Number {} Thread {}", i, Thread.currentThread().getName());
                    return i;
                });

        StepVerifier.create(flux)
                .expectSubscription()
                .expectNext(1, 2, 3, 4)
                .verifyComplete();
    }

    @Test
    public void publishOnSingle() {
        Flux<Integer> flux = Flux.range(1, 4)
                .map(i -> {
                    log.info("Map 1 - Number {} Thread {}", i, Thread.currentThread().getName());
                    return i;
                })
                .publishOn(Schedulers.boundedElastic())
                .map(i -> {
                    log.info("Map 2 - Number {} Thread {}", i, Thread.currentThread().getName());
                    return i;
                });

        flux.subscribe();
        flux.subscribe();
        StepVerifier.create(flux)
                .expectSubscription()
                .expectNext(1, 2, 3, 4)
                .verifyComplete();
    }

    @Test
    public void multipleSubscribeOnSingle() {
        Flux<Integer> flux = Flux.range(1, 4)
                .subscribeOn(Schedulers.single())
                .map(i -> {
                    log.info("Map 1 - Number {} Thread {}", i, Thread.currentThread().getName());
                    return i;
                })
                .subscribeOn(Schedulers.boundedElastic())
                .map(i -> {
                    log.info("Map 2 - Number {} Thread {}", i, Thread.currentThread().getName());
                    return i;
                });

        StepVerifier.create(flux)
                .expectSubscription()
                .expectNext(1, 2, 3, 4)
                .verifyComplete();
    }

    @Test
    public void multiplePublishOnSingle() {
        Flux<Integer> flux = Flux.range(1, 4)
                .publishOn(Schedulers.single())
                .map(i -> {
                    log.info("Map 1 - Number {} Thread {}", i, Thread.currentThread().getName());
                    return i;
                })
                .publishOn(Schedulers.boundedElastic())
                .map(i -> {
                    log.info("Map 2 - Number {} Thread {}", i, Thread.currentThread().getName());
                    return i;
                });

        flux.subscribe();
        flux.subscribe();
        StepVerifier.create(flux)
                .expectSubscription()
                .expectNext(1, 2, 3, 4)
                .verifyComplete();
    }

    @Test
    public void publishAndSubscribeOnSingle() {
        Flux<Integer> flux = Flux.range(1, 4)
                .publishOn(Schedulers.single())
                .map(i -> {
                    log.info("Map 1 - Number {} Thread {}", i, Thread.currentThread().getName());
                    return i;
                })
                .subscribeOn(Schedulers.boundedElastic())
                .map(i -> {
                    log.info("Map 2 - Number {} Thread {}", i, Thread.currentThread().getName());
                    return i;
                });

        StepVerifier.create(flux)
                .expectSubscription()
                .expectNext(1, 2, 3, 4)
                .verifyComplete();
    }

    @Test
    public void subscribeAndPublishOnSingle() {
        Flux<Integer> flux = Flux.range(1, 4)
                .subscribeOn(Schedulers.single())
                .map(i -> {
                    log.info("Map 1 - Number {} Thread {}", i, Thread.currentThread().getName());
                    return i;
                })
                .publishOn(Schedulers.boundedElastic())
                .map(i -> {
                    log.info("Map 2 - Number {} Thread {}", i, Thread.currentThread().getName());
                    return i;
                });

        StepVerifier.create(flux)
                .expectSubscription()
                .expectNext(1, 2, 3, 4)
                .verifyComplete();
    }

    @Test
    public void subscribeOnIO() {
        Mono<List<String>> list = Mono.fromCallable(() -> Files.readAllLines(Path.of("text-test-file")))
                .log()
                .subscribeOn(Schedulers.boundedElastic());

//        list.subscribe(strings -> log.info("{}", strings));

        StepVerifier.create(list)
                .expectSubscription()
                .thenConsumeWhile(l -> {
                    Assertions.assertFalse(l.isEmpty());
                    log.info("Size {}", l.size());
                    return true;
                })
                .verifyComplete();
    }

    @Test
    public void switchEmptyOperator() {
        Flux<Object> flux = emptyFlux()
                .switchIfEmpty(Flux.just("not empty anymore"))
                .log();

        StepVerifier.create(flux)
                .expectSubscription()
                .expectNext("not empty anymore")
                .expectComplete()
                .verify();
    }

    private Flux<Object> emptyFlux() {
        return Flux.empty();
    }

    @Test
    public void deferOperator() throws InterruptedException {
        Mono<Long> just = Mono.just(System.currentTimeMillis());
        Mono<Long> defer = Mono.defer(() -> Mono.just(System.currentTimeMillis()));

        just.subscribe(j -> log.info("time {}", j));
        Thread.sleep(100);
        just.subscribe(j -> log.info("time {}", j));
        Thread.sleep(100);
        just.subscribe(j -> log.info("time {}", j));
        Thread.sleep(100);
        just.subscribe(j -> log.info("time {}", j));


        defer.subscribe(j -> log.info("time {}", j));
        Thread.sleep(100);
        defer.subscribe(j -> log.info("time {}", j));
        Thread.sleep(100);
        defer.subscribe(j -> log.info("time {}", j));
        Thread.sleep(100);
        defer.subscribe(j -> log.info("time {}", j));

        AtomicLong atomicLong = new AtomicLong();
        defer.subscribe(atomicLong::set);
        Assertions.assertTrue(atomicLong.get() > 0);
    }

    @Test
    public void concatOperator() {
        Flux<String> flux1 = Flux.just("a", "b");
        Flux<String> flux2 = Flux.just("c", "d");

        Flux<String> concat = Flux.concat(flux1, flux2).log();

        StepVerifier.create(concat)
                .expectSubscription()
                .expectNext("a", "b", "c", "d")
                .expectComplete()
                .verify();
    }

    @Test
    public void concatWithOperator() {
        Flux<String> flux1 = Flux.just("a", "b");
        Flux<String> flux2 = Flux.just("c", "d");

        Flux<String> concatFlux = flux1.concatWith(flux2).log();

        StepVerifier.create(concatFlux)
                .expectSubscription()
                .expectNext("a", "b", "c", "d")
                .expectComplete()
                .verify();
    }

    @Test
    public void concatOperatorError() {
        Flux<String> flux1 = Flux.just("a", "b")
                .map(f -> {
                    if (f.equals("b")) {
                        throw new IllegalArgumentException();
                    }
                    return f;
                });
        Flux<String> flux2 = Flux.just("c", "d");

//            Aplicacao para de rodar ao chegar no erro
//        Flux<String> concat = Flux.concat(flux1, flux2).log();

//        aplicacao continua roando apos o erro
        Flux<String> concat = Flux.concatDelayError(flux1, flux2).log();

        StepVerifier.create(concat)
                .expectSubscription()
                .expectNext("a", "c", "d")
                .expectError()
                .verify();
    }


    @Test
    public void combineLatestOperator() {
        Flux<String> flux1 = Flux.just("a", "b");
        Flux<String> flux2 = Flux.just("c", "d");

        Flux<String> combineLatest = Flux.combineLatest(flux1, flux2,
                        (f1, f2) -> f1.toUpperCase() + f2.toUpperCase())
                .log();

        StepVerifier.create(combineLatest)
                .expectSubscription()
                .expectNext("BC", "BD")
                .expectComplete()
                .verify();
    }

    @Test
    public void mergeOperator() throws InterruptedException {
        Flux<String> flux1 = Flux.just("a", "b").delayElements(Duration.ofMillis(200));
        Flux<String> flux2 = Flux.just("c", "d");

        Flux<String> mergeFlux = Flux.merge(flux1, flux2)
                .delayElements(Duration.ofMillis(200))
                .log();

        mergeFlux.subscribe(log::info);

        Thread.sleep(1000);

        StepVerifier.create(mergeFlux)
                .expectSubscription()
                .expectNext("c", "d", "a", "b")
                .expectComplete()
                .verify();
    }

    @Test
    public void mergeWithOperator() throws InterruptedException {
        Flux<String> flux1 = Flux.just("a", "b").delayElements(Duration.ofMillis(200));
        Flux<String> flux2 = Flux.just("c", "d");

        Flux<String> mergeWith = flux1.mergeWith(flux2).delayElements(Duration.ofMillis(200))
                .log();

        mergeWith.subscribe(OperatorsTest.log::info);

        Thread.sleep(1000);

        StepVerifier.create(mergeWith)
                .expectSubscription()
                .expectNext("c", "d", "a", "b")
                .expectComplete()
                .verify();
    }

    @Test
    public void mergeSequentialOperator() {
        Flux<String> flux1 = Flux.just("a", "b").delayElements(Duration.ofMillis(200));
        Flux<String> flux2 = Flux.just("c", "d");

        Flux<String> mergeSequential = Flux.mergeSequential(flux1, flux2, flux1)
                .delayElements(Duration.ofMillis(200))
                .log();

        StepVerifier.create(mergeSequential)
                .expectSubscription()
                .expectNext("a", "b", "c", "d", "a", "b")
                .expectComplete()
                .verify();
    }

    @Test
    public void mergeDelayErrorOperator() {
        Flux<String> flux1 = Flux.just("a", "b")
                .map(f -> {
                    if (f.equals("b")) {
                        throw new IllegalArgumentException();
                    }
                    return f;
                }).doOnError(f -> log.info("We could do something with this"));

        Flux<String> flux2 = Flux.just("c", "d");

        Flux<String> mergeError = Flux.mergeDelayError(1, flux1, flux2, flux1)
                .log();

        mergeError.subscribe(log::info);

        StepVerifier.create(mergeError)
                .expectSubscription()
                .expectNext("a", "c", "d", "a")
                .expectError()
                .verify();
    }

    @Test
    public void flatMapOperator() {
        Flux<String> flux = Flux.just("a", "b");

        Flux<String> flatFlux = flux.map(String::toUpperCase)
                .flatMap(this::findByName)
                .log();

        StepVerifier.create(flatFlux)
                .expectSubscription()
                .expectNext("nameB1", "nameB2", "nameA1", "nameA2")
                .verifyComplete();
    }

    @Test
    public void flatMapSequentialOperator() {
        Flux<String> flux = Flux.just("a", "b");

        Flux<String> flatFlux = flux.map(String::toUpperCase)
                .flatMapSequential(this::findByName)
                .log();

        StepVerifier.create(flatFlux)
                .expectSubscription()
                .expectNext("nameA1", "nameA2", "nameB1", "nameB2")
                .verifyComplete();
    }

    @Test
    public void zipOperator() {
        Flux<String> titleFlux = Flux.just("Grand blue", "Baki");
        Flux<String> studioFlux = Flux.just("Zero-G", "TMS Entertainment");
        Flux<Integer> episodesFlux = Flux.just(12, 21);

        Flux<Anime> animeFlux = Flux.zip(titleFlux, studioFlux, episodesFlux)
                .flatMap(tuple -> Flux.just(new Anime(tuple.getT1(), tuple.getT2(), tuple.getT3())));

        StepVerifier
                .create(animeFlux)
                .expectSubscription()
                .expectNext(new Anime("Grand blue", "Zero-G", 12),
                        new Anime("Baki", "TMS Entertainment", 21))
                .verifyComplete();
    }

    @Test
    public void zipWithOperator() {
        Flux<String> titleFlux = Flux.just("Grand blue", "Baki");
        Flux<String> studioFlux = Flux.just("Zero-G", "TMS Entertainment");
        Flux<Integer> episodesFlux = Flux.just(12, 21);

        Flux<Anime> animeFlux = titleFlux.zipWith(episodesFlux)
                .flatMap(tuple -> Flux.just(new Anime(tuple.getT1(), null, tuple.getT2())));

        StepVerifier
                .create(animeFlux)
                .expectSubscription()
                .expectNext(new Anime("Grand blue", null, 12),
                        new Anime("Baki", null, 21))
                .verifyComplete();
    }

    private Flux<String> findByName(String name) {
        return name.equals("A") ? Flux.just("nameA1", "nameA2").delayElements(Duration.ofMillis(100)) : Flux.just("nameB1", "nameB2");
    }

    @Getter
    @ToString
    @EqualsAndHashCode
    @AllArgsConstructor
    class Anime {
        private String title;
        private String studio;
        private int episodes;
    }

}
