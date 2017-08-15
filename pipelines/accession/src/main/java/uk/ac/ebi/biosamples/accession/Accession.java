package uk.ac.ebi.biosamples.accession;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.Date;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.SortedSet;
import java.util.TreeSet;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import java.util.concurrent.ThreadPoolExecutor;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.jdbc.core.RowCallbackHandler;
import org.springframework.stereotype.Component;

import uk.ac.ebi.biosamples.PipelinesProperties;
import uk.ac.ebi.biosamples.client.BioSamplesClient;
import uk.ac.ebi.biosamples.model.Attribute;
import uk.ac.ebi.biosamples.model.Sample;
import uk.ac.ebi.biosamples.utils.AdaptiveThreadPoolExecutor;
import uk.ac.ebi.biosamples.utils.ThreadUtils;

@Component
public class Accession implements ApplicationRunner{

	private Logger log = LoggerFactory.getLogger(getClass());

	@Autowired
	private PipelinesProperties pipelinesProperties;


	@Autowired
	private AccessionDao accessionDao;
	@Autowired
	private BioSamplesClient bioSamplesClient;

	@Override
	public void run(ApplicationArguments args) throws Exception {
		log.info("Processing Accession pipeline...");
		
		try (AdaptiveThreadPoolExecutor executorService = AdaptiveThreadPoolExecutor.create(100, 10000, true, 
				pipelinesProperties.getThreadCount(), pipelinesProperties.getThreadCountMax())) {
			Map<String, Future<Void>> futures = new HashMap<>();
			Map<String, Callable<Void>> callables = new HashMap<>();
			int futureMax = pipelinesProperties.getThreadCountMax()*10;
			accessionDao.doAssayAccessionCallback(new AccessionCallbackHandler(executorService, futures, callables, "SAMEA", futureMax));
			accessionDao.doReferenceAccessionCallback(new AccessionCallbackHandler(executorService, futures, callables, "SAME", futureMax));
			accessionDao.doGroupAccessionCallback(new AccessionCallbackHandler(executorService, futures, callables, "SAMEG", futureMax));
			//wait for everything to finish
			ThreadUtils.checkFutures(futures, 0);
		}
	}	
	
	private class AccessionCallbackHandler implements RowCallbackHandler {
		
		private final ThreadPoolExecutor executor;
		private final Map<String, Future<Void>> futures;
		private final Map<String, Callable<Void>> callables;
		private final String prefix;
		private final int futureMax;
		
		public AccessionCallbackHandler(ThreadPoolExecutor executor, Map<String, Future<Void>> futures, Map<String, Callable<Void>> callables, String prefix, int futureMax) {
			this.executor = executor;
			this.futures = futures;
			this.prefix = prefix;
			this.futureMax = futureMax;
			this.callables = callables;
		}

		@Override
		public void processRow(ResultSet rs) throws SQLException {
			int accessionNo = rs.getInt("ACCESSION");
			String userAccession = rs.getString("USER_ACCESSION");
			String submissionAccession = rs.getString("SUBMISSION_ACCESSION");
			Date dateAssigned = rs.getDate("DATE_ASSIGNED");
			boolean deleted = rs.getBoolean("IS_DELETED");

			String accession = prefix+accessionNo;
			log.trace(""+accessionNo+" "+userAccession+" "+submissionAccession+" "+dateAssigned+" "+deleted);
			
			Callable<Void> callable = new AccessionCallable(accession, userAccession, submissionAccession, dateAssigned, deleted);		
			callables.put(accession, callable);
			Future<Void> future = executor.submit(callable);
			futures.put(accession, future);

			try {
				ThreadUtils.checkAndRetryFutures(futures, callables, futureMax, executor);
			} catch (InterruptedException e) {
				log.warn("Interupted while checking for futures", e);
			} 
		}
	}
	
	private class AccessionCallable implements Callable<Void> {
		private final String accession;
		private final String userAccession;
		private final String submissionAccession;
		private final Date dateAssigned;
		private final boolean deleted;
		
		public AccessionCallable(String accession, String userAccession, String submissionAccession, Date dateAssigned, boolean deleted) {
			this.accession = accession;
			this.userAccession = userAccession;
			this.submissionAccession = submissionAccession;
			this.dateAssigned = dateAssigned;
			this.deleted = deleted;			
		}
		
		@Override
		public Void call() throws Exception {
			String name = userAccession;
			LocalDateTime release = LocalDateTime.now().plusYears(100);
			LocalDateTime update = LocalDateTime.ofInstant(Instant.ofEpochMilli(dateAssigned.getTime()), ZoneId.systemDefault());

			SortedSet<Attribute> attributes = new TreeSet<>();
			attributes.add(Attribute.build("other", "migrated from accession database at "+LocalDateTime.now().format(DateTimeFormatter.ISO_DATE_TIME)));
			//this is the migrated domain in an AAP context
			//attributes.add(Attribute.build("submission accession", submissionAccession));
			attributes.add(Attribute.build("deleted", Boolean.toString(deleted)));
			
			Sample sample = Sample.build(name, accession, release, update, attributes, null, null);
			bioSamplesClient.persistSample(sample);
			return null;
		}
	}
}
