package org.aksw.facete.v3.api;

import java.util.Map.Entry;

import org.aksw.jena_sparql_api.concepts.Concept;
import org.aksw.jena_sparql_api.concepts.UnaryRelation;
import org.apache.jena.query.Query;
import org.apache.jena.rdf.model.Property;
import org.apache.jena.rdf.model.RDFNode;
import org.apache.jena.sparql.core.Var;

import io.reactivex.Flowable;


interface MultiNode {
	
}



// Actually, this is pretty much a resource

/**
 * Query builder for retrieval batch retrieval of related *optional* information for each entity of an underlying
 * relation.
 * 
 * Hence, limit and offset apply to the base relation.
 * 
 * 
 * @author raven
 *
 */
public interface DataQuery<T extends RDFNode> {
	// For every predicate, list how many root root resources there are having this predicate
	//getPredicatesAndRootCount();
	
	Concept fetchPredicates();
	
	DataNode getRoot();
	
	DataMultiNode add(Property property);
	
	
	// Return the same data query with intersection on the given concept
	DataQuery<T> filter(UnaryRelation concept);
		
	DataQuery<T> limit(Long limit);

	default DataQuery<T> limit(Integer limit) {
		return limit(limit == null ? null : limit.longValue());
	}

	DataQuery<T> offset(Long offset);

	default DataQuery<T> offset(Integer offset) {
		return offset(offset == null ? null : offset.longValue());
	}

	DataQuery<T> sample(boolean onOrOff);
	boolean sample();
	
	/**
	 * Return a SPARQL construct query together with the designated root variable
	 * 
	 * @return
	 */
	Entry<Var, Query> toConstructQuery();

	
	Flowable<T> exec();
}
