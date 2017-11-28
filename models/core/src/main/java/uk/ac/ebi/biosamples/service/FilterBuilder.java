package uk.ac.ebi.biosamples.service;

import org.springframework.stereotype.Service;
import uk.ac.ebi.biosamples.model.filter.*;

import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class FilterBuilder {
    public AttributeFilter.Builder onAttribute(String label) {
        return new AttributeFilter.Builder(label);
    }

    public RelationFilter.Builder onRelation(String label) {
        return new RelationFilter.Builder(label);
    }

    public InverseRelationFilter.Builder onInverseRelation(String label) {
        return new InverseRelationFilter.Builder(label);
    }

    public DateRangeFilter.Builder onDate(String fieldLabel) {
        return new DateRangeFilter.Builder(fieldLabel);
    }

    public DateRangeFilter.Builder onReleaseDate() {
        return new DateRangeFilter.Builder("release");
    }

    public DateRangeFilter.Builder onUpdateDate() {
        return new DateRangeFilter.Builder("update");
    }

    public DomainFilter.Builder onDomain(String domain) {
        return new DomainFilter.Builder(domain);
    }

    public NameFilter.Builder onName(String name) {
        return new NameFilter.Builder(name);
    }

    public AccessionFilter.Builder onAccession(String accession) {
        return new AccessionFilter.Builder(accession);
    }

    public ExternalReferenceDataFilter.Builder onDataFromExternalReference(String extReference) {
        return new ExternalReferenceDataFilter.Builder(extReference);
    }

    public  Filter buildFromString(String serializedFilter) {
        FilterType filterType = FilterType.ofFilterString(serializedFilter);
        List<String> filterParts = filterParts(serializedFilter);

        if (filterParts.size() > 2) {
            return filterType.getBuilderForLabel(filterParts.get(1)).parseContent(filterParts.get(2)).build();
        } else {
            return filterType.getBuilderForLabel(filterParts.get(1)).build();
        }
    }

    private List<String> filterParts(String filterLabelAndValue) {
        // TODO hack, need to be improved
        return Arrays.stream(filterLabelAndValue.split("(?<!\\\\):", 3))
                .map(s -> s.replace("\\:", ":"))
                .collect(Collectors.toList());
    }

    public static FilterBuilder create() {
        return new FilterBuilder();
    }


}