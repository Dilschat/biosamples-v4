package uk.ac.ebi.biosamples;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.ExitCodeGenerator;
import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.context.annotation.Profile;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.core.annotation.Order;
import org.springframework.http.MediaType;
import org.springframework.http.RequestEntity;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestOperations;
import org.springframework.web.util.UriComponentsBuilder;
import uk.ac.ebi.biosamples.client.BioSamplesClient;
import uk.ac.ebi.biosamples.model.Autocomplete;

import java.net.URI;

@Component
@Order(4)
@Profile({"default", "rest"})
public class RestAutocompleteRunner implements ApplicationRunner, ExitCodeGenerator {

	private Logger log = LoggerFactory.getLogger(this.getClass());

	private final IntegrationProperties integrationProperties;

	private final RestOperations restTemplate;

	private final BioSamplesClient client;
	
	public RestAutocompleteRunner(RestTemplateBuilder restTemplateBuilder, IntegrationProperties integrationProperties, BioSamplesClient client) {
		this.client = client;
		this.restTemplate = restTemplateBuilder.build();
		this.integrationProperties = integrationProperties;
	}
	
	private int exitCode = 1;

	@Override
	public void run(ApplicationArguments args) throws Exception {

		log.info("Starting RestAutocompleteRunner");

		if (args.containsOption("phase") && Integer.parseInt(args.getOptionValues("phase").get(0)) == 1) {
		} else if (args.containsOption("phase") && Integer.parseInt(args.getOptionValues("phase").get(0)) == 2) {

			URI uri = UriComponentsBuilder.fromUri(integrationProperties.getBiosampleSubmissionUri())
					.pathSegment("samples").pathSegment("autocomplete").build().toUri();

			log.info("GETting from "+uri);
			RequestEntity<Void> request = RequestEntity.get(uri).accept(MediaType.APPLICATION_JSON).build();
			ResponseEntity<Autocomplete> response = restTemplate.exchange(request, new ParameterizedTypeReference<Autocomplete>(){});
			//check that there is at least one sample returned
			//if there are zero, then probably nothing was indexed
			if (response.getBody().getSuggestions().size() <= 0) {
				throw new RuntimeException("No autocomplete suggestions found!");
			}
		}
		
		//if we got here without throwing, then we finished sucessfully
		exitCode = 0;
		log.info("Finished RestAutocompleteRunner");
	}


	@Override
	public int getExitCode() {
		return exitCode;
	}

}
