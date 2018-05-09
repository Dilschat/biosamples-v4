package uk.ac.ebi.biosamples.curation;

import java.time.LocalDate;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Callable;
import java.util.concurrent.Future;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.hateoas.Resource;
import org.springframework.stereotype.Component;

import uk.ac.ebi.biosamples.PipelinesProperties;
import uk.ac.ebi.biosamples.client.BioSamplesClient;
import uk.ac.ebi.biosamples.model.Sample;
import uk.ac.ebi.biosamples.model.filter.DateRangeFilter;
import uk.ac.ebi.biosamples.model.filter.Filter;
import uk.ac.ebi.biosamples.ols.OlsProcessor;
import uk.ac.ebi.biosamples.service.CurationApplicationService;
import uk.ac.ebi.biosamples.utils.AdaptiveThreadPoolExecutor;
import uk.ac.ebi.biosamples.utils.ArgUtils;
import uk.ac.ebi.biosamples.utils.ThreadUtils;

@Component
public class CurationApplicationRunner implements ApplicationRunner {

	private Logger log = LoggerFactory.getLogger(getClass());

	private final BioSamplesClient bioSamplesClient;	
	private final PipelinesProperties pipelinesProperties;
	private final OlsProcessor olsProcessor;
	private final CurationApplicationService curationApplicationService;
	
	public CurationApplicationRunner(BioSamplesClient bioSamplesClient, 
			PipelinesProperties pipelinesProperties, 
			OlsProcessor olsProcessor, 
			CurationApplicationService curationApplicationService) {
		this.bioSamplesClient = bioSamplesClient;
		this.pipelinesProperties = pipelinesProperties;
		this.olsProcessor = olsProcessor;
		this.curationApplicationService = curationApplicationService;
	}
	
	
	@Override
	public void run(ApplicationArguments args) throws Exception {
		
		Collection<Filter> filters = ArgUtils.getDateFilters(args);
		
		try (AdaptiveThreadPoolExecutor executorService = AdaptiveThreadPoolExecutor.create(100, 10000, true, 
				pipelinesProperties.getThreadCount(), pipelinesProperties.getThreadCountMax())) {

			Map<String, Future<Void>> futures = new HashMap<>();
			
			for (Resource<Sample> sampleResource : bioSamplesClient.fetchSampleResourceAll("", filters)) {
				log.trace("Handling "+sampleResource);
				Sample sample = sampleResource.getContent();
				if (sample == null) {
					throw new RuntimeException("Sample should not be null");
				}

				Callable<Void> task = new SampleCurationCallable(bioSamplesClient, sample, olsProcessor, 
						curationApplicationService, pipelinesProperties.getCurationDomain());
				
				futures.put(sample.getAccession(), executorService.submit(task));
			}
			
			log.info("waiting for futures");
			// wait for anything to finish
			ThreadUtils.checkFutures(futures, 0);
		} finally {
			//now print a list of things that failed
			if (SampleCurationCallable.failedQueue.size() > 0) {
				//put the first ones on the queue into a list
				//limit the size of list to avoid overload
				List<String> fails = new LinkedList<>();
				while (fails.size() < 100 && SampleCurationCallable.failedQueue.peek() != null) {
					fails.add(SampleCurationCallable.failedQueue.poll());
				}
				log.info("Failed files ("+SampleCurationCallable.failedQueue.size()+") "+String.join(" , ", fails));
			}
		}
	}

}
