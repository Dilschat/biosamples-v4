package uk.ac.ebi.biosamples.client.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.hateoas.MediaTypes;
import org.springframework.hateoas.PagedResources;
import org.springframework.hateoas.Resource;
import org.springframework.hateoas.client.Hop;
import org.springframework.hateoas.client.Traverson;
import org.springframework.http.HttpStatus;
import org.springframework.http.RequestEntity;
import org.springframework.http.ResponseEntity;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.HttpStatusCodeException;
import org.springframework.web.client.RestOperations;
import org.springframework.web.util.UriComponentsBuilder;
import uk.ac.ebi.biosamples.client.utils.IterableResourceFetchAll;
import uk.ac.ebi.biosamples.model.Sample;
import uk.ac.ebi.biosamples.model.filter.Filter;

import java.net.URI;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Future;
import java.util.stream.Collectors;

public class SampleRetrievalService {

	private Logger log = LoggerFactory.getLogger(getClass());
	
	public static final DateTimeFormatter solrFormatter = DateTimeFormatter.ofPattern("YYYY-MM-dd'T'HH:mm:ss'Z'");

	private static final ParameterizedTypeReference<PagedResources<Resource<Sample>>> parameterizedTypeReferencePagedResourcesSample = new ParameterizedTypeReference<PagedResources<Resource<Sample>>>(){};
	
	private final Traverson traverson;
	private final ExecutorService executor;
	private final RestOperations restOperations;
	private final int pageSize;

	
	
	public SampleRetrievalService(RestOperations restOperations, Traverson traverson,
			ExecutorService executor, int pageSize) {
		this.restOperations = restOperations;
		this.traverson = traverson;
		this.executor = executor;
		this.pageSize = pageSize;
	}

	/**
	 * This will get an existing sample from biosamples using the accession
	 * 
	 * @param sample
	 * @return
	 */
	public Future<Optional<Resource<Sample>>> fetch(String accession) {
		return executor.submit(new FetchCallable(accession));
	}

	public PagedResources<Resource<Sample>> search(String text, int page, int size) {
		//TODO remove duplicate with other search methods

		//TODO make a proper HAL link to do this properly
		return this.search(text, Collections.EMPTY_LIST, page, size);

	}

	public PagedResources<Resource<Sample>> search(String text, Collection<Filter> filters, int page, int size) {
		MultiValueMap<String,String> params = new LinkedMultiValueMap<>();
		params.add("page", Integer.toString(page));
		params.add("size", Integer.toString(size));
		params.add("searchTerm", !text.isEmpty() ? text : "*:*");
		for (Filter filter: filters) {
            params.add("filter", filter.getSerialization());
		}

		params = encodePlusInQueryParameters(params);

		URI uri = UriComponentsBuilder.fromUriString(traverson.follow("samples").asLink().getHref())
				.queryParams(params)
				.build()
				.toUri();

		log.info("GETing " + uri);

		RequestEntity<Void> requestEntity = RequestEntity.get(uri).accept(MediaTypes.HAL_JSON).build();
		ResponseEntity<PagedResources<Resource<Sample>>> responseEntity = restOperations.exchange(requestEntity,
				new ParameterizedTypeReference<PagedResources<Resource<Sample>>>() {
				});

		if (!responseEntity.getStatusCode().is2xxSuccessful()) {
			throw new RuntimeException("Problem GETing samples");
		}


		log.info("GETted " + uri);

		return responseEntity.getBody();
	}

	private class FetchCallable implements Callable<Optional<Resource<Sample>>> {

		private final String accession;

		public FetchCallable(String accession) {
			this.accession = accession;
		}

		@Override
		public Optional<Resource<Sample>> call() throws Exception {

			URI uri = URI.create(traverson.follow("samples")
					.follow(Hop.rel("sample").withParameter("accession", accession))
					.asLink().getHref());
			
			log.info("GETing " + uri);

			RequestEntity<Void> requestEntity = RequestEntity.get(uri).accept(MediaTypes.HAL_JSON).build();
			
			ResponseEntity<Resource<Sample>> responseEntity = null;
			try {
				responseEntity = restOperations.exchange(requestEntity,
					new ParameterizedTypeReference<Resource<Sample>>() {
					});
			} catch (HttpStatusCodeException e) {
				if (e.getStatusCode().equals(HttpStatus.FORBIDDEN)
						|| e.getStatusCode().equals(HttpStatus.NOT_FOUND)) {
					return Optional.empty();
				} else {
					throw e;
				}
			}
			log.info("GETted " + uri);

			return Optional.of(responseEntity.getBody());
		}
	}
	
	public Iterable<Resource<Sample>> fetchAll() {
		MultiValueMap<String,String> params = new LinkedMultiValueMap<>();		
		params.add("size", Integer.toString(pageSize));
		return new IterableResourceFetchAll<Sample>(executor, traverson, restOperations,
				parameterizedTypeReferencePagedResourcesSample, 
				params,	"samples");
	}
	
