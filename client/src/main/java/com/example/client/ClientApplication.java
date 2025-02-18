package com.example.client;

import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.observation.DefaultMeterObservationHandler;
import io.micrometer.observation.Observation;
import io.micrometer.observation.ObservationRegistry;
import io.micrometer.tracing.Tracer;
import io.micrometer.tracing.handler.TracingAwareMeterObservationHandler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.WebApplicationType;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.builder.SpringApplicationBuilder;
import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.web.client.RestTemplate;

@SpringBootApplication
@Import(ExemplarsConfiguration.class)
public class ClientApplication {

	private static final Logger log = LoggerFactory.getLogger(ClientApplication.class);

	public static void main(String[] args) {
		SpringApplication.run(ClientApplication.class, args);
	}

	// tag::rest-template[]
	// IMPORTANT! To instrument RestTemplate you must inject the RestTemplateBuilder
	@Bean
	RestTemplate restTemplate(RestTemplateBuilder builder) {
		return builder.build();
	}
	// end::rest-template[]

	// tag::runner[]
	@Bean
	CommandLineRunner myCommandLineRunner(ObservationRegistry registry, RestTemplate restTemplate) {
		return args -> {
			// Example of using the Observability API manually
			// <my.observation> is a "technical" name that does not depend on the context. It will be used to name e.g. Metrics
			 Observation.createNotStarted("my.observation", registry)
					 // Low cardinality means that the number of potential values won't be big. Low cardinality entries will end up in e.g. Metrics
					.lowCardinalityKeyValue("low.cardinality.key", "low cardinality value")
					 // High cardinality means that the number of potential values can be large. High cardinality entries will end up in e.g. Spans
					.highCardinalityKeyValue("high.cardinality.key", "high cardinality value")
					 // <command-line-runner> is a "contextual" name that gives more details within the provided context. It will be used to name e.g. Spans
					.contextualName("command-line-runner")
					 // The following lambda will be executed with an observation scope (e.g. all the MDC entries will be populated with tracing information). Also the observation will be started, stopped and if an error occurred it will be recorded on the observation
					.observe(() -> {
						log.info("Will send a request to the server"); // Since we're in an observation scope - this log line will contain tracing MDC entries ...
						String response = restTemplate.getForObject("http://localhost:7654/foo", String.class); // Boot's RestTemplate instrumentation creates a child span here
						log.info("Got response [{}]", response); // ... so will this line
					});

		};
	}
	// end::runner[]
}