	public Iterable<Resource<Sample>> fetchAll(String text) {
		MultiValueMap<String,String> params = new LinkedMultiValueMap<>();		
		params.add("text", text);
		params.add("size", Integer.toString(pageSize));
		return new IterableResourceFetchAll<Sample>(executor, traverson, restOperations,
				parameterizedTypeReferencePagedResourcesSample, 
				params,	"samples");
	}

	public Iterable<Resource<Sample>> fetchAll(String text, Collection<Filter> filterCollection) {
		MultiValueMap<String,String> params = new LinkedMultiValueMap<>();
		params.add("text", text);
		for (Filter filter: filterCollection) {
			params.add("filter", filter.getSerialization());
		}
		params.add("size", Integer.toString(pageSize));

		params = encodePlusInQueryParameters(params);

		return new IterableResourceFetchAll<Sample>(executor, traverson, restOperations,
				parameterizedTypeReferencePagedResourcesSample,
				params,	"samples");

	}

    // FIXME, If you can! The only movie where a plus goes incognito to be actually recognize by the system
    // Only way to keep the + in a (not encoded) query parameter is to force encoding
	private MultiValueMap<String, String> encodePlusInQueryParameters(MultiValueMap<String, String> queryParameters) {
	    MultiValueMap<String,String> encodedQueryParameters = new LinkedMultiValueMap<>();
	    for (Map.Entry<String, List<String>> param: queryParameters.entrySet()) {
            encodedQueryParameters.put(
                    param.getKey(),
                    param.getValue().stream().map(v->v.replaceAll("\\+","%2B")).collect(Collectors.toList()));
        }
        return encodedQueryParameters;
    }


//	public Iterable<Resource<Sample>> fetchUpdatedAfter(Instant updatedAfter) {
//		MultiValueMap<String,String> params = new LinkedMultiValueMap<>();
//		params.add("size", Integer.toString(pageSize));
//		params.add("updatedafter", solrFormatter.format(updatedAfter));
//		return new IterableResourceFetchAll<Sample>(executor, traverson, restOperations,
//				parameterizedTypeReferencePagedResourcesSample,
//				params,	"samples");
//	}

//	public Iterable<Resource<Sample>> fetchUpdatedBefore(Instant updatedBefore) {
//		MultiValueMap<String,String> params = new LinkedMultiValueMap<>();
//		params.add("size", Integer.toString(pageSize));
//		params.add("updatedbefore", solrFormatter.format(updatedBefore));
//		return new IterableResourceFetchAll<Sample>(executor, traverson, restOperations,
//				parameterizedTypeReferencePagedResourcesSample,
//				params,	"samples");
//	}

//	public Iterable<Resource<Sample>> fetchUpdatedBetween(Instant updatedAfter, Instant updatedBefore) {
//		MultiValueMap<String,String> params = new LinkedMultiValueMap<>();
//		params.add("size", Integer.toString(pageSize));
//		params.add("updatedafter", solrFormatter.format(updatedAfter));
//		params.add("updatedbefore", solrFormatter.format(updatedBefore));
//		return new IterableResourceFetchAll<Sample>(executor, traverson, restOperations,
//				parameterizedTypeReferencePagedResourcesSample,
//				params,	"samples");
//	}

	public Iterable<Optional<Resource<Sample>>> fetchAll(Iterable<String> accessions) {
		return new IterableResourceFetch(accessions);
	}

	private class IterableResourceFetch implements Iterable<Optional<Resource<Sample>>> {

		private final Iterable<String> accessions;

		public IterableResourceFetch(Iterable<String> accessions) {
			this.accessions = accessions;
		}

		@Override
		public Iterator<Optional<Resource<Sample>>> iterator() {
			return new IteratorResourceFetch(accessions.iterator());
		}

		private class IteratorResourceFetch implements Iterator<Optional<Resource<Sample>>> {

			private final Iterator<String> accessions;
			private final Queue<Future<Optional<Resource<Sample>>>> queue = new LinkedList<>();
			// TODO application property this
			private final int queueMaxSize = 1000;

			public IteratorResourceFetch(Iterator<String> accessions) {
				this.accessions = accessions;
			}

			@Override
			public boolean hasNext() {
				if (this.accessions.hasNext()) {
					return true;
				} else if (!queue.isEmpty()) {
					return true;
				}
				return false;
			}

			@Override
			public Optional<Resource<Sample>> next() {
				if (!hasNext()) {
					throw new NoSuchElementException();
				}

				// fill up the queue if possible
				while (queue.size() < queueMaxSize && accessions.hasNext()) {
					log.info("Queue size is " + queue.size());
					String nextAccession = accessions.next();
					queue.add(fetch(nextAccession));
				}

				// get the end of the queue and wait for it to finish if needed
				Future<Optional<Resource<Sample>>> future = queue.poll();
				// this shouldn't happen, but best to check
				if (future == null) {
					throw new NoSuchElementException();
				}

				try {
					return future.get();
				} catch (InterruptedException e) {
					throw new RuntimeException(e);
				} catch (ExecutionException e) {
					throw new RuntimeException(e.getCause());
				}
			}
		}
	}
}
